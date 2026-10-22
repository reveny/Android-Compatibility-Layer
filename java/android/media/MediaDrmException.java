package android.media;

public class MediaDrmException extends Exception implements MediaDrmThrowable
{
    public MediaDrmException(String message)
    {
        super(message);
    }

    public int getErrorContext()
    {
        return 0;
    }

    public int getOemError()
    {
        return 0;
    }

    public int getVendorError()
    {
        return 0;
    }
}
