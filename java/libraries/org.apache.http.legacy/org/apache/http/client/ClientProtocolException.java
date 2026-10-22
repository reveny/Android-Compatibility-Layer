package org.apache.http.client;

import java.io.IOException;

public class ClientProtocolException extends IOException
{
    public ClientProtocolException()
    {
        super();
    }

    public ClientProtocolException(String s)
    {
        super(s);
    }

    public ClientProtocolException(Throwable cause)
    {
        super(cause);
    }

    public ClientProtocolException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
