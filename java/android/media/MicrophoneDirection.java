package android.media;

public interface MicrophoneDirection
{
    int MIC_DIRECTION_AWAY_FROM_USER = 2;
    int MIC_DIRECTION_EXTERNAL = 3;
    int MIC_DIRECTION_TOWARDS_USER = 1;
    int MIC_DIRECTION_UNSPECIFIED = 0;

    boolean setPreferredMicrophoneDirection(int direction);

    boolean setPreferredMicrophoneFieldDimension(float zoom);
}
