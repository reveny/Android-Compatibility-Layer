package android.media.metrics;

import android.os.Bundle;

public abstract class Event
{
    private final long mTimeSinceCreatedMillis;
    private final Bundle mMetricsBundle;

    Event(long timeSinceCreatedMillis, Bundle metricsBundle)
    {
        mTimeSinceCreatedMillis = timeSinceCreatedMillis;
        mMetricsBundle = metricsBundle == null ? new Bundle() : metricsBundle;
    }

    public Bundle getMetricsBundle()
    {
        return mMetricsBundle;
    }

    public long getTimeSinceCreatedMillis()
    {
        return mTimeSinceCreatedMillis;
    }
}
