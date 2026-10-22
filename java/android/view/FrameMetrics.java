package android.view;

public final class FrameMetrics
{
    public static final int UNKNOWN_DELAY_DURATION = 0;
    public static final int INPUT_HANDLING_DURATION = 1;
    public static final int ANIMATION_DURATION = 2;
    public static final int LAYOUT_MEASURE_DURATION = 3;
    public static final int DRAW_DURATION = 4;
    public static final int SYNC_DURATION = 5;
    public static final int COMMAND_ISSUE_DURATION = 6;
    public static final int SWAP_BUFFERS_DURATION = 7;
    public static final int TOTAL_DURATION = 8;
    public static final int FIRST_DRAW_FRAME = 9;
    public static final int INTENDED_VSYNC_TIMESTAMP = 10;
    public static final int VSYNC_TIMESTAMP = 11;
    public static final int GPU_DURATION = 12;
    public static final int DEADLINE = 13;
    public static final int FRAME_TIMELINE_VSYNC_ID = 14;

    private static final int METRIC_COUNT = 15;

    private static long sNextVsyncId;

    private final long[] mMetrics = new long[METRIC_COUNT];

    FrameMetrics()
    {
    }

    public FrameMetrics(FrameMetrics other)
    {
        System.arraycopy(other.mMetrics, 0, mMetrics, 0, METRIC_COUNT);
    }

    static FrameMetrics forFrame(long vsyncNanos, long startNanos, long endNanos, boolean firstFrame)
    {
        FrameMetrics metrics = new FrameMetrics();
        long now = android.os.SystemClock.uptimeNanos();
        long drawNanos = Math.max(0, endNanos - startNanos);
        long delay = Math.max(0, now - drawNanos - vsyncNanos);
        long interval = (long)(1000000000L / Math.max(1.0f, runtime.WindowBridge.refreshRate()));

        metrics.setMetric(UNKNOWN_DELAY_DURATION, delay);
        metrics.setMetric(LAYOUT_MEASURE_DURATION, 0);
        metrics.setMetric(DRAW_DURATION, drawNanos);
        metrics.setMetric(TOTAL_DURATION, delay + drawNanos);
        metrics.setMetric(FIRST_DRAW_FRAME, firstFrame ? 1 : 0);
        metrics.setMetric(INTENDED_VSYNC_TIMESTAMP, vsyncNanos);
        metrics.setMetric(VSYNC_TIMESTAMP, vsyncNanos);
        metrics.setMetric(DEADLINE, interval);

        synchronized (FrameMetrics.class)
        {
            metrics.setMetric(FRAME_TIMELINE_VSYNC_ID, ++sNextVsyncId);
        }

        return metrics;
    }

    void setMetric(int id, long value)
    {
        if (id >= 0 && id < METRIC_COUNT) mMetrics[id] = value;
    }

    public long getMetric(int id)
    {
        if (id < 0 || id >= METRIC_COUNT) return -1;
        return mMetrics[id];
    }
}
