package android.media;

public final class MediaCryptoException extends Exception implements MediaDrmThrowable
{
    public MediaCryptoException(String message)
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
