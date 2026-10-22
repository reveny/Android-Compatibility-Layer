package android.view;

import android.graphics.Canvas;
import android.util.Log;

public final class ViewRootImpl implements Choreographer.FrameCallback, ViewParent, AttachedSurfaceControl
{
    private static final String TAG = "ViewRootImpl";

    private final SurfaceControl mSurfaceControl;
    private final java.util.ArrayList<SurfaceControl.Transaction> mDrawTransactions = new java.util.ArrayList<>();
    private static final java.util.ArrayList<FrameMetricsObserver> sFrameMetricsObservers = new java.util.ArrayList<>();

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

    private static ViewRootImpl sActive;

    private static final class Panel
    {
        final View view;
        final WindowManager.LayoutParams params;
        final android.graphics.Rect frame = new android.graphics.Rect();

        Panel(View view, WindowManager.LayoutParams params)
        {
            this.view = view;
            this.params = params;
        }

        Window.Callback callback()
        {
            if (!(view instanceof DecorView)) return null;

            Window window = ((DecorView)view).getWindow();
            return window == null ? null : window.getCallback();
        }

        boolean visible()
        {
            return view.getVisibility() == View.VISIBLE;
        }
    }

    private final java.util.ArrayList<Panel> mPanels = new java.util.ArrayList<>();
    private Panel mTouchPanel;

    public ViewRootImpl(View root, runtime.WindowBridge window, int frameLimit)
    {
        mRoot = root;
        mWindow = window;
        mWidth = window.getWidth();
        mHeight = window.getHeight();
        mWindowInsets = buildWindowInsets();
        mSurfaceControl = new SurfaceControl.Builder().setName("ViewRoot").setBufferSize(mWidth, mHeight).build();
        mFrameLimit = frameLimit;
        mContinuous = frameLimit > 0 && !"0".equals(runtime.Settings.get("continuous-frames"));

        if (mRoot != null)
        {
            mRoot.assignParent(this);
            mRoot.dispatchAttachedToWindow();
        }

        sActive = this;
    }

    public static ViewRootImpl active()
    {
        return sActive;
    }

    static boolean panelOrigin(View root, int[] origin)
    {
        ViewRootImpl active = sActive;
        if (active == null || root == null) return false;

        for (Panel panel : active.mPanels)
        {
            if (panel.view != root) continue;

            origin[0] = panel.frame.left;
            origin[1] = panel.frame.top;
            return true;
        }

        return false;
    }

    java.util.List<View> windowViews()
    {
        java.util.ArrayList<View> views = new java.util.ArrayList<>();
        if (mRoot != null) views.add(mRoot);

        for (Panel panel : mPanels) views.add(panel.view);

        return views;
    }

    java.util.List<WindowManager.LayoutParams> windowParams()
    {
        java.util.ArrayList<WindowManager.LayoutParams> params = new java.util.ArrayList<>();

        if (mRoot != null)
        {
            ViewGroup.LayoutParams rootParams = mRoot.getLayoutParams();
            params.add(rootParams instanceof WindowManager.LayoutParams ? (WindowManager.LayoutParams)rootParams
                                                                      : new WindowManager.LayoutParams());
        }

        for (Panel panel : mPanels) params.add(panel.params);

        return params;
    }

    public void addPanel(View view, WindowManager.LayoutParams params)
    {
        if (findPanel(view) != null) throw new IllegalStateException("View " + view + " has already been added to the window manager.");

        mPanels.add(new Panel(view, params));
        view.setLayoutParams(params);
        view.assignParent(this);
        view.dispatchAttachedToWindow();

        Log.i(TAG, "a " + view.getClass().getName() + " window of type " + params.type + " was added above the activity, "
                       + mPanels.size() + " in all");

        requestLayout();
    }

    public void updatePanel(View view, WindowManager.LayoutParams params)
    {
        Panel panel = findPanel(view);
        if (panel == null) throw new IllegalArgumentException("View=" + view + " not attached to window manager");

        if (params != panel.params) panel.params.copyFrom(params);
        view.setLayoutParams(panel.params);
        requestLayout();
    }

