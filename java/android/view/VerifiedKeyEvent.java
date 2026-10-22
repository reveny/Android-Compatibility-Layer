package android.view;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class VerifiedKeyEvent extends VerifiedInputEvent implements Parcelable
{
    public static final Parcelable.Creator<VerifiedKeyEvent> CREATOR = new Parcelable.Creator<VerifiedKeyEvent>()
    {
        @Override
        public VerifiedKeyEvent createFromParcel(Parcel in)
        {
            if (in.readInt() != VERIFIED_KEY) throw new IllegalArgumentException("the parcel does not hold a verified key event");

            return new VerifiedKeyEvent(in);
        }

        @Override
        public VerifiedKeyEvent[] newArray(int size)
        {
            return new VerifiedKeyEvent[size];
        }
    };

    private final int mAction;
    private final long mDownTimeNanos;
    private final int mFlags;
    private final int mKeyCode;
    private final int mScanCode;
    private final int mMetaState;
    private final int mRepeatCount;

    public VerifiedKeyEvent(int deviceId, long eventTimeNanos, int source, int displayId, int action, long downTimeNanos, int flags,
                            int keyCode, int scanCode, int metaState, int repeatCount)
    {
        super(VERIFIED_KEY, deviceId, eventTimeNanos, source, displayId);

        if (action != KeyEvent.ACTION_DOWN && action != KeyEvent.ACTION_UP)
        {
            throw new IllegalArgumentException("Unexpected key event action " + action);
        }

        mAction = action;
        mDownTimeNanos = downTimeNanos;
        mFlags = flags;
        mKeyCode = keyCode;
        mScanCode = scanCode;
        mMetaState = metaState;
        mRepeatCount = repeatCount;
    }

    VerifiedKeyEvent(Parcel in)
    {
        super(in, VERIFIED_KEY);

        mAction = in.readInt();
        mDownTimeNanos = in.readLong();
        mFlags = in.readInt();
        mKeyCode = in.readInt();
        mScanCode = in.readInt();
        mMetaState = in.readInt();
        mRepeatCount = in.readInt();
    }

    public int getAction()
    {
        return mAction;
    }

    public long getDownTimeNanos()
    {
        return mDownTimeNanos;
    }

    public int getKeyCode()
    {
        return mKeyCode;
    }

    public int getScanCode()
    {
        return mScanCode;
    }

    public int getMetaState()
    {
        return mMetaState;
    }

    public int getRepeatCount()
    {
        return mRepeatCount;
    }

    public Boolean getFlag(int flag)
    {
        switch (flag)
        {
            case KeyEvent.FLAG_CANCELED:
                return (mFlags & flag) != 0;
            default:
                return null;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        super.writeToParcel(dest, flags);

        dest.writeInt(mAction);
        dest.writeLong(mDownTimeNanos);
        dest.writeInt(mFlags);
        dest.writeInt(mKeyCode);
        dest.writeInt(mScanCode);
        dest.writeInt(mMetaState);
        dest.writeInt(mRepeatCount);
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

        VerifiedKeyEvent event = (VerifiedKeyEvent)other;
        return mAction == event.mAction && mDownTimeNanos == event.mDownTimeNanos && mFlags == event.mFlags && mKeyCode == event.mKeyCode
               && mScanCode == event.mScanCode && mMetaState == event.mMetaState && mRepeatCount == event.mRepeatCount;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), mAction, mDownTimeNanos, mFlags, mKeyCode, mScanCode, mMetaState, mRepeatCount);
    }
}
