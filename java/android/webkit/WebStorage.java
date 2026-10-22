package android.webkit;

import java.util.HashMap;
import java.util.Map;

public class WebStorage
{
    private static final WebStorage INSTANCE = new WebStorage();

    private final Map<String, Origin> mOrigins = new HashMap<>();

    WebStorage()
    {
    }

    public static WebStorage getInstance()
    {
        return INSTANCE;
    }

    public void getOrigins(ValueCallback<Map> callback)
    {
        Map<String, Origin> origins;
        synchronized (mOrigins)
        {
            origins = new HashMap<>(mOrigins);
        }
        if (callback != null) callback.onReceiveValue(origins);
    }

    public void getUsageForOrigin(String origin, ValueCallback<Long> callback)
    {
        Origin entry;
        synchronized (mOrigins)
        {
            entry = origin == null ? null : mOrigins.get(origin);
        }
        if (callback != null) callback.onReceiveValue(entry == null ? 0L : entry.getUsage());
    }

    public void getQuotaForOrigin(String origin, ValueCallback<Long> callback)
    {
        Origin entry;
        synchronized (mOrigins)
        {
            entry = origin == null ? null : mOrigins.get(origin);
        }
        if (callback != null) callback.onReceiveValue(entry == null ? 0L : entry.getQuota());
    }

    public void setQuotaForOrigin(String origin, long quota)
    {
        if (origin == null) return;
        synchronized (mOrigins)
        {
            Origin entry = mOrigins.get(origin);
            mOrigins.put(origin, new Origin(origin, quota, entry == null ? 0 : entry.getUsage()));
        }
    }

    public void deleteOrigin(String origin)
    {
        synchronized (mOrigins)
        {
            mOrigins.remove(origin);
        }
    }

    public void deleteAllData()
    {
        synchronized (mOrigins)
        {
            mOrigins.clear();
        }
    }

    public interface QuotaUpdater
    {
        void updateQuota(long newQuota);
    }

    public static class Origin
    {
        private final String mOrigin;
        private final long mQuota;
        private final long mUsage;

        protected Origin(String origin, long quota, long usage)
        {
            mOrigin = origin;
            mQuota = quota;
            mUsage = usage;
        }

        public String getOrigin()
        {
            return mOrigin;
        }

        public long getQuota()
        {
            return mQuota;
        }

        public long getUsage()
        {
            return mUsage;
        }
    }
}
