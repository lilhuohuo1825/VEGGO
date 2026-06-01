package com.veggo.app.core.preferences;

public class UserPreferences {
    private final String accessToken;

    public UserPreferences(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
