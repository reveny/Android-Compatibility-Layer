package android.media;

import android.content.Context;
import android.os.Handler;

import java.util.Arrays;

public final class MediaCas implements AutoCloseable
{
    public static final int PLUGIN_STATUS_PHYSICAL_MODULE_CHANGED = 0;
    public static final int PLUGIN_STATUS_SESSION_NUMBER_CHANGED = 1;
    public static final int SCRAMBLING_MODE_AES128 = 9;
    public static final int SCRAMBLING_MODE_AES_CBC = 14;
    public static final int SCRAMBLING_MODE_AES_ECB = 10;
    public static final int SCRAMBLING_MODE_AES_SCTE52 = 11;
    public static final int SCRAMBLING_MODE_DVB_CISSA_V1 = 6;
    public static final int SCRAMBLING_MODE_DVB_CSA1 = 1;
    public static final int SCRAMBLING_MODE_DVB_CSA2 = 2;
    public static final int SCRAMBLING_MODE_DVB_CSA3_ENHANCE = 5;
    public static final int SCRAMBLING_MODE_DVB_CSA3_MINIMAL = 4;
    public static final int SCRAMBLING_MODE_DVB_CSA3_STANDARD = 3;
    public static final int SCRAMBLING_MODE_DVB_IDSA = 7;
    public static final int SCRAMBLING_MODE_MULTI2 = 8;
    public static final int SCRAMBLING_MODE_RESERVED = 0;
    public static final int SCRAMBLING_MODE_TDES_ECB = 12;
    public static final int SCRAMBLING_MODE_TDES_SCTE52 = 13;
    public static final int SESSION_USAGE_LIVE = 0;
    public static final int SESSION_USAGE_PLAYBACK = 1;
    public static final int SESSION_USAGE_RECORD = 2;
    public static final int SESSION_USAGE_TIMESHIFT = 3;

    private boolean mClosed;

    public MediaCas(Context context, int casSystemId, String tvInputServiceSessionId, int priorityHint)
        throws MediaCasException.UnsupportedCasException
    {
        this(casSystemId);
    }

    public MediaCas(Context context, int casSystemId, String tvInputServiceSessionId, int priorityHint, Handler handler,
        MediaCas.EventListener listener) throws MediaCasException.UnsupportedCasException
    {
        this(casSystemId);
    }

    public MediaCas(int casSystemId) throws MediaCasException.UnsupportedCasException
    {
        throw new MediaCasException.UnsupportedCasException("Unsupported CA_system_id " + casSystemId);
    }

    private IllegalStateException unavailable()
    {
        return new IllegalStateException(mClosed ? "MediaCas instance is closed" : "MediaCas has no plugin");
    }

    public void close()
    {
        mClosed = true;
    }

    public static MediaCas.PluginDescriptor[] enumeratePlugins()
    {
        return new PluginDescriptor[0];
    }

    public static boolean isSystemIdSupported(int casSystemId)
    {
        return false;
    }

    public MediaCas.Session openSession() throws MediaCasException
    {
        throw unavailable();
    }

    public MediaCas.Session openSession(int sessionUsage, int scramblingMode) throws MediaCasException
    {
        throw unavailable();
    }

    public void processEmm(byte[] data) throws MediaCasException
    {
        processEmm(data, 0, data.length);
    }

    public void processEmm(byte[] data, int offset, int length) throws MediaCasException
    {
        throw unavailable();
    }

    public void provision(String provisionString) throws MediaCasException
    {
        throw unavailable();
    }

    public void refreshEntitlements(int refreshType, byte[] refreshData) throws MediaCasException
    {
        throw unavailable();
    }

    public void sendEvent(int event, int arg, byte[] data) throws MediaCasException
    {
        throw unavailable();
    }

    public void setEventListener(MediaCas.EventListener listener, Handler handler)
    {
    }

    public void setPrivateData(byte[] data) throws MediaCasException
    {
        throw unavailable();
    }

    public final class Session implements AutoCloseable
    {
        private final byte[] mSessionId;

        Session(byte[] sessionId)
        {
            mSessionId = sessionId;
        }

        public void close()
        {
        }

        @Override
        public boolean equals(Object object)
        {
            if (!(object instanceof Session)) return false;

            return Arrays.equals(mSessionId, ((Session)object).mSessionId);
        }

        @Override
        public int hashCode()
        {
            return Arrays.hashCode(mSessionId);
        }

        public byte[] getSessionId()
        {
            return mSessionId;
        }

        public void processEcm(byte[] data) throws MediaCasException
        {
            processEcm(data, 0, data.length);
        }

        public void processEcm(byte[] data, int offset, int length) throws MediaCasException
        {
            throw unavailable();
        }

        public void sendSessionEvent(int event, int arg, byte[] data) throws MediaCasException
        {
            throw unavailable();
        }

        public void setPrivateData(byte[] data) throws MediaCasException
        {
            throw unavailable();
        }
    }

    public static class PluginDescriptor
    {
        private final int mSystemId;
        private final String mName;

        PluginDescriptor(int systemId, String name)
        {
            mSystemId = systemId;
            mName = name;
        }

        public String getName()
        {
            return mName;
        }

        public int getSystemId()
        {
            return mSystemId;
        }

        @Override
        public String toString()
        {
            return "PluginDescriptor {" + mSystemId + ", " + mName + "}";
        }
    }

    public interface EventListener
    {
        void onEvent(MediaCas mediaCas, int event, int arg, byte[] data);

        default void onPluginStatusUpdate(MediaCas mediaCas, int status, int arg)
        {
        }

        default void onResourceLost(MediaCas mediaCas)
        {
        }

        default void onSessionEvent(MediaCas mediaCas, MediaCas.Session session, int event, int arg, byte[] data)
        {
        }
    }
}
