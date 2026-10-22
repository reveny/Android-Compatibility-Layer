package android.location;

import android.os.Bundle;

import java.util.List;

public interface LocationListener
{
    void onLocationChanged(Location location);

    default void onLocationChanged(List<Location> locations)
    {
        int size = locations.size();
        for (int index = 0; index < size; index++)
        {
            onLocationChanged(locations.get(index));
        }
    }

    default void onFlushComplete(int requestCode)
    {
    }

    @Deprecated
    default void onStatusChanged(String provider, int status, Bundle extras)
    {
    }

    default void onProviderEnabled(String provider)
    {
    }

    default void onProviderDisabled(String provider)
    {
    }
}
