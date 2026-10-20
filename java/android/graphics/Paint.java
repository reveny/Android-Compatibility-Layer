package android.graphics;

public class Paint
{
    public static final int ANTI_ALIAS_FLAG = 0x01;
    public static final int FILTER_BITMAP_FLAG = 0x02;
    public static final int DITHER_FLAG = 0x04;
    public static final int UNDERLINE_TEXT_FLAG = 0x08;
    public static final int STRIKE_THRU_TEXT_FLAG = 0x10;
    public static final int FAKE_BOLD_TEXT_FLAG = 0x20;
    public static final int LINEAR_TEXT_FLAG = 0x40;
    public static final int SUBPIXEL_TEXT_FLAG = 0x80;
    public static final int DEV_KERN_TEXT_FLAG = 0x100;
    public static final int EMBEDDED_BITMAP_TEXT_FLAG = 0x400;

    private static final float DEFAULT_TEXT_SIZE = 16.0f;

    private final float[] mMetricsScratch = new float[5];
    private final int[] mBoundsScratch = new int[4];
    private final float[] mBreakScratch = new float[1];

    private int mStyleId;
    private int mFlags;
    private int mColor = 0xFF000000;
    private float mTextSize = DEFAULT_TEXT_SIZE;
    private float mTextScaleX = 1.0f;
    private float mTextSkewX;
    private float mLetterSpacing;
    private float mStrokeWidth;
    private float mStrokeMiter = 4.0f;
    private Style mStyle = Style.FILL;
    private Cap mStrokeCap = Cap.BUTT;
    private Join mStrokeJoin = Join.MITER;
    private Align mTextAlign = Align.LEFT;
    private Typeface mTypeface;
    private String mFontFeatureSettings;
    private java.util.Locale mTextLocale = java.util.Locale.getDefault();
    private int mHinting = HINTING_ON;
    private boolean mElegantTextHeight;
    private Shader mShader;
    private ColorFilter mColorFilter;
    private BlendMode mBlendMode = BlendMode.SRC_OVER;
    private MaskFilter mMaskFilter;
    private PathEffect mPathEffect;

    private final int[] mPaintInts = new int[11];
    private final float[] mPaintFloats = new float[13];

    public Paint()
    {
    }

    public Paint(int flags)
    {
        mFlags = flags;
    }

    public Paint(Paint from)
    {
        set(from);
    }

    public void set(Paint from)
    {
        if (from == null) return;

        mFlags = from.mFlags;
        mColor = from.mColor;
        mTextSize = from.mTextSize;
        mTextScaleX = from.mTextScaleX;
        mTextSkewX = from.mTextSkewX;
        mLetterSpacing = from.mLetterSpacing;
        mStrokeWidth = from.mStrokeWidth;
        mStrokeMiter = from.mStrokeMiter;
        mStyle = from.mStyle;
        mStrokeCap = from.mStrokeCap;
        mStrokeJoin = from.mStrokeJoin;
        mTextAlign = from.mTextAlign;
        mTypeface = from.mTypeface;
        mFontFeatureSettings = from.mFontFeatureSettings;
        mShader = from.mShader;
        mColorFilter = from.mColorFilter;
        mBlendMode = from.mBlendMode;
        mMaskFilter = from.mMaskFilter;
        mPathEffect = from.mPathEffect;
        mStyleId = 0;
    }

    public static final int HINTING_OFF = 0;
    public static final int HINTING_ON = 1;

    public java.util.Locale getTextLocale()
    {
        return mTextLocale;
    }

    public void setTextLocale(java.util.Locale locale)
    {
        mTextLocale = locale == null ? java.util.Locale.getDefault() : locale;
    }

    public android.os.LocaleList getTextLocales()
    {
        return android.os.LocaleList.getDefault();
    }

    public void setTextLocales(android.os.LocaleList locales)
    {
        if (locales != null && locales.size() > 0) mTextLocale = locales.get(0);
    }

    public void setHinting(int mode)
    {
        mHinting = mode;
    }

    public int getHinting()
    {
        return mHinting;
    }

    public void setElegantTextHeight(boolean elegant)
    {
        mElegantTextHeight = elegant;
    }

    public boolean isElegantTextHeight()
    {
        return mElegantTextHeight;
    }

    public Cap getStrokeCap()
    {
        return mStrokeCap;
    }

