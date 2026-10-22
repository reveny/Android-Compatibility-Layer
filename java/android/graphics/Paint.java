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
        mTextLocale = from.mTextLocale;
        mHinting = from.mHinting;
        mElegantTextHeight = from.mElegantTextHeight;
        mShadowRadius = from.mShadowRadius;
        mShadowDx = from.mShadowDx;
        mShadowDy = from.mShadowDy;
        mShadowColor = from.mShadowColor;
        mWordSpacing = from.mWordSpacing;
        mFontVariationSettings = from.mFontVariationSettings;
        mStartHyphenEdit = from.mStartHyphenEdit;
        mEndHyphenEdit = from.mEndHyphenEdit;
        mStyleId = 0;
    }

    public static final int CURSOR_AFTER = 0;
    public static final int CURSOR_AT_OR_AFTER = 1;
    public static final int CURSOR_BEFORE = 2;
    public static final int CURSOR_AT_OR_BEFORE = 3;
    public static final int CURSOR_AT = 4;

    public static final int START_HYPHEN_EDIT_NO_EDIT = 0;
    public static final int START_HYPHEN_EDIT_INSERT_HYPHEN = 1;
    public static final int START_HYPHEN_EDIT_INSERT_ZWJ = 2;

    public static final int END_HYPHEN_EDIT_NO_EDIT = 0;
    public static final int END_HYPHEN_EDIT_REPLACE_WITH_HYPHEN = 1;
    public static final int END_HYPHEN_EDIT_INSERT_HYPHEN = 2;
    public static final int END_HYPHEN_EDIT_INSERT_ARMENIAN_HYPHEN = 3;
    public static final int END_HYPHEN_EDIT_INSERT_MAQAF = 4;
    public static final int END_HYPHEN_EDIT_INSERT_UCAS_HYPHEN = 5;
    public static final int END_HYPHEN_EDIT_INSERT_ZWJ_AND_HYPHEN = 6;

    private static final float UNDERLINE_POSITION_RATIO = 1.0f / 9.0f;
    private static final float DECORATION_THICKNESS_RATIO = 1.0f / 18.0f;
    private static final float STRIKE_THRU_POSITION_RATIO = -0.27f;

    private float mWordSpacing;
    private String mFontVariationSettings;
    private int mStartHyphenEdit;
    private int mEndHyphenEdit;

    public boolean isLinearText()
    {
        return (mFlags & LINEAR_TEXT_FLAG) != 0;
    }

    public void setLinearText(boolean enabled)
    {
        setFlagBit(LINEAR_TEXT_FLAG, enabled);
    }

    public float getUnderlinePosition()
    {
        return mTextSize * UNDERLINE_POSITION_RATIO;
    }

    public float getUnderlineThickness()
    {
        return Math.max(1.0f, mTextSize * DECORATION_THICKNESS_RATIO);
    }

    public float getStrikeThruPosition()
    {
        return mTextSize * STRIKE_THRU_POSITION_RATIO;
    }

    public float getStrikeThruThickness()
    {
        return Math.max(1.0f, mTextSize * DECORATION_THICKNESS_RATIO);
    }

    public long getColorLong()
    {
        return Color.pack(mColor);
    }

    public void setColor(long color)
    {
        setColor(Color.toArgb(color));
    }

    public void setARGB(int alpha, int red, int green, int blue)
    {
        setColor(((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF));
    }

    public boolean getFillPath(Path source, Path destination)
    {
        if (source == null || destination == null) return false;

        destination.set(source);
        return mStyle == null || mStyle == Style.FILL;
    }

    public void setShadowLayer(float radius, float dx, float dy, long shadowColor)
    {
        setShadowLayer(radius, dx, dy, Color.toArgb(shadowColor));
    }

    public long getShadowLayerColorLong()
    {
        return Color.pack(mShadowColor);
    }

    boolean hasShadowLayer()
    {
        return (mShadowRadius > 0 || mShadowDx != 0 || mShadowDy != 0) && (mShadowColor >>> 24) != 0;
    }

    public float getWordSpacing()
    {
        return mWordSpacing;
    }

    public void setWordSpacing(float wordSpacing)
    {
        mWordSpacing = wordSpacing;
    }

    public String getFontVariationSettings()
    {
        return mFontVariationSettings;
    }

    public boolean setFontVariationSettings(String settings)
    {
        String trimmed = settings == null ? null : settings.trim();
        if (trimmed != null && trimmed.isEmpty()) trimmed = null;

        if (trimmed != null)
        {
            for (String entry : trimmed.split(","))
            {
                String part = entry.trim();
                if (part.length() < 7 || (part.charAt(0) != '\'' && part.charAt(0) != '"')) return false;
                if (part.charAt(5) != part.charAt(0)) return false;

                try
                {
                    Float.parseFloat(part.substring(6).trim());
                }
                catch (NumberFormatException exception)
                {
                    return false;
                }
            }
        }

        mFontVariationSettings = trimmed;
        mStyleId = 0;
        return true;
    }

    public int getStartHyphenEdit()
    {
        return mStartHyphenEdit;
    }

    public int getEndHyphenEdit()
    {
        return mEndHyphenEdit;
    }

    public void setStartHyphenEdit(int startHyphen)
    {
        mStartHyphenEdit = startHyphen;
    }

    public void setEndHyphenEdit(int endHyphen)
    {
        mEndHyphenEdit = endHyphen;
    }

    public void getFontMetricsInt(CharSequence text, int start, int count, int contextStart, int contextCount, boolean isRtl,
                                  FontMetricsInt outMetrics)
    {
        if (text == null) throw new IllegalArgumentException("text must not be null");
        if (outMetrics == null) throw new IllegalArgumentException("outMetrics must not be null");

        getFontMetricsInt(outMetrics);
    }

    public void getFontMetricsInt(char[] text, int start, int count, int contextStart, int contextCount, boolean isRtl,
                                  FontMetricsInt outMetrics)
    {
        if (text == null) throw new IllegalArgumentException("text must not be null");
        if (outMetrics == null) throw new IllegalArgumentException("outMetrics must not be null");

        getFontMetricsInt(outMetrics);
    }

    public boolean equalsForTextMeasurement(Paint other)
    {
        if (other == null) return false;
        if (other == this) return true;

        return mTextSize == other.mTextSize && mTextScaleX == other.mTextScaleX && mTextSkewX == other.mTextSkewX
            && mLetterSpacing == other.mLetterSpacing && mWordSpacing == other.mWordSpacing && mFlags == other.mFlags
            && mTypeface == other.mTypeface && mStartHyphenEdit == other.mStartHyphenEdit
            && mEndHyphenEdit == other.mEndHyphenEdit && java.util.Objects.equals(mTextLocale, other.mTextLocale)
            && java.util.Objects.equals(mFontFeatureSettings, other.mFontFeatureSettings)
            && java.util.Objects.equals(mFontVariationSettings, other.mFontVariationSettings);
    }

    private static char[] charsOf(CharSequence text, int start, int end)
    {
        char[] chars = new char[end - start];
        for (int index = start; index < end; index++)
        {
            chars[index - start] = text.charAt(index);
        }

        return chars;
    }

    private static void checkRange(int length, int start, int end)
    {
        if ((start | end | (end - start) | (length - end)) < 0) throw new IndexOutOfBoundsException();
    }

    public float measureText(String text, int start, int end)
    {
        if (text == null) throw new IllegalArgumentException("text cannot be null");
        checkRange(text.length(), start, end);

        return measureText((CharSequence) text, start, end);
    }

    public int breakText(CharSequence text, int start, int end, boolean measureForwards, float maxWidth, float[] measuredWidth)
    {
        if (text == null) throw new IllegalArgumentException("text cannot be null");
        checkRange(text.length(), start, end);
        if (end == start) return 0;

        return breakText(charsOf(text, start, end), 0, end - start, maxWidth, measureForwards, measuredWidth);
    }

    public int getTextWidths(CharSequence text, int start, int end, float[] widths)
    {
        if (text == null) throw new IllegalArgumentException("text cannot be null");
        checkRange(text.length(), start, end);
        if (end - start > widths.length) throw new ArrayIndexOutOfBoundsException();
        if (end == start) return 0;

        return getTextWidths(charsOf(text, start, end), 0, end - start, widths);
    }

    public float getTextRunAdvances(char[] chars, int index, int count, int contextIndex, int contextCount, boolean isRtl,
                                    float[] advances, int advancesIndex)
    {
        if (chars == null) throw new IllegalArgumentException("text cannot be null");
        if ((index | count | contextIndex | contextCount | advancesIndex | (index - contextIndex)
             | (contextCount - count) | ((contextIndex + contextCount) - (index + count))
             | (chars.length - (contextIndex + contextCount))
             | (advances == null ? 0 : (advances.length - (advancesIndex + count)))) < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        if (count == 0) return 0;

        float[] widths = new float[count];
        getTextWidths(chars, index, count, widths);

        float total = 0;
        for (int offset = 0; offset < count; offset++)
        {
            total += widths[offset];
            if (advances != null) advances[advancesIndex + offset] = widths[offset];
        }

        return total;
    }

    public int getTextRunCursor(char[] text, int contextStart, int contextLength, boolean isRtl, int offset, int cursorOpt)
    {
        int contextEnd = contextStart + contextLength;
        if (((contextStart | contextEnd | offset | (contextEnd - contextStart) | (offset - contextStart) | (contextEnd - offset)
              | (text.length - contextEnd) | cursorOpt) < 0) || cursorOpt > CURSOR_AT)
        {
            throw new IndexOutOfBoundsException();
        }

        return runCursor(new String(text, contextStart, contextLength), offset - contextStart, cursorOpt, contextStart);
    }

    public int getTextRunCursor(CharSequence text, int contextStart, int contextEnd, boolean isRtl, int offset, int cursorOpt)
    {
        if (((contextStart | contextEnd | offset | (contextEnd - contextStart) | (offset - contextStart) | (contextEnd - offset)
              | (text.length() - contextEnd) | cursorOpt) < 0) || cursorOpt > CURSOR_AT)
        {
            throw new IndexOutOfBoundsException();
        }

        return runCursor(text.subSequence(contextStart, contextEnd).toString(), offset - contextStart, cursorOpt, contextStart);
    }

    private static boolean isCursorBoundary(String text, int position)
    {
        if (position <= 0 || position >= text.length()) return true;

        return !(Character.isHighSurrogate(text.charAt(position - 1)) && Character.isLowSurrogate(text.charAt(position)));
    }

    private static int runCursor(String text, int offset, int cursorOpt, int base)
    {
        int length = text.length();

        switch (cursorOpt)
        {
            case CURSOR_AT:
                return isCursorBoundary(text, offset) ? offset + base : -1;
            case CURSOR_AT_OR_AFTER:
                if (isCursorBoundary(text, offset)) return offset + base;
                return Math.min(length, offset + 1) + base;
            case CURSOR_AT_OR_BEFORE:
                if (isCursorBoundary(text, offset)) return offset + base;
                return Math.max(0, offset - 1) + base;
            case CURSOR_AFTER:
            {
                if (offset >= length) return -1;
                int next = offset + 1;
                while (next < length && !isCursorBoundary(text, next)) next++;
                return next + base;
            }
            default:
            {
                if (offset <= 0) return -1;
                int previous = offset - 1;
                while (previous > 0 && !isCursorBoundary(text, previous)) previous--;
                return previous + base;
            }
        }
    }

    public void getTextPath(char[] text, int index, int count, float x, float y, Path path)
    {
        if ((index | count) < 0 || index + count > text.length) throw new ArrayIndexOutOfBoundsException();
        if (path == null) return;

        path.reset();
        if (count == 0) return;

        float[] widths = new float[count];
        getTextWidths(text, index, count, widths);

        float total = 0;
        for (float width : widths) total += width;

        float cursor = x;
        if (mTextAlign == Align.CENTER) cursor -= total * 0.5f;
        else if (mTextAlign == Align.RIGHT) cursor -= total;

        Rect glyph = new Rect();
        for (int offset = 0; offset < count; offset++)
        {
            char character = text[index + offset];

            if (!Character.isWhitespace(character) && !Character.isLowSurrogate(character))
            {
                int glyphLength = Character.isHighSurrogate(character) && offset + 1 < count ? 2 : 1;
                getTextBounds(text, index + offset, glyphLength, glyph);

                if (!glyph.isEmpty())
                {
                    path.addRect(cursor + glyph.left, y + glyph.top, cursor + glyph.right, y + glyph.bottom, Path.Direction.CW);
                }
            }

            cursor += widths[offset];
        }
    }

    public void getTextPath(String text, int start, int end, float x, float y, Path path)
    {
        if ((start | end | (end - start) | (text.length() - end)) < 0) throw new IndexOutOfBoundsException();

        getTextPath(text.toCharArray(), start, end - start, x, y, path);
    }

    public float getRunAdvance(CharSequence text, int start, int end, int contextStart, int contextEnd, boolean isRtl, int offset)
    {
        if (text == null) throw new IllegalArgumentException("text cannot be null");
        if ((contextStart | start | offset | end | contextEnd | (start - contextStart) | (offset - start) | (end - offset)
             | (contextEnd - end) | (text.length() - contextEnd)) < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        if (end == start) return 0.0f;

        return measureText(text, start, offset);
    }

    public float getRunCharacterAdvance(char[] text, int start, int end, int contextStart, int contextEnd, boolean isRtl,
                                        int offset, float[] advances, int advancesIndex)
    {
        if (text == null) throw new IllegalArgumentException("text cannot be null");
        if ((contextStart | start | offset | end | contextEnd | (start - contextStart) | (offset - start) | (end - offset)
             | (contextEnd - end) | (text.length - contextEnd)) < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        if (end == start) return 0.0f;

        float[] widths = new float[end - start];
        getTextWidths(text, start, end - start, widths);

        if (advances != null)
        {
            if (advancesIndex < 0 || advancesIndex + widths.length > advances.length) throw new IndexOutOfBoundsException();
            System.arraycopy(widths, 0, advances, advancesIndex, widths.length);
        }

        float total = 0;
        for (int index = 0; index < offset - start; index++) total += widths[index];

        return total;
    }

    public float getRunCharacterAdvance(CharSequence text, int start, int end, int contextStart, int contextEnd, boolean isRtl,
                                        int offset, float[] advances, int advancesIndex)
    {
        if (text == null) throw new IllegalArgumentException("text cannot be null");
        if ((contextStart | contextEnd | (contextEnd - contextStart) | (text.length() - contextEnd)) < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        char[] chars = charsOf(text, contextStart, contextEnd);
        return getRunCharacterAdvance(chars, start - contextStart, end - contextStart, 0, contextEnd - contextStart, isRtl,
                                      offset - contextStart, advances, advancesIndex);
    }

    public int getOffsetForAdvance(char[] text, int start, int end, int contextStart, int contextEnd, boolean isRtl,
                                   float advance)
    {
        if (text == null) throw new IllegalArgumentException("text cannot be null");
        if ((contextStart | start | end | contextEnd | (start - contextStart) | (end - start) | (contextEnd - end)
             | (text.length - contextEnd)) < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        if (end == start || advance <= 0) return start;

        float[] widths = new float[end - start];
        getTextWidths(text, start, end - start, widths);

        float walked = 0;
        for (int index = 0; index < widths.length; index++)
        {
            float next = walked + widths[index];

            if (next >= advance)
            {
                int candidate = advance - walked < next - advance ? start + index : start + index + 1;
                if (candidate > start && candidate < end && Character.isLowSurrogate(text[candidate])) candidate--;
                return candidate;
            }

            walked = next;
        }

        return end;
    }

    public int getOffsetForAdvance(CharSequence text, int start, int end, int contextStart, int contextEnd, boolean isRtl,
                                   float advance)
    {
        if (text == null) throw new IllegalArgumentException("text cannot be null");
        if ((contextStart | start | end | contextEnd | (start - contextStart) | (end - start) | (contextEnd - end)
             | (text.length() - contextEnd)) < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        char[] chars = charsOf(text, contextStart, contextEnd);
        return getOffsetForAdvance(chars, start - contextStart, end - contextStart, 0, contextEnd - contextStart, isRtl, advance)
            + contextStart;
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

        if (mColorFilter != null) color = mColorFilter.apply(color);

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
        mPaintInts[10] = tintsImages() ? 1 : 0;

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
        if (mColorFilter != null) return mColorFilter.imageTint();

        return 0;
    }

    private boolean tintsImages()
    {
        if (mColorFilter instanceof BlendModeColorFilter || mColorFilter instanceof PorterDuffColorFilter) return true;

        return mColorFilter != null && mColorFilter.tintsImages();
    }

    float[] dashIntervals(PathEffect effect)
    {
        if (effect instanceof DashPathEffect) return ((DashPathEffect) effect).intervals();
        if (effect instanceof ComposePathEffect) return dashIntervals(((ComposePathEffect) effect).dashSource());
        if (effect instanceof SumPathEffect) return dashIntervals(((SumPathEffect) effect).dashSource());

        return null;
    }

    float dashPhase(PathEffect effect)
    {
        if (effect instanceof DashPathEffect) return ((DashPathEffect) effect).phase();
        if (effect instanceof ComposePathEffect) return dashPhase(((ComposePathEffect) effect).dashSource());
        if (effect instanceof SumPathEffect) return dashPhase(((SumPathEffect) effect).dashSource());

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
        return dashIntervals(mPathEffect);
    }

    float dashPhase()
    {
        return dashPhase(mPathEffect);
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

        @Override
        public String toString()
        {
            return "FontMetricsInt: top=" + top + " ascent=" + ascent + " descent=" + descent + " bottom=" + bottom + " leading="
                + leading;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof FontMetricsInt)) return false;

            FontMetricsInt metrics = (FontMetricsInt) other;
            return top == metrics.top && ascent == metrics.ascent && descent == metrics.descent && bottom == metrics.bottom
                && leading == metrics.leading;
        }

        @Override
        public int hashCode()
        {
            return java.util.Objects.hash(top, ascent, descent, bottom, leading);
        }
    }
}
