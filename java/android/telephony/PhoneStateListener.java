package android.telephony;

public class PhoneStateListener
{
    public static final int LISTEN_NONE = 0;
    public static final int LISTEN_CALL_STATE = 0x00000020;

    public void onCallStateChanged(int state, String phoneNumber)
    {
    }

    public void onServiceStateChanged(ServiceState serviceState)
    {
    }

    public void onDataConnectionStateChanged(int state)
    {
    }
}
