package android.app;

import android.os.Parcel;
import android.os.Parcelable;

public final class ForegroundServiceStartNotAllowedException extends ServiceStartNotAllowedException implements Parcelable
{
    public static final Parcelable.Creator<ForegroundServiceStartNotAllowedException> CREATOR =
        new Parcelable.Creator<ForegroundServiceStartNotAllowedException>()
    {
        @Override
        public ForegroundServiceStartNotAllowedException createFromParcel(Parcel source)
        {
            return new ForegroundServiceStartNotAllowedException(source.readString());
        }

        @Override
        public ForegroundServiceStartNotAllowedException[] newArray(int size)
        {
            return new ForegroundServiceStartNotAllowedException[size];
        }
    };

    public ForegroundServiceStartNotAllowedException(String message)
    {
        super(message);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(getMessage());
    }
}
