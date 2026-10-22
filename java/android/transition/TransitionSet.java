package android.transition;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class TransitionSet extends Transition
{
    public static final int ORDERING_TOGETHER = 0;
    public static final int ORDERING_SEQUENTIAL = 1;

    private ArrayList<Transition> mTransitions = new ArrayList<>();
    private boolean mPlayTogether = true;

    public TransitionSet()
    {
    }

    public TransitionSet(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        setOrdering(TransitionAttributes.getInt(attributes, "transitionOrdering", ORDERING_TOGETHER));
    }

    private ArrayList<Transition> children()
    {
        return mTransitions == null ? new ArrayList<>() : new ArrayList<>(mTransitions);
    }

    public TransitionSet setOrdering(int ordering)
    {
        switch (ordering)
        {
            case ORDERING_SEQUENTIAL: mPlayTogether = false; break;
            case ORDERING_TOGETHER: mPlayTogether = true; break;
            default: throw new android.util.AndroidRuntimeException("Invalid parameter for TransitionSet ordering: " + ordering);
        }

        return this;
    }

    public int getOrdering()
    {
        return mPlayTogether ? ORDERING_TOGETHER : ORDERING_SEQUENTIAL;
    }

    public TransitionSet addTransition(Transition transition)
    {
        if (transition == null) return this;

        mTransitions.add(transition);
        if (getDuration() >= 0) transition.setDuration(getDuration());
        if (getInterpolator() != null) transition.setInterpolator(getInterpolator());
        return this;
    }

    public int getTransitionCount()
    {
        return mTransitions.size();
    }

    public Transition getTransitionAt(int index)
    {
        if (index < 0 || index >= mTransitions.size()) return null;

        return mTransitions.get(index);
    }

    @Override
    public TransitionSet setDuration(long duration)
    {
        super.setDuration(duration);

        if (duration < 0) return this;

        for (Transition transition : children())
        {
            transition.setDuration(duration);
        }

        return this;
    }

    @Override
    public TransitionSet setStartDelay(long startDelay)
    {
        return (TransitionSet)super.setStartDelay(startDelay);
    }

    @Override
    public TransitionSet setInterpolator(TimeInterpolator interpolator)
    {
        super.setInterpolator(interpolator);

        for (Transition transition : children())
        {
            transition.setInterpolator(interpolator);
        }

        return this;
    }

    @Override
    public TransitionSet addTarget(View target)
    {
        for (Transition transition : children())
        {
            transition.addTarget(target);
        }

        return (TransitionSet)super.addTarget(target);
    }

    @Override
    public TransitionSet addTarget(int targetId)
    {
        for (Transition transition : children())
        {
            transition.addTarget(targetId);
        }

        return (TransitionSet)super.addTarget(targetId);
    }

    @Override
    public TransitionSet addTarget(String targetName)
    {
        for (Transition transition : children())
        {
            transition.addTarget(targetName);
        }

        return (TransitionSet)super.addTarget(targetName);
    }

    @Override
    public TransitionSet addTarget(Class targetType)
    {
        for (Transition transition : children())
        {
            transition.addTarget(targetType);
        }

        return (TransitionSet)super.addTarget(targetType);
    }

    @Override
    public TransitionSet addListener(TransitionListener listener)
    {
        return (TransitionSet)super.addListener(listener);
    }

    @Override
    public TransitionSet removeTarget(int targetId)
    {
        for (Transition transition : children())
        {
            transition.removeTarget(targetId);
        }

        return (TransitionSet)super.removeTarget(targetId);
    }

    @Override
    public TransitionSet removeTarget(View target)
    {
        for (Transition transition : children())
        {
            transition.removeTarget(target);
        }

        return (TransitionSet)super.removeTarget(target);
    }

    @Override
    public TransitionSet removeTarget(Class target)
    {
        for (Transition transition : children())
        {
            transition.removeTarget(target);
        }

        return (TransitionSet)super.removeTarget(target);
    }

    @Override
    public TransitionSet removeTarget(String target)
    {
        for (Transition transition : children())
        {
            transition.removeTarget(target);
        }

        return (TransitionSet)super.removeTarget(target);
    }

    @Override
    public Transition excludeTarget(View target, boolean exclude)
    {
        for (Transition transition : children())
        {
            transition.excludeTarget(target, exclude);
        }

        return super.excludeTarget(target, exclude);
    }

    @Override
    public Transition excludeTarget(String targetName, boolean exclude)
    {
        for (Transition transition : children())
        {
            transition.excludeTarget(targetName, exclude);
        }

        return super.excludeTarget(targetName, exclude);
    }

    @Override
    public Transition excludeTarget(int targetId, boolean exclude)
    {
        for (Transition transition : children())
        {
            transition.excludeTarget(targetId, exclude);
        }

        return super.excludeTarget(targetId, exclude);
    }

    @Override
    public Transition excludeTarget(Class type, boolean exclude)
    {
        for (Transition transition : children())
        {
            transition.excludeTarget(type, exclude);
        }

        return super.excludeTarget(type, exclude);
    }

    @Override
    public TransitionSet removeListener(TransitionListener listener)
    {
        return (TransitionSet)super.removeListener(listener);
    }

    @Override
    public void setPathMotion(PathMotion pathMotion)
    {
        super.setPathMotion(pathMotion);

        for (Transition transition : children())
        {
            transition.setPathMotion(pathMotion);
        }
    }

    public TransitionSet removeTransition(Transition transition)
    {
        mTransitions.remove(transition);
        return this;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues)
    {
        for (Transition transition : children())
        {
            transition.captureStartValues(transitionValues);
        }
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues)
    {
        for (Transition transition : children())
        {
            transition.captureEndValues(transitionValues);
        }
    }

    @Override
    public void setPropagation(TransitionPropagation propagation)
    {
        super.setPropagation(propagation);

        for (Transition transition : children())
        {
            transition.setPropagation(propagation);
        }
    }

    @Override
    public void setEpicenterCallback(EpicenterCallback epicenterCallback)
    {
        super.setEpicenterCallback(epicenterCallback);

        for (Transition transition : children())
        {
            transition.setEpicenterCallback(epicenterCallback);
        }
    }

    @Override
    void playStart()
    {
        dispatchStart();

        for (Transition transition : children())
        {
            transition.playStart();
        }
    }

    @Override
    void playEnd()
    {
        for (Transition transition : children())
        {
            transition.playEnd();
        }

        dispatchEnd();
    }

    @Override
    String toString(String indent)
    {
        StringBuilder builder = new StringBuilder(super.toString(indent));

        for (Transition transition : children())
        {
            builder.append("\n").append(transition.toString(indent + "  "));
        }

        return builder.toString();
    }

    @Override
    public TransitionSet clone()
    {
        TransitionSet clone = (TransitionSet)super.clone();
        clone.mTransitions = new ArrayList<>();

        for (Transition transition : children())
        {
            clone.addTransition(transition.clone());
        }

        return clone;
    }
}
