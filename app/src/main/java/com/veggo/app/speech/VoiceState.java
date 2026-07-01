package com.veggo.app.speech;

/**
 * Represents the lifecycle state of a speech recognition session.
 * {@link SpeechManager} updates this as recognition progresses.
 */
public enum VoiceState {
    /** No active session; recognizer is idle or has been torn down. */
    IDLE,

    /** Microphone is ready; waiting for the user to start speaking. */
    READY,

    /** Actively capturing audio from the user. */
    LISTENING,

    /** Audio capture finished; waiting for recognition results. */
    PROCESSING,

    /** An error occurred during the session. */
    ERROR
}
