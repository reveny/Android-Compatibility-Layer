package android.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Rect;

public final class ViewAnimationUtils
{
    private ViewAnimationUtils()
    {
    }

    public static Animator createCircularReveal(final View view, final int centerX, final int centerY, float startRadius, float endRadius)
    {
        final ValueAnimator animator = ValueAnimator.ofFloat(startRadius, endRadius);
        final Rect previousClip = view.getClipBounds();

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float radius = (Float)animation.getAnimatedValue();
                int extent = (int)Math.ceil(radius);
                view.setClipBounds(new Rect(centerX - extent, centerY - extent, centerX + extent, centerY + extent));
            }
        });

        animator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                view.setClipBounds(previousClip);
            }
        });

        return animator;
    }
}
