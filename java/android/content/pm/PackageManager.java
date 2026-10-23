package android.content.pm;

import android.content.ComponentName;
import java.util.ArrayList;
import java.util.List;

public class PackageManager
{
    public static final int PERMISSION_GRANTED = 0;
    public static final int PERMISSION_DENIED = -1;

    public static final int GET_ACTIVITIES = 0x00000001;
    public static final int GET_RECEIVERS = 0x00000002;
    public static final int GET_SERVICES = 0x00000004;
    public static final int GET_PROVIDERS = 0x00000008;
    public static final int GET_INSTRUMENTATION = 0x00000010;
    public static final int GET_INTENT_FILTERS = 0x00000020;
    public static final int GET_SIGNATURES = 0x00000040;
    public static final int GET_RESOLVED_FILTER = 0x00000040;
    public static final int GET_META_DATA = 0x00000080;
    public static final int GET_GIDS = 0x00000100;
    public static final int GET_DISABLED_COMPONENTS = 0x00000200;
    public static final int GET_SHARED_LIBRARY_FILES = 0x00000400;
    public static final int GET_URI_PERMISSION_PATTERNS = 0x00000800;
    public static final int GET_PERMISSIONS = 0x00001000;
    public static final int GET_UNINSTALLED_PACKAGES = 0x00002000;
    public static final int GET_CONFIGURATIONS = 0x00004000;
    public static final int GET_DISABLED_UNTIL_USED_COMPONENTS = 0x00008000;
    public static final int GET_SIGNING_CERTIFICATES = 0x08000000;
    public static final int GET_ATTRIBUTIONS = 0x80000000;
    public static final long GET_ATTRIBUTIONS_LONG = 0x80000000L;

    public static final int MATCH_DISABLED_COMPONENTS = 0x00000200;
    public static final int MATCH_UNINSTALLED_PACKAGES = 0x00002000;
    public static final int MATCH_DISABLED_UNTIL_USED_COMPONENTS = 0x00008000;
    public static final int MATCH_DEFAULT_ONLY = 0x00010000;
    public static final int MATCH_ALL = 0x00020000;
    public static final int MATCH_DIRECT_BOOT_UNAWARE = 0x00040000;
    public static final int MATCH_DIRECT_BOOT_AWARE = 0x00080000;
    public static final int MATCH_SYSTEM_ONLY = 0x00100000;
    public static final int MATCH_DIRECT_BOOT_AUTO = 0x10000000;
    public static final int MATCH_APEX = 0x40000000;

    public static final int COMPONENT_ENABLED_STATE_DEFAULT = 0;
    public static final int COMPONENT_ENABLED_STATE_ENABLED = 1;
    public static final int COMPONENT_ENABLED_STATE_DISABLED = 2;
    public static final int COMPONENT_ENABLED_STATE_DISABLED_USER = 3;
    public static final int COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED = 4;

    public static final int DONT_KILL_APP = 0x00000001;
    public static final int SYNCHRONOUS = 0x00000002;

    public static final int SIGNATURE_MATCH = 0;
    public static final int SIGNATURE_NEITHER_SIGNED = 1;
    public static final int SIGNATURE_FIRST_NOT_SIGNED = -1;
    public static final int SIGNATURE_SECOND_NOT_SIGNED = -2;
    public static final int SIGNATURE_NO_MATCH = -3;
    public static final int SIGNATURE_UNKNOWN_PACKAGE = -4;

    public static final int CERT_INPUT_RAW_X509 = 0;
    public static final int CERT_INPUT_SHA256 = 1;

    public static final int FLAG_PERMISSION_WHITELIST_SYSTEM = 1 << 0;
    public static final int FLAG_PERMISSION_WHITELIST_INSTALLER = 1 << 1;
    public static final int FLAG_PERMISSION_WHITELIST_UPGRADE = 1 << 2;

    public static final int INSTALL_REASON_UNKNOWN = 0;
    public static final int INSTALL_REASON_POLICY = 1;
    public static final int INSTALL_REASON_DEVICE_RESTORE = 2;
    public static final int INSTALL_REASON_DEVICE_SETUP = 3;
    public static final int INSTALL_REASON_USER = 4;

    public static final int INSTALL_SCENARIO_DEFAULT = 0;
    public static final int INSTALL_SCENARIO_FAST = 1;
    public static final int INSTALL_SCENARIO_BULK = 2;
    public static final int INSTALL_SCENARIO_BULK_SECONDARY = 3;

    public static final int VERIFICATION_ALLOW = 1;
    public static final int VERIFICATION_REJECT = -1;
    public static final int VERSION_CODE_HIGHEST = -1;
    public static final long MAXIMUM_VERIFICATION_TIMEOUT = 3600000L;

    public static final String EXTRA_VERIFICATION_ID = "android.content.pm.extra.VERIFICATION_ID";
    public static final String EXTRA_VERIFICATION_RESULT = "android.content.pm.extra.VERIFICATION_RESULT";

    public static final String PROPERTY_COMPAT_OVERRIDE_LANDSCAPE_TO_PORTRAIT =
            "android.camera.PROPERTY_COMPAT_OVERRIDE_LANDSCAPE_TO_PORTRAIT";
    public static final String PROPERTY_MEDIA_CAPABILITIES = "android.media.PROPERTY_MEDIA_CAPABILITIES";
    public static final String PROPERTY_SELF_CERTIFIED_NETWORK_CAPABILITIES = "android.net.PROPERTY_SELF_CERTIFIED_NETWORK_CAPABILITIES";
    public static final String PROPERTY_SPECIAL_USE_FGS_SUBTYPE = "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE";

    public static final java.util.List<java.security.cert.Certificate> TRUST_ALL =
            java.util.Collections.unmodifiableList(new ArrayList<java.security.cert.Certificate>());
    public static final java.util.List<java.security.cert.Certificate> TRUST_NONE =
            java.util.Collections.unmodifiableList(new ArrayList<java.security.cert.Certificate>());

    private static final String TAG = "PackageManager";
    private static final String SYSTEM_PACKAGE = "android";
    private static final int PACKAGE_SOURCE_UNSPECIFIED = 0;

    private final ApplicationInfo mApplicationInfo;
    private final List<ProviderInfo> mProviders = new ArrayList<>();
    private final List<ActivityInfo> mActivities = new ArrayList<>();

    public PackageManager(ApplicationInfo applicationInfo)
    {
        mApplicationInfo = applicationInfo;
    }

