package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellSignalStrengthLte extends CellSignalStrength implements Parcelable
{
    private static final int[] RSRP_THRESHOLDS = {-115, -105, -95, -85};

    public static final Parcelable.Creator<CellSignalStrengthLte> CREATOR = new Parcelable.Creator<CellSignalStrengthLte>()
    {
        @Override
        public CellSignalStrengthLte createFromParcel(Parcel source)
        {
            return new CellSignalStrengthLte(source.readInt(), source.readInt(), source.readInt(), source.readInt(), source.readInt(),
                source.readInt(), source.readInt());
        }

        @Override
        public CellSignalStrengthLte[] newArray(int size)
        {
            return new CellSignalStrengthLte[size];
        }
    };

    private final int mRssi;
    private final int mRsrp;
    private final int mRsrq;
    private final int mRssnr;
    private final int mCqiTableIndex;
    private final int mCqi;
    private final int mTimingAdvance;

    CellSignalStrengthLte()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE,
            CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE);
    }

    CellSignalStrengthLte(int rssi, int rsrp, int rsrq, int rssnr, int cqiTableIndex, int cqi, int timingAdvance)
    {
        mRssi = inRangeOrUnavailable(rssi, -113, -51);
        mRsrp = inRangeOrUnavailable(rsrp, -140, -43);
        mRsrq = inRangeOrUnavailable(rsrq, -34, 3);
        mRssnr = inRangeOrUnavailable(rssnr, -20, 30);
        mCqiTableIndex = inRangeOrUnavailable(cqiTableIndex, 1, 6);
        mCqi = inRangeOrUnavailable(cqi, 0, 15);
        mTimingAdvance = inRangeOrUnavailable(timingAdvance, 0, 1282);
    }

    @Override
    public int getLevel()
    {
        return levelFromThresholds(mRsrp, RSRP_THRESHOLDS);
    }

    public int getRsrq()
    {
        return mRsrq;
    }

    public int getRssi()
    {
        return mRssi;
    }

    public int getRssnr()
    {
        return mRssnr;
    }

    public int getRsrp()
    {
        return mRsrp;
    }

    public int getCqiTableIndex()
    {
        return mCqiTableIndex;
    }

    public int getCqi()
    {
        return mCqi;
    }

    @Override
    public int getDbm()
    {
        return mRsrp;
    }

    @Override
    public int getAsuLevel()
    {
        return mRsrp == CellInfo.UNAVAILABLE ? 99 : Math.max(0, Math.min(97, mRsrp + 140));
    }

    public int getTimingAdvance()
    {
        return mTimingAdvance;
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
        dest.writeInt(mRsrp);
        dest.writeInt(mRsrq);
        dest.writeInt(mRssnr);
        dest.writeInt(mCqiTableIndex);
        dest.writeInt(mCqi);
        dest.writeInt(mTimingAdvance);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellSignalStrengthLte)) return false;
        CellSignalStrengthLte strength = (CellSignalStrengthLte)other;
        return mRssi == strength.mRssi && mRsrp == strength.mRsrp && mRsrq == strength.mRsrq && mRssnr == strength.mRssnr
            && mCqiTableIndex == strength.mCqiTableIndex && mCqi == strength.mCqi && mTimingAdvance == strength.mTimingAdvance;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mRssi, mRsrp, mRsrq, mRssnr, mCqiTableIndex, mCqi, mTimingAdvance);
    }

    @Override
    public String toString()
    {
        return "CellSignalStrengthLte: rssi=" + mRssi + " rsrp=" + mRsrp + " rsrq=" + mRsrq + " rssnr=" + mRssnr + " cqiTableIndex="
            + mCqiTableIndex + " cqi=" + mCqi + " ta=" + mTimingAdvance + " level=" + getLevel();
    }
}
