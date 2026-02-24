package com.travelplanner.schedule.service;

import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.common.exception.ValidationException;
import com.travelplanner.schedule.domain.ScheduleItem;
import com.travelplanner.schedule.domain.Trip;
import com.travelplanner.schedule.dto.internal.ScheduleResult;
import com.travelplanner.schedule.dto.request.CreateTripRequest;
import com.travelplanner.schedule.repository.ScheduleItemRepository;
import com.travelplanner.schedule.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 여행 서비스 구현체.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final ScheduleItemRepository scheduleItemRepository;

    @Override
    public List<Trip> getTrips(String userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Trip createTrip(String userId, CreateTripRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());

        String tripId = UUID.randomUUID().toString();
        Trip trip = new Trip(
            tripId,
            userId,
            request.getName(),
            request.getStartDate(),
            request.getEndDate(),
            request.getCity()
        );

        Trip saved = tripRepository.save(trip);
        log.info("여행 생성 완료 - tripId: {}, userId: {}", saved.getId(), userId);
        return saved;
    }

    @Override
    public Trip getTrip(String tripId, String userId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND",
                "여행을 찾을 수 없습니다. tripId: " + tripId));
    }

    @Override
    public ScheduleResult getSchedule(String tripId, String userId) {
        return getSchedule(tripId, userId, null);
    }

    @Override
    public ScheduleResult getSchedule(String tripId, String userId, LocalDate date) {
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND",
                "여행을 찾을 수 없습니다. tripId: " + tripId));

        List<ScheduleItem> items;
        if (date != null) {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            items = scheduleItemRepository.findByTripIdAndDate(tripId, startOfDay, endOfDay);
        } else {
            items = scheduleItemRepository.findByTripIdOrderByVisitDatetimeAsc(tripId);
        }

        return ScheduleResult.builder()
            .trip(trip)
            .items(items)
            .build();
    }

    @Override
    @Transactional
    public void deleteTrip(String tripId, String userId) {
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND",
                "여행을 찾을 수 없습니다. tripId: " + tripId));

        scheduleItemRepository.deleteAllByTripId(tripId);
        tripRepository.delete(trip);
        log.info("여행 삭제 완료 - tripId: {}, userId: {}", tripId, userId);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("end_date",
                "여행 종료일은 시작일 이후여야 합니다.", endDate);
        }
    }
}
