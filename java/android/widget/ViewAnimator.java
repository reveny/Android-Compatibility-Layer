package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class ViewAnimator extends FrameLayout
{
    private static StyleableSet sStyleable;

    int mWhichChild = 0;
    boolean mFirstTime = true;
    boolean mAnimateFirstTime = true;

    Animation mInAnimation;
    Animation mOutAnimation;

    public ViewAnimator(Context context)
    {
        super(context);
    }

    public ViewAnimator(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        if (attributes == null) return;

        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "inAnimation", "outAnimation", "animateFirstView", "measureAllChildren");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), 0, 0);

        int index = sStyleable.indexOf("inAnimation");
        if (index >= 0)
        {
            int resource = array.getResourceId(index, 0);
            if (resource > 0) setInAnimation(context, resource);
        }

        index = sStyleable.indexOf("outAnimation");
        if (index >= 0)
        {
            int resource = array.getResourceId(index, 0);
            if (resource > 0) setOutAnimation(context, resource);
        }

        index = sStyleable.indexOf("animateFirstView");
        if (index >= 0) setAnimateFirstView(array.getBoolean(index, true));

        index = sStyleable.indexOf("measureAllChildren");
        if (index >= 0) setMeasureAllChildren(array.getBoolean(index, true));

        array.recycle();
    }

    public void setDisplayedChild(int whichChild)
    {
        mWhichChild = whichChild;

        if (whichChild >= getChildCount()) mWhichChild = 0;
        else if (whichChild < 0) mWhichChild = getChildCount() - 1;

        boolean hasFocus = getFocusedChild() != null;

        showOnly(mWhichChild);

        if (hasFocus) requestFocus();
    }

    public int getDisplayedChild()
    {
        return mWhichChild;
    }

    public void showNext()
    {
        setDisplayedChild(mWhichChild + 1);
    }

    public void showPrevious()
    {
        setDisplayedChild(mWhichChild - 1);
    }

    void showOnly(int childIndex, boolean animate)
    {
        int count = getChildCount();

        for (int index = 0; index < count; index++)
        {
            View child = getChildAt(index);

            if (index == childIndex)
            {
                if (animate && mInAnimation != null) child.startAnimation(mInAnimation);

                child.setVisibility(View.VISIBLE);
                mFirstTime = false;
            }
            else
            {
                if (animate && mOutAnimation != null && child.getVisibility() == View.VISIBLE) child.startAnimation(mOutAnimation);
                else if (child.getAnimation() == mInAnimation) child.clearAnimation();

                child.setVisibility(View.GONE);
            }
        }
    }

    void showOnly(int childIndex)
    {
        boolean animate = !mFirstTime || mAnimateFirstTime;
        showOnly(childIndex, animate);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        super.addView(child, index, params);

        if (getChildCount() == 1) child.setVisibility(View.VISIBLE);
        else child.setVisibility(View.GONE);

        if (index >= 0 && mWhichChild >= index) setDisplayedChild(mWhichChild + 1);
    }

    @Override
    public void removeAllViews()
    {
        super.removeAllViews();

        mWhichChild = 0;
        mFirstTime = true;
    }

    @Override
    public void removeView(View view)
    {
        int index = indexOfChild(view);
        if (index < 0) return;

        super.removeView(view);
        afterChildrenRemoved(index, 1);
    }

    @Override
    public void removeViewAt(int index)
    {
        View child = getChildAt(index);
        if (child == null) return;

        super.removeView(child);
        afterChildrenRemoved(index, 1);
    }

    @Override
    public void removeViewInLayout(View view)
    {
        int index = indexOfChild(view);
        if (index < 0) return;

        super.removeViewInLayout(view);
        afterChildrenRemoved(index, 1);
    }

    @Override
    public void removeViews(int start, int count)
    {
        super.removeViews(start, count);
        afterChildrenRemoved(start, count);
    }

    @Override
    public void removeViewsInLayout(int start, int count)
    {
        super.removeViewsInLayout(start, count);
        afterChildrenRemoved(start, count);
    }

    private void afterChildrenRemoved(int start, int count)
    {
        int childCount = getChildCount();

        if (childCount == 0)
        {
            mWhichChild = 0;
            mFirstTime = true;
            return;
        }

        if (mWhichChild >= childCount) setDisplayedChild(childCount - 1);
        else if (mWhichChild >= start && mWhichChild < start + count) setDisplayedChild(mWhichChild);
    }

    public View getCurrentView()
    {
        return getChildAt(mWhichChild);
    }

    public Animation getInAnimation()
    {
        return mInAnimation;
    }

    public void setInAnimation(Animation inAnimation)
    {
        mInAnimation = inAnimation;
    }

    public Animation getOutAnimation()
    {
        return mOutAnimation;
    }

    public void setOutAnimation(Animation outAnimation)
    {
        mOutAnimation = outAnimation;
    }

    public void setInAnimation(Context context, int resourceId)
    {
        setInAnimation(loadAnimation(context, resourceId));
    }

    public void setOutAnimation(Context context, int resourceId)
    {
        setOutAnimation(loadAnimation(context, resourceId));
    }

    private static Animation loadAnimation(Context context, int resourceId)
    {
        try
        {
            return AnimationUtils.loadAnimation(context, resourceId);
        }
        catch (RuntimeException unsupported)
        {
            android.util.Log.w("ViewAnimator", "animation 0x" + Integer.toHexString(resourceId) + " did not load", unsupported);
            return null;
        }
    }

    public boolean getAnimateFirstView()
    {
        return mAnimateFirstTime;
    }

    public void setAnimateFirstView(boolean animate)
    {
        mAnimateFirstTime = animate;
    }

    @Override
    public int getBaseline()
    {
        View current = getCurrentView();
        return current != null ? current.getBaseline() : super.getBaseline();
    }

    public CharSequence getAccessibilityClassName()
    {
        return ViewAnimator.class.getName();
    }
}
