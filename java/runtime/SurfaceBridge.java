package runtime;

public final class SurfaceBridge
{
    private SurfaceBridge()
    {
    }

    public static long acquireNativeWindow(int width, int height)
    {
        return nativeAcquireWindow(width, height);
    }

    private static boolean sLooperBridgeMissing;

    public static boolean serviceLooper()
    {
        if (sLooperBridgeMissing) return false;

        try
        {
            return nativeServiceLooper();
        }
        catch (UnsatisfiedLinkError missing)
        {
            sLooperBridgeMissing = true;
            return false;
        }
    }

    public static void prepareLooper()
    {
        if (sLooperBridgeMissing) return;

        try
        {
            nativePrepareLooper();
        }
        catch (UnsatisfiedLinkError missing)
        {
            sLooperBridgeMissing = true;
        }
    }

    private static native long nativeAcquireWindow(int width, int height);

    private static native boolean nativeServiceLooper();

    private static native void nativePrepareLooper();
}
