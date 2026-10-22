package android.location;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.StringTokenizer;

public class Location implements Parcelable
{
    public static final int FORMAT_DEGREES = 0;
    public static final int FORMAT_MINUTES = 1;
    public static final int FORMAT_SECONDS = 2;

    private static final int HAS_ALTITUDE_MASK = 1 << 0;
    private static final int HAS_SPEED_MASK = 1 << 1;
    private static final int HAS_BEARING_MASK = 1 << 2;
    private static final int HAS_HORIZONTAL_ACCURACY_MASK = 1 << 3;
    private static final int HAS_MOCK_PROVIDER_MASK = 1 << 4;
    private static final int HAS_ALTITUDE_ACCURACY_MASK = 1 << 5;
    private static final int HAS_SPEED_ACCURACY_MASK = 1 << 6;
    private static final int HAS_BEARING_ACCURACY_MASK = 1 << 7;
    private static final int HAS_ELAPSED_REALTIME_UNCERTAINTY_MASK = 1 << 8;
    private static final int HAS_MSL_ALTITUDE_MASK = 1 << 9;
    private static final int HAS_MSL_ALTITUDE_ACCURACY_MASK = 1 << 10;

    private String mProvider;
    private int mFieldsMask;
    private long mTimeMs;
    private long mElapsedRealtimeNs;
    private double mElapsedRealtimeUncertaintyNs;
    private double mLatitudeDegrees;
    private double mLongitudeDegrees;
    private float mHorizontalAccuracyMeters;
    private double mAltitudeMeters;
    private float mAltitudeAccuracyMeters;
    private float mSpeedMetersPerSecond;
    private float mSpeedAccuracyMetersPerSecond;
    private float mBearingDegrees;
    private float mBearingAccuracyDegrees;
    private double mMslAltitudeMeters;
    private float mMslAltitudeAccuracyMeters;
    private Bundle mExtras;

    public Location(String provider)
    {
        mProvider = provider;
    }

    public Location(Location location)
    {
        set(location);
    }

    public void set(Location location)
    {
        mFieldsMask = location.mFieldsMask;
        mProvider = location.mProvider;
        mTimeMs = location.mTimeMs;
        mElapsedRealtimeNs = location.mElapsedRealtimeNs;
        mElapsedRealtimeUncertaintyNs = location.mElapsedRealtimeUncertaintyNs;
        mLatitudeDegrees = location.mLatitudeDegrees;
        mLongitudeDegrees = location.mLongitudeDegrees;
        mHorizontalAccuracyMeters = location.mHorizontalAccuracyMeters;
        mAltitudeMeters = location.mAltitudeMeters;
        mAltitudeAccuracyMeters = location.mAltitudeAccuracyMeters;
        mSpeedMetersPerSecond = location.mSpeedMetersPerSecond;
        mSpeedAccuracyMetersPerSecond = location.mSpeedAccuracyMetersPerSecond;
        mBearingDegrees = location.mBearingDegrees;
        mBearingAccuracyDegrees = location.mBearingAccuracyDegrees;
        mMslAltitudeMeters = location.mMslAltitudeMeters;
        mMslAltitudeAccuracyMeters = location.mMslAltitudeAccuracyMeters;
        mExtras = location.mExtras == null ? null : new Bundle(location.mExtras);
    }

    public void reset()
    {
        mProvider = null;
        mFieldsMask = 0;
        mTimeMs = 0;
        mElapsedRealtimeNs = 0;
        mElapsedRealtimeUncertaintyNs = 0.0;
        mLatitudeDegrees = 0;
        mLongitudeDegrees = 0;
        mHorizontalAccuracyMeters = 0;
        mAltitudeMeters = 0;
        mAltitudeAccuracyMeters = 0;
        mSpeedMetersPerSecond = 0;
        mSpeedAccuracyMetersPerSecond = 0;
        mBearingDegrees = 0;
        mBearingAccuracyDegrees = 0;
        mMslAltitudeMeters = 0;
        mMslAltitudeAccuracyMeters = 0;
        mExtras = null;
    }

    public float distanceTo(Location destination)
    {
        float[] results = new float[3];
        computeDistanceAndBearing(mLatitudeDegrees, mLongitudeDegrees, destination.mLatitudeDegrees, destination.mLongitudeDegrees,
                                  results);
        return results[0];
    }

