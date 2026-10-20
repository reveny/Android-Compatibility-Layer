package android.media;

public class AudioManager
{
    public static final int GET_DEVICES_OUTPUTS = 2;
    public static final int GET_DEVICES_INPUTS = 1;
    public static final int GET_DEVICES_ALL = GET_DEVICES_OUTPUTS | GET_DEVICES_INPUTS;

    public static final int STREAM_MUSIC = 3;
    public static final int STREAM_SYSTEM = 1;

    public static final int MODE_NORMAL = 0;

    public static final String PROPERTY_OUTPUT_SAMPLE_RATE = "android.media.property.OUTPUT_SAMPLE_RATE";
    public static final String PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "android.media.property.OUTPUT_FRAMES_PER_BUFFER";

    public static final int AUDIOFOCUS_GAIN = 1;
    public static final int AUDIOFOCUS_REQUEST_GRANTED = 1;
    public static final int AUDIOFOCUS_LOSS = -1;

    public interface OnAudioFocusChangeListener
    {
        void onAudioFocusChange(int focusChange);
    }

    public String getProperty(String key)
    {
        if (PROPERTY_OUTPUT_SAMPLE_RATE.equals(key)) return "48000";
        if (PROPERTY_OUTPUT_FRAMES_PER_BUFFER.equals(key)) return "256";

        return null;
    }

    public int getStreamVolume(int streamType)
    {
        return 10;
    }

    public int getStreamMaxVolume(int streamType)
    {
        return 15;
    }

    public void setStreamVolume(int streamType, int index, int flags)
    {
    }

    public int getMode()
    {
        return MODE_NORMAL;
    }

    public void setMode(int mode)
    {
    }

    public boolean isMusicActive()
    {
        return false;
    }

    public boolean isWiredHeadsetOn()
    {
        return false;
    }

    public boolean isBluetoothA2dpOn()
    {
        return false;
    }

    public boolean isSpeakerphoneOn()
    {
        return true;
    }

    public int requestAudioFocus(OnAudioFocusChangeListener listener, int streamType, int durationHint)
    {
        return AUDIOFOCUS_REQUEST_GRANTED;
    }

    public int abandonAudioFocus(OnAudioFocusChangeListener listener)
    {
        return AUDIOFOCUS_REQUEST_GRANTED;
    }

    public AudioDeviceInfo[] getDevices(int flags)
    {
        return new AudioDeviceInfo[0];
    }
}
