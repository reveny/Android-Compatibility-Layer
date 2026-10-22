package android.view.inputmethod;

import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class TextAppearanceInfo implements Parcelable
{
    public static final Parcelable.Creator<TextAppearanceInfo> CREATOR = new Parcelable.Creator<TextAppearanceInfo>()
    {
        @Override
        public TextAppearanceInfo createFromParcel(Parcel in)
        {
            Builder builder = new Builder();
            builder.mTextSize = in.readFloat();
            String locales = in.readString();
            builder.mTextLocales = locales == null ? LocaleList.getEmptyLocaleList() : LocaleList.forLanguageTags(locales);
            builder.mAllCaps = in.readInt() != 0;
            builder.mSystemFontFamilyName = in.readString();
            builder.mTextFontWeight = in.readInt();
            builder.mTextStyle = in.readInt();
            builder.mShadowDx = in.readFloat();
            builder.mShadowDy = in.readFloat();
            builder.mShadowRadius = in.readFloat();
            builder.mShadowColor = in.readInt();
            builder.mElegantTextHeight = in.readInt() != 0;
            builder.mFallbackLineSpacing = in.readInt() != 0;
            builder.mLetterSpacing = in.readFloat();
            builder.mFontFeatureSettings = in.readString();
            builder.mFontVariationSettings = in.readString();
            builder.mLineBreakStyle = in.readInt();
            builder.mLineBreakWordStyle = in.readInt();
            builder.mTextScaleX = in.readFloat();
            builder.mHighlightTextColor = in.readInt();
            builder.mTextColor = in.readInt();
            builder.mHintTextColor = in.readInt();
            builder.mLinkTextColor = in.readInt();
            return builder.build();
        }

        @Override
        public TextAppearanceInfo[] newArray(int size)
        {
            return new TextAppearanceInfo[size];
        }
    };

    private final float mTextSize;
    private final LocaleList mTextLocales;
    private final String mSystemFontFamilyName;
    private final int mTextFontWeight;
    private final int mTextStyle;
    private final boolean mAllCaps;
    private final float mShadowDx;
    private final float mShadowDy;
    private final float mShadowRadius;
    private final int mShadowColor;
    private final boolean mElegantTextHeight;
    private final boolean mFallbackLineSpacing;
    private final float mLetterSpacing;
    private final String mFontFeatureSettings;
    private final String mFontVariationSettings;
    private final int mLineBreakStyle;
    private final int mLineBreakWordStyle;
    private final float mTextScaleX;
    private final int mHighlightTextColor;
    private final int mTextColor;
    private final int mHintTextColor;
    private final int mLinkTextColor;

    private TextAppearanceInfo(Builder builder)
    {
        mTextSize = builder.mTextSize;
        mTextLocales = builder.mTextLocales;
        mSystemFontFamilyName = builder.mSystemFontFamilyName;
        mTextFontWeight = builder.mTextFontWeight;
        mTextStyle = builder.mTextStyle;
        mAllCaps = builder.mAllCaps;
        mShadowDx = builder.mShadowDx;
        mShadowDy = builder.mShadowDy;
        mShadowRadius = builder.mShadowRadius;
        mShadowColor = builder.mShadowColor;
        mElegantTextHeight = builder.mElegantTextHeight;
        mFallbackLineSpacing = builder.mFallbackLineSpacing;
        mLetterSpacing = builder.mLetterSpacing;
        mFontFeatureSettings = builder.mFontFeatureSettings;
        mFontVariationSettings = builder.mFontVariationSettings;
        mLineBreakStyle = builder.mLineBreakStyle;
        mLineBreakWordStyle = builder.mLineBreakWordStyle;
        mTextScaleX = builder.mTextScaleX;
        mHighlightTextColor = builder.mHighlightTextColor;
        mTextColor = builder.mTextColor;
        mHintTextColor = builder.mHintTextColor;
        mLinkTextColor = builder.mLinkTextColor;
    }

    public float getTextSize()
    {
        return mTextSize;
    }

    public LocaleList getTextLocales()
    {
        return mTextLocales;
    }

    public int getTextFontWeight()
    {
        return mTextFontWeight;
    }

    public int getTextStyle()
    {
        return mTextStyle;
    }

    public boolean isAllCaps()
    {
        return mAllCaps;
    }

    public String getSystemFontFamilyName()
    {
        return mSystemFontFamilyName;
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

    public int getShadowColor()
    {
        return mShadowColor;
    }

    public boolean isElegantTextHeight()
    {
        return mElegantTextHeight;
    }

    public boolean isFallbackLineSpacing()
    {
        return mFallbackLineSpacing;
    }

    public float getLetterSpacing()
    {
        return mLetterSpacing;
    }

    public String getFontFeatureSettings()
    {
        return mFontFeatureSettings;
    }

    public String getFontVariationSettings()
    {
        return mFontVariationSettings;
    }

    public int getLineBreakStyle()
    {
        return mLineBreakStyle;
    }

    public int getLineBreakWordStyle()
    {
        return mLineBreakWordStyle;
    }

    public float getTextScaleX()
    {
        return mTextScaleX;
    }

    public int getHighlightTextColor()
    {
        return mHighlightTextColor;
    }

    public int getTextColor()
    {
        return mTextColor;
    }

    public int getHintTextColor()
    {
        return mHintTextColor;
    }

    public int getLinkTextColor()
    {
        return mLinkTextColor;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeFloat(mTextSize);
        dest.writeString(mTextLocales == null ? null : mTextLocales.toLanguageTags());
        dest.writeInt(mAllCaps ? 1 : 0);
        dest.writeString(mSystemFontFamilyName);
        dest.writeInt(mTextFontWeight);
        dest.writeInt(mTextStyle);
        dest.writeFloat(mShadowDx);
        dest.writeFloat(mShadowDy);
        dest.writeFloat(mShadowRadius);
        dest.writeInt(mShadowColor);
        dest.writeInt(mElegantTextHeight ? 1 : 0);
        dest.writeInt(mFallbackLineSpacing ? 1 : 0);
        dest.writeFloat(mLetterSpacing);
        dest.writeString(mFontFeatureSettings);
        dest.writeString(mFontVariationSettings);
        dest.writeInt(mLineBreakStyle);
        dest.writeInt(mLineBreakWordStyle);
        dest.writeFloat(mTextScaleX);
        dest.writeInt(mHighlightTextColor);
        dest.writeInt(mTextColor);
        dest.writeInt(mHintTextColor);
        dest.writeInt(mLinkTextColor);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof TextAppearanceInfo)) return false;

        TextAppearanceInfo that = (TextAppearanceInfo)other;
        return Float.compare(that.mTextSize, mTextSize) == 0 && mTextFontWeight == that.mTextFontWeight && mTextStyle == that.mTextStyle
               && mAllCaps == that.mAllCaps && Float.compare(that.mShadowDx, mShadowDx) == 0
               && Float.compare(that.mShadowDy, mShadowDy) == 0 && Float.compare(that.mShadowRadius, mShadowRadius) == 0
               && mShadowColor == that.mShadowColor && mElegantTextHeight == that.mElegantTextHeight
               && mFallbackLineSpacing == that.mFallbackLineSpacing && Float.compare(that.mLetterSpacing, mLetterSpacing) == 0
               && mLineBreakStyle == that.mLineBreakStyle && mLineBreakWordStyle == that.mLineBreakWordStyle
               && Float.compare(that.mTextScaleX, mTextScaleX) == 0 && mHighlightTextColor == that.mHighlightTextColor
               && mTextColor == that.mTextColor && mLinkTextColor == that.mLinkTextColor && mHintTextColor == that.mHintTextColor
               && Objects.equals(languageTags(mTextLocales), languageTags(that.mTextLocales))
               && Objects.equals(mSystemFontFamilyName, that.mSystemFontFamilyName)
               && Objects.equals(mFontFeatureSettings, that.mFontFeatureSettings)
               && Objects.equals(mFontVariationSettings, that.mFontVariationSettings);
    }

    private static String languageTags(LocaleList locales)
    {
        return locales == null ? null : locales.toLanguageTags();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mTextSize, languageTags(mTextLocales), mSystemFontFamilyName, mTextFontWeight, mTextStyle, mAllCaps, mShadowDx,
                            mShadowDy, mShadowRadius, mShadowColor, mElegantTextHeight, mFallbackLineSpacing, mLetterSpacing,
                            mFontFeatureSettings, mFontVariationSettings, mLineBreakStyle, mLineBreakWordStyle, mTextScaleX,
                            mHighlightTextColor, mTextColor, mHintTextColor, mLinkTextColor);
    }

    @Override
    public String toString()
    {
        return "TextAppearanceInfo{mTextSize=" + mTextSize + ", mTextLocales=" + mTextLocales + ", mSystemFontFamilyName="
               + mSystemFontFamilyName + ", mTextFontWeight=" + mTextFontWeight + ", mTextStyle=" + mTextStyle + ", mAllCaps=" + mAllCaps
               + ", mTextColor=" + mTextColor + "}";
    }

    public static final class Builder
    {
        private float mTextSize = -1;
        private LocaleList mTextLocales = LocaleList.getEmptyLocaleList();
        private String mSystemFontFamilyName;
        private int mTextFontWeight = -1;
        private int mTextStyle;
        private boolean mAllCaps;
        private float mShadowDx;
        private float mShadowDy;
        private float mShadowRadius;
        private int mShadowColor;
        private boolean mElegantTextHeight;
        private boolean mFallbackLineSpacing;
        private float mLetterSpacing;
        private String mFontFeatureSettings;
        private String mFontVariationSettings;
        private int mLineBreakStyle;
        private int mLineBreakWordStyle;
        private float mTextScaleX = 1;
        private int mHighlightTextColor;
        private int mTextColor;
        private int mHintTextColor;
        private int mLinkTextColor;

        public Builder()
        {
        }

        public Builder setTextSize(float textSize)
        {
            mTextSize = textSize;
            return this;
        }

        public Builder setTextLocales(LocaleList textLocales)
        {
            mTextLocales = textLocales;
            return this;
        }

        public Builder setSystemFontFamilyName(String systemFontFamilyName)
        {
            mSystemFontFamilyName = systemFontFamilyName;
            return this;
        }

        public Builder setTextFontWeight(int textFontWeight)
        {
            mTextFontWeight = textFontWeight;
            return this;
        }

        public Builder setTextStyle(int textStyle)
        {
            mTextStyle = textStyle;
            return this;
        }

        public Builder setAllCaps(boolean allCaps)
        {
            mAllCaps = allCaps;
            return this;
        }

        public Builder setShadowDx(float shadowDx)
        {
            mShadowDx = shadowDx;
            return this;
        }

        public Builder setShadowDy(float shadowDy)
        {
            mShadowDy = shadowDy;
            return this;
        }

        public Builder setShadowRadius(float shadowRadius)
        {
            mShadowRadius = shadowRadius;
            return this;
        }

        public Builder setShadowColor(int shadowColor)
        {
            mShadowColor = shadowColor;
            return this;
        }

        public Builder setElegantTextHeight(boolean elegantTextHeight)
        {
            mElegantTextHeight = elegantTextHeight;
            return this;
        }

        public Builder setFallbackLineSpacing(boolean fallbackLineSpacing)
        {
            mFallbackLineSpacing = fallbackLineSpacing;
            return this;
        }

        public Builder setLetterSpacing(float letterSpacing)
        {
            mLetterSpacing = letterSpacing;
            return this;
        }

        public Builder setFontFeatureSettings(String fontFeatureSettings)
        {
            mFontFeatureSettings = fontFeatureSettings;
            return this;
        }

        public Builder setFontVariationSettings(String fontVariationSettings)
        {
            mFontVariationSettings = fontVariationSettings;
            return this;
        }

        public Builder setLineBreakStyle(int lineBreakStyle)
        {
            mLineBreakStyle = lineBreakStyle;
            return this;
        }

        public Builder setLineBreakWordStyle(int lineBreakWordStyle)
        {
            mLineBreakWordStyle = lineBreakWordStyle;
            return this;
        }

        public Builder setTextScaleX(float textScaleX)
        {
            mTextScaleX = textScaleX;
            return this;
        }

        public Builder setHighlightTextColor(int highlightTextColor)
        {
            mHighlightTextColor = highlightTextColor;
            return this;
        }

        public Builder setTextColor(int textColor)
        {
            mTextColor = textColor;
            return this;
        }

        public Builder setHintTextColor(int hintTextColor)
        {
            mHintTextColor = hintTextColor;
            return this;
        }

        public Builder setLinkTextColor(int linkTextColor)
        {
            mLinkTextColor = linkTextColor;
            return this;
        }

        public TextAppearanceInfo build()
        {
            return new TextAppearanceInfo(this);
        }
    }
}
