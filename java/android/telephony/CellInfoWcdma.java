package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellInfoWcdma extends CellInfo implements Parcelable
{
    public static final Parcelable.Creator<CellInfoWcdma> CREATOR = new Parcelable.Creator<CellInfoWcdma>()
    {
        @Override
        public CellInfoWcdma createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellInfoWcdma(source);
        }

        @Override
        public CellInfoWcdma[] newArray(int size)
        {
            return new CellInfoWcdma[size];
        }
    };

    private final CellIdentityWcdma mCellIdentity;
    private final CellSignalStrengthWcdma mCellSignalStrength;

    CellInfoWcdma()
    {
        mCellIdentity = new CellIdentityWcdma();
        mCellSignalStrength = new CellSignalStrengthWcdma();
    }

    CellInfoWcdma(Parcel source)
    {
        super(source);
        source.readInt();
        mCellIdentity = new CellIdentityWcdma(source);
        mCellSignalStrength = CellSignalStrengthWcdma.CREATOR.createFromParcel(source);
    }

    @Override
    public CellIdentityWcdma getCellIdentity()
    {
        return mCellIdentity;
    }

    @Override
    public CellSignalStrengthWcdma getCellSignalStrength()
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
        writeBaseToParcel(dest, CellIdentity.TYPE_WCDMA);
        mCellIdentity.writeToParcel(dest, flags);
        mCellSignalStrength.writeToParcel(dest, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellInfoWcdma) || !super.equals(other)) return false;
        CellInfoWcdma info = (CellInfoWcdma)other;
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
        return "CellInfoWcdma:{ " + super.toString() + " " + mCellIdentity + " " + mCellSignalStrength + "}";
    }
}
