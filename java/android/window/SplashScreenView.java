package android.window;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.time.Duration;
import java.time.Instant;

public final class SplashScreenView extends FrameLayout
{
    private View mIconView;
    private Instant mIconAnimationStart;
    private Duration mIconAnimationDuration;
    private boolean mRemoved;

    public SplashScreenView(Context context)
    {
        super(context);
    }

    public SplashScreenView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public void setIconView(View iconView, Instant animationStart, Duration animationDuration)
    {
        if (mIconView != null && mIconView.getParent() == this) removeView(mIconView);

        mIconView = iconView;
        mIconAnimationStart = animationStart;
        mIconAnimationDuration = animationDuration;

        if (iconView != null && iconView.getParent() == null)
        {
            addView(iconView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                                                           android.view.Gravity.CENTER));
        }
    }

    public Duration getIconAnimationDuration()
    {
        return mIconAnimationDuration;
    }

    public Instant getIconAnimationStart()
    {
        return mIconAnimationStart;
    }

    public View getIconView()
    {
        return mIconView;
    }

    public void remove()
    {
        if (mRemoved) return;

        mRemoved = true;
        setVisibility(View.GONE);

        if (getParent() instanceof ViewGroup) ((ViewGroup)getParent()).removeView(this);
    }

    @Override
    public void setAlpha(float alpha)
    {
        super.setAlpha(alpha);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
    }
}
