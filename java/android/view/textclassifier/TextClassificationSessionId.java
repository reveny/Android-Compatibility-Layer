package android.view.textclassifier;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

public final class TextClassificationSessionId implements Parcelable
{
    private final String mValue;

    TextClassificationSessionId()
    {
        this(UUID.randomUUID().toString());
    }

    TextClassificationSessionId(String value)
    {
        mValue = value;
    }

    public String getValue()
    {
        return mValue;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof TextClassificationSessionId)) return false;

        return mValue.equals(((TextClassificationSessionId)other).mValue);
    }

    @Override
    public int hashCode()
    {
        return mValue.hashCode();
    }

    @Override
    public String toString()
    {
        return "TextClassificationSessionId {" + mValue + "}";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mValue);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<TextClassificationSessionId> CREATOR = new Creator<TextClassificationSessionId>()
    {
        @Override
        public TextClassificationSessionId createFromParcel(Parcel source)
        {
            return new TextClassificationSessionId(source.readString());
        }

        @Override
        public TextClassificationSessionId[] newArray(int size)
        {
            return new TextClassificationSessionId[size];
        }
    };
}