    private boolean isSelf(String packageName)
    {
        return mApplicationInfo != null && mApplicationInfo.packageName != null && mApplicationInfo.packageName.equals(packageName);
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
        info.splitNames = mApplicationInfo.splitNames;

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

        if ((flags & GET_ACTIVITIES) != 0) info.activities = mActivities.toArray(new ActivityInfo[0]);
        if ((flags & GET_PROVIDERS) != 0) info.providers = mProviders.toArray(new ProviderInfo[0]);

        if ((flags & GET_SERVICES) != 0)
        {
            List<ServiceInfo> services = new ArrayList<>();
            for (runtime.Components.Record record : runtime.Components.declared(runtime.Components.SERVICE))
            {
                services.add(describeService(record, flags));
            }
            info.services = services.toArray(new ServiceInfo[0]);
        }

        if ((flags & GET_RECEIVERS) != 0)
        {
            List<ActivityInfo> receivers = new ArrayList<>();
            for (runtime.Components.Record record : runtime.Components.declared(runtime.Components.RECEIVER))
            {
                receivers.add(describeReceiver(record, flags));
            }
            info.receivers = receivers.toArray(new ActivityInfo[0]);
        }

        if ((flags & GET_CONFIGURATIONS) != 0) info.reqFeatures = new FeatureInfo[0];

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
            android.util.Log.w(TAG, "this package cannot describe itself", impossible);
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

    public java.util.List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags)
    {
        java.util.List<PackageInfo> holders = new java.util.ArrayList<>();
        if (permissions == null) return holders;

        for (String permission : permissions)
        {
            if (runtime.Permissions.check(permission) != PERMISSION_GRANTED) continue;

            try
            {
                holders.add(getPackageInfo(mApplicationInfo.packageName, flags));
            }
            catch (NameNotFoundException impossible)
            {
                android.util.Log.w(TAG, "this package cannot describe itself", impossible);
            }

            break;
        }

        return holders;
    }

