package runtime;

import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewRootImpl;

import java.util.ArrayList;
import java.util.List;

public final class Activities
{
    private static final String TAG = "Activities";

    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_OK = -1;

    public static final int LAUNCH_MULTIPLE = 0;
    public static final int LAUNCH_SINGLE_TOP = 1;
    public static final int LAUNCH_SINGLE_TASK = 2;
    public static final int LAUNCH_SINGLE_INSTANCE = 3;

    private static final class Entry
    {
        Activity activity;
        String className;
        Intent intent;
        android.os.Bundle savedState;
        int taskId = 1;
        int launchMode = LAUNCH_MULTIPLE;
        int requestCode = -1;
        int resultCode = RESULT_CANCELED;
        Intent resultData;
        boolean finishing;
        boolean noHistory;
    }

    private static int sNextTaskId = 1;

    private static final List<Entry> sStack = new ArrayList<>();

    private static Context sBaseContext;
    private static Application sApplication;
    private static AppComponentFactory sFactory;
    private static ClassLoader sLoader;
    private static Instrumentation sInstrumentation;
    private static ViewRootImpl sViewRoot;
    private static String sPackageName;
    private static String sDataDir;

    private Activities()
    {
    }

    public static void attachRuntime(Context base, Application application, AppComponentFactory factory, ClassLoader loader,
                                     Instrumentation instrumentation, String packageName)
    {
        sBaseContext = base;
        sApplication = application;
        sFactory = factory;
        sLoader = loader;
        sInstrumentation = instrumentation;
        sPackageName = packageName;
    }

    public static void attachDataDir(String dataDir)
    {
        sDataDir = dataDir;
    }

    public static android.os.Bundle restoreInstanceState(String className)
    {
        if (sDataDir == null || className == null) return null;

        java.io.File source = SavedState.fileFor(sDataDir, className);
        android.os.Bundle state = SavedState.read(source);

        if (state == null) return null;

        source.delete();
        Log.i(TAG, className + " comes back with the state it left behind: " + state.size() + " value(s)");

        return state;
    }

    public static void discardInstanceState(String className)
    {
        if (sDataDir == null || className == null) return;

        SavedState.fileFor(sDataDir, className).delete();
    }

    public static void attachViewRoot(ViewRootImpl root)
    {
        sViewRoot = root;
        showTop();
    }

    public static void adoptLaunchedActivity(Activity activity)
    {
        Entry entry = new Entry();
        entry.activity = activity;
        entry.className = activity.getClass().getName();
        entry.intent = activity.getIntent();
        entry.launchMode = Bootstrap.nativeActivityLaunchMode(entry.className);
        entry.taskId = sNextTaskId;
        sStack.add(entry);
    }

    public static Activity current()
    {
        return sStack.isEmpty() ? null : sStack.get(sStack.size() - 1).activity;
    }

    public static int depth()
    {
        return sStack.size();
    }

    private static String targetPackage(Intent intent)
    {
        if (intent == null || sPackageName == null) return null;

        String named = intent.getPackage();

        if (named == null && intent.getComponent() != null) named = intent.getComponent().getPackageName();
        if (named == null || named.isEmpty() || named.equals(sPackageName)) return null;

        return named;
    }

    public static void start(Intent intent, int requestCode)
    {
        if (sBaseContext == null)
        {
            Log.w(TAG, "startActivity before the runtime was attached: " + intent);
            return;
        }

        String otherPackage = targetPackage(intent);

        if (otherPackage != null)
        {
            if (Packages.launch(otherPackage))
            {
                Log.i(TAG, "asked the shell to start " + otherPackage + ", which runs in its own process");
                return;
            }

            Log.w(TAG, "nothing here can start " + otherPackage + "; it is not installed, or this player has no shell");
            return;
        }

        Components.Record record = Components.resolve(Components.ACTIVITY, intent);
        if (record == null)
        {
            Log.w(TAG, "no activity answers " + intent + ", so nothing was started");
            return;
        }

        Intent launch = new Intent(intent);
        launch.setComponent(new ComponentName(sPackageName, record.className));

        final int flags = launch.getFlags();
        final int launchMode = Bootstrap.nativeActivityLaunchMode(record.className);

        if (reusedExisting(record.className, launch, flags, launchMode)) return;

        Entry caller = sStack.isEmpty() ? null : sStack.get(sStack.size() - 1);

        Activity started;
        try
        {
            started = sFactory.instantiateActivity(sLoader, record.className, launch);
            started.attach(sBaseContext, sApplication);
            started.setIntent(launch);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "could not instantiate activity " + record.className, cause);
            return;
        }