    public float bearingTo(Location destination)
    {
        float[] results = new float[3];
        computeDistanceAndBearing(mLatitudeDegrees, mLongitudeDegrees, destination.mLatitudeDegrees, destination.mLongitudeDegrees,
                                  results);
        return results[1];
    }

    public String getProvider()
    {
        return mProvider;
    }

    public void setProvider(String provider)
    {
        mProvider = provider;
    }

    public long getTime()
    {
        return mTimeMs;
    }

    public void setTime(long timeMs)
    {
        mTimeMs = timeMs;
    }

    public long getElapsedRealtimeNanos()
    {
        return mElapsedRealtimeNs;
    }

    public long getElapsedRealtimeMillis()
    {
        return mElapsedRealtimeNs / 1000000L;
    }

    public long getElapsedRealtimeAgeMillis()
    {
        return getElapsedRealtimeAgeMillis(SystemClock.elapsedRealtime());
    }

    public long getElapsedRealtimeAgeMillis(long referenceRealtimeMs)
    {
        return referenceRealtimeMs - getElapsedRealtimeMillis();
    }

    public void setElapsedRealtimeNanos(long elapsedRealtimeNs)
    {
        mElapsedRealtimeNs = elapsedRealtimeNs;
    }

    public double getElapsedRealtimeUncertaintyNanos()
    {
        return mElapsedRealtimeUncertaintyNs;
    }

    public void setElapsedRealtimeUncertaintyNanos(double elapsedRealtimeUncertaintyNs)
    {
        mElapsedRealtimeUncertaintyNs = elapsedRealtimeUncertaintyNs;
        mFieldsMask |= HAS_ELAPSED_REALTIME_UNCERTAINTY_MASK;
    }

    public boolean hasElapsedRealtimeUncertaintyNanos()
    {
        return (mFieldsMask & HAS_ELAPSED_REALTIME_UNCERTAINTY_MASK) != 0;
    }

    public void removeElapsedRealtimeUncertaintyNanos()
    {
        mFieldsMask &= ~HAS_ELAPSED_REALTIME_UNCERTAINTY_MASK;
    }

    public double getLatitude()
    {
        return mLatitudeDegrees;
    }

    public void setLatitude(double latitudeDegrees)
    {
        mLatitudeDegrees = latitudeDegrees;
    }

    public double getLongitude()
    {
        return mLongitudeDegrees;
    }

    public void setLongitude(double longitudeDegrees)
    {
        mLongitudeDegrees = longitudeDegrees;
    }

    public float getAccuracy()
    {
        return mHorizontalAccuracyMeters;
    }

    public void setAccuracy(float horizontalAccuracyMeters)
    {
        mHorizontalAccuracyMeters = horizontalAccuracyMeters;
        mFieldsMask |= HAS_HORIZONTAL_ACCURACY_MASK;
    }

    public boolean hasAccuracy()
    {
        return (mFieldsMask & HAS_HORIZONTAL_ACCURACY_MASK) != 0;
    }

    public void removeAccuracy()
    {
        mFieldsMask &= ~HAS_HORIZONTAL_ACCURACY_MASK;
    }

    public double getAltitude()
    {
        return mAltitudeMeters;
    }

    public void setAltitude(double altitudeMeters)
    {
        mAltitudeMeters = altitudeMeters;
        mFieldsMask |= HAS_ALTITUDE_MASK;
    }

    public boolean hasAltitude()
    {
        return (mFieldsMask & HAS_ALTITUDE_MASK) != 0;
    }

    public void removeAltitude()
    {
        mFieldsMask &= ~HAS_ALTITUDE_MASK;
    }

    public float getVerticalAccuracyMeters()
    {
        return mAltitudeAccuracyMeters;
    }

    public void setVerticalAccuracyMeters(float altitudeAccuracyMeters)
    {
        mAltitudeAccuracyMeters = altitudeAccuracyMeters;
        mFieldsMask |= HAS_ALTITUDE_ACCURACY_MASK;
    }

