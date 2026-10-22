package android.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;

import java.util.List;

public abstract class Window
{
    public static final int ID_ANDROID_CONTENT = 0x01020002;

    public static final String STATUS_BAR_BACKGROUND_TRANSITION_NAME = "android:status:background";
    public static final String NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME = "android:navigation:background";

    public static final int FEATURE_OPTIONS_PANEL = 0;
    public static final int FEATURE_NO_TITLE = 1;
    public static final int FEATURE_PROGRESS = 2;
    public static final int FEATURE_LEFT_ICON = 3;
    public static final int FEATURE_RIGHT_ICON = 4;
    public static final int FEATURE_INDETERMINATE_PROGRESS = 5;
    public static final int FEATURE_CONTEXT_MENU = 6;
    public static final int FEATURE_CUSTOM_TITLE = 7;
    public static final int FEATURE_ACTION_BAR = 8;
    public static final int FEATURE_ACTION_BAR_OVERLAY = 9;
    public static final int FEATURE_ACTION_MODE_OVERLAY = 10;
    public static final int FEATURE_SWIPE_TO_DISMISS = 11;
    public static final int FEATURE_CONTENT_TRANSITIONS = 12;
    public static final int FEATURE_ACTIVITY_TRANSITIONS = 13;
    protected static final int DEFAULT_FEATURES = (1 << FEATURE_OPTIONS_PANEL) | (1 << FEATURE_CONTEXT_MENU);

    public static final int PROGRESS_VISIBILITY_ON = -1;
    public static final int PROGRESS_VISIBILITY_OFF = -2;
    public static final int PROGRESS_INDETERMINATE_ON = -3;
    public static final int PROGRESS_INDETERMINATE_OFF = -4;
    public static final int PROGRESS_START = 0;
    public static final int PROGRESS_END = 10000;
    public static final int PROGRESS_SECONDARY_START = 20000;
    public static final int PROGRESS_SECONDARY_END = 30000;

    public static final int DECOR_CAPTION_SHADE_AUTO = 0;
    public static final int DECOR_CAPTION_SHADE_LIGHT = 1;
    public static final int DECOR_CAPTION_SHADE_DARK = 2;

    private static final long DEFAULT_TRANSITION_BACKGROUND_FADE_DURATION = 300;

    private static StyleableSet sWindowStyleable;

    private final Context mContext;
    private CharSequence mTitle = "";
    private Callback mCallback;
    private boolean mDecorFitsSystemWindows = true;
    private final WindowManager.LayoutParams mAttributes = new WindowManager.LayoutParams();
    private int mFeatures = DEFAULT_FEATURES;
    private int mLocalFeatures = DEFAULT_FEATURES;
    private int mForcedWindowFlags;
    private boolean mHasSoftInputMode;
    private int mDefaultWindowFormat = android.graphics.PixelFormat.OPAQUE;
    private boolean mHaveWindowFormat;
    private boolean mCloseOnTouchOutside;
    private boolean mSetCloseOnTouchOutside;
    private Window mContainer;
    private Window mActiveChild;
    private boolean mHasChildren;
    private boolean mIsActive;
    private WindowManager mWindowManager;
    private IBinder mAppToken;
    private String mAppName;
    private boolean mHardwareAccelerated;
    private TypedArray mWindowStyle;
    private int mColorMode;
    private float mDesiredHdrHeadroom;
    private boolean mFrameRateBoostOnTouch = true;
    private boolean mFrameRatePowerSavingsBalanced = true;
    private int mNavigationBarDividerColor;
    private boolean mStatusBarContrastEnforced;
    private boolean mNavigationBarContrastEnforced = true;
    private OnRestrictedCaptionAreaChangedListener mRestrictedCaptionAreaListener;
    private final android.window.RuntimeBackDispatcher mBackDispatcher = new android.window.RuntimeBackDispatcher();

    private Transition mEnterTransition;
    private Transition mReturnTransition;
    private boolean mReturnTransitionSet;
    private Transition mExitTransition;
    private Transition mReenterTransition;
    private boolean mReenterTransitionSet;
    private Transition mSharedElementEnterTransition;
    private Transition mSharedElementReturnTransition;
    private boolean mSharedElementReturnTransitionSet;
    private Transition mSharedElementExitTransition;
    private Transition mSharedElementReenterTransition;
    private boolean mSharedElementReenterTransitionSet;
    private boolean mAllowEnterTransitionOverlap = true;
    private boolean mAllowReturnTransitionOverlap = true;
    private boolean mSharedElementsUseOverlay = true;
    private long mBackgroundFadeDuration = -1;
    private TransitionManager mTransitionManager;
    private Scene mContentScene;

