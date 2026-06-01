package com.veggo.app.core.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;

public final class NetworkUtils {
    private NetworkUtils() {
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = manager != null ? manager.getActiveNetwork() : null;
        return network != null;
    }
}
