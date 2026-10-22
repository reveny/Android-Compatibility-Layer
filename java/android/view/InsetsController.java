package android.view;

import android.os.CancellationSignal;
import android.view.animation.Interpolator;

import java.util.ArrayList;

public final class InsetsController implements WindowInsetsController
{
    private int mAppearance;
    private int mBehavior = BEHAVIOR_DEFAULT;
    private final ArrayList<OnControllableInsetsChangedListener> mControllableListeners = new ArrayList<>();

    @Override
    public void setSystemBarsAppearance(int appearance, int mask)
    {
        mAppearance = (mAppearance & ~mask) | (appearance & mask);
    }

    @Override
    public int getSystemBarsAppearance()
    {
        return mAppearance;
    }

    @Override
    public void setSystemBarsBehavior(int behavior)
    {
        mBehavior = behavior;
    }

    @Override
    public int getSystemBarsBehavior()
    {
        return mBehavior;
    }

    @Override
    public void show(int types)
    {
    }

    @Override
    public void hide(int types)
    {
    }

    private int controllableTypes()
    {
        return 0;
    }

    @Override
    public void controlWindowInsetsAnimation(int types, long durationMillis, Interpolator interpolator, CancellationSignal cancellationSignal,
                                             WindowInsetsAnimationControlListener listener)
    {
        if (listener == null) return;

        if ((types & controllableTypes()) == 0 || (cancellationSignal != null && cancellationSignal.isCanceled()))
        {
            listener.onCancelled(null);
        }
    }

    @Override
    public void addOnControllableInsetsChangedListener(OnControllableInsetsChangedListener listener)
    {
        if (listener == null || mControllableListeners.contains(listener)) return;

        mControllableListeners.add(listener);
        listener.onControllableInsetsChanged(this, controllableTypes());
    }

    @Override
    public void removeOnControllableInsetsChangedListener(OnControllableInsetsChangedListener listener)
    {
        mControllableListeners.remove(listener);
    }
}