    public Window(Context context)
    {
        mContext = context;
    }

    public final Context getContext()
    {
        return mContext;
    }

    public final TypedArray getWindowStyle()
    {
        synchronized (this)
        {
            if (mWindowStyle == null)
            {
                if (sWindowStyleable == null)
                {
                    sWindowStyleable = StyleableSet.of(mContext.getResources(), "windowBackground", "windowFrame", "windowNoTitle",
                                                       "windowFullscreen", "windowIsFloating", "windowIsTranslucent",
                                                       "windowContentOverlay", "windowAnimationStyle", "windowSoftInputMode",
                                                       "windowActionBar", "windowActionBarOverlay", "windowCloseOnTouchOutside",
                                                       "windowTranslucentStatus", "windowTranslucentNavigation", "statusBarColor",
                                                       "navigationBarColor", "windowLightStatusBar", "windowLightNavigationBar",
                                                       "windowLayoutInDisplayCutoutMode", "backgroundDimEnabled",
                                                       "backgroundDimAmount", "windowMinWidthMajor", "windowMinWidthMinor");
                }

                mWindowStyle = mContext.obtainStyledAttributes(sWindowStyleable.attributes());
            }

            return mWindowStyle;
        }
    }

    final boolean themeBoolean(String attributeName, boolean fallback)
    {
        int attribute = mContext.getResources().getIdentifier(attributeName, "attr", "android");
        if (attribute == 0) return fallback;

        android.util.TypedValue value = new android.util.TypedValue();
        if (!mContext.getTheme().resolveAttribute(attribute, value, true)) return fallback;
        if (value.type != android.util.TypedValue.TYPE_INT_BOOLEAN) return fallback;

        return value.data != 0;
    }

    public void setContainer(Window container)
    {
        mContainer = container;

        if (container == null) return;

        mFeatures |= 1 << FEATURE_NO_TITLE;
        mLocalFeatures |= 1 << FEATURE_NO_TITLE;
        container.mHasChildren = true;
    }

    public final Window getContainer()
    {
        return mContainer;
    }

    public final boolean hasChildren()
    {
        return mHasChildren;
    }

    public void setWindowManager(WindowManager windowManager, IBinder appToken, String appName)
    {
        setWindowManager(windowManager, appToken, appName, false);
    }

    public void setWindowManager(WindowManager windowManager, IBinder appToken, String appName, boolean hardwareAccelerated)
    {
        mAppToken = appToken;
        mAppName = appName;
        mHardwareAccelerated = hardwareAccelerated;
        mWindowManager = windowManager;
    }

