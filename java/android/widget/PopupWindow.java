package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.transition.Transition;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.WindowInsets;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

public class PopupWindow
{
    public static final int INPUT_METHOD_FROM_FOCUSABLE = 0;
    public static final int INPUT_METHOD_NEEDED = 1;
    public static final int INPUT_METHOD_NOT_NEEDED = 2;

    private static final int DEFAULT_ANCHORED_GRAVITY = Gravity.TOP | Gravity.START;
    private static final int ANIMATION_STYLE_DEFAULT = -1;
    private static final int DEFAULT_POPUP_COLOR = 0xFFFAFAFA;

    private static StyleableSet sStyleable;

    private final Context mContext;
    private WindowManager mWindowManager;
    private View mContentView;
    private PopupDecorView mDecorView;
    private Drawable mBackground;
    private float mElevation;
    private int mAnimationStyle = ANIMATION_STYLE_DEFAULT;

    private int mWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int mHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int mWidthMode;
    private int mHeightMode;

    private boolean mFocusable;
    private boolean mTouchable = true;
    private boolean mOutsideTouchable;
    private boolean mClippingEnabled = true;
    private boolean mClippedToScreen;
    private boolean mSplitTouchEnabled = true;
    private boolean mLayoutInScreen;
    private boolean mAttachedInDecor = true;
    private boolean mTouchModal = true;
    private boolean mOverlapAnchor;
    private boolean mIgnoreCheekPress;
    private int mWindowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
    private int mInputMethodMode = INPUT_METHOD_FROM_FOCUSABLE;
    private int mSoftInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED;

    private boolean mIsShowing;
    private boolean mIsDropdown;
    private boolean mAboveAnchor;
    private WeakReference<View> mAnchor;
    private int mAnchorXoff;
    private int mAnchorYoff;
    private int mAnchoredGravity = DEFAULT_ANCHORED_GRAVITY;
    private Rect mEpicenterBounds;
    private Transition mEnterTransition;
    private Transition mExitTransition;

    private OnDismissListener mOnDismissListener;
    private View.OnTouchListener mTouchInterceptor;

    private final View.OnAttachStateChangeListener mAnchorDetachListener = new View.OnAttachStateChangeListener()
    {
        @Override
        public void onViewAttachedToWindow(View view)
        {
        }

        @Override
        public void onViewDetachedFromWindow(View view)
        {
            if (mIsShowing) dismiss();
        }
    };

    public PopupWindow(Context context)
    {
        this(context, null);
    }

