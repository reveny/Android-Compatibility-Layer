package android.transition;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

public class Slide extends Visibility
{
    private static final String PROPNAME_SCREEN_POSITION = "android:slide:screenPosition";

    private int mSlideEdge = Gravity.BOTTOM;

    public Slide()
    {
        setSlideEdge(Gravity.BOTTOM);
    }

    public Slide(int slideEdge)
    {
        setSlideEdge(slideEdge);
    }

    public Slide(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        setSlideEdge(TransitionAttributes.getInt(attributes, "slideEdge", Gravity.BOTTOM));
    }

    private void captureValues(TransitionValues transitionValues)
    {
        View view = transitionValues.view;
        if (view == null) return;

        int[] position = new int[2];
        view.getLocationOnScreen(position);
        transitionValues.values.put(PROPNAME_SCREEN_POSITION, position);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues)
    {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues)
    {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
    }

    public void setSlideEdge(int slideEdge)
    {
        switch (slideEdge)
        {
            case Gravity.LEFT:
            case Gravity.TOP:
            case Gravity.RIGHT:
            case Gravity.BOTTOM:
            case Gravity.START:
            case Gravity.END:
                break;
            default:
                throw new IllegalArgumentException("Invalid slide direction");
        }

        mSlideEdge = slideEdge;

        SidePropagation propagation = new SidePropagation();
        propagation.setSide(slideEdge);
        setPropagation(propagation);
    }

    public int getSlideEdge()
    {
        return mSlideEdge;
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues)
    {
        return null;
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues)
    {
        return null;
    }
}
