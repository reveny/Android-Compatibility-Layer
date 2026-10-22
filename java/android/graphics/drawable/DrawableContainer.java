package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Rect;

public class DrawableContainer extends Drawable implements Drawable.Callback
{
    private DrawableContainerState mDrawableContainerState;
    private boolean mExplicitState;
    private Drawable mCurrent;
    private int mCurrentIndex = -1;
    private boolean mAutoMirrored;
    private final Rect mHotspotBounds = new Rect();
    private boolean mHasHotspotBounds;
    private boolean mMutated;

    public DrawableContainer()
    {
    }

    private DrawableContainerState containerState()
    {
        if (mDrawableContainerState == null) mDrawableContainerState = new ContainerState(this);
        return mDrawableContainerState;
    }

    int addChild(Drawable drawable)
    {
        return containerState().addChild(drawable);
    }

    int getChildCount()
    {
        return containerState().getChildCount();
    }

    Drawable getChild(int index)
    {
        return containerState().getChild(index);
    }

    int getCurrentIndex()
    {
        return mCurrentIndex;
    }

    public boolean selectDrawable(int index)
    {
        if (index == mCurrentIndex) return false;

        Drawable wanted = index >= 0 && index < getChildCount() ? getChild(index) : null;

        if (mCurrent != null) mCurrent.setVisible(false, false);

        mCurrent = wanted;
        mCurrentIndex = wanted == null ? -1 : index;

        if (mCurrent != null)
        {
            mCurrent.setVisible(isVisible(), true);
            mCurrent.setAlpha(getAlpha());
            mCurrent.setState(getState());
            mCurrent.setLevel(getLevel());
            mCurrent.setLayoutDirection(getLayoutDirection());
            if (getColorFilter() != null) mCurrent.setColorFilter(getColorFilter());
            mCurrent.setBounds(getBounds());
            if (mHasHotspotBounds)
            {
                mCurrent.setHotspotBounds(mHotspotBounds.left, mHotspotBounds.top, mHotspotBounds.right, mHotspotBounds.bottom);
            }
        }

        invalidateSelf();
        return true;
    }

    @Override
    public Drawable getCurrent()
    {
        return mCurrent;
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mCurrent == null) return;

