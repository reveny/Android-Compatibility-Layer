package android.graphics;

public final class BlendModeColorFilter extends ColorFilter
{
    private final int mColor;
    private final BlendMode mMode;

    public BlendModeColorFilter(int color, BlendMode mode)
    {
        mColor = color;
        mMode = mode == null ? BlendMode.SRC_IN : mode;
    }

    public int getColor()
    {
        return mColor;
    }

    public BlendMode getMode()
    {
        return mMode;
    }

    int apply(int source)
    {
        switch (mMode)
        {
            case SRC:
            case SRC_IN:
            case SRC_ATOP:
                return (source & 0xFF000000) | (mColor & 0x00FFFFFF);

            case MODULATE:
            case MULTIPLY:
                return Color.argb(Color.alpha(source), Color.red(source) * Color.red(mColor) / 255,
                                  Color.green(source) * Color.green(mColor) / 255,
                                  Color.blue(source) * Color.blue(mColor) / 255);

            case DST:
            case DST_IN:
            case DST_ATOP:
                return source;

            default:
                return mColor;
        }
    }
}
