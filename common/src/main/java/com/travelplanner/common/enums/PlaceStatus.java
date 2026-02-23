package com.travelplanner.common.enums;

public enum PlaceStatus {
    GREEN,
    YELLOW,
    RED,
    GREY;

    public boolean isAlert() {
        return this == RED || this == YELLOW;
    }
}
