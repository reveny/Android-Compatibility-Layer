package android.content.pm;

import android.content.ComponentName;
import java.util.ArrayList;
import java.util.List;

public class PackageManager
{
    public static final int PERMISSION_GRANTED = 0;
    public static final int PERMISSION_DENIED = -1;

    public static final int GET_META_DATA = 0x00000080;
    public static final int GET_PROVIDERS = 0x00000008;
    public static final int GET_ACTIVITIES = 0x00000001;

    private final ApplicationInfo mApplicationInfo;
    private final List<ProviderInfo> mProviders = new ArrayList<>();
    private final List<ActivityInfo> mActivities = new ArrayList<>();

    public PackageManager(ApplicationInfo applicationInfo)
    {
        mApplicationInfo = applicationInfo;
    }

    public PackageInfo getPackageInfo(String packageName, PackageInfoFlags flags) throws NameNotFoundException
    {
        return getPackageInfo(packageName, flags == null ? 0 : (int)flags.getValue());
    }

    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException
    {
        if (packageName != null && !packageName.equals(mApplicationInfo.packageName))
        {
            throw new NameNotFoundException(packageName);
        }

        PackageInfo info = new PackageInfo();
        info.packageName = mApplicationInfo.packageName;
        info.applicationInfo = mApplicationInfo;
        long code = runtime.Bootstrap.nativeVersionCode();
        String name = runtime.Bootstrap.nativeVersionName();

        info.versionName = name != null && !name.isEmpty() ? name : null;
        info.versionCode = (int)code;
        info.longVersionCode = code;
        info.firstInstallTime = 0L;
        info.lastUpdateTime = 0L;

        info.requestedPermissions = runtime.Permissions.declared();
        info.requestedPermissionsFlags = new int[info.requestedPermissions.length];

        for (int index = 0; index < info.requestedPermissions.length; index++)
        {
            boolean granted = runtime.Permissions.check(info.requestedPermissions[index]) == PERMISSION_GRANTED;
            info.requestedPermissionsFlags[index] = granted ? PackageInfo.REQUESTED_PERMISSION_GRANTED : 0;
        }

        Signature[] signatures = signatures();

        if ((flags & GET_SIGNATURES) != 0) info.signatures = signatures;
        if ((flags & GET_SIGNING_CERTIFICATES) != 0) info.signingInfo = new SigningInfo(signatures, signatures.length > 1);

        return info;
    }

    private static Signature[] signatures()
    {
        if (sSignatures != null) return sSignatures;

        int count = runtime.Bootstrap.nativeSignatureCount();
        Signature[] signatures = new Signature[Math.max(count, 0)];

        for (int index = 0; index < signatures.length; index++)
        {
            signatures[index] = new Signature(runtime.Bootstrap.nativeSignatureAt(index));
        }

        sSignatures = signatures;

        return signatures;
    }

    private static Signature[] sSignatures;

    static ApplicationInfo describeInstalled(runtime.Packages.Entry entry)
    {
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = entry.packageName;
        info.name = entry.label;
        info.sourceDir = entry.archivePath;
        info.publicSourceDir = entry.archivePath;
        info.dataDir = null;
        info.targetSdkVersion = entry.targetSdkVersion;
        info.enabled = true;
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        return info;
    }

    public java.util.List<PackageInfo> getInstalledPackages(int flags)
    {
        java.util.List<PackageInfo> packages = new java.util.ArrayList<>();

        try
        {
            packages.add(getPackageInfo(mApplicationInfo.packageName, flags));
        }
        catch (NameNotFoundException impossible)
        {
            android.util.Log.w("PackageManager", "this package cannot describe itself", impossible);
        }

        for (runtime.Packages.Entry entry : runtime.Packages.installed())
        {
            if (entry.packageName.equals(mApplicationInfo.packageName)) continue;

            PackageInfo info = new PackageInfo();
            info.packageName = entry.packageName;
            info.applicationInfo = describeInstalled(entry);
            packages.add(info);
        }

        return packages;
    }

    public java.util.List<PackageInfo> getInstalledPackages(PackageInfoFlags flags)
    {
        return getInstalledPackages(flags == null ? 0 : (int)flags.getValue());
    }

    public java.util.List<ApplicationInfo> getInstalledApplications(int flags)
    {
        java.util.List<ApplicationInfo> applications = new java.util.ArrayList<>();
        if (mApplicationInfo != null) applications.add(mApplicationInfo);

        for (runtime.Packages.Entry entry : runtime.Packages.installed())
        {
            if (mApplicationInfo != null && entry.packageName.equals(mApplicationInfo.packageName)) continue;

            applications.add(describeInstalled(entry));
        }

        return applications;
    }

