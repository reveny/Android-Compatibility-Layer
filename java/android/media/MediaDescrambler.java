package android.media;

import java.nio.ByteBuffer;

public final class MediaDescrambler implements AutoCloseable
{
    public static final byte SCRAMBLE_CONTROL_EVEN_KEY = 2;
    public static final byte SCRAMBLE_CONTROL_ODD_KEY = 3;
    public static final byte SCRAMBLE_CONTROL_RESERVED = 1;
    public static final byte SCRAMBLE_CONTROL_UNSCRAMBLED = 0;
    public static final byte SCRAMBLE_FLAG_PES_HEADER = 1;

    public MediaDescrambler(int casSystemId) throws MediaCasException.UnsupportedCasException
    {
        throw new MediaCasException.UnsupportedCasException("Unsupported CA_system_id " + casSystemId);
    }

    public void close()
    {
    }

    public int descramble(ByteBuffer source, ByteBuffer destination, MediaCodec.CryptoInfo cryptoInfo)
    {
        throw new IllegalStateException("descrambler is not initialized");
    }

    public boolean requiresSecureDecoderComponent(String mime)
    {
        return false;
    }

    public void setMediaCasSession(MediaCas.Session session)
    {
        throw new IllegalStateException("descrambler is not initialized");
    }
}
