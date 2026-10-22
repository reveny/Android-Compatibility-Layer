package android.opengl;

public abstract class EGLObjectHandle
{
    private final long mHandle;

    @Deprecated
    protected EGLObjectHandle(int handle)
    {
        mHandle = handle;
    }

    protected EGLObjectHandle(long handle)
    {
        mHandle = handle;
    }

    @Deprecated
    public int getHandle()
    {
        if ((mHandle & 0xFFFFFFFFL) != mHandle) throw new UnsupportedOperationException();
        return (int) mHandle;
    }

    public long getNativeHandle()
    {
        return mHandle;
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + (int) (mHandle ^ (mHandle >>> 32));
        return result;
    }
}
