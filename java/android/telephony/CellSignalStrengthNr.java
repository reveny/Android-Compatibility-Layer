package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CellSignalStrengthNr extends CellSignalStrength implements Parcelable
{
    private static final int[] SS_RSRP_THRESHOLDS = {-110, -90, -80, -65};

    public static final Parcelable.Creator<CellSignalStrengthNr> CREATOR = new Parcelable.Creator<CellSignalStrengthNr>()
    {
        @Override
        public CellSignalStrengthNr createFromParcel(Parcel source)
        {
            int csiRsrp = source.readInt();
            int csiRsrq = source.readInt();
            int csiSinr = source.readInt();
            int csiCqiTableIndex = source.readInt();
            int[] report = source.createIntArray();
            List<Integer> csiCqiReport = new ArrayList<>();
            for (int value : report) csiCqiReport.add(value);
            return new CellSignalStrengthNr(csiRsrp, csiRsrq, csiSinr, csiCqiTableIndex, csiCqiReport, source.readInt(), source.readInt(),
                source.readInt(), source.readInt());
        }

        @Override
        public CellSignalStrengthNr[] newArray(int size)
        {
            return new CellSignalStrengthNr[size];
        }
    };

    private final int mCsiRsrp;
    private final int mCsiRsrq;
    private final int mCsiSinr;
    private final int mCsiCqiTableIndex;
    private final List<Integer> mCsiCqiReport;
    private final int mSsRsrp;
    private final int mSsRsrq;
    private final int mSsSinr;
    private final int mTimingAdvance;

    CellSignalStrengthNr()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, null, CellInfo.UNAVAILABLE,
            CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE);
    }

    CellSignalStrengthNr(int csiRsrp, int csiRsrq, int csiSinr, int csiCqiTableIndex, List<Integer> csiCqiReport, int ssRsrp, int ssRsrq,
        int ssSinr, int timingAdvance)
    {
        mCsiRsrp = inRangeOrUnavailable(csiRsrp, -156, -31);
        mCsiRsrq = inRangeOrUnavailable(csiRsrq, -20, -3);
        mCsiSinr = inRangeOrUnavailable(csiSinr, -23, 23);
        mCsiCqiTableIndex = inRangeOrUnavailable(csiCqiTableIndex, 1, 3);
        mCsiCqiReport = csiCqiReport == null ? new ArrayList<>() : new ArrayList<>(csiCqiReport);
        mSsRsrp = inRangeOrUnavailable(ssRsrp, -156, -31);
        mSsRsrq = inRangeOrUnavailable(ssRsrq, -43, 20);
        mSsSinr = inRangeOrUnavailable(ssSinr, -23, 40);
        mTimingAdvance = inRangeOrUnavailable(timingAdvance, 0, 1282);
    }

    public int getSsRsrp()
    {
        return mSsRsrp;
    }

    public int getSsRsrq()
    {
        return mSsRsrq;
    }

    public int getSsSinr()
    {
        return mSsSinr;
    }

    public int getCsiRsrp()
    {
        return mCsiRsrp;
    }

    public int getCsiRsrq()
    {
        return mCsiRsrq;
    }

    public int getCsiSinr()
    {
        return mCsiSinr;
    }

    public int getCsiCqiTableIndex()
    {
        return mCsiCqiTableIndex;
    }

    public List<Integer> getCsiCqiReport()
    {
        return Collections.unmodifiableList(mCsiCqiReport);
    }

    public int getTimingAdvanceMicros()
    {
        return mTimingAdvance;
    }

    @Override
    public int getLevel()
    {
        return levelFromThresholds(mSsRsrp, SS_RSRP_THRESHOLDS);
    }

    @Override
    public int getAsuLevel()
    {
        int dbm = getDbm();
        if (dbm == CellInfo.UNAVAILABLE) return 99;
        if (dbm <= -140) return 0;
        if (dbm >= -43) return 97;
        return dbm + 140;
    }

    @Override
    public int getDbm()
    {
        return mSsRsrp;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mCsiRsrp);
        dest.writeInt(mCsiRsrq);
        dest.writeInt(mCsiSinr);
        dest.writeInt(mCsiCqiTableIndex);
        int[] report = new int[mCsiCqiReport.size()];
        for (int index = 0; index < report.length; index++) report[index] = mCsiCqiReport.get(index);
        dest.writeIntArray(report);
        dest.writeInt(mSsRsrp);
        dest.writeInt(mSsRsrq);
        dest.writeInt(mSsSinr);
        dest.writeInt(mTimingAdvance);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellSignalStrengthNr)) return false;
        CellSignalStrengthNr strength = (CellSignalStrengthNr)other;
        return mCsiRsrp == strength.mCsiRsrp && mCsiRsrq == strength.mCsiRsrq && mCsiSinr == strength.mCsiSinr
            && mCsiCqiTableIndex == strength.mCsiCqiTableIndex && mCsiCqiReport.equals(strength.mCsiCqiReport) && mSsRsrp == strength.mSsRsrp
            && mSsRsrq == strength.mSsRsrq && mSsSinr == strength.mSsSinr && mTimingAdvance == strength.mTimingAdvance;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mCsiRsrp, mCsiRsrq, mCsiSinr, mCsiCqiTableIndex, mCsiCqiReport, mSsRsrp, mSsRsrq, mSsSinr, mTimingAdvance);
    }

    @Override
    public String toString()
    {
        return "CellSignalStrengthNr:{ csiRsrp = " + mCsiRsrp + " csiRsrq = " + mCsiRsrq + " csiCqiTableIndex = " + mCsiCqiTableIndex
            + " csiCqiReport = " + mCsiCqiReport + " ssRsrp = " + mSsRsrp + " ssRsrq = " + mSsRsrq + " ssSinr = " + mSsSinr
            + " level = " + getLevel() + " timingAdvance = " + mTimingAdvance + " }";
    }
}
