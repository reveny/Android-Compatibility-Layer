package android.app;

import android.content.Context;
import android.content.LocusId;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Notification implements Parcelable
{
    public static final AudioAttributes AUDIO_ATTRIBUTES_DEFAULT =
            new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

    public static final int BADGE_ICON_NONE = 0;
    public static final int BADGE_ICON_SMALL = 1;
    public static final int BADGE_ICON_LARGE = 2;

    public static final String CATEGORY_ALARM = "alarm";
    public static final String CATEGORY_CALL = "call";
    public static final String CATEGORY_EMAIL = "email";
    public static final String CATEGORY_ERROR = "err";
    public static final String CATEGORY_EVENT = "event";
    public static final String CATEGORY_LOCATION_SHARING = "location_sharing";
    public static final String CATEGORY_MESSAGE = "msg";
    public static final String CATEGORY_MISSED_CALL = "missed_call";
    public static final String CATEGORY_NAVIGATION = "navigation";
    public static final String CATEGORY_PROGRESS = "progress";
    public static final String CATEGORY_PROMO = "promo";
    public static final String CATEGORY_RECOMMENDATION = "recommendation";
    public static final String CATEGORY_REMINDER = "reminder";
    public static final String CATEGORY_SERVICE = "service";
    public static final String CATEGORY_SOCIAL = "social";
    public static final String CATEGORY_STATUS = "status";
    public static final String CATEGORY_STOPWATCH = "stopwatch";
    public static final String CATEGORY_SYSTEM = "sys";
    public static final String CATEGORY_TRANSPORT = "transport";
    public static final String CATEGORY_VOICEMAIL = "voicemail";
    public static final String CATEGORY_WORKOUT = "workout";

    public static final int COLOR_DEFAULT = 0;

    public static final int DEFAULT_ALL = ~0;
    public static final int DEFAULT_SOUND = 1;
    public static final int DEFAULT_VIBRATE = 2;
    public static final int DEFAULT_LIGHTS = 4;

    public static final String EXTRA_ANSWER_COLOR = "android.answerColor";
    public static final String EXTRA_ANSWER_INTENT = "android.answerIntent";
    public static final String EXTRA_AUDIO_CONTENTS_URI = "android.audioContents";
    public static final String EXTRA_BACKGROUND_IMAGE_URI = "android.backgroundImageUri";
    public static final String EXTRA_BIG_TEXT = "android.bigText";
    public static final String EXTRA_CALL_IS_VIDEO = "android.callIsVideo";
    public static final String EXTRA_CALL_PERSON = "android.callPerson";
    public static final String EXTRA_CALL_TYPE = "android.callType";
    public static final String EXTRA_CHANNEL_GROUP_ID = "android.intent.extra.CHANNEL_GROUP_ID";
    public static final String EXTRA_CHANNEL_ID = "android.intent.extra.CHANNEL_ID";
    public static final String EXTRA_CHRONOMETER_COUNT_DOWN = "android.chronometerCountDown";
    public static final String EXTRA_COLORIZED = "android.colorized";
    public static final String EXTRA_COMPACT_ACTIONS = "android.compactActions";
    public static final String EXTRA_CONVERSATION_TITLE = "android.conversationTitle";
    public static final String EXTRA_DECLINE_COLOR = "android.declineColor";
    public static final String EXTRA_DECLINE_INTENT = "android.declineIntent";
    public static final String EXTRA_HANG_UP_INTENT = "android.hangUpIntent";
    public static final String EXTRA_HISTORIC_MESSAGES = "android.messages.historic";
    public static final String EXTRA_INFO_TEXT = "android.infoText";
    public static final String EXTRA_IS_GROUP_CONVERSATION = "android.isGroupConversation";
    public static final String EXTRA_LARGE_ICON = "android.largeIcon";
    public static final String EXTRA_LARGE_ICON_BIG = "android.largeIcon.big";
    public static final String EXTRA_MEDIA_SESSION = "android.mediaSession";
    public static final String EXTRA_MESSAGES = "android.messages";
    public static final String EXTRA_MESSAGING_PERSON = "android.messagingUser";
    public static final String EXTRA_NOTIFICATION_ID = "android.intent.extra.NOTIFICATION_ID";
    public static final String EXTRA_NOTIFICATION_TAG = "android.intent.extra.NOTIFICATION_TAG";
    public static final String EXTRA_PEOPLE = "android.people";
    public static final String EXTRA_PEOPLE_LIST = "android.people.list";
    public static final String EXTRA_PICTURE = "android.picture";
    public static final String EXTRA_PICTURE_CONTENT_DESCRIPTION = "android.pictureContentDescription";
    public static final String EXTRA_PICTURE_ICON = "android.pictureIcon";
    public static final String EXTRA_PROGRESS = "android.progress";
    public static final String EXTRA_PROGRESS_INDETERMINATE = "android.progressIndeterminate";
    public static final String EXTRA_PROGRESS_MAX = "android.progressMax";
    public static final String EXTRA_REMOTE_INPUT_DRAFT = "android.remoteInputDraft";
    public static final String EXTRA_REMOTE_INPUT_HISTORY = "android.remoteInputHistory";
    public static final String EXTRA_SELF_DISPLAY_NAME = "android.selfDisplayName";
    public static final String EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED = "android.showBigPictureWhenCollapsed";
    public static final String EXTRA_SHOW_CHRONOMETER = "android.showChronometer";
    public static final String EXTRA_SHOW_WHEN = "android.showWhen";
    public static final String EXTRA_SMALL_ICON = "android.icon";
    public static final String EXTRA_SUB_TEXT = "android.subText";
    public static final String EXTRA_SUMMARY_TEXT = "android.summaryText";
    public static final String EXTRA_TEMPLATE = "android.template";
    public static final String EXTRA_TEXT = "android.text";
    public static final String EXTRA_TEXT_LINES = "android.textLines";
    public static final String EXTRA_TITLE = "android.title";
    public static final String EXTRA_TITLE_BIG = "android.title.big";
    public static final String EXTRA_VERIFICATION_ICON = "android.verificationIcon";
    public static final String EXTRA_VERIFICATION_TEXT = "android.verificationText";

    public static final int FLAG_SHOW_LIGHTS = 0x00000001;
    public static final int FLAG_ONGOING_EVENT = 0x00000002;
    public static final int FLAG_INSISTENT = 0x00000004;
    public static final int FLAG_ONLY_ALERT_ONCE = 0x00000008;
    public static final int FLAG_AUTO_CANCEL = 0x00000010;
    public static final int FLAG_NO_CLEAR = 0x00000020;
    public static final int FLAG_FOREGROUND_SERVICE = 0x00000040;
    public static final int FLAG_HIGH_PRIORITY = 0x00000080;
    public static final int FLAG_LOCAL_ONLY = 0x00000100;
    public static final int FLAG_GROUP_SUMMARY = 0x00000200;
    public static final int FLAG_BUBBLE = 0x00001000;
    public static final int FLAG_PROMOTED_ONGOING = 0x00040000;

    public static final int FOREGROUND_SERVICE_DEFAULT = 0;
    public static final int FOREGROUND_SERVICE_IMMEDIATE = 1;
    public static final int FOREGROUND_SERVICE_DEFERRED = 2;

    public static final int GROUP_ALERT_ALL = 0;
    public static final int GROUP_ALERT_SUMMARY = 1;
    public static final int GROUP_ALERT_CHILDREN = 2;

    public static final String INTENT_CATEGORY_NOTIFICATION_PREFERENCES = "android.intent.category.NOTIFICATION_PREFERENCES";

    public static final int PRIORITY_MIN = -2;
    public static final int PRIORITY_LOW = -1;
    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_MAX = 2;

    public static final int STREAM_DEFAULT = -1;

    public static final int VISIBILITY_SECRET = -1;
    public static final int VISIBILITY_PRIVATE = 0;
    public static final int VISIBILITY_PUBLIC = 1;

    public static final Parcelable.Creator<Notification> CREATOR = new Parcelable.Creator<Notification>()
    {
        @Override
        public Notification createFromParcel(Parcel source)
        {
            return new Notification(source);
        }

        @Override
        public Notification[] newArray(int size)
        {
            return new Notification[size];
        }
    };

    public Action[] actions;
    public AudioAttributes audioAttributes = AUDIO_ATTRIBUTES_DEFAULT;
    public int audioStreamType = STREAM_DEFAULT;
    public RemoteViews bigContentView;
    public String category;
    public int color = COLOR_DEFAULT;
    public PendingIntent contentIntent;
    public RemoteViews contentView;
    public int defaults;
    public PendingIntent deleteIntent;
    public Bundle extras = new Bundle();
    public int flags;
    public PendingIntent fullScreenIntent;
    public RemoteViews headsUpContentView;
    public int icon;
    public int iconLevel;
    public Bitmap largeIcon;
    public int ledARGB;
    public int ledOffMS;
    public int ledOnMS;
    public int number;
    public int priority;
    public Notification publicVersion;
    public Uri sound;
    public CharSequence tickerText;
    public RemoteViews tickerView;
    public long[] vibrate;
    public int visibility;
    public long when;

    private String mChannelId;
    private String mGroupKey;
    private String mSortKey;
    private String mShortcutId;
    private LocusId mLocusId;
    private CharSequence mSettingsText;
    private long mTimeout;
    private int mBadgeIcon = BADGE_ICON_NONE;
    private int mGroupAlertBehavior = GROUP_ALERT_ALL;
    private BubbleMetadata mBubbleMetadata;
    private boolean mAllowSystemGeneratedContextualActions = true;
    private int mForegroundServiceBehavior = FOREGROUND_SERVICE_DEFAULT;
    private String mShortCriticalText;
    private Icon mSmallIcon;
    private Icon mLargeIcon;

    public Notification()
    {
        when = System.currentTimeMillis();
        priority = PRIORITY_DEFAULT;
    }

    @Deprecated
    public Notification(int icon, CharSequence tickerText, long when)
    {
        this.icon = icon;
        this.tickerText = tickerText;
        this.when = when;
        if (icon != 0) mSmallIcon = Icon.createWithResource("", icon);
    }

    public Notification(Parcel parcel)
    {
        icon = parcel.readInt();
        iconLevel = parcel.readInt();
        number = parcel.readInt();
        when = parcel.readLong();
        flags = parcel.readInt();
        priority = parcel.readInt();
        defaults = parcel.readInt();
        color = parcel.readInt();
        visibility = parcel.readInt();
        audioStreamType = parcel.readInt();
        ledARGB = parcel.readInt();
        ledOnMS = parcel.readInt();
        ledOffMS = parcel.readInt();
        category = parcel.readString();
        tickerText = (CharSequence)parcel.readValue(null);
        contentIntent = (PendingIntent)parcel.readValue(null);
        deleteIntent = (PendingIntent)parcel.readValue(null);
        fullScreenIntent = (PendingIntent)parcel.readValue(null);
        contentView = (RemoteViews)parcel.readValue(null);
        bigContentView = (RemoteViews)parcel.readValue(null);
        headsUpContentView = (RemoteViews)parcel.readValue(null);
        tickerView = (RemoteViews)parcel.readValue(null);
        largeIcon = (Bitmap)parcel.readValue(null);
        sound = (Uri)parcel.readValue(null);
        audioAttributes = (AudioAttributes)parcel.readValue(null);
        vibrate = parcel.createLongArray();
        Bundle readExtras = parcel.readBundle();
        extras = readExtras == null ? new Bundle() : readExtras;
        actions = (Action[])parcel.readValue(null);
        publicVersion = (Notification)parcel.readValue(null);
        mChannelId = parcel.readString();
        mGroupKey = parcel.readString();
        mSortKey = parcel.readString();
        mShortcutId = parcel.readString();
        mLocusId = (LocusId)parcel.readValue(null);
        mSettingsText = (CharSequence)parcel.readValue(null);
        mTimeout = parcel.readLong();
        mBadgeIcon = parcel.readInt();
        mGroupAlertBehavior = parcel.readInt();
        mBubbleMetadata = (BubbleMetadata)parcel.readValue(null);
        mAllowSystemGeneratedContextualActions = parcel.readBoolean();
        mForegroundServiceBehavior = parcel.readInt();
        mShortCriticalText = parcel.readString();
        mSmallIcon = (Icon)parcel.readValue(null);
        mLargeIcon = (Icon)parcel.readValue(null);
    }

    @Override
    public void writeToParcel(Parcel parcel, int parcelableFlags)
    {
        parcel.writeInt(icon);
        parcel.writeInt(iconLevel);
        parcel.writeInt(number);
        parcel.writeLong(when);
        parcel.writeInt(flags);
        parcel.writeInt(priority);
        parcel.writeInt(defaults);
        parcel.writeInt(color);
        parcel.writeInt(visibility);
        parcel.writeInt(audioStreamType);
        parcel.writeInt(ledARGB);
        parcel.writeInt(ledOnMS);
        parcel.writeInt(ledOffMS);
        parcel.writeString(category);
        parcel.writeValue(tickerText);
        parcel.writeValue(contentIntent);
        parcel.writeValue(deleteIntent);
        parcel.writeValue(fullScreenIntent);
        parcel.writeValue(contentView);
        parcel.writeValue(bigContentView);
        parcel.writeValue(headsUpContentView);
        parcel.writeValue(tickerView);
        parcel.writeValue(largeIcon);
        parcel.writeValue(sound);
        parcel.writeValue(audioAttributes);
        parcel.writeLongArray(vibrate);
        parcel.writeBundle(extras);
        parcel.writeValue(actions == null ? null : actions.clone());
        parcel.writeValue(publicVersion == null ? null : publicVersion.clone());
        parcel.writeString(mChannelId);
        parcel.writeString(mGroupKey);
        parcel.writeString(mSortKey);
        parcel.writeString(mShortcutId);
        parcel.writeValue(mLocusId);
        parcel.writeValue(mSettingsText);
        parcel.writeLong(mTimeout);
        parcel.writeInt(mBadgeIcon);
        parcel.writeInt(mGroupAlertBehavior);
        parcel.writeValue(mBubbleMetadata);
        parcel.writeBoolean(mAllowSystemGeneratedContextualActions);
        parcel.writeInt(mForegroundServiceBehavior);
        parcel.writeString(mShortCriticalText);
        parcel.writeValue(mSmallIcon);
        parcel.writeValue(mLargeIcon);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public Notification clone()
    {
        Notification that = new Notification();

        that.when = when;
        that.icon = icon;
        that.iconLevel = iconLevel;
        that.number = number;
        that.contentIntent = contentIntent;
        that.deleteIntent = deleteIntent;
        that.fullScreenIntent = fullScreenIntent;
        that.tickerText = tickerText;
        that.tickerView = tickerView;
        that.contentView = contentView;
        that.bigContentView = bigContentView;
        that.headsUpContentView = headsUpContentView;
        that.largeIcon = largeIcon;
        that.sound = sound;
        that.audioStreamType = audioStreamType;
        that.audioAttributes = audioAttributes;
        that.vibrate = vibrate == null ? null : vibrate.clone();
        that.ledARGB = ledARGB;
        that.ledOnMS = ledOnMS;
        that.ledOffMS = ledOffMS;
        that.defaults = defaults;
        that.flags = flags;
        that.priority = priority;
        that.category = category;
        that.color = color;
        that.visibility = visibility;
        that.extras = new Bundle(extras);

        if (actions != null)
        {
            that.actions = new Action[actions.length];
            for (int index = 0; index < actions.length; index++)
            {
                that.actions[index] = actions[index] == null ? null : actions[index].clone();
            }
        }

        that.publicVersion = publicVersion == null ? null : publicVersion.clone();
        that.mChannelId = mChannelId;
        that.mGroupKey = mGroupKey;
        that.mSortKey = mSortKey;
        that.mShortcutId = mShortcutId;
        that.mLocusId = mLocusId;
        that.mSettingsText = mSettingsText;
        that.mTimeout = mTimeout;
        that.mBadgeIcon = mBadgeIcon;
        that.mGroupAlertBehavior = mGroupAlertBehavior;
        that.mBubbleMetadata = mBubbleMetadata;
        that.mAllowSystemGeneratedContextualActions = mAllowSystemGeneratedContextualActions;
        that.mForegroundServiceBehavior = mForegroundServiceBehavior;
        that.mShortCriticalText = mShortCriticalText;
        that.mSmallIcon = mSmallIcon;
        that.mLargeIcon = mLargeIcon;

        return that;
    }

    public String getChannelId()
    {
        return mChannelId;
    }

    public String getGroup()
    {
        return mGroupKey;
    }

    public String getSortKey()
    {
        return mSortKey;
    }

    public String getShortcutId()
    {
        return mShortcutId;
    }

    public LocusId getLocusId()
    {
        return mLocusId;
    }

    public CharSequence getSettingsText()
    {
        return mSettingsText;
    }

    public long getTimeoutAfter()
    {
        return mTimeout;
    }

    public int getBadgeIconType()
    {
        return mBadgeIcon;
    }

    public int getGroupAlertBehavior()
    {
        return mGroupAlertBehavior;
    }

    public BubbleMetadata getBubbleMetadata()
    {
        return mBubbleMetadata;
    }

    public boolean getAllowSystemGeneratedContextualActions()
    {
        return mAllowSystemGeneratedContextualActions;
    }

    public String getShortCriticalText()
    {
        return mShortCriticalText;
    }

    public Icon getSmallIcon()
    {
        return mSmallIcon;
    }

    public Icon getLargeIcon()
    {
        return mLargeIcon;
    }

    public List<Action> getContextualActions()
    {
        List<Action> contextual = new ArrayList<>();
        if (actions == null) return contextual;

        for (Action action : actions)
        {
            if (action != null && action.isContextual()) contextual.add(action);
        }

        return contextual;
    }

    public Pair<RemoteInput, Action> findRemoteInputActionPair(boolean requiresFreeform)
    {
        if (actions == null) return null;

        for (Action action : actions)
        {
            if (action == null || action.getRemoteInputs() == null) continue;

            RemoteInput resultInput = null;
            for (RemoteInput input : action.getRemoteInputs())
            {
                if (input.getAllowFreeFormInput() || !requiresFreeform) resultInput = input;
            }

            if (resultInput != null) return Pair.create(resultInput, action);
        }

        return null;
    }

    public boolean hasImage()
    {
        if (extras.getParcelable(EXTRA_PICTURE_ICON) != null || extras.get(EXTRA_PICTURE) != null) return true;

        if (BigPictureStyle.class.getName().equals(extras.getString(EXTRA_TEMPLATE)))
        {
            return extras.get(EXTRA_LARGE_ICON_BIG) != null;
        }

        return mLargeIcon != null || largeIcon != null;
    }

    public boolean hasPromotableCharacteristics()
    {
        boolean titled = extras.getCharSequence(EXTRA_TITLE) != null;
        boolean ongoing = (flags & FLAG_ONGOING_EVENT) != 0;
        boolean colorized = extras.getBoolean(EXTRA_COLORIZED);

        return titled && ongoing && !colorized && mGroupKey == null;
    }

    void setChannelId(String channelId)
    {
        mChannelId = channelId;
    }

    void setFlag(int mask, boolean value)
    {
        if (value) flags |= mask;
        else flags &= ~mask;
    }

    private static String visibilityToString(int visibility)
    {
        switch (visibility)
        {
            case VISIBILITY_PRIVATE: return "PRIVATE";
            case VISIBILITY_PUBLIC: return "PUBLIC";
            case VISIBILITY_SECRET: return "SECRET";
            default: return "UNKNOWN(" + visibility + ")";
        }
    }

    private static String priorityToString(int priority)
    {
        switch (priority)
        {
            case PRIORITY_MIN: return "MIN";
            case PRIORITY_LOW: return "LOW";
            case PRIORITY_DEFAULT: return "DEFAULT";
            case PRIORITY_HIGH: return "HIGH";
            case PRIORITY_MAX: return "MAX";
            default: return "UNKNOWN(" + priority + ")";
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Notification(channel=").append(mChannelId);
        builder.append(" shortcut=").append(mShortcutId);
        builder.append(" title=").append(extras.getCharSequence(EXTRA_TITLE));
        builder.append(" text=").append(extras.getCharSequence(EXTRA_TEXT));
        builder.append(" pri=").append(priorityToString(priority));
        builder.append(" contentView=").append(contentView == null ? "null" : contentView.getPackage() + "/0x"
                                                                               + Integer.toHexString(contentView.getLayoutId()));
        builder.append(" vibrate=").append(vibrate == null ? "null" : Arrays.toString(vibrate));
        builder.append(" sound=").append(sound);
        builder.append(" defaults=0x").append(Integer.toHexString(defaults));
        builder.append(" flags=0x").append(Integer.toHexString(flags));
        builder.append(" color=0x").append(String.format("%08x", color));
        if (category != null) builder.append(" category=").append(category);
        if (mGroupKey != null) builder.append(" groupKey=").append(mGroupKey);
        if (mSortKey != null) builder.append(" sortKey=").append(mSortKey);
        if (actions != null) builder.append(" actions=").append(actions.length);
        builder.append(" vis=").append(visibilityToString(visibility));
        if (publicVersion != null) builder.append(" publicVersion=").append(publicVersion);
        if (mLocusId != null) builder.append(" locusId=").append(mLocusId);
        builder.append(")");

        return builder.toString();
    }

    public interface Extender
    {
        Builder extend(Builder builder);
    }

    public static class Builder
    {
        private final Context mContext;
        private final Notification mNotification;
        private final Bundle mUserExtras = new Bundle();
        private final ArrayList<Action> mActions = new ArrayList<>();
        private final ArrayList<Person> mPersonList = new ArrayList<>();
        private Style mStyle;
        private CharSequence mContentTitle;
        private CharSequence mContentText;
        private CharSequence mSubText;
        private CharSequence mContentInfo;
        private CharSequence[] mRemoteInputHistory;
        private int mProgressMax;
        private int mProgress;
        private boolean mProgressIndeterminate;
        private boolean mShowWhen = true;
        private boolean mUsesChronometer;
        private boolean mChronometerCountDown;
        private boolean mColorized;

        public Builder(Context context, String channelId)
        {
            mContext = context;
            mNotification = new Notification();
            mNotification.mChannelId = channelId;
        }

        @Deprecated
        public Builder(Context context)
        {
            this(context, (String)null);
        }

        private Builder(Context context, Notification notification)
        {
            mContext = context;
            mNotification = notification;

            Bundle source = notification.extras == null ? new Bundle() : notification.extras;
            mContentTitle = source.getCharSequence(EXTRA_TITLE);
            mContentText = source.getCharSequence(EXTRA_TEXT);
            mSubText = source.getCharSequence(EXTRA_SUB_TEXT);
            mContentInfo = source.getCharSequence(EXTRA_INFO_TEXT);
            mRemoteInputHistory = source.getCharSequenceArray(EXTRA_REMOTE_INPUT_HISTORY);
            mProgressMax = source.getInt(EXTRA_PROGRESS_MAX);
            mProgress = source.getInt(EXTRA_PROGRESS);
            mProgressIndeterminate = source.getBoolean(EXTRA_PROGRESS_INDETERMINATE);
            mShowWhen = source.getBoolean(EXTRA_SHOW_WHEN, true);
            mUsesChronometer = source.getBoolean(EXTRA_SHOW_CHRONOMETER);
            mChronometerCountDown = source.getBoolean(EXTRA_CHRONOMETER_COUNT_DOWN);
            mColorized = source.getBoolean(EXTRA_COLORIZED);

            ArrayList<Person> people = source.getParcelableArrayList(EXTRA_PEOPLE_LIST);
            if (people != null) mPersonList.addAll(people);

            if (notification.actions != null)
            {
                for (Action action : notification.actions)
                {
                    if (action != null) mActions.add(action);
                }
            }

            mUserExtras.putAll(source);
            for (String key : BUILDER_KEYS) mUserExtras.remove(key);

            Style style = Style.fromTemplate(source.getString(EXTRA_TEMPLATE));
            if (style != null)
            {
                style.restoreFromExtras(source);
                setStyle(style);
            }
        }

        private static final String[] BUILDER_KEYS = new String[] {
                EXTRA_TITLE, EXTRA_TEXT, EXTRA_SUB_TEXT, EXTRA_INFO_TEXT, EXTRA_REMOTE_INPUT_HISTORY, EXTRA_PROGRESS_MAX,
                EXTRA_PROGRESS, EXTRA_PROGRESS_INDETERMINATE, EXTRA_SHOW_WHEN, EXTRA_SHOW_CHRONOMETER,
                EXTRA_CHRONOMETER_COUNT_DOWN, EXTRA_COLORIZED, EXTRA_PEOPLE_LIST, EXTRA_TEMPLATE, EXTRA_SMALL_ICON,
                EXTRA_LARGE_ICON, EXTRA_TITLE_BIG, EXTRA_SUMMARY_TEXT, EXTRA_BIG_TEXT, EXTRA_TEXT_LINES, EXTRA_PICTURE,
                EXTRA_PICTURE_ICON, EXTRA_LARGE_ICON_BIG, EXTRA_PICTURE_CONTENT_DESCRIPTION,
                EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED, EXTRA_COMPACT_ACTIONS, EXTRA_MEDIA_SESSION, EXTRA_MESSAGING_PERSON,
                EXTRA_SELF_DISPLAY_NAME, EXTRA_CONVERSATION_TITLE, EXTRA_MESSAGES, EXTRA_HISTORIC_MESSAGES,
                EXTRA_IS_GROUP_CONVERSATION, EXTRA_CALL_TYPE, EXTRA_CALL_IS_VIDEO, EXTRA_CALL_PERSON, EXTRA_VERIFICATION_ICON,
                EXTRA_VERIFICATION_TEXT, EXTRA_ANSWER_INTENT, EXTRA_DECLINE_INTENT, EXTRA_HANG_UP_INTENT, EXTRA_ANSWER_COLOR,
                EXTRA_DECLINE_COLOR
        };

        public static Builder recoverBuilder(Context context, Notification notification)
        {
            return new Builder(context, notification);
        }

        String packageName()
        {
            if (mContext == null) return "";

            String name = mContext.getPackageName();
            return name == null ? "" : name;
        }

        public Builder setShortcutId(String shortcutId)
        {
            mNotification.mShortcutId = shortcutId;
            return this;
        }

        public Builder setLocusId(LocusId locusId)
        {
            mNotification.mLocusId = locusId;
            return this;
        }

        public Builder setBadgeIconType(int icon)
        {
            mNotification.mBadgeIcon = icon;
            return this;
        }

        public Builder setGroupAlertBehavior(int groupAlertBehavior)
        {
            mNotification.mGroupAlertBehavior = groupAlertBehavior;
            return this;
        }

        public Builder setBubbleMetadata(BubbleMetadata data)
        {
            mNotification.mBubbleMetadata = data;
            return this;
        }

        public Builder setChannelId(String channelId)
        {
            mNotification.mChannelId = channelId;
            return this;
        }

        public Builder setTimeoutAfter(long durationMs)
        {
            mNotification.mTimeout = durationMs;
            return this;
        }

        public Builder setWhen(long when)
        {
            mNotification.when = when;
            return this;
        }

        public Builder setShowWhen(boolean show)
        {
            mShowWhen = show;
            return this;
        }

        public Builder setUsesChronometer(boolean usesChronometer)
        {
            mUsesChronometer = usesChronometer;
            return this;
        }

        public Builder setChronometerCountDown(boolean countDown)
        {
            mChronometerCountDown = countDown;
            return this;
        }

        public Builder setSmallIcon(int icon)
        {
            return setSmallIcon(icon == 0 ? null : Icon.createWithResource(packageName(), icon));
        }

        public Builder setSmallIcon(int icon, int level)
        {
            mNotification.iconLevel = level;
            return setSmallIcon(icon);
        }

        public Builder setSmallIcon(Icon icon)
        {
            mNotification.mSmallIcon = icon;
            if (icon != null && icon.getType() == Icon.TYPE_RESOURCE) mNotification.icon = icon.getResId();
            else if (icon == null) mNotification.icon = 0;

            return this;
        }

        public Builder setContentTitle(CharSequence title)
        {
            mContentTitle = title;
            return this;
        }

        public Builder setContentText(CharSequence text)
        {
            mContentText = text;
            return this;
        }

        public Builder setSubText(CharSequence text)
        {
            mSubText = text;
            return this;
        }

        public Builder setSettingsText(CharSequence text)
        {
            mNotification.mSettingsText = text;
            return this;
        }

        public Builder setRemoteInputHistory(CharSequence[] text)
        {
            mRemoteInputHistory = text == null ? null : text.clone();
            return this;
        }

        @Deprecated
        public Builder setNumber(int number)
        {
            mNotification.number = number;
            return this;
        }

        @Deprecated
        public Builder setContentInfo(CharSequence info)
        {
            mContentInfo = info;
            return this;
        }

        public Builder setProgress(int max, int progress, boolean indeterminate)
        {
            mProgressMax = max;
            mProgress = progress;
            mProgressIndeterminate = indeterminate;
            return this;
        }

        @Deprecated
        public Builder setContent(RemoteViews views)
        {
            return setCustomContentView(views);
        }

        public Builder setCustomContentView(RemoteViews contentView)
        {
            mNotification.contentView = contentView;
            return this;
        }

        public Builder setCustomBigContentView(RemoteViews contentView)
        {
            mNotification.bigContentView = contentView;
            return this;
        }

        public Builder setCustomHeadsUpContentView(RemoteViews contentView)
        {
            mNotification.headsUpContentView = contentView;
            return this;
        }

        public Builder setContentIntent(PendingIntent intent)
        {
            mNotification.contentIntent = intent;
            return this;
        }

        public Builder setDeleteIntent(PendingIntent intent)
        {
            mNotification.deleteIntent = intent;
            return this;
        }

        public Builder setFullScreenIntent(PendingIntent intent, boolean highPriority)
        {
            mNotification.fullScreenIntent = intent;
            mNotification.setFlag(FLAG_HIGH_PRIORITY, highPriority);
            return this;
        }

        public Builder setTicker(CharSequence tickerText)
        {
            mNotification.tickerText = tickerText;
            return this;
        }

        @Deprecated
        public Builder setTicker(CharSequence tickerText, RemoteViews views)
        {
            mNotification.tickerText = tickerText;
            mNotification.tickerView = views;
            return this;
        }

        public Builder setLargeIcon(Icon icon)
        {
            mNotification.mLargeIcon = icon;
            if (icon == null) mNotification.largeIcon = null;
            return this;
        }

        public Builder setLargeIcon(Bitmap bitmap)
        {
            mNotification.largeIcon = bitmap;
            mNotification.mLargeIcon = bitmap == null ? null : Icon.createWithBitmap(bitmap);
            return this;
        }

        @Deprecated
        public Builder setSound(Uri sound)
        {
            mNotification.sound = sound;
            mNotification.audioAttributes = AUDIO_ATTRIBUTES_DEFAULT;
            return this;
        }

        @Deprecated
        public Builder setSound(Uri sound, int streamType)
        {
            mNotification.sound = sound;
            mNotification.audioStreamType = streamType;
            return this;
        }

        @Deprecated
        public Builder setSound(Uri sound, AudioAttributes audioAttributes)
        {
            mNotification.sound = sound;
            mNotification.audioAttributes = audioAttributes;
            return this;
        }

        @Deprecated
        public Builder setVibrate(long[] pattern)
        {
            mNotification.vibrate = pattern;
            return this;
        }

        @Deprecated
        public Builder setLights(int argb, int onMs, int offMs)
        {
            mNotification.ledARGB = argb;
            mNotification.ledOnMS = onMs;
            mNotification.ledOffMS = offMs;
            if (onMs != 0 || offMs != 0) mNotification.flags |= FLAG_SHOW_LIGHTS;
            return this;
        }

        public Builder setOngoing(boolean ongoing)
        {
            mNotification.setFlag(FLAG_ONGOING_EVENT, ongoing);
            return this;
        }

        public Builder setColorized(boolean colorize)
        {
            mColorized = colorize;
            return this;
        }

        public Builder setOnlyAlertOnce(boolean onlyAlertOnce)
        {
            mNotification.setFlag(FLAG_ONLY_ALERT_ONCE, onlyAlertOnce);
            return this;
        }

        public Builder setAutoCancel(boolean autoCancel)
        {
            mNotification.setFlag(FLAG_AUTO_CANCEL, autoCancel);
            return this;
        }

        public Builder setLocalOnly(boolean localOnly)
        {
            mNotification.setFlag(FLAG_LOCAL_ONLY, localOnly);
            return this;
        }

        public Builder setFlag(int mask, boolean value)
        {
            mNotification.setFlag(mask, value);
            return this;
        }

        @Deprecated
        public Builder setDefaults(int defaults)
        {
            mNotification.defaults = defaults;
            return this;
        }

        @Deprecated
        public Builder setPriority(int priority)
        {
            mNotification.priority = priority;
            return this;
        }

        public Builder setCategory(String category)
        {
            mNotification.category = category;
            return this;
        }

        @Deprecated
        public Builder addPerson(String uri)
        {
            return addPerson(new Person.Builder().setUri(uri).build());
        }

        public Builder addPerson(Person person)
        {
            if (person != null) mPersonList.add(person);
            return this;
        }

        public Builder setGroup(String groupKey)
        {
            mNotification.mGroupKey = groupKey;
            return this;
        }

        public Builder setGroupSummary(boolean isGroupSummary)
        {
            mNotification.setFlag(FLAG_GROUP_SUMMARY, isGroupSummary);
            return this;
        }

        public Builder setSortKey(String sortKey)
        {
            mNotification.mSortKey = sortKey;
            return this;
        }

        public Builder addExtras(Bundle extras)
        {
            if (extras != null) mUserExtras.putAll(extras);
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mUserExtras.clear();
            if (extras != null) mUserExtras.putAll(extras);
            return this;
        }

        public Bundle getExtras()
        {
            return mUserExtras;
        }

        @Deprecated
        public Builder addAction(int icon, CharSequence title, PendingIntent intent)
        {
            mActions.add(new Action(icon, title, intent));
            return this;
        }

        public Builder addAction(Action action)
        {
            if (action != null) mActions.add(action);
            return this;
        }

        public Builder setActions(Action... actions)
        {
            mActions.clear();
            if (actions == null) return this;

            for (Action action : actions)
            {
                if (action != null) mActions.add(action);
            }

            return this;
        }

        public Builder setStyle(Style style)
        {
            if (mStyle != style)
            {
                mStyle = style;
                if (style != null) style.setBuilder(this);
            }

            return this;
        }

        public Style getStyle()
        {
            return mStyle;
        }

        public Builder setVisibility(int visibility)
        {
            mNotification.visibility = visibility;
            return this;
        }

        public Builder setPublicVersion(Notification notification)
        {
            mNotification.publicVersion = notification == null ? null : notification.clone();
            return this;
        }

        public Builder extend(Extender extender)
        {
            if (extender != null) extender.extend(this);
            return this;
        }

        public Builder setColor(int argb)
        {
            mNotification.color = argb;
            return this;
        }

        public Builder setAllowSystemGeneratedContextualActions(boolean allowed)
        {
            mNotification.mAllowSystemGeneratedContextualActions = allowed;
            return this;
        }

        public Builder setForegroundServiceBehavior(int behavior)
        {
            mNotification.mForegroundServiceBehavior = behavior;
            return this;
        }

        public Builder setShortCriticalText(String shortCriticalText)
        {
            mNotification.mShortCriticalText = shortCriticalText;
            return this;
        }

        public RemoteViews createContentView()
        {
            if (mNotification.contentView != null && (mStyle == null || !mStyle.displayCustomViewInline()))
            {
                return mNotification.contentView;
            }

            return new RemoteViews(packageName(), 0);
        }

        public RemoteViews createBigContentView()
        {
            if (mNotification.bigContentView != null) return mNotification.bigContentView;

            return new RemoteViews(packageName(), 0);
        }

        public RemoteViews createHeadsUpContentView()
        {
            if (mNotification.headsUpContentView != null) return mNotification.headsUpContentView;

            return new RemoteViews(packageName(), 0);
        }

        @Deprecated
        public Notification getNotification()
        {
            return build();
        }

        public Notification build()
        {
            Notification notification = mNotification;

            Bundle extras = new Bundle(mUserExtras);
            if (mContentTitle != null) extras.putCharSequence(EXTRA_TITLE, mContentTitle);
            if (mContentText != null) extras.putCharSequence(EXTRA_TEXT, mContentText);
            if (mSubText != null) extras.putCharSequence(EXTRA_SUB_TEXT, mSubText);
            if (mContentInfo != null) extras.putCharSequence(EXTRA_INFO_TEXT, mContentInfo);
            if (mRemoteInputHistory != null) extras.putCharSequenceArray(EXTRA_REMOTE_INPUT_HISTORY, mRemoteInputHistory);
            if (notification.mSmallIcon != null) extras.putParcelable(EXTRA_SMALL_ICON, notification.mSmallIcon);
            if (notification.mLargeIcon != null) extras.putParcelable(EXTRA_LARGE_ICON, notification.mLargeIcon);

            extras.putInt(EXTRA_PROGRESS, mProgress);
            extras.putInt(EXTRA_PROGRESS_MAX, mProgressMax);
            extras.putBoolean(EXTRA_PROGRESS_INDETERMINATE, mProgressIndeterminate);
            extras.putBoolean(EXTRA_SHOW_CHRONOMETER, mUsesChronometer);
            extras.putBoolean(EXTRA_CHRONOMETER_COUNT_DOWN, mChronometerCountDown);
            extras.putBoolean(EXTRA_SHOW_WHEN, mShowWhen);
            if (mColorized) extras.putBoolean(EXTRA_COLORIZED, true);
            if (!mPersonList.isEmpty()) extras.putParcelableArrayList(EXTRA_PEOPLE_LIST, new ArrayList<>(mPersonList));

            notification.extras = extras;
            notification.actions = mActions.isEmpty() ? null : mActions.toArray(new Action[0]);

            if (mStyle != null)
            {
                extras.putString(EXTRA_TEMPLATE, mStyle.getClass().getName());
                mStyle.addExtras(extras);
            }

            return notification;
        }
    }

    public static class Action implements Parcelable
    {
        public static final int SEMANTIC_ACTION_NONE = 0;
        public static final int SEMANTIC_ACTION_REPLY = 1;
        public static final int SEMANTIC_ACTION_MARK_AS_READ = 2;
        public static final int SEMANTIC_ACTION_MARK_AS_UNREAD = 3;
        public static final int SEMANTIC_ACTION_DELETE = 4;
        public static final int SEMANTIC_ACTION_ARCHIVE = 5;
        public static final int SEMANTIC_ACTION_MUTE = 6;
        public static final int SEMANTIC_ACTION_UNMUTE = 7;
        public static final int SEMANTIC_ACTION_THUMBS_UP = 8;
        public static final int SEMANTIC_ACTION_THUMBS_DOWN = 9;
        public static final int SEMANTIC_ACTION_CALL = 10;

        public static final Parcelable.Creator<Action> CREATOR = new Parcelable.Creator<Action>()
        {
            @Override
            public Action createFromParcel(Parcel source)
            {
                Icon icon = (Icon)source.readValue(null);
                CharSequence title = (CharSequence)source.readValue(null);
                PendingIntent intent = (PendingIntent)source.readValue(null);
                Bundle extras = source.readBundle();
                RemoteInput[] remoteInputs = (RemoteInput[])source.readValue(null);
                RemoteInput[] dataOnlyInputs = (RemoteInput[])source.readValue(null);
                boolean allowGeneratedReplies = source.readBoolean();
                int semanticAction = source.readInt();
                boolean contextual = source.readBoolean();
                boolean authenticationRequired = source.readBoolean();

                return new Action(icon, title, intent, extras, remoteInputs, dataOnlyInputs, allowGeneratedReplies,
                                  semanticAction, contextual, authenticationRequired);
            }

            @Override
            public Action[] newArray(int size)
            {
                return new Action[size];
            }
        };

        public PendingIntent actionIntent;
        @Deprecated
        public int icon;
        public CharSequence title;

        private final Bundle mExtras;
        private Icon mIcon;
        private final RemoteInput[] mRemoteInputs;
        private final RemoteInput[] mDataOnlyRemoteInputs;
        private boolean mAllowGeneratedReplies = true;
        private final int mSemanticAction;
        private final boolean mIsContextual;
        private final boolean mAuthenticationRequired;

        @Deprecated
        public Action(int icon, CharSequence title, PendingIntent intent)
        {
            this(icon == 0 ? null : Icon.createWithResource("", icon), title, intent, new Bundle(), null, null, true,
                 SEMANTIC_ACTION_NONE, false, false);
        }

        private Action(Icon icon, CharSequence title, PendingIntent intent, Bundle extras, RemoteInput[] remoteInputs,
                       RemoteInput[] dataOnlyRemoteInputs, boolean allowGeneratedReplies, int semanticAction,
                       boolean isContextual, boolean authenticationRequired)
        {
            mIcon = icon;
            if (icon != null && icon.getType() == Icon.TYPE_RESOURCE) this.icon = icon.getResId();
            this.title = title;
            this.actionIntent = intent;
            mExtras = extras == null ? new Bundle() : extras;
            mRemoteInputs = remoteInputs;
            mDataOnlyRemoteInputs = dataOnlyRemoteInputs;
            mAllowGeneratedReplies = allowGeneratedReplies;
            mSemanticAction = semanticAction;
            mIsContextual = isContextual;
            mAuthenticationRequired = authenticationRequired;
        }

        public Icon getIcon()
        {
            if (mIcon == null && icon != 0) mIcon = Icon.createWithResource("", icon);
            return mIcon;
        }

        public Bundle getExtras()
        {
            return mExtras;
        }

        public boolean getAllowGeneratedReplies()
        {
            return mAllowGeneratedReplies;
        }

        public RemoteInput[] getRemoteInputs()
        {
            return mRemoteInputs;
        }

        public RemoteInput[] getDataOnlyRemoteInputs()
        {
            return mDataOnlyRemoteInputs;
        }

        public int getSemanticAction()
        {
            return mSemanticAction;
        }

        public boolean isContextual()
        {
            return mIsContextual;
        }

        public boolean isAuthenticationRequired()
        {
            return mAuthenticationRequired;
        }

        @Override
        public Action clone()
        {
            return new Action(getIcon(), title, actionIntent, new Bundle(mExtras),
                              mRemoteInputs == null ? null : mRemoteInputs.clone(),
                              mDataOnlyRemoteInputs == null ? null : mDataOnlyRemoteInputs.clone(), mAllowGeneratedReplies,
                              mSemanticAction, mIsContextual, mAuthenticationRequired);
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags)
        {
            parcel.writeValue(getIcon());
            parcel.writeValue(title);
            parcel.writeValue(actionIntent);
            parcel.writeBundle(mExtras);
            parcel.writeValue(mRemoteInputs == null ? null : mRemoteInputs.clone());
            parcel.writeValue(mDataOnlyRemoteInputs == null ? null : mDataOnlyRemoteInputs.clone());
            parcel.writeBoolean(mAllowGeneratedReplies);
            parcel.writeInt(mSemanticAction);
            parcel.writeBoolean(mIsContextual);
            parcel.writeBoolean(mAuthenticationRequired);
        }

        @Override
        public String toString()
        {
            return "Action(" + title + ")";
        }

        public interface Extender
        {
            Builder extend(Builder builder);
        }

        public static final class Builder
        {
            private final Icon mIcon;
            private final CharSequence mTitle;
            private final PendingIntent mIntent;
            private final Bundle mExtras;
            private final ArrayList<RemoteInput> mRemoteInputs = new ArrayList<>();
            private boolean mAllowGeneratedReplies = true;
            private int mSemanticAction;
            private boolean mIsContextual;
            private boolean mAuthenticationRequired;

            @Deprecated
            public Builder(int icon, CharSequence title, PendingIntent intent)
            {
                this(icon == 0 ? null : Icon.createWithResource("", icon), title, intent);
            }

            public Builder(Icon icon, CharSequence title, PendingIntent intent)
            {
                this(icon, title, intent, new Bundle(), null, true, SEMANTIC_ACTION_NONE, false);
            }

            public Builder(Action action)
            {
                this(action.getIcon(), action.title, action.actionIntent, new Bundle(action.mExtras),
                     action.getRemoteInputs(), action.getAllowGeneratedReplies(), action.getSemanticAction(),
                     action.isAuthenticationRequired());

                if (action.mDataOnlyRemoteInputs != null)
                {
                    mRemoteInputs.addAll(Arrays.asList(action.mDataOnlyRemoteInputs));
                }

                mIsContextual = action.isContextual();
            }

            private Builder(Icon icon, CharSequence title, PendingIntent intent, Bundle extras, RemoteInput[] remoteInputs,
                            boolean allowGeneratedReplies, int semanticAction, boolean authenticationRequired)
            {
                mIcon = icon;
                mTitle = title;
                mIntent = intent;
                mExtras = extras;
                if (remoteInputs != null) mRemoteInputs.addAll(Arrays.asList(remoteInputs));
                mAllowGeneratedReplies = allowGeneratedReplies;
                mSemanticAction = semanticAction;
                mAuthenticationRequired = authenticationRequired;
            }

            public Builder addExtras(Bundle extras)
            {
                if (extras != null) mExtras.putAll(extras);
                return this;
            }

            public Bundle getExtras()
            {
                return mExtras;
            }

            public Builder addRemoteInput(RemoteInput remoteInput)
            {
                if (remoteInput != null) mRemoteInputs.add(remoteInput);
                return this;
            }

            public Builder setAllowGeneratedReplies(boolean allowGeneratedReplies)
            {
                mAllowGeneratedReplies = allowGeneratedReplies;
                return this;
            }

            public Builder setSemanticAction(int semanticAction)
            {
                mSemanticAction = semanticAction;
                return this;
            }

            public Builder setContextual(boolean isContextual)
            {
                mIsContextual = isContextual;
                return this;
            }

            public Builder setAuthenticationRequired(boolean authenticationRequired)
            {
                mAuthenticationRequired = authenticationRequired;
                return this;
            }

            public Builder extend(Extender extender)
            {
                if (extender != null) extender.extend(this);
                return this;
            }

            public Action build()
            {
                if (mIsContextual && mIntent == null)
                {
                    throw new NullPointerException("Contextual Actions must contain a valid PendingIntent");
                }

                ArrayList<RemoteInput> textInputs = new ArrayList<>();
                ArrayList<RemoteInput> dataOnlyInputs = new ArrayList<>();

                for (RemoteInput input : mRemoteInputs)
                {
                    if (input.isDataOnly()) dataOnlyInputs.add(input);
                    else textInputs.add(input);
                }

                return new Action(mIcon, mTitle, mIntent, mExtras,
                                  textInputs.isEmpty() ? null : textInputs.toArray(new RemoteInput[0]),
                                  dataOnlyInputs.isEmpty() ? null : dataOnlyInputs.toArray(new RemoteInput[0]),
                                  mAllowGeneratedReplies, mSemanticAction, mIsContextual, mAuthenticationRequired);
            }
        }
    }

    public abstract static class Style
    {
        protected Builder mBuilder;

        private CharSequence mBigContentTitle;
        private CharSequence mSummaryText;
        private boolean mSummaryTextSet;

        public Style()
        {
        }

        static Style fromTemplate(String template)
        {
            if (template == null) return null;

            if (template.equals(BigTextStyle.class.getName())) return new BigTextStyle();
            if (template.equals(BigPictureStyle.class.getName())) return new BigPictureStyle();
            if (template.equals(InboxStyle.class.getName())) return new InboxStyle();
            if (template.equals(DecoratedMediaCustomViewStyle.class.getName())) return new DecoratedMediaCustomViewStyle();
            if (template.equals(MediaStyle.class.getName())) return new MediaStyle();
            if (template.equals(DecoratedCustomViewStyle.class.getName())) return new DecoratedCustomViewStyle();
            if (template.equals(MessagingStyle.class.getName())) return new MessagingStyle((CharSequence)null);
            if (template.equals(CallStyle.class.getName())) return new CallStyle();

            return null;
        }

        protected void internalSetBigContentTitle(CharSequence title)
        {
            mBigContentTitle = title;
        }

        protected void internalSetSummaryText(CharSequence summaryText)
        {
            mSummaryText = summaryText;
            mSummaryTextSet = true;
        }

        public void setBuilder(Builder builder)
        {
            if (mBuilder != builder)
            {
                mBuilder = builder;
                if (builder != null) builder.setStyle(this);
            }
        }

        protected void checkBuilder()
        {
            if (mBuilder == null) throw new IllegalArgumentException("Style requires a valid Builder object");
        }

        protected RemoteViews getStandardView(int layoutId)
        {
            checkBuilder();
            return new RemoteViews(mBuilder.packageName(), layoutId);
        }

        boolean displayCustomViewInline()
        {
            return false;
        }

        void addExtras(Bundle extras)
        {
            if (mSummaryTextSet) extras.putCharSequence(EXTRA_SUMMARY_TEXT, mSummaryText);
            if (mBigContentTitle != null) extras.putCharSequence(EXTRA_TITLE_BIG, mBigContentTitle);
        }

        void restoreFromExtras(Bundle extras)
        {
            if (extras.containsKey(EXTRA_SUMMARY_TEXT))
            {
                mSummaryText = extras.getCharSequence(EXTRA_SUMMARY_TEXT);
                mSummaryTextSet = true;
            }

            if (extras.containsKey(EXTRA_TITLE_BIG)) mBigContentTitle = extras.getCharSequence(EXTRA_TITLE_BIG);
        }

        public Notification build()
        {
            checkBuilder();
            return mBuilder.build();
        }
    }

    public static class BigPictureStyle extends Style
    {
        private Icon mPictureIcon;
        private Icon mBigLargeIcon;
        private boolean mBigLargeIconSet;
        private CharSequence mPictureContentDescription;
        private boolean mShowBigPictureWhenCollapsed;

        public BigPictureStyle()
        {
        }

        @Deprecated
        public BigPictureStyle(Builder builder)
        {
            setBuilder(builder);
        }

        public BigPictureStyle setBigContentTitle(CharSequence title)
        {
            internalSetBigContentTitle(title);
            return this;
        }

        public BigPictureStyle setSummaryText(CharSequence cs)
        {
            internalSetSummaryText(cs);
            return this;
        }

        public BigPictureStyle setContentDescription(CharSequence contentDescription)
        {
            mPictureContentDescription = contentDescription;
            return this;
        }

        public BigPictureStyle bigPicture(Bitmap bitmap)
        {
            mPictureIcon = bitmap == null ? null : Icon.createWithBitmap(bitmap);
            return this;
        }

        public BigPictureStyle bigPicture(Icon icon)
        {
            mPictureIcon = icon;
            return this;
        }

        public BigPictureStyle showBigPictureWhenCollapsed(boolean show)
        {
            mShowBigPictureWhenCollapsed = show;
            return this;
        }

        public BigPictureStyle bigLargeIcon(Bitmap bitmap)
        {
            return bigLargeIcon(bitmap == null ? null : Icon.createWithBitmap(bitmap));
        }

        public BigPictureStyle bigLargeIcon(Icon icon)
        {
            mBigLargeIconSet = true;
            mBigLargeIcon = icon;
            return this;
        }

        @Override
        void addExtras(Bundle extras)
        {
            super.addExtras(extras);

            if (mBigLargeIconSet) extras.putParcelable(EXTRA_LARGE_ICON_BIG, mBigLargeIcon);
            if (mPictureContentDescription != null)
            {
                extras.putCharSequence(EXTRA_PICTURE_CONTENT_DESCRIPTION, mPictureContentDescription);
            }

            extras.putBoolean(EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED, mShowBigPictureWhenCollapsed);
            if (mPictureIcon != null) extras.putParcelable(EXTRA_PICTURE_ICON, mPictureIcon);
        }

        @Override
        void restoreFromExtras(Bundle extras)
        {
            super.restoreFromExtras(extras);

            if (extras.containsKey(EXTRA_LARGE_ICON_BIG))
            {
                mBigLargeIconSet = true;
                mBigLargeIcon = extras.getParcelable(EXTRA_LARGE_ICON_BIG);
            }

            mPictureContentDescription = extras.getCharSequence(EXTRA_PICTURE_CONTENT_DESCRIPTION);
            mShowBigPictureWhenCollapsed = extras.getBoolean(EXTRA_SHOW_BIG_PICTURE_WHEN_COLLAPSED);
            mPictureIcon = extras.getParcelable(EXTRA_PICTURE_ICON);
        }
    }

    public static class BigTextStyle extends Style
    {
        private CharSequence mBigText;

        public BigTextStyle()
        {
        }

        @Deprecated
        public BigTextStyle(Builder builder)
        {
            setBuilder(builder);
        }

        public BigTextStyle setBigContentTitle(CharSequence title)
        {
            internalSetBigContentTitle(title);
            return this;
        }

        public BigTextStyle setSummaryText(CharSequence cs)
        {
            internalSetSummaryText(cs);
            return this;
        }

        public BigTextStyle bigText(CharSequence cs)
        {
            mBigText = cs;
            return this;
        }

        @Override
        void addExtras(Bundle extras)
        {
            super.addExtras(extras);
            extras.putCharSequence(EXTRA_BIG_TEXT, mBigText);
        }

        @Override
        void restoreFromExtras(Bundle extras)
        {
            super.restoreFromExtras(extras);
            mBigText = extras.getCharSequence(EXTRA_BIG_TEXT);
        }
    }

    public static class InboxStyle extends Style
    {
        private final ArrayList<CharSequence> mTexts = new ArrayList<>(5);

        public InboxStyle()
        {
        }

        @Deprecated
        public InboxStyle(Builder builder)
        {
            setBuilder(builder);
        }

        public InboxStyle setBigContentTitle(CharSequence title)
        {
            internalSetBigContentTitle(title);
            return this;
        }

        public InboxStyle setSummaryText(CharSequence cs)
        {
            internalSetSummaryText(cs);
            return this;
        }

        public InboxStyle addLine(CharSequence cs)
        {
            mTexts.add(cs);
            return this;
        }

        @Override
        void addExtras(Bundle extras)
        {
            super.addExtras(extras);
            extras.putCharSequenceArray(EXTRA_TEXT_LINES, mTexts.toArray(new CharSequence[0]));
        }

        @Override
        void restoreFromExtras(Bundle extras)
        {
            super.restoreFromExtras(extras);

            mTexts.clear();
            CharSequence[] lines = extras.getCharSequenceArray(EXTRA_TEXT_LINES);
            if (lines != null) mTexts.addAll(Arrays.asList(lines));
        }
    }

    public static class MediaStyle extends Style
    {
        private static final String EXTRA_MEDIA_REMOTE_DEVICE = "android.mediaRemoteDevice";
        private static final String EXTRA_MEDIA_REMOTE_ICON = "android.mediaRemoteIcon";
        private static final String EXTRA_MEDIA_REMOTE_INTENT = "android.mediaRemoteIntent";

        private int[] mActionsToShowInCompact;
        private android.media.session.MediaSession.Token mToken;
        private CharSequence mDeviceName;
        private int mDeviceIcon;
        private PendingIntent mDeviceIntent;

        public MediaStyle()
        {
        }

        @Deprecated
        public MediaStyle(Builder builder)
        {
            setBuilder(builder);
        }

        public MediaStyle setShowActionsInCompactView(int... actions)
        {
            mActionsToShowInCompact = actions;
            return this;
        }

        public MediaStyle setMediaSession(android.media.session.MediaSession.Token token)
        {
            mToken = token;
            return this;
        }

        public MediaStyle setRemotePlaybackInfo(CharSequence deviceName, int iconResource, PendingIntent chipIntent)
        {
            mDeviceName = deviceName;
            mDeviceIcon = iconResource;
            mDeviceIntent = chipIntent;
            return this;
        }

        @Override
        void addExtras(Bundle extras)
        {
            super.addExtras(extras);

            if (mToken != null) extras.putParcelable(EXTRA_MEDIA_SESSION, mToken);
            if (mActionsToShowInCompact != null) extras.putIntArray(EXTRA_COMPACT_ACTIONS, mActionsToShowInCompact);
            if (mDeviceName != null) extras.putCharSequence(EXTRA_MEDIA_REMOTE_DEVICE, mDeviceName);
            if (mDeviceIcon > 0) extras.putInt(EXTRA_MEDIA_REMOTE_ICON, mDeviceIcon);
            if (mDeviceIntent != null) extras.putParcelable(EXTRA_MEDIA_REMOTE_INTENT, mDeviceIntent);
        }

        @Override
        void restoreFromExtras(Bundle extras)
        {
            super.restoreFromExtras(extras);

            if (extras.containsKey(EXTRA_MEDIA_SESSION)) mToken = extras.getParcelable(EXTRA_MEDIA_SESSION);
            if (extras.containsKey(EXTRA_COMPACT_ACTIONS)) mActionsToShowInCompact = extras.getIntArray(EXTRA_COMPACT_ACTIONS);
            mDeviceName = extras.getCharSequence(EXTRA_MEDIA_REMOTE_DEVICE);
            mDeviceIcon = extras.getInt(EXTRA_MEDIA_REMOTE_ICON);
            mDeviceIntent = extras.getParcelable(EXTRA_MEDIA_REMOTE_INTENT);
        }
    }

    public static class DecoratedCustomViewStyle extends Style
    {
        public DecoratedCustomViewStyle()
        {
        }

        @Override
        boolean displayCustomViewInline()
        {
            return true;
        }
    }

    public static class DecoratedMediaCustomViewStyle extends MediaStyle
    {
        public DecoratedMediaCustomViewStyle()
        {
        }

        @Override
        boolean displayCustomViewInline()
        {
            return true;
        }
    }

    public static class CallStyle extends Style
    {
        public static final int CALL_TYPE_UNKNOWN = 0;
        public static final int CALL_TYPE_INCOMING = 1;
        public static final int CALL_TYPE_ONGOING = 2;
        public static final int CALL_TYPE_SCREENING = 3;

        private int mCallType;
        private Person mPerson;
        private PendingIntent mAnswerIntent;
        private PendingIntent mDeclineIntent;
        private PendingIntent mHangUpIntent;
        private boolean mIsVideo;
        private Integer mAnswerButtonColor;
        private Integer mDeclineButtonColor;
        private Icon mVerificationIcon;
        private CharSequence mVerificationText;

        CallStyle()
        {
        }

        private CallStyle(int callType, Person person, PendingIntent hangUpIntent, PendingIntent declineIntent,
                          PendingIntent answerIntent)
        {
            if (person == null || person.getName() == null || person.getName().length() == 0)
            {
                throw new IllegalArgumentException("person must have a non-empty a name");
            }

            mCallType = callType;
            mPerson = person;
            mHangUpIntent = hangUpIntent;
            mDeclineIntent = declineIntent;
            mAnswerIntent = answerIntent;
        }

        private static <T> T requireNonNull(T value, String message)
        {
            if (value == null) throw new NullPointerException(message);
            return value;
        }

        public static CallStyle forIncomingCall(Person person, PendingIntent declineIntent, PendingIntent answerIntent)
        {
            return new CallStyle(CALL_TYPE_INCOMING, person, null, requireNonNull(declineIntent, "declineIntent is required"),
                                 requireNonNull(answerIntent, "answerIntent is required"));
        }

        public static CallStyle forOngoingCall(Person person, PendingIntent hangUpIntent)
        {
            return new CallStyle(CALL_TYPE_ONGOING, person, requireNonNull(hangUpIntent, "hangUpIntent is required"), null,
                                 null);
        }

        public static CallStyle forScreeningCall(Person person, PendingIntent hangUpIntent, PendingIntent answerIntent)
        {
            return new CallStyle(CALL_TYPE_SCREENING, person, requireNonNull(hangUpIntent, "hangUpIntent is required"), null,
                                 requireNonNull(answerIntent, "answerIntent is required"));
        }

        public CallStyle setIsVideo(boolean isVideo)
        {
            mIsVideo = isVideo;
            return this;
        }

        public CallStyle setVerificationIcon(Icon verificationIcon)
        {
            mVerificationIcon = verificationIcon;
            return this;
        }

        public CallStyle setVerificationText(CharSequence verificationText)
        {
            mVerificationText = verificationText;
            return this;
        }

        public CallStyle setAnswerButtonColorHint(int color)
        {
            mAnswerButtonColor = color;
            return this;
        }

        public CallStyle setDeclineButtonColorHint(int color)
        {
            mDeclineButtonColor = color;
            return this;
        }

        @Override
        void addExtras(Bundle extras)
        {
            super.addExtras(extras);

            extras.putInt(EXTRA_CALL_TYPE, mCallType);
            extras.putBoolean(EXTRA_CALL_IS_VIDEO, mIsVideo);
            if (mPerson != null) extras.putParcelable(EXTRA_CALL_PERSON, mPerson);
            if (mVerificationIcon != null) extras.putParcelable(EXTRA_VERIFICATION_ICON, mVerificationIcon);
            if (mVerificationText != null) extras.putCharSequence(EXTRA_VERIFICATION_TEXT, mVerificationText);
            if (mAnswerIntent != null) extras.putParcelable(EXTRA_ANSWER_INTENT, mAnswerIntent);
            if (mDeclineIntent != null) extras.putParcelable(EXTRA_DECLINE_INTENT, mDeclineIntent);
            if (mHangUpIntent != null) extras.putParcelable(EXTRA_HANG_UP_INTENT, mHangUpIntent);
            if (mAnswerButtonColor != null) extras.putInt(EXTRA_ANSWER_COLOR, mAnswerButtonColor);
            if (mDeclineButtonColor != null) extras.putInt(EXTRA_DECLINE_COLOR, mDeclineButtonColor);
        }

        @Override
        void restoreFromExtras(Bundle extras)
        {
            super.restoreFromExtras(extras);

            mCallType = extras.getInt(EXTRA_CALL_TYPE);
            mIsVideo = extras.getBoolean(EXTRA_CALL_IS_VIDEO);
            mPerson = extras.getParcelable(EXTRA_CALL_PERSON);
            mVerificationIcon = extras.getParcelable(EXTRA_VERIFICATION_ICON);
            mVerificationText = extras.getCharSequence(EXTRA_VERIFICATION_TEXT);
            mAnswerIntent = extras.getParcelable(EXTRA_ANSWER_INTENT);
            mDeclineIntent = extras.getParcelable(EXTRA_DECLINE_INTENT);
            mHangUpIntent = extras.getParcelable(EXTRA_HANG_UP_INTENT);
            mAnswerButtonColor = extras.containsKey(EXTRA_ANSWER_COLOR) ? extras.getInt(EXTRA_ANSWER_COLOR) : null;
            mDeclineButtonColor = extras.containsKey(EXTRA_DECLINE_COLOR) ? extras.getInt(EXTRA_DECLINE_COLOR) : null;
        }
    }

    public static class MessagingStyle extends Style
    {
        public static final int MAXIMUM_RETAINED_MESSAGES = 25;

        private Person mUser;
        private CharSequence mConversationTitle;
        private final List<Message> mMessages = new ArrayList<>();
        private final List<Message> mHistoricMessages = new ArrayList<>();
        private boolean mIsGroupConversation;

        @Deprecated
        public MessagingStyle(CharSequence userDisplayName)
        {
            this(new Person.Builder().setName(userDisplayName).build());
        }

        public MessagingStyle(Person user)
        {
            mUser = user;
        }

        public Person getUser()
        {
            return mUser;
        }

        public CharSequence getUserDisplayName()
        {
            return mUser == null ? null : mUser.getName();
        }

        public MessagingStyle setConversationTitle(CharSequence conversationTitle)
        {
            mConversationTitle = conversationTitle;
            return this;
        }

        public CharSequence getConversationTitle()
        {
            return mConversationTitle;
        }

        @Deprecated
        public MessagingStyle addMessage(CharSequence text, long timestamp, CharSequence sender)
        {
            return addMessage(text, timestamp, sender == null ? null : new Person.Builder().setName(sender).build());
        }

        public MessagingStyle addMessage(CharSequence text, long timestamp, Person sender)
        {
            return addMessage(new Message(text, timestamp, sender));
        }

        public MessagingStyle addMessage(Message message)
        {
            mMessages.add(message);
            if (mMessages.size() > MAXIMUM_RETAINED_MESSAGES) mMessages.remove(0);
            return this;
        }

        public MessagingStyle addHistoricMessage(Message message)
        {
            mHistoricMessages.add(message);
            if (mHistoricMessages.size() > MAXIMUM_RETAINED_MESSAGES) mHistoricMessages.remove(0);
            return this;
        }

        public List<Message> getMessages()
        {
            return mMessages;
        }

        public List<Message> getHistoricMessages()
        {
            return mHistoricMessages;
        }

        public MessagingStyle setGroupConversation(boolean isGroupConversation)
        {
            mIsGroupConversation = isGroupConversation;
            return this;
        }

        public boolean isGroupConversation()
        {
            return mIsGroupConversation;
        }

        @Override
        void addExtras(Bundle extras)
        {
            super.addExtras(extras);

            if (mUser != null)
            {
                extras.putCharSequence(EXTRA_SELF_DISPLAY_NAME, mUser.getName());
                extras.putParcelable(EXTRA_MESSAGING_PERSON, mUser);
            }

            if (mConversationTitle != null) extras.putCharSequence(EXTRA_CONVERSATION_TITLE, mConversationTitle);
            if (!mMessages.isEmpty()) extras.putParcelableArray(EXTRA_MESSAGES, Message.getBundleArrayForMessages(mMessages));
            if (!mHistoricMessages.isEmpty())
            {
                extras.putParcelableArray(EXTRA_HISTORIC_MESSAGES, Message.getBundleArrayForMessages(mHistoricMessages));
            }

            extras.putBoolean(EXTRA_IS_GROUP_CONVERSATION, mIsGroupConversation);
        }

        @Override
        void restoreFromExtras(Bundle extras)
        {
            super.restoreFromExtras(extras);

            Person user = extras.getParcelable(EXTRA_MESSAGING_PERSON);
            if (user == null)
            {
                CharSequence displayName = extras.getCharSequence(EXTRA_SELF_DISPLAY_NAME);
                user = new Person.Builder().setName(displayName).build();
            }

            mUser = user;
            mConversationTitle = extras.getCharSequence(EXTRA_CONVERSATION_TITLE);
            mMessages.clear();
            mMessages.addAll(Message.getMessagesFromBundleArray(extras.getParcelableArray(EXTRA_MESSAGES)));
            mHistoricMessages.clear();
            mHistoricMessages.addAll(Message.getMessagesFromBundleArray(extras.getParcelableArray(EXTRA_HISTORIC_MESSAGES)));
            mIsGroupConversation = extras.getBoolean(EXTRA_IS_GROUP_CONVERSATION);
        }

        public static final class Message
        {
            private static final String KEY_TEXT = "text";
            private static final String KEY_TIMESTAMP = "time";
            private static final String KEY_SENDER = "sender";
            private static final String KEY_SENDER_PERSON = "sender_person";
            private static final String KEY_DATA_MIME_TYPE = "type";
            private static final String KEY_DATA_URI = "uri";
            private static final String KEY_EXTRAS_BUNDLE = "extras";

            private final CharSequence mText;
            private final long mTimestamp;
            private final Person mSender;
            private final Bundle mExtras = new Bundle();
            private String mDataMimeType;
            private Uri mDataUri;

            @Deprecated
            public Message(CharSequence text, long timestamp, CharSequence sender)
            {
                this(text, timestamp, sender == null ? null : new Person.Builder().setName(sender).build());
            }

            public Message(CharSequence text, long timestamp, Person sender)
            {
                mText = text;
                mTimestamp = timestamp;
                mSender = sender;
            }

            public Message setData(String dataMimeType, Uri dataUri)
            {
                mDataMimeType = dataMimeType;
                mDataUri = dataUri;
                return this;
            }

            public CharSequence getText()
            {
                return mText;
            }

            public long getTimestamp()
            {
                return mTimestamp;
            }

            public Bundle getExtras()
            {
                return mExtras;
            }

            @Deprecated
            public CharSequence getSender()
            {
                return mSender == null ? null : mSender.getName();
            }

            public Person getSenderPerson()
            {
                return mSender;
            }

            public String getDataMimeType()
            {
                return mDataMimeType;
            }

            public Uri getDataUri()
            {
                return mDataUri;
            }

            Bundle toBundle()
            {
                Bundle bundle = new Bundle();
                if (mText != null) bundle.putCharSequence(KEY_TEXT, mText);
                bundle.putLong(KEY_TIMESTAMP, mTimestamp);

                if (mSender != null)
                {
                    bundle.putCharSequence(KEY_SENDER, mSender.getName());
                    bundle.putParcelable(KEY_SENDER_PERSON, mSender);
                }

                if (mDataMimeType != null) bundle.putString(KEY_DATA_MIME_TYPE, mDataMimeType);
                if (mDataUri != null) bundle.putParcelable(KEY_DATA_URI, mDataUri);
                if (!mExtras.isEmpty()) bundle.putBundle(KEY_EXTRAS_BUNDLE, mExtras);

                return bundle;
            }

            static Bundle[] getBundleArrayForMessages(List<Message> messages)
            {
                Bundle[] bundles = new Bundle[messages.size()];
                for (int index = 0; index < bundles.length; index++) bundles[index] = messages.get(index).toBundle();
                return bundles;
            }

            public static List<Message> getMessagesFromBundleArray(Parcelable[] bundles)
            {
                List<Message> messages = new ArrayList<>();
                if (bundles == null) return messages;

                for (Parcelable parcelable : bundles)
                {
                    if (!(parcelable instanceof Bundle)) continue;

                    Message message = getMessageFromBundle((Bundle)parcelable);
                    if (message != null) messages.add(message);
                }

                return messages;
            }

            static Message getMessageFromBundle(Bundle bundle)
            {
                if (!bundle.containsKey(KEY_TEXT) || !bundle.containsKey(KEY_TIMESTAMP)) return null;

                Person sender = bundle.getParcelable(KEY_SENDER_PERSON);
                if (sender == null)
                {
                    CharSequence senderName = bundle.getCharSequence(KEY_SENDER);
                    if (senderName != null) sender = new Person.Builder().setName(senderName).build();
                }

                Message message = new Message(bundle.getCharSequence(KEY_TEXT), bundle.getLong(KEY_TIMESTAMP), sender);

                if (bundle.containsKey(KEY_DATA_MIME_TYPE) && bundle.containsKey(KEY_DATA_URI))
                {
                    message.setData(bundle.getString(KEY_DATA_MIME_TYPE), (Uri)bundle.getParcelable(KEY_DATA_URI));
                }

                Bundle extras = bundle.getBundle(KEY_EXTRAS_BUNDLE);
                if (extras != null) message.getExtras().putAll(extras);

                return message;
            }
        }
    }

    public static final class BubbleMetadata implements Parcelable
    {
        private static final int FLAG_AUTO_EXPAND_BUBBLE = 0x00000001;
        private static final int FLAG_SUPPRESS_NOTIFICATION = 0x00000002;
        private static final int FLAG_SUPPRESSABLE_BUBBLE = 0x00000004;
        private static final int FLAG_SUPPRESS_BUBBLE = 0x00000008;

        public static final Parcelable.Creator<BubbleMetadata> CREATOR = new Parcelable.Creator<BubbleMetadata>()
        {
            @Override
            public BubbleMetadata createFromParcel(Parcel source)
            {
                BubbleMetadata data = new BubbleMetadata();
                data.mPendingIntent = (PendingIntent)source.readValue(null);
                data.mIcon = (Icon)source.readValue(null);
                data.mDesiredHeight = source.readInt();
                data.mDesiredHeightResId = source.readInt();
                data.mDeleteIntent = (PendingIntent)source.readValue(null);
                data.mFlags = source.readInt();
                data.mShortcutId = source.readString();
                return data;
            }

            @Override
            public BubbleMetadata[] newArray(int size)
            {
                return new BubbleMetadata[size];
            }
        };

        private PendingIntent mPendingIntent;
        private PendingIntent mDeleteIntent;
        private Icon mIcon;
        private int mDesiredHeight;
        private int mDesiredHeightResId;
        private int mFlags;
        private String mShortcutId;

        BubbleMetadata()
        {
        }

        public String getShortcutId()
        {
            return mShortcutId;
        }

        public PendingIntent getIntent()
        {
            return mPendingIntent;
        }

        public PendingIntent getDeleteIntent()
        {
            return mDeleteIntent;
        }

        public Icon getIcon()
        {
            return mIcon;
        }

        public int getDesiredHeight()
        {
            return mDesiredHeight;
        }

        public int getDesiredHeightResId()
        {
            return mDesiredHeightResId;
        }

        public boolean getAutoExpandBubble()
        {
            return (mFlags & FLAG_AUTO_EXPAND_BUBBLE) != 0;
        }

        public boolean isNotificationSuppressed()
        {
            return (mFlags & FLAG_SUPPRESS_NOTIFICATION) != 0;
        }

        public boolean isBubbleSuppressable()
        {
            return (mFlags & FLAG_SUPPRESSABLE_BUBBLE) != 0;
        }

        public boolean isBubbleSuppressed()
        {
            return (mFlags & FLAG_SUPPRESS_BUBBLE) != 0;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags)
        {
            parcel.writeValue(mPendingIntent);
            parcel.writeValue(mIcon);
            parcel.writeInt(mDesiredHeight);
            parcel.writeInt(mDesiredHeightResId);
            parcel.writeValue(mDeleteIntent);
            parcel.writeInt(mFlags);
            parcel.writeString(mShortcutId);
        }

        public static final class Builder
        {
            private PendingIntent mPendingIntent;
            private Icon mIcon;
            private int mDesiredHeight;
            private int mDesiredHeightResId;
            private int mFlags;
            private PendingIntent mDeleteIntent;
            private String mShortcutId;

            @Deprecated
            public Builder()
            {
            }

            public Builder(String shortcutId)
            {
                if (shortcutId == null || shortcutId.isEmpty())
                {
                    throw new NullPointerException("Bubble requires a non-null shortcut id");
                }

                mShortcutId = shortcutId;
            }

            public Builder(PendingIntent intent, Icon icon)
            {
                if (intent == null) throw new NullPointerException("Bubble requires non-null pending intent");
                if (icon == null) throw new NullPointerException("Bubbles require non-null icon");

                if (icon.getType() != Icon.TYPE_URI_ADAPTIVE_BITMAP && icon.getType() != Icon.TYPE_URI)
                {
                    android.util.Log.w("Notification", "Bubbles work best with icons of TYPE_URI or TYPE_URI_ADAPTIVE_BITMAP.");
                }

                mPendingIntent = intent;
                mIcon = icon;
            }

            public Builder setIntent(PendingIntent intent)
            {
                if (mShortcutId != null)
                {
                    throw new IllegalStateException("Created as a shortcut bubble, cannot set a PendingIntent. "
                                                    + "Consider using BubbleMetadata.Builder(PendingIntent,Icon) instead.");
                }

                if (intent == null) throw new NullPointerException("Bubble requires non-null pending intent");

                mPendingIntent = intent;
                return this;
            }

            public Builder setIcon(Icon icon)
            {
                if (mShortcutId != null)
                {
                    throw new IllegalStateException("Created as a shortcut bubble, cannot set an Icon. "
                                                    + "Consider using BubbleMetadata.Builder(PendingIntent,Icon) instead.");
                }

                if (icon == null) throw new NullPointerException("Bubbles require non-null icon");

                mIcon = icon;
                return this;
            }

            public Builder setDesiredHeight(int height)
            {
                mDesiredHeight = Math.max(height, 0);
                mDesiredHeightResId = 0;
                return this;
            }

            public Builder setDesiredHeightResId(int heightResId)
            {
                mDesiredHeightResId = heightResId;
                mDesiredHeight = 0;
                return this;
            }

            public Builder setAutoExpandBubble(boolean shouldExpand)
            {
                setFlag(FLAG_AUTO_EXPAND_BUBBLE, shouldExpand);
                return this;
            }

            public Builder setSuppressNotification(boolean shouldSuppressNotif)
            {
                setFlag(FLAG_SUPPRESS_NOTIFICATION, shouldSuppressNotif);
                return this;
            }

            public Builder setSuppressableBubble(boolean suppressBubble)
            {
                setFlag(FLAG_SUPPRESSABLE_BUBBLE, suppressBubble);
                return this;
            }

            public Builder setDeleteIntent(PendingIntent deleteIntent)
            {
                mDeleteIntent = deleteIntent;
                return this;
            }

            private void setFlag(int mask, boolean value)
            {
                if (value) mFlags |= mask;
                else mFlags &= ~mask;
            }

            public BubbleMetadata build()
            {
                if (mShortcutId == null && mPendingIntent == null)
                {
                    throw new NullPointerException("Must supply pending intent or shortcut to bubble");
                }

                if (mShortcutId == null && mIcon == null)
                {
                    throw new NullPointerException("Must supply an icon or shortcut for the bubble");
                }

                BubbleMetadata data = new BubbleMetadata();
                data.mShortcutId = mShortcutId;
                data.mPendingIntent = mPendingIntent;
                data.mIcon = mIcon;
                data.mDesiredHeight = mDesiredHeight;
                data.mDesiredHeightResId = mDesiredHeightResId;
                data.mDeleteIntent = mDeleteIntent;
                data.mFlags = mFlags;
                return data;
            }
        }
    }
}
