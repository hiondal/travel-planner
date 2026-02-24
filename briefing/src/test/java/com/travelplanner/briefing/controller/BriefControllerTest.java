package com.travelplanner.briefing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelplanner.briefing.domain.Briefing;
import com.travelplanner.briefing.domain.BriefingContent;
import com.travelplanner.briefing.domain.StatusLevel;
import com.travelplanner.briefing.service.BriefingService;
import com.travelplanner.common.enums.BriefingType;
import com.travelplanner.common.enums.SubscriptionTier;
import com.travelplanner.common.exception.GlobalExceptionHandler;
import com.travelplanner.common.exception.ResourceNotFoundException;
import com.travelplanner.common.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BriefController 단위 테스트.
 */
@WebMvcTest(BriefController.class)
@Import({com.travelplanner.briefing.config.SecurityConfig.class, GlobalExceptionHandler.class})
class BriefControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BriefingService briefingService;

    void setAuthentication(String userId) {
        UserPrincipal principal = new UserPrincipal(userId, "test@test.com", SubscriptionTier.FREE);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("GET /api/v1/briefings/{briefingId}")
    class GetBriefingTest {

        @Test
        @DisplayName("브리핑 상세 조회에 성공한다")
        void getBriefing_success() throws Exception {
            // given
            setAuthentication("user_001");
            Briefing briefing = createBriefing("brif_001", "user_001", BriefingType.SAFE);
            given(briefingService.getBriefing("brif_001", "user_001")).willReturn(briefing);

            // when & then
            mockMvc.perform(get("/api/v1/briefings/brif_001")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.briefing_id").value("brif_001"))
                    .andExpect(jsonPath("$.data.type").value("SAFE"));
        }

        @Test
        @DisplayName("존재하지 않는 브리핑 조회 시 404를 반환한다")
        void getBriefing_notFound() throws Exception {
            // given
            setAuthentication("user_001");
            given(briefingService.getBriefing("brif_999", "user_001"))
                    .willThrow(new ResourceNotFoundException("BRIEFING", "brif_999"));

            // when & then
            mockMvc.perform(get("/api/v1/briefings/brif_999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/briefings")
    class GetBriefingListTest {

        @Test
        @DisplayName("브리핑 목록 조회에 성공한다")
        void getBriefingList_success() throws Exception {
            // given
            setAuthentication("user_001");
            List<Briefing> briefings = List.of(
                    createBriefing("brif_001", "user_001", BriefingType.SAFE),
                    createBriefing("brif_002", "user_001", BriefingType.WARNING)
            );
            given(briefingService.getBriefingList(eq("user_001"), any())).willReturn(briefings);

            // when & then
            mockMvc.perform(get("/api/v1/briefings")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.briefings").isArray())
                    .andExpect(jsonPath("$.data.briefings.length()").value(2));
        }

        @Test
        @DisplayName("날짜 파라미터를 지정하여 조회한다")
        void getBriefingList_withDate() throws Exception {
            // given
            setAuthentication("user_001");
            given(briefingService.getBriefingList(any(), any())).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/briefings")
                            .param("date", "2026-03-16")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.date").value("2026-03-16"));
        }
    }

    private Briefing createBriefing(String id, String userId, BriefingType type) {
        BriefingContent content = new BriefingContent("영업 중", "보통", "맑음", 15, null, 420);
        StatusLevel statusLevel = type == BriefingType.SAFE ? StatusLevel.SAFE : StatusLevel.CAUTION;
        return Briefing.create(id, userId, "si_001", "place_001", "테스트 장소",
                type, LocalDateTime.now().plusHours(1), UUID.randomUUID().toString(),
                "테스트 브리핑", statusLevel, content, List.of());
    }
}
