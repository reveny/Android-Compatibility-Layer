package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class TelephonyDisplayInfo implements Parcelable
{
    public static final int OVERRIDE_NETWORK_TYPE_NONE = 0;
    public static final int OVERRIDE_NETWORK_TYPE_LTE_CA = 1;
    public static final int OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO = 2;
    public static final int OVERRIDE_NETWORK_TYPE_NR_NSA = 3;
    public static final int OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE = 4;
    public static final int OVERRIDE_NETWORK_TYPE_NR_ADVANCED = 5;

    public static final Parcelable.Creator<TelephonyDisplayInfo> CREATOR = new Parcelable.Creator<TelephonyDisplayInfo>()
    {
        @Override
        public TelephonyDisplayInfo createFromParcel(Parcel source)
        {
            return new TelephonyDisplayInfo(source.readInt(), source.readInt(), source.readBoolean());
        }

        @Override
        public TelephonyDisplayInfo[] newArray(int size)
        {
            return new TelephonyDisplayInfo[size];
        }
    };

    private final int mNetworkType;
    private final int mOverrideNetworkType;
    private final boolean mIsRoaming;

    TelephonyDisplayInfo(int networkType, int overrideNetworkType, boolean isRoaming)
    {
        mNetworkType = networkType;
        mOverrideNetworkType = overrideNetworkType;
        mIsRoaming = isRoaming;
    }

    public int getNetworkType()
    {
        return mNetworkType;
    }

    public int getOverrideNetworkType()
    {
        return mOverrideNetworkType;
    }

    public boolean isRoaming()
    {
        return mIsRoaming;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mNetworkType);
        dest.writeInt(mOverrideNetworkType);
        dest.writeBoolean(mIsRoaming);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof TelephonyDisplayInfo)) return false;
        TelephonyDisplayInfo info = (TelephonyDisplayInfo)other;
        return mNetworkType == info.mNetworkType && mOverrideNetworkType == info.mOverrideNetworkType && mIsRoaming == info.mIsRoaming;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mNetworkType, mOverrideNetworkType, mIsRoaming);
    }

    @Override
    public String toString()
    {
        return "TelephonyDisplayInfo {network=" + mNetworkType + ", overrideNetwork=" + mOverrideNetworkType + ", isRoaming=" + mIsRoaming
            + "}";
    }
}
