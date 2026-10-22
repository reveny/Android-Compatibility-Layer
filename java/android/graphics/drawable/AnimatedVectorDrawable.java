package android.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

public class AnimatedVectorDrawable extends Drawable implements Animatable2
{
    private static final long UNKNOWN_DURATION = -2;

    private final ArrayList<AnimationCallback> mCallbacks = new ArrayList<>();
    private final ArrayList<Integer> mTargetAnimations = new ArrayList<>();
    private final ArrayList<String> mTargetNames = new ArrayList<>();
    private final Drawable.Callback mForwarder = new Forwarder();

    private final Runnable mFinisher = new Runnable()
    {
        @Override
        public void run()
        {
            finish();
        }
    };

    private Drawable mVector;
    private Resources mResources;
    private Resources.Theme mTheme;
    private Handler mHandler;
    private long mTotalDuration = UNKNOWN_DURATION;
    private boolean mRunning;

    public AnimatedVectorDrawable()
    {
    }

    @Override
    public Drawable mutate()
    {
        if (mVector != null) mVector.mutate();
        return this;
    }

    @Override
    public ConstantState getConstantState()
    {
        return null;
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations() | (mVector == null ? 0 : mVector.getChangingConfigurations());
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mVector != null) mVector.draw(canvas);
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        if (mVector != null) mVector.setBounds(bounds);
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        return mVector != null && mVector.setState(stateSet);
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        return mVector != null && mVector.setLevel(level);
    }

    @Override
    public boolean onLayoutDirectionChanged(int layoutDirection)
    {
        return mVector != null && mVector.setLayoutDirection(layoutDirection);
    }

    @Override
    public int getAlpha()
    {
        return mVector == null ? super.getAlpha() : mVector.getAlpha();
    }

    @Override
    public void setAlpha(int alpha)
    {
        super.setAlpha(alpha);
        if (mVector != null) mVector.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter)
    {
        super.setColorFilter(filter);
        if (mVector != null) mVector.setColorFilter(filter);
    }

    @Override
    public ColorFilter getColorFilter()
    {
        return mVector == null ? super.getColorFilter() : mVector.getColorFilter();
    }

    @Override
    public void setTintList(ColorStateList tint)
    {
        if (mVector != null) mVector.setTintList(tint);
    }

    @Override
    public void setHotspot(float x, float y)
    {
        if (mVector != null) mVector.setHotspot(x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom)
    {
        if (mVector != null) mVector.setHotspotBounds(left, top, right, bottom);
    }

    @Override
    public void setTintBlendMode(BlendMode blendMode)
    {
        if (mVector != null) mVector.setTintBlendMode(blendMode);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart)
    {
        if (mVector != null) mVector.setVisible(visible, restart);

        return super.setVisible(visible, restart);
    }

    @Override
    public boolean isStateful()
    {
        return mVector != null && mVector.isStateful();
    }

    @Override
    public int getOpacity()
    {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mVector == null ? -1 : mVector.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mVector == null ? -1 : mVector.getIntrinsicHeight();
    }

    @Override
    public void getOutline(Outline outline)
    {
        if (mVector != null) mVector.getOutline(outline);
    }

    @Override
    public Insets getOpticalInsets()
    {
        return mVector == null ? Insets.NONE : mVector.getOpticalInsets();
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        mResources = resources;
        mTheme = theme;

        int reference = DrawableInflater.reference(attributes, "drawable", 0);
        if (reference != 0) setVector(resources.getDrawable(reference));

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            if ("target".equals(parser.getName()))
            {
                mTargetNames.add(DrawableInflater.text(attributes, "name"));
                mTargetAnimations.add(DrawableInflater.reference(attributes, "animation", 0));
                DrawableInflater.skipElement(parser);
                continue;
            }

            Drawable child = DrawableInflater.inflate(resources, parser, attributes, theme);
            if (child != null) setVector(child);
        }
    }

    @Override
    public boolean canApplyTheme()
    {
        return mVector != null && mVector.canApplyTheme();
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        if (mVector != null) mVector.applyTheme(theme);
    }

    private void setVector(Drawable vector)
    {
        if (mVector != null) mVector.setCallback(null);

        mVector = vector;
        if (vector == null) return;

        vector.setCallback(mForwarder);
        vector.setBounds(getBounds());
        vector.setState(getState());
        vector.setLevel(getLevel());
        if (super.getAlpha() != 255) vector.setAlpha(super.getAlpha());
        if (super.getColorFilter() != null) vector.setColorFilter(super.getColorFilter());
    }

    @Override
    public boolean isRunning()
    {
        return mRunning;
    }

    public void reset()
    {
        if (!mRunning) return;

        mRunning = false;
        handler().removeCallbacks(mFinisher);
        invalidateSelf();
    }

    @Override
    public void start()
    {
        if (mRunning) return;

        mRunning = true;
        long duration = totalDuration();

        for (AnimationCallback callback : new ArrayList<>(mCallbacks)) callback.onAnimationStart(this);

        if (duration >= 0) handler().postDelayed(mFinisher, duration);

        invalidateSelf();
    }

    @Override
    public void stop()
    {
        if (!mRunning) return;

        handler().removeCallbacks(mFinisher);
        finish();
    }

    private void finish()
    {
        if (!mRunning) return;

        mRunning = false;
        invalidateSelf();

        for (AnimationCallback callback : new ArrayList<>(mCallbacks)) callback.onAnimationEnd(this);
    }

    private Handler handler()
    {
        if (mHandler == null)
        {
            Looper looper = Looper.myLooper();
            mHandler = new Handler(looper != null ? looper : Looper.getMainLooper());
        }

        return mHandler;
    }

    private long totalDuration()
    {
        if (mTotalDuration != UNKNOWN_DURATION) return mTotalDuration;

        long longest = 0;

        for (int animation : mTargetAnimations)
        {
            if (animation == 0 || mResources == null) continue;

            try
            {
                Animator animator = AnimatorInflater.loadAnimator(mResources, mTheme, animation);
                long duration = durationOf(animator);

                if (duration < 0)
                {
                    longest = -1;
                    break;
                }

                longest = Math.max(longest, duration);
            }
            catch (RuntimeException failure)
            {
                continue;
            }
        }

        mTotalDuration = longest;
        return longest;
    }

    private static long durationOf(Animator animator)
    {
        if (animator == null) return 0;

        if (animator instanceof AnimatorSet)
        {
            long longest = 0;

            for (Animator child : ((AnimatorSet) animator).getChildAnimations())
            {
                long duration = durationOf(child);
                if (duration < 0) return -1;

                longest = Math.max(longest, duration);
            }

            return animator.getStartDelay() + longest;
        }

        if (animator instanceof ValueAnimator)
        {
            ValueAnimator value = (ValueAnimator) animator;
            if (value.getRepeatCount() == ValueAnimator.INFINITE) return -1;

            return value.getStartDelay() + value.getDuration() * (value.getRepeatCount() + 1);
        }

        long total = animator.getTotalDuration();
        return total < 0 ? -1 : animator.getStartDelay() + total;
    }

    @Override
    public void registerAnimationCallback(AnimationCallback callback)
    {
        if (callback != null && !mCallbacks.contains(callback)) mCallbacks.add(callback);
    }

    @Override
    public boolean unregisterAnimationCallback(AnimationCallback callback)
    {
        return mCallbacks.remove(callback);
    }

    @Override
    public void clearAnimationCallbacks()
    {
        mCallbacks.clear();
    }

    private final class Forwarder implements Drawable.Callback
    {
        @Override
        public void invalidateDrawable(Drawable who)
        {
            invalidateSelf();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when)
        {
            scheduleSelf(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what)
        {
            unscheduleSelf(what);
        }
    }
}
