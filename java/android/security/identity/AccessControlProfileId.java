package android.security.identity;

public class AccessControlProfileId
{
    private final int mId;

    public AccessControlProfileId(int id)
    {
        mId = id;
    }

    public int getId()
    {
        return mId;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof AccessControlProfileId && ((AccessControlProfileId)other).mId == mId;
    }

    @Override
    public int hashCode()
    {
        return mId;
    }
}
