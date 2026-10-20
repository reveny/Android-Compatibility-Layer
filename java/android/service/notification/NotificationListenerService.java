package android.service.notification;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public abstract class NotificationListenerService extends Service
{
    public static final String SERVICE_INTERFACE = "android.service.notification.NotificationListenerService";

    public static final int INTERRUPTION_FILTER_ALL = 1;
    public static final int INTERRUPTION_FILTER_PRIORITY = 2;
    public static final int INTERRUPTION_FILTER_NONE = 3;
    public static final int INTERRUPTION_FILTER_ALARMS = 4;
    public static final int INTERRUPTION_FILTER_UNKNOWN = 0;

    public static final int HINT_HOST_DISABLE_EFFECTS = 1;
    public static final int HINT_HOST_DISABLE_NOTIFICATION_EFFECTS = 1 << 1;
    public static final int HINT_HOST_DISABLE_CALL_EFFECTS = 1 << 2;

    public static final int REASON_CLICK = 1;
    public static final int REASON_CANCEL = 2;
    public static final int REASON_CANCEL_ALL = 3;
    public static final int REASON_LISTENER_CANCEL = 10;
    public static final int REASON_LISTENER_CANCEL_ALL = 11;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public void onListenerConnected()
    {
    }

    public void onListenerDisconnected()
    {
    }

    public void onNotificationPosted(StatusBarNotification notification)
    {
    }

    public void onNotificationPosted(StatusBarNotification notification, RankingMap rankingMap)
    {
        onNotificationPosted(notification);
    }

    public void onNotificationRemoved(StatusBarNotification notification)
    {
    }

    public void onNotificationRemoved(StatusBarNotification notification, RankingMap rankingMap)
    {
        onNotificationRemoved(notification);
    }

    public void onNotificationRemoved(StatusBarNotification notification, RankingMap rankingMap, int reason)
    {
        onNotificationRemoved(notification, rankingMap);
    }

    public void onNotificationRankingUpdate(RankingMap rankingMap)
    {
    }

    public void onListenerHintsChanged(int hints)
    {
    }

    public void onInterruptionFilterChanged(int interruptionFilter)
    {
    }

    public StatusBarNotification[] getActiveNotifications()
    {
        return new StatusBarNotification[0];
    }

    public StatusBarNotification[] getActiveNotifications(String[] keys)
    {
        return new StatusBarNotification[0];
    }

    public RankingMap getCurrentRanking()
    {
        return new RankingMap();
    }

    public void cancelNotification(String key)
    {
    }

    public void cancelAllNotifications()
    {
    }

    public void requestUnbind()
    {
    }

    public static void requestRebind(ComponentName componentName)
    {
    }

    public int getCurrentListenerHints()
    {
        return 0;
    }

    public int getCurrentInterruptionFilter()
    {
        return INTERRUPTION_FILTER_ALL;
    }

    public static class Ranking
    {
        public String getKey()
        {
            return null;
        }

        public int getRank()
        {
            return 0;
        }

        public boolean isAmbient()
        {
            return false;
        }

        public boolean canShowBadge()
        {
            return true;
        }

        public int getImportance()
        {
            return 3;
        }

        public String getOverrideGroupKey()
        {
            return null;
        }

        public boolean matchesInterruptionFilter()
        {
            return true;
        }
    }

    public static class RankingMap
    {
        public String[] getOrderedKeys()
        {
            return new String[0];
        }

        public boolean getRanking(String key, Ranking outRanking)
        {
            return false;
        }
    }
}
