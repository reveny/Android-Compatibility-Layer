package android.app;

import android.os.Bundle;

public class Instrumentation
{
    public void callApplicationOnCreate(Application application)
    {
        ApplicationStartInfo.recordTimestamp(ApplicationStartInfo.START_TIMESTAMP_APPLICATION_ONCREATE,
                                             android.os.SystemClock.elapsedRealtimeNanos());
        application.onCreate();
    }

    public void callActivityOnCreate(Activity activity, Bundle savedInstanceState)
    {
        ActivityThread.currentActivityThread().activityCreated(activity);
        activity.onCreate(savedInstanceState);
        activity.dispatchLifecycleCreated(savedInstanceState);
    }

    public void callActivityOnPostCreate(Activity activity, Bundle savedInstanceState)
    {
        activity.onPostCreate(savedInstanceState);
    }

    public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState)
    {
        if (savedInstanceState == null) return;

        activity.onRestoreInstanceState(savedInstanceState);
    }

    public void callActivityOnSaveInstanceState(Activity activity, Bundle outState)
    {
        activity.onSaveInstanceState(outState);
        activity.dispatchLifecycleSaveInstanceState(outState);
    }

    public void callActivityOnNewIntent(Activity activity, android.content.Intent intent)
    {
        activity.setIntent(intent);
        activity.onNewIntent(intent);
    }

    public void callActivityOnStart(Activity activity)
    {
        activity.onStart();
        activity.mStarted = true;
        activity.dispatchLifecycleStarted();
        ActivityThread.currentActivityThread().activityStateChanged(activity, true, false);
    }

    public void callActivityOnResume(Activity activity)
    {
        activity.onResume();
        activity.mResumed = true;
        activity.dispatchLifecycleResumed();
        ActivityThread.currentActivityThread().activityStateChanged(activity, false, false);
    }

    public void callActivityOnPause(Activity activity)
    {
        if (!activity.mResumed) return;

        activity.mResumed = false;
        activity.dispatchLifecyclePaused();
        activity.onPause();
        ActivityThread.currentActivityThread().activityStateChanged(activity, true, false);
    }

    public void callActivityOnStop(Activity activity)
    {
        if (!activity.mStarted) return;

        activity.mStarted = false;
        activity.dispatchLifecycleStopped();
        activity.onStop();
        ActivityThread.currentActivityThread().activityStateChanged(activity, true, true);
    }

    public void callActivityOnDestroy(Activity activity)
    {
        activity.dispatchLifecycleDestroyed();
        activity.onDestroy();
        ActivityThread.currentActivityThread().activityDestroyed(activity);
    }
}
