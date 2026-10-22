package org.apache.http.message;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;

public class BasicStatusLine implements StatusLine, Cloneable
{
    private final ProtocolVersion protoVersion;
    private final int statusCode;
    private final String reasonPhrase;

    public BasicStatusLine(ProtocolVersion version, int statusCode, String reasonPhrase)
    {
        if (version == null) throw new IllegalArgumentException("Protocol version may not be null.");
        if (statusCode < 0) throw new IllegalArgumentException("Status code may not be negative.");

        this.protoVersion = version;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public ProtocolVersion getProtocolVersion()
    {
        return protoVersion;
    }

    @Override
    public int getStatusCode()
    {
        return statusCode;
    }

    @Override
    public String getReasonPhrase()
    {
        return reasonPhrase;
    }

    @Override
    public String toString()
    {
        return reasonPhrase == null ? protoVersion + " " + statusCode : protoVersion + " " + statusCode + " " + reasonPhrase;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
