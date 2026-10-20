package android.app;

import java.util.List;

public class NotificationManager
{
    public static final int IMPORTANCE_NONE = 0;
    public static final int IMPORTANCE_MIN = 1;
    public static final int IMPORTANCE_LOW = 2;
    public static final int IMPORTANCE_DEFAULT = 3;
    public static final int IMPORTANCE_HIGH = 4;
    public static final int IMPORTANCE_MAX = 5;

    public void notify(int id, Notification notification)
    {
        runtime.NotificationStore.post(null, id, notification);
    }

    public void notify(String tag, int id, Notification notification)
    {
        runtime.NotificationStore.post(tag, id, notification);
    }

    public void cancel(int id)
    {
        runtime.NotificationStore.cancel(null, id);
    }

    public void cancel(String tag, int id)
    {
        runtime.NotificationStore.cancel(tag, id);
    }

    public void cancelAll()
    {
        runtime.NotificationStore.cancelAll();
    }

    public void createNotificationChannel(NotificationChannel channel)
    {
        runtime.NotificationStore.addChannel(channel);
    }

    public void createNotificationChannels(List<NotificationChannel> channels)
    {
        if (channels == null) return;

        for (NotificationChannel channel : channels) runtime.NotificationStore.addChannel(channel);
    }

    public NotificationChannel getNotificationChannel(String channelId)
    {
        return runtime.NotificationStore.channel(channelId);
    }

    public List<NotificationChannel> getNotificationChannels()
    {
        return runtime.NotificationStore.channels();
    }

    public void deleteNotificationChannel(String channelId)
    {
        runtime.NotificationStore.removeChannel(channelId);
    }

    public boolean areNotificationsEnabled()
    {
        return true;
    }

    public int getImportance()
    {
        return IMPORTANCE_DEFAULT;
    }
}
