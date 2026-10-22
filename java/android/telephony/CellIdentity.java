package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public abstract class CellIdentity implements Parcelable
{
    static final int TYPE_GSM = 1;
    static final int TYPE_CDMA = 2;
    static final int TYPE_LTE = 3;
    static final int TYPE_WCDMA = 4;
    static final int TYPE_TDSCDMA = 5;
    static final int TYPE_NR = 6;

    public static final Parcelable.Creator<CellIdentity> CREATOR = new Parcelable.Creator<CellIdentity>()
    {
        @Override
        public CellIdentity createFromParcel(Parcel source)
        {
            int type = source.readInt();
            switch (type)
            {
                case TYPE_GSM: return new CellIdentityGsm(source);
                case TYPE_CDMA: return new CellIdentityCdma(source);
                case TYPE_LTE: return new CellIdentityLte(source);
                case TYPE_WCDMA: return new CellIdentityWcdma(source);
                case TYPE_NR: return new CellIdentityNr(source);
                default: throw new IllegalArgumentException("Bad CellIdentity type " + type);
            }
        }

        @Override
        public CellIdentity[] newArray(int size)
        {
            return new CellIdentity[size];
        }
    };

    final int mType;
    final String mMccString;
    final String mMncString;
    final String mAlphaLong;
    final String mAlphaShort;

    CellIdentity(int type, String mcc, String mnc, String alphaLong, String alphaShort)
    {
        mType = type;
        mMccString = mcc;
        mMncString = mnc;
        mAlphaLong = alphaLong;
        mAlphaShort = alphaShort;
    }

    CellIdentity(int type, Parcel source)
    {
        mType = type;
        mMccString = source.readString();
        mMncString = source.readString();
        mAlphaLong = source.readString();
        mAlphaShort = source.readString();
    }

    public CharSequence getOperatorAlphaLong()
    {
        return mAlphaLong;
    }

    public CharSequence getOperatorAlphaShort()
    {
        return mAlphaShort;
    }

    String getPlmn()
    {
        return mMccString == null || mMncString == null ? null : mMccString + mMncString;
    }

    static int parseCode(String code)
    {
        if (code == null) return CellInfo.UNAVAILABLE;
        try
        {
            return Integer.parseInt(code);
        }
        catch (NumberFormatException exception)
        {
            return CellInfo.UNAVAILABLE;
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mType);
        dest.writeString(mMccString);
        dest.writeString(mMncString);
        dest.writeString(mAlphaLong);
        dest.writeString(mAlphaShort);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellIdentity)) return false;
        CellIdentity identity = (CellIdentity)other;
        return mType == identity.mType && Objects.equals(mMccString, identity.mMccString) && Objects.equals(mMncString, identity.mMncString)
            && Objects.equals(mAlphaLong, identity.mAlphaLong) && Objects.equals(mAlphaShort, identity.mAlphaShort);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mType, mMccString, mMncString, mAlphaLong, mAlphaShort);
    }

    String baseToString()
    {
        return "mMcc=" + mMccString + " mMnc=" + mMncString + " mAlphaLong=" + mAlphaLong + " mAlphaShort=" + mAlphaShort;
    }
}
