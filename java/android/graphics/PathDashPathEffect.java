package android.graphics;

public class PathDashPathEffect extends PathEffect
{
    private final Path mShape;
    private final float mAdvance;
    private final float mPhase;
    private final Style mStyle;

    public PathDashPathEffect(Path shape, float advance, float phase, Style style)
    {
        if (advance <= 0) throw new IllegalArgumentException("PathDashPathEffect: advance must be > 0");

        mShape = shape == null ? new Path() : new Path(shape);
        mAdvance = advance;
        mPhase = phase;
        mStyle = style == null ? Style.TRANSLATE : style;
    }

    Path shape()
    {
        return mShape;
    }

    float advance()
    {
        return mAdvance;
    }

    float phase()
    {
        return mPhase;
    }

    Style style()
    {
        return mStyle;
    }

    public enum Style
    {
        TRANSLATE,
        ROTATE,
        MORPH
    }
}
