package android.app.usage;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

public class NetworkStatsManager
{
    private final Context mContext;
    private final List<UsageCallback> mCallbacks = new ArrayList<>();

    public NetworkStatsManager(Context context)
    {
        mContext = context;
    }

    public NetworkStats.Bucket querySummaryForDevice(int networkType, String subscriberId, long startTime, long endTime)
        throws SecurityException, RemoteException
    {
        return NetworkStats.Bucket.empty(NetworkStats.Bucket.UID_ALL, startTime, endTime);
    }

    public NetworkStats.Bucket querySummaryForUser(int networkType, String subscriberId, long startTime, long endTime)
        throws SecurityException, RemoteException
    {
        return NetworkStats.Bucket.empty(NetworkStats.Bucket.UID_ALL, startTime, endTime);
    }

    public NetworkStats querySummary(int networkType, String subscriberId, long startTime, long endTime)
        throws SecurityException, RemoteException
    {
        return new NetworkStats();
    }

    public NetworkStats queryDetailsForUid(int networkType, String subscriberId, long startTime, long endTime, int uid)
        throws SecurityException
    {
        return new NetworkStats();
    }

    public NetworkStats queryDetailsForUidTag(int networkType, String subscriberId, long startTime, long endTime, int uid, int tag)
        throws SecurityException
    {
        return new NetworkStats();
    }

    public NetworkStats queryDetailsForUidTagState(int networkType, String subscriberId, long startTime, long endTime, int uid, int tag,
                                                   int state) throws SecurityException
    {
        return new NetworkStats();
    }

    public NetworkStats queryDetails(int networkType, String subscriberId, long startTime, long endTime)
        throws SecurityException, RemoteException
    {
        return new NetworkStats();
    }

    public void registerUsageCallback(int networkType, String subscriberId, long thresholdBytes, UsageCallback callback)
    {
        registerUsageCallback(networkType, subscriberId, thresholdBytes, callback, null);
    }

    public void registerUsageCallback(int networkType, String subscriberId, long thresholdBytes, UsageCallback callback, Handler handler)
    {
        if (callback == null) throw new NullPointerException("UsageCallback cannot be null");

        synchronized (mCallbacks)
        {
            if (!mCallbacks.contains(callback)) mCallbacks.add(callback);
        }
    }

    public void unregisterUsageCallback(UsageCallback callback)
    {
        synchronized (mCallbacks)
        {
            mCallbacks.remove(callback);
        }
    }

    public abstract static class UsageCallback
    {
        public UsageCallback()
        {
        }

        public abstract void onThresholdReached(int networkType, String subscriberId);
    }
}
