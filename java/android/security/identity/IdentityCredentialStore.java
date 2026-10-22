package android.security.identity;

import android.content.Context;

public abstract class IdentityCredentialStore
{
    public static final int CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256 = 1;

    IdentityCredentialStore()
    {
    }

    public static IdentityCredentialStore getInstance(Context context)
    {
        return null;
    }

    public static IdentityCredentialStore getDirectAccessInstance(Context context)
    {
        return null;
    }

    public abstract String[] getSupportedDocTypes();

    public abstract WritableIdentityCredential createCredential(String credentialName, String docType)
            throws AlreadyPersonalizedException, DocTypeNotSupportedException;

    public abstract IdentityCredential getCredentialByName(String credentialName, int cipherSuite)
            throws CipherSuiteNotSupportedException;

    public abstract byte[] deleteCredentialByName(String credentialName);

    public PresentationSession createPresentationSession(int cipherSuite) throws CipherSuiteNotSupportedException
    {
        throw new UnsupportedOperationException();
    }
}
