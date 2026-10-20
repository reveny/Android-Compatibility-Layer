package android.os;

public final class SystemClock
{
    private static final long BOOT_NANOS = System.nanoTime();

    private SystemClock()
    {
    }

    public static long uptimeMillis()
    {
        return (System.nanoTime() - BOOT_NANOS) / 1000000L;
    }

    public static long uptimeNanos()
    {
        return System.nanoTime() - BOOT_NANOS;
    }

    public static long elapsedRealtime()
    {
        return uptimeMillis();
    }

    public static long elapsedRealtimeNanos()
    {
        return uptimeNanos();
    }

    public static long currentThreadTimeMillis()
    {
        return uptimeMillis();
    }

    public static boolean setCurrentTimeMillis(long millis)
    {
        return false;
    }

    public static void sleep(long ms)
    {
        if (ms <= 0) return;

        long deadline = uptimeMillis() + ms;
        long remaining = ms;
        boolean interrupted = false;

        while (remaining > 0)
        {
            try
            {
                Thread.sleep(remaining);
            }
            catch (InterruptedException ignored)
            {
                interrupted = true;
            }
            remaining = deadline - uptimeMillis();
        }

        if (interrupted) Thread.currentThread().interrupt();
    }
}
