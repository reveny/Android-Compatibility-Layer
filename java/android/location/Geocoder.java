package android.location;

import android.content.Context;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class Geocoder
{
    private final Context mContext;
    private final Locale mLocale;

    public Geocoder(Context context)
    {
        this(context, Locale.getDefault());
    }

    public Geocoder(Context context, Locale locale)
    {
        if (locale == null) throw new NullPointerException("locale");

        mContext = context;
        mLocale = locale;
    }

    public interface GeocodeListener
    {
        void onGeocode(List<Address> addresses);

        default void onError(String errorMessage)
        {
        }
    }

    public static boolean isPresent()
    {
        return false;
    }

    public List<Address> getFromLocation(double latitude, double longitude, int maxResults) throws IOException
    {
        checkLatitude(latitude);
        checkLongitude(longitude);

        throw new IOException("Service not Available");
    }

    public void getFromLocation(double latitude, double longitude, int maxResults, GeocodeListener listener)
    {
        checkLatitude(latitude);
        checkLongitude(longitude);

        listener.onError("Service not Available");
    }

    public List<Address> getFromLocationName(String locationName, int maxResults) throws IOException
    {
        if (locationName == null) throw new IllegalArgumentException("locationName == null");

        throw new IOException("Service not Available");
    }

    public void getFromLocationName(String locationName, int maxResults, GeocodeListener listener)
    {
        if (locationName == null) throw new IllegalArgumentException("locationName == null");

        listener.onError("Service not Available");
    }

    public List<Address> getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude,
                                             double upperRightLatitude, double upperRightLongitude) throws IOException
    {
        return getFromLocationName(locationName, maxResults);
    }

    public void getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude,
                                    double upperRightLatitude, double upperRightLongitude, GeocodeListener listener)
    {
        getFromLocationName(locationName, maxResults, listener);
    }

    private static void checkLatitude(double latitude)
    {
        if (latitude < -90.0 || latitude > 90.0) throw new IllegalArgumentException("latitude == " + latitude);
    }

    private static void checkLongitude(double longitude)
    {
        if (longitude < -180.0 || longitude > 180.0) throw new IllegalArgumentException("longitude == " + longitude);
    }
}
