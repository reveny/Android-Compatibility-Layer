package android.telecom;

import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.view.Surface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class Connection extends Conferenceable
{
    public static final int STATE_INITIALIZING = 0;
    public static final int STATE_NEW = 1;
    public static final int STATE_RINGING = 2;
    public static final int STATE_DIALING = 3;
    public static final int STATE_ACTIVE = 4;
    public static final int STATE_HOLDING = 5;
    public static final int STATE_DISCONNECTED = 6;
    public static final int STATE_PULLING_CALL = 7;

    public static final int CAPABILITY_HOLD = 0x00000001;
    public static final int CAPABILITY_SUPPORT_HOLD = 0x00000002;
    public static final int CAPABILITY_MERGE_CONFERENCE = 0x00000004;
    public static final int CAPABILITY_SWAP_CONFERENCE = 0x00000008;
    public static final int CAPABILITY_RESPOND_VIA_TEXT = 0x00000020;
    public static final int CAPABILITY_MUTE = 0x00000040;
    public static final int CAPABILITY_MANAGE_CONFERENCE = 0x00000080;
    public static final int CAPABILITY_SUPPORTS_VT_LOCAL_RX = 0x00000100;
    public static final int CAPABILITY_SUPPORTS_VT_LOCAL_TX = 0x00000200;
    public static final int CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL = CAPABILITY_SUPPORTS_VT_LOCAL_RX | CAPABILITY_SUPPORTS_VT_LOCAL_TX;
    public static final int CAPABILITY_SUPPORTS_VT_REMOTE_RX = 0x00000400;
    public static final int CAPABILITY_SUPPORTS_VT_REMOTE_TX = 0x00000800;
    public static final int CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL = CAPABILITY_SUPPORTS_VT_REMOTE_RX | CAPABILITY_SUPPORTS_VT_REMOTE_TX;
    public static final int CAPABILITY_SEPARATE_FROM_CONFERENCE = 0x00001000;
    public static final int CAPABILITY_DISCONNECT_FROM_CONFERENCE = 0x00002000;
    public static final int CAPABILITY_CAN_UPGRADE_TO_VIDEO = 0x00080000;
    public static final int CAPABILITY_CAN_PAUSE_VIDEO = 0x00100000;
    public static final int CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION = 0x00400000;
    public static final int CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO = 0x00800000;
    public static final int CAPABILITY_CAN_PULL_CALL = 0x01000000;
    public static final int CAPABILITY_SUPPORT_DEFLECT = 0x02000000;
    public static final int CAPABILITY_ADD_PARTICIPANT = 0x04000000;
    public static final int CAPABILITY_REMOTE_PARTY_SUPPORTS_RTT = 0x20000000;

    public static final int PROPERTY_HIGH_DEF_AUDIO = 1 << 2;
    public static final int PROPERTY_WIFI = 1 << 3;
    public static final int PROPERTY_IS_EXTERNAL_CALL = 1 << 4;
    public static final int PROPERTY_HAS_CDMA_VOICE_PRIVACY = 1 << 5;
    public static final int PROPERTY_SELF_MANAGED = 1 << 7;
    public static final int PROPERTY_IS_RTT = 1 << 8;
    public static final int PROPERTY_ASSISTED_DIALING = 1 << 9;
    public static final int PROPERTY_NETWORK_IDENTIFIED_EMERGENCY_CALL = 1 << 10;
    public static final int PROPERTY_IS_ADHOC_CONFERENCE = 1 << 12;
    public static final int PROPERTY_CROSS_SIM = 1 << 13;

    public static final int AUDIO_CODEC_NONE = 0;
    public static final int AUDIO_CODEC_AMR = 1;
    public static final int AUDIO_CODEC_AMR_WB = 2;
    public static final int AUDIO_CODEC_QCELP13K = 3;
    public static final int AUDIO_CODEC_EVRC = 4;
    public static final int AUDIO_CODEC_EVRC_B = 5;
    public static final int AUDIO_CODEC_EVRC_WB = 6;
    public static final int AUDIO_CODEC_EVRC_NW = 7;
    public static final int AUDIO_CODEC_GSM_EFR = 8;
    public static final int AUDIO_CODEC_GSM_FR = 9;
    public static final int AUDIO_CODEC_GSM_HR = 10;
    public static final int AUDIO_CODEC_G711U = 11;
    public static final int AUDIO_CODEC_G723 = 12;
    public static final int AUDIO_CODEC_G711A = 13;
    public static final int AUDIO_CODEC_G722 = 14;
    public static final int AUDIO_CODEC_G711AB = 15;
    public static final int AUDIO_CODEC_G729 = 16;
    public static final int AUDIO_CODEC_EVS_NB = 17;
    public static final int AUDIO_CODEC_EVS_WB = 18;
    public static final int AUDIO_CODEC_EVS_SWB = 19;
    public static final int AUDIO_CODEC_EVS_FB = 20;

    public static final int VERIFICATION_STATUS_NOT_VERIFIED = 0;
    public static final int VERIFICATION_STATUS_PASSED = 1;
    public static final int VERIFICATION_STATUS_FAILED = 2;

    public static final String EVENT_CALL_HOLD_FAILED = "android.telecom.event.CALL_HOLD_FAILED";
    public static final String EVENT_CALL_MERGE_FAILED = "android.telecom.event.CALL_MERGE_FAILED";
    public static final String EVENT_CALL_PULL_FAILED = "android.telecom.event.CALL_PULL_FAILED";
    public static final String EVENT_CALL_REMOTELY_HELD = "android.telecom.event.CALL_REMOTELY_HELD";
    public static final String EVENT_CALL_REMOTELY_UNHELD = "android.telecom.event.CALL_REMOTELY_UNHELD";
    public static final String EVENT_CALL_SWITCH_FAILED = "android.telecom.event.CALL_SWITCH_FAILED";
    public static final String EVENT_MERGE_COMPLETE = "android.telecom.event.MERGE_COMPLETE";
    public static final String EVENT_MERGE_START = "android.telecom.event.MERGE_START";
    public static final String EVENT_ON_HOLD_TONE_END = "android.telecom.event.ON_HOLD_TONE_END";
    public static final String EVENT_ON_HOLD_TONE_START = "android.telecom.event.ON_HOLD_TONE_START";
    public static final String EVENT_RTT_AUDIO_INDICATION_CHANGED = "android.telecom.event.RTT_AUDIO_INDICATION_CHANGED";

    public static final String EXTRA_ANSWERING_DROPS_FG_CALL = "android.telecom.extra.ANSWERING_DROPS_FG_CALL";
    public static final String EXTRA_ANSWERING_DROPS_FG_CALL_APP_NAME = "android.telecom.extra.ANSWERING_DROPS_FG_CALL_APP_NAME";
    public static final String EXTRA_AUDIO_CODEC = "android.telecom.extra.AUDIO_CODEC";
    public static final String EXTRA_AUDIO_CODEC_BANDWIDTH_KHZ = "android.telecom.extra.AUDIO_CODEC_BANDWIDTH_KHZ";
    public static final String EXTRA_AUDIO_CODEC_BITRATE_KBPS = "android.telecom.extra.AUDIO_CODEC_BITRATE_KBPS";
    public static final String EXTRA_CALL_SUBJECT = "android.telecom.extra.CALL_SUBJECT";
    public static final String EXTRA_CHILD_ADDRESS = "android.telecom.extra.CHILD_ADDRESS";
    public static final String EXTRA_IS_RTT_AUDIO_PRESENT = "android.telecom.extra.IS_RTT_AUDIO_PRESENT";
    public static final String EXTRA_LAST_FORWARDED_NUMBER = "android.telecom.extra.LAST_FORWARDED_NUMBER";
    public static final String EXTRA_LAST_KNOWN_CELL_IDENTITY = "android.telecom.extra.LAST_KNOWN_CELL_IDENTITY";
    public static final String EXTRA_SIP_INVITE = "android.telecom.extra.SIP_INVITE";

    private int mState = STATE_NEW;
    private Uri mAddress;
    private int mAddressPresentation;
    private String mCallerDisplayName;
    private int mCallerDisplayNamePresentation;
    private boolean mRingbackRequested;
    private int mConnectionCapabilities;
    private int mConnectionProperties;
    private VideoProvider mVideoProvider;
    private boolean mAudioModeIsVoip;
    private StatusHints mStatusHints;
    private int mVideoState;
    private DisconnectCause mDisconnectCause;
    private Conference mConference;
    private final List<Conferenceable> mConferenceables = new ArrayList<>();
    private Bundle mExtras;
    private CallAudioState mCallAudioState;
    private CallEndpoint mCallEndpoint;
    private int mCallerNumberVerificationStatus;
    private String mPostDialWait;
    private char mNextPostDialChar;

    public Connection()
    {
    }

    public static String stateToString(int state)
    {
        switch (state)
        {
            case STATE_INITIALIZING: return "INITIALIZING";
            case STATE_NEW: return "NEW";
            case STATE_RINGING: return "RINGING";
            case STATE_DIALING: return "DIALING";
            case STATE_PULLING_CALL: return "PULLING_CALL";
            case STATE_ACTIVE: return "ACTIVE";
            case STATE_HOLDING: return "HOLDING";
            case STATE_DISCONNECTED: return "DISCONNECTED";
            default: return "UNKNOWN";
        }
    }

    public static String capabilitiesToString(int capabilities)
    {
        String[] names = {"CAPABILITY_HOLD", "CAPABILITY_SUPPORT_HOLD", "CAPABILITY_MERGE_CONFERENCE", "CAPABILITY_SWAP_CONFERENCE", null,
            "CAPABILITY_RESPOND_VIA_TEXT", "CAPABILITY_MUTE", "CAPABILITY_MANAGE_CONFERENCE", "CAPABILITY_SUPPORTS_VT_LOCAL_RX",
            "CAPABILITY_SUPPORTS_VT_LOCAL_TX", "CAPABILITY_SUPPORTS_VT_REMOTE_RX", "CAPABILITY_SUPPORTS_VT_REMOTE_TX",
            "CAPABILITY_SEPARATE_FROM_CONFERENCE", "CAPABILITY_DISCONNECT_FROM_CONFERENCE", null, null, null, null, null,
            "CAPABILITY_CAN_UPGRADE_TO_VIDEO", "CAPABILITY_CAN_PAUSE_VIDEO", null, "CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION",
            "CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO", "CAPABILITY_CAN_PULL_CALL", "CAPABILITY_SUPPORT_DEFLECT",
            "CAPABILITY_ADD_PARTICIPANT", null, null, "CAPABILITY_REMOTE_PARTY_SUPPORTS_RTT"};
        return "[Capabilities:" + flagsToString(capabilities, names) + "]";
    }

    public static String propertiesToString(int properties)
    {
        String[] names = {null, null, "PROPERTY_HIGH_DEF_AUDIO", "PROPERTY_WIFI", "PROPERTY_IS_EXTERNAL_CALL",
            "PROPERTY_HAS_CDMA_VOICE_PRIVACY", null, "PROPERTY_SELF_MANAGED", "PROPERTY_IS_RTT", "PROPERTY_ASSISTED_DIALING",
            "PROPERTY_NETWORK_IDENTIFIED_EMERGENCY_CALL", null, "PROPERTY_IS_ADHOC_CONFERENCE", "PROPERTY_CROSS_SIM"};
        return "[Properties:" + flagsToString(properties, names) + "]";
    }

    private static String flagsToString(int flags, String[] names)
    {
        StringBuilder builder = new StringBuilder();
        for (int bit = 0; bit < names.length; bit++)
        {
            if (names[bit] != null && (flags & (1 << bit)) != 0) builder.append(' ').append(names[bit]);
        }
        return builder.toString();
    }

    public static Connection createFailedConnection(DisconnectCause cause)
    {
        return new FailureSignalingConnection(cause);
    }

    public static Connection createCanceledConnection()
    {
        return new FailureSignalingConnection(new DisconnectCause(DisconnectCause.CANCELED));
    }

    public final Uri getAddress()
    {
        return mAddress;
    }

    public final int getAddressPresentation()
    {
        return mAddressPresentation;
    }

    public final String getCallerDisplayName()
    {
        return mCallerDisplayName;
    }

    public final int getCallerDisplayNamePresentation()
    {
        return mCallerDisplayNamePresentation;
    }

    public final int getState()
    {
        return mState;
    }

    public final int getVideoState()
    {
        return mVideoState;
    }

    @Deprecated
    public final CallAudioState getCallAudioState()
    {
        return mCallAudioState;
    }

    public final CallEndpoint getCurrentCallEndpoint()
    {
        return mCallEndpoint;
    }

    public final Conference getConference()
    {
        return mConference;
    }

    public final boolean isRingbackRequested()
    {
        return mRingbackRequested;
    }

    public final boolean getAudioModeIsVoip()
    {
        return mAudioModeIsVoip;
    }

    public final StatusHints getStatusHints()
    {
        return mStatusHints;
    }

    public final Bundle getExtras()
    {
        return mExtras == null ? null : new Bundle(mExtras);
    }

    public final int getConnectionCapabilities()
    {
        return mConnectionCapabilities;
    }

    public final int getConnectionProperties()
    {
        return mConnectionProperties;
    }

    public final int getCallerNumberVerificationStatus()
    {
        return mCallerNumberVerificationStatus;
    }

    public final void setCallerNumberVerificationStatus(int callerNumberVerificationStatus)
    {
        mCallerNumberVerificationStatus = callerNumberVerificationStatus;
    }

    public final void setAddress(Uri address, int presentation)
    {
        mAddress = address;
        mAddressPresentation = presentation;
    }

    public final void setCallerDisplayName(String callerDisplayName, int presentation)
    {
        mCallerDisplayName = callerDisplayName;
        mCallerDisplayNamePresentation = presentation;
    }

    public final void setVideoState(int videoState)
    {
        mVideoState = videoState;
    }

    public final void setActive()
    {
        setRingbackRequested(false);
        setState(STATE_ACTIVE);
    }

    public final void setRinging()
    {
        setState(STATE_RINGING);
    }

    public final void setInitializing()
    {
        setState(STATE_INITIALIZING);
    }

    public final void setInitialized()
    {
        setState(STATE_NEW);
    }

    public final void setDialing()
    {
        setState(STATE_DIALING);
    }

    public final void setPulling()
    {
        setState(STATE_PULLING_CALL);
    }

    public final void setOnHold()
    {
        setState(STATE_HOLDING);
    }

    public final void setVideoProvider(VideoProvider videoProvider)
    {
        mVideoProvider = videoProvider;
    }

    public final VideoProvider getVideoProvider()
    {
        return mVideoProvider;
    }

    public final void setDisconnected(DisconnectCause disconnectCause)
    {
        mDisconnectCause = disconnectCause;
        setState(STATE_DISCONNECTED);
    }

    public final void setPostDialWait(String remaining)
    {
        mPostDialWait = remaining;
    }

    public final void setNextPostDialChar(char nextChar)
    {
        mNextPostDialChar = nextChar;
    }

    public final void setRingbackRequested(boolean ringback)
    {
        mRingbackRequested = ringback;
    }

    public final void setConnectionCapabilities(int connectionCapabilities)
    {
        mConnectionCapabilities = connectionCapabilities;
    }

    public final void setConnectionProperties(int connectionProperties)
    {
        mConnectionProperties = connectionProperties;
    }

    public final void destroy()
    {
        resetConference();
        mConferenceables.clear();
    }

    public final void setAudioModeIsVoip(boolean isVoip)
    {
        mAudioModeIsVoip = isVoip;
    }

    public final void setStatusHints(StatusHints statusHints)
    {
        mStatusHints = statusHints;
    }

    public final void setConferenceableConnections(List<Connection> conferenceableConnections)
    {
        mConferenceables.clear();
        if (conferenceableConnections == null) return;
        for (Connection connection : conferenceableConnections)
        {
            if (!mConferenceables.contains(connection)) mConferenceables.add(connection);
        }
    }

    public final void setConferenceables(List<Conferenceable> conferenceables)
    {
        mConferenceables.clear();
        if (conferenceables == null) return;
        for (Conferenceable conferenceable : conferenceables)
        {
            if (!mConferenceables.contains(conferenceable)) mConferenceables.add(conferenceable);
        }
    }

    public final List<Conferenceable> getConferenceables()
    {
        return Collections.unmodifiableList(mConferenceables);
    }

    public final void notifyConferenceMergeFailed()
    {
        sendConnectionEvent(EVENT_CALL_MERGE_FAILED, null);
    }

    public final void setExtras(Bundle extras)
    {
        mExtras = extras == null ? null : new Bundle(extras);
        onExtrasChanged(getExtras());
    }

    public final void putExtras(Bundle extras)
    {
        if (extras == null) return;
        if (mExtras == null) mExtras = new Bundle();
        mExtras.putAll(extras);
        onExtrasChanged(getExtras());
    }

    public final void removeExtras(List<String> keys)
    {
        if (keys == null || mExtras == null) return;
        for (String key : keys) mExtras.remove(key);
        onExtrasChanged(getExtras());
    }

    public final void removeExtras(String... keys)
    {
        if (keys == null) return;
        List<String> list = new ArrayList<>();
        Collections.addAll(list, keys);
        removeExtras(list);
    }

    @Deprecated
    public final void setAudioRoute(int route)
    {
        if (mCallAudioState == null) return;
        mCallAudioState = new CallAudioState(mCallAudioState.isMuted(), route, mCallAudioState.getSupportedRouteMask());
    }

    @Deprecated
    public void requestBluetoothAudio(BluetoothDevice bluetoothDevice)
    {
    }

    public final void requestCallEndpointChange(CallEndpoint endpoint, Executor executor,
        OutcomeReceiver<Void, CallEndpointException> callback)
    {
        if (executor == null || callback == null) return;
        executor.execute(() -> callback.onError(new CallEndpointException("Requested CallEndpoint does not exist",
            CallEndpointException.ERROR_ENDPOINT_DOES_NOT_EXIST)));
    }

    public final void queryLocationForEmergency(long timeoutMillis, String provider, Executor executor,
        OutcomeReceiver<Location, QueryLocationException> callback)
    {
        if (executor == null || callback == null) return;
        executor.execute(() -> callback.onError(new QueryLocationException("Location query is only allowed for emergency calls",
            QueryLocationException.ERROR_NOT_ALLOWED_FOR_NON_EMERGENCY_CONNECTIONS)));
    }

    public final void sendRttInitiationSuccess()
    {
    }

    public final void sendRttInitiationFailure(int reason)
    {
    }

    public final void sendRttSessionRemotelyTerminated()
    {
    }

    public final void sendRemoteRttRequest()
    {
    }

    public final DisconnectCause getDisconnectCause()
    {
        return mDisconnectCause;
    }

    @Deprecated
    public void onCallAudioStateChanged(CallAudioState state)
    {
    }

    public void onCallEndpointChanged(CallEndpoint callEndpoint)
    {
    }

    public void onAvailableCallEndpointsChanged(List<CallEndpoint> availableEndpoints)
    {
    }

    public void onMuteStateChanged(boolean isMuted)
    {
    }

    public void onUsingAlternativeUi(boolean isUsingAlternativeUi)
    {
    }

    public void onTrackedByNonUiService(boolean isTracked)
    {
    }

    public void onStateChanged(int state)
    {
    }

    public void onPlayDtmfTone(char c)
    {
    }

    public void onStopDtmfTone()
    {
    }

    public void onDisconnect()
    {
    }

    public void onSeparate()
    {
    }

    public void onAddConferenceParticipants(List<Uri> participants)
    {
    }

    public void onAbort()
    {
    }

    public void onHold()
    {
    }

    public void onUnhold()
    {
    }

    public void onAnswer(int videoState)
    {
        onAnswer();
    }

    public void onAnswer()
    {
    }

    public void onDeflect(Uri address)
    {
    }

    public void onReject()
    {
    }

    public void onReject(int rejectReason)
    {
        onReject();
    }

    public void onReject(String replyMessage)
    {
        onReject();
    }

    public void onSilence()
    {
    }

    public void onPostDialContinue(boolean proceed)
    {
    }

    public void onPullExternalCall()
    {
    }

    public void onCallEvent(String event, Bundle extras)
    {
    }

    public void onHandoverComplete()
    {
    }

    public void onExtrasChanged(Bundle extras)
    {
    }

    public void onShowIncomingCallUi()
    {
    }

    public void onStartRtt(RttTextStream rttTextStream)
    {
    }

    public void onStopRtt()
    {
    }

    public void handleRttUpgradeResponse(RttTextStream rttTextStream)
    {
    }

    public void sendConnectionEvent(String event, Bundle extras)
    {
    }

    private void setState(int state)
    {
        if (mState == STATE_DISCONNECTED && mState != state) return;
        if (mState != state)
        {
            mState = state;
            onStateChanged(state);
        }
    }

    boolean setConference(Conference conference)
    {
        if (mConference != null) return false;
        mConference = conference;
        return true;
    }

    void resetConference()
    {
        mConference = null;
    }

    private static final class FailureSignalingConnection extends Connection
    {
        FailureSignalingConnection(DisconnectCause disconnectCause)
        {
            setDisconnected(disconnectCause);
        }
    }

    public static final class RttTextStream
    {
        RttTextStream()
        {
        }

        public void write(String input) throws IOException
        {
        }

        public String read() throws IOException
        {
            return null;
        }

        public String readImmediately() throws IOException
        {
            return null;
        }
    }

    public static final class RttModifyStatus
    {
        public static final int SESSION_MODIFY_REQUEST_SUCCESS = 1;
        public static final int SESSION_MODIFY_REQUEST_FAIL = 2;
        public static final int SESSION_MODIFY_REQUEST_INVALID = 3;
        public static final int SESSION_MODIFY_REQUEST_TIMED_OUT = 4;
        public static final int SESSION_MODIFY_REQUEST_REJECTED_BY_REMOTE = 5;

        private RttModifyStatus()
        {
        }
    }

    public abstract static class VideoProvider
    {
        public static final int SESSION_EVENT_RX_PAUSE = 1;
        public static final int SESSION_EVENT_RX_RESUME = 2;
        public static final int SESSION_EVENT_TX_START = 3;
        public static final int SESSION_EVENT_TX_STOP = 4;
        public static final int SESSION_EVENT_CAMERA_FAILURE = 5;
        public static final int SESSION_EVENT_CAMERA_READY = 6;
        public static final int SESSION_EVENT_CAMERA_PERMISSION_ERROR = 7;

        public static final int SESSION_MODIFY_REQUEST_SUCCESS = 1;
        public static final int SESSION_MODIFY_REQUEST_FAIL = 2;
        public static final int SESSION_MODIFY_REQUEST_INVALID = 3;
        public static final int SESSION_MODIFY_REQUEST_TIMED_OUT = 4;
        public static final int SESSION_MODIFY_REQUEST_REJECTED_BY_REMOTE = 5;

        public VideoProvider()
        {
        }

        public abstract void onSetCamera(String cameraId);

        public abstract void onSetPreviewSurface(Surface surface);

        public abstract void onSetDisplaySurface(Surface surface);

        public abstract void onSetDeviceOrientation(int rotation);

        public abstract void onSetZoom(float value);

        public abstract void onSendSessionModifyRequest(VideoProfile fromProfile, VideoProfile toProfile);

        public abstract void onSendSessionModifyResponse(VideoProfile responseProfile);

        public abstract void onRequestCameraCapabilities();

        public abstract void onRequestConnectionDataUsage();

        public abstract void onSetPauseImage(Uri uri);

        public void receiveSessionModifyRequest(VideoProfile videoProfile)
        {
        }

        public void receiveSessionModifyResponse(int status, VideoProfile requestedProfile, VideoProfile responseProfile)
        {
        }

        public void handleCallSessionEvent(int event)
        {
        }

        public void changePeerDimensions(int width, int height)
        {
        }

        public void setCallDataUsage(long dataUsage)
        {
        }

        public void changeCameraCapabilities(VideoProfile.CameraCapabilities cameraCapabilities)
        {
        }

        public void changeVideoQuality(int videoQuality)
        {
        }
    }
}
