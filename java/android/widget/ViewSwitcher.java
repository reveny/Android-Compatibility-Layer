package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ViewSwitcher extends ViewAnimator
{
    public interface ViewFactory
    {
        View makeView();
    }

    private ViewFactory mFactory;

    public ViewSwitcher(Context context)
    {
        super(context);
    }

    public ViewSwitcher(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        if (getChildCount() >= 2) throw new IllegalStateException("Can't add more than 2 views to a ViewSwitcher");

        super.addView(child, index, params);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ViewSwitcher.class.getName();
    }

    public View getNextView()
    {
        int which = mWhichChild == 0 ? 1 : 0;
        return getChildAt(which);
    }

    private View obtainView()
    {
        View child = mFactory.makeView();

        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        addView(child, params);
        return child;
    }

    public void setFactory(ViewFactory factory)
    {
        mFactory = factory;

        obtainView();
        obtainView();
    }

    public void reset()
    {
        mFirstTime = true;

        View first = getChildAt(0);
        if (first != null) first.setVisibility(View.GONE);

        View second = getChildAt(1);
        if (second != null) second.setVisibility(View.GONE);
    }
}
