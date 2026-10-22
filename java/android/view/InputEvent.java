package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class InputEvent implements Parcelable
{
    static final int PARCEL_TOKEN_MOTION_EVENT = 1;
    static final int PARCEL_TOKEN_KEY_EVENT = 2;

    public static final Parcelable.Creator<InputEvent> CREATOR = new Parcelable.Creator<InputEvent>()
    {
        @Override
        public InputEvent createFromParcel(Parcel in)
        {
            int token = in.readInt();
            if (token == PARCEL_TOKEN_KEY_EVENT) return KeyEvent.createFromParcelBody(in);
            if (token == PARCEL_TOKEN_MOTION_EVENT) return MotionEvent.createFromParcelBody(in);

            throw new IllegalStateException("Unexpected input event type token in parcel.");
        }

        @Override
        public InputEvent[] newArray(int size)
        {
            return new InputEvent[size];
        }
    };

    InputEvent()
    {
    }

    public abstract int getDeviceId();

    public final InputDevice getDevice()
    {
        return InputDevice.getDevice(getDeviceId());
    }

    public abstract int getSource();

    public abstract long getEventTime();

    public boolean isFromSource(int source)
    {
        return (getSource() & source) == source;
    }

    public void recycle()
    {
    }

    @Override
    public int describeContents()
    {
        return 0;
    }
}
