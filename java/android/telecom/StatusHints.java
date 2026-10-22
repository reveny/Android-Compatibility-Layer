package android.telecom;

import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Objects;

public final class StatusHints implements Parcelable
{
    public static final Parcelable.Creator<StatusHints> CREATOR = new Parcelable.Creator<StatusHints>()
    {
        @Override
        public StatusHints createFromParcel(Parcel source)
        {
            String label = source.readString();
            Icon icon = source.readBoolean() ? Icon.CREATOR.createFromParcel(source) : null;
            Bundle extras = source.readBundle();
            return new StatusHints(label, icon, extras);
        }

        @Override
        public StatusHints[] newArray(int size)
        {
            return new StatusHints[size];
        }
    };

    private final CharSequence mLabel;
    private final Icon mIcon;
    private final Bundle mExtras;

    public StatusHints(CharSequence label, Icon icon, Bundle extras)
    {
        mLabel = label;
        mIcon = icon;
        mExtras = extras;
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public Icon getIcon()
    {
        return mIcon;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mLabel == null ? null : mLabel.toString());
        dest.writeBoolean(mIcon != null);
        if (mIcon != null) mIcon.writeToParcel(dest, flags);
        dest.writeBundle(mExtras);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof StatusHints)) return false;
        StatusHints hints = (StatusHints)other;
        return TextUtils.equals(mLabel, hints.mLabel) && Objects.equals(mIcon, hints.mIcon) && Objects.equals(mExtras, hints.mExtras);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(mLabel == null ? null : mLabel.toString()) + Objects.hashCode(mIcon) + Objects.hashCode(mExtras);
    }
}
