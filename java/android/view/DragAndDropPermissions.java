package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public final class DragAndDropPermissions implements Parcelable
{
    public static final Parcelable.Creator<DragAndDropPermissions> CREATOR = new Parcelable.Creator<DragAndDropPermissions>()
    {
        @Override
        public DragAndDropPermissions createFromParcel(Parcel source)
        {
            return new DragAndDropPermissions(source.readBoolean());
        }

        @Override
        public DragAndDropPermissions[] newArray(int size)
        {
            return new DragAndDropPermissions[size];
        }
    };

    private boolean mHeld;

    DragAndDropPermissions(boolean held)
    {
        mHeld = held;
    }

    static DragAndDropPermissions obtain(DragEvent event)
    {
        if (event == null || event.getClipData() == null) return null;
        return new DragAndDropPermissions(true);
    }

    public void release()
    {
        mHeld = false;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeBoolean(mHeld);
    }
}
