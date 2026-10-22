package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellInfoLte extends CellInfo implements Parcelable
{
    public static final Parcelable.Creator<CellInfoLte> CREATOR = new Parcelable.Creator<CellInfoLte>()
    {
        @Override
        public CellInfoLte createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellInfoLte(source);
        }

        @Override
        public CellInfoLte[] newArray(int size)
        {
            return new CellInfoLte[size];
        }
    };

    private final CellIdentityLte mCellIdentity;
    private final CellSignalStrengthLte mCellSignalStrength;

    CellInfoLte()
    {
        mCellIdentity = new CellIdentityLte();
        mCellSignalStrength = new CellSignalStrengthLte();
    }

    CellInfoLte(Parcel source)
    {
        super(source);
        source.readInt();
        mCellIdentity = new CellIdentityLte(source);
        mCellSignalStrength = CellSignalStrengthLte.CREATOR.createFromParcel(source);
    }

    @Override
    public CellIdentityLte getCellIdentity()
    {
        return mCellIdentity;
    }

    @Override
    public CellSignalStrengthLte getCellSignalStrength()
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
        writeBaseToParcel(dest, CellIdentity.TYPE_LTE);
        mCellIdentity.writeToParcel(dest, flags);
        mCellSignalStrength.writeToParcel(dest, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellInfoLte) || !super.equals(other)) return false;
        CellInfoLte info = (CellInfoLte)other;
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
        return "CellInfoLte:{ " + super.toString() + " " + mCellIdentity + " " + mCellSignalStrength + "}";
    }
}
