package com.travelplanner.common.response;

import java.time.LocalDateTime;

public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, data);
    }

    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(true, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
