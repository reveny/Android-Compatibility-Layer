package android.app;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class AsyncNotedAppOp implements Parcelable
{
    public static final Parcelable.Creator<AsyncNotedAppOp> CREATOR = new Parcelable.Creator<AsyncNotedAppOp>()
    {
        @Override
        public AsyncNotedAppOp createFromParcel(Parcel source)
        {
            return new AsyncNotedAppOp(source.readString(), source.readInt(), source.readString(), source.readString(), source.readLong());
        }

        @Override
        public AsyncNotedAppOp[] newArray(int size)
        {
            return new AsyncNotedAppOp[size];
        }
    };

    private final String mOp;
    private final int mNotingUid;
    private final String mAttributionTag;
    private final String mMessage;
    private final long mTime;

    AsyncNotedAppOp(String op, int notingUid, String attributionTag, String message, long time)
    {
        mOp = op;
        mNotingUid = notingUid;
        mAttributionTag = attributionTag;
        mMessage = message;
        mTime = time;
    }

    public String getOp()
    {
        return mOp;
    }

    public int getNotingUid()
    {
        return mNotingUid;
    }

    public String getAttributionTag()
    {
        return mAttributionTag;
    }

    public String getMessage()
    {
        return mMessage;
    }

    public long getTime()
    {
        return mTime;
    }

    @Override
    public String toString()
    {
        return "AsyncNotedAppOp { op = " + mOp + ", notingUid = " + mNotingUid + ", attributionTag = " + mAttributionTag
               + ", message = " + mMessage + ", time = " + mTime + " }";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof AsyncNotedAppOp)) return false;

        AsyncNotedAppOp that = (AsyncNotedAppOp)other;
        return Objects.equals(mOp, that.mOp) && mNotingUid == that.mNotingUid && Objects.equals(mAttributionTag, that.mAttributionTag)
               && Objects.equals(mMessage, that.mMessage) && mTime == that.mTime;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mOp, mNotingUid, mAttributionTag, mMessage, mTime);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mOp);
        dest.writeInt(mNotingUid);
        dest.writeString(mAttributionTag);
        dest.writeString(mMessage);
        dest.writeLong(mTime);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }
}
