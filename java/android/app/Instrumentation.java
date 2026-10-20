package android.app;

import android.os.Bundle;

public class Instrumentation
{
    public void callApplicationOnCreate(Application application)
    {
        application.onCreate();
    }

    public void callActivityOnCreate(Activity activity, Bundle savedInstanceState)
    {
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
        activity.dispatchLifecycleStarted();
    }

    public void callActivityOnResume(Activity activity)
    {
        activity.onResume();
        activity.dispatchLifecycleResumed();
    }

    public void callActivityOnPause(Activity activity)
    {
        activity.dispatchLifecyclePaused();
        activity.onPause();
    }

    public void callActivityOnStop(Activity activity)
    {
        activity.dispatchLifecycleStopped();
        activity.onStop();
    }

    public void callActivityOnDestroy(Activity activity)
    {
        activity.dispatchLifecycleDestroyed();
        activity.onDestroy();
    }
}