    public boolean hasVerticalAccuracy()
    {
        return (mFieldsMask & HAS_ALTITUDE_ACCURACY_MASK) != 0;
    }

    public void removeVerticalAccuracy()
    {
        mFieldsMask &= ~HAS_ALTITUDE_ACCURACY_MASK;
    }

    public float getSpeed()
    {
        return mSpeedMetersPerSecond;
    }

    public void setSpeed(float speedMetersPerSecond)
    {
        mSpeedMetersPerSecond = speedMetersPerSecond;
        mFieldsMask |= HAS_SPEED_MASK;
    }

    public boolean hasSpeed()
    {
        return (mFieldsMask & HAS_SPEED_MASK) != 0;
    }

    public void removeSpeed()
    {
        mFieldsMask &= ~HAS_SPEED_MASK;
    }

    public float getSpeedAccuracyMetersPerSecond()
    {
        return mSpeedAccuracyMetersPerSecond;
    }

    public void setSpeedAccuracyMetersPerSecond(float speedAccuracyMeterPerSecond)
    {
        mSpeedAccuracyMetersPerSecond = speedAccuracyMeterPerSecond;
        mFieldsMask |= HAS_SPEED_ACCURACY_MASK;
    }

    public boolean hasSpeedAccuracy()
    {
        return (mFieldsMask & HAS_SPEED_ACCURACY_MASK) != 0;
    }

    public void removeSpeedAccuracy()
    {
        mFieldsMask &= ~HAS_SPEED_ACCURACY_MASK;
    }

    public float getBearing()
    {
        return mBearingDegrees;
    }

    public void setBearing(float bearingDegrees)
    {
        if (Float.isNaN(bearingDegrees) || Float.isInfinite(bearingDegrees)) throw new IllegalArgumentException("invalid bearing");

        float modBearing = bearingDegrees % 360f + 0f;
        if (modBearing < 0) modBearing += 360f;

        mBearingDegrees = modBearing;
        mFieldsMask |= HAS_BEARING_MASK;
    }

    public boolean hasBearing()
    {
        return (mFieldsMask & HAS_BEARING_MASK) != 0;
    }

    public void removeBearing()
    {
        mFieldsMask &= ~HAS_BEARING_MASK;
    }

    public float getBearingAccuracyDegrees()
    {
        return mBearingAccuracyDegrees;
    }

    public void setBearingAccuracyDegrees(float bearingAccuracyDegrees)
    {
        mBearingAccuracyDegrees = bearingAccuracyDegrees;
        mFieldsMask |= HAS_BEARING_ACCURACY_MASK;
    }

    public boolean hasBearingAccuracy()
    {
        return (mFieldsMask & HAS_BEARING_ACCURACY_MASK) != 0;
    }

    public void removeBearingAccuracy()
    {
        mFieldsMask &= ~HAS_BEARING_ACCURACY_MASK;
    }

    public double getMslAltitudeMeters()
    {
        if (!hasMslAltitude()) throw new IllegalStateException("the mean sea level altitude is not set");

        return mMslAltitudeMeters;
    }

    public void setMslAltitudeMeters(double mslAltitudeMeters)
    {
        mMslAltitudeMeters = mslAltitudeMeters;
        mFieldsMask |= HAS_MSL_ALTITUDE_MASK;
    }

    public boolean hasMslAltitude()
    {
        return (mFieldsMask & HAS_MSL_ALTITUDE_MASK) != 0;
    }

    public void removeMslAltitude()
    {
        mFieldsMask &= ~HAS_MSL_ALTITUDE_MASK;
    }

    public float getMslAltitudeAccuracyMeters()
    {
        if (!hasMslAltitudeAccuracy()) throw new IllegalStateException("the mean sea level altitude accuracy is not set");

        return mMslAltitudeAccuracyMeters;
    }

    public void setMslAltitudeAccuracyMeters(float mslAltitudeAccuracyMeters)
    {
        mMslAltitudeAccuracyMeters = mslAltitudeAccuracyMeters;
        mFieldsMask |= HAS_MSL_ALTITUDE_ACCURACY_MASK;
    }

    public boolean hasMslAltitudeAccuracy()
    {
        return (mFieldsMask & HAS_MSL_ALTITUDE_ACCURACY_MASK) != 0;
    }

