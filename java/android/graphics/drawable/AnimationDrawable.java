package android.graphics.drawable;

import android.content.res.Resources;
import android.os.SystemClock;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class AnimationDrawable extends DrawableContainer implements Runnable, Animatable
{
    private AnimationState mAnimationState;
    private int mCurrentFrame = 0;
    private boolean mRunning;
    private boolean mAnimating;
    private boolean mMutated;

    public AnimationDrawable()
    {
        this(null, null);
    }

    private AnimationDrawable(AnimationState state, Resources resources)
    {
        setConstantState(new AnimationState(state, this, resources));
        if (state != null) setFrame(0, true, false);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart)
    {
        boolean changed = super.setVisible(visible, restart);

        if (visible)
        {
            if (restart || changed)
            {
                boolean startFromZero = restart || (!mRunning && !mAnimationState.mOneShot) || mCurrentFrame >= getChildCount();
                setFrame(startFromZero ? 0 : mCurrentFrame, true, mAnimating);
            }
        }
        else
        {
            unscheduleSelf(this);
        }

        return changed;
    }

    @Override
    public void start()
    {
        mAnimating = true;

        if (!isRunning()) setFrame(0, false, getChildCount() > 1 || !mAnimationState.mOneShot);
    }

    @Override
    public void stop()
    {
        mAnimating = false;

        if (isRunning())
        {
            mCurrentFrame = 0;
            unscheduleSelf(this);
        }
    }

    @Override
    public boolean isRunning()
    {
        return mRunning;
    }

    @Override
    public void run()
    {
        nextFrame(false);
    }

    @Override
    public void unscheduleSelf(Runnable what)
    {
        mRunning = false;
        super.unscheduleSelf(what);
    }

    public int getNumberOfFrames()
    {
        return getChildCount();
    }

    public Drawable getFrame(int index)
    {
        return getChild(index);
    }

    public int getDuration(int index)
    {
        return index < 0 || index >= getChildCount() ? 0 : mAnimationState.mDurations[index];
    }

    public boolean isOneShot()
    {
        return mAnimationState.mOneShot;
    }

    public void setOneShot(boolean oneShot)
    {
        mAnimationState.mOneShot = oneShot;
    }

    public void addFrame(Drawable frame, int duration)
    {
        if (frame == null) throw new NullPointerException("frame");

        mAnimationState.addFrame(frame, duration);

        if (getCurrentIndex() < 0) setFrame(0, true, false);
    }

    private void nextFrame(boolean unschedule)
    {
        int next = mCurrentFrame + 1;
        int frameCount = getChildCount();
        boolean oneShot = mAnimationState.mOneShot;
        boolean lastFrame = oneShot && next >= frameCount - 1;

        if (!oneShot && next >= frameCount) next = 0;

        setFrame(next, unschedule, !lastFrame);
    }

    private void setFrame(int frame, boolean unschedule, boolean animate)
    {
        if (frame >= getChildCount()) return;

        mAnimating = animate;
        mCurrentFrame = frame;
        selectDrawable(frame);

        if (unschedule || animate) unscheduleSelf(this);

        if (animate)
        {
            mCurrentFrame = frame;
            mRunning = true;
            scheduleSelf(this, SystemClock.uptimeMillis() + mAnimationState.mDurations[frame]);
        }
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        mAnimationState.mOneShot = DrawableInflater.flag(attributes, "oneshot", false);
        mAnimationState.setVariablePadding(DrawableInflater.flag(attributes, "variablePadding", false));

        boolean visible = DrawableInflater.flag(attributes, "visible", true);
        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            if (!"item".equals(parser.getName()))
            {
                DrawableInflater.skipElement(parser);
                continue;
            }

            int duration = DrawableInflater.integer(attributes, "duration", -1);
            if (duration < 0) throw new org.xmlpull.v1.XmlPullParserException("<item> tag requires a 'duration' attribute");

            Drawable frame = DrawableInflater.itemDrawable(resources, parser, attributes, theme);
            if (frame == null) throw new org.xmlpull.v1.XmlPullParserException("<item> tag requires a 'drawable' attribute");

            mAnimationState.addFrame(frame, duration);
        }

        setFrame(0, true, false);
        super.setVisible(visible, true);
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            mAnimationState.mutate();
            mMutated = true;
        }

        return this;
    }

    @Override
    protected void setConstantState(DrawableContainerState state)
    {
        super.setConstantState(state);

        if (state instanceof AnimationState) mAnimationState = (AnimationState) state;
    }

    private static final class AnimationState extends DrawableContainerState
    {
        private int[] mDurations;
        private boolean mOneShot;

        AnimationState(AnimationState original, AnimationDrawable owner, Resources resources)
        {
            super(original, owner, resources);

            mDurations = new int[mDrawables.length];

            if (original != null)
            {
                System.arraycopy(original.mDurations, 0, mDurations, 0, Math.min(original.mDurations.length, mDurations.length));
                mOneShot = original.mOneShot;
            }
        }

        void mutate()
        {
            mDurations = mDurations.clone();
        }

        void addFrame(Drawable frame, int duration)
        {
            int position = addChild(frame);
            mDurations[position] = duration;
        }

        @Override
        public Drawable newDrawable()
        {
            return new AnimationDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources resources)
        {
            return new AnimationDrawable(this, resources);
        }

        @Override
        public void growArray(int oldSize, int newSize)
        {
            super.growArray(oldSize, newSize);

            int[] grown = new int[newSize];
            System.arraycopy(mDurations, 0, grown, 0, Math.min(oldSize, mDurations.length));
            mDurations = grown;
        }
    }
}
