package android.os;

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class PowerManager
{
    public static final int PARTIAL_WAKE_LOCK = 0x00000001;
    public static final int SCREEN_DIM_WAKE_LOCK = 0x00000006;
    public static final int SCREEN_BRIGHT_WAKE_LOCK = 0x0000000a;
    public static final int FULL_WAKE_LOCK = 0x0000001a;
    public static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 0x00000020;
    public static final int ACQUIRE_CAUSES_WAKEUP = 0x10000000;
    public static final int ON_AFTER_RELEASE = 0x20000000;
    public static final int RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY = 1;

    public static final int THERMAL_STATUS_NONE = 0;
    public static final int THERMAL_STATUS_LIGHT = 1;
    public static final int THERMAL_STATUS_MODERATE = 2;
    public static final int THERMAL_STATUS_SEVERE = 3;
    public static final int THERMAL_STATUS_CRITICAL = 4;
    public static final int THERMAL_STATUS_EMERGENCY = 5;
    public static final int THERMAL_STATUS_SHUTDOWN = 6;

    public static final int LOCATION_MODE_NO_CHANGE = 0;
    public static final int LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF = 1;
    public static final int LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF = 2;
    public static final int LOCATION_MODE_FOREGROUND_ONLY = 3;
    public static final int LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF = 4;

    public static final String ACTION_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED";
    public static final String ACTION_DEVICE_IDLE_MODE_CHANGED = "android.os.action.DEVICE_IDLE_MODE_CHANGED";
    public static final String ACTION_DEVICE_LIGHT_IDLE_MODE_CHANGED = "android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED";
    public static final String ACTION_LOW_POWER_STANDBY_ENABLED_CHANGED = "android.os.action.LOW_POWER_STANDBY_ENABLED_CHANGED";

    private static final int WAKE_LOCK_LEVEL_MASK = 0x0000ffff;

    private final ArrayList<OnThermalStatusChangedListener> mThermalListeners = new ArrayList<>();

    public interface OnThermalStatusChangedListener
    {
        void onThermalStatusChanged(int status);
    }

    public final class WakeLock
    {
        private final int mFlags;
        private final String mTag;
        private final Runnable mReleaser = new Runnable()
        {
            @Override
            public void run()
            {
                release(RELEASE_FLAG_TIMEOUT);
            }
        };
        private int mCount;
        private boolean mRefCounted = true;
        private boolean mHeld;
        private int mTimeouts;
        private WorkSource mWorkSource;

        WakeLock(int flags, String tag)
        {
            mFlags = flags;
            mTag = tag;
        }

        public void setReferenceCounted(boolean counted)
        {
            synchronized (this)
            {
                mRefCounted = counted;
            }
        }

        public void acquire()
        {
            synchronized (this)
            {
                acquireLocked();
            }
        }

        public void acquire(long timeout)
        {
            synchronized (this)
            {
                acquireLocked();

                Handler handler = handler();
                if (handler == null) return;

                mTimeouts++;
                handler.postDelayed(mReleaser, timeout);
            }
        }

        private void acquireLocked()
        {
            if (!mRefCounted || mCount++ == 0)
            {
                Handler handler = handler();
                if (handler != null) handler.removeCallbacks(mReleaser);
                mTimeouts = 0;
                mHeld = true;
            }
        }

        public void release()
        {
            release(0);
        }

        public void release(int flags)
        {
            synchronized (this)
            {
                if (flags == RELEASE_FLAG_TIMEOUT)
                {
                    if (mTimeouts <= 0) return;
                    mTimeouts--;
                }

                if (mRefCounted && mCount > 0) mCount--;

                if (!mRefCounted || mCount == 0)
                {
                    Handler handler = handler();
                    if (handler != null) handler.removeCallbacks(mReleaser);
                    mTimeouts = 0;
                    mHeld = false;
                }
            }
        }

        public boolean isHeld()
        {
            synchronized (this)
            {
                return mHeld;
            }
        }

        public void setWorkSource(WorkSource workSource)
        {
            synchronized (this)
            {
                mWorkSource = workSource == null ? null : new WorkSource(workSource);
            }
        }

        @Override
        public String toString()
        {
            synchronized (this)
            {
                return "WakeLock{" + Integer.toHexString(System.identityHashCode(this)) + " held=" + mHeld + ", refCount=" + mCount
                       + "}";
            }
        }
    }

    private static final int RELEASE_FLAG_TIMEOUT = 0x10000;

    private static Handler handler()
    {
        Looper main = Looper.getMainLooper();
        return main == null ? null : new Handler(main);
    }

    public WakeLock newWakeLock(int levelAndFlags, String tag)
    {
        if (tag == null) throw new IllegalArgumentException("The tag must not be null.");
        if (!isWakeLockLevelSupported(levelAndFlags & WAKE_LOCK_LEVEL_MASK))
        {
            throw new IllegalArgumentException("Must specify a valid wake lock level.");
        }

        return new WakeLock(levelAndFlags, tag);
    }

    public boolean isWakeLockLevelSupported(int level)
    {
        switch (level)
        {
            case PARTIAL_WAKE_LOCK:
            case SCREEN_DIM_WAKE_LOCK:
            case SCREEN_BRIGHT_WAKE_LOCK:
            case FULL_WAKE_LOCK:
                return true;
            default:
                return false;
        }
    }

    public boolean isInteractive()
    {
        return true;
    }

    public boolean isScreenOn()
    {
        return true;
    }

    public boolean isPowerSaveMode()
    {
        return false;
    }

    public boolean isDeviceIdleMode()
    {
        return false;
    }

    public boolean isDeviceLightIdleMode()
    {
        return false;
    }

    public boolean isLowPowerStandbyEnabled()
    {
        return false;
    }

    public boolean isExemptFromLowPowerStandby()
    {
        return true;
    }

    public boolean isAllowedInLowPowerStandby(int reason)
    {
        return true;
    }

    public boolean isAllowedInLowPowerStandby(String feature)
    {
        return true;
    }

    public boolean isIgnoringBatteryOptimizations(String packageName)
    {
        return true;
    }

    public boolean isSustainedPerformanceModeSupported()
    {
        return false;
    }

    public boolean isRebootingUserspaceSupported()
    {
        return false;
    }

    public void reboot(String reason)
    {
        throw new SecurityException("android.permission.REBOOT is not granted to this application");
    }

    public java.time.Duration getBatteryDischargePrediction()
    {
        return null;
    }

    public boolean isBatteryDischargePredictionPersonalized()
    {
        return false;
    }

    public int getLocationPowerSaveMode()
    {
        return LOCATION_MODE_NO_CHANGE;
    }

    public int getCurrentThermalStatus()
    {
        return THERMAL_STATUS_NONE;
    }

    public void addThermalStatusListener(OnThermalStatusChangedListener listener)
    {
        addThermalStatusListener(null, listener);
    }

    public void addThermalStatusListener(Executor executor, final OnThermalStatusChangedListener listener)
    {
        if (listener == null) throw new NullPointerException("listener cannot be null");

        synchronized (mThermalListeners)
        {
            if (mThermalListeners.contains(listener)) throw new IllegalArgumentException("Listener already registered: " + listener);
            mThermalListeners.add(listener);
        }

        Runnable report = new Runnable()
        {
            @Override
            public void run()
            {
                listener.onThermalStatusChanged(getCurrentThermalStatus());
            }
        };

        if (executor != null)
        {
            executor.execute(report);
            return;
        }

        Handler handler = handler();
        if (handler != null) handler.post(report);
    }

    public void removeThermalStatusListener(OnThermalStatusChangedListener listener)
    {
        if (listener == null) throw new NullPointerException("listener cannot be null");

        synchronized (mThermalListeners)
        {
            if (!mThermalListeners.remove(listener)) throw new IllegalArgumentException("Listener was not added: " + listener);
        }
    }

    public float getThermalHeadroom(int forecastSeconds)
    {
        if (forecastSeconds < 0 || forecastSeconds > 60) return Float.NaN;
        return 0.3f;
    }
}
