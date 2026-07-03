package com.veggo.app.speech;

import java.util.Locale;

/**
 * Configuration for speech recognition sessions.
 * Keeps locale and recognizer options in one place so they can be changed
 * without modifying {@link SpeechManager}.
 *
 * <p>Future cloud engines (Google Cloud Speech, Whisper) can extend or replace
 * this config object while keeping the same public API surface.
 */
public final class SpeechConfig {

    private final Locale locale;
    private final int maxResults;
    private final boolean partialResults;

    private SpeechConfig(Builder builder) {
        this.locale = builder.locale;
        this.maxResults = builder.maxResults;
        this.partialResults = builder.partialResults;
    }

    public static SpeechConfig defaultConfig() {
        return new Builder().build();
    }

    public Locale getLocale() {
        return locale;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public boolean isPartialResultsEnabled() {
        return partialResults;
    }

    public SpeechConfig withLocale(Locale newLocale) {
        return new Builder()
                .locale(newLocale != null ? newLocale : locale)
                .maxResults(maxResults)
                .partialResults(partialResults)
                .build();
    }

    public static final class Builder {
        private Locale locale = new Locale("vi", "VN");
        private int maxResults = 1;
        private boolean partialResults = true;

        public Builder locale(Locale value) {
            this.locale = value;
            return this;
        }

        public Builder maxResults(int value) {
            this.maxResults = Math.max(1, value);
            return this;
        }

        public Builder partialResults(boolean value) {
            this.partialResults = value;
            return this;
        }

        public SpeechConfig build() {
            return new SpeechConfig(this);
        }
    }
}
