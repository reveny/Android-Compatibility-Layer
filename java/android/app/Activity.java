package android.app;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.RuntimeWindow;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public class Activity extends ContextThemeWrapper implements LayoutInflater.Factory2, Window.Callback, android.view.KeyEvent.Callback,
                                                             View.OnCreateContextMenuListener, android.content.ComponentCallbacks2
{
    private Window mWindow;
    private Application mApplication;
    private CharSequence mTitle = "";
    private android.content.Intent mIntent = new android.content.Intent();
    private final android.window.RuntimeBackDispatcher mBackDispatcher = new android.window.RuntimeBackDispatcher();
    private final RuntimeFragmentManager mFragmentManager = new RuntimeFragmentManager(this);
    private final android.util.SparseArray<FragmentRequest> mFragmentRequests = new android.util.SparseArray<>();
    private final java.util.ArrayList<ScreenCaptureRegistration> mScreenCaptureCallbacks = new java.util.ArrayList<>();
    private int mNextFragmentRequest;
    private RuntimeLoaderManager mLoaderManager;
    private PictureInPictureParams mPictureInPictureParams;
    private android.media.session.MediaController mMediaController;
    private SharedElementCallback mEnterSharedElementCallback;
    private SharedElementCallback mExitSharedElementCallback;
    private android.content.LocusId mLocusId;
    private Bundle mLocusBundle;

    private static final int FRAGMENT_REQUEST_BASE = 0x00ff0000;

    private static final class FragmentRequest
    {
        final Fragment mFragment;
        final int mRequestCode;

        FragmentRequest(Fragment fragment, int requestCode)
        {
            mFragment = fragment;
            mRequestCode = requestCode;
        }
    }

    private static final class ScreenCaptureRegistration
    {
        final java.util.concurrent.Executor mExecutor;
        final ScreenCaptureCallback mCallback;

        ScreenCaptureRegistration(java.util.concurrent.Executor executor, ScreenCaptureCallback callback)
        {
            mExecutor = executor;
            mCallback = callback;
        }
    }

    public interface ScreenCaptureCallback
    {
        void onScreenCaptured();
    }

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
        return mWindow == null ? null : mWindow.getCurrentFocus();
    }

    public boolean onKeyLongPress(int keyCode, android.view.KeyEvent event)
    {
        return false;
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, android.view.KeyEvent event)
    {
        return false;
    }

    public boolean onKeyShortcut(int keyCode, android.view.KeyEvent event)
    {
        return false;
    }

    public boolean dispatchKeyShortcutEvent(android.view.KeyEvent event)
    {
        onUserInteraction();
        if (mWindow != null && mWindow.superDispatchKeyShortcutEvent(event)) return true;

        return onKeyShortcut(event.getKeyCode(), event);
    }

    public boolean dispatchTrackballEvent(android.view.MotionEvent event)
    {
        onUserInteraction();
        if (mWindow != null && mWindow.superDispatchTrackballEvent(event)) return true;

        return onTrackballEvent(event);
    }

    public boolean onTrackballEvent(android.view.MotionEvent event)
    {
        return false;
    }

    public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent event)
    {
        event.setClassName(getClass().getName());
        event.setPackageName(getPackageName());
        event.getText().add(getTitle());
        return true;
    }

    public void onUserInteraction()
    {
    }

    public void onWindowAttributesChanged(android.view.WindowManager.LayoutParams params)
    {
    }

    public View onCreatePanelView(int featureId)
    {
        return null;
    }

    public boolean onCreatePanelMenu(int featureId, android.view.Menu menu)
    {
        if (featureId != Window.FEATURE_OPTIONS_PANEL) return false;

        boolean show = onCreateOptionsMenu(menu);
        show |= mFragmentManager.dispatchCreateOptionsMenu(menu, getMenuInflater());
        return show;
    }

    public boolean onPreparePanel(int featureId, View view, android.view.Menu menu)
    {
        if (featureId != Window.FEATURE_OPTIONS_PANEL) return true;

        boolean show = onPrepareOptionsMenu(menu);
        show |= mFragmentManager.dispatchPrepareOptionsMenu(menu);
        return show && menu.hasVisibleItems();
    }

    public boolean onMenuOpened(int featureId, android.view.Menu menu)
    {
        return true;
    }

    public boolean onMenuItemSelected(int featureId, android.view.MenuItem item)
    {
        if (featureId == Window.FEATURE_OPTIONS_PANEL)
        {
            return onOptionsItemSelected(item) || mFragmentManager.dispatchOptionsItemSelected(item);
        }

        if (featureId == Window.FEATURE_CONTEXT_MENU)
        {
            return onContextItemSelected(item) || mFragmentManager.dispatchContextItemSelected(item);
        }

        return false;
    }

    public void onPanelClosed(int featureId, android.view.Menu menu)
    {
        if (featureId == Window.FEATURE_OPTIONS_PANEL)
        {
            mFragmentManager.dispatchOptionsMenuClosed(menu);
            onOptionsMenuClosed(menu);
        }
        if (featureId == Window.FEATURE_CONTEXT_MENU) onContextMenuClosed(menu);
    }

    public boolean onCreateOptionsMenu(android.view.Menu menu)
    {
        return true;
    }

    public boolean onPrepareOptionsMenu(android.view.Menu menu)
    {
        return true;
    }

    public boolean onOptionsItemSelected(android.view.MenuItem item)
    {
        return false;
    }

    public void onOptionsMenuClosed(android.view.Menu menu)
    {
    }

    public void openOptionsMenu()
    {
        if (mActionBar != null && mActionBar.openOptionsMenu()) return;

        android.util.Log.i("Activity", "openOptionsMenu on " + getClass().getName() + " has no panel to show it in");
    }

    public void closeOptionsMenu()
    {
        if (mActionBar != null) mActionBar.closeOptionsMenu();
    }

    private RuntimeActionBar mActionBar;
    private boolean mActionBarOwnedByDecor;
    private boolean mActionBarResolved;

    public ActionBar getActionBar()
    {
        initWindowDecorActionBar();
        return mActionBar;
    }

    public void setActionBar(android.widget.Toolbar toolbar)
    {
        mActionBarResolved = true;
        if (mActionBarOwnedByDecor) detachDecorActionBar();

        mActionBar = toolbar == null ? null : new RuntimeActionBar(toolbar, getTitle(), this);
        scheduleOptionsMenu();
    }

    private boolean themeFlag(String name)
    {
        int attribute = getResources().getIdentifier(name, "attr", "android");
        if (attribute == 0) return false;

        android.util.TypedValue value = new android.util.TypedValue();
        if (!getTheme().resolveAttribute(attribute, value, true)) return false;

        return value.type == android.util.TypedValue.TYPE_INT_BOOLEAN && value.data != 0;
    }

    private void initWindowDecorActionBar()
    {
        if (mActionBarResolved || mWindow == null) return;
        mActionBarResolved = true;

        if (mWindow.hasFeature(Window.FEATURE_NO_TITLE) || themeFlag("windowNoTitle")) return;
        if (!mWindow.hasFeature(Window.FEATURE_ACTION_BAR) && !themeFlag("windowActionBar")) return;
        if (!(mWindow.getDecorView() instanceof android.view.DecorView)) return;

        mActionBar = new RuntimeActionBar(this, this);
        mActionBar.setTitle(getTitle());
        ((android.view.DecorView)mWindow.getDecorView()).setActionBarView(mActionBar.getView());
        mActionBarOwnedByDecor = true;

        scheduleOptionsMenu();
    }

    private void detachDecorActionBar()
    {
        if (mWindow != null && mWindow.getDecorView() instanceof android.view.DecorView)
        {
            ((android.view.DecorView)mWindow.getDecorView()).setActionBarView(null);
        }

        mActionBarOwnedByDecor = false;
    }

    private void scheduleOptionsMenu()
    {
        View decor = mWindow == null ? null : mWindow.getDecorView();
        if (decor == null || mActionBar == null) return;

        decor.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (mActionBar != null) mActionBar.invalidateOptionsMenu();
            }
        });
    }

    public void onCreateContextMenu(android.view.ContextMenu menu, View view, android.view.ContextMenu.ContextMenuInfo menuInfo)
    {
    }

    public void registerForContextMenu(View view)
    {
        view.setOnCreateContextMenuListener(this);
    }

    public void unregisterForContextMenu(View view)
    {
        view.setOnCreateContextMenuListener(null);
    }

    public void openContextMenu(View view)
    {
    }

    public void closeContextMenu()
    {
    }

    public boolean onContextItemSelected(android.view.MenuItem item)
    {
        return false;
    }

    public void onContextMenuClosed(android.view.Menu menu)
    {
    }

    public boolean onSearchRequested()
    {
        return false;
    }

    public boolean onSearchRequested(android.view.SearchEvent searchEvent)
    {
        return onSearchRequested();
    }

    public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback)
    {
        return null;
    }

    public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback, int type)
    {
        return type == android.view.ActionMode.TYPE_PRIMARY ? onWindowStartingActionMode(callback) : null;
    }

    public android.view.ActionMode startActionMode(android.view.ActionMode.Callback callback)
    {
        return null;
    }

    public android.view.ActionMode startActionMode(android.view.ActionMode.Callback callback, int type)
    {
        return null;
    }

    public void onActionModeStarted(android.view.ActionMode mode)
    {
    }

    public void onActionModeFinished(android.view.ActionMode mode)
    {
    }

    public final boolean requestWindowFeature(int featureId)
    {
        return mWindow != null && mWindow.requestFeature(featureId);
    }

    private android.view.MenuInflater mMenuInflater;

    public android.view.MenuInflater getMenuInflater()
    {
        if (mMenuInflater == null) mMenuInflater = new android.view.MenuInflater(this);
        return mMenuInflater;
    }

    public View onCreateView(String name, Context context, android.util.AttributeSet attrs)
    {
        return null;
    }

    public View onCreateView(View parent, String name, Context context, android.util.AttributeSet attrs)
    {
        return onCreateView(name, context, attrs);
    }

    public void addContentView(View view, ViewGroup.LayoutParams params)
    {
        View content = mWindow == null ? null : mWindow.findViewById(Window.ID_ANDROID_CONTENT);
        if (content instanceof ViewGroup) ((ViewGroup)content).addView(view, params);
    }

    public void onCreate(Bundle savedInstanceState, android.os.PersistableBundle persistentState)
    {
        onCreate(savedInstanceState);
    }

    public void onPostCreate(Bundle savedInstanceState, android.os.PersistableBundle persistentState)
    {
        onPostCreate(savedInstanceState);
    }

    protected void onRestart()
    {
    }

    public void onSaveInstanceState(Bundle outState, android.os.PersistableBundle outPersistentState)
    {
        onSaveInstanceState(outState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState, android.os.PersistableBundle persistentState)
    {
        if (savedInstanceState != null) onRestoreInstanceState(savedInstanceState);
    }

    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, android.content.res.Configuration newConfig)
    {
        mFragmentManager.dispatchMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        onMultiWindowModeChanged(isInMultiWindowMode);
    }

    public void onMultiWindowModeChanged(boolean isInMultiWindowMode)
    {
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig)
    {
        mFragmentManager.dispatchPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        onPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode)
    {
    }

    public void enterPictureInPictureMode()
    {
        enterPictureInPictureMode(mPictureInPictureParams != null ? mPictureInPictureParams
                                                                  : new PictureInPictureParams.Builder().build());
    }

    public boolean enterPictureInPictureMode(PictureInPictureParams params)
    {
        if (params == null) throw new IllegalArgumentException("Expected non-null picture-in-picture params");

        android.util.Log.i("Activity", getClass().getName() + " asked for picture-in-picture, which this single-window device does not offer");
        return false;
    }

    public void setPictureInPictureParams(PictureInPictureParams params)
    {
        if (params == null) throw new IllegalArgumentException("Expected non-null picture-in-picture params");

        mPictureInPictureParams = params;
    }

    public int getMaxNumPictureInPictureActions()
    {
        return 0;
    }

    public boolean onPictureInPictureRequested()
    {
        return false;
    }

    public void onPictureInPictureUiStateChanged(PictureInPictureUiState state)
    {
    }

    public final void setMediaController(android.media.session.MediaController controller)
    {
        mMediaController = controller;
    }

    public final android.media.session.MediaController getMediaController()
    {
        return mMediaController;
    }

    public void onProvideAssistData(Bundle data)
    {
    }

    public void onProvideAssistContent(android.app.assist.AssistContent outContent)
    {
    }

    public void registerScreenCaptureCallback(java.util.concurrent.Executor executor, ScreenCaptureCallback callback)
    {
        if (executor == null || callback == null) throw new NullPointerException("executor and callback must not be null");

        synchronized (mScreenCaptureCallbacks)
        {
            mScreenCaptureCallbacks.add(new ScreenCaptureRegistration(executor, callback));
        }
    }

    public void unregisterScreenCaptureCallback(ScreenCaptureCallback callback)
    {
        synchronized (mScreenCaptureCallbacks)
        {
            for (int index = mScreenCaptureCallbacks.size() - 1; index >= 0; index--)
            {
                if (mScreenCaptureCallbacks.get(index).mCallback == callback) mScreenCaptureCallbacks.remove(index);
            }
        }
    }

    void dispatchScreenCaptured()
    {
        java.util.ArrayList<ScreenCaptureRegistration> registrations;
        synchronized (mScreenCaptureCallbacks)
        {
            registrations = new java.util.ArrayList<>(mScreenCaptureCallbacks);
        }

        for (final ScreenCaptureRegistration registration : registrations)
        {
            registration.mExecutor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    registration.mCallback.onScreenCaptured();
                }
            });
        }
    }

    public android.view.DragAndDropPermissions requestDragAndDropPermissions(android.view.DragEvent event)
    {
        if (event == null || event.getClipData() == null) return null;

        android.os.Parcel parcel = android.os.Parcel.obtain();
        try
        {
            parcel.writeBoolean(true);
            parcel.setDataPosition(0);
            return android.view.DragAndDropPermissions.CREATOR.createFromParcel(parcel);
        }
        finally
        {
            parcel.recycle();
        }
    }

    public void setEnterSharedElementCallback(SharedElementCallback callback)
    {
        mEnterSharedElementCallback = callback;
    }

    public void setExitSharedElementCallback(SharedElementCallback callback)
    {
        mExitSharedElementCallback = callback;
    }

    public void setLocusContext(android.content.LocusId locusId, Bundle bundle)
    {
        mLocusId = locusId;
        mLocusBundle = bundle;
    }

    public LoaderManager getLoaderManager()
    {
        if (mLoaderManager == null) mLoaderManager = new RuntimeLoaderManager(getClass().getName(), mLoadersStarted);

        return mLoaderManager;
    }

    private boolean mLoadersStarted;

    public void startActivityFromFragment(Fragment fragment, android.content.Intent intent, int requestCode)
    {
        startActivityFromFragment(fragment, intent, requestCode, null);
    }

    public void startActivityFromFragment(Fragment fragment, android.content.Intent intent, int requestCode, Bundle options)
    {
        if (requestCode < 0)
        {
            startActivity(intent, options);
            return;
        }

        startActivityForResult(intent, fragmentRequestCode(fragment, requestCode), options);
    }

    private int fragmentRequestCode(Fragment fragment, int requestCode)
    {
        int code = FRAGMENT_REQUEST_BASE + (mNextFragmentRequest++ & 0xffff);
        mFragmentRequests.put(code, new FragmentRequest(fragment, requestCode));
        return code;
    }

    void startIntentSenderFromFragment(Fragment fragment, android.content.IntentSender sender, int requestCode, android.content.Intent fillIn,
                                       int flagsMask, int flagsValues) throws android.content.IntentSender.SendIntentException
    {
        startIntentSenderInternal(sender, requestCode < 0 ? -1 : fragmentRequestCode(fragment, requestCode), fillIn, flagsMask, flagsValues);
    }

    public void startIntentSenderForResult(android.content.IntentSender intent, int requestCode, android.content.Intent fillInIntent,
                                           int flagsMask, int flagsValues, int extraFlags)
        throws android.content.IntentSender.SendIntentException
    {
        startIntentSenderInternal(intent, requestCode, fillInIntent, flagsMask, flagsValues);
    }

    public void startIntentSenderForResult(android.content.IntentSender intent, int requestCode, android.content.Intent fillInIntent,
                                           int flagsMask, int flagsValues, int extraFlags, Bundle options)
        throws android.content.IntentSender.SendIntentException
    {
        startIntentSenderInternal(intent, requestCode, fillInIntent, flagsMask, flagsValues);
    }

    public void startIntentSender(android.content.IntentSender intent, android.content.Intent fillInIntent, int flagsMask, int flagsValues,
                                  int extraFlags) throws android.content.IntentSender.SendIntentException
    {
        startIntentSenderInternal(intent, -1, fillInIntent, flagsMask, flagsValues);
    }

    public void startIntentSender(android.content.IntentSender intent, android.content.Intent fillInIntent, int flagsMask, int flagsValues,
                                  int extraFlags, Bundle options) throws android.content.IntentSender.SendIntentException
    {
        startIntentSenderInternal(intent, -1, fillInIntent, flagsMask, flagsValues);
    }

    private void startIntentSenderInternal(android.content.IntentSender sender, int requestCode, android.content.Intent fillIn, int flagsMask,
                                           int flagsValues) throws android.content.IntentSender.SendIntentException
    {
        if (sender == null) throw new android.content.IntentSender.SendIntentException("the IntentSender is null");

        PendingIntent pending = sender.getPendingIntent();

        if (pending != null && pending.isActivity() && pending.getIntent() != null)
        {
            android.content.Intent launch = new android.content.Intent(pending.getIntent());
            if (fillIn != null) launch.fillIn(fillIn, 0);
            launch.setFlags((launch.getFlags() & ~flagsMask) | (flagsValues & flagsMask));

            runtime.Activities.start(launch, requestCode);
            return;
        }

        sender.sendIntent(this, 0, fillIn, null, null);
    }

    public void onTopResumedActivityChanged(boolean isTopResumedActivity)
    {
    }

    public void onAttachFragment(Fragment fragment)
    {
    }

    public void startActivityForResult(android.content.Intent intent, int requestCode, Bundle options)
    {
        startActivityForResult(intent, requestCode);
    }

    public void startActivities(android.content.Intent[] intents, Bundle options)
    {
        startActivities(intents);
    }

    public boolean startActivityIfNeeded(android.content.Intent intent, int requestCode, Bundle options)
    {
        startActivityForResult(intent, requestCode);
        return true;
    }

    public boolean startActivityIfNeeded(android.content.Intent intent, int requestCode)
    {
        return startActivityIfNeeded(intent, requestCode, null);
    }

    public void overrideActivityTransition(int overrideType, int enterAnim, int exitAnim)
    {
    }

    public void overrideActivityTransition(int overrideType, int enterAnim, int exitAnim, int backgroundColor)
    {
    }

    public void clearOverrideActivityTransition(int overrideType)
    {
    }

    public android.net.Uri getReferrer()
    {
        return null;
    }

    public String getLaunchedFromPackage()
    {
        return null;
    }

    public void setVisible(boolean visible)
    {
        View decor = mWindow == null ? null : mWindow.getDecorView();
        if (decor != null) decor.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    public void finishAffinity()
    {
        finish();
    }

    public void finishAfterTransition()
    {
        finish();
    }

    public void finishActivity(int requestCode)
    {
    }

    public void finishAndRemoveTask()
    {
        finish();
    }

    public boolean isTaskRoot()
    {
        return runtime.Activities.depth() <= 1;
    }

    public final Activity getParent()
    {
        return null;
    }

    public String getLocalClassName()
    {
        String packageName = getPackageName();
        String className = getClass().getName();

        if (packageName == null || !className.startsWith(packageName)) return className;
        if (className.length() > packageName.length() && className.charAt(packageName.length()) == '.')
            return className.substring(packageName.length() + 1);

        return className;
    }

    public android.content.SharedPreferences getPreferences(int mode)
    {
        return getSharedPreferences(getLocalClassName(), mode);
    }

    public boolean isLaunchedFromBubble()
    {
        return false;
    }

    protected void onTitleChanged(CharSequence title, int color)
    {
    }

    protected void onChildTitleChanged(Activity childActivity, CharSequence title)
    {
    }

    public final void setProgress(int progress)
    {
    }

    public final void setProgressBarVisibility(boolean visible)
    {
    }

    public final void setProgressBarIndeterminateVisibility(boolean visible)
    {
    }

    private int mVolumeControlStream = -1;

    public final void setVolumeControlStream(int streamType)
    {
        mVolumeControlStream = streamType;
    }

    public final int getVolumeControlStream()
    {
        return mVolumeControlStream;
    }

    public void dump(String prefix, java.io.FileDescriptor fd, java.io.PrintWriter writer, String[] args)
    {
        if (writer == null) return;

        writer.println(prefix + "Activity " + getComponentName());
        mFragmentManager.dump(prefix + "  ", fd, writer, args);
        if (mLoaderManager != null) mLoaderManager.dump(prefix + "  ", fd, writer, args);
    }

    public boolean shouldUpRecreateTask(android.content.Intent targetIntent)
    {
        return false;
    }

    public boolean navigateUpTo(android.content.Intent upIntent)
    {
        finish();
        return true;
    }

    public boolean onNavigateUp()
    {
        android.content.Intent upIntent = getParentActivityIntent();
        if (upIntent == null) return false;

        return navigateUpTo(upIntent);
    }

    public android.content.Intent getParentActivityIntent()
    {
        return null;
    }

    public void postponeEnterTransition()
    {
    }

    public void startPostponedEnterTransition()
    {
    }

    public void setRecentsScreenshotEnabled(boolean enabled)
    {
    }

    public void setTranslucent(boolean translucent)
    {
    }

    public boolean isVoiceInteraction()
    {
        return false;
    }

    public boolean isVoiceInteractionRoot()
    {
        return false;
    }

    public boolean isImmersive()
    {
        return false;
    }

    public void setImmersive(boolean immersive)
    {
    }

    public boolean isActivityTransitionRunning()
    {
        return false;
    }

    public boolean releaseInstance()
    {
        return false;
    }

    public void onEnterAnimationComplete()
    {
    }

    public void onLocalVoiceInteractionStarted()
    {
    }

    public void onLocalVoiceInteractionStopped()
    {
    }

    public boolean onCreateThumbnail(android.graphics.Bitmap outBitmap, android.graphics.Canvas canvas)
    {
        return false;
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    public void setTitle(CharSequence title)
    {
        mTitle = title == null ? "" : title;
        if (mWindow != null) mWindow.setTitle(mTitle);
        if (mActionBar != null) mActionBar.setTitle(mTitle);
    }

    public void setTaskDescription(ActivityManager.TaskDescription taskDescription)
    {
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
        mFragmentManager.dispatchTrimMemory(level);
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
        initWindowDecorActionBar();
        mWindow.setContentView(view);
    }

    public void setContentView(View view, ViewGroup.LayoutParams layoutParams)
    {
        initWindowDecorActionBar();
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
    boolean mStarted;
    boolean mResumed;
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
        if (mActionBar != null) mActionBar.invalidateOptionsMenu();
    }

    public void onConfigurationChanged(android.content.res.Configuration newConfig)
    {
        mFragmentManager.dispatchConfigurationChanged(newConfig);
    }

    public void onLowMemory()
    {
        mFragmentManager.dispatchLowMemory();
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
        Bundle fragments = mFragmentManager.saveAllState();
        if (fragments != null) outState.putBundle(RuntimeFragmentManager.SAVED_FRAGMENTS, fragments);
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
        FragmentRequest request = mFragmentRequests.get(requestCode);

        if (request != null)
        {
            mFragmentRequests.remove(requestCode);
            request.mFragment.onActivityResult(request.mRequestCode, resultCode, data);
            return;
        }

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

        if (mFragmentManager.handleBackPressed()) return;

        finish();
    }

    public final void requestPermissions(String[] permissions, int requestCode)
    {
        requestPermissionsFor(null, permissions, requestCode);
    }

    public final void requestPermissions(String[] permissions, int requestCode, int deviceId)
    {
        requestPermissionsFor(null, permissions, requestCode);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults, int deviceId)
    {
        onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    void requestPermissionsFromFragment(Fragment fragment, String[] permissions, int requestCode)
    {
        requestPermissionsFor(fragment, permissions, requestCode);
    }

    private void requestPermissionsFor(final Fragment fragment, final String[] permissions, final int requestCode)
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
                if (fragment != null) fragment.onRequestPermissionsResult(requestCode, requested, results);
                else onRequestPermissionsResult(requestCode, requested, results);
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
        Bundle fragments = savedInstanceState == null ? null : savedInstanceState.getBundle(RuntimeFragmentManager.SAVED_FRAGMENTS);
        if (fragments != null) mFragmentManager.restoreAllState(fragments);

        mFragmentManager.dispatchCreate();
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
        mFragmentManager.dispatchCreate();
        mFragmentManager.dispatchActivityCreated();

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
        mFragmentManager.dispatchStart();
        mLoadersStarted = true;
        if (mLoaderManager != null) mLoaderManager.doStart();

        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPreStarted(this);
            callback.onActivityStarted(this);
            callback.onActivityPostStarted(this);
        }
    }

    void dispatchLifecycleResumed()
    {
        mFragmentManager.dispatchResume();
        onPostResume();

        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPreResumed(this);
            callback.onActivityResumed(this);
            callback.onActivityPostResumed(this);
        }
    }

    void dispatchLifecyclePaused()
    {
        mFragmentManager.dispatchPause();

        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPrePaused(this);
            callback.onActivityPaused(this);
            callback.onActivityPostPaused(this);
        }
    }

    void dispatchLifecycleStopped()
    {
        mFragmentManager.dispatchStop();
        mLoadersStarted = false;
        if (mLoaderManager != null) mLoaderManager.doStop();

        for (Application.ActivityLifecycleCallbacks callback : collectLifecycleCallbacks())
        {
            callback.onActivityPreStopped(this);
            callback.onActivityStopped(this);
            callback.onActivityPostStopped(this);
        }
    }

    void dispatchLifecycleDestroyed()
    {
        mFragmentManager.dispatchDestroy();
        mFragmentRequests.clear();

        if (mLoaderManager != null)
        {
            mLoaderManager.doDestroy();
            mLoaderManager = null;
        }

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
