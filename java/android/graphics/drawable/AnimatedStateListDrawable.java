package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

public class AnimatedStateListDrawable extends StateListDrawable
{
    private static final long DEFAULT_TRANSITION_MILLIS = 300;
    private static final int ATTRIBUTE_ID = 0x010100d0;
    private static final int ATTRIBUTE_DRAWABLE = 0x01010199;

    private final Drawable.Callback mForwarder = new Forwarder();

    private final Runnable mFinisher = new Runnable()
    {
        @Override
        public void run()
        {
            endTransition();
        }
    };

    private final Animatable2.AnimationCallback mTransitionCallback = new Animatable2.AnimationCallback()
    {
        @Override
        public void onAnimationEnd(Drawable drawable)
        {
            if (drawable == mActiveTransition) endTransition();
        }
    };

    private Drawable mActiveTransition;
    private Handler mHandler;
    private AnimatedStateListState mAnimatedState;
    private boolean mMutated;

    public AnimatedStateListDrawable()
    {
        this(null, null);
    }

    private AnimatedStateListDrawable(AnimatedStateListState state, Resources resources)
    {
        super(null);

        AnimatedStateListState created = new AnimatedStateListState(state, this, resources);
        setConstantState(created);
        onStateChange(getState());
        jumpToCurrentState();
    }

    @Override
    public void addState(int[] stateSet, Drawable drawable)
    {
        addState(stateSet, drawable, 0);
    }

    public void addState(int[] stateSet, Drawable drawable, int id)
    {
        if (drawable == null) throw new IllegalArgumentException("Drawable must not be null");

        mAnimatedState.addStateSet(stateSet == null ? android.util.StateSet.WILD_CARD : stateSet, drawable, id);
        onStateChange(getState());
    }

    public <T extends Drawable & Animatable> void addTransition(int fromId, int toId, T transition, boolean reversible)
    {
        if (transition == null) throw new IllegalArgumentException("Transition drawable must not be null");

        mAnimatedState.mTransitions.add(new Transition(fromId, toId, transition, reversible));
    }

    @Override
    public boolean isStateful()
    {
        return true;
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        if (mAnimatedState == null) return super.onStateChange(stateSet);

        int previous = idAt(getCurrentIndex());
        boolean changed = super.onStateChange(stateSet);
        int next = idAt(getCurrentIndex());

        if (previous != next && previous != 0 && next != 0) beginTransition(previous, next);

        return changed;
    }

    private int idAt(int index)
    {
        if (mAnimatedState == null || index < 0 || index >= mAnimatedState.getChildCount()) return 0;

        return mAnimatedState.mStateIds[index];
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            mAnimatedState.mStateIds = mAnimatedState.mStateIds.clone();

            for (Transition transition : mAnimatedState.mTransitions) transition.drawable.mutate();

            mMutated = true;
        }

