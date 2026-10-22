package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ServiceManager;
import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;

public final class ActivityThread
{
    public static final String TAG = "ActivityThread";

    public static final class ActivityClientRecord
    {
        public IBinder token;
        public Activity activity;
        public android.content.Intent intent;
        public String embeddedID;
        public Bundle state;
        public boolean paused;
        public boolean stopped;
        public boolean hideForNow;
        public android.content.pm.ActivityInfo activityInfo;

        ActivityClientRecord(IBinder token, Activity activity)
        {
            this.token = token;
            this.activity = activity;
            this.intent = activity.getIntent();
            this.paused = true;
        }

        public boolean isPaused()
        {
            return paused;
        }

        public boolean isStopped()
        {
            return stopped;
        }

        @Override
        public String toString()
        {
            return "ActivityRecord{" + Integer.toHexString(System.identityHashCode(this)) + " token=" + token + " "
                   + (activity == null ? "no component name" : activity.getClass().getName()) + "}";
        }
    }

    final class H extends Handler
    {
        H(Looper looper)
        {
            super(looper);
        }
    }

    private final class ApplicationThread extends IApplicationThread.Stub
    {
    }

    private static volatile ActivityThread sCurrentActivityThread;
    private static volatile IPackageManager sPackageManager;

    final ApplicationThread mAppThread = new ApplicationThread();
    final Looper mLooper;
    final H mH;
    final ArrayMap<IBinder, ActivityClientRecord> mActivities = new ArrayMap<>();
    final ArrayList<Application> mAllApplications = new ArrayList<>();
    final ResourcesManager mResourcesManager = ResourcesManager.getInstance();
    final Instrumentation mInstrumentation = new Instrumentation();

    Application mInitialApplication;
    ContextImpl mSystemContext;
    Configuration mConfiguration;

    private final ArrayMap<Activity, IBinder> mTokens = new ArrayMap<>();
    private ContextImpl mBaseContext;

    private ActivityThread()
    {
        Looper main = Looper.getMainLooper();
        mLooper = main == null ? Looper.myLooper() : main;
        mH = new H(mLooper);
    }

    public static ActivityThread currentActivityThread()
    {
        ActivityThread thread = sCurrentActivityThread;
        if (thread != null) return thread;

        synchronized (ActivityThread.class)
        {
            if (sCurrentActivityThread == null) sCurrentActivityThread = new ActivityThread();
            return sCurrentActivityThread;
        }
    }

    public static void attachRuntime(ContextImpl base)
    {
        ActivityThread thread = currentActivityThread();
        ApplicationStartInfo.recordTimestamp(ApplicationStartInfo.START_TIMESTAMP_BIND_APPLICATION,
                                             android.os.SystemClock.elapsedRealtimeNanos());

        synchronized (thread)
        {
            thread.mBaseContext = base;
            thread.mSystemContext = base;
            thread.mConfiguration = base.getResources().getConfiguration();
        }

        ResourcesManager.getInstance().attach(base.getResources());
        ServiceManager.addService("package", new PackageManagerBinder(base));
    }

    public static boolean isSystem()
    {
        return false;
    }

    public static String currentOpPackageName()
    {
        return currentPackageName();
    }

    public static String currentPackageName()
    {
        ContextImpl base = currentActivityThread().mBaseContext;
        return base == null ? null : base.getPackageName();
    }

    public static String currentProcessName()
    {
        String name = currentActivityThread().getProcessName();
        return name != null ? name : android.os.Process.myProcessName();
    }

    public static Application currentApplication()
    {
        return currentActivityThread().getApplication();
    }

    public static IPackageManager getPackageManager()
    {
        IPackageManager manager = sPackageManager;
        if (manager != null) return manager;

        manager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        sPackageManager = manager;
        return manager;
    }

    public static ActivityThread systemMain()
    {
        return currentActivityThread();
    }

    public Application getApplication()
    {
        Application application = runtime.Bootstrap.getApplication();

        synchronized (this)
        {
            if (application != null && mInitialApplication != application)
            {
                mInitialApplication = application;
                if (!mAllApplications.contains(application)) mAllApplications.add(application);
            }

            return mInitialApplication;
        }
    }

