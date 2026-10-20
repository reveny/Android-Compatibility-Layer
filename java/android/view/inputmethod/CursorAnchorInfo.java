package android.view.inputmethod;

public final class CursorAnchorInfo
{
    public static final int FLAG_HAS_VISIBLE_REGION = 0x01;
    public static final int FLAG_HAS_INVISIBLE_REGION = 0x02;
    public static final int FLAG_IS_RTL = 0x04;

    private int mSelectionStart = -1;
    private int mSelectionEnd = -1;
    private int mComposingTextStart = -1;
    private CharSequence mComposingText;

    private CursorAnchorInfo()
    {
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
        return Float.NaN;
    }

    public float getInsertionMarkerTop()
    {
        return Float.NaN;
    }

    public float getInsertionMarkerBaseline()
    {
        return Float.NaN;
    }

    public float getInsertionMarkerBottom()
    {
        return Float.NaN;
    }

    public int getInsertionMarkerFlags()
    {
        return 0;
    }

    public android.graphics.RectF getCharacterBounds(int index)
    {
        return null;
    }

    public int getCharacterBoundsFlags(int index)
    {
        return 0;
    }

    public Object getMatrix()
    {
        return null;
    }

    public static final class Builder
    {
        private final CursorAnchorInfo mInfo = new CursorAnchorInfo();

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
            return this;
        }

        public Builder addCharacterBounds(int index, float left, float top, float right, float bottom, int flags)
        {
            return this;
        }

        public Builder setMatrix(Object matrix)
        {
            return this;
        }

        public Builder setEditorBoundsInfo(Object editorBoundsInfo)
        {
            return this;
        }

        public Builder addVisibleLineBounds(float left, float top, float right, float bottom)
        {
            return this;
        }

        public Builder setTextAppearanceInfo(Object textAppearanceInfo)
        {
            return this;
        }

        public void reset()
        {
        }

        public CursorAnchorInfo build()
        {
            return mInfo;
        }
    }
}
