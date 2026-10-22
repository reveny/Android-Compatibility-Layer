package android.text.style;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.LocaleList;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.StyleableSet;

public class TextAppearanceSpan extends MetricAffectingSpan implements ParcelableSpan
{
    private static StyleableSet sStyleable;

    private final String mFamilyName;
    private final int mStyle;
    private final int mTextSize;
    private final ColorStateList mTextColor;
    private final ColorStateList mTextColorLink;
    private final Typeface mTypeface;
    private final int mTextFontWeight;
    private final LocaleList mTextLocales;
    private final float mShadowRadius;
    private final float mShadowDx;
    private final float mShadowDy;
    private final int mShadowColor;
    private final boolean mHasElegantTextHeight;
    private final boolean mElegantTextHeight;
    private final boolean mHasLetterSpacing;
    private final float mLetterSpacing;
    private final String mFontFeatureSettings;
    private final String mFontVariationSettings;

    public TextAppearanceSpan(Context context, int appearance)
    {
        this(context, appearance, -1);
    }

    public TextAppearanceSpan(Context context, int appearance, int colorList)
    {
        StyleableSet styleable = styleable(context);
        TypedArray array = styleable.isEmpty() ? null : context.obtainStyledAttributes(appearance, styleable.attributes());

        ColorStateList textColor = null;
        ColorStateList linkColor = null;
        int size = -1;
        int style = Typeface.NORMAL;
        String family = null;
        Typeface typeface = null;
        int weight = -1;
        float shadowRadius = 0f;
        float shadowDx = 0f;
        float shadowDy = 0f;
        int shadowColor = 0;
        boolean hasElegant = false;
        boolean elegant = false;
        boolean hasLetterSpacing = false;
        float letterSpacing = 0f;
        String featureSettings = null;
        String variationSettings = null;
        LocaleList locales = null;

        if (array != null)
        {
            textColor = colorStateList(array, styleable.indexOf("textColor"));
            linkColor = colorStateList(array, styleable.indexOf("textColorLink"));
            size = dimensionPixelSize(array, styleable.indexOf("textSize"), -1);
            style = integer(array, styleable.indexOf("textStyle"), Typeface.NORMAL);
            weight = integer(array, styleable.indexOf("textFontWeight"), -1);
            shadowRadius = floatValue(array, styleable.indexOf("shadowRadius"), 0f);
            shadowDx = floatValue(array, styleable.indexOf("shadowDx"), 0f);
            shadowDy = floatValue(array, styleable.indexOf("shadowDy"), 0f);
            shadowColor = color(array, styleable.indexOf("shadowColor"), 0);

            int elegantIndex = styleable.indexOf("elegantTextHeight");
            hasElegant = elegantIndex >= 0 && array.hasValue(elegantIndex);
            elegant = hasElegant && array.getBoolean(elegantIndex, false);

            int spacingIndex = styleable.indexOf("letterSpacing");
            hasLetterSpacing = spacingIndex >= 0 && array.hasValue(spacingIndex);
            letterSpacing = hasLetterSpacing ? array.getFloat(spacingIndex, 0f) : 0f;

            featureSettings = string(array, styleable.indexOf("fontFeatureSettings"));
            variationSettings = string(array, styleable.indexOf("fontVariationSettings"));

            String localeTags = string(array, styleable.indexOf("textLocale"));
            if (localeTags != null) locales = LocaleList.forLanguageTags(localeTags);

            int fontIndex = styleable.indexOf("fontFamily");
            if (fontIndex >= 0 && array.hasValue(fontIndex))
            {
                try
                {
                    typeface = array.getFont(fontIndex);
                }
                catch (RuntimeException exception)
                {
                    typeface = null;
                }

                if (typeface == null) family = array.getString(fontIndex);
            }

            if (typeface == null && family == null)
            {
                int typefaceIndex = integer(array, styleable.indexOf("typeface"), 0);
                if (typefaceIndex == 1) family = "sans";
                else if (typefaceIndex == 2) family = "serif";
                else if (typefaceIndex == 3) family = "monospace";
            }

            array.recycle();
        }

        if (colorList >= 0)
        {
            try
            {
                textColor = context.getColorStateList(colorList);
            }
            catch (RuntimeException exception)
            {
                textColor = null;
            }
        }

        mTextColor = textColor;
        mTextColorLink = linkColor;
        mTextSize = size;
        mStyle = style;
        mFamilyName = family;
        mTypeface = typeface;
        mTextFontWeight = weight;
        mShadowRadius = shadowRadius;
        mShadowDx = shadowDx;
        mShadowDy = shadowDy;
        mShadowColor = shadowColor;
        mHasElegantTextHeight = hasElegant;
        mElegantTextHeight = elegant;
        mHasLetterSpacing = hasLetterSpacing;
        mLetterSpacing = letterSpacing;
        mFontFeatureSettings = featureSettings;
        mFontVariationSettings = variationSettings;
        mTextLocales = locales;
    }

