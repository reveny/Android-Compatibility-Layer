package android.view.inputmethod;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class EditorBoundsInfo implements Parcelable
{
    private final RectF mEditorBounds;
    private final RectF mHandwritingBounds;

    private EditorBoundsInfo(RectF editorBounds, RectF handwritingBounds)
    {
        mEditorBounds = editorBounds;
        mHandwritingBounds = handwritingBounds;
    }

    public RectF getEditorBounds()
    {
        return mEditorBounds;
    }

    public RectF getHandwritingBounds()
    {
        return mHandwritingBounds;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mEditorBounds, mHandwritingBounds);
    }

    @Override
    public String toString()
    {
        return "EditorBoundsInfo{mEditorBounds=" + mEditorBounds + " mHandwritingBounds=" + mHandwritingBounds + "}";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof EditorBoundsInfo)) return false;

        EditorBoundsInfo that = (EditorBoundsInfo)other;
        return Objects.equals(mEditorBounds, that.mEditorBounds) && Objects.equals(mHandwritingBounds, that.mHandwritingBounds);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(mEditorBounds, flags);
        dest.writeParcelable(mHandwritingBounds, flags);
    }

    public static final Creator<EditorBoundsInfo> CREATOR = new Creator<EditorBoundsInfo>()
    {
        @Override
        public EditorBoundsInfo createFromParcel(Parcel source)
        {
            RectF editorBounds = source.readParcelable(RectF.class.getClassLoader());
            RectF handwritingBounds = source.readParcelable(RectF.class.getClassLoader());
            return new EditorBoundsInfo(editorBounds, handwritingBounds);
        }

        @Override
        public EditorBoundsInfo[] newArray(int size)
        {
            return new EditorBoundsInfo[size];
        }
    };

    public static final class Builder
    {
        private RectF mEditorBounds;
        private RectF mHandwritingBounds;

        public Builder()
        {
        }

        public Builder setEditorBounds(RectF bounds)
        {
            mEditorBounds = bounds == null ? null : new RectF(bounds);
            return this;
        }

        public Builder setHandwritingBounds(RectF bounds)
        {
            mHandwritingBounds = bounds == null ? null : new RectF(bounds);
            return this;
        }

        public EditorBoundsInfo build()
        {
            return new EditorBoundsInfo(mEditorBounds, mHandwritingBounds);
        }
    }
}
