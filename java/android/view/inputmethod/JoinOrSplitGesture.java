package android.view.inputmethod;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class JoinOrSplitGesture extends HandwritingGesture implements Parcelable
{
    private final PointF mPoint;

    private JoinOrSplitGesture(PointF point, String fallbackText)
    {
        mType = GESTURE_TYPE_JOIN_OR_SPLIT;
        mPoint = point;
        mFallbackText = fallbackText;
    }

    public PointF getJoinOrSplitPoint()
    {
        return mPoint;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mPoint, mFallbackText);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof JoinOrSplitGesture)) return false;

        JoinOrSplitGesture that = (JoinOrSplitGesture)other;
        return same(mPoint, that.mPoint) && same(mFallbackText, that.mFallbackText);
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
        dest.writeParcelable(mPoint, flags);
    }

    public static final Creator<JoinOrSplitGesture> CREATOR = new Creator<JoinOrSplitGesture>()
    {
        @Override
        public JoinOrSplitGesture createFromParcel(Parcel source)
        {
            String fallbackText = source.readString();
            PointF point = source.readParcelable(PointF.class.getClassLoader());
            return new JoinOrSplitGesture(point, fallbackText);
        }

        @Override
        public JoinOrSplitGesture[] newArray(int size)
        {
            return new JoinOrSplitGesture[size];
        }
    };

    public static final class Builder
    {
        private PointF mPoint;
        private String mFallbackText;

        public Builder()
        {
        }

        public Builder setJoinOrSplitPoint(PointF point)
        {
            mPoint = point;
            return this;
        }

        public Builder setFallbackText(String fallbackText)
        {
            mFallbackText = fallbackText;
            return this;
        }

        public JoinOrSplitGesture build()
        {
            if (mPoint == null) throw new IllegalArgumentException("Point must be set.");

            return new JoinOrSplitGesture(mPoint, mFallbackText);
        }
    }
}
