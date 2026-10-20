package android.view;

import android.graphics.Canvas;
import android.util.Log;

public final class ViewRootImpl implements Choreographer.FrameCallback, ViewParent
{
    private static final String TAG = "ViewRootImpl";

    private View mRoot;
    private final runtime.WindowBridge mWindow;
    private int mWidth;
    private int mHeight;
    private final int mFrameLimit;
    private final boolean mContinuous;

    private int mSystemBarTop;
    private int mSystemBarBottom;
    private WindowInsets mWindowInsets = WindowInsets.empty();
    private boolean mInsetsDispatched;

    private int mFramesDrawn;
    private boolean mTraversalScheduled;
    private boolean mDrawNeeded = true;
    private boolean mLayoutNeeded = true;
    private int mLaidOutWidth = -1;
    private int mLaidOutHeight = -1;
    private Runnable mFirstFrameCallback;
    private boolean mGuestPresents;
    private boolean mReportedIdle;
    private View mFocused;

    public ViewRootImpl(View root, runtime.WindowBridge window, int frameLimit)
    {
        mRoot = root;
        mWindow = window;
        mWidth = window.getWidth();
        mHeight = window.getHeight();
        mFrameLimit = frameLimit;
        mContinuous = frameLimit > 0 && !"0".equals(runtime.Settings.get("continuous-frames"));

        if (mRoot != null)
        {
            mRoot.assignParent(this);
            mRoot.dispatchAttachedToWindow();
        }
    }

    public void setSystemBarInsets(int top, int bottom)
    {
        if (top == mSystemBarTop && bottom == mSystemBarBottom) return;

        mSystemBarTop = Math.max(top, 0);
        mSystemBarBottom = Math.max(bottom, 0);

        mWindowInsets = new WindowInsets.Builder()
                                .setSystemWindowInsets(android.graphics.Insets.of(0, mSystemBarTop, 0, mSystemBarBottom))
                                .build();

        mInsetsDispatched = false;
        mLayoutNeeded = true;
        mDrawNeeded = true;
        scheduleTraversal();

        Log.i(TAG, "the system bars take " + mSystemBarTop + "px at the top and " + mSystemBarBottom + "px at the bottom");
    }

    public WindowInsets getWindowInsets()
    {
        return mWindowInsets;
    }

    private void dispatchInsets()
    {
        if (mInsetsDispatched || mRoot == null) return;

        mInsetsDispatched = true;
        mRoot.dispatchApplyWindowInsets(mWindowInsets);
    }

    public void onWindowResized(int width, int height)
    {
        if (width <= 0 || height <= 0) return;
        if (width == mWidth && height == mHeight) return;

        Log.i(TAG, "the window became " + width + "x" + height + ", so everything is laid out again");

        mWidth = width;
        mHeight = height;
        mLaidOutWidth = -1;
        mLaidOutHeight = -1;
        mLayoutNeeded = true;
        mDrawNeeded = true;
        mReportedIdle = false;

        scheduleTraversal();
    }

    public int getWindowWidth()
    {
        return mWidth;
    }

    public int getWindowHeight()
    {
        return mHeight;
    }

    public void setContentRoot(View root)
    {
        if (root == null || root == mRoot) return;

        if (mRoot != null)
        {
            mRoot.dispatchDetachedFromWindow();
            mRoot.assignParent(null);
        }

        mRoot = root;
        mRoot.assignParent(this);
        mRoot.dispatchAttachedToWindow();
        mLaidOutWidth = -1;
        mLaidOutHeight = -1;
        mLayoutNeeded = true;
        mDrawNeeded = true;
        scheduleTraversal();
    }

    @Override
    public void requestLayout()
    {
        mLayoutNeeded = true;
        mDrawNeeded = true;
        scheduleTraversal();
    }

    @Override
    public void invalidateChild(View child, android.graphics.Rect dirty)
    {
        mDrawNeeded = true;
        scheduleTraversal();
    }

    @Override
    public void onDescendantInvalidated(View child, View target)
    {
        mDrawNeeded = true;
        scheduleTraversal();
    }

    @Override
    public ViewParent getParent()
    {
        return null;
    }

    public View getFocusedView()
    {
        return mFocused;
    }

    public void setFocusedView(View view)
    {
        if (mFocused == view) return;

        View previous = mFocused;
        mFocused = view;

        if (previous != null) previous.onFocusLost();

        android.view.inputmethod.InputMethodManager input =
            android.view.inputmethod.InputMethodManager.getInstance();

        if (view != null && view.onCheckIsTextEditor()) input.startInput(view);
        else if (previous != null && input.getServedView() == previous) input.finishInput();
    }

    public boolean dispatchKeyToFocus(KeyEvent event)
    {
        if (android.view.inputmethod.InputMethodManager.getInstance().dispatchKeyEvent(event)) return true;

        return mFocused != null && mFocused.dispatchKeyEvent(event);
    }

    public int getFramesDrawn()
    {
        return mFramesDrawn;
    }

    public void setFirstFrameCallback(Runnable callback)
    {
        mFirstFrameCallback = callback;
    }

    public void start()
    {
        doFrame(android.os.SystemClock.uptimeNanos());
    }

