package android.view;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Parcel;
import android.os.Parcelable;

public class DragEvent implements Parcelable
{
    public static final int ACTION_DRAG_STARTED = 1;
    public static final int ACTION_DRAG_LOCATION = 2;
    public static final int ACTION_DROP = 3;
    public static final int ACTION_DRAG_ENDED = 4;
    public static final int ACTION_DRAG_ENTERED = 5;
    public static final int ACTION_DRAG_EXITED = 6;

    public static final Parcelable.Creator<DragEvent> CREATOR = new Parcelable.Creator<DragEvent>()
    {
        @Override
        public DragEvent createFromParcel(Parcel source)
        {
            int action = source.readInt();
            float x = source.readFloat();
            float y = source.readFloat();
            boolean result = source.readBoolean();
            ClipData clip = source.readInt() != 0 ? ClipData.CREATOR.createFromParcel(source) : null;
            ClipDescription description = clip != null ? clip.getDescription() : null;
            return obtain(action, x, y, null, description, clip, result);
        }

        @Override
        public DragEvent[] newArray(int size)
        {
            return new DragEvent[size];
        }
    };

    private int mAction;
    private float mX;
    private float mY;
    private ClipDescription mClipDescription;
    private ClipData mClipData;
    private Object mLocalState;
    private boolean mResult;

    private DragEvent()
    {
    }

    static DragEvent obtain(int action, float x, float y, Object localState, ClipDescription description, ClipData data, boolean result)
    {
        DragEvent event = new DragEvent();
        event.mAction = action;
        event.mX = x;
        event.mY = y;
        event.mLocalState = localState;
        event.mClipDescription = description;
        event.mClipData = data;
        event.mResult = result;
        return event;
    }

    static DragEvent obtain(DragEvent source)
    {
        return obtain(source.mAction, source.mX, source.mY, source.mLocalState, source.mClipDescription, source.mClipData, source.mResult);
    }

    void setLocation(float x, float y)
    {
        mX = x;
        mY = y;
    }

    void setAction(int action)
    {
        mAction = action;
    }

    public int getAction()
    {
        return mAction;
    }

    public float getX()
    {
        return mX;
    }

    public float getY()
    {
        return mY;
    }

    public ClipData getClipData()
    {
        return mClipData;
    }

    public ClipDescription getClipDescription()
    {
        return mClipDescription;
    }

    public Object getLocalState()
    {
        return mLocalState;
    }

    public boolean getResult()
    {
        return mResult;
    }

    public final void recycle()
    {
    }

    @Override
    public String toString()
    {
        return "DragEvent{action=" + mAction + " @ (" + mX + ", " + mY + ") desc=" + mClipDescription + " data=" + mClipData
               + " local=" + mLocalState + " result=" + mResult + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mAction);
        dest.writeFloat(mX);
        dest.writeFloat(mY);
        dest.writeBoolean(mResult);
        dest.writeInt(mClipData != null ? 1 : 0);
        if (mClipData != null) mClipData.writeToParcel(dest, flags);
    }
}
