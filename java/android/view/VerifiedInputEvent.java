package android.view;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public abstract class VerifiedInputEvent implements Parcelable
{
    static final int VERIFIED_KEY = 1;
    static final int VERIFIED_MOTION = 2;

    public static final Parcelable.Creator<VerifiedInputEvent> CREATOR = new Parcelable.Creator<VerifiedInputEvent>()
    {
        @Override
        public VerifiedInputEvent createFromParcel(Parcel in)
        {
            int type = in.readInt();
            if (type == VERIFIED_KEY) return new VerifiedKeyEvent(in);
            if (type == VERIFIED_MOTION) return new VerifiedMotionEvent(in);

            throw new IllegalArgumentException("Unexpected input event type in parcel.");
        }

        @Override
        public VerifiedInputEvent[] newArray(int size)
        {
            return new VerifiedInputEvent[size];
        }
    };

    private final int mType;
    private final int mDeviceId;
    private final long mEventTimeNanos;
    private final int mSource;
    private final int mDisplayId;

    VerifiedInputEvent(int type, int deviceId, long eventTimeNanos, int source, int displayId)
    {
        mType = type;
        mDeviceId = deviceId;
        mEventTimeNanos = eventTimeNanos;
        mSource = source;
        mDisplayId = displayId;
    }

    VerifiedInputEvent(Parcel in, int expectedType)
    {
        mType = expectedType;
        mDeviceId = in.readInt();
        mEventTimeNanos = in.readLong();
        mSource = in.readInt();
        mDisplayId = in.readInt();
    }

    public int getDeviceId()
    {
        return mDeviceId;
    }

    public long getEventTimeNanos()
    {
        return mEventTimeNanos;
    }

    public int getSource()
    {
        return mSource;
    }

    public int getDisplayId()
    {
        return mDisplayId;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mType);
        dest.writeInt(mDeviceId);
        dest.writeLong(mEventTimeNanos);
        dest.writeInt(mSource);
        dest.writeInt(mDisplayId);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        VerifiedInputEvent event = (VerifiedInputEvent)other;
        return mType == event.mType && mDeviceId == event.mDeviceId && mEventTimeNanos == event.mEventTimeNanos && mSource == event.mSource
               && mDisplayId == event.mDisplayId;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mType, mDeviceId, mEventTimeNanos, mSource, mDisplayId);
    }
}
