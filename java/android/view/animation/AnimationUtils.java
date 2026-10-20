package android.view.animation;

import android.content.Context;

public class AnimationUtils
{
    public static long currentAnimationTimeMillis()
    {
        return android.os.SystemClock.uptimeMillis();
    }

    public static Animation loadAnimation(Context context, int id)
    {
        return new Animation();
    }

    public static Interpolator loadInterpolator(Context context, int id)
    {
        return new LinearInterpolator();
    }

    public static LayoutAnimationController loadLayoutAnimation(Context context, int id)
    {
        return new LayoutAnimationController();
    }
}
