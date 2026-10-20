package android.net;

public class Network
{
    private final int mHandle;

    public Network(int handle)
    {
        mHandle = handle;
    }

    public long getNetworkHandle()
    {
        return mHandle;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof Network && ((Network)other).mHandle == mHandle;
    }

    @Override
    public int hashCode()
    {
        return mHandle;
    }

    @Override
    public String toString()
    {
        return Integer.toString(mHandle);
    }
}