    public void setStrokeCap(Cap cap)
    {
        mStrokeCap = cap == null ? Cap.BUTT : cap;
    }

    public Join getStrokeJoin()
    {
        return mStrokeJoin;
    }

    public void setStrokeJoin(Join join)
    {
        mStrokeJoin = join == null ? Join.MITER : join;
    }

    public Shader getShader()
    {
        return mShader;
    }

    public Shader setShader(Shader shader)
    {
        mShader = shader;
        return shader;
    }

    public ColorFilter getColorFilter()
    {
        return mColorFilter;
    }

    public ColorFilter setColorFilter(ColorFilter filter)
    {
        mColorFilter = filter;
        return filter;
    }

    public BlendMode getBlendMode()
    {
        return mBlendMode;
    }

    public void setBlendMode(BlendMode mode)
    {
        mBlendMode = mode == null ? BlendMode.SRC_OVER : mode;
    }

    public Xfermode getXfermode()
    {
        return null;
    }

    public Xfermode setXfermode(Xfermode mode)
    {
        if (mode instanceof PorterDuffXfermode) mBlendMode = ((PorterDuffXfermode) mode).blendMode();
        else if (mode == null) mBlendMode = BlendMode.SRC_OVER;

        return mode;
    }

    public MaskFilter getMaskFilter()
    {
        return mMaskFilter;
    }

    public MaskFilter setMaskFilter(MaskFilter filter)
    {
        mMaskFilter = filter;
        return filter;
    }

    public PathEffect getPathEffect()
    {
        return mPathEffect;
    }

    public PathEffect setPathEffect(PathEffect effect)
    {
        mPathEffect = effect;
        return effect;
    }

    int[] snapshotInts()
    {
        return snapshotInts(-1);
    }

    int[] snapshotInts(int styleOverride)
    {
        int color = mColor;

        if (mColorFilter instanceof PorterDuffColorFilter) color = ((PorterDuffColorFilter) mColorFilter).apply(color);
        else if (mColorFilter instanceof BlendModeColorFilter) color = ((BlendModeColorFilter) mColorFilter).apply(color);

        mPaintInts[0] = color;
        mPaintInts[1] = styleOverride >= 0 ? styleOverride : (mStyle == null ? 0 : mStyle.ordinal());
        mPaintInts[2] = mStrokeCap.ordinal();
        mPaintInts[3] = mStrokeJoin.ordinal();
        mPaintInts[4] = mBlendMode.nativeId();
        mPaintInts[5] = 255;
        mPaintInts[6] = mShader == null ? Shader.KIND_NONE : mShader.kind();
        mPaintInts[7] = mShader == null ? 0 : mShader.tileModeId();
        mPaintInts[8] = isFilterBitmap() ? 1 : 0;
        mPaintInts[9] = tintColour();
        mPaintInts[10] = mColorFilter != null ? 1 : 0;

        return mPaintInts;
    }

    float[] snapshotFloats()
    {
        mPaintFloats[0] = mStrokeWidth;
        mPaintFloats[1] = mStrokeMiter;
        mPaintFloats[2] = mShader == null ? 0 : mShader.startX();
        mPaintFloats[3] = mShader == null ? 0 : mShader.startY();
        mPaintFloats[4] = mShader == null ? 0 : mShader.endX();
        mPaintFloats[5] = mShader == null ? 0 : mShader.endY();
        mPaintFloats[6] = mShader == null ? 0 : mShader.radius();

        Matrix local = mShader == null ? null : mShader.localMatrix();

        float[] values = new float[9];
        (local == null ? Matrix.IDENTITY_MATRIX : local).getValues(values);

        System.arraycopy(values, 0, mPaintFloats, 7, 6);
        return mPaintFloats;
    }

    int[] snapshotStopColors()
    {
        return mShader == null ? null : mShader.stopColors();
    }

    float[] snapshotStopPositions()
    {
        return mShader == null ? null : mShader.stopPositions();
    }

    private int tintColour()
    {
        if (mColorFilter instanceof BlendModeColorFilter) return ((BlendModeColorFilter) mColorFilter).getColor();
        if (mColorFilter instanceof PorterDuffColorFilter) return ((PorterDuffColorFilter) mColorFilter).getColor();

        return 0;
    }

    long shaderBitmap()
    {
        return mShader == null ? 0 : mShader.bitmapHandle();
    }