    public String getProcessName()
    {
        ContextImpl base = mBaseContext;
        if (base == null) return null;

        ApplicationInfo info = base.getApplicationInfo();
        return info == null || info.processName == null ? base.getPackageName() : info.processName;
    }

    public ContextImpl getSystemContext()
    {
        return mSystemContext;
    }

    public ContextImpl getSystemUiContext()
    {
        return mSystemContext;
    }

    public Looper getLooper()
    {
        return mLooper;
    }

    public Handler getHandler()
    {
        return mH;
    }

    public java.util.concurrent.Executor getExecutor()
    {
        return new HandlerExecutor(mH);
    }

    public IApplicationThread getApplicationThread()
    {
        return mAppThread;
    }

    public Instrumentation getInstrumentation()
    {
        return mInstrumentation;
    }

    public Configuration getConfiguration()
    {
        ContextImpl base = mBaseContext;
        return base == null ? mConfiguration : base.getResources().getConfiguration();
    }

    public boolean isProfiling()
    {
        return false;
    }

    public String getProfileFilePath()
    {
        return null;
    }

    public int getIntCoreSetting(String key, int defaultValue)
    {
        return defaultValue;
    }

    public String getStringCoreSetting(String key, String defaultValue)
    {
        return defaultValue;
    }

    public Bundle getCoreSettings()
    {
        return new Bundle();
    }

    public final Activity getActivity(IBinder token)
    {
        synchronized (mActivities)
        {
            ActivityClientRecord record = mActivities.get(token);
            return record == null ? null : record.activity;
        }
    }

    public ActivityClientRecord getActivityClient(IBinder token)
    {
        synchronized (mActivities)
        {
            return mActivities.get(token);
        }
    }

    public final Context getContextForActivity(IBinder token)
    {
        return getActivity(token);
    }

    public Handler getMainThreadHandler()
    {
        return mH;
    }

    void activityCreated(Activity activity)
    {
        synchronized (mActivities)
        {
            if (mTokens.containsKey(activity)) return;

            IBinder token = new Binder(activity.getClass().getName());
            mTokens.put(activity, token);
            mActivities.put(token, new ActivityClientRecord(token, activity));
        }
    }

    void activityStateChanged(Activity activity, boolean paused, boolean stopped)
    {
        synchronized (mActivities)
        {
            IBinder token = mTokens.get(activity);
            ActivityClientRecord record = token == null ? null : mActivities.get(token);
            if (record == null) return;

            record.paused = paused;
            record.stopped = stopped;
        }
    }

    void activityDestroyed(Activity activity)
    {
        synchronized (mActivities)
        {
            IBinder token = mTokens.remove(activity);
            if (token != null) mActivities.remove(token);
        }
    }

    IBinder tokenOf(Activity activity)
    {
        synchronized (mActivities)
        {
            return mTokens.get(activity);
        }
    }

    private static final class HandlerExecutor implements java.util.concurrent.Executor
    {
        private final Handler mHandler;

        HandlerExecutor(Handler handler)
        {
            mHandler = handler;
        }

        @Override
        public void execute(Runnable command)
        {
            if (!mHandler.post(command)) throw new java.util.concurrent.RejectedExecutionException(mHandler + " is shutting down");
        }
    }

    private static final class PackageManagerBinder extends IPackageManager.Stub
    {
        private final ContextImpl mContext;

        PackageManagerBinder(ContextImpl context)
        {
            mContext = context;
        }

        private PackageManager manager()
        {
            return mContext.getPackageManager();
        }

        @Override
        public PackageInfo getPackageInfo(String packageName, long flags, int userId)
        {
            try
            {
                return manager().getPackageInfo(packageName, (int)flags);
            }
            catch (PackageManager.NameNotFoundException missing)
            {
                return null;
            }
        }

        @Override
        public PackageInfo getPackageInfo(String packageName, int flags, int userId)
        {
            return getPackageInfo(packageName, (long)flags, userId);
        }

        @Override
        public ApplicationInfo getApplicationInfo(String packageName, long flags, int userId)
        {
            try
            {
                return manager().getApplicationInfo(packageName, (int)flags);
            }
            catch (PackageManager.NameNotFoundException missing)
            {
                return null;
            }
        }

