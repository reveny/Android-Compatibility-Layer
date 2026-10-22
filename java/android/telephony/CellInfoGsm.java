package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellInfoGsm extends CellInfo implements Parcelable
{
    public static final Parcelable.Creator<CellInfoGsm> CREATOR = new Parcelable.Creator<CellInfoGsm>()
    {
        @Override
        public CellInfoGsm createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellInfoGsm(source);
        }

        @Override
        public CellInfoGsm[] newArray(int size)
        {
            return new CellInfoGsm[size];
        }
    };

    private final CellIdentityGsm mCellIdentity;
    private final CellSignalStrengthGsm mCellSignalStrength;

    CellInfoGsm()
    {
        mCellIdentity = new CellIdentityGsm();
        mCellSignalStrength = new CellSignalStrengthGsm();
    }

    CellInfoGsm(Parcel source)
    {
        super(source);
        source.readInt();
        mCellIdentity = new CellIdentityGsm(source);
        mCellSignalStrength = CellSignalStrengthGsm.CREATOR.createFromParcel(source);
    }

    @Override
    public CellIdentityGsm getCellIdentity()
    {
        return mCellIdentity;
    }

    @Override
    public CellSignalStrengthGsm getCellSignalStrength()
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
        writeBaseToParcel(dest, CellIdentity.TYPE_GSM);
        mCellIdentity.writeToParcel(dest, flags);
        mCellSignalStrength.writeToParcel(dest, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellInfoGsm) || !super.equals(other)) return false;
        CellInfoGsm info = (CellInfoGsm)other;
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
        return "CellInfoGsm:{ " + super.toString() + " " + mCellIdentity + " " + mCellSignalStrength + "}";
    }
}