    float blurRadius()
    {
        return mMaskFilter instanceof BlurMaskFilter ? ((BlurMaskFilter) mMaskFilter).getRadius() : 0;
    }

    float[] dashIntervals()
    {
        return mPathEffect instanceof DashPathEffect ? ((DashPathEffect) mPathEffect).intervals() : null;
    }

    float dashPhase()
    {
        return mPathEffect instanceof DashPathEffect ? ((DashPathEffect) mPathEffect).phase() : 0;
    }

    public void reset()
    {
        set(new Paint());
    }

    public int getFlags()
    {
        return mFlags;
    }

    public void setFlags(int flags)
    {
        mFlags = flags;
        mStyleId = 0;
    }

    public boolean isAntiAlias()
    {
        return (mFlags & ANTI_ALIAS_FLAG) != 0;
    }

    public void setAntiAlias(boolean enabled)
    {
        setFlagBit(ANTI_ALIAS_FLAG, enabled);
    }

    public boolean isDither()
    {
        return (mFlags & DITHER_FLAG) != 0;
    }

    public void setDither(boolean enabled)
    {
        setFlagBit(DITHER_FLAG, enabled);
    }

    public boolean isFilterBitmap()
    {
        return (mFlags & FILTER_BITMAP_FLAG) != 0;
    }

    public void setFilterBitmap(boolean enabled)
    {
        setFlagBit(FILTER_BITMAP_FLAG, enabled);
    }

    public boolean isFakeBoldText()
    {
        return (mFlags & FAKE_BOLD_TEXT_FLAG) != 0;
    }

    public void setFakeBoldText(boolean enabled)
    {
        setFlagBit(FAKE_BOLD_TEXT_FLAG, enabled);
        mStyleId = 0;
    }

    public boolean isUnderlineText()
    {
        return (mFlags & UNDERLINE_TEXT_FLAG) != 0;
    }

    public void setUnderlineText(boolean enabled)
    {
        setFlagBit(UNDERLINE_TEXT_FLAG, enabled);
    }

    public boolean isStrikeThruText()
    {
        return (mFlags & STRIKE_THRU_TEXT_FLAG) != 0;
    }

    public void setStrikeThruText(boolean enabled)
    {
        setFlagBit(STRIKE_THRU_TEXT_FLAG, enabled);
    }

    public boolean isSubpixelText()
    {
        return (mFlags & SUBPIXEL_TEXT_FLAG) != 0;
    }

    public void setSubpixelText(boolean enabled)
    {
        setFlagBit(SUBPIXEL_TEXT_FLAG, enabled);
    }

    public int getColor()
    {
        return mColor;
    }

    public void setColor(int color)
    {
        mColor = color;
    }

    private float mShadowRadius;
    private float mShadowDx;
    private float mShadowDy;
    private int mShadowColor;

    public void setShadowLayer(float radius, float dx, float dy, int shadowColor)
    {
        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;
        mShadowColor = shadowColor;
    }

    public void clearShadowLayer()
    {
        setShadowLayer(0.0f, 0.0f, 0.0f, 0);
    }

    public float getShadowLayerRadius()
    {
        return mShadowRadius;
    }

    public float getShadowLayerDx()
    {
        return mShadowDx;
    }

    public float getShadowLayerDy()
    {
        return mShadowDy;
    }

    public int getShadowLayerColor()
    {
        return mShadowColor;
    }

    public int getAlpha()
    {
        return mColor >>> 24;
    }

    public void setAlpha(int alpha)
    {
        int clamped = alpha < 0 ? 0 : (alpha > 255 ? 255 : alpha);
        mColor = (mColor & 0x00FFFFFF) | (clamped << 24);
    }

    public Style getStyle()
    {
        return mStyle;
    }

    public void setStyle(Style style)
    {
        mStyle = style;
    }

    public float getStrokeWidth()
    {
        return mStrokeWidth;
    }

    public void setStrokeWidth(float width)
    {
        mStrokeWidth = width;
    }

    public float getStrokeMiter()
    {
        return mStrokeMiter;
    }

    public void setStrokeMiter(float miter)
    {
        mStrokeMiter = miter;
    }

    public float getTextSize()
    {
        return mTextSize;
    }

    public void setTextSize(float size)
    {
        mTextSize = size;
        mStyleId = 0;
    }

