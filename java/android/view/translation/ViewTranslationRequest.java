package android.view.translation;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.autofill.AutofillId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ViewTranslationRequest implements Parcelable
{
    public static final String ID_TEXT = "android:text";

    public static final Parcelable.Creator<ViewTranslationRequest> CREATOR = new Parcelable.Creator<ViewTranslationRequest>()
    {
        @Override
        public ViewTranslationRequest createFromParcel(Parcel source)
        {
            AutofillId id = source.readParcelable(AutofillId.class.getClassLoader());
            int count = source.readInt();
            Builder builder = new Builder(id);

            for (int index = 0; index < count; index++)
            {
                String key = source.readString();
                builder.setValue(key, TranslationRequestValue.CREATOR.createFromParcel(source));
            }

            return builder.build();
        }

        @Override
        public ViewTranslationRequest[] newArray(int size)
        {
            return new ViewTranslationRequest[size];
        }
    };

    private final AutofillId mAutofillId;
    private final Map<String, TranslationRequestValue> mTranslationRequestValues;

    ViewTranslationRequest(AutofillId autofillId, Map<String, TranslationRequestValue> values)
    {
        mAutofillId = autofillId;
        mTranslationRequestValues = values;
    }

    public TranslationRequestValue getValue(String key)
    {
        if (key == null) throw new NullPointerException("key must not be null");
        if (!mTranslationRequestValues.containsKey(key))
        {
            throw new IllegalArgumentException("Request does not contain value for key=" + key);
        }

        return mTranslationRequestValues.get(key);
    }

    public Set<String> getKeys()
    {
        return mTranslationRequestValues.keySet();
    }

    public AutofillId getAutofillId()
    {
        return mAutofillId;
    }

    @Override
    public String toString()
    {
        return "ViewTranslationRequest { autofillId = " + mAutofillId + ", translationRequestValues = " + mTranslationRequestValues + " }";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ViewTranslationRequest)) return false;

        ViewTranslationRequest request = (ViewTranslationRequest)other;
        return java.util.Objects.equals(mAutofillId, request.mAutofillId)
               && mTranslationRequestValues.equals(request.mTranslationRequestValues);
    }

    @Override
    public int hashCode()
    {
        return 31 * java.util.Objects.hashCode(mAutofillId) + mTranslationRequestValues.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(mAutofillId, flags);
        dest.writeInt(mTranslationRequestValues.size());

        for (Map.Entry<String, TranslationRequestValue> entry : mTranslationRequestValues.entrySet())
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
        private final Map<String, TranslationRequestValue> mValues = new HashMap<>();

        public Builder(AutofillId autofillId)
        {
            if (autofillId == null) throw new NullPointerException("autofillId must not be null");
            mAutofillId = autofillId;
        }

        public Builder(AutofillId autofillId, long virtualChildId)
        {
            this(autofillId);
        }

        public Builder setValue(String key, TranslationRequestValue value)
        {
            mValues.put(key, value);
            return this;
        }

        public ViewTranslationRequest build()
        {
            return new ViewTranslationRequest(mAutofillId, new HashMap<>(mValues));
        }
    }
}
