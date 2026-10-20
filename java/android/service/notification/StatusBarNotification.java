package android.service.notification;

import android.app.Notification;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

public class StatusBarNotification implements Parcelable
{
    private final String mPackageName;
    private final String mOpPackageName;
    private final int mId;
    private final String mTag;
    private final String mKey;
    private final long mPostTime;
    private final Notification mNotification;
    private final UserHandle mUser;

    public StatusBarNotification(String packageName, String opPackageName, int id, String tag, int uid, int initialPid,
                                 Notification notification, UserHandle user, String overrideGroupKey, long postTime)
    {
        mPackageName = packageName;
        mOpPackageName = opPackageName;
        mId = id;
        mTag = tag;
        mNotification = notification;
        mUser = user;
        mPostTime = postTime;
        mKey = packageName + "|" + id + "|" + (tag == null ? "" : tag);
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    public String getOpPkg()
    {
        return mOpPackageName;
    }

    public int getId()
    {
        return mId;
    }

    public String getTag()
    {
        return mTag;
    }

    public String getKey()
    {
        return mKey;
    }

    public String getGroupKey()
    {
        return mKey;
    }

    public long getPostTime()
    {
        return mPostTime;
    }

    public Notification getNotification()
    {
        return mNotification;
    }

    public UserHandle getUser()
    {
        return mUser;
    }

    public boolean isClearable()
    {
        return mNotification == null || (mNotification.flags & Notification.FLAG_NO_CLEAR) == 0;
    }

    public boolean isOngoing()
    {
        return mNotification != null && (mNotification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
    }

    public boolean isGroup()
    {
        return false;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeValue(mPackageName);
        parcel.writeValue(Integer.valueOf(mId));
        parcel.writeValue(mTag);
    }

    public static final Creator<StatusBarNotification> CREATOR = new Creator<StatusBarNotification>()
    {
        @Override
        public StatusBarNotification createFromParcel(Parcel parcel)
        {
            String packageName = (String)parcel.readValue(null);
            Object id = parcel.readValue(null);
            String tag = (String)parcel.readValue(null);

            return new StatusBarNotification(packageName, packageName, id instanceof Integer ? ((Integer)id).intValue() : 0, tag,
                                             0, 0, null, null, null, 0);
        }

        @Override
        public StatusBarNotification[] newArray(int size)
        {
            return new StatusBarNotification[size];
        }
    };
}
