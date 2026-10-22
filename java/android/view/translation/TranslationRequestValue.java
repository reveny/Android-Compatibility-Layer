package android.view.translation;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class TranslationRequestValue implements Parcelable
{
    public static final Parcelable.Creator<TranslationRequestValue> CREATOR = new Parcelable.Creator<TranslationRequestValue>()
    {
        @Override
        public TranslationRequestValue createFromParcel(Parcel source)
        {
            return new TranslationRequestValue(source.readString());
        }

        @Override
        public TranslationRequestValue[] newArray(int size)
        {
            return new TranslationRequestValue[size];
        }
    };

    private final CharSequence mText;

    TranslationRequestValue(CharSequence text)
    {
        mText = text;
    }

    public static TranslationRequestValue forText(CharSequence text)
    {
        if (text == null) throw new NullPointerException("text must not be null");
        return new TranslationRequestValue(text);
    }

    public CharSequence getText()
    {
        return mText;
    }

    @Override
    public String toString()
    {
        return "TranslationRequestValue { text = " + mText + " }";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof TranslationRequestValue)) return false;

        return TextUtils.equals(mText, ((TranslationRequestValue)other).mText);
    }

    @Override
    public int hashCode()
    {
        return mText == null ? 0 : mText.toString().hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mText == null ? null : mText.toString());
    }

    @Override
    public int describeContents()
    {
        return 0;
    }
}
