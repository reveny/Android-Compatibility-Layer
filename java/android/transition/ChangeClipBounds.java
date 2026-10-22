package android.transition;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ChangeClipBounds extends Transition
{
    private static final String PROPNAME_CLIP = "android:clipBounds:clip";
    private static final String PROPNAME_BOUNDS = "android:clipBounds:bounds";

    private static final String[] TRANSITION_PROPERTIES = { PROPNAME_CLIP };

    public ChangeClipBounds()
    {
    }

    public ChangeClipBounds(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    @Override
    public String[] getTransitionProperties()
    {
        return TRANSITION_PROPERTIES;
    }

    private void captureValues(TransitionValues values)
    {
        View view = values.view;
        if (view == null || view.getVisibility() == View.GONE) return;

        Rect clip = view.getClipBounds();
        values.values.put(PROPNAME_CLIP, clip);

        if (clip == null) values.values.put(PROPNAME_BOUNDS, new Rect(0, 0, view.getWidth(), view.getHeight()));
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
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues)
    {
        return null;
    }
}