        if (caller != null)
        {
            caller.requestCode = requestCode;
            pause(caller);

            if (caller.noHistory)
            {
                Log.i(TAG, caller.className + " asked not to be kept, so it leaves the stack as soon as it is left");
                sStack.remove(caller);
                stop(caller);
                destroy(caller);
                caller = null;
            }
        }

        Entry entry = new Entry();
        entry.activity = started;
        entry.className = record.className;
        entry.intent = launch;
        entry.launchMode = launchMode;
        entry.noHistory = (flags & Intent.FLAG_ACTIVITY_NO_HISTORY) != 0;

        final boolean newTask = (flags & Intent.FLAG_ACTIVITY_NEW_TASK) != 0 || launchMode == LAUNCH_SINGLE_TASK
                                || launchMode == LAUNCH_SINGLE_INSTANCE;

        if (newTask || caller == null)
        {
            sNextTaskId += 1;
            entry.taskId = sNextTaskId;
        }
        else
        {
            entry.taskId = caller.taskId;
        }

        sStack.add(entry);

        Log.i(TAG, "started " + record.className + " in task " + entry.taskId + ", stack is now " + sStack.size() + " deep");

        android.os.Bundle restored = restoreInstanceState(record.className);
        entry.savedState = restored;

        try
        {
            sInstrumentation.callActivityOnCreate(started, restored);
            sInstrumentation.callActivityOnPostCreate(started, restored);
            if (restored != null) sInstrumentation.callActivityOnRestoreInstanceState(started, restored);
            sInstrumentation.callActivityOnStart(started);
            sInstrumentation.callActivityOnResume(started);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity did not start, so the one that launched it stays on screen", cause);

            entry.finishing = true;
            sStack.remove(entry);
            destroy(entry);

            if (caller != null)
            {
                try
                {
                    sInstrumentation.callActivityOnResume(caller.activity);
                }
                catch (Throwable resumeFailed)
                {
                    Log.e(TAG, "the activity underneath did not come back", resumeFailed);
                }
            }

            showTop();
            return;
        }

        if (caller != null) stop(caller);

