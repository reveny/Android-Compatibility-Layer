package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellSignalStrengthWcdma extends CellSignalStrength implements Parcelable
{
    private static final int[] RSCP_THRESHOLDS = {-115, -105, -95, -85};

    public static final Parcelable.Creator<CellSignalStrengthWcdma> CREATOR = new Parcelable.Creator<CellSignalStrengthWcdma>()
    {
        @Override
        public CellSignalStrengthWcdma createFromParcel(Parcel source)
        {
            return new CellSignalStrengthWcdma(source.readInt(), source.readInt(), source.readInt(), source.readInt());
        }

        @Override
        public CellSignalStrengthWcdma[] newArray(int size)
        {
            return new CellSignalStrengthWcdma[size];
        }
    };

    private final int mRssi;
    private final int mBitErrorRate;
    private final int mRscp;
    private final int mEcNo;

    CellSignalStrengthWcdma()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE);
    }

    CellSignalStrengthWcdma(int rssi, int bitErrorRate, int rscp, int ecNo)
    {
        mRssi = inRangeOrUnavailable(rssi, -113, -51);
        mBitErrorRate = inRangeOrUnavailable(bitErrorRate, 0, 7);
        mRscp = inRangeOrUnavailable(rscp, -120, -24);
        mEcNo = inRangeOrUnavailable(ecNo, -24, 1);
    }

    @Override
    public int getLevel()
    {
        return levelFromThresholds(mRscp, RSCP_THRESHOLDS);
    }

    @Override
    public int getDbm()
    {
        return mRscp != CellInfo.UNAVAILABLE ? mRscp : mRssi;
    }

    @Override
    public int getAsuLevel()
    {
        if (mRscp != CellInfo.UNAVAILABLE) return Math.max(0, Math.min(96, mRscp + 120));
        if (mRssi != CellInfo.UNAVAILABLE) return Math.max(0, Math.min(31, (mRssi + 113) / 2));
        return 255;
    }

    public int getEcNo()
    {
        return mEcNo;
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
        dest.writeInt(mRscp);
        dest.writeInt(mEcNo);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellSignalStrengthWcdma)) return false;
        CellSignalStrengthWcdma strength = (CellSignalStrengthWcdma)other;
        return mRssi == strength.mRssi && mBitErrorRate == strength.mBitErrorRate && mRscp == strength.mRscp && mEcNo == strength.mEcNo;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mRssi, mBitErrorRate, mRscp, mEcNo);
    }

    @Override
    public String toString()
    {
        return "CellSignalStrengthWcdma: ss=" + mRssi + " ber=" + mBitErrorRate + " rscp=" + mRscp + " ecno=" + mEcNo + " level=" + getLevel();
    }
}
