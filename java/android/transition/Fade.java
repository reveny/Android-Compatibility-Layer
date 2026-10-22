package android.transition;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class Fade extends Visibility
{
    public static final int IN = Visibility.MODE_IN;
    public static final int OUT = Visibility.MODE_OUT;

    private static final String PROPNAME_TRANSITION_ALPHA = "android:fade:transitionAlpha";

    public Fade()
    {
    }

    public Fade(int fadingMode)
    {
        setMode(fadingMode);
    }

    public Fade(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        setMode(TransitionAttributes.getInt(attributes, "fadingMode", getMode()));
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues)
    {
        super.captureStartValues(transitionValues);

        if (transitionValues.view != null) transitionValues.values.put(PROPNAME_TRANSITION_ALPHA, transitionValues.view.getAlpha());
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
