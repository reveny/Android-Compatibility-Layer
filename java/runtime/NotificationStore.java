package runtime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NotificationStore
{
    private static final String TAG = "Notifications";

    public static final class Posted
    {
        public final String tag;
        public final int id;
        public final Notification notification;

        Posted(String tag, int id, Notification notification)
        {
            this.tag = tag;
            this.id = id;
            this.notification = notification;
        }
    }

    private static final Map<String, Posted> sActive = new LinkedHashMap<>();
    private static final Map<String, NotificationChannel> sChannels = new LinkedHashMap<>();

    private NotificationStore()
    {
    }

    public static void addChannel(NotificationChannel channel)
    {
        if (channel == null || channel.getId() == null) return;

        synchronized (sChannels)
        {
            sChannels.put(channel.getId(), channel);
        }

        Log.i(TAG, "channel " + channel.getId() + " (" + channel.getName() + ") at importance " + channel.getImportance());
    }

    public static void removeChannel(String channelId)
    {
        synchronized (sChannels)
        {
            sChannels.remove(channelId);
        }
    }

    public static NotificationChannel channel(String channelId)
    {
        synchronized (sChannels)
        {
            return sChannels.get(channelId);
        }
    }

    public static List<NotificationChannel> channels()
    {
        synchronized (sChannels)
        {
            return new ArrayList<>(sChannels.values());
        }
    }

    public static void post(String tag, int id, Notification notification)
    {
        if (notification == null) return;

        String key = keyFor(tag, id);
        boolean replaced;

        synchronized (sActive)
        {
            replaced = sActive.containsKey(key);
            sActive.put(key, new Posted(tag, id, notification));
        }

        Log.i(TAG, (replaced ? "updated " : "posted ") + key + " " + notification);

        NotificationChannel channel = channel(notification.getChannelId());

        nativePost(tag, id, notification.getChannelId(), asText(notification.getTitle()), asText(notification.getText()),
                   notification.when, notification.priority, channel == null ? -1 : channel.getImportance(),
                   notification.flags);
    }

    private static String asText(CharSequence value)
    {
        return value == null ? "" : value.toString();
    }

    public static void cancel(String tag, int id)
    {
        String key = keyFor(tag, id);
        Posted removed;

        synchronized (sActive)
        {
            removed = sActive.remove(key);
        }

        if (removed != null)
        {
            Log.i(TAG, "cancelled " + key);
            nativeCancel(tag, id);
        }
    }

    public static void cancelAll()
    {
        int count;

        synchronized (sActive)
        {
            count = sActive.size();
            sActive.clear();
        }

        if (count != 0)
        {
            Log.i(TAG, "cancelled all " + count + " notification(s)");
            nativeCancelAll();
        }
    }

    private static native void nativePost(String tag, int id, String channelId, String title, String text, long whenMillis,
                                          int priority, int importance, int flags);

    private static native void nativeCancel(String tag, int id);

    private static native void nativeCancelAll();

    public static List<Posted> active()
    {
        synchronized (sActive)
        {
            return new ArrayList<>(sActive.values());
        }
    }

    private static String keyFor(String tag, int id)
    {
        return tag == null ? Integer.toString(id) : tag + ":" + id;
    }
}