    public PopupWindow(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public PopupWindow(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public PopupWindow(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        mContext = context;
        mWindowManager = context == null ? null : (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);

        if (context != null && attributes != null) readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "popupBackground", "popupElevation", "overlapAnchor");
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int backgroundIndex = sStyleable.indexOf("popupBackground");
        if (backgroundIndex >= 0 && array.hasValue(backgroundIndex))
        {
            try
            {
                mBackground = array.getDrawable(backgroundIndex);
            }
            catch (RuntimeException unsupported)
            {
                mBackground = null;
            }
        }

        int elevationIndex = sStyleable.indexOf("popupElevation");
        if (elevationIndex >= 0) mElevation = array.getDimension(elevationIndex, 0);

        int overlapIndex = sStyleable.indexOf("overlapAnchor");
        if (overlapIndex >= 0) mOverlapAnchor = array.getBoolean(overlapIndex, false);

        array.recycle();
    }

    public PopupWindow()
    {
        this(null, 0, 0);
    }

    public PopupWindow(View contentView)
    {
        this(contentView, 0, 0);
    }

    public PopupWindow(int width, int height)
    {
        this(null, width, height);
    }

    public PopupWindow(View contentView, int width, int height)
    {
        this(contentView, width, height, false);
    }

    public PopupWindow(View contentView, int width, int height, boolean focusable)
    {
        mContext = contentView == null ? null : contentView.getContext();
        mWindowManager = mContext == null ? null : (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);

        setContentView(contentView);
        setWidth(width);
        setHeight(height);
        setFocusable(focusable);
    }

    public void setEnterTransition(Transition enterTransition)
    {
        mEnterTransition = enterTransition;
    }

    public Transition getEnterTransition()
    {
        return mEnterTransition;
    }

    public void setExitTransition(Transition exitTransition)
    {
        mExitTransition = exitTransition;
    }

    public Transition getExitTransition()
    {
        return mExitTransition;
    }

    public Rect getEpicenterBounds()
    {
        return mEpicenterBounds == null ? null : new Rect(mEpicenterBounds);
    }

    public void setEpicenterBounds(Rect bounds)
    {
        mEpicenterBounds = bounds == null ? null : new Rect(bounds);
    }

    public Drawable getBackground()
    {
        return mBackground;
    }

    public void setBackgroundDrawable(Drawable background)
    {
        mBackground = background;
        if (mDecorView != null) mDecorView.setBackground(background);
    }

    public float getElevation()
    {
        return mElevation;
    }

    public void setElevation(float elevation)
    {
        mElevation = elevation;
        if (mDecorView != null) mDecorView.setElevation(elevation);
    }

    public int getAnimationStyle()
    {
        return mAnimationStyle;
    }

    public void setIgnoreCheekPress()
    {
        mIgnoreCheekPress = true;
    }

    public void setAnimationStyle(int animationStyle)
    {
        mAnimationStyle = animationStyle;
    }

    public View getContentView()
    {
        return mContentView;
    }

    public void setContentView(View contentView)
    {
        if (isShowing()) return;

        mContentView = contentView;

        if (mContext == null && contentView != null)
        {
            mWindowManager = (WindowManager)contentView.getContext().getSystemService(Context.WINDOW_SERVICE);
        }
    }

    public void setTouchInterceptor(View.OnTouchListener listener)
    {
        mTouchInterceptor = listener;
    }

    public boolean isFocusable()
    {
        return mFocusable;
    }

    public void setFocusable(boolean focusable)
    {
        mFocusable = focusable;
    }

    public int getInputMethodMode()
    {
        return mInputMethodMode;
    }

    public void setInputMethodMode(int mode)
    {
        mInputMethodMode = mode;
    }

    public void setSoftInputMode(int mode)
    {
        mSoftInputMode = mode;
    }

    public int getSoftInputMode()
    {
        return mSoftInputMode;
    }

    public boolean isTouchable()
    {
        return mTouchable;
    }

    public void setTouchable(boolean touchable)
    {
        mTouchable = touchable;
    }

    public boolean isOutsideTouchable()
    {
        return mOutsideTouchable;
    }

    public void setOutsideTouchable(boolean touchable)
    {
        mOutsideTouchable = touchable;
    }

    public boolean isClippingEnabled()
    {
        return mClippingEnabled;
    }

    public void setClippingEnabled(boolean enabled)
    {
        mClippingEnabled = enabled;
    }

    public boolean isClippedToScreen()
    {
        return mClippedToScreen;
    }

    public void setIsClippedToScreen(boolean enabled)
    {
        mClippedToScreen = enabled;
    }

    public boolean isSplitTouchEnabled()
    {
        return mSplitTouchEnabled;
    }

    public void setSplitTouchEnabled(boolean enabled)
    {
        mSplitTouchEnabled = enabled;
    }

    public boolean isLaidOutInScreen()
    {
        return mLayoutInScreen;
    }

    public void setIsLaidOutInScreen(boolean enabled)
    {
        mLayoutInScreen = enabled;
    }

    public boolean isAttachedInDecor()
    {
        return mAttachedInDecor;
    }

    public void setAttachedInDecor(boolean enabled)
    {
        mAttachedInDecor = enabled;
    }

    public void setWindowLayoutType(int layoutType)
    {
        mWindowLayoutType = layoutType;
    }

    public int getWindowLayoutType()
    {
        return mWindowLayoutType;
    }

    public boolean isTouchModal()
    {
        return mTouchModal;
    }

    public void setTouchModal(boolean touchModal)
    {
        mTouchModal = touchModal;
    }

    public void setWindowLayoutMode(int widthSpec, int heightSpec)
    {
        mWidthMode = widthSpec;
        mHeightMode = heightSpec;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public void setHeight(int height)
    {
        mHeight = height;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public void setWidth(int width)
    {
        mWidth = width;
    }

    public void setOverlapAnchor(boolean overlapAnchor)
    {
        mOverlapAnchor = overlapAnchor;
    }

    public boolean getOverlapAnchor()
    {
        return mOverlapAnchor;
    }

    public boolean isShowing()
    {
        return mIsShowing;
    }

    public void showAtLocation(View parent, int gravity, int x, int y)
    {
        if (isShowing() || mContentView == null) return;

        detachFromAnchor();

        mIsShowing = true;
        mIsDropdown = false;

        WindowManager.LayoutParams params = createPopupLayoutParams();
        params.gravity = gravity == Gravity.NO_GRAVITY ? Gravity.TOP | Gravity.LEFT : gravity;
        params.x = x;
        params.y = y;

        preparePopup(params);
        invokePopup(params);
    }

    public void showAsDropDown(View anchor)
    {
        showAsDropDown(anchor, 0, 0);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff)
    {
        showAsDropDown(anchor, xoff, yoff, DEFAULT_ANCHORED_GRAVITY);
    }

    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity)
    {
        if (isShowing() || mContentView == null || anchor == null) return;

        attachToAnchor(anchor, xoff, yoff, gravity);

        mIsShowing = true;
        mIsDropdown = true;

        WindowManager.LayoutParams params = createPopupLayoutParams();
        preparePopup(params);

        mAboveAnchor = findDropDownPosition(anchor, params, xoff, yoff, gravity);
        invokePopup(params);
    }

    public boolean isAboveAnchor()
    {
        return mAboveAnchor;
    }

    public int getMaxAvailableHeight(View anchor)
    {
        return getMaxAvailableHeight(anchor, 0);
    }

    public int getMaxAvailableHeight(View anchor, int yOffset)
    {
        return getMaxAvailableHeight(anchor, yOffset, false);
    }

    public int getMaxAvailableHeight(View anchor, int yOffset, boolean ignoreBottomDecorations)
    {
        Rect display = displayFrame(anchor, ignoreBottomDecorations);
        int[] location = new int[2];
        locateInWindow(anchor, location);

        int distanceToBottom = display.bottom - (location[1] + anchor.getHeight()) - yOffset;
        int distanceToTop = location[1] - display.top + yOffset;
        int available = Math.max(distanceToBottom, distanceToTop);

        if (mBackground != null)
        {
            Rect padding = new Rect();
            mBackground.getPadding(padding);
            available -= padding.top + padding.bottom;
        }

        return available;
    }

    public void dismiss()
    {
        if (!isShowing()) return;

        mIsShowing = false;

        PopupDecorView decor = mDecorView;
        mDecorView = null;

        if (decor != null)
        {
            try
            {
                if (mWindowManager != null) mWindowManager.removeViewImmediate(decor);
            }
            finally
            {
                decor.removeAllViews();
            }
        }

        detachFromAnchor();

        if (mOnDismissListener != null) mOnDismissListener.onDismiss();
    }

    public void setOnDismissListener(OnDismissListener listener)
    {
        mOnDismissListener = listener;
    }

    public void update()
    {
        if (!isShowing() || mDecorView == null) return;

        WindowManager.LayoutParams params = (WindowManager.LayoutParams)mDecorView.getLayoutParams();
        params.flags = computeFlags(params.flags);
        if (mAnimationStyle != ANIMATION_STYLE_DEFAULT) params.windowAnimations = mAnimationStyle;

        mDecorView.setBackground(mBackground);
        mDecorView.setElevation(mElevation);
        mWindowManager.updateViewLayout(mDecorView, params);
    }

    public void update(int width, int height)
    {
        if (!isShowing() || mDecorView == null) return;

        WindowManager.LayoutParams params = (WindowManager.LayoutParams)mDecorView.getLayoutParams();
        update(params.x, params.y, width, height, false);
    }

    public void update(int x, int y, int width, int height)
    {
        update(x, y, width, height, false);
    }

    public void update(int x, int y, int width, int height, boolean force)
    {
        if (width >= 0) setWidth(width);
        if (height >= 0) setHeight(height);

        if (!isShowing() || mDecorView == null) return;

        WindowManager.LayoutParams params = (WindowManager.LayoutParams)mDecorView.getLayoutParams();
        params.width = resolvedWidth();
        params.height = resolvedHeight();

        if (x != -1) params.x = x;
        if (y != -1) params.y = y;

        params.flags = computeFlags(params.flags);
        mWindowManager.updateViewLayout(mDecorView, params);
    }

    public void update(View anchor, int width, int height)
    {
        update(anchor, false, 0, 0, width, height);
    }

    public void update(View anchor, int xoff, int yoff, int width, int height)
    {
        update(anchor, true, xoff, yoff, width, height);
    }

    private void update(View anchor, boolean updateLocation, int xoff, int yoff, int width, int height)
    {
        if (!isShowing() || mDecorView == null || anchor == null) return;

        WeakReference<View> oldAnchor = mAnchor;
        if (oldAnchor == null || oldAnchor.get() != anchor || (updateLocation && (mAnchorXoff != xoff || mAnchorYoff != yoff)))
        {
            attachToAnchor(anchor, updateLocation ? xoff : mAnchorXoff, updateLocation ? yoff : mAnchorYoff, mAnchoredGravity);
        }

        if (width >= 0) setWidth(width);
        if (height >= 0) setHeight(height);

        WindowManager.LayoutParams params = (WindowManager.LayoutParams)mDecorView.getLayoutParams();
        params.width = resolvedWidth();
        params.height = resolvedHeight();
        params.flags = computeFlags(params.flags);

        measureDecor(params);
        mAboveAnchor = findDropDownPosition(anchor, params, mAnchorXoff, mAnchorYoff, mAnchoredGravity);
        mWindowManager.updateViewLayout(mDecorView, params);
    }

    private void attachToAnchor(View anchor, int xoff, int yoff, int gravity)
    {
        detachFromAnchor();

        anchor.addOnAttachStateChangeListener(mAnchorDetachListener);

        mAnchor = new WeakReference<View>(anchor);
        mAnchorXoff = xoff;
        mAnchorYoff = yoff;
        mAnchoredGravity = gravity;
    }

    private void detachFromAnchor()
    {
        View anchor = mAnchor == null ? null : mAnchor.get();
        if (anchor != null) anchor.removeOnAttachStateChangeListener(mAnchorDetachListener);

        mAnchor = null;
    }

    private int resolvedWidth()
    {
        return mWidthMode < 0 ? mWidthMode : mWidth;
    }

    private int resolvedHeight()
    {
        return mHeightMode < 0 ? mHeightMode : mHeight;
    }

    private WindowManager.LayoutParams createPopupLayoutParams()
    {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.flags = computeFlags(params.flags);
        params.type = mWindowLayoutType;
        params.softInputMode = mSoftInputMode;
        params.format = PixelFormat.TRANSLUCENT;
        params.width = resolvedWidth();
        params.height = resolvedHeight();
        if (mAnimationStyle != ANIMATION_STYLE_DEFAULT) params.windowAnimations = mAnimationStyle;
        params.setTitle("PopupWindow:" + Integer.toHexString(hashCode()));
        return params;
    }

    private int computeFlags(int flags)
    {
        flags &= ~(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                   | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                   | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                   | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                   | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        if (mIgnoreCheekPress) flags |= WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;

        if (!mFocusable)
        {
            flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            if (mInputMethodMode == INPUT_METHOD_NEEDED) flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        }
        else if (mInputMethodMode == INPUT_METHOD_NOT_NEEDED)
        {
            flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        }

        if (!mTouchModal) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        if (!mTouchable) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        if (mOutsideTouchable) flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        if (!mClippingEnabled || mClippedToScreen) flags |= WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        if (mSplitTouchEnabled) flags |= WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;

        flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        return flags;
    }

    private void preparePopup(WindowManager.LayoutParams params)
    {
        if (mDecorView != null) mDecorView.removeAllViews();

        ViewParent oldParent = mContentView.getParent();
        if (oldParent instanceof ViewGroup) ((ViewGroup)oldParent).removeView(mContentView);

        Context context = mContext != null ? mContext : mContentView.getContext();
        mDecorView = new PopupDecorView(context);
        mDecorView.setBackground(mBackground);
        mDecorView.setElevation(mElevation);
        mDecorView.setClipChildren(true);

        ViewGroup.LayoutParams contentParams = mContentView.getLayoutParams();
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        if (contentParams != null)
        {
            if (contentParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) height = ViewGroup.LayoutParams.WRAP_CONTENT;
            if (contentParams.width == ViewGroup.LayoutParams.WRAP_CONTENT) width = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        mDecorView.addView(mContentView, new FrameLayout.LayoutParams(width, height));
        mDecorView.setLayoutParams(params);

        if (mFocusable)
        {
            mDecorView.setFocusable(true);
            mDecorView.setFocusableInTouchMode(true);
        }

        measureDecor(params);
    }

    private void invokePopup(WindowManager.LayoutParams params)
    {
        if (mWindowManager == null)
        {
            android.util.Log.w("PopupWindow", "a popup window has no window manager to show it, so it stays hidden");
            return;
        }

        mWindowManager.addView(mDecorView, params);
        if (mFocusable) mDecorView.requestFocus();
    }

    private void measureDecor(WindowManager.LayoutParams params)
    {
        Rect display = displayFrame(anchorView(), false);
        int widthSpec = decorSpec(params.width, display.width());
        int heightSpec = decorSpec(params.height, display.height());
        mDecorView.measure(widthSpec, heightSpec);
    }

    private static int decorSpec(int size, int available)
    {
        if (size == ViewGroup.LayoutParams.MATCH_PARENT) return View.MeasureSpec.makeMeasureSpec(available, View.MeasureSpec.EXACTLY);
        if (size == ViewGroup.LayoutParams.WRAP_CONTENT) return View.MeasureSpec.makeMeasureSpec(available, View.MeasureSpec.AT_MOST);
        return View.MeasureSpec.makeMeasureSpec(Math.max(0, size), View.MeasureSpec.EXACTLY);
    }

    private View anchorView()
    {
        return mAnchor == null ? null : mAnchor.get();
    }

    private boolean findDropDownPosition(View anchor, WindowManager.LayoutParams params, int xOffset, int yOffset, int gravity)
    {
        int anchorHeight = anchor.getHeight();
        int anchorWidth = anchor.getWidth();
        if (mOverlapAnchor) yOffset -= anchorHeight;

        int[] location = new int[2];
        locateInWindow(anchor, location);

        int width = mDecorView.getMeasuredWidth();
        int height = mDecorView.getMeasuredHeight();

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = location[0] + xOffset;
        params.y = location[1] + anchorHeight + yOffset;

        int horizontal = Gravity.getAbsoluteGravity(gravity, anchor.getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (horizontal == Gravity.RIGHT) params.x -= width - anchorWidth;

        Rect display = displayFrame(anchor, false);
        boolean above = false;

        if (params.y + height > display.bottom)
        {
            int aboveTop = location[1] - height + (mOverlapAnchor ? anchorHeight : 0) - (mOverlapAnchor ? 0 : yOffset);
            int spaceAbove = location[1] - display.top;
            int spaceBelow = display.bottom - (location[1] + anchorHeight);

            if (aboveTop >= display.top || spaceAbove > spaceBelow)
            {
                params.y = Math.max(display.top, aboveTop);
                above = true;
            }
        }

        if (mClippingEnabled)
        {
            if (params.x + width > display.right) params.x = display.right - width;
            if (params.x < display.left) params.x = display.left;

            if (params.y + height > display.bottom) params.y = Math.max(display.top, display.bottom - height);
            if (params.y < display.top) params.y = display.top;
        }

        return above;
    }

    Rect displayFrame(View anchor, boolean ignoreBottomDecorations)
    {
        ViewRootImpl root = ViewRootImpl.active();
        int width;
        int height;
        int insetTop = 0;
        int insetBottom = 0;

        if (root != null)
        {
            width = root.getWindowWidth();
            height = root.getWindowHeight();

            WindowInsets insets = root.getWindowInsets();
            if (insets != null)
            {
                insetTop = insets.getSystemWindowInsetTop();
                insetBottom = insets.getSystemWindowInsetBottom();
            }
        }
        else if (anchor != null)
        {
            View top = anchor.getRootView();
            width = top.getWidth();
            height = top.getHeight();
        }
        else
        {
            android.util.DisplayMetrics metrics = mContext == null ? null : mContext.getResources().getDisplayMetrics();
            width = metrics == null ? 1280 : metrics.widthPixels;
            height = metrics == null ? 720 : metrics.heightPixels;
        }

        if (ignoreBottomDecorations) insetBottom = 0;
        return new Rect(0, insetTop, width, Math.max(insetTop, height - insetBottom));
    }

    static void locateInWindow(View view, int[] out)
    {
        float x = 0;
        float y = 0;
        View cursor = view;

        for (;;)
        {
            x += cursor.getLeft() + cursor.getTranslationX();
            y += cursor.getTop() + cursor.getTranslationY();

            ViewParent parent = cursor.getParent();
            if (!(parent instanceof View)) break;

            View parentView = (View)parent;
            x -= parentView.getScrollX();
            y -= parentView.getScrollY();
            cursor = parentView;
        }

        ViewRootImpl root = ViewRootImpl.active();
        ViewGroup.LayoutParams rootParams = cursor.getLayoutParams();

        if (root != null && rootParams instanceof WindowManager.LayoutParams && root.hasPanel(cursor))
        {
            Rect frame = panelFrame(root, cursor, (WindowManager.LayoutParams)rootParams);
            x += frame.left;
            y += frame.top;
        }

        out[0] = Math.round(x);
        out[1] = Math.round(y);
    }

    private static Rect panelFrame(ViewRootImpl root, View panel, WindowManager.LayoutParams params)
    {
        int width = root.getWindowWidth();
        int height = root.getWindowHeight();
        int insetTop = 0;
        int insetBottom = 0;

        WindowInsets insets = root.getWindowInsets();
        if (insets != null)
        {
            insetTop = insets.getSystemWindowInsetTop();
            insetBottom = insets.getSystemWindowInsetBottom();
        }

        int screenFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        boolean fullScreen = (params.flags & screenFlags) != 0;

        int top = fullScreen ? 0 : insetTop;
        int bottom = fullScreen ? height : height - insetBottom;
        Rect container = new Rect(0, top, width, Math.max(top, bottom));

        int gravity = params.gravity == Gravity.NO_GRAVITY ? Gravity.TOP | Gravity.LEFT : params.gravity;
        gravity = Gravity.getAbsoluteGravity(gravity, View.LAYOUT_DIRECTION_LTR);

        Rect frame = new Rect();
        Gravity.apply(gravity, panel.getWidth(), panel.getHeight(), container, params.x, params.y, frame);
        return frame;
    }

    static Drawable defaultPopupBackground(Context context)
    {
        int color = DEFAULT_POPUP_COLOR;

        try
        {
            int attribute = context.getResources().getIdentifier("colorBackgroundFloating", "attr", "android");
            TypedValue value = new TypedValue();

            if (attribute != 0 && context.getTheme().resolveAttribute(attribute, value, true))
            {
                boolean isColor = value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT;
                boolean isReference = value.type == TypedValue.TYPE_REFERENCE && value.data != 0;

                if (isColor) color = value.data;
                else if (isReference) color = context.getResources().getColor(value.data, context.getTheme());
            }
        }
        catch (RuntimeException unresolved)
        {
            color = DEFAULT_POPUP_COLOR;
        }

        if ((color >>> 24) == 0) color = DEFAULT_POPUP_COLOR;
        return new ColorDrawable(color);
    }

    private boolean handleKey(KeyEvent event)
    {
        int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_ESCAPE) return false;

        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) dismiss();
        return true;
    }

    public interface OnDismissListener
    {
        void onDismiss();
    }

    private class PopupDecorView extends FrameLayout
    {
        PopupDecorView(Context context)
        {
            super(context);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event)
        {
            if (super.dispatchKeyEvent(event)) return true;
            return handleKey(event);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event)
        {
            if (mTouchInterceptor != null && mTouchInterceptor.onTouch(this, event)) return true;
            return super.dispatchTouchEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            int x = (int)event.getX();
            int y = (int)event.getY();
            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN && (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()))
            {
                dismiss();
                return true;
            }

            if (action == MotionEvent.ACTION_OUTSIDE)
            {
                dismiss();
                return true;
            }

            return super.onTouchEvent(event);
        }
    }
}
