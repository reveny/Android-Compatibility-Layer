package android.graphics;

public enum BlendMode
{
    CLEAR,
    SRC,
    DST,
    SRC_OVER,
    DST_OVER,
    SRC_IN,
    DST_IN,
    SRC_OUT,
    DST_OUT,
    SRC_ATOP,
    DST_ATOP,
    XOR,
    PLUS,
    MODULATE,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN,
    COLOR_DODGE,
    COLOR_BURN,
    HARD_LIGHT,
    SOFT_LIGHT,
    DIFFERENCE,
    EXCLUSION,
    MULTIPLY,
    HUE,
    SATURATION,
    COLOR,
    LUMINOSITY;

    int nativeId()
    {
        switch (this)
        {
            case CLEAR:
                return 9;
            case SRC:
                return 1;
            case SRC_IN:
                return 2;
            case SRC_ATOP:
                return 3;
            case DST_OVER:
                return 4;
            case DST_OUT:
                return 5;
            case MULTIPLY:
            case MODULATE:
                return 6;
            case SCREEN:
                return 7;
            case PLUS:
                return 8;
            case DST_IN:
                return 10;
            case DST_ATOP:
                return 11;
            case SRC_OUT:
                return 12;
            case XOR:
                return 13;
            case DST:
                return 14;
            default:
                return 0;
        }
    }

    public static BlendMode fromPorterDuffMode(PorterDuff.Mode mode)
    {
        if (mode == null) return null;
        if (mode == PorterDuff.Mode.SRC_OVER) return SRC_OVER;

        BlendMode converted = fromPorterDuff(mode);
        return converted == SRC_OVER ? null : converted;
    }

    public static PorterDuff.Mode blendModeToPorterDuffMode(BlendMode mode)
    {
        if (mode == null) return null;

        switch (mode)
        {
            case CLEAR: return PorterDuff.Mode.CLEAR;
            case SRC: return PorterDuff.Mode.SRC;
            case DST: return PorterDuff.Mode.DST;
            case SRC_OVER: return PorterDuff.Mode.SRC_OVER;
            case DST_OVER: return PorterDuff.Mode.DST_OVER;
            case SRC_IN: return PorterDuff.Mode.SRC_IN;
            case DST_IN: return PorterDuff.Mode.DST_IN;
            case SRC_OUT: return PorterDuff.Mode.SRC_OUT;
            case DST_OUT: return PorterDuff.Mode.DST_OUT;
            case SRC_ATOP: return PorterDuff.Mode.SRC_ATOP;
            case DST_ATOP: return PorterDuff.Mode.DST_ATOP;
            case XOR: return PorterDuff.Mode.XOR;
            case DARKEN: return PorterDuff.Mode.DARKEN;
            case LIGHTEN: return PorterDuff.Mode.LIGHTEN;
            case MULTIPLY:
            case MODULATE:
                return PorterDuff.Mode.MULTIPLY;
            case SCREEN: return PorterDuff.Mode.SCREEN;
            case PLUS: return PorterDuff.Mode.ADD;
            case OVERLAY: return PorterDuff.Mode.OVERLAY;
            default: return null;
        }
    }

    static BlendMode fromPorterDuff(PorterDuff.Mode mode)
    {
        if (mode == null) return SRC_OVER;

        switch (mode)
        {
            case CLEAR: return CLEAR;
            case SRC: return SRC;
            case DST: return DST;
            case DST_OVER: return DST_OVER;
            case SRC_IN: return SRC_IN;
            case DST_IN: return DST_IN;
            case SRC_OUT: return SRC_OUT;
            case DST_OUT: return DST_OUT;
            case SRC_ATOP: return SRC_ATOP;
            case DST_ATOP: return DST_ATOP;
            case XOR: return XOR;
            case DARKEN: return DARKEN;
            case LIGHTEN: return LIGHTEN;
            case MULTIPLY: return MULTIPLY;
            case SCREEN: return SCREEN;
            case ADD: return PLUS;
            case OVERLAY: return OVERLAY;
            default: return SRC_OVER;
        }
    }
}
