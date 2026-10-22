package android.view.animation;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

public class AnimationSet extends Animation
{
    private static final int PROPERTY_FILL_AFTER_MASK = 0x1;
    private static final int PROPERTY_FILL_BEFORE_MASK = 0x2;
    private static final int PROPERTY_REPEAT_MODE_MASK = 0x4;
    private static final int PROPERTY_START_OFFSET_MASK = 0x8;
    private static final int PROPERTY_SHARE_INTERPOLATOR_MASK = 0x10;
    private static final int PROPERTY_DURATION_MASK = 0x20;
    private static final int PROPERTY_MORPH_MATRIX_MASK = 0x40;
    private static final int PROPERTY_CHANGE_BOUNDS_MASK = 0x80;

    private int mFlags = 0;
    private boolean mDirty = false;
    private boolean mHasAlpha = false;

    private ArrayList<Animation> mAnimations = new ArrayList<>();
    private Transformation mTempTransformation = new Transformation();
    private long mLastEnd = 0;
    private long[] mStoredOffsets = null;

    public AnimationSet(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        AnimationAttributes values = new AnimationAttributes(context, attributes);
        setFlag(PROPERTY_SHARE_INTERPOLATOR_MASK, values.getBoolean("shareInterpolator", true));
        init();

        if (values.has("duration")) mFlags |= PROPERTY_DURATION_MASK;
        if (values.has("fillBefore")) mFlags |= PROPERTY_FILL_BEFORE_MASK;
        if (values.has("fillAfter")) mFlags |= PROPERTY_FILL_AFTER_MASK;
        if (values.has("repeatMode")) mFlags |= PROPERTY_REPEAT_MODE_MASK;
        if (values.has("startOffset")) mFlags |= PROPERTY_START_OFFSET_MASK;
    }

    public AnimationSet(boolean shareInterpolator)
    {
        setFlag(PROPERTY_SHARE_INTERPOLATOR_MASK, shareInterpolator);
        init();
    }

    @Override
    protected AnimationSet clone() throws CloneNotSupportedException
    {
        AnimationSet animation = (AnimationSet)super.clone();
        animation.mTempTransformation = new Transformation();
        animation.mAnimations = new ArrayList<>();

        for (Animation child : mAnimations)
        {
            animation.mAnimations.add(child.clone());
        }

        if (mStoredOffsets != null) animation.mStoredOffsets = mStoredOffsets.clone();

        return animation;
    }

    private void setFlag(int mask, boolean value)
    {
        if (value) mFlags |= mask;
        else mFlags &= ~mask;
    }

    private void init()
    {
        mStartTime = 0;
    }

    @Override
    public void setFillAfter(boolean fillAfter)
    {
        mFlags |= PROPERTY_FILL_AFTER_MASK;
        super.setFillAfter(fillAfter);
    }

    @Override
    public void setFillBefore(boolean fillBefore)
    {
        mFlags |= PROPERTY_FILL_BEFORE_MASK;
        super.setFillBefore(fillBefore);
    }

    @Override
    public void setRepeatMode(int repeatMode)
    {
        mFlags |= PROPERTY_REPEAT_MODE_MASK;
        super.setRepeatMode(repeatMode);
    }

    @Override
    public void setStartOffset(long startOffset)
    {
        mFlags |= PROPERTY_START_OFFSET_MASK;
        super.setStartOffset(startOffset);
    }

    @Override
    public boolean hasAlpha()
    {
        if (mDirty)
        {
            mDirty = false;
            mHasAlpha = false;

            for (Animation child : mAnimations)
            {
                if (!child.hasAlpha()) continue;

                mHasAlpha = true;
                break;
            }
        }

        return mHasAlpha;
    }

    @Override
    public void setDuration(long durationMillis)
    {
        mFlags |= PROPERTY_DURATION_MASK;
        super.setDuration(durationMillis);
        mLastEnd = mStartOffset + mDuration;
    }

    public void addAnimation(Animation animation)
    {
        mAnimations.add(animation);

        boolean noMatrix = (mFlags & PROPERTY_MORPH_MATRIX_MASK) == 0;
        if (noMatrix && animation.willChangeTransformationMatrix()) mFlags |= PROPERTY_MORPH_MATRIX_MASK;

        boolean changeBounds = (mFlags & PROPERTY_CHANGE_BOUNDS_MASK) == 0;
        if (changeBounds && animation.willChangeBounds()) mFlags |= PROPERTY_CHANGE_BOUNDS_MASK;

        if ((mFlags & PROPERTY_DURATION_MASK) == PROPERTY_DURATION_MASK)
        {
            mLastEnd = mStartOffset + mDuration;
        }
        else if (mAnimations.size() == 1)
        {
            mDuration = animation.getStartOffset() + animation.getDuration();
            mLastEnd = mStartOffset + mDuration;
        }
        else
        {
            mLastEnd = Math.max(mLastEnd, mStartOffset + animation.getStartOffset() + animation.getDuration());
            mDuration = mLastEnd - mStartOffset;
        }

        mDirty = true;
    }

    @Override
    public void setStartTime(long startTimeMillis)
    {
        super.setStartTime(startTimeMillis);

        for (Animation child : mAnimations)
        {
            child.setStartTime(startTimeMillis);
        }
    }

    @Override
    public long getStartTime()
    {
        long startTime = Long.MAX_VALUE;

        for (Animation child : mAnimations)
        {
            startTime = Math.min(startTime, child.getStartTime());
        }

        return startTime;
    }

