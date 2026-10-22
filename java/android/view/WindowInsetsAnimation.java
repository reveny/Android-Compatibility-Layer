package android.view;

import android.graphics.Insets;
import android.view.animation.Interpolator;

import java.util.List;

public final class WindowInsetsAnimation
{
    private final int mTypeMask;
    private final long mDurationMillis;
    private final Interpolator mInterpolator;

    private float mFraction;
    private float mAlpha = 1.0f;

    public WindowInsetsAnimation(int typeMask, Interpolator interpolator, long durationMillis)
    {
        mTypeMask = typeMask;
        mInterpolator = interpolator;
        mDurationMillis = durationMillis;
    }

    public int getTypeMask()
    {
        return mTypeMask;
    }

    public float getFraction()
    {
        return mFraction;
    }

    public void setFraction(float fraction)
    {
        mFraction = fraction;
    }

    public float getInterpolatedFraction()
    {
        return mInterpolator == null ? mFraction : mInterpolator.getInterpolation(mFraction);
    }

    public Interpolator getInterpolator()
    {
        return mInterpolator;
    }

    public long getDurationMillis()
    {
        return mDurationMillis;
    }

    public float getAlpha()
    {
        return mAlpha;
    }

    public void setAlpha(float alpha)
    {
        mAlpha = alpha;
    }

    public static final class Bounds
    {
        private final Insets mLowerBound;
        private final Insets mUpperBound;

        public Bounds(Insets lowerBound, Insets upperBound)
        {
            mLowerBound = lowerBound;
            mUpperBound = upperBound;
        }

        public Insets getLowerBound()
        {
            return mLowerBound;
        }

        public Insets getUpperBound()
        {
            return mUpperBound;
        }

        public Bounds inset(Insets insets)
        {
            return new Bounds(subtract(mLowerBound, insets), subtract(mUpperBound, insets));
        }

        private static Insets subtract(Insets source, Insets insets)
        {
            return Insets.of(Math.max(0, source.left - insets.left), Math.max(0, source.top - insets.top),
                             Math.max(0, source.right - insets.right), Math.max(0, source.bottom - insets.bottom));
        }

        @Override
        public String toString()
        {
            return "Bounds{lower=" + mLowerBound + " upper=" + mUpperBound + "}";
        }
    }

    public abstract static class Callback
    {
        public static final int DISPATCH_MODE_STOP = 0;
        public static final int DISPATCH_MODE_CONTINUE_ON_SUBTREE = 1;

        private final int mDispatchMode;

        public Callback(int dispatchMode)
        {
            mDispatchMode = dispatchMode;
        }

        public final int getDispatchMode()
        {
            return mDispatchMode;
        }

        public void onPrepare(WindowInsetsAnimation animation)
        {
        }

        public Bounds onStart(WindowInsetsAnimation animation, Bounds bounds)
        {
            return bounds;
        }

        public abstract WindowInsets onProgress(WindowInsets insets, List<WindowInsetsAnimation> runningAnimations);

        public void onEnd(WindowInsetsAnimation animation)
        {
        }
    }
}
