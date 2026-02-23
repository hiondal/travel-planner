package com.travelplanner.common.exception;

public class ExternalApiException extends BusinessException {

    private final String provider;

    public ExternalApiException(String provider, String message, Throwable cause) {
        super("EXTERNAL_API_ERROR",
                String.format("[%s] 외부 API 호출 실패: %s", provider, message),
                502);
        this.provider = provider;
        initCause(cause);
    }

    public String getProvider() {
        return provider;
    }
}
