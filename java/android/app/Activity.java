package android.app;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.RuntimeWindow;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class Activity extends ContextThemeWrapper implements Window.Callback, android.content.ComponentCallbacks
{
    private Window mWindow;
    private Application mApplication;
    private CharSequence mTitle = "";
    private android.content.Intent mIntent = new android.content.Intent();
    private final android.window.RuntimeBackDispatcher mBackDispatcher = new android.window.RuntimeBackDispatcher();
    private final FragmentManager mFragmentManager = new RuntimeFragmentManager();

    public Activity()
    {
    }

    public final void attach(Context context, Application application)
    {
        attachBaseContext(context);
        mApplication = application;
        mWindow = new RuntimeWindow(context);
        mWindow.setCallback(this);
    }

    @Override
    public void onContentChanged()
    {
    }

    public boolean dispatchGenericMotionEvent(android.view.MotionEvent event)
    {
        android.view.View decor = getWindow() == null ? null : getWindow().getDecorView();
        if (decor != null && decor.dispatchGenericMotionEvent(event)) return true;

        return onGenericMotionEvent(event);
    }

    public boolean dispatchTouchEvent(android.view.MotionEvent event)
    {
        boolean traced = android.view.View.TRACE_INPUT;
        boolean consumedByView = getWindow() != null && getWindow().superDispatchTouchEvent(event);

        if (consumedByView)
        {
            if (traced) android.util.Log.i("Activity", "the view hierarchy consumed action " + event.getActionMasked());
            return true;
        }

        boolean handled = onTouchEvent(event);

        if (traced)
        {
            android.util.Log.i("Activity", "onTouchEvent(" + event.getActionMasked() + ") on "
                                               + getClass().getName() + " returned " + handled);
        }

        return handled;
    }

    public boolean onTouchEvent(android.view.MotionEvent event)
    {
        return false;
    }

    public boolean dispatchKeyEvent(android.view.KeyEvent event)
    {
        if (android.view.inputmethod.InputMethodManager.getInstance().dispatchKeyEvent(event)) return true;

        if (getWindow() != null && getWindow().superDispatchKeyEvent(event)) return true;

        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) return onKeyDown(event.getKeyCode(), event);
        if (event.getAction() == android.view.KeyEvent.ACTION_UP) return onKeyUp(event.getKeyCode(), event);

        return false;
    }

    public boolean onKeyDown(int keyCode, android.view.KeyEvent event)
    {
        return false;
    }

    public boolean onKeyUp(int keyCode, android.view.KeyEvent event)
    {
        return false;
    }

    public boolean onGenericMotionEvent(android.view.MotionEvent event)
    {
        return false;
    }

    public void onWindowFocusChanged(boolean hasFocus)
    {
    }

    @Override
    public void onAttachedToWindow()
    {
    }

    @Override
    public void onDetachedFromWindow()
    {
    }

    public final Application getApplication()
    {
        return mApplication;
    }

    public Window getWindow()
    {
        return mWindow;
    }

    public View getCurrentFocus()
    {
        return null;
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    public void setTitle(CharSequence title)
    {
        mTitle = title == null ? "" : title;
        if (mWindow != null) mWindow.setTitle(mTitle);
    }

    public static final int DEFAULT_KEYS_DISABLE = 0;
    public static final int DEFAULT_KEYS_DIALER = 1;
    public static final int DEFAULT_KEYS_SHORTCUT = 2;
    public static final int DEFAULT_KEYS_SEARCH_LOCAL = 3;
    public static final int DEFAULT_KEYS_SEARCH_GLOBAL = 4;

    private int mDefaultKeyMode = DEFAULT_KEYS_DISABLE;

    public final void setDefaultKeyMode(int mode)
    {
        mDefaultKeyMode = mode;
    }

    public void onTrimMemory(int level)
    {
    }

    public void reportFullyDrawn()
    {
    }

    public void onUserLeaveHint()
    {
    }

    public void overridePendingTransition(int enterAnim, int exitAnim)
    {
    }

    public void setContentView(int layoutResourceId)
    {
        setContentView(getLayoutInflater().inflate(layoutResourceId, null));
    }

    public LayoutInflater getLayoutInflater()
    {
        return LayoutInflater.from(this);
    }

    public void setContentView(View view)
    {
        mWindow.setContentView(view);
    }

    public void setContentView(View view, ViewGroup.LayoutParams layoutParams)
    {
        mWindow.setContentView(view, layoutParams);
    }

    public View findViewById(int id)
    {
        return mWindow == null ? null : mWindow.findViewById(id);
    }

    public int getRequestedOrientation()
    {
        return mRequestedOrientation;
    }

    public void setRequestedOrientation(int requestedOrientation)
    {
        mRequestedOrientation = requestedOrientation;
    }

    public boolean isDestroyed()
    {
        return mDestroyed;
    }

    public boolean isInMultiWindowMode()
    {
        return false;
    }

    public boolean isInPictureInPictureMode()
    {
        return false;
    }

    public boolean hasWindowFocus()
    {
        return true;
    }

    public void runOnUiThread(Runnable action)
    {
        if (action == null) return;

        android.os.Looper main = android.os.Looper.getMainLooper();
        if (main == null || main.isCurrentThread())
        {
            action.run();
            return;
        }

        new android.os.Handler(main).post(action);
    }

    public String getCallingPackage()
    {
        return null;
    }

    public android.content.ComponentName getCallingActivity()
    {
        return null;
    }

    private int mRequestedOrientation = -1;
    private boolean mDestroyed;
    private boolean mChangingConfigurations;
    private Object mLastNonConfigurationInstance;

    public final View requireViewById(int id)
    {
        View found = findViewById(id);
        if (found == null) throw new IllegalArgumentException("No view with id 0x" + Integer.toHexString(id));
        return found;
    }

    public FragmentManager getFragmentManager()
    {
        return mFragmentManager;
    }

    public android.window.OnBackInvokedDispatcher getOnBackInvokedDispatcher()
    {
        return mBackDispatcher;
    }

    public Object getLastNonConfigurationInstance()
    {
        return mLastNonConfigurationInstance;
    }

    public void setLastNonConfigurationInstance(Object retained)
    {
        mLastNonConfigurationInstance = retained;
    }

    public void setChangingConfigurations(boolean changing)
    {
        mChangingConfigurations = changing;
    }

    public void setDestroyed()
    {
        mDestroyed = true;
    }

    public Object onRetainNonConfigurationInstance()
    {
        return null;
    }

    public android.content.Intent getIntent()
    {
        return mIntent;
    }

    public void setIntent(android.content.Intent intent)
    {
        mIntent = intent;
    }

    public android.content.ComponentName getComponentName()
    {
        return new android.content.ComponentName(getPackageName(), getClass().getName());
    }

    public Object getSystemService(String name)
    {
        return super.getSystemService(name);
    }

    public android.view.WindowManager getWindowManager()
    {
        return (android.view.WindowManager)getSystemService(Context.WINDOW_SERVICE);
    }

    public boolean isChild()
    {
        return false;
    }

    public void invalidateOptionsMenu()
    {
    }

    public void onConfigurationChanged(android.content.res.Configuration newConfig)
    {
    }

    public void onLowMemory()
    {
    }

    public void setShowWhenLocked(boolean showWhenLocked)
    {
    }

    public void setTurnScreenOn(boolean turnScreenOn)
    {
    }

    public void setInheritShowWhenLocked(boolean inherit)
    {
    }

    public void setVrModeEnabled(boolean enabled, android.content.ComponentName component)
    {
    }

    protected void onSaveInstanceState(Bundle outState)
    {
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    }

    protected void onPostCreate(Bundle savedInstanceState)
    {
    }

    protected void onPostResume()
    {
    }

    public void startActivityForResult(android.content.Intent intent, int requestCode)
    {
        runtime.Activities.start(intent, requestCode);
    }

    public void setResult(int resultCode)
    {
        runtime.Activities.setResult(this, resultCode, null);
    }

    public void setResult(int resultCode, android.content.Intent data)
    {
        runtime.Activities.setResult(this, resultCode, data);
    }

    public final void deliverActivityResult(int requestCode, int resultCode, android.content.Intent data)
    {
        onActivityResult(requestCode, resultCode, data);
    }

    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data)
    {
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
    }

    protected void onNewIntent(android.content.Intent intent)
    {
    }

    public void recreate()
    {
        runtime.Activities.recreate(this);
    }

    public int getTaskId()
    {
        return runtime.Activities.taskIdOf(this);
    }

    public boolean moveTaskToBack(boolean nonRoot)
    {
        return runtime.Activities.moveTaskToBack(this, nonRoot);
    }

    public void startActivities(android.content.Intent[] intents)
    {
        if (intents == null) return;

        for (android.content.Intent intent : intents) runtime.Activities.start(intent, -1);
    }

    public void onBackPressed()
    {
        android.window.OnBackInvokedDispatcher dispatcher = getOnBackInvokedDispatcher();

        if (dispatcher instanceof android.window.RuntimeBackDispatcher
            && ((android.window.RuntimeBackDispatcher)dispatcher).dispatchBack())
        {
            return;
        }

        finish();
    }

    public final void requestPermissions(final String[] permissions, final int requestCode)
    {
        if (permissions == null) throw new IllegalArgumentException("permissions must not be null");

        final String[] requested = permissions.clone();
        final int[] results = new int[requested.length];

        for (int index = 0; index < requested.length; index++)
        {
            results[index] = runtime.Permissions.check(requested[index]);

            if (results[index] == android.content.pm.PackageManager.PERMISSION_GRANTED) continue;
            if (!runtime.Permissions.isDeclared(requested[index]))
            {
                android.util.Log.w("Activity", requested[index] + " was requested but the manifest does not declare it, so it stays denied");
                continue;
            }

            runtime.Permissions.deny(requested[index]);
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                onRequestPermissionsResult(requestCode, requested, results);
            }
        });
    }

    public boolean shouldShowRequestPermissionRationale(String permission)
    {
        return runtime.Permissions.isDeclared(permission) && runtime.Permissions.wasRefused(permission);
    }

    public boolean isFinishing()
    {
        return runtime.Activities.isFinishing(this);
    }

    public boolean isChangingConfigurations()
    {
        return mChangingConfigurations;
    }

    public void finish()
    {
        runtime.Activities.finish(this);
    }

    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_OK = -1;
    public static final int RESULT_FIRST_USER = 1;

    protected void onCreate(Bundle savedInstanceState)
    {
    }

    protected void onStart()
    {
    }

    protected void onResume()
    {
    }

    protected void onPause()
    {
    }

    protected void onStop()
    {
    }

    protected void onDestroy()
    {
    }

    public void registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback)
    {
        if (callback == null) return;

        synchronized (mLifecycleCallbacks)
        {
            mLifecycleCallbacks.add(callback);
        }
    }

    public void unregisterActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callback)
    {
        synchronized (mLifecycleCallbacks)
        {
            mLifecycleCallbacks.remove(callback);
        }
    }

    private Application.ActivityLifecycleCallbacks[] collectLifecycleCallbacks()
    {
        java.util.ArrayList<Application.ActivityLifecycleCallbacks> collected = new java.util.ArrayList<>();

        synchronized (mLifecycleCallbacks)
        {
            collected.addAll(mLifecycleCallbacks);
        }

        Application application = getApplication();
        if (application != null)
        {
            for (Application.ActivityLifecycleCallbacks callback : application.collectActivityLifecycleCallbacks())
            {
                collected.add(callback);
            }
        }

        return collected.toArray(new Application.ActivityLifecycleCallbacks[0]);
    }

    void dispatchLifecycleCreated(android.os.Bundle savedInstanceState)
    {
        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPreCreated(this, savedInstanceState);
            callback.onActivityCreated(this, savedInstanceState);
            callback.onActivityPostCreated(this, savedInstanceState);
        }
    }

    void dispatchLifecycleSaveInstanceState(android.os.Bundle outState)
    {
        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivitySaveInstanceState(this, outState);
        }
    }

    void dispatchLifecycleStarted()
    {
        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPreStarted(this);
            callback.onActivityStarted(this);
            callback.onActivityPostStarted(this);
        }
    }

    void dispatchLifecycleResumed()
    {
        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPreResumed(this);
            callback.onActivityResumed(this);
            callback.onActivityPostResumed(this);
        }
    }

    void dispatchLifecyclePaused()
    {
        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPrePaused(this);
            callback.onActivityPaused(this);
            callback.onActivityPostPaused(this);
        }
    }

    void dispatchLifecycleStopped()
    {
        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPreStopped(this);
            callback.onActivityStopped(this);
            callback.onActivityPostStopped(this);
        }
    }

    void dispatchLifecycleDestroyed()
    {
        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPreDestroyed(this);
            callback.onActivityDestroyed(this);
            callback.onActivityPostDestroyed(this);
        }
    }

    private final java.util.ArrayList<Application.ActivityLifecycleCallbacks> mLifecycleCallbacks =
        new java.util.ArrayList<>();
}
