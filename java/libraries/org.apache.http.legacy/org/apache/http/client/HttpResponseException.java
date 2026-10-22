package org.apache.http.client;

public class HttpResponseException extends ClientProtocolException
{
    private final int statusCode;

    public HttpResponseException(int statusCode, String s)
    {
        super(s);
        this.statusCode = statusCode;
    }

    public int getStatusCode()
    {
        return statusCode;
    }
}
