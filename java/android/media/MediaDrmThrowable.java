package android.media;

public interface MediaDrmThrowable
{
    default int getErrorContext()
    {
        return 0;
    }

    default int getOemError()
    {
        return 0;
    }

    default int getVendorError()
    {
        return 0;
    }
}