    public float getTextScaleX()
    {
        return mTextScaleX;
    }

    public void setTextScaleX(float scaleX)
    {
        mTextScaleX = scaleX;
        mStyleId = 0;
    }

    public float getTextSkewX()
    {
        return mTextSkewX;
    }

    public void setTextSkewX(float skewX)
    {
        mTextSkewX = skewX;
        mStyleId = 0;
    }

    public float getLetterSpacing()
    {
        return mLetterSpacing;
    }

    public void setLetterSpacing(float letterSpacing)
    {
        mLetterSpacing = letterSpacing;
        mStyleId = 0;
    }

    public String getFontFeatureSettings()
    {
        return mFontFeatureSettings;
    }

    public void setFontFeatureSettings(String settings)
    {
        mFontFeatureSettings = settings;
    }

    public Align getTextAlign()
    {
        return mTextAlign;
    }

    public void setTextAlign(Align align)
    {
        mTextAlign = align;
    }

    public Typeface getTypeface()
    {
        return mTypeface;
    }

    public Typeface setTypeface(Typeface typeface)
    {
        mTypeface = typeface;
        mStyleId = 0;
        return typeface;
    }

    public float measureText(String text)
    {
        if (text == null) return 0.0f;
        return measureText(text, 0, text.length());
    }

    public float measureText(CharSequence text, int start, int end)
    {
        if (text == null || end <= start) return 0.0f;

        char[] chars = new char[end - start];
        for (int index = start; index < end; index++)
        {
            chars[index - start] = text.charAt(index);
        }

        return measureText(chars, 0, chars.length);
    }

    public float measureText(char[] text, int index, int count)
    {
        if (text == null || count <= 0) return 0.0f;
        return runtime.TextMeasurers.get().measureText(styleId(), text, index, count);
    }

    public int styleId()
    {
        if (mStyleId != 0) return mStyleId;

        Typeface typeface = mTypeface != null ? mTypeface : Typeface.DEFAULT;
        int font = typeface != null ? typeface.getNativeFont() : 0;
        int flags = isFakeBoldText() ? 1 : 0;

        mStyleId = runtime.TextMeasurers.get().internStyle(font, mTextSize, mTextScaleX, mTextSkewX, mLetterSpacing, flags);
        return mStyleId;
    }

    public FontMetrics getFontMetrics()
    {
        FontMetrics metrics = new FontMetrics();
        getFontMetrics(metrics);
        return metrics;
    }

    public float getFontMetrics(FontMetrics metrics)
    {
        runtime.TextMeasurers.get().fontMetrics(styleId(), mMetricsScratch);

        if (metrics != null)
        {
            metrics.top = mMetricsScratch[0];
            metrics.ascent = mMetricsScratch[1];
            metrics.descent = mMetricsScratch[2];
            metrics.bottom = mMetricsScratch[3];
            metrics.leading = mMetricsScratch[4];
        }

        return mMetricsScratch[2] - mMetricsScratch[1];
    }

    public FontMetricsInt getFontMetricsInt()
    {
        FontMetricsInt metrics = new FontMetricsInt();
        getFontMetricsInt(metrics);
        return metrics;
    }

    public int getFontMetricsInt(FontMetricsInt metrics)
    {
        runtime.TextMeasurers.get().fontMetrics(styleId(), mMetricsScratch);

        int top = (int)Math.floor(mMetricsScratch[0]);
        int ascent = Math.round(mMetricsScratch[1]);
        int descent = Math.round(mMetricsScratch[2]);
        int bottom = (int)Math.ceil(mMetricsScratch[3]);
        int leading = Math.round(mMetricsScratch[4]);

        if (metrics != null)
        {
            metrics.top = top;
            metrics.ascent = ascent;
            metrics.descent = descent;
            metrics.bottom = bottom;
            metrics.leading = leading;
        }

        return descent - ascent;
    }

    public float ascent()
    {
        runtime.TextMeasurers.get().fontMetrics(styleId(), mMetricsScratch);
        return mMetricsScratch[1];
    }

    public float descent()
    {
        runtime.TextMeasurers.get().fontMetrics(styleId(), mMetricsScratch);
        return mMetricsScratch[2];
    }

    public float getFontSpacing()
    {
        runtime.TextMeasurers.get().fontMetrics(styleId(), mMetricsScratch);
        return mMetricsScratch[3] - mMetricsScratch[0];
    }

