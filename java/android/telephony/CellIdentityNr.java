package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class CellIdentityNr extends CellIdentity
{
    public static final Parcelable.Creator<CellIdentityNr> CREATOR = new Parcelable.Creator<CellIdentityNr>()
    {
        @Override
        public CellIdentityNr createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellIdentityNr(source);
        }

        @Override
        public CellIdentityNr[] newArray(int size)
        {
            return new CellIdentityNr[size];
        }
    };

    private final int mPci;
    private final int mTac;
    private final int mNrArfcn;
    private final int[] mBands;
    private final long mNci;
    private final Set<String> mAdditionalPlmns;

    CellIdentityNr()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, new int[0], CellInfo.UNAVAILABLE_LONG, null, null, null,
            null, null);
    }

    CellIdentityNr(int pci, int tac, int nrArfcn, int[] bands, long nci, String mcc, String mnc, String alphaLong, String alphaShort,
        Set<String> additionalPlmns)
    {
        super(TYPE_NR, mcc, mnc, alphaLong, alphaShort);
        mPci = pci;
        mTac = tac;
        mNrArfcn = nrArfcn;
        mBands = bands == null ? new int[0] : bands.clone();
        mNci = nci;
        mAdditionalPlmns = additionalPlmns == null ? new LinkedHashSet<>() : new LinkedHashSet<>(additionalPlmns);
    }

    CellIdentityNr(Parcel source)
    {
        super(TYPE_NR, source);
        mPci = source.readInt();
        mTac = source.readInt();
        mNrArfcn = source.readInt();
        mBands = source.createIntArray();
        mNci = source.readLong();
        mAdditionalPlmns = new LinkedHashSet<>(source.createStringArrayList());
    }

    public long getNci()
    {
        return mNci;
    }

    public int getNrarfcn()
    {
        return mNrArfcn;
    }

    public int getPci()
    {
        return mPci;
    }

    public int getTac()
    {
        return mTac;
    }

    public int[] getBands()
    {
        return mBands.clone();
    }

    public String getMccString()
    {
        return mMccString;
    }

    public String getMncString()
    {
        return mMncString;
    }

    public Set<String> getAdditionalPlmns()
    {
        return Collections.unmodifiableSet(mAdditionalPlmns);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        super.writeToParcel(dest, flags);
        dest.writeInt(mPci);
        dest.writeInt(mTac);
        dest.writeInt(mNrArfcn);
        dest.writeIntArray(mBands);
        dest.writeLong(mNci);
        dest.writeStringList(new ArrayList<>(mAdditionalPlmns));
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellIdentityNr) || !super.equals(other)) return false;
        CellIdentityNr identity = (CellIdentityNr)other;
        return mPci == identity.mPci && mTac == identity.mTac && mNrArfcn == identity.mNrArfcn && Arrays.equals(mBands, identity.mBands)
            && mNci == identity.mNci && mAdditionalPlmns.equals(identity.mAdditionalPlmns);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), mPci, mTac, mNrArfcn, Arrays.hashCode(mBands), mNci, mAdditionalPlmns);
    }

    @Override
    public String toString()
    {
        return "CellIdentityNr:{ mPci=" + mPci + " mTac=" + mTac + " mNrArfcn=" + mNrArfcn + " mBands=" + Arrays.toString(mBands)
            + " mNci=" + mNci + " " + baseToString() + " mAdditionalPlmns=" + mAdditionalPlmns + "}";
    }
}
