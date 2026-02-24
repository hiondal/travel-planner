package com.travelplanner.schedule.service;

import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.common.exception.ValidationException;
import com.travelplanner.schedule.domain.ScheduleItem;
import com.travelplanner.schedule.domain.Trip;
import com.travelplanner.schedule.dto.internal.ScheduleResult;
import com.travelplanner.schedule.dto.request.CreateTripRequest;
import com.travelplanner.schedule.repository.ScheduleItemRepository;
import com.travelplanner.schedule.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * TripServiceImpl 단위 테스트.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ScheduleItemRepository scheduleItemRepository;

    @InjectMocks
    private TripServiceImpl tripService;

    private CreateTripRequest createTripRequest;
    private Trip sampleTrip;
    private static final String USER_ID = "user_test_001";
    private static final String TRIP_ID = "trip_test_001";

    @BeforeEach
    void setUp() throws Exception {
        createTripRequest = createRequest("도쿄 3박4일",
            LocalDate.of(2026, 3, 15),
            LocalDate.of(2026, 3, 18),
            "도쿄");

        sampleTrip = new Trip(TRIP_ID, USER_ID, "도쿄 3박4일",
            LocalDate.of(2026, 3, 15),
            LocalDate.of(2026, 3, 18),
            "도쿄");
    }

    @Test
    @DisplayName("여행 목록 조회 - 성공")
    void getTrips_success() {
        given(tripRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
            .willReturn(List.of(sampleTrip));

        List<Trip> result = tripService.getTrips(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("도쿄 3박4일");
    }

    @Test
    @DisplayName("여행 생성 - 성공")
    void createTrip_success() {
        given(tripRepository.save(any(Trip.class))).willAnswer(inv -> inv.getArgument(0));

        Trip result = tripService.createTrip(USER_ID, createTripRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("도쿄 3박4일");
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getCity()).isEqualTo("도쿄");
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    @DisplayName("여행 생성 - 종료일이 시작일 이전이면 ValidationException")
    void createTrip_invalidDateRange() throws Exception {
        CreateTripRequest invalidRequest = createRequest("잘못된 여행",
            LocalDate.of(2026, 3, 18),
            LocalDate.of(2026, 3, 15),
            "도쿄");

        assertThatThrownBy(() -> tripService.createTrip(USER_ID, invalidRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("종료일");
    }

    @Test
    @DisplayName("여행 조회 - 성공")
    void getTrip_success() {
        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.of(sampleTrip));

        Trip result = tripService.getTrip(TRIP_ID, USER_ID);

        assertThat(result.getId()).isEqualTo(TRIP_ID);
        assertThat(result.getName()).isEqualTo("도쿄 3박4일");
    }

    @Test
    @DisplayName("여행 조회 - 존재하지 않으면 ResourceNotFoundException")
    void getTrip_notFound() {
        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTrip(TRIP_ID, USER_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("일정표 조회 - 성공")
    void getSchedule_success() {
        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.of(sampleTrip));
        given(scheduleItemRepository.findByTripIdOrderByVisitDatetimeAsc(TRIP_ID))
            .willReturn(List.of());

        ScheduleResult result = tripService.getSchedule(TRIP_ID, USER_ID);

        assertThat(result.getTrip().getId()).isEqualTo(TRIP_ID);
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    @DisplayName("일정표 조회 - 여행 없으면 ResourceNotFoundException")
    void getSchedule_tripNotFound() {
        given(tripRepository.findByIdAndUserId(TRIP_ID, USER_ID))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getSchedule(TRIP_ID, USER_ID))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateTripRequest createRequest(String name, LocalDate startDate,
                                            LocalDate endDate, String city) throws Exception {
        CreateTripRequest req = new CreateTripRequest();
        setField(req, "name", name);
        setField(req, "startDate", startDate);
        setField(req, "endDate", endDate);
        setField(req, "city", city);
        return req;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
