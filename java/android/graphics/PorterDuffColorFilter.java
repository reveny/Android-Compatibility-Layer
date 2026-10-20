package android.graphics;

public class PorterDuffColorFilter extends ColorFilter
{
    private final int mColor;
    private final PorterDuff.Mode mMode;

    public PorterDuffColorFilter(int color, PorterDuff.Mode mode)
    {
        mColor = color;
        mMode = mode == null ? PorterDuff.Mode.SRC_IN : mode;
    }

    public int getColor()
    {
        return mColor;
    }

    public PorterDuff.Mode getMode()
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
