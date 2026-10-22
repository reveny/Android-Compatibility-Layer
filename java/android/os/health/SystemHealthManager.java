package android.os.health;

import android.os.Process;
import android.os.SystemClock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SystemHealthManager
{
    private static final long CLOCK_TICK_MILLIS = 10L;

    public SystemHealthManager()
    {
    }

    public static SystemHealthManager from(android.content.Context context)
    {
        return (SystemHealthManager)context.getSystemService(android.content.Context.SYSTEM_HEALTH_SERVICE);
    }

    public HealthStats takeUidSnapshot(int uid)
    {
        if (uid != Process.myUid())
        {
            throw new SecurityException("uid " + Process.myUid() + " does not hold android.permission.BATTERY_STATS for uid " + uid);
        }

        return takeMyUidSnapshot();
    }

    public HealthStats takeMyUidSnapshot()
    {
        long[] cpu = cpuTimesMillis();
        long realtime = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime();
        long uptime = SystemClock.uptimeMillis() - Process.getStartUptimeMillis();

        String processName = Process.myProcessName();
        if (processName == null) processName = "";

        HealthStats process = new HealthStats.Builder("ProcessHealthStats")
                                      .measurement(ProcessHealthStats.MEASUREMENT_USER_TIME_MS, cpu[0])
                                      .measurement(ProcessHealthStats.MEASUREMENT_SYSTEM_TIME_MS, cpu[1])
                                      .measurement(ProcessHealthStats.MEASUREMENT_STARTS_COUNT, 1)
                                      .measurement(ProcessHealthStats.MEASUREMENT_CRASHES_COUNT, 0)
                                      .measurement(ProcessHealthStats.MEASUREMENT_ANR_COUNT, 0)
                                      .measurement(ProcessHealthStats.MEASUREMENT_FOREGROUND_MS, realtime)
                                      .build();

        HealthStats pid = new HealthStats.Builder("PidHealthStats")
                                  .measurement(PidHealthStats.MEASUREMENT_WAKE_NESTING_COUNT, 0)
                                  .measurement(PidHealthStats.MEASUREMENT_WAKE_SUM_MS, 0)
                                  .measurement(PidHealthStats.MEASUREMENT_WAKE_START_MS, 0)
                                  .build();

        return new HealthStats.Builder("UidHealthStats")
                .measurement(UidHealthStats.MEASUREMENT_REALTIME_BATTERY_MS, realtime)
                .measurement(UidHealthStats.MEASUREMENT_UPTIME_BATTERY_MS, uptime)
                .measurement(UidHealthStats.MEASUREMENT_REALTIME_SCREEN_OFF_BATTERY_MS, 0)
                .measurement(UidHealthStats.MEASUREMENT_UPTIME_SCREEN_OFF_BATTERY_MS, 0)
                .measurement(UidHealthStats.MEASUREMENT_USER_CPU_TIME_MS, cpu[0])
                .measurement(UidHealthStats.MEASUREMENT_SYSTEM_CPU_TIME_MS, cpu[1])
                .timer(UidHealthStats.TIMER_PROCESS_STATE_TOP_MS, 1, realtime)
                .stats(UidHealthStats.STATS_PROCESSES, processName, process)
                .stats(UidHealthStats.STATS_PIDS, Integer.toString(Process.myPid()), pid)
                .build();
    }

    public HealthStats[] takeUidSnapshots(int[] uids)
    {
        HealthStats[] result = new HealthStats[uids.length];
        for (int index = 0; index < uids.length; index++) result[index] = takeUidSnapshot(uids[index]);
        return result;
    }

    private static long[] cpuTimesMillis()
    {
        try
        {
            String stat = new String(Files.readAllBytes(Paths.get("/proc/self/stat")), StandardCharsets.US_ASCII);
            String[] fields = stat.substring(stat.lastIndexOf(')') + 2).trim().split("\\s+");
            return new long[] {Long.parseLong(fields[11]) * CLOCK_TICK_MILLIS, Long.parseLong(fields[12]) * CLOCK_TICK_MILLIS};
        }
        catch (IOException | RuntimeException unreadable)
        {
            return new long[] {Process.getElapsedCpuTime(), 0L};
        }
    }
}
