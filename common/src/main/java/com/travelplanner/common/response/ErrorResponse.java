package com.travelplanner.common.response;

import java.time.LocalDateTime;

public class ErrorResponse {

    private final String error;
    private final String message;
    private final LocalDateTime timestamp;

    private ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message);
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
