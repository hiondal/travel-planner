package com.travelplanner.common.security;

import com.travelplanner.common.enums.SubscriptionTier;

public class UserPrincipal {

    private final String userId;
    private final String email;
    private final SubscriptionTier tier;

    public UserPrincipal(String userId, String email, SubscriptionTier tier) {
        this.userId = userId;
        this.email = email;
        this.tier = tier;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public SubscriptionTier getTier() {
        return tier;
    }

    public boolean isFreeTier() {
        return tier == SubscriptionTier.FREE;
    }

    public boolean isPaidTier() {
        return tier.isPaid();
    }
}
