package com.travelplanner.briefing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelplanner.briefing.dto.internal.GenerateBriefingResult;
import com.travelplanner.common.enums.BriefingType;
import lombok.Getter;

/**
 * 브리핑 생성 응답 DTO.
 */
@Getter
public class GenerateBriefingResponse {

    @JsonProperty("briefing_id")
    private final String briefingId;

    @JsonProperty("status")
    private final String status;

    @JsonProperty("type")
    private final String type;

    private GenerateBriefingResponse(String briefingId, String status, BriefingType type) {
        this.briefingId = briefingId;
        this.status = status;
        this.type = type != null ? type.name() : null;
    }

    public static GenerateBriefingResponse created(String briefingId, BriefingType type) {
        return new GenerateBriefingResponse(briefingId, "CREATED", type);
    }

    public static GenerateBriefingResponse existing(String briefingId, BriefingType type) {
        return new GenerateBriefingResponse(briefingId, "EXISTING", type);
    }

    public static GenerateBriefingResponse from(GenerateBriefingResult result) {
        return new GenerateBriefingResponse(result.getBriefingId(), result.getStatus(), result.getType());
    }
}
