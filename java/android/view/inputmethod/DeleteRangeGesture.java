package android.view.inputmethod;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class DeleteRangeGesture extends PreviewableHandwritingGesture implements Parcelable
{
    private final int mGranularity;
    private final RectF mStartArea;
    private final RectF mEndArea;

    private DeleteRangeGesture(int granularity, RectF startArea, RectF endArea, String fallbackText)
    {
        mType = GESTURE_TYPE_DELETE_RANGE;
        mGranularity = granularity;
        mStartArea = startArea;
        mEndArea = endArea;
        mFallbackText = fallbackText;
    }

    public int getGranularity()
    {
        return mGranularity;
    }

    public RectF getDeletionStartArea()
    {
        return mStartArea;
    }

    public RectF getDeletionEndArea()
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
        if (!(other instanceof DeleteRangeGesture)) return false;

        DeleteRangeGesture that = (DeleteRangeGesture)other;
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

    public static final Creator<DeleteRangeGesture> CREATOR = new Creator<DeleteRangeGesture>()
    {
        @Override
        public DeleteRangeGesture createFromParcel(Parcel source)
        {
            String fallbackText = source.readString();
            int granularity = source.readInt();
            RectF startArea = source.readParcelable(RectF.class.getClassLoader());
            RectF endArea = source.readParcelable(RectF.class.getClassLoader());
            return new DeleteRangeGesture(granularity, startArea, endArea, fallbackText);
        }

        @Override
        public DeleteRangeGesture[] newArray(int size)
        {
            return new DeleteRangeGesture[size];
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

        public Builder setDeletionStartArea(RectF startArea)
        {
            mStartArea = startArea;
            return this;
        }

        public Builder setDeletionEndArea(RectF endArea)
        {
            mEndArea = endArea;
            return this;
        }

        public Builder setFallbackText(String fallbackText)
        {
            mFallbackText = fallbackText;
            return this;
        }

        public DeleteRangeGesture build()
        {
            if (mStartArea == null || mStartArea.isEmpty() || mEndArea == null || mEndArea.isEmpty())
            {
                throw new IllegalArgumentException("Deletion area must be set.");
            }
            if (mGranularity <= 0) throw new IllegalArgumentException("Deletion granularity must be set.");

            return new DeleteRangeGesture(mGranularity, mStartArea, mEndArea, mFallbackText);
        }
    }
}