        @Override
        public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId)
        {
            return getApplicationInfo(packageName, (long)flags, userId);
        }

        @Override
        public ActivityInfo getActivityInfo(ComponentName className, long flags, int userId)
        {
            try
            {
                return manager().getActivityInfo(className, (int)flags);
            }
            catch (PackageManager.NameNotFoundException missing)
            {
                return null;
            }
        }

        @Override
        public ActivityInfo getReceiverInfo(ComponentName className, long flags, int userId)
        {
            try
            {
                return manager().getReceiverInfo(className, (int)flags);
            }
            catch (PackageManager.NameNotFoundException missing)
            {
                return null;
            }
        }

        @Override
        public ServiceInfo getServiceInfo(ComponentName className, long flags, int userId)
        {
            try
            {
                return manager().getServiceInfo(className, (int)flags);
            }
            catch (PackageManager.NameNotFoundException missing)
            {
                return null;
            }
        }

        @Override
        public ProviderInfo getProviderInfo(ComponentName className, long flags, int userId)
        {
            try
            {
                return manager().getProviderInfo(className, (int)flags);
            }
            catch (PackageManager.NameNotFoundException missing)
            {
                return null;
            }
        }

        @Override
        public int getPackageUid(String packageName, long flags, int userId)
        {
            try
            {
                return manager().getPackageUid(packageName, (int)flags);
            }
            catch (PackageManager.NameNotFoundException missing)
            {
                return -1;
            }
        }

        @Override
        public String[] getPackagesForUid(int uid)
        {
            return manager().getPackagesForUid(uid);
        }

        @Override
        public String getNameForUid(int uid)
        {
            return manager().getNameForUid(uid);
        }

        @Override
        public String getInstallerPackageName(String packageName)
        {
            return manager().getInstallerPackageName(packageName);
        }

        @Override
        public int checkPermission(String permissionName, String packageName, int userId)
        {
            return manager().checkPermission(permissionName, packageName);
        }

        @Override
        public int checkUidPermission(String permissionName, int uid)
        {
            String[] packages = manager().getPackagesForUid(uid);
            if (packages == null || packages.length == 0) return PackageManager.PERMISSION_DENIED;

            return manager().checkPermission(permissionName, packages[0]);
        }

        @Override
        public int checkSignatures(String firstPackage, String secondPackage, int userId)
        {
            return manager().checkSignatures(firstPackage, secondPackage);
        }

        @Override
        public boolean hasSystemFeature(String name, int version)
        {
            return manager().hasSystemFeature(name, version);
        }

        @Override
        public boolean isSafeMode()
        {
            return manager().isSafeMode();
        }

        @Override
        public boolean isPackageAvailable(String packageName, int userId)
        {
            return getApplicationInfo(packageName, 0L, userId) != null;
        }

        @Override
        public ResolveInfo resolveIntent(Intent intent, String resolvedType, long flags, int userId)
        {
            return manager().resolveActivity(intent, (int)flags);
        }

        @Override
        public ResolveInfo resolveService(Intent intent, String resolvedType, long flags, int userId)
        {
            return manager().resolveService(intent, (int)flags);
        }

        @Override
        public ProviderInfo resolveContentProvider(String authority, long flags, int userId)
        {
            return manager().resolveContentProvider(authority, (int)flags);
        }

        @Override
        public List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, long flags, int userId)
        {
            return manager().queryIntentActivities(intent, (int)flags);
        }

        @Override
        public List<ResolveInfo> queryIntentServices(Intent intent, String resolvedType, long flags, int userId)
        {
            return manager().queryIntentServices(intent, (int)flags);
        }

        @Override
        public List<ResolveInfo> queryIntentReceivers(Intent intent, String resolvedType, long flags, int userId)
        {
            return manager().queryBroadcastReceivers(intent, (int)flags);
        }

        @Override
        public int getComponentEnabledSetting(ComponentName componentName, int userId)
        {
            return manager().getComponentEnabledSetting(componentName);
        }

        @Override
        public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId, String callingPackage)
        {
            manager().setComponentEnabledSetting(componentName, newState, flags);
        }

        @Override
        public int getApplicationEnabledSetting(String packageName, int userId)
        {
            return manager().getApplicationEnabledSetting(packageName);
        }
    }
}
