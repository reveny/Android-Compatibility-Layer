package android.net;

public class NetworkInfo
{
    public enum State
    {
        CONNECTING,
        CONNECTED,
        SUSPENDED,
        DISCONNECTING,
        DISCONNECTED,
        UNKNOWN
    }

    public enum DetailedState
    {
        IDLE,
        SCANNING,
        CONNECTING,
        AUTHENTICATING,
        OBTAINING_IPADDR,
        CONNECTED,
        SUSPENDED,
        DISCONNECTING,
        DISCONNECTED,
        FAILED,
        BLOCKED,
        VERIFYING_POOR_LINK,
        CAPTIVE_PORTAL_CHECK
    }

    public int getType()
    {
        return -1;
    }

    public String getTypeName()
    {
        return "NONE";
    }

    public int getSubtype()
    {
        return 0;
    }

    public String getSubtypeName()
    {
        return "";
    }

    public boolean isConnected()
    {
        return false;
    }

    public boolean isConnectedOrConnecting()
    {
        return false;
    }

    public boolean isAvailable()
    {
        return false;
    }

    public State getState()
    {
        return State.DISCONNECTED;
    }

    public DetailedState getDetailedState()
    {
        return DetailedState.DISCONNECTED;
    }
}
