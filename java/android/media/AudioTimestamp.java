package android.media;

import android.os.Parcel;
import android.os.Parcelable;

public final class AudioTimestamp implements Parcelable
{
    public static final int TIMEBASE_BOOTTIME = 1;
    public static final int TIMEBASE_MONOTONIC = 0;

    public static final Parcelable.Creator<AudioTimestamp> CREATOR = new Parcelable.Creator<AudioTimestamp>()
    {
        @Override
        public AudioTimestamp createFromParcel(Parcel source)
        {
            AudioTimestamp timestamp = new AudioTimestamp();
            timestamp.framePosition = source.readLong();
            timestamp.nanoTime = source.readLong();
            return timestamp;
        }

        @Override
        public AudioTimestamp[] newArray(int size)
        {
            return new AudioTimestamp[size];
        }
    };

    public long framePosition;
    public long nanoTime;

    public AudioTimestamp()
    {
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "AudioTimeStamp:{ Frame Position: " + framePosition + " Time in nanos: " + nanoTime + " }";
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeLong(framePosition);
        parcel.writeLong(nanoTime);
    }
}
