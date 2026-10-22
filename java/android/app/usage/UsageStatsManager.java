package android.app.usage;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UsageStatsManager
{
    public static final int INTERVAL_DAILY = 0;
    public static final int INTERVAL_WEEKLY = 1;
    public static final int INTERVAL_MONTHLY = 2;
    public static final int INTERVAL_YEARLY = 3;
    public static final int INTERVAL_BEST = 4;

    public static final int STANDBY_BUCKET_ACTIVE = 10;
    public static final int STANDBY_BUCKET_WORKING_SET = 20;
    public static final int STANDBY_BUCKET_FREQUENT = 30;
    public static final int STANDBY_BUCKET_RARE = 40;
    public static final int STANDBY_BUCKET_RESTRICTED = 45;

    private final Context mContext;

    public UsageStatsManager(Context context)
    {
        mContext = context;
    }

    public List<UsageStats> queryUsageStats(int intervalType, long beginTime, long endTime)
    {
        return new ArrayList<>();
    }

    public List<ConfigurationStats> queryConfigurations(int intervalType, long beginTime, long endTime)
    {
        return new ArrayList<>();
    }

    public List<EventStats> queryEventStats(int intervalType, long beginTime, long endTime)
    {
        return new ArrayList<>();
    }

    public UsageEvents queryEvents(long beginTime, long endTime)
    {
        return new UsageEvents();
    }

    public UsageEvents queryEventsForSelf(long beginTime, long endTime)
    {
        return new UsageEvents();
    }

    public Map<String, UsageStats> queryAndAggregateUsageStats(long beginTime, long endTime)
    {
        return new HashMap<>();
    }

    public boolean isAppInactive(String packageName)
    {
        return false;
    }

    public int getAppStandbyBucket()
    {
        return STANDBY_BUCKET_ACTIVE;
    }
}
