package android.location;

import android.os.Parcel;
import android.os.Parcelable;

public final class GnssCapabilities implements Parcelable
{
    public static final int CAPABILITY_UNKNOWN = 0;
    public static final int CAPABILITY_SUPPORTED = 1;
    public static final int CAPABILITY_UNSUPPORTED = 2;

    GnssCapabilities()
    {
    }

    public boolean hasScheduling()
    {
        return false;
    }

    public boolean hasMsb()
    {
        return false;
    }

    public boolean hasMsa()
    {
        return false;
    }

    public boolean hasSingleShotFix()
    {
        return false;
    }

    public boolean hasOnDemandTime()
    {
        return false;
    }

    public boolean hasGeofencing()
    {
        return false;
    }

    public boolean hasMeasurements()
    {
        return false;
    }

    public boolean hasNavigationMessages()
    {
        return false;
    }

    public boolean hasLowPowerMode()
    {
        return false;
    }

    public boolean hasSatelliteBlocklist()
    {
        return false;
    }

    public boolean hasSatellitePvt()
    {
        return false;
    }

    public boolean hasMeasurementCorrections()
    {
        return false;
    }

    public boolean hasGnssAntennaInfo()
    {
        return false;
    }

    public boolean hasAntennaInfo()
    {
        return false;
    }

    public boolean hasMeasurementCorrelationVectors()
    {
        return false;
    }

    public boolean hasMeasurementCorrectionsForDriving()
    {
        return false;
    }

    public int hasAccumulatedDeltaRange()
    {
        return CAPABILITY_UNSUPPORTED;
    }

    public boolean hasMeasurementCorrectionsLosSats()
    {
        return false;
    }

    public boolean hasMeasurementCorrectionsExcessPathLength()
    {
        return false;
    }

    public boolean hasMeasurementCorrectionsReflectingPlane()
    {
        return false;
    }

    public boolean hasPowerTotal()
    {
        return false;
    }

    public boolean hasPowerSinglebandTracking()
    {
        return false;
    }

    public boolean hasPowerMultibandTracking()
    {
        return false;
    }

    public boolean hasPowerSinglebandAcquisition()
    {
        return false;
    }

    public boolean hasPowerMultibandAcquisition()
    {
        return false;
    }

    public boolean hasPowerOtherModes()
    {
        return false;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof GnssCapabilities;
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
    }

    @Override
    public String toString()
    {
        return "[]";
    }

    public static final Creator<GnssCapabilities> CREATOR = new Creator<GnssCapabilities>()
    {
        @Override
        public GnssCapabilities createFromParcel(Parcel parcel)
        {
            return new GnssCapabilities();
        }

        @Override
        public GnssCapabilities[] newArray(int size)
        {
            return new GnssCapabilities[size];
        }
    };
}
