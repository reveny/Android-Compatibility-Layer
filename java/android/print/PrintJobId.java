package android.print;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

public final class PrintJobId implements Parcelable
{
    public static final Parcelable.Creator<PrintJobId> CREATOR = new Parcelable.Creator<PrintJobId>()
    {
        @Override
        public PrintJobId createFromParcel(Parcel source)
        {
            return new PrintJobId(source.readString());
        }

        @Override
        public PrintJobId[] newArray(int size)
        {
            return new PrintJobId[size];
        }
    };

    private final String mValue;

    PrintJobId()
    {
        this(UUID.randomUUID().toString());
    }

    PrintJobId(String value)
    {
        mValue = value;
    }

    @Override
    public int hashCode()
    {
        return mValue == null ? 0 : mValue.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PrintJobId)) return false;

        PrintJobId id = (PrintJobId)other;
        return mValue == null ? id.mValue == null : mValue.equals(id.mValue);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mValue);
    }

    @Override
    public String toString()
    {
        return mValue;
    }
}
