package android.media.projection;

import android.os.Parcel;
import android.os.Parcelable;

public final class MediaProjectionConfig implements Parcelable
{
    private static final int CAPTURE_REGION_USER_CHOICE = 0;
    private static final int CAPTURE_REGION_FIXED_DISPLAY = 1;

    public static final Parcelable.Creator<MediaProjectionConfig> CREATOR = new Parcelable.Creator<MediaProjectionConfig>()
    {
        @Override
        public MediaProjectionConfig createFromParcel(Parcel source)
        {
            int displayToCapture = source.readInt();
            int regionToCapture = source.readInt();
            return new MediaProjectionConfig(displayToCapture, regionToCapture);
        }

        @Override
        public MediaProjectionConfig[] newArray(int size)
        {
            return new MediaProjectionConfig[size];
        }
    };

    private final int mDisplayToCapture;
    private final int mRegionToCapture;

    private MediaProjectionConfig(int displayToCapture, int regionToCapture)
    {
        mDisplayToCapture = displayToCapture;
        mRegionToCapture = regionToCapture;
    }

    public static MediaProjectionConfig createConfigForDefaultDisplay()
    {
        return new MediaProjectionConfig(0, CAPTURE_REGION_FIXED_DISPLAY);
    }

    public static MediaProjectionConfig createConfigForUserChoice()
    {
        return new MediaProjectionConfig(0, CAPTURE_REGION_USER_CHOICE);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof MediaProjectionConfig)) return false;

        MediaProjectionConfig other = (MediaProjectionConfig)object;
        return mDisplayToCapture == other.mDisplayToCapture && mRegionToCapture == other.mRegionToCapture;
    }

    @Override
    public int hashCode()
    {
        return 31 * mDisplayToCapture + mRegionToCapture;
    }

    @Override
    public String toString()
    {
        return "MediaProjectionConfig { displayToCapture = " + mDisplayToCapture + ", regionToCapture = "
            + (mRegionToCapture == CAPTURE_REGION_FIXED_DISPLAY ? "CAPTURE_REGION_FIXED_DISPLAY" : "CAPTURE_REGION_USER_CHOICE") + " }";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mDisplayToCapture);
        destination.writeInt(mRegionToCapture);
    }
}
