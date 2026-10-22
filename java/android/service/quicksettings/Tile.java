package android.service.quicksettings;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;

public final class Tile implements Parcelable
{
    public static final int STATE_UNAVAILABLE = 0;
    public static final int STATE_INACTIVE = 1;
    public static final int STATE_ACTIVE = 2;

    private static final String TAG = "Tile";

    public static final Parcelable.Creator<Tile> CREATOR = new Parcelable.Creator<Tile>()
    {
        @Override
        public Tile createFromParcel(Parcel source)
        {
            Tile tile = new Tile();
            tile.mIcon = source.readInt() != 0 ? Icon.CREATOR.createFromParcel(source) : null;
            tile.mState = source.readInt();
            tile.mLabel = source.readString();
            tile.mSubtitle = source.readString();
            tile.mContentDescription = source.readString();
            tile.mStateDescription = source.readString();
            return tile;
        }

        @Override
        public Tile[] newArray(int size)
        {
            return new Tile[size];
        }
    };

    private Icon mIcon;
    private CharSequence mLabel;
    private CharSequence mSubtitle;
    private CharSequence mContentDescription;
    private CharSequence mStateDescription;
    private PendingIntent mPendingIntent;
    private int mState = STATE_ACTIVE;

    Tile()
    {
    }

    public int getState()
    {
        return mState;
    }

    public void setState(int state)
    {
        mState = state;
    }

    public Icon getIcon()
    {
        return mIcon;
    }

    public void setIcon(Icon icon)
    {
        mIcon = icon;
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public void setLabel(CharSequence label)
    {
        mLabel = label;
    }

    public CharSequence getSubtitle()
    {
        return mSubtitle;
    }

    public void setSubtitle(CharSequence subtitle)
    {
        mSubtitle = subtitle;
    }

    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    public CharSequence getStateDescription()
    {
        return mStateDescription;
    }

    public void setContentDescription(CharSequence contentDescription)
    {
        mContentDescription = contentDescription;
    }

    public void setStateDescription(CharSequence stateDescription)
    {
        mStateDescription = stateDescription;
    }

    public PendingIntent getActivityLaunchForClick()
    {
        return mPendingIntent;
    }

    public void setActivityLaunchForClick(PendingIntent pendingIntent)
    {
        mPendingIntent = pendingIntent;
    }

    public void updateTile()
    {
        android.util.Log.i(TAG, "tile '" + mLabel + "' updated to state " + mState + "; no quick settings panel shows it");
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        if (mIcon == null)
        {
            destination.writeInt(0);
        }
        else
        {
            destination.writeInt(1);
            mIcon.writeToParcel(destination, flags);
        }
        destination.writeInt(mState);
        destination.writeString(asString(mLabel));
        destination.writeString(asString(mSubtitle));
        destination.writeString(asString(mContentDescription));
        destination.writeString(asString(mStateDescription));
    }

    private static String asString(CharSequence value)
    {
        return value == null ? null : value.toString();
    }
}