    public java.util.List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, PackageInfoFlags flags)
    {
        return getPackagesHoldingPermissions(permissions, flags == null ? 0 : (int)flags.getValue());
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

    public java.util.List<ModuleInfo> getInstalledModules(int flags)
    {
        return new java.util.ArrayList<>();
    }

    public ModuleInfo getModuleInfo(String packageName, int flags) throws NameNotFoundException
    {
        throw new NameNotFoundException(packageName);
    }

    public String getInstallerPackageName(String packageName)
    {
        return null;
    }

    public void setInstallerPackageName(String targetPackage, String installerPackageName)
    {
    }

    public InstallSourceInfo getInstallSourceInfo(String packageName) throws NameNotFoundException
    {
        if (!isSelf(packageName) && runtime.Packages.find(packageName) == null) throw new NameNotFoundException(packageName);

        return new InstallSourceInfo(null, null, getInstallerPackageName(packageName), null, PACKAGE_SOURCE_UNSPECIFIED);
    }

    public int getPackageUid(String packageName, int flags) throws NameNotFoundException
    {
        if (mApplicationInfo == null || !mApplicationInfo.packageName.equals(packageName)) throw new NameNotFoundException(packageName);

        return mApplicationInfo.uid;
    }

    public int getPackageUid(String packageName, PackageInfoFlags flags) throws NameNotFoundException
    {
        return getPackageUid(packageName, flags == null ? 0 : (int)flags.getValue());
    }

    public int[] getPackageGids(String packageName) throws NameNotFoundException
    {
        return getPackageGids(packageName, 0);
    }

    public int[] getPackageGids(String packageName, int flags) throws NameNotFoundException
    {
        if (!isSelf(packageName)) throw new NameNotFoundException(packageName);

        return new int[0];
    }

    public int[] getPackageGids(String packageName, PackageInfoFlags flags) throws NameNotFoundException
    {
        return getPackageGids(packageName, flags == null ? 0 : (int)flags.getValue());
    }

    public int getTargetSdkVersion(String packageName) throws NameNotFoundException
    {
        return getApplicationInfo(packageName, 0).targetSdkVersion;
    }

    public String[] getPackagesForUid(int uid)
    {
        if (mApplicationInfo == null || mApplicationInfo.uid != uid) return null;

        return new String[] {mApplicationInfo.packageName};
    }

    public String getNameForUid(int uid)
    {
        if (mApplicationInfo == null || mApplicationInfo.uid != uid) return null;

        return mApplicationInfo.packageName;
    }

    public String[] currentToCanonicalPackageNames(String[] packageNames)
    {
        return packageNames;
    }

    public String[] canonicalToCurrentPackageNames(String[] packageNames)
    {
        return packageNames;
    }

    public int checkSignatures(String firstPackage, String secondPackage)
    {
        boolean firstKnown = isSelf(firstPackage) || runtime.Packages.find(firstPackage) != null;
        boolean secondKnown = isSelf(secondPackage) || runtime.Packages.find(secondPackage) != null;
        if (!firstKnown || !secondKnown) return SIGNATURE_UNKNOWN_PACKAGE;

        if (isSelf(firstPackage) && isSelf(secondPackage)) return signatures().length == 0 ? SIGNATURE_NEITHER_SIGNED : SIGNATURE_MATCH;

        return SIGNATURE_NO_MATCH;
    }

    public int checkSignatures(int firstUid, int secondUid)
    {
        String[] first = getPackagesForUid(firstUid);
        String[] second = getPackagesForUid(secondUid);
        if (first == null || second == null) return SIGNATURE_UNKNOWN_PACKAGE;

        return checkSignatures(first[0], second[0]);
    }

    public boolean hasSigningCertificate(String packageName, byte[] certificate, int type)
    {
        if (!isSelf(packageName) || certificate == null) return false;

        for (Signature signature : signatures())
        {
            byte[] encoded = signature.toByteArray();

            if (type == CERT_INPUT_RAW_X509 && java.util.Arrays.equals(encoded, certificate)) return true;
            if (type == CERT_INPUT_SHA256 && java.util.Arrays.equals(digest("SHA-256", encoded), certificate)) return true;
        }

        return false;
    }

    public boolean hasSigningCertificate(int uid, byte[] certificate, int type)
    {
        String[] packages = getPackagesForUid(uid);
        return packages != null && hasSigningCertificate(packages[0], certificate, type);
    }

    private static byte[] digest(String algorithm, byte[] data)
    {
        try
        {
            return java.security.MessageDigest.getInstance(algorithm).digest(data);
        }
        catch (java.security.NoSuchAlgorithmException missing)
        {
            return null;
        }
    }

    public void requestChecksums(String packageName, boolean includeSplits, int required,
                                 List<java.security.cert.Certificate> trustedInstallers, OnChecksumsReadyListener onChecksumsReadyListener)
            throws java.security.cert.CertificateEncodingException, NameNotFoundException
    {
        if (onChecksumsReadyListener == null) throw new NullPointerException("onChecksumsReadyListener");
        if (!isSelf(packageName)) throw new NameNotFoundException(packageName);

        final List<ApkChecksum> checksums = new ArrayList<>();
        checksumFile(checksums, null, mApplicationInfo.sourceDir, required);

        if (includeSplits && mApplicationInfo.splitSourceDirs != null)
        {
            for (int index = 0; index < mApplicationInfo.splitSourceDirs.length; index++)
            {
                String splitName = mApplicationInfo.splitNames == null ? null : mApplicationInfo.splitNames[index];
                checksumFile(checksums, splitName, mApplicationInfo.splitSourceDirs[index], required);
            }
        }

        final OnChecksumsReadyListener listener = onChecksumsReadyListener;
        android.os.Looper main = android.os.Looper.getMainLooper();

        if (main == null)
        {
            listener.onChecksumsReady(checksums);
            return;
        }

        new android.os.Handler(main).post(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onChecksumsReady(checksums);
            }
        });
    }

    private static final int[] CHECKSUM_TYPES = {2, 4, 8, 16};
    private static final String[] CHECKSUM_ALGORITHMS = {"MD5", "SHA-1", "SHA-256", "SHA-512"};

    private static void checksumFile(List<ApkChecksum> checksums, String splitName, String path, int required)
    {
        if (path == null) return;

        for (int index = 0; index < CHECKSUM_TYPES.length; index++)
        {
            if ((required & CHECKSUM_TYPES[index]) == 0) continue;

            try (java.io.InputStream input = new java.io.FileInputStream(path))
            {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance(CHECKSUM_ALGORITHMS[index]);
                byte[] buffer = new byte[64 * 1024];
                int read;

                while ((read = input.read(buffer)) > 0) digest.update(buffer, 0, read);

                checksums.add(new ApkChecksum(splitName, CHECKSUM_TYPES[index], digest.digest()));
            }
            catch (java.io.IOException | java.security.NoSuchAlgorithmException unreadable)
            {
                android.util.Log.w(TAG, "could not checksum " + path, unreadable);
            }
        }
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
        if (info.nonLocalizedLabel != null) return info.nonLocalizedLabel;

        if (info.labelRes != 0)
        {
            CharSequence label = getText(info.packageName, info.labelRes, info);
            if (label != null) return label;
        }

        if (info.name != null && !info.name.isEmpty()) return info.name;

        runtime.Packages.Entry entry = runtime.Packages.find(info.packageName);
        if (entry != null) return entry.label;

        return info.packageName != null ? info.packageName : "";
    }

    public android.graphics.drawable.Drawable getApplicationIcon(ApplicationInfo info)
    {
        if (info == null) return null;

        if (info.icon != 0)
        {
            android.graphics.drawable.Drawable drawable = getDrawable(info.packageName, info.icon, info);
            if (drawable != null) return drawable;
        }

        android.graphics.drawable.Drawable installed = loadInstalledIcon(info.packageName);
        return installed != null ? installed : getDefaultActivityIcon();
    }

    public android.graphics.drawable.Drawable getApplicationIcon(String packageName) throws NameNotFoundException
    {
        if (isSelf(packageName)) return getApplicationIcon(mApplicationInfo);

        android.graphics.drawable.Drawable icon = loadInstalledIcon(packageName);
        if (icon == null && runtime.Packages.find(packageName) == null) throw new NameNotFoundException(packageName);

        return icon;
    }

    public android.graphics.drawable.Drawable getApplicationLogo(ApplicationInfo info)
    {
        if (info == null || info.logo == 0) return null;

        return getDrawable(info.packageName, info.logo, info);
    }

    public android.graphics.drawable.Drawable getApplicationLogo(String packageName) throws NameNotFoundException
    {
        return getApplicationLogo(getApplicationInfo(packageName, 0));
    }

    public android.graphics.drawable.Drawable getApplicationBanner(ApplicationInfo info)
    {
        if (info == null || info.banner == 0) return null;

        return getDrawable(info.packageName, info.banner, info);
    }

    public android.graphics.drawable.Drawable getApplicationBanner(String packageName) throws NameNotFoundException
    {
        return getApplicationBanner(getApplicationInfo(packageName, 0));
    }

    public android.graphics.drawable.Drawable getActivityIcon(ComponentName component) throws NameNotFoundException
    {
        if (component == null) return null;

        if (isSelf(component.getPackageName()))
        {
            for (ActivityInfo activity : mActivities)
            {
                if (activity.name.equals(component.getClassName())) return activity.loadIcon(this);
            }
        }

        return getApplicationIcon(component.getPackageName());
    }

    public android.graphics.drawable.Drawable getActivityIcon(android.content.Intent intent) throws NameNotFoundException
    {
        if (intent == null) return null;
        if (intent.getComponent() != null) return getActivityIcon(intent.getComponent());

        ResolveInfo resolved = resolveActivity(intent, MATCH_DEFAULT_ONLY);
        if (resolved == null) throw new NameNotFoundException(intent.toString());

        return resolved.loadIcon(this);
    }

    public android.graphics.drawable.Drawable getActivityLogo(ComponentName component) throws NameNotFoundException
    {
        return component == null ? null : getApplicationLogo(component.getPackageName());
    }

    public android.graphics.drawable.Drawable getActivityBanner(ComponentName component) throws NameNotFoundException
    {
        return component == null ? null : getApplicationBanner(component.getPackageName());
    }

    public android.graphics.drawable.Drawable getDefaultActivityIcon()
    {
        return new android.graphics.drawable.ColorDrawable(DEFAULT_ICON_COLOR);
    }

    private static final int DEFAULT_ICON_COLOR = 0xFF9E9E9E;

    public android.graphics.drawable.Drawable getUserBadgedIcon(android.graphics.drawable.Drawable drawable, android.os.UserHandle user)
    {
        return drawable;
    }

    public android.graphics.drawable.Drawable getUserBadgedDrawableForDensity(android.graphics.drawable.Drawable drawable,
                                                                             android.os.UserHandle user,
                                                                             android.graphics.Rect badgeLocation, int badgeDensity)
    {
        return drawable;
    }

    public CharSequence getUserBadgedLabel(CharSequence label, android.os.UserHandle user)
    {
        return label;
    }

    private android.content.res.Resources ownResources()
    {
        android.app.Application application = runtime.Bootstrap.getApplication();
        return application == null ? null : application.getResources();
    }

    public android.content.res.Resources getResourcesForApplication(ApplicationInfo info) throws NameNotFoundException
    {
        if (info == null) throw new NameNotFoundException("null");

        return getResourcesForApplication(info.packageName);
    }

    public android.content.res.Resources getResourcesForApplication(String packageName) throws NameNotFoundException
    {
        if (SYSTEM_PACKAGE.equals(packageName)) return android.content.res.Resources.getSystem();
        if (!isSelf(packageName)) throw new NameNotFoundException(packageName);

        android.content.res.Resources resources = ownResources();
        if (resources == null) throw new NameNotFoundException(packageName);

        return resources;
    }

    public CharSequence getText(String packageName, int resourceId, ApplicationInfo info)
    {
        if (resourceId == 0) return null;

        try
        {
            return getResourcesForApplication(packageName).getText(resourceId);
        }
        catch (NameNotFoundException | RuntimeException unavailable)
        {
            return null;
        }
    }

    public android.content.res.XmlResourceParser getXml(String packageName, int resourceId, ApplicationInfo info)
    {
        if (resourceId == 0) return null;

        try
        {
            return getResourcesForApplication(packageName).getXml(resourceId);
        }
        catch (NameNotFoundException | RuntimeException unavailable)
        {
            android.util.Log.w(TAG, "the xml resource 0x" + Integer.toHexString(resourceId) + " in " + packageName + " is unavailable");
            return null;
        }
    }

    public android.graphics.drawable.Drawable getDrawable(String packageName, int resourceId, ApplicationInfo info)
    {
        if (resourceId == 0) return null;

        try
        {
            return getResourcesForApplication(packageName).getDrawable(resourceId, null);
        }
        catch (NameNotFoundException | RuntimeException unavailable)
        {
            return null;
        }
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
            android.util.Log.w(TAG, "the icon for " + packageName + " did not decode", unreadable);
            return null;
        }
    }

    public PermissionInfo getPermissionInfo(String permissionName, int flags) throws NameNotFoundException
    {
        if (permissionName == null) throw new NameNotFoundException("null");

        boolean platform = permissionName.startsWith("android.permission.");
        if (!platform && !runtime.Permissions.isDeclared(permissionName)) throw new NameNotFoundException(permissionName);

        PermissionInfo info = new PermissionInfo();
        info.name = permissionName;
        info.packageName = platform ? SYSTEM_PACKAGE : mApplicationInfo.packageName;
        info.protectionLevel = runtime.Permissions.isDangerous(permissionName) ? PermissionInfo.PROTECTION_DANGEROUS
                                                                               : PermissionInfo.PROTECTION_NORMAL;
        info.flags = PermissionInfo.FLAG_INSTALLED;

        return info;
    }

    public boolean isPermissionRevokedByPolicy(String permissionName, String packageName)
    {
        return false;
    }

    public CharSequence getBackgroundPermissionOptionLabel()
    {
        return "Allow all the time";
    }

    public boolean addPermission(PermissionInfo info)
    {
        throw new SecurityException("Not allowed to modify non-dynamic permission " + (info == null ? null : info.name));
    }

    public boolean addPermissionAsync(PermissionInfo info)
    {
        return addPermission(info);
    }

    public void removePermission(String permissionName)
    {
        throw new SecurityException("Not allowed to modify non-dynamic permission " + permissionName);
    }

    public java.util.Set<String> getWhitelistedRestrictedPermissions(String packageName, int whitelistFlag)
    {
        return new java.util.HashSet<>();
    }

    public boolean addWhitelistedRestrictedPermission(String packageName, String permissionName, int whitelistFlags)
    {
        return false;
    }

    public boolean removeWhitelistedRestrictedPermission(String packageName, String permissionName, int whitelistFlags)
    {
        return false;
    }

    public boolean isAutoRevokeWhitelisted()
    {
        return false;
    }

    public boolean isAutoRevokeWhitelisted(String packageName)
    {
        return false;
    }

    public boolean setAutoRevokeWhitelisted(String packageName, boolean whitelisted)
    {
        return false;
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

    public ProviderInfo resolveContentProvider(String authority, ComponentInfoFlags flags)
    {
        return resolveContentProvider(authority, flags == null ? 0 : (int)flags.getValue());
    }

    public java.util.List<ProviderInfo> queryContentProviders(String processName, int uid, int flags)
    {
        return new java.util.ArrayList<>(mProviders);
    }

    public java.util.List<ProviderInfo> queryContentProviders(String processName, int uid, ComponentInfoFlags flags)
    {
        return queryContentProviders(processName, uid, flags == null ? 0 : (int)flags.getValue());
    }

    public java.util.List<ResolveInfo> queryIntentContentProviders(android.content.Intent intent, int flags)
    {
        java.util.List<ResolveInfo> matches = new java.util.ArrayList<>();
        if (intent == null) return matches;

        ComponentName component = intent.getComponent();
        String targetPackage = component != null ? component.getPackageName() : intent.getPackage();
        if (targetPackage != null && !isSelf(targetPackage)) return matches;

        for (ProviderInfo provider : mProviders)
        {
            if (component == null || !provider.name.equals(component.getClassName())) continue;

            ResolveInfo resolved = new ResolveInfo();
            resolved.providerInfo = provider;
            matches.add(resolved);
        }

        return matches;
    }

    public java.util.List<ResolveInfo> queryIntentContentProviders(android.content.Intent intent, ResolveInfoFlags flags)
    {
        return queryIntentContentProviders(intent, flags == null ? 0 : (int)flags.getValue());
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException
    {
        for (ProviderInfo provider : mProviders)
        {
            if (provider.name.equals(component.getClassName())) return provider;
        }
        throw new NameNotFoundException(component.getClassName());
    }

    public ProviderInfo getProviderInfo(ComponentName component, ComponentInfoFlags flags) throws NameNotFoundException
    {
        return getProviderInfo(component, flags == null ? 0 : (int)flags.getValue());
    }

    private ServiceInfo describeService(runtime.Components.Record record, int flags)
    {
        ServiceInfo service = new ServiceInfo();
        service.name = record.className;
        service.packageName = mApplicationInfo.packageName;
        service.applicationInfo = mApplicationInfo;
        service.processName = mApplicationInfo.processName;

        if ((flags & GET_META_DATA) != 0) service.metaData = record.metaData;

        return service;
    }

    private ActivityInfo describeReceiver(runtime.Components.Record record, int flags)
    {
        ActivityInfo receiver = new ActivityInfo();
        receiver.name = record.className;
        receiver.packageName = mApplicationInfo.packageName;
        receiver.applicationInfo = mApplicationInfo;
        receiver.processName = mApplicationInfo.processName;

        if ((flags & GET_META_DATA) != 0) receiver.metaData = record.metaData;

        return receiver;
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException
    {
        for (runtime.Components.Record record : runtime.Components.declared(runtime.Components.SERVICE))
        {
            if (record.className.equals(component.getClassName())) return describeService(record, flags);
        }

        throw new NameNotFoundException(component.getClassName());
    }

    public ServiceInfo getServiceInfo(ComponentName component, ComponentInfoFlags flags) throws NameNotFoundException
    {
        return getServiceInfo(component, flags == null ? 0 : (int)flags.getValue());
    }

    public ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException
    {
        for (runtime.Components.Record record : runtime.Components.declared(runtime.Components.RECEIVER))
        {
            if (record.className.equals(component.getClassName())) return describeReceiver(record, flags);
        }

        throw new NameNotFoundException(component.getClassName());
    }

    public ActivityInfo getReceiverInfo(ComponentName component, ComponentInfoFlags flags) throws NameNotFoundException
    {
        return getReceiverInfo(component, flags == null ? 0 : (int)flags.getValue());
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException
    {
        for (ActivityInfo activity : mActivities)
        {
            if (activity.name.equals(component.getClassName())) return activity;
        }

        for (runtime.Components.Record record : runtime.Components.declared(runtime.Components.ACTIVITY))
        {
            if (!record.className.equals(component.getClassName())) continue;

            ActivityInfo activity = describeReceiver(record, flags);
            activity.theme = mApplicationInfo.theme;
            return activity;
        }

        throw new NameNotFoundException(component.getClassName());
    }

    public ActivityInfo getActivityInfo(ComponentName component, ComponentInfoFlags flags) throws NameNotFoundException
    {
        return getActivityInfo(component, flags == null ? 0 : (int)flags.getValue());
    }

    public void addActivity(ActivityInfo activity)
    {
        mActivities.add(activity);
    }

    private final java.util.Map<String, Integer> mComponentState = new java.util.HashMap<>();

    private void requireInstalledComponent(ComponentName component)
    {
        if (!mApplicationInfo.packageName.equals(component.getPackageName()))
        {
            throw new IllegalArgumentException("Unknown component: " + component);
        }
    }

    public void setComponentEnabledSetting(ComponentName component, int newState, int flags)
    {
        if (component == null) return;

        requireInstalledComponent(component);
        mComponentState.put(component.flattenToString(), newState);
        android.util.Log.i(TAG, "component " + component.getClassName() + " set to state " + newState);
    }

    public void setComponentEnabledSettings(List<ComponentEnabledSetting> settings)
    {
        if (settings == null) return;

        for (ComponentEnabledSetting setting : settings)
        {
            setComponentEnabledSetting(setting.getComponentName(), setting.getEnabledState(), setting.getEnabledFlags());
        }
    }

    public int getComponentEnabledSetting(ComponentName component)
    {
        if (component == null) return COMPONENT_ENABLED_STATE_DEFAULT;

        requireInstalledComponent(component);
        Integer state = mComponentState.get(component.flattenToString());
        return state == null ? COMPONENT_ENABLED_STATE_DEFAULT : state;
    }

    public void setApplicationEnabledSetting(String packageName, int newState, int flags)
    {
    }

    public int getApplicationEnabledSetting(String packageName)
    {
        if (!mApplicationInfo.packageName.equals(packageName)) throw new IllegalArgumentException("Unknown package: " + packageName);

        return COMPONENT_ENABLED_STATE_DEFAULT;
    }

    public void setApplicationCategoryHint(String packageName, int categoryHint)
    {
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

    public java.util.List<ResolveInfo> queryBroadcastReceivers(android.content.Intent intent, ResolveInfoFlags flags)
    {
        return queryBroadcastReceivers(intent, flags == null ? 0 : (int)flags.getValue());
    }

    public java.util.List<ResolveInfo> queryIntentServices(android.content.Intent intent, int flags)
    {
        return resolveAll(runtime.Components.SERVICE, intent);
    }

    public java.util.List<ResolveInfo> queryIntentServices(android.content.Intent intent, ResolveInfoFlags flags)
    {
        return queryIntentServices(intent, flags == null ? 0 : (int)flags.getValue());
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

    public java.util.List<ResolveInfo> queryIntentActivities(android.content.Intent intent, ResolveInfoFlags flags)
    {
        return queryIntentActivities(intent, flags == null ? 0 : (int)flags.getValue());
    }

    public java.util.List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, android.content.Intent[] specifics,
                                                                   android.content.Intent intent, int flags)
    {
        java.util.List<ResolveInfo> results = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        if (specifics != null)
        {
            for (int index = 0; index < specifics.length; index++)
            {
                android.content.Intent specific = specifics[index];
                if (specific == null) continue;

                ResolveInfo resolved = resolveActivity(specific, flags);
                if (resolved == null || resolved.activityInfo == null) continue;

                String key = resolved.activityInfo.packageName + "/" + resolved.activityInfo.name;
                if (caller != null && key.equals(caller.getPackageName() + "/" + caller.getClassName())) continue;
                if (!seen.add(key)) continue;

                resolved.specificIndex = index;
                results.add(resolved);
            }
        }

        for (ResolveInfo resolved : queryIntentActivities(intent, flags))
        {
            if (resolved.activityInfo == null) continue;

            String key = resolved.activityInfo.packageName + "/" + resolved.activityInfo.name;
            if (caller != null && key.equals(caller.getPackageName() + "/" + caller.getClassName())) continue;
            if (!seen.add(key)) continue;

            results.add(resolved);
        }

        return results;
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

    public ResolveInfo resolveActivity(android.content.Intent intent, ResolveInfoFlags flags)
    {
        return resolveActivity(intent, flags == null ? 0 : (int)flags.getValue());
    }

    public ResolveInfo resolveService(android.content.Intent intent, int flags)
    {
        java.util.List<ResolveInfo> matches = queryIntentServices(intent, flags);
        return matches.isEmpty() ? null : matches.get(0);
    }

    public ResolveInfo resolveService(android.content.Intent intent, ResolveInfoFlags flags)
    {
        return resolveService(intent, flags == null ? 0 : (int)flags.getValue());
    }

    public int getPreferredActivities(List<android.content.IntentFilter> outFilters, List<ComponentName> outActivities, String packageName)
    {
        return 0;
    }

    public void clearPackagePreferredActivities(String packageName)
    {
    }

    public void addPackageToPreferred(String packageName)
    {
    }

    public void removePackageFromPreferred(String packageName)
    {
    }

    public List<PackageInfo> getPreferredPackages(int flags)
    {
        return new ArrayList<>();
    }

    public android.content.Intent getLaunchIntentForPackage(String packageName)
    {
        if (packageName == null || packageName.isEmpty()) return null;

        if (isSelf(packageName))
        {
            android.content.Intent main = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            main.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
            main.setPackage(packageName);

            for (ResolveInfo resolved : resolveAll(runtime.Components.ACTIVITY, main))
            {
                if (resolved.activityInfo == null) continue;

                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
                intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
                intent.setClassName(packageName, resolved.activityInfo.name);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            }
        }

        if (runtime.Packages.find(packageName) == null) return null;

        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(packageName, LAUNCH_ALIAS));
        intent.setPackage(packageName);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    public android.content.Intent getLeanbackLaunchIntentForPackage(String packageName)
    {
        return null;
    }

    private ActivityInfo declaredActivity(String className)
    {
        for (ActivityInfo activity : mActivities)
        {
            if (activity.name != null && activity.name.equals(className)) return activity;
        }

        return null;
    }

    private java.util.List<ResolveInfo> resolveAll(int kind, android.content.Intent intent)
    {
        java.util.List<ResolveInfo> matches = new java.util.ArrayList<>();
        String packageName = mApplicationInfo == null ? null : mApplicationInfo.packageName;

        if (intent != null)
        {
            String targetPackage = intent.getComponent() != null ? intent.getComponent().getPackageName() : intent.getPackage();
            if (targetPackage != null && packageName != null && !targetPackage.equals(packageName)) return matches;
        }

        for (runtime.Components.Record record : runtime.Components.resolveAll(kind, intent))
        {
            ResolveInfo resolved = new ResolveInfo();

            if (kind == runtime.Components.SERVICE)
            {
                resolved.serviceInfo = new ServiceInfo();
                resolved.serviceInfo.name = record.className;
                resolved.serviceInfo.packageName = packageName;
                resolved.serviceInfo.applicationInfo = mApplicationInfo;
            }
            else if (kind == runtime.Components.ACTIVITY)
            {
                ActivityInfo declared = declaredActivity(record.className);

                if (declared != null)
                {
                    resolved.activityInfo = declared;
                }
                else
                {
                    resolved.activityInfo = new ActivityInfo();
                    resolved.activityInfo.name = record.className;
                    resolved.activityInfo.packageName = packageName;
                    resolved.activityInfo.applicationInfo = mApplicationInfo;
                }
            }
            else
            {
                resolved.activityInfo = new ActivityInfo();
                resolved.activityInfo.name = record.className;
                resolved.activityInfo.packageName = packageName;
                resolved.activityInfo.applicationInfo = mApplicationInfo;
            }

            if (record.filters != null && record.filters.length > 0)
            {
                resolved.filter = record.filters[0];
                resolved.priority = record.filters[0].getPriority();
                resolved.isDefault = record.filters[0].hasCategory(android.content.Intent.CATEGORY_DEFAULT);
            }

            matches.add(resolved);
        }

        return matches;
    }

    public static final String FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS = "android.software.activities_on_secondary_displays";
    public static final String FEATURE_APP_WIDGETS = "android.software.app_widgets";
    public static final String FEATURE_AUDIO_LOW_LATENCY = "android.hardware.audio.low_latency";
    public static final String FEATURE_AUDIO_OUTPUT = "android.hardware.audio.output";
    public static final String FEATURE_AUDIO_PRO = "android.hardware.audio.pro";
    public static final String FEATURE_AUTOFILL = "android.software.autofill";
    public static final String FEATURE_AUTOMOTIVE = "android.hardware.type.automotive";
    public static final String FEATURE_BACKUP = "android.software.backup";
    public static final String FEATURE_BLUETOOTH = "android.hardware.bluetooth";
    public static final String FEATURE_BLUETOOTH_LE = "android.hardware.bluetooth_le";
    public static final String FEATURE_CAMERA = "android.hardware.camera";
    public static final String FEATURE_CAMERA_ANY = "android.hardware.camera.any";
    public static final String FEATURE_CAMERA_AR = "android.hardware.camera.ar";
    public static final String FEATURE_CAMERA_AUTOFOCUS = "android.hardware.camera.autofocus";
    public static final String FEATURE_CAMERA_CAPABILITY_MANUAL_POST_PROCESSING =
            "android.hardware.camera.capability.manual_post_processing";
    public static final String FEATURE_CAMERA_CAPABILITY_MANUAL_SENSOR = "android.hardware.camera.capability.manual_sensor";
    public static final String FEATURE_CAMERA_CAPABILITY_RAW = "android.hardware.camera.capability.raw";
    public static final String FEATURE_CAMERA_CONCURRENT = "android.hardware.camera.concurrent";
    public static final String FEATURE_CAMERA_EXTERNAL = "android.hardware.camera.external";
    public static final String FEATURE_CAMERA_FLASH = "android.hardware.camera.flash";
    public static final String FEATURE_CAMERA_FRONT = "android.hardware.camera.front";
    public static final String FEATURE_CAMERA_LEVEL_FULL = "android.hardware.camera.level.full";
    public static final String FEATURE_CANT_SAVE_STATE = "android.software.cant_save_state";
    public static final String FEATURE_COMPANION_DEVICE_SETUP = "android.software.companion_device_setup";
    public static final String FEATURE_CONNECTION_SERVICE = "android.software.connectionservice";
    public static final String FEATURE_CONSUMER_IR = "android.hardware.consumerir";
    public static final String FEATURE_CONTROLS = "android.software.controls";
    public static final String FEATURE_CREDENTIALS = "android.software.credentials";
    public static final String FEATURE_DEVICE_ADMIN = "android.software.device_admin";
    public static final String FEATURE_DEVICE_LOCK = "android.software.device_lock";
    public static final String FEATURE_EMBEDDED = "android.hardware.type.embedded";
    public static final String FEATURE_ETHERNET = "android.hardware.ethernet";
    public static final String FEATURE_EXPANDED_PICTURE_IN_PICTURE = "android.software.expanded_picture_in_picture";
    public static final String FEATURE_FACE = "android.hardware.biometrics.face";
    public static final String FEATURE_FAKETOUCH = "android.hardware.faketouch";
    public static final String FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT = "android.hardware.faketouch.multitouch.distinct";
    public static final String FEATURE_FAKETOUCH_MULTITOUCH_JAZZHAND = "android.hardware.faketouch.multitouch.jazzhand";
    public static final String FEATURE_FINGERPRINT = "android.hardware.fingerprint";
    public static final String FEATURE_FREEFORM_WINDOW_MANAGEMENT = "android.software.freeform_window_management";
    public static final String FEATURE_GAMEPAD = "android.hardware.gamepad";
    public static final String FEATURE_HARDWARE_KEYSTORE = "android.hardware.hardware_keystore";
    public static final String FEATURE_HIFI_SENSORS = "android.hardware.sensor.hifi_sensors";
    public static final String FEATURE_HOME_SCREEN = "android.software.home_screen";
    public static final String FEATURE_IDENTITY_CREDENTIAL_HARDWARE = "android.hardware.identity_credential";
    public static final String FEATURE_IDENTITY_CREDENTIAL_HARDWARE_DIRECT_ACCESS = "android.hardware.identity_credential_direct_access";
    public static final String FEATURE_INPUT_METHODS = "android.software.input_methods";
    public static final String FEATURE_IPSEC_TUNNELS = "android.software.ipsec_tunnels";
    public static final String FEATURE_IPSEC_TUNNEL_MIGRATION = "android.software.ipsec_tunnel_migration";
    public static final String FEATURE_IRIS = "android.hardware.biometrics.iris";
    public static final String FEATURE_KEYSTORE_APP_ATTEST_KEY = "android.hardware.keystore.app_attest_key";
    public static final String FEATURE_KEYSTORE_LIMITED_USE_KEY = "android.hardware.keystore.limited_use_key";
    public static final String FEATURE_KEYSTORE_SINGLE_USE_KEY = "android.hardware.keystore.single_use_key";
    public static final String FEATURE_LEANBACK = "android.software.leanback";
    public static final String FEATURE_LEANBACK_ONLY = "android.software.leanback_only";
    public static final String FEATURE_LIVE_TV = "android.software.live_tv";
    public static final String FEATURE_LIVE_WALLPAPER = "android.software.live_wallpaper";
    public static final String FEATURE_LOCATION = "android.hardware.location";
    public static final String FEATURE_LOCATION_GPS = "android.hardware.location.gps";
    public static final String FEATURE_LOCATION_NETWORK = "android.hardware.location.network";
    public static final String FEATURE_MANAGED_USERS = "android.software.managed_users";
    public static final String FEATURE_MICROPHONE = "android.hardware.microphone";
    public static final String FEATURE_MIDI = "android.software.midi";
    public static final String FEATURE_NFC = "android.hardware.nfc";
    public static final String FEATURE_NFC_BEAM = "android.sofware.nfc.beam";
    public static final String FEATURE_NFC_HOST_CARD_EMULATION = "android.hardware.nfc.hce";
    public static final String FEATURE_NFC_HOST_CARD_EMULATION_NFCF = "android.hardware.nfc.hcef";
    public static final String FEATURE_NFC_OFF_HOST_CARD_EMULATION_ESE = "android.hardware.nfc.ese";
    public static final String FEATURE_NFC_OFF_HOST_CARD_EMULATION_UICC = "android.hardware.nfc.uicc";
    public static final String FEATURE_OPENGLES_DEQP_LEVEL = "android.software.opengles.deqp.level";
    public static final String FEATURE_OPENGLES_EXTENSION_PACK = "android.hardware.opengles.aep";
    public static final String FEATURE_PC = "android.hardware.type.pc";
    public static final String FEATURE_PICTURE_IN_PICTURE = "android.software.picture_in_picture";
    public static final String FEATURE_PRINTING = "android.software.print";
    public static final String FEATURE_RAM_LOW = "android.hardware.ram.low";
    public static final String FEATURE_RAM_NORMAL = "android.hardware.ram.normal";
    public static final String FEATURE_SCREEN_LANDSCAPE = "android.hardware.screen.landscape";
    public static final String FEATURE_SCREEN_PORTRAIT = "android.hardware.screen.portrait";
    public static final String FEATURE_SECURELY_REMOVES_USERS = "android.software.securely_removes_users";
    public static final String FEATURE_SECURE_LOCK_SCREEN = "android.software.secure_lock_screen";
    public static final String FEATURE_SECURITY_MODEL_COMPATIBLE = "android.hardware.security.model.compatible";
    public static final String FEATURE_SENSOR_ACCELEROMETER = "android.hardware.sensor.accelerometer";
    public static final String FEATURE_SENSOR_ACCELEROMETER_LIMITED_AXES = "android.hardware.sensor.accelerometer_limited_axes";
    public static final String FEATURE_SENSOR_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED =
            "android.hardware.sensor.accelerometer_limited_axes_uncalibrated";
    public static final String FEATURE_SENSOR_AMBIENT_TEMPERATURE = "android.hardware.sensor.ambient_temperature";
    public static final String FEATURE_SENSOR_BAROMETER = "android.hardware.sensor.barometer";
    public static final String FEATURE_SENSOR_COMPASS = "android.hardware.sensor.compass";
    public static final String FEATURE_SENSOR_DYNAMIC_HEAD_TRACKER = "android.hardware.sensor.dynamic.head_tracker";
    public static final String FEATURE_SENSOR_GYROSCOPE = "android.hardware.sensor.gyroscope";
    public static final String FEATURE_SENSOR_GYROSCOPE_LIMITED_AXES = "android.hardware.sensor.gyroscope_limited_axes";
    public static final String FEATURE_SENSOR_GYROSCOPE_LIMITED_AXES_UNCALIBRATED =
            "android.hardware.sensor.gyroscope_limited_axes_uncalibrated";
    public static final String FEATURE_SENSOR_HEADING = "android.hardware.sensor.heading";
    public static final String FEATURE_SENSOR_HEART_RATE = "android.hardware.sensor.heartrate";
    public static final String FEATURE_SENSOR_HEART_RATE_ECG = "android.hardware.sensor.heartrate.ecg";
    public static final String FEATURE_SENSOR_HINGE_ANGLE = "android.hardware.sensor.hinge_angle";
    public static final String FEATURE_SENSOR_LIGHT = "android.hardware.sensor.light";
    public static final String FEATURE_SENSOR_PROXIMITY = "android.hardware.sensor.proximity";
    public static final String FEATURE_SENSOR_RELATIVE_HUMIDITY = "android.hardware.sensor.relative_humidity";
    public static final String FEATURE_SENSOR_STEP_COUNTER = "android.hardware.sensor.stepcounter";
    public static final String FEATURE_SENSOR_STEP_DETECTOR = "android.hardware.sensor.stepdetector";
    public static final String FEATURE_SE_OMAPI_ESE = "android.hardware.se.omapi.ese";
    public static final String FEATURE_SE_OMAPI_SD = "android.hardware.se.omapi.sd";
    public static final String FEATURE_SE_OMAPI_UICC = "android.hardware.se.omapi.uicc";
    public static final String FEATURE_SIP = "android.software.sip";
    public static final String FEATURE_SIP_VOIP = "android.software.sip.voip";
    public static final String FEATURE_STRONGBOX_KEYSTORE = "android.hardware.strongbox_keystore";
    public static final String FEATURE_TELECOM = "android.software.telecom";
    public static final String FEATURE_TELEPHONY = "android.hardware.telephony";
    public static final String FEATURE_TELEPHONY_CALLING = "android.hardware.telephony.calling";
    public static final String FEATURE_TELEPHONY_CDMA = "android.hardware.telephony.cdma";
    public static final String FEATURE_TELEPHONY_DATA = "android.hardware.telephony.data";
    public static final String FEATURE_TELEPHONY_EUICC = "android.hardware.telephony.euicc";
    public static final String FEATURE_TELEPHONY_EUICC_MEP = "android.hardware.telephony.euicc.mep";
    public static final String FEATURE_TELEPHONY_GSM = "android.hardware.telephony.gsm";
    public static final String FEATURE_TELEPHONY_IMS = "android.hardware.telephony.ims";
    public static final String FEATURE_TELEPHONY_MBMS = "android.hardware.telephony.mbms";
    public static final String FEATURE_TELEPHONY_MESSAGING = "android.hardware.telephony.messaging";
    public static final String FEATURE_TELEPHONY_RADIO_ACCESS = "android.hardware.telephony.radio.access";
    public static final String FEATURE_TELEPHONY_SUBSCRIPTION = "android.hardware.telephony.subscription";
    public static final String FEATURE_TELEVISION = "android.hardware.type.television";
    public static final String FEATURE_TOUCHSCREEN = "android.hardware.touchscreen";
    public static final String FEATURE_TOUCHSCREEN_MULTITOUCH = "android.hardware.touchscreen.multitouch";
    public static final String FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT = "android.hardware.touchscreen.multitouch.distinct";
    public static final String FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND = "android.hardware.touchscreen.multitouch.jazzhand";
    public static final String FEATURE_USB_ACCESSORY = "android.hardware.usb.accessory";
    public static final String FEATURE_USB_HOST = "android.hardware.usb.host";
    public static final String FEATURE_UWB = "android.hardware.uwb";
    public static final String FEATURE_VERIFIED_BOOT = "android.software.verified_boot";
    public static final String FEATURE_VR_HEADTRACKING = "android.hardware.vr.headtracking";
    public static final String FEATURE_VR_MODE = "android.software.vr.mode";
    public static final String FEATURE_VR_MODE_HIGH_PERFORMANCE = "android.hardware.vr.high_performance";
    public static final String FEATURE_VULKAN_DEQP_LEVEL = "android.software.vulkan.deqp.level";
    public static final String FEATURE_VULKAN_HARDWARE_COMPUTE = "android.hardware.vulkan.compute";
    public static final String FEATURE_VULKAN_HARDWARE_LEVEL = "android.hardware.vulkan.level";
    public static final String FEATURE_VULKAN_HARDWARE_VERSION = "android.hardware.vulkan.version";
    public static final String FEATURE_WALLET_LOCATION_BASED_SUGGESTIONS = "android.software.wallet_location_based_suggestions";
    public static final String FEATURE_WATCH = "android.hardware.type.watch";
    public static final String FEATURE_WEBVIEW = "android.software.webview";
    public static final String FEATURE_WIFI = "android.hardware.wifi";
    public static final String FEATURE_WIFI_AWARE = "android.hardware.wifi.aware";
    public static final String FEATURE_WIFI_DIRECT = "android.hardware.wifi.direct";
    public static final String FEATURE_WIFI_PASSPOINT = "android.hardware.wifi.passpoint";
    public static final String FEATURE_WIFI_RTT = "android.hardware.wifi.rtt";
    public static final String FEATURE_WINDOW_MAGNIFICATION = "android.software.window_magnification";
    public static final String FEATURE_SOFTWARE_KEYBOARD = FEATURE_INPUT_METHODS;

    private static final String[] PRESENT_FEATURES = {
        FEATURE_TOUCHSCREEN, FEATURE_TOUCHSCREEN_MULTITOUCH, FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT,
        FEATURE_FAKETOUCH, FEATURE_SCREEN_PORTRAIT, FEATURE_SCREEN_LANDSCAPE, FEATURE_AUDIO_OUTPUT, FEATURE_INPUT_METHODS,
        FEATURE_VULKAN_HARDWARE_LEVEL, FEATURE_VULKAN_HARDWARE_VERSION, FEATURE_RAM_NORMAL,
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

    public FeatureInfo[] getSystemAvailableFeatures()
    {
        FeatureInfo[] features = new FeatureInfo[PRESENT_FEATURES.length + 1];

        for (int index = 0; index < PRESENT_FEATURES.length; index++)
        {
            FeatureInfo feature = new FeatureInfo();
            feature.name = PRESENT_FEATURES[index];
            features[index] = feature;
        }

        FeatureInfo graphics = new FeatureInfo();
        graphics.reqGlEsVersion = packGlEsVersion(runtime.PlatformConfig.GL_ES_VERSION);
        features[PRESENT_FEATURES.length] = graphics;

        return features;
    }

    private static int packGlEsVersion(String version)
    {
        int dot = version == null ? -1 : version.indexOf('.');
        if (dot <= 0) return FeatureInfo.GL_ES_VERSION_UNDEFINED;

        try
        {
            int major = Integer.parseInt(version.substring(0, dot));
            int minor = Integer.parseInt(version.substring(dot + 1));
            return (major << 16) | (minor & 0xffff);
        }
        catch (NumberFormatException malformed)
        {
            return FeatureInfo.GL_ES_VERSION_UNDEFINED;
        }
    }

    public String[] getSystemSharedLibraryNames()
    {
        return new String[0];
    }

    public boolean isSafeMode()
    {
        return false;
    }

    public boolean isInstantApp()
    {
        return false;
    }

    public boolean isInstantApp(String packageName)
    {
        return false;
    }

    public int getInstantAppCookieMaxBytes()
    {
        return 0;
    }

    public byte[] getInstantAppCookie()
    {
        return new byte[0];
    }

    public void clearInstantAppCookie()
    {
    }

    public void updateInstantAppCookie(byte[] cookie)
    {
    }

    public boolean isDeviceUpgrading()
    {
        return false;
    }

    public boolean canRequestPackageInstalls()
    {
        return false;
    }

    public boolean isPackageSuspended()
    {
        return false;
    }

    public boolean isPackageSuspended(String packageName) throws NameNotFoundException
    {
        if (!isSelf(packageName) && runtime.Packages.find(packageName) == null) throw new NameNotFoundException(packageName);

        return false;
    }

    public android.os.Bundle getSuspendedPackageAppExtras()
    {
        return null;
    }

    public boolean getSyntheticAppDetailsActivityEnabled(String packageName)
    {
        return true;
    }

    public void setMimeGroup(String mimeGroup, java.util.Set<String> mimeTypes)
    {
    }

    public java.util.Set<String> getMimeGroup(String mimeGroup)
    {
        return new java.util.HashSet<>();
    }

    public void verifyPendingInstall(int id, int verificationCode)
    {
    }

    public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay)
    {
    }

    private PackageInstaller mPackageInstaller;

    public PackageInstaller getPackageInstaller()
    {
        if (mPackageInstaller == null) mPackageInstaller = new PackageInstaller();

        return mPackageInstaller;
    }

    public interface OnChecksumsReadyListener
    {
        void onChecksumsReady(List<ApkChecksum> checksums);
    }

    public static final class ComponentEnabledSetting implements android.os.Parcelable
    {
        public static final android.os.Parcelable.Creator<ComponentEnabledSetting> CREATOR =
                new android.os.Parcelable.Creator<ComponentEnabledSetting>()
        {
            @Override
            public ComponentEnabledSetting createFromParcel(android.os.Parcel source)
            {
                Object value = source == null ? null : source.readValue(null);
                return value instanceof ComponentEnabledSetting ? (ComponentEnabledSetting)value
                                                                : new ComponentEnabledSetting(null, COMPONENT_ENABLED_STATE_DEFAULT, 0);
            }

            @Override
            public ComponentEnabledSetting[] newArray(int size)
            {
                return new ComponentEnabledSetting[size];
            }
        };

        private final ComponentName mComponentName;
        private final int mEnabledState;
        private final int mEnabledFlags;

        public ComponentEnabledSetting(ComponentName componentName, int newState, int flags)
        {
            mComponentName = componentName;
            mEnabledState = newState;
            mEnabledFlags = flags;
        }

        public ComponentName getComponentName()
        {
            return mComponentName;
        }

        public int getEnabledState()
        {
            return mEnabledState;
        }

        public int getEnabledFlags()
        {
            return mEnabledFlags;
        }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags)
        {
            dest.writeValue(this);
        }

        @Override
        public int describeContents()
        {
            return 0;
        }
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

    public static final class ComponentInfoFlags
    {
        private final long mValue;

        private ComponentInfoFlags(long value)
        {
            mValue = value;
        }

        public long getValue()
        {
            return mValue;
        }

        public static ComponentInfoFlags of(long value)
        {
            return new ComponentInfoFlags(value);
        }
    }

    public static final class ResolveInfoFlags
    {
        private final long mValue;

        private ResolveInfoFlags(long value)
        {
            mValue = value;
        }

        public long getValue()
        {
            return mValue;
        }

        public static ResolveInfoFlags of(long value)
        {
            return new ResolveInfoFlags(value);
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
