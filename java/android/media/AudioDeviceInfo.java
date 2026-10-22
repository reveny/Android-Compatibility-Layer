package android.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AudioDeviceInfo
{
    public static final int TYPE_AUX_LINE = 19;
    public static final int TYPE_BLE_BROADCAST = 30;
    public static final int TYPE_BLE_HEADSET = 26;
    public static final int TYPE_BLE_SPEAKER = 27;
    public static final int TYPE_BLUETOOTH_A2DP = 8;
    public static final int TYPE_BLUETOOTH_SCO = 7;
    public static final int TYPE_BUILTIN_EARPIECE = 1;
    public static final int TYPE_BUILTIN_MIC = 15;
    public static final int TYPE_BUILTIN_SPEAKER = 2;
    public static final int TYPE_BUILTIN_SPEAKER_SAFE = 24;
    public static final int TYPE_BUS = 21;
    public static final int TYPE_DOCK = 13;
    public static final int TYPE_DOCK_ANALOG = 31;
    public static final int TYPE_FM = 14;
    public static final int TYPE_FM_TUNER = 16;
    public static final int TYPE_HDMI = 9;
    public static final int TYPE_HDMI_ARC = 10;
    public static final int TYPE_HDMI_EARC = 29;
    public static final int TYPE_HEARING_AID = 23;
    public static final int TYPE_IP = 20;
    public static final int TYPE_LINE_ANALOG = 5;
    public static final int TYPE_LINE_DIGITAL = 6;
    public static final int TYPE_MULTICHANNEL_GROUP = 32;
    public static final int TYPE_REMOTE_SUBMIX = 25;
    public static final int TYPE_TELEPHONY = 18;
    public static final int TYPE_TV_TUNER = 17;
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_USB_ACCESSORY = 12;
    public static final int TYPE_USB_DEVICE = 11;
    public static final int TYPE_USB_HEADSET = 22;
    public static final int TYPE_WIRED_HEADPHONES = 4;
    public static final int TYPE_WIRED_HEADSET = 3;

    static final int GET_DEVICES_OUTPUTS = 2;
    static final int GET_DEVICES_INPUTS = 1;
    static final int GET_DEVICES_ALL = 3;

    private static final int[] OUTPUT_SAMPLE_RATES = {44100, 48000};
    private static final int[] INPUT_SAMPLE_RATES = {8000, 16000, 44100, 48000};
    private static final int[] CHANNEL_COUNTS = {1, 2};
    private static final int[] OUTPUT_CHANNEL_MASKS = {AudioFormat.CHANNEL_OUT_MONO, AudioFormat.CHANNEL_OUT_STEREO};
    private static final int[] INPUT_CHANNEL_MASKS = {AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO};
    private static final int[] CHANNEL_INDEX_MASKS = {1, 3};
    private static final int[] ENCODINGS = {AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_FLOAT};

    private final int mId;
    private final int mType;
    private final boolean mSink;
    private final String mAddress;
    private final CharSequence mProductName;

    AudioDeviceInfo(int type)
    {
        this(type, type, !isInputType(type), "", "acl");
    }

    AudioDeviceInfo(int id, int type, boolean sink, String address, CharSequence productName)
    {
        mId = id;
        mType = type;
        mSink = sink;
        mAddress = address == null ? "" : address;
        mProductName = productName == null ? "" : productName;
    }

    private static boolean isInputType(int type)
    {
        switch (type)
        {
            case TYPE_BUILTIN_MIC:
            case TYPE_FM_TUNER:
            case TYPE_TV_TUNER:
            case TYPE_TELEPHONY:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof AudioDeviceInfo)) return false;

        return mId == ((AudioDeviceInfo)object).mId;
    }

    public String getAddress()
    {
        return mAddress;
    }

    public List<AudioDescriptor> getAudioDescriptors()
    {
        return Collections.emptyList();
    }

    public List<AudioProfile> getAudioProfiles()
    {
        List<AudioProfile> profiles = new ArrayList<>();

        for (int encoding : ENCODINGS)
        {
            profiles.add(new AudioProfile(encoding, getSampleRates(), getChannelMasks(), getChannelIndexMasks(),
                AudioProfile.AUDIO_ENCAPSULATION_TYPE_NONE));
        }

        return profiles;
    }

    public int[] getChannelCounts()
    {
        return CHANNEL_COUNTS.clone();
    }

    public int[] getChannelIndexMasks()
    {
        return CHANNEL_INDEX_MASKS.clone();
    }

    public int[] getChannelMasks()
    {
        return mSink ? OUTPUT_CHANNEL_MASKS.clone() : INPUT_CHANNEL_MASKS.clone();
    }

    public int[] getEncapsulationMetadataTypes()
    {
        return new int[0];
    }

    public int[] getEncapsulationModes()
    {
        return new int[0];
    }

    public int[] getEncodings()
    {
        return ENCODINGS.clone();
    }

    public int getId()
    {
        return mId;
    }

    public CharSequence getProductName()
    {
        return mProductName;
    }

    public int[] getSampleRates()
    {
        return mSink ? OUTPUT_SAMPLE_RATES.clone() : INPUT_SAMPLE_RATES.clone();
    }

    public int getSpeakerLayoutChannelMask()
    {
        return mSink && mType == TYPE_BUILTIN_SPEAKER ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_INVALID;
    }

    public int getType()
    {
        return mType;
    }

    @Override
    public int hashCode()
    {
        return mId;
    }

    public boolean isSink()
    {
        return mSink;
    }

    public boolean isSource()
    {
        return !mSink;
    }
}
