package com.travelplanner.common.exception;

public class PaywallException extends BusinessException {

    private final String upgradeUrl;

    public PaywallException(String upgradeUrl) {
        super("PAYWALL", "구독이 필요한 기능입니다.", 402);
        this.upgradeUrl = upgradeUrl;
    }

    public String getUpgradeUrl() {
        return upgradeUrl;
    }
}
