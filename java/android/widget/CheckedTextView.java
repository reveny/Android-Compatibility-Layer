package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.AbsSavedState;
import android.view.Gravity;
import android.view.StyleableSet;

public class CheckedTextView extends TextView implements Checkable
{
    private static final int STATE_CHECKED = 0x010100a0;

    private static StyleableSet sStyleable;

    private boolean mChecked;
    private int mCheckMarkResource;
    private Drawable mCheckMarkDrawable;
    private ColorStateList mCheckMarkTintList;
    private PorterDuff.Mode mCheckMarkTintMode;
    private BlendMode mCheckMarkBlendMode;
    private int mBasePaddingRight;
    private int mCheckMarkWidth;
    private int mCheckMarkGravity = Gravity.END;

    public CheckedTextView(Context context)
    {
        this(context, null);
    }

    public CheckedTextView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public CheckedTextView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public CheckedTextView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mBasePaddingRight = getPaddingRight();
        if (attributes != null) readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "checked", "checkMark", "checkMarkGravity");
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int markIndex = sStyleable.indexOf("checkMark");
        if (markIndex >= 0 && array.hasValue(markIndex))
        {
            try
            {
                Drawable mark = array.getDrawable(markIndex);
                if (mark != null) setCheckMarkDrawable(mark);
            }
            catch (RuntimeException unsupported)
            {
                android.util.Log.w("CheckedTextView", "the check mark could not be loaded", unsupported);
            }
        }

        int gravityIndex = sStyleable.indexOf("checkMarkGravity");
        if (gravityIndex >= 0) mCheckMarkGravity = array.getInt(gravityIndex, Gravity.END);

        int checkedIndex = sStyleable.indexOf("checked");
        if (checkedIndex >= 0) setChecked(array.getBoolean(checkedIndex, false));

        array.recycle();
    }

    @Override
    public void toggle()
    {
        setChecked(!mChecked);
    }

    @Override
    public boolean isChecked()
    {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked)
    {
        if (mChecked == checked) return;

        mChecked = checked;
        refreshDrawableState();
        invalidate();
    }

    public void setCheckMarkDrawable(int resId)
    {
        if (resId != 0 && resId == mCheckMarkResource) return;

        Drawable drawable = resId == 0 ? null : getContext().getDrawable(resId);
        setCheckMarkDrawableInternal(drawable, resId);
    }

    public void setCheckMarkDrawable(Drawable drawable)
    {
        setCheckMarkDrawableInternal(drawable, 0);
    }

    private void setCheckMarkDrawableInternal(Drawable drawable, int resId)
    {
        if (mCheckMarkDrawable != null)
        {
            mCheckMarkDrawable.setCallback(null);
            unscheduleDrawable(mCheckMarkDrawable);
        }

        mCheckMarkDrawable = drawable;
        mCheckMarkResource = resId;

        if (drawable != null)
        {
            drawable.setCallback(this);
            drawable.setVisible(getVisibility() == VISIBLE, false);
            drawable.setState(getDrawableState());
            mCheckMarkWidth = Math.max(0, drawable.getIntrinsicWidth());
            applyCheckMarkTint();
        }
        else
        {
            mCheckMarkWidth = 0;
        }

        updatePadding();
        requestLayout();
        invalidate();
    }

    public void setCheckMarkTintList(ColorStateList tint)
    {
        mCheckMarkTintList = tint;
        applyCheckMarkTint();
    }

    public ColorStateList getCheckMarkTintList()
    {
        return mCheckMarkTintList;
    }

    public void setCheckMarkTintMode(PorterDuff.Mode tintMode)
    {
        mCheckMarkTintMode = tintMode;
        applyCheckMarkTint();
    }

    public void setCheckMarkTintBlendMode(BlendMode blendMode)
    {
        mCheckMarkBlendMode = blendMode;
        applyCheckMarkTint();
    }

    public PorterDuff.Mode getCheckMarkTintMode()
    {
        return mCheckMarkTintMode;
    }

    public BlendMode getCheckMarkTintBlendMode()
    {
        return mCheckMarkBlendMode;
    }

    private void applyCheckMarkTint()
    {
        if (mCheckMarkDrawable == null) return;
        if (mCheckMarkTintList == null && mCheckMarkTintMode == null && mCheckMarkBlendMode == null) return;

        mCheckMarkDrawable = mCheckMarkDrawable.mutate();
        if (mCheckMarkTintList != null) mCheckMarkDrawable.setTintList(mCheckMarkTintList);
        if (mCheckMarkTintMode != null) mCheckMarkDrawable.setTintMode(mCheckMarkTintMode);
        if (mCheckMarkBlendMode != null) mCheckMarkDrawable.setTintBlendMode(mCheckMarkBlendMode);
        if (mCheckMarkDrawable.isStateful()) mCheckMarkDrawable.setState(getDrawableState());
    }

    @Override
    public void setVisibility(int visibility)
    {
        super.setVisibility(visibility);
        if (mCheckMarkDrawable != null) mCheckMarkDrawable.setVisible(visibility == VISIBLE, false);
    }

    public void jumpDrawablesToCurrentState()
    {
        if (mCheckMarkDrawable != null) mCheckMarkDrawable.jumpToCurrentState();
    }

    @Override
    protected boolean verifyDrawable(Drawable who)
    {
        return who == mCheckMarkDrawable || super.verifyDrawable(who);
    }

    public Drawable getCheckMarkDrawable()
    {
        return mCheckMarkDrawable;
    }

    public void onRtlPropertiesChanged(int layoutDirection)
    {
        updatePadding();
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom)
    {
        mBasePaddingRight = right;
        super.setPadding(left, top, right + mCheckMarkWidth, bottom);
    }

    private void updatePadding()
    {
        super.setPadding(getPaddingLeft(), getPaddingTop(), mBasePaddingRight + mCheckMarkWidth, getPaddingBottom());
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        Drawable mark = mCheckMarkDrawable;
        if (mark == null) return;

        int height = mark.getIntrinsicHeight();
        if (height < 0) height = getHeight();

        int top = (getHeight() - height) / 2;
        int gravity = Gravity.getAbsoluteGravity(mCheckMarkGravity, getLayoutDirection());
        boolean atStart = (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.LEFT;

        int right = atStart ? getPaddingLeft() + mCheckMarkWidth : getWidth() - mBasePaddingRight;
        int left = right - mCheckMarkWidth;

        mark.setBounds(getScrollX() + left, top, getScrollX() + right, top + height);
        mark.draw(canvas);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace)
    {
        int[] state = super.onCreateDrawableState(extraSpace + 1);
        if (!mChecked) return state;

        for (int index = 0; index < state.length; index++)
        {
            if (state[index] != 0) continue;

            state[index] = STATE_CHECKED;
            return state;
        }

        int[] grown = new int[state.length + 1];
        System.arraycopy(state, 0, grown, 0, state.length);
        grown[state.length] = STATE_CHECKED;
        return grown;
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();

        if (mCheckMarkDrawable != null && mCheckMarkDrawable.isStateful() && mCheckMarkDrawable.setState(getDrawableState())) invalidate();
    }

    public void drawableHotspotChanged(float x, float y)
    {
        if (mCheckMarkDrawable != null) mCheckMarkDrawable.setHotspot(x, y);
    }

    public CharSequence getAccessibilityClassName()
    {
        return CheckedTextView.class.getName();
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.checked = isChecked();
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable)
    {
        if (!(parcelable instanceof SavedState))
        {
            super.onRestoreInstanceState(parcelable);
            return;
        }

        SavedState state = (SavedState)parcelable;
        super.onRestoreInstanceState(state.getSuperState());
        setChecked(state.checked);
        requestLayout();
    }

    static class SavedState extends BaseSavedState
    {
        boolean checked;

        SavedState(Parcelable superState)
        {
            super(superState == null ? AbsSavedState.EMPTY_STATE : superState);
        }

        private SavedState(Parcel source)
        {
            super(source);
            checked = source.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeInt(checked ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
        {
            @Override
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }
}
