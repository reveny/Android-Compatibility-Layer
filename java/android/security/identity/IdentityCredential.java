package android.security.identity;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class IdentityCredential
{
    IdentityCredential()
    {
    }

    public abstract KeyPair createEphemeralKeyPair();

    public abstract void setReaderEphemeralPublicKey(PublicKey readerEphemeralPublicKey) throws InvalidKeyException;

    public abstract byte[] encryptMessageToReader(byte[] messagePlaintext);

    public abstract byte[] decryptMessageFromReader(byte[] messageCiphertext) throws MessageDecryptionException;

    public abstract Collection<X509Certificate> getCredentialKeyCertificateChain();

    public abstract void setAllowUsingExhaustedKeys(boolean allowUsingExhaustedKeys);

    public void setAllowUsingExpiredKeys(boolean allowUsingExpiredKeys)
    {
        throw new UnsupportedOperationException();
    }

    public byte[] proveOwnership(byte[] challenge)
    {
        throw new UnsupportedOperationException();
    }

    public byte[] delete(byte[] challenge)
    {
        throw new UnsupportedOperationException();
    }

    public byte[] update(PersonalizationData personalizationData)
    {
        throw new UnsupportedOperationException();
    }

    public abstract ResultData getEntries(byte[] requestMessage, Map<String, Collection<String>> entriesToRequest,
            byte[] sessionTranscript, byte[] readerSignature)
            throws EphemeralPublicKeyNotFoundException, InvalidReaderSignatureException, InvalidRequestMessageException,
            NoAuthenticationKeyAvailableException, SessionTranscriptMismatchException;

    public abstract void setAvailableAuthenticationKeys(int keyCount, int maxUsesPerKey);

    public void setAvailableAuthenticationKeys(int keyCount, int maxUsesPerKey, long minValidTimeMillis)
    {
        throw new UnsupportedOperationException();
    }

    public abstract Collection<X509Certificate> getAuthKeysNeedingCertification();

    public abstract void storeStaticAuthenticationData(X509Certificate authenticationKey, byte[] staticAuthData)
            throws UnknownAuthenticationKeyException;

    public void storeStaticAuthenticationData(X509Certificate authenticationKey, Instant expirationDate, byte[] staticAuthData)
            throws UnknownAuthenticationKeyException
    {
        throw new UnsupportedOperationException();
    }

    public abstract int[] getAuthenticationDataUsageCount();

    public List<AuthenticationKeyMetadata> getAuthenticationKeyMetadata()
    {
        throw new UnsupportedOperationException();
    }
}
