package android.content;

import android.os.Parcel;
import android.os.Parcelable;

public final class LocusId implements Parcelable
{
    public static final Parcelable.Creator<LocusId> CREATOR = new Parcelable.Creator<LocusId>()
    {
        @Override
        public LocusId createFromParcel(Parcel source)
        {
            return new LocusId(source.readString());
        }

        @Override
        public LocusId[] newArray(int size)
        {
            return new LocusId[size];
        }
    };

    private final String mId;

    public LocusId(String id)
    {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("id cannot be empty");

        mId = id;
    }

    public String getId()
    {
        return mId;
    }

    @Override
    public int hashCode()
    {
        return 31 + mId.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof LocusId)) return false;

        return mId.equals(((LocusId)other).mId);
    }

    @Override
    public String toString()
    {
        return "LocusId[" + mId.length() + "_chars]";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mId);
    }
}
