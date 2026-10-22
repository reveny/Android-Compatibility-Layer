package android.view.inputmethod;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class SelectGesture extends PreviewableHandwritingGesture implements Parcelable
{
    private final int mGranularity;
    private final RectF mArea;

    private SelectGesture(int granularity, RectF area, String fallbackText)
    {
        mType = GESTURE_TYPE_SELECT;
        mGranularity = granularity;
        mArea = area;
        mFallbackText = fallbackText;
    }

    public int getGranularity()
    {
        return mGranularity;
    }

    public RectF getSelectionArea()
    {
        return mArea;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mGranularity, mArea, mFallbackText);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof SelectGesture)) return false;

        SelectGesture that = (SelectGesture)other;
        return mGranularity == that.mGranularity && same(mArea, that.mArea) && same(mFallbackText, that.mFallbackText);
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
        dest.writeParcelable(mArea, flags);
    }

    public static final Creator<SelectGesture> CREATOR = new Creator<SelectGesture>()
    {
        @Override
        public SelectGesture createFromParcel(Parcel source)
        {
            String fallbackText = source.readString();
            int granularity = source.readInt();
            RectF area = source.readParcelable(RectF.class.getClassLoader());
            return new SelectGesture(granularity, area, fallbackText);
        }

        @Override
        public SelectGesture[] newArray(int size)
        {
            return new SelectGesture[size];
        }
    };

    public static final class Builder
    {
        private int mGranularity;
        private RectF mArea;
        private String mFallbackText;

        public Builder()
        {
        }

        public Builder setGranularity(int granularity)
        {
            mGranularity = granularity;
            return this;
        }

        public Builder setSelectionArea(RectF area)
        {
            mArea = area;
            return this;
        }

        public Builder setFallbackText(String fallbackText)
        {
            mFallbackText = fallbackText;
            return this;
        }

        public SelectGesture build()
        {
            if (mArea == null || mArea.isEmpty()) throw new IllegalArgumentException("Selection area must be set.");
            if (mGranularity <= 0) throw new IllegalArgumentException("Selection granularity must be set.");

            return new SelectGesture(mGranularity, mArea, mFallbackText);
        }
    }
}
