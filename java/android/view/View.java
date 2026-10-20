package android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

public class View implements android.graphics.drawable.Drawable.Callback
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
        if (attributes != null) applyViewAttributes(context, attributes, defStyleAttr, defStyleRes);
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

        mPaddingLeft = readDimension(array, styleable, "paddingLeft", mPaddingLeft);
        mPaddingTop = readDimension(array, styleable, "paddingTop", mPaddingTop);
        mPaddingRight = readDimension(array, styleable, "paddingRight", mPaddingRight);
        mPaddingBottom = readDimension(array, styleable, "paddingBottom", mPaddingBottom);
        mMinimumWidth = readDimension(array, styleable, "minWidth", 0);
        mMinimumHeight = readDimension(array, styleable, "minHeight", 0);

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

        array.recycle();
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "id", "background", "padding", "paddingLeft", "paddingTop", "paddingRight",
                                         "paddingBottom", "visibility", "minWidth", "minHeight");
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

        mVisibility = visibility;
        requestLayout();
        invalidate();
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
        out.set(0, 0, getWidth(), getHeight());
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
        boolean changed = left != mLeft || top != mTop || right != mRight || bottom != mBottom;

        int oldWidth = mRight - mLeft;
        int oldHeight = mBottom - mTop;

        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
        mLayoutRequested = false;

        onLayout(changed, left, top, right, bottom);

        int width = right - left;
        int height = bottom - top;

        if (width != oldWidth || height != oldHeight)
        {
            if (mBackground != null) mBackground.setBounds(0, 0, width, height);

            onSizeChanged(width, height, oldWidth, oldHeight);
        }
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
        if (mParent != null) mParent.invalidateChild(this, new Rect(0, 0, getWidth(), getHeight()));
    }

    static final boolean TRACE_DRAW = false;

    public void draw(Canvas canvas)
    {
        if (mVisibility != VISIBLE) return;

        if (TRACE_DRAW)
        {
            android.util.Log.i("View", "draw " + getClass().getName() + " " + getWidth() + "x" + getHeight()
                                       + " alpha=" + mAlpha);
        }

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
            return;
        }

        int saveCount = canvas.save();

        if (mClipBounds != null) canvas.clipRect(mClipBounds);

        canvas.translate(-mScrollX, -mScrollY);
        onDraw(canvas);
        dispatchDraw(canvas);
        canvas.restoreToCount(saveCount);
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
        int x = 0;
        int y = 0;

        View cursor = this;
        for (;;)
        {
            x += cursor.mLeft;
            y += cursor.mTop;

            ViewParent parent = cursor.mParent;
            if (!(parent instanceof View)) break;
            cursor = (View)parent;
        }

        location[0] = x;
        location[1] = y;
    }

    public void getLocationOnScreen(int[] location)
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
        return mRight > mLeft && mBottom > mTop;
    }

    public boolean isInLayout()
    {
        return false;
    }

    public boolean isInEditMode()
    {
        return false;
    }

    public boolean isShown()
    {
        return mVisibility == VISIBLE;
    }

    public void requestApplyInsets()
    {
    }

    public void setPaddingRelative(int start, int top, int end, int bottom)
    {
        setPadding(start, top, end, bottom);
    }

    public int getPaddingStart()
    {
        return mPaddingLeft;
    }

    public int getPaddingEnd()
    {
        return mPaddingRight;
    }

    public int getMeasuredState()
    {
        return (mMeasuredWidth & MEASURED_STATE_MASK)
                | ((mMeasuredHeight >> MEASURED_HEIGHT_STATE_SHIFT) & (MEASURED_STATE_MASK >> MEASURED_HEIGHT_STATE_SHIFT));
    }

    public void setContentDescription(CharSequence description)
    {
    }

    public CharSequence getContentDescription()
    {
        return null;
    }

    public void setSaveFromParentEnabled(boolean enabled)
    {
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
    }

    public void offsetTopAndBottom(int offset)
    {
        mTop += offset;
        mBottom += offset;
    }

    public void offsetLeftAndRight(int offset)
    {
        mLeft += offset;
        mRight += offset;
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
        mSystemUiVisibility = visibility;
    }

    public WindowInsetsController getWindowInsetsController()
    {
        return mInsetsController;
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
    }

    public boolean isClickable()
    {
        return mClickable || mClickListener != null;
    }

    public boolean dispatchTouchEvent(MotionEvent event)
    {
        if (mTouchListener != null && mTouchListener.onTouch(this, event)) return true;
        return onTouchEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        if (TRACE_INPUT && event.getActionMasked() == MotionEvent.ACTION_DOWN)
        {
            android.util.Log.i("View", "the base onTouchEvent ran for " + getClass().getName()
                                           + ", so that class does not override it; touch listener "
                                           + (mTouchListener != null ? "present" : "absent"));
        }

        if (!isClickable() && !mLongClickable) return false;

        int action = event.getActionMasked();

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

        if (wasPressed && !mLongClickFired && containsLocal(event.getX(), event.getY())) performClick();

        return true;
    }

    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (mKeyListener != null && mKeyListener.onKey(this, event.getKeyCode(), event)) return true;

        if (event.getAction() == KeyEvent.ACTION_DOWN) return onKeyDown(event.getKeyCode(), event);
        if (event.getAction() == KeyEvent.ACTION_UP) return onKeyUp(event.getKeyCode(), event);

        return false;
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
        return false;
    }

    public boolean dispatchHoverEvent(MotionEvent event)
    {
        return onHoverEvent(event);
    }

    public boolean dispatchGenericMotionEvent(MotionEvent event)
    {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE
            || action == MotionEvent.ACTION_HOVER_EXIT)
        {
            return dispatchHoverEvent(event);
        }

        return onGenericMotionEvent(event);
    }

    public boolean onGenericMotionEvent(MotionEvent event)
    {
        return false;
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

        if (TRACE_DRAW) android.util.Log.i("View", "attach " + getClass().getName());

        onAttachedToWindow();
        onWindowVisibilityChanged(mVisibility);

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
        onDetachedFromWindow();

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
        public void sendAccessibilityEvent(View host, int eventType)
        {
        }

        public boolean performAccessibilityAction(View host, int action, android.os.Bundle arguments)
        {
            return false;
        }

        public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info)
        {
        }

        public void onInitializeAccessibilityEvent(View host, android.view.accessibility.AccessibilityEvent event)
        {
        }

        public void onPopulateAccessibilityEvent(View host, android.view.accessibility.AccessibilityEvent event)
        {
        }

        public boolean dispatchPopulateAccessibilityEvent(View host, android.view.accessibility.AccessibilityEvent event)
        {
            return false;
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

    public static final int[] EMPTY_STATE_SET = new int[0];

    private static final int STATE_WINDOW_FOCUSED = 0x0101009d;
    private static final int STATE_FOCUSED = 0x0101009c;
    private static final int STATE_ENABLED = 0x0101009e;
    private static final int STATE_SELECTED = 0x010100a1;
    private static final int STATE_PRESSED = 0x010100a7;
    private static final int STATE_ACTIVATED = 0x010102fe;

    public final int[] getDrawableState()
    {
        if (mDrawableState != null && !mDrawableStateStale) return mDrawableState;

        mDrawableState = onCreateDrawableState(0);
        mDrawableStateStale = false;

        return mDrawableState;
    }

    protected int[] onCreateDrawableState(int extraSpace)
    {
        int count = 1;

        if (isEnabled()) count++;
        if (mFocused) count++;
        if (mSelected) count++;
        if (mPressed) count++;
        if (mActivated) count++;

        int[] state = new int[count + extraSpace];
        int index = 0;

        state[index++] = STATE_WINDOW_FOCUSED;
        if (isEnabled()) state[index++] = STATE_ENABLED;
        if (mFocused) state[index++] = STATE_FOCUSED;
        if (mSelected) state[index++] = STATE_SELECTED;
        if (mPressed) state[index++] = STATE_PRESSED;
        if (mActivated) state[index++] = STATE_ACTIVATED;

        return state;
    }

    public void refreshDrawableState()
    {
        mDrawableStateStale = true;
        drawableStateChanged();

        if (mParent instanceof View) ((View)mParent).childDrawableStateChanged(this);
    }

    protected void drawableStateChanged()
    {
        if (mBackground != null && mBackground.isStateful() && mBackground.setState(getDrawableState()))
        {
            invalidate();
        }
    }

    protected void childDrawableStateChanged(View child)
    {
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
        setFocusable(focusable != NOT_FOCUSABLE);
    }

    public void setFocusableInTouchMode(boolean focusable)
    {
        mFocusableInTouchMode = focusable;
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
    }

    public CharSequence getAccessibilityPaneTitle()
    {
        return null;
    }

    public void setScreenReaderFocusable(boolean focusable)
    {
    }

    public void setForceDarkAllowed(boolean allowed)
    {
    }

    public boolean isForceDarkAllowed()
    {
        return false;
    }

    public void setTooltipText(CharSequence text)
    {
    }

    public CharSequence getTooltipText()
    {
        return null;
    }

    public void setNestedScrollingEnabled(boolean enabled)
    {
    }

    public boolean isNestedScrollingEnabled()
    {
        return false;
    }

    public void setHorizontalScrollBarEnabled(boolean enabled)
    {
    }

    public void setVerticalScrollBarEnabled(boolean enabled)
    {
    }

    public void setScrollContainer(boolean isScrollContainer)
    {
    }

    public void setSaveEnabled(boolean enabled)
    {
    }

    public void setDuplicateParentStateEnabled(boolean enabled)
    {
    }

    public void setHasTransientState(boolean hasTransientState)
    {
    }

    public void cancelPendingInputEvents()
    {
    }

    public void setLabelFor(int id)
    {
    }

    public void setNextFocusForwardId(int id)
    {
    }

    public void setTransitionName(String name)
    {
    }

    public String getTransitionName()
    {
        return null;
    }

    public void setKeepScreenOn(boolean keepOn)
    {
    }

    public void setDefaultFocusHighlightEnabled(boolean enabled)
    {
    }

    public boolean getDefaultFocusHighlightEnabled()
    {
        return false;
    }

    public void setFocusedByDefault(boolean focused)
    {
    }

    public void setKeyboardNavigationCluster(boolean cluster)
    {
    }

    public void setRevealOnFocusHint(boolean reveal)
    {
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

    public void setLayerType(int layerType, android.graphics.Paint paint)
    {
    }

    public int getLayerType()
    {
        return LAYER_TYPE_NONE;
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
        if (rect == null) return false;

        rect.set(0, 0, getWidth(), getHeight());
        return getWidth() > 0 && getHeight() > 0;
    }

    public boolean getLocalVisibleRect(android.graphics.Rect rect)
    {
        return getGlobalVisibleRect(rect);
    }

    public void getWindowVisibleDisplayFrame(android.graphics.Rect frame)
    {
        if (frame != null) frame.set(0, 0, getWidth(), getHeight());
    }

    public Display getDisplay()
    {
        return null;
    }

    public boolean performHapticFeedback(int feedbackConstant)
    {
        return false;
    }

    public boolean performHapticFeedback(int feedbackConstant, int flags)
    {
        return false;
    }

    public void playSoundEffect(int soundConstant)
    {
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
        if (mScrollX == value) return;

        mScrollX = value;
        invalidate();
    }

    public void setScrollY(int value)
    {
        if (mScrollY == value) return;

        mScrollY = value;
        invalidate();
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

        mScrollX = x;
        mScrollY = y;
        invalidate();
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
        mTranslationZ = translation;
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

    public void setViewTranslationCallback(android.view.translation.ViewTranslationCallback callback)
    {
    }

    public void clearViewTranslationCallback()
    {
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

    public boolean verifyDrawable(android.graphics.drawable.Drawable who)
    {
        return false;
    }

    public long getUniqueDrawingId()
    {
        if (mUniqueDrawingId == 0) mUniqueDrawingId = ++sNextDrawingId;

        return mUniqueDrawingId;
    }

    public android.view.contentcapture.ContentCaptureSession getContentCaptureSession()
    {
        return null;
    }

    public void setContentCaptureSession(android.view.contentcapture.ContentCaptureSession session)
    {
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
    }

    public void setSelected(boolean selected)
    {
        if (mSelected == selected) return;

        mSelected = selected;
        refreshDrawableState();
        invalidate();
    }

    public boolean isSelected()
    {
        return mSelected;
    }

    public void setPressed(boolean pressed)
    {
        if (mPressed == pressed) return;

        mPressed = pressed;

        if (mBackground != null) mBackground.setHotspot(mLastTouchX, mLastTouchY);

        refreshDrawableState();
        invalidate();
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
        return new android.view.autofill.AutofillId(getId());
    }

    public void setAutofillHints(String... hints)
    {
    }

    public String[] getAutofillHints()
    {
        return null;
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
        if (mFocused) return true;

        ViewRootImpl root = findViewRoot();
        if (root == null) return false;

        root.setFocusedView(this);

        mFocused = true;
        onFocusChanged(true, direction, previouslyFocusedRect);

        if (mFocusChangeListener != null) mFocusChangeListener.onFocusChange(this, true);

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
        invalidate();
    }

    public void postInvalidate()
    {
        invalidate();
    }

    public View findViewById(int id)
    {
        if (id != NO_ID && id == mId) return this;
        return null;
    }

    public interface OnDragListener
    {
        boolean onDrag(View view, Object event);
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
        void onCreateContextMenu(Object menu, View view, Object menuInfo);
    }

    public void setOnDragListener(OnDragListener listener)
    {
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
    }

    public void setOnGenericMotionListener(OnGenericMotionListener listener)
    {
    }

    public void setOnScrollChangeListener(OnScrollChangeListener listener)
    {
    }

    public void setOnCreateContextMenuListener(OnCreateContextMenuListener listener)
    {
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
    }
}
