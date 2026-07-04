package com.veggo.app.data.remote.request;

public class FirebaseLoginRequest {
    private String idToken;
    private String avatarUrl;

    public FirebaseLoginRequest(String idToken) {
        this.idToken = idToken;
    }

    public FirebaseLoginRequest(String idToken, String avatarUrl) {
        this.idToken = idToken;
        this.avatarUrl = avatarUrl;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
