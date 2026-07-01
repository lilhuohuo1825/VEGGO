package com.veggo.app.speech;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Detects speech recognition services installed on the device (Google, Samsung, etc.).
 */
final class SpeechAvailability {

    static final ComponentName GOOGLE_RECOGNITION_SERVICE = new ComponentName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
    );

    private SpeechAvailability() {
    }

    static boolean isApiRecognitionAvailable(Context context) {
        return SpeechRecognizer.isRecognitionAvailable(context)
                || findRecognitionService(context) != null;
    }

    static boolean isIntentRecognitionAvailable(Context context) {
        return findRecognitionActivity(context) != null;
    }

    static boolean isAnyRecognitionAvailable(Context context) {
        return isApiRecognitionAvailable(context) || isIntentRecognitionAvailable(context);
    }

    @Nullable
    static ComponentName findRecognitionService(Context context) {
        Intent intent = new Intent(RecognitionService.SERVICE_INTERFACE);
        List<ResolveInfo> services = context.getPackageManager().queryIntentServices(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
        );
        if (services == null || services.isEmpty()) {
            return null;
        }

        ComponentName samsung = null;
        ComponentName google = null;
        for (ResolveInfo info : services) {
            if (info.serviceInfo == null) {
                continue;
            }
            ComponentName component = new ComponentName(
                    info.serviceInfo.packageName,
                    info.serviceInfo.name
            );
            String packageName = component.getPackageName().toLowerCase();
            if (packageName.contains("samsung")) {
                samsung = component;
            } else if (packageName.contains("google")) {
                google = component;
            }
        }

        if (samsung != null) {
            return samsung;
        }
        if (google != null) {
            return google;
        }

        ResolveInfo first = services.get(0);
        if (first.serviceInfo == null) {
            return null;
        }
        return new ComponentName(first.serviceInfo.packageName, first.serviceInfo.name);
    }

    @Nullable
    static ComponentName findRecognitionActivity(Context context) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
        );
        if (activities == null || activities.isEmpty()) {
            return null;
        }

        ComponentName samsung = null;
        ComponentName google = null;
        for (ResolveInfo info : activities) {
            if (info.activityInfo == null) {
                continue;
            }
            ComponentName component = new ComponentName(
                    info.activityInfo.packageName,
                    info.activityInfo.name
            );
            String packageName = component.getPackageName().toLowerCase();
            if (packageName.contains("samsung")) {
                samsung = component;
            } else if (packageName.contains("google")) {
                google = component;
            }
        }

        if (samsung != null) {
            return samsung;
        }
        if (google != null) {
            return google;
        }

        ResolveInfo first = activities.get(0);
        if (first.activityInfo == null) {
            return null;
        }
        return new ComponentName(first.activityInfo.packageName, first.activityInfo.name);
    }
}
