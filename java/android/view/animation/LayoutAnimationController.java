package android.view.animation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;

public class LayoutAnimationController
{
    public static final int ORDER_NORMAL = 0;
    public static final int ORDER_REVERSE = 1;
    public static final int ORDER_RANDOM = 2;

    private static final String[] ORDER_NAMES = {"normal", "reverse", "random"};
    private static final int[] ORDER_VALUES = {ORDER_NORMAL, ORDER_REVERSE, ORDER_RANDOM};

    private static java.lang.reflect.Field sParametersField;
    private static boolean sParametersFieldResolved;

    protected Animation mAnimation;
    protected Random mRandomizer;
    protected Interpolator mInterpolator;

    private float mDelay;
    private int mOrder;
    private long mDuration;
    private long mMaxDelay;

    public LayoutAnimationController(Context context, AttributeSet attributes)
    {
        AnimationAttributes values = new AnimationAttributes(context, attributes);

        mDelay = values.getDescription("delay").value;
        mOrder = values.getEnum("animationOrder", ORDER_NAMES, ORDER_VALUES, ORDER_NORMAL);

        int animation = values.getResourceId("animation", 0);
        if (animation > 0) setAnimation(context, animation);

        int interpolator = values.getResourceId("interpolator", 0);
        if (interpolator > 0) setInterpolator(context, interpolator);
    }

    public LayoutAnimationController(Animation animation)
    {
        this(animation, 0.5f);
    }

    public LayoutAnimationController(Animation animation, float delay)
    {
        mDelay = delay;
        setAnimation(animation);
    }

    public int getOrder()
    {
        return mOrder;
    }

    public void setOrder(int order)
    {
        mOrder = order;
    }

    public void setAnimation(Context context, int resourceId)
    {
        setAnimation(AnimationUtils.loadAnimation(context, resourceId));
    }

    public void setAnimation(Animation animation)
    {
        mAnimation = animation;
        if (mAnimation != null) mAnimation.setFillBefore(true);
    }

    public Animation getAnimation()
    {
        return mAnimation;
    }

    public void setInterpolator(Context context, int resourceId)
    {
        setInterpolator(AnimationUtils.loadInterpolator(context, resourceId));
    }

    public void setInterpolator(Interpolator interpolator)
    {
        mInterpolator = interpolator;
    }

    public Interpolator getInterpolator()
    {
        return mInterpolator;
    }

    public float getDelay()
    {
        return mDelay;
    }

    public void setDelay(float delay)
    {
        mDelay = delay;
    }

    public boolean willOverlap()
    {
        return mDelay < 1.0f;
    }

    public void start()
    {
        if (mAnimation == null) return;

        mDuration = mAnimation.getDuration();
        mMaxDelay = Long.MIN_VALUE;
        mAnimation.setStartTime(-1);
    }

    public final Animation getAnimationForView(View view)
    {
        if (mAnimation == null) return null;

        long delay = getDelayForView(view) + mAnimation.getStartOffset();
        mMaxDelay = Math.max(mMaxDelay, delay);

        try
        {
            Animation animation = mAnimation.clone();
            animation.setStartOffset(delay);
            return animation;
        }
        catch (CloneNotSupportedException failure)
        {
            return null;
        }
    }

    public boolean isDone()
    {
        if (mAnimation == null) return true;

        return AnimationUtils.currentAnimationTimeMillis() > mAnimation.getStartTime() + mMaxDelay + mDuration;
    }

    protected long getDelayForView(View view)
    {
        AnimationParameters parameters = parametersOf(view);
        if (parameters == null || mAnimation == null) return 0;

        float delay = mDelay * mAnimation.getDuration();
        long viewDelay = (long)(getTransformedIndex(parameters) * delay);
        float totalDelay = delay * parameters.count;
        if (totalDelay == 0) return 0;

        if (mInterpolator == null) mInterpolator = new LinearInterpolator();

        float normalizedDelay = viewDelay / totalDelay;
        normalizedDelay = mInterpolator.getInterpolation(normalizedDelay);

        return (long)(normalizedDelay * totalDelay);
    }

    protected int getTransformedIndex(AnimationParameters parameters)
    {
        switch (getOrder())
        {
            case ORDER_REVERSE:
                return parameters.count - 1 - parameters.index;
            case ORDER_RANDOM:
                if (mRandomizer == null) mRandomizer = new Random();
                return (int)(parameters.count * mRandomizer.nextFloat());
            case ORDER_NORMAL:
            default:
                return parameters.index;
        }
    }

    static AnimationParameters parametersOf(View view)
    {
        if (view == null) return null;

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) return null;

        if (!sParametersFieldResolved)
        {
            sParametersFieldResolved = true;

            try
            {
                sParametersField = ViewGroup.LayoutParams.class.getField("layoutAnimationParameters");
            }
            catch (NoSuchFieldException failure)
            {
                sParametersField = null;
            }
        }

        if (sParametersField == null) return null;

        try
        {
            Object value = sParametersField.get(layoutParams);
            return value instanceof AnimationParameters ? (AnimationParameters)value : null;
        }
        catch (IllegalAccessException failure)
        {
            return null;
        }
    }

    public static class AnimationParameters
    {
        public int count;
        public int index;

        public AnimationParameters()
        {
        }
    }
}
