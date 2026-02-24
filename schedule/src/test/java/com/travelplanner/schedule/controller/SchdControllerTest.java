package com.travelplanner.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplanner.common.exception.GlobalExceptionHandler;
import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.schedule.domain.Trip;
import com.travelplanner.schedule.dto.internal.ReplaceResult;
import com.travelplanner.schedule.dto.internal.ScheduleItemAddResult;
import com.travelplanner.schedule.dto.internal.ScheduleResult;
import com.travelplanner.schedule.domain.ScheduleItem;
import com.travelplanner.schedule.service.ScheduleItemService;
import com.travelplanner.schedule.service.TripService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SchdController 단위 테스트.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@WebMvcTest(controllers = SchdController.class)
@Import({GlobalExceptionHandler.class,
    com.travelplanner.schedule.config.SecurityConfig.class})
class SchdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripService tripService;

    @MockBean
    private ScheduleItemService scheduleItemService;

    private static final String TRIP_ID = "trip_test_001";
    private static final String ITEM_ID = "si_test_001";
    private static final String PLACE_ID = "place_abc123";

    private Trip sampleTrip() {
        return new Trip(TRIP_ID, "user_001", "도쿄 3박4일",
            LocalDate.of(2026, 3, 15),
            LocalDate.of(2026, 3, 18),
            "도쿄");
    }

    private ScheduleItem sampleItem() {
        return new ScheduleItem(ITEM_ID, TRIP_ID, PLACE_ID, "이치란 라멘",
            LocalDateTime.of(2026, 3, 16, 12, 0),
            "Asia/Tokyo", 1);
    }

    @Test
    @DisplayName("GET /api/v1/trips - 여행 목록 조회 성공")
    void getTrips_success() throws Exception {
        given(tripService.getTrips(any())).willReturn(List.of(sampleTrip()));

        mockMvc.perform(get("/api/v1/trips"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.trips").isArray())
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/trips - 여행 생성 성공")
    void createTrip_success() throws Exception {
        given(tripService.createTrip(any(), any())).willReturn(sampleTrip());

        String body = """
            {
              "name": "도쿄 3박4일",
              "start_date": "2026-03-15",
              "end_date": "2026-03-18",
              "city": "도쿄"
            }
            """;

        mockMvc.perform(post("/api/v1/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.trip_id").value(TRIP_ID))
            .andExpect(jsonPath("$.data.name").value("도쿄 3박4일"));
    }

    @Test
    @DisplayName("POST /api/v1/trips - 여행명 누락 시 400")
    void createTrip_missingName() throws Exception {
        String body = """
            {
              "start_date": "2026-03-15",
              "end_date": "2026-03-18",
              "city": "도쿄"
            }
            """;

        mockMvc.perform(post("/api/v1/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/trips/{tripId} - 여행 조회 성공")
    void getTrip_success() throws Exception {
        given(tripService.getTrip(eq(TRIP_ID), any())).willReturn(sampleTrip());

        mockMvc.perform(get("/api/v1/trips/{tripId}", TRIP_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.trip_id").value(TRIP_ID));
    }

    @Test
    @DisplayName("GET /api/v1/trips/{tripId} - 없는 여행 404")
    void getTrip_notFound() throws Exception {
        given(tripService.getTrip(eq(TRIP_ID), any()))
            .willThrow(new ResourceNotFoundException("NOT_FOUND", "여행을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/trips/{tripId}", TRIP_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/trips/{tripId}/schedule - 일정표 조회 성공")
    void getSchedule_success() throws Exception {
        ScheduleResult result = ScheduleResult.builder()
            .trip(sampleTrip())
            .items(List.of(sampleItem()))
            .build();

        given(tripService.getSchedule(eq(TRIP_ID), any())).willReturn(result);

        mockMvc.perform(get("/api/v1/trips/{tripId}/schedule", TRIP_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.trip_id").value(TRIP_ID))
            .andExpect(jsonPath("$.data.schedule_items").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedule-items - 장소 추가 성공")
    void addScheduleItem_success() throws Exception {
        ScheduleItemAddResult result = ScheduleItemAddResult.builder()
            .item(sampleItem())
            .outsideBusinessHours(false)
            .businessHoursRange(null)
            .build();

        given(scheduleItemService.addScheduleItem(eq(TRIP_ID), any(), any()))
            .willReturn(result);

        String body = """
            {
              "place_id": "place_abc123",
              "visit_datetime": "2026-03-16T12:00:00",
              "timezone": "Asia/Tokyo"
            }
            """;

        mockMvc.perform(post("/api/v1/trips/{tripId}/schedule-items", TRIP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.place_id").value(PLACE_ID));
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedule-items - 영업시간 외 경고 200 반환")
    void addScheduleItem_outsideHoursWarning() throws Exception {
        ScheduleItemAddResult result = ScheduleItemAddResult.builder()
            .item(null)
            .outsideBusinessHours(true)
            .businessHoursRange("11:00~22:00")
            .build();

        given(scheduleItemService.addScheduleItem(eq(TRIP_ID), any(), any()))
            .willReturn(result);

        String body = """
            {
              "place_id": "place_abc123",
              "visit_datetime": "2026-03-16T23:00:00",
              "timezone": "Asia/Tokyo"
            }
            """;

        mockMvc.perform(post("/api/v1/trips/{tripId}/schedule-items", TRIP_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warning").value("OUTSIDE_BUSINESS_HOURS"));
    }

    @Test
    @DisplayName("DELETE /api/v1/trips/{tripId}/schedule-items/{itemId} - 삭제 성공")
    void deleteScheduleItem_success() throws Exception {
        doNothing().when(scheduleItemService)
            .deleteScheduleItem(eq(TRIP_ID), eq(ITEM_ID), any());

        mockMvc.perform(delete("/api/v1/trips/{tripId}/schedule-items/{itemId}",
                TRIP_ID, ITEM_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /api/v1/trips/{tripId}/schedule-items/{itemId}/replace - 교체 성공")
    void replaceScheduleItem_success() throws Exception {
        ScheduleItem newItem = new ScheduleItem(ITEM_ID, TRIP_ID, "place_xyz789", "후쿠로쿠 라멘",
            LocalDateTime.of(2026, 3, 16, 12, 0), "Asia/Tokyo", 1);

        ReplaceResult result = ReplaceResult.builder()
            .item(newItem)
            .originalPlaceId(PLACE_ID)
            .originalPlaceName("이치란 라멘")
            .travelTimeDiffMinutes(-5)
            .updatedItems(List.of(newItem))
            .build();

        given(scheduleItemService.replaceScheduleItem(eq(TRIP_ID), eq(ITEM_ID), any(), any()))
            .willReturn(result);

        String body = """
            {
              "new_place_id": "place_xyz789"
            }
            """;

        mockMvc.perform(put("/api/v1/trips/{tripId}/schedule-items/{itemId}/replace",
                TRIP_ID, ITEM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.schedule_item_id").value(ITEM_ID));
    }
}
