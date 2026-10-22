package android.telephony;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.Executor;

public class PhoneStateListener
{
    public static final int LISTEN_NONE = 0;
    public static final int LISTEN_SERVICE_STATE = 0x00000001;
    public static final int LISTEN_SIGNAL_STRENGTH = 0x00000002;
    public static final int LISTEN_MESSAGE_WAITING_INDICATOR = 0x00000004;
    public static final int LISTEN_CALL_FORWARDING_INDICATOR = 0x00000008;
    public static final int LISTEN_CELL_LOCATION = 0x00000010;
    public static final int LISTEN_CALL_STATE = 0x00000020;
    public static final int LISTEN_DATA_CONNECTION_STATE = 0x00000040;
    public static final int LISTEN_DATA_ACTIVITY = 0x00000080;
    public static final int LISTEN_SIGNAL_STRENGTHS = 0x00000100;
    public static final int LISTEN_CELL_INFO = 0x00000400;
    public static final int LISTEN_PRECISE_DATA_CONNECTION_STATE = 0x00001000;
    public static final int LISTEN_USER_MOBILE_DATA_STATE = 0x00080000;
    public static final int LISTEN_DISPLAY_INFO_CHANGED = 0x00100000;
    public static final int LISTEN_ACTIVE_DATA_SUBSCRIPTION_ID_CHANGE = 0x00400000;
    public static final int LISTEN_EMERGENCY_NUMBER_LIST = 0x01000000;
    public static final int LISTEN_CALL_DISCONNECT_CAUSES = 0x02000000;
    public static final int LISTEN_IMS_CALL_DISCONNECT_CAUSES = 0x08000000;
    public static final int LISTEN_REGISTRATION_FAILURE = 0x40000000;
    public static final int LISTEN_BARRING_INFO = 0x80000000;

    final Executor mExecutor;

    public PhoneStateListener()
    {
        Looper looper = Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper();
        Handler handler = new Handler(looper);
        mExecutor = command -> handler.post(command);
    }

    public PhoneStateListener(Executor executor)
    {
        if (executor == null) throw new IllegalArgumentException("PhoneStateListener Executor must be non-null");
        mExecutor = executor;
    }

    @Deprecated
    public void onServiceStateChanged(ServiceState serviceState)
    {
    }

    @Deprecated
    public void onSignalStrengthChanged(int asu)
    {
    }

    @Deprecated
    public void onMessageWaitingIndicatorChanged(boolean messageWaitingIndicator)
    {
    }

    @Deprecated
    public void onCallForwardingIndicatorChanged(boolean callForwardingIndicator)
    {
    }

    @Deprecated
    public void onCellLocationChanged(CellLocation location)
    {
    }

    @Deprecated
    public void onCallStateChanged(int state, String phoneNumber)
    {
    }

    @Deprecated
    public void onDataConnectionStateChanged(int state)
    {
    }

    @Deprecated
    public void onDataConnectionStateChanged(int state, int networkType)
    {
    }

    @Deprecated
    public void onDataActivity(int direction)
    {
    }

    @Deprecated
    public void onSignalStrengthsChanged(SignalStrength signalStrength)
    {
    }

    @Deprecated
    public void onCellInfoChanged(List<CellInfo> cellInfo)
    {
    }

    @Deprecated
    public void onCallDisconnectCauseChanged(int disconnectCause, int preciseDisconnectCause)
    {
    }

    @Deprecated
    public void onUserMobileDataStateChanged(boolean enabled)
    {
    }

    @Deprecated
    public void onDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo)
    {
    }

    @Deprecated
    public void onActiveDataSubscriptionIdChanged(int subId)
    {
    }

    @Deprecated
    public void onRegistrationFailed(CellIdentity cellIdentity, String chosenPlmn, int domain, int causeCode, int additionalCauseCode)
    {
    }
}