    public java.util.List<ApplicationInfo> getInstalledApplications(ApplicationInfoFlags flags)
    {
        return getInstalledApplications(flags == null ? 0 : (int)flags.getValue());
    }

    public String getInstallerPackageName(String packageName)
    {
        return null;
    }

    public int getPackageUid(String packageName, int flags) throws NameNotFoundException
    {
        if (mApplicationInfo == null || !mApplicationInfo.packageName.equals(packageName)) throw new NameNotFoundException(packageName);

        return mApplicationInfo.uid;
    }

    public String[] getPackagesForUid(int uid)
    {
        if (mApplicationInfo == null || mApplicationInfo.uid != uid) return null;

        return new String[] {mApplicationInfo.packageName};
    }


    public void addProvider(ProviderInfo provider)
    {
        mProviders.add(provider);
    }

    public List<ProviderInfo> getProviders()
    {
        return mProviders;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException
    {
        if (mApplicationInfo != null && mApplicationInfo.packageName.equals(packageName)) return mApplicationInfo;

        runtime.Packages.Entry entry = runtime.Packages.find(packageName);
        if (entry != null) return describeInstalled(entry);

        throw new NameNotFoundException(packageName);
    }

    public ApplicationInfo getApplicationInfo(String packageName, ApplicationInfoFlags flags) throws NameNotFoundException
    {
        return getApplicationInfo(packageName, flags == null ? 0 : (int)flags.getValue());
    }

    public CharSequence getApplicationLabel(ApplicationInfo info)
    {
        if (info == null) return "";
        if (info.name != null && !info.name.isEmpty()) return info.name;

        runtime.Packages.Entry entry = runtime.Packages.find(info.packageName);
        if (entry != null) return entry.label;

        return info.packageName != null ? info.packageName : "";
    }

    public android.graphics.drawable.Drawable getApplicationIcon(ApplicationInfo info)
    {
        return info == null ? null : loadInstalledIcon(info.packageName);
    }

    public android.graphics.drawable.Drawable getApplicationIcon(String packageName) throws NameNotFoundException
    {
        android.graphics.drawable.Drawable icon = loadInstalledIcon(packageName);
        if (icon == null && runtime.Packages.find(packageName) == null) throw new NameNotFoundException(packageName);

        return icon;
    }

    public android.graphics.drawable.Drawable getActivityIcon(ComponentName component) throws NameNotFoundException
    {
        return component == null ? null : getApplicationIcon(component.getPackageName());
    }

    private android.graphics.drawable.Drawable loadInstalledIcon(String packageName)
    {
        runtime.Packages.Entry entry = runtime.Packages.find(packageName);
        if (entry == null || entry.iconPath == null || entry.iconPath.isEmpty()) return null;

        try
        {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(entry.iconPath);
            if (bitmap == null) return null;

            return new android.graphics.drawable.BitmapDrawable(null, bitmap);
        }
        catch (Throwable unreadable)
        {
            android.util.Log.w("PackageManager", "the icon for " + packageName + " did not decode", unreadable);
            return null;
        }
    }

    public ProviderInfo resolveContentProvider(String authority, int flags)
    {
        if (authority == null) return null;

        for (ProviderInfo provider : mProviders)
        {
            if (provider.authority == null) continue;

            for (String declared : provider.authority.split(";"))
            {
                if (authority.equals(declared)) return provider;
            }
        }

        return null;
    }

    public java.util.List<ProviderInfo> queryContentProviders(String processName, int uid, int flags)
    {
        return new java.util.ArrayList<>(mProviders);
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException
    {
        for (ProviderInfo provider : mProviders)
        {
            if (provider.name.equals(component.getClassName())) return provider;
        }
        throw new NameNotFoundException(component.getClassName());
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException
    {
        for (runtime.Components.Record record : runtime.Components.declared(runtime.Components.SERVICE))
        {
            if (!record.className.equals(component.getClassName())) continue;

            ServiceInfo service = new ServiceInfo();
            service.name = record.className;
            service.packageName = mApplicationInfo.packageName;
            service.applicationInfo = mApplicationInfo;

            if ((flags & GET_META_DATA) != 0) service.metaData = record.metaData;

            return service;
        }

        throw new NameNotFoundException(component.getClassName());
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException
    {
        for (ActivityInfo activity : mActivities)
        {
            if (activity.name.equals(component.getClassName())) return activity;
        }
        throw new NameNotFoundException(component.getClassName());
    }

    public void addActivity(ActivityInfo activity)
    {
        mActivities.add(activity);
    }

    public static final int COMPONENT_ENABLED_STATE_DEFAULT = 0;
    public static final int COMPONENT_ENABLED_STATE_ENABLED = 1;
    public static final int COMPONENT_ENABLED_STATE_DISABLED = 2;
    public static final int COMPONENT_ENABLED_STATE_DISABLED_USER = 3;
    public static final int COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED = 4;

    public static final int DONT_KILL_APP = 0x00000001;
    public static final int MATCH_DEFAULT_ONLY = 0x00010000;
    public static final int MATCH_ALL = 0x00020000;
    public static final int MATCH_DISABLED_COMPONENTS = 0x00000200;
    public static final int GET_SERVICES = 0x00000004;
    public static final int GET_RECEIVERS = 0x00000002;
    public static final int GET_SHARED_LIBRARY_FILES = 0x00000400;
    public static final int GET_SIGNATURES = 0x00000040;
    public static final int GET_SIGNING_CERTIFICATES = 0x08000000;
    public static final int GET_PERMISSIONS = 0x00001000;
    public static final int GET_CONFIGURATIONS = 0x00004000;

    private final java.util.Map<String, Integer> mComponentState = new java.util.HashMap<>();

    public void setComponentEnabledSetting(ComponentName component, int newState, int flags)
    {
        if (component == null) return;

        mComponentState.put(component.flattenToString(), newState);
        android.util.Log.i("PackageManager", "component " + component.getClassName() + " set to state " + newState);
    }

    public int getComponentEnabledSetting(ComponentName component)
    {
        if (component == null) return COMPONENT_ENABLED_STATE_DEFAULT;

        Integer state = mComponentState.get(component.flattenToString());
        return state == null ? COMPONENT_ENABLED_STATE_DEFAULT : state;
    }

    public void setApplicationEnabledSetting(String packageName, int newState, int flags)
    {
    }

    public int getApplicationEnabledSetting(String packageName)
    {
        return COMPONENT_ENABLED_STATE_DEFAULT;
    }

    public int checkPermission(String permissionName, String packageName)
    {
        if (packageName != null && !packageName.equals(mApplicationInfo.packageName)) return PERMISSION_DENIED;

        return runtime.Permissions.check(permissionName);
    }

    public java.util.List<ResolveInfo> queryBroadcastReceivers(android.content.Intent intent, int flags)
    {
        return resolveAll(runtime.Components.RECEIVER, intent);
    }

    public java.util.List<ResolveInfo> queryIntentServices(android.content.Intent intent, int flags)
    {
        return resolveAll(runtime.Components.SERVICE, intent);
    }

    public java.util.List<ResolveInfo> queryIntentActivities(android.content.Intent intent, int flags)
    {
        java.util.List<ResolveInfo> matches = resolveAll(runtime.Components.ACTIVITY, intent);

        if (!isLauncherQuery(intent)) return matches;

        for (runtime.Packages.Entry entry : runtime.Packages.installed())
        {
            if (mApplicationInfo != null && entry.packageName.equals(mApplicationInfo.packageName)) continue;

            ResolveInfo resolved = new ResolveInfo();
            resolved.activityInfo = new ActivityInfo();
            resolved.activityInfo.name = LAUNCH_ALIAS;
            resolved.activityInfo.packageName = entry.packageName;
            resolved.activityInfo.applicationInfo = describeInstalled(entry);
            matches.add(resolved);
        }

        return matches;
    }

    static final String LAUNCH_ALIAS = "android.intent.action.MAIN";

    private static boolean isLauncherQuery(android.content.Intent intent)
    {
        if (intent == null) return false;
        if (!android.content.Intent.ACTION_MAIN.equals(intent.getAction())) return false;

        java.util.Set<String> categories = intent.getCategories();
        return categories != null && categories.contains(android.content.Intent.CATEGORY_LAUNCHER);
    }

    public ResolveInfo resolveActivity(android.content.Intent intent, int flags)
    {
        java.util.List<ResolveInfo> matches = queryIntentActivities(intent, flags);
        return matches.isEmpty() ? null : matches.get(0);
    }

    public ResolveInfo resolveService(android.content.Intent intent, int flags)
    {
        java.util.List<ResolveInfo> matches = queryIntentServices(intent, flags);
        return matches.isEmpty() ? null : matches.get(0);
    }

    public android.content.Intent getLaunchIntentForPackage(String packageName)
    {
        if (packageName == null || packageName.isEmpty()) return null;
        if (runtime.Packages.find(packageName) == null) return null;

        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(packageName, LAUNCH_ALIAS));
        intent.setPackage(packageName);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    private java.util.List<ResolveInfo> resolveAll(int kind, android.content.Intent intent)
    {
        java.util.List<ResolveInfo> matches = new java.util.ArrayList<>();

        for (runtime.Components.Record record : runtime.Components.resolveAll(kind, intent))
        {
            ResolveInfo resolved = new ResolveInfo();

            if (kind == runtime.Components.SERVICE)
            {
                resolved.serviceInfo = new ServiceInfo();
                resolved.serviceInfo.name = record.className;
                resolved.serviceInfo.packageName = mApplicationInfo == null ? null : mApplicationInfo.packageName;
            }
            else if (kind == runtime.Components.ACTIVITY)
            {
                resolved.activityInfo = new ActivityInfo();
                resolved.activityInfo.name = record.className;
                resolved.activityInfo.packageName = mApplicationInfo == null ? null : mApplicationInfo.packageName;
            }
            else
            {
                resolved.activityInfo = new ActivityInfo();
                resolved.activityInfo.name = record.className;
                resolved.activityInfo.packageName = mApplicationInfo == null ? null : mApplicationInfo.packageName;
            }

            matches.add(resolved);
        }

        return matches;
    }

    public static final String FEATURE_TOUCHSCREEN = "android.hardware.touchscreen";
    public static final String FEATURE_TOUCHSCREEN_MULTITOUCH = "android.hardware.touchscreen.multitouch";
    public static final String FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT = "android.hardware.touchscreen.multitouch.distinct";
    public static final String FEATURE_SCREEN_PORTRAIT = "android.hardware.screen.portrait";
    public static final String FEATURE_SCREEN_LANDSCAPE = "android.hardware.screen.landscape";
    public static final String FEATURE_VULKAN_HARDWARE_LEVEL = "android.hardware.vulkan.level";
    public static final String FEATURE_VULKAN_HARDWARE_VERSION = "android.hardware.vulkan.version";
    public static final String FEATURE_AUDIO_OUTPUT = "android.hardware.audio.output";
    public static final String FEATURE_SOFTWARE_KEYBOARD = "android.software.input_methods";
    public static final String FEATURE_OPENGLES_EXTENSION_PACK = "android.hardware.opengles.aep";

    private static final String[] PRESENT_FEATURES = {
        FEATURE_TOUCHSCREEN, FEATURE_TOUCHSCREEN_MULTITOUCH, FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT,
        FEATURE_SCREEN_PORTRAIT, FEATURE_SCREEN_LANDSCAPE, FEATURE_AUDIO_OUTPUT, FEATURE_SOFTWARE_KEYBOARD,
        FEATURE_VULKAN_HARDWARE_LEVEL, FEATURE_VULKAN_HARDWARE_VERSION,
    };

    public boolean hasSystemFeature(String name)
    {
        if (name == null) return false;

        for (String present : PRESENT_FEATURES)
        {
            if (present.equals(name)) return true;
        }

        return false;
    }

    public boolean hasSystemFeature(String name, int version)
    {
        return hasSystemFeature(name);
    }

    public boolean isSafeMode()
    {
        return false;
    }

    private PackageInstaller mPackageInstaller;

    public PackageInstaller getPackageInstaller()
    {
        if (mPackageInstaller == null) mPackageInstaller = new PackageInstaller();

        return mPackageInstaller;
    }

    public static final class PackageInfoFlags
    {
        private final long mValue;

        private PackageInfoFlags(long value)
        {
            mValue = value;
        }

        public long getValue()
        {
            return mValue;
        }

        public static PackageInfoFlags of(long value)
        {
            return new PackageInfoFlags(value);
        }
    }

    public static final class ApplicationInfoFlags
    {
        private final long mValue;

        private ApplicationInfoFlags(long value)
        {
            mValue = value;
        }

        public long getValue()
        {
            return mValue;
        }

        public static ApplicationInfoFlags of(long value)
        {
            return new ApplicationInfoFlags(value);
        }
    }

    public static class NameNotFoundException extends android.util.AndroidException
    {
        public NameNotFoundException()
        {
        }

        public NameNotFoundException(String name)
        {
            super(name);
        }
    }
}
