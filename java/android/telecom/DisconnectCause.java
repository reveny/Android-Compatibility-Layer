package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Objects;

public final class DisconnectCause implements Parcelable
{
    public static final int UNKNOWN = 0;
    public static final int ERROR = 1;
    public static final int LOCAL = 2;
    public static final int REMOTE = 3;
    public static final int CANCELED = 4;
    public static final int MISSED = 5;
    public static final int REJECTED = 6;
    public static final int BUSY = 7;
    public static final int RESTRICTED = 8;
    public static final int OTHER = 9;
    public static final int CONNECTION_MANAGER_NOT_SUPPORTED = 10;
    public static final int ANSWERED_ELSEWHERE = 11;
    public static final int CALL_PULLED = 12;

    public static final String REASON_EMERGENCY_CALL_PLACED = "REASON_EMERGENCY_CALL_PLACED";
    public static final String REASON_EMULATING_SINGLE_CALL = "EMULATING_SINGLE_CALL";
    public static final String REASON_IMS_ACCESS_BLOCKED = "REASON_IMS_ACCESS_BLOCKED";
    public static final String REASON_WIFI_ON_BUT_WFC_OFF = "REASON_WIFI_ON_BUT_WFC_OFF";

    private static final int TONE_UNKNOWN = -1;

    public static final Parcelable.Creator<DisconnectCause> CREATOR = new Parcelable.Creator<DisconnectCause>()
    {
        @Override
        public DisconnectCause createFromParcel(Parcel source)
        {
            int code = source.readInt();
            String label = source.readString();
            String description = source.readString();
            String reason = source.readString();
            int tone = source.readInt();
            return new DisconnectCause(code, label, description, reason, tone);
        }

        @Override
        public DisconnectCause[] newArray(int size)
        {
            return new DisconnectCause[size];
        }
    };

    private final int mDisconnectCode;
    private final CharSequence mDisconnectLabel;
    private final CharSequence mDisconnectDescription;
    private final String mDisconnectReason;
    private final int mToneToPlay;

    public DisconnectCause(int code)
    {
        this(code, null, null, null, TONE_UNKNOWN);
    }

    public DisconnectCause(int code, String reason)
    {
        this(code, null, null, reason, TONE_UNKNOWN);
    }

    public DisconnectCause(int code, CharSequence label, CharSequence description, String reason)
    {
        this(code, label, description, reason, TONE_UNKNOWN);
    }

    public DisconnectCause(int code, CharSequence label, CharSequence description, String reason, int toneToPlay)
    {
        mDisconnectCode = code;
        mDisconnectLabel = label;
        mDisconnectDescription = description;
        mDisconnectReason = reason;
        mToneToPlay = toneToPlay;
    }

    public int getCode()
    {
        return mDisconnectCode;
    }

    public CharSequence getLabel()
    {
        return mDisconnectLabel;
    }

    public CharSequence getDescription()
    {
        return mDisconnectDescription;
    }

    public String getReason()
    {
        return mDisconnectReason;
    }

    public int getTone()
    {
        return mToneToPlay;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mDisconnectCode);
        dest.writeString(mDisconnectLabel == null ? null : mDisconnectLabel.toString());
        dest.writeString(mDisconnectDescription == null ? null : mDisconnectDescription.toString());
        dest.writeString(mDisconnectReason);
        dest.writeInt(mToneToPlay);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof DisconnectCause)) return false;
        DisconnectCause cause = (DisconnectCause)other;
        return mDisconnectCode == cause.mDisconnectCode && TextUtils.equals(mDisconnectLabel, cause.mDisconnectLabel)
            && TextUtils.equals(mDisconnectDescription, cause.mDisconnectDescription)
            && Objects.equals(mDisconnectReason, cause.mDisconnectReason) && mToneToPlay == cause.mToneToPlay;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mDisconnectCode, mDisconnectLabel == null ? null : mDisconnectLabel.toString(),
            mDisconnectDescription == null ? null : mDisconnectDescription.toString(), mDisconnectReason, mToneToPlay);
    }

    @Override
    public String toString()
    {
        String[] names = {"UNKNOWN", "ERROR", "LOCAL", "REMOTE", "CANCELED", "MISSED", "REJECTED", "BUSY", "RESTRICTED", "OTHER",
            "CONNECTION_MANAGER_NOT_SUPPORTED", "ANSWERED_ELSEWHERE", "CALL_PULLED"};
        String code = mDisconnectCode >= 0 && mDisconnectCode < names.length ? names[mDisconnectCode] : "invalid code: " + mDisconnectCode;
        return "DisconnectCause [ Code: (" + code + ") Label: (" + mDisconnectLabel + ") Description: (" + mDisconnectDescription
            + ") Reason: (" + mDisconnectReason + ") Tone: (" + mToneToPlay + ") ]";
    }
}
