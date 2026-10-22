package android.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class AttributionSource implements Parcelable
{
    public static final Parcelable.Creator<AttributionSource> CREATOR = new Parcelable.Creator<AttributionSource>()
    {
        @Override
        public AttributionSource createFromParcel(Parcel source)
        {
            int uid = source.readInt();
            int pid = source.readInt();
            String packageName = source.readString();
            String attributionTag = source.readString();
            AttributionSource next = (AttributionSource)source.readValue(null);
            return new AttributionSource(uid, pid, packageName, attributionTag, next);
        }

        @Override
        public AttributionSource[] newArray(int size)
        {
            return new AttributionSource[size];
        }
    };

    private final int mUid;
    private final int mPid;
    private final String mPackageName;
    private final String mAttributionTag;
    private final AttributionSource mNext;

    private AttributionSource(int uid, int pid, String packageName, String attributionTag, AttributionSource next)
    {
        mUid = uid;
        mPid = pid;
        mPackageName = packageName;
        mAttributionTag = attributionTag;
        mNext = next;
    }

    public static AttributionSource myAttributionSource()
    {
        return new Builder(android.os.Process.myUid()).setPid(android.os.Process.myPid())
                   .setPackageName(android.app.Application.getProcessName()).build();
    }

    public void enforceCallingUid()
    {
        if (!checkCallingUid())
        {
            throw new SecurityException("Calling uid: " + android.os.Binder.getCallingUid() + " doesn't match source uid: " + mUid);
        }
    }

    public boolean checkCallingUid()
    {
        int callingUid = android.os.Binder.getCallingUid();
        return callingUid == mUid || callingUid == android.os.Process.myUid();
    }

    public boolean isTrusted(Context context)
    {
        return mUid == android.os.Process.myUid();
    }

    public int getUid()
    {
        return mUid;
    }

    public int getPid()
    {
        return mPid;
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    public String getAttributionTag()
    {
        return mAttributionTag;
    }

    public AttributionSource getNext()
    {
        return mNext;
    }

    @Override
    public String toString()
    {
        return "AttributionSource { uid = " + mUid + ", packageName = " + mPackageName + ", attributionTag = " + mAttributionTag
            + ", next = " + mNext + " }";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof AttributionSource)) return false;

        AttributionSource compared = (AttributionSource)other;
        return mUid == compared.mUid && mPid == compared.mPid && Objects.equals(mPackageName, compared.mPackageName)
            && Objects.equals(mAttributionTag, compared.mAttributionTag) && Objects.equals(mNext, compared.mNext);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mUid, mPid, mPackageName, mAttributionTag, mNext);
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mUid);
        destination.writeInt(mPid);
        destination.writeString(mPackageName);
        destination.writeString(mAttributionTag);
        destination.writeValue(mNext);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final class Builder
    {
        private final int mUid;
        private int mPid = -1;
        private String mPackageName;
        private String mAttributionTag;
        private AttributionSource mNext;

        public Builder(int uid)
        {
            mUid = uid;
        }

        public Builder(AttributionSource current)
        {
            if (current == null) throw new IllegalArgumentException("current AttributionSource can not be null");

            mUid = current.mUid;
            mPid = current.mPid;
            mPackageName = current.mPackageName;
            mAttributionTag = current.mAttributionTag;
            mNext = current.mNext;
        }

        public Builder setPid(int pid)
        {
            mPid = pid;
            return this;
        }

        public Builder setPackageName(String packageName)
        {
            mPackageName = packageName;
            return this;
        }

        public Builder setAttributionTag(String attributionTag)
        {
            mAttributionTag = attributionTag;
            return this;
        }

        public Builder setNext(AttributionSource next)
        {
            mNext = next;
            return this;
        }

        public AttributionSource build()
        {
            return new AttributionSource(mUid, mPid, mPackageName, mAttributionTag, mNext);
        }
    }
}
