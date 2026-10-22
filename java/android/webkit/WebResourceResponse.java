package android.webkit;

import java.io.InputStream;
import java.util.Map;

public class WebResourceResponse
{
    private String mMimeType;
    private String mEncoding;
    private int mStatusCode = 200;
    private String mReasonPhrase = "OK";
    private Map<String, String> mResponseHeaders;
    private InputStream mInputStream;

    public WebResourceResponse(String mimeType, String encoding, InputStream data)
    {
        mMimeType = mimeType;
        mEncoding = encoding;
        setData(data);
    }

    public WebResourceResponse(String mimeType, String encoding, int statusCode, String reasonPhrase,
        Map<String, String> responseHeaders, InputStream data)
    {
        this(mimeType, encoding, data);
        setStatusCodeAndReasonPhrase(statusCode, reasonPhrase);
        setResponseHeaders(responseHeaders);
    }

    public void setMimeType(String mimeType)
    {
        mMimeType = mimeType;
    }

    public String getMimeType()
    {
        return mMimeType;
    }

    public void setEncoding(String encoding)
    {
        mEncoding = encoding;
    }

    public String getEncoding()
    {
        return mEncoding;
    }

    public void setStatusCodeAndReasonPhrase(int statusCode, String reasonPhrase)
    {
        if (statusCode < 100) throw new IllegalArgumentException("statusCode can't be less than 100.");
        if (statusCode > 599) throw new IllegalArgumentException("statusCode can't be greater than 599.");
        if (statusCode > 299 && statusCode < 400)
        {
            throw new IllegalArgumentException("statusCode can't be in the [300, 399] range.");
        }
        if (reasonPhrase == null) throw new IllegalArgumentException("reasonPhrase can't be null.");
        if (reasonPhrase.trim().isEmpty()) throw new IllegalArgumentException("reasonPhrase can't be empty.");
        for (int index = 0; index < reasonPhrase.length(); index++)
        {
            if (reasonPhrase.charAt(index) > 0x7F)
            {
                throw new IllegalArgumentException("reasonPhrase can't contain non-ASCII characters.");
            }
        }
        mStatusCode = statusCode;
        mReasonPhrase = reasonPhrase;
    }

    public int getStatusCode()
    {
        return mStatusCode;
    }

    public String getReasonPhrase()
    {
        return mReasonPhrase;
    }

    public void setResponseHeaders(Map<String, String> headers)
    {
        mResponseHeaders = headers;
    }

    public Map<String, String> getResponseHeaders()
    {
        return mResponseHeaders;
    }

    public void setData(InputStream data)
    {
        if (data instanceof java.io.StringBufferInputStream)
        {
            throw new IllegalArgumentException("StringBufferInputStream is deprecated and must not be passed to a "
                + "WebResourceResponse");
        }
        mInputStream = data;
    }

    public InputStream getData()
    {
        return mInputStream;
    }
}
