package android.location;

public class LocationProvider
{
    public static final int OUT_OF_SERVICE = 0;
    public static final int TEMPORARILY_UNAVAILABLE = 1;
    public static final int AVAILABLE = 2;

    private final String mName;
    private final boolean mRequiresNetwork;
    private final boolean mRequiresSatellite;
    private final boolean mRequiresCell;
    private final int mPowerRequirement;
    private final int mAccuracy;

    LocationProvider(String name, boolean requiresNetwork, boolean requiresSatellite, boolean requiresCell, int powerRequirement,
                     int accuracy)
    {
        mName = name;
        mRequiresNetwork = requiresNetwork;
        mRequiresSatellite = requiresSatellite;
        mRequiresCell = requiresCell;
        mPowerRequirement = powerRequirement;
        mAccuracy = accuracy;
    }

    public String getName()
    {
        return mName;
    }

    public boolean meetsCriteria(Criteria criteria)
    {
        if (criteria.getAccuracy() != Criteria.NO_REQUIREMENT && criteria.getAccuracy() < mAccuracy) return false;
        if (criteria.getPowerRequirement() != Criteria.NO_REQUIREMENT && criteria.getPowerRequirement() < mPowerRequirement) return false;
        if (criteria.isAltitudeRequired() && !supportsAltitude()) return false;
        if (criteria.isSpeedRequired() && !supportsSpeed()) return false;
        if (criteria.isBearingRequired() && !supportsBearing()) return false;
        if (!criteria.isCostAllowed() && hasMonetaryCost()) return false;

        return true;
    }

    public boolean requiresNetwork()
    {
        return mRequiresNetwork;
    }

    public boolean requiresSatellite()
    {
        return mRequiresSatellite;
    }

    public boolean requiresCell()
    {
        return mRequiresCell;
    }

    public boolean hasMonetaryCost()
    {
        return false;
    }

    public boolean supportsAltitude()
    {
        return mRequiresSatellite;
    }

    public boolean supportsSpeed()
    {
        return mRequiresSatellite;
    }

    public boolean supportsBearing()
    {
        return mRequiresSatellite;
    }

    public int getPowerRequirement()
    {
        return mPowerRequirement;
    }

    public int getAccuracy()
    {
        return mAccuracy;
    }
}
