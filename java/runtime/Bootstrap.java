package runtime;

import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.ContextImpl;
import android.app.Instrumentation;
import android.content.ContentProvider;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewRootImpl;

public final class Bootstrap
{
    private static final String TAG = "Bootstrap";

    public static final int OK = 0;
    public static final int FACTORY_FAILED = 1;
    public static final int APPLICATION_FAILED = 2;
    public static final int PROVIDER_FAILED = 3;
    public static final int ON_CREATE_FAILED = 4;
    public static final int WINDOW_FAILED = 5;
    public static final int ACTIVITY_FAILED = 6;
    public static final int NO_FRAMES = 7;

    private static Application sApplication;
    private static Activity sActivity;

    private Bootstrap()
    {
    }

    public static Application getApplication()
    {
        return sApplication;
    }

    public static int start(String dataDir, String nativeLibraryDir)
    {
        ClassLoader loader = Bootstrap.class.getClassLoader();

        TextMeasurers.install(new NativeTextMeasurer());

        if (Looper.getMainLooper() == null) Looper.prepareMainLooper();

        SurfaceBridge.prepareLooper();

        ApplicationInfo info = new ApplicationInfo();
        info.packageName = nativePackageName();
        info.name = info.packageName;
        info.className = nativeApplicationClassName();
        info.appComponentFactory = nativeAppComponentFactory();
        info.sourceDir = nativeSourceDir();
        info.publicSourceDir = info.sourceDir;
        info.uid = android.os.Process.myUid();
        info.processName = info.packageName;

        int splitCount = Math.max(nativeSplitCount(), 0);
        if (splitCount > 0)
        {
            info.splitNames = new String[splitCount];
            info.splitSourceDirs = new String[splitCount];

            for (int index = 0; index < splitCount; index++)
            {
                info.splitNames[index] = nativeSplitName(index);
                info.splitSourceDirs[index] = nativeSplitPath(index);
            }

            info.splitPublicSourceDirs = info.splitSourceDirs;
            Log.i(TAG, "the package is installed as a base plus " + splitCount + " split(s)");
        }

        info.dataDir = dataDir;
        info.nativeLibraryDir = nativeLibraryDir;
        info.nativeLibraryRootDir = nativeLibraryDir == null ? null : new java.io.File(nativeLibraryDir).getParent();
        info.nativeLibraryRootRequiresIsa = true;
        info.primaryCpuAbi = "arm64-v8a";
        info.credentialProtectedDataDir = dataDir;
        info.scanSourceDir = info.sourceDir;
        info.scanPublicSourceDir = info.publicSourceDir;
        info.seInfo = "default:targetSdkVersion=" + nativeTargetSdkVersion();
        info.minSdkVersion = nativeMinSdkVersion();
        info.targetSdkVersion = nativeTargetSdkVersion();
        if (nativeDebuggable()) info.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        if (nativeExtractNativeLibs()) info.flags |= ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS;

        java.io.File dataRoot = new java.io.File(dataDir);

        String externalStorage = nativeExternalStorageDir();
        java.io.File externalRoot =
                externalStorage != null && !externalStorage.isEmpty() ? new java.io.File(externalStorage)
                                                                      : new java.io.File(dataRoot, "external");

        android.os.Environment.setDirectories(dataRoot.getParentFile(), dataRoot, externalRoot);

        Log.i(TAG, "storage: data=" + dataRoot + " external=" + externalRoot);

        Permissions.initialize(dataDir);

        android.security.keystore.AndroidKeyStoreProvider.install(dataRoot);

        PackageManager packageManager = new PackageManager(info);
        Resources resources = new Resources(nativeAssetsHandle());
        info.metaData = MetaData.read(resources, MetaData.APPLICATION, 0);
        if (info.metaData != null) Log.i(TAG, "application meta-data: " + info.metaData.size() + " entries");

        AppComponentFactory factory;
        try
        {
            factory = createFactory(loader, info.appComponentFactory);
            Log.i(TAG, "factory: " + factory.getClass().getName());
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "could not instantiate AppComponentFactory " + info.appComponentFactory, cause);
            return FACTORY_FAILED;
        }

