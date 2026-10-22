package android.view;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ViewTreeObserver
{
    private final ArrayList<OnGlobalLayoutListener> mGlobalLayout = new ArrayList<>();
    private final ArrayList<OnPreDrawListener> mPreDraw = new ArrayList<>();
    private final ArrayList<OnDrawListener> mDraw = new ArrayList<>();
    private final ArrayList<OnScrollChangedListener> mScrollChanged = new ArrayList<>();
    private final ArrayList<OnWindowFocusChangeListener> mWindowFocus = new ArrayList<>();
    private final ArrayList<OnWindowAttachListener> mWindowAttach = new ArrayList<>();
    private final ArrayList<OnWindowVisibilityChangeListener> mWindowVisibility = new ArrayList<>();
    private final ArrayList<OnTouchModeChangeListener> mTouchMode = new ArrayList<>();
    private final ArrayList<OnGlobalFocusChangeListener> mGlobalFocus = new ArrayList<>();
    private final ArrayList<Consumer<List<Rect>>> mGestureExclusion = new ArrayList<>();
    private final ArrayList<Runnable> mFrameCommit = new ArrayList<>();
    private boolean mAlive = true;

    ViewTreeObserver()
    {
    }

    public boolean isAlive()
    {
        return mAlive;
    }

    void kill()
    {
        mAlive = false;
    }

    private void checkIsAlive()
    {
        if (!mAlive) throw new IllegalStateException("This ViewTreeObserver is not alive, call getViewTreeObserver() again");
    }

    void merge(ViewTreeObserver observer)
    {
        if (observer == null || observer == this) return;

        mGlobalLayout.addAll(observer.mGlobalLayout);
        mPreDraw.addAll(observer.mPreDraw);
        mDraw.addAll(observer.mDraw);
        mScrollChanged.addAll(observer.mScrollChanged);
        mWindowFocus.addAll(observer.mWindowFocus);
        mWindowAttach.addAll(observer.mWindowAttach);
        mWindowVisibility.addAll(observer.mWindowVisibility);
        mTouchMode.addAll(observer.mTouchMode);
        mGlobalFocus.addAll(observer.mGlobalFocus);
        mGestureExclusion.addAll(observer.mGestureExclusion);
        mFrameCommit.addAll(observer.mFrameCommit);

        observer.kill();
    }

    public void addOnGlobalLayoutListener(OnGlobalLayoutListener listener)
    {
        checkIsAlive();
        if (listener != null) mGlobalLayout.add(listener);
    }

    public void removeOnGlobalLayoutListener(OnGlobalLayoutListener listener)
    {
        checkIsAlive();
        mGlobalLayout.remove(listener);
    }

    @Deprecated
    public void removeGlobalOnLayoutListener(OnGlobalLayoutListener listener)
    {
        removeOnGlobalLayoutListener(listener);
    }

    public void addOnPreDrawListener(OnPreDrawListener listener)
    {
        checkIsAlive();
        if (listener != null) mPreDraw.add(listener);
    }

    public void removeOnPreDrawListener(OnPreDrawListener listener)
    {
        checkIsAlive();
        mPreDraw.remove(listener);
    }

    public void addOnDrawListener(OnDrawListener listener)
    {
        checkIsAlive();
        if (listener != null) mDraw.add(listener);
    }

    public void removeOnDrawListener(OnDrawListener listener)
    {
        checkIsAlive();
        mDraw.remove(listener);
    }

    public void registerFrameCommitCallback(Runnable callback)
    {
        checkIsAlive();
        if (callback != null) mFrameCommit.add(callback);
    }

    public boolean unregisterFrameCommitCallback(Runnable callback)
    {
        checkIsAlive();
        return mFrameCommit.remove(callback);
    }

    public void addOnScrollChangedListener(OnScrollChangedListener listener)
    {
        checkIsAlive();
        if (listener != null) mScrollChanged.add(listener);
    }

    public void removeOnScrollChangedListener(OnScrollChangedListener listener)
    {
        checkIsAlive();
        mScrollChanged.remove(listener);
    }

    public void addOnWindowFocusChangeListener(OnWindowFocusChangeListener listener)
    {
        checkIsAlive();
        if (listener != null) mWindowFocus.add(listener);
    }

    public void removeOnWindowFocusChangeListener(OnWindowFocusChangeListener listener)
    {
        checkIsAlive();
        mWindowFocus.remove(listener);
    }

    public void addOnWindowAttachListener(OnWindowAttachListener listener)
    {
        checkIsAlive();
        if (listener != null) mWindowAttach.add(listener);
    }

    public void removeOnWindowAttachListener(OnWindowAttachListener listener)
    {
        checkIsAlive();
        mWindowAttach.remove(listener);
    }

    public void addOnWindowVisibilityChangeListener(OnWindowVisibilityChangeListener listener)
    {
        checkIsAlive();
        if (listener != null) mWindowVisibility.add(listener);
    }

    public void removeOnWindowVisibilityChangeListener(OnWindowVisibilityChangeListener listener)
    {
        checkIsAlive();
        mWindowVisibility.remove(listener);
    }

    public void addOnTouchModeChangeListener(OnTouchModeChangeListener listener)
    {
        checkIsAlive();
        if (listener != null) mTouchMode.add(listener);
    }

    public void removeOnTouchModeChangeListener(OnTouchModeChangeListener listener)
    {
        checkIsAlive();
        mTouchMode.remove(listener);
    }

    public void addOnGlobalFocusChangeListener(OnGlobalFocusChangeListener listener)
    {
        checkIsAlive();
        if (listener != null) mGlobalFocus.add(listener);
    }

    public void removeOnGlobalFocusChangeListener(OnGlobalFocusChangeListener listener)
    {
        checkIsAlive();
        mGlobalFocus.remove(listener);
    }

    public void addOnSystemGestureExclusionRectsChangedListener(Consumer<List<Rect>> listener)
    {
        checkIsAlive();
        if (listener != null) mGestureExclusion.add(listener);
    }

    public void removeOnSystemGestureExclusionRectsChangedListener(Consumer<List<Rect>> listener)
    {
        checkIsAlive();
        mGestureExclusion.remove(listener);
    }

    public void dispatchOnGlobalLayout()
    {
        for (OnGlobalLayoutListener listener : new ArrayList<>(mGlobalLayout))
        {
            listener.onGlobalLayout();
        }
    }

    public boolean dispatchOnPreDraw()
    {
        boolean cancelDraw = false;
        for (OnPreDrawListener listener : new ArrayList<>(mPreDraw))
        {
            cancelDraw |= !listener.onPreDraw();
        }
        return cancelDraw;
    }

    public void dispatchOnDraw()
    {
        for (OnDrawListener listener : new ArrayList<>(mDraw))
        {
            listener.onDraw();
        }
    }

    void dispatchOnFrameCommit()
    {
        if (mFrameCommit.isEmpty()) return;

        ArrayList<Runnable> callbacks = new ArrayList<>(mFrameCommit);
        mFrameCommit.clear();

        for (Runnable callback : callbacks)
        {
            callback.run();
        }
    }

    void dispatchOnScrollChanged()
    {
        for (OnScrollChangedListener listener : new ArrayList<>(mScrollChanged))
        {
            listener.onScrollChanged();
        }
    }

    void dispatchOnWindowFocusChange(boolean hasFocus)
    {
        for (OnWindowFocusChangeListener listener : new ArrayList<>(mWindowFocus))
        {
            listener.onWindowFocusChanged(hasFocus);
        }
    }

    void dispatchOnWindowAttachedChange(boolean attached)
    {
        for (OnWindowAttachListener listener : new ArrayList<>(mWindowAttach))
        {
            if (attached) listener.onWindowAttached();
            else listener.onWindowDetached();
        }
    }

    void dispatchOnWindowVisibilityChange(int visibility)
    {
        for (OnWindowVisibilityChangeListener listener : new ArrayList<>(mWindowVisibility))
        {
            listener.onWindowVisibilityChanged(visibility);
        }
    }

    void dispatchOnTouchModeChanged(boolean inTouchMode)
    {
        for (OnTouchModeChangeListener listener : new ArrayList<>(mTouchMode))
        {
            listener.onTouchModeChanged(inTouchMode);
        }
    }

    void dispatchOnGlobalFocusChange(View oldFocus, View newFocus)
    {
        for (OnGlobalFocusChangeListener listener : new ArrayList<>(mGlobalFocus))
        {
            listener.onGlobalFocusChanged(oldFocus, newFocus);
        }
    }

    void dispatchOnSystemGestureExclusionRectsChanged(List<Rect> rects)
    {
        for (Consumer<List<Rect>> listener : new ArrayList<>(mGestureExclusion))
        {
            listener.accept(rects);
        }
    }

    boolean hasOnPreDrawListeners()
    {
        return !mPreDraw.isEmpty();
    }

    public interface OnGlobalLayoutListener
    {
        void onGlobalLayout();
    }

    public interface OnPreDrawListener
    {
        boolean onPreDraw();
    }

    public interface OnDrawListener
    {
        void onDraw();
    }

    public interface OnScrollChangedListener
    {
        void onScrollChanged();
    }

    public interface OnWindowFocusChangeListener
    {
        void onWindowFocusChanged(boolean hasFocus);
    }

    public interface OnWindowAttachListener
    {
        void onWindowAttached();

        void onWindowDetached();
    }

    public interface OnWindowVisibilityChangeListener
    {
        void onWindowVisibilityChanged(int visibility);
    }

    public interface OnGlobalFocusChangeListener
    {
        void onGlobalFocusChanged(View oldFocus, View newFocus);
    }

    public interface OnTouchModeChangeListener
    {
        void onTouchModeChanged(boolean isInTouchMode);
    }
}