    @Override
    public void restrictDuration(long durationMillis)
    {
        super.restrictDuration(durationMillis);

        for (Animation child : mAnimations)
        {
            child.restrictDuration(durationMillis);
        }
    }

    @Override
    public long getDuration()
    {
        if ((mFlags & PROPERTY_DURATION_MASK) == PROPERTY_DURATION_MASK) return mDuration;

        long duration = 0;
        for (Animation child : mAnimations)
        {
            duration = Math.max(duration, child.getDuration());
        }

        return duration;
    }

    @Override
    public long computeDurationHint()
    {
        long duration = 0;

        for (int index = mAnimations.size() - 1; index >= 0; --index)
        {
            long hint = mAnimations.get(index).computeDurationHint();
            if (hint > duration) duration = hint;
        }

        return duration;
    }

    @Override
    public void initializeInvalidateRegion(int left, int top, int right, int bottom)
    {
        RectF region = mPreviousRegion;
        region.set(left, top, right, bottom);
        region.inset(-1.0f, -1.0f);

        if (!mFillBefore) return;

        Transformation temporary = mTempTransformation;
        Transformation previous = mPreviousTransformation;
        previous.clear();

        for (int index = mAnimations.size() - 1; index >= 0; --index)
        {
            Animation child = mAnimations.get(index);
            if (child.isFillEnabled() && !child.getFillBefore() && child.getStartOffset() != 0) continue;

            temporary.clear();
            Interpolator interpolator = child.mInterpolator;
            child.applyTransformation(interpolator != null ? interpolator.getInterpolation(0.0f) : 0.0f, temporary);
            previous.compose(temporary);
        }
    }

    @Override
    public boolean getTransformation(long currentTime, Transformation transformation)
    {
        boolean more = false;
        boolean started = false;
        boolean ended = true;

        transformation.clear();

        Transformation temporary = mTempTransformation;

        for (int index = mAnimations.size() - 1; index >= 0; --index)
        {
            Animation child = mAnimations.get(index);

            temporary.clear();
            more = child.getTransformation(currentTime, temporary, getScaleFactor()) || more;
            transformation.compose(temporary);

            started = started || child.hasStarted();
            ended = child.hasEnded() && ended;
        }

        if (started && !mStarted)
        {
            fireAnimationStart();
            mStarted = true;
        }

        if (ended != mEnded)
        {
            fireAnimationEnd();
            mEnded = ended;
        }

        return more;
    }

    @Override
    public void scaleCurrentDuration(float scale)
    {
        for (Animation child : mAnimations)
        {
            child.scaleCurrentDuration(scale);
        }
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
        super.initialize(width, height, parentWidth, parentHeight);

        boolean durationSet = (mFlags & PROPERTY_DURATION_MASK) == PROPERTY_DURATION_MASK;
        boolean fillAfterSet = (mFlags & PROPERTY_FILL_AFTER_MASK) == PROPERTY_FILL_AFTER_MASK;
        boolean fillBeforeSet = (mFlags & PROPERTY_FILL_BEFORE_MASK) == PROPERTY_FILL_BEFORE_MASK;
        boolean repeatModeSet = (mFlags & PROPERTY_REPEAT_MODE_MASK) == PROPERTY_REPEAT_MODE_MASK;
        boolean shareInterpolator = (mFlags & PROPERTY_SHARE_INTERPOLATOR_MASK) == PROPERTY_SHARE_INTERPOLATOR_MASK;
        boolean startOffsetSet = (mFlags & PROPERTY_START_OFFSET_MASK) == PROPERTY_START_OFFSET_MASK;

        if (shareInterpolator) ensureInterpolator();

        int count = mAnimations.size();
        long[] storedOffsets = mStoredOffsets;

        if (startOffsetSet)
        {
            if (storedOffsets == null || storedOffsets.length != count)
            {
                storedOffsets = new long[count];
                mStoredOffsets = storedOffsets;
            }
        }
        else if (storedOffsets != null)
        {
            storedOffsets = null;
            mStoredOffsets = null;
        }

        for (int index = 0; index < count; index++)
        {
            Animation child = mAnimations.get(index);

            if (durationSet) child.setDuration(mDuration);
            if (fillAfterSet) child.setFillAfter(mFillAfter);
            if (fillBeforeSet) child.setFillBefore(mFillBefore);
            if (repeatModeSet) child.setRepeatMode(mRepeatMode);
            if (shareInterpolator) child.setInterpolator(mInterpolator);

            if (startOffsetSet)
            {
                long offset = child.getStartOffset();
                child.setStartOffset(offset + mStartOffset);
                storedOffsets[index] = offset;
            }

            child.initialize(width, height, parentWidth, parentHeight);
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        restoreChildrenStartOffset();
    }

    void restoreChildrenStartOffset()
    {
        long[] offsets = mStoredOffsets;
        if (offsets == null) return;

        int count = Math.min(offsets.length, mAnimations.size());
        for (int index = 0; index < count; index++)
        {
            mAnimations.get(index).setStartOffset(offsets[index]);
        }
    }

    public List<Animation> getAnimations()
    {
        return mAnimations;
    }

    @Override
    public boolean willChangeTransformationMatrix()
    {
        return (mFlags & PROPERTY_MORPH_MATRIX_MASK) == PROPERTY_MORPH_MATRIX_MASK;
    }

    @Override
    public boolean willChangeBounds()
    {
        return (mFlags & PROPERTY_CHANGE_BOUNDS_MASK) == PROPERTY_CHANGE_BOUNDS_MASK;
    }
}
