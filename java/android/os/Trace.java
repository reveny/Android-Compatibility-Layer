package android.os;

public final class Trace
{
    public static final long TRACE_TAG_APP = 1L << 12;

    private Trace()
    {
    }

    public static boolean isEnabled()
    {
        return false;
    }

    public static void beginSection(String sectionName)
    {
    }

    public static void endSection()
    {
    }

    public static void beginAsyncSection(String methodName, int cookie)
    {
    }

    public static void endAsyncSection(String methodName, int cookie)
    {
    }

    public static void setCounter(String counterName, long counterValue)
    {
    }

    public static boolean isTagEnabled(long traceTag)
    {
        return false;
    }

    public static void traceBegin(long traceTag, String methodName)
    {
    }

    public static void traceEnd(long traceTag)
    {
    }

    public static void traceCounter(long traceTag, String counterName, int counterValue)
    {
    }
}
