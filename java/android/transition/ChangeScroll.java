package android.transition;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class ChangeScroll extends Transition
{
    private static final String PROPNAME_SCROLL_X = "android:changeScroll:x";
    private static final String PROPNAME_SCROLL_Y = "android:changeScroll:y";

    private static final String[] PROPERTIES = { PROPNAME_SCROLL_X, PROPNAME_SCROLL_Y };

    public ChangeScroll()
    {
    }

    public ChangeScroll(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    private void captureValues(TransitionValues transitionValues)
    {
        if (transitionValues.view == null) return;

        transitionValues.values.put(PROPNAME_SCROLL_X, transitionValues.view.getScrollX());
        transitionValues.values.put(PROPNAME_SCROLL_Y, transitionValues.view.getScrollY());
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues)
    {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues)
    {
        captureValues(transitionValues);
    }

    @Override
    public String[] getTransitionProperties()
    {
        return PROPERTIES;
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues)
    {
        return null;
    }
}
