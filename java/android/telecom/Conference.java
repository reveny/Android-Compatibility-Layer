package android.telecom;

import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Conference extends Conferenceable
{
    public static final long CONNECT_TIME_NOT_SPECIFIED = 0;

    private final PhoneAccountHandle mPhoneAccount;
    private final List<Connection> mChildConnections = new ArrayList<>();
    private final List<Connection> mConferenceableConnections = new ArrayList<>();
    private int mState = Connection.STATE_NEW;
    private DisconnectCause mDisconnectCause;
    private int mConnectionCapabilities;
    private int mConnectionProperties;
    private long mConnectTimeMillis = CONNECT_TIME_NOT_SPECIFIED;
    private long mConnectionStartElapsedRealtimeMillis = CONNECT_TIME_NOT_SPECIFIED;
    private StatusHints mStatusHints;
    private Bundle mExtras;
    private boolean mRingbackRequested;
    private Connection.VideoProvider mVideoProvider;
    private int mVideoState = VideoProfile.STATE_AUDIO_ONLY;
    private CallAudioState mCallAudioState;
    private CallEndpoint mCallEndpoint;

    public Conference(PhoneAccountHandle phoneAccount)
    {
        mPhoneAccount = phoneAccount;
    }

    public static Conference createFailedConference(DisconnectCause disconnectCause, PhoneAccountHandle phoneAccount)
    {
        Conference failed = new FailureSignalingConference(phoneAccount);
        failed.setDisconnected(disconnectCause);
        return failed;
    }

    public final PhoneAccountHandle getPhoneAccountHandle()
    {
        return mPhoneAccount;
    }

    public final List<Connection> getConnections()
    {
        return Collections.unmodifiableList(mChildConnections);
    }

    public final int getState()
    {
        return mState;
    }

    public final boolean isRingbackRequested()
    {
        return mRingbackRequested;
    }

    public final int getConnectionCapabilities()
    {
        return mConnectionCapabilities;
    }

    public final int getConnectionProperties()
    {
        return mConnectionProperties;
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

    public Connection.VideoProvider getVideoProvider()
    {
        return mVideoProvider;
    }

    public int getVideoState()
    {
        return mVideoState;
    }

    public void onDisconnect()
    {
    }

    public void onSeparate(Connection connection)
    {
    }

    public void onMerge(Connection connection)
    {
    }

    public void onHold()
    {
    }

    public void onUnhold()
    {
    }

    public void onMerge()
    {
    }

    public void onSwap()
    {
    }

    public void onPlayDtmfTone(char c)
    {
    }

    public void onStopDtmfTone()
    {
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

    public void onConnectionAdded(Connection connection)
    {
    }

    public void onAddConferenceParticipants(List<Uri> participants)
    {
    }

    public void onAnswer(int videoState)
    {
    }

    public void onReject()
    {
    }

    public void onExtrasChanged(Bundle extras)
    {
    }

    public final void setOnHold()
    {
        setState(Connection.STATE_HOLDING);
    }

    public final void setDialing()
    {
        setState(Connection.STATE_DIALING);
    }

    public final void setRinging()
    {
        setState(Connection.STATE_RINGING);
    }

    public final void setActive()
    {
        setRingbackRequested(false);
        setState(Connection.STATE_ACTIVE);
    }

    public final void setDisconnected(DisconnectCause disconnectCause)
    {
        mDisconnectCause = disconnectCause;
        setState(Connection.STATE_DISCONNECTED);
    }

    public final DisconnectCause getDisconnectCause()
    {
        return mDisconnectCause;
    }

    public final void setConnectionCapabilities(int connectionCapabilities)
    {
        mConnectionCapabilities = connectionCapabilities;
    }

    public final void setConnectionProperties(int connectionProperties)
    {
        mConnectionProperties = connectionProperties;
    }

    public final boolean addConnection(Connection connection)
    {
        if (connection == null || mChildConnections.contains(connection)) return false;
        if (!connection.setConference(this)) return false;
        mChildConnections.add(connection);
        onConnectionAdded(connection);
        return true;
    }

    public final void removeConnection(Connection connection)
    {
        if (connection != null && mChildConnections.remove(connection)) connection.resetConference();
    }

    public final void setConferenceableConnections(List<Connection> conferenceableConnections)
    {
        mConferenceableConnections.clear();
        if (conferenceableConnections == null) return;
        for (Connection connection : conferenceableConnections)
        {
            if (!mConferenceableConnections.contains(connection)) mConferenceableConnections.add(connection);
        }
    }

    public final void setRingbackRequested(boolean ringback)
    {
        mRingbackRequested = ringback;
    }

    public final void setVideoState(Connection connection, int videoState)
    {
        mVideoState = videoState;
    }

    public final void setVideoProvider(Connection connection, Connection.VideoProvider videoProvider)
    {
        mVideoProvider = videoProvider;
    }

    public final List<Connection> getConferenceableConnections()
    {
        return Collections.unmodifiableList(mConferenceableConnections);
    }

    public final void destroy()
    {
        for (Connection connection : new ArrayList<>(mChildConnections)) removeConnection(connection);
        if (mState != Connection.STATE_DISCONNECTED) setDisconnected(new DisconnectCause(DisconnectCause.OTHER));
    }

    @Deprecated
    public final void setConnectionTime(long connectionTimeMillis)
    {
        mConnectTimeMillis = connectionTimeMillis;
    }

    @Deprecated
    public final void setConnectionStartElapsedRealTime(long connectionStartElapsedRealTime)
    {
        setConnectionStartElapsedRealtimeMillis(connectionStartElapsedRealTime);
    }

    public final void setConnectionStartElapsedRealtimeMillis(long connectionStartElapsedRealTime)
    {
        mConnectionStartElapsedRealtimeMillis = connectionStartElapsedRealTime;
    }

    public final long getConnectionTime()
    {
        return mConnectTimeMillis;
    }

    public final long getConnectionStartElapsedRealtimeMillis()
    {
        return mConnectionStartElapsedRealtimeMillis;
    }

    public final void setStatusHints(StatusHints statusHints)
    {
        mStatusHints = statusHints;
    }

    public final StatusHints getStatusHints()
    {
        return mStatusHints;
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

    public final Bundle getExtras()
    {
        return mExtras == null ? null : new Bundle(mExtras);
    }

    public void sendConferenceEvent(String event, Bundle extras)
    {
    }

    private void setState(int newState)
    {
        if (mState == newState) return;
        if (newState == Connection.STATE_ACTIVE && mConnectionStartElapsedRealtimeMillis == CONNECT_TIME_NOT_SPECIFIED)
        {
            mConnectTimeMillis = System.currentTimeMillis();
            mConnectionStartElapsedRealtimeMillis = SystemClock.elapsedRealtime();
        }
        mState = newState;
    }

    @Override
    public String toString()
    {
        return "[State: " + Connection.stateToString(mState) + ",Capabilities: " + Connection.capabilitiesToString(mConnectionCapabilities)
            + ", VideoState: " + mVideoState + ", VideoProvider: " + mVideoProvider + ", isRingbackRequested: " + mRingbackRequested
            + ", ThisObject " + super.toString() + "]";
    }

    private static final class FailureSignalingConference extends Conference
    {
        FailureSignalingConference(PhoneAccountHandle phoneAccount)
        {
            super(phoneAccount);
        }
    }
}
