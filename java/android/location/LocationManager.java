package android.location;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class LocationManager
{
    public static final String ACTION_GNSS_CAPABILITIES_CHANGED = "android.location.action.GNSS_CAPABILITIES_CHANGED";
    public static final String EXTRA_GNSS_CAPABILITIES = "android.location.extra.GNSS_CAPABILITIES";
    public static final String EXTRA_LOCATION_ENABLED = "android.location.extra.LOCATION_ENABLED";
    public static final String EXTRA_PROVIDER_ENABLED = "android.location.extra.PROVIDER_ENABLED";
    public static final String EXTRA_PROVIDER_NAME = "android.location.extra.PROVIDER_NAME";
    public static final String FUSED_PROVIDER = "fused";
    public static final String GPS_PROVIDER = "gps";
    public static final String KEY_FLUSH_COMPLETE = "flushComplete";
    public static final String KEY_LOCATIONS = "locations";
    public static final String KEY_LOCATION_CHANGED = "location";
    public static final String KEY_PROVIDER_ENABLED = "providerEnabled";
    public static final String KEY_PROXIMITY_ENTERING = "entering";
    public static final String KEY_STATUS_CHANGED = "status";
    public static final String MODE_CHANGED_ACTION = "android.location.MODE_CHANGED";
    public static final String NETWORK_PROVIDER = "network";
    public static final String PASSIVE_PROVIDER = "passive";
    public static final String PROVIDERS_CHANGED_ACTION = "android.location.PROVIDERS_CHANGED";

    private static final String TAG = "LocationManager";
    private static final List<String> PROVIDERS = Collections.unmodifiableList(Arrays.asList(PASSIVE_PROVIDER, GPS_PROVIDER,
                                                                                            NETWORK_PROVIDER, FUSED_PROVIDER));

    private final Context mContext;
    private final Map<LocationListener, String> mListeners = new LinkedHashMap<>();
    private final List<PendingIntent> mPendingIntents = new ArrayList<>();
    private final List<Object> mStatusCallbacks = new ArrayList<>();

    public LocationManager(Context context)
    {
        mContext = context;
    }

    public boolean isLocationEnabled()
    {
        return false;
    }

    public boolean isProviderEnabled(String provider)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");

        return false;
    }

    public Location getLastKnownLocation(String provider)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");

        enforceLocationPermission();
        return null;
    }

    public void getCurrentLocation(String provider, CancellationSignal cancellationSignal, Executor executor,
                                   Consumer<Location> consumer)
    {
        getCurrentLocation(provider, new LocationRequest.Builder(0).build(), cancellationSignal, executor, consumer);
    }

    public void getCurrentLocation(String provider, LocationRequest locationRequest, CancellationSignal cancellationSignal,
                                   Executor executor, final Consumer<Location> consumer)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");
        if (executor == null) throw new IllegalArgumentException("invalid null executor");
        if (consumer == null) throw new IllegalArgumentException("invalid null consumer");

        enforceLocationPermission();

        if (cancellationSignal != null && cancellationSignal.isCanceled()) return;

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                consumer.accept(null);
            }
        });
    }

    @Deprecated
    public void requestSingleUpdate(String provider, LocationListener listener, Looper looper)
    {
        register(provider, listener, executorFor(looper));
    }

    @Deprecated
    public void requestSingleUpdate(Criteria criteria, LocationListener listener, Looper looper)
    {
        register(getBestProvider(criteria, false), listener, executorFor(looper));
    }

    @Deprecated
    public void requestSingleUpdate(String provider, PendingIntent pendingIntent)
    {
        register(provider, pendingIntent);
    }

    @Deprecated
    public void requestSingleUpdate(Criteria criteria, PendingIntent pendingIntent)
    {
        register(getBestProvider(criteria, false), pendingIntent);
    }

    public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, LocationListener listener)
    {
        register(provider, listener, executorFor(null));
    }

    public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, LocationListener listener, Looper looper)
    {
        register(provider, listener, executorFor(looper));
    }

    public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, Executor executor, LocationListener listener)
    {
        register(provider, listener, executor);
    }

    public void requestLocationUpdates(long minTimeMs, float minDistanceM, Criteria criteria, LocationListener listener, Looper looper)
    {
        register(getBestProvider(criteria, false), listener, executorFor(looper));
    }

    public void requestLocationUpdates(long minTimeMs, float minDistanceM, Criteria criteria, Executor executor,
                                       LocationListener listener)
    {
        register(getBestProvider(criteria, false), listener, executor);
    }

    public void requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, PendingIntent pendingIntent)
    {
        register(provider, pendingIntent);
    }

    public void requestLocationUpdates(long minTimeMs, float minDistanceM, Criteria criteria, PendingIntent pendingIntent)
    {
        register(getBestProvider(criteria, false), pendingIntent);
    }

    public void requestLocationUpdates(String provider, LocationRequest locationRequest, Executor executor, LocationListener listener)
    {
        register(provider, listener, executor);
    }

    public void requestLocationUpdates(String provider, LocationRequest locationRequest, PendingIntent pendingIntent)
    {
        register(provider, pendingIntent);
    }

    public void requestFlush(String provider, final LocationListener listener, final int requestCode)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");
        if (listener == null) throw new IllegalArgumentException("invalid null listener");

        synchronized (mListeners)
        {
            if (!mListeners.containsKey(listener)) throw new IllegalArgumentException("unregistered listener cannot be flushed");
        }

        executorFor(null).execute(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onFlushComplete(requestCode);
            }
        });
    }

    public void requestFlush(String provider, PendingIntent pendingIntent, int requestCode)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");
        if (pendingIntent == null) throw new IllegalArgumentException("invalid null pending intent");
    }

    public void removeUpdates(LocationListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");

        synchronized (mListeners)
        {
            mListeners.remove(listener);
        }
    }

    public void removeUpdates(PendingIntent pendingIntent)
    {
        if (pendingIntent == null) throw new IllegalArgumentException("pendingIntent must not be null");

        synchronized (mPendingIntents)
        {
            mPendingIntents.remove(pendingIntent);
        }
    }

    public boolean hasProvider(String provider)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");

        return PROVIDERS.contains(provider);
    }

    public List<String> getAllProviders()
    {
        return new ArrayList<>(PROVIDERS);
    }

    public List<String> getProviders(boolean enabledOnly)
    {
        if (enabledOnly) return new ArrayList<>();

        return new ArrayList<>(PROVIDERS);
    }

    public List<String> getProviders(Criteria criteria, boolean enabledOnly)
    {
        if (criteria == null) throw new IllegalArgumentException("invalid null criteria");
        if (enabledOnly) return new ArrayList<>();

        List<String> matches = new ArrayList<>();

        for (String name : PROVIDERS)
        {
            LocationProvider provider = getProvider(name);
            if (provider != null && provider.meetsCriteria(criteria)) matches.add(name);
        }

        return matches;
    }

    public String getBestProvider(Criteria criteria, boolean enabledOnly)
    {
        if (criteria == null) throw new IllegalArgumentException("invalid null criteria");
        if (enabledOnly) return null;

        if (criteria.getAccuracy() == Criteria.ACCURACY_FINE) return GPS_PROVIDER;
        if (criteria.getPowerRequirement() == Criteria.POWER_HIGH) return GPS_PROVIDER;

        return NETWORK_PROVIDER;
    }

    public LocationProvider getProvider(String provider)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");

        if (GPS_PROVIDER.equals(provider))
        {
            return new LocationProvider(provider, false, true, false, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
        }

        if (NETWORK_PROVIDER.equals(provider))
        {
            return new LocationProvider(provider, true, false, true, Criteria.POWER_LOW, Criteria.ACCURACY_COARSE);
        }

        if (FUSED_PROVIDER.equals(provider))
        {
            return new LocationProvider(provider, false, false, false, Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        }

        if (PASSIVE_PROVIDER.equals(provider))
        {
            return new LocationProvider(provider, false, false, false, Criteria.POWER_LOW, Criteria.ACCURACY_COARSE);
        }

        return null;
    }

    public boolean sendExtraCommand(String provider, String command, Bundle extras)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");
        if (command == null) throw new IllegalArgumentException("invalid null command");

        return false;
    }

    public void addTestProvider(String provider, boolean requiresNetwork, boolean requiresSatellite, boolean requiresCell,
                                boolean hasMonetaryCost, boolean supportsAltitude, boolean supportsSpeed, boolean supportsBearing,
                                int powerUsage, int accuracy)
    {
        throw new SecurityException(mContext.getPackageName() + " is not the selected mock location app");
    }

    public void removeTestProvider(String provider)
    {
        throw new SecurityException(mContext.getPackageName() + " is not the selected mock location app");
    }

    public void setTestProviderLocation(String provider, Location location)
    {
        throw new SecurityException(mContext.getPackageName() + " is not the selected mock location app");
    }

    @Deprecated
    public void clearTestProviderLocation(String provider)
    {
    }

    public void setTestProviderEnabled(String provider, boolean enabled)
    {
        throw new SecurityException(mContext.getPackageName() + " is not the selected mock location app");
    }

    @Deprecated
    public void clearTestProviderEnabled(String provider)
    {
    }

    @Deprecated
    public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime)
    {
    }

    @Deprecated
    public void clearTestProviderStatus(String provider)
    {
    }

    public void addProximityAlert(double latitude, double longitude, float radius, long expiration, PendingIntent pendingIntent)
    {
        if (pendingIntent == null) throw new IllegalArgumentException("invalid null pending intent");

        enforceLocationPermission();
    }

    public void removeProximityAlert(PendingIntent intent)
    {
    }

    public GnssCapabilities getGnssCapabilities()
    {
        return new GnssCapabilities();
    }

    public int getGnssYearOfHardware()
    {
        return 0;
    }

    public String getGnssHardwareModelName()
    {
        return null;
    }

    @Deprecated
    public GpsStatus getGpsStatus(GpsStatus status)
    {
        if (status == null) return GpsStatus.createEmpty();

        return status;
    }

    @Deprecated
    public boolean addGpsStatusListener(GpsStatus.Listener listener)
    {
        return addStatusCallback(listener);
    }

    @Deprecated
    public void removeGpsStatusListener(GpsStatus.Listener listener)
    {
        removeStatusCallback(listener);
    }

    public boolean registerGnssStatusCallback(GnssStatus.Callback callback)
    {
        return addStatusCallback(callback);
    }

    public boolean registerGnssStatusCallback(GnssStatus.Callback callback, Handler handler)
    {
        return addStatusCallback(callback);
    }

    public boolean registerGnssStatusCallback(Executor executor, GnssStatus.Callback callback)
    {
        return addStatusCallback(callback);
    }

    public void unregisterGnssStatusCallback(GnssStatus.Callback callback)
    {
        removeStatusCallback(callback);
    }

    @Deprecated
    public boolean addNmeaListener(GpsStatus.NmeaListener listener)
    {
        return false;
    }

    @Deprecated
    public void removeNmeaListener(GpsStatus.NmeaListener listener)
    {
    }

    public boolean addNmeaListener(OnNmeaMessageListener listener)
    {
        return addStatusCallback(listener);
    }

    public boolean addNmeaListener(OnNmeaMessageListener listener, Handler handler)
    {
        return addStatusCallback(listener);
    }

    public boolean addNmeaListener(Executor executor, OnNmeaMessageListener listener)
    {
        return addStatusCallback(listener);
    }

    public void removeNmeaListener(OnNmeaMessageListener listener)
    {
        removeStatusCallback(listener);
    }

    public boolean registerGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback)
    {
        return registerGnssMeasurementsCallback(executorFor(null), callback);
    }

    public boolean registerGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback, Handler handler)
    {
        return registerGnssMeasurementsCallback(executorFor(handler == null ? null : handler.getLooper()), callback);
    }

    public boolean registerGnssMeasurementsCallback(Executor executor, final GnssMeasurementsEvent.Callback callback)
    {
        if (callback == null) throw new IllegalArgumentException("invalid null callback");

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onStatusChanged(GnssMeasurementsEvent.Callback.STATUS_NOT_SUPPORTED);
            }
        });

        return addStatusCallback(callback);
    }

    public void unregisterGnssMeasurementsCallback(GnssMeasurementsEvent.Callback callback)
    {
        removeStatusCallback(callback);
    }

    private boolean addStatusCallback(Object callback)
    {
        if (callback == null) throw new IllegalArgumentException("invalid null callback");

        enforceFineLocationPermission();

        synchronized (mStatusCallbacks)
        {
            if (!mStatusCallbacks.contains(callback)) mStatusCallbacks.add(callback);
        }

        return true;
    }

    private void removeStatusCallback(Object callback)
    {
        synchronized (mStatusCallbacks)
        {
            mStatusCallbacks.remove(callback);
        }
    }

    private void register(final String provider, final LocationListener listener, Executor executor)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");
        if (listener == null) throw new IllegalArgumentException("invalid null listener");
        if (executor == null) throw new IllegalArgumentException("invalid null executor");

        enforceLocationPermission();

        synchronized (mListeners)
        {
            mListeners.put(listener, provider);
        }

        Log.i(TAG, "a listener is registered for the disabled " + provider + " provider");

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                boolean registered;
                synchronized (mListeners)
                {
                    registered = mListeners.containsKey(listener);
                }

                if (registered) listener.onProviderDisabled(provider);
            }
        });
    }

    private void register(String provider, PendingIntent pendingIntent)
    {
        if (provider == null) throw new IllegalArgumentException("invalid null provider");
        if (pendingIntent == null) throw new IllegalArgumentException("invalid null pending intent");

        enforceLocationPermission();

        synchronized (mPendingIntents)
        {
            if (!mPendingIntents.contains(pendingIntent)) mPendingIntents.add(pendingIntent);
        }
    }

    private Executor executorFor(Looper looper)
    {
        Looper target = looper != null ? looper : Looper.myLooper();
        if (target == null) target = Looper.getMainLooper();

        if (target == null)
        {
            return new Executor()
            {
                @Override
                public void execute(Runnable command)
                {
                    command.run();
                }
            };
        }

        final Handler handler = new Handler(target);
        return new Executor()
        {
            @Override
            public void execute(Runnable command)
            {
                handler.post(command);
            }
        };
    }

    private static boolean granted(String permission)
    {
        return runtime.Permissions.check(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private static void enforceLocationPermission()
    {
        if (granted("android.permission.ACCESS_FINE_LOCATION")) return;
        if (granted("android.permission.ACCESS_COARSE_LOCATION")) return;

        throw new SecurityException("uid " + android.os.Process.myUid() + " does not have android.permission.ACCESS_COARSE_LOCATION"
                                    + " or android.permission.ACCESS_FINE_LOCATION.");
    }

    private static void enforceFineLocationPermission()
    {
        if (granted("android.permission.ACCESS_FINE_LOCATION")) return;

        throw new SecurityException("uid " + android.os.Process.myUid() + " does not have android.permission.ACCESS_FINE_LOCATION.");
    }
}
