package android.transition;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ChangeBounds extends Transition
{
    private static final String PROPNAME_BOUNDS = "android:changeBounds:bounds";
    private static final String PROPNAME_CLIP = "android:changeBounds:clip";
    private static final String PROPNAME_PARENT = "android:changeBounds:parent";
    private static final String PROPNAME_WINDOW_X = "android:changeBounds:windowX";
    private static final String PROPNAME_WINDOW_Y = "android:changeBounds:windowY";

    private static final String[] TRANSITION_PROPERTIES = { PROPNAME_BOUNDS, PROPNAME_CLIP, PROPNAME_PARENT, PROPNAME_WINDOW_X,
                                                            PROPNAME_WINDOW_Y };

    private boolean mResizeClip;
    private boolean mReparent;

    public ChangeBounds()
    {
    }

    public ChangeBounds(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        setResizeClip(TransitionAttributes.getBoolean(attributes, "resizeClip", false));
    }

    @Override
    public String[] getTransitionProperties()
    {
        return TRANSITION_PROPERTIES;
    }

    public void setResizeClip(boolean resizeClip)
    {
        mResizeClip = resizeClip;
    }

    public boolean getResizeClip()
    {
        return mResizeClip;
    }

    @Deprecated
    public void setReparent(boolean reparent)
    {
        mReparent = reparent;
    }

    private void captureValues(TransitionValues values)
    {
        View view = values.view;
        if (view == null) return;

        if (view.isLaidOut() || view.getWidth() != 0 || view.getHeight() != 0)
        {
            values.values.put(PROPNAME_BOUNDS, new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
            values.values.put(PROPNAME_PARENT, values.view.getParent());

            if (mReparent)
            {
                int[] location = new int[2];
                values.view.getLocationInWindow(location);
                values.values.put(PROPNAME_WINDOW_X, location[0]);
                values.values.put(PROPNAME_WINDOW_Y, location[1]);
            }

            if (mResizeClip) values.values.put(PROPNAME_CLIP, view.getClipBounds());
        }
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
