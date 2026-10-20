package android.view;

import java.util.ArrayList;

public final class ViewTreeObserver
{
    private final ArrayList<OnGlobalLayoutListener> mGlobalLayout = new ArrayList<>();
    private final ArrayList<OnPreDrawListener> mPreDraw = new ArrayList<>();
    private final ArrayList<OnDrawListener> mDraw = new ArrayList<>();
    private final ArrayList<OnScrollChangedListener> mScrollChanged = new ArrayList<>();
    private boolean mAlive = true;

    public boolean isAlive()
    {
        return mAlive;
    }

    public void kill()
    {
        mAlive = false;
    }

    public void addOnGlobalLayoutListener(OnGlobalLayoutListener listener)
    {
        if (listener != null) mGlobalLayout.add(listener);
    }

    public void removeOnGlobalLayoutListener(OnGlobalLayoutListener listener)
    {
        mGlobalLayout.remove(listener);
    }

    public void removeGlobalOnLayoutListener(OnGlobalLayoutListener listener)
    {
        removeOnGlobalLayoutListener(listener);
    }

    public void addOnPreDrawListener(OnPreDrawListener listener)
    {
        if (listener != null) mPreDraw.add(listener);
    }

    public void removeOnPreDrawListener(OnPreDrawListener listener)
    {
        mPreDraw.remove(listener);
    }

    public void addOnDrawListener(OnDrawListener listener)
    {
        if (listener != null) mDraw.add(listener);
    }

    public void removeOnDrawListener(OnDrawListener listener)
    {
        mDraw.remove(listener);
    }

    public void addOnScrollChangedListener(OnScrollChangedListener listener)
    {
        if (listener != null) mScrollChanged.add(listener);
    }

    public void removeOnScrollChangedListener(OnScrollChangedListener listener)
    {
        mScrollChanged.remove(listener);
    }

    public void addOnWindowFocusChangeListener(OnWindowFocusChangeListener listener)
    {
    }

    public void removeOnWindowFocusChangeListener(OnWindowFocusChangeListener listener)
    {
    }

    public void addOnWindowAttachListener(OnWindowAttachListener listener)
    {
    }

    public void removeOnWindowAttachListener(OnWindowAttachListener listener)
    {
    }

    public void addOnTouchModeChangeListener(OnTouchModeChangeListener listener)
    {
    }

    public void removeOnTouchModeChangeListener(OnTouchModeChangeListener listener)
    {
    }

    public void addOnGlobalFocusChangeListener(OnGlobalFocusChangeListener listener)
    {
    }

    public void removeOnGlobalFocusChangeListener(OnGlobalFocusChangeListener listener)
    {
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

    public interface OnGlobalFocusChangeListener
    {
        void onGlobalFocusChanged(View oldFocus, View newFocus);
    }

    public interface OnTouchModeChangeListener
    {
        void onTouchModeChanged(boolean isInTouchMode);
    }
}
