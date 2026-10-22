package android.app;

import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.PrintWriter;
import java.util.Objects;

public final class RemoteAction implements Parcelable
{
    public static final Parcelable.Creator<RemoteAction> CREATOR = new Parcelable.Creator<RemoteAction>()
    {
        @Override
        public RemoteAction createFromParcel(Parcel source)
        {
            Icon icon = Icon.CREATOR.createFromParcel(source);
            String title = source.readString();
            String contentDescription = source.readString();
            RemoteAction action = new RemoteAction(icon, title, contentDescription, null);
            action.mEnabled = source.readInt() != 0;
            action.mShouldShowIcon = source.readInt() != 0;
            return action;
        }

        @Override
        public RemoteAction[] newArray(int size)
        {
            return new RemoteAction[size];
        }
    };

    private final Icon mIcon;
    private final CharSequence mTitle;
    private final CharSequence mContentDescription;
    private final PendingIntent mActionIntent;
    private boolean mEnabled;
    private boolean mShouldShowIcon;

    public RemoteAction(Icon icon, CharSequence title, CharSequence contentDescription, PendingIntent intent)
    {
        if (icon == null || title == null || contentDescription == null)
        {
            throw new IllegalArgumentException("Expected icon, title, and content description to be non-null");
        }

        mIcon = icon;
        mTitle = title;
        mContentDescription = contentDescription;
        mActionIntent = intent;
        mEnabled = true;
        mShouldShowIcon = true;
    }

    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    public void setShouldShowIcon(boolean shouldShowIcon)
    {
        mShouldShowIcon = shouldShowIcon;
    }

    public boolean shouldShowIcon()
    {
        return mShouldShowIcon;
    }

    public Icon getIcon()
    {
        return mIcon;
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    public PendingIntent getActionIntent()
    {
        return mActionIntent;
    }

    @Override
    public RemoteAction clone()
    {
        RemoteAction action = new RemoteAction(mIcon, mTitle, mContentDescription, mActionIntent);
        action.setEnabled(mEnabled);
        action.setShouldShowIcon(mShouldShowIcon);
        return action;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof RemoteAction)) return false;

        RemoteAction that = (RemoteAction)other;
        return Objects.equals(mIcon, that.mIcon) && TextUtils.equals(mTitle, that.mTitle)
               && TextUtils.equals(mContentDescription, that.mContentDescription) && Objects.equals(mActionIntent, that.mActionIntent)
               && mEnabled == that.mEnabled && mShouldShowIcon == that.mShouldShowIcon;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mIcon, mTitle == null ? null : mTitle.toString(),
                            mContentDescription == null ? null : mContentDescription.toString(), mActionIntent, mEnabled, mShouldShowIcon);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        mIcon.writeToParcel(dest, 0);
        dest.writeString(mTitle.toString());
        dest.writeString(mContentDescription.toString());
        dest.writeInt(mEnabled ? 1 : 0);
        dest.writeInt(mShouldShowIcon ? 1 : 0);
    }

    public void dump(String prefix, PrintWriter writer)
    {
        writer.print(prefix);
        writer.print("title=" + mTitle);
        writer.print(" enabled=" + mEnabled);
        writer.print(" contentDescription=" + mContentDescription);
        writer.print(" icon=" + mIcon);
        writer.print(" action=" + mActionIntent);
        writer.print(" shouldShowIcon=" + mShouldShowIcon);
        writer.println();
    }
}
