package android.text.style;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;
import java.util.Arrays;
import java.util.Locale;

public class SuggestionSpan extends CharacterStyle implements ParcelableSpan
{
    public static final int FLAG_EASY_CORRECT = 0x0001;
    public static final int FLAG_MISSPELLED = 0x0002;
    public static final int FLAG_AUTO_CORRECTION = 0x0004;
    public static final int FLAG_GRAMMAR_ERROR = 0x0008;

    public static final String ACTION_SUGGESTION_PICKED = "android.text.style.SUGGESTION_PICKED";
    public static final String SUGGESTION_SPAN_PICKED_AFTER = "after";
    public static final String SUGGESTION_SPAN_PICKED_BEFORE = "before";
    public static final String SUGGESTION_SPAN_PICKED_HASHCODE = "hashcode";

    public static final int SUGGESTIONS_MAX_SIZE = 5;

    private static final int DEFAULT_UNDERLINE_COLOR = 0xFF4285F4;
    private static final int MISSPELLED_UNDERLINE_COLOR = 0xFFFF0000;
    private static final int GRAMMAR_UNDERLINE_COLOR = 0xFF00A000;

    public static final Parcelable.Creator<SuggestionSpan> CREATOR = new Parcelable.Creator<SuggestionSpan>()
    {
        @Override
        public SuggestionSpan createFromParcel(Parcel source)
        {
            return new SuggestionSpan(source);
        }

        @Override
        public SuggestionSpan[] newArray(int size)
        {
            return new SuggestionSpan[size];
        }
    };

    private int mFlags;
    private final String[] mSuggestions;
    private final String mLocaleStringForCompatibility;
    private final String mLanguageTag;
    private final int mHashCode;
    private final float mUnderlineThickness;

    public SuggestionSpan(Context context, String[] suggestions, int flags)
    {
        this(context, null, suggestions, flags, null);
    }

    public SuggestionSpan(Locale locale, String[] suggestions, int flags)
    {
        this(null, locale, suggestions, flags, null);
    }

    public SuggestionSpan(Context context, Locale locale, String[] suggestions, int flags, Class<?> notificationTargetClass)
    {
        int size = Math.min(SUGGESTIONS_MAX_SIZE, suggestions.length);
        mSuggestions = Arrays.copyOf(suggestions, size);
        mFlags = flags;

        Locale resolved = locale;
        if (resolved == null && context != null) resolved = context.getResources().getConfiguration().locale;
        if (resolved == null) resolved = Locale.getDefault();

        mLocaleStringForCompatibility = resolved.toString();
        mLanguageTag = resolved.toLanguageTag();
        mHashCode = hashCodeInternal(mSuggestions, mLanguageTag, mLocaleStringForCompatibility);
        mUnderlineThickness = 2f;
    }

    public SuggestionSpan(Parcel source)
    {
        int count = source.readInt();
        mSuggestions = new String[count];
        for (int index = 0; index < count; index++)
        {
            mSuggestions[index] = source.readString();
        }

        mFlags = source.readInt();
        mLocaleStringForCompatibility = source.readString();
        mLanguageTag = source.readString();
        mHashCode = source.readInt();
        mUnderlineThickness = source.readFloat();
    }

    public String[] getSuggestions()
    {
        return mSuggestions;
    }

    @Deprecated
    public String getLocale()
    {
        return mLocaleStringForCompatibility;
    }

    public Locale getLocaleObject()
    {
        return mLanguageTag == null || mLanguageTag.isEmpty() ? null : Locale.forLanguageTag(mLanguageTag);
    }

    public int getFlags()
    {
        return mFlags;
    }

    public void setFlags(int flags)
    {
        mFlags = flags;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mSuggestions.length);
        for (String suggestion : mSuggestions)
        {
            dest.writeString(suggestion);
        }

        dest.writeInt(mFlags);
        dest.writeString(mLocaleStringForCompatibility);
        dest.writeString(mLanguageTag);
        dest.writeInt(mHashCode);
        dest.writeFloat(mUnderlineThickness);
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.SUGGESTION_SPAN;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof SuggestionSpan) return ((SuggestionSpan)other).hashCode() == mHashCode;

        return false;
    }

    @Override
    public int hashCode()
    {
        return mHashCode;
    }

    private static int hashCodeInternal(String[] suggestions, String languageTag, String localeString)
    {
        return Arrays.hashCode(new Object[] {Long.valueOf(SystemClock.uptimeMillis()), suggestions, languageTag, localeString});
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint == null) return;

        boolean misspelled = (mFlags & FLAG_MISSPELLED) != 0;
        boolean easy = (mFlags & FLAG_EASY_CORRECT) != 0;
        boolean autoCorrection = (mFlags & FLAG_AUTO_CORRECTION) != 0;
        boolean grammar = (mFlags & FLAG_GRAMMAR_ERROR) != 0;

        if (easy || misspelled || grammar || autoCorrection)
        {
            paint.underlineColor = getUnderlineColor();
            paint.underlineThickness = mUnderlineThickness;
        }
    }

    public int getUnderlineColor()
    {
        boolean misspelled = (mFlags & FLAG_MISSPELLED) != 0;
        boolean easy = (mFlags & FLAG_EASY_CORRECT) != 0;
        boolean autoCorrection = (mFlags & FLAG_AUTO_CORRECTION) != 0;
        boolean grammar = (mFlags & FLAG_GRAMMAR_ERROR) != 0;

        if (easy)
        {
            if (grammar) return GRAMMAR_UNDERLINE_COLOR;
            if (misspelled) return MISSPELLED_UNDERLINE_COLOR;
            return DEFAULT_UNDERLINE_COLOR;
        }

        if (autoCorrection) return DEFAULT_UNDERLINE_COLOR;
        if (misspelled) return MISSPELLED_UNDERLINE_COLOR;
        if (grammar) return GRAMMAR_UNDERLINE_COLOR;

        return 0;
    }
}