        showTop();
    }

    private static boolean reusedExisting(String className, Intent launch, int flags, int launchMode)
    {
        final boolean singleTop = launchMode == LAUNCH_SINGLE_TOP || (flags & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0;
        final boolean clearTop = (flags & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0;
        final boolean singleTask = launchMode == LAUNCH_SINGLE_TASK || launchMode == LAUNCH_SINGLE_INSTANCE;

        if (!singleTop && !clearTop && !singleTask) return false;

        Entry top = sStack.isEmpty() ? null : sStack.get(sStack.size() - 1);

        if (singleTop && top != null && className.equals(top.className))
        {
            Log.i(TAG, className + " is already on top, so it is given the intent rather than a second instance");
            deliverNewIntent(top, launch);
            return true;
        }

        if (!clearTop && !singleTask) return false;

        int found = -1;
        for (int index = sStack.size() - 1; index >= 0; index--)
        {
            if (!className.equals(sStack.get(index).className)) continue;

            found = index;
            break;
        }

        if (found < 0) return false;

        while (sStack.size() - 1 > found)
        {
            Entry above = sStack.remove(sStack.size() - 1);
            Log.i(TAG, "clearing " + above.className + " from above " + className);
            pause(above);
            stop(above);
            destroy(above);
        }

        Entry entry = sStack.get(found);
        deliverNewIntent(entry, launch);
        showTop();

        return true;
    }

    private static void deliverNewIntent(Entry entry, Intent launch)
    {
        entry.intent = launch;

        try
        {
            sInstrumentation.callActivityOnNewIntent(entry.activity, launch);
            sInstrumentation.callActivityOnResume(entry.activity);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity did not take the new intent", cause);
        }
    }

    public static int taskIdOf(Activity activity)
    {
        Entry entry = entryFor(activity);
        return entry != null ? entry.taskId : -1;
    }

    public static boolean moveTaskToBack(Activity activity, boolean nonRoot)
    {
        Entry entry = entryFor(activity);
        if (entry == null) return false;

        int lowest = -1;
        for (int index = 0; index < sStack.size(); index++)
        {
            if (sStack.get(index).taskId != entry.taskId) continue;

            lowest = index;
            break;
        }

        if (lowest < 0) return false;
        if (!nonRoot && sStack.get(lowest) != entry) return false;

        List<Entry> moved = new ArrayList<>();
        for (int index = sStack.size() - 1; index >= 0; index--)
        {
            if (sStack.get(index).taskId != entry.taskId) continue;

            moved.add(0, sStack.remove(index));
        }

        if (moved.isEmpty()) return false;
        if (sStack.isEmpty())
        {
            sStack.addAll(moved);
            Log.i(TAG, "task " + entry.taskId + " is the only one, so it stays in front");
            return false;
        }

        pause(entry);
        stop(entry);
        sStack.addAll(0, moved);

        Entry top = sStack.get(sStack.size() - 1);
        try
        {
            sInstrumentation.callActivityOnStart(top.activity);
            sInstrumentation.callActivityOnResume(top.activity);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity brought forward did not resume", cause);
        }

        Log.i(TAG, "task " + entry.taskId + " went to the back, so " + top.className + " is in front");
        showTop();

        return true;
    }

    public static void recreate(Activity activity)
    {
        Entry entry = entryFor(activity);
        if (entry == null || entry.className == null) return;

        android.os.Bundle state = new android.os.Bundle();
        sInstrumentation.callActivityOnSaveInstanceState(activity, state);

        Object retained = null;
        try
        {
            retained = activity.onRetainNonConfigurationInstance();
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity threw while retaining its non-configuration instance", cause);
        }

        activity.setChangingConfigurations(true);
        pause(entry);
        stop(entry);
        destroy(entry);

        Activity replacement;
        try
        {
            replacement = sFactory.instantiateActivity(sLoader, entry.className, entry.intent);
            replacement.attach(sBaseContext, sApplication);
            replacement.setIntent(entry.intent);
            replacement.setLastNonConfigurationInstance(retained);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "could not build a replacement for " + entry.className, cause);
            sStack.remove(entry);
            return;
        }

        entry.activity = replacement;
        entry.savedState = state;

        Log.i(TAG, "recreated " + entry.className + " with " + state.size() + " saved value(s)");

        try
        {
            sInstrumentation.callActivityOnCreate(replacement, state);
            sInstrumentation.callActivityOnRestoreInstanceState(replacement, state);
            sInstrumentation.callActivityOnPostCreate(replacement, state);
            sInstrumentation.callActivityOnStart(replacement);
            sInstrumentation.callActivityOnResume(replacement);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the recreated activity did not start", cause);
        }

        showTop();
    }

    public static void recreateAll()
    {
        List<Entry> snapshot = new ArrayList<>(sStack);

        for (Entry entry : snapshot)
        {
            if (entry.activity == null) continue;

            recreate(entry.activity);
        }
    }

    public static List<String> classNames()
    {
        List<String> names = new ArrayList<>();

        for (Entry entry : sStack)
        {
            if (entry.className != null) names.add(entry.className);
        }

        return names;
    }

    public static void deliverConfigurationChange(android.content.res.Configuration configuration, int changed)
    {
        List<Entry> snapshot = new ArrayList<>(sStack);

        for (Entry entry : snapshot)
        {
            if (entry.activity == null || entry.className == null) continue;

            int declared = Bootstrap.nativeActivityConfigChanges(entry.className);

            if ((declared & changed) == changed)
            {
                try
                {
                    entry.activity.onConfigurationChanged(configuration);
                }
                catch (Throwable cause)
                {
                    Log.e(TAG, entry.className + " threw while handling the configuration change", cause);
                }

                continue;
            }

            recreate(entry.activity);
        }
    }

    public static void setResult(Activity activity, int resultCode, Intent data)
    {
        Entry entry = entryFor(activity);
        if (entry == null) return;

        entry.resultCode = resultCode;
        entry.resultData = data;
    }

    public static boolean isFinishing(Activity activity)
    {
        Entry entry = entryFor(activity);
        return entry != null && entry.finishing;
    }

    public static void finish(Activity activity)
    {
        Entry entry = entryFor(activity);
        if (entry == null) return;

        entry.finishing = true;
        discardInstanceState(entry.className);

        if (sStack.size() <= 1)
        {
            Log.i(TAG, "the last activity finished, so the main loop will end");
            android.os.Looper.quitMainLooper();
            return;
        }

        if (entry != sStack.get(sStack.size() - 1))
        {
            sStack.remove(entry);
            destroy(entry);
            return;
        }

        sStack.remove(sStack.size() - 1);
        pause(entry);
        stop(entry);
        destroy(entry);

        Entry below = sStack.get(sStack.size() - 1);
        Log.i(TAG, "finished " + entry.activity.getClass().getName() + ", back to " + below.activity.getClass().getName());

        try
        {
            sInstrumentation.callActivityOnStart(below.activity);

            if (below.requestCode >= 0)
            {
                below.activity.deliverActivityResult(below.requestCode, entry.resultCode, entry.resultData);
                below.requestCode = -1;
            }

            sInstrumentation.callActivityOnResume(below.activity);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity underneath did not resume", cause);
        }

        showTop();
    }

    public static boolean back()
    {
        Activity top = current();
        if (top == null) return false;

        finish(top);
        return true;
    }

    private static void showTop()
    {
        Activity top = current();
        if (top == null || sViewRoot == null) return;
        if (top.getWindow() == null) return;

        View decor = top.getWindow().getDecorView();
        if (decor != null) sViewRoot.setContentRoot(decor);
    }

    public static void shutdown()
    {
        List<Entry> snapshot = new ArrayList<>(sStack);
        sStack.clear();

        for (int index = snapshot.size() - 1; index >= 0; index--)
        {
            Entry entry = snapshot.get(index);
            if (entry.activity == null || entry.activity.isDestroyed()) continue;

            pause(entry);
            stop(entry);
            destroy(entry);
        }
    }

    private static Entry entryFor(Activity activity)
    {
        for (Entry entry : sStack)
        {
            if (entry.activity == activity) return entry;
        }

        return null;
    }

    private static void pause(Entry entry)
    {
        try
        {
            sInstrumentation.callActivityOnPause(entry.activity);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity did not pause", cause);
        }
    }

    private static void stop(Entry entry)
    {
        saveInstanceState(entry);

        try
        {
            sInstrumentation.callActivityOnStop(entry.activity);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity did not stop", cause);
        }
    }

    private static void saveInstanceState(Entry entry)
    {
        if (entry.activity == null || entry.finishing) return;
        if (entry.activity.isChangingConfigurations()) return;

        android.os.Bundle state = new android.os.Bundle();

        try
        {
            sInstrumentation.callActivityOnSaveInstanceState(entry.activity, state);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity threw while saving its state", cause);
            return;
        }

        entry.savedState = state;

        if (sDataDir == null || entry.className == null) return;

        if (SavedState.write(SavedState.fileFor(sDataDir, entry.className), state))
        {
            Log.i(TAG, "the state of " + entry.className + " is on disk, so it survives the process");
        }
    }

    private static void destroy(Entry entry)
    {
        try
        {
            sInstrumentation.callActivityOnDestroy(entry.activity);
            entry.activity.setDestroyed();
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the activity did not shut down cleanly", cause);
        }
    }
}