    public TextAppearanceSpan(String family, int style, int size, ColorStateList color, ColorStateList linkColor)
    {
        mFamilyName = family;
        mStyle = style;
        mTextSize = size;
        mTextColor = color;
        mTextColorLink = linkColor;
        mTypeface = null;
        mTextFontWeight = -1;
        mTextLocales = null;
        mShadowRadius = 0f;
        mShadowDx = 0f;
        mShadowDy = 0f;
        mShadowColor = 0;
        mHasElegantTextHeight = false;
        mElegantTextHeight = false;
        mHasLetterSpacing = false;
        mLetterSpacing = 0f;
        mFontFeatureSettings = null;
        mFontVariationSettings = null;
    }

    public TextAppearanceSpan(Parcel source)
    {
        mFamilyName = source.readString();
        mStyle = source.readInt();
        mTextSize = source.readInt();
        mTextColor = (ColorStateList)source.readValue(null);
        mTextColorLink = (ColorStateList)source.readValue(null);
        mTypeface = (Typeface)source.readValue(null);
        mTextFontWeight = source.readInt();
        String locales = source.readString();
        mTextLocales = locales == null ? null : LocaleList.forLanguageTags(locales);
        mShadowRadius = source.readFloat();
        mShadowDx = source.readFloat();
        mShadowDy = source.readFloat();
        mShadowColor = source.readInt();
        mHasElegantTextHeight = source.readInt() != 0;
        mElegantTextHeight = source.readInt() != 0;
        mHasLetterSpacing = source.readInt() != 0;
        mLetterSpacing = source.readFloat();
        mFontFeatureSettings = source.readString();
        mFontVariationSettings = source.readString();
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "textColor", "textColorLink", "textSize", "textStyle",
                                         "textFontWeight", "shadowRadius", "shadowDx", "shadowDy", "shadowColor",
                                         "elegantTextHeight", "letterSpacing", "fontFeatureSettings", "fontVariationSettings",
                                         "textLocale", "fontFamily", "typeface");
        }
        return sStyleable;
    }

    private static ColorStateList colorStateList(TypedArray array, int index)
    {
        if (index < 0 || !array.hasValue(index)) return null;

        return array.getColorStateList(index);
    }

    private static int dimensionPixelSize(TypedArray array, int index, int defaultValue)
    {
        return index < 0 ? defaultValue : array.getDimensionPixelSize(index, defaultValue);
    }

    private static int integer(TypedArray array, int index, int defaultValue)
    {
        return index < 0 ? defaultValue : array.getInt(index, defaultValue);
    }

    private static float floatValue(TypedArray array, int index, float defaultValue)
    {
        return index < 0 ? defaultValue : array.getFloat(index, defaultValue);
    }

    private static int color(TypedArray array, int index, int defaultValue)
    {
        return index < 0 ? defaultValue : array.getColor(index, defaultValue);
    }

    private static String string(TypedArray array, int index)
    {
        return index < 0 ? null : array.getString(index);
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.TEXT_APPEARANCE_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mFamilyName);
        dest.writeInt(mStyle);
        dest.writeInt(mTextSize);
        dest.writeValue(mTextColor);
        dest.writeValue(mTextColorLink);
        dest.writeValue(mTypeface);
        dest.writeInt(mTextFontWeight);
        dest.writeString(mTextLocales == null ? null : mTextLocales.toLanguageTags());
        dest.writeFloat(mShadowRadius);
        dest.writeFloat(mShadowDx);
        dest.writeFloat(mShadowDy);
        dest.writeInt(mShadowColor);
        dest.writeInt(mHasElegantTextHeight ? 1 : 0);
        dest.writeInt(mElegantTextHeight ? 1 : 0);
        dest.writeInt(mHasLetterSpacing ? 1 : 0);
        dest.writeFloat(mLetterSpacing);
        dest.writeString(mFontFeatureSettings);
        dest.writeString(mFontVariationSettings);
    }

    public String getFamily()
    {
        return mFamilyName;
    }

    public ColorStateList getTextColor()
    {
        return mTextColor;
    }

    public ColorStateList getLinkTextColor()
    {
        return mTextColorLink;
    }

    public int getTextSize()
    {
        return mTextSize;
    }

    public int getTextStyle()
    {
        return mStyle;
    }

    public int getTextFontWeight()
    {
        return mTextFontWeight;
    }

    public LocaleList getTextLocales()
    {
        return mTextLocales;
    }

    public Typeface getTypeface()
    {
        return mTypeface;
    }

    public int getShadowColor()
    {
        return mShadowColor;
    }

    public float getShadowDx()
    {
        return mShadowDx;
    }

    public float getShadowDy()
    {
        return mShadowDy;
    }

    public float getShadowRadius()
    {
        return mShadowRadius;
    }

    public String getFontFeatureSettings()
    {
        return mFontFeatureSettings;
    }

    public String getFontVariationSettings()
    {
        return mFontVariationSettings;
    }

    public boolean isElegantTextHeight()
    {
        return mElegantTextHeight;
    }

    public float getLetterSpacing()
    {
        return mLetterSpacing;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint == null) return;

        updateMeasureState(paint);

        if (mTextColor != null) paint.setColor(mTextColor.getColorForState(paint.drawableState, 0));
        if (mTextColorLink != null) paint.linkColor = mTextColorLink.getColorForState(paint.drawableState, 0);
        if (mShadowColor != 0) paint.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, mShadowColor);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        if (paint == null) return;

        Typeface styled = null;
        int style = 0;

        if (mTypeface != null)
        {
            style = mStyle;
            styled = Typeface.create(mTypeface, style);
        }
        else if (mFamilyName != null || mStyle != 0)
        {
            Typeface old = paint.getTypeface();
            if (old != null) style = old.getStyle();

            style |= mStyle;

            if (mFamilyName != null) styled = Typeface.create(mFamilyName, style);
            else if (old == null) styled = Typeface.defaultFromStyle(style);
            else styled = Typeface.create(old, style);
        }

        if (styled != null)
        {
            if (mTextFontWeight >= 0) styled = Typeface.create(styled, Math.min(1000, mTextFontWeight), (style & Typeface.ITALIC) != 0);

            int fake = style & ~styled.getStyle();
            if ((fake & Typeface.BOLD) != 0) paint.setFakeBoldText(true);
            if ((fake & Typeface.ITALIC) != 0) paint.setTextSkewX(-0.25f);

            paint.setTypeface(styled);
        }

        if (mTextSize > 0) paint.setTextSize(mTextSize);
        if (mTextLocales != null) paint.setTextLocales(mTextLocales);
        if (mHasElegantTextHeight) paint.setElegantTextHeight(mElegantTextHeight);
        if (mHasLetterSpacing) paint.setLetterSpacing(mLetterSpacing);
        if (mFontFeatureSettings != null) paint.setFontFeatureSettings(mFontFeatureSettings);
    }

    @Override
    public String toString()
    {
        return "TextAppearanceSpan{familyName='" + mFamilyName + "', style=" + mStyle + ", textSize=" + mTextSize + ", textColor="
               + mTextColor + ", textColorLink=" + mTextColorLink + ", typeface=" + mTypeface + ", textFontWeight=" + mTextFontWeight
               + ", textLocales=" + mTextLocales + ", shadowRadius=" + mShadowRadius + ", shadowDx=" + mShadowDx + ", shadowDy="
               + mShadowDy + ", shadowColor=#" + String.format("%08X", mShadowColor) + ", elegantTextHeight=" + mElegantTextHeight
               + ", letterSpacing=" + mLetterSpacing + ", fontFeatureSettings='" + mFontFeatureSettings
               + "', fontVariationSettings='" + mFontVariationSettings + "'}";
    }
}
