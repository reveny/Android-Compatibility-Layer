package android.telephony.cdma;

import android.os.Bundle;
import android.telephony.CellLocation;

public class CdmaCellLocation extends CellLocation
{
    private static final int INVALID_LAT_LONG = Integer.MAX_VALUE;

    private int mBaseStationId = -1;
    private int mBaseStationLatitude = INVALID_LAT_LONG;
    private int mBaseStationLongitude = INVALID_LAT_LONG;
    private int mSystemId = -1;
    private int mNetworkId = -1;

    public CdmaCellLocation()
    {
    }

    public CdmaCellLocation(Bundle bundle)
    {
        mBaseStationId = bundle.getInt("baseStationId", -1);
        mBaseStationLatitude = bundle.getInt("baseStationLatitude", INVALID_LAT_LONG);
        mBaseStationLongitude = bundle.getInt("baseStationLongitude", INVALID_LAT_LONG);
        mSystemId = bundle.getInt("systemId", -1);
        mNetworkId = bundle.getInt("networkId", -1);
    }

    public int getBaseStationId()
    {
        return mBaseStationId;
    }

    public int getBaseStationLatitude()
    {
        return mBaseStationLatitude;
    }

    public int getBaseStationLongitude()
    {
        return mBaseStationLongitude;
    }

    public int getSystemId()
    {
        return mSystemId;
    }

    public int getNetworkId()
    {
        return mNetworkId;
    }

    @Override
    public void setStateInvalid()
    {
        mBaseStationId = -1;
        mBaseStationLatitude = INVALID_LAT_LONG;
        mBaseStationLongitude = INVALID_LAT_LONG;
        mSystemId = -1;
        mNetworkId = -1;
    }

    public void setCellLocationData(int baseStationId, int baseStationLatitude, int baseStationLongitude)
    {
        mBaseStationId = baseStationId;
        mBaseStationLatitude = baseStationLatitude;
        mBaseStationLongitude = baseStationLongitude;
    }

    public void setCellLocationData(int baseStationId, int baseStationLatitude, int baseStationLongitude, int systemId, int networkId)
    {
        setCellLocationData(baseStationId, baseStationLatitude, baseStationLongitude);
        mSystemId = systemId;
        mNetworkId = networkId;
    }

    public static double convertQuartSecToDecDegrees(int quartSec)
    {
        if (quartSec < -2592000 || quartSec > 2592000)
        {
            throw new IllegalArgumentException("Invalid coordiante value:" + quartSec);
        }
        return ((double)quartSec) / (3600 * 4);
    }

    @Override
    public void fillInNotifierBundle(Bundle bundle)
    {
        bundle.putInt("baseStationId", mBaseStationId);
        bundle.putInt("baseStationLatitude", mBaseStationLatitude);
        bundle.putInt("baseStationLongitude", mBaseStationLongitude);
        bundle.putInt("systemId", mSystemId);
        bundle.putInt("networkId", mNetworkId);
    }

    @Override
    public boolean isEmpty()
    {
        return mBaseStationId == -1 && mBaseStationLatitude == INVALID_LAT_LONG && mBaseStationLongitude == INVALID_LAT_LONG
            && mSystemId == -1 && mNetworkId == -1;
    }

    @Override
    public int hashCode()
    {
        return mBaseStationId ^ mBaseStationLatitude ^ mBaseStationLongitude ^ mSystemId ^ mNetworkId;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CdmaCellLocation)) return false;
        CdmaCellLocation location = (CdmaCellLocation)other;
        return mBaseStationId == location.mBaseStationId && mBaseStationLatitude == location.mBaseStationLatitude
            && mBaseStationLongitude == location.mBaseStationLongitude && mSystemId == location.mSystemId && mNetworkId == location.mNetworkId;
    }

    @Override
    public String toString()
    {
        return "[" + mBaseStationId + "," + mBaseStationLatitude + "," + mBaseStationLongitude + "," + mSystemId + "," + mNetworkId + "]";
    }
}
