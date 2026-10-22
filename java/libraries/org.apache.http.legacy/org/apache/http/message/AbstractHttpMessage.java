package org.apache.http.message;

import java.util.ArrayList;
import org.apache.http.Header;
import org.apache.http.HttpMessage;

public abstract class AbstractHttpMessage implements HttpMessage
{
    protected final ArrayList<Header> headergroup = new ArrayList<>();

    protected AbstractHttpMessage()
    {
    }

    @Override
    public boolean containsHeader(String name)
    {
        return getFirstHeader(name) != null;
    }

    @Override
    public Header[] getHeaders(String name)
    {
        ArrayList<Header> matching = new ArrayList<>();
        for (Header header : headergroup)
        {
            if (header.getName().equalsIgnoreCase(name)) matching.add(header);
        }
        return matching.toArray(new Header[0]);
    }

    @Override
    public Header getFirstHeader(String name)
    {
        for (Header header : headergroup)
        {
            if (header.getName().equalsIgnoreCase(name)) return header;
        }
        return null;
    }

    @Override
    public Header getLastHeader(String name)
    {
        for (int index = headergroup.size() - 1; index >= 0; index--)
        {
            Header header = headergroup.get(index);
            if (header.getName().equalsIgnoreCase(name)) return header;
        }
        return null;
    }

    @Override
    public Header[] getAllHeaders()
    {
        return headergroup.toArray(new Header[0]);
    }

    @Override
    public void addHeader(Header header)
    {
        if (header != null) headergroup.add(header);
    }

    @Override
    public void addHeader(String name, String value)
    {
        if (name == null) throw new IllegalArgumentException("Header name may not be null");
        headergroup.add(new BasicHeader(name, value));
    }

    @Override
    public void setHeader(Header header)
    {
        if (header == null) return;

        for (int index = 0; index < headergroup.size(); index++)
        {
            if (!headergroup.get(index).getName().equalsIgnoreCase(header.getName())) continue;

            headergroup.set(index, header);
            return;
        }

        headergroup.add(header);
    }

    @Override
    public void setHeader(String name, String value)
    {
        if (name == null) throw new IllegalArgumentException("Header name may not be null");
        setHeader(new BasicHeader(name, value));
    }

    @Override
    public void setHeaders(Header[] headers)
    {
        headergroup.clear();
        if (headers == null) return;

        for (Header header : headers) addHeader(header);
    }

    @Override
    public void removeHeader(Header header)
    {
        headergroup.remove(header);
    }

    @Override
    public void removeHeaders(String name)
    {
        if (name == null) return;
        headergroup.removeIf(header -> header.getName().equalsIgnoreCase(name));
    }
}
