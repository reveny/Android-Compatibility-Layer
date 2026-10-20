package android.telephony;

public class ServiceState
{
    public static final int STATE_IN_SERVICE = 0;
    public static final int STATE_OUT_OF_SERVICE = 1;
    public static final int STATE_EMERGENCY_ONLY = 2;
    public static final int STATE_POWER_OFF = 3;

    private int mState = STATE_OUT_OF_SERVICE;
    private boolean mRoaming;

    public int getState()
    {
        return mState;
    }

    public void setState(int state)
    {
        mState = state;
    }

    public boolean getRoaming()
    {
        return mRoaming;
    }

    public void setRoaming(boolean roaming)
    {
        mRoaming = roaming;
    }

    public String getOperatorAlphaLong()
    {
        return null;
    }

    public boolean isSearching()
    {
        return false;
    }
}
