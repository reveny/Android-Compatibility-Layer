package android.media;

import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.PersistableBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

public final class MediaDrm implements AutoCloseable
{
    public static final int EVENT_KEY_EXPIRED = 3;
    public static final int EVENT_KEY_REQUIRED = 2;
    public static final int EVENT_PROVISION_REQUIRED = 1;
    public static final int EVENT_SESSION_RECLAIMED = 5;
    public static final int EVENT_VENDOR_DEFINED = 4;
    public static final int HDCP_LEVEL_UNKNOWN = 0;
    public static final int HDCP_NONE = 1;
    public static final int HDCP_NO_DIGITAL_OUTPUT = 2147483647;
    public static final int HDCP_V1 = 2;
    public static final int HDCP_V2 = 3;
    public static final int HDCP_V2_1 = 4;
    public static final int HDCP_V2_2 = 5;
    public static final int HDCP_V2_3 = 6;
    public static final int KEY_TYPE_OFFLINE = 2;
    public static final int KEY_TYPE_RELEASE = 3;
    public static final int KEY_TYPE_STREAMING = 1;
    public static final int OFFLINE_LICENSE_STATE_RELEASED = 2;
    public static final int OFFLINE_LICENSE_STATE_UNKNOWN = 0;
    public static final int OFFLINE_LICENSE_STATE_USABLE = 1;
    public static final String PROPERTY_ALGORITHMS = "algorithms";
    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_DEVICE_UNIQUE_ID = "deviceUniqueId";
    public static final String PROPERTY_VENDOR = "vendor";
    public static final String PROPERTY_VERSION = "version";
    public static final int SECURITY_LEVEL_HW_SECURE_ALL = 5;
    public static final int SECURITY_LEVEL_HW_SECURE_CRYPTO = 3;
    public static final int SECURITY_LEVEL_HW_SECURE_DECODE = 4;
    public static final int SECURITY_LEVEL_SW_SECURE_CRYPTO = 1;
    public static final int SECURITY_LEVEL_SW_SECURE_DECODE = 2;
    public static final int SECURITY_LEVEL_UNKNOWN = 0;

    private boolean mClosed;

    public MediaDrm(UUID uuid) throws UnsupportedSchemeException
    {
        if (uuid == null) throw new IllegalArgumentException("uuid must not be null");

        throw new UnsupportedSchemeException("Failed to instantiate drm object: scheme " + uuid + " is not supported");
    }

    private IllegalStateException unavailable()
    {
        return new IllegalStateException(mClosed ? "MediaDrm instance is closed" : "MediaDrm has no plugin");
    }

    public void clearOnEventListener()
    {
    }

    public void clearOnExpirationUpdateListener()
    {
    }

    public void clearOnKeyStatusChangeListener()
    {
    }

    public void clearOnSessionLostStateListener()
    {
    }

    public void close()
    {
        mClosed = true;
    }

    public void closeSession(byte[] sessionId)
    {
        throw unavailable();
    }

    public int getConnectedHdcpLevel()
    {
        return HDCP_LEVEL_UNKNOWN;
    }

    public MediaDrm.CryptoSession getCryptoSession(byte[] sessionId, String cipherAlgorithm, String macAlgorithm)
    {
        throw unavailable();
    }

    public MediaDrm.KeyRequest getKeyRequest(byte[] scope, byte[] init, String mimeType, int keyType,
        HashMap<String, String> optionalParameters) throws NotProvisionedException
    {
        throw unavailable();
    }

    public List<MediaDrm.LogMessage> getLogMessages()
    {
        return Collections.emptyList();
    }

    public int getMaxHdcpLevel()
    {
        return HDCP_LEVEL_UNKNOWN;
    }

    public static int getMaxSecurityLevel()
    {
        return SECURITY_LEVEL_HW_SECURE_ALL;
    }

    public int getMaxSessionCount()
    {
        return 0;
    }

    public PersistableBundle getMetrics()
    {
        return new PersistableBundle();
    }

    public List<byte[]> getOfflineLicenseKeySetIds()
    {
        return new ArrayList<>();
    }

    public int getOfflineLicenseState(byte[] keySetId)
    {
        return OFFLINE_LICENSE_STATE_UNKNOWN;
    }

    public int getOpenSessionCount()
    {
        return 0;
    }

