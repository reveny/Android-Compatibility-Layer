package android.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import java.util.ArrayList;

public class Application extends ContextWrapper
{
    private final ArrayList<ActivityLifecycleCallbacks> mActivityLifecycleCallbacks = new ArrayList<>();

    public Application()
    {
        super(null);
    }

    public void onCreate()
    {
    }

    public void onTerminate()
    {
    }

    public void onLowMemory()
    {
    }

    public void onTrimMemory(int level)
    {
    }

    public void onConfigurationChanged(Configuration newConfig)
    {
    }

    private static String sProcessName;

    public static String getProcessName()
    {
        return sProcessName;
    }

    public final void attach(Context context)
    {
        if (context != null && sProcessName == null) sProcessName = context.getPackageName();

        attachBaseContext(context);
    }

    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback)
    {
        if (callback == null) return;
        synchronized (mActivityLifecycleCallbacks)
        {
            mActivityLifecycleCallbacks.add(callback);
        }
    }

    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback)
    {
        synchronized (mActivityLifecycleCallbacks)
        {
            mActivityLifecycleCallbacks.remove(callback);
        }
    }

    public ActivityLifecycleCallbacks[] collectActivityLifecycleCallbacks()
    {
        synchronized (mActivityLifecycleCallbacks)
        {
            return mActivityLifecycleCallbacks.toArray(new ActivityLifecycleCallbacks[0]);
        }
    }

    public interface ActivityLifecycleCallbacks
    {
        void onActivityCreated(Activity activity, android.os.Bundle savedInstanceState);

        void onActivityStarted(Activity activity);

        void onActivityResumed(Activity activity);

        void onActivityPaused(Activity activity);

        void onActivityStopped(Activity activity);

        void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState);

        void onActivityDestroyed(Activity activity);

        default void onActivityPreCreated(Activity activity, android.os.Bundle savedInstanceState)
        {
        }

        default void onActivityPostCreated(Activity activity, android.os.Bundle savedInstanceState)
        {
        }

        default void onActivityPreStarted(Activity activity)
        {
        }

        default void onActivityPostStarted(Activity activity)
        {
        }

        default void onActivityPreResumed(Activity activity)
        {
        }

        default void onActivityPostResumed(Activity activity)
        {
        }

        default void onActivityPrePaused(Activity activity)
        {
        }

        default void onActivityPostPaused(Activity activity)
        {
        }

        default void onActivityPreStopped(Activity activity)
        {
        }

        default void onActivityPostStopped(Activity activity)
        {
        }

        default void onActivityPreSaveInstanceState(Activity activity, android.os.Bundle outState)
        {
        }

        default void onActivityPostSaveInstanceState(Activity activity, android.os.Bundle outState)
        {
        }

        default void onActivityPreDestroyed(Activity activity)
        {
        }

        default void onActivityPostDestroyed(Activity activity)
        {
        }
    }
}
