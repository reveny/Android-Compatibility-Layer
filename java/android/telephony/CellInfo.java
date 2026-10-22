package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public abstract class CellInfo implements Parcelable
{
    public static final int UNAVAILABLE = Integer.MAX_VALUE;
    public static final long UNAVAILABLE_LONG = Long.MAX_VALUE;

    public static final int CONNECTION_NONE = 0;
    public static final int CONNECTION_PRIMARY_SERVING = 1;
    public static final int CONNECTION_SECONDARY_SERVING = 2;
    public static final int CONNECTION_UNKNOWN = Integer.MAX_VALUE;

    public static final Parcelable.Creator<CellInfo> CREATOR = new Parcelable.Creator<CellInfo>()
    {
        @Override
        public CellInfo createFromParcel(Parcel source)
        {
            int type = source.readInt();
            switch (type)
            {
                case CellIdentity.TYPE_GSM: return new CellInfoGsm(source);
                case CellIdentity.TYPE_CDMA: return new CellInfoCdma(source);
                case CellIdentity.TYPE_LTE: return new CellInfoLte(source);
                case CellIdentity.TYPE_WCDMA: return new CellInfoWcdma(source);
                case CellIdentity.TYPE_NR: return new CellInfoNr(source);
                default: throw new IllegalArgumentException("Bad CellInfo type " + type);
            }
        }

        @Override
        public CellInfo[] newArray(int size)
        {
            return new CellInfo[size];
        }
    };

    private final boolean mRegistered;
    private final long mTimestampNanos;
    private final int mCellConnectionStatus;

    CellInfo()
    {
        mRegistered = false;
        mTimestampNanos = UNAVAILABLE_LONG;
        mCellConnectionStatus = CONNECTION_NONE;
    }

    CellInfo(Parcel source)
    {
        mRegistered = source.readBoolean();
        mTimestampNanos = source.readLong();
        mCellConnectionStatus = source.readInt();
    }

    public boolean isRegistered()
    {
        return mRegistered;
    }

    public long getTimestampMillis()
    {
        return mTimestampNanos == UNAVAILABLE_LONG ? UNAVAILABLE_LONG : mTimestampNanos / 1000000L;
    }

    @Deprecated
    public long getTimeStamp()
    {
        return mTimestampNanos;
    }

    public int getCellConnectionStatus()
    {
        return mCellConnectionStatus;
    }

    public abstract CellIdentity getCellIdentity();

    public abstract CellSignalStrength getCellSignalStrength();

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public abstract void writeToParcel(Parcel dest, int flags);

    void writeBaseToParcel(Parcel dest, int type)
    {
        dest.writeInt(type);
        dest.writeBoolean(mRegistered);
        dest.writeLong(mTimestampNanos);
        dest.writeInt(mCellConnectionStatus);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellInfo)) return false;
        CellInfo info = (CellInfo)other;
        return mRegistered == info.mRegistered && mTimestampNanos == info.mTimestampNanos && mCellConnectionStatus == info.mCellConnectionStatus;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mRegistered, mTimestampNanos, mCellConnectionStatus);
    }

    @Override
    public String toString()
    {
        return "mRegistered=" + (mRegistered ? "YES" : "NO") + " mTimeStamp=" + mTimestampNanos + "ns mCellConnectionStatus="
            + mCellConnectionStatus;
    }
}
