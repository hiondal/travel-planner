package com.travelplanner.common.exception;

public class ConsentRequiredException extends BusinessException {

    public ConsentRequiredException() {
        super("CONSENT_REQUIRED", "사용자 동의가 필요합니다.", 403);
    }
}