        try
        {
            ClassLoader chosen = factory.instantiateClassLoader(loader, info);
            if (chosen != null && chosen != loader)
            {
                Log.i(TAG, "the component factory supplied the class loader " + chosen.getClass().getName());
                loader = chosen;
            }
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the component factory could not supply a class loader, so the default one is kept", cause);
        }

        Thread.currentThread().setContextClassLoader(loader);

        ContextImpl base = new ContextImpl(info, packageManager, resources, loader, dataDir);
        android.app.ActivityThread.attachRuntime(base);

        Application application;
        try
        {
            application = factory.instantiateApplication(loader, info.className);
            application.attach(base);
            base.setApplicationContext(application);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "could not instantiate Application " + info.className, cause);
            return APPLICATION_FAILED;
        }

        sApplication = application;

        describeComponents();

        Services.attachRuntime(base, application, factory, loader, base.getPackageName());
        Broadcasts.attachRuntime(base, factory, loader, base.getPackageName());
        Jobs.attachRuntime(base, application, factory, loader);

        int providerCount = nativeProviderCount();
        ProviderInfo[] providers = new ProviderInfo[providerCount];
        for (int index = 0; index < providerCount; index++)
        {
            providers[index] = readProviderInfo(info, resources, index);
            packageManager.addProvider(providers[index]);
        }

        for (ProviderInfo provider : providers)
        {
            int setting = packageManager.getComponentEnabledSetting(new android.content.ComponentName(provider.packageName, provider.name));
            boolean enabled = setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                              (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && provider.enabled);

            if (!enabled)
            {
                Log.i(TAG, "provider skipped, it is disabled: " + provider.name);
                continue;
            }

            if (!runsInThisProcess(info, provider))
            {
                Log.i(TAG, "provider skipped, it runs in process " + provider.processName + ": " + provider.name);
                continue;
            }

            try
            {
                ContentProvider instance = factory.instantiateProvider(loader, provider.name);
                instance.attachInfo(application, provider);
                Log.i(TAG, "provider ready: " + provider.name + " authority=" + provider.authority);
            }
            catch (Throwable cause)
            {
                Log.e(TAG, "provider failed: " + provider.name, cause);
                return PROVIDER_FAILED;
            }
        }

        StartupProbe.reportInitializers(application, providers);

        Instrumentation instrumentation = new Instrumentation();

        try
        {
            instrumentation.callApplicationOnCreate(application);
            Log.i(TAG, "Application.onCreate returned: " + info.className);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "Application.onCreate failed", cause);
            return ON_CREATE_FAILED;
        }

        return launchActivity(loader, factory, instrumentation, base, application);
    }

    private static int launchActivity(ClassLoader loader, AppComponentFactory factory, Instrumentation instrumentation, ContextImpl base,
                                      Application application)
    {
        String requested = Settings.get("activity");
        String className = requested != null ? requested : nativeLauncherActivity();

        if (className == null || className.isEmpty())
        {
            Log.i(TAG, "no launcher activity in the manifest; stopping after Application.onCreate");
            return OK;
        }

        if (requested != null) Log.i(TAG, "activity selected by --activity: " + className);

        if ("0".equals(Settings.get("launch-activity")))
        {
            Log.i(TAG, "activity launch disabled; stopping after Application.onCreate");
            return OK;
        }

        int width = Settings.integer("window-width", 1080);
        int height = Settings.integer("window-height", 2400);
        int frameLimit = Settings.integer("max-frames", 0);

        WindowBridge window = WindowBridge.open(width, height, application.getPackageName());
        if (window == null)
        {
            Log.e(TAG, "no window: a compositor is required to start " + className);
            return WINDOW_FAILED;
        }

        int theme = nativeActivityTheme(className);
        int densityDpi = intProperty(android.view.WindowManagerImpl.DENSITY_SETTING, android.util.DisplayMetrics.DENSITY_DEFAULT);
        base.updateMetricsForWindow(width, height, densityDpi);
        Configurations.attach(base);

        Log.i(TAG, "display: " + width + "x" + height + " at " + densityDpi + " dpi, "
                   + base.getResources().getConfiguration().screenWidthDp + "x"
                   + base.getResources().getConfiguration().screenHeightDp + " dp, "
                   + WindowBridge.refreshRate() + " Hz");

        android.content.pm.ActivityInfo activityInfo = new android.content.pm.ActivityInfo();
        activityInfo.name = className;
        activityInfo.packageName = base.getPackageName();
        activityInfo.applicationInfo = base.getApplicationInfo();
        activityInfo.theme = theme;

        int activityCount = nativeComponentCount(MetaData.ACTIVITY);
        for (int index = 0; index < activityCount; index++)
        {
            if (!className.equals(nativeComponentClassName(MetaData.ACTIVITY, index))) continue;

            activityInfo.metaData = MetaData.read(base.getResources(), MetaData.ACTIVITY, index);
            break;
        }

        base.getPackageManager().addActivity(activityInfo);
        if (theme != 0)
        {
            base.setTheme(theme);
            Log.i(TAG, "activity theme: 0x" + Integer.toHexString(theme));
        }
        else
        {
            Log.w(TAG, "no android:theme on the manifest's application or activity");
        }

        Activity activity;
        try
        {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new android.content.ComponentName(base.getPackageName(), className));

            activity = factory.instantiateActivity(loader, className, intent);
            activity.attach(base, application);
            activity.setIntent(intent);
            sActivity = activity;
            Activities.attachRuntime(base, application, factory, loader, instrumentation, base.getPackageName());
            Activities.attachDataDir(base.getApplicationInfo().dataDir);
            Activities.adoptLaunchedActivity(activity);
            Log.i(TAG, "activity: " + activity.getClass().getName());
            Log.i(TAG, "activity dirs: files=" + activity.getFilesDir() + " cache=" + activity.getCacheDir() + " obb="
                           + activity.getObbDir() + " external=" + activity.getExternalFilesDir(null));
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "could not instantiate Activity " + className, cause);
            window.close();
            return ACTIVITY_FAILED;
        }

        android.os.Bundle restored = Activities.restoreInstanceState(className);

        try
        {
            instrumentation.callActivityOnCreate(activity, restored);
            instrumentation.callActivityOnPostCreate(activity, restored);
            if (restored != null) instrumentation.callActivityOnRestoreInstanceState(activity, restored);

            if (activity.isFinishing())
            {
                Log.i(TAG, activity.getClass().getName() + " finished while it was being created, so it is never shown");
            }
            else if (Activities.current() != activity)
            {
                Log.i(TAG, activity.getClass().getName() + " started " + Activities.current().getClass().getName()
                               + " while it was being created, so it stays behind it until that one finishes");
            }
            else
            {
                instrumentation.callActivityOnStart(activity);
                instrumentation.callActivityOnResume(activity);
                if (activity instanceof android.app.NativeActivity) ((android.app.NativeActivity)activity).attachWindow(width, height);
            }
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "Activity.onCreate failed", cause);
            window.close();
            return ACTIVITY_FAILED;
        }

        if (activity instanceof android.app.NativeActivity)
        {
            return runGuestPresentedLoop((android.app.NativeActivity)activity, instrumentation, window, base);
        }

        View decor = activity.getWindow().getDecorView();
        final ViewRootImpl root = new ViewRootImpl(decor, window, frameLimit);
        Activities.attachViewRoot(root);

        root.setSystemBarInsets(intProperty("RUNTIME_STATUS_BAR_HEIGHT", 0), intProperty("RUNTIME_NAVIGATION_BAR_HEIGHT", 0));

        final int windowDensityDpi = densityDpi;
        window.setResizeListener(new WindowBridge.ResizeListener()
        {
            @Override
            public void onWindowResized(int newWidth, int newHeight)
            {
                root.onWindowResized(newWidth, newHeight);
                Configurations.apply(newWidth, newHeight, windowDensityDpi);
            }
        });

        final Activity inputTarget = activity;
        sDescribedInputState = !Settings.tracing("input");

        if (Settings.tracing("input"))
        {
            String[] wanted = {"onTouchEvent", "dispatchTouchEvent", "dispatchGenericMotionEvent", "onGenericMotionEvent"};
            for (String name : wanted)
            {
                try
                {
                    java.lang.reflect.Method found = inputTarget.getClass().getMethod(name, android.view.MotionEvent.class);
                    Log.i(TAG, "input: " + name + " is declared by " + found.getDeclaringClass().getName());
                }
                catch (Throwable cause)
                {
                    Log.i(TAG, "input: " + name + " is not present: " + cause);
                }
            }

            for (Class<?> level = inputTarget.getClass(); level != null && !level.getName().startsWith("android.app.");
                 level = level.getSuperclass())
            {
                for (java.lang.reflect.Field field : level.getDeclaredFields())
                {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;

                    try
                    {
                        field.setAccessible(true);
                        Object value = field.get(inputTarget);
                        Log.i(TAG, "input: " + level.getSimpleName() + "." + field.getName() + " = " + value);
                    }
                    catch (Throwable ignored)
                    {
                    }
                }
            }
        }
        window.setInputSink(new WindowBridge.InputSink()
        {
            @Override
            public void onTouch(android.view.MotionEvent event)
            {
                try
                {
                    int action = event.getActionMasked();

                    if (action == android.view.MotionEvent.ACTION_HOVER_ENTER
                        || action == android.view.MotionEvent.ACTION_HOVER_MOVE
                        || action == android.view.MotionEvent.ACTION_HOVER_EXIT
                        || action == android.view.MotionEvent.ACTION_SCROLL)
                    {
                        Activity hoverTarget = Activities.current();
                        windowCallbackOf(hoverTarget != null ? hoverTarget : inputTarget).dispatchGenericMotionEvent(event);
                        return;
                    }

                    if (root.dispatchPanelTouch(event)) return;

                    Activity touchTarget = Activities.current();
                    if (touchTarget == null) touchTarget = inputTarget;

                    if (action == android.view.MotionEvent.ACTION_DOWN) describeInputState(touchTarget);
                    windowCallbackOf(touchTarget).dispatchTouchEvent(event);
                }
                catch (Throwable cause)
                {
                    Log.e(TAG, "the activity threw while handling a touch", cause);
                }
            }

            @Override
            public void onKey(android.view.KeyEvent event)
            {
                try
                {
                    Activity target = Activities.current();
                    if (target == null) target = inputTarget;

                    if (android.view.inputmethod.InputMethodManager.getInstance().dispatchKeyEvent(event)) return;
                    if (root.dispatchPanelKey(event)) return;
                    if (windowCallbackOf(target).dispatchKeyEvent(event)) return;

                    if (event.getKeyCode() != android.view.KeyEvent.KEYCODE_BACK) return;
                    if (event.getAction() != android.view.KeyEvent.ACTION_UP) return;

                    Log.i(TAG, "back was pressed, so " + target.getClass().getName() + " is asked to handle it");
                    target.onBackPressed();
                }
                catch (Throwable cause)
                {
                    Log.e(TAG, "the activity threw while handling a key", cause);
                }
            }
        });

        scheduleScriptedReconfigure();

        Log.i(TAG, "entering the main loop, frameLimit=" + frameLimit);

        final Activity focusTarget = activity;
        root.setFirstFrameCallback(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    focusTarget.onWindowFocusChanged(true);
                }
                catch (Throwable cause)
                {
                    Log.e(TAG, "the activity did not accept window focus", cause);
                }
            }
        });

        root.start();
        if (Looper.getMainLooper() != null) Looper.loop();
        Log.i(TAG, "main loop finished after " + root.getFramesDrawn() + " frames");

        try
        {
            Log.i(TAG, "unwinding the activity stack");
            Activities.shutdown();
            Log.i(TAG, "the activity is destroyed");
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity did not shut down cleanly", cause);
        }

        window.close();
        base.getResources().releaseThemes();
        return root.getFramesDrawn() > 0 ? OK : NO_FRAMES;
    }

    private static boolean sDescribedInputState = true;

    private static void scheduleScriptedReconfigure()
    {
        String script = Settings.get("reconfigure");
        if (script == null || script.isEmpty()) return;

        for (String entry : script.split(";"))
        {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;

            int colon = trimmed.indexOf(':');
            int cross = trimmed.indexOf('x', colon + 1);
            int at = trimmed.indexOf('@', cross + 1);

            if (colon < 0 || cross < 0 || at < 0)
            {
                Log.w(TAG, "a scripted reconfigure could not be read: " + trimmed + " (want <millis>:<width>x<height>@<dpi>)");
                continue;
            }

            try
            {
                long delay = Long.parseLong(trimmed.substring(0, colon).trim());
                final int width = Integer.parseInt(trimmed.substring(colon + 1, cross).trim());
                final int height = Integer.parseInt(trimmed.substring(cross + 1, at).trim());
                final int densityDpi = Integer.parseInt(trimmed.substring(at + 1).trim());

                new android.os.Handler(Looper.getMainLooper()).postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.i(TAG, "scripted reconfigure to " + width + "x" + height + " at " + densityDpi + " dpi");
                        Configurations.apply(width, height, densityDpi);
                    }
                }, delay);
            }
            catch (NumberFormatException ignored)
            {
                Log.w(TAG, "a scripted reconfigure could not be read: " + trimmed);
            }
        }
    }

    private static void describeInputState(Activity activity)
    {
        if (sDescribedInputState) return;
        sDescribedInputState = true;

        String[] wanted = {"mCurSurfaceHolder", "mDestroyed", "mNativeHandle", "mLastContentWidth", "mLastContentHeight",
                           "mSurfaceView"};

        for (Class<?> level = activity.getClass(); level != null && !level.getName().startsWith("android.app.");
             level = level.getSuperclass())
        {
            for (String name : wanted)
            {
                try
                {
                    java.lang.reflect.Field field = level.getDeclaredField(name);
                    field.setAccessible(true);
                    Log.i(TAG, "at touch: " + level.getSimpleName() + "." + name + " = " + field.get(activity));
                }
                catch (Throwable ignored)
                {
                }
            }
        }
    }

    private static int runGuestPresentedLoop(android.app.NativeActivity activity, Instrumentation instrumentation, WindowBridge window,
                                             ContextImpl base)
    {
        long deadlineNanos = System.nanoTime() + intProperty("RUNTIME_GUEST_TIMEOUT_SECONDS", 10) * 1000000000L;
        Log.i(TAG, "the guest owns presentation; the framework will not draw to this window");

        boolean finished = false;
        while (!window.isCloseRequested())
        {
            window.poll(16);

            if (activity.isFinishRequested())
            {
                finished = true;
                break;
            }

            if (System.nanoTime() > deadlineNanos)
            {
                Log.e(TAG, "the guest never finished presenting");
                break;
            }
        }

        Log.i(TAG, finished ? "the guest finished presenting" : "the window closed before the guest finished");

        try
        {
            activity.detachWindow();
            instrumentation.callActivityOnPause(activity);
            instrumentation.callActivityOnStop(activity);
            instrumentation.callActivityOnDestroy(activity);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the native activity did not shut down cleanly", cause);
            window.close();
            base.getResources().releaseThemes();
            return ACTIVITY_FAILED;
        }

        window.close();
        base.getResources().releaseThemes();
        return finished ? OK : NO_FRAMES;
    }

    private static android.view.Window.Callback windowCallbackOf(Activity activity)
    {
        android.view.Window window = activity.getWindow();
        android.view.Window.Callback callback = window == null ? null : window.getCallback();

        return callback != null ? callback : activity;
    }

    private static int intProperty(String name, int fallback)
    {
        String value = Settings.get(name);
        if (value == null) return fallback;

        int parsed;
        try
        {
            parsed = Integer.parseInt(value.trim());
        }
        catch (NumberFormatException malformed)
        {
            throw new IllegalArgumentException(name + " is not an integer: '" + value + "'", malformed);
        }

        if (parsed < 0) throw new IllegalArgumentException(name + " must not be negative: '" + value + "'");
        return parsed;
    }

    private static AppComponentFactory createFactory(ClassLoader loader, String className) throws Exception
    {
        if (className == null || className.isEmpty()) return AppComponentFactory.DEFAULT;
        return (AppComponentFactory)loader.loadClass(className).getDeclaredConstructor().newInstance();
    }

    private static String processNameFor(ApplicationInfo applicationInfo, String declared)
    {
        String main = applicationInfo.processName != null ? applicationInfo.processName : applicationInfo.packageName;
        if (declared == null || declared.isEmpty()) return main;
        if (declared.startsWith(":")) return applicationInfo.packageName + declared;
        return declared;
    }

    private static boolean runsInThisProcess(ApplicationInfo applicationInfo, ProviderInfo provider)
    {
        String main = applicationInfo.processName != null ? applicationInfo.processName : applicationInfo.packageName;
        return provider.processName == null || provider.processName.equals(main);
    }

    private static ProviderInfo readProviderInfo(ApplicationInfo applicationInfo, Resources resources, int index)
    {
        ProviderInfo provider = new ProviderInfo();
        provider.applicationInfo = applicationInfo;
        provider.packageName = applicationInfo.packageName;
        provider.processName = processNameFor(applicationInfo, nativeProviderProcess(index));
        provider.enabled = nativeProviderEnabled(index);
        provider.name = nativeProviderClassName(index);
        provider.authority = nativeProviderAuthority(index);
        provider.directBootAware = nativeProviderDirectBootAware(index);
        provider.grantUriPermissions = nativeProviderGrantUriPermissions(index);

        provider.metaData = MetaData.read(resources, MetaData.PROVIDER, index);

        return provider;
    }

    private static native String nativePackageName();

    private static native String nativeApplicationClassName();

    private static native String nativeAppComponentFactory();

    private static void describeComponents()
    {
        int[] kinds = {Components.ACTIVITY, Components.SERVICE, Components.RECEIVER};

        for (int kind : kinds)
        {
            for (Components.Record record : Components.declared(kind))
            {
                StringBuilder described = new StringBuilder();
                for (android.content.IntentFilter filter : record.filters)
                {
                    for (int index = 0; index < filter.countActions(); index++)
                    {
                        described.append(described.length() == 0 ? " answers " : ", ").append(filter.getAction(index));
                    }
                }

                Log.i(TAG, "declared " + describeKind(kind) + " " + record.className + described);
            }
        }
    }

    private static String describeKind(int kind)
    {
        if (kind == Components.SERVICE) return "service";
        if (kind == Components.RECEIVER) return "receiver";

        return "activity";
    }

    static native int nativeComponentCount(int kind);

    static native int nativePermissionCount();

    static native String nativePermissionAt(int index);

    static native int nativeActivityLaunchMode(String className);

    static native int nativeActivityConfigChanges(String className);

    public static native long nativeVersionCode();

    public static native String nativeVersionName();

    public static native int nativeSignatureCount();

    public static native byte[] nativeSignatureAt(int index);

    public static native int nativeSignatureScheme();

    public static native boolean nativeSignatureVerified();

    static native int nativeSplitCount();

    static native String nativeSplitName(int index);

    static native String nativeSplitPath(int index);

    static native String nativeComponentClassName(int kind, int index);

    static native int nativeComponentFilterCount(int kind, int index);

    static native int nativeComponentMetaDataCount(int kind, int index);

    static native String nativeComponentMetaDataName(int kind, int index, int entry);

    static native String nativeComponentMetaDataValue(int kind, int index, int entry);

    static native int nativeComponentMetaDataType(int kind, int index, int entry);

    static native int nativeComponentMetaDataData(int kind, int index, int entry);

    static native String nativeFilterActions(int kind, int index, int filterIndex);

    static native String nativeFilterCategories(int kind, int index, int filterIndex);

    static native String nativeFilterSchemes(int kind, int index, int filterIndex);

    static native String nativeFilterTypes(int kind, int index, int filterIndex);

    static native int nativeFilterPriority(int kind, int index, int filterIndex);

    private static native String nativeSourceDir();

    private static native String nativeExternalStorageDir();

    private static native String nativeLauncherActivity();

    private static native int nativeActivityTheme(String className);

    private static native int nativeMinSdkVersion();

    private static native int nativeTargetSdkVersion();

    private static native boolean nativeDebuggable();

    private static native boolean nativeExtractNativeLibs();

    private static native long nativeAssetsHandle();

    private static native int nativeProviderCount();

    private static native String nativeProviderClassName(int index);

    private static native String nativeProviderAuthority(int index);

    private static native boolean nativeProviderDirectBootAware(int index);

    private static native boolean nativeProviderEnabled(int index);

    private static native String nativeProviderProcess(int index);

    private static native boolean nativeProviderGrantUriPermissions(int index);

    private static native int nativeProviderMetaDataCount(int index);

    private static native String nativeProviderMetaDataName(int index, int entry);

    private static native String nativeProviderMetaDataValue(int index, int entry);

    private static native int nativeActivityMetaDataCount(String className);

    private static native String nativeActivityMetaDataName(String className, int entry);

    private static native String nativeActivityMetaDataValue(String className, int entry);
}
