package android.view.inputmethod;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CursorAnchorInfo implements Parcelable
{
    public static final int FLAG_HAS_VISIBLE_REGION = 0x01;
    public static final int FLAG_HAS_INVISIBLE_REGION = 0x02;
    public static final int FLAG_IS_RTL = 0x04;

    private int mSelectionStart = -1;
    private int mSelectionEnd = -1;
    private int mComposingTextStart = -1;
    private CharSequence mComposingText;
    private int mInsertionMarkerFlags;
    private float mInsertionMarkerHorizontal = Float.NaN;
    private float mInsertionMarkerTop = Float.NaN;
    private float mInsertionMarkerBaseline = Float.NaN;
    private float mInsertionMarkerBottom = Float.NaN;
    private final SparseArray<RectF> mCharacterBounds = new SparseArray<>();
    private final SparseArray<Integer> mCharacterBoundsFlags = new SparseArray<>();
    private final ArrayList<RectF> mVisibleLineBounds = new ArrayList<>();
    private EditorBoundsInfo mEditorBoundsInfo;
    private TextAppearanceInfo mTextAppearanceInfo;
    private final Matrix mMatrix = new Matrix();

    private CursorAnchorInfo()
    {
    }

    public CursorAnchorInfo(Parcel source)
    {
        mSelectionStart = source.readInt();
        mSelectionEnd = source.readInt();
        mComposingTextStart = source.readInt();
        mComposingText = (CharSequence)source.readValue(null);
        mInsertionMarkerFlags = source.readInt();
        mInsertionMarkerHorizontal = source.readFloat();
        mInsertionMarkerTop = source.readFloat();
        mInsertionMarkerBaseline = source.readFloat();
        mInsertionMarkerBottom = source.readFloat();
        mEditorBoundsInfo = source.readParcelable(EditorBoundsInfo.class.getClassLoader());

        float[] values = (float[])source.readValue(null);
        if (values != null) mMatrix.setValues(values);

        if (source.readInt() != 0) mTextAppearanceInfo = TextAppearanceInfo.CREATOR.createFromParcel(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mSelectionStart);
        dest.writeInt(mSelectionEnd);
        dest.writeInt(mComposingTextStart);
        dest.writeValue(mComposingText);
        dest.writeInt(mInsertionMarkerFlags);
        dest.writeFloat(mInsertionMarkerHorizontal);
        dest.writeFloat(mInsertionMarkerTop);
        dest.writeFloat(mInsertionMarkerBaseline);
        dest.writeFloat(mInsertionMarkerBottom);
        dest.writeParcelable(mEditorBoundsInfo, flags);

        float[] values = new float[9];
        mMatrix.getValues(values);
        dest.writeValue(values);

        dest.writeInt(mTextAppearanceInfo == null ? 0 : 1);
        if (mTextAppearanceInfo != null) mTextAppearanceInfo.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<CursorAnchorInfo> CREATOR = new Creator<CursorAnchorInfo>()
    {
        @Override
        public CursorAnchorInfo createFromParcel(Parcel source)
        {
            return new CursorAnchorInfo(source);
        }

        @Override
        public CursorAnchorInfo[] newArray(int size)
        {
            return new CursorAnchorInfo[size];
        }
    };

    @Override
    public int hashCode()
    {
        return Objects.hash(mSelectionStart, mSelectionEnd, mComposingTextStart, mComposingText, mInsertionMarkerFlags,
                            mInsertionMarkerHorizontal, mInsertionMarkerTop, mInsertionMarkerBaseline, mInsertionMarkerBottom,
                            mEditorBoundsInfo, mMatrix);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof CursorAnchorInfo)) return false;

        CursorAnchorInfo that = (CursorAnchorInfo)other;
        return mSelectionStart == that.mSelectionStart && mSelectionEnd == that.mSelectionEnd
            && mComposingTextStart == that.mComposingTextStart && Objects.equals(mComposingText, that.mComposingText)
            && mInsertionMarkerFlags == that.mInsertionMarkerFlags
            && Float.compare(mInsertionMarkerHorizontal, that.mInsertionMarkerHorizontal) == 0
            && Float.compare(mInsertionMarkerTop, that.mInsertionMarkerTop) == 0
            && Float.compare(mInsertionMarkerBaseline, that.mInsertionMarkerBaseline) == 0
            && Float.compare(mInsertionMarkerBottom, that.mInsertionMarkerBottom) == 0
            && Objects.equals(mEditorBoundsInfo, that.mEditorBoundsInfo) && mMatrix.equals(that.mMatrix)
            && mVisibleLineBounds.equals(that.mVisibleLineBounds);
    }

    @Override
    public String toString()
    {
        return "CursorAnchorInfo{mSelection=" + mSelectionStart + "," + mSelectionEnd + " mComposingTextStart=" + mComposingTextStart
               + " mComposingText=" + mComposingText + " mInsertionMarkerFlags=" + mInsertionMarkerFlags
               + " mInsertionMarkerHorizontal=" + mInsertionMarkerHorizontal + " mInsertionMarkerTop=" + mInsertionMarkerTop
               + " mInsertionMarkerBaseline=" + mInsertionMarkerBaseline + " mInsertionMarkerBottom=" + mInsertionMarkerBottom
               + " mEditorBoundsInfo=" + mEditorBoundsInfo + " mMatrix=" + mMatrix.toShortString() + "}";
    }

    public int getSelectionStart()
    {
        return mSelectionStart;
    }

    public int getSelectionEnd()
    {
        return mSelectionEnd;
    }

    public int getComposingTextStart()
    {
        return mComposingTextStart;
    }

    public CharSequence getComposingText()
    {
        return mComposingText;
    }

    public float getInsertionMarkerHorizontal()
    {
        return mInsertionMarkerHorizontal;
    }

    public float getInsertionMarkerTop()
    {
        return mInsertionMarkerTop;
    }

    public float getInsertionMarkerBaseline()
    {
        return mInsertionMarkerBaseline;
    }

    public float getInsertionMarkerBottom()
    {
        return mInsertionMarkerBottom;
    }

    public int getInsertionMarkerFlags()
    {
        return mInsertionMarkerFlags;
    }

    public RectF getCharacterBounds(int index)
    {
        RectF bounds = mCharacterBounds.get(index);
        return bounds == null ? null : new RectF(bounds);
    }

    public int getCharacterBoundsFlags(int index)
    {
        Integer flags = mCharacterBoundsFlags.get(index);
        return flags == null ? 0 : flags.intValue();
    }

    public List<RectF> getVisibleLineBounds()
    {
        return new ArrayList<>(mVisibleLineBounds);
    }

    public EditorBoundsInfo getEditorBoundsInfo()
    {
        return mEditorBoundsInfo;
    }

    public TextAppearanceInfo getTextAppearanceInfo()
    {
        return mTextAppearanceInfo;
    }

    public Matrix getMatrix()
    {
        return new Matrix(mMatrix);
    }

    public static final class Builder
    {
        private CursorAnchorInfo mInfo = new CursorAnchorInfo();

        public Builder()
        {
        }

        public Builder setSelectionRange(int newStart, int newEnd)
        {
            mInfo.mSelectionStart = newStart;
            mInfo.mSelectionEnd = newEnd;
            return this;
        }

        public Builder setComposingText(int composingTextStart, CharSequence composingText)
        {
            mInfo.mComposingTextStart = composingTextStart;
            mInfo.mComposingText = composingText;
            return this;
        }

        public Builder setInsertionMarkerLocation(float horizontalPosition, float lineTop, float lineBaseline, float lineBottom,
                                                  int flags)
        {
            mInfo.mInsertionMarkerHorizontal = horizontalPosition;
            mInfo.mInsertionMarkerTop = lineTop;
            mInfo.mInsertionMarkerBaseline = lineBaseline;
            mInfo.mInsertionMarkerBottom = lineBottom;
            mInfo.mInsertionMarkerFlags = flags;
            return this;
        }

        public Builder addCharacterBounds(int index, float left, float top, float right, float bottom, int flags)
        {
            if (index < 0) throw new IllegalArgumentException("index must not be a negative integer.");

            mInfo.mCharacterBounds.put(index, new RectF(left, top, right, bottom));
            mInfo.mCharacterBoundsFlags.put(index, flags);
            return this;
        }

        public Builder setMatrix(Matrix matrix)
        {
            mInfo.mMatrix.set(matrix != null ? matrix : Matrix.IDENTITY_MATRIX);
            return this;
        }

        public Builder setEditorBoundsInfo(EditorBoundsInfo editorBoundsInfo)
        {
            mInfo.mEditorBoundsInfo = editorBoundsInfo;
            return this;
        }

        public Builder addVisibleLineBounds(float left, float top, float right, float bottom)
        {
            mInfo.mVisibleLineBounds.add(new RectF(left, top, right, bottom));
            return this;
        }

        public Builder clearVisibleLineBounds()
        {
            mInfo.mVisibleLineBounds.clear();
            return this;
        }

        public Builder setTextAppearanceInfo(TextAppearanceInfo textAppearanceInfo)
        {
            mInfo.mTextAppearanceInfo = textAppearanceInfo;
            return this;
        }

        public void reset()
        {
            mInfo = new CursorAnchorInfo();
        }

        public CursorAnchorInfo build()
        {
            CursorAnchorInfo built = mInfo;
            CursorAnchorInfo copy = new CursorAnchorInfo();
            copy.mSelectionStart = built.mSelectionStart;
            copy.mSelectionEnd = built.mSelectionEnd;
            copy.mComposingTextStart = built.mComposingTextStart;
            copy.mComposingText = built.mComposingText;
            copy.mInsertionMarkerFlags = built.mInsertionMarkerFlags;
            copy.mInsertionMarkerHorizontal = built.mInsertionMarkerHorizontal;
            copy.mInsertionMarkerTop = built.mInsertionMarkerTop;
            copy.mInsertionMarkerBaseline = built.mInsertionMarkerBaseline;
            copy.mInsertionMarkerBottom = built.mInsertionMarkerBottom;
            copy.mEditorBoundsInfo = built.mEditorBoundsInfo;
            copy.mTextAppearanceInfo = built.mTextAppearanceInfo;
            copy.mMatrix.set(built.mMatrix);
            copy.mVisibleLineBounds.addAll(built.mVisibleLineBounds);

            for (int index = 0; index < built.mCharacterBounds.size(); index++)
            {
                copy.mCharacterBounds.put(built.mCharacterBounds.keyAt(index), built.mCharacterBounds.valueAt(index));
                copy.mCharacterBoundsFlags.put(built.mCharacterBoundsFlags.keyAt(index), built.mCharacterBoundsFlags.valueAt(index));
            }

            return copy;
        }
    }
}
