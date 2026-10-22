package android.location;

import android.os.Parcel;
import android.os.Parcelable;

public final class GnssStatus implements Parcelable
{
    public static final int CONSTELLATION_UNKNOWN = 0;
    public static final int CONSTELLATION_GPS = 1;
    public static final int CONSTELLATION_SBAS = 2;
    public static final int CONSTELLATION_GLONASS = 3;
    public static final int CONSTELLATION_QZSS = 4;
    public static final int CONSTELLATION_BEIDOU = 5;
    public static final int CONSTELLATION_GALILEO = 6;
    public static final int CONSTELLATION_IRNSS = 7;

    private static final int SVID_FLAGS_HAS_EPHEMERIS_DATA = 1 << 0;
    private static final int SVID_FLAGS_HAS_ALMANAC_DATA = 1 << 1;
    private static final int SVID_FLAGS_USED_IN_FIX = 1 << 2;
    private static final int SVID_FLAGS_HAS_CARRIER_FREQUENCY = 1 << 3;
    private static final int SVID_FLAGS_HAS_BASEBAND_CN0 = 1 << 4;
    private static final int SVID_SHIFT_WIDTH = 12;
    private static final int CONSTELLATION_TYPE_SHIFT_WIDTH = 8;
    private static final int CONSTELLATION_TYPE_MASK = 0xf;

    private final int mSvCount;
    private final int[] mSvidWithFlags;
    private final float[] mCn0DbHzs;
    private final float[] mElevations;
    private final float[] mAzimuths;
    private final float[] mCarrierFrequencies;
    private final float[] mBasebandCn0DbHzs;

    private GnssStatus(int svCount, int[] svidWithFlags, float[] cn0DbHzs, float[] elevations, float[] azimuths,
                       float[] carrierFrequencies, float[] basebandCn0DbHzs)
    {
        mSvCount = svCount;
        mSvidWithFlags = svidWithFlags;
        mCn0DbHzs = cn0DbHzs;
        mElevations = elevations;
        mAzimuths = azimuths;
        mCarrierFrequencies = carrierFrequencies;
        mBasebandCn0DbHzs = basebandCn0DbHzs;
    }

    static GnssStatus empty()
    {
        return new GnssStatus(0, new int[0], new float[0], new float[0], new float[0], new float[0], new float[0]);
    }

    public abstract static class Callback
    {
        public void onStarted()
        {
        }

        public void onStopped()
        {
        }

        public void onFirstFix(int ttffMillis)
        {
        }

        public void onSatelliteStatusChanged(GnssStatus status)
        {
        }
    }

    public int getSatelliteCount()
    {
        return mSvCount;
    }

    public int getConstellationType(int satelliteIndex)
    {
        return (mSvidWithFlags[satelliteIndex] >> CONSTELLATION_TYPE_SHIFT_WIDTH) & CONSTELLATION_TYPE_MASK;
    }

    public int getSvid(int satelliteIndex)
    {
        return mSvidWithFlags[satelliteIndex] >> SVID_SHIFT_WIDTH;
    }

    public float getCn0DbHz(int satelliteIndex)
    {
        return mCn0DbHzs[satelliteIndex];
    }

    public float getElevationDegrees(int satelliteIndex)
    {
        return mElevations[satelliteIndex];
    }

    public float getAzimuthDegrees(int satelliteIndex)
    {
        return mAzimuths[satelliteIndex];
    }

    public boolean hasEphemerisData(int satelliteIndex)
    {
        return (mSvidWithFlags[satelliteIndex] & SVID_FLAGS_HAS_EPHEMERIS_DATA) != 0;
    }

    public boolean hasAlmanacData(int satelliteIndex)
    {
        return (mSvidWithFlags[satelliteIndex] & SVID_FLAGS_HAS_ALMANAC_DATA) != 0;
    }

    public boolean usedInFix(int satelliteIndex)
    {
        return (mSvidWithFlags[satelliteIndex] & SVID_FLAGS_USED_IN_FIX) != 0;
    }

    public boolean hasCarrierFrequencyHz(int satelliteIndex)
    {
        return (mSvidWithFlags[satelliteIndex] & SVID_FLAGS_HAS_CARRIER_FREQUENCY) != 0;
    }

    public float getCarrierFrequencyHz(int satelliteIndex)
    {
        return mCarrierFrequencies[satelliteIndex];
    }

    public boolean hasBasebandCn0DbHz(int satelliteIndex)
    {
        return (mSvidWithFlags[satelliteIndex] & SVID_FLAGS_HAS_BASEBAND_CN0) != 0;
    }

    public float getBasebandCn0DbHz(int satelliteIndex)
    {
        return mBasebandCn0DbHzs[satelliteIndex];
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof GnssStatus)) return false;

        GnssStatus status = (GnssStatus)other;
        return mSvCount == status.mSvCount && java.util.Arrays.equals(mSvidWithFlags, status.mSvidWithFlags)
            && java.util.Arrays.equals(mCn0DbHzs, status.mCn0DbHzs) && java.util.Arrays.equals(mElevations, status.mElevations)
            && java.util.Arrays.equals(mAzimuths, status.mAzimuths)
            && java.util.Arrays.equals(mCarrierFrequencies, status.mCarrierFrequencies)
            && java.util.Arrays.equals(mBasebandCn0DbHzs, status.mBasebandCn0DbHzs);
    }

    @Override
    public int hashCode()
    {
        return 31 * mSvCount + java.util.Arrays.hashCode(mSvidWithFlags);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mSvCount);

        for (int index = 0; index < mSvCount; index++)
        {
            parcel.writeInt(mSvidWithFlags[index]);
            parcel.writeFloat(mCn0DbHzs[index]);
            parcel.writeFloat(mElevations[index]);
            parcel.writeFloat(mAzimuths[index]);
            parcel.writeFloat(mCarrierFrequencies[index]);
            parcel.writeFloat(mBasebandCn0DbHzs[index]);
        }
    }

    public static final Creator<GnssStatus> CREATOR = new Creator<GnssStatus>()
    {
        @Override
        public GnssStatus createFromParcel(Parcel parcel)
        {
            int count = parcel.readInt();
            int[] svidWithFlags = new int[count];
            float[] cn0DbHzs = new float[count];
            float[] elevations = new float[count];
            float[] azimuths = new float[count];
            float[] carrierFrequencies = new float[count];
            float[] basebandCn0DbHzs = new float[count];

            for (int index = 0; index < count; index++)
            {
                svidWithFlags[index] = parcel.readInt();
                cn0DbHzs[index] = parcel.readFloat();
                elevations[index] = parcel.readFloat();
                azimuths[index] = parcel.readFloat();
                carrierFrequencies[index] = parcel.readFloat();
                basebandCn0DbHzs[index] = parcel.readFloat();
            }

            return new GnssStatus(count, svidWithFlags, cn0DbHzs, elevations, azimuths, carrierFrequencies, basebandCn0DbHzs);
        }

        @Override
        public GnssStatus[] newArray(int size)
        {
            return new GnssStatus[size];
        }
    };
}
