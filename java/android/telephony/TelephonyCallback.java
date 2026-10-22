package android.telephony;

import java.util.List;

public class TelephonyCallback
{
    public TelephonyCallback()
    {
    }

    public interface ServiceStateListener
    {
        void onServiceStateChanged(ServiceState serviceState);
    }

    public interface MessageWaitingIndicatorListener
    {
        void onMessageWaitingIndicatorChanged(boolean messageWaitingIndicator);
    }

    public interface CallForwardingIndicatorListener
    {
        void onCallForwardingIndicatorChanged(boolean callForwardingIndicator);
    }

    public interface CellLocationListener
    {
        void onCellLocationChanged(CellLocation location);
    }

    public interface CallStateListener
    {
        void onCallStateChanged(int state);
    }

    public interface DataConnectionStateListener
    {
        void onDataConnectionStateChanged(int state, int networkType);
    }

    public interface DataActivityListener
    {
        void onDataActivity(int direction);
    }

    public interface SignalStrengthsListener
    {
        void onSignalStrengthsChanged(SignalStrength signalStrength);
    }

    public interface CellInfoListener
    {
        void onCellInfoChanged(List<CellInfo> cellInfo);
    }

    public interface CallDisconnectCauseListener
    {
        void onCallDisconnectCauseChanged(int disconnectCause, int preciseDisconnectCause);
    }

    public interface UserMobileDataStateListener
    {
        void onUserMobileDataStateChanged(boolean enabled);
    }

    public interface DisplayInfoListener
    {
        void onDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo);
    }

    public interface CarrierNetworkListener
    {
        void onCarrierNetworkChange(boolean active);
    }

    public interface RegistrationFailedListener
    {
        void onRegistrationFailed(CellIdentity cellIdentity, String chosenPlmn, int domain, int causeCode, int additionalCauseCode);
    }

    public interface ActiveDataSubscriptionIdListener
    {
        void onActiveDataSubscriptionIdChanged(int subId);
    }

    public interface DataActivationStateListener
    {
        void onDataActivationStateChanged(int state);
    }
}
