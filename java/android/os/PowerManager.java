package android.os;

public class PowerManager
{
    public static final int PARTIAL_WAKE_LOCK = 0x00000001;
    public static final int FULL_WAKE_LOCK = 0x0000001a;
    public static final int SCREEN_BRIGHT_WAKE_LOCK = 0x0000000a;

    public class WakeLock
    {
        private boolean mHeld;

        public void acquire()
        {
            mHeld = true;
        }

        public void acquire(long timeout)
        {
            mHeld = true;
        }

        public void release()
        {
            mHeld = false;
        }

        public boolean isHeld()
        {
            return mHeld;
        }

        public void setReferenceCounted(boolean counted)
        {
        }
    }

    public WakeLock newWakeLock(int levelAndFlags, String tag)
    {
        return new WakeLock();
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
}
