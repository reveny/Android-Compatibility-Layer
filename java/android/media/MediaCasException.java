package android.media;

public class MediaCasException extends Exception
{
    MediaCasException(String message)
    {
        super(message);
    }

    public static final class UnsupportedCasException extends MediaCasException
    {
        UnsupportedCasException(String message)
        {
            super(message);
        }
    }

    public static final class ResourceBusyException extends MediaCasException
    {
        ResourceBusyException(String message)
        {
            super(message);
        }
    }

    public static final class NotProvisionedException extends MediaCasException
    {
        NotProvisionedException(String message)
        {
            super(message);
        }
    }

    public static final class InsufficientResourceException extends MediaCasException
    {
        InsufficientResourceException(String message)
        {
            super(message);
        }
    }

    public static final class DeniedByServerException extends MediaCasException
    {
        DeniedByServerException(String message)
        {
            super(message);
        }
    }
}
