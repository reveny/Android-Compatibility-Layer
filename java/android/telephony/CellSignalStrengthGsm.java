package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellSignalStrengthGsm extends CellSignalStrength implements Parcelable
{
    private static final int[] RSSI_THRESHOLDS = {-107, -103, -97, -89};

    public static final Parcelable.Creator<CellSignalStrengthGsm> CREATOR = new Parcelable.Creator<CellSignalStrengthGsm>()
    {
        @Override
        public CellSignalStrengthGsm createFromParcel(Parcel source)
        {
            return new CellSignalStrengthGsm(source.readInt(), source.readInt(), source.readInt());
        }

        @Override
        public CellSignalStrengthGsm[] newArray(int size)
        {
            return new CellSignalStrengthGsm[size];
        }
    };

    private final int mRssi;
    private final int mBitErrorRate;
    private final int mTimingAdvance;

    CellSignalStrengthGsm()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE);
    }

    CellSignalStrengthGsm(int rssi, int bitErrorRate, int timingAdvance)
    {
        mRssi = inRangeOrUnavailable(rssi, -113, -51);
        mBitErrorRate = inRangeOrUnavailable(bitErrorRate, 0, 7);
        mTimingAdvance = inRangeOrUnavailable(timingAdvance, 0, 219);
    }

    @Override
    public int getLevel()
    {
        return levelFromThresholds(mRssi, RSSI_THRESHOLDS);
    }

    public int getTimingAdvance()
    {
        return mTimingAdvance;
    }

    @Override
    public int getDbm()
    {
        return mRssi;
    }

    @Override
    public int getAsuLevel()
    {
        return mRssi == CellInfo.UNAVAILABLE ? 99 : Math.max(0, Math.min(31, (mRssi + 113) / 2));
    }

    public int getRssi()
    {
        return mRssi;
    }

    public int getBitErrorRate()
    {
        return mBitErrorRate;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mRssi);
        dest.writeInt(mBitErrorRate);
        dest.writeInt(mTimingAdvance);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellSignalStrengthGsm)) return false;
        CellSignalStrengthGsm strength = (CellSignalStrengthGsm)other;
        return mRssi == strength.mRssi && mBitErrorRate == strength.mBitErrorRate && mTimingAdvance == strength.mTimingAdvance;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mRssi, mBitErrorRate, mTimingAdvance);
    }

    @Override
    public String toString()
    {
        return "CellSignalStrengthGsm: rssi=" + mRssi + " ber=" + mBitErrorRate + " mTa=" + mTimingAdvance + " mLevel=" + getLevel();
    }
}
