package org.apache.http.client.methods;

import java.net.URI;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.BasicRequestLine;

public abstract class HttpRequestBase extends AbstractHttpMessage implements HttpUriRequest, Cloneable
{
    private URI uri;
    private volatile boolean aborted;

    public HttpRequestBase()
    {
        super();
    }

    @Override
    public abstract String getMethod();

    @Override
    public ProtocolVersion getProtocolVersion()
    {
        return HttpVersion.HTTP_1_1;
    }

    @Override
    public URI getURI()
    {
        return uri;
    }

    @Override
    public RequestLine getRequestLine()
    {
        String target = uri == null ? null : uri.toASCIIString();
        if (target == null || target.length() == 0) target = "/";

        return new BasicRequestLine(getMethod(), target, getProtocolVersion());
    }

    public void setURI(URI uri)
    {
        this.uri = uri;
    }

    @Override
    public void abort()
    {
        aborted = true;
    }

    @Override
    public boolean isAborted()
    {
        return aborted;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
