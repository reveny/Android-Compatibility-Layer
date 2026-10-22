package android.view.contentcapture;

import android.os.Parcel;
import android.os.Parcelable;

public final class ContentCaptureSessionId implements Parcelable
{
    public static final Parcelable.Creator<ContentCaptureSessionId> CREATOR = new Parcelable.Creator<ContentCaptureSessionId>()
    {
        @Override
        public ContentCaptureSessionId createFromParcel(Parcel source)
        {
            return new ContentCaptureSessionId(source.readInt());
        }

        @Override
        public ContentCaptureSessionId[] newArray(int size)
        {
            return new ContentCaptureSessionId[size];
        }
    };

    private final int mValue;

    public ContentCaptureSessionId(int value)
    {
        mValue = value;
    }

    public int getValue()
    {
        return mValue;
    }

    @Override
    public int hashCode()
    {
        return mValue;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof ContentCaptureSessionId && ((ContentCaptureSessionId)other).mValue == mValue;
    }

    @Override
    public String toString()
    {
        return Integer.toString(mValue);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mValue);
    }
}
