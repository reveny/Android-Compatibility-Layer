package android.app;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotificationManager
{
    public static final String ACTION_APP_BLOCK_STATE_CHANGED = "android.app.action.APP_BLOCK_STATE_CHANGED";
    public static final String ACTION_AUTOMATIC_ZEN_RULE = "android.app.action.AUTOMATIC_ZEN_RULE";
    public static final String ACTION_AUTOMATIC_ZEN_RULE_STATUS_CHANGED = "android.app.action.AUTOMATIC_ZEN_RULE_STATUS_CHANGED";
    public static final String ACTION_CONSOLIDATED_NOTIFICATION_POLICY_CHANGED =
            "android.app.action.CONSOLIDATED_NOTIFICATION_POLICY_CHANGED";
    public static final String ACTION_INTERRUPTION_FILTER_CHANGED = "android.app.action.INTERRUPTION_FILTER_CHANGED";
    public static final String ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED =
            "android.app.action.NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED";
    public static final String ACTION_NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED =
            "android.app.action.NOTIFICATION_CHANNEL_GROUP_BLOCK_STATE_CHANGED";
    public static final String ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED =
            "android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED";
    public static final String ACTION_NOTIFICATION_POLICY_CHANGED = "android.app.action.NOTIFICATION_POLICY_CHANGED";

    public static final int AUTOMATIC_RULE_STATUS_UNKNOWN = -1;
    public static final int AUTOMATIC_RULE_STATUS_ENABLED = 1;
    public static final int AUTOMATIC_RULE_STATUS_DISABLED = 2;
    public static final int AUTOMATIC_RULE_STATUS_REMOVED = 3;
    public static final int AUTOMATIC_RULE_STATUS_ACTIVATED = 4;
    public static final int AUTOMATIC_RULE_STATUS_DEACTIVATED = 5;

    public static final int BUBBLE_PREFERENCE_NONE = 0;
    public static final int BUBBLE_PREFERENCE_ALL = 1;
    public static final int BUBBLE_PREFERENCE_SELECTED = 2;

    public static final String EXTRA_AUTOMATIC_RULE_ID = "android.app.extra.AUTOMATIC_RULE_ID";
    public static final String EXTRA_AUTOMATIC_ZEN_RULE_ID = "android.app.extra.AUTOMATIC_ZEN_RULE_ID";
    public static final String EXTRA_AUTOMATIC_ZEN_RULE_STATUS = "android.app.extra.AUTOMATIC_ZEN_RULE_STATUS";
    public static final String EXTRA_BLOCKED_STATE = "android.app.extra.BLOCKED_STATE";
    public static final String EXTRA_NOTIFICATION_CHANNEL_GROUP_ID = "android.app.extra.NOTIFICATION_CHANNEL_GROUP_ID";
    public static final String EXTRA_NOTIFICATION_CHANNEL_ID = "android.app.extra.NOTIFICATION_CHANNEL_ID";
    public static final String EXTRA_NOTIFICATION_POLICY = "android.app.extra.NOTIFICATION_POLICY";

    public static final int IMPORTANCE_UNSPECIFIED = -1000;
    public static final int IMPORTANCE_NONE = 0;
    public static final int IMPORTANCE_MIN = 1;
    public static final int IMPORTANCE_LOW = 2;
    public static final int IMPORTANCE_DEFAULT = 3;
    public static final int IMPORTANCE_HIGH = 4;
    public static final int IMPORTANCE_MAX = 5;

    public static final int INTERRUPTION_FILTER_UNKNOWN = 0;
    public static final int INTERRUPTION_FILTER_ALL = 1;
    public static final int INTERRUPTION_FILTER_PRIORITY = 2;
    public static final int INTERRUPTION_FILTER_NONE = 3;
    public static final int INTERRUPTION_FILTER_ALARMS = 4;

    public static final String META_DATA_AUTOMATIC_RULE_TYPE = "android.service.zen.automatic.ruleType";
    public static final String META_DATA_RULE_INSTANCE_LIMIT = "android.service.zen.automatic.ruleInstanceLimit";

    private static final Policy DEFAULT_POLICY =
            new Policy(Policy.PRIORITY_CATEGORY_ALARMS | Policy.PRIORITY_CATEGORY_MEDIA | Policy.PRIORITY_CATEGORY_SYSTEM
                       | Policy.PRIORITY_CATEGORY_REMINDERS | Policy.PRIORITY_CATEGORY_EVENTS
                       | Policy.PRIORITY_CATEGORY_CONVERSATIONS | Policy.PRIORITY_CATEGORY_CALLS
                       | Policy.PRIORITY_CATEGORY_REPEAT_CALLERS,
                       Policy.PRIORITY_SENDERS_STARRED, Policy.PRIORITY_SENDERS_STARRED, 0,
                       Policy.CONVERSATION_SENDERS_IMPORTANT);

    NotificationManager()
    {
    }

    public void notify(int id, Notification notification)
    {
        notify(null, id, notification);
    }

    public void notify(String tag, int id, Notification notification)
    {
        if (notification == null) throw new IllegalArgumentException("notification must not be null");

        runtime.NotificationStore.post(tag, id, notification);
    }

    public void notifyAsPackage(String targetPackage, String tag, int id, Notification notification)
    {
        if (!canNotifyAsPackage(targetPackage))
        {
            throw new SecurityException("this application is not the notification delegate of " + targetPackage);
        }

        notify(tag, id, notification);
    }

    public boolean canNotifyAsPackage(String pkg)
    {
        return pkg != null && pkg.equals(ownPackage());
    }

    private static String ownPackage()
    {
        Application application = runtime.Bootstrap.getApplication();
        return application == null ? null : application.getPackageName();
    }

    public void cancel(int id)
    {
        cancel(null, id);
    }

    public void cancel(String tag, int id)
    {
        runtime.NotificationStore.cancel(tag, id);
    }

    public void cancelAsPackage(String targetPackage, String tag, int id)
    {
        if (!canNotifyAsPackage(targetPackage))
        {
            throw new SecurityException("this application is not the notification delegate of " + targetPackage);
        }

        cancel(tag, id);
    }

    public void cancelAll()
    {
        runtime.NotificationStore.cancelAll();
    }

    public void setNotificationDelegate(String delegate)
    {
        runtime.NotificationStore.setDelegate(delegate);
    }

    public String getNotificationDelegate()
    {
        return runtime.NotificationStore.delegate();
    }

    public StatusBarNotification[] getActiveNotifications()
    {
        return runtime.NotificationStore.statusBarNotifications();
    }

    public void createNotificationChannelGroup(NotificationChannelGroup group)
    {
        if (group == null) return;

        runtime.NotificationStore.addGroup(group);
    }

    public void createNotificationChannelGroups(List<NotificationChannelGroup> groups)
    {
        if (groups == null) return;

        for (NotificationChannelGroup group : groups) createNotificationChannelGroup(group);
    }

    public void createNotificationChannel(NotificationChannel channel)
    {
        if (channel == null) return;

        checkChannelGroup(channel);
        runtime.NotificationStore.addChannel(channel);
    }

    public void createNotificationChannels(List<NotificationChannel> channels)
    {
        if (channels == null) return;

        for (NotificationChannel channel : channels) createNotificationChannel(channel);
    }

    private static void checkChannelGroup(NotificationChannel channel)
    {
        String group = channel.getGroup();
        if (group != null && runtime.NotificationStore.group(group) == null)
        {
            throw new IllegalArgumentException("NotificationChannelGroup doesn't exist");
        }
    }

    public NotificationChannel getNotificationChannel(String channelId)
    {
        return runtime.NotificationStore.channel(channelId);
    }

    public NotificationChannel getNotificationChannel(String channelId, String conversationId)
    {
        if (conversationId != null)
        {
            for (NotificationChannel channel : runtime.NotificationStore.channels())
            {
                if (Objects.equals(channelId, channel.getParentChannelId())
                    && conversationId.equals(channel.getConversationId()))
                {
                    return channel;
                }
            }
        }

        return runtime.NotificationStore.channel(channelId);
    }

    public List<NotificationChannel> getNotificationChannels()
    {
        return runtime.NotificationStore.channels();
    }

    public void deleteNotificationChannel(String channelId)
    {
        if (NotificationChannel.DEFAULT_CHANNEL_ID.equals(channelId))
        {
            throw new IllegalArgumentException("Cannot delete default channel");
        }

        runtime.NotificationStore.removeChannel(channelId);
    }

    public NotificationChannelGroup getNotificationChannelGroup(String channelGroupId)
    {
        NotificationChannelGroup stored = runtime.NotificationStore.group(channelGroupId);
        return stored == null ? null : populated(stored);
    }

    public List<NotificationChannelGroup> getNotificationChannelGroups()
    {
        List<NotificationChannelGroup> result = new ArrayList<>();
        for (NotificationChannelGroup group : runtime.NotificationStore.groups()) result.add(populated(group));
        return result;
    }

    private static NotificationChannelGroup populated(NotificationChannelGroup stored)
    {
        NotificationChannelGroup group = stored.clone();
        List<NotificationChannel> channels = new ArrayList<>();

        for (NotificationChannel channel : runtime.NotificationStore.channels())
        {
            if (Objects.equals(stored.getId(), channel.getGroup())) channels.add(channel);
        }

        group.setChannels(channels);
        return group;
    }

    public void deleteNotificationChannelGroup(String groupId)
    {
        runtime.NotificationStore.removeGroup(groupId);
    }

    public boolean areNotificationsEnabled()
    {
        return true;
    }

    public boolean areNotificationsPaused()
    {
        return false;
    }

    public boolean areBubblesAllowed()
    {
        return false;
    }

    public boolean areBubblesEnabled()
    {
        return false;
    }

    public int getBubblePreference()
    {
        return BUBBLE_PREFERENCE_NONE;
    }

    public boolean canUseFullScreenIntent()
    {
        return true;
    }

    public boolean canPostPromotedNotifications()
    {
        return false;
    }

    public boolean shouldHideSilentStatusBarIcons()
    {
        return false;
    }

    public int getImportance()
    {
        return IMPORTANCE_DEFAULT;
    }

    public boolean isNotificationListenerAccessGranted(ComponentName listener)
    {
        return false;
    }

    public boolean isNotificationPolicyAccessGranted()
    {
        return false;
    }

    public boolean areAutomaticZenRulesUserManaged()
    {
        return false;
    }

    public boolean matchesCallFilter(Uri uri)
    {
        return true;
    }

    public final int getCurrentInterruptionFilter()
    {
        return INTERRUPTION_FILTER_ALL;
    }

    public final void setInterruptionFilter(int interruptionFilter)
    {
        throw new SecurityException("Notification policy access denied");
    }

    public Policy getNotificationPolicy()
    {
        return DEFAULT_POLICY;
    }

    public Policy getConsolidatedNotificationPolicy()
    {
        return DEFAULT_POLICY;
    }

    public void setNotificationPolicy(Policy policy)
    {
        throw new SecurityException("Notification policy access denied");
    }

    public static class Policy implements Parcelable
    {
        public static final int PRIORITY_CATEGORY_REMINDERS = 1 << 0;
        public static final int PRIORITY_CATEGORY_EVENTS = 1 << 1;
        public static final int PRIORITY_CATEGORY_MESSAGES = 1 << 2;
        public static final int PRIORITY_CATEGORY_CALLS = 1 << 3;
        public static final int PRIORITY_CATEGORY_REPEAT_CALLERS = 1 << 4;
        public static final int PRIORITY_CATEGORY_ALARMS = 1 << 5;
        public static final int PRIORITY_CATEGORY_MEDIA = 1 << 6;
        public static final int PRIORITY_CATEGORY_SYSTEM = 1 << 7;
        public static final int PRIORITY_CATEGORY_CONVERSATIONS = 1 << 8;

        public static final int PRIORITY_SENDERS_ANY = 0;
        public static final int PRIORITY_SENDERS_CONTACTS = 1;
        public static final int PRIORITY_SENDERS_STARRED = 2;

        public static final int CONVERSATION_SENDERS_ANYONE = 1;
        public static final int CONVERSATION_SENDERS_IMPORTANT = 2;
        public static final int CONVERSATION_SENDERS_NONE = 3;

        public static final int SUPPRESSED_EFFECT_SCREEN_OFF = 1 << 0;
        public static final int SUPPRESSED_EFFECT_SCREEN_ON = 1 << 1;
        public static final int SUPPRESSED_EFFECT_FULL_SCREEN_INTENT = 1 << 2;
        public static final int SUPPRESSED_EFFECT_LIGHTS = 1 << 3;
        public static final int SUPPRESSED_EFFECT_PEEK = 1 << 4;
        public static final int SUPPRESSED_EFFECT_STATUS_BAR = 1 << 5;
        public static final int SUPPRESSED_EFFECT_BADGE = 1 << 6;
        public static final int SUPPRESSED_EFFECT_AMBIENT = 1 << 7;
        public static final int SUPPRESSED_EFFECT_NOTIFICATION_LIST = 1 << 8;

        private static final int SUPPRESSED_EFFECTS_UNSET = -1;
        private static final int CONVERSATION_SENDERS_UNSET = -1;

        public static final Parcelable.Creator<Policy> CREATOR = new Parcelable.Creator<Policy>()
        {
            @Override
            public Policy createFromParcel(Parcel source)
            {
                return new Policy(source.readInt(), source.readInt(), source.readInt(), source.readInt(), source.readInt());
            }

            @Override
            public Policy[] newArray(int size)
            {
                return new Policy[size];
            }
        };

        public final int priorityCategories;
        public final int priorityCallSenders;
        public final int priorityMessageSenders;
        public final int priorityConversationSenders;
        public final int suppressedVisualEffects;

        public Policy(int priorityCategories, int priorityCallSenders, int priorityMessageSenders)
        {
            this(priorityCategories, priorityCallSenders, priorityMessageSenders, SUPPRESSED_EFFECTS_UNSET,
                 CONVERSATION_SENDERS_UNSET);
        }

        public Policy(int priorityCategories, int priorityCallSenders, int priorityMessageSenders, int suppressedVisualEffects)
        {
            this(priorityCategories, priorityCallSenders, priorityMessageSenders, suppressedVisualEffects,
                 CONVERSATION_SENDERS_UNSET);
        }

        public Policy(int priorityCategories, int priorityCallSenders, int priorityMessageSenders, int suppressedVisualEffects,
                      int priorityConversationSenders)
        {
            this.priorityCategories = priorityCategories;
            this.priorityCallSenders = priorityCallSenders;
            this.priorityMessageSenders = priorityMessageSenders;
            this.suppressedVisualEffects = suppressedVisualEffects;
            this.priorityConversationSenders = priorityConversationSenders;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags)
        {
            parcel.writeInt(priorityCategories);
            parcel.writeInt(priorityCallSenders);
            parcel.writeInt(priorityMessageSenders);
            parcel.writeInt(suppressedVisualEffects);
            parcel.writeInt(priorityConversationSenders);
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof Policy)) return false;

            Policy that = (Policy)other;
            return priorityCategories == that.priorityCategories && priorityCallSenders == that.priorityCallSenders
                   && priorityMessageSenders == that.priorityMessageSenders
                   && suppressedVisualEffects == that.suppressedVisualEffects
                   && priorityConversationSenders == that.priorityConversationSenders;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(priorityCategories, priorityCallSenders, priorityMessageSenders, suppressedVisualEffects,
                                priorityConversationSenders);
        }

        @Override
        public String toString()
        {
            return "NotificationManager.Policy[priorityCategories=" + priorityCategoriesToString(priorityCategories)
                   + ",priorityCallSenders=" + prioritySendersToString(priorityCallSenders) + ",priorityMessageSenders="
                   + prioritySendersToString(priorityMessageSenders) + ",priorityConvSenders="
                   + conversationSendersToString(priorityConversationSenders) + ",suppressedVisualEffects="
                   + suppressedEffectsToString(suppressedVisualEffects) + "]";
        }

        private static final int[] ALL_PRIORITY_CATEGORIES = new int[] {
                PRIORITY_CATEGORY_ALARMS, PRIORITY_CATEGORY_MEDIA, PRIORITY_CATEGORY_SYSTEM, PRIORITY_CATEGORY_REMINDERS,
                PRIORITY_CATEGORY_EVENTS, PRIORITY_CATEGORY_MESSAGES, PRIORITY_CATEGORY_CALLS,
                PRIORITY_CATEGORY_REPEAT_CALLERS, PRIORITY_CATEGORY_CONVERSATIONS
        };

        private static final int[] ALL_SUPPRESSED_EFFECTS = new int[] {
                SUPPRESSED_EFFECT_SCREEN_OFF, SUPPRESSED_EFFECT_SCREEN_ON, SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
                SUPPRESSED_EFFECT_LIGHTS, SUPPRESSED_EFFECT_PEEK, SUPPRESSED_EFFECT_STATUS_BAR, SUPPRESSED_EFFECT_BADGE,
                SUPPRESSED_EFFECT_AMBIENT, SUPPRESSED_EFFECT_NOTIFICATION_LIST
        };

        public static String suppressedEffectsToString(int effects)
        {
            if (effects <= 0) return "";

            StringBuilder builder = new StringBuilder();
            for (int effect : ALL_SUPPRESSED_EFFECTS)
            {
                if ((effects & effect) == 0) continue;

                if (builder.length() > 0) builder.append(',');
                builder.append(effectToString(effect));
                effects &= ~effect;
            }

            if (effects != 0)
            {
                if (builder.length() > 0) builder.append(',');
                builder.append("UNKNOWN_").append(effects);
            }

            return builder.toString();
        }

        public static String priorityCategoriesToString(int priorityCategories)
        {
            if (priorityCategories == 0) return "";

            StringBuilder builder = new StringBuilder();
            for (int category : ALL_PRIORITY_CATEGORIES)
            {
                if ((priorityCategories & category) == 0) continue;

                if (builder.length() > 0) builder.append(',');
                builder.append(priorityCategoryToString(category));
                priorityCategories &= ~category;
            }

            if (priorityCategories != 0)
            {
                if (builder.length() > 0) builder.append(',');
                builder.append("PRIORITY_CATEGORY_UNKNOWN_").append(priorityCategories);
            }

            return builder.toString();
        }

        private static String effectToString(int effect)
        {
            switch (effect)
            {
                case SUPPRESSED_EFFECT_SCREEN_OFF: return "SUPPRESSED_EFFECT_SCREEN_OFF";
                case SUPPRESSED_EFFECT_SCREEN_ON: return "SUPPRESSED_EFFECT_SCREEN_ON";
                case SUPPRESSED_EFFECT_FULL_SCREEN_INTENT: return "SUPPRESSED_EFFECT_FULL_SCREEN_INTENT";
                case SUPPRESSED_EFFECT_LIGHTS: return "SUPPRESSED_EFFECT_LIGHTS";
                case SUPPRESSED_EFFECT_PEEK: return "SUPPRESSED_EFFECT_PEEK";
                case SUPPRESSED_EFFECT_STATUS_BAR: return "SUPPRESSED_EFFECT_STATUS_BAR";
                case SUPPRESSED_EFFECT_BADGE: return "SUPPRESSED_EFFECT_BADGE";
                case SUPPRESSED_EFFECT_AMBIENT: return "SUPPRESSED_EFFECT_AMBIENT";
                case SUPPRESSED_EFFECT_NOTIFICATION_LIST: return "SUPPRESSED_EFFECT_NOTIFICATION_LIST";
                default: return "UNKNOWN_" + effect;
            }
        }

        private static String priorityCategoryToString(int priorityCategory)
        {
            switch (priorityCategory)
            {
                case PRIORITY_CATEGORY_REMINDERS: return "PRIORITY_CATEGORY_REMINDERS";
                case PRIORITY_CATEGORY_EVENTS: return "PRIORITY_CATEGORY_EVENTS";
                case PRIORITY_CATEGORY_MESSAGES: return "PRIORITY_CATEGORY_MESSAGES";
                case PRIORITY_CATEGORY_CALLS: return "PRIORITY_CATEGORY_CALLS";
                case PRIORITY_CATEGORY_REPEAT_CALLERS: return "PRIORITY_CATEGORY_REPEAT_CALLERS";
                case PRIORITY_CATEGORY_ALARMS: return "PRIORITY_CATEGORY_ALARMS";
                case PRIORITY_CATEGORY_MEDIA: return "PRIORITY_CATEGORY_MEDIA";
                case PRIORITY_CATEGORY_SYSTEM: return "PRIORITY_CATEGORY_SYSTEM";
                case PRIORITY_CATEGORY_CONVERSATIONS: return "PRIORITY_CATEGORY_CONVERSATIONS";
                default: return "PRIORITY_CATEGORY_UNKNOWN_" + priorityCategory;
            }
        }

        public static String prioritySendersToString(int prioritySenders)
        {
            switch (prioritySenders)
            {
                case PRIORITY_SENDERS_ANY: return "PRIORITY_SENDERS_ANY";
                case PRIORITY_SENDERS_CONTACTS: return "PRIORITY_SENDERS_CONTACTS";
                case PRIORITY_SENDERS_STARRED: return "PRIORITY_SENDERS_STARRED";
                default: return "PRIORITY_SENDERS_UNKNOWN_" + prioritySenders;
            }
        }

        private static String conversationSendersToString(int senders)
        {
            switch (senders)
            {
                case CONVERSATION_SENDERS_ANYONE: return "anyone";
                case CONVERSATION_SENDERS_IMPORTANT: return "important";
                case CONVERSATION_SENDERS_NONE: return "none";
                case CONVERSATION_SENDERS_UNSET: return "unset";
                default: return "invalidConversationType{" + senders + "}";
            }
        }
    }
}
