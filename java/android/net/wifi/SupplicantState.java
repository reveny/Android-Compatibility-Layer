package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public enum SupplicantState implements Parcelable
{
    DISCONNECTED,
    INTERFACE_DISABLED,
    INACTIVE,
    SCANNING,
    AUTHENTICATING,
    ASSOCIATING,
    ASSOCIATED,
    FOUR_WAY_HANDSHAKE,
    GROUP_HANDSHAKE,
    COMPLETED,
    DORMANT,
    UNINITIALIZED,
    INVALID;

    public static boolean isValidState(SupplicantState state)
    {
        return state != UNINITIALIZED && state != INVALID;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(name());
    }

    public static final Creator<SupplicantState> CREATOR = new Creator<SupplicantState>()
    {
        @Override
        public SupplicantState createFromParcel(Parcel source)
        {
            return SupplicantState.valueOf(source.readString());
        }

        @Override
        public SupplicantState[] newArray(int size)
        {
            return new SupplicantState[size];
        }
    };
}
