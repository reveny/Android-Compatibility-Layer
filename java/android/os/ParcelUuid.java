package android.os;

import java.util.UUID;

public final class ParcelUuid implements Parcelable
{
    public static final Parcelable.Creator<ParcelUuid> CREATOR = new Parcelable.Creator<ParcelUuid>()
    {
        @Override
        public ParcelUuid createFromParcel(Parcel source)
        {
            long mostSignificantBits = source.readLong();
            long leastSignificantBits = source.readLong();
            return new ParcelUuid(new UUID(mostSignificantBits, leastSignificantBits));
        }

        @Override
        public ParcelUuid[] newArray(int size)
        {
            return new ParcelUuid[size];
        }
    };

    private final UUID mUuid;

    public ParcelUuid(UUID uuid)
    {
        mUuid = uuid;
    }

    public static ParcelUuid fromString(String uuid)
    {
        return new ParcelUuid(UUID.fromString(uuid));
    }

    public UUID getUuid()
    {
        return mUuid;
    }

    @Override
    public String toString()
    {
        return mUuid.toString();
    }

    @Override
    public int hashCode()
    {
        return mUuid.hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) return false;
        if (this == object) return true;
        if (!(object instanceof ParcelUuid)) return false;

        return mUuid.equals(((ParcelUuid)object).mUuid);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(mUuid.getMostSignificantBits());
        dest.writeLong(mUuid.getLeastSignificantBits());
    }
}
