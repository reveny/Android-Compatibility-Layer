package android.net;

public abstract class NetworkSpecifier
{
    public NetworkSpecifier()
    {
    }

    public boolean canBeSatisfiedBy(NetworkSpecifier other)
    {
        return false;
    }

    public NetworkSpecifier redact()
    {
        return this;
    }
}
