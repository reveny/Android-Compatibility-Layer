package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellInfoNr extends CellInfo
{
    public static final Parcelable.Creator<CellInfoNr> CREATOR = new Parcelable.Creator<CellInfoNr>()
    {
        @Override
        public CellInfoNr createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellInfoNr(source);
        }

        @Override
        public CellInfoNr[] newArray(int size)
        {
            return new CellInfoNr[size];
        }
    };

    private final CellIdentityNr mCellIdentity;
    private final CellSignalStrengthNr mCellSignalStrength;

    CellInfoNr()
    {
        mCellIdentity = new CellIdentityNr();
        mCellSignalStrength = new CellSignalStrengthNr();
    }

    CellInfoNr(Parcel source)
    {
        super(source);
        source.readInt();
        mCellIdentity = new CellIdentityNr(source);
        mCellSignalStrength = CellSignalStrengthNr.CREATOR.createFromParcel(source);
    }

    @Override
    public CellIdentity getCellIdentity()
    {
        return mCellIdentity;
    }

    @Override
    public CellSignalStrength getCellSignalStrength()
    {
        return mCellSignalStrength;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        writeBaseToParcel(dest, CellIdentity.TYPE_NR);
        mCellIdentity.writeToParcel(dest, flags);
        mCellSignalStrength.writeToParcel(dest, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellInfoNr) || !super.equals(other)) return false;
        CellInfoNr info = (CellInfoNr)other;
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
        return "CellInfoNr:{ " + super.toString() + " " + mCellIdentity + " " + mCellSignalStrength + " }";
    }
}
