package android.location;

public final class GpsSatellite
{
    boolean mValid;
    boolean mHasEphemeris;
    boolean mHasAlmanac;
    boolean mUsedInFix;
    int mPrn;
    float mSnr;
    float mElevation;
    float mAzimuth;

    GpsSatellite(int prn)
    {
        mPrn = prn;
    }

    public int getPrn()
    {
        return mPrn;
    }

    public float getSnr()
    {
        return mSnr;
    }

    public float getElevation()
    {
        return mElevation;
    }

    public float getAzimuth()
    {
        return mAzimuth;
    }

    public boolean hasEphemeris()
    {
        return mHasEphemeris;
    }

    public boolean hasAlmanac()
    {
        return mHasAlmanac;
    }

    public boolean usedInFix()
    {
        return mUsedInFix;
    }
}
