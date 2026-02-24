package com.travelplanner.monitor.domain;

import lombok.Getter;

/**
 * 영업 상태 데이터.
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
public class BusinessStatusData {

    private final String status;
    private final boolean isFallback;

    public BusinessStatusData(String status, boolean isFallback) {
        this.status = status;
        this.isFallback = isFallback;
    }

    public static BusinessStatusData unknown() {
        return new BusinessStatusData("UNKNOWN", false);
    }

    public static BusinessStatusData fallback(String status) {
        return new BusinessStatusData(status, true);
    }
}
