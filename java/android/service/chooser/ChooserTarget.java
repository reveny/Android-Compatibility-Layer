package android.service.chooser;

import android.content.ComponentName;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public final class ChooserTarget implements Parcelable
{
    public static final Parcelable.Creator<ChooserTarget> CREATOR = new Parcelable.Creator<ChooserTarget>()
    {
        @Override
        public ChooserTarget createFromParcel(Parcel source)
        {
            CharSequence title = source.readString();
            Icon icon = source.readInt() != 0 ? Icon.CREATOR.createFromParcel(source) : null;
            float score = source.readFloat();
            ComponentName componentName = ComponentName.readFromParcel(source);
            Bundle extras = source.readBundle();
            return new ChooserTarget(title, icon, score, componentName, extras);
        }

        @Override
        public ChooserTarget[] newArray(int size)
        {
            return new ChooserTarget[size];
        }
    };

    private final CharSequence mTitle;
    private final Icon mIcon;
    private final float mScore;
    private final ComponentName mComponentName;
    private final Bundle mIntentExtras;

    public ChooserTarget(CharSequence title, Icon icon, float score, ComponentName componentName, Bundle intentExtras)
    {
        if (score > 1.f || score < 0.f)
        {
            throw new IllegalArgumentException("Score " + score + " out of range; must be between 0.0f and 1.0f");
        }

        mTitle = title;
        mIcon = icon;
        mScore = score;
        mComponentName = componentName;
        mIntentExtras = intentExtras;
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    public Icon getIcon()
    {
        return mIcon;
    }

    public float getScore()
    {
        return mScore;
    }

    public ComponentName getComponentName()
    {
        return mComponentName;
    }

    public Bundle getIntentExtras()
    {
        return mIntentExtras;
    }

    @Override
    public String toString()
    {
        return "ChooserTarget{" + mComponentName + ", " + mIntentExtras + ", '" + mTitle + "', " + mScore + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mTitle == null ? null : mTitle.toString());
        if (mIcon == null)
        {
            destination.writeInt(0);
        }
        else
        {
            destination.writeInt(1);
            mIcon.writeToParcel(destination, flags);
        }
        destination.writeFloat(mScore);
        ComponentName.writeToParcel(mComponentName, destination);
        destination.writeBundle(mIntentExtras);
    }
}