    public WindowManager getWindowManager()
    {
        if (mWindowManager != null) return mWindowManager;

        return (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    public void setCallback(Callback callback)
    {
        mCallback = callback;
    }

    public final Callback getCallback()
    {
        return mCallback;
    }

    public final void addOnFrameMetricsAvailableListener(OnFrameMetricsAvailableListener listener, Handler handler)
    {
        if (listener == null) throw new NullPointerException("listener cannot be null");

        View decorView = getDecorView();
        if (decorView == null) throw new IllegalStateException("can't observe a Window without an attached view");

        Handler target = handler == null ? new Handler(android.os.Looper.getMainLooper()) : handler;
        ViewRootImpl.addFrameMetricsObserver(new ViewRootImpl.FrameMetricsObserver(this, listener, target));
    }

    public final void removeOnFrameMetricsAvailableListener(OnFrameMetricsAvailableListener listener)
    {
        if (listener == null) return;

        ViewRootImpl.removeFrameMetricsObserver(listener);
    }

    public final void setRestrictedCaptionAreaListener(OnRestrictedCaptionAreaChangedListener listener)
    {
        mRestrictedCaptionAreaListener = listener;
    }

    public final void makeActive()
    {
        if (mContainer != null)
        {
            if (mContainer.mActiveChild != null) mContainer.mActiveChild.mIsActive = false;
            mContainer.mActiveChild = this;
        }

        mIsActive = true;
        onActive();
    }

    public final boolean isActive()
    {
        return mIsActive || mContainer == null;
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T findViewById(int id)
    {
        return (T)getDecorView().findViewById(id);
    }

    public final <T extends View> T requireViewById(int id)
    {
        T view = findViewById(id);
        if (view == null) throw new IllegalArgumentException("ID does not reference a View inside this Window");
        return view;
    }

    public void setLayout(int width, int height)
    {
        mAttributes.width = width;
        mAttributes.height = height;
        dispatchWindowAttributesChanged();
    }

    public void setGravity(int gravity)
    {
        mAttributes.gravity = gravity;
        dispatchWindowAttributesChanged();
    }

    public void setType(int type)
    {
        mAttributes.type = type;
        dispatchWindowAttributesChanged();
    }

    public void setFormat(int format)
    {
        if (format != android.graphics.PixelFormat.UNKNOWN)
        {
            mAttributes.format = format;
            mHaveWindowFormat = true;
        }
        else
        {
            mAttributes.format = mDefaultWindowFormat;
            mHaveWindowFormat = false;
        }

        dispatchWindowAttributesChanged();
    }

    public int getFormat()
    {
        return mAttributes.format;
    }

    protected void setDefaultWindowFormat(int format)
    {
        mDefaultWindowFormat = format;

        if (mHaveWindowFormat) return;

        mAttributes.format = format;
        dispatchWindowAttributesChanged();
    }

    public void setWindowAnimations(int resId)
    {
        mAttributes.windowAnimations = resId;
        dispatchWindowAttributesChanged();
    }

    public void setSoftInputMode(int mode)
    {
        if (mode != 0)
        {
            mAttributes.softInputMode = mode;
            mHasSoftInputMode = true;
        }
        else
        {
            mHasSoftInputMode = false;
        }

        dispatchWindowAttributesChanged();
    }

    protected final boolean hasSoftInputMode()
    {
        return mHasSoftInputMode;
    }

    public void addFlags(int flags)
    {
        setFlags(flags, flags);
    }

    public void clearFlags(int flags)
    {
        setFlags(0, flags);
    }

    public void setFlags(int flags, int mask)
    {
        mAttributes.flags = (mAttributes.flags & ~mask) | (flags & mask);
        mForcedWindowFlags |= mask;
        dispatchWindowAttributesChanged();
    }

    protected final int getForcedWindowFlags()
    {
        return mForcedWindowFlags;
    }

    public void setColorMode(int colorMode)
    {
        mColorMode = colorMode;
        mAttributes.setColorMode(colorMode);
        dispatchWindowAttributesChanged();
    }

    public int getColorMode()
    {
        return mColorMode;
    }

    public boolean isWideColorGamut()
    {
        return mColorMode == android.content.pm.ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
               && getContext().getResources().getConfiguration().isScreenWideColorGamut();
    }

    public void setDesiredHdrHeadroom(float desiredHeadroom)
    {
        if (!Float.isFinite(desiredHeadroom) || (desiredHeadroom != 0 && (desiredHeadroom < 1.0f || desiredHeadroom > 10000.0f)))
        {
            throw new IllegalArgumentException("desiredHeadroom must be 0 or within [1.0, 10000.0], received: " + desiredHeadroom);
        }

        mDesiredHdrHeadroom = desiredHeadroom;
    }

    public float getDesiredHdrHeadroom()
    {
        return mDesiredHdrHeadroom;
    }

    public void setFrameRateBoostOnTouchEnabled(boolean enabled)
    {
        mFrameRateBoostOnTouch = enabled;
    }

    public boolean getFrameRateBoostOnTouchEnabled()
    {
        return mFrameRateBoostOnTouch;
    }

    public void setFrameRatePowerSavingsBalanced(boolean enabled)
    {
        mFrameRatePowerSavingsBalanced = enabled;
    }

    public boolean isFrameRatePowerSavingsBalanced()
    {
        return mFrameRatePowerSavingsBalanced;
    }

    public void setDimAmount(float amount)
    {
        mAttributes.dimAmount = amount;
        dispatchWindowAttributesChanged();
    }

    public void setDecorFitsSystemWindows(boolean decorFitsSystemWindows)
    {
        mDecorFitsSystemWindows = decorFitsSystemWindows;
    }

    public boolean getDecorFitsSystemWindows()
    {
        return mDecorFitsSystemWindows;
    }

    public void setAttributes(WindowManager.LayoutParams attributes)
    {
        if (attributes == null) return;

        if (attributes != mAttributes) mAttributes.copyFrom(attributes);
        dispatchWindowAttributesChanged();
    }

    public final WindowManager.LayoutParams getAttributes()
    {
        return mAttributes;
    }

    protected void dispatchWindowAttributesChanged()
    {
        Callback callback = getCallback();
        if (callback != null) callback.onWindowAttributesChanged(mAttributes);
    }

    protected final int getFeatures()
    {
        return mFeatures;
    }

    public static int getDefaultFeatures(Context context)
    {
        return DEFAULT_FEATURES;
    }

    public boolean hasFeature(int feature)
    {
        return (getFeatures() & (1 << feature)) != 0;
    }

    protected final int getLocalFeatures()
    {
        return mLocalFeatures;
    }

    public boolean requestFeature(int featureId)
    {
        int flag = 1 << featureId;
        boolean already = (mFeatures & flag) != 0;

        mFeatures |= flag;
        mLocalFeatures |= mContainer != null ? (flag & ~mContainer.mFeatures) : flag;

        return !already || featureId == FEATURE_NO_TITLE;
    }

    public void setUiOptions(int uiOptions)
    {
    }

    public void setUiOptions(int uiOptions, int mask)
    {
    }

    public void setIcon(int resId)
    {
    }

    public void setLogo(int resId)
    {
    }

    public void setLocalFocus(boolean hasFocus, boolean inTouchMode)
    {
    }

    public void injectInputEvent(InputEvent event)
    {
        if (event instanceof KeyEvent)
        {
            superDispatchKeyEvent((KeyEvent)event);
            return;
        }

        if (event instanceof MotionEvent) superDispatchTouchEvent((MotionEvent)event);
    }

    public TransitionManager getTransitionManager()
    {
        return mTransitionManager;
    }

    public void setTransitionManager(TransitionManager transitionManager)
    {
        mTransitionManager = transitionManager;
    }

    public Scene getContentScene()
    {
        return mContentScene;
    }

    final void setContentScene(Scene scene)
    {
        mContentScene = scene;
    }

    public void setEnterTransition(Transition transition)
    {
        mEnterTransition = transition;
    }

    public Transition getEnterTransition()
    {
        return mEnterTransition;
    }

    public void setReturnTransition(Transition transition)
    {
        mReturnTransition = transition;
        mReturnTransitionSet = true;
    }

    public Transition getReturnTransition()
    {
        return mReturnTransitionSet ? mReturnTransition : getEnterTransition();
    }

    public void setExitTransition(Transition transition)
    {
        mExitTransition = transition;
    }

    public Transition getExitTransition()
    {
        return mExitTransition;
    }

    public void setReenterTransition(Transition transition)
    {
        mReenterTransition = transition;
        mReenterTransitionSet = true;
    }

    public Transition getReenterTransition()
    {
        return mReenterTransitionSet ? mReenterTransition : getExitTransition();
    }

    public void setSharedElementEnterTransition(Transition transition)
    {
        mSharedElementEnterTransition = transition;
    }

    public Transition getSharedElementEnterTransition()
    {
        return mSharedElementEnterTransition;
    }

    public void setSharedElementReturnTransition(Transition transition)
    {
        mSharedElementReturnTransition = transition;
        mSharedElementReturnTransitionSet = true;
    }

    public Transition getSharedElementReturnTransition()
    {
        return mSharedElementReturnTransitionSet ? mSharedElementReturnTransition : getSharedElementEnterTransition();
    }

    public void setSharedElementExitTransition(Transition transition)
    {
        mSharedElementExitTransition = transition;
    }

    public Transition getSharedElementExitTransition()
    {
        return mSharedElementExitTransition;
    }

    public void setSharedElementReenterTransition(Transition transition)
    {
        mSharedElementReenterTransition = transition;
        mSharedElementReenterTransitionSet = true;
    }

    public Transition getSharedElementReenterTransition()
    {
        return mSharedElementReenterTransitionSet ? mSharedElementReenterTransition : getSharedElementExitTransition();
    }

    public void setAllowEnterTransitionOverlap(boolean allow)
    {
        mAllowEnterTransitionOverlap = allow;
    }

    public boolean getAllowEnterTransitionOverlap()
    {
        return mAllowEnterTransitionOverlap;
    }

    public void setAllowReturnTransitionOverlap(boolean allow)
    {
        mAllowReturnTransitionOverlap = allow;
    }

    public boolean getAllowReturnTransitionOverlap()
    {
        return mAllowReturnTransitionOverlap;
    }

    public long getTransitionBackgroundFadeDuration()
    {
        return mBackgroundFadeDuration < 0 ? DEFAULT_TRANSITION_BACKGROUND_FADE_DURATION : mBackgroundFadeDuration;
    }

    public void setTransitionBackgroundFadeDuration(long fadeDurationMillis)
    {
        if (fadeDurationMillis < 0) throw new IllegalArgumentException("negative durations are not allowed");

        mBackgroundFadeDuration = fadeDurationMillis;
    }

    public void setSharedElementsUseOverlay(boolean sharedElementsUseOverlay)
    {
        mSharedElementsUseOverlay = sharedElementsUseOverlay;
    }

    public boolean getSharedElementsUseOverlay()
    {
        return mSharedElementsUseOverlay;
    }

    public void setCloseOnTouchOutside(boolean close)
    {
        mCloseOnTouchOutside = close;
        mSetCloseOnTouchOutside = true;
    }

    public void setCloseOnTouchOutsideIfNotSet(boolean close)
    {
        if (!mSetCloseOnTouchOutside) mCloseOnTouchOutside = close;
    }

    public boolean shouldCloseOnTouch(Context context, MotionEvent event)
    {
        if (!mCloseOnTouchOutside || event.getAction() != MotionEvent.ACTION_DOWN) return false;

        View decor = peekDecorView();
        if (decor == null) return false;

        int x = (int)event.getX();
        int y = (int)event.getY();
        return x < 0 || y < 0 || x > decor.getWidth() || y > decor.getHeight();
    }

    public final void setHideOverlayWindows(boolean hide)
    {
    }

    public void setSustainedPerformanceMode(boolean enable)
    {
    }

    public void setPreferMinimalPostProcessing(boolean isPreferred)
    {
        mAttributes.preferMinimalPostProcessing = isPreferred;
        dispatchWindowAttributesChanged();
    }

    public void setBackgroundBlurRadius(int blurRadius)
    {
    }

    public void setElevation(float elevation)
    {
    }

    public void setClipToOutline(boolean clipToOutline)
    {
    }

    public void setBackgroundDrawableResource(int resId)
    {
        setBackgroundDrawable(resId == 0 ? null : mContext.getDrawable(resId));
    }

    public WindowInsetsController getInsetsController()
    {
        return getDecorView().getWindowInsetsController();
    }

    public AttachedSurfaceControl getRootSurfaceControl()
    {
        View decor = peekDecorView();
        return decor == null ? null : decor.getRootSurfaceControl();
    }

    public android.window.OnBackInvokedDispatcher getOnBackInvokedDispatcher()
    {
        Callback callback = getCallback();
        if (callback instanceof android.app.Activity) return ((android.app.Activity)callback).getOnBackInvokedDispatcher();
        if (callback instanceof android.app.Dialog) return ((android.app.Dialog)callback).getOnBackInvokedDispatcher();

        return mBackDispatcher;
    }

    public void setSystemGestureExclusionRects(List<Rect> rects)
    {
        getDecorView().setSystemGestureExclusionRects(rects);
    }

    public List<Rect> getSystemGestureExclusionRects()
    {
        return getDecorView().getSystemGestureExclusionRects();
    }

    public void registerScrollCaptureCallback(ScrollCaptureCallback callback)
    {
        View decor = getDecorView();
        if (decor != null) decor.setScrollCaptureCallback(callback);
    }

    public void unregisterScrollCaptureCallback(ScrollCaptureCallback callback)
    {
        View decor = peekDecorView();
        if (decor != null) decor.setScrollCaptureCallback(null);
    }

    public void setStatusBarContrastEnforced(boolean ensureContrast)
    {
        mStatusBarContrastEnforced = ensureContrast;
    }

    public boolean isStatusBarContrastEnforced()
    {
        return mStatusBarContrastEnforced;
    }

    public void setNavigationBarContrastEnforced(boolean enforceContrast)
    {
        mNavigationBarContrastEnforced = enforceContrast;
    }

    public boolean isNavigationBarContrastEnforced()
    {
        return mNavigationBarContrastEnforced;
    }

    public void setNavigationBarDividerColor(int dividerColor)
    {
        mNavigationBarDividerColor = dividerColor;
    }

    public int getNavigationBarDividerColor()
    {
        return mNavigationBarDividerColor;
    }

    public final CharSequence getTitle()
    {
        return mTitle;
    }

    final void storeTitle(CharSequence title)
    {
        mTitle = title == null ? "" : title;
    }

    public abstract void takeSurface(SurfaceHolder.Callback2 callback);

    public abstract void takeInputQueue(InputQueue.Callback callback);

    public abstract boolean isFloating();

    public abstract void setContentView(int layoutResID);

    public abstract void setContentView(View view);

    public abstract void setContentView(View view, ViewGroup.LayoutParams params);

    public abstract void addContentView(View view, ViewGroup.LayoutParams params);

    public abstract View getCurrentFocus();

    public abstract LayoutInflater getLayoutInflater();

    public abstract void setTitle(CharSequence title);

    @Deprecated
    public abstract void setTitleColor(int textColor);

    public abstract void openPanel(int featureId, KeyEvent event);

    public abstract void closePanel(int featureId);

    public abstract void togglePanel(int featureId, KeyEvent event);

    public abstract void invalidatePanelMenu(int featureId);

    public abstract boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags);

    public abstract boolean performPanelIdentifierAction(int featureId, int id, int flags);

    public abstract void closeAllPanels();

    public abstract boolean performContextMenuIdentifierAction(int id, int flags);

    public abstract void onConfigurationChanged(Configuration newConfig);

    public abstract void setBackgroundDrawable(Drawable drawable);

    public abstract void setFeatureDrawableResource(int featureId, int resId);

    public abstract void setFeatureDrawableUri(int featureId, Uri uri);

    public abstract void setFeatureDrawable(int featureId, Drawable drawable);

    public abstract void setFeatureDrawableAlpha(int featureId, int alpha);

    public abstract void setFeatureInt(int featureId, int value);

    public abstract void takeKeyEvents(boolean get);

    public abstract boolean superDispatchKeyEvent(KeyEvent event);

    public abstract boolean superDispatchKeyShortcutEvent(KeyEvent event);

    public abstract boolean superDispatchTouchEvent(MotionEvent event);

    public abstract boolean superDispatchTrackballEvent(MotionEvent event);

    public abstract boolean superDispatchGenericMotionEvent(MotionEvent event);

    public abstract View getDecorView();

    public abstract View peekDecorView();

    public abstract Bundle saveHierarchyState();

    public abstract void restoreHierarchyState(Bundle savedInstanceState);

    protected abstract void onActive();

    public abstract void setChildDrawable(int featureId, Drawable drawable);

    public abstract void setChildInt(int featureId, int value);

    public abstract boolean isShortcutKey(int keyCode, KeyEvent event);

    public abstract void setVolumeControlStream(int streamType);

    public abstract int getVolumeControlStream();

    public abstract int getStatusBarColor();

    public abstract void setStatusBarColor(int color);

    public abstract int getNavigationBarColor();

    public abstract void setNavigationBarColor(int color);

    public abstract void setDecorCaptionShade(int decorCaptionShade);

    public abstract void setResizingCaptionDrawable(Drawable drawable);

    public interface OnFrameMetricsAvailableListener
    {
        void onFrameMetricsAvailable(Window window, FrameMetrics frameMetrics, int dropCountSinceLastInvocation);
    }

    public interface OnRestrictedCaptionAreaChangedListener
    {
        void onRestrictedCaptionAreaChanged(Rect rect);
    }

    public interface Callback
    {
        boolean dispatchKeyEvent(KeyEvent event);

        boolean dispatchKeyShortcutEvent(KeyEvent event);

        boolean dispatchTouchEvent(MotionEvent event);

        boolean dispatchTrackballEvent(MotionEvent event);

        boolean dispatchGenericMotionEvent(MotionEvent event);

        boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent event);

        View onCreatePanelView(int featureId);

        boolean onCreatePanelMenu(int featureId, Menu menu);

        boolean onPreparePanel(int featureId, View view, Menu menu);

        boolean onMenuOpened(int featureId, Menu menu);

        boolean onMenuItemSelected(int featureId, MenuItem item);

        void onWindowAttributesChanged(WindowManager.LayoutParams attrs);

        void onContentChanged();

        void onWindowFocusChanged(boolean hasFocus);

        void onAttachedToWindow();

        void onDetachedFromWindow();

        void onPanelClosed(int featureId, Menu menu);

        boolean onSearchRequested();

        boolean onSearchRequested(SearchEvent searchEvent);

        ActionMode onWindowStartingActionMode(ActionMode.Callback callback);

        ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type);

        void onActionModeStarted(ActionMode mode);

        void onActionModeFinished(ActionMode mode);

        default void onProvideKeyboardShortcuts(java.util.List<KeyboardShortcutGroup> data, Menu menu, int deviceId)
        {
        }

        default void onPointerCaptureChanged(boolean hasCapture)
        {
        }
    }
}
