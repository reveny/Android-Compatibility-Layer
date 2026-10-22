package android.util;

import java.io.IOException;

public final class MalformedJsonException extends IOException
{
    private static final long serialVersionUID = 1L;

    public MalformedJsonException(String message)
    {
        super(message);
    }
}
