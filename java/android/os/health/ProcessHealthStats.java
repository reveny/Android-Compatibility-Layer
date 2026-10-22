package android.os.health;

public final class ProcessHealthStats
{
    public static final int MEASUREMENT_USER_TIME_MS = 30001;
    public static final int MEASUREMENT_SYSTEM_TIME_MS = 30002;
    public static final int MEASUREMENT_STARTS_COUNT = 30003;
    public static final int MEASUREMENT_CRASHES_COUNT = 30004;
    public static final int MEASUREMENT_ANR_COUNT = 30005;
    public static final int MEASUREMENT_FOREGROUND_MS = 30006;

    private ProcessHealthStats()
    {
    }
}
