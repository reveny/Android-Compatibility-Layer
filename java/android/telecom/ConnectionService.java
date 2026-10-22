package android.telecom;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ConnectionService extends Service
{
    public static final String SERVICE_INTERFACE = "android.telecom.ConnectionService";

    private final IBinder mBinder = new Binder(SERVICE_INTERFACE);
    private final List<Connection> mConnections = new ArrayList<>();
    private final List<Conference> mConferences = new ArrayList<>();

    public ConnectionService()
    {
    }

    @Override
    public final IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        synchronized (mConnections)
        {
            for (Connection connection : new ArrayList<>(mConnections))
            {
                if (connection.getConference() == null) connection.onDisconnect();
            }
        }
        synchronized (mConferences)
        {
            for (Conference conference : new ArrayList<>(mConferences)) conference.onDisconnect();
        }
        return super.onUnbind(intent);
    }

    public final RemoteConnection createRemoteIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
        ConnectionRequest request)
    {
        return null;
    }

    public final RemoteConnection createRemoteOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount,
        ConnectionRequest request)
    {
        return null;
    }

    public final RemoteConference createRemoteIncomingConference(PhoneAccountHandle connectionManagerPhoneAccount,
        ConnectionRequest request)
    {
        return null;
    }

    public final RemoteConference createRemoteOutgoingConference(PhoneAccountHandle connectionManagerPhoneAccount,
        ConnectionRequest request)
    {
        return null;
    }

    public final void conferenceRemoteConnections(RemoteConnection remoteConnection1, RemoteConnection remoteConnection2)
    {
    }

    public final void addConference(Conference conference)
    {
        if (conference == null) return;
        synchronized (mConferences)
        {
            if (!mConferences.contains(conference)) mConferences.add(conference);
        }
    }

    public final void addExistingConnection(PhoneAccountHandle phoneAccountHandle, Connection connection)
    {
        if (connection == null) return;
        synchronized (mConnections)
        {
            if (!mConnections.contains(connection)) mConnections.add(connection);
        }
    }

    public final Collection<Connection> getAllConnections()
    {
        synchronized (mConnections)
        {
            return new ArrayList<>(mConnections);
        }
    }

    public final Collection<Conference> getAllConferences()
    {
        synchronized (mConferences)
        {
            return new ArrayList<>(mConferences);
        }
    }

    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request)
    {
        return null;
    }

    public Conference onCreateIncomingConference(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request)
    {
        return null;
    }

    public void onCreateIncomingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request)
    {
    }

    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request)
    {
    }

    public void onCreateIncomingConferenceFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request)
    {
    }

    public void onCreateOutgoingConferenceFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request)
    {
    }

    public void onHandoverFailed(ConnectionRequest request, int error)
    {
    }

    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request)
    {
        return null;
    }

    public Conference onCreateOutgoingConference(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request)
    {
        return null;
    }

    public Connection onCreateOutgoingHandoverConnection(PhoneAccountHandle fromPhoneAccountHandle, ConnectionRequest request)
    {
        return null;
    }

    public Connection onCreateIncomingHandoverConnection(PhoneAccountHandle fromPhoneAccountHandle, ConnectionRequest request)
    {
        return null;
    }

    public void onConference(Connection connection1, Connection connection2)
    {
    }

    public void onRemoteConferenceAdded(RemoteConference conference)
    {
    }

    public void onRemoteExistingConnectionAdded(RemoteConnection connection)
    {
    }

    public void onConnectionServiceFocusLost()
    {
    }

    public void onConnectionServiceFocusGained()
    {
    }

    public final void connectionServiceFocusReleased()
    {
    }
}
