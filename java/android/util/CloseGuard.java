package android.util;

public final class CloseGuard
{
    private Throwable mCloserNameOrAllocationInfo;

    public CloseGuard()
    {
    }

    public void open(String closeMethodName)
    {
        if (closeMethodName == null) throw new NullPointerException("closeMethodName == null");

        mCloserNameOrAllocationInfo = new Throwable("Explicit termination method '" + closeMethodName + "' not called");
    }

    public void close()
    {
        mCloserNameOrAllocationInfo = null;
    }

    public void warnIfOpen()
    {
        if (mCloserNameOrAllocationInfo == null) return;

        Log.w("System", "A resource was acquired at attached stack trace but never released. "
                            + "See java.io.Closeable for information on avoiding resource leaks.",
              mCloserNameOrAllocationInfo);
    }
}