    public MediaDrm.PlaybackComponent getPlaybackComponent(byte[] sessionId)
    {
        throw unavailable();
    }

    public byte[] getPropertyByteArray(String propertyName)
    {
        throw unavailable();
    }

    public String getPropertyString(String propertyName)
    {
        throw unavailable();
    }

    public MediaDrm.ProvisionRequest getProvisionRequest()
    {
        throw unavailable();
    }

    public byte[] getSecureStop(byte[] ssid)
    {
        throw unavailable();
    }

    public List<byte[]> getSecureStopIds()
    {
        return new ArrayList<>();
    }

    public List<byte[]> getSecureStops()
    {
        return new ArrayList<>();
    }

    public int getSecurityLevel(byte[] sessionId)
    {
        throw unavailable();
    }

    public static List<UUID> getSupportedCryptoSchemes()
    {
        return new ArrayList<>();
    }

    public static boolean isCryptoSchemeSupported(UUID uuid)
    {
        return false;
    }

    public static boolean isCryptoSchemeSupported(UUID uuid, String mimeType)
    {
        return false;
    }

    public static boolean isCryptoSchemeSupported(UUID uuid, String mimeType, int securityLevel)
    {
        return false;
    }

    public byte[] openSession() throws NotProvisionedException, ResourceBusyException
    {
        throw unavailable();
    }

    public byte[] openSession(int level) throws NotProvisionedException, ResourceBusyException
    {
        throw unavailable();
    }

    public byte[] provideKeyResponse(byte[] scope, byte[] response) throws DeniedByServerException, NotProvisionedException
    {
        throw unavailable();
    }

    public void provideProvisionResponse(byte[] response) throws DeniedByServerException
    {
        throw unavailable();
    }

    public HashMap<String, String> queryKeyStatus(byte[] sessionId)
    {
        throw unavailable();
    }

    public void release()
    {
        close();
    }

    public void releaseAllSecureStops()
    {
    }

    public void releaseSecureStops(byte[] ssRelease)
    {
    }

    public void removeAllSecureStops()
    {
    }

    public void removeKeys(byte[] sessionId)
    {
        throw unavailable();
    }

    public void removeOfflineLicense(byte[] keySetId)
    {
        throw unavailable();
    }

    public void removeSecureStop(byte[] ssid)
    {
    }

    public boolean requiresSecureDecoder(String mime)
    {
        return false;
    }

    public boolean requiresSecureDecoder(String mime, int level)
    {
        return false;
    }

    public void restoreKeys(byte[] sessionId, byte[] keySetId)
    {
        throw unavailable();
    }

    public void setOnEventListener(MediaDrm.OnEventListener listener)
    {
    }

    public void setOnEventListener(MediaDrm.OnEventListener listener, Handler handler)
    {
    }

    public void setOnEventListener(Executor executor, MediaDrm.OnEventListener listener)
    {
    }

    public void setOnExpirationUpdateListener(MediaDrm.OnExpirationUpdateListener listener, Handler handler)
    {
    }

    public void setOnExpirationUpdateListener(Executor executor, MediaDrm.OnExpirationUpdateListener listener)
    {
    }

    public void setOnKeyStatusChangeListener(MediaDrm.OnKeyStatusChangeListener listener, Handler handler)
    {
    }

    public void setOnKeyStatusChangeListener(Executor executor, MediaDrm.OnKeyStatusChangeListener listener)
    {
    }

    public void setOnSessionLostStateListener(MediaDrm.OnSessionLostStateListener listener, Handler handler)
    {
    }

    public void setOnSessionLostStateListener(Executor executor, MediaDrm.OnSessionLostStateListener listener)
    {
    }

    public void setPropertyByteArray(String propertyName, byte[] value)
    {
        throw unavailable();
    }

    public void setPropertyString(String propertyName, String value)
    {
        throw unavailable();
    }

    public static final class SessionException extends RuntimeException implements MediaDrmThrowable
    {
        public static final int ERROR_RESOURCE_CONTENTION = 1;
        public static final int ERROR_UNKNOWN = 0;

        private final int mErrorCode;

        public SessionException(int errorCode, String detailMessage)
        {
            super(detailMessage);
            mErrorCode = errorCode;
        }

        public int getErrorCode()
        {
            return mErrorCode;
        }

        public int getErrorContext()
        {
            return 0;
        }

        public int getOemError()
        {
            return 0;
        }

