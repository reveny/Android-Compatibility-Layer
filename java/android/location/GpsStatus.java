package android.location;

import java.util.ArrayList;
import java.util.List;

public final class GpsStatus
{
    public static final int GPS_EVENT_STARTED = 1;
    public static final int GPS_EVENT_STOPPED = 2;
    public static final int GPS_EVENT_FIRST_FIX = 3;
    public static final int GPS_EVENT_SATELLITE_STATUS = 4;

    private static final int MAX_SATELLITES = 255;
    private static final int GLONASS_SVID_OFFSET = 64;
    private static final int BEIDOU_SVID_OFFSET = 200;
    private static final int SBAS_SVID_OFFSET = -87;

    private final List<GpsSatellite> mSatellites = new ArrayList<>();
    private int mTimeToFirstFix;

    private GpsStatus()
    {
    }

    public interface Listener
    {
        void onGpsStatusChanged(int event);
    }

    public interface NmeaListener
    {
        void onNmeaReceived(long timestamp, String nmea);
    }

    public static GpsStatus create(GnssStatus gnssStatus, int timeToFirstFix)
    {
        GpsStatus status = new GpsStatus();
        status.setStatus(gnssStatus, timeToFirstFix);
        return status;
    }

    static GpsStatus createEmpty()
    {
        return new GpsStatus();
    }

    void setStatus(GnssStatus gnssStatus, int timeToFirstFix)
    {
        mSatellites.clear();
        mTimeToFirstFix = timeToFirstFix;

        for (int index = 0; index < gnssStatus.getSatelliteCount(); index++)
        {
            int prn = gnssStatus.getSvid(index);
            int type = gnssStatus.getConstellationType(index);

            if (type == GnssStatus.CONSTELLATION_GLONASS) prn += GLONASS_SVID_OFFSET;
            else if (type == GnssStatus.CONSTELLATION_BEIDOU) prn += BEIDOU_SVID_OFFSET;
            else if (type == GnssStatus.CONSTELLATION_SBAS) prn += SBAS_SVID_OFFSET;
            else if (type != GnssStatus.CONSTELLATION_GPS && type != GnssStatus.CONSTELLATION_QZSS) continue;

            if (prn <= 0 || prn > MAX_SATELLITES) continue;

            GpsSatellite satellite = new GpsSatellite(prn);
            satellite.mValid = true;
            satellite.mSnr = gnssStatus.getCn0DbHz(index);
            satellite.mElevation = gnssStatus.getElevationDegrees(index);
            satellite.mAzimuth = gnssStatus.getAzimuthDegrees(index);
            satellite.mHasEphemeris = gnssStatus.hasEphemerisData(index);
            satellite.mHasAlmanac = gnssStatus.hasAlmanacData(index);
            satellite.mUsedInFix = gnssStatus.usedInFix(index);
            mSatellites.add(satellite);
        }
    }

    public int getTimeToFirstFix()
    {
        return mTimeToFirstFix;
    }

    public Iterable<GpsSatellite> getSatellites()
    {
        return new ArrayList<>(mSatellites);
    }

    public int getMaxSatellites()
    {
        return mSatellites.size();
    }
}
