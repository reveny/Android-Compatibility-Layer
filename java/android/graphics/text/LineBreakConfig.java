package android.graphics.text;

public final class LineBreakConfig
{
    public static final int LINE_BREAK_STYLE_NONE = 0;
    public static final int LINE_BREAK_STYLE_LOOSE = 1;
    public static final int LINE_BREAK_STYLE_NORMAL = 2;
    public static final int LINE_BREAK_STYLE_STRICT = 3;
    public static final int LINE_BREAK_STYLE_UNSPECIFIED = 4;

    public static final int LINE_BREAK_WORD_STYLE_NONE = 0;
    public static final int LINE_BREAK_WORD_STYLE_PHRASE = 1;
    public static final int LINE_BREAK_WORD_STYLE_UNSPECIFIED = 2;

    private final int mLineBreakStyle;
    private final int mLineBreakWordStyle;

    private LineBreakConfig(int lineBreakStyle, int lineBreakWordStyle)
    {
        mLineBreakStyle = lineBreakStyle;
        mLineBreakWordStyle = lineBreakWordStyle;
    }

    public int getLineBreakStyle()
    {
        return mLineBreakStyle;
    }

    public int getLineBreakWordStyle()
    {
        return mLineBreakWordStyle;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof LineBreakConfig)) return false;

        LineBreakConfig that = (LineBreakConfig)other;
        return mLineBreakStyle == that.mLineBreakStyle && mLineBreakWordStyle == that.mLineBreakWordStyle;
    }

    @Override
    public int hashCode()
    {
        return 31 * mLineBreakStyle + mLineBreakWordStyle;
    }

    public static final class Builder
    {
        private int mLineBreakStyle = LINE_BREAK_STYLE_NONE;
        private int mLineBreakWordStyle = LINE_BREAK_WORD_STYLE_NONE;

        public Builder setLineBreakStyle(int style)
        {
            mLineBreakStyle = style;
            return this;
        }

        public Builder setLineBreakWordStyle(int style)
        {
            mLineBreakWordStyle = style;
            return this;
        }

        public LineBreakConfig build()
        {
            return new LineBreakConfig(mLineBreakStyle, mLineBreakWordStyle);
        }
    }
}
