package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.RuntimeWindow;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class Dialog implements DialogInterface, Window.Callback, KeyEvent.Callback, View.OnCreateContextMenuListener
{
    private static final String TAG = "Dialog";
    private static final float DEFAULT_DIM_AMOUNT = 0.6f;
    private static final float DEFAULT_MINIMUM_WIDTH_FRACTION = 0.65f;
    private static final String SAVED_SHOWING = "android:dialogShowing";

    private final Context mContext;
    private final Window mWindow;
    private final WindowManager mWindowManager;
    private final Handler mHandler;
    private final KeyEvent.DispatcherState mKeyDispatchState = new KeyEvent.DispatcherState();
    private final android.window.RuntimeBackDispatcher mBackDispatcher = new android.window.RuntimeBackDispatcher();

    private View mDecor;
    private Activity mOwnerActivity;
    private boolean mCreated;
    private boolean mShowing;
    private boolean mCanceled;
    private boolean mCancelable = true;
    private int mVolumeControlStream = -1;

    private OnCancelListener mCancelListener;
    private OnDismissListener mDismissListener;
    private OnShowListener mShowListener;
    private OnKeyListener mOnKeyListener;
    private Message mCancelMessage;
    private Message mDismissMessage;
    private ActionMode mActionMode;

    public Dialog(Context context)
    {
        this(context, 0, true);
    }

    public Dialog(Context context, int themeResId)
    {
        this(context, themeResId, true);
    }

    Dialog(Context context, int themeResId, boolean createContextThemeWrapper)
    {
        if (createContextThemeWrapper)
        {
            if (themeResId == 0) themeResId = resolveDialogTheme(context);
            mContext = themeResId == 0 ? context : new ContextThemeWrapper(context, themeResId);
        }
        else
        {
            mContext = context;
        }

        mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());

        mWindow = new RuntimeWindow(mContext);
        mWindow.setCallback(this);
        applyWindowTheme();

        if (context instanceof Activity) mOwnerActivity = (Activity)context;
    }

    protected Dialog(Context context, boolean cancelable, OnCancelListener cancelListener)
    {
        this(context);
        mCancelable = cancelable;
        setOnCancelListener(cancelListener);
    }

    protected Dialog(Context context, boolean cancelable, Message cancelCallback)
    {
        this(context);
        mCancelable = cancelable;
        mCancelMessage = cancelCallback;
    }

    private static int resolveDialogTheme(Context context)
    {
        int attribute = context.getResources().getIdentifier("dialogTheme", "attr", "android");
        if (attribute == 0) return 0;

        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(attribute, value, true) ? value.resourceId : 0;
    }

    private TypedValue themeValue(String name)
    {
        int attribute = mContext.getResources().getIdentifier(name, "attr", "android");
        if (attribute == 0) return null;

        TypedValue value = new TypedValue();
        return mContext.getTheme().resolveAttribute(attribute, value, true) ? value : null;
    }

    private void applyWindowTheme()
    {
        WindowManager.LayoutParams attributes = mWindow.getAttributes();
        attributes.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        attributes.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        attributes.gravity = android.view.Gravity.CENTER;
        attributes.type = WindowManager.LayoutParams.TYPE_APPLICATION;

        TypedValue dimEnabled = themeValue("backgroundDimEnabled");
        boolean dim = dimEnabled == null || dimEnabled.type != TypedValue.TYPE_INT_BOOLEAN || dimEnabled.data != 0;
        if (dim) attributes.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;

        TypedValue dimAmount = themeValue("backgroundDimAmount");
        attributes.dimAmount = dimAmount != null && dimAmount.type == TypedValue.TYPE_FLOAT ? dimAmount.getFloat() : DEFAULT_DIM_AMOUNT;

        TypedValue closeOutside = themeValue("windowCloseOnTouchOutside");
        if (closeOutside != null && closeOutside.type == TypedValue.TYPE_INT_BOOLEAN) mWindow.setCloseOnTouchOutside(closeOutside.data != 0);

        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        boolean portrait = screenWidth < screenHeight;

        TypedValue minimum = themeValue(portrait ? "windowMinWidthMinor" : "windowMinWidthMajor");
        float minimumWidth = screenWidth * DEFAULT_MINIMUM_WIDTH_FRACTION;

        if (minimum != null && minimum.type == TypedValue.TYPE_FRACTION) minimumWidth = minimum.getFraction(screenWidth, screenWidth);
        else if (minimum != null && minimum.type == TypedValue.TYPE_DIMENSION) minimumWidth = minimum.getDimension(mContext.getResources().getDisplayMetrics());

        mWindow.getDecorView().setMinimumWidth(Math.round(minimumWidth));

        TypedValue background = themeValue("windowBackground");
        if (background != null && background.resourceId != 0)
        {
            try
            {
                mWindow.setBackgroundDrawable(mContext.getResources().getDrawable(background.resourceId, mContext.getTheme()));
            }
            catch (RuntimeException unusable)
            {
                android.util.Log.w(TAG, "the dialog theme's window background could not be loaded", unusable);
            }
        }
    }

    public final Context getContext()
    {
        return mContext;
    }

    public final void setOwnerActivity(Activity activity)
    {
        mOwnerActivity = activity;
    }

    public final Activity getOwnerActivity()
    {
        return mOwnerActivity;
    }

    public boolean isShowing()
    {
        return mDecor != null && mShowing && mDecor.getVisibility() == View.VISIBLE;
    }

    public void create()
    {
        if (!mCreated) dispatchOnCreate(null);
    }

    public void show()
    {
        if (mShowing)
        {
            if (mDecor != null) mDecor.setVisibility(View.VISIBLE);
            return;
        }

        mCanceled = false;

        if (!mCreated) dispatchOnCreate(null);

        onStart();

        mDecor = mWindow.getDecorView();

        WindowManager.LayoutParams attributes = new WindowManager.LayoutParams();
        attributes.copyFrom(mWindow.getAttributes());

        try
        {
            mWindowManager.addView(mDecor, attributes);
            mShowing = true;
        }
        catch (RuntimeException failure)
        {
            android.util.Log.e(TAG, "the dialog window could not be added", failure);
            mDecor = null;
            return;
        }

        if (mShowListener != null)
        {
            final OnShowListener listener = mShowListener;
            mHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    listener.onShow(Dialog.this);
                }
            });
        }
    }

    public void hide()
    {
        if (mDecor != null) mDecor.setVisibility(View.GONE);
    }

    @Override
    public void dismiss()
    {
        if (Looper.myLooper() == mHandler.getLooper())
        {
            dismissDialog();
            return;
        }

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                dismissDialog();
            }
        });
    }

    void dismissDialog()
    {
        if (mDecor == null || !mShowing) return;

        try
        {
            mWindowManager.removeViewImmediate(mDecor);
        }
        finally
        {
            if (mActionMode != null) mActionMode.finish();

            mDecor = null;
            mWindow.closeAllPanels();
            onStop();
            mShowing = false;

            if (mDismissMessage != null) Message.obtain(mDismissMessage).sendToTarget();
            if (mDismissListener != null) mDismissListener.onDismiss(this);
        }
    }

    void dispatchOnCreate(Bundle savedInstanceState)
    {
        if (mCreated) return;

        onCreate(savedInstanceState);
        mCreated = true;
    }

    protected void onCreate(Bundle savedInstanceState)
    {
    }

    protected void onStart()
    {
    }

    protected void onStop()
    {
    }

    public Bundle onSaveInstanceState()
    {
        Bundle state = new Bundle();
        state.putBoolean(SAVED_SHOWING, mShowing);
        return state;
    }

    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        if (savedInstanceState == null) return;

        dispatchOnCreate(savedInstanceState);
        if (savedInstanceState.getBoolean(SAVED_SHOWING)) show();
    }

    public Window getWindow()
    {
        return mWindow;
    }

    public View getCurrentFocus()
    {
        return mWindow.getCurrentFocus();
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T findViewById(int id)
    {
        return (T)mWindow.findViewById(id);
    }

    public final <T extends View> T requireViewById(int id)
    {
        T view = findViewById(id);
        if (view == null) throw new IllegalArgumentException("ID does not reference a View inside this Dialog");
        return view;
    }

    public void setContentView(int layoutResID)
    {
        setContentView(getLayoutInflater().inflate(layoutResID, (ViewGroup)mWindow.findViewById(Window.ID_ANDROID_CONTENT), false));
    }

    public void setContentView(View view)
    {
        mWindow.setContentView(view);
    }

    public void setContentView(View view, ViewGroup.LayoutParams params)
    {
        mWindow.setContentView(view, params);
    }

    public void addContentView(View view, ViewGroup.LayoutParams params)
    {
        View content = mWindow.findViewById(Window.ID_ANDROID_CONTENT);
        if (content instanceof ViewGroup) ((ViewGroup)content).addView(view, params);
    }

    public void setTitle(CharSequence title)
    {
        mWindow.setTitle(title);
        mWindow.getAttributes().setTitle(title);
    }

    public void setTitle(int titleId)
    {
        setTitle(mContext.getText(titleId));
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)
        {
            event.startTracking();
            return true;
        }

        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event)
    {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) && event.isTracking() && !event.isCanceled())
        {
            onBackPressed();
            return true;
        }

        return false;
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event)
    {
        return false;
    }

    public void onBackPressed()
    {
        if (mBackDispatcher.dispatchBack()) return;
        if (mCancelable) cancel();
    }

    public boolean onKeyShortcut(int keyCode, KeyEvent event)
    {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        if (mCancelable && mShowing && mWindow.shouldCloseOnTouch(mContext, event))
        {
            cancel();
            return true;
        }

        return false;
    }

    public boolean onTrackballEvent(MotionEvent event)
    {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent event)
    {
        return false;
    }

    public void onWindowAttributesChanged(WindowManager.LayoutParams params)
    {
        if (mDecor == null) return;

        try
        {
            mWindowManager.updateViewLayout(mDecor, params);
        }
        catch (RuntimeException stale)
        {
            android.util.Log.w(TAG, "the dialog window could not take its new attributes", stale);
        }
    }

    public void onContentChanged()
    {
    }

    public void onWindowFocusChanged(boolean hasFocus)
    {
    }

    public void onAttachedToWindow()
    {
    }

    public void onDetachedFromWindow()
    {
    }

    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (mOnKeyListener != null && mOnKeyListener.onKey(this, event.getKeyCode(), event)) return true;
        if (mWindow.superDispatchKeyEvent(event)) return true;

        return event.dispatch(this, mKeyDispatchState, this);
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent event)
    {
        if (mWindow.superDispatchKeyShortcutEvent(event)) return true;
        return onKeyShortcut(event.getKeyCode(), event);
    }

    public boolean dispatchTouchEvent(MotionEvent event)
    {
        if (mWindow.superDispatchTouchEvent(event)) return true;
        return onTouchEvent(event);
    }

    public boolean dispatchTrackballEvent(MotionEvent event)
    {
        if (mWindow.superDispatchTrackballEvent(event)) return true;
        return onTrackballEvent(event);
    }

    public boolean dispatchGenericMotionEvent(MotionEvent event)
    {
        if (mWindow.superDispatchGenericMotionEvent(event)) return true;
        return onGenericMotionEvent(event);
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event)
    {
        event.setClassName(getClass().getName());
        event.setPackageName(mContext.getPackageName());
        return false;
    }

    public View onCreatePanelView(int featureId)
    {
        return null;
    }

    public boolean onCreatePanelMenu(int featureId, Menu menu)
    {
        return featureId == Window.FEATURE_OPTIONS_PANEL && onCreateOptionsMenu(menu);
    }

    public boolean onPreparePanel(int featureId, View view, Menu menu)
    {
        return featureId != Window.FEATURE_OPTIONS_PANEL || (onPrepareOptionsMenu(menu) && menu.hasVisibleItems());
    }

    public boolean onMenuOpened(int featureId, Menu menu)
    {
        return true;
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
        return false;
    }

    public void onPanelClosed(int featureId, Menu menu)
    {
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        return false;
    }

    public void onOptionsMenuClosed(Menu menu)
    {
    }

    public void openOptionsMenu()
    {
    }

    public void closeOptionsMenu()
    {
    }

    public void invalidateOptionsMenu()
    {
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
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
        view.showContextMenu();
    }

    public boolean onContextItemSelected(MenuItem item)
    {
        return false;
    }

    public void onContextMenuClosed(Menu menu)
    {
    }

    public boolean onSearchRequested(SearchEvent searchEvent)
    {
        return onSearchRequested();
    }

    public boolean onSearchRequested()
    {
        return false;
    }

    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback)
    {
        return null;
    }

    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type)
    {
        return null;
    }

    public void onActionModeStarted(ActionMode mode)
    {
        mActionMode = mode;
    }

    public void onActionModeFinished(ActionMode mode)
    {
        if (mode == mActionMode) mActionMode = null;
    }

    public void takeKeyEvents(boolean get)
    {
    }

    public final boolean requestWindowFeature(int featureId)
    {
        return mWindow.requestFeature(featureId);
    }

    public final void setFeatureDrawableResource(int featureId, int resId)
    {
    }

    public final void setFeatureDrawableUri(int featureId, android.net.Uri uri)
    {
    }

    public final void setFeatureDrawable(int featureId, android.graphics.drawable.Drawable drawable)
    {
    }

    public final void setFeatureDrawableAlpha(int featureId, int alpha)
    {
    }

    public LayoutInflater getLayoutInflater()
    {
        return mWindow.getLayoutInflater();
    }

    public void setCancelable(boolean flag)
    {
        mCancelable = flag;
    }

    public void setCanceledOnTouchOutside(boolean cancel)
    {
        if (cancel && !mCancelable) mCancelable = true;
        mWindow.setCloseOnTouchOutside(cancel);
    }

    @Override
    public void cancel()
    {
        if (!mCanceled)
        {
            mCanceled = true;

            if (mCancelMessage != null) Message.obtain(mCancelMessage).sendToTarget();
            if (mCancelListener != null) mCancelListener.onCancel(this);
        }

        dismiss();
    }

    public void setOnCancelListener(OnCancelListener listener)
    {
        mCancelListener = listener;
    }

    public void setCancelMessage(Message message)
    {
        mCancelMessage = message;
    }

    public void setOnDismissListener(OnDismissListener listener)
    {
        mDismissListener = listener;
    }

    public void setOnShowListener(OnShowListener listener)
    {
        mShowListener = listener;
    }

    public void setDismissMessage(Message message)
    {
        mDismissMessage = message;
    }

    public final void setVolumeControlStream(int streamType)
    {
        mVolumeControlStream = streamType;
    }

    public final int getVolumeControlStream()
    {
        return mVolumeControlStream;
    }

    public void setOnKeyListener(OnKeyListener listener)
    {
        mOnKeyListener = listener;
    }

    public android.window.OnBackInvokedDispatcher getOnBackInvokedDispatcher()
    {
        return mBackDispatcher;
    }
}
