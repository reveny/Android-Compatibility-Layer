package android.speech;

import android.os.Bundle;

public interface RecognitionListener
{
    void onBeginningOfSpeech();

    void onBufferReceived(byte[] buffer);

    default void onEndOfSegmentedSession()
    {
    }

    void onEndOfSpeech();

    void onError(int error);

    void onEvent(int eventType, Bundle params);

    default void onLanguageDetection(Bundle results)
    {
    }

    void onPartialResults(Bundle partialResults);

    void onReadyForSpeech(Bundle params);

    void onResults(Bundle results);

    void onRmsChanged(float rmsdB);

    default void onSegmentResults(Bundle segmentResults)
    {
    }
}
