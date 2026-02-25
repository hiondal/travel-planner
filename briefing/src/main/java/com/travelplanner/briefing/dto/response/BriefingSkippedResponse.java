package com.travelplanner.briefing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 브리핑 생성 스킵 응답 DTO (Free 티어 한도 초과).
 */
@Getter
@Builder
@AllArgsConstructor
public class BriefingSkippedResponse {

    @JsonProperty("status")
    private final String status;

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("message")
    private final String message;

    public static BriefingSkippedResponse freeTierLimitExceeded() {
        return BriefingSkippedResponse.builder()
                .status("SKIPPED")
                .reason("FREE_TIER_LIMIT_EXCEEDED")
                .message("오늘의 브리핑을 모두 사용했습니다.")
                .build();
    }
}
