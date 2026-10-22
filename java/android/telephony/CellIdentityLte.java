package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class CellIdentityLte extends CellIdentity
{
    public static final Parcelable.Creator<CellIdentityLte> CREATOR = new Parcelable.Creator<CellIdentityLte>()
    {
        @Override
        public CellIdentityLte createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellIdentityLte(source);
        }

        @Override
        public CellIdentityLte[] newArray(int size)
        {
            return new CellIdentityLte[size];
        }
    };

    private final int mCi;
    private final int mPci;
    private final int mTac;
    private final int mEarfcn;
    private final int[] mBands;
    private final int mBandwidth;
    private final Set<String> mAdditionalPlmns;
    private final ClosedSubscriberGroupInfo mCsgInfo;

    CellIdentityLte()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, new int[0], CellInfo.UNAVAILABLE,
            null, null, null, null, null, null);
    }

    CellIdentityLte(int ci, int pci, int tac, int earfcn, int[] bands, int bandwidth, String mcc, String mnc, String alphaLong,
        String alphaShort, Set<String> additionalPlmns, ClosedSubscriberGroupInfo csgInfo)
    {
        super(TYPE_LTE, mcc, mnc, alphaLong, alphaShort);
        mCi = ci;
        mPci = pci;
        mTac = tac;
        mEarfcn = earfcn;
        mBands = bands == null ? new int[0] : bands.clone();
        mBandwidth = bandwidth;
        mAdditionalPlmns = additionalPlmns == null ? new LinkedHashSet<>() : new LinkedHashSet<>(additionalPlmns);
        mCsgInfo = csgInfo;
    }

    CellIdentityLte(Parcel source)
    {
        super(TYPE_LTE, source);
        mCi = source.readInt();
        mPci = source.readInt();
        mTac = source.readInt();
        mEarfcn = source.readInt();
        mBands = source.createIntArray();
        mBandwidth = source.readInt();
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

    public int getCi()
    {
        return mCi;
    }

    public int getPci()
    {
        return mPci;
    }

    public int getTac()
    {
        return mTac;
    }

    public int getEarfcn()
    {
        return mEarfcn;
    }

    public int[] getBands()
    {
        return mBands.clone();
    }

    public int getBandwidth()
    {
        return mBandwidth;
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
        dest.writeInt(mCi);
        dest.writeInt(mPci);
        dest.writeInt(mTac);
        dest.writeInt(mEarfcn);
        dest.writeIntArray(mBands);
        dest.writeInt(mBandwidth);
        dest.writeStringList(new ArrayList<>(mAdditionalPlmns));
        dest.writeBoolean(mCsgInfo != null);
        if (mCsgInfo != null) mCsgInfo.writeToParcel(dest, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellIdentityLte) || !super.equals(other)) return false;
        CellIdentityLte identity = (CellIdentityLte)other;
        return mCi == identity.mCi && mPci == identity.mPci && mTac == identity.mTac && mEarfcn == identity.mEarfcn
            && Arrays.equals(mBands, identity.mBands) && mBandwidth == identity.mBandwidth && mAdditionalPlmns.equals(identity.mAdditionalPlmns)
            && Objects.equals(mCsgInfo, identity.mCsgInfo);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), mCi, mPci, mTac, mEarfcn, Arrays.hashCode(mBands), mBandwidth, mAdditionalPlmns, mCsgInfo);
    }

    @Override
    public String toString()
    {
        return "CellIdentityLte:{ mCi=" + mCi + " mPci=" + mPci + " mTac=" + mTac + " mEarfcn=" + mEarfcn + " mBands=" + Arrays.toString(mBands)
            + " mBandwidth=" + mBandwidth + " " + baseToString() + " mAdditionalPlmns=" + mAdditionalPlmns + " mCsgInfo=" + mCsgInfo + "}";
    }
}
