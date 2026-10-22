package android.print;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class PrinterId implements Parcelable
{
    public static final Parcelable.Creator<PrinterId> CREATOR = new Parcelable.Creator<PrinterId>()
    {
        @Override
        public PrinterId createFromParcel(Parcel source)
        {
            String packageName = source.readString();
            String className = source.readString();
            String localId = source.readString();
            ComponentName serviceName = packageName == null ? null : new ComponentName(packageName, className);
            return new PrinterId(serviceName, localId);
        }

        @Override
        public PrinterId[] newArray(int size)
        {
            return new PrinterId[size];
        }
    };

    private final ComponentName mServiceName;
    private final String mLocalId;

    PrinterId(ComponentName serviceName, String localId)
    {
        mServiceName = serviceName;
        mLocalId = localId;
    }

    public String getLocalId()
    {
        return mLocalId;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mServiceName == null ? null : mServiceName.getPackageName());
        dest.writeString(mServiceName == null ? null : mServiceName.getClassName());
        dest.writeString(mLocalId);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PrinterId)) return false;

        PrinterId id = (PrinterId)other;
        return Objects.equals(mServiceName, id.mServiceName) && Objects.equals(mLocalId, id.mLocalId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mServiceName, mLocalId);
    }

    @Override
    public String toString()
    {
        return "PrinterId{serviceName=" + (mServiceName == null ? null : mServiceName.flattenToString()) + ", localId=" + mLocalId + "}";
    }
}
