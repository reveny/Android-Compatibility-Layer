package android.transition;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public abstract class Visibility extends Transition
{
    static final String PROPNAME_VISIBILITY = "android:visibility:visibility";
    private static final String PROPNAME_PARENT = "android:visibility:parent";
    private static final String PROPNAME_SCREEN_LOCATION = "android:visibility:screenLocation";

    public static final int MODE_IN = 0x1;
    public static final int MODE_OUT = 0x2;

    private static final String[] TRANSITION_PROPERTIES = { PROPNAME_VISIBILITY, PROPNAME_PARENT };

    private int mMode = MODE_IN | MODE_OUT;

    public Visibility()
    {
    }

    public Visibility(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        int mode = TransitionAttributes.getInt(attributes, "transitionVisibilityMode", 0);
        if (mode != 0) setMode(mode);
    }

    public void setMode(int mode)
    {
        if ((mode & ~(MODE_IN | MODE_OUT)) != 0) throw new IllegalArgumentException("Only MODE_IN and MODE_OUT flags are allowed");

        mMode = mode;
    }

    public int getMode()
    {
        return mMode;
    }

    @Override
    public String[] getTransitionProperties()
    {
        return TRANSITION_PROPERTIES;
    }

    private void captureValues(TransitionValues transitionValues)
    {
        View view = transitionValues.view;
        if (view == null) return;

        transitionValues.values.put(PROPNAME_VISIBILITY, view.getVisibility());
        transitionValues.values.put(PROPNAME_PARENT, view.getParent());

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        transitionValues.values.put(PROPNAME_SCREEN_LOCATION, location);
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

    public boolean isVisible(TransitionValues values)
    {
        if (values == null) return false;

        Integer visibility = (Integer)values.values.get(PROPNAME_VISIBILITY);
        boolean parent = values.values.get(PROPNAME_PARENT) != null;

        return visibility != null && visibility == View.VISIBLE && parent;
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues)
    {
        boolean startVisible = isVisible(startValues);
        boolean endVisible = isVisible(endValues);

        if (startVisible == endVisible) return null;

        int startVisibility = visibilityOf(startValues);
        int endVisibility = visibilityOf(endValues);

        if (endVisible) return onAppear(sceneRoot, startValues, startVisibility, endValues, endVisibility);

        return onDisappear(sceneRoot, startValues, startVisibility, endValues, endVisibility);
    }

    private static int visibilityOf(TransitionValues values)
    {
        if (values == null) return -1;

        Integer visibility = (Integer)values.values.get(PROPNAME_VISIBILITY);
        return visibility == null ? -1 : visibility;
    }

    public Animator onAppear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility, TransitionValues endValues,
                             int endVisibility)
    {
        if ((mMode & MODE_IN) != MODE_IN || endValues == null) return null;

        return onAppear(sceneRoot, endValues.view, startValues, endValues);
    }

    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues)
    {
        return null;
    }

    public Animator onDisappear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility, TransitionValues endValues,
                                int endVisibility)
    {
        if ((mMode & MODE_OUT) != MODE_OUT || startValues == null) return null;

        return onDisappear(sceneRoot, startValues.view, startValues, endValues);
    }

    @Override
    public boolean isTransitionRequired(TransitionValues startValues, TransitionValues endValues)
    {
        if (startValues == null && endValues == null) return false;

        if (startValues != null && endValues != null)
        {
            Object startVisibility = startValues.values.get(PROPNAME_VISIBILITY);
            Object endVisibility = endValues.values.get(PROPNAME_VISIBILITY);
            if (startVisibility != null && !startVisibility.equals(endVisibility)) return true;
        }

        return isVisible(startValues) != isVisible(endValues);
    }

    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues)
    {
        return null;
    }
}
