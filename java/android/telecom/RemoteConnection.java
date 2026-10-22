package android.telecom;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class RemoteConnection
{
    private final Set<Callback> mCallbacks = new CopyOnWriteArraySet<>();
    private final List<RemoteConnection> mConferenceableConnections = new ArrayList<>();
    private int mState = Connection.STATE_NEW;
    private DisconnectCause mDisconnectCause;
    private boolean mRingbackRequested;
    private int mConnectionCapabilities;
    private int mConnectionProperties;
    private int mVideoState;
    private VideoProvider mVideoProvider;
    private boolean mIsVoipAudioMode;
    private StatusHints mStatusHints;
    private Uri mAddress;
    private int mAddressPresentation;
    private String mCallerDisplayName;
    private int mCallerDisplayNamePresentation;
    private RemoteConference mConference;
    private Bundle mExtras;

    RemoteConnection(DisconnectCause disconnectCause)
    {
        mDisconnectCause = disconnectCause;
        mState = Connection.STATE_DISCONNECTED;
    }

    public void registerCallback(Callback callback)
    {
        registerCallback(callback, null);
    }

    public void registerCallback(Callback callback, Handler handler)
    {
        if (callback != null) mCallbacks.add(callback);
    }

    public void unregisterCallback(Callback callback)
    {
        if (callback != null) mCallbacks.remove(callback);
    }

    public int getState()
    {
        return mState;
    }

    public DisconnectCause getDisconnectCause()
    {
        return mDisconnectCause;
    }

    public int getConnectionCapabilities()
    {
        return mConnectionCapabilities;
    }

    public int getConnectionProperties()
    {
        return mConnectionProperties;
    }

    public boolean isVoipAudioMode()
    {
        return mIsVoipAudioMode;
    }

    public StatusHints getStatusHints()
    {
        return mStatusHints;
    }

    public Uri getAddress()
    {
        return mAddress;
    }

    public int getAddressPresentation()
    {
        return mAddressPresentation;
    }

    public CharSequence getCallerDisplayName()
    {
        return mCallerDisplayName;
    }

    public int getCallerDisplayNamePresentation()
    {
        return mCallerDisplayNamePresentation;
    }

    public int getVideoState()
    {
        return mVideoState;
    }

    public VideoProvider getVideoProvider()
    {
        return mVideoProvider;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public boolean isRingbackRequested()
    {
        return mRingbackRequested;
    }

    public void abort()
    {
        disconnectLocally();
    }

    public void answer()
    {
    }

    public void reject()
    {
        disconnectLocally();
    }

    public void hold()
    {
    }

    public void unhold()
    {
    }

    public void disconnect()
    {
        disconnectLocally();
    }

    public void playDtmfTone(char digit)
    {
    }

    public void stopDtmfTone()
    {
    }

    public void postDialContinue(boolean proceed)
    {
    }

    public void pullExternalCall()
    {
    }

    public void addConferenceParticipants(List<Uri> participants)
    {
    }

    @Deprecated
    public void setCallAudioState(CallAudioState state)
    {
    }

    public List<RemoteConnection> getConferenceableConnections()
    {
        return Collections.unmodifiableList(mConferenceableConnections);
    }

    public RemoteConference getConference()
    {
        return mConference;
    }

    private void disconnectLocally()
    {
        if (mState == Connection.STATE_DISCONNECTED) return;
        mState = Connection.STATE_DISCONNECTED;
        mDisconnectCause = new DisconnectCause(DisconnectCause.LOCAL);
        for (Callback callback : mCallbacks)
        {
            callback.onDisconnected(this, mDisconnectCause);
            callback.onStateChanged(this, mState);
            callback.onDestroyed(this);
        }
    }

    public abstract static class Callback
    {
        public Callback()
        {
        }

        public void onStateChanged(RemoteConnection connection, int state)
        {
        }

        public void onDisconnected(RemoteConnection connection, DisconnectCause disconnectCause)
        {
        }

        public void onRingbackRequested(RemoteConnection connection, boolean ringback)
        {
        }

        public void onConnectionCapabilitiesChanged(RemoteConnection connection, int connectionCapabilities)
        {
        }

        public void onConnectionPropertiesChanged(RemoteConnection connection, int connectionProperties)
        {
        }

        public void onPostDialWait(RemoteConnection connection, String remainingPostDialSequence)
        {
        }

        public void onPostDialChar(RemoteConnection connection, char nextChar)
        {
        }

        public void onVoipAudioChanged(RemoteConnection connection, boolean isVoip)
        {
        }

        public void onStatusHintsChanged(RemoteConnection connection, StatusHints statusHints)
        {
        }

        public void onAddressChanged(RemoteConnection connection, Uri address, int presentation)
        {
        }

        public void onCallerDisplayNameChanged(RemoteConnection connection, String callerDisplayName, int presentation)
        {
        }

        public void onVideoStateChanged(RemoteConnection connection, int videoState)
        {
        }

        public void onDestroyed(RemoteConnection connection)
        {
        }

        public void onConferenceableConnectionsChanged(RemoteConnection connection, List<RemoteConnection> conferenceableConnections)
        {
        }

        public void onVideoProviderChanged(RemoteConnection connection, VideoProvider videoProvider)
        {
        }

        public void onConferenceChanged(RemoteConnection connection, RemoteConference conference)
        {
        }

        public void onExtrasChanged(RemoteConnection connection, Bundle extras)
        {
        }

        public void onConnectionEvent(RemoteConnection connection, String event, Bundle extras)
        {
        }
    }

    public static class VideoProvider
    {
        private final Set<Callback> mCallbacks = new CopyOnWriteArraySet<>();

        VideoProvider()
        {
        }

        public void registerCallback(Callback callback)
        {
            if (callback != null) mCallbacks.add(callback);
        }

        public void unregisterCallback(Callback callback)
        {
            if (callback != null) mCallbacks.remove(callback);
        }

        public void setCamera(String cameraId)
        {
        }

        public void setPreviewSurface(Surface surface)
        {
        }

        public void setDisplaySurface(Surface surface)
        {
        }

        public void setDeviceOrientation(int rotation)
        {
        }

        public void setZoom(float value)
        {
        }

        public void sendSessionModifyRequest(VideoProfile fromProfile, VideoProfile toProfile)
        {
        }

        public void sendSessionModifyResponse(VideoProfile responseProfile)
        {
        }

        public void requestCameraCapabilities()
        {
        }

        public void requestCallDataUsage()
        {
        }

        public void setPauseImage(Uri uri)
        {
        }

        public abstract static class Callback
        {
            public Callback()
            {
            }

            public void onSessionModifyRequestReceived(VideoProvider videoProvider, VideoProfile videoProfile)
            {
            }

            public void onSessionModifyResponseReceived(VideoProvider videoProvider, int status, VideoProfile requestedProfile,
                VideoProfile responseProfile)
            {
            }

            public void onCallSessionEvent(VideoProvider videoProvider, int event)
            {
            }

            public void onPeerDimensionsChanged(VideoProvider videoProvider, int width, int height)
            {
            }

            public void onCallDataUsageChanged(VideoProvider videoProvider, long dataUsage)
            {
            }

            public void onCameraCapabilitiesChanged(VideoProvider videoProvider, VideoProfile.CameraCapabilities cameraCapabilities)
            {
            }

            public void onVideoQualityChanged(VideoProvider videoProvider, int videoQuality)
            {
            }
        }
    }
}
