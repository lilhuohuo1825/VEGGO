package com.veggo.app.core.network;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder();

        if (context != null) {
            String token = new com.veggo.app.core.preferences.PreferencesManager(context).getAccessToken();
            if (token != null && !token.trim().isEmpty()) {
                builder.header("Authorization", "Bearer " + token.trim());
            }
        }

        Request request = builder.build();
        return chain.proceed(request);
    }
}
