package com.travelplanner.common.exception;

public class ValidationException extends BusinessException {

    private final String field;
    private final Object rejectedValue;

    public ValidationException(String field, String message, Object rejectedValue) {
        super("VALIDATION_ERROR", message, 400);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public String getField() {
        return field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }
}