    public void removeMslAltitudeAccuracy()
    {
        mFieldsMask &= ~HAS_MSL_ALTITUDE_ACCURACY_MASK;
    }

    @Deprecated
    public boolean isFromMockProvider()
    {
        return isMock();
    }

    public boolean isMock()
    {
        return (mFieldsMask & HAS_MOCK_PROVIDER_MASK) != 0;
    }

    public void setMock(boolean mock)
    {
        if (mock) mFieldsMask |= HAS_MOCK_PROVIDER_MASK;
        else mFieldsMask &= ~HAS_MOCK_PROVIDER_MASK;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public void setExtras(Bundle extras)
    {
        mExtras = extras == null ? null : new Bundle(extras);
    }

    public boolean isComplete()
    {
        return mProvider != null && hasAccuracy() && mTimeMs != 0 && mElapsedRealtimeNs != 0;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Location)) return false;

        Location location = (Location)other;
        return mTimeMs == location.mTimeMs && mElapsedRealtimeNs == location.mElapsedRealtimeNs
            && mFieldsMask == location.mFieldsMask && Double.compare(location.mLatitudeDegrees, mLatitudeDegrees) == 0
            && Double.compare(location.mLongitudeDegrees, mLongitudeDegrees) == 0
            && (!hasAltitude() || Double.compare(location.mAltitudeMeters, mAltitudeMeters) == 0)
            && (!hasSpeed() || Float.compare(location.mSpeedMetersPerSecond, mSpeedMetersPerSecond) == 0)
            && (!hasBearing() || Float.compare(location.mBearingDegrees, mBearingDegrees) == 0)
            && (!hasAccuracy() || Float.compare(location.mHorizontalAccuracyMeters, mHorizontalAccuracyMeters) == 0)
            && (!hasVerticalAccuracy() || Float.compare(location.mAltitudeAccuracyMeters, mAltitudeAccuracyMeters) == 0)
            && (!hasSpeedAccuracy() || Float.compare(location.mSpeedAccuracyMetersPerSecond, mSpeedAccuracyMetersPerSecond) == 0)
            && (!hasBearingAccuracy() || Float.compare(location.mBearingAccuracyDegrees, mBearingAccuracyDegrees) == 0)
            && (!hasMslAltitude() || Double.compare(location.mMslAltitudeMeters, mMslAltitudeMeters) == 0)
            && java.util.Objects.equals(mProvider, location.mProvider);
    }

    @Override
    public int hashCode()
    {
        return java.util.Objects.hash(mProvider, mElapsedRealtimeNs, mLatitudeDegrees, mLongitudeDegrees);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Location[");
        builder.append(mProvider);
        builder.append(" ").append(String.format(Locale.ROOT, "%.6f,%.6f", mLatitudeDegrees, mLongitudeDegrees));
        if (hasAccuracy()) builder.append(" hAcc=").append(mHorizontalAccuracyMeters);
        builder.append(" et=").append(mElapsedRealtimeNs / 1000000L).append("ms");
        if (hasAltitude()) builder.append(" alt=").append(mAltitudeMeters);
        if (hasVerticalAccuracy()) builder.append(" vAcc=").append(mAltitudeAccuracyMeters);
        if (hasMslAltitude()) builder.append(" mslAlt=").append(mMslAltitudeMeters);
        if (hasSpeed()) builder.append(" vel=").append(mSpeedMetersPerSecond);
        if (hasSpeedAccuracy()) builder.append(" sAcc=").append(mSpeedAccuracyMetersPerSecond);
        if (hasBearing()) builder.append(" bear=").append(mBearingDegrees);
        if (hasBearingAccuracy()) builder.append(" bAcc=").append(mBearingAccuracyDegrees);
        if (isMock()) builder.append(" mock");
        if (mExtras != null) builder.append(" {").append(mExtras).append('}');
        builder.append(']');
        return builder.toString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeString(mProvider);
        parcel.writeInt(mFieldsMask);
        parcel.writeLong(mTimeMs);
        parcel.writeLong(mElapsedRealtimeNs);
        parcel.writeLong(Double.doubleToRawLongBits(mElapsedRealtimeUncertaintyNs));
        parcel.writeLong(Double.doubleToRawLongBits(mLatitudeDegrees));
        parcel.writeLong(Double.doubleToRawLongBits(mLongitudeDegrees));
        parcel.writeFloat(mHorizontalAccuracyMeters);
        parcel.writeLong(Double.doubleToRawLongBits(mAltitudeMeters));
        parcel.writeFloat(mAltitudeAccuracyMeters);
        parcel.writeFloat(mSpeedMetersPerSecond);
        parcel.writeFloat(mSpeedAccuracyMetersPerSecond);
        parcel.writeFloat(mBearingDegrees);
        parcel.writeFloat(mBearingAccuracyDegrees);
        parcel.writeLong(Double.doubleToRawLongBits(mMslAltitudeMeters));
        parcel.writeFloat(mMslAltitudeAccuracyMeters);
        parcel.writeBundle(mExtras);
    }

    private static Location readFromParcel(Parcel parcel)
    {
        Location location = new Location(parcel.readString());
        location.mFieldsMask = parcel.readInt();
        location.mTimeMs = parcel.readLong();
        location.mElapsedRealtimeNs = parcel.readLong();
        location.mElapsedRealtimeUncertaintyNs = Double.longBitsToDouble(parcel.readLong());
        location.mLatitudeDegrees = Double.longBitsToDouble(parcel.readLong());
        location.mLongitudeDegrees = Double.longBitsToDouble(parcel.readLong());
        location.mHorizontalAccuracyMeters = parcel.readFloat();
        location.mAltitudeMeters = Double.longBitsToDouble(parcel.readLong());
        location.mAltitudeAccuracyMeters = parcel.readFloat();
        location.mSpeedMetersPerSecond = parcel.readFloat();
        location.mSpeedAccuracyMetersPerSecond = parcel.readFloat();
        location.mBearingDegrees = parcel.readFloat();
        location.mBearingAccuracyDegrees = parcel.readFloat();
        location.mMslAltitudeMeters = Double.longBitsToDouble(parcel.readLong());
        location.mMslAltitudeAccuracyMeters = parcel.readFloat();
        location.mExtras = parcel.readBundle();
        return location;
    }

    public static final Creator<Location> CREATOR = new Creator<Location>()
    {
        @Override
        public Location createFromParcel(Parcel parcel)
        {
            return readFromParcel(parcel);
        }

        @Override
        public Location[] newArray(int size)
        {
            return new Location[size];
        }
    };

    public static String convert(double coordinate, int outputType)
    {
        if (coordinate < -180.0 || coordinate > 180.0 || Double.isNaN(coordinate))
        {
            throw new IllegalArgumentException("coordinate=" + coordinate);
        }

        if (outputType != FORMAT_DEGREES && outputType != FORMAT_MINUTES && outputType != FORMAT_SECONDS)
        {
            throw new IllegalArgumentException("outputType=" + outputType);
        }

        StringBuilder builder = new StringBuilder();

        if (coordinate < 0)
        {
            builder.append('-');
            coordinate = -coordinate;
        }

        DecimalFormat format = new DecimalFormat("###.#####");

        if (outputType == FORMAT_MINUTES || outputType == FORMAT_SECONDS)
        {
            int degrees = (int)Math.floor(coordinate);
            builder.append(degrees);
            builder.append(':');
            coordinate -= degrees;
            coordinate *= 60.0;

            if (outputType == FORMAT_SECONDS)
            {
                int minutes = (int)Math.floor(coordinate);
                builder.append(minutes);
                builder.append(':');
                coordinate -= minutes;
                coordinate *= 60.0;
            }
        }

        builder.append(format.format(coordinate));
        return builder.toString();
    }

    public static double convert(String coordinate)
    {
        if (coordinate == null) throw new NullPointerException("coordinate");

        boolean negative = false;

        if (coordinate.charAt(0) == '-')
        {
            coordinate = coordinate.substring(1);
            negative = true;
        }

        StringTokenizer tokenizer = new StringTokenizer(coordinate, ":");
        int tokens = tokenizer.countTokens();
        if (tokens < 1) throw new IllegalArgumentException("coordinate=" + coordinate);

        try
        {
            String degrees = tokenizer.nextToken();
            double value;

            if (tokens == 1)
            {
                value = Double.parseDouble(degrees);
                return negative ? -value : value;
            }

            String minutes = tokenizer.nextToken();
            int degreesValue = Integer.parseInt(degrees);
            double minutesValue;
            double secondsValue = 0.0;
            boolean secondsPresent = false;

            if (tokenizer.hasMoreTokens())
            {
                minutesValue = Integer.parseInt(minutes);
                String seconds = tokenizer.nextToken();
                secondsValue = Double.parseDouble(seconds);
                secondsPresent = true;
            }
            else
            {
                minutesValue = Double.parseDouble(minutes);
            }

            boolean isNegative180 = negative && degreesValue == 180 && minutesValue == 0 && secondsValue == 0;

            if (degreesValue < 0.0 || (degreesValue > 179 && !isNegative180))
            {
                throw new IllegalArgumentException("coordinate=" + coordinate);
            }

            if (minutesValue < 0 || minutesValue >= 60 || (secondsPresent && minutesValue > 59))
            {
                throw new IllegalArgumentException("coordinate=" + coordinate);
            }

            if (secondsValue < 0 || secondsValue >= 60) throw new IllegalArgumentException("coordinate=" + coordinate);

            value = degreesValue * 3600.0 + minutesValue * 60.0 + secondsValue;
            value /= 3600.0;
            return negative ? -value : value;
        }
        catch (NumberFormatException malformed)
        {
            throw new IllegalArgumentException("coordinate=" + coordinate);
        }
    }

    public static void distanceBetween(double startLatitude, double startLongitude, double endLatitude, double endLongitude,
                                       float[] results)
    {
        if (results == null || results.length < 1) throw new IllegalArgumentException("results is null or has length < 1");

        float[] computed = new float[3];
        computeDistanceAndBearing(startLatitude, startLongitude, endLatitude, endLongitude, computed);

        results[0] = computed[0];
        if (results.length > 1) results[1] = computed[1];
        if (results.length > 2) results[2] = computed[2];
    }

    private static void computeDistanceAndBearing(double lat1, double lon1, double lat2, double lon2, float[] results)
    {
        int maxIterations = 20;

        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;

        double a = 6378137.0;
        double b = 6356752.3142;
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

        double l = lon2 - lon1;
        double aA = 0.0;
        double u1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double u2 = Math.atan((1.0 - f) * Math.tan(lat2));

        double cosU1 = Math.cos(u1);
        double cosU2 = Math.cos(u2);
        double sinU1 = Math.sin(u1);
        double sinU2 = Math.sin(u2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;

        double sigma = 0.0;
        double deltaSigma = 0.0;
        double cosSqAlpha;
        double cos2SM;
        double cosSigma;
        double sinSigma;
        double cosLambda = 0.0;
        double sinLambda = 0.0;

        double lambda = l;

        for (int iteration = 0; iteration < maxIterations; iteration++)
        {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2;
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            double sinAlpha = (sinSigma == 0) ? 0.0 : cosU1cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 : cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha;

            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq;
            aA = 1 + (uSquared / 16384.0) * (4096.0 + uSquared * (-768 + uSquared * (320.0 - 175.0 * uSquared)));
            double bB = (uSquared / 1024.0) * (256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
            double cC = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha));
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = bB * sinSigma
                * (cos2SM + (bB / 4.0)
                   * (cosSigma * (-1.0 + 2.0 * cos2SMSq) - (bB / 6.0) * cos2SM * (-3.0 + 4.0 * sinSigma * sinSigma)
                      * (-3.0 + 4.0 * cos2SMSq)));

            lambda = l + (1.0 - cC) * f * sinAlpha * (sigma + cC * sinSigma * (cos2SM + cC * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM)));

            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) break;
        }

        results[0] = (float)(b * aA * (sigma - deltaSigma));

        float initialBearing = (float)Math.atan2(cosU2 * sinLambda, cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
        initialBearing = (float)(initialBearing * (180.0 / Math.PI));
        results[1] = initialBearing;

        float finalBearing = (float)Math.atan2(cosU1 * sinLambda, -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
        results[2] = (float)(finalBearing * (180.0 / Math.PI));
    }
}
