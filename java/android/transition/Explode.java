package android.transition;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class Explode extends Visibility
{
    private static final String PROPNAME_SCREEN_BOUNDS = "android:explode:screenBounds";

    public Explode()
    {
        setPropagation(new CircularPropagation());
    }

    public Explode(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        setPropagation(new CircularPropagation());
    }

    private void captureValues(TransitionValues transitionValues)
    {
        View view = transitionValues.view;
        if (view == null) return;

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        transitionValues.values.put(PROPNAME_SCREEN_BOUNDS, new Rect(left, top, left + view.getWidth(), top + view.getHeight()));
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
