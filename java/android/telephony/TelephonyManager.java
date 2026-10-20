package android.telephony;

public class TelephonyManager
{
    public static final int CALL_STATE_IDLE = 0;
    public static final int CALL_STATE_RINGING = 1;
    public static final int CALL_STATE_OFFHOOK = 2;

    public static final int PHONE_TYPE_NONE = 0;
    public static final int NETWORK_TYPE_UNKNOWN = 0;

    public void listen(PhoneStateListener listener, int events)
    {
    }

    public int getCallState()
    {
        return CALL_STATE_IDLE;
    }

    public int getPhoneType()
    {
        return PHONE_TYPE_NONE;
    }

    public int getNetworkType()
    {
        return NETWORK_TYPE_UNKNOWN;
    }

    public String getNetworkOperatorName()
    {
        return "";
    }

    public String getSimOperatorName()
    {
        return "";
    }

    public boolean isNetworkRoaming()
    {
        return false;
    }
}
