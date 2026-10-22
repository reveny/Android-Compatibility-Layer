package android.media;

import java.util.UUID;

public final class MediaCrypto
{
    public MediaCrypto(UUID uuid, byte[] sessionId) throws MediaCryptoException
    {
        throw new MediaCryptoException("Failed to instantiate crypto object: scheme " + uuid + " is not supported");
    }

    public static boolean isCryptoSchemeSupported(UUID uuid)
    {
        return false;
    }

    public void release()
    {
    }

    public boolean requiresSecureDecoderComponent(String mime)
    {
        return false;
    }

    public void setMediaDrmSession(byte[] sessionId) throws MediaCryptoException
    {
        throw new MediaCryptoException("No crypto plugin is available");
    }
}
