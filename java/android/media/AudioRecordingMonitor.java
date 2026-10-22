package android.media;

import java.util.concurrent.Executor;

public interface AudioRecordingMonitor
{
    AudioRecordingConfiguration getActiveRecordingConfiguration();

    void registerAudioRecordingCallback(Executor executor, AudioManager.AudioRecordingCallback callback);

    void unregisterAudioRecordingCallback(AudioManager.AudioRecordingCallback callback);
}
