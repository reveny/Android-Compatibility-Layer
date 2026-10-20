package android.os;

public final class UserHandle implements Parcelable
{
    public static final int USER_SYSTEM = 0;

    public static final Creator<UserHandle> CREATOR = new Creator<UserHandle>()
    {
        @Override
        public UserHandle createFromParcel(Parcel source)
        {
            return new UserHandle(source == null ? USER_SYSTEM : source.readInt());
        }

        @Override
        public UserHandle[] newArray(int size)
        {
            return new UserHandle[size];
        }
    };

    private final int mIdentifier;

    public UserHandle(int identifier)
    {
        mIdentifier = identifier;
    }

    public static int myUserId()
    {
        return USER_SYSTEM;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        if (destination != null) destination.writeInt(mIdentifier);
    }

    public int getIdentifier()
    {
        return mIdentifier;
    }

    public static UserHandle getUserHandleForUid(int uid)
    {
        return new UserHandle(uid / 100000);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof UserHandle && ((UserHandle)other).mIdentifier == mIdentifier;
    }

    @Override
    public int hashCode()
    {
        return mIdentifier;
    }

    @Override
    public String toString()
    {
        return "UserHandle{" + mIdentifier + "}";
    }
}
