package com.travelplanner.common.exception;

public class ResourceNotFoundException extends BusinessException {

    private final String resourceName;
    private final String resourceId;

    public ResourceNotFoundException(String resourceName, String resourceId) {
        super("RESOURCE_NOT_FOUND",
                String.format("%s(id=%s)를 찾을 수 없습니다.", resourceName, resourceId),
                404);
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceId() {
        return resourceId;
    }
}
