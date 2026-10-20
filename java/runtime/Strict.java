package runtime;

public final class Strict
{
    private Strict()
    {
    }

    public static void unimplemented(String method)
    {
        nativeUnimplemented(method);
    }

    public static boolean isTracing(String topic)
    {
        return nativeIsTracing(topic);
    }

    private static native void nativeUnimplemented(String method);

    private static native boolean nativeIsTracing(String topic);
}
