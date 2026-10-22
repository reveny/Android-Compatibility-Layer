package android.transition;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class Transition implements Cloneable
{
    public static final int MATCH_INSTANCE = 0x1;
    public static final int MATCH_NAME = 0x2;
    public static final int MATCH_ID = 0x3;
    public static final int MATCH_ITEM_ID = 0x4;

    private static final int MATCH_FIRST = MATCH_INSTANCE;
    private static final int MATCH_LAST = MATCH_ITEM_ID;
    private static final int[] DEFAULT_MATCH_ORDER = { MATCH_NAME, MATCH_INSTANCE, MATCH_ID, MATCH_ITEM_ID };

    private static final PathMotion STRAIGHT_PATH_MOTION = new PathMotion()
    {
        @Override
        public Path getPath(float startX, float startY, float endX, float endY)
        {
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);
            return path;
        }
    };

    private long mStartDelay = -1;
    private long mDuration = -1;
    private TimeInterpolator mInterpolator;
    private int[] mMatchOrder = DEFAULT_MATCH_ORDER;
    private ArrayList<Integer> mTargetIds = new ArrayList<>();
    private ArrayList<View> mTargets = new ArrayList<>();
    private ArrayList<String> mTargetNames;
    private ArrayList<Class> mTargetTypes;
    private ArrayList<Integer> mTargetIdExcludes;
    private ArrayList<View> mTargetExcludes;
    private ArrayList<Class> mTargetTypeExcludes;
    private ArrayList<String> mTargetNameExcludes;
    private ArrayList<Integer> mTargetIdChildExcludes;
    private ArrayList<View> mTargetChildExcludes;
    private ArrayList<Class> mTargetTypeChildExcludes;
    private ArrayList<TransitionListener> mListeners;
    private EpicenterCallback mEpicenterCallback;
    private PathMotion mPathMotion = STRAIGHT_PATH_MOTION;
    private TransitionPropagation mPropagation;
    private String mName = getClass().getName();

    public Transition()
    {
    }

    public Transition(Context context, AttributeSet attributes)
    {
        int duration = TransitionAttributes.getInt(attributes, "duration", -1);
        if (duration >= 0) setDuration(duration);

        int startDelay = TransitionAttributes.getInt(attributes, "startDelay", -1);
        if (startDelay > 0) setStartDelay(startDelay);

        int interpolator = TransitionAttributes.getResource(attributes, "interpolator", 0);
        if (interpolator != 0 && context != null)
        {
            try
            {
                setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(context, interpolator));
            }
            catch (RuntimeException exception)
            {
            }
        }

        String matchOrder = TransitionAttributes.getString(attributes, "matchOrder");
        if (matchOrder != null) setMatchOrder(parseMatchOrder(matchOrder));
    }

    private static int[] parseMatchOrder(String order)
    {
        String[] parts = order.split(",");
        int[] matches = new int[parts.length];
        int count = 0;

        for (String part : parts)
        {
            String token = part.trim();

            if ("id".equalsIgnoreCase(token)) matches[count++] = MATCH_ID;
            else if ("instance".equalsIgnoreCase(token)) matches[count++] = MATCH_INSTANCE;
            else if ("name".equalsIgnoreCase(token)) matches[count++] = MATCH_NAME;
            else if ("itemId".equalsIgnoreCase(token)) matches[count++] = MATCH_ITEM_ID;
            else if (!token.isEmpty()) throw new android.view.InflateException("Unknown match type in matchOrder: '" + token + "'");
        }

        return java.util.Arrays.copyOf(matches, count);
    }

    public Transition setDuration(long duration)
    {
        mDuration = duration;
        return this;
    }

    public long getDuration()
    {
        return mDuration;
    }

    public Transition setStartDelay(long startDelay)
    {
        mStartDelay = startDelay;
        return this;
    }

    public long getStartDelay()
    {
        return mStartDelay;
    }

    public Transition setInterpolator(TimeInterpolator interpolator)
    {
        mInterpolator = interpolator;
        return this;
    }

    public TimeInterpolator getInterpolator()
    {
        return mInterpolator;
    }

    public String[] getTransitionProperties()
    {
        return null;
    }

    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues, TransitionValues endValues)
    {
        return null;
    }

    public void setMatchOrder(int... matches)
    {
        if (matches == null || matches.length == 0)
        {
            mMatchOrder = DEFAULT_MATCH_ORDER;
            return;
        }

        for (int index = 0; index < matches.length; index++)
        {
            int match = matches[index];
            if (match < MATCH_FIRST || match > MATCH_LAST) throw new IllegalArgumentException("matches contains invalid value");

            for (int previous = 0; previous < index; previous++)
            {
                if (matches[previous] == match) throw new IllegalArgumentException("matches contains a duplicate value");
            }
        }

        mMatchOrder = matches.clone();
    }

    public abstract void captureStartValues(TransitionValues transitionValues);

    public abstract void captureEndValues(TransitionValues transitionValues);

    public Transition addTarget(int targetId)
    {
        if (targetId > 0) mTargetIds.add(targetId);
        return this;
    }

    public Transition addTarget(String targetName)
    {
        if (targetName == null) return this;
        if (mTargetNames == null) mTargetNames = new ArrayList<>();

        mTargetNames.add(targetName);
        return this;
    }

    public Transition addTarget(Class targetType)
    {
        if (targetType == null) return this;
        if (mTargetTypes == null) mTargetTypes = new ArrayList<>();

        mTargetTypes.add(targetType);
        return this;
    }

    public Transition addTarget(View target)
    {
        mTargets.add(target);
        return this;
    }

    public Transition removeTarget(int targetId)
    {
        if (targetId > 0) mTargetIds.remove((Integer)targetId);
        return this;
    }

    public Transition removeTarget(String targetName)
    {
        if (targetName != null && mTargetNames != null) mTargetNames.remove(targetName);
        return this;
    }

    public Transition removeTarget(View target)
    {
        if (target != null) mTargets.remove(target);
        return this;
    }

    public Transition removeTarget(Class target)
    {
        if (target != null && mTargetTypes != null) mTargetTypes.remove(target);
        return this;
    }

    private static <T> ArrayList<T> excludeObject(ArrayList<T> list, T target, boolean exclude)
    {
        if (target == null) return list;

        if (exclude)
        {
            if (list == null) list = new ArrayList<>();
            if (!list.contains(target)) list.add(target);
            return list;
        }

        if (list != null)
        {
            list.remove(target);
            if (list.isEmpty()) list = null;
        }

        return list;
    }

    public Transition excludeTarget(int targetId, boolean exclude)
    {
        if (targetId >= 0) mTargetIdExcludes = excludeObject(mTargetIdExcludes, targetId, exclude);
        return this;
    }

    public Transition excludeTarget(String targetName, boolean exclude)
    {
        mTargetNameExcludes = excludeObject(mTargetNameExcludes, targetName, exclude);
        return this;
    }

    public Transition excludeChildren(int targetId, boolean exclude)
    {
        if (targetId >= 0) mTargetIdChildExcludes = excludeObject(mTargetIdChildExcludes, targetId, exclude);
        return this;
    }

    public Transition excludeTarget(View target, boolean exclude)
    {
        mTargetExcludes = excludeObject(mTargetExcludes, target, exclude);
        return this;
    }

    public Transition excludeChildren(View target, boolean exclude)
    {
        mTargetChildExcludes = excludeObject(mTargetChildExcludes, target, exclude);
        return this;
    }

    public Transition excludeTarget(Class type, boolean exclude)
    {
        mTargetTypeExcludes = excludeObject(mTargetTypeExcludes, type, exclude);
        return this;
    }

    public Transition excludeChildren(Class type, boolean exclude)
    {
        mTargetTypeChildExcludes = excludeObject(mTargetTypeChildExcludes, type, exclude);
        return this;
    }

    public List<Integer> getTargetIds()
    {
        return mTargetIds;
    }

    public List<View> getTargets()
    {
        return mTargets;
    }

    public List<String> getTargetNames()
    {
        return mTargetNames;
    }

    public List<Class> getTargetTypes()
    {
        return mTargetTypes;
    }

    public TransitionValues getTransitionValues(View view, boolean start)
    {
        return null;
    }

    public boolean isTransitionRequired(TransitionValues startValues, TransitionValues endValues)
    {
        if (startValues == null || endValues == null) return false;

        String[] properties = getTransitionProperties();
        if (properties != null)
        {
            for (String property : properties)
            {
                if (isValueChanged(startValues, endValues, property)) return true;
            }
            return false;
        }

        for (String key : startValues.values.keySet())
        {
            if (isValueChanged(startValues, endValues, key)) return true;
        }

        return false;
    }

    private static boolean isValueChanged(TransitionValues oldValues, TransitionValues newValues, String key)
    {
        Object oldValue = oldValues.values.get(key);
        Object newValue = newValues.values.get(key);

        if (oldValue == null && newValue == null) return false;
        if (oldValue == null || newValue == null) return true;

        return !oldValue.equals(newValue);
    }

    public Transition addListener(TransitionListener listener)
    {
        if (mListeners == null) mListeners = new ArrayList<>();

        mListeners.add(listener);
        return this;
    }

    public Transition removeListener(TransitionListener listener)
    {
        if (mListeners == null) return this;

        mListeners.remove(listener);
        if (mListeners.isEmpty()) mListeners = null;
        return this;
    }

    public void setEpicenterCallback(EpicenterCallback epicenterCallback)
    {
        mEpicenterCallback = epicenterCallback;
    }

    public EpicenterCallback getEpicenterCallback()
    {
        return mEpicenterCallback;
    }

    public Rect getEpicenter()
    {
        if (mEpicenterCallback == null) return null;

        return mEpicenterCallback.onGetEpicenter(this);
    }

    public void setPathMotion(PathMotion pathMotion)
    {
        mPathMotion = pathMotion == null ? STRAIGHT_PATH_MOTION : pathMotion;
    }

    public PathMotion getPathMotion()
    {
        return mPathMotion;
    }

    public void setPropagation(TransitionPropagation transitionPropagation)
    {
        mPropagation = transitionPropagation;
    }

    public TransitionPropagation getPropagation()
    {
        return mPropagation;
    }

    public boolean canRemoveViews()
    {
        return false;
    }

    void dispatchStart()
    {
        if (mListeners == null) return;

        ArrayList<TransitionListener> listeners = new ArrayList<>(mListeners);
        for (TransitionListener listener : listeners)
        {
            if (listener != null) listener.onTransitionStart(this);
        }
    }

    void dispatchEnd()
    {
        if (mListeners == null) return;

        ArrayList<TransitionListener> listeners = new ArrayList<>(mListeners);
        for (TransitionListener listener : listeners)
        {
            if (listener != null) listener.onTransitionEnd(this);
        }
    }

    void dispatchCancel()
    {
        if (mListeners == null) return;

        ArrayList<TransitionListener> listeners = new ArrayList<>(mListeners);
        for (TransitionListener listener : listeners)
        {
            if (listener != null) listener.onTransitionCancel(this);
        }
    }

    void playStart()
    {
        dispatchStart();
    }

    void playEnd()
    {
        dispatchEnd();
    }

    @Override
    public String toString()
    {
        return toString("");
    }

    String toString(String indent)
    {
        StringBuilder builder = new StringBuilder(indent);
        builder.append(getClass().getSimpleName()).append("@").append(Integer.toHexString(hashCode())).append(": ");

        if (mDuration != -1) builder.append("dur(").append(mDuration).append(") ");
        if (mStartDelay != -1) builder.append("dly(").append(mStartDelay).append(") ");
        if (mInterpolator != null) builder.append("interp(").append(mInterpolator).append(") ");

        if (!mTargetIds.isEmpty() || !mTargets.isEmpty())
        {
            builder.append("tgts(");
            builder.append(mTargetIds);
            builder.append(mTargets);
            builder.append(")");
        }

        return builder.toString();
    }

    @Override
    public Transition clone()
    {
        try
        {
            Transition clone = (Transition)super.clone();
            clone.mTargetIds = new ArrayList<>(mTargetIds);
            clone.mTargets = new ArrayList<>(mTargets);
            if (mTargetNames != null) clone.mTargetNames = new ArrayList<>(mTargetNames);
            if (mTargetTypes != null) clone.mTargetTypes = new ArrayList<>(mTargetTypes);
            if (mListeners != null) clone.mListeners = new ArrayList<>(mListeners);
            return clone;
        }
        catch (CloneNotSupportedException exception)
        {
            return null;
        }
    }

    public String getName()
    {
        return mName;
    }

    public interface TransitionListener
    {
        void onTransitionStart(Transition transition);

        void onTransitionEnd(Transition transition);

        void onTransitionCancel(Transition transition);

        void onTransitionPause(Transition transition);

        void onTransitionResume(Transition transition);
    }

    public static abstract class EpicenterCallback
    {
        public EpicenterCallback()
        {
        }

        public abstract Rect onGetEpicenter(Transition transition);
    }
}