    public void scheduleTraversal()
    {
        if (mTraversalScheduled) return;

        mTraversalScheduled = true;
        Choreographer.getInstance().postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversal, this);
    }

    private final Runnable mTraversal = new Runnable()
    {
        @Override
        public void run()
        {
            doFrame(Choreographer.getInstance().getFrameTimeNanos());
        }
    };

    @Override
    public void doFrame(long frameTimeNanos)
    {
        mTraversalScheduled = false;

        if (mContinuous)
        {
            mDrawNeeded = true;
            mLayoutNeeded = true;
        }

        boolean drew = performTraversal();

        if (mWindow.isCloseRequested())
        {
            Log.i(TAG, "compositor requested close after " + mFramesDrawn + " frames");
            android.os.Looper.quitMainLooper();
            return;
        }

        if (mFrameLimit > 0 && mFramesDrawn >= mFrameLimit)
        {
            Log.i(TAG, "frame limit reached: " + mFramesDrawn);
            android.os.Looper.quitMainLooper();
            return;
        }

        if (mContinuous || mDrawNeeded || mLayoutNeeded)
        {
            mReportedIdle = false;
            scheduleTraversal();
            return;
        }

        if (drew || !mReportedIdle)
        {
            mReportedIdle = true;
            Log.i(TAG, "nothing is dirty after " + mFramesDrawn + " frames, so the next frame waits for an invalidate");
        }
    }

    public boolean performTraversal()
    {
        if (!mDrawNeeded && !mLayoutNeeded) return false;

        if (mLayoutNeeded)
        {
            mLayoutNeeded = false;

            dispatchInsets();

            int widthSpec = View.MeasureSpec.makeMeasureSpec(mWidth, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(mHeight, View.MeasureSpec.EXACTLY);

            mRoot.measure(widthSpec, heightSpec);
            mRoot.layout(0, 0, mRoot.getMeasuredWidth(), mRoot.getMeasuredHeight());

            if (mRoot.getWidth() != mLaidOutWidth || mRoot.getHeight() != mLaidOutHeight)
            {
                mLaidOutWidth = mRoot.getWidth();
                mLaidOutHeight = mRoot.getHeight();
                mRoot.getViewTreeObserver().dispatchOnGlobalLayout();
            }
        }

        mDrawNeeded = false;

        int surface = surfaceState(mRoot);

        if (surface == SURFACE_READY)
        {
            if (!mGuestPresents)
            {
                mGuestPresents = true;
                mWindow.setGuestPresents(true);
                Log.i(TAG, "a surface view owns the window, so its producer presents it from here on");
            }

            mFramesDrawn++;
        }
        else
        {
            if (mGuestPresents)
            {
                mGuestPresents = false;
                mWindow.setGuestPresents(false);
                Log.i(TAG, "the surface view that owned the window is gone, so the framework presents it again");
            }

            Canvas canvas = mWindow.beginFrame();
            if (canvas == null)
            {
                Log.w(TAG, "no buffer available for this frame");
                mDrawNeeded = true;
                return false;
            }

            int before = android.graphics.Canvas.sDrawOperations;

            mRoot.draw(canvas);
            mWindow.endFrame();
            mFramesDrawn++;

            if (View.TRACE_DRAW)
            {
                Log.i(TAG, "frame " + mFramesDrawn + " issued "
                           + (android.graphics.Canvas.sDrawOperations - before) + " shape operations");
            }
        }

        if (mFramesDrawn == 1 && runtime.Strict.isTracing("views")) dumpHierarchy(mRoot, 0);

        if (mFirstFrameCallback != null && surface != SURFACE_PENDING)
        {
            Runnable callback = mFirstFrameCallback;
            mFirstFrameCallback = null;
            callback.run();
        }

        return true;
    }

    private static final int SURFACE_ABSENT = 0;
    private static final int SURFACE_PENDING = 1;
    private static final int SURFACE_READY = 2;

    private static int surfaceState(View view)
    {
        if (view instanceof SurfaceView)
        {
            Surface surface = ((SurfaceView)view).getHolder().getSurface();
            return surface != null && surface.isValid() ? SURFACE_READY : SURFACE_PENDING;
        }

        int state = SURFACE_ABSENT;

        if (view instanceof ViewGroup)
        {
            ViewGroup group = (ViewGroup)view;
            for (int index = 0; index < group.getChildCount(); ++index)
            {
                int child = surfaceState(group.getChildAt(index));
                if (child == SURFACE_PENDING) return SURFACE_PENDING;
                if (child == SURFACE_READY) state = SURFACE_READY;
            }
        }

        return state;
    }

    private static void dumpHierarchy(View view, int depth)
    {
        StringBuilder indent = new StringBuilder();
        for (int level = 0; level < depth; level++)
        {
            indent.append("  ");
        }

        Log.i(TAG, indent + view.getClass().getName() + " id=0x" + Integer.toHexString(view.getId()) + " bounds=" + view.getLeft() + ","
                       + view.getTop() + "-" + view.getRight() + "," + view.getBottom());

        if (!(view instanceof ViewGroup)) return;

        ViewGroup group = (ViewGroup)view;
        for (int index = 0; index < group.getChildCount(); index++)
        {
            dumpHierarchy(group.getChildAt(index), depth + 1);
        }
    }
}
