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
    private final int mUid;
    private final int mInitialPid;
    private final long mPostTime;
    private final Notification mNotification;
    private final UserHandle mUser;
    private String mOverrideGroupKey;
    private String mKey;
    private String mGroupKey;

    public StatusBarNotification(String packageName, String opPackageName, int id, String tag, int uid, int initialPid,
                                 int score, Notification notification, UserHandle user, long postTime)
    {
        if (packageName == null) throw new NullPointerException();
        if (notification == null) throw new NullPointerException();

        mPackageName = packageName;
        mOpPackageName = opPackageName;
        mId = id;
        mTag = tag;
        mUid = uid;
        mInitialPid = initialPid;
        mNotification = notification;
        mUser = user == null ? new UserHandle(0) : user;
        mPostTime = postTime;
        updateKeys();
    }

    public StatusBarNotification(Parcel parcel)
    {
        mPackageName = parcel.readString();
        mOpPackageName = parcel.readString();
        mId = parcel.readInt();
        mTag = parcel.readString();
        mUid = parcel.readInt();
        mInitialPid = parcel.readInt();
        Notification notification = parcel.readParcelable(Notification.class.getClassLoader());
        mNotification = notification == null ? new Notification() : notification;
        UserHandle user = parcel.readParcelable(UserHandle.class.getClassLoader());
        mUser = user == null ? new UserHandle(0) : user;
        mPostTime = parcel.readLong();
        mOverrideGroupKey = parcel.readString();
        updateKeys();
    }

    private void updateKeys()
    {
        mKey = mUser.getIdentifier() + "|" + mPackageName + "|" + mId + "|" + mTag + "|" + mUid;

        String group = mNotification.getGroup();
        String sortKey = mNotification.getSortKey();

        if (mOverrideGroupKey != null) mGroupKey = mUser.getIdentifier() + "|" + mPackageName + "|g:" + mOverrideGroupKey;
        else if (group == null && sortKey == null) mGroupKey = mKey;
        else if (group != null) mGroupKey = mUser.getIdentifier() + "|" + mPackageName + "|g:" + group;
        else mGroupKey = mUser.getIdentifier() + "|" + mPackageName + "|p:" + mNotification.priority;
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

    public int getUid()
    {
        return mUid;
    }

    public String getKey()
    {
        return mKey;
    }

    public String getGroupKey()
    {
        return mGroupKey;
    }

    public String getOverrideGroupKey()
    {
        return mOverrideGroupKey;
    }

    public void setOverrideGroupKey(String overrideGroupKey)
    {
        mOverrideGroupKey = overrideGroupKey;
        updateKeys();
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

    @Deprecated
    public int getUserId()
    {
        return mUser.getIdentifier();
    }

    public boolean isClearable()
    {
        return (mNotification.flags & Notification.FLAG_ONGOING_EVENT) == 0
               && (mNotification.flags & Notification.FLAG_NO_CLEAR) == 0;
    }

    public boolean isOngoing()
    {
        return (mNotification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
    }

    public boolean isGroup()
    {
        return mOverrideGroupKey != null || isAppGroup();
    }

    public boolean isAppGroup()
    {
        return mNotification.getGroup() != null || mNotification.getSortKey() != null;
    }

    @Override
    public StatusBarNotification clone()
    {
        StatusBarNotification cloned = new StatusBarNotification(mPackageName, mOpPackageName, mId, mTag, mUid, mInitialPid,
                                                                 0, mNotification.clone(), mUser, mPostTime);
        cloned.setOverrideGroupKey(mOverrideGroupKey);
        return cloned;
    }

    @Override
    public String toString()
    {
        return "StatusBarNotification(pkg=" + mPackageName + " user=" + mUser + " id=" + mId + " tag=" + mTag + " key=" + mKey
               + ": " + mNotification + ")";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeString(mPackageName);
        parcel.writeString(mOpPackageName);
        parcel.writeInt(mId);
        parcel.writeString(mTag);
        parcel.writeInt(mUid);
        parcel.writeInt(mInitialPid);
        parcel.writeParcelable(mNotification, flags);
        parcel.writeParcelable(mUser, flags);
        parcel.writeLong(mPostTime);
        parcel.writeString(mOverrideGroupKey);
    }

    public static final Creator<StatusBarNotification> CREATOR = new Creator<StatusBarNotification>()
    {
        @Override
        public StatusBarNotification createFromParcel(Parcel parcel)
        {
            return new StatusBarNotification(parcel);
        }

        @Override
        public StatusBarNotification[] newArray(int size)
        {
            return new StatusBarNotification[size];
        }
    };
}
