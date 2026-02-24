package com.travelplanner.briefing.dto.internal;

import com.travelplanner.common.enums.BriefingType;
import lombok.Getter;

/**
 * 브리핑 생성 서비스 내부 결과.
 */
@Getter
public class GenerateBriefingResult {

    private final String briefingId;
    private final String status;
    private final BriefingType type;

    private GenerateBriefingResult(String briefingId, String status, BriefingType type) {
        this.briefingId = briefingId;
        this.status = status;
        this.type = type;
    }

    public static GenerateBriefingResult created(String briefingId, BriefingType type) {
        return new GenerateBriefingResult(briefingId, "CREATED", type);
    }

    public static GenerateBriefingResult existing(String briefingId, BriefingType type) {
        return new GenerateBriefingResult(briefingId, "EXISTING", type);
    }

    public static GenerateBriefingResult skipped() {
        return new GenerateBriefingResult(null, "SKIPPED", null);
    }

    public boolean isCreated() {
        return "CREATED".equals(status);
    }

    public boolean isSkipped() {
        return "SKIPPED".equals(status);
    }
}
