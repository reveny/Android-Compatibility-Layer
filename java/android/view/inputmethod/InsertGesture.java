package android.view.inputmethod;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class InsertGesture extends HandwritingGesture implements Parcelable
{
    private final String mTextToInsert;
    private final PointF mPoint;

    private InsertGesture(String textToInsert, PointF point, String fallbackText)
    {
        mType = GESTURE_TYPE_INSERT;
        mTextToInsert = textToInsert;
        mPoint = point;
        mFallbackText = fallbackText;
    }

    public String getTextToInsert()
    {
        return mTextToInsert;
    }

    public PointF getInsertionPoint()
    {
        return mPoint;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mPoint, mTextToInsert, mFallbackText);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof InsertGesture)) return false;

        InsertGesture that = (InsertGesture)other;
        return same(mPoint, that.mPoint) && same(mTextToInsert, that.mTextToInsert) && same(mFallbackText, that.mFallbackText);
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
        dest.writeString(mTextToInsert);
        dest.writeParcelable(mPoint, flags);
    }

    public static final Creator<InsertGesture> CREATOR = new Creator<InsertGesture>()
    {
        @Override
        public InsertGesture createFromParcel(Parcel source)
        {
            String fallbackText = source.readString();
            String textToInsert = source.readString();
            PointF point = source.readParcelable(PointF.class.getClassLoader());
            return new InsertGesture(textToInsert, point, fallbackText);
        }

        @Override
        public InsertGesture[] newArray(int size)
        {
            return new InsertGesture[size];
        }
    };

    public static final class Builder
    {
        private String mTextToInsert;
        private PointF mPoint;
        private String mFallbackText;

        public Builder()
        {
        }

        public Builder setTextToInsert(String text)
        {
            mTextToInsert = text;
            return this;
        }

        public Builder setInsertionPoint(PointF point)
        {
            mPoint = point;
            return this;
        }

        public Builder setFallbackText(String fallbackText)
        {
            mFallbackText = fallbackText;
            return this;
        }

        public InsertGesture build()
        {
            if (mPoint == null) throw new IllegalArgumentException("Insertion point must be set.");
            if (mTextToInsert == null) throw new IllegalArgumentException("Text to insert must be set.");

            return new InsertGesture(mTextToInsert, mPoint, mFallbackText);
        }
    }
}
