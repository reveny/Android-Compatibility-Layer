package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class ClosedSubscriberGroupInfo implements Parcelable
{
    public static final Parcelable.Creator<ClosedSubscriberGroupInfo> CREATOR = new Parcelable.Creator<ClosedSubscriberGroupInfo>()
    {
        @Override
        public ClosedSubscriberGroupInfo createFromParcel(Parcel source)
        {
            return new ClosedSubscriberGroupInfo(source.readBoolean(), source.readString(), source.readInt());
        }

        @Override
        public ClosedSubscriberGroupInfo[] newArray(int size)
        {
            return new ClosedSubscriberGroupInfo[size];
        }
    };

    private final boolean mCsgIndicator;
    private final String mHomeNodebName;
    private final int mCsgIdentity;

    ClosedSubscriberGroupInfo(boolean csgIndicator, String homeNodebName, int csgIdentity)
    {
        mCsgIndicator = csgIndicator;
        mHomeNodebName = homeNodebName == null ? "" : homeNodebName;
        mCsgIdentity = csgIdentity;
    }

    public boolean getCsgIndicator()
    {
        return mCsgIndicator;
    }

    public String getHomeNodebName()
    {
        return mHomeNodebName;
    }

    public int getCsgIdentity()
    {
        return mCsgIdentity;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeBoolean(mCsgIndicator);
        dest.writeString(mHomeNodebName);
        dest.writeInt(mCsgIdentity);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ClosedSubscriberGroupInfo)) return false;
        ClosedSubscriberGroupInfo info = (ClosedSubscriberGroupInfo)other;
        return mCsgIndicator == info.mCsgIndicator && mCsgIdentity == info.mCsgIdentity && mHomeNodebName.equals(info.mHomeNodebName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mCsgIndicator, mHomeNodebName, mCsgIdentity);
    }

    @Override
    public String toString()
    {
        return "ClosedSubscriberGroupInfo:{ mCsgIndicator = " + mCsgIndicator + " mHomeNodebName = " + mHomeNodebName
            + " mCsgIdentity = " + mCsgIdentity + "}";
    }
}
