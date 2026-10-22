package android.view.inputmethod;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class RemoveSpaceGesture extends HandwritingGesture implements Parcelable
{
    private final PointF mStartPoint;
    private final PointF mEndPoint;

    private RemoveSpaceGesture(PointF startPoint, PointF endPoint, String fallbackText)
    {
        mType = GESTURE_TYPE_REMOVE_SPACE;
        mStartPoint = startPoint;
        mEndPoint = endPoint;
        mFallbackText = fallbackText;
    }

    public PointF getStartPoint()
    {
        return mStartPoint;
    }

    public PointF getEndPoint()
    {
        return mEndPoint;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mStartPoint, mEndPoint, mFallbackText);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof RemoveSpaceGesture)) return false;

        RemoveSpaceGesture that = (RemoveSpaceGesture)other;
        return same(mStartPoint, that.mStartPoint) && same(mEndPoint, that.mEndPoint) && same(mFallbackText, that.mFallbackText);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mFallbackText);
        dest.writeParcelable(mStartPoint, flags);
        dest.writeParcelable(mEndPoint, flags);
    }

    public static final Creator<RemoveSpaceGesture> CREATOR = new Creator<RemoveSpaceGesture>()
    {
        @Override
        public RemoveSpaceGesture createFromParcel(Parcel source)
        {
            String fallbackText = source.readString();
            PointF startPoint = source.readParcelable(PointF.class.getClassLoader());
            PointF endPoint = source.readParcelable(PointF.class.getClassLoader());
            return new RemoveSpaceGesture(startPoint, endPoint, fallbackText);
        }

        @Override
        public RemoveSpaceGesture[] newArray(int size)
        {
            return new RemoveSpaceGesture[size];
        }
    };

    public static final class Builder
    {
        private PointF mStartPoint;
        private PointF mEndPoint;
        private String mFallbackText;

        public Builder()
        {
        }

        public Builder setPoints(PointF startPoint, PointF endPoint)
        {
            mStartPoint = startPoint;
            mEndPoint = endPoint;
            return this;
        }

        public Builder setFallbackText(String fallbackText)
        {
            mFallbackText = fallbackText;
            return this;
        }

        public RemoveSpaceGesture build()
        {
            if (mStartPoint == null || mEndPoint == null) throw new IllegalArgumentException("Start and end points must be set.");

            return new RemoveSpaceGesture(mStartPoint, mEndPoint, mFallbackText);
        }
    }
}
