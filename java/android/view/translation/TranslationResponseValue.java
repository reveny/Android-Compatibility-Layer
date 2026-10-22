package android.view.translation;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class TranslationResponseValue implements Parcelable
{
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_ERROR = 1;
    public static final String EXTRA_DEFINITIONS = "android.view.translation.extra.DEFINITIONS";

    public static final Parcelable.Creator<TranslationResponseValue> CREATOR = new Parcelable.Creator<TranslationResponseValue>()
    {
        @Override
        public TranslationResponseValue createFromParcel(Parcel source)
        {
            int status = source.readInt();
            String text = source.readString();
            String transliteration = source.readString();
            Bundle extras = source.readBundle();
            return new TranslationResponseValue(status, text, extras, transliteration);
        }

        @Override
        public TranslationResponseValue[] newArray(int size)
        {
            return new TranslationResponseValue[size];
        }
    };

    private final int mStatusCode;
    private final CharSequence mText;
    private final Bundle mExtras;
    private final CharSequence mTransliteration;

    TranslationResponseValue(int statusCode, CharSequence text, Bundle extras, CharSequence transliteration)
    {
        mStatusCode = statusCode;
        mText = text;
        mExtras = extras == null ? Bundle.EMPTY : extras;
        mTransliteration = transliteration;
    }

    public static TranslationResponseValue forError()
    {
        return new TranslationResponseValue(STATUS_ERROR, null, Bundle.EMPTY, null);
    }

    public int getStatusCode()
    {
        return mStatusCode;
    }

    public CharSequence getText()
    {
        return mText;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public CharSequence getTransliteration()
    {
        return mTransliteration;
    }

    @Override
    public String toString()
    {
        return "TranslationResponseValue { statusCode = " + mStatusCode + ", text = " + mText + ", transliteration = " + mTransliteration
               + " }";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof TranslationResponseValue)) return false;

        TranslationResponseValue value = (TranslationResponseValue)other;
        return mStatusCode == value.mStatusCode && TextUtils.equals(mText, value.mText)
               && TextUtils.equals(mTransliteration, value.mTransliteration);
    }

    @Override
    public int hashCode()
    {
        int result = mStatusCode;
        result = 31 * result + (mText == null ? 0 : mText.toString().hashCode());
        result = 31 * result + (mTransliteration == null ? 0 : mTransliteration.toString().hashCode());
        return result;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mStatusCode);
        dest.writeString(mText == null ? null : mText.toString());
        dest.writeString(mTransliteration == null ? null : mTransliteration.toString());
        dest.writeBundle(mExtras);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final class Builder
    {
        private final int mStatusCode;
        private CharSequence mText;
        private Bundle mExtras;
        private CharSequence mTransliteration;

        public Builder(int statusCode)
        {
            mStatusCode = statusCode;
        }

        public Builder setText(CharSequence text)
        {
            mText = text;
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras;
            return this;
        }

        public Builder setTransliteration(CharSequence transliteration)
        {
            mTransliteration = transliteration;
            return this;
        }

        public TranslationResponseValue build()
        {
            return new TranslationResponseValue(mStatusCode, mText, mExtras, mTransliteration);
        }
    }
}
