package android.content.pm;

import android.content.ComponentName;
import android.os.UserHandle;

public class ShortcutInfo
{
    private String mId;
    private String mPackageName;
    private ComponentName mActivity;
    private CharSequence mShortLabel;
    private CharSequence mLongLabel;

    public String getId()
    {
        return mId;
    }

    public String getPackage()
    {
        return mPackageName;
    }

    public ComponentName getActivity()
    {
        return mActivity;
    }

    public CharSequence getShortLabel()
    {
        return mShortLabel;
    }

    public CharSequence getLongLabel()
    {
        return mLongLabel;
    }

    public UserHandle getUserHandle()
    {
        return android.os.Process.myUserHandle();
    }

    public boolean isEnabled()
    {
        return false;
    }

    public boolean isDynamic()
    {
        return false;
    }

    public boolean isPinned()
    {
        return false;
    }

    public boolean isDeclaredInManifest()
    {
        return false;
    }
}