        public int getVendorError()
        {
            return 0;
        }

        public boolean isTransient()
        {
            return mErrorCode == ERROR_RESOURCE_CONTENTION;
        }
    }

    public static final class ProvisionRequest
    {
        private final byte[] mData;
        private final String mDefaultUrl;

        ProvisionRequest(byte[] data, String defaultUrl)
        {
            mData = data;
            mDefaultUrl = defaultUrl;
        }

        public byte[] getData()
        {
            return mData;
        }

        public String getDefaultUrl()
        {
            return mDefaultUrl;
        }
    }

    public final class PlaybackComponent
    {
        private final byte[] mSessionId;
        private LogSessionId mLogSessionId = LogSessionId.LOG_SESSION_ID_NONE;

        PlaybackComponent(byte[] sessionId)
        {
            mSessionId = sessionId;
        }

        public LogSessionId getLogSessionId()
        {
            return mLogSessionId;
        }

        public void setLogSessionId(LogSessionId logSessionId)
        {
            if (logSessionId == null) throw new NullPointerException("logSessionId must not be null");

            mLogSessionId = logSessionId;
        }
    }

    public interface OnSessionLostStateListener
    {
        void onSessionLostState(MediaDrm md, byte[] sessionId);
    }

    public interface OnKeyStatusChangeListener
    {
        void onKeyStatusChange(MediaDrm md, byte[] sessionId, List<MediaDrm.KeyStatus> keyInformation, boolean hasNewUsableKey);
    }

    public interface OnExpirationUpdateListener
    {
        void onExpirationUpdate(MediaDrm md, byte[] sessionId, long expirationTime);
    }

    public interface OnEventListener
    {
        void onEvent(MediaDrm md, byte[] sessionId, int event, int extra, byte[] data);
    }

    public static final class MetricsConstants
    {
        public static final String CLOSE_SESSION_ERROR_COUNT = "drm.mediadrm.close_session.error.count";
        public static final String CLOSE_SESSION_ERROR_LIST = "drm.mediadrm.close_session.error.list";
        public static final String CLOSE_SESSION_OK_COUNT = "drm.mediadrm.close_session.ok.count";
        public static final String EVENT_KEY_EXPIRED_COUNT = "drm.mediadrm.event.KEY_EXPIRED.count";
        public static final String EVENT_KEY_NEEDED_COUNT = "drm.mediadrm.event.KEY_NEEDED.count";
        public static final String EVENT_PROVISION_REQUIRED_COUNT = "drm.mediadrm.event.PROVISION_REQUIRED.count";
        public static final String EVENT_SESSION_RECLAIMED_COUNT = "drm.mediadrm.event.SESSION_RECLAIMED.count";
        public static final String EVENT_VENDOR_DEFINED_COUNT = "drm.mediadrm.event.VENDOR_DEFINED.count";
        public static final String GET_DEVICE_UNIQUE_ID_ERROR_COUNT = "drm.mediadrm.get_device_unique_id.error.count";
        public static final String GET_DEVICE_UNIQUE_ID_ERROR_LIST = "drm.mediadrm.get_device_unique_id.error.list";
        public static final String GET_DEVICE_UNIQUE_ID_OK_COUNT = "drm.mediadrm.get_device_unique_id.ok.count";
        public static final String GET_KEY_REQUEST_ERROR_COUNT = "drm.mediadrm.get_key_request.error.count";
        public static final String GET_KEY_REQUEST_ERROR_LIST = "drm.mediadrm.get_key_request.error.list";
        public static final String GET_KEY_REQUEST_OK_COUNT = "drm.mediadrm.get_key_request.ok.count";
        public static final String GET_KEY_REQUEST_OK_TIME_MICROS = "drm.mediadrm.get_key_request.ok.average_time_micros";
        public static final String GET_PROVISION_REQUEST_ERROR_COUNT = "drm.mediadrm.get_provision_request.error.count";
        public static final String GET_PROVISION_REQUEST_ERROR_LIST = "drm.mediadrm.get_provision_request.error.list";
        public static final String GET_PROVISION_REQUEST_OK_COUNT = "drm.mediadrm.get_provision_request.ok.count";
        public static final String KEY_STATUS_EXPIRED_COUNT = "drm.mediadrm.key_status.EXPIRED.count";
        public static final String KEY_STATUS_INTERNAL_ERROR_COUNT = "drm.mediadrm.key_status.INTERNAL_ERROR.count";
        public static final String KEY_STATUS_OUTPUT_NOT_ALLOWED_COUNT = "drm.mediadrm.key_status_change.OUTPUT_NOT_ALLOWED.count";
        public static final String KEY_STATUS_PENDING_COUNT = "drm.mediadrm.key_status_change.PENDING.count";
        public static final String KEY_STATUS_USABLE_COUNT = "drm.mediadrm.key_status_change.USABLE.count";
        public static final String OPEN_SESSION_ERROR_COUNT = "drm.mediadrm.open_session.error.count";
        public static final String OPEN_SESSION_ERROR_LIST = "drm.mediadrm.open_session.error.list";
        public static final String OPEN_SESSION_OK_COUNT = "drm.mediadrm.open_session.ok.count";
        public static final String PROVIDE_KEY_RESPONSE_ERROR_COUNT = "drm.mediadrm.provide_key_response.error.count";
        public static final String PROVIDE_KEY_RESPONSE_ERROR_LIST = "drm.mediadrm.provide_key_response.error.list";
        public static final String PROVIDE_KEY_RESPONSE_OK_COUNT = "drm.mediadrm.provide_key_response.ok.count";
        public static final String PROVIDE_KEY_RESPONSE_OK_TIME_MICROS = "drm.mediadrm.provide_key_response.ok.average_time_micros";
        public static final String PROVIDE_PROVISION_RESPONSE_ERROR_COUNT = "drm.mediadrm.provide_provision_response.error.count";
        public static final String PROVIDE_PROVISION_RESPONSE_ERROR_LIST = "drm.mediadrm.provide_provision_response.error.list";
        public static final String PROVIDE_PROVISION_RESPONSE_OK_COUNT = "drm.mediadrm.provide_provision_response.ok.count";
        public static final String SESSION_END_TIMES_MS = "drm.mediadrm.session_end_times_ms";
        public static final String SESSION_START_TIMES_MS = "drm.mediadrm.session_start_times_ms";

