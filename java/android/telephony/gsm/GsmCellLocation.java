package android.telephony.gsm;

import android.os.Bundle;
import android.telephony.CellLocation;

public class GsmCellLocation extends CellLocation
{
    private int mLac;
    private int mCid;
    private int mPsc;

    public GsmCellLocation()
    {
        mLac = -1;
        mCid = -1;
        mPsc = -1;
    }

    public GsmCellLocation(Bundle bundle)
    {
        mLac = bundle.getInt("lac", -1);
        mCid = bundle.getInt("cid", -1);
        mPsc = bundle.getInt("psc", -1);
    }

    public int getLac()
    {
        return mLac;
    }

    public int getCid()
    {
        return mCid;
    }

    public int getPsc()
    {
        return mPsc;
    }

    @Override
    public void setStateInvalid()
    {
        mLac = -1;
        mCid = -1;
        mPsc = -1;
    }

    public void setLacAndCid(int lac, int cid)
    {
        mLac = lac;
        mCid = cid;
    }

    public void setPsc(int psc)
    {
        mPsc = psc;
    }

    @Override
    public void fillInNotifierBundle(Bundle bundle)
    {
        bundle.putInt("lac", mLac);
        bundle.putInt("cid", mCid);
        bundle.putInt("psc", mPsc);
    }

    @Override
    public boolean isEmpty()
    {
        return mLac == -1 && mCid == -1 && mPsc == -1;
    }

    @Override
    public int hashCode()
    {
        return mLac ^ mCid;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof GsmCellLocation)) return false;
        GsmCellLocation location = (GsmCellLocation)other;
        return mLac == location.mLac && mCid == location.mCid && mPsc == location.mPsc;
    }

    @Override
    public String toString()
    {
        return "[" + mLac + "," + mCid + "," + mPsc + "]";
    }
}
