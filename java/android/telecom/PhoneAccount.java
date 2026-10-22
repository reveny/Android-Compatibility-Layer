package android.telecom;

import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PhoneAccount implements Parcelable
{
    public static final int CAPABILITY_CONNECTION_MANAGER = 0x1;
    public static final int CAPABILITY_CALL_PROVIDER = 0x2;
    public static final int CAPABILITY_SIM_SUBSCRIPTION = 0x4;
    public static final int CAPABILITY_VIDEO_CALLING = 0x8;
    public static final int CAPABILITY_PLACE_EMERGENCY_CALLS = 0x10;
    public static final int CAPABILITY_CALL_SUBJECT = 0x40;
    public static final int CAPABILITY_VIDEO_CALLING_RELIES_ON_PRESENCE = 0x100;
    public static final int CAPABILITY_SUPPORTS_VIDEO_CALLING = 0x400;
    public static final int CAPABILITY_SELF_MANAGED = 0x800;
    public static final int CAPABILITY_RTT = 0x1000;
    public static final int CAPABILITY_ADHOC_CONFERENCE_CALLING = 0x4000;
    public static final int CAPABILITY_CALL_COMPOSER = 0x8000;
    public static final int CAPABILITY_SUPPORTS_VOICE_CALLING_INDICATIONS = 0x10000;
    public static final int CAPABILITY_VOICE_CALLING_AVAILABLE = 0x20000;
    public static final int CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS = 0x40000;
    public static final int CAPABILITY_SUPPORTS_CALL_STREAMING = 0x80000;

    public static final String EXTRA_ADD_SELF_MANAGED_CALLS_TO_INCALLSERVICE = "android.telecom.extra.ADD_SELF_MANAGED_CALLS_TO_INCALLSERVICE";
    public static final String EXTRA_ALWAYS_USE_VOIP_AUDIO_MODE = "android.telecom.extra.ALWAYS_USE_VOIP_AUDIO_MODE";
    public static final String EXTRA_CALL_SUBJECT_CHARACTER_ENCODING = "android.telecom.extra.CALL_SUBJECT_CHARACTER_ENCODING";
    public static final String EXTRA_CALL_SUBJECT_MAX_LENGTH = "android.telecom.extra.CALL_SUBJECT_MAX_LENGTH";
    public static final String EXTRA_LOG_SELF_MANAGED_CALLS = "android.telecom.extra.LOG_SELF_MANAGED_CALLS";
    public static final String EXTRA_SUPPORTS_HANDOVER_FROM = "android.telecom.extra.SUPPORTS_HANDOVER_FROM";
    public static final String EXTRA_SUPPORTS_HANDOVER_TO = "android.telecom.extra.SUPPORTS_HANDOVER_TO";

    public static final int NO_HIGHLIGHT_COLOR = 0;
    public static final int NO_RESOURCE_ID = -1;

    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_VOICEMAIL = "voicemail";
    public static final String SCHEME_SIP = "sip";

    public static final Parcelable.Creator<PhoneAccount> CREATOR = new Parcelable.Creator<PhoneAccount>()
    {
        @Override
        public PhoneAccount createFromParcel(Parcel source)
        {
            PhoneAccountHandle handle = source.readBoolean() ? PhoneAccountHandle.CREATOR.createFromParcel(source) : null;
            Builder builder = new Builder(handle, source.readString());
            builder.setAddress(source.readBoolean() ? Uri.CREATOR.createFromParcel(source) : null);
            builder.setSubscriptionAddress(source.readBoolean() ? Uri.CREATOR.createFromParcel(source) : null);
            builder.setCapabilities(source.readInt());
            builder.setHighlightColor(source.readInt());
            builder.setShortDescription(source.readString());
            builder.setSupportedUriSchemes(source.createStringArrayList());
            builder.setIcon(source.readBoolean() ? Icon.CREATOR.createFromParcel(source) : null);
            builder.setExtras(source.readBundle());
            builder.mIsEnabled = source.readBoolean();
            List<PhoneAccountHandle> restriction = source.createTypedArrayList(PhoneAccountHandle.CREATOR);
            if (restriction != null) builder.setSimultaneousCallingRestriction(new LinkedHashSet<>(restriction));
            return builder.build();
        }

        @Override
        public PhoneAccount[] newArray(int size)
        {
            return new PhoneAccount[size];
        }
    };

    private final PhoneAccountHandle mAccountHandle;
    private final Uri mAddress;
    private final Uri mSubscriptionAddress;
    private final int mCapabilities;
    private final int mHighlightColor;
    private final CharSequence mLabel;
    private final CharSequence mShortDescription;
    private final List<String> mSupportedUriSchemes;
    private final Icon mIcon;
    private final Bundle mExtras;
    private final boolean mIsEnabled;
    private final Set<PhoneAccountHandle> mSimultaneousCallingRestriction;

    private PhoneAccount(Builder builder)
    {
        mAccountHandle = builder.mAccountHandle;
        mAddress = builder.mAddress;
        mSubscriptionAddress = builder.mSubscriptionAddress;
        mCapabilities = builder.mCapabilities;
        mHighlightColor = builder.mHighlightColor;
        mLabel = builder.mLabel;
        mShortDescription = builder.mShortDescription;
        mSupportedUriSchemes = Collections.unmodifiableList(new ArrayList<>(builder.mSupportedUriSchemes));
        mIcon = builder.mIcon;
        mExtras = builder.mExtras;
        mIsEnabled = builder.mIsEnabled;
        mSimultaneousCallingRestriction = builder.mSimultaneousCallingRestriction == null ? null
            : Collections.unmodifiableSet(new LinkedHashSet<>(builder.mSimultaneousCallingRestriction));
    }

    public static Builder builder(PhoneAccountHandle accountHandle, CharSequence label)
    {
        return new Builder(accountHandle, label);
    }

    public Builder toBuilder()
    {
        return new Builder(this);
    }

    public PhoneAccountHandle getAccountHandle()
    {
        return mAccountHandle;
    }

    public Uri getAddress()
    {
        return mAddress;
    }

    public Uri getSubscriptionAddress()
    {
        return mSubscriptionAddress;
    }

    public int getCapabilities()
    {
        return mCapabilities;
    }

    public boolean hasCapabilities(int capability)
    {
        return (mCapabilities & capability) == capability;
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public CharSequence getShortDescription()
    {
        return mShortDescription;
    }

    public List<String> getSupportedUriSchemes()
    {
        return mSupportedUriSchemes;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public boolean supportsUriScheme(String uriScheme)
    {
        if (uriScheme == null) return false;
        for (String scheme : mSupportedUriSchemes)
        {
            if (scheme != null && scheme.equals(uriScheme)) return true;
        }
        return false;
    }

    public int getHighlightColor()
    {
        return mHighlightColor;
    }

    public Icon getIcon()
    {
        return mIcon;
    }

    public boolean isEnabled()
    {
        return mIsEnabled;
    }

    public Set<PhoneAccountHandle> getSimultaneousCallingRestriction()
    {
        if (mSimultaneousCallingRestriction == null) throw new IllegalStateException("This method can not be called if there is no "
            + "simultaneous calling restriction. See #hasSimultaneousCallingRestriction");
        return mSimultaneousCallingRestriction;
    }

    public boolean hasSimultaneousCallingRestriction()
    {
        return mSimultaneousCallingRestriction != null;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeBoolean(mAccountHandle != null);
        if (mAccountHandle != null) mAccountHandle.writeToParcel(dest, flags);
        dest.writeString(mLabel == null ? null : mLabel.toString());
        dest.writeBoolean(mAddress != null);
        if (mAddress != null) mAddress.writeToParcel(dest, flags);
        dest.writeBoolean(mSubscriptionAddress != null);
        if (mSubscriptionAddress != null) mSubscriptionAddress.writeToParcel(dest, flags);
        dest.writeInt(mCapabilities);
        dest.writeInt(mHighlightColor);
        dest.writeString(mShortDescription == null ? null : mShortDescription.toString());
        dest.writeStringList(mSupportedUriSchemes);
        dest.writeBoolean(mIcon != null);
        if (mIcon != null) mIcon.writeToParcel(dest, flags);
        dest.writeBundle(mExtras);
        dest.writeBoolean(mIsEnabled);
        dest.writeTypedList(mSimultaneousCallingRestriction == null ? null : new ArrayList<>(mSimultaneousCallingRestriction));
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PhoneAccount)) return false;
        PhoneAccount account = (PhoneAccount)other;
        return mCapabilities == account.mCapabilities && mHighlightColor == account.mHighlightColor && mIsEnabled == account.mIsEnabled
            && Objects.equals(mAccountHandle, account.mAccountHandle) && Objects.equals(mAddress, account.mAddress)
            && Objects.equals(mSubscriptionAddress, account.mSubscriptionAddress) && TextUtils.equals(mLabel, account.mLabel)
            && TextUtils.equals(mShortDescription, account.mShortDescription) && mSupportedUriSchemes.equals(account.mSupportedUriSchemes)
            && Objects.equals(mSimultaneousCallingRestriction, account.mSimultaneousCallingRestriction);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mAccountHandle, mAddress, mSubscriptionAddress, mCapabilities, mHighlightColor,
            mLabel == null ? null : mLabel.toString(), mShortDescription == null ? null : mShortDescription.toString(), mSupportedUriSchemes,
            mIsEnabled, mSimultaneousCallingRestriction);
    }

    @Override
    public String toString()
    {
        return "[[" + (mIsEnabled ? 'X' : ' ') + "] PhoneAccount: " + mAccountHandle + " Capabilities: " + mCapabilities
            + " Schemes: " + TextUtils.join(" ", mSupportedUriSchemes) + "]";
    }

    public static class Builder
    {
        private PhoneAccountHandle mAccountHandle;
        private Uri mAddress;
        private Uri mSubscriptionAddress;
        private int mCapabilities;
        private int mHighlightColor = NO_HIGHLIGHT_COLOR;
        private CharSequence mLabel;
        private CharSequence mShortDescription;
        private List<String> mSupportedUriSchemes = new ArrayList<>();
        private Icon mIcon;
        private Bundle mExtras;
        private boolean mIsEnabled;
        private Set<PhoneAccountHandle> mSimultaneousCallingRestriction;

        public Builder(PhoneAccountHandle accountHandle, CharSequence label)
        {
            mAccountHandle = accountHandle;
            mLabel = label;
        }

        public Builder(PhoneAccount phoneAccount)
        {
            mAccountHandle = phoneAccount.mAccountHandle;
            mAddress = phoneAccount.mAddress;
            mSubscriptionAddress = phoneAccount.mSubscriptionAddress;
            mCapabilities = phoneAccount.mCapabilities;
            mHighlightColor = phoneAccount.mHighlightColor;
            mLabel = phoneAccount.mLabel;
            mShortDescription = phoneAccount.mShortDescription;
            mSupportedUriSchemes.addAll(phoneAccount.mSupportedUriSchemes);
            mIcon = phoneAccount.mIcon;
            mExtras = phoneAccount.mExtras;
            mIsEnabled = phoneAccount.mIsEnabled;
            mSimultaneousCallingRestriction = phoneAccount.mSimultaneousCallingRestriction;
        }

        public Builder setAddress(Uri value)
        {
            mAddress = value;
            return this;
        }

        public Builder setSubscriptionAddress(Uri value)
        {
            mSubscriptionAddress = value;
            return this;
        }

        public Builder setCapabilities(int value)
        {
            mCapabilities = value;
            return this;
        }

        public Builder setIcon(Icon icon)
        {
            mIcon = icon;
            return this;
        }

        public Builder setHighlightColor(int value)
        {
            mHighlightColor = value;
            return this;
        }

        public Builder setShortDescription(CharSequence value)
        {
            mShortDescription = value;
            return this;
        }

        public Builder addSupportedUriScheme(String uriScheme)
        {
            if (!TextUtils.isEmpty(uriScheme) && !mSupportedUriSchemes.contains(uriScheme)) mSupportedUriSchemes.add(uriScheme);
            return this;
        }

        public Builder setSupportedUriSchemes(List<String> uriSchemes)
        {
            mSupportedUriSchemes.clear();
            if (uriSchemes != null)
            {
                for (String scheme : uriSchemes) addSupportedUriScheme(scheme);
            }
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras;
            return this;
        }

        public Builder setSimultaneousCallingRestriction(Set<PhoneAccountHandle> handles)
        {
            if (handles == null) throw new IllegalArgumentException("the Set of PhoneAccountHandles must not be null");
            mSimultaneousCallingRestriction = handles;
            return this;
        }

        public Builder clearSimultaneousCallingRestriction()
        {
            mSimultaneousCallingRestriction = null;
            return this;
        }

        public PhoneAccount build()
        {
            if (mSupportedUriSchemes.isEmpty()) addSupportedUriScheme(SCHEME_TEL);
            return new PhoneAccount(this);
        }
    }
}
