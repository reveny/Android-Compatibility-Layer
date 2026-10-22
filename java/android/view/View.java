package android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.translation.ViewTranslationRequest;
import java.util.ArrayList;

public class View implements android.view.accessibility.AccessibilityEventSource, android.graphics.drawable.Drawable.Callback,
    KeyEvent.Callback
{
    public static final int VISIBLE = 0;
    public static final int INVISIBLE = 4;
    public static final int GONE = 8;

    public static final int NO_ID = -1;

    public static final int LAYOUT_DIRECTION_LTR = 0;
    public static final int LAYOUT_DIRECTION_RTL = 1;
    public static final int LAYOUT_DIRECTION_INHERIT = 2;
    public static final int LAYOUT_DIRECTION_LOCALE = 3;

    public static final boolean TRACE_INPUT = runtime.Settings.tracing("input");

    private static final int[] VISIBILITY_FLAGS = {VISIBLE, INVISIBLE, GONE};

    protected final Context mContext;

    private int mId = NO_ID;
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;
    private int mMeasuredWidth;
    private int mMeasuredHeight;
    private int mVisibility = VISIBLE;
    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;
    private int mMinimumWidth;
    private int mMinimumHeight;
    private int mBackgroundColor;
    private boolean mHasBackground;
    private android.graphics.drawable.Drawable mBackground;
    private int[] mDrawableState;
    private boolean mDrawableStateStale = true;
    private boolean mEnabled = true;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mDownX;
    private float mDownY;
    private boolean mLongClickable;
    private boolean mLongClickFired;
    private OnLongClickListener mLongClickListener;
    private Runnable mLongPressCallback;
    private ViewPropertyAnimator mPropertyAnimator;
    private boolean mLayoutRequested = true;
    private Object mTag;
    private ViewParent mParent;
    private int mLayoutDirection = LAYOUT_DIRECTION_INHERIT;
    private AccessibilityDelegate mAccessibilityDelegate;
    private ViewGroup.LayoutParams mLayoutParams;
    private boolean mWillNotDraw;
    private boolean mFitsSystemWindows;
    private OnApplyWindowInsetsListener mInsetsListener;
    private OnClickListener mClickListener;
    private ViewTreeObserver mViewTreeObserver;
    private android.os.IBinder mWindowToken;
    private java.util.HashMap<Integer, Object> mKeyedTags;
    private int mSystemUiVisibility;
    private android.os.Handler mHandler;
    private final WindowInsetsController mInsetsController = new InsetsController();

    private final Paint mBackgroundPaint = new Paint();
    private boolean mAutoHandwritingEnabled = true;
    private boolean mHandwritingDelegate;
    private int mHandwritingDelegateFlags;
    private Runnable mHandwritingDelegatorCallback;
    private String mAllowedHandwritingDelegatePackage;
    private String mAllowedHandwritingDelegatorPackage;
    private float mHandwritingOffsetLeft;
    private float mHandwritingOffsetTop;
    private float mHandwritingOffsetRight;
    private float mHandwritingOffsetBottom;

    private static StyleableSet sStyleable;

    public View(Context context)
    {
        mContext = context;
    }

    public View(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0, 0);
    }

    public View(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public View(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        mContext = context;
        if (attributes != null || defStyleAttr != 0 || defStyleRes != 0) applyViewAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void applyViewAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        StyleableSet styleable = styleable(context);
        if (styleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);

        mId = readResourceId(array, styleable, "id", NO_ID);

        int visibilityIndex = styleable.indexOf("visibility");
        if (visibilityIndex >= 0 && array.hasValue(visibilityIndex))
        {
            int ordinal = array.getInt(visibilityIndex, 0);
            mVisibility = ordinal >= 0 && ordinal < VISIBILITY_FLAGS.length ? VISIBILITY_FLAGS[ordinal] : VISIBLE;
        }

        int padding = readDimension(array, styleable, "padding", -1);
        if (padding >= 0)
        {
            mPaddingLeft = padding;
            mPaddingTop = padding;
            mPaddingRight = padding;
            mPaddingBottom = padding;
        }

        int paddingHorizontal = readDimension(array, styleable, "paddingHorizontal", -1);
        if (paddingHorizontal >= 0)
        {
            mPaddingLeft = paddingHorizontal;
            mPaddingRight = paddingHorizontal;
        }

        int paddingVertical = readDimension(array, styleable, "paddingVertical", -1);
        if (paddingVertical >= 0)
        {
            mPaddingTop = paddingVertical;
            mPaddingBottom = paddingVertical;
        }

        mPaddingLeft = readDimension(array, styleable, "paddingLeft", mPaddingLeft);
        mPaddingTop = readDimension(array, styleable, "paddingTop", mPaddingTop);
        mPaddingRight = readDimension(array, styleable, "paddingRight", mPaddingRight);
        mPaddingBottom = readDimension(array, styleable, "paddingBottom", mPaddingBottom);

        int paddingStart = readDimension(array, styleable, "paddingStart", -1);
        int paddingEnd = readDimension(array, styleable, "paddingEnd", -1);
        if (paddingStart >= 0) mPaddingLeft = paddingStart;
        if (paddingEnd >= 0) mPaddingRight = paddingEnd;
        mPaddingRelative = paddingStart >= 0 || paddingEnd >= 0;

        mMinimumWidth = readDimension(array, styleable, "minWidth", 0);
        mMinimumHeight = readDimension(array, styleable, "minHeight", 0);

        applyBehaviourAttributes(array, styleable);
        applyTransformAttributes(array, styleable);
        applyAccessibilityAttributes(array, styleable);

        int background = styleable.indexOf("background");
        if (background >= 0 && array.hasValue(background))
        {
            int type = array.getType(background);
            if (type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT && type <= android.util.TypedValue.TYPE_LAST_COLOR_INT)
            {
                setBackgroundColor(array.getColor(background, 0));
            }
            else
            {
                android.graphics.drawable.Drawable drawable = array.getDrawable(background);
                if (drawable != null) setBackground(drawable);
            }
        }

        if (mBackground != null)
        {
            if (mBackgroundTintList != null) mBackground.setTintList(mBackgroundTintList);
            if (mBackgroundTintMode != null) mBackground.setTintMode(mBackgroundTintMode);
        }

        array.recycle();
    }

    private void applyBehaviourAttributes(TypedArray array, StyleableSet styleable)
    {
        int focusableIndex = styleable.indexOf("focusable");
        if (focusableIndex >= 0 && array.hasValue(focusableIndex))
        {
            if (array.getType(focusableIndex) == android.util.TypedValue.TYPE_INT_BOOLEAN)
            {
                mFocusable = array.getBoolean(focusableIndex, false);
                mFocusableAuto = false;
            }
            else
            {
                int focusable = array.getInt(focusableIndex, NOT_FOCUSABLE);
                mFocusable = focusable == FOCUSABLE;
                mFocusableAuto = focusable == FOCUSABLE_AUTO;
            }
        }

        mClickable = readBoolean(array, styleable, "clickable", mClickable);
        mLongClickable = readBoolean(array, styleable, "longClickable", mLongClickable);
        mContextClickable = readBoolean(array, styleable, "contextClickable", mContextClickable);
        mFocusableInTouchMode = readBoolean(array, styleable, "focusableInTouchMode", mFocusableInTouchMode);
        if (mFocusableInTouchMode) mFocusable = true;
        if (mFocusableAuto && (mClickable || mLongClickable)) mFocusable = true;

        mEnabled = readBoolean(array, styleable, "enabled", mEnabled);
        mSaveEnabled = readBoolean(array, styleable, "saveEnabled", mSaveEnabled);
        mDuplicateParentState = readBoolean(array, styleable, "duplicateParentState", mDuplicateParentState);
        mHapticFeedbackEnabled = readBoolean(array, styleable, "hapticFeedbackEnabled", mHapticFeedbackEnabled);
        mSoundEffectsEnabled = readBoolean(array, styleable, "soundEffectsEnabled", mSoundEffectsEnabled);
        mKeepScreenOn = readBoolean(array, styleable, "keepScreenOn", mKeepScreenOn);
        mNestedScrollingEnabled = readBoolean(array, styleable, "nestedScrollingEnabled", mNestedScrollingEnabled);
        mFitsSystemWindows = readBoolean(array, styleable, "fitsSystemWindows", mFitsSystemWindows);
        mFocusedByDefault = readBoolean(array, styleable, "focusedByDefault", mFocusedByDefault);
        mKeyboardNavigationCluster = readBoolean(array, styleable, "keyboardNavigationCluster", mKeyboardNavigationCluster);
        mClipToOutline = readBoolean(array, styleable, "clipToOutline", mClipToOutline);
        mScrollContainer = readBoolean(array, styleable, "isScrollContainer", mScrollContainer);
        mFilterTouchesWhenObscured = readBoolean(array, styleable, "filterTouchesWhenObscured", mFilterTouchesWhenObscured);
        mForceDarkAllowed = readBoolean(array, styleable, "forceDarkAllowed", mForceDarkAllowed);
        mDefaultFocusHighlightEnabled = readBoolean(array, styleable, "defaultFocusHighlightEnabled", mDefaultFocusHighlightEnabled);

        mNextFocusLeftId = readResourceId(array, styleable, "nextFocusLeft", mNextFocusLeftId);
        mNextFocusRightId = readResourceId(array, styleable, "nextFocusRight", mNextFocusRightId);
        mNextFocusUpId = readResourceId(array, styleable, "nextFocusUp", mNextFocusUpId);
        mNextFocusDownId = readResourceId(array, styleable, "nextFocusDown", mNextFocusDownId);
        mNextFocusForwardId = readResourceId(array, styleable, "nextFocusForward", mNextFocusForwardId);
        mNextClusterForwardId = readResourceId(array, styleable, "nextClusterForward", mNextClusterForwardId);

        mScrollX = readDimension(array, styleable, "scrollX", mScrollX);
        mScrollY = readDimension(array, styleable, "scrollY", mScrollY);
        mOverScrollMode = readInt(array, styleable, "overScrollMode", mOverScrollMode);
        mLayerType = readInt(array, styleable, "layerType", mLayerType);

        int scrollbars = readInt(array, styleable, "scrollbars", -1);
        if (scrollbars >= 0)
        {
            mHorizontalScrollBarEnabled = (scrollbars & 0x100) != 0;
            mVerticalScrollBarEnabled = (scrollbars & 0x200) != 0;
        }

        mScrollIndicators = readInt(array, styleable, "scrollIndicators", mScrollIndicators) & SCROLL_INDICATORS_ALL;

        int fadingEdges = readInt(array, styleable, "requiresFadingEdge", -1);
        if (fadingEdges >= 0)
        {
            mHorizontalFadingEdge = (fadingEdges & 0x1000) != 0;
            mVerticalFadingEdge = (fadingEdges & 0x2000) != 0;
        }
        mFadingEdgeLength = readDimension(array, styleable, "fadingEdgeLength", mFadingEdgeLength);

        mLayoutDirection = readInt(array, styleable, "layoutDirection", mLayoutDirection);
        mRawTextDirection = readInt(array, styleable, "textDirection", mRawTextDirection);
        mRawTextAlignment = readInt(array, styleable, "textAlignment", mRawTextAlignment);

        CharSequence tag = readText(array, styleable, "tag");
        if (tag != null) mTag = tag;

        CharSequence transitionName = readText(array, styleable, "transitionName");
        if (transitionName != null) mTransitionName = transitionName.toString();

        int foregroundIndex = styleable.indexOf("foreground");
        if (foregroundIndex >= 0 && array.hasValue(foregroundIndex))
        {
            android.graphics.drawable.Drawable foreground = array.getDrawable(foregroundIndex);
            if (foreground != null)
            {
                mForeground = foreground;
                foreground.setCallback(this);
            }
        }

        mForegroundGravity = readInt(array, styleable, "foregroundGravity", mForegroundGravity);

        int foregroundTint = styleable.indexOf("foregroundTint");
        if (foregroundTint >= 0 && array.hasValue(foregroundTint))
        {
            mForegroundTintList = array.getColorStateList(foregroundTint);
            if (mForeground != null && mForegroundTintList != null) mForeground.setTintList(mForegroundTintList);
        }

        int backgroundTint = styleable.indexOf("backgroundTint");
        if (backgroundTint >= 0 && array.hasValue(backgroundTint)) mBackgroundTintList = array.getColorStateList(backgroundTint);

        int backgroundTintMode = readInt(array, styleable, "backgroundTintMode", -1);
        if (backgroundTintMode >= 0) mBackgroundTintMode = parseTintMode(backgroundTintMode);

        int spotShadow = styleable.indexOf("outlineSpotShadowColor");
        if (spotShadow >= 0 && array.hasValue(spotShadow)) mOutlineSpotShadowColor = array.getColor(spotShadow, mOutlineSpotShadowColor);

        int ambientShadow = styleable.indexOf("outlineAmbientShadowColor");
        if (ambientShadow >= 0 && array.hasValue(ambientShadow))
        {
            mOutlineAmbientShadowColor = array.getColor(ambientShadow, mOutlineAmbientShadowColor);
        }
    }

    private void applyTransformAttributes(TypedArray array, StyleableSet styleable)
    {
        mAlpha = readFloat(array, styleable, "alpha", mAlpha);
        mElevation = readDimensionFloat(array, styleable, "elevation", mElevation);
        mTranslationX = readDimensionFloat(array, styleable, "translationX", mTranslationX);
        mTranslationY = readDimensionFloat(array, styleable, "translationY", mTranslationY);
        mTranslationZ = readDimensionFloat(array, styleable, "translationZ", mTranslationZ);
        mRotation = readFloat(array, styleable, "rotation", mRotation);
        mRotationX = readFloat(array, styleable, "rotationX", mRotationX);
        mRotationY = readFloat(array, styleable, "rotationY", mRotationY);
        mScaleX = readFloat(array, styleable, "scaleX", mScaleX);
        mScaleY = readFloat(array, styleable, "scaleY", mScaleY);

        int pivotX = styleable.indexOf("transformPivotX");
        if (pivotX >= 0 && array.hasValue(pivotX))
        {
            mPivotX = array.getDimension(pivotX, 0);
            mPivotExplicit = true;
        }

        int pivotY = styleable.indexOf("transformPivotY");
        if (pivotY >= 0 && array.hasValue(pivotY))
        {
            mPivotY = array.getDimension(pivotY, 0);
            mPivotExplicit = true;
        }

        mMatrixStale = true;
    }

    private void applyAccessibilityAttributes(TypedArray array, StyleableSet styleable)
    {
        CharSequence contentDescription = readText(array, styleable, "contentDescription");
        if (contentDescription != null) mContentDescription = contentDescription;

        CharSequence tooltip = readText(array, styleable, "tooltipText");
        if (tooltip != null) mTooltipText = tooltip;

        CharSequence paneTitle = readText(array, styleable, "accessibilityPaneTitle");
        if (paneTitle != null) mAccessibilityPaneTitle = paneTitle;

        mImportantForAccessibility = readInt(array, styleable, "importantForAccessibility", mImportantForAccessibility);
        mAccessibilityLiveRegion = readInt(array, styleable, "accessibilityLiveRegion", mAccessibilityLiveRegion);
        mImportantForAutofill = readInt(array, styleable, "importantForAutofill", mImportantForAutofill);
        mImportantForContentCapture = readInt(array, styleable, "importantForContentCapture", mImportantForContentCapture);
        mScreenReaderFocusable = readBoolean(array, styleable, "screenReaderFocusable", mScreenReaderFocusable);
        mAccessibilityHeading = readBoolean(array, styleable, "accessibilityHeading", mAccessibilityHeading);
        mLabelForId = readResourceId(array, styleable, "labelFor", mLabelForId);
        mAccessibilityTraversalBeforeId = readResourceId(array, styleable, "accessibilityTraversalBefore", mAccessibilityTraversalBeforeId);
        mAccessibilityTraversalAfterId = readResourceId(array, styleable, "accessibilityTraversalAfter", mAccessibilityTraversalAfterId);

        CharSequence hints = readText(array, styleable, "autofillHints");
        if (hints != null && hints.length() > 0) mAutofillHints = hints.toString().split(",");
    }

    private static android.graphics.PorterDuff.Mode parseTintMode(int value)
    {
        switch (value)
        {
            case 3:
                return android.graphics.PorterDuff.Mode.SRC_OVER;
            case 5:
                return android.graphics.PorterDuff.Mode.SRC_IN;
            case 9:
                return android.graphics.PorterDuff.Mode.SRC_ATOP;
            case 14:
                return android.graphics.PorterDuff.Mode.MULTIPLY;
            case 15:
                return android.graphics.PorterDuff.Mode.SCREEN;
            case 16:
                return android.graphics.PorterDuff.Mode.ADD;
            default:
                return null;
        }
    }

    private static boolean readBoolean(TypedArray array, StyleableSet styleable, String name, boolean fallback)
    {
        int index = styleable.indexOf(name);
        return index < 0 || !array.hasValue(index) ? fallback : array.getBoolean(index, fallback);
    }

    private static float readFloat(TypedArray array, StyleableSet styleable, String name, float fallback)
    {
        int index = styleable.indexOf(name);
        return index < 0 || !array.hasValue(index) ? fallback : array.getFloat(index, fallback);
    }

    private static float readDimensionFloat(TypedArray array, StyleableSet styleable, String name, float fallback)
    {
        int index = styleable.indexOf(name);
        return index < 0 || !array.hasValue(index) ? fallback : array.getDimension(index, fallback);
    }

    private static CharSequence readText(TypedArray array, StyleableSet styleable, String name)
    {
        int index = styleable.indexOf(name);
        return index < 0 || !array.hasValue(index) ? null : array.getText(index);
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "id", "background", "padding", "paddingLeft", "paddingTop", "paddingRight",
                                         "paddingBottom", "paddingStart", "paddingEnd", "paddingHorizontal", "paddingVertical",
                                         "visibility", "minWidth", "minHeight", "focusable", "clickable", "longClickable", "contextClickable",
                                         "focusableInTouchMode", "enabled", "saveEnabled", "duplicateParentState", "hapticFeedbackEnabled",
                                         "soundEffectsEnabled", "keepScreenOn", "nestedScrollingEnabled", "fitsSystemWindows",
                                         "focusedByDefault", "keyboardNavigationCluster", "clipToOutline", "isScrollContainer",
                                         "filterTouchesWhenObscured", "forceDarkAllowed", "defaultFocusHighlightEnabled", "nextFocusLeft",
                                         "nextFocusRight", "nextFocusUp", "nextFocusDown", "nextFocusForward", "nextClusterForward",
                                         "scrollX", "scrollY", "overScrollMode", "layerType", "scrollbars", "scrollIndicators",
                                         "requiresFadingEdge", "fadingEdgeLength", "layoutDirection", "textDirection", "textAlignment", "tag",
                                         "transitionName", "foreground", "foregroundGravity", "foregroundTint", "backgroundTint",
                                         "backgroundTintMode", "outlineSpotShadowColor", "outlineAmbientShadowColor", "alpha", "elevation",
                                         "translationX", "translationY", "translationZ", "rotation", "rotationX", "rotationY", "scaleX",
                                         "scaleY", "transformPivotX", "transformPivotY", "contentDescription", "tooltipText",
                                         "accessibilityPaneTitle", "importantForAccessibility", "accessibilityLiveRegion",
                                         "importantForAutofill", "importantForContentCapture", "screenReaderFocusable", "accessibilityHeading",
                                         "labelFor", "accessibilityTraversalBefore", "accessibilityTraversalAfter", "autofillHints");
        }
        return sStyleable;
    }

    private static int readResourceId(TypedArray array, StyleableSet styleable, String name, int fallback)
    {
        int index = styleable.indexOf(name);
        return index < 0 ? fallback : array.getResourceId(index, fallback);
    }

    private static int readInt(TypedArray array, StyleableSet styleable, String name, int fallback)
    {
        int index = styleable.indexOf(name);
        return index < 0 ? fallback : array.getInt(index, fallback);
    }

    private static int readDimension(TypedArray array, StyleableSet styleable, String name, int fallback)
    {
        int index = styleable.indexOf(name);
        return index < 0 ? fallback : array.getDimensionPixelSize(index, fallback);
    }

    public Context getContext()
    {
        return mContext;
    }

    public int getId()
    {
        return mId;
    }

    public void setId(int id)
    {
        mId = id;
    }

    public Object getTag()
    {
        return mTag;
    }

    public void setTag(Object tag)
    {
        mTag = tag;
    }

    public ViewParent getParent()
    {
        return mParent;
    }

    void assignParent(ViewParent parent)
    {
        mParent = parent;
    }

    public ViewGroup.LayoutParams getLayoutParams()
    {
        return mLayoutParams;
    }

    public void setLayoutParams(ViewGroup.LayoutParams layoutParams)
    {
        mLayoutParams = layoutParams;
        requestLayout();
    }

    public int getVisibility()
    {
        return mVisibility;
    }

    public void setVisibility(int visibility)
    {
        if (mVisibility == visibility) return;

        int oldVisibility = mVisibility;
        mVisibility = visibility;

        if (visibility != VISIBLE && hasFocus()) clearFocus();

        if (visibility == GONE || oldVisibility == GONE) requestLayout();
        invalidate();

        dispatchVisibilityChanged(this, visibility);

        if (mAttached) dispatchVisibilityAggregated(visibility == VISIBLE && isParentShown());
        if (mParent instanceof ViewGroup) ((ViewGroup)mParent).onChildVisibilityChanged(this, oldVisibility, visibility);
    }

    private boolean isParentShown()
    {
        ViewParent parent = mParent;
        if (!(parent instanceof View)) return parent != null;
        return ((View)parent).isShown();
    }

    protected void dispatchVisibilityChanged(View changedView, int visibility)
    {
        onVisibilityChanged(changedView, visibility);
    }

    protected void onVisibilityChanged(View changedView, int visibility)
    {
    }

    boolean dispatchVisibilityAggregated(boolean isVisible)
    {
        boolean thisVisible = mVisibility == VISIBLE;
        if (thisVisible || !isVisible) onVisibilityAggregated(isVisible);
        return thisVisible && isVisible;
    }

    public void onVisibilityAggregated(boolean isVisible)
    {
        if (mBackground != null && mBackground.isVisible() != isVisible) mBackground.setVisible(isVisible, false);
        if (mForeground != null && mForeground.isVisible() != isVisible) mForeground.setVisible(isVisible, false);
    }

    public void setBackgroundColor(int color)
    {
        mBackgroundColor = color;
        mHasBackground = true;
        invalidate();
    }

    public int getBackgroundColor()
    {
        return mBackgroundColor;
    }

    public void setPadding(int left, int top, int right, int bottom)
    {
        mPaddingRelative = false;
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
        requestLayout();
    }

    public int getPaddingLeft()
    {
        return mPaddingLeft;
    }

    public int getPaddingTop()
    {
        return mPaddingTop;
    }

    public int getPaddingRight()
    {
        return mPaddingRight;
    }

    public int getPaddingBottom()
    {
        return mPaddingBottom;
    }

    public void setMinimumWidth(int width)
    {
        mMinimumWidth = width;
        requestLayout();
    }

    public int getMinimumWidth()
    {
        return mMinimumWidth;
    }

    public void setMinimumHeight(int height)
    {
        mMinimumHeight = height;
        requestLayout();
    }

    public int getMinimumHeight()
    {
        return mMinimumHeight;
    }

    public int getSuggestedMinimumWidth()
    {
        return mMinimumWidth;
    }

    public int getSuggestedMinimumHeight()
    {
        return mMinimumHeight;
    }

    public final int getLeft()
    {
        return mLeft;
    }

    public final void setLeft(int left)
    {
        mLeft = left;
        invalidate();
    }

    public final void setTop(int top)
    {
        mTop = top;
        invalidate();
    }

    public final void setRight(int right)
    {
        mRight = right;
        invalidate();
    }

    public final void setBottom(int bottom)
    {
        mBottom = bottom;
        invalidate();
    }

    public final int getTop()
    {
        return mTop;
    }

    public final int getRight()
    {
        return mRight;
    }

    public final int getBottom()
    {
        return mBottom;
    }

    public final int getWidth()
    {
        return mRight - mLeft;
    }

    public final int getHeight()
    {
        return mBottom - mTop;
    }

    public final int getMeasuredWidth()
    {
        return mMeasuredWidth & MEASURED_SIZE_MASK;
    }

    public final int getMeasuredHeight()
    {
        return mMeasuredHeight & MEASURED_SIZE_MASK;
    }

    public final int getMeasuredWidthAndState()
    {
        return mMeasuredWidth;
    }

    public final int getMeasuredHeightAndState()
    {
        return mMeasuredHeight;
    }

    public void getDrawingRect(Rect out)
    {
        out.set(mScrollX, mScrollY, mScrollX + getWidth(), mScrollY + getHeight());
    }

    public final void measure(int widthMeasureSpec, int heightMeasureSpec)
    {
        resolveLayoutParams();
        onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void resolveLayoutParams()
    {
        if (mLayoutParams != null) mLayoutParams.resolveLayoutDirection(getLayoutDirection());
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                             getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight)
    {
        mMeasuredWidth = measuredWidth;
        mMeasuredHeight = measuredHeight;
    }

    public static int getDefaultSize(int size, int measureSpec)
    {
        int mode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (mode == MeasureSpec.UNSPECIFIED) return size;
        return specSize;
    }

    public static final int MEASURED_SIZE_MASK = 0x00ffffff;
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;
    public static final int MEASURED_STATE_MASK = 0xff000000;
    public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;

    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState)
    {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        int result;
        if (specMode == MeasureSpec.AT_MOST)
        {
            result = specSize < size ? specSize | MEASURED_STATE_TOO_SMALL : size;
        }
        else if (specMode == MeasureSpec.EXACTLY)
        {
            result = specSize;
        }
        else
        {
            result = size;
        }

        return result | (childMeasuredState & MEASURED_STATE_MASK);
    }

    public static int combineMeasuredStates(int currentState, int newState)
    {
        return currentState | newState;
    }

    public static int resolveSize(int size, int measureSpec)
    {
        int mode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (mode == MeasureSpec.EXACTLY) return specSize;
        if (mode == MeasureSpec.AT_MOST) return Math.min(size, specSize);
        return size;
    }

    public void layout(int left, int top, int right, int bottom)
    {
        int oldLeft = mLeft;
        int oldTop = mTop;
        int oldRight = mRight;
        int oldBottom = mBottom;
        boolean changed;

        mInLayout = true;

        try
        {
            changed = setFrame(left, top, right, bottom);
            onLayout(changed, mLeft, mTop, mRight, mBottom);
        }
        finally
        {
            mInLayout = false;
        }

        left = mLeft;
        top = mTop;
        right = mRight;
        bottom = mBottom;
        boolean sizeChanged = right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop;

        mLayoutRequested = false;
        mEverLaidOut = true;

        if (!mLayoutChangeListeners.isEmpty())
        {
            ArrayList<OnLayoutChangeListener> listeners = new ArrayList<>(mLayoutChangeListeners);
            for (int index = 0; index < listeners.size(); index++)
            {
                listeners.get(index).onLayoutChange(this, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
            }
        }

        if (mParent instanceof ViewRootImpl && !sizeChanged && mViewTreeObserver != null) mViewTreeObserver.dispatchOnGlobalLayout();
    }

    protected boolean setFrame(int left, int top, int right, int bottom)
    {
        if (left == mLeft && top == mTop && right == mRight && bottom == mBottom) return false;

        int oldWidth = mRight - mLeft;
        int oldHeight = mBottom - mTop;

        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;

        int width = right - left;
        int height = bottom - top;

        if (width != oldWidth || height != oldHeight)
        {
            mMatrixStale = true;
            if (mBackground != null) mBackground.setBounds(0, 0, width, height);
            if (mForeground != null) mForeground.setBounds(0, 0, width, height);
            if (mOverlay != null)
            {
                mOverlay.getOverlayView().setRight(width);
                mOverlay.getOverlayView().setBottom(height);
            }

            onSizeChanged(width, height, oldWidth, oldHeight);
        }

        return true;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
    }

    public void requestLayout()
    {
        mLayoutRequested = true;
        if (mParent != null) mParent.requestLayout();
    }

    public boolean isLayoutRequested()
    {
        return mLayoutRequested;
    }

    public void invalidate()
    {
        invalidateInternal(0, 0, getWidth(), getHeight());
    }

    public void invalidate(Rect dirty)
    {
        if (dirty == null)
        {
            invalidateInternal(0, 0, getWidth(), getHeight());
            return;
        }

        invalidateInternal(dirty.left - mScrollX, dirty.top - mScrollY, dirty.right - mScrollX, dirty.bottom - mScrollY);
    }

    private void invalidateInternal(int left, int top, int right, int bottom)
    {
        mDirty = true;
        if (mParent != null) mParent.invalidateChild(this, new Rect(left, top, right, bottom));
    }

    public void forceLayout()
    {
        mLayoutRequested = true;
    }

    public boolean isDirty()
    {
        return mDirty;
    }

    static final boolean TRACE_DRAW = false;

    public void draw(Canvas canvas)
    {
        if (mVisibility != VISIBLE && mAnimation == null) return;

        boolean windowRoot = mParent instanceof ViewRootImpl;
        if (windowRoot && mViewTreeObserver != null && mViewTreeObserver.dispatchOnPreDraw()) invalidate();

        if (TRACE_DRAW)
        {
            android.util.Log.i("View", "draw " + getClass().getName() + " " + getWidth() + "x" + getHeight()
                                       + " alpha=" + mAlpha);
        }

        mDirty = false;

        if (mHasBackground)
        {
            mBackgroundPaint.setColor(mBackgroundColor);
            canvas.drawRect(0, 0, getWidth(), getHeight(), mBackgroundPaint);
        }
        else if (mBackground != null)
        {
            mBackground.setBounds(0, 0, getWidth(), getHeight());
            mBackground.draw(canvas);
        }

        computeScroll();

        if (mScrollX == 0 && mScrollY == 0 && mClipBounds == null)
        {
            onDraw(canvas);
            dispatchDraw(canvas);
        }
        else
        {
            int saveCount = canvas.save();

            if (mClipBounds != null) canvas.clipRect(mClipBounds);

            canvas.translate(-mScrollX, -mScrollY);
            onDraw(canvas);
            dispatchDraw(canvas);
            canvas.restoreToCount(saveCount);
        }

        if (mOverlay != null && !mOverlay.isEmpty()) mOverlay.getOverlayView().dispatchDraw(canvas);

        onDrawForeground(canvas);

        if (windowRoot && mViewTreeObserver != null)
        {
            mViewTreeObserver.dispatchOnDraw();
            mViewTreeObserver.dispatchOnFrameCommit();
        }
    }

    protected void onDraw(Canvas canvas)
    {
    }

    protected void dispatchDraw(Canvas canvas)
    {
    }

    public final void saveAttributeDataForStyleable(Context context, int[] styleable, AttributeSet attributes, TypedArray array,
                                                    int defStyleAttr, int defStyleRes)
    {
    }

    public void getLocationInWindow(int[] location)
    {
        if (location == null || location.length < 2) throw new IllegalArgumentException("outLocation must be an array of two integers");

        float[] position = {0, 0};
        transformFromViewToWindowSpace(position);

        location[0] = (int)(position[0] + 0.5f);
        location[1] = (int)(position[1] + 0.5f);
    }

    void transformFromViewToWindowSpace(float[] position)
    {
        if (!hasIdentityMatrix()) getMatrix().mapPoints(position);

        position[0] += mLeft;
        position[1] += mTop;

        ViewParent parent = mParent;
        while (parent instanceof View)
        {
            View view = (View)parent;

            position[0] -= view.mScrollX;
            position[1] -= view.mScrollY;

            if (!view.hasIdentityMatrix()) view.getMatrix().mapPoints(position);

            position[0] += view.mLeft;
            position[1] += view.mTop;

            parent = view.mParent;
        }
    }

    public void getLocationOnScreen(int[] location)
    {
        getLocationInWindow(location);

        int[] origin = new int[2];
        if (ViewRootImpl.panelOrigin(getRootView(), origin))
        {
            location[0] += origin[0];
            location[1] += origin[1];
        }
    }

    public void getLocationInSurface(int[] location)
    {
        getLocationInWindow(location);
    }

    protected void onFinishInflate()
    {
    }

    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
    }

    public int getBaseline()
    {
        return -1;
    }

    public boolean isLaidOut()
    {
        return mEverLaidOut;
    }

    public boolean isInLayout()
    {
        View current = this;

        while (true)
        {
            if (current.mInLayout) return true;
            if (!(current.mParent instanceof View)) return false;

            current = (View)current.mParent;
        }
    }

    public boolean isInEditMode()
    {
        return false;
    }

    public boolean isShown()
    {
        View current = this;

        while (true)
        {
            if (current.mVisibility != VISIBLE) return false;

            ViewParent parent = current.mParent;
            if (parent == null) return false;
            if (!(parent instanceof View)) return true;

            current = (View)parent;
        }
    }

    public void requestApplyInsets()
    {
        ViewRootImpl root = findViewRoot();
        if (root == null)
        {
            mPendingInsetsRequest = true;
            return;
        }

        final View top = getRootView();
        if (top.mInsetsRequestPosted) return;

        top.mInsetsRequestPosted = true;

        Runnable dispatch = new Runnable()
        {
            @Override
            public void run()
            {
                top.mInsetsRequestPosted = false;

                ViewRootImpl current = top.findViewRoot();
                if (current == null) return;

                WindowInsets insets = current.getWindowInsets();
                if (insets != null) top.dispatchApplyWindowInsets(insets);
                top.requestLayout();
            }
        };

        if (!top.post(dispatch)) dispatch.run();
    }

    private boolean mInsetsRequestPosted;

    @Deprecated
    public void requestFitSystemWindows()
    {
        requestApplyInsets();
    }

    @Deprecated
    protected boolean fitSystemWindows(Rect insets)
    {
        if (!mFitsSystemWindows) return false;

        internalSetPadding(insets.left, insets.top, insets.right, insets.bottom);
        return true;
    }

    private void internalSetPadding(int left, int top, int right, int bottom)
    {
        setPadding(left, top, right, bottom);
    }

    public WindowInsets computeSystemWindowInsets(WindowInsets in, Rect outLocalInsets)
    {
        if (outLocalInsets != null)
        {
            outLocalInsets.set(in.getSystemWindowInsetLeft(), in.getSystemWindowInsetTop(), in.getSystemWindowInsetRight(),
                               in.getSystemWindowInsetBottom());
        }

        return in.consumeSystemWindowInsets();
    }

    public void setPaddingRelative(int start, int top, int end, int bottom)
    {
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        setPadding(rtl ? end : start, top, rtl ? start : end, bottom);
        mPaddingRelative = true;
    }

    public int getPaddingStart()
    {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL ? mPaddingRight : mPaddingLeft;
    }

    public int getPaddingEnd()
    {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL ? mPaddingLeft : mPaddingRight;
    }

    public boolean isPaddingRelative()
    {
        return mPaddingRelative;
    }

    public int getMeasuredState()
    {
        return (mMeasuredWidth & MEASURED_STATE_MASK)
                | ((mMeasuredHeight >> MEASURED_HEIGHT_STATE_SHIFT) & (MEASURED_STATE_MASK >> MEASURED_HEIGHT_STATE_SHIFT));
    }

    public void setContentDescription(CharSequence description)
    {
        mContentDescription = description;
    }

    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    public void setSaveFromParentEnabled(boolean enabled)
    {
        mSaveFromParentEnabled = enabled;
    }

    public boolean isSaveFromParentEnabled()
    {
        return mSaveFromParentEnabled;
    }

    private int mOverScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS;

    public void setOverScrollMode(int mode)
    {
        mOverScrollMode = mode;
    }

    public int getOverScrollMode()
    {
        return mOverScrollMode;
    }

    public void setScrollBarStyle(int style)
    {
        mScrollBarStyle = style;
    }

    public int getScrollBarStyle()
    {
        return mScrollBarStyle;
    }

    public void offsetTopAndBottom(int offset)
    {
        if (offset == 0) return;

        mTop += offset;
        mBottom += offset;
        invalidateParentIfNeeded();
    }

    public void offsetLeftAndRight(int offset)
    {
        if (offset == 0) return;

        mLeft += offset;
        mRight += offset;
        invalidateParentIfNeeded();
    }

    private void invalidateParentIfNeeded()
    {
        if (mParent instanceof View) ((View)mParent).invalidate();
        else if (mParent != null) mParent.invalidateChild(this, null);
    }

    public void measureChildForAccessibility()
    {
    }

    public int getSystemUiVisibility()
    {
        return mSystemUiVisibility;
    }

    public void setSystemUiVisibility(int visibility)
    {
        if (mSystemUiVisibility == visibility) return;

        mSystemUiVisibility = visibility;

        if (mSystemUiVisibilityListener != null) mSystemUiVisibilityListener.onSystemUiVisibilityChange(visibility);
    }

    public int getWindowSystemUiVisibility()
    {
        return getRootView().mSystemUiVisibility | mSystemUiVisibility;
    }

    public void onWindowSystemUiVisibilityChanged(int visible)
    {
    }

    public void dispatchWindowSystemUiVisiblityChanged(int visible)
    {
        onWindowSystemUiVisibilityChanged(visible);
    }

    public void dispatchSystemUiVisibilityChanged(int visibility)
    {
        if (mSystemUiVisibilityListener != null) mSystemUiVisibilityListener.onSystemUiVisibilityChange(visibility);
    }

    public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener listener)
    {
        mSystemUiVisibilityListener = listener;
    }

    public WindowInsetsController getWindowInsetsController()
    {
        return mInsetsController;
    }

    public AttachedSurfaceControl getRootSurfaceControl()
    {
        return mAttached ? findViewRoot() : null;
    }

    public WindowInsets getRootWindowInsets()
    {
        ViewParent parent = mParent;

        while (parent instanceof View) parent = ((View)parent).mParent;

        if (parent instanceof ViewRootImpl) return ((ViewRootImpl)parent).getWindowInsets();

        return WindowInsets.empty();
    }

    public void setWillNotDraw(boolean willNotDraw)
    {
        mWillNotDraw = willNotDraw;
    }

    public boolean willNotDraw()
    {
        return mWillNotDraw;
    }

    public void setFitsSystemWindows(boolean fits)
    {
        mFitsSystemWindows = fits;
    }

    public boolean getFitsSystemWindows()
    {
        return mFitsSystemWindows;
    }

    public void setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener listener)
    {
        mInsetsListener = listener;
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets)
    {
        return insets;
    }

    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets)
    {
        if (mInsetsListener != null) return mInsetsListener.onApplyWindowInsets(this, insets);
        return onApplyWindowInsets(insets);
    }

    public void setOnClickListener(OnClickListener listener)
    {
        mClickListener = listener;
    }

    public boolean hasOnClickListeners()
    {
        return mClickListener != null;
    }

    public boolean performClick()
    {
        boolean result = false;

        if (mClickListener != null)
        {
            playSoundEffect(SoundEffectConstants.CLICK);
            mClickListener.onClick(this);
            result = true;
        }

        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
        return result;
    }

    public boolean callOnClick()
    {
        if (mClickListener == null) return false;

        mClickListener.onClick(this);
        return true;
    }

    private boolean withinTouchSlop(float x, float y)
    {
        int slop = ViewConfiguration.get(mContext).getScaledTouchSlop();

        return Math.abs(x - mDownX) <= slop && Math.abs(y - mDownY) <= slop;
    }

    private void scheduleLongPress()
    {
        if (!mLongClickable && mLongClickListener == null) return;

        if (mLongPressCallback == null)
        {
            mLongPressCallback = new Runnable()
            {
                @Override
                public void run()
                {
                    if (!mPressed) return;

                    mLongClickFired = performLongClick();
                }
            };
        }

        postDelayed(mLongPressCallback, ViewConfiguration.getLongPressTimeout());
    }

    public void cancelLongPress()
    {
        if (mLongPressCallback != null) removeCallbacks(mLongPressCallback);
    }

    public void setClickable(boolean clickable)
    {
        mClickable = clickable;
        if (clickable && mFocusableAuto) mFocusable = true;
    }

    public boolean isClickable()
    {
        return mClickable || mClickListener != null;
    }

    public boolean dispatchTouchEvent(MotionEvent event)
    {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) stopNestedScroll();

        boolean result = false;

        if (onFilterTouchEventForSecurity(event))
        {
            if (mEnabled && mTouchListener != null && mTouchListener.onTouch(this, event)) result = true;
            if (!result && onTouchEvent(event)) result = true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || (action == MotionEvent.ACTION_DOWN && !result))
        {
            stopNestedScroll();
        }

        return result;
    }

    public boolean onFilterTouchEventForSecurity(MotionEvent event)
    {
        return true;
    }

    public void setFilterTouchesWhenObscured(boolean enabled)
    {
        mFilterTouchesWhenObscured = enabled;
    }

    public boolean getFilterTouchesWhenObscured()
    {
        return mFilterTouchesWhenObscured;
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        if (TRACE_INPUT && event.getActionMasked() == MotionEvent.ACTION_DOWN)
        {
            android.util.Log.i("View", "the base onTouchEvent ran for " + getClass().getName()
                                           + ", so that class does not override it; touch listener "
                                           + (mTouchListener != null ? "present" : "absent"));
        }

        int action = event.getActionMasked();
        boolean clickable = isClickable() || mLongClickable || mContextClickable;

        if (!mEnabled)
        {
            if (action == MotionEvent.ACTION_UP && mPressed) setPressed(false);
            return clickable;
        }

        if (mTouchDelegate != null && mTouchDelegate.onTouchEvent(event)) return true;

        if (!clickable) return false;

        mLastTouchX = event.getX();
        mLastTouchY = event.getY();

        if (action == MotionEvent.ACTION_DOWN)
        {
            mDownX = event.getX();
            mDownY = event.getY();
            mLongClickFired = false;

            setPressed(true);
            scheduleLongPress();
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE)
        {
            if (mPressed && !withinTouchSlop(event.getX(), event.getY()))
            {
                cancelLongPress();
                setPressed(false);
            }

            return mPressed;
        }

        if (action == MotionEvent.ACTION_CANCEL)
        {
            cancelLongPress();
            setPressed(false);
            return true;
        }

        if (action != MotionEvent.ACTION_UP) return false;

        boolean wasPressed = mPressed;

        cancelLongPress();
        setPressed(false);

        if (wasPressed && !mLongClickFired && containsLocal(event.getX(), event.getY()))
        {
            boolean focusTaken = mFocusableInTouchMode && mFocusable && !mFocused && requestFocus();
            if (!focusTaken) performClick();
        }

        return true;
    }

    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (mEnabled && mKeyListener != null && mKeyListener.onKey(this, event.getKeyCode(), event)) return true;

        if (event.getAction() == KeyEvent.ACTION_DOWN) return onKeyDown(event.getKeyCode(), event);
        if (event.getAction() == KeyEvent.ACTION_UP) return onKeyUp(event.getKeyCode(), event);
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE) return onKeyMultiple(event.getKeyCode(), event.getRepeatCount(), event);

        return false;
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event)
    {
        return false;
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event)
    {
        return false;
    }

    public boolean dispatchKeyEventPreIme(KeyEvent event)
    {
        return onKeyPreIme(event.getKeyCode(), event);
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        return false;
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent event)
    {
        return onKeyShortcut(event.getKeyCode(), event);
    }

    public boolean onKeyShortcut(int keyCode, KeyEvent event)
    {
        return false;
    }

    public KeyEvent.DispatcherState getKeyDispatcherState()
    {
        ViewRootImpl root = findViewRoot();
        if (root == null) return null;

        if (sKeyDispatcherState == null) sKeyDispatcherState = new KeyEvent.DispatcherState();
        return sKeyDispatcherState;
    }

    private static KeyEvent.DispatcherState sKeyDispatcherState;

    public void addOnUnhandledKeyEventListener(OnUnhandledKeyEventListener listener)
    {
        if (listener == null) return;

        if (mUnhandledKeyListeners == null) mUnhandledKeyListeners = new ArrayList<>();
        mUnhandledKeyListeners.add(listener);
    }

    public void removeOnUnhandledKeyEventListener(OnUnhandledKeyEventListener listener)
    {
        if (mUnhandledKeyListeners != null) mUnhandledKeyListeners.remove(listener);
    }

    boolean dispatchUnhandledKeyEvent(KeyEvent event)
    {
        View current = this;

        while (true)
        {
            if (current.mUnhandledKeyListeners != null)
            {
                ArrayList<OnUnhandledKeyEventListener> listeners = new ArrayList<>(current.mUnhandledKeyListeners);
                for (int index = listeners.size() - 1; index >= 0; index--)
                {
                    if (listeners.get(index).onUnhandledKeyEvent(current, event)) return true;
                }
            }

            if (!(current.mParent instanceof View)) return false;
            current = (View)current.mParent;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        return false;
    }

    boolean containsLocal(float x, float y)
    {
        return x >= 0 && y >= 0 && x < getWidth() && y < getHeight();
    }

    public void setFocusable(boolean focusable)
    {
        mFocusableAuto = false;
        mFocusable = focusable;
        if (!focusable && mFocused) clearFocus();
    }

    public boolean isFocusable()
    {
        return mFocusable;
    }

    public boolean isFocused()
    {
        return mFocused;
    }

    public boolean hasFocus()
    {
        return mFocused;
    }

    public View findFocus()
    {
        return mFocused ? this : null;
    }

    ViewRootImpl findViewRoot()
    {
        ViewParent parent = mParent;

        while (parent != null)
        {
            if (parent instanceof ViewRootImpl) return (ViewRootImpl)parent;
            parent = parent.getParent();
        }

        return null;
    }

    void onFocusLost()
    {
        if (!mFocused) return;

        mFocused = false;
        if (mParent != null) mParent.clearChildFocus(this);

        refreshDrawableState();
        onFocusChanged(false, 0, null);

        if (mFocusChangeListener != null) mFocusChangeListener.onFocusChange(this, false);
    }

    protected void onFocusChanged(boolean gained, int direction, Rect previouslyFocusedRect)
    {
    }

    public boolean onCheckIsTextEditor()
    {
        return false;
    }

    public android.view.inputmethod.InputConnection onCreateInputConnection(
        android.view.inputmethod.EditorInfo outAttributes)
    {
        return null;
    }

    public boolean onHoverEvent(MotionEvent event)
    {
        int action = event.getActionMasked();

        if (!mSendingHoverAccessibilityEvents && (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE)
            && isHoverable())
        {
            mSendingHoverAccessibilityEvents = true;
            setHovered(true);
        }
        else if (action == MotionEvent.ACTION_HOVER_EXIT)
        {
            mSendingHoverAccessibilityEvents = false;
            setHovered(false);
        }

        return isHoverable();
    }

    private boolean isHoverable()
    {
        if (!mEnabled) return false;
        return isClickable() || mLongClickable || mContextClickable;
    }

    public boolean dispatchHoverEvent(MotionEvent event)
    {
        if (mHoverListener != null && mEnabled && mHoverListener.onHover(this, event)) return true;
        return onHoverEvent(event);
    }

    public void setHovered(boolean hovered)
    {
        if (mHovered == hovered) return;

        mHovered = hovered;
        refreshDrawableState();
        onHoverChanged(hovered);
    }

    public boolean isHovered()
    {
        return mHovered;
    }

    public void onHoverChanged(boolean hovered)
    {
    }

    public boolean dispatchGenericMotionEvent(MotionEvent event)
    {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE
            || action == MotionEvent.ACTION_HOVER_EXIT)
        {
            return dispatchHoverEvent(event);
        }

        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0)
        {
            if (dispatchGenericPointerEvent(event)) return true;
        }
        else if (dispatchGenericFocusedEvent(event))
        {
            return true;
        }

        if (mGenericMotionListener != null && mEnabled && mGenericMotionListener.onGenericMotion(this, event)) return true;
        return onGenericMotionEvent(event);
    }

    public boolean onGenericMotionEvent(MotionEvent event)
    {
        return false;
    }

    public boolean dispatchTrackballEvent(MotionEvent event)
    {
        return onTrackballEvent(event);
    }

    public boolean onTrackballEvent(MotionEvent event)
    {
        return false;
    }

    public boolean isVisibleToUserForAutofill(int virtualId)
    {
        return isShown() && getVisibility() == VISIBLE;
    }

    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY,
                                   int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent)
    {
        int overScrollMode = getOverScrollMode();
        boolean canScrollHorizontal = computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        boolean canScrollVertical = computeVerticalScrollRange() > computeVerticalScrollExtent();
        boolean overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS
                                       || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        boolean overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS
                                     || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) maxOverScrollX = 0;

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) maxOverScrollY = 0;

        int left = -maxOverScrollX;
        int right = maxOverScrollX + scrollRangeX;
        int top = -maxOverScrollY;
        int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right)
        {
            newScrollX = right;
            clampedX = true;
        }
        else if (newScrollX < left)
        {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom)
        {
            newScrollY = bottom;
            clampedY = true;
        }
        else if (newScrollY < top)
        {
            newScrollY = top;
            clampedY = true;
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY)
    {
    }

    public void setEnabled(boolean enabled)
    {
        if (mEnabled == enabled) return;

        mEnabled = enabled;
        refreshDrawableState();
        invalidate();
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    public boolean isAttachedToWindow()
    {
        return mAttached;
    }

    void dispatchAttachedToWindow()
    {
        if (mAttached) return;

        mAttached = true;
        mWindowAttachCount++;

        if (TRACE_DRAW) android.util.Log.i("View", "attach " + getClass().getName());

        View top = getRootView();
        if (top != this && mViewTreeObserver != null)
        {
            top.getViewTreeObserver().merge(mViewTreeObserver);
            mViewTreeObserver = null;
        }

        if (mOverlay != null) mOverlay.getOverlayView().dispatchAttachedToWindow();

        onAttachedToWindow();

        if (mParent instanceof ViewRootImpl && mViewTreeObserver != null) mViewTreeObserver.dispatchOnWindowAttachedChange(true);

        onWindowVisibilityChanged(mVisibility);
        if (mVisibility == VISIBLE) onVisibilityAggregated(isShown());

        if (mPendingInsetsRequest)
        {
            mPendingInsetsRequest = false;
            requestApplyInsets();
        }

        if (mAttachListeners != null)
        {
            for (int index = 0; index < mAttachListeners.size(); index++)
            {
                mAttachListeners.get(index).onViewAttachedToWindow(this);
            }
        }
    }

    void dispatchDetachedFromWindow()
    {
        if (!mAttached) return;

        mAttached = false;

        if (mParent instanceof ViewRootImpl && mViewTreeObserver != null) mViewTreeObserver.dispatchOnWindowAttachedChange(false);

        cancelLongPress();
        if (mPressed) setPressed(false);
        if (mAnimation != null && !mAnimation.hasEnded()) mAnimation.cancel();
        stopNestedScroll();

        onDetachedFromWindow();

        if (mOverlay != null) mOverlay.getOverlayView().dispatchDetachedFromWindow();

        if (mAttachListeners != null)
        {
            for (int index = 0; index < mAttachListeners.size(); index++)
            {
                mAttachListeners.get(index).onViewDetachedFromWindow(this);
            }
        }
    }

    protected void onAttachedToWindow()
    {
    }

    protected void onDetachedFromWindow()
    {
    }

    protected void onWindowVisibilityChanged(int visibility)
    {
    }

    public void onWindowFocusChanged(boolean hasFocus)
    {
    }

    public boolean hasWindowFocus()
    {
        return mAttached;
    }

    public int getWindowVisibility()
    {
        return mAttached ? VISIBLE : GONE;
    }

    public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener)
    {
        if (listener == null) return;

        if (mAttachListeners == null) mAttachListeners = new java.util.ArrayList<>();
        mAttachListeners.add(listener);

        if (mAttached) listener.onViewAttachedToWindow(this);
    }

    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener)
    {
        if (mAttachListeners != null) mAttachListeners.remove(listener);
    }

    public View getRootView()
    {
        View cursor = this;
        while (cursor.mParent instanceof View)
        {
            cursor = (View)cursor.mParent;
        }
        return cursor;
    }

    public android.os.IBinder getWindowToken()
    {
        if (mParent == null) return null;

        View root = getRootView();
        if (root != this) return root.getWindowToken();

        if (mWindowToken == null) mWindowToken = new android.os.Binder("android.view.ViewRootImpl");
        return mWindowToken;
    }

    public android.os.IBinder getApplicationWindowToken()
    {
        return getWindowToken();
    }

    public ViewTreeObserver getViewTreeObserver()
    {
        View root = getRootView();
        if (root != this) return root.getViewTreeObserver();

        if (mViewTreeObserver == null) mViewTreeObserver = new ViewTreeObserver();
        return mViewTreeObserver;
    }

    public android.content.res.Resources getResources()
    {
        return mContext.getResources();
    }

    public void setTag(int key, Object tag)
    {
        if (mKeyedTags == null) mKeyedTags = new java.util.HashMap<>();
        mKeyedTags.put(key, tag);
    }

    public Object getTag(int key)
    {
        return mKeyedTags == null ? null : mKeyedTags.get(key);
    }

    public void setAccessibilityDelegate(AccessibilityDelegate delegate)
    {
        mAccessibilityDelegate = delegate;
    }

    public AccessibilityDelegate getAccessibilityDelegate()
    {
        return mAccessibilityDelegate;
    }

    public static class AccessibilityDelegate
    {
        public AccessibilityDelegate()
        {
        }

        public void sendAccessibilityEvent(View host, int eventType)
        {
            host.sendAccessibilityEventInternal(eventType);
        }

        public boolean performAccessibilityAction(View host, int action, Bundle arguments)
        {
            return host.performAccessibilityActionInternal(action, arguments);
        }

        public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event)
        {
            host.sendAccessibilityEventUncheckedInternal(event);
        }

        public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event)
        {
            return host.dispatchPopulateAccessibilityEventInternal(event);
        }

        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event)
        {
            host.onPopulateAccessibilityEventInternal(event);
        }

        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event)
        {
            host.onInitializeAccessibilityEventInternal(event);
        }

        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info)
        {
            host.onInitializeAccessibilityNodeInfoInternal(info);
        }

        public void addExtraDataToAccessibilityNodeInfo(View host, AccessibilityNodeInfo info, String extraDataKey, Bundle arguments)
        {
            host.addExtraDataToAccessibilityNodeInfo(info, extraDataKey, arguments);
        }

        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event)
        {
            return host.onRequestSendAccessibilityEventInternal(child, event);
        }

        public android.view.accessibility.AccessibilityNodeProvider getAccessibilityNodeProvider(View host)
        {
            return null;
        }

        public AccessibilityNodeInfo createAccessibilityNodeInfo(View host)
        {
            return host.createAccessibilityNodeInfoInternal();
        }
    }

    public void setImportantForAccessibility(int mode)
    {
        mImportantForAccessibility = mode;
    }

    public void setLayoutDirection(int layoutDirection)
    {
        if (layoutDirection < LAYOUT_DIRECTION_LTR || layoutDirection > LAYOUT_DIRECTION_LOCALE) return;
        mLayoutDirection = layoutDirection;
    }

    public int getLayoutDirection()
    {
        if (mLayoutDirection == LAYOUT_DIRECTION_INHERIT)
        {
            return mParent instanceof View ? ((View)mParent).getLayoutDirection() : LAYOUT_DIRECTION_LTR;
        }

        if (mLayoutDirection == LAYOUT_DIRECTION_LOCALE) return LAYOUT_DIRECTION_LTR;
        return mLayoutDirection;
    }

    public void setElevation(float elevation)
    {
        if (mElevation == elevation) return;

        mElevation = elevation;
        invalidate();
    }

    public void setAlpha(float alpha)
    {
        if (mAlpha == alpha) return;

        mAlpha = alpha;
        invalidate();
    }

    public void setBackground(android.graphics.drawable.Drawable background)
    {
        if (mBackground == background) return;

        if (mBackground != null) mBackground.setCallback(null);

        mBackground = background;

        if (background == null)
        {
            mHasBackground = false;
            invalidate();
            return;
        }

        background.setCallback(this);
        background.setBounds(0, 0, getWidth(), getHeight());

        if (background.isStateful()) background.setState(getDrawableState());

        if (background instanceof android.graphics.drawable.ColorDrawable)
        {
            mBackgroundColor = ((android.graphics.drawable.ColorDrawable)background).getColor();
            mHasBackground = true;
        }
        else
        {
            mHasBackground = false;
        }

        invalidate();
    }

    public android.graphics.drawable.Drawable getBackground()
    {
        if (mBackground != null) return mBackground;

        return mHasBackground ? new android.graphics.drawable.ColorDrawable(mBackgroundColor) : null;
    }

    public void setBackgroundResource(int resourceId)
    {
        if (resourceId == 0)
        {
            setBackground(null);
            return;
        }

        setBackground(mContext.getResources().getDrawable(resourceId));
    }

    protected static final int[] EMPTY_STATE_SET = new int[0];

    private static final int STATE_WINDOW_FOCUSED = 0x0101009d;
    private static final int STATE_FOCUSED = 0x0101009c;
    private static final int STATE_ENABLED = 0x0101009e;
    private static final int STATE_SELECTED = 0x010100a1;
    private static final int STATE_PRESSED = 0x010100a7;
    private static final int STATE_ACTIVATED = 0x010102fe;
    private static final int STATE_HOVERED = 0x01010367;

    protected static final int[] ENABLED_STATE_SET = {STATE_ENABLED};
    protected static final int[] FOCUSED_STATE_SET = {STATE_FOCUSED};
    protected static final int[] SELECTED_STATE_SET = {STATE_SELECTED};
    protected static final int[] PRESSED_STATE_SET = {STATE_PRESSED};
    protected static final int[] WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED};
    protected static final int[] ENABLED_FOCUSED_STATE_SET = {STATE_FOCUSED, STATE_ENABLED};
    protected static final int[] ENABLED_SELECTED_STATE_SET = {STATE_SELECTED, STATE_ENABLED};
    protected static final int[] ENABLED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_ENABLED};
    protected static final int[] FOCUSED_SELECTED_STATE_SET = {STATE_SELECTED, STATE_FOCUSED};
    protected static final int[] FOCUSED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_FOCUSED};
    protected static final int[] SELECTED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_SELECTED};
    protected static final int[] ENABLED_FOCUSED_SELECTED_STATE_SET = {STATE_SELECTED, STATE_FOCUSED, STATE_ENABLED};
    protected static final int[] ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_FOCUSED, STATE_ENABLED};
    protected static final int[] ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_SELECTED, STATE_ENABLED};
    protected static final int[] FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_SELECTED, STATE_FOCUSED};
    protected static final int[] ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET =
        {STATE_WINDOW_FOCUSED, STATE_SELECTED, STATE_FOCUSED, STATE_ENABLED};
    protected static final int[] PRESSED_ENABLED_STATE_SET = {STATE_ENABLED, STATE_PRESSED};
    protected static final int[] PRESSED_FOCUSED_STATE_SET = {STATE_FOCUSED, STATE_PRESSED};
    protected static final int[] PRESSED_SELECTED_STATE_SET = {STATE_SELECTED, STATE_PRESSED};
    protected static final int[] PRESSED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_PRESSED};
    protected static final int[] PRESSED_ENABLED_FOCUSED_STATE_SET = {STATE_FOCUSED, STATE_ENABLED, STATE_PRESSED};
    protected static final int[] PRESSED_ENABLED_SELECTED_STATE_SET = {STATE_SELECTED, STATE_ENABLED, STATE_PRESSED};
    protected static final int[] PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_ENABLED, STATE_PRESSED};
    protected static final int[] PRESSED_FOCUSED_SELECTED_STATE_SET = {STATE_SELECTED, STATE_FOCUSED, STATE_PRESSED};
    protected static final int[] PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_FOCUSED, STATE_PRESSED};
    protected static final int[] PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = {STATE_WINDOW_FOCUSED, STATE_SELECTED, STATE_PRESSED};
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = {STATE_SELECTED, STATE_FOCUSED, STATE_ENABLED, STATE_PRESSED};
    protected static final int[] PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET =
        {STATE_WINDOW_FOCUSED, STATE_FOCUSED, STATE_ENABLED, STATE_PRESSED};
    protected static final int[] PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET =
        {STATE_WINDOW_FOCUSED, STATE_SELECTED, STATE_ENABLED, STATE_PRESSED};
    protected static final int[] PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET =
        {STATE_WINDOW_FOCUSED, STATE_SELECTED, STATE_FOCUSED, STATE_PRESSED};
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET =
        {STATE_WINDOW_FOCUSED, STATE_SELECTED, STATE_FOCUSED, STATE_ENABLED, STATE_PRESSED};

    public final int[] getDrawableState()
    {
        if (mDrawableState != null && !mDrawableStateStale) return mDrawableState;

        mDrawableState = onCreateDrawableState(0);
        mDrawableStateStale = false;

        return mDrawableState;
    }

    protected int[] onCreateDrawableState(int extraSpace)
    {
        if (mDuplicateParentState && mParent instanceof View) return ((View)mParent).onCreateDrawableState(extraSpace);

        int count = 1;

        if (isEnabled()) count++;
        if (mFocused) count++;
        if (mSelected) count++;
        if (mPressed) count++;
        if (mActivated) count++;
        if (mHovered) count++;

        int[] state = new int[count + extraSpace];
        int index = 0;

        state[index++] = STATE_WINDOW_FOCUSED;
        if (isEnabled()) state[index++] = STATE_ENABLED;
        if (mFocused) state[index++] = STATE_FOCUSED;
        if (mSelected) state[index++] = STATE_SELECTED;
        if (mPressed) state[index++] = STATE_PRESSED;
        if (mActivated) state[index++] = STATE_ACTIVATED;
        if (mHovered) state[index++] = STATE_HOVERED;

        return state;
    }

    protected static int[] mergeDrawableStates(int[] baseState, int[] additionalState)
    {
        int last = baseState.length - 1;
        while (last >= 0 && baseState[last] == 0) last--;

        System.arraycopy(additionalState, 0, baseState, last + 1, additionalState.length);
        return baseState;
    }

    public void refreshDrawableState()
    {
        mDrawableStateStale = true;
        drawableStateChanged();

        if (mParent != null) mParent.childDrawableStateChanged(this);
    }

    protected void drawableStateChanged()
    {
        int[] state = getDrawableState();
        boolean changed = false;

        if (mBackground != null && mBackground.isStateful()) changed |= mBackground.setState(state);
        if (mForeground != null && mForeground.isStateful()) changed |= mForeground.setState(state);
        if (mStateListAnimator != null) mStateListAnimator.setState(state);

        if (changed) invalidate();
    }

    public void drawableHotspotChanged(float x, float y)
    {
        if (mBackground != null) mBackground.setHotspot(x, y);
        if (mForeground != null) mForeground.setHotspot(x, y);

        dispatchDrawableHotspotChanged(x, y);
    }

    public void dispatchDrawableHotspotChanged(float x, float y)
    {
    }

    public void jumpDrawablesToCurrentState()
    {
        if (mBackground != null) mBackground.jumpToCurrentState();
        if (mForeground != null) mForeground.jumpToCurrentState();
        if (mStateListAnimator != null) mStateListAnimator.jumpToCurrentState();
    }

    public void setBackgroundDrawable(android.graphics.drawable.Drawable background)
    {
        setBackground(background);
    }

    public boolean isInTouchMode()
    {
        return true;
    }

    public static final int FOCUSABLE_AUTO = 0x00000010;
    public static final int NOT_FOCUSABLE = 0x00000000;
    public static final int FOCUSABLE = 0x00000001;

    public static final int LAYER_TYPE_NONE = 0;
    public static final int LAYER_TYPE_SOFTWARE = 1;
    public static final int LAYER_TYPE_HARDWARE = 2;

    public static final int FOCUS_BACKWARD = 1;
    public static final int FOCUS_FORWARD = 2;
    public static final int FOCUS_LEFT = 17;
    public static final int FOCUS_UP = 33;
    public static final int FOCUS_RIGHT = 66;
    public static final int FOCUS_DOWN = 130;

    private boolean mFocusableInTouchMode;
    private boolean mFocusable;
    private boolean mFocusableAuto = true;
    private boolean mFocused;
    private boolean mSelected;
    private boolean mActivated;
    private int mScrollX;
    private int mScrollY;
    private float mElevation;
    private final android.graphics.Matrix mMatrix = new android.graphics.Matrix();
    private boolean mMatrixStale;
    private float mTranslationX;
    private float mTranslationY;
    private float mTranslationZ;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private float mRotation;
    private float mRotationX;
    private float mRotationY;
    private float mPivotX;
    private float mPivotY;
    private boolean mPivotExplicit;
    private float mAlpha = 1.0f;
    private boolean mAttached;
    private long mUniqueDrawingId;
    private static long sNextDrawingId;
    private java.util.ArrayList<OnAttachStateChangeListener> mAttachListeners;
    private WindowInsetsAnimation.Callback mInsetsAnimationCallback;

    public void setFocusable(int focusable)
    {
        if (focusable == FOCUSABLE_AUTO)
        {
            mFocusableAuto = true;
            mFocusable = isClickable() || mLongClickable;
            return;
        }

        setFocusable(focusable != NOT_FOCUSABLE);
    }

    public void setFocusableInTouchMode(boolean focusable)
    {
        mFocusableInTouchMode = focusable;
        if (focusable) setFocusable(true);
    }

    public boolean isFocusableInTouchMode()
    {
        return mFocusableInTouchMode;
    }

    public void clearFocus()
    {
        if (!mFocused) return;

        ViewRootImpl root = findViewRoot();
        if (root != null && root.getFocusedView() == this) root.setFocusedView(null);
        else onFocusLost();
    }

    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 1;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 2;
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 4;

    public static final int ACCESSIBILITY_LIVE_REGION_NONE = 0;
    public static final int ACCESSIBILITY_LIVE_REGION_POLITE = 1;
    public static final int ACCESSIBILITY_LIVE_REGION_ASSERTIVE = 2;

    private int mImportantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_AUTO;
    private int mAccessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_NONE;
    private CharSequence mStateDescription;

    public int getImportantForAccessibility()
    {
        return mImportantForAccessibility;
    }

    public boolean isImportantForAccessibility()
    {
        return mImportantForAccessibility != IMPORTANT_FOR_ACCESSIBILITY_NO;
    }

    public void setAccessibilityLiveRegion(int mode)
    {
        mAccessibilityLiveRegion = mode;
    }

    public int getAccessibilityLiveRegion()
    {
        return mAccessibilityLiveRegion;
    }

    public void setStateDescription(CharSequence description)
    {
        mStateDescription = description;
    }

    public CharSequence getStateDescription()
    {
        return mStateDescription;
    }

    public void setAccessibilityPaneTitle(CharSequence title)
    {
        mAccessibilityPaneTitle = title;
    }

    public CharSequence getAccessibilityPaneTitle()
    {
        return mAccessibilityPaneTitle;
    }

    public void setScreenReaderFocusable(boolean focusable)
    {
        mScreenReaderFocusable = focusable;
    }

    public boolean isScreenReaderFocusable()
    {
        return mScreenReaderFocusable;
    }

    public void setAccessibilityHeading(boolean isHeading)
    {
        mAccessibilityHeading = isHeading;
    }

    public boolean isAccessibilityHeading()
    {
        return mAccessibilityHeading;
    }

    public void setForceDarkAllowed(boolean allowed)
    {
        mForceDarkAllowed = allowed;
    }

    public boolean isForceDarkAllowed()
    {
        return mForceDarkAllowed;
    }

    public void setTooltipText(CharSequence text)
    {
        mTooltipText = text;
        if (text != null && text.length() > 0) mLongClickable = true;
    }

    public CharSequence getTooltipText()
    {
        return mTooltipText;
    }

    public void setNestedScrollingEnabled(boolean enabled)
    {
        if (!enabled && mNestedScrollingEnabled) stopNestedScroll();
        mNestedScrollingEnabled = enabled;
    }

    public boolean isNestedScrollingEnabled()
    {
        return mNestedScrollingEnabled;
    }

    public void setHorizontalScrollBarEnabled(boolean enabled)
    {
        mHorizontalScrollBarEnabled = enabled;
    }

    public boolean isHorizontalScrollBarEnabled()
    {
        return mHorizontalScrollBarEnabled;
    }

    public void setVerticalScrollBarEnabled(boolean enabled)
    {
        mVerticalScrollBarEnabled = enabled;
    }

    public boolean isVerticalScrollBarEnabled()
    {
        return mVerticalScrollBarEnabled;
    }

    public void setVerticalScrollbarPosition(int position)
    {
        mVerticalScrollbarPosition = position;
    }

    public int getVerticalScrollbarPosition()
    {
        return mVerticalScrollbarPosition;
    }

    public void setScrollIndicators(int indicators)
    {
        setScrollIndicators(indicators, SCROLL_INDICATORS_ALL);
    }

    public void setScrollIndicators(int indicators, int mask)
    {
        mScrollIndicators = (mScrollIndicators & ~mask) | (indicators & mask);
        invalidate();
    }

    public int getScrollIndicators()
    {
        return mScrollIndicators;
    }

    public void setScrollContainer(boolean isScrollContainer)
    {
        mScrollContainer = isScrollContainer;
    }

    public boolean isScrollContainer()
    {
        return mScrollContainer;
    }

    public void setSaveEnabled(boolean enabled)
    {
        mSaveEnabled = enabled;
    }

    public boolean isSaveEnabled()
    {
        return mSaveEnabled;
    }

    public void setDuplicateParentStateEnabled(boolean enabled)
    {
        mDuplicateParentState = enabled;
    }

    public boolean isDuplicateParentStateEnabled()
    {
        return mDuplicateParentState;
    }

    public void setHasTransientState(boolean hasTransientState)
    {
        int oldCount = mTransientStateCount;
        mTransientStateCount = hasTransientState ? mTransientStateCount + 1 : Math.max(0, mTransientStateCount - 1);

        boolean changed = (oldCount == 0) != (mTransientStateCount == 0);
        if (changed && mParent != null) mParent.childHasTransientStateChanged(this, mTransientStateCount > 0);
    }

    public boolean hasTransientState()
    {
        return mTransientStateCount > 0;
    }

    public void cancelPendingInputEvents()
    {
        cancelLongPress();
    }

    public void setLabelFor(int id)
    {
        mLabelForId = id;
    }

    public int getLabelFor()
    {
        return mLabelForId;
    }

    public void setAccessibilityTraversalBefore(int beforeId)
    {
        mAccessibilityTraversalBeforeId = beforeId;
    }

    public int getAccessibilityTraversalBefore()
    {
        return mAccessibilityTraversalBeforeId;
    }

    public void setAccessibilityTraversalAfter(int afterId)
    {
        mAccessibilityTraversalAfterId = afterId;
    }

    public int getAccessibilityTraversalAfter()
    {
        return mAccessibilityTraversalAfterId;
    }

    public void setNextFocusForwardId(int id)
    {
        mNextFocusForwardId = id;
    }

    public int getNextFocusForwardId()
    {
        return mNextFocusForwardId;
    }

    public void setNextFocusLeftId(int id)
    {
        mNextFocusLeftId = id;
    }

    public int getNextFocusLeftId()
    {
        return mNextFocusLeftId;
    }

    public void setNextFocusRightId(int id)
    {
        mNextFocusRightId = id;
    }

    public int getNextFocusRightId()
    {
        return mNextFocusRightId;
    }

    public void setNextFocusUpId(int id)
    {
        mNextFocusUpId = id;
    }

    public int getNextFocusUpId()
    {
        return mNextFocusUpId;
    }

    public void setNextFocusDownId(int id)
    {
        mNextFocusDownId = id;
    }

    public int getNextFocusDownId()
    {
        return mNextFocusDownId;
    }

    public void setNextClusterForwardId(int id)
    {
        mNextClusterForwardId = id;
    }

    public int getNextClusterForwardId()
    {
        return mNextClusterForwardId;
    }

    int getNextFocusId(int direction)
    {
        switch (direction)
        {
            case FOCUS_LEFT:
                return mNextFocusLeftId;
            case FOCUS_RIGHT:
                return mNextFocusRightId;
            case FOCUS_UP:
                return mNextFocusUpId;
            case FOCUS_DOWN:
                return mNextFocusDownId;
            case FOCUS_FORWARD:
                return mNextFocusForwardId;
            default:
                return NO_ID;
        }
    }

    public void setTransitionName(String name)
    {
        mTransitionName = name;
    }

    public String getTransitionName()
    {
        return mTransitionName;
    }

    public void setTransitionVisibility(int visibility)
    {
        mVisibility = visibility;
        invalidate();
    }

    public void setTransitionAlpha(float alpha)
    {
        if (mTransitionAlpha == alpha) return;

        mTransitionAlpha = alpha;
        invalidate();
    }

    public float getTransitionAlpha()
    {
        return mTransitionAlpha;
    }

    public void setKeepScreenOn(boolean keepOn)
    {
        mKeepScreenOn = keepOn;
    }

    public boolean getKeepScreenOn()
    {
        return mKeepScreenOn;
    }

    public void setDefaultFocusHighlightEnabled(boolean enabled)
    {
        mDefaultFocusHighlightEnabled = enabled;
    }

    public boolean getDefaultFocusHighlightEnabled()
    {
        return mDefaultFocusHighlightEnabled;
    }

    public void setFocusedByDefault(boolean focused)
    {
        mFocusedByDefault = focused;
    }

    public final boolean isFocusedByDefault()
    {
        return mFocusedByDefault;
    }

    public void setKeyboardNavigationCluster(boolean cluster)
    {
        mKeyboardNavigationCluster = cluster;
    }

    public final boolean isKeyboardNavigationCluster()
    {
        return mKeyboardNavigationCluster;
    }

    public void setRevealOnFocusHint(boolean reveal)
    {
        mRevealOnFocusHint = reveal;
    }

    public final boolean getRevealOnFocusHint()
    {
        return mRevealOnFocusHint;
    }

    public android.graphics.Matrix getMatrix()
    {
        if (!mMatrixStale) return mMatrix;

        mMatrixStale = false;
        mMatrix.reset();

        float pivotX = getPivotX();
        float pivotY = getPivotY();

        mMatrix.postTranslate(-pivotX, -pivotY);
        if (mScaleX != 1 || mScaleY != 1) mMatrix.postScale(mScaleX, mScaleY);
        if (mRotation != 0) mMatrix.postRotate(mRotation);
        mMatrix.postTranslate(pivotX + mTranslationX, pivotY + mTranslationY);

        return mMatrix;
    }

    private int mLayerType = LAYER_TYPE_NONE;
    private android.graphics.Paint mLayerPaint;
    private android.graphics.RenderEffect mRenderEffect;

    public void setLayerType(int layerType, android.graphics.Paint paint)
    {
        mLayerType = layerType;
        mLayerPaint = paint;
        invalidate();
    }

    public int getLayerType()
    {
        return mLayerType;
    }

    public void setLayerPaint(android.graphics.Paint paint)
    {
        mLayerPaint = paint;
        invalidate();
    }

    public void setRenderEffect(android.graphics.RenderEffect renderEffect)
    {
        if (mRenderEffect == renderEffect) return;

        mRenderEffect = renderEffect;
        invalidate();
    }

    public float getElevation()
    {
        return mElevation;
    }

    public boolean getGlobalVisibleRect(android.graphics.Rect rect)
    {
        return getGlobalVisibleRect(rect, null);
    }

    public boolean getGlobalVisibleRect(android.graphics.Rect rect, android.graphics.Point offset)
    {
        int width = mRight - mLeft;
        int height = mBottom - mTop;

        if (width <= 0 || height <= 0) return false;

        rect.set(0, 0, width, height);
        if (offset != null) offset.set(-mScrollX, -mScrollY);

        return mParent == null || mParent.getChildVisibleRect(this, rect, offset);
    }

    public boolean getLocalVisibleRect(android.graphics.Rect rect)
    {
        android.graphics.Point offset = new android.graphics.Point();

        if (!getGlobalVisibleRect(rect, offset)) return false;

        rect.offset(-offset.x, -offset.y);
        return true;
    }

    public void getWindowVisibleDisplayFrame(android.graphics.Rect frame)
    {
        if (frame == null) return;

        ViewRootImpl root = findViewRoot();
        if (root == null)
        {
            android.util.DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            frame.set(0, 0, metrics.widthPixels, metrics.heightPixels);
            return;
        }

        frame.set(0, 0, root.getWindowWidth(), root.getWindowHeight());

        WindowInsets insets = root.getWindowInsets();
        if (insets == null) return;

        frame.left += insets.getSystemWindowInsetLeft();
        frame.top += insets.getSystemWindowInsetTop();
        frame.right -= insets.getSystemWindowInsetRight();
        frame.bottom -= insets.getSystemWindowInsetBottom();
    }

    public void getWindowDisplayFrame(android.graphics.Rect frame)
    {
        ViewRootImpl root = findViewRoot();
        if (root != null) frame.set(0, 0, root.getWindowWidth(), root.getWindowHeight());
        else getWindowVisibleDisplayFrame(frame);
    }

    public void getBoundsOnScreen(Rect outRect)
    {
        getBoundsOnScreen(outRect, false);
    }

    public void getBoundsOnScreen(Rect outRect, boolean clipToParent)
    {
        int[] location = new int[2];
        getLocationOnScreen(location);
        outRect.set(location[0], location[1], location[0] + getWidth(), location[1] + getHeight());
    }

    public Display getDisplay()
    {
        return null;
    }

    public boolean performHapticFeedback(int feedbackConstant)
    {
        return performHapticFeedback(feedbackConstant, 0);
    }

    public boolean performHapticFeedback(int feedbackConstant, int flags)
    {
        if (feedbackConstant == HapticFeedbackConstants.NO_HAPTICS) return false;
        if ((flags & HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING) == 0 && !mHapticFeedbackEnabled) return false;

        return mAttached;
    }

    public void playSoundEffect(int soundConstant)
    {
    }

    public void setSoundEffectsEnabled(boolean soundEffectsEnabled)
    {
        mSoundEffectsEnabled = soundEffectsEnabled;
    }

    public boolean isSoundEffectsEnabled()
    {
        return mSoundEffectsEnabled;
    }

    public int getScrollX()
    {
        return mScrollX;
    }

    public int getScrollY()
    {
        return mScrollY;
    }

    public void setScrollX(int value)
    {
        scrollTo(value, mScrollY);
    }

    public void setScrollY(int value)
    {
        scrollTo(mScrollX, value);
    }

    protected void onScrollChanged(int left, int top, int oldLeft, int oldTop)
    {
        if (mScrollChangeListener != null) mScrollChangeListener.onScrollChange(this, left, top, oldLeft, oldTop);

        if (mAttached)
        {
            ViewTreeObserver observer = getRootView().mViewTreeObserver;
            if (observer != null) observer.dispatchOnScrollChanged();
        }
    }

    public void computeScroll()
    {
    }

    public static final android.util.Property<View, Float> ALPHA = new android.util.FloatProperty<View>("alpha")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setAlpha(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getAlpha());
        }
    };

    public static final android.util.Property<View, Float> TRANSLATION_X = new android.util.FloatProperty<View>("translationX")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setTranslationX(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getTranslationX());
        }
    };

    public static final android.util.Property<View, Float> TRANSLATION_Y = new android.util.FloatProperty<View>("translationY")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setTranslationY(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getTranslationY());
        }
    };

    public static final android.util.Property<View, Float> TRANSLATION_Z = new android.util.FloatProperty<View>("translationZ")
    {
        @Override
        public void setValue(View view, float value)
        {
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(0.0f);
        }
    };

    public static final android.util.Property<View, Float> SCALE_X = new android.util.FloatProperty<View>("scaleX")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setScaleX(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getScaleX());
        }
    };

    public static final android.util.Property<View, Float> SCALE_Y = new android.util.FloatProperty<View>("scaleY")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setScaleY(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getScaleY());
        }
    };

    public static final android.util.Property<View, Float> ROTATION = new android.util.FloatProperty<View>("rotation")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setRotation(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getRotation());
        }
    };

    public static final android.util.Property<View, Float> ROTATION_X = new android.util.FloatProperty<View>("rotationX")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setRotationX(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getRotationX());
        }
    };

    public static final android.util.Property<View, Float> ROTATION_Y = new android.util.FloatProperty<View>("rotationY")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setRotationY(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getRotationY());
        }
    };

    public static final android.util.Property<View, Float> X = new android.util.FloatProperty<View>("x")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setX(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getX());
        }
    };

    public static final android.util.Property<View, Float> Y = new android.util.FloatProperty<View>("y")
    {
        @Override
        public void setValue(View view, float value)
        {
            view.setY(value);
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(view.getY());
        }
    };

    public static final android.util.Property<View, Float> Z = new android.util.FloatProperty<View>("z")
    {
        @Override
        public void setValue(View view, float value)
        {
        }

        @Override
        public Float get(View view)
        {
            return Float.valueOf(0.0f);
        }
    };

    public static final int OVER_SCROLL_ALWAYS = 0;
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;
    public static final int OVER_SCROLL_NEVER = 2;

    private java.util.List<Rect> mSystemGestureExclusionRects = new java.util.ArrayList<>();

    public void setSystemGestureExclusionRects(java.util.List<Rect> rects)
    {
        mSystemGestureExclusionRects = rects == null ? new java.util.ArrayList<Rect>() : rects;
    }

    public java.util.List<Rect> getSystemGestureExclusionRects()
    {
        return mSystemGestureExclusionRects;
    }

    protected boolean awakenScrollBars()
    {
        return false;
    }

    protected boolean awakenScrollBars(int startDelay)
    {
        return false;
    }

    protected boolean awakenScrollBars(int startDelay, boolean invalidate)
    {
        return false;
    }

    public void setScrollbarFadingEnabled(boolean fadeScrollbars)
    {
    }

    public boolean isScrollbarFadingEnabled()
    {
        return false;
    }

    private Rect mClipBounds;

    public void setClipBounds(Rect clipBounds)
    {
        mClipBounds = clipBounds == null ? null : new Rect(clipBounds);
        invalidate();
    }

    public Rect getClipBounds()
    {
        return mClipBounds == null ? null : new Rect(mClipBounds);
    }

    public boolean getClipBounds(Rect outRect)
    {
        if (outRect == null || mClipBounds == null) return false;

        outRect.set(mClipBounds);
        return true;
    }

    private boolean mVerticalFadingEdge;
    private boolean mHorizontalFadingEdge;
    private int mFadingEdgeLength;

    public void setVerticalFadingEdgeEnabled(boolean enabled)
    {
        mVerticalFadingEdge = enabled;
    }

    public boolean isVerticalFadingEdgeEnabled()
    {
        return mVerticalFadingEdge;
    }

    public void setHorizontalFadingEdgeEnabled(boolean enabled)
    {
        mHorizontalFadingEdge = enabled;
    }

    public boolean isHorizontalFadingEdgeEnabled()
    {
        return mHorizontalFadingEdge;
    }

    public void setFadingEdgeLength(int length)
    {
        mFadingEdgeLength = length;
    }

    public int getVerticalFadingEdgeLength()
    {
        return mVerticalFadingEdge ? mFadingEdgeLength : 0;
    }

    public int getHorizontalFadingEdgeLength()
    {
        return mHorizontalFadingEdge ? mFadingEdgeLength : 0;
    }

    protected float getTopFadingEdgeStrength()
    {
        return 1.0f;
    }

    protected float getBottomFadingEdgeStrength()
    {
        return 1.0f;
    }

    protected float getLeftFadingEdgeStrength()
    {
        return 1.0f;
    }

    protected float getRightFadingEdgeStrength()
    {
        return 1.0f;
    }

    protected int computeHorizontalScrollRange()
    {
        return getWidth();
    }

    protected int computeHorizontalScrollOffset()
    {
        return mScrollX;
    }

    protected int computeHorizontalScrollExtent()
    {
        return getWidth();
    }

    protected int computeVerticalScrollRange()
    {
        return getHeight();
    }

    protected int computeVerticalScrollOffset()
    {
        return mScrollY;
    }

    protected int computeVerticalScrollExtent()
    {
        return getHeight();
    }

    public boolean canScrollHorizontally(int direction)
    {
        int offset = computeHorizontalScrollOffset();
        int range = computeHorizontalScrollRange() - computeHorizontalScrollExtent();

        if (range == 0) return false;

        return direction < 0 ? offset > 0 : offset < range - 1;
    }

    public boolean canScrollVertically(int direction)
    {
        int offset = computeVerticalScrollOffset();
        int range = computeVerticalScrollRange() - computeVerticalScrollExtent();

        if (range == 0) return false;

        return direction < 0 ? offset > 0 : offset < range - 1;
    }

    public void getHitRect(Rect outRect)
    {
        if (outRect == null) return;

        outRect.set(mLeft, mTop, mRight, mBottom);
    }

    private boolean mHapticFeedbackEnabled = true;

    public void setHapticFeedbackEnabled(boolean enabled)
    {
        mHapticFeedbackEnabled = enabled;
    }

    public boolean isHapticFeedbackEnabled()
    {
        return mHapticFeedbackEnabled;
    }

    public ViewPropertyAnimator animate()
    {
        if (mPropertyAnimator == null) mPropertyAnimator = new ViewPropertyAnimator(this);

        return mPropertyAnimator;
    }

    public float getX()
    {
        return mLeft + mTranslationX;
    }

    public float getY()
    {
        return mTop + mTranslationY;
    }

    public void setX(float x)
    {
        setTranslationX(x - mLeft);
    }

    public void setY(float y)
    {
        setTranslationY(y - mTop);
    }

    public void scrollTo(int x, int y)
    {
        if (mScrollX == x && mScrollY == y) return;

        int oldX = mScrollX;
        int oldY = mScrollY;
        mScrollX = x;
        mScrollY = y;
        onScrollChanged(mScrollX, mScrollY, oldX, oldY);
        if (!awakenScrollBars()) invalidate();
    }

    public void scrollBy(int x, int y)
    {
        scrollTo(mScrollX + x, mScrollY + y);
    }

    public void setPivotX(float pivotX)
    {
        mPivotX = pivotX;
        mPivotExplicit = true;
        mMatrixStale = true;
    }

    public float getPivotX()
    {
        return mPivotExplicit ? mPivotX : getWidth() / 2.0f;
    }

    public void setPivotY(float pivotY)
    {
        mPivotY = pivotY;
        mPivotExplicit = true;
        mMatrixStale = true;
    }

    public float getPivotY()
    {
        return mPivotExplicit ? mPivotY : getHeight() / 2.0f;
    }

    public void setRotation(float rotation)
    {
        if (mRotation == rotation) return;

        mRotation = rotation;
        mMatrixStale = true;
        invalidate();
    }

    public float getRotation()
    {
        return mRotation;
    }

    public void setRotationX(float rotation)
    {
        mRotationX = rotation;
    }

    public float getRotationX()
    {
        return mRotationX;
    }

    public void setRotationY(float rotation)
    {
        mRotationY = rotation;
    }

    public float getRotationY()
    {
        return mRotationY;
    }

    public void setScaleX(float scale)
    {
        if (mScaleX == scale) return;

        mScaleX = scale;
        mMatrixStale = true;
        invalidate();
    }

    public float getScaleX()
    {
        return mScaleX;
    }

    public void setScaleY(float scale)
    {
        if (mScaleY == scale) return;

        mScaleY = scale;
        mMatrixStale = true;
        invalidate();
    }

    public float getScaleY()
    {
        return mScaleY;
    }

    public void setTranslationX(float translation)
    {
        if (mTranslationX == translation) return;

        mTranslationX = translation;
        mMatrixStale = true;
        invalidate();
    }

    public float getTranslationX()
    {
        return mTranslationX;
    }

    public void setTranslationY(float translation)
    {
        if (mTranslationY == translation) return;

        mTranslationY = translation;
        mMatrixStale = true;
        invalidate();
    }

    public float getTranslationY()
    {
        return mTranslationY;
    }

    public void setTranslationZ(float translation)
    {
        if (mTranslationZ == translation) return;

        mTranslationZ = translation;
        invalidate();
    }

    public float getZ()
    {
        return mElevation + mTranslationZ;
    }

    public void setZ(float z)
    {
        setTranslationZ(z - mElevation);
    }

    public float getTranslationZ()
    {
        return mTranslationZ;
    }

    public void setWindowInsetsAnimationCallback(WindowInsetsAnimation.Callback callback)
    {
        mInsetsAnimationCallback = callback;
    }

    public WindowInsetsAnimation.Callback getWindowInsetsAnimationCallback()
    {
        return mInsetsAnimationCallback;
    }

    public void dispatchWindowInsetsAnimationPrepare(WindowInsetsAnimation animation)
    {
        if (mInsetsAnimationCallback != null) mInsetsAnimationCallback.onPrepare(animation);
    }

    public WindowInsetsAnimation.Bounds dispatchWindowInsetsAnimationStart(WindowInsetsAnimation animation,
                                                                          WindowInsetsAnimation.Bounds bounds)
    {
        if (mInsetsAnimationCallback != null) return mInsetsAnimationCallback.onStart(animation, bounds);

        return bounds;
    }

    public WindowInsets dispatchWindowInsetsAnimationProgress(WindowInsets insets, java.util.List<WindowInsetsAnimation> runningAnimations)
    {
        if (mInsetsAnimationCallback != null) return mInsetsAnimationCallback.onProgress(insets, runningAnimations);

        return insets;
    }

    public void dispatchWindowInsetsAnimationEnd(WindowInsetsAnimation animation)
    {
        if (mInsetsAnimationCallback != null) mInsetsAnimationCallback.onEnd(animation);
    }

    public static final float REQUESTED_FRAME_RATE_CATEGORY_DEFAULT = Float.NaN;
    public static final float REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE = -1.0f;
    public static final float REQUESTED_FRAME_RATE_CATEGORY_LOW = -2.0f;
    public static final float REQUESTED_FRAME_RATE_CATEGORY_NORMAL = -3.0f;
    public static final float REQUESTED_FRAME_RATE_CATEGORY_HIGH = -4.0f;

    public static final int CONTENT_SENSITIVITY_AUTO = 0;
    public static final int CONTENT_SENSITIVITY_SENSITIVE = 1;
    public static final int CONTENT_SENSITIVITY_NOT_SENSITIVE = 2;

    public static final int DRAG_FLAG_GLOBAL_SAME_APPLICATION = 1 << 12;
    public static final int DRAG_FLAG_START_INTENT_SENDER_ON_UNHANDLED_DRAG = 1 << 13;
    public static final int DRAG_FLAG_HIDE_CALLING_TASK_ON_DRAG_START = 1 << 14;

    private int mScrollBarSize = -1;
    private int mScrollBarFadeDuration = ViewConfiguration.getScrollBarFadeDuration();
    private int mScrollBarDefaultDelayBeforeFade = ViewConfiguration.getScrollDefaultDelay();
    private android.graphics.drawable.Drawable mVerticalScrollbarThumb;
    private android.graphics.drawable.Drawable mVerticalScrollbarTrack;
    private android.graphics.drawable.Drawable mHorizontalScrollbarThumb;
    private android.graphics.drawable.Drawable mHorizontalScrollbarTrack;
    private boolean mPreferKeepClear;
    private java.util.List<Rect> mPreferKeepClearRects = new ArrayList<>();
    private boolean mAllowClickWhenDisabled;
    private int mContentSensitivity = CONTENT_SENSITIVITY_AUTO;
    private CharSequence mSupplementalDescription;
    private boolean mIsCredential;
    private android.graphics.BlendMode mForegroundTintBlendMode;

    protected ContextMenu.ContextMenuInfo getContextMenuInfo()
    {
        return null;
    }

    protected boolean dispatchGenericPointerEvent(MotionEvent event)
    {
        return false;
    }

    protected boolean dispatchGenericFocusedEvent(MotionEvent event)
    {
        return false;
    }

    protected boolean isPaddingOffsetRequired()
    {
        return false;
    }

    protected int getLeftPaddingOffset()
    {
        return 0;
    }

    protected int getRightPaddingOffset()
    {
        return 0;
    }

    protected int getTopPaddingOffset()
    {
        return 0;
    }

    protected int getBottomPaddingOffset()
    {
        return 0;
    }

    public int getScrollBarSize()
    {
        return mScrollBarSize < 0 ? ViewConfiguration.get(mContext).getScaledScrollBarSize() : mScrollBarSize;
    }

    public void setScrollBarSize(int scrollBarSize)
    {
        mScrollBarSize = scrollBarSize;
    }

    protected int getHorizontalScrollbarHeight()
    {
        return mHorizontalScrollbarThumb != null ? Math.max(getScrollBarSize(), mHorizontalScrollbarThumb.getIntrinsicHeight())
                                                 : getScrollBarSize();
    }

    public int getVerticalScrollbarWidth()
    {
        return mVerticalScrollbarThumb != null ? Math.max(getScrollBarSize(), mVerticalScrollbarThumb.getIntrinsicWidth())
                                               : getScrollBarSize();
    }

    public int getScrollBarFadeDuration()
    {
        return mScrollBarFadeDuration;
    }

    public void setScrollBarFadeDuration(int scrollBarFadeDuration)
    {
        mScrollBarFadeDuration = scrollBarFadeDuration;
    }

    public int getScrollBarDefaultDelayBeforeFade()
    {
        return mScrollBarDefaultDelayBeforeFade;
    }

    public void setScrollBarDefaultDelayBeforeFade(int scrollBarDefaultDelayBeforeFade)
    {
        mScrollBarDefaultDelayBeforeFade = scrollBarDefaultDelayBeforeFade;
    }

    public void setVerticalScrollbarThumbDrawable(android.graphics.drawable.Drawable drawable)
    {
        mVerticalScrollbarThumb = drawable;
    }

    public android.graphics.drawable.Drawable getVerticalScrollbarThumbDrawable()
    {
        return mVerticalScrollbarThumb;
    }

    public void setVerticalScrollbarTrackDrawable(android.graphics.drawable.Drawable drawable)
    {
        mVerticalScrollbarTrack = drawable;
    }

    public android.graphics.drawable.Drawable getVerticalScrollbarTrackDrawable()
    {
        return mVerticalScrollbarTrack;
    }

    public void setHorizontalScrollbarThumbDrawable(android.graphics.drawable.Drawable drawable)
    {
        mHorizontalScrollbarThumb = drawable;
    }

    public android.graphics.drawable.Drawable getHorizontalScrollbarThumbDrawable()
    {
        return mHorizontalScrollbarThumb;
    }

    public void setHorizontalScrollbarTrackDrawable(android.graphics.drawable.Drawable drawable)
    {
        mHorizontalScrollbarTrack = drawable;
    }

    public android.graphics.drawable.Drawable getHorizontalScrollbarTrackDrawable()
    {
        return mHorizontalScrollbarTrack;
    }

    protected final void onDrawScrollBars(Canvas canvas)
    {
        if (mVerticalScrollBarEnabled && mVerticalScrollbarThumb != null)
        {
            int range = computeVerticalScrollRange();
            int extent = computeVerticalScrollExtent();

            if (range > extent && extent > 0)
            {
                int size = getVerticalScrollbarWidth();
                int height = getHeight();
                int length = Math.max(size * 2, height * extent / range);
                int offset = (int)((long)(height - length) * computeVerticalScrollOffset() / Math.max(1, range - extent));
                int left = mScrollX + getWidth() - size;
                int top = mScrollY + offset;

                if (mVerticalScrollbarTrack != null)
                {
                    mVerticalScrollbarTrack.setBounds(left, mScrollY, left + size, mScrollY + height);
                    mVerticalScrollbarTrack.draw(canvas);
                }

                mVerticalScrollbarThumb.setBounds(left, top, left + size, top + length);
                mVerticalScrollbarThumb.draw(canvas);
            }
        }

        if (mHorizontalScrollBarEnabled && mHorizontalScrollbarThumb != null)
        {
            int range = computeHorizontalScrollRange();
            int extent = computeHorizontalScrollExtent();

            if (range > extent && extent > 0)
            {
                int size = getHorizontalScrollbarHeight();
                int width = getWidth();
                int length = Math.max(size * 2, width * extent / range);
                int offset = (int)((long)(width - length) * computeHorizontalScrollOffset() / Math.max(1, range - extent));
                int top = mScrollY + getHeight() - size;
                int left = mScrollX + offset;

                if (mHorizontalScrollbarTrack != null)
                {
                    mHorizontalScrollbarTrack.setBounds(mScrollX, top, mScrollX + width, top + size);
                    mHorizontalScrollbarTrack.draw(canvas);
                }

                mHorizontalScrollbarThumb.setBounds(left, top, left + length, top + size);
                mHorizontalScrollbarThumb.draw(canvas);
            }
        }
    }

    public boolean hasOnLongClickListeners()
    {
        return mLongClickListener != null;
    }

    public boolean performLongClick(float x, float y)
    {
        return performLongClick();
    }

    public boolean checkInputConnectionProxy(View view)
    {
        return false;
    }

    public boolean dispatchNestedPrePerformAccessibilityAction(int action, Bundle arguments)
    {
        ViewParent parent = mParent;

        while (parent instanceof View)
        {
            if (parent.onNestedPrePerformAccessibilityAction(this, action, arguments)) return true;
            parent = parent.getParent();
        }

        return false;
    }

    public boolean gatherTransparentRegion(android.graphics.Region region)
    {
        return true;
    }

    public void setPreferKeepClear(boolean preferKeepClear)
    {
        mPreferKeepClear = preferKeepClear;
    }

    public boolean isPreferKeepClear()
    {
        return mPreferKeepClear;
    }

    public void setPreferKeepClearRects(java.util.List<Rect> rects)
    {
        mPreferKeepClearRects = rects == null ? new ArrayList<Rect>() : new ArrayList<>(rects);
    }

    public java.util.List<Rect> getPreferKeepClearRects()
    {
        return new ArrayList<>(mPreferKeepClearRects);
    }

    public void setAllowClickWhenDisabled(boolean clickableWhenDisabled)
    {
        mAllowClickWhenDisabled = clickableWhenDisabled;
    }

    public void onCancelPendingInputEvents()
    {
        cancelLongPress();
    }

    public void requestUnbufferedDispatch(MotionEvent event)
    {
    }

    public void requestUnbufferedDispatch(int source)
    {
    }

    public void setContentSensitivity(int mode)
    {
        mContentSensitivity = mode;
    }

    public int getContentSensitivity()
    {
        return mContentSensitivity;
    }

    public boolean isContentSensitive()
    {
        return mContentSensitivity == CONTENT_SENSITIVITY_SENSITIVE;
    }

    public int getSourceLayoutResId()
    {
        return 0;
    }

    public void setSupplementalDescription(CharSequence supplementalDescription)
    {
        mSupplementalDescription = supplementalDescription;
    }

    public CharSequence getSupplementalDescription()
    {
        return mSupplementalDescription;
    }

    public void setIsCredential(boolean isCredential)
    {
        mIsCredential = isCredential;
    }

    public boolean isCredential()
    {
        return mIsCredential;
    }

    public void setForegroundTintBlendMode(android.graphics.BlendMode blendMode)
    {
        mForegroundTintBlendMode = blendMode;

        android.graphics.drawable.Drawable foreground = getForeground();
        if (foreground != null && blendMode != null) foreground.setTintBlendMode(blendMode);
    }

    public android.graphics.BlendMode getForegroundTintBlendMode()
    {
        return mForegroundTintBlendMode;
    }

    public void setViewTranslationCallback(android.view.translation.ViewTranslationCallback callback)
    {
        mViewTranslationCallback = callback;
    }

    public void clearViewTranslationCallback()
    {
        mViewTranslationCallback = null;
    }

    public android.view.translation.ViewTranslationCallback getViewTranslationCallback()
    {
        return mViewTranslationCallback;
    }

    public void onCreateViewTranslationRequest(int[] supportedFormats,
                                               java.util.function.Consumer<ViewTranslationRequest> requestsCollector)
    {
    }

    public void onCreateVirtualViewTranslationRequests(long[] virtualIds, int[] supportedFormats,
                                                       java.util.function.Consumer<ViewTranslationRequest> requestsCollector)
    {
    }

    public void onViewTranslationResponse(android.view.translation.ViewTranslationResponse response)
    {
        mViewTranslationResponse = response;
    }

    public void clearViewTranslationResponse()
    {
        mViewTranslationResponse = null;
    }

    public android.view.translation.ViewTranslationResponse getViewTranslationResponse()
    {
        return mViewTranslationResponse;
    }

    public void onVirtualViewTranslationResponses(android.util.LongSparseArray<android.view.translation.ViewTranslationResponse> response)
    {
    }

    private android.view.translation.ViewTranslationCallback mViewTranslationCallback;
    private android.view.translation.ViewTranslationResponse mViewTranslationResponse;
    private ScrollCaptureCallback mScrollCaptureCallback;

    public void setScrollCaptureCallback(ScrollCaptureCallback callback)
    {
        mScrollCaptureCallback = callback;
    }

    public void onScrollCaptureSearch(Rect localVisibleRect, android.graphics.Point windowOffset,
                                      java.util.function.Consumer<ScrollCaptureTarget> targets)
    {
        if (mScrollCaptureHint == SCROLL_CAPTURE_HINT_EXCLUDE || mScrollCaptureCallback == null) return;

        android.graphics.Point offset = new android.graphics.Point(windowOffset.x, windowOffset.y);
        targets.accept(new ScrollCaptureTarget(this, new Rect(localVisibleRect), offset, mScrollCaptureCallback));
    }

    public void dispatchScrollCaptureSearch(Rect localVisibleRect, android.graphics.Point windowOffset,
                                            java.util.function.Consumer<ScrollCaptureTarget> targets)
    {
        onScrollCaptureSearch(localVisibleRect, windowOffset, targets);
    }

    public boolean isHardwareAccelerated()
    {
        return false;
    }

    public void invalidateDrawable(android.graphics.drawable.Drawable who)
    {
        invalidate();
    }

    public void scheduleDrawable(android.graphics.drawable.Drawable who, Runnable what, long when)
    {
        if (what != null) postDelayed(what, Math.max(0, when - android.os.SystemClock.uptimeMillis()));
    }

    public void unscheduleDrawable(android.graphics.drawable.Drawable who, Runnable what)
    {
        if (what != null) removeCallbacks(what);
    }

    public void unscheduleDrawable(android.graphics.drawable.Drawable who)
    {
    }

    protected boolean verifyDrawable(android.graphics.drawable.Drawable who)
    {
        return who != null && (who == mBackground || who == mForeground);
    }

    public boolean isLayoutRtl()
    {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    public void setAutoHandwritingEnabled(boolean enabled)
    {
        mAutoHandwritingEnabled = enabled;
    }

    public boolean isAutoHandwritingEnabled()
    {
        return mAutoHandwritingEnabled;
    }

    public void setIsHandwritingDelegate(boolean isHandwritingDelegate)
    {
        mHandwritingDelegate = isHandwritingDelegate;
    }

    public boolean isHandwritingDelegate()
    {
        return mHandwritingDelegate;
    }

    public void setHandwritingDelegateFlags(int flags)
    {
        mHandwritingDelegateFlags = flags;
    }

    public int getHandwritingDelegateFlags()
    {
        return mHandwritingDelegateFlags;
    }

    public void setHandwritingDelegatorCallback(Runnable callback)
    {
        mHandwritingDelegatorCallback = callback;
    }

    public Runnable getHandwritingDelegatorCallback()
    {
        return mHandwritingDelegatorCallback;
    }

    public void setAllowedHandwritingDelegatePackage(String allowedPackageName)
    {
        mAllowedHandwritingDelegatePackage = allowedPackageName;
    }

    public String getAllowedHandwritingDelegatePackageName()
    {
        return mAllowedHandwritingDelegatePackage;
    }

    public void setAllowedHandwritingDelegatorPackage(String allowedPackageName)
    {
        mAllowedHandwritingDelegatorPackage = allowedPackageName;
    }

    public String getAllowedHandwritingDelegatorPackageName()
    {
        return mAllowedHandwritingDelegatorPackage;
    }

    public void setHandwritingBoundsOffsets(float offsetLeft, float offsetTop, float offsetRight, float offsetBottom)
    {
        mHandwritingOffsetLeft = offsetLeft;
        mHandwritingOffsetTop = offsetTop;
        mHandwritingOffsetRight = offsetRight;
        mHandwritingOffsetBottom = offsetBottom;
    }

    public float getHandwritingBoundsOffsetLeft()
    {
        return mHandwritingOffsetLeft;
    }

    public float getHandwritingBoundsOffsetTop()
    {
        return mHandwritingOffsetTop;
    }

    public float getHandwritingBoundsOffsetRight()
    {
        return mHandwritingOffsetRight;
    }

    public float getHandwritingBoundsOffsetBottom()
    {
        return mHandwritingOffsetBottom;
    }

    public long getUniqueDrawingId()
    {
        if (mUniqueDrawingId == 0) mUniqueDrawingId = ++sNextDrawingId;

        return mUniqueDrawingId;
    }

    public android.view.contentcapture.ContentCaptureSession getContentCaptureSession()
    {
        View current = this;

        while (true)
        {
            if (current.mContentCaptureSession != null) return current.mContentCaptureSession;
            if (!(current.mParent instanceof View)) return null;

            current = (View)current.mParent;
        }
    }

    public void setContentCaptureSession(android.view.contentcapture.ContentCaptureSession session)
    {
        mContentCaptureSession = session;
    }

    public boolean isShowingLayoutBounds()
    {
        return false;
    }

    public ViewRootImpl getViewRootImpl()
    {
        return findViewRoot();
    }

    public boolean hasIdentityMatrix()
    {
        return mTranslationX == 0 && mTranslationY == 0 && mScaleX == 1 && mScaleY == 1 && mRotation == 0;
    }

    public void transformMatrixToGlobal(android.graphics.Matrix matrix)
    {
        if (matrix == null) return;

        if (mParent instanceof View)
        {
            View parent = (View)mParent;
            parent.transformMatrixToGlobal(matrix);
            matrix.preTranslate(-parent.getScrollX(), -parent.getScrollY());
        }

        matrix.preTranslate(mLeft, mTop);

        if (!hasIdentityMatrix()) matrix.preConcat(getMatrix());
    }

    public void transformMatrixToLocal(android.graphics.Matrix matrix)
    {
        if (matrix == null) return;

        if (mParent instanceof View)
        {
            View parent = (View)mParent;
            parent.transformMatrixToLocal(matrix);
            matrix.postTranslate(parent.getScrollX(), parent.getScrollY());
        }

        matrix.postTranslate(-mLeft, -mTop);

        if (!hasIdentityMatrix())
        {
            android.graphics.Matrix inverse = new android.graphics.Matrix();
            if (getMatrix().invert(inverse)) matrix.postConcat(inverse);
        }
    }

    public float getAlpha()
    {
        return mAlpha;
    }

    public void bringToFront()
    {
        if (mParent != null) mParent.bringChildToFront(this);
    }

    public void setSelected(boolean selected)
    {
        if (mSelected == selected) return;

        mSelected = selected;
        refreshDrawableState();
        invalidate();
        dispatchSetSelected(selected);
    }

    protected void dispatchSetSelected(boolean selected)
    {
    }

    protected void dispatchSetPressed(boolean pressed)
    {
    }

    protected void dispatchSetActivated(boolean activated)
    {
    }

    public boolean isSelected()
    {
        return mSelected;
    }

    public void setPressed(boolean pressed)
    {
        if (mPressed == pressed) return;

        mPressed = pressed;

        if (pressed) drawableHotspotChanged(mLastTouchX, mLastTouchY);

        refreshDrawableState();
        invalidate();
        dispatchSetPressed(pressed);
    }

    public boolean isPressed()
    {
        return mPressed;
    }

    public void setActivated(boolean activated)
    {
        if (mActivated == activated) return;

        mActivated = activated;
        refreshDrawableState();
        invalidate();
        dispatchSetActivated(activated);
    }

    public boolean isActivated()
    {
        return mActivated;
    }

    public static final int IMPORTANT_FOR_AUTOFILL_AUTO = 0;
    public static final int IMPORTANT_FOR_AUTOFILL_YES = 1;
    public static final int IMPORTANT_FOR_AUTOFILL_NO = 2;
    public static final int IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS = 4;
    public static final int IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS = 8;

    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_AUTO = 0;
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES = 1;
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO = 2;
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS = 4;
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS = 8;

    public static final int SCROLL_AXIS_NONE = 0;
    public static final int SCROLL_AXIS_HORIZONTAL = 1;
    public static final int SCROLL_AXIS_VERTICAL = 2;

    private int mImportantForAutofill = IMPORTANT_FOR_AUTOFILL_AUTO;
    private int mImportantForContentCapture = IMPORTANT_FOR_CONTENT_CAPTURE_AUTO;
    private ViewOutlineProvider mOutlineProvider;
    private boolean mClipToOutline;

    public void setImportantForAutofill(int mode)
    {
        mImportantForAutofill = mode;
    }

    public int getImportantForAutofill()
    {
        return mImportantForAutofill;
    }

    public boolean isImportantForAutofill()
    {
        return mImportantForAutofill == IMPORTANT_FOR_AUTOFILL_YES;
    }

    public void setImportantForContentCapture(int mode)
    {
        mImportantForContentCapture = mode;
    }

    public int getImportantForContentCapture()
    {
        return mImportantForContentCapture;
    }

    public android.view.autofill.AutofillId getAutofillId()
    {
        if (mAutofillId == null) mAutofillId = new android.view.autofill.AutofillId(getAutofillViewId());
        return mAutofillId;
    }

    public void setAutofillId(android.view.autofill.AutofillId id)
    {
        if (mAttached && id != null) throw new IllegalStateException("Cannot set autofill id when view is attached");
        mAutofillId = id;
    }

    private int getAutofillViewId()
    {
        if (mAutofillViewId == NO_ID) mAutofillViewId = sNextAutofillViewId.getAndIncrement();
        return mAutofillViewId;
    }

    private int mAutofillViewId = NO_ID;
    private static final java.util.concurrent.atomic.AtomicInteger sNextAutofillViewId =
        new java.util.concurrent.atomic.AtomicInteger(0x3FFFFFFF);

    public void setAutofillHints(String... hints)
    {
        mAutofillHints = hints == null || hints.length == 0 ? null : hints;
    }

    public String[] getAutofillHints()
    {
        return mAutofillHints;
    }

    public void autofill(SparseArray<android.view.autofill.AutofillValue> values)
    {
    }

    public final boolean isImportantForContentCapture()
    {
        int mode = mImportantForContentCapture;
        return mode != IMPORTANT_FOR_CONTENT_CAPTURE_NO && mode != IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS;
    }

    public int getAutofillType()
    {
        return 0;
    }

    public android.view.autofill.AutofillValue getAutofillValue()
    {
        return null;
    }

    public void autofill(android.view.autofill.AutofillValue value)
    {
    }

    public void setOutlineProvider(ViewOutlineProvider provider)
    {
        mOutlineProvider = provider;
    }

    public ViewOutlineProvider getOutlineProvider()
    {
        return mOutlineProvider;
    }

    public void invalidateOutline()
    {
        invalidate();
    }

    public void setClipToOutline(boolean clip)
    {
        mClipToOutline = clip;
    }

    public boolean getClipToOutline()
    {
        return mClipToOutline;
    }

    public android.os.Handler getHandler()
    {
        if (mHandler != null) return mHandler;

        android.os.Looper main = android.os.Looper.getMainLooper();
        if (main == null) return null;

        mHandler = new android.os.Handler(main);
        return mHandler;
    }

    public boolean post(Runnable action)
    {
        if (action == null) return false;

        android.os.Handler handler = getHandler();
        if (handler == null) return false;

        return handler.post(action);
    }

    public boolean postDelayed(Runnable action, long delayMillis)
    {
        if (action == null) return false;

        android.os.Handler handler = getHandler();
        if (handler == null) return false;

        return handler.postDelayed(action, delayMillis);
    }

    public void postOnAnimation(Runnable action)
    {
        post(action);
    }

    public void postOnAnimationDelayed(Runnable action, long delayMillis)
    {
        postDelayed(action, delayMillis);
    }

    public boolean removeCallbacks(Runnable action)
    {
        if (action == null) return false;

        android.os.Handler handler = getHandler();
        if (handler == null) return false;

        handler.removeCallbacks(action);
        return true;
    }

    public boolean requestFocus()
    {
        return requestFocus(FOCUS_DOWN, null);
    }

    public boolean requestFocus(int direction)
    {
        return requestFocus(direction, null);
    }

    public boolean requestFocus(int direction, Rect previouslyFocusedRect)
    {
        if (!mFocusable || mVisibility != VISIBLE) return false;
        if (mFocusableAuto && !mFocusableInTouchMode && isInTouchMode()) return false;
        if (mFocused) return true;

        ViewRootImpl root = findViewRoot();
        if (root == null) return false;

        View previous = root.getFocusedView();

        root.setFocusedView(this);

        mFocused = true;
        if (mParent != null) mParent.requestChildFocus(this, this);

        refreshDrawableState();
        onFocusChanged(true, direction, previouslyFocusedRect);

        if (mFocusChangeListener != null) mFocusChangeListener.onFocusChange(this, true);

        ViewTreeObserver observer = getRootView().mViewTreeObserver;
        if (observer != null) observer.dispatchOnGlobalFocusChange(previous, this);

        return true;
    }

    public boolean requestFocusFromTouch()
    {
        return requestFocus();
    }

    public long getDrawingTime()
    {
        return android.os.SystemClock.uptimeMillis();
    }

    public void invalidate(int left, int top, int right, int bottom)
    {
        invalidateInternal(left - mScrollX, top - mScrollY, right - mScrollX, bottom - mScrollY);
    }

    public void postInvalidate()
    {
        postInvalidateDelayed(0);
    }

    public void postInvalidate(int left, int top, int right, int bottom)
    {
        postInvalidateDelayed(0);
    }

    public void postInvalidateDelayed(long delayMilliseconds)
    {
        if (!postDelayed(mInvalidateRunnable, delayMilliseconds)) invalidate();
    }

    public void postInvalidateDelayed(long delayMilliseconds, int left, int top, int right, int bottom)
    {
        postInvalidateDelayed(delayMilliseconds);
    }

    public void postInvalidateOnAnimation()
    {
        postInvalidateDelayed(0);
    }

    public void postInvalidateOnAnimation(int left, int top, int right, int bottom)
    {
        postInvalidateDelayed(0);
    }

    private final Runnable mInvalidateRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            invalidate();
        }
    };

    public View findViewById(int id)
    {
        if (id != NO_ID && id == mId) return this;
        return null;
    }

    public interface OnDragListener
    {
        boolean onDrag(View view, DragEvent event);
    }

    public interface OnContextClickListener
    {
        boolean onContextClick(View view);
    }

    public interface OnCapturedPointerListener
    {
        boolean onCapturedPointer(View view, MotionEvent event);
    }

    public interface OnSystemUiVisibilityChangeListener
    {
        void onSystemUiVisibilityChange(int visibility);
    }

    public interface OnUnhandledKeyEventListener
    {
        boolean onUnhandledKeyEvent(View view, KeyEvent event);
    }

    public interface OnLongClickListener
    {
        boolean onLongClick(View view);
    }

    public interface OnHoverListener
    {
        boolean onHover(View view, MotionEvent event);
    }

    public interface OnGenericMotionListener
    {
        boolean onGenericMotion(View view, MotionEvent event);
    }

    public interface OnScrollChangeListener
    {
        void onScrollChange(View view, int scrollX, int scrollY, int oldScrollX, int oldScrollY);
    }

    public interface OnCreateContextMenuListener
    {
        void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo);
    }

    public void setOnDragListener(OnDragListener listener)
    {
        mDragListener = listener;
    }

    public boolean dispatchDragEvent(DragEvent event)
    {
        if (mDragListener != null && mEnabled && mDragListener.onDrag(this, event)) return true;
        return onDragEvent(event);
    }

    public boolean onDragEvent(DragEvent event)
    {
        return false;
    }

    public final boolean startDrag(android.content.ClipData data, DragShadowBuilder shadowBuilder, Object localState, int flags)
    {
        return startDragAndDrop(data, shadowBuilder, localState, flags);
    }

    public final boolean startDragAndDrop(android.content.ClipData data, DragShadowBuilder shadowBuilder, Object localState, int flags)
    {
        if (!mAttached) return false;

        View root = getRootView();
        android.content.ClipDescription description = data != null ? data.getDescription() : null;
        DragEvent started = DragEvent.obtain(DragEvent.ACTION_DRAG_STARTED, 0, 0, localState, description, null, false);
        root.dispatchDragEventToTree(started);

        DragEvent ended = DragEvent.obtain(DragEvent.ACTION_DRAG_ENDED, 0, 0, localState, description, null, false);
        root.dispatchDragEventToTree(ended);
        return false;
    }

    void dispatchDragEventToTree(DragEvent event)
    {
        dispatchDragEvent(event);
    }

    public final void cancelDragAndDrop()
    {
    }

    public final void updateDragShadow(DragShadowBuilder shadowBuilder)
    {
    }

    public static class DragShadowBuilder
    {
        private final java.lang.ref.WeakReference<View> mView;

        public DragShadowBuilder(View view)
        {
            mView = new java.lang.ref.WeakReference<>(view);
        }

        public DragShadowBuilder()
        {
            mView = new java.lang.ref.WeakReference<>(null);
        }

        public final View getView()
        {
            return mView.get();
        }

        public void onProvideShadowMetrics(android.graphics.Point outShadowSize, android.graphics.Point outShadowTouchPoint)
        {
            View view = mView.get();
            if (view != null)
            {
                outShadowSize.set(view.getWidth(), view.getHeight());
                outShadowTouchPoint.set(outShadowSize.x / 2, outShadowSize.y / 2);
            }
        }

        public void onDrawShadow(Canvas canvas)
        {
            View view = mView.get();
            if (view != null) view.draw(canvas);
        }
    }

    public void setOnLongClickListener(OnLongClickListener listener)
    {
        mLongClickListener = listener;
        if (listener != null) mLongClickable = true;
    }

    public void setLongClickable(boolean longClickable)
    {
        mLongClickable = longClickable;
    }

    public boolean isLongClickable()
    {
        return mLongClickable;
    }

    public boolean performLongClick()
    {
        if (mLongClickListener == null) return false;

        return mLongClickListener.onLongClick(this);
    }

    public void setOnHoverListener(OnHoverListener listener)
    {
        mHoverListener = listener;
    }

    public void setOnGenericMotionListener(OnGenericMotionListener listener)
    {
        mGenericMotionListener = listener;
    }

    public void setOnScrollChangeListener(OnScrollChangeListener listener)
    {
        mScrollChangeListener = listener;
    }

    public void setOnContextClickListener(OnContextClickListener listener)
    {
        if (!mContextClickable) mContextClickable = listener != null;
        mContextClickListener = listener;
    }

    public boolean performContextClick()
    {
        return mContextClickListener != null && mContextClickListener.onContextClick(this);
    }

    public boolean performContextClick(float x, float y)
    {
        return performContextClick();
    }

    public void setContextClickable(boolean contextClickable)
    {
        mContextClickable = contextClickable;
    }

    public boolean isContextClickable()
    {
        return mContextClickable;
    }

    public void setOnCapturedPointerListener(OnCapturedPointerListener listener)
    {
        mCapturedPointerListener = listener;
    }

    public void requestPointerCapture()
    {
        mPointerCaptured = true;
        onPointerCaptureChange(true);
    }

    public void releasePointerCapture()
    {
        if (!mPointerCaptured) return;

        mPointerCaptured = false;
        onPointerCaptureChange(false);
    }

    public boolean hasPointerCapture()
    {
        return mPointerCaptured;
    }

    public void onPointerCaptureChange(boolean hasCapture)
    {
    }

    public void dispatchPointerCaptureChanged(boolean hasCapture)
    {
        onPointerCaptureChange(hasCapture);
    }

    public boolean onCapturedPointerEvent(MotionEvent event)
    {
        return false;
    }

    public final boolean dispatchCapturedPointerEvent(MotionEvent event)
    {
        if (mCapturedPointerListener != null && mCapturedPointerListener.onCapturedPointer(this, event)) return true;
        return onCapturedPointerEvent(event);
    }

    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex)
    {
        return mPointerIcon;
    }

    public void setPointerIcon(PointerIcon pointerIcon)
    {
        mPointerIcon = pointerIcon;
    }

    public PointerIcon getPointerIcon()
    {
        return mPointerIcon;
    }

    private OnCreateContextMenuListener mContextMenuListener;

    public void setOnCreateContextMenuListener(OnCreateContextMenuListener listener)
    {
        mContextMenuListener = listener;
        if (listener != null) mLongClickable = true;
    }

    protected void onCreateContextMenu(ContextMenu menu)
    {
    }

    public void createContextMenu(ContextMenu menu)
    {
        onCreateContextMenu(menu);
        if (mContextMenuListener != null) mContextMenuListener.onCreateContextMenu(menu, this, null);

        ViewParent parent = getParent();
        if (parent instanceof View) ((View)parent).createContextMenu(menu);
    }

    public boolean showContextMenu()
    {
        return false;
    }

    public boolean showContextMenu(float x, float y)
    {
        return showContextMenu();
    }

    public interface OnClickListener
    {
        void onClick(View view);
    }

    public interface OnApplyWindowInsetsListener
    {
        WindowInsets onApplyWindowInsets(View view, WindowInsets insets);
    }

    public interface OnAttachStateChangeListener
    {
        void onViewAttachedToWindow(View view);

        void onViewDetachedFromWindow(View view);
    }

    public interface OnKeyListener
    {
        boolean onKey(View view, int keyCode, KeyEvent event);
    }

    public static int generateViewId()
    {
        while (true)
        {
            int current = sNextGeneratedId.get();
            int next = current + 1;
            if (next > 0x00FFFFFF) next = 1;

            if (sNextGeneratedId.compareAndSet(current, next)) return current;
        }
    }

    private static final java.util.concurrent.atomic.AtomicInteger sNextGeneratedId =
        new java.util.concurrent.atomic.AtomicInteger(1);

    public interface OnTouchListener
    {
        boolean onTouch(View view, MotionEvent event);
    }

    public interface OnFocusChangeListener
    {
        void onFocusChange(View view, boolean hasFocus);
    }

    public interface OnLayoutChangeListener
    {
        void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight,
                            int oldBottom);
    }

    public void setOnKeyListener(OnKeyListener listener)
    {
        mKeyListener = listener;
    }

    public void setOnTouchListener(OnTouchListener listener)
    {
        mTouchListener = listener;
    }

    public void setOnFocusChangeListener(OnFocusChangeListener listener)
    {
        mFocusChangeListener = listener;
    }

    public OnFocusChangeListener getOnFocusChangeListener()
    {
        return mFocusChangeListener;
    }

    public void addOnLayoutChangeListener(OnLayoutChangeListener listener)
    {
        if (listener != null && !mLayoutChangeListeners.contains(listener)) mLayoutChangeListeners.add(listener);
    }

    public void removeOnLayoutChangeListener(OnLayoutChangeListener listener)
    {
        mLayoutChangeListeners.remove(listener);
    }

    private OnKeyListener mKeyListener;
    private OnTouchListener mTouchListener;
    private boolean mClickable;
    private boolean mPressed;
    private OnFocusChangeListener mFocusChangeListener;
    private final java.util.ArrayList<OnLayoutChangeListener> mLayoutChangeListeners = new java.util.ArrayList<>();

    public final View requireViewById(int id)
    {
        View found = findViewById(id);
        if (found == null) throw new IllegalArgumentException("No view with id 0x" + Integer.toHexString(id));
        return found;
    }

    @Override
    public String toString()
    {
        return getClass().getName() + "{id=0x" + Integer.toHexString(mId) + " " + mLeft + "," + mTop + "-" + mRight + "," + mBottom + "}";
    }

    protected static final String VIEW_LOG_TAG = "View";

    public static final int TEXT_DIRECTION_INHERIT = 0;
    public static final int TEXT_DIRECTION_FIRST_STRONG = 1;
    public static final int TEXT_DIRECTION_ANY_RTL = 2;
    public static final int TEXT_DIRECTION_LTR = 3;
    public static final int TEXT_DIRECTION_RTL = 4;
    public static final int TEXT_DIRECTION_LOCALE = 5;
    public static final int TEXT_DIRECTION_FIRST_STRONG_LTR = 6;
    public static final int TEXT_DIRECTION_FIRST_STRONG_RTL = 7;

    public static final int TEXT_ALIGNMENT_INHERIT = 0;
    public static final int TEXT_ALIGNMENT_GRAVITY = 1;
    public static final int TEXT_ALIGNMENT_TEXT_START = 2;
    public static final int TEXT_ALIGNMENT_TEXT_END = 3;
    public static final int TEXT_ALIGNMENT_CENTER = 4;
    public static final int TEXT_ALIGNMENT_VIEW_START = 5;
    public static final int TEXT_ALIGNMENT_VIEW_END = 6;

    public static final int SCROLL_INDICATOR_TOP = 1;
    public static final int SCROLL_INDICATOR_BOTTOM = 2;
    public static final int SCROLL_INDICATOR_LEFT = 4;
    public static final int SCROLL_INDICATOR_RIGHT = 8;
    public static final int SCROLL_INDICATOR_START = 16;
    public static final int SCROLL_INDICATOR_END = 32;
    private static final int SCROLL_INDICATORS_ALL = 0x3f;

    public static final int SCROLLBAR_POSITION_DEFAULT = 0;
    public static final int SCROLLBAR_POSITION_LEFT = 1;
    public static final int SCROLLBAR_POSITION_RIGHT = 2;

    public static final int SCROLLBARS_INSIDE_OVERLAY = 0;
    public static final int SCROLLBARS_INSIDE_INSET = 0x01000000;
    public static final int SCROLLBARS_OUTSIDE_OVERLAY = 0x02000000;
    public static final int SCROLLBARS_OUTSIDE_INSET = 0x03000000;

    public static final int SCROLL_CAPTURE_HINT_AUTO = 0;
    public static final int SCROLL_CAPTURE_HINT_EXCLUDE = 1;
    public static final int SCROLL_CAPTURE_HINT_INCLUDE = 2;
    public static final int SCROLL_CAPTURE_HINT_EXCLUDE_DESCENDANTS = 4;

    public static final int DRAG_FLAG_GLOBAL_URI_READ = 1;
    public static final int DRAG_FLAG_GLOBAL_URI_WRITE = 2;
    public static final int DRAG_FLAG_GLOBAL_PERSISTABLE_URI_PERMISSION = 64;
    public static final int DRAG_FLAG_GLOBAL_PREFIX_URI_PERMISSION = 128;
    public static final int DRAG_FLAG_GLOBAL = 256;
    public static final int DRAG_FLAG_OPAQUE = 512;
    public static final int DRAG_FLAG_ACCESSIBILITY_ACTION = 1024;

    public static final int DRAWING_CACHE_QUALITY_AUTO = 0;
    public static final int DRAWING_CACHE_QUALITY_LOW = 0x00080000;
    public static final int DRAWING_CACHE_QUALITY_HIGH = 0x00100000;

    public static final int FIND_VIEWS_WITH_TEXT = 1;
    public static final int FIND_VIEWS_WITH_CONTENT_DESCRIPTION = 2;

    public static final int FOCUSABLES_ALL = 0;
    public static final int FOCUSABLES_TOUCH_MODE = 1;

    public static final int AUTOFILL_TYPE_NONE = 0;
    public static final int AUTOFILL_TYPE_TEXT = 1;
    public static final int AUTOFILL_TYPE_TOGGLE = 2;
    public static final int AUTOFILL_TYPE_LIST = 3;
    public static final int AUTOFILL_TYPE_DATE = 4;
    public static final int AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 1;

    public static final String AUTOFILL_HINT_EMAIL_ADDRESS = "emailAddress";
    public static final String AUTOFILL_HINT_NAME = "name";
    public static final String AUTOFILL_HINT_USERNAME = "username";
    public static final String AUTOFILL_HINT_PASSWORD = "password";
    public static final String AUTOFILL_HINT_PHONE = "phone";
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS = "postalAddress";
    public static final String AUTOFILL_HINT_POSTAL_CODE = "postalCode";
    public static final String AUTOFILL_HINT_CREDIT_CARD_NUMBER = "creditCardNumber";
    public static final String AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE = "creditCardSecurityCode";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE = "creditCardExpirationDate";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH = "creditCardExpirationMonth";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR = "creditCardExpirationYear";
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY = "creditCardExpirationDay";

    public static final int ACCESSIBILITY_DATA_SENSITIVE_AUTO = 0;
    public static final int ACCESSIBILITY_DATA_SENSITIVE_YES = 1;
    public static final int ACCESSIBILITY_DATA_SENSITIVE_NO = 2;

    public static final int HAPTIC_FEEDBACK_ENABLED = 0x10000000;
    public static final int SOUND_EFFECTS_ENABLED = 0x08000000;
    public static final int KEEP_SCREEN_ON = 0x04000000;

    public static final int SCREEN_STATE_OFF = 0;
    public static final int SCREEN_STATE_ON = 1;

    public static final int STATUS_BAR_VISIBLE = 0;
    public static final int STATUS_BAR_HIDDEN = 1;
    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 1;
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 2;
    public static final int SYSTEM_UI_FLAG_FULLSCREEN = 4;
    public static final int SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = 8192;
    public static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 16;
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 256;
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 512;
    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 1024;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE = 2048;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 4096;
    public static final int SYSTEM_UI_LAYOUT_FLAGS = SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

    private boolean mInLayout;
    private boolean mEverLaidOut;
    private boolean mDirty = true;
    private boolean mPendingInsetsRequest;
    private boolean mPaddingRelative;
    private CharSequence mContentDescription;
    private boolean mSaveFromParentEnabled = true;
    private boolean mSaveEnabled = true;
    private int mScrollBarStyle = SCROLLBARS_INSIDE_OVERLAY;
    private OnSystemUiVisibilityChangeListener mSystemUiVisibilityListener;
    private boolean mFilterTouchesWhenObscured;
    private boolean mContextClickable;
    private TouchDelegate mTouchDelegate;
    private ArrayList<OnUnhandledKeyEventListener> mUnhandledKeyListeners;
    private int mWindowAttachCount;
    private OnDragListener mDragListener;
    private OnContextClickListener mContextClickListener;
    private OnCapturedPointerListener mCapturedPointerListener;
    private boolean mPointerCaptured;
    private PointerIcon mPointerIcon;
    private OnHoverListener mHoverListener;
    private OnGenericMotionListener mGenericMotionListener;
    private OnScrollChangeListener mScrollChangeListener;
    private boolean mSendingHoverAccessibilityEvents;
    private boolean mHovered;
    private boolean mDuplicateParentState;
    private boolean mSoundEffectsEnabled = true;
    private CharSequence mAccessibilityPaneTitle;
    private boolean mScreenReaderFocusable;
    private boolean mAccessibilityHeading;
    private boolean mForceDarkAllowed = true;
    private CharSequence mTooltipText;
    private boolean mNestedScrollingEnabled;
    private ViewParent mNestedScrollingParent;
    private int[] mTempNestedScrollConsumed;
    private boolean mHorizontalScrollBarEnabled;
    private boolean mVerticalScrollBarEnabled;
    private int mVerticalScrollbarPosition = SCROLLBAR_POSITION_DEFAULT;
    private int mScrollIndicators;
    private boolean mScrollContainer;
    private int mTransientStateCount;
    private int mLabelForId = NO_ID;
    private int mAccessibilityTraversalBeforeId = NO_ID;
    private int mAccessibilityTraversalAfterId = NO_ID;
    private int mNextFocusForwardId = NO_ID;
    private int mNextFocusLeftId = NO_ID;
    private int mNextFocusRightId = NO_ID;
    private int mNextFocusUpId = NO_ID;
    private int mNextFocusDownId = NO_ID;
    private int mNextClusterForwardId = NO_ID;
    private String mTransitionName;
    private float mTransitionAlpha = 1.0f;
    private boolean mKeepScreenOn;
    private boolean mDefaultFocusHighlightEnabled = true;
    private boolean mFocusedByDefault;
    private boolean mKeyboardNavigationCluster;
    private boolean mRevealOnFocusHint = true;
    private android.view.autofill.AutofillId mAutofillId;
    private String[] mAutofillHints;
    private android.view.contentcapture.ContentCaptureSession mContentCaptureSession;
    private android.graphics.drawable.Drawable mForeground;
    private int mForegroundGravity = Gravity.FILL;
    private android.content.res.ColorStateList mForegroundTintList;
    private android.graphics.PorterDuff.Mode mForegroundTintMode;
    private android.content.res.ColorStateList mBackgroundTintList;
    private android.graphics.PorterDuff.Mode mBackgroundTintMode;
    private android.graphics.BlendMode mBackgroundTintBlendMode;
    private ViewOverlay mOverlay;
    private Animation mAnimation;
    private boolean mAnimationStarted;
    private android.graphics.Matrix mAnimationMatrix;
    private android.animation.StateListAnimator mStateListAnimator;
    private float mCameraDistance;
    private boolean mHasOverlappingRendering = true;
    private boolean mOverlappingRenderingForced;
    private int mOutlineSpotShadowColor = 0xff000000;
    private int mOutlineAmbientShadowColor = 0xff000000;
    private int mRawTextDirection = TEXT_DIRECTION_INHERIT;
    private int mRawTextAlignment = TEXT_ALIGNMENT_INHERIT;
    private String[] mReceiveContentMimeTypes;
    private OnReceiveContentListener mReceiveContentListener;
    private boolean mTemporarilyDetached;
    private boolean mDrawingCacheEnabled;
    private android.graphics.Bitmap mDrawingCache;
    private int mDrawingCacheBackgroundColor;
    private int mDrawingCacheQuality = DRAWING_CACHE_QUALITY_AUTO;
    private boolean mWillNotCacheDrawing;
    private boolean mAccessibilityFocused;
    private int mAccessibilityDataSensitive = ACCESSIBILITY_DATA_SENSITIVE_AUTO;
    private int mScrollCaptureHint = SCROLL_CAPTURE_HINT_AUTO;
    private float mRequestedFrameRate = Float.NaN;
    private float mFrameContentVelocity;
    private WindowId mWindowId;

    public void setTouchDelegate(TouchDelegate delegate)
    {
        mTouchDelegate = delegate;
    }

    public TouchDelegate getTouchDelegate()
    {
        return mTouchDelegate;
    }

    public java.util.Map<Integer, Integer> getAttributeSourceResourceMap()
    {
        return new java.util.HashMap<>();
    }

    public int getExplicitStyle()
    {
        return 0;
    }

    public int[] getAttributeResolutionStack(int attribute)
    {
        return new int[0];
    }

    public ActionMode startActionMode(ActionMode.Callback callback)
    {
        return startActionMode(callback, ActionMode.TYPE_PRIMARY);
    }

    public ActionMode startActionMode(ActionMode.Callback callback, int type)
    {
        ViewParent parent = getParent();
        if (parent == null) return null;

        return parent.startActionModeForChild(this, callback, type);
    }

    public final android.window.OnBackInvokedDispatcher findOnBackInvokedDispatcher()
    {
        ViewParent parent = getParent();
        if (parent == null) return null;

        return parent.findOnBackInvokedDispatcherForChild(this, this);
    }

    public static View inflate(Context context, int resource, ViewGroup root)
    {
        return LayoutInflater.from(context).inflate(resource, root);
    }

    public boolean requestRectangleOnScreen(Rect rectangle)
    {
        return requestRectangleOnScreen(rectangle, false);
    }

    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate)
    {
        if (mParent == null) return false;

        View child = this;
        android.graphics.RectF position = new android.graphics.RectF(rectangle);
        boolean scrolled = false;
        ViewParent parent = mParent;

        while (parent != null)
        {
            rectangle.set((int)position.left, (int)position.top, (int)position.right, (int)position.bottom);
            scrolled |= parent.requestChildRectangleOnScreen(child, rectangle, immediate);

            if (!(parent instanceof View)) break;

            position.offset(child.mLeft - child.getScrollX(), child.mTop - child.getScrollY());

            child = (View)parent;
            parent = child.getParent();
        }

        return scrolled;
    }

    public void getFocusedRect(Rect rect)
    {
        getDrawingRect(rect);
    }

    public int getFocusable()
    {
        if (mFocusableAuto) return FOCUSABLE_AUTO;
        return mFocusable ? FOCUSABLE : NOT_FOCUSABLE;
    }

    boolean canTakeFocus()
    {
        return mVisibility == VISIBLE && mFocusable && mEnabled;
    }

    public boolean hasFocusable()
    {
        return mVisibility == VISIBLE && mFocusable;
    }

    public boolean hasExplicitFocusable()
    {
        return hasFocusable();
    }

    public View focusSearch(int direction)
    {
        if (mParent != null) return mParent.focusSearch(this, direction);
        return null;
    }

    public View keyboardNavigationClusterSearch(View currentCluster, int direction)
    {
        if (isKeyboardNavigationCluster()) currentCluster = this;

        if (mParent instanceof View) return mParent.keyboardNavigationClusterSearch(currentCluster, direction);

        return FocusFinder.getInstance().findNextKeyboardNavigationCluster(this, currentCluster, direction);
    }

    public boolean dispatchUnhandledMove(View focused, int direction)
    {
        return false;
    }

    public ArrayList<View> getFocusables(int direction)
    {
        ArrayList<View> result = new ArrayList<>(24);
        addFocusables(result, direction);
        return result;
    }

    public void addFocusables(ArrayList<View> views, int direction)
    {
        addFocusables(views, direction, isInTouchMode() ? FOCUSABLES_TOUCH_MODE : FOCUSABLES_ALL);
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode)
    {
        if (views == null) return;
        if (!canTakeFocus()) return;
        if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE && !isFocusableInTouchMode()) return;

        views.add(this);
    }

    public void addKeyboardNavigationClusters(java.util.Collection<View> views, int direction)
    {
        if (!isKeyboardNavigationCluster()) return;
        if (!hasFocusable()) return;

        views.add(this);
    }

    public void findViewsWithText(ArrayList<View> outViews, CharSequence searched, int flags)
    {
        if ((flags & FIND_VIEWS_WITH_CONTENT_DESCRIPTION) == 0) return;
        if (searched == null || searched.length() == 0 || mContentDescription == null || mContentDescription.length() == 0) return;

        String searchedLowerCase = searched.toString().toLowerCase();
        String contentDescriptionLowerCase = mContentDescription.toString().toLowerCase();
        if (contentDescriptionLowerCase.contains(searchedLowerCase)) outViews.add(this);
    }

    public ArrayList<View> getTouchables()
    {
        ArrayList<View> result = new ArrayList<>();
        addTouchables(result);
        return result;
    }

    public void addTouchables(ArrayList<View> views)
    {
        if ((isClickable() || mLongClickable || mContextClickable) && mEnabled) views.add(this);
    }

    public boolean restoreDefaultFocus()
    {
        return requestFocus(FOCUS_DOWN);
    }

    boolean hasDefaultFocus()
    {
        return mFocusedByDefault;
    }

    public boolean isAccessibilityFocused()
    {
        return mAccessibilityFocused;
    }

    public void setAccessibilityDataSensitive(int accessibilityDataSensitive)
    {
        mAccessibilityDataSensitive = accessibilityDataSensitive;
    }

    public int getAccessibilityDataSensitive()
    {
        return mAccessibilityDataSensitive;
    }

    public boolean isAccessibilityDataSensitive()
    {
        return mAccessibilityDataSensitive == ACCESSIBILITY_DATA_SENSITIVE_YES;
    }

    public void sendAccessibilityEvent(int eventType)
    {
        if (mAccessibilityDelegate != null) mAccessibilityDelegate.sendAccessibilityEvent(this, eventType);
        else sendAccessibilityEventInternal(eventType);
    }

    void sendAccessibilityEventInternal(int eventType)
    {
        if (!isAccessibilityEnabled()) return;

        sendAccessibilityEventUnchecked(AccessibilityEvent.obtain(eventType));
    }

    private boolean isAccessibilityEnabled()
    {
        return android.view.accessibility.AccessibilityManager.getInstance(mContext).isEnabled();
    }

    public void announceForAccessibility(CharSequence text)
    {
        if (!isAccessibilityEnabled() || mParent == null) return;

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        onInitializeAccessibilityEvent(event);
        event.getText().add(text);
        event.setContentDescription(null);
        mParent.requestSendAccessibilityEvent(this, event);
    }

    public void sendAccessibilityEventUnchecked(AccessibilityEvent event)
    {
        if (mAccessibilityDelegate != null) mAccessibilityDelegate.sendAccessibilityEventUnchecked(this, event);
        else sendAccessibilityEventUncheckedInternal(event);
    }

    void sendAccessibilityEventUncheckedInternal(AccessibilityEvent event)
    {
        if (!isShown()) return;

        onInitializeAccessibilityEvent(event);
        dispatchPopulateAccessibilityEvent(event);

        ViewParent parent = getParent();
        if (parent != null) parent.requestSendAccessibilityEvent(this, event);
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event)
    {
        if (mAccessibilityDelegate != null) return mAccessibilityDelegate.dispatchPopulateAccessibilityEvent(this, event);
        return dispatchPopulateAccessibilityEventInternal(event);
    }

    boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event)
    {
        onPopulateAccessibilityEvent(event);
        return false;
    }

    public void onPopulateAccessibilityEvent(AccessibilityEvent event)
    {
        if (mAccessibilityDelegate != null) mAccessibilityDelegate.onPopulateAccessibilityEvent(this, event);
        else onPopulateAccessibilityEventInternal(event);
    }

    void onPopulateAccessibilityEventInternal(AccessibilityEvent event)
    {
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event)
    {
        if (mAccessibilityDelegate != null) mAccessibilityDelegate.onInitializeAccessibilityEvent(this, event);
        else onInitializeAccessibilityEventInternal(event);
    }

    void onInitializeAccessibilityEventInternal(AccessibilityEvent event)
    {
        event.setSource(this);
        event.setClassName(getAccessibilityClassName());
        event.setPackageName(mContext.getPackageName());
        event.setEnabled(isEnabled());
        event.setContentDescription(mContentDescription);
        event.setScrollX(mScrollX);
        event.setScrollY(mScrollY);
    }

    public AccessibilityNodeInfo createAccessibilityNodeInfo()
    {
        if (mAccessibilityDelegate != null) return mAccessibilityDelegate.createAccessibilityNodeInfo(this);
        return createAccessibilityNodeInfoInternal();
    }

    AccessibilityNodeInfo createAccessibilityNodeInfoInternal()
    {
        android.view.accessibility.AccessibilityNodeProvider provider = getAccessibilityNodeProvider();
        int hostId = android.view.accessibility.AccessibilityNodeProvider.HOST_VIEW_ID;
        if (provider != null) return provider.createAccessibilityNodeInfo(hostId);

        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(this);
        onInitializeAccessibilityNodeInfo(info);
        return info;
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info)
    {
        if (mAccessibilityDelegate != null) mAccessibilityDelegate.onInitializeAccessibilityNodeInfo(this, info);
        else onInitializeAccessibilityNodeInfoInternal(info);
    }

    void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info)
    {
        Rect bounds = new Rect();
        getDrawingRect(bounds);
        bounds.offset(-mScrollX, -mScrollY);
        bounds.offset(mLeft, mTop);
        info.setBoundsInParent(bounds);

        getBoundsOnScreen(bounds, true);
        info.setBoundsInScreen(bounds);

        ViewParent parent = getParentForAccessibility();
        if (parent instanceof View) info.setParent((View)parent);

        if (mLabelForId != NO_ID)
        {
            View root = getRootView();
            View label = root.findViewById(mLabelForId);
            if (label != null) info.setLabelFor(label);
        }

        info.setVisibleToUser(isShown());
        info.setPackageName(mContext.getPackageName());
        info.setClassName(getAccessibilityClassName());
        info.setStateDescription(mStateDescription);
        info.setContentDescription(mContentDescription);
        info.setEnabled(isEnabled());
        info.setClickable(isClickable());
        info.setFocusable(isFocusable());
        info.setScreenReaderFocusable(mScreenReaderFocusable);
        info.setFocused(isFocused());
        info.setAccessibilityFocused(mAccessibilityFocused);
        info.setSelected(isSelected());
        info.setLongClickable(isLongClickable());
        info.setContextClickable(mContextClickable);
        info.setLiveRegion(getAccessibilityLiveRegion());
        info.setImportantForAccessibility(isImportantForAccessibility());
        info.setHeading(mAccessibilityHeading);
        info.setPaneTitle(mAccessibilityPaneTitle);
        if (mTooltipText != null) info.setTooltipText(mTooltipText);

        if (mId != NO_ID)
        {
            try
            {
                info.setViewIdResourceName(getResources().getResourceName(mId));
            }
            catch (RuntimeException ignored)
            {
            }
        }

        if (isFocusable())
        {
            info.addAction(isFocused() ? AccessibilityNodeInfo.ACTION_CLEAR_FOCUS : AccessibilityNodeInfo.ACTION_FOCUS);
        }

        info.addAction(mAccessibilityFocused ? AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS
                                             : AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);

        if (isClickable() && isEnabled()) info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (isLongClickable() && isEnabled()) info.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        if (mContextClickable && isEnabled()) info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CONTEXT_CLICK);
    }

    public void addExtraDataToAccessibilityNodeInfo(AccessibilityNodeInfo info, String extraDataKey, Bundle arguments)
    {
    }

    public CharSequence getAccessibilityClassName()
    {
        return View.class.getName();
    }

    public android.view.accessibility.AccessibilityNodeProvider getAccessibilityNodeProvider()
    {
        if (mAccessibilityDelegate != null) return mAccessibilityDelegate.getAccessibilityNodeProvider(this);
        return null;
    }

    public boolean performAccessibilityAction(int action, Bundle arguments)
    {
        if (mAccessibilityDelegate != null) return mAccessibilityDelegate.performAccessibilityAction(this, action, arguments);
        return performAccessibilityActionInternal(action, arguments);
    }

    boolean performAccessibilityActionInternal(int action, Bundle arguments)
    {
        if (mParent != null && mParent.onNestedPrePerformAccessibilityAction(this, action, arguments)) return true;

        switch (action)
        {
            case AccessibilityNodeInfo.ACTION_CLICK:
                if (isClickable() && isEnabled())
                {
                    performClick();
                    return true;
                }
                return false;
            case AccessibilityNodeInfo.ACTION_LONG_CLICK:
                return isLongClickable() && isEnabled() && performLongClick();
            case AccessibilityNodeInfo.ACTION_FOCUS:
                return !hasFocus() && requestFocus();
            case AccessibilityNodeInfo.ACTION_CLEAR_FOCUS:
                if (!hasFocus()) return false;
                clearFocus();
                return !isFocused();
            case AccessibilityNodeInfo.ACTION_SELECT:
                if (isSelected()) return false;
                setSelected(true);
                return isSelected();
            case AccessibilityNodeInfo.ACTION_CLEAR_SELECTION:
                if (!isSelected()) return false;
                setSelected(false);
                return !isSelected();
            case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS:
                if (mAccessibilityFocused) return false;
                mAccessibilityFocused = true;
                invalidate();
                return true;
            case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                if (!mAccessibilityFocused) return false;
                mAccessibilityFocused = false;
                invalidate();
                return true;
            default:
                return false;
        }
    }

    public ViewParent getParentForAccessibility()
    {
        ViewParent parent = mParent;

        while (parent instanceof View)
        {
            View view = (View)parent;
            if (view.includeForAccessibility()) return parent;
            parent = parent.getParent();
        }

        return null;
    }

    boolean includeForAccessibility()
    {
        if (mImportantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_NO
            || mImportantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
        {
            return false;
        }

        if (mImportantForAccessibility == IMPORTANT_FOR_ACCESSIBILITY_YES) return true;

        return isClickable() || mLongClickable || mFocusable || mContentDescription != null || mAccessibilityDelegate != null
               || getAccessibilityLiveRegion() != ACCESSIBILITY_LIVE_REGION_NONE || mAccessibilityPaneTitle != null
               || mAccessibilityHeading || mScreenReaderFocusable;
    }

    public void addChildrenForAccessibility(ArrayList<View> outChildren)
    {
    }

    public void notifySubtreeAccessibilityStateChanged(View child, View source, int changeType)
    {
        if (mParent != null) mParent.notifySubtreeAccessibilityStateChanged(this, source, changeType);
    }

    public void onProvideStructure(ViewStructure structure)
    {
        onProvideStructureInternal(structure, false);
    }

    public void onProvideAutofillStructure(ViewStructure structure, int flags)
    {
        onProvideStructureInternal(structure, true);
    }

    public void onProvideContentCaptureStructure(ViewStructure structure, int flags)
    {
        onProvideStructureInternal(structure, false);
    }

    private void onProvideStructureInternal(ViewStructure structure, boolean forAutofill)
    {
        String packageName = null;
        String typeName = null;
        String entryName = null;

        if (mId != NO_ID && (mId & 0xff000000) != 0 && (mId & 0x00ff0000) != 0 && (mId & 0x0000ffff) != 0)
        {
            try
            {
                android.content.res.Resources resources = getResources();
                entryName = resources.getResourceEntryName(mId);
                typeName = "id";
                packageName = resources.getResourceName(mId);
                int colon = packageName.indexOf(':');
                packageName = colon > 0 ? packageName.substring(0, colon) : null;
            }
            catch (RuntimeException ignored)
            {
                packageName = null;
                typeName = null;
                entryName = null;
            }
        }

        structure.setId(mId, packageName, typeName, entryName);

        if (forAutofill)
        {
            structure.setAutofillId(getAutofillId());
            structure.setAutofillType(getAutofillType());
            structure.setAutofillHints(getAutofillHints());
            structure.setAutofillValue(getAutofillValue());
            structure.setImportantForAutofill(getImportantForAutofill());
        }

        structure.setDimens(mLeft, mTop, mScrollX, mScrollY, getWidth(), getHeight());

        if (!hasIdentityMatrix()) structure.setTransformation(getMatrix());
        structure.setElevation(getZ());
        structure.setVisibility(getVisibility());
        structure.setEnabled(isEnabled());
        if (isClickable()) structure.setClickable(true);
        if (isFocusable()) structure.setFocusable(true);
        if (isFocused()) structure.setFocused(true);
        if (isAccessibilityFocused()) structure.setAccessibilityFocused(true);
        if (isSelected()) structure.setSelected(true);
        if (isActivated()) structure.setActivated(true);
        if (isLongClickable()) structure.setLongClickable(true);
        if (isContextClickable()) structure.setContextClickable(true);

        CharSequence className = getAccessibilityClassName();
        if (className != null) structure.setClassName(className.toString());
        structure.setContentDescription(getContentDescription());
    }

    public void onProvideVirtualStructure(ViewStructure structure)
    {
    }

    public void onProvideAutofillVirtualStructure(ViewStructure structure, int flags)
    {
    }

    public void dispatchProvideStructure(ViewStructure structure)
    {
        onProvideStructure(structure);
        onProvideVirtualStructure(structure);
    }

    public void dispatchProvideAutofillStructure(ViewStructure structure, int flags)
    {
        onProvideAutofillStructure(structure, flags);
        onProvideAutofillVirtualStructure(structure, flags);
    }

    public void setOnReceiveContentListener(String[] mimeTypes, OnReceiveContentListener listener)
    {
        if (listener != null && (mimeTypes == null || mimeTypes.length == 0))
        {
            throw new IllegalArgumentException("When the listener is set, MIME types must also be set");
        }

        if (mimeTypes != null && mimeTypes.length > 0)
        {
            for (String mimeType : mimeTypes)
            {
                if (mimeType.startsWith("*")) throw new IllegalArgumentException("A MIME type set here must not start with *: " + mimeType);
            }
        }

        mReceiveContentMimeTypes = mimeTypes == null || mimeTypes.length == 0 ? null : mimeTypes;
        mReceiveContentListener = listener;
    }

    public ContentInfo performReceiveContent(ContentInfo payload)
    {
        if (mReceiveContentListener != null)
        {
            ContentInfo remaining = mReceiveContentListener.onReceiveContent(this, payload);
            return remaining == null ? null : onReceiveContent(remaining);
        }

        return onReceiveContent(payload);
    }

    public ContentInfo onReceiveContent(ContentInfo payload)
    {
        return payload;
    }

    public String[] getReceiveContentMimeTypes()
    {
        return mReceiveContentMimeTypes;
    }

    public void setDrawingCacheEnabled(boolean enabled)
    {
        mDrawingCacheEnabled = enabled;
        if (!enabled) destroyDrawingCache();
    }

    public boolean isDrawingCacheEnabled()
    {
        return mDrawingCacheEnabled;
    }

    public android.graphics.Bitmap getDrawingCache()
    {
        return getDrawingCache(false);
    }

    public android.graphics.Bitmap getDrawingCache(boolean autoScale)
    {
        if (mWillNotCacheDrawing) return null;
        if (mDrawingCacheEnabled || mDrawingCache == null) buildDrawingCache(autoScale);

        return mDrawingCache;
    }

    public void destroyDrawingCache()
    {
        mDrawingCache = null;
    }

    public void setDrawingCacheBackgroundColor(int color)
    {
        if (color == mDrawingCacheBackgroundColor) return;

        mDrawingCacheBackgroundColor = color;
        destroyDrawingCache();
    }

    public int getDrawingCacheBackgroundColor()
    {
        return mDrawingCacheBackgroundColor;
    }

    public void buildDrawingCache()
    {
        buildDrawingCache(false);
    }

    public void buildDrawingCache(boolean autoScale)
    {
        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0)
        {
            mDrawingCache = null;
            return;
        }

        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (mDrawingCacheBackgroundColor != 0) canvas.drawColor(mDrawingCacheBackgroundColor);
        draw(canvas);
        mDrawingCache = bitmap;
    }

    public void setDrawingCacheQuality(int quality)
    {
        mDrawingCacheQuality = quality;
    }

    public int getDrawingCacheQuality()
    {
        return mDrawingCacheQuality;
    }

    public void setWillNotCacheDrawing(boolean willNotCacheDrawing)
    {
        mWillNotCacheDrawing = willNotCacheDrawing;
    }

    public boolean willNotCacheDrawing()
    {
        return mWillNotCacheDrawing;
    }

    public void buildLayer()
    {
    }

    ViewOverlay createOverlay()
    {
        return new ViewOverlay(mContext, this);
    }

    public ViewOverlay getOverlay()
    {
        if (mOverlay == null)
        {
            mOverlay = createOverlay();
            if (mAttached) mOverlay.getOverlayView().dispatchAttachedToWindow();
        }

        return mOverlay;
    }

    public int getSolidColor()
    {
        return 0;
    }

    public final void setLeftTopRightBottom(int left, int top, int right, int bottom)
    {
        int oldWidth = mRight - mLeft;
        int oldHeight = mBottom - mTop;

        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;

        int width = right - left;
        int height = bottom - top;

        if (width != oldWidth || height != oldHeight)
        {
            mMatrixStale = true;
            if (mBackground != null) mBackground.setBounds(0, 0, width, height);
            if (mForeground != null) mForeground.setBounds(0, 0, width, height);
            onSizeChanged(width, height, oldWidth, oldHeight);
        }

        invalidateParentIfNeeded();
    }

    public void setBackgroundTintList(android.content.res.ColorStateList tint)
    {
        mBackgroundTintList = tint;
        applyBackgroundTint();
    }

    public android.content.res.ColorStateList getBackgroundTintList()
    {
        return mBackgroundTintList;
    }

    public void setBackgroundTintMode(android.graphics.PorterDuff.Mode tintMode)
    {
        mBackgroundTintMode = tintMode;
        applyBackgroundTint();
    }

    public android.graphics.PorterDuff.Mode getBackgroundTintMode()
    {
        return mBackgroundTintMode;
    }

    public void setBackgroundTintBlendMode(android.graphics.BlendMode blendMode)
    {
        mBackgroundTintBlendMode = blendMode;
        applyBackgroundTint();
    }

    public android.graphics.BlendMode getBackgroundTintBlendMode()
    {
        return mBackgroundTintBlendMode;
    }

    private void applyBackgroundTint()
    {
        android.graphics.drawable.Drawable background = mBackground;
        if (background == null && mHasBackground) return;
        if (background == null) return;

        if (mBackgroundTintList != null) background.setTintList(mBackgroundTintList);
        if (mBackgroundTintMode != null) background.setTintMode(mBackgroundTintMode);
        if (mBackgroundTintBlendMode != null) background.setTintBlendMode(mBackgroundTintBlendMode);
        if (background.isStateful()) background.setState(getDrawableState());

        invalidate();
    }

    public android.graphics.drawable.Drawable getForeground()
    {
        return mForeground;
    }

    public void setForeground(android.graphics.drawable.Drawable foreground)
    {
        if (mForeground == foreground) return;

        if (mForeground != null)
        {
            if (mAttached) mForeground.setVisible(false, false);
            mForeground.setCallback(null);
        }

        mForeground = foreground;

        if (foreground != null)
        {
            foreground.setLayoutDirection(getLayoutDirection());
            if (foreground.isStateful()) foreground.setState(getDrawableState());
            foreground.setCallback(this);
            if (mForegroundTintList != null) foreground.setTintList(mForegroundTintList);
            if (mForegroundTintMode != null) foreground.setTintMode(mForegroundTintMode);
            if (mAttached) foreground.setVisible(getWindowVisibility() == VISIBLE && isShown(), false);
        }

        requestLayout();
        invalidate();
    }

    public int getForegroundGravity()
    {
        return mForegroundGravity;
    }

    public void setForegroundGravity(int gravity)
    {
        if (mForegroundGravity == gravity) return;

        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) gravity |= Gravity.START;
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) gravity |= Gravity.TOP;

        mForegroundGravity = gravity;
        requestLayout();
    }

    public void setForegroundTintList(android.content.res.ColorStateList tint)
    {
        mForegroundTintList = tint;
        if (mForeground != null) mForeground.setTintList(tint);
    }

    public android.content.res.ColorStateList getForegroundTintList()
    {
        return mForegroundTintList;
    }

    public void setForegroundTintMode(android.graphics.PorterDuff.Mode tintMode)
    {
        mForegroundTintMode = tintMode;
        if (mForeground != null && tintMode != null) mForeground.setTintMode(tintMode);
    }

    public android.graphics.PorterDuff.Mode getForegroundTintMode()
    {
        return mForegroundTintMode;
    }

    public void onDrawForeground(Canvas canvas)
    {
        android.graphics.drawable.Drawable foreground = mForeground;
        if (foreground == null) return;

        Rect selfBounds = new Rect(0, 0, getWidth(), getHeight());
        if (mForegroundGravity == Gravity.FILL)
        {
            foreground.setBounds(selfBounds);
        }
        else
        {
            Rect overlayBounds = new Rect();
            Gravity.apply(mForegroundGravity, foreground.getIntrinsicWidth(), foreground.getIntrinsicHeight(), selfBounds, overlayBounds,
                          getLayoutDirection());
            foreground.setBounds(overlayBounds);
        }

        foreground.draw(canvas);
    }

    public final <T extends View> T findViewWithTag(Object tag)
    {
        if (tag == null) return null;

        @SuppressWarnings("unchecked")
        T found = (T)findViewWithTagTraversal(tag);
        return found;
    }

    View findViewWithTagTraversal(Object tag)
    {
        if (tag != null && tag.equals(mTag)) return this;
        return null;
    }

    public Animation getAnimation()
    {
        return mAnimation;
    }

    public void startAnimation(Animation animation)
    {
        animation.setStartTime(Animation.START_ON_FIRST_FRAME);
        setAnimation(animation);
        invalidateParentIfNeeded();
        invalidate();
    }

    public void clearAnimation()
    {
        if (mAnimation != null) mAnimation.detach();

        mAnimation = null;
        invalidateParentIfNeeded();
    }

    public void setAnimation(Animation animation)
    {
        mAnimation = animation;
        mAnimationStarted = false;

        if (animation != null) animation.reset();
    }

    protected void onAnimationStart()
    {
        mAnimationStarted = true;
    }

    protected void onAnimationEnd()
    {
        mAnimationStarted = false;
    }

    boolean consumeAnimationStarted()
    {
        boolean started = mAnimationStarted;
        mAnimationStarted = false;
        return started;
    }

    protected boolean onSetAlpha(int alpha)
    {
        return false;
    }

    boolean applyLegacyAnimation(ViewGroup parent, long drawingTime, Animation animation, Transformation transformation)
    {
        if (!animation.isInitialized())
        {
            animation.initialize(getWidth(), getHeight(), parent.getWidth(), parent.getHeight());
            onAnimationStart();
        }

        boolean more = animation.getTransformation(drawingTime, transformation, 1.0f);
        if (more) parent.invalidate();

        return more;
    }

    public void setAnimationMatrix(android.graphics.Matrix matrix)
    {
        mAnimationMatrix = matrix == null ? null : new android.graphics.Matrix(matrix);
        invalidateParentIfNeeded();
        invalidate();
    }

    public android.graphics.Matrix getAnimationMatrix()
    {
        return mAnimationMatrix;
    }

    public android.animation.StateListAnimator getStateListAnimator()
    {
        return mStateListAnimator;
    }

    public void setStateListAnimator(android.animation.StateListAnimator stateListAnimator)
    {
        if (mStateListAnimator == stateListAnimator) return;

        if (mStateListAnimator != null) mStateListAnimator.setTarget(null);

        mStateListAnimator = stateListAnimator;

        if (stateListAnimator != null)
        {
            stateListAnimator.setTarget(this);
            if (mAttached) stateListAnimator.setState(getDrawableState());
        }
    }

    public void setOutlineSpotShadowColor(int color)
    {
        mOutlineSpotShadowColor = color;
        invalidate();
    }

    public int getOutlineSpotShadowColor()
    {
        return mOutlineSpotShadowColor;
    }

    public void setOutlineAmbientShadowColor(int color)
    {
        mOutlineAmbientShadowColor = color;
        invalidate();
    }

    public int getOutlineAmbientShadowColor()
    {
        return mOutlineAmbientShadowColor;
    }

    public float getCameraDistance()
    {
        if (mCameraDistance != 0) return mCameraDistance;

        float densityDpi = mContext.getResources().getDisplayMetrics().densityDpi;
        return 8.0f * (densityDpi > 0 ? densityDpi : 160.0f);
    }

    public void setCameraDistance(float distance)
    {
        mCameraDistance = Math.abs(distance);
        invalidate();
    }

    public void resetPivot()
    {
        if (!mPivotExplicit) return;

        mPivotExplicit = false;
        mMatrixStale = true;
        invalidate();
    }

    public boolean isPivotSet()
    {
        return mPivotExplicit;
    }

    public boolean hasOverlappingRendering()
    {
        return mHasOverlappingRendering;
    }

    public void forceHasOverlappingRendering(boolean hasOverlappingRendering)
    {
        mOverlappingRenderingForced = true;
        mHasOverlappingRendering = hasOverlappingRendering;
    }

    public final boolean getHasOverlappingRendering()
    {
        return mOverlappingRenderingForced ? mHasOverlappingRendering : hasOverlappingRendering();
    }

    public boolean isOpaque()
    {
        if (mAlpha * mTransitionAlpha < 1.0f) return false;
        if (mHasBackground) return (mBackgroundColor >>> 24) == 0xff;
        if (mBackground != null) return mBackground.getOpacity() == android.graphics.PixelFormat.OPAQUE;

        return false;
    }

    public void onScreenStateChanged(int screenState)
    {
    }

    public void onRtlPropertiesChanged(int layoutDirection)
    {
    }

    public boolean isLayoutDirectionResolved()
    {
        return true;
    }

    public boolean canResolveLayoutDirection()
    {
        return true;
    }

    public boolean isTextDirectionResolved()
    {
        return true;
    }

    public boolean canResolveTextDirection()
    {
        return true;
    }

    public boolean isTextAlignmentResolved()
    {
        return true;
    }

    public boolean canResolveTextAlignment()
    {
        return true;
    }

    public int getRawLayoutDirection()
    {
        return mLayoutDirection;
    }

    public void setTextDirection(int textDirection)
    {
        if (mRawTextDirection == textDirection) return;

        mRawTextDirection = textDirection;
        requestLayout();
        invalidate();
    }

    public int getTextDirection()
    {
        if (mRawTextDirection != TEXT_DIRECTION_INHERIT) return mRawTextDirection;
        if (mParent != null) return mParent.getTextDirection();

        return TEXT_DIRECTION_FIRST_STRONG;
    }

    public int getRawTextDirection()
    {
        return mRawTextDirection;
    }

    public void setTextAlignment(int textAlignment)
    {
        if (mRawTextAlignment == textAlignment) return;

        mRawTextAlignment = textAlignment;
        requestLayout();
        invalidate();
    }

    public int getTextAlignment()
    {
        if (mRawTextAlignment != TEXT_ALIGNMENT_INHERIT) return mRawTextAlignment;
        if (mParent != null) return mParent.getTextAlignment();

        return TEXT_ALIGNMENT_GRAVITY;
    }

    public int getRawTextAlignment()
    {
        return mRawTextAlignment;
    }

    protected int getWindowAttachCount()
    {
        return mWindowAttachCount;
    }

    public WindowId getWindowId()
    {
        if (!mAttached) return null;

        View root = getRootView();
        if (root.mWindowId == null) root.mWindowId = new WindowId(root.getWindowToken());
        return root.mWindowId;
    }

    public final boolean isTemporarilyDetached()
    {
        return mTemporarilyDetached;
    }

    public void dispatchStartTemporaryDetach()
    {
        mTemporarilyDetached = true;
        onStartTemporaryDetach();
    }

    public void onStartTemporaryDetach()
    {
        cancelLongPress();
        if (mPressed) setPressed(false);
    }

    public void dispatchFinishTemporaryDetach()
    {
        mTemporarilyDetached = false;
        onFinishTemporaryDetach();
    }

    public void onFinishTemporaryDetach()
    {
    }

    protected void onConfigurationChanged(android.content.res.Configuration newConfig)
    {
    }

    public void dispatchConfigurationChanged(android.content.res.Configuration newConfig)
    {
        onConfigurationChanged(newConfig);
    }

    public void dispatchWindowFocusChanged(boolean hasFocus)
    {
        onWindowFocusChanged(hasFocus);
    }

    public void dispatchWindowVisibilityChanged(int visibility)
    {
        onWindowVisibilityChanged(visibility);
    }

    public void dispatchDisplayHint(int hint)
    {
        onDisplayHint(hint);
    }

    protected void onDisplayHint(int hint)
    {
    }

    public void setScrollCaptureHint(int hint)
    {
        mScrollCaptureHint = hint;
    }

    public int getScrollCaptureHint()
    {
        return mScrollCaptureHint;
    }

    public void setRequestedFrameRate(float frameRate)
    {
        mRequestedFrameRate = frameRate;
    }

    public float getRequestedFrameRate()
    {
        return mRequestedFrameRate;
    }

    public void setFrameContentVelocity(float pixelsPerSecond)
    {
        mFrameContentVelocity = pixelsPerSecond;
    }

    public float getFrameContentVelocity()
    {
        return mFrameContentVelocity;
    }

    public boolean startNestedScroll(int axes)
    {
        if (hasNestedScrollingParent()) return true;
        if (!isNestedScrollingEnabled()) return false;

        ViewParent parent = getParent();
        View child = this;

        while (parent != null)
        {
            if (parent.onStartNestedScroll(child, this, axes))
            {
                mNestedScrollingParent = parent;
                parent.onNestedScrollAccepted(child, this, axes);
                return true;
            }

            if (parent instanceof View) child = (View)parent;
            parent = parent.getParent();
        }

        return false;
    }

    public void stopNestedScroll()
    {
        if (mNestedScrollingParent == null) return;

        ViewParent parent = mNestedScrollingParent;
        mNestedScrollingParent = null;
        parent.onStopNestedScroll(this);
    }

    public boolean hasNestedScrollingParent()
    {
        return mNestedScrollingParent != null;
    }

    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow)
    {
        if (!isNestedScrollingEnabled() || mNestedScrollingParent == null) return false;

        if (dxConsumed != 0 || dyConsumed != 0 || dxUnconsumed != 0 || dyUnconsumed != 0)
        {
            int startX = 0;
            int startY = 0;

            if (offsetInWindow != null)
            {
                getLocationInWindow(offsetInWindow);
                startX = offsetInWindow[0];
                startY = offsetInWindow[1];
            }

            mNestedScrollingParent.onNestedScroll(this, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);

            if (offsetInWindow != null)
            {
                getLocationInWindow(offsetInWindow);
                offsetInWindow[0] -= startX;
                offsetInWindow[1] -= startY;
            }

            return true;
        }

        if (offsetInWindow != null)
        {
            offsetInWindow[0] = 0;
            offsetInWindow[1] = 0;
        }

        return false;
    }

    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow)
    {
        if (!isNestedScrollingEnabled() || mNestedScrollingParent == null) return false;

        if (dx != 0 || dy != 0)
        {
            int startX = 0;
            int startY = 0;

            if (offsetInWindow != null)
            {
                getLocationInWindow(offsetInWindow);
                startX = offsetInWindow[0];
                startY = offsetInWindow[1];
            }

            if (consumed == null)
            {
                if (mTempNestedScrollConsumed == null) mTempNestedScrollConsumed = new int[2];
                consumed = mTempNestedScrollConsumed;
            }

            consumed[0] = 0;
            consumed[1] = 0;
            mNestedScrollingParent.onNestedPreScroll(this, dx, dy, consumed);

            if (offsetInWindow != null)
            {
                getLocationInWindow(offsetInWindow);
                offsetInWindow[0] -= startX;
                offsetInWindow[1] -= startY;
            }

            return consumed[0] != 0 || consumed[1] != 0;
        }

        if (offsetInWindow != null)
        {
            offsetInWindow[0] = 0;
            offsetInWindow[1] = 0;
        }

        return false;
    }

    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed)
    {
        if (!isNestedScrollingEnabled() || mNestedScrollingParent == null) return false;
        return mNestedScrollingParent.onNestedFling(this, velocityX, velocityY, consumed);
    }

    public boolean dispatchNestedPreFling(float velocityX, float velocityY)
    {
        if (!isNestedScrollingEnabled() || mNestedScrollingParent == null) return false;
        return mNestedScrollingParent.onNestedPreFling(this, velocityX, velocityY);
    }

    public void saveHierarchyState(SparseArray<Parcelable> container)
    {
        dispatchSaveInstanceState(container);
    }

    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container)
    {
        if (mId == NO_ID || !mSaveEnabled) return;

        Parcelable state = onSaveInstanceState();
        if (state != null) container.put(mId, state);
    }

    protected Parcelable onSaveInstanceState()
    {
        return BaseSavedState.EMPTY_STATE;
    }

    public void restoreHierarchyState(SparseArray<Parcelable> container)
    {
        dispatchRestoreInstanceState(container);
    }

    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container)
    {
        if (mId == NO_ID) return;

        Parcelable state = container.get(mId);
        if (state != null) onRestoreInstanceState(state);
    }

    protected void onRestoreInstanceState(Parcelable state)
    {
        if (state != null && !(state instanceof AbsSavedState))
        {
            throw new IllegalArgumentException("Wrong state class, expecting View State but received " + state.getClass().toString()
                                               + " instead. This usually happens when two views of different type have the same id"
                                               + " in the same hierarchy. This view's id is 0x" + Integer.toHexString(mId)
                                               + ". Make sure other views do not use the same id.");
        }
    }

    public static class BaseSavedState extends AbsSavedState
    {
        public static final Parcelable.Creator<BaseSavedState> CREATOR = new Parcelable.ClassLoaderCreator<BaseSavedState>()
        {
            @Override
            public BaseSavedState createFromParcel(Parcel source)
            {
                return new BaseSavedState(source);
            }

            @Override
            public BaseSavedState createFromParcel(Parcel source, ClassLoader loader)
            {
                return new BaseSavedState(source, loader);
            }

            @Override
            public BaseSavedState[] newArray(int size)
            {
                return new BaseSavedState[size];
            }
        };

        public BaseSavedState(Parcel source)
        {
            this(source, null);
        }

        public BaseSavedState(Parcel source, ClassLoader loader)
        {
            super(source, loader);
        }

        public BaseSavedState(Parcelable superState)
        {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
        }
    }

    public static class MeasureSpec
    {
        public static final int UNSPECIFIED = 0;
        public static final int EXACTLY = 1 << 30;
        public static final int AT_MOST = 2 << 30;

        private static final int MODE_SHIFT = 30;
        private static final int MODE_MASK = 3 << MODE_SHIFT;

        public static int makeMeasureSpec(int size, int mode)
        {
            return (size & ~MODE_MASK) | (mode & MODE_MASK);
        }

        public static int getMode(int measureSpec)
        {
            return measureSpec & MODE_MASK;
        }

        public static int getSize(int measureSpec)
        {
            return measureSpec & ~MODE_MASK;
        }

        public static String toString(int measureSpec)
        {
            int mode = getMode(measureSpec);
            int size = getSize(measureSpec);

            StringBuilder builder = new StringBuilder("MeasureSpec: ");

            if (mode == UNSPECIFIED) builder.append("UNSPECIFIED ");
            else if (mode == EXACTLY) builder.append("EXACTLY ");
            else if (mode == AT_MOST) builder.append("AT_MOST ");
            else builder.append(mode).append(" ");

            builder.append(size);
            return builder.toString();
        }
    }
}
