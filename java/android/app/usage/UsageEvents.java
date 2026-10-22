package android.app.usage;

import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public final class UsageEvents implements Parcelable
{
    public static final Parcelable.Creator<UsageEvents> CREATOR = new Parcelable.Creator<UsageEvents>()
    {
        @Override
        public UsageEvents createFromParcel(Parcel source)
        {
            UsageEvents events = new UsageEvents();
            int count = source.readInt();

            for (int index = 0; index < count; index++)
            {
                Event event = new Event();
                event.mPackage = source.readString();
                event.mClass = source.readString();
                event.mTimeStamp = source.readLong();
                event.mEventType = source.readInt();
                event.mShortcutId = source.readString();
                event.mBucket = source.readInt();
                events.mEvents.add(event);
            }

            return events;
        }

        @Override
        public UsageEvents[] newArray(int size)
        {
            return new UsageEvents[size];
        }
    };

    private final List<Event> mEvents = new ArrayList<>();
    private int mIndex;

    UsageEvents()
    {
    }

    public boolean hasNextEvent()
    {
        return mIndex < mEvents.size();
    }

    public boolean getNextEvent(Event eventOut)
    {
        if (eventOut == null) throw new IllegalArgumentException("Given eventOut must not be null");
        if (mIndex >= mEvents.size()) return false;

        eventOut.copyFrom(mEvents.get(mIndex++));
        return true;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mEvents.size() - mIndex);

        for (int index = mIndex; index < mEvents.size(); index++)
        {
            Event event = mEvents.get(index);
            dest.writeString(event.mPackage);
            dest.writeString(event.mClass);
            dest.writeLong(event.mTimeStamp);
            dest.writeInt(event.mEventType);
            dest.writeString(event.mShortcutId);
            dest.writeInt(event.mBucket);
        }
    }

    public static final class Event
    {
        public static final int NONE = 0;
        public static final int MOVE_TO_FOREGROUND = 1;
        public static final int ACTIVITY_RESUMED = 1;
        public static final int MOVE_TO_BACKGROUND = 2;
        public static final int ACTIVITY_PAUSED = 2;
        public static final int CONFIGURATION_CHANGE = 5;
        public static final int USER_INTERACTION = 7;
        public static final int SHORTCUT_INVOCATION = 8;
        public static final int STANDBY_BUCKET_CHANGED = 11;
        public static final int SCREEN_INTERACTIVE = 15;
        public static final int SCREEN_NON_INTERACTIVE = 16;
        public static final int KEYGUARD_SHOWN = 17;
        public static final int KEYGUARD_HIDDEN = 18;
        public static final int FOREGROUND_SERVICE_START = 19;
        public static final int FOREGROUND_SERVICE_STOP = 20;
        public static final int ACTIVITY_STOPPED = 23;
        public static final int DEVICE_SHUTDOWN = 26;
        public static final int DEVICE_STARTUP = 27;

        String mPackage;
        String mClass;
        long mTimeStamp;
        int mEventType;
        Configuration mConfiguration;
        String mShortcutId;
        int mBucket;

        public Event()
        {
        }

        void copyFrom(Event other)
        {
            mPackage = other.mPackage;
            mClass = other.mClass;
            mTimeStamp = other.mTimeStamp;
            mEventType = other.mEventType;
            mConfiguration = other.mConfiguration;
            mShortcutId = other.mShortcutId;
            mBucket = other.mBucket;
        }

        public String getPackageName()
        {
            return mPackage;
        }

        public String getClassName()
        {
            return mClass;
        }

        public long getTimeStamp()
        {
            return mTimeStamp;
        }

        public int getEventType()
        {
            return mEventType;
        }

        public Configuration getConfiguration()
        {
            return mConfiguration;
        }

        public String getShortcutId()
        {
            return mShortcutId;
        }

        public int getAppStandbyBucket()
        {
            return mBucket;
        }
    }
}
