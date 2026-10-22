package android.media;

import android.os.Parcel;
import android.os.Parcelable;

public class MediaSyncEvent implements Parcelable
{
    public static final int SYNC_EVENT_NONE = 0;
    public static final int SYNC_EVENT_PRESENTATION_COMPLETE = 1;

    public static final Parcelable.Creator<MediaSyncEvent> CREATOR = new Parcelable.Creator<MediaSyncEvent>()
    {
        @Override
        public MediaSyncEvent createFromParcel(Parcel source)
        {
            MediaSyncEvent event = new MediaSyncEvent(source.readInt());
            event.mAudioSessionId = source.readInt();
            return event;
        }

        @Override
        public MediaSyncEvent[] newArray(int size)
        {
            return new MediaSyncEvent[size];
        }
    };

    private final int mType;
    private int mAudioSessionId;

    private MediaSyncEvent(int type)
    {
        mType = type;
    }

    public static MediaSyncEvent createEvent(int type) throws IllegalArgumentException
    {
        if (type != SYNC_EVENT_NONE && type != SYNC_EVENT_PRESENTATION_COMPLETE)
        {
            throw new IllegalArgumentException(type + " is not a valid MediaSyncEvent type.");
        }

        return new MediaSyncEvent(type);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof MediaSyncEvent)) return false;

        MediaSyncEvent other = (MediaSyncEvent)object;
        return mType == other.mType && mAudioSessionId == other.mAudioSessionId;
    }

    public int getAudioSessionId()
    {
        return mAudioSessionId;
    }

    public int getType()
    {
        return mType;
    }

    @Override
    public int hashCode()
    {
        return java.util.Objects.hash(mType, mAudioSessionId);
    }

    public MediaSyncEvent setAudioSessionId(int audioSessionId) throws IllegalArgumentException
    {
        if (audioSessionId <= 0) throw new IllegalArgumentException(audioSessionId + " is not a valid session ID.");
        mAudioSessionId = audioSessionId;
        return this;
    }

    @Override
    public String toString()
    {
        return "MediaSyncEvent: type=" + (mType == SYNC_EVENT_NONE ? "NONE" : "PRESENTATION_COMPLETE")
            + " session=" + mAudioSessionId;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mType);
        destination.writeInt(mAudioSessionId);
    }
}
