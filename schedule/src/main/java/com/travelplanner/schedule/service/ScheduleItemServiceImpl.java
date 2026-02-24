package com.travelplanner.schedule.service;

import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.schedule.client.PlaceServiceClient;
import com.travelplanner.schedule.domain.ScheduleItem;
import com.travelplanner.schedule.domain.Trip;
import com.travelplanner.schedule.dto.internal.ReplaceResult;
import com.travelplanner.schedule.dto.internal.ScheduleItemAddResult;
import com.travelplanner.schedule.dto.request.AddScheduleItemRequest;
import com.travelplanner.schedule.event.ScheduleItemAddedEvent;
import com.travelplanner.schedule.event.ScheduleItemDeletedEvent;
import com.travelplanner.schedule.event.ScheduleItemReplacedEvent;
import com.travelplanner.schedule.repository.ScheduleItemRepository;
import com.travelplanner.schedule.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 일정 아이템 서비스 구현체.
 *
 * <p>장소 추가/삭제/교체 로직을 처리하며, 각 변경 시 ApplicationEvent를 발행한다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScheduleItemServiceImpl implements ScheduleItemService {

    private final ScheduleItemRepository scheduleItemRepository;
    private final TripRepository tripRepository;
    private final PlaceServiceClient placeServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ScheduleItemAddResult addScheduleItem(
            String tripId, String userId, AddScheduleItemRequest request) {

        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND",
                "여행을 찾을 수 없습니다. tripId: " + tripId));

        PlaceServiceClient.PlaceDetail placeDetail =
            placeServiceClient.getPlaceDetail(request.getPlaceId());

        boolean outsideHours = checkBusinessHours(
            placeDetail.getBusinessHours(),
            request.getVisitDatetime(),
            request.getTimezone()
        );

        if (outsideHours && !request.isForce()) {
            String range = buildBusinessHoursRange(
                placeDetail.getBusinessHours(), request.getVisitDatetime(), request.getTimezone());
            return ScheduleItemAddResult.builder()
                .item(null)
                .outsideBusinessHours(true)
                .businessHoursRange(range)
                .build();
        }

        int order = scheduleItemRepository.countByTripId(tripId) + 1;
        String itemId = "si_" + UUID.randomUUID().toString().replace("-", "");

        ScheduleItem item = new ScheduleItem(
            itemId,
            tripId,
            request.getPlaceId(),
            placeDetail.getName(),
            request.getVisitDatetime(),
            request.getTimezone(),
            order
        );

        if (outsideHours) {
            item.markOutsideBusinessHours();
        }

        ScheduleItem saved = scheduleItemRepository.save(item);
        log.info("일정 장소 추가 완료 - itemId: {}, tripId: {}", saved.getId(), tripId);

        eventPublisher.publishEvent(new ScheduleItemAddedEvent(
            saved.getId(),
            saved.getPlaceId(),
            saved.getPlaceName(),
            saved.getVisitDatetime(),
            saved.getTimezone(),
            tripId,
            userId
        ));

        return ScheduleItemAddResult.builder()
            .item(saved)
            .outsideBusinessHours(outsideHours)
            .businessHoursRange(null)
            .build();
    }

    @Override
    @Transactional
    public void deleteScheduleItem(String tripId, String itemId, String userId) {
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND",
                "여행을 찾을 수 없습니다. tripId: " + tripId));

        ScheduleItem item = scheduleItemRepository.findByIdAndTripId(itemId, tripId)
            .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND",
                "일정 아이템을 찾을 수 없습니다. itemId: " + itemId));

        String placeId = item.getPlaceId();
        trip.getScheduleItems().removeIf(si -> si.getId().equals(itemId));
        log.info("일정 장소 삭제 완료 - itemId: {}, tripId: {}", itemId, tripId);

        eventPublisher.publishEvent(new ScheduleItemDeletedEvent(itemId, placeId, tripId));
    }

    @Override
    @Transactional
    public ReplaceResult replaceScheduleItem(
            String tripId, String itemId, String newPlaceId, String userId) {

        tripRepository.findByIdAndUserId(tripId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND",
                "여행을 찾을 수 없습니다. tripId: " + tripId));

        ScheduleItem item = scheduleItemRepository.findByIdAndTripId(itemId, tripId)
            .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND",
                "일정 아이템을 찾을 수 없습니다. itemId: " + itemId));

        String originalPlaceId = item.getPlaceId();
        String originalPlaceName = item.getPlaceName();

        PlaceServiceClient.PlaceDetail newPlaceDetail =
            placeServiceClient.getPlaceDetail(newPlaceId);

        item.updatePlace(newPlaceId, newPlaceDetail.getName());
        scheduleItemRepository.save(item);
        log.info("일정 장소 교체 완료 - itemId: {}, oldPlaceId: {}, newPlaceId: {}",
            itemId, originalPlaceId, newPlaceId);

        List<ScheduleItem> updatedItems =
            scheduleItemRepository.findByTripIdOrderByVisitDatetimeAsc(tripId);

        eventPublisher.publishEvent(
            new ScheduleItemReplacedEvent(itemId, originalPlaceId, newPlaceId, tripId));

        return ReplaceResult.builder()
            .item(item)
            .originalPlaceId(originalPlaceId)
            .originalPlaceName(originalPlaceName)
            .travelTimeDiffMinutes(0)
            .updatedItems(updatedItems)
            .build();
    }

    /**
     * 방문 일시가 영업시간 외인지 확인한다.
     *
     * @param businessHours 영업시간 목록
     * @param visitDatetime 방문 일시
     * @param timezone IANA 타임존
     * @return 영업시간 외이면 true
     */
    private boolean checkBusinessHours(
            List<PlaceServiceClient.BusinessHour> businessHours,
            LocalDateTime visitDatetime,
            String timezone) {

        if (businessHours == null || businessHours.isEmpty()) {
            return false;
        }

        try {
            ZonedDateTime zdt = visitDatetime.atZone(ZoneId.of(timezone));
            DayOfWeek dayOfWeek = zdt.getDayOfWeek();
            String dayName = dayOfWeek.name().substring(0, 3).toUpperCase();

            return businessHours.stream()
                .filter(bh -> bh.getDay() != null &&
                    bh.getDay().toUpperCase().startsWith(dayName))
                .findFirst()
                .map(bh -> !bh.isWithinHours(zdt.toLocalTime()))
                .orElse(false);
        } catch (Exception e) {
            log.warn("영업시간 확인 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }

    private String buildBusinessHoursRange(
            List<PlaceServiceClient.BusinessHour> businessHours,
            LocalDateTime visitDatetime,
            String timezone) {

        if (businessHours == null || businessHours.isEmpty()) {
            return "정보 없음";
        }
        try {
            ZonedDateTime zdt = visitDatetime.atZone(ZoneId.of(timezone));
            String dayName = zdt.getDayOfWeek().name().substring(0, 3).toUpperCase();
            return businessHours.stream()
                .filter(bh -> bh.getDay() != null &&
                    bh.getDay().toUpperCase().startsWith(dayName))
                .findFirst()
                .map(bh -> bh.getOpen() + "~" + bh.getClose())
                .orElse("정보 없음");
        } catch (Exception e) {
            return "정보 없음";
        }
    }
}
