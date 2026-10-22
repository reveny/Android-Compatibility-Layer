package android.text.style;

import android.graphics.text.LineBreakConfig;

public class LineBreakConfigSpan
{
    private final LineBreakConfig mLineBreakConfig;

    public LineBreakConfigSpan(LineBreakConfig lineBreakConfig)
    {
        if (lineBreakConfig == null) throw new NullPointerException("lineBreakConfig");

        mLineBreakConfig = lineBreakConfig;
    }

    public LineBreakConfig getLineBreakConfig()
    {
        return mLineBreakConfig;
    }

    public static LineBreakConfigSpan createNoBreakSpan()
    {
        return new LineBreakConfigSpan(new LineBreakConfig.Builder().setLineBreakStyle(LineBreakConfig.LINE_BREAK_STYLE_STRICT).build());
    }

    public static LineBreakConfigSpan createNoHyphenationSpan()
    {
        return new LineBreakConfigSpan(new LineBreakConfig.Builder().build());
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof LineBreakConfigSpan)) return false;

        LineBreakConfig config = ((LineBreakConfigSpan)other).mLineBreakConfig;
        return config.getLineBreakStyle() == mLineBreakConfig.getLineBreakStyle()
               && config.getLineBreakWordStyle() == mLineBreakConfig.getLineBreakWordStyle();
    }

    @Override
    public int hashCode()
    {
        return 31 * mLineBreakConfig.getLineBreakStyle() + mLineBreakConfig.getLineBreakWordStyle();
    }

    @Override
    public String toString()
    {
        return "LineBreakConfigSpan{mLineBreakConfig=" + mLineBreakConfig + "}";
    }
}
