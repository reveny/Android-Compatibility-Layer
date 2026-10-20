package android.content.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.List;

public class LauncherApps
{
    public static abstract class Callback
    {
        public abstract void onPackageRemoved(String packageName, UserHandle user);

        public abstract void onPackageAdded(String packageName, UserHandle user);

        public abstract void onPackageChanged(String packageName, UserHandle user);

        public abstract void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing);

        public abstract void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing);

        public void onPackagesSuspended(String[] packageNames, UserHandle user)
        {
        }

        public void onPackagesUnsuspended(String[] packageNames, UserHandle user)
        {
        }

        public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, UserHandle user)
        {
        }
    }

    private static final String TAG = "LauncherApps";

    private final Context mContext;
    private final List<Callback> mCallbacks = new ArrayList<>();

    public LauncherApps(Context context)
    {
        mContext = context;
    }

    public List<LauncherActivityInfo> getActivityList(String packageName, UserHandle user)
    {
        List<LauncherActivityInfo> activities = new ArrayList<>();
        PackageManager packageManager = mContext == null ? null : mContext.getPackageManager();
        if (packageManager == null) return activities;

        for (runtime.Packages.Entry entry : runtime.Packages.installed())
        {
            if (packageName != null && !packageName.equals(entry.packageName)) continue;

            ApplicationInfo info = PackageManager.describeInstalled(entry);
            ComponentName component = new ComponentName(entry.packageName, PackageManager.LAUNCH_ALIAS);

            activities.add(new LauncherActivityInfo(packageManager, component, info, entry.label, android.os.Process.myUserHandle()));
        }

        return activities;
    }

    public LauncherActivityInfo resolveActivity(Intent intent, UserHandle user)
    {
        if (intent == null || intent.getComponent() == null) return null;

        List<LauncherActivityInfo> activities = getActivityList(intent.getComponent().getPackageName(), user);
        return activities.isEmpty() ? null : activities.get(0);
    }

    public void startMainActivity(ComponentName component, UserHandle user, Rect sourceBounds, Bundle options)
    {
        if (component == null) return;

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(component);
        intent.setPackage(component.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (mContext != null) mContext.startActivity(intent);
    }

    public boolean isPackageEnabled(String packageName, UserHandle user)
    {
        return runtime.Packages.find(packageName) != null;
    }

    public boolean isActivityEnabled(ComponentName component, UserHandle user)
    {
        return component != null && isPackageEnabled(component.getPackageName(), user);
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, UserHandle user) throws PackageManager.NameNotFoundException
    {
        runtime.Packages.Entry entry = runtime.Packages.find(packageName);
        if (entry == null) throw new PackageManager.NameNotFoundException(packageName);

        return PackageManager.describeInstalled(entry);
    }

    public List<UserHandle> getProfiles()
    {
        List<UserHandle> profiles = new ArrayList<>();
        profiles.add(android.os.Process.myUserHandle());
        return profiles;
    }

    public boolean hasShortcutHostPermission()
    {
        return false;
    }

    public static class ShortcutQuery
    {
        public static final int FLAG_MATCH_DYNAMIC = 1 << 0;
        public static final int FLAG_MATCH_PINNED = 1 << 1;
        public static final int FLAG_MATCH_MANIFEST = 1 << 3;
        public static final int FLAG_MATCH_CACHED = 1 << 4;
        public static final int FLAG_GET_KEY_FIELDS_ONLY = 1 << 2;
        public static final int FLAG_MATCH_PINNED_BY_ANY_LAUNCHER = 1 << 10;

        private long mChangedSince;
        private String mPackage;
        private List<String> mShortcutIds;
        private ComponentName mActivity;
        private int mQueryFlags;

        public ShortcutQuery setChangedSince(long changedSince)
        {
            mChangedSince = changedSince;
            return this;
        }

        public ShortcutQuery setPackage(String packageName)
        {
            mPackage = packageName;
            return this;
        }

        public ShortcutQuery setShortcutIds(List<String> shortcutIds)
        {
            mShortcutIds = shortcutIds;
            return this;
        }

        public ShortcutQuery setActivity(ComponentName activity)
        {
            mActivity = activity;
            return this;
        }

        public ShortcutQuery setQueryFlags(int queryFlags)
        {
            mQueryFlags = queryFlags;
            return this;
        }

        public String getPackage()
        {
            return mPackage;
        }

        public List<String> getShortcutIds()
        {
            return mShortcutIds;
        }

        public ComponentName getActivity()
        {
            return mActivity;
        }

        public int getQueryFlags()
        {
            return mQueryFlags;
        }
    }

    public List<ShortcutInfo> getShortcuts(ShortcutQuery query, UserHandle user)
    {
        return new java.util.ArrayList<>();
    }

    public List<ShortcutInfo> getShortcuts(Object query, UserHandle user)
    {
        return new ArrayList<>();
    }

    public void registerCallback(Callback callback)
    {
        registerCallback(callback, null);
    }

    public void registerCallback(Callback callback, android.os.Handler handler)
    {
        if (callback == null || mCallbacks.contains(callback)) return;

        mCallbacks.add(callback);
    }

    public void unregisterCallback(Callback callback)
    {
        mCallbacks.remove(callback);
    }

    private final java.util.ArrayList<PackageInstaller.SessionCallback> mSessionCallbacks = new java.util.ArrayList<>();

    public void registerPackageInstallerSessionCallback(java.util.concurrent.Executor executor,
                                                        PackageInstaller.SessionCallback callback)
    {
        if (callback == null || mSessionCallbacks.contains(callback)) return;

        mSessionCallbacks.add(callback);
    }

    public void unregisterPackageInstallerSessionCallback(PackageInstaller.SessionCallback callback)
    {
        mSessionCallbacks.remove(callback);
    }

    public java.util.List<PackageInstaller.SessionInfo> getAllPackageInstallerSessions()
    {
        return new java.util.ArrayList<>();
    }
}
