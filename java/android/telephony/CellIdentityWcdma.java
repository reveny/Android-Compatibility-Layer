package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class CellIdentityWcdma extends CellIdentity
{
    public static final Parcelable.Creator<CellIdentityWcdma> CREATOR = new Parcelable.Creator<CellIdentityWcdma>()
    {
        @Override
        public CellIdentityWcdma createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellIdentityWcdma(source);
        }

        @Override
        public CellIdentityWcdma[] newArray(int size)
        {
            return new CellIdentityWcdma[size];
        }
    };

    private final int mLac;
    private final int mCid;
    private final int mPsc;
    private final int mUarfcn;
    private final Set<String> mAdditionalPlmns;
    private final ClosedSubscriberGroupInfo mCsgInfo;

    CellIdentityWcdma()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, null, null, null, null, null, null);
    }

    CellIdentityWcdma(int lac, int cid, int psc, int uarfcn, String mcc, String mnc, String alphaLong, String alphaShort,
        Set<String> additionalPlmns, ClosedSubscriberGroupInfo csgInfo)
    {
        super(TYPE_WCDMA, mcc, mnc, alphaLong, alphaShort);
        mLac = lac;
        mCid = cid;
        mPsc = psc;
        mUarfcn = uarfcn;
        mAdditionalPlmns = additionalPlmns == null ? new LinkedHashSet<>() : new LinkedHashSet<>(additionalPlmns);
        mCsgInfo = csgInfo;
    }

    CellIdentityWcdma(Parcel source)
    {
        super(TYPE_WCDMA, source);
        mLac = source.readInt();
        mCid = source.readInt();
        mPsc = source.readInt();
        mUarfcn = source.readInt();
        mAdditionalPlmns = new LinkedHashSet<>(source.createStringArrayList());
        mCsgInfo = source.readBoolean() ? ClosedSubscriberGroupInfo.CREATOR.createFromParcel(source) : null;
    }

    public int getMcc()
    {
        return parseCode(mMccString);
    }

    public int getMnc()
    {
        return parseCode(mMncString);
    }

    public int getLac()
    {
        return mLac;
    }

    public int getCid()
    {
        return mCid;
    }

    public int getPsc()
    {
        return mPsc;
    }

    public int getUarfcn()
    {
        return mUarfcn;
    }

    public String getMccString()
    {
        return mMccString;
    }

    public String getMncString()
    {
        return mMncString;
    }

    public String getMobileNetworkOperator()
    {
        return getPlmn();
    }

    public Set<String> getAdditionalPlmns()
    {
        return Collections.unmodifiableSet(mAdditionalPlmns);
    }

    public ClosedSubscriberGroupInfo getClosedSubscriberGroupInfo()
    {
        return mCsgInfo;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        super.writeToParcel(dest, flags);
        dest.writeInt(mLac);
        dest.writeInt(mCid);
        dest.writeInt(mPsc);
        dest.writeInt(mUarfcn);
        dest.writeStringList(new ArrayList<>(mAdditionalPlmns));
        dest.writeBoolean(mCsgInfo != null);
        if (mCsgInfo != null) mCsgInfo.writeToParcel(dest, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellIdentityWcdma) || !super.equals(other)) return false;
        CellIdentityWcdma identity = (CellIdentityWcdma)other;
        return mLac == identity.mLac && mCid == identity.mCid && mPsc == identity.mPsc && mUarfcn == identity.mUarfcn
            && mAdditionalPlmns.equals(identity.mAdditionalPlmns) && Objects.equals(mCsgInfo, identity.mCsgInfo);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), mLac, mCid, mPsc, mUarfcn, mAdditionalPlmns, mCsgInfo);
    }

    @Override
    public String toString()
    {
        return "CellIdentityWcdma:{ mLac=" + mLac + " mCid=" + mCid + " mPsc=" + mPsc + " mUarfcn=" + mUarfcn + " " + baseToString()
            + " mAdditionalPlmns=" + mAdditionalPlmns + " mCsgInfo=" + mCsgInfo + "}";
    }
}