        return this;
    }

    @Override
    protected void setConstantState(DrawableContainerState state)
    {
        super.setConstantState(state);

        if (state instanceof AnimatedStateListState) mAnimatedState = (AnimatedStateListState) state;
    }

    private static final class AnimatedStateListState extends StateListState
    {
        private int[] mStateIds;
        private ArrayList<Transition> mTransitions = new ArrayList<>();

        AnimatedStateListState(AnimatedStateListState original, AnimatedStateListDrawable owner, Resources resources)
        {
            super(original, owner, resources);

            mStateIds = new int[mDrawables.length];

            if (original == null) return;

            System.arraycopy(original.mStateIds, 0, mStateIds, 0, Math.min(original.mStateIds.length, mStateIds.length));

            for (Transition transition : original.mTransitions)
            {
                ConstantState state = transition.drawable.getConstantState();
                Drawable copy = state == null ? transition.drawable : state.newDrawable(resources);
                mTransitions.add(new Transition(transition.fromId, transition.toId, copy, transition.reversible));
            }
        }

        int addStateSet(int[] stateSet, Drawable drawable, int id)
        {
            int position = addStateSet(stateSet, drawable);
            mStateIds[position] = id;
            return position;
        }

        @Override
        public Drawable newDrawable()
        {
            return new AnimatedStateListDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources resources)
        {
            return new AnimatedStateListDrawable(this, resources);
        }

        @Override
        public void growArray(int oldSize, int newSize)
        {
            super.growArray(oldSize, newSize);

            int[] grown = new int[newSize];
            System.arraycopy(mStateIds, 0, grown, 0, Math.min(oldSize, mStateIds.length));
            mStateIds = grown;
        }
    }

    private void beginTransition(int fromId, int toId)
    {
        endTransition();

        for (Transition transition : mAnimatedState.mTransitions)
        {
            if (transition.fromId != fromId || transition.toId != toId) continue;

            Drawable drawable = transition.drawable;
            drawable.setCallback(mForwarder);
            drawable.setBounds(getBounds());
            drawable.setAlpha(getAlpha());
            drawable.setVisible(isVisible(), true);

            mActiveTransition = drawable;

            if (drawable instanceof Animatable2) ((Animatable2) drawable).registerAnimationCallback(mTransitionCallback);

            ((Animatable) drawable).start();

            if (!(drawable instanceof Animatable2)) handler().postDelayed(mFinisher, durationOf(drawable));

            invalidateSelf();
            return;
        }
    }

    private void endTransition()
    {
        Drawable active = mActiveTransition;
        if (active == null) return;

        mActiveTransition = null;
        handler().removeCallbacks(mFinisher);

        if (active instanceof Animatable2) ((Animatable2) active).unregisterAnimationCallback(mTransitionCallback);
        if (((Animatable) active).isRunning()) ((Animatable) active).stop();

        invalidateSelf();
    }

    private static long durationOf(Drawable drawable)
    {
        if (!(drawable instanceof AnimationDrawable)) return DEFAULT_TRANSITION_MILLIS;

        AnimationDrawable frames = (AnimationDrawable) drawable;
        long total = 0;

        for (int index = 0; index < frames.getNumberOfFrames(); index++) total += frames.getDuration(index);

        return total;
    }

    private Handler handler()
    {
        if (mHandler == null)
        {
            Looper looper = Looper.myLooper();
            mHandler = new Handler(looper != null ? looper : Looper.getMainLooper());
        }

        return mHandler;
    }

    @Override
    public void jumpToCurrentState()
    {
        super.jumpToCurrentState();
        endTransition();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart)
    {
        boolean changed = super.setVisible(visible, restart);
        if (mActiveTransition != null && (changed || restart)) mActiveTransition.setVisible(visible, restart);

        return changed;
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mActiveTransition == null)
        {
            super.draw(canvas);
            return;
        }

        mActiveTransition.setBounds(getBounds());
        mActiveTransition.draw(canvas);
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if ("item".equals(name))
            {
                int id = DrawableInflater.reference(attributes, "id", 0);
                int[] stateSet = readItemStateSet(attributes);
                Drawable drawable = DrawableInflater.itemDrawable(resources, parser, attributes, theme);

                if (drawable != null) addState(stateSet, drawable, id);
                continue;
            }

            if ("transition".equals(name))
            {
                int fromId = DrawableInflater.reference(attributes, "fromId", 0);
                int toId = DrawableInflater.reference(attributes, "toId", 0);
                boolean reversible = DrawableInflater.flag(attributes, "reversible", false);
                Drawable drawable = DrawableInflater.itemDrawable(resources, parser, attributes, theme);

                if (drawable instanceof Animatable)
                {
                    mAnimatedState.mTransitions.add(new Transition(fromId, toId, drawable, reversible));
                }
                continue;
            }

            DrawableInflater.skipElement(parser);
        }

        onStateChange(getState());
    }

    private static int[] readItemStateSet(AttributeSet attributes)
    {
        ArrayList<Integer> collected = new ArrayList<>();

        for (int index = 0; index < attributes.getAttributeCount(); index++)
        {
            int name = attributes.getAttributeNameResource(index);
            if (name == 0 || name == ATTRIBUTE_ID || name == ATTRIBUTE_DRAWABLE) continue;

            String value = attributes.getAttributeValue(index);
            if (!"true".equals(value) && !"false".equals(value) && !isBoolean(attributes, index)) continue;

            collected.add(attributes.getAttributeBooleanValue(index, false) ? name : -name);
        }

        int[] stateSet = new int[collected.size()];
        for (int index = 0; index < stateSet.length; index++) stateSet[index] = collected.get(index);

        return stateSet;
    }

    private static boolean isBoolean(AttributeSet attributes, int index)
    {
        if (!(attributes instanceof android.content.res.XmlBlock.Parser)) return false;

        int[] packed = ((android.content.res.XmlBlock.Parser) attributes).getAttributeTypedValue(index);
        return packed != null && packed[0] == android.util.TypedValue.TYPE_INT_BOOLEAN;
    }

    private static final class Transition
    {
        final int fromId;
        final int toId;
        final Drawable drawable;
        final boolean reversible;

        Transition(int fromId, int toId, Drawable drawable, boolean reversible)
        {
            this.fromId = fromId;
            this.toId = toId;
            this.drawable = drawable;
            this.reversible = reversible;
        }
    }

    private final class Forwarder implements Drawable.Callback
    {
        @Override
        public void invalidateDrawable(Drawable who)
        {
            invalidateSelf();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when)
        {
            scheduleSelf(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what)
        {
            unscheduleSelf(what);
        }
    }
}
