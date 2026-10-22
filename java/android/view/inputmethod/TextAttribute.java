package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TextAttribute implements Parcelable
{
    public static final Parcelable.Creator<TextAttribute> CREATOR = new Parcelable.Creator<TextAttribute>()
    {
        @Override
        public TextAttribute createFromParcel(Parcel source)
        {
            List<String> suggestions = source.createStringArrayList();
            PersistableBundle extras = source.readPersistableBundle();
            return new TextAttribute(suggestions, extras);
        }

        @Override
        public TextAttribute[] newArray(int size)
        {
            return new TextAttribute[size];
        }
    };

    private final List<String> mTextConversionSuggestions;
    private final PersistableBundle mExtras;

    private TextAttribute(List<String> textConversionSuggestions, PersistableBundle extras)
    {
        mTextConversionSuggestions = textConversionSuggestions == null ? new ArrayList<String>()
                                                                       : new ArrayList<>(textConversionSuggestions);
        mExtras = extras == null ? new PersistableBundle() : extras;
    }

    public List<String> getTextConversionSuggestions()
    {
        return Collections.unmodifiableList(mTextConversionSuggestions);
    }

    public PersistableBundle getExtras()
    {
        return mExtras;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeStringList(mTextConversionSuggestions);
        dest.writePersistableBundle(mExtras);
    }

    public static final class Builder
    {
        private List<String> mTextConversionSuggestions = new ArrayList<>();
        private PersistableBundle mExtras = new PersistableBundle();

        public Builder()
        {
        }

        public Builder setTextConversionSuggestions(List<String> textConversionSuggestions)
        {
            mTextConversionSuggestions = Collections.unmodifiableList(new ArrayList<>(textConversionSuggestions));
            return this;
        }

        public Builder setExtras(PersistableBundle extras)
        {
            mExtras = extras;
            return this;
        }

        public TextAttribute build()
        {
            return new TextAttribute(mTextConversionSuggestions, mExtras);
        }
    }
}