        MetricsConstants()
        {
        }
    }

    public static final class MediaDrmStateException extends IllegalStateException implements MediaDrmThrowable
    {
        private final int mErrorCode;
        private final String mDiagnosticInfo;

        MediaDrmStateException(int errorCode, String detailMessage)
        {
            super(detailMessage);
            mErrorCode = errorCode;
            mDiagnosticInfo = "android.media.MediaDrm.error_" + (errorCode < 0 ? "neg_" : "") + Math.abs(errorCode);
        }

        public String getDiagnosticInfo()
        {
            return mDiagnosticInfo;
        }

        public int getErrorCode()
        {
            return mErrorCode;
        }

        public int getErrorContext()
        {
            return 0;
        }

        public int getOemError()
        {
            return 0;
        }

        public int getVendorError()
        {
            return 0;
        }

        public boolean isTransient()
        {
            return mErrorCode == ErrorCodes.ERROR_SESSION_NOT_OPENED || mErrorCode == ErrorCodes.ERROR_RESOURCE_CONTENTION;
        }
    }

    public static final class LogMessage
    {
        private final long mTimestampMillis;
        private final int mPriority;
        private final String mMessage;

        LogMessage(long timestampMillis, int priority, String message)
        {
            mTimestampMillis = timestampMillis;
            mPriority = priority;
            mMessage = message;
        }

        public String getMessage()
        {
            return mMessage;
        }

        public int getPriority()
        {
            return mPriority;
        }

        public long getTimestampMillis()
        {
            return mTimestampMillis;
        }

        @Override
        public String toString()
        {
            return "LogMessage{timestampMillis=" + mTimestampMillis + ", priority=" + mPriority + ", message='" + mMessage + "'}";
        }
    }

    public static final class KeyStatus
    {
        public static final int STATUS_EXPIRED = 1;
        public static final int STATUS_INTERNAL_ERROR = 4;
        public static final int STATUS_OUTPUT_NOT_ALLOWED = 2;
        public static final int STATUS_PENDING = 3;
        public static final int STATUS_USABLE = 0;
        public static final int STATUS_USABLE_IN_FUTURE = 5;

        private final byte[] mKeyId;
        private final int mStatusCode;

