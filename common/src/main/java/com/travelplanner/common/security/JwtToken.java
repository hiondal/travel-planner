package com.travelplanner.common.security;

public class JwtToken {

    private final String accessToken;
    private final String refreshToken;
    private final int expiresIn;

    public JwtToken(String accessToken, String refreshToken, int expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }
}
