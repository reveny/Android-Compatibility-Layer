package android.view;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class VerifiedMotionEvent extends VerifiedInputEvent implements Parcelable
{
    public static final Parcelable.Creator<VerifiedMotionEvent> CREATOR = new Parcelable.Creator<VerifiedMotionEvent>()
    {
        @Override
        public VerifiedMotionEvent createFromParcel(Parcel in)
        {
            if (in.readInt() != VERIFIED_MOTION) throw new IllegalArgumentException("the parcel does not hold a verified motion event");

            return new VerifiedMotionEvent(in);
        }

        @Override
        public VerifiedMotionEvent[] newArray(int size)
        {
            return new VerifiedMotionEvent[size];
        }
    };

    private final float mRawX;
    private final float mRawY;
    private final int mActionMasked;
    private final long mDownTimeNanos;
    private final int mFlags;
    private final int mMetaState;
    private final int mButtonState;

    public VerifiedMotionEvent(int deviceId, long eventTimeNanos, int source, int displayId, float rawX, float rawY, int actionMasked,
                               long downTimeNanos, int flags, int metaState, int buttonState)
    {
        super(VERIFIED_MOTION, deviceId, eventTimeNanos, source, displayId);

        mRawX = rawX;
        mRawY = rawY;
        mActionMasked = actionMasked;
        mDownTimeNanos = downTimeNanos;
        mFlags = flags;
        mMetaState = metaState;
        mButtonState = buttonState;
    }

    VerifiedMotionEvent(Parcel in)
    {
        super(in, VERIFIED_MOTION);

        mRawX = in.readFloat();
        mRawY = in.readFloat();
        mActionMasked = in.readInt();
        mDownTimeNanos = in.readLong();
        mFlags = in.readInt();
        mMetaState = in.readInt();
        mButtonState = in.readInt();
    }

    public float getRawX()
    {
        return mRawX;
    }

    public float getRawY()
    {
        return mRawY;
    }

    public int getActionMasked()
    {
        return mActionMasked;
    }

    public long getDownTimeNanos()
    {
        return mDownTimeNanos;
    }

    public int getMetaState()
    {
        return mMetaState;
    }

    public int getButtonState()
    {
        return mButtonState;
    }

    public Boolean getFlag(int flag)
    {
        switch (flag)
        {
            case MotionEvent.FLAG_WINDOW_IS_OBSCURED:
            case MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED:
                return (mFlags & flag) != 0;
            default:
                return null;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        super.writeToParcel(dest, flags);

        dest.writeFloat(mRawX);
        dest.writeFloat(mRawY);
        dest.writeInt(mActionMasked);
        dest.writeLong(mDownTimeNanos);
        dest.writeInt(mFlags);
        dest.writeInt(mMetaState);
        dest.writeInt(mButtonState);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!super.equals(other)) return false;

        VerifiedMotionEvent event = (VerifiedMotionEvent)other;
        return mRawX == event.mRawX && mRawY == event.mRawY && mActionMasked == event.mActionMasked && mDownTimeNanos == event.mDownTimeNanos
               && mFlags == event.mFlags && mMetaState == event.mMetaState && mButtonState == event.mButtonState;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), mRawX, mRawY, mActionMasked, mDownTimeNanos, mFlags, mMetaState, mButtonState);
    }
}
