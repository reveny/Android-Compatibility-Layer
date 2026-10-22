package android.telecom;

import android.os.Bundle;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class RemoteConference
{
    private final Set<Callback> mCallbacks = new CopyOnWriteArraySet<>();
    private final List<RemoteConnection> mChildConnections = new ArrayList<>();
    private final List<RemoteConnection> mConferenceableConnections = new ArrayList<>();
    private int mState = Connection.STATE_NEW;
    private DisconnectCause mDisconnectCause;
    private int mConnectionCapabilities;
    private int mConnectionProperties;
    private Bundle mExtras;

    RemoteConference(DisconnectCause disconnectCause)
    {
        mDisconnectCause = disconnectCause;
        mState = Connection.STATE_DISCONNECTED;
    }

    public List<RemoteConnection> getConnections()
    {
        return Collections.unmodifiableList(mChildConnections);
    }

    public int getState()
    {
        return mState;
    }

    public int getConnectionCapabilities()
    {
        return mConnectionCapabilities;
    }

    public int getConnectionProperties()
    {
        return mConnectionProperties;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public void disconnect()
    {
        if (mState == Connection.STATE_DISCONNECTED) return;
        int oldState = mState;
        mState = Connection.STATE_DISCONNECTED;
        mDisconnectCause = new DisconnectCause(DisconnectCause.LOCAL);
        for (Callback callback : mCallbacks)
        {
            callback.onDisconnected(this, mDisconnectCause);
            callback.onStateChanged(this, oldState, mState);
            callback.onDestroyed(this);
        }
    }

    public void separate(RemoteConnection connection)
    {
    }

    public void merge()
    {
    }

    public void swap()
    {
    }

    public void hold()
    {
    }

    public void unhold()
    {
    }

    public DisconnectCause getDisconnectCause()
    {
        return mDisconnectCause;
    }

    public void playDtmfTone(char digit)
    {
    }

    public void stopDtmfTone()
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

    public abstract static class Callback
    {
        public Callback()
        {
        }

        public void onStateChanged(RemoteConference conference, int oldState, int newState)
        {
        }

        public void onDisconnected(RemoteConference conference, DisconnectCause disconnectCause)
        {
        }

        public void onConnectionAdded(RemoteConference conference, RemoteConnection connection)
        {
        }

        public void onConnectionRemoved(RemoteConference conference, RemoteConnection connection)
        {
        }

        public void onConnectionCapabilitiesChanged(RemoteConference conference, int connectionCapabilities)
        {
        }

        public void onConnectionPropertiesChanged(RemoteConference conference, int connectionProperties)
        {
        }

        public void onConferenceableConnectionsChanged(RemoteConference conference, List<RemoteConnection> conferenceableConnections)
        {
        }

        public void onDestroyed(RemoteConference conference)
        {
        }

        public void onExtrasChanged(RemoteConference conference, Bundle extras)
        {
        }
    }
}