    public void getTextBounds(String text, int start, int end, Rect bounds)
    {
        if (text == null || bounds == null) return;
        getTextBounds(text.toCharArray(), start, end - start, bounds);
    }

    public void getTextBounds(CharSequence text, int start, int end, Rect bounds)
    {
        if (text == null || bounds == null) return;

        getTextBounds(text.toString(), start, end, bounds);
    }

    public void getTextBounds(char[] text, int index, int count, Rect bounds)
    {
        if (bounds == null) return;
        if (text == null || count <= 0)
        {
            bounds.setEmpty();
            return;
        }

        runtime.TextMeasurers.get().textBounds(styleId(), text, index, count, mBoundsScratch);
        bounds.set(mBoundsScratch[0], mBoundsScratch[1], mBoundsScratch[2], mBoundsScratch[3]);
    }

    public int getTextWidths(String text, int start, int end, float[] widths)
    {
        if (text == null || widths == null) return 0;
        return getTextWidths(text.toCharArray(), start, end - start, widths);
    }

    public int getTextWidths(String text, float[] widths)
    {
        if (text == null) return 0;
        return getTextWidths(text, 0, text.length(), widths);
    }

    public int getTextWidths(char[] text, int index, int count, float[] widths)
    {
        if (text == null || widths == null || count <= 0) return 0;
        if (widths.length < count) throw new ArrayIndexOutOfBoundsException("widths is shorter than the run");

        return runtime.TextMeasurers.get().textWidths(styleId(), text, index, count, widths);
    }

    public int breakText(String text, boolean measureForwards, float maxWidth, float[] measuredWidth)
    {
        if (text == null) return 0;
        return breakText(text.toCharArray(), 0, text.length(), maxWidth, measureForwards, measuredWidth);
    }

    public int breakText(char[] text, int index, int count, float maxWidth, float[] measuredWidth)
    {
        return breakText(text, index, count, maxWidth, count >= 0, measuredWidth);
    }

    private int breakText(char[] text, int index, int count, float maxWidth, boolean forwards, float[] measuredWidth)
    {
        if (text == null || count <= 0) return 0;

        int broke = runtime.TextMeasurers.get().breakText(styleId(), text, index, count, maxWidth, forwards, mBreakScratch);
        if (measuredWidth != null && measuredWidth.length > 0) measuredWidth[0] = mBreakScratch[0];
        return broke;
    }

    public float getRunAdvance(char[] text, int start, int end, int contextStart, int contextEnd, boolean rightToLeft, int offset)
    {
        if (text == null || offset <= start) return 0.0f;
        return measureText(text, start, Math.min(offset, end) - start);
    }

    public boolean hasGlyph(String glyph)
    {
        if (glyph == null || glyph.isEmpty()) return false;
        return runtime.TextMeasurers.get().hasGlyph(styleId(), glyph);
    }

    public static native int nativeInternStyle(int font, float textSize, float scaleX, float skewX, float letterSpacing, int flags);

    public static native float nativeMeasureText(int style, char[] text, int index, int count);

    public static native int nativeGetTextWidths(int style, char[] text, int index, int count, float[] outWidths);

    public static native void nativeGetFontMetrics(int style, float[] outMetrics);

    public static native void nativeGetTextBounds(int style, char[] text, int index, int count, int[] outBounds);

    public static native int nativeBreakText(int style, char[] text, int index, int count, float maxWidth, boolean forwards,
                                             float[] outMeasured);

    public static native boolean nativeHasGlyph(int style, String text);

    private void setFlagBit(int bit, boolean enabled)
    {
        if (enabled)
        {
            mFlags |= bit;
            return;
        }
        mFlags &= ~bit;
    }

    public enum Style
    {
        FILL,
        STROKE,
        FILL_AND_STROKE
    }

    public enum Cap
    {
        BUTT,
        ROUND,
        SQUARE
    }

    public enum Join
    {
        MITER,
        ROUND,
        BEVEL
    }

    public enum Align
    {
        LEFT,
        CENTER,
        RIGHT
    }

    public static class FontMetrics
    {
        public float top;
        public float ascent;
        public float descent;
        public float bottom;
        public float leading;
    }

    public static class FontMetricsInt
    {
        public int top;
        public int ascent;
        public int descent;
        public int bottom;
        public int leading;
    }
}
