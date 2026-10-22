package android.app;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NotificationChannelGroup implements Parcelable
{
    private static final int MAX_TEXT_LENGTH = 1000;

    public static final Parcelable.Creator<NotificationChannelGroup> CREATOR =
            new Parcelable.Creator<NotificationChannelGroup>()
    {
        @Override
        public NotificationChannelGroup createFromParcel(Parcel source)
        {
            NotificationChannelGroup group = new NotificationChannelGroup(source.readString(),
                                                                          (CharSequence)source.readValue(null));
            group.mDescription = source.readString();
            group.mBlocked = source.readBoolean();

            List<NotificationChannel> channels = new ArrayList<>();
            source.readParcelableList(channels, NotificationChannel.class.getClassLoader());
            group.mChannels.addAll(channels);

            return group;
        }

        @Override
        public NotificationChannelGroup[] newArray(int size)
        {
            return new NotificationChannelGroup[size];
        }
    };

    private final String mId;
    private CharSequence mName;
    private String mDescription;
    private boolean mBlocked;
    private final List<NotificationChannel> mChannels = new ArrayList<>();

    public NotificationChannelGroup(String id, CharSequence name)
    {
        mId = trimmed(id);
        mName = name == null ? null : trimmed(name.toString());
    }

    private static String trimmed(String input)
    {
        if (input != null && input.length() > MAX_TEXT_LENGTH) return input.substring(0, MAX_TEXT_LENGTH);
        return input;
    }

    public String getId()
    {
        return mId;
    }

    public CharSequence getName()
    {
        return mName;
    }

    void setName(CharSequence name)
    {
        mName = name == null ? null : trimmed(name.toString());
    }

    public String getDescription()
    {
        return mDescription;
    }

    public void setDescription(String description)
    {
        mDescription = trimmed(description);
    }

    public List<NotificationChannel> getChannels()
    {
        return mChannels;
    }

    void setChannels(List<NotificationChannel> channels)
    {
        mChannels.clear();
        if (channels != null) mChannels.addAll(channels);
    }

    public boolean isBlocked()
    {
        return mBlocked;
    }

    @Override
    public NotificationChannelGroup clone()
    {
        NotificationChannelGroup cloned = new NotificationChannelGroup(mId, mName);
        cloned.mDescription = mDescription;
        cloned.mBlocked = mBlocked;
        cloned.mChannels.addAll(mChannels);
        return cloned;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeString(mId);
        parcel.writeValue(mName);
        parcel.writeString(mDescription);
        parcel.writeBoolean(mBlocked);
        parcel.writeParcelableList(mChannels, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof NotificationChannelGroup)) return false;

        NotificationChannelGroup that = (NotificationChannelGroup)other;
        return mBlocked == that.mBlocked && Objects.equals(mId, that.mId)
               && Objects.equals(mName == null ? null : mName.toString(), that.mName == null ? null : that.mName.toString())
               && Objects.equals(mDescription, that.mDescription) && Objects.equals(mChannels, that.mChannels);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mId, mName == null ? null : mName.toString(), mDescription, mBlocked, mChannels);
    }

    @Override
    public String toString()
    {
        return "NotificationChannelGroup{mId='" + mId + "', mName=" + mName + ", mDescription="
               + (mDescription == null ? "null" : "hasDescription") + ", mBlocked=" + mBlocked + ", mChannels=" + mChannels
               + "}";
    }
}