        mCurrent.setBounds(getBounds());
        mCurrent.setAlpha(getAlpha());
        mCurrent.draw(canvas);
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations() | containerState().getChangingConfigurations();
    }

    @Override
    public boolean getPadding(Rect padding)
    {
        Rect constant = containerState().getConstantPadding();
        if (constant != null)
        {
            padding.set(constant);
            return (constant.left | constant.top | constant.bottom | constant.right) != 0;
        }

        return mCurrent != null ? mCurrent.getPadding(padding) : super.getPadding(padding);
    }

    @Override
    public Insets getOpticalInsets()
    {
        return mCurrent != null ? mCurrent.getOpticalInsets() : Insets.NONE;
    }

    @Override
    public void getOutline(Outline outline)
    {
        if (mCurrent != null) mCurrent.getOutline(outline);
    }

    @Override
    public void setAlpha(int alpha)
    {
        super.setAlpha(alpha);
        if (mCurrent != null) mCurrent.setAlpha(alpha);
    }

    @Override
    public int getAlpha()
    {
        return super.getAlpha();
    }

    @Override
    public void setDither(boolean dither)
    {
        if (mCurrent != null) mCurrent.setDither(dither);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter filter)
    {
        super.setColorFilter(filter);

        DrawableContainerState state = containerState();
        for (int index = 0; index < state.getChildCount(); index++) state.getChild(index).setColorFilter(filter);
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        return mCurrent != null && mCurrent.setLevel(level);
    }

    @Override
    protected boolean onStateChange(int[] state)
    {
        return mCurrent != null && mCurrent.setState(state);
    }

    @Override
    public boolean onLayoutDirectionChanged(int layoutDirection)
    {
        boolean changed = false;
        DrawableContainerState state = containerState();

        for (int index = 0; index < state.getChildCount(); index++)
        {
            if (state.getChild(index).setLayoutDirection(layoutDirection)) changed = true;
        }

        return changed;
    }

    @Override
    public void setTintList(android.content.res.ColorStateList tint)
    {
        DrawableContainerState state = containerState();
        for (int index = 0; index < state.getChildCount(); index++) state.getChild(index).setTintList(tint);
    }

    @Override
    public void setTintBlendMode(android.graphics.BlendMode blendMode)
    {
        DrawableContainerState state = containerState();
        for (int index = 0; index < state.getChildCount(); index++) state.getChild(index).setTintBlendMode(blendMode);
    }

    public void setEnterFadeDuration(int milliseconds)
    {
        containerState().setEnterFadeDuration(milliseconds);
    }

    public void setExitFadeDuration(int milliseconds)
    {
        containerState().setExitFadeDuration(milliseconds);
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        if (mCurrent != null) mCurrent.setBounds(bounds);
    }

    @Override
    public boolean isStateful()
    {
        return containerState().isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        return mCurrent != null && mCurrent.hasFocusStateSpecified();
    }

    @Override
    public void setAutoMirrored(boolean mirrored)
    {
        mAutoMirrored = mirrored;
        if (mCurrent != null) mCurrent.setAutoMirrored(mirrored);
    }

    @Override
    public boolean isAutoMirrored()
    {
        return mAutoMirrored;
    }

    @Override
    public void jumpToCurrentState()
    {
        if (mCurrent != null) mCurrent.jumpToCurrentState();
    }

    @Override
    public void setHotspot(float x, float y)
    {
        if (mCurrent != null) mCurrent.setHotspot(x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom)
    {
        mHotspotBounds.set(left, top, right, bottom);
        mHasHotspotBounds = true;
        if (mCurrent != null) mCurrent.setHotspotBounds(left, top, right, bottom);
    }

    @Override
    public void getHotspotBounds(Rect outRect)
    {
        if (mHasHotspotBounds) outRect.set(mHotspotBounds);
        else super.getHotspotBounds(outRect);
    }

    @Override
    public boolean setState(int[] stateSet)
    {
        boolean changed = super.setState(stateSet);
        if (mCurrent != null && mCurrent.setState(stateSet)) changed = true;

        return changed;
    }

    @Override
    public int getIntrinsicWidth()
    {
        DrawableContainerState state = containerState();
        if (state.isConstantSize()) return state.getConstantWidth();
        return mCurrent == null ? -1 : mCurrent.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight()
    {
        DrawableContainerState state = containerState();
        if (state.isConstantSize()) return state.getConstantHeight();
        return mCurrent == null ? -1 : mCurrent.getIntrinsicHeight();
    }

    @Override
    public int getMinimumWidth()
    {
        DrawableContainerState state = containerState();
        if (state.isConstantSize()) return state.getConstantMinimumWidth();
        return mCurrent == null ? 0 : mCurrent.getMinimumWidth();
    }

    @Override
    public int getMinimumHeight()
    {
        DrawableContainerState state = containerState();
        if (state.isConstantSize()) return state.getConstantMinimumHeight();
        return mCurrent == null ? 0 : mCurrent.getMinimumHeight();
    }

    @Override
    public void invalidateDrawable(Drawable who)
    {
        Callback callback = getCallback();
        if (who == mCurrent && callback != null) callback.invalidateDrawable(this);
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when)
    {
        Callback callback = getCallback();
        if (who == mCurrent && callback != null) callback.scheduleDrawable(this, what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what)
    {
        Callback callback = getCallback();
        if (who == mCurrent && callback != null) callback.unscheduleDrawable(this, what);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart)
    {
        boolean changed = super.setVisible(visible, restart);
        if (mCurrent != null) mCurrent.setVisible(visible, restart);

        return changed;
    }

    @Override
    public int getOpacity()
    {
        return mCurrent == null || !mCurrent.isVisible() ? PixelFormat.TRANSPARENT : containerState().getOpacity();
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        DrawableContainerState state = containerState();
        for (int index = 0; index < state.getChildCount(); index++)
        {
            Drawable child = state.getChild(index);
            if (child.canApplyTheme()) child.applyTheme(theme);
        }
    }

    @Override
    public boolean canApplyTheme()
    {
        return containerState().canApplyTheme();
    }

    @Override
    public ConstantState getConstantState()
    {
        if (mExplicitState && mDrawableContainerState != null && mDrawableContainerState.canConstantState())
        {
            mDrawableContainerState.mChangingConfigurations = getChangingConfigurations();
            return mDrawableContainerState;
        }

        return null;
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            DrawableContainerState state = containerState();
            for (int index = 0; index < state.getChildCount(); index++) state.getChild(index).mutate();
            mMutated = true;
        }

        return this;
    }

    protected void setConstantState(DrawableContainerState state)
    {
        mDrawableContainerState = state;
        mExplicitState = state != null;

        if (mCurrentIndex >= 0)
        {
            mCurrent = state == null ? null : state.getChild(mCurrentIndex);
            if (mCurrent == null) mCurrentIndex = -1;
        }

        if (state == null) return;

        state.mOwner = this;
        for (int index = 0; index < state.getChildCount(); index++) state.mDrawables[index].setCallback(this);
    }

    private static final class ContainerState extends DrawableContainerState
    {
        ContainerState(DrawableContainer owner)
        {
            super(null, owner, null);
        }

        @Override
        public Drawable newDrawable()
        {
            DrawableContainer copy = new DrawableContainer();
            for (int index = 0; index < getChildCount(); index++)
            {
                Drawable child = getChild(index);
                ConstantState childState = child.getConstantState();
                copy.addChild(childState != null ? childState.newDrawable() : child);
            }
            return copy;
        }
    }

    public abstract static class DrawableContainerState extends ConstantState
    {
        DrawableContainer mOwner;
        Drawable[] mDrawables;
        int mNumChildren;
        int mChangingConfigurations;
        int mChildrenChangingConfigurations;
        boolean mVariablePadding;
        Rect mConstantPadding;
        boolean mConstantSize;
        boolean mComputedConstantSize;
        int mConstantWidth;
        int mConstantHeight;
        int mConstantMinimumWidth;
        int mConstantMinimumHeight;
        int mEnterFadeDuration;
        int mExitFadeDuration;

        DrawableContainerState()
        {
            this(null, null, null);
        }

        DrawableContainerState(DrawableContainerState original, DrawableContainer owner, Resources resources)
        {
            mOwner = owner;

            if (original == null)
            {
                mDrawables = new Drawable[10];
                return;
            }

            mChangingConfigurations = original.mChangingConfigurations;
            mChildrenChangingConfigurations = original.mChildrenChangingConfigurations;
            mVariablePadding = original.mVariablePadding;
            mConstantSize = original.mConstantSize;
            mEnterFadeDuration = original.mEnterFadeDuration;
            mExitFadeDuration = original.mExitFadeDuration;
            mDrawables = new Drawable[Math.max(10, original.mDrawables.length)];
            mNumChildren = original.mNumChildren;

            for (int index = 0; index < mNumChildren; index++)
            {
                Drawable child = original.mDrawables[index];
                ConstantState childState = child.getConstantState();
                Drawable copy = childState != null ? childState.newDrawable(resources) : child;
                if (owner != null) copy.setCallback(owner);
                mDrawables[index] = copy;
            }
        }

        @Override
        public int getChangingConfigurations()
        {
            return mChangingConfigurations | mChildrenChangingConfigurations;
        }

        public final int addChild(Drawable drawable)
        {
            int position = mNumChildren;
            if (position >= mDrawables.length) growArray(position, position + 10);

            if (mOwner != null)
            {
                drawable.setVisible(mOwner.isVisible(), true);
                drawable.setCallback(mOwner);
            }
            else
            {
                drawable.setVisible(false, true);
            }

            mDrawables[position] = drawable;
            mNumChildren++;
            mChildrenChangingConfigurations |= drawable.getChangingConfigurations();
            mComputedConstantSize = false;
            mConstantPadding = null;

            return position;
        }

        public final int getChildCount()
        {
            return mNumChildren;
        }

        public final Drawable[] getChildren()
        {
            return mDrawables;
        }

        public final Drawable getChild(int index)
        {
            return index < 0 || index >= mNumChildren ? null : mDrawables[index];
        }

        @Override
        public boolean canApplyTheme()
        {
            for (int index = 0; index < mNumChildren; index++)
            {
                if (mDrawables[index].canApplyTheme()) return true;
            }
            return false;
        }

        public final void setVariablePadding(boolean variable)
        {
            mVariablePadding = variable;
            mConstantPadding = null;
        }

        public final Rect getConstantPadding()
        {
            if (mVariablePadding || mNumChildren == 0) return null;
            if (mConstantPadding != null) return mConstantPadding;

            Rect result = null;
            Rect candidate = new Rect();

            for (int index = 0; index < mNumChildren; index++)
            {
                if (!mDrawables[index].getPadding(candidate)) continue;

                if (result == null) result = new Rect(0, 0, 0, 0);
                if (candidate.left > result.left) result.left = candidate.left;
                if (candidate.top > result.top) result.top = candidate.top;
                if (candidate.right > result.right) result.right = candidate.right;
                if (candidate.bottom > result.bottom) result.bottom = candidate.bottom;
            }

            mConstantPadding = result;
            return result;
        }

        public final void setConstantSize(boolean constant)
        {
            mConstantSize = constant;
        }

        public final boolean isConstantSize()
        {
            return mConstantSize;
        }

        public final int getConstantWidth()
        {
            if (!mComputedConstantSize) computeConstantSize();
            return mConstantWidth;
        }

        public final int getConstantHeight()
        {
            if (!mComputedConstantSize) computeConstantSize();
            return mConstantHeight;
        }

        public final int getConstantMinimumWidth()
        {
            if (!mComputedConstantSize) computeConstantSize();
            return mConstantMinimumWidth;
        }

        public final int getConstantMinimumHeight()
        {
            if (!mComputedConstantSize) computeConstantSize();
            return mConstantMinimumHeight;
        }

        protected void computeConstantSize()
        {
            mComputedConstantSize = true;
            mConstantWidth = -1;
            mConstantHeight = -1;
            mConstantMinimumWidth = 0;
            mConstantMinimumHeight = 0;

            for (int index = 0; index < mNumChildren; index++)
            {
                Drawable child = mDrawables[index];
                mConstantWidth = Math.max(mConstantWidth, child.getIntrinsicWidth());
                mConstantHeight = Math.max(mConstantHeight, child.getIntrinsicHeight());
                mConstantMinimumWidth = Math.max(mConstantMinimumWidth, child.getMinimumWidth());
                mConstantMinimumHeight = Math.max(mConstantMinimumHeight, child.getMinimumHeight());
            }
        }

        public final void setEnterFadeDuration(int duration)
        {
            mEnterFadeDuration = duration;
        }

        public final int getEnterFadeDuration()
        {
            return mEnterFadeDuration;
        }

        public final void setExitFadeDuration(int duration)
        {
            mExitFadeDuration = duration;
        }

        public final int getExitFadeDuration()
        {
            return mExitFadeDuration;
        }

        public final int getOpacity()
        {
            if (mNumChildren == 0) return PixelFormat.TRANSPARENT;

            int opacity = mDrawables[0].getOpacity();
            for (int index = 1; index < mNumChildren; index++) opacity = Drawable.resolveOpacity(opacity, mDrawables[index].getOpacity());
            return opacity;
        }

        public final boolean isStateful()
        {
            for (int index = 0; index < mNumChildren; index++)
            {
                if (mDrawables[index].isStateful()) return true;
            }
            return false;
        }

        public void growArray(int oldSize, int newSize)
        {
            Drawable[] grown = new Drawable[newSize];
            System.arraycopy(mDrawables, 0, grown, 0, Math.min(oldSize, mDrawables.length));
            mDrawables = grown;
        }

        public synchronized boolean canConstantState()
        {
            for (int index = 0; index < mNumChildren; index++)
            {
                if (mDrawables[index].getConstantState() == null) return false;
            }
            return true;
        }
    }
}
