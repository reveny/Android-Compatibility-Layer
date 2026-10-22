package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ScanResult implements Parcelable
{
    public static final int UNSPECIFIED = -1;
    public static final int WIFI_STANDARD_UNKNOWN = 0;
    public static final int WIFI_STANDARD_LEGACY = 1;
    public static final int WIFI_STANDARD_11N = 4;
    public static final int WIFI_STANDARD_11AC = 5;
    public static final int WIFI_STANDARD_11AX = 6;
    public static final int WIFI_STANDARD_11AD = 7;
    public static final int WIFI_STANDARD_11BE = 8;
    public static final int WIFI_BAND_24_GHZ = 1;
    public static final int WIFI_BAND_5_GHZ = 2;
    public static final int WIFI_BAND_6_GHZ = 8;
    public static final int WIFI_BAND_60_GHZ = 16;
    public static final int CHANNEL_WIDTH_20MHZ = 0;
    public static final int CHANNEL_WIDTH_40MHZ = 1;
    public static final int CHANNEL_WIDTH_80MHZ = 2;
    public static final int CHANNEL_WIDTH_160MHZ = 3;
    public static final int CHANNEL_WIDTH_80MHZ_PLUS_MHZ = 4;
    public static final int CHANNEL_WIDTH_320MHZ = 5;
    public static final int PREAMBLE_LEGACY = 0;
    public static final int PREAMBLE_HT = 1;
    public static final int PREAMBLE_VHT = 2;
    public static final int PREAMBLE_HE = 3;
    public static final int PREAMBLE_EHT = 4;

    public String SSID;
    public String BSSID;
    public String capabilities;
    public int level;
    public int frequency;
    public long timestamp;
    public int channelWidth;
    public int centerFreq0;
    public int centerFreq1;
    public CharSequence operatorFriendlyName;
    public CharSequence venueName;

    private int mWifiStandard = WIFI_STANDARD_UNKNOWN;
    private boolean mIs80211mcResponder;
    private boolean mIsPasspointNetwork;
    private final List<InformationElement> mInformationElements = new ArrayList<>();

    public ScanResult()
    {
    }

    public ScanResult(ScanResult source)
    {
        if (source == null) return;

        SSID = source.SSID;
        BSSID = source.BSSID;
        capabilities = source.capabilities;
        level = source.level;
        frequency = source.frequency;
        timestamp = source.timestamp;
        channelWidth = source.channelWidth;
        centerFreq0 = source.centerFreq0;
        centerFreq1 = source.centerFreq1;
        operatorFriendlyName = source.operatorFriendlyName;
        venueName = source.venueName;
        mWifiStandard = source.mWifiStandard;
        mIs80211mcResponder = source.mIs80211mcResponder;
        mIsPasspointNetwork = source.mIsPasspointNetwork;
        mInformationElements.addAll(source.mInformationElements);
    }

    public int getWifiStandard()
    {
        return mWifiStandard;
    }

    public int getApMloLinkId()
    {
        return -1;
    }

    public boolean is80211mcResponder()
    {
        return mIs80211mcResponder;
    }

    public boolean isPasspointNetwork()
    {
        return mIsPasspointNetwork;
    }

    public static int convertChannelToFrequencyMhzIfSupported(int channel, int band)
    {
        if (band == WIFI_BAND_24_GHZ)
        {
            if (channel == 14) return 2484;
            if (channel >= 1 && channel <= 13) return 2407 + channel * 5;

            return UNSPECIFIED;
        }

        if (band == WIFI_BAND_5_GHZ)
        {
            if (channel >= 32 && channel <= 177) return 5000 + channel * 5;

            return UNSPECIFIED;
        }

        if (band == WIFI_BAND_6_GHZ)
        {
            if (channel == 2) return 5935;
            if (channel >= 1 && channel <= 233) return 5950 + channel * 5;

            return UNSPECIFIED;
        }

        if (band == WIFI_BAND_60_GHZ)
        {
            if (channel >= 1 && channel <= 6) return 58320 + (channel - 1) * 2160;

            return UNSPECIFIED;
        }

        return UNSPECIFIED;
    }

    public static int convertFrequencyMhzToChannelIfSupported(int frequencyMhz)
    {
        if (frequencyMhz == 2484) return 14;
        if (frequencyMhz >= 2412 && frequencyMhz <= 2472) return (frequencyMhz - 2407) / 5;
        if (frequencyMhz >= 5160 && frequencyMhz <= 5885) return (frequencyMhz - 5000) / 5;
        if (frequencyMhz == 5935) return 2;
        if (frequencyMhz >= 5955 && frequencyMhz <= 7115) return (frequencyMhz - 5950) / 5;
        if (frequencyMhz >= 58320 && frequencyMhz <= 70200) return (frequencyMhz - 58320) / 2160 + 1;

        return UNSPECIFIED;
    }

    public List<InformationElement> getInformationElements()
    {
        return new ArrayList<>(mInformationElements);
    }

    public int[] getSecurityTypes()
    {
        return new int[0];
    }

    @Override
    public String toString()
    {
        return "SSID: " + (SSID == null ? WifiManager.UNKNOWN_SSID : SSID) + ", BSSID: " + (BSSID == null ? "<none>" : BSSID)
            + ", capabilities: " + (capabilities == null ? "<none>" : capabilities) + ", level: " + level + ", frequency: " + frequency
            + ", timestamp: " + timestamp;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(SSID);
        dest.writeString(BSSID);
        dest.writeString(capabilities);
        dest.writeInt(level);
        dest.writeInt(frequency);
        dest.writeLong(timestamp);
        dest.writeInt(channelWidth);
        dest.writeInt(centerFreq0);
        dest.writeInt(centerFreq1);
        dest.writeString(operatorFriendlyName == null ? null : operatorFriendlyName.toString());
        dest.writeString(venueName == null ? null : venueName.toString());
        dest.writeInt(mWifiStandard);
        dest.writeInt(mIs80211mcResponder ? 1 : 0);
        dest.writeInt(mIsPasspointNetwork ? 1 : 0);
        dest.writeInt(mInformationElements.size());

        for (InformationElement element : mInformationElements)
        {
            element.writeToParcel(dest, flags);
        }
    }

    public static final Creator<ScanResult> CREATOR = new Creator<ScanResult>()
    {
        @Override
        public ScanResult createFromParcel(Parcel source)
        {
            ScanResult result = new ScanResult();
            result.SSID = source.readString();
            result.BSSID = source.readString();
            result.capabilities = source.readString();
            result.level = source.readInt();
            result.frequency = source.readInt();
            result.timestamp = source.readLong();
            result.channelWidth = source.readInt();
            result.centerFreq0 = source.readInt();
            result.centerFreq1 = source.readInt();
            result.operatorFriendlyName = source.readString();
            result.venueName = source.readString();
            result.mWifiStandard = source.readInt();
            result.mIs80211mcResponder = source.readInt() != 0;
            result.mIsPasspointNetwork = source.readInt() != 0;

            int count = source.readInt();
            for (int index = 0; index < count; index++)
            {
                result.mInformationElements.add(InformationElement.CREATOR.createFromParcel(source));
            }

            return result;
        }

        @Override
        public ScanResult[] newArray(int size)
        {
            return new ScanResult[size];
        }
    };

    public static class InformationElement implements Parcelable
    {
        private final int mId;
        private final int mIdExt;
        private final byte[] mBytes;

        public InformationElement(int id, int idExt, byte[] bytes)
        {
            mId = id;
            mIdExt = idExt;
            mBytes = bytes == null ? new byte[0] : bytes.clone();
        }

        public InformationElement(InformationElement source)
        {
            this(source.mId, source.mIdExt, source.mBytes);
        }

        public int getId()
        {
            return mId;
        }

        public int getIdExt()
        {
            return mIdExt;
        }

        public ByteBuffer getBytes()
        {
            return ByteBuffer.wrap(mBytes).asReadOnlyBuffer();
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(mId);
            dest.writeInt(mIdExt);
            dest.writeInt(mBytes.length);

            for (byte value : mBytes)
            {
                dest.writeInt(value);
            }
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof InformationElement)) return false;

            InformationElement element = (InformationElement)other;
            return mId == element.mId && mIdExt == element.mIdExt && Arrays.equals(mBytes, element.mBytes);
        }

        @Override
        public int hashCode()
        {
            return 31 * (31 * mId + mIdExt) + Arrays.hashCode(mBytes);
        }

        public static final Creator<InformationElement> CREATOR = new Creator<InformationElement>()
        {
            @Override
            public InformationElement createFromParcel(Parcel source)
            {
                int id = source.readInt();
                int idExt = source.readInt();
                byte[] bytes = new byte[source.readInt()];

                for (int index = 0; index < bytes.length; index++)
                {
                    bytes[index] = (byte)source.readInt();
                }

                return new InformationElement(id, idExt, bytes);
            }

            @Override
            public InformationElement[] newArray(int size)
            {
                return new InformationElement[size];
            }
        };
    }
}
