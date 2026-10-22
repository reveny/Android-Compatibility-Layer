package android.net.http;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.X509TrustManager;

public class X509TrustManagerExtensions
{
    private final X509TrustManager mDelegate;

    public X509TrustManagerExtensions(X509TrustManager trustManager) throws IllegalArgumentException
    {
        if (trustManager == null) throw new IllegalArgumentException("tm is null");

        mDelegate = trustManager;
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, String host)
        throws CertificateException
    {
        if (chain == null || chain.length == 0) throw new IllegalArgumentException("null or zero-length certificate chain");

        mDelegate.checkServerTrusted(chain, authType);

        return new ArrayList<>(Arrays.asList(chain));
    }

    public boolean isUserAddedCertificate(X509Certificate cert)
    {
        return false;
    }

    public boolean isSameTrustConfiguration(String hostname1, String hostname2)
    {
        return true;
    }
}
