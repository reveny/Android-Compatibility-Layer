package android.media;

public class MediaDrmResetException extends IllegalStateException implements MediaDrmThrowable
{
    public MediaDrmResetException(String message)
    {
        super(message);
    }
}
