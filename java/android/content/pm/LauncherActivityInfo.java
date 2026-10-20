package android.content.pm;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

public class LauncherActivityInfo
{
    private final PackageManager mPackageManager;
    private final ComponentName mComponent;
    private final ApplicationInfo mApplicationInfo;
    private final CharSequence mLabel;
    private final UserHandle mUser;

    LauncherActivityInfo(PackageManager packageManager, ComponentName component, ApplicationInfo applicationInfo, CharSequence label,
                         UserHandle user)
    {
        mPackageManager = packageManager;
        mComponent = component;
        mApplicationInfo = applicationInfo;
        mLabel = label;
        mUser = user;
    }

    public ComponentName getComponentName()
    {
        return mComponent;
    }

    public UserHandle getUser()
    {
        return mUser;
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public ApplicationInfo getApplicationInfo()
    {
        return mApplicationInfo;
    }

    public String getName()
    {
        return mComponent == null ? null : mComponent.getClassName();
    }

    public long getFirstInstallTime()
    {
        return 0L;
    }

    public Drawable getIcon(int density)
    {
        if (mPackageManager == null || mApplicationInfo == null) return null;

        return mPackageManager.getApplicationIcon(mApplicationInfo);
    }

    public Drawable getBadgedIcon(int density)
    {
        return getIcon(density);
    }
}
