package android.nfc;

import android.os.Parcel;
import android.os.Parcelable;

public final class AvailableNfcAntenna implements Parcelable
{
    public static final Parcelable.Creator<AvailableNfcAntenna> CREATOR = new Parcelable.Creator<AvailableNfcAntenna>()
    {
        @Override
        public AvailableNfcAntenna createFromParcel(Parcel source)
        {
            int locationX = source.readInt();
            int locationY = source.readInt();
            return new AvailableNfcAntenna(locationX, locationY);
        }

        @Override
        public AvailableNfcAntenna[] newArray(int size)
        {
            return new AvailableNfcAntenna[size];
        }
    };

    private final int mLocationX;
    private final int mLocationY;

    public AvailableNfcAntenna(int locationX, int locationY)
    {
        mLocationX = locationX;
        mLocationY = locationY;
    }

    public int getLocationX()
    {
        return mLocationX;
    }

    public int getLocationY()
    {
        return mLocationY;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof AvailableNfcAntenna)) return false;

        AvailableNfcAntenna antenna = (AvailableNfcAntenna)other;
        return mLocationX == antenna.mLocationX && mLocationY == antenna.mLocationY;
    }

    @Override
    public int hashCode()
    {
        return 31 * mLocationX + mLocationY;
    }

    @Override
    public String toString()
    {
        return "AvailableNfcAntenna x: " + mLocationX + " y: " + mLocationY;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mLocationX);
        dest.writeInt(mLocationY);
    }
}
