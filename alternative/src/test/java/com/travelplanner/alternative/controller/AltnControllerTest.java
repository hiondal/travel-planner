package com.travelplanner.alternative.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplanner.alternative.domain.Alternative;
import com.travelplanner.alternative.domain.PlaceCandidate;
import com.travelplanner.alternative.dto.internal.AlternativeSearchResult;
import com.travelplanner.alternative.dto.internal.SelectResult;
import com.travelplanner.alternative.dto.request.AlternativeSearchRequest;
import com.travelplanner.alternative.dto.request.SelectAlternativeRequest;
import com.travelplanner.alternative.service.AlternativeService;
import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.exception.GlobalExceptionHandler;
import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.common.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AltnController 단위 테스트.
 */
@WebMvcTest(AltnController.class)
@Import({com.travelplanner.alternative.config.SecurityConfig.class, GlobalExceptionHandler.class})
class AltnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AlternativeService alternativeService;

    void setAuthentication(String userId, SubscriptionTier tier) {
        UserPrincipal principal = new UserPrincipal(userId, "test@test.com", tier);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("POST /api/v1/alternatives/search")
    class SearchAlternativesTest {

        @Test
        @DisplayName("Trip Pass 사용자는 대안 검색에 성공한다")
        void searchAlternatives_tripPassUser_success() throws Exception {
            // given
            setAuthentication("user_001", SubscriptionTier.TRIP_PASS);

            AlternativeSearchResult searchResult = new AlternativeSearchResult(
                    createMockAlternatives(), 1000);
            given(alternativeService.searchAlternatives(anyString(), anyString(), anyString(),
                    anyDouble(), anyDouble())).willReturn(searchResult);

            String requestBody = """
                    {
                        "place_id": "place_001",
                        "category": "라멘",
                        "location": {"lat": 35.6595, "lng": 139.7004}
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/alternatives/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.original_place_id").value("place_001"))
                    .andExpect(jsonPath("$.data.cards").isArray());
        }

        @Test
        @DisplayName("Free 티어 사용자는 402 Paywall을 반환한다")
        void searchAlternatives_freeTier_paywall() throws Exception {
            // given
            setAuthentication("user_001", SubscriptionTier.FREE);

            String requestBody = """
                    {
                        "place_id": "place_001",
                        "category": "라멘",
                        "location": {"lat": 35.6595, "lng": 139.7004}
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/alternatives/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isPaymentRequired())
                    .andExpect(jsonPath("$.paywall").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/alternatives/{altId}/select")
    class SelectAlternativeTest {

        @Test
        @DisplayName("대안 선택에 성공한다")
        void selectAlternative_success() throws Exception {
            // given
            setAuthentication("user_001", SubscriptionTier.TRIP_PASS);

            SelectResult selectResult = new SelectResult(
                    "si_001", "place_001", "기존 장소", "mock_place_001", "새 장소", -3);
            given(alternativeService.selectAlternative(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt())).willReturn(selectResult);

            String requestBody = """
                    {
                        "original_place_id": "place_001",
                        "schedule_item_id": "si_001",
                        "trip_id": "trip_001",
                        "selected_rank": 1,
                        "elapsed_seconds": 12
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/alternatives/alt_001/select")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.schedule_item_id").value("si_001"))
                    .andExpect(jsonPath("$.data.travel_time_diff_minutes").value(-3));
        }

        @Test
        @DisplayName("존재하지 않는 대안 선택 시 404를 반환한다")
        void selectAlternative_notFound() throws Exception {
            // given
            setAuthentication("user_001", SubscriptionTier.TRIP_PASS);

            given(alternativeService.selectAlternative(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willThrow(new ResourceNotFoundException("ALTERNATIVE", "alt_999"));

            String requestBody = """
                    {
                        "original_place_id": "place_001",
                        "schedule_item_id": "si_001",
                        "trip_id": "trip_001",
                        "selected_rank": 1,
                        "elapsed_seconds": 12
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/alternatives/alt_999/select")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }

    private List<Alternative> createMockAlternatives() {
        List<Alternative> alternatives = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            PlaceCandidate candidate = new PlaceCandidate();
            candidate.setPlaceId("mock_place_00" + i);
            candidate.setName("Mock 장소 " + i);
            candidate.setDistanceM(300 * i);
            candidate.setRating(4.0f);
            candidate.setLat(35.6595);
            candidate.setLng(139.7004);
            candidate.setCongestion("낮음");
            candidate.setWalkingMinutes(5);
            alternatives.add(Alternative.create("alt_00" + i, "user_001", "place_001",
                    candidate, "근거리 추천", 0.8));
        }
        return alternatives;
    }
}
