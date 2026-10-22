package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class UriPermission implements Parcelable
{
    public static final long INVALID_TIME = Long.MIN_VALUE;

    public static final Parcelable.Creator<UriPermission> CREATOR = new Parcelable.Creator<UriPermission>()
    {
        @Override
        public UriPermission createFromParcel(Parcel source)
        {
            return new UriPermission((Uri)source.readValue(null), source.readInt(), source.readLong());
        }

        @Override
        public UriPermission[] newArray(int size)
        {
            return new UriPermission[size];
        }
    };

    private final Uri mUri;
    private final int mModeFlags;
    private final long mPersistedTime;

    UriPermission(Uri uri, int modeFlags, long persistedTime)
    {
        mUri = uri;
        mModeFlags = modeFlags;
        mPersistedTime = persistedTime;
    }

    public Uri getUri()
    {
        return mUri;
    }

    public boolean isReadPermission()
    {
        return (mModeFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0;
    }

    public boolean isWritePermission()
    {
        return (mModeFlags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0;
    }

    public long getPersistedTime()
    {
        return mPersistedTime;
    }

    @Override
    public String toString()
    {
        return "UriPermission {uri=" + mUri + ", modeFlags=" + mModeFlags + ", persistedTime=" + mPersistedTime + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeValue(mUri);
        destination.writeInt(mModeFlags);
        destination.writeLong(mPersistedTime);
    }
}