        KeyStatus(byte[] keyId, int statusCode)
        {
            mKeyId = keyId;
            mStatusCode = statusCode;
        }

        public byte[] getKeyId()
        {
            return mKeyId;
        }

        public int getStatusCode()
        {
            return mStatusCode;
        }
    }

    public static final class KeyRequest
    {
        public static final int REQUEST_TYPE_INITIAL = 0;
        public static final int REQUEST_TYPE_NONE = 3;
        public static final int REQUEST_TYPE_RELEASE = 2;
        public static final int REQUEST_TYPE_RENEWAL = 1;
        public static final int REQUEST_TYPE_UPDATE = 4;

        private final byte[] mData;
        private final String mDefaultUrl;
        private final int mRequestType;

        KeyRequest(byte[] data, String defaultUrl, int requestType)
        {
            mData = data;
            mDefaultUrl = defaultUrl;
            mRequestType = requestType;
        }

        public byte[] getData()
        {
            if (mData == null) throw new RuntimeException("KeyRequest is not initialized");

            return mData;
        }

        public String getDefaultUrl()
        {
            if (mDefaultUrl == null) throw new RuntimeException("KeyRequest is not initialized");

            return mDefaultUrl;
        }

        public int getRequestType()
        {
            return mRequestType;
        }
    }

    public static final class ErrorCodes
    {
        public static final int ERROR_CERTIFICATE_MALFORMED = 10;
        public static final int ERROR_CERTIFICATE_MISSING = 11;
        public static final int ERROR_CRYPTO_LIBRARY = 12;
        public static final int ERROR_FRAME_TOO_LARGE = 8;
        public static final int ERROR_GENERIC_OEM = 13;
        public static final int ERROR_GENERIC_PLUGIN = 14;
        public static final int ERROR_INIT_DATA = 15;
        public static final int ERROR_INSUFFICIENT_OUTPUT_PROTECTION = 4;
        public static final int ERROR_INSUFFICIENT_SECURITY = 7;
        public static final int ERROR_KEY_EXPIRED = 2;
        public static final int ERROR_KEY_NOT_LOADED = 16;
        public static final int ERROR_LICENSE_PARSE = 17;
        public static final int ERROR_LICENSE_POLICY = 18;
        public static final int ERROR_LICENSE_RELEASE = 19;
        public static final int ERROR_LICENSE_REQUEST_REJECTED = 20;
        public static final int ERROR_LICENSE_RESTORE = 21;
        public static final int ERROR_LICENSE_STATE = 22;
        public static final int ERROR_LOST_STATE = 9;
        public static final int ERROR_MEDIA_FRAMEWORK = 23;
        public static final int ERROR_NO_KEY = 1;
        public static final int ERROR_PROVISIONING_CERTIFICATE = 24;
        public static final int ERROR_PROVISIONING_CONFIG = 25;
        public static final int ERROR_PROVISIONING_PARSE = 26;
        public static final int ERROR_PROVISIONING_REQUEST_REJECTED = 27;
        public static final int ERROR_PROVISIONING_RETRY = 28;
        public static final int ERROR_RESOURCE_BUSY = 3;
        public static final int ERROR_RESOURCE_CONTENTION = 29;
        public static final int ERROR_SECURE_STOP_RELEASE = 30;
        public static final int ERROR_SESSION_NOT_OPENED = 5;
        public static final int ERROR_STORAGE_READ = 31;
        public static final int ERROR_STORAGE_WRITE = 32;
        public static final int ERROR_UNKNOWN = 0;
        public static final int ERROR_UNSUPPORTED_OPERATION = 6;
        public static final int ERROR_ZERO_SUBSAMPLES = 33;

        ErrorCodes()
        {
        }
    }

    public final class CryptoSession
    {
        private final byte[] mSessionId;

        CryptoSession(byte[] sessionId)
        {
            mSessionId = Arrays.copyOf(sessionId, sessionId.length);
        }

        public byte[] decrypt(byte[] keyId, byte[] input, byte[] iv)
        {
            throw unavailable();
        }

        public byte[] encrypt(byte[] keyId, byte[] input, byte[] iv)
        {
            throw unavailable();
        }

        public byte[] sign(byte[] keyId, byte[] message)
        {
            throw unavailable();
        }

        public boolean verify(byte[] keyId, byte[] message, byte[] signature)
        {
            throw unavailable();
        }
    }
}