    public void removePanel(View view)
    {
        Panel panel = findPanel(view);
        if (panel == null) return;

        mPanels.remove(panel);
        if (mTouchPanel == panel) mTouchPanel = null;

        view.dispatchDetachedFromWindow();
        view.assignParent(null);

        if (mFocused != null && isDescendant(view, mFocused)) setFocusedView(null);

        mDrawNeeded = true;
        scheduleTraversal();
    }

    public boolean hasPanel(View view)
    {
        return findPanel(view) != null;
    }

    private Panel findPanel(View view)
    {
        for (Panel panel : mPanels)
        {
            if (panel.view == view) return panel;
        }

        return null;
    }

    private static boolean isDescendant(View ancestor, View view)
    {
        for (ViewParent parent = view == null ? null : view.getParent(); view != null; parent = view.getParent())
        {
            if (view == ancestor) return true;
            view = parent instanceof View ? (View)parent : null;
        }

        return false;
    }

    private void layoutPanels()
    {
        for (Panel panel : new java.util.ArrayList<>(mPanels)) layoutPanel(panel);
    }

    private void layoutPanel(Panel panel)
    {
        WindowManager.LayoutParams params = panel.params;
        boolean fullScreen = (params.flags & (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)) != 0;

        int top = fullScreen ? 0 : mSystemBarTop;
        int bottom = fullScreen ? mHeight : mHeight - mSystemBarBottom;
        android.graphics.Rect container = new android.graphics.Rect(0, top, mWidth, Math.max(top, bottom));

        panel.view.measure(panelSpec(params.width, container.width()), panelSpec(params.height, container.height()));

        int gravity = params.gravity == Gravity.NO_GRAVITY ? Gravity.TOP | Gravity.LEFT : params.gravity;
        gravity = Gravity.getAbsoluteGravity(gravity, View.LAYOUT_DIRECTION_LTR);

        Gravity.apply(gravity, panel.view.getMeasuredWidth(), panel.view.getMeasuredHeight(), container, params.x, params.y,
                      panel.frame);

        panel.view.layout(0, 0, panel.frame.width(), panel.frame.height());
    }

    private static int panelSpec(int size, int available)
    {
        if (size == ViewGroup.LayoutParams.MATCH_PARENT) return View.MeasureSpec.makeMeasureSpec(available, View.MeasureSpec.EXACTLY);
        if (size == ViewGroup.LayoutParams.WRAP_CONTENT) return View.MeasureSpec.makeMeasureSpec(available, View.MeasureSpec.AT_MOST);

        return View.MeasureSpec.makeMeasureSpec(Math.max(size, 0), View.MeasureSpec.EXACTLY);
    }

