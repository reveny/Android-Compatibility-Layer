package android.view.inputmethod;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class DeleteGesture extends PreviewableHandwritingGesture implements Parcelable
{
    private final int mGranularity;
    private final RectF mArea;

    private DeleteGesture(int granularity, RectF area, String fallbackText)
    {
        mType = GESTURE_TYPE_DELETE;
        mGranularity = granularity;
        mArea = area;
        mFallbackText = fallbackText;
    }

    public int getGranularity()
    {
        return mGranularity;
    }

    public RectF getDeletionArea()
    {
        return mArea;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mArea, mGranularity, mFallbackText);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof DeleteGesture)) return false;

        DeleteGesture that = (DeleteGesture)other;
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

    public static final Creator<DeleteGesture> CREATOR = new Creator<DeleteGesture>()
    {
        @Override
        public DeleteGesture createFromParcel(Parcel source)
        {
            String fallbackText = source.readString();
            int granularity = source.readInt();
            RectF area = source.readParcelable(RectF.class.getClassLoader());
            return new DeleteGesture(granularity, area, fallbackText);
        }

        @Override
        public DeleteGesture[] newArray(int size)
        {
            return new DeleteGesture[size];
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

        public Builder setDeletionArea(RectF area)
        {
            mArea = area;
            return this;
        }

        public Builder setFallbackText(String fallbackText)
        {
            mFallbackText = fallbackText;
            return this;
        }

        public DeleteGesture build()
        {
            if (mArea == null || mArea.isEmpty()) throw new IllegalArgumentException("Deletion area must be set.");
            if (mGranularity <= 0) throw new IllegalArgumentException("Deletion granularity must be set.");

            return new DeleteGesture(mGranularity, mArea, mFallbackText);
        }
    }
}
