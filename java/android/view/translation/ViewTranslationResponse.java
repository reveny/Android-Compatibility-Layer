package android.view.translation;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.autofill.AutofillId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ViewTranslationResponse implements Parcelable
{
    public static final Parcelable.Creator<ViewTranslationResponse> CREATOR = new Parcelable.Creator<ViewTranslationResponse>()
    {
        @Override
        public ViewTranslationResponse createFromParcel(Parcel source)
        {
            AutofillId id = source.readParcelable(AutofillId.class.getClassLoader());
            int count = source.readInt();
            Builder builder = new Builder(id);

            for (int index = 0; index < count; index++)
            {
                String key = source.readString();
                builder.setValue(key, TranslationResponseValue.CREATOR.createFromParcel(source));
            }

            return builder.build();
        }

        @Override
        public ViewTranslationResponse[] newArray(int size)
        {
            return new ViewTranslationResponse[size];
        }
    };

    private final AutofillId mAutofillId;
    private final Map<String, TranslationResponseValue> mTranslationResponseValues;

    ViewTranslationResponse(AutofillId autofillId, Map<String, TranslationResponseValue> values)
    {
        mAutofillId = autofillId;
        mTranslationResponseValues = values;
    }

    public TranslationResponseValue getValue(String key)
    {
        if (key == null) throw new NullPointerException("key must not be null");
        if (!mTranslationResponseValues.containsKey(key))
        {
            throw new IllegalArgumentException("Request does not contain value for key=" + key);
        }

        return mTranslationResponseValues.get(key);
    }

    public Set<String> getKeys()
    {
        return mTranslationResponseValues.keySet();
    }

    public AutofillId getAutofillId()
    {
        return mAutofillId;
    }

    @Override
    public String toString()
    {
        return "ViewTranslationResponse { autofillId = " + mAutofillId + ", translationResponseValues = " + mTranslationResponseValues
               + " }";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ViewTranslationResponse)) return false;

        ViewTranslationResponse response = (ViewTranslationResponse)other;
        return java.util.Objects.equals(mAutofillId, response.mAutofillId)
               && mTranslationResponseValues.equals(response.mTranslationResponseValues);
    }

    @Override
    public int hashCode()
    {
        return 31 * java.util.Objects.hashCode(mAutofillId) + mTranslationResponseValues.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(mAutofillId, flags);
        dest.writeInt(mTranslationResponseValues.size());

        for (Map.Entry<String, TranslationResponseValue> entry : mTranslationResponseValues.entrySet())
        {
            dest.writeString(entry.getKey());
            entry.getValue().writeToParcel(dest, flags);
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final class Builder
    {
        private final AutofillId mAutofillId;
        private final Map<String, TranslationResponseValue> mValues = new HashMap<>();

        public Builder(AutofillId autofillId)
        {
            if (autofillId == null) throw new NullPointerException("autofillId must not be null");
            mAutofillId = autofillId;
        }

        public Builder setValue(String key, TranslationResponseValue value)
        {
            mValues.put(key, value);
            return this;
        }

        public ViewTranslationResponse build()
        {
            return new ViewTranslationResponse(mAutofillId, new HashMap<>(mValues));
        }
    }
}
