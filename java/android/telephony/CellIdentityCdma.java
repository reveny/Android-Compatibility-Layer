package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CellIdentityCdma extends CellIdentity
{
    public static final Parcelable.Creator<CellIdentityCdma> CREATOR = new Parcelable.Creator<CellIdentityCdma>()
    {
        @Override
        public CellIdentityCdma createFromParcel(Parcel source)
        {
            source.readInt();
            return new CellIdentityCdma(source);
        }

        @Override
        public CellIdentityCdma[] newArray(int size)
        {
            return new CellIdentityCdma[size];
        }
    };

    private final int mNetworkId;
    private final int mSystemId;
    private final int mBasestationId;
    private final int mLongitude;
    private final int mLatitude;

    CellIdentityCdma()
    {
        this(CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE, null, null);
    }

    CellIdentityCdma(int networkId, int systemId, int basestationId, int longitude, int latitude, String alphaLong, String alphaShort)
    {
        super(TYPE_CDMA, null, null, alphaLong, alphaShort);
        mNetworkId = networkId;
        mSystemId = systemId;
        mBasestationId = basestationId;
        mLongitude = longitude;
        mLatitude = latitude;
    }

    CellIdentityCdma(Parcel source)
    {
        super(TYPE_CDMA, source);
        mNetworkId = source.readInt();
        mSystemId = source.readInt();
        mBasestationId = source.readInt();
        mLongitude = source.readInt();
        mLatitude = source.readInt();
    }

    public int getNetworkId()
    {
        return mNetworkId;
    }

    public int getSystemId()
    {
        return mSystemId;
    }

    public int getBasestationId()
    {
        return mBasestationId;
    }

    public int getLongitude()
    {
        return mLongitude;
    }

    public int getLatitude()
    {
        return mLatitude;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        super.writeToParcel(dest, flags);
        dest.writeInt(mNetworkId);
        dest.writeInt(mSystemId);
        dest.writeInt(mBasestationId);
        dest.writeInt(mLongitude);
        dest.writeInt(mLatitude);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CellIdentityCdma) || !super.equals(other)) return false;
        CellIdentityCdma identity = (CellIdentityCdma)other;
        return mNetworkId == identity.mNetworkId && mSystemId == identity.mSystemId && mBasestationId == identity.mBasestationId
            && mLongitude == identity.mLongitude && mLatitude == identity.mLatitude;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), mNetworkId, mSystemId, mBasestationId, mLongitude, mLatitude);
    }

    @Override
    public String toString()
    {
        return "CellIdentityCdma:{ mNetworkId=" + mNetworkId + " mSystemId=" + mSystemId + " mBasestationId=" + mBasestationId
            + " mLongitude=" + mLongitude + " mLatitude=" + mLatitude + " " + baseToString() + "}";
    }
}
