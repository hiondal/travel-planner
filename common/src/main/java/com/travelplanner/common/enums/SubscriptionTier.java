package com.travelplanner.common.enums;

public enum SubscriptionTier {
    FREE,
    TRIP_PASS,
    PRO;

    public boolean isPaid() {
        return this == TRIP_PASS || this == PRO;
    }
}
