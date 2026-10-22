package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class CellIdentityGsm extends CellIdentity
{
    public static final Parcelable.Creator<CellIdentityGsm> CREATOR = new Parcelable.Creator<CellIdentityGsm>()
    {
        @Override
        public CellIdentityGsm createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellIdentityGsm(source);
        }

        @Override
        public CellIdentityGsm[] newArray(int size)
        {
            return new CellIdentityGsm[size];
        }
    };

    private final int mLac;
    private final int mCid;
    private final int mArfcn;
    private final int mBsic;
    private final Set<String> mAdditionalPlmns;

    CellIdentityGsm()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, null, null, null, null, null);
    }

    CellIdentityGsm(int lac, int cid, int arfcn, int bsic, String mcc, String mnc, String alphaLong, String alphaShort,
        Set<String> additionalPlmns)
    {
        super(TYPE_GSM, mcc, mnc, alphaLong, alphaShort);
        mLac = lac;
        mCid = cid;
        mArfcn = arfcn;
        mBsic = bsic;
        mAdditionalPlmns = additionalPlmns == null ? new LinkedHashSet<>() : new LinkedHashSet<>(additionalPlmns);
    }

    CellIdentityGsm(Parcel source)
    {
        super(TYPE_GSM, source);
        mLac = source.readInt();
        mCid = source.readInt();
        mArfcn = source.readInt();
        mBsic = source.readInt();
        mAdditionalPlmns = new LinkedHashSet<>(source.createStringArrayList());
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

    public int getArfcn()
    {
        return mArfcn;
    }

    public int getBsic()
    {
        return mBsic;
    }

    public int getPsc()
    {
        return CellInfo.UNAVAILABLE;
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

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        super.writeToParcel(dest, flags);
        dest.writeInt(mLac);
        dest.writeInt(mCid);
        dest.writeInt(mArfcn);
        dest.writeInt(mBsic);
        dest.writeStringList(new ArrayList<>(mAdditionalPlmns));
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellIdentityGsm) || !super.equals(other)) return false;
        CellIdentityGsm identity = (CellIdentityGsm)other;
        return mLac == identity.mLac && mCid == identity.mCid && mArfcn == identity.mArfcn && mBsic == identity.mBsic
            && mAdditionalPlmns.equals(identity.mAdditionalPlmns);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), mLac, mCid, mArfcn, mBsic, mAdditionalPlmns);
    }

    @Override
    public String toString()
    {
        return "CellIdentityGsm:{ mLac=" + mLac + " mCid=" + mCid + " mArfcn=" + mArfcn + " mBsic=" + mBsic + " " + baseToString()
            + " mAdditionalPlmns=" + mAdditionalPlmns + "}";
    }
}
