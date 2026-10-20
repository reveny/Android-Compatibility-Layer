package android.app;

import android.content.Context;

public class Notification
{
    public static final int FLAG_ONGOING_EVENT = 0x00000002;
    public static final int FLAG_AUTO_CANCEL = 0x00000010;
    public static final int FLAG_NO_CLEAR = 0x00000020;

    public static final int PRIORITY_MIN = -2;
    public static final int PRIORITY_LOW = -1;
    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_MAX = 2;

    public static final int DEFAULT_ALL = ~0;
    public static final int DEFAULT_SOUND = 1;
    public static final int DEFAULT_VIBRATE = 2;
    public static final int DEFAULT_LIGHTS = 4;

    public int icon;
    public int flags;
    public int priority;
    public long when;
    public CharSequence tickerText;
    public PendingIntent contentIntent;
    public PendingIntent deleteIntent;
    public String category;
    public android.os.Bundle extras = new android.os.Bundle();

    private String mChannelId;
    private CharSequence mTitle;
    private CharSequence mText;

    public String getChannelId()
    {
        return mChannelId;
    }

    void setChannelId(String channelId)
    {
        mChannelId = channelId;
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    void setTitle(CharSequence title)
    {
        mTitle = title;
    }

    public CharSequence getText()
    {
        return mText;
    }

    void setText(CharSequence text)
    {
        mText = text;
    }

    @Override
    public String toString()
    {
        return "Notification{channel=" + mChannelId + " title=" + mTitle + " text=" + mText + "}";
    }

    public static class Builder
    {
        private final Notification mNotification = new Notification();

        public Builder(Context context, String channelId)
        {
            mNotification.setChannelId(channelId);
            mNotification.when = System.currentTimeMillis();
        }

        public Builder(Context context)
        {
            this(context, null);
        }

        public Builder setContentTitle(CharSequence title)
        {
            mNotification.setTitle(title);
            return this;
        }

        public Builder setContentText(CharSequence text)
        {
            mNotification.setText(text);
            return this;
        }

        public Builder setSmallIcon(int icon)
        {
            mNotification.icon = icon;
            return this;
        }

        public Builder setChannelId(String channelId)
        {
            mNotification.setChannelId(channelId);
            return this;
        }

        public Builder setPriority(int priority)
        {
            mNotification.priority = priority;
            return this;
        }

        public Builder setAutoCancel(boolean autoCancel)
        {
            if (autoCancel) mNotification.flags |= FLAG_AUTO_CANCEL;
            else mNotification.flags &= ~FLAG_AUTO_CANCEL;

            return this;
        }

        public Builder setOngoing(boolean ongoing)
        {
            if (ongoing) mNotification.flags |= FLAG_ONGOING_EVENT;
            else mNotification.flags &= ~FLAG_ONGOING_EVENT;

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

        public Builder setWhen(long when)
        {
            mNotification.when = when;
            return this;
        }

        public Builder setTicker(CharSequence ticker)
        {
            mNotification.tickerText = ticker;
            return this;
        }

        public Builder setCategory(String category)
        {
            mNotification.category = category;
            return this;
        }

        public Builder setDefaults(int defaults)
        {
            return this;
        }

        public Notification build()
        {
            return mNotification;
        }
    }
}
