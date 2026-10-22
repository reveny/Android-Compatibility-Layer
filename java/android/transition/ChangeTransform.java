package android.transition;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ChangeTransform extends Transition
{
    private static final String PROPNAME_MATRIX = "android:changeTransform:matrix";
    private static final String PROPNAME_TRANSFORMS = "android:changeTransform:transforms";
    private static final String PROPNAME_PARENT = "android:changeTransform:parent";
    private static final String PROPNAME_PARENT_MATRIX = "android:changeTransform:parentMatrix";

    private static final String[] TRANSITION_PROPERTIES = { PROPNAME_MATRIX, PROPNAME_TRANSFORMS, PROPNAME_PARENT_MATRIX };

    private boolean mUseOverlay = true;
    private boolean mReparent = true;

    public ChangeTransform()
    {
    }

    public ChangeTransform(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        mUseOverlay = TransitionAttributes.getBoolean(attributes, "reparentWithOverlay", true);
        mReparent = TransitionAttributes.getBoolean(attributes, "reparent", true);
    }

    public boolean getReparentWithOverlay()
    {
        return mUseOverlay;
    }

    public void setReparentWithOverlay(boolean reparentWithOverlay)
    {
        mUseOverlay = reparentWithOverlay;
    }

    public boolean getReparent()
    {
        return mReparent;
    }

    public void setReparent(boolean reparent)
    {
        mReparent = reparent;
    }

    @Override
    public String[] getTransitionProperties()
    {
        return TRANSITION_PROPERTIES;
    }

    private void captureValues(TransitionValues transitionValues)
    {
        View view = transitionValues.view;
        if (view == null || view.getVisibility() == View.GONE) return;

        transitionValues.values.put(PROPNAME_PARENT, view.getParent());

        float[] transforms = { view.getTranslationX(), view.getTranslationY(), view.getTranslationZ(), view.getScaleX(),
                               view.getScaleY(), view.getRotationX(), view.getRotationY(), view.getRotation() };
        transitionValues.values.put(PROPNAME_TRANSFORMS, transforms);

        Matrix matrix = view.getMatrix();
        transitionValues.values.put(PROPNAME_MATRIX, matrix == null || matrix.isIdentity() ? null : new Matrix(matrix));
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
