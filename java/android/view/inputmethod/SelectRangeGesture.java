package android.view.inputmethod;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class SelectRangeGesture extends PreviewableHandwritingGesture implements Parcelable
{
    private final int mGranularity;
    private final RectF mStartArea;
    private final RectF mEndArea;

    private SelectRangeGesture(int granularity, RectF startArea, RectF endArea, String fallbackText)
    {
        mType = GESTURE_TYPE_SELECT_RANGE;
        mGranularity = granularity;
        mStartArea = startArea;
        mEndArea = endArea;
        mFallbackText = fallbackText;
    }

    public int getGranularity()
    {
        return mGranularity;
    }

    public RectF getSelectionStartArea()
    {
        return mStartArea;
    }

    public RectF getSelectionEndArea()
    {
        return mEndArea;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mGranularity, mStartArea, mEndArea, mFallbackText);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof SelectRangeGesture)) return false;

        SelectRangeGesture that = (SelectRangeGesture)other;
        return mGranularity == that.mGranularity && same(mStartArea, that.mStartArea) && same(mEndArea, that.mEndArea)
            && same(mFallbackText, that.mFallbackText);
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
        dest.writeInt(mGranularity);
        dest.writeParcelable(mStartArea, flags);
        dest.writeParcelable(mEndArea, flags);
    }

    public static final Creator<SelectRangeGesture> CREATOR = new Creator<SelectRangeGesture>()
    {
        @Override
        public SelectRangeGesture createFromParcel(Parcel source)
        {
            String fallbackText = source.readString();
            int granularity = source.readInt();
            RectF startArea = source.readParcelable(RectF.class.getClassLoader());
            RectF endArea = source.readParcelable(RectF.class.getClassLoader());
            return new SelectRangeGesture(granularity, startArea, endArea, fallbackText);
        }

        @Override
        public SelectRangeGesture[] newArray(int size)
        {
            return new SelectRangeGesture[size];
        }
    };

    public static final class Builder
    {
        private int mGranularity;
        private RectF mStartArea;
        private RectF mEndArea;
        private String mFallbackText;

        public Builder()
        {
        }

        public Builder setGranularity(int granularity)
        {
            mGranularity = granularity;
            return this;
        }

        public Builder setSelectionStartArea(RectF startArea)
        {
            mStartArea = startArea;
            return this;
        }

        public Builder setSelectionEndArea(RectF endArea)
        {
            mEndArea = endArea;
            return this;
        }

        public Builder setFallbackText(String fallbackText)
        {
            mFallbackText = fallbackText;
            return this;
        }

        public SelectRangeGesture build()
        {
            if (mStartArea == null || mStartArea.isEmpty() || mEndArea == null || mEndArea.isEmpty())
            {
                throw new IllegalArgumentException("Selection area must be set.");
            }
            if (mGranularity <= 0) throw new IllegalArgumentException("Selection granularity must be set.");

            return new SelectRangeGesture(mGranularity, mStartArea, mEndArea, mFallbackText);
        }
    }
}
