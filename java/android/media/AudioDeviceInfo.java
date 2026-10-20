package android.media;

public final class AudioDeviceInfo
{
    public static final int TYPE_BUILTIN_SPEAKER = 2;
    public static final int TYPE_WIRED_HEADSET = 3;
    public static final int TYPE_WIRED_HEADPHONES = 4;

    public static final int GET_DEVICES_OUTPUTS = 2;
    public static final int GET_DEVICES_INPUTS = 1;
    public static final int GET_DEVICES_ALL = 3;

    private final int mType;

    public AudioDeviceInfo(int type)
    {
        mType = type;
    }

    public int getType()
    {
        return mType;
    }

    public boolean isSink()
    {
        return true;
    }

    public boolean isSource()
    {
        return false;
    }

    public CharSequence getProductName()
    {
        return "acl";
    }

    public int[] getSampleRates()
    {
        return new int[] {48000};
    }
}
