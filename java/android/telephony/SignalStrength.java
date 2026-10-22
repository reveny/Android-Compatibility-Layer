package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SignalStrength implements Parcelable
{
    public static final int INVALID = Integer.MAX_VALUE;

    public static final Parcelable.Creator<SignalStrength> CREATOR = new Parcelable.Creator<SignalStrength>()
    {
        @Override
        public SignalStrength createFromParcel(Parcel source)
        {
            return new SignalStrength(source);
        }

        @Override
        public SignalStrength[] newArray(int size)
        {
            return new SignalStrength[size];
        }
    };

    private final CellSignalStrengthCdma mCdma;
    private final CellSignalStrengthGsm mGsm;
    private final CellSignalStrengthWcdma mWcdma;
    private final CellSignalStrengthLte mLte;
    private final CellSignalStrengthNr mNr;
    private final long mTimestampMillis;

    SignalStrength()
    {
        mCdma = new CellSignalStrengthCdma();
        mGsm = new CellSignalStrengthGsm();
        mWcdma = new CellSignalStrengthWcdma();
        mLte = new CellSignalStrengthLte();
        mNr = new CellSignalStrengthNr();
        mTimestampMillis = SystemClock.elapsedRealtime();
    }

    public SignalStrength(SignalStrength source)
    {
        mCdma = source.mCdma;
        mGsm = source.mGsm;
        mWcdma = source.mWcdma;
        mLte = source.mLte;
        mNr = source.mNr;
        mTimestampMillis = source.mTimestampMillis;
    }

    SignalStrength(Parcel source)
    {
        mCdma = CellSignalStrengthCdma.CREATOR.createFromParcel(source);
        mGsm = CellSignalStrengthGsm.CREATOR.createFromParcel(source);
        mWcdma = CellSignalStrengthWcdma.CREATOR.createFromParcel(source);
        mLte = CellSignalStrengthLte.CREATOR.createFromParcel(source);
        mNr = CellSignalStrengthNr.CREATOR.createFromParcel(source);
        mTimestampMillis = source.readLong();
    }

    public List<CellSignalStrength> getCellSignalStrengths()
    {
        return getCellSignalStrengths(CellSignalStrength.class);
    }

    public <T extends CellSignalStrength> List<T> getCellSignalStrengths(Class<T> type)
    {
        List<T> result = new ArrayList<>();
        addIfValid(result, type, mLte, new CellSignalStrengthLte());
        addIfValid(result, type, mNr, new CellSignalStrengthNr());
        addIfValid(result, type, mWcdma, new CellSignalStrengthWcdma());
        addIfValid(result, type, mGsm, new CellSignalStrengthGsm());
        addIfValid(result, type, mCdma, new CellSignalStrengthCdma());
        return result;
    }

    private static <T extends CellSignalStrength> void addIfValid(List<T> result, Class<T> type, CellSignalStrength strength,
        CellSignalStrength empty)
    {
        if (type.isInstance(strength) && !strength.equals(empty)) result.add(type.cast(strength));
    }

    public long getTimestampMillis()
    {
        return mTimestampMillis;
    }

    @Deprecated
    public int getGsmSignalStrength()
    {
        if (!mWcdma.equals(new CellSignalStrengthWcdma())) return mWcdma.getAsuLevel();
        return mGsm.getAsuLevel();
    }

    @Deprecated
    public int getGsmBitErrorRate()
    {
        return mGsm.getBitErrorRate();
    }

    @Deprecated
    public int getCdmaDbm()
    {
        return mCdma.getCdmaDbm();
    }

    @Deprecated
    public int getCdmaEcio()
    {
        return mCdma.getCdmaEcio();
    }

    @Deprecated
    public int getEvdoDbm()
    {
        return mCdma.getEvdoDbm();
    }

    @Deprecated
    public int getEvdoEcio()
    {
        return mCdma.getEvdoEcio();
    }

    @Deprecated
    public int getEvdoSnr()
    {
        return mCdma.getEvdoSnr();
    }

    public int getLevel()
    {
        int level = CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        for (CellSignalStrength strength : getCellSignalStrengths()) level = Math.max(level, strength.getLevel());
        return level;
    }

    @Deprecated
    public boolean isGsm()
    {
        return mCdma.equals(new CellSignalStrengthCdma());
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        mCdma.writeToParcel(dest, flags);
        mGsm.writeToParcel(dest, flags);
        mWcdma.writeToParcel(dest, flags);
        mLte.writeToParcel(dest, flags);
        mNr.writeToParcel(dest, flags);
        dest.writeLong(mTimestampMillis);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof SignalStrength)) return false;
        SignalStrength strength = (SignalStrength)other;
        return mCdma.equals(strength.mCdma) && mGsm.equals(strength.mGsm) && mWcdma.equals(strength.mWcdma) && mLte.equals(strength.mLte)
            && mNr.equals(strength.mNr);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mCdma, mGsm, mWcdma, mLte, mNr);
    }

    @Override
    public String toString()
    {
        return "SignalStrength:{mCdma=" + mCdma + ",mGsm=" + mGsm + ",mWcdma=" + mWcdma + ",mLte=" + mLte + ",mNr=" + mNr
            + ",primary=" + (getCellSignalStrengths().isEmpty() ? "CellSignalStrength" : getCellSignalStrengths().get(0)) + "}";
    }
}
