package android.net;

public interface TransportInfo
{
    default long getApplicableRedactions()
    {
        return 0;
    }

    default TransportInfo makeCopy(long redactions)
    {
        return this;
    }
}
