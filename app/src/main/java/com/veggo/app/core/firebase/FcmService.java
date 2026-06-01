package com.veggo.app.core.firebase;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;

public class FcmService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // TODO: Send this token to the VEGGO backend after auth/profile is implemented.
    }
}
