package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellInfoCdma extends CellInfo implements Parcelable
{
    public static final Parcelable.Creator<CellInfoCdma> CREATOR = new Parcelable.Creator<CellInfoCdma>()
    {
        @Override
        public CellInfoCdma createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellInfoCdma(source);
        }

        @Override
        public CellInfoCdma[] newArray(int size)
        {
            return new CellInfoCdma[size];
        }
    };

    private final CellIdentityCdma mCellIdentity;
    private final CellSignalStrengthCdma mCellSignalStrength;

    CellInfoCdma()
    {
        mCellIdentity = new CellIdentityCdma();
        mCellSignalStrength = new CellSignalStrengthCdma();
    }

    CellInfoCdma(Parcel source)
    {
        super(source);
        source.readInt();
        mCellIdentity = new CellIdentityCdma(source);
        mCellSignalStrength = CellSignalStrengthCdma.CREATOR.createFromParcel(source);
    }

    @Override
    public CellIdentityCdma getCellIdentity()
    {
        return mCellIdentity;
    }

    @Override
    public CellSignalStrengthCdma getCellSignalStrength()
    {
        return mCellSignalStrength;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        writeBaseToParcel(dest, CellIdentity.TYPE_CDMA);
        mCellIdentity.writeToParcel(dest, flags);
        mCellSignalStrength.writeToParcel(dest, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellInfoCdma) || !super.equals(other)) return false;
        CellInfoCdma info = (CellInfoCdma)other;
        return mCellIdentity.equals(info.mCellIdentity) && mCellSignalStrength.equals(info.mCellSignalStrength);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), mCellIdentity, mCellSignalStrength);
    }

    @Override
    public String toString()
    {
        return "CellInfoCdma:{ " + super.toString() + " " + mCellIdentity + " " + mCellSignalStrength + "}";
    }
}
