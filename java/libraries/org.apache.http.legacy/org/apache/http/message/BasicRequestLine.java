package org.apache.http.message;

import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;

public class BasicRequestLine implements RequestLine, Cloneable
{
    private final ProtocolVersion protoversion;
    private final String method;
    private final String uri;

    public BasicRequestLine(String method, String uri, ProtocolVersion version)
    {
        if (method == null) throw new IllegalArgumentException("Method must not be null.");
        if (uri == null) throw new IllegalArgumentException("URI must not be null.");
        if (version == null) throw new IllegalArgumentException("Protocol version must not be null.");

        this.method = method;
        this.uri = uri;
        this.protoversion = version;
    }

    @Override
    public String getMethod()
    {
        return method;
    }

    @Override
    public ProtocolVersion getProtocolVersion()
    {
        return protoversion;
    }

    @Override
    public String getUri()
    {
        return uri;
    }

    @Override
    public String toString()
    {
        return method + " " + uri + " " + protoversion;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
