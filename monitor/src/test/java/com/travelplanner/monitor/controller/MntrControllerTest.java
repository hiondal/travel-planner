package com.travelplanner.monitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplanner.common.exception.GlobalExceptionHandler;
import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.monitor.domain.*;
import com.travelplanner.monitor.dto.internal.StatusDetail;
import com.travelplanner.monitor.service.BadgeService;
import com.travelplanner.monitor.service.DataCollectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MntrController 단위 테스트.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@WebMvcTest(controllers = MntrController.class)
@Import({GlobalExceptionHandler.class,
    com.travelplanner.monitor.config.SecurityConfig.class})
class MntrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BadgeService badgeService;

    @MockBean
    private DataCollectionService dataCollectionService;

    private static final String PLACE_ID = "place_abc123";

    @Test
    @DisplayName("GET /api/v1/badges - 배지 목록 조회 성공")
    void getBadges_success() throws Exception {
        StatusBadge badge = new StatusBadge(
            PLACE_ID, PlaceStatusEnum.GREEN, LocalDateTime.now());

        given(badgeService.getBadgeStatuses(any())).willReturn(List.of(badge));

        mockMvc.perform(get("/api/v1/badges")
                .param("place_ids", PLACE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.badges").isArray())
            .andExpect(jsonPath("$.data.badges[0].place_id").value(PLACE_ID))
            .andExpect(jsonPath("$.data.badges[0].status").value("GREEN"))
            .andExpect(jsonPath("$.data.badges[0].icon").value("CHECK"))
            .andExpect(jsonPath("$.data.badges[0].color_hex").value("#4CAF50"));
    }

    @Test
    @DisplayName("GET /api/v1/badges - 여러 장소 배지 조회")
    void getBadges_multiplePlaces() throws Exception {
        StatusBadge badge1 = new StatusBadge(
            "place_001", PlaceStatusEnum.GREEN, LocalDateTime.now());
        StatusBadge badge2 = new StatusBadge(
            "place_002", PlaceStatusEnum.YELLOW, LocalDateTime.now());

        given(badgeService.getBadgeStatuses(any())).willReturn(List.of(badge1, badge2));

        mockMvc.perform(get("/api/v1/badges")
                .param("place_ids", "place_001,place_002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.badges").isArray())
            .andExpect(jsonPath("$.data.badges.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/badges/{placeId}/detail - 상태 상세 조회 성공")
    void getStatusDetail_success() throws Exception {
        StatusDetail detail = StatusDetail.builder()
            .placeId(PLACE_ID)
            .placeName("시부야 스크램블 교차로")
            .overallStatus(PlaceStatusEnum.YELLOW)
            .businessStatus(new BusinessStatusData("OPEN", false))
            .weatherData(new WeatherData(15, "Clear", false))
            .travelTimeData(new TravelTimeData(15, 8, 620, false))
            .congestionValue("혼잡")
            .congestionUnknown(false)
            .reason("혼잡도 높음")
            .showAlternativeButton(true)
            .updatedAt(LocalDateTime.now())
            .build();

        given(badgeService.getStatusDetail(PLACE_ID)).willReturn(detail);

        mockMvc.perform(get("/api/v1/badges/{placeId}/detail", PLACE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.place_id").value(PLACE_ID))
            .andExpect(jsonPath("$.data.overall_status").value("YELLOW"))
            .andExpect(jsonPath("$.data.show_alternative_button").value(true))
            .andExpect(jsonPath("$.data.reason").value("혼잡도 높음"));
    }

    @Test
    @DisplayName("GET /api/v1/badges/{placeId}/detail - 없는 장소 404")
    void getStatusDetail_notFound() throws Exception {
        given(badgeService.getStatusDetail(PLACE_ID))
            .willThrow(new ResourceNotFoundException("NOT_FOUND", "장소 상태 정보를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/badges/{placeId}/detail", PLACE_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/monitor/collect - 수집 트리거 202 반환")
    void triggerDataCollection_success() throws Exception {
        CollectionJob job = new CollectionJob(
            "job_001", "ACCEPTED", 5, LocalDateTime.now());

        given(dataCollectionService.triggerCollection(any(), any())).willReturn(job);

        String body = """
            {
              "triggered_by": "scheduler",
              "triggered_at": "2026-03-16T10:00:00"
            }
            """;

        mockMvc.perform(post("/api/v1/monitor/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.target_count").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/monitor/collect - 요청 본문 없이도 202 반환")
    void triggerDataCollection_noBody() throws Exception {
        CollectionJob job = new CollectionJob(
            "job_001", "ACCEPTED", 0, LocalDateTime.now());

        given(dataCollectionService.triggerCollection(any(), any())).willReturn(job);

        mockMvc.perform(post("/api/v1/monitor/collect"))
            .andExpect(status().isAccepted());
    }
}
