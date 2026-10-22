package android.media;

public class MediaActionSound
{
    public static final int FOCUS_COMPLETE = 1;
    public static final int SHUTTER_CLICK = 0;
    public static final int START_VIDEO_RECORDING = 2;
    public static final int STOP_VIDEO_RECORDING = 3;

    private static final int SOUND_COUNT = 4;

    private final boolean[] mLoaded = new boolean[SOUND_COUNT];
    private boolean mReleased;

    public MediaActionSound()
    {
    }

    public void load(int soundName)
    {
        checkSoundName(soundName);
        synchronized (mLoaded)
        {
            mLoaded[soundName] = true;
        }
    }

    public static boolean mustPlayShutterSound()
    {
        return false;
    }

    public void play(int soundName)
    {
        checkSoundName(soundName);
        if (mReleased) throw new IllegalStateException("MediaActionSound has been released");
        load(soundName);
    }

    public void release()
    {
        synchronized (mLoaded)
        {
            mReleased = true;
            java.util.Arrays.fill(mLoaded, false);
        }
    }

    private static void checkSoundName(int soundName)
    {
        if (soundName < 0 || soundName >= SOUND_COUNT) throw new RuntimeException("Unknown sound requested: " + soundName);
    }
}
