package android.location;

import android.os.Parcel;
import android.os.Parcelable;

public final class GnssMeasurementsEvent implements Parcelable
{
    private final boolean mHasIsFullTracking;
    private final boolean mIsFullTracking;

    private GnssMeasurementsEvent(boolean hasIsFullTracking, boolean isFullTracking)
    {
        mHasIsFullTracking = hasIsFullTracking;
        mIsFullTracking = isFullTracking;
    }

    public abstract static class Callback
    {
        public static final int STATUS_NOT_SUPPORTED = 0;
        public static final int STATUS_READY = 1;
        public static final int STATUS_LOCATION_DISABLED = 2;
        public static final int STATUS_NOT_ALLOWED = 3;

        public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs)
        {
        }

        public void onStatusChanged(int status)
        {
        }
    }

    public boolean isFullTracking()
    {
        return mIsFullTracking;
    }

    public boolean hasIsFullTracking()
    {
        return mHasIsFullTracking;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mHasIsFullTracking ? 1 : 0);
        parcel.writeInt(mIsFullTracking ? 1 : 0);
    }

    @Override
    public String toString()
    {
        return "GnssMeasurementsEvent[measurements=[]" + (mHasIsFullTracking ? ", isFullTracking=" + mIsFullTracking : "") + "]";
    }

    public static final Creator<GnssMeasurementsEvent> CREATOR = new Creator<GnssMeasurementsEvent>()
    {
        @Override
        public GnssMeasurementsEvent createFromParcel(Parcel parcel)
        {
            return new GnssMeasurementsEvent(parcel.readInt() != 0, parcel.readInt() != 0);
        }

        @Override
        public GnssMeasurementsEvent[] newArray(int size)
        {
            return new GnssMeasurementsEvent[size];
        }
    };
}
