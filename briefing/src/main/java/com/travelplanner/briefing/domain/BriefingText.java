package com.travelplanner.briefing.domain;

import lombok.Getter;

/**
 * 브리핑 생성 텍스트 결과.
 */
@Getter
public class BriefingText {

    private final String text;

    public BriefingText(String text) {
        this.text = text;
    }
}
