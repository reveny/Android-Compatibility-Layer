package android.security;

public class NetworkSecurityPolicy
{
    private static final NetworkSecurityPolicy INSTANCE = new NetworkSecurityPolicy();

    private NetworkSecurityPolicy()
    {
    }

    public static NetworkSecurityPolicy getInstance()
    {
        return INSTANCE;
    }

    public boolean isCleartextTrafficPermitted()
    {
        return true;
    }

    public boolean isCleartextTrafficPermitted(String hostname)
    {
        return isCleartextTrafficPermitted();
    }
}
