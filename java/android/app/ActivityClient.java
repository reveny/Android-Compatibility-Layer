package android.app;

import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;

public class ActivityClient
{
    private static final ActivityClient sInstance = new ActivityClient();

    private ActivityClient()
    {
    }

    public static ActivityClient getInstance()
    {
        return sInstance;
    }

    private static Activity activity(IBinder token)
    {
        return ActivityThread.currentActivityThread().getActivity(token);
    }

    public boolean finishActivity(IBinder token, int resultCode, Intent resultData, int finishTask)
    {
        Activity activity = activity(token);
        if (activity == null) return false;

        activity.setResult(resultCode, resultData);
        activity.finish();
        return true;
    }

    public boolean finishActivityAffinity(IBinder token)
    {
        Activity activity = activity(token);
        if (activity == null) return false;

        activity.finishAffinity();
        return true;
    }

    public boolean isTopOfTask(IBinder token)
    {
        Activity activity = activity(token);
        return activity != null && runtime.Activities.current() == activity;
    }

    public boolean isRootVoiceInteraction(IBinder token)
    {
        return false;
    }

    public int getTaskForActivity(IBinder token, boolean onlyRoot)
    {
        Activity activity = activity(token);
        if (activity == null) return -1;
        if (onlyRoot && !activity.isTaskRoot()) return -1;

        return activity.getTaskId();
    }

    public boolean moveActivityTaskToBack(IBinder token, boolean nonRoot)
    {
        Activity activity = activity(token);
        return activity != null && activity.moveTaskToBack(nonRoot);
    }

    public ComponentName getCallingActivity(IBinder token)
    {
        Activity activity = activity(token);
        return activity == null ? null : activity.getCallingActivity();
    }

    public String getCallingPackage(IBinder token)
    {
        Activity activity = activity(token);
        return activity == null ? null : activity.getCallingPackage();
    }

    public int getRequestedOrientation(IBinder token)
    {
        Activity activity = activity(token);
        return activity == null ? -1 : activity.getRequestedOrientation();
    }

    public void setRequestedOrientation(IBinder token, int requestedOrientation)
    {
        Activity activity = activity(token);
        if (activity != null) activity.setRequestedOrientation(requestedOrientation);
    }

    public ComponentName getActivityClassForToken(IBinder token)
    {
        Activity activity = activity(token);
        return activity == null ? null : activity.getComponentName();
    }

    public int getDisplayId(IBinder token)
    {
        return activity(token) == null ? -1 : 0;
    }

    public IBinder getActivityTokenBelow(IBinder token)
    {
        return null;
    }

    public void activityIdle(IBinder token, android.content.res.Configuration configuration, boolean stopProfiling)
    {
    }

    public void activityResumed(IBinder token, boolean handleSplashScreenExit)
    {
    }

    public void activityPaused(IBinder token)
    {
    }

    public void activityStopped(IBinder token, android.os.Bundle state, android.os.PersistableBundle persistentState,
                                CharSequence description)
    {
    }

    public void activityDestroyed(IBinder token)
    {
    }
}
