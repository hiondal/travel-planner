package com.travelplanner.monitor.domain;

/**
 * 장소 상태 열거형.
 *
 * <p>4단계 상태 판정 기준:</p>
 * <ul>
 *   <li>GREEN: 정상 — 날씨 좋음, 혼잡도 낮음, 영업 중</li>
 *   <li>YELLOW: 주의 — 날씨 약간 불량 또는 혼잡도 중간</li>
 *   <li>RED: 경고 — 날씨 불량 또는 혼잡도 높음 또는 영업 종료</li>
 *   <li>GREY: 정보 없음 — 외부 API 응답 실패</li>
 * </ul>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
public enum PlaceStatusEnum {
    GREEN, YELLOW, RED, GREY;

    public boolean isAlert() {
        return this == YELLOW || this == RED;
    }
}
