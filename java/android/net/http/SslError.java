package android.net.http;

import java.security.cert.X509Certificate;

public class SslError
{
    public static final int SSL_NOTYETVALID = 0;
    public static final int SSL_EXPIRED = 1;
    public static final int SSL_IDMISMATCH = 2;
    public static final int SSL_UNTRUSTED = 3;
    public static final int SSL_DATE_INVALID = 4;
    public static final int SSL_INVALID = 5;
    @Deprecated
    public static final int SSL_MAX_ERROR = 6;

    private int mErrors;
    private final SslCertificate mCertificate;
    private final String mUrl;

    @Deprecated
    public SslError(int error, SslCertificate certificate)
    {
        this(error, certificate, "");
    }

    @Deprecated
    public SslError(int error, X509Certificate certificate)
    {
        this(error, certificate, "");
    }

    public SslError(int error, SslCertificate certificate, String url)
    {
        if (certificate == null) throw new NullPointerException("certificate is null");
        if (url == null) throw new NullPointerException("url is null");
        addError(error);
        mCertificate = certificate;
        mUrl = url;
    }

    public SslError(int error, X509Certificate certificate, String url)
    {
        this(error, new SslCertificate(certificate), url);
    }

    public SslCertificate getCertificate()
    {
        return mCertificate;
    }

    public String getUrl()
    {
        return mUrl;
    }

    public boolean addError(int error)
    {
        boolean valid = error >= 0 && error < SSL_MAX_ERROR;
        if (valid) mErrors |= 1 << error;
        return valid;
    }

    public boolean hasError(int error)
    {
        if (error < 0 || error >= SSL_MAX_ERROR) return false;
        return (mErrors & (1 << error)) != 0;
    }

    public int getPrimaryError()
    {
        for (int error = SSL_MAX_ERROR - 1; error >= 0; error--)
        {
            if ((mErrors & (1 << error)) != 0) return error;
        }
        return -1;
    }

    @Override
    public String toString()
    {
        return "primary error: " + getPrimaryError() + " certificate: " + getCertificate() + " on URL: " + getUrl();
    }
}