    private void drawPanels(Canvas canvas)
    {
        android.graphics.Paint dim = null;

        for (Panel panel : new java.util.ArrayList<>(mPanels))
        {
            if (!panel.visible()) continue;

            if ((panel.params.flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0 && panel.params.dimAmount > 0f)
            {
                if (dim == null) dim = new android.graphics.Paint();

                int alpha = Math.round(Math.min(1f, panel.params.dimAmount) * 255f);
                dim.setColor(android.graphics.Color.argb(alpha, 0, 0, 0));
                canvas.drawRect(0, 0, mWidth, mHeight, dim);
            }

            int saved = canvas.save();
            canvas.translate(panel.frame.left, panel.frame.top);
            panel.view.draw(canvas);
            canvas.restoreToCount(saved);
        }
    }

    public boolean dispatchPanelTouch(MotionEvent event)
    {
        if (mPanels.isEmpty())
        {
            mTouchPanel = null;
            return false;
        }

        int action = event.getActionMasked();
        int x = (int)event.getX();
        int y = (int)event.getY();

        if (action == MotionEvent.ACTION_DOWN)
        {
            mTouchPanel = null;

            for (int index = mPanels.size() - 1; index >= 0; index--)
            {
                Panel panel = mPanels.get(index);
                int flags = panel.params.flags;

                if (!panel.visible() || (flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0) continue;

                boolean inside = panel.frame.contains(x, y);
                boolean modal = (flags & WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) == 0 &&
                                (flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0;

                if (inside || modal)
                {
                    mTouchPanel = panel;
                    break;
                }

                if ((flags & WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH) != 0)
                {
                    MotionEvent outside = MotionEvent.obtain(event);
                    outside.setAction(MotionEvent.ACTION_OUTSIDE);
                    deliverToPanel(panel, outside);
                }
            }
        }

        Panel target = mTouchPanel;
        if (target == null || !mPanels.contains(target)) return false;

        deliverToPanel(target, MotionEvent.obtain(event));

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) mTouchPanel = null;
        return true;
    }

    private static void deliverToPanel(Panel panel, MotionEvent event)
    {
        event.offsetLocation(-panel.frame.left, -panel.frame.top);

        Window.Callback callback = panel.callback();
        if (callback != null) callback.dispatchTouchEvent(event);
        else panel.view.dispatchTouchEvent(event);
    }

    public boolean dispatchPanelKey(KeyEvent event)
    {
        for (int index = mPanels.size() - 1; index >= 0; index--)
        {
            Panel panel = mPanels.get(index);

            if (!panel.visible() || (panel.params.flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0) continue;

            Window.Callback callback = panel.callback();
            if (callback != null) callback.dispatchKeyEvent(event);
            else panel.view.dispatchKeyEvent(event);

            return true;
        }

        return false;
    }

    public void setSystemBarInsets(int top, int bottom)
    {
        if (top == mSystemBarTop && bottom == mSystemBarBottom) return;

        mSystemBarTop = Math.max(top, 0);
        mSystemBarBottom = Math.max(bottom, 0);

        mWindowInsets = buildWindowInsets();

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

    private WindowInsets buildWindowInsets()
    {
        android.graphics.Insets status = android.graphics.Insets.of(0, mSystemBarTop, 0, 0);
        android.graphics.Insets navigation = android.graphics.Insets.of(0, 0, 0, mSystemBarBottom);
        android.graphics.Insets bars = android.graphics.Insets.of(0, mSystemBarTop, 0, mSystemBarBottom);

        return new WindowInsets.Builder()
                   .setInsets(WindowInsets.Type.statusBars(), status)
                   .setInsetsIgnoringVisibility(WindowInsets.Type.statusBars(), status)
                   .setInsets(WindowInsets.Type.navigationBars(), navigation)
                   .setInsetsIgnoringVisibility(WindowInsets.Type.navigationBars(), navigation)
                   .setInsets(WindowInsets.Type.tappableElement(), bars)
                   .setInsetsIgnoringVisibility(WindowInsets.Type.tappableElement(), bars)
                   .setVisible(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars(), true)
                   .setVisible(WindowInsets.Type.ime(), false)
                   .setFrame(mWidth, mHeight)
                   .setDisplayShape(DisplayShape.rectangle(mWidth, mHeight))
                   .build();
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
        mWindowInsets = buildWindowInsets();
        mInsetsDispatched = false;
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
    public SurfaceControl.Transaction buildReparentTransaction(SurfaceControl child)
    {
        if (child == null) throw new IllegalArgumentException("child must not be null");

        return new SurfaceControl.Transaction().reparent(child, mSurfaceControl);
    }

    @Override
    public boolean applyTransactionOnDraw(SurfaceControl.Transaction transaction)
    {
        if (transaction == null) return false;

        synchronized (mDrawTransactions)
        {
            mDrawTransactions.add(transaction);
        }

        mDrawNeeded = true;
        scheduleTraversal();
        return true;
    }

    private void applyDrawTransactions()
    {
        SurfaceControl.Transaction[] pending;

        synchronized (mDrawTransactions)
        {
            if (mDrawTransactions.isEmpty()) return;

            pending = mDrawTransactions.toArray(new SurfaceControl.Transaction[0]);
            mDrawTransactions.clear();
        }

        for (SurfaceControl.Transaction transaction : pending)
        {
            transaction.apply();
        }
    }

    static final class FrameMetricsObserver
    {
        final Window.OnFrameMetricsAvailableListener listener;
        final android.os.Handler handler;
        final Window window;
        int dropped;

        FrameMetricsObserver(Window window, Window.OnFrameMetricsAvailableListener listener, android.os.Handler handler)
        {
            this.window = window;
            this.listener = listener;
            this.handler = handler;
        }
    }

    static void addFrameMetricsObserver(FrameMetricsObserver observer)
    {
        synchronized (sFrameMetricsObservers)
        {
            sFrameMetricsObservers.add(observer);
        }
    }

    static boolean removeFrameMetricsObserver(Window.OnFrameMetricsAvailableListener listener)
    {
        synchronized (sFrameMetricsObservers)
        {
            for (int index = 0; index < sFrameMetricsObservers.size(); index++)
            {
                if (sFrameMetricsObservers.get(index).listener != listener) continue;

                sFrameMetricsObservers.remove(index);
                return true;
            }
        }

        return false;
    }

    private void reportFrameMetrics(long vsyncNanos, long startNanos, long endNanos)
    {
        FrameMetricsObserver[] observers;

        synchronized (sFrameMetricsObservers)
        {
            if (sFrameMetricsObservers.isEmpty()) return;

            observers = sFrameMetricsObservers.toArray(new FrameMetricsObserver[0]);
        }

        for (FrameMetricsObserver observer : observers)
        {
            FrameMetrics metrics = FrameMetrics.forFrame(vsyncNanos, startNanos, endNanos, mFramesDrawn == 1);
            int dropped = observer.dropped;
            observer.dropped = 0;

            Runnable delivery = () -> observer.listener.onFrameMetricsAvailable(observer.window, metrics, dropped);
            if (observer.handler == null || !observer.handler.post(delivery)) observer.dropped++;
        }
    }

    @Override
    public void doFrame(long frameTimeNanos)
    {
        mTraversalScheduled = false;

        if (mContinuous)
        {
            mDrawNeeded = true;
            mLayoutNeeded = true;
        }

        long startNanos = System.nanoTime();
        int framesBefore = mFramesDrawn;

        boolean drew = performTraversal();

        if (mFramesDrawn != framesBefore)
        {
            applyDrawTransactions();
            reportFrameMetrics(frameTimeNanos, startNanos, System.nanoTime());
        }

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

            layoutPanels();
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
            drawPanels(canvas);
            mWindow.endFrame();
            mFramesDrawn++;

            if (View.TRACE_DRAW)
            {
                Log.i(TAG, "frame " + mFramesDrawn + " issued "
                           + (android.graphics.Canvas.sDrawOperations - before) + " shape operations");
            }
        }

        if (runtime.Strict.isTracing("views"))
        {
            int views = countViews(mRoot);
            if (views != mDumpedViewCount)
            {
                mDumpedViewCount = views;
                Log.i(TAG, "frame " + mFramesDrawn + " shows " + views + " view(s)");
                dumpHierarchy(mRoot, 0);
            }
        }

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
            if (((SurfaceView)view).presentsThroughFramework()) return SURFACE_ABSENT;

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

    private int mDumpedViewCount = -1;

    private static int countViews(View view)
    {
        if (!(view instanceof ViewGroup)) return 1;

        ViewGroup group = (ViewGroup)view;
        int total = 1;
        for (int index = 0; index < group.getChildCount(); index++) total += countViews(group.getChildAt(index));

        return total;
    }

    private static void dumpHierarchy(View view, int depth)
    {
        StringBuilder indent = new StringBuilder();
        for (int level = 0; level < depth; level++)
        {
            indent.append("  ");
        }

        String detail = "";
        if (view.getVisibility() != View.VISIBLE) detail += " visibility=" + view.getVisibility();
        if (view instanceof android.widget.TextView)
        {
            android.widget.TextView text = (android.widget.TextView)view;
            detail += " text=\"" + text.getText() + "\" color=#" + Integer.toHexString(text.getCurrentTextColor()) + " size=" +
                      text.getTextSize();
        }

        Log.i(TAG, indent + view.getClass().getName() + " id=0x" + Integer.toHexString(view.getId()) + " bounds=" + view.getLeft() + ","
                       + view.getTop() + "-" + view.getRight() + "," + view.getBottom() + detail);

        if (!(view instanceof ViewGroup)) return;

        ViewGroup group = (ViewGroup)view;
        for (int index = 0; index < group.getChildCount(); index++)
        {
            dumpHierarchy(group.getChildAt(index), depth + 1);
        }
    }
}
