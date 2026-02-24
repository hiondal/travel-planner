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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * ScheduleItemServiceImpl 단위 테스트.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ScheduleItemServiceImplTest {

    @Mock
    private ScheduleItemRepository scheduleItemRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private PlaceServiceClient placeServiceClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ScheduleItemServiceImpl scheduleItemService;

    private Trip sampleTrip;
    private ScheduleItem sampleItem;
    private PlaceServiceClient.PlaceDetail samplePlaceDetail;

    private static final String USER_ID = "user_test_001";
    private static final String TRIP_ID = "trip_test_001";
    private static final String ITEM_ID = "si_test_001";
    private static final String PLACE_ID = "place_abc123";
    private static final String NEW_PLACE_ID = "place_xyz789";

    @BeforeEach
    void setUp() {
        sampleTrip = new Trip(TRIP_ID, USER_ID, "도쿄 3박4일",
            LocalDate.of(2026, 3, 15),
            LocalDate.of(2026, 3, 18),
            "도쿄");

        sampleItem = new ScheduleItem(
            ITEM_ID, TRIP_ID, PLACE_ID, "이치란 라멘",
            LocalDateTime.of(2026, 3, 16, 12, 0),
            "Asia/Tokyo", 1
        );

        samplePlaceDetail = PlaceServiceClient.PlaceDetail.unknown(PLACE_ID);
    }

    @Test
    @DisplayName("장소 추가 - 영업시간 내 정상 추가")
    void addScheduleItem_success() throws Exception {
        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.of(sampleTrip));
        given(placeServiceClient.getPlaceDetail(PLACE_ID))
            .willReturn(samplePlaceDetail);
        given(scheduleItemRepository.countByTripId(TRIP_ID)).willReturn(0);
        given(scheduleItemRepository.save(any(ScheduleItem.class)))
            .willAnswer(inv -> inv.getArgument(0));

        AddScheduleItemRequest request = createAddRequest(PLACE_ID,
            LocalDateTime.of(2026, 3, 16, 12, 0), "Asia/Tokyo", false);

        ScheduleItemAddResult result =
            scheduleItemService.addScheduleItem(TRIP_ID, USER_ID, request);

        assertThat(result.getItem()).isNotNull();
        assertThat(result.isOutsideBusinessHours()).isFalse();
        verify(scheduleItemRepository).save(any(ScheduleItem.class));

        ArgumentCaptor<ScheduleItemAddedEvent> eventCaptor =
            ArgumentCaptor.forClass(ScheduleItemAddedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPlaceId()).isEqualTo(PLACE_ID);
    }

    @Test
    @DisplayName("장소 추가 - 영업시간 외, force=false 시 경고 반환")
    void addScheduleItem_outsideHours_warning() throws Exception {
        PlaceServiceClient.PlaceDetail detailWithHours = createPlaceDetailWithHours();

        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.of(sampleTrip));
        given(placeServiceClient.getPlaceDetail(PLACE_ID))
            .willReturn(detailWithHours);

        AddScheduleItemRequest request = createAddRequest(PLACE_ID,
            LocalDateTime.of(2026, 3, 16, 23, 0), "Asia/Tokyo", false);

        ScheduleItemAddResult result =
            scheduleItemService.addScheduleItem(TRIP_ID, USER_ID, request);

        assertThat(result.getItem()).isNull();
        assertThat(result.isOutsideBusinessHours()).isTrue();
    }

    @Test
    @DisplayName("장소 추가 - 영업시간 외, force=true 시 강제 추가")
    void addScheduleItem_outsideHours_force() throws Exception {
        PlaceServiceClient.PlaceDetail detailWithHours = createPlaceDetailWithHours();

        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.of(sampleTrip));
        given(placeServiceClient.getPlaceDetail(PLACE_ID))
            .willReturn(detailWithHours);
        given(scheduleItemRepository.countByTripId(TRIP_ID)).willReturn(0);
        given(scheduleItemRepository.save(any(ScheduleItem.class)))
            .willAnswer(inv -> inv.getArgument(0));

        AddScheduleItemRequest request = createAddRequest(PLACE_ID,
            LocalDateTime.of(2026, 3, 16, 23, 0), "Asia/Tokyo", true);

        ScheduleItemAddResult result =
            scheduleItemService.addScheduleItem(TRIP_ID, USER_ID, request);

        assertThat(result.getItem()).isNotNull();
        assertThat(result.isOutsideBusinessHours()).isTrue();
        verify(scheduleItemRepository).save(any(ScheduleItem.class));
    }

    @Test
    @DisplayName("장소 추가 - 여행 없으면 ResourceNotFoundException")
    void addScheduleItem_tripNotFound() throws Exception {
        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.empty());

        AddScheduleItemRequest request = createAddRequest(PLACE_ID,
            LocalDateTime.now(), "Asia/Tokyo", false);

        assertThatThrownBy(() ->
            scheduleItemService.addScheduleItem(TRIP_ID, USER_ID, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("장소 삭제 - 성공")
    void deleteScheduleItem_success() {
        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.of(sampleTrip));
        given(scheduleItemRepository.findByIdAndTripId(ITEM_ID, TRIP_ID))
            .willReturn(Optional.of(sampleItem));

        scheduleItemService.deleteScheduleItem(TRIP_ID, ITEM_ID, USER_ID);

        verify(scheduleItemRepository).deleteById(ITEM_ID);

        ArgumentCaptor<ScheduleItemDeletedEvent> eventCaptor =
            ArgumentCaptor.forClass(ScheduleItemDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getScheduleItemId()).isEqualTo(ITEM_ID);
    }

    @Test
    @DisplayName("장소 삭제 - 아이템 없으면 ResourceNotFoundException")
    void deleteScheduleItem_notFound() {
        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.of(sampleTrip));
        given(scheduleItemRepository.findByIdAndTripId(ITEM_ID, TRIP_ID))
            .willReturn(Optional.empty());

        assertThatThrownBy(() ->
            scheduleItemService.deleteScheduleItem(TRIP_ID, ITEM_ID, USER_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("장소 교체 - 성공")
    void replaceScheduleItem_success() {
        PlaceServiceClient.PlaceDetail newPlaceDetail =
            PlaceServiceClient.PlaceDetail.unknown(NEW_PLACE_ID);

        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.of(sampleTrip));
        given(scheduleItemRepository.findByIdAndTripId(ITEM_ID, TRIP_ID))
            .willReturn(Optional.of(sampleItem));
        given(placeServiceClient.getPlaceDetail(NEW_PLACE_ID))
            .willReturn(newPlaceDetail);
        given(scheduleItemRepository.save(any(ScheduleItem.class)))
            .willAnswer(inv -> inv.getArgument(0));
        given(scheduleItemRepository.findByTripIdOrderByVisitDatetimeAsc(TRIP_ID))
            .willReturn(List.of(sampleItem));

        ReplaceResult result =
            scheduleItemService.replaceScheduleItem(TRIP_ID, ITEM_ID, NEW_PLACE_ID, USER_ID);

        assertThat(result.getItem()).isNotNull();
        assertThat(result.getOriginalPlaceId()).isEqualTo(PLACE_ID);

        ArgumentCaptor<ScheduleItemReplacedEvent> eventCaptor =
            ArgumentCaptor.forClass(ScheduleItemReplacedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getOldPlaceId()).isEqualTo(PLACE_ID);
        assertThat(eventCaptor.getValue().getNewPlaceId()).isEqualTo(NEW_PLACE_ID);
    }

    private AddScheduleItemRequest createAddRequest(
            String placeId, LocalDateTime visitDatetime,
            String timezone, boolean force) throws Exception {
        AddScheduleItemRequest req = new AddScheduleItemRequest();
        setField(req, "placeId", placeId);
        setField(req, "visitDatetime", visitDatetime);
        setField(req, "timezone", timezone);
        setField(req, "force", force);
        return req;
    }

    private PlaceServiceClient.PlaceDetail createPlaceDetailWithHours() throws Exception {
        PlaceServiceClient.BusinessHour hour = new PlaceServiceClient.BusinessHour();
        setField(hour, "day", "MON");
        setField(hour, "open", "11:00");
        setField(hour, "close", "22:00");

        PlaceServiceClient.PlaceDetail detail = new PlaceServiceClient.PlaceDetail();
        setField(detail, "placeId", PLACE_ID);
        setField(detail, "name", "이치란 라멘");
        setField(detail, "businessHours", List.of(hour));
        setField(detail, "timezone", "Asia/Tokyo");
        return detail;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
