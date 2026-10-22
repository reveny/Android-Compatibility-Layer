package runtime;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.os.Process;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
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
        public final long postTime;

        Posted(String tag, int id, Notification notification, long postTime)
        {
            this.tag = tag;
            this.id = id;
            this.notification = notification;
            this.postTime = postTime;
        }
    }

    private static final Map<String, Posted> sActive = new LinkedHashMap<>();
    private static final Map<String, NotificationChannel> sChannels = new LinkedHashMap<>();
    private static final Map<String, NotificationChannelGroup> sGroups = new LinkedHashMap<>();

    private static String sDelegate;

    private NotificationStore()
    {
    }

    public static void addChannel(NotificationChannel channel)
    {
        if (channel == null || channel.getId() == null) return;

        synchronized (sChannels)
        {
            NotificationChannel existing = sChannels.get(channel.getId());
            if (existing != null)
            {
                existing.setName(channel.getName());
                existing.setDescription(channel.getDescription());
                if (channel.getImportance() < existing.getImportance()) existing.setImportance(channel.getImportance());
                if (existing.getGroup() == null) existing.setGroup(channel.getGroup());
                return;
            }

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

    public static void addGroup(NotificationChannelGroup group)
    {
        if (group == null || group.getId() == null) return;

        synchronized (sChannels)
        {
            sGroups.put(group.getId(), group);
        }

        Log.i(TAG, "channel group " + group.getId() + " (" + group.getName() + ")");
    }

    public static void removeGroup(String groupId)
    {
        if (groupId == null) return;

        synchronized (sChannels)
        {
            sGroups.remove(groupId);

            Iterator<NotificationChannel> iterator = sChannels.values().iterator();
            while (iterator.hasNext())
            {
                if (groupId.equals(iterator.next().getGroup())) iterator.remove();
            }
        }
    }

    public static NotificationChannelGroup group(String groupId)
    {
        synchronized (sChannels)
        {
            return sGroups.get(groupId);
        }
    }

    public static List<NotificationChannelGroup> groups()
    {
        synchronized (sChannels)
        {
            return new ArrayList<>(sGroups.values());
        }
    }

    public static void setDelegate(String delegate)
    {
        sDelegate = delegate;
    }

    public static String delegate()
    {
        return sDelegate;
    }

    public static void post(String tag, int id, Notification notification)
    {
        if (notification == null) return;

        String key = keyFor(tag, id);
        boolean replaced;

        synchronized (sActive)
        {
            replaced = sActive.containsKey(key);
            sActive.put(key, new Posted(tag, id, notification, System.currentTimeMillis()));
        }

        Log.i(TAG, (replaced ? "updated " : "posted ") + key + " " + notification);

        if (notification.getChannelId() != null && channel(notification.getChannelId()) == null)
        {
            Log.w(TAG, "no channel " + notification.getChannelId() + " was created for notification " + key);
        }

        NotificationChannel channel = channel(notification.getChannelId());

        nativePost(tag, id, notification.getChannelId(), asText(notification.extras.getCharSequence(Notification.EXTRA_TITLE)),
                   asText(notification.extras.getCharSequence(Notification.EXTRA_TEXT)), notification.when,
                   notification.priority, channel == null ? -1 : channel.getImportance(), notification.flags);
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

    public static Posted find(String tag, int id)
    {
        synchronized (sActive)
        {
            return sActive.get(keyFor(tag, id));
        }
    }

    public static StatusBarNotification[] statusBarNotifications()
    {
        List<Posted> posted = active();
        String packageName = packageName();
        int uid = Process.myUid();

        StatusBarNotification[] result = new StatusBarNotification[posted.size()];
        for (int index = 0; index < result.length; index++)
        {
            Posted entry = posted.get(index);
            result[index] = new StatusBarNotification(packageName, packageName, entry.id, entry.tag, uid, Process.myPid(), 0,
                                                      entry.notification.clone(), Process.myUserHandle(), entry.postTime);
        }

        return result;
    }

    private static String packageName()
    {
        Application application = Bootstrap.getApplication();
        String name = application == null ? null : application.getPackageName();
        return name == null ? "android" : name;
    }

    private static String keyFor(String tag, int id)
    {
        return tag == null ? Integer.toString(id) : tag + ":" + id;
    }
}
