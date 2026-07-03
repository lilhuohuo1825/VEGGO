package com.veggo.app.speech;

/**
 * Callback contract for speech recognition events.
 * Activities or Fragments implement this interface to receive UI updates
 * without interacting with {@link android.speech.SpeechRecognizer} directly.
 */
public interface SpeechCallback {

    /** Called when the recognizer is ready to accept speech input. */
    void onReady();

    /** Called when the user has started speaking. */
    void onListening();

    /** Called when audio capture ended and text is being recognized. */
    default void onProcessing() {
    }

    /**
     * Called with interim transcription while the user is still speaking.
     *
     * @param text Partial recognition result.
     */
    default void onPartialResult(String text) {
    }

    /**
     * Called with the final recognized text when recognition succeeds.
     *
     * @param text The best-match transcription result.
     */
    void onResult(String text);

    /**
     * Called when recognition fails or is interrupted.
     *
     * @param message A user-friendly error description.
     */
    void onError(String message);

    /** Called when the recognition session ends (success, cancel, or error). */
    void onVoiceStop();
}
