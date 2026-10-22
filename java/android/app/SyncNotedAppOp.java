package android.app;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class SyncNotedAppOp implements Parcelable
{
    public static final Parcelable.Creator<SyncNotedAppOp> CREATOR = new Parcelable.Creator<SyncNotedAppOp>()
    {
        @Override
        public SyncNotedAppOp createFromParcel(Parcel source)
        {
            return new SyncNotedAppOp(source.readString(), source.readString());
        }

        @Override
        public SyncNotedAppOp[] newArray(int size)
        {
            return new SyncNotedAppOp[size];
        }
    };

    private final String mOp;
    private final String mAttributionTag;

    public SyncNotedAppOp(int opMode, String attributionTag)
    {
        this(String.valueOf(opMode), attributionTag);
    }

    SyncNotedAppOp(String op, String attributionTag)
    {
        mOp = op;
        mAttributionTag = attributionTag;
    }

    public String getOp()
    {
        return mOp;
    }

    public String getAttributionTag()
    {
        return mAttributionTag;
    }

    @Override
    public String toString()
    {
        return "SyncNotedAppOp { op = " + mOp + ", attributionTag = " + mAttributionTag + " }";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof SyncNotedAppOp)) return false;

        SyncNotedAppOp that = (SyncNotedAppOp)other;
        return Objects.equals(mOp, that.mOp) && Objects.equals(mAttributionTag, that.mAttributionTag);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mOp, mAttributionTag);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mOp);
        dest.writeString(mAttributionTag);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }
}
