package android.webkit;

import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public abstract class ClientCertRequest
{
    public ClientCertRequest()
    {
    }

    public abstract String[] getKeyTypes();

    public abstract Principal[] getPrincipals();

    public abstract String getHost();

    public abstract int getPort();

    public abstract void proceed(PrivateKey privateKey, X509Certificate[] chain);

    public abstract void ignore();

    public abstract void cancel();
}
