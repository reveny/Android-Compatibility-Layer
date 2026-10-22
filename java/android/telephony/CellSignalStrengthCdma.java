package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellSignalStrengthCdma extends CellSignalStrength implements Parcelable
{
    private static final int[] CDMA_DBM_THRESHOLDS = {-100, -95, -85, -75};
    private static final int[] EVDO_DBM_THRESHOLDS = {-105, -90, -75, -65};

    public static final Parcelable.Creator<CellSignalStrengthCdma> CREATOR = new Parcelable.Creator<CellSignalStrengthCdma>()
    {
        @Override
        public CellSignalStrengthCdma createFromParcel(Parcel source)
        {
            return new CellSignalStrengthCdma(source.readInt(), source.readInt(), source.readInt(), source.readInt(), source.readInt());
        }

        @Override
        public CellSignalStrengthCdma[] newArray(int size)
        {
            return new CellSignalStrengthCdma[size];
        }
    };

    private final int mCdmaDbm;
    private final int mCdmaEcio;
    private final int mEvdoDbm;
    private final int mEvdoEcio;
    private final int mEvdoSnr;

    CellSignalStrengthCdma()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE);
    }

    CellSignalStrengthCdma(int cdmaDbm, int cdmaEcio, int evdoDbm, int evdoEcio, int evdoSnr)
    {
        mCdmaDbm = inRangeOrUnavailable(cdmaDbm, -120, 0);
        mCdmaEcio = inRangeOrUnavailable(cdmaEcio, -160, 0);
        mEvdoDbm = inRangeOrUnavailable(evdoDbm, -120, 0);
        mEvdoEcio = inRangeOrUnavailable(evdoEcio, -160, 0);
        mEvdoSnr = inRangeOrUnavailable(evdoSnr, 0, 8);
    }

    @Override
    public int getLevel()
    {
        int cdmaLevel = getCdmaLevel();
        int evdoLevel = getEvdoLevel();
        if (evdoLevel == SIGNAL_STRENGTH_NONE_OR_UNKNOWN) return cdmaLevel;
        if (cdmaLevel == SIGNAL_STRENGTH_NONE_OR_UNKNOWN) return evdoLevel;
        return Math.min(cdmaLevel, evdoLevel);
    }

    @Override
    public int getAsuLevel()
    {
        if (mCdmaDbm == CellInfo.UNAVAILABLE) return 99;
        if (mCdmaDbm >= -75) return 16;
        if (mCdmaDbm >= -82) return 8;
        if (mCdmaDbm >= -90) return 4;
        if (mCdmaDbm >= -95) return 2;
        if (mCdmaDbm >= -100) return 1;
        return 99;
    }

    public int getCdmaLevel()
    {
        return levelFromThresholds(mCdmaDbm, CDMA_DBM_THRESHOLDS);
    }

    public int getEvdoLevel()
    {
        return levelFromThresholds(mEvdoDbm, EVDO_DBM_THRESHOLDS);
    }

    @Override
    public int getDbm()
    {
        if (mCdmaDbm == CellInfo.UNAVAILABLE) return mEvdoDbm;
        if (mEvdoDbm == CellInfo.UNAVAILABLE) return mCdmaDbm;
        return Math.min(mCdmaDbm, mEvdoDbm);
    }

    public int getCdmaDbm()
    {
        return mCdmaDbm;
    }

    public int getCdmaEcio()
    {
        return mCdmaEcio;
    }

    public int getEvdoDbm()
    {
        return mEvdoDbm;
    }

    public int getEvdoEcio()
    {
        return mEvdoEcio;
    }

    public int getEvdoSnr()
    {
        return mEvdoSnr;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mCdmaDbm);
        dest.writeInt(mCdmaEcio);
        dest.writeInt(mEvdoDbm);
        dest.writeInt(mEvdoEcio);
        dest.writeInt(mEvdoSnr);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellSignalStrengthCdma)) return false;
        CellSignalStrengthCdma strength = (CellSignalStrengthCdma)other;
        return mCdmaDbm == strength.mCdmaDbm && mCdmaEcio == strength.mCdmaEcio && mEvdoDbm == strength.mEvdoDbm
            && mEvdoEcio == strength.mEvdoEcio && mEvdoSnr == strength.mEvdoSnr;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mCdmaDbm, mCdmaEcio, mEvdoDbm, mEvdoEcio, mEvdoSnr);
    }

    @Override
    public String toString()
    {
        return "CellSignalStrengthCdma: cdmaDbm=" + mCdmaDbm + " cdmaEcio=" + mCdmaEcio + " evdoDbm=" + mEvdoDbm + " evdoEcio=" + mEvdoEcio
            + " evdoSnr=" + mEvdoSnr + " level=" + getLevel();
    }
}
