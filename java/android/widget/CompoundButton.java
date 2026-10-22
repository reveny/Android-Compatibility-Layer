package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.AbsSavedState;
import android.view.Gravity;
import android.view.StyleableSet;
import android.view.autofill.AutofillValue;

public abstract class CompoundButton extends Button implements Checkable
{
    private static final int[] CHECKED_STATE_SET = {0x010100a0};

    private static StyleableSet sStyleable;

    private OnCheckedChangeListener mListener;
    private OnCheckedChangeListener mWidgetListener;
    private boolean mChecked;
    private boolean mBroadcasting;
    private Drawable mButtonDrawable;
    private int mButtonResource;
    private ColorStateList mButtonTintList;
    private PorterDuff.Mode mButtonTintMode;
    private BlendMode mButtonBlendMode;
    private CharSequence mCustomStateDescription;

    public CompoundButton(Context context)
    {
        this(context, null);
    }

    public CompoundButton(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public CompoundButton(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public CompoundButton(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        if (attributes != null || defStyleAttr != 0 || defStyleRes != 0) readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "button", "buttonTint", "buttonTintMode", "checked");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int buttonIndex = sStyleable.indexOf("button");
        if (buttonIndex >= 0 && array.hasValue(buttonIndex))
        {
            try
            {
                Drawable button = array.getDrawable(buttonIndex);
                if (button != null) setButtonDrawable(button);
            }
            catch (RuntimeException unsupported)
            {
                android.util.Log.w("CompoundButton", "the button drawable could not be loaded", unsupported);
            }
        }

        int tintIndex = sStyleable.indexOf("buttonTint");
        if (tintIndex >= 0 && array.hasValue(tintIndex)) mButtonTintList = array.getColorStateList(tintIndex);

        int modeIndex = sStyleable.indexOf("buttonTintMode");
        if (modeIndex >= 0 && array.hasValue(modeIndex)) mButtonTintMode = parseTintMode(array.getInt(modeIndex, -1));

        int checkedIndex = sStyleable.indexOf("checked");
        if (checkedIndex >= 0 && array.hasValue(checkedIndex)) mChecked = array.getBoolean(checkedIndex, false);

        array.recycle();

        applyButtonTint();
        refreshDrawableState();
    }

    private static PorterDuff.Mode parseTintMode(int value)
    {
        switch (value)
        {
            case 3:
                return PorterDuff.Mode.SRC_OVER;
            case 5:
                return PorterDuff.Mode.SRC_IN;
            case 9:
                return PorterDuff.Mode.SRC_ATOP;
            case 14:
                return PorterDuff.Mode.MULTIPLY;
            case 15:
                return PorterDuff.Mode.SCREEN;
            case 16:
                return PorterDuff.Mode.ADD;
            default:
                return null;
        }
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener)
    {
        mListener = listener;
    }

    public void setOnCheckedChangeWidgetListener(OnCheckedChangeListener listener)
    {
        mWidgetListener = listener;
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

        if (mBroadcasting) return;

        mBroadcasting = true;
        if (mListener != null) mListener.onCheckedChanged(this, mChecked);
        if (mWidgetListener != null) mWidgetListener.onCheckedChanged(this, mChecked);

        android.view.autofill.AutofillManager manager = getContext().getSystemService(android.view.autofill.AutofillManager.class);
        if (manager != null) manager.notifyValueChanged(this);

        mBroadcasting = false;
    }

    @Override
    public void toggle()
    {
        setChecked(!mChecked);
    }

    @Override
    public boolean performClick()
    {
        toggle();
        boolean handled = super.performClick();
        if (!handled) playSoundEffect(android.view.SoundEffectConstants.CLICK);
        return handled;
    }

    @Override
    public void setStateDescription(CharSequence stateDescription)
    {
        mCustomStateDescription = stateDescription;
        super.setStateDescription(stateDescription);
    }

    public void setButtonDrawable(int resId)
    {
        if (resId != 0 && resId == mButtonResource) return;

        Drawable drawable = resId == 0 ? null : getContext().getDrawable(resId);
        setButtonDrawable(drawable);
        mButtonResource = resId;
    }

    public void setButtonDrawable(Drawable drawable)
    {
        if (mButtonDrawable == drawable) return;

        if (mButtonDrawable != null)
        {
            mButtonDrawable.setCallback(null);
            unscheduleDrawable(mButtonDrawable);
        }

        mButtonDrawable = drawable;
        mButtonResource = 0;

        if (drawable != null)
        {
            drawable.setCallback(this);
            drawable.setLayoutDirection(getLayoutDirection());
            if (drawable.isStateful()) drawable.setState(getDrawableState());
            drawable.setVisible(getVisibility() == VISIBLE, false);
            setMinHeight(drawable.getIntrinsicHeight());
            applyButtonTint();
        }

        requestLayout();
        invalidate();
    }

    public void setButtonIcon(Icon icon)
    {
        setButtonDrawable(icon == null ? null : icon.loadDrawable(getContext()));
    }

    public Drawable getButtonDrawable()
    {
        return mButtonDrawable;
    }

    public void setButtonTintList(ColorStateList tint)
    {
        mButtonTintList = tint;
        applyButtonTint();
    }

    public ColorStateList getButtonTintList()
    {
        return mButtonTintList;
    }

    public void setButtonTintMode(PorterDuff.Mode tintMode)
    {
        mButtonTintMode = tintMode;
        mButtonBlendMode = null;
        applyButtonTint();
    }

    public PorterDuff.Mode getButtonTintMode()
    {
        return mButtonTintMode;
    }

    public void setButtonTintBlendMode(BlendMode tintMode)
    {
        mButtonBlendMode = tintMode;
        applyButtonTint();
    }

    public BlendMode getButtonTintBlendMode()
    {
        return mButtonBlendMode;
    }

    private void applyButtonTint()
    {
        if (mButtonDrawable == null) return;
        if (mButtonTintList == null && mButtonTintMode == null && mButtonBlendMode == null) return;

        mButtonDrawable = mButtonDrawable.mutate();
        if (mButtonTintList != null) mButtonDrawable.setTintList(mButtonTintList);
        if (mButtonTintMode != null) mButtonDrawable.setTintMode(mButtonTintMode);
        if (mButtonBlendMode != null) mButtonDrawable.setTintBlendMode(mButtonBlendMode);
        if (mButtonDrawable.isStateful()) mButtonDrawable.setState(getDrawableState());
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return CompoundButton.class.getName();
    }

    @Override
    public int getCompoundPaddingLeft()
    {
        int padding = super.getCompoundPaddingLeft();
        if (!isLayoutRtl() && mButtonDrawable != null) padding += Math.max(0, mButtonDrawable.getIntrinsicWidth());
        return padding;
    }

    @Override
    public int getCompoundPaddingRight()
    {
        int padding = super.getCompoundPaddingRight();
        if (isLayoutRtl() && mButtonDrawable != null) padding += Math.max(0, mButtonDrawable.getIntrinsicWidth());
        return padding;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        Drawable button = mButtonDrawable;

        if (button != null)
        {
            int drawableHeight = Math.max(0, button.getIntrinsicHeight());
            int drawableWidth = Math.max(0, button.getIntrinsicWidth());

            int top;
            int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
            if (verticalGravity == Gravity.BOTTOM) top = getHeight() - drawableHeight;
            else if (verticalGravity == Gravity.CENTER_VERTICAL) top = (getHeight() - drawableHeight) / 2;
            else top = 0;

            boolean rtl = isLayoutRtl();
            int left = rtl ? getWidth() - drawableWidth : 0;
            int right = rtl ? getWidth() : drawableWidth;

            button.setBounds(left, top, right, top + drawableHeight);

            Drawable background = getBackground();
            if (background != null) background.setHotspotBounds(left, top, right, top + drawableHeight);
        }

        super.onDraw(canvas);

        if (button == null) return;

        int scrollX = getScrollX();
        int scrollY = getScrollY();

        if (scrollX == 0 && scrollY == 0)
        {
            button.draw(canvas);
            return;
        }

        canvas.translate(scrollX, scrollY);
        button.draw(canvas);
        canvas.translate(-scrollX, -scrollY);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace)
    {
        int[] state = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) mergeDrawableStates(state, CHECKED_STATE_SET);
        return state;
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();

        if (mButtonDrawable != null && mButtonDrawable.isStateful() && mButtonDrawable.setState(getDrawableState()))
        {
            invalidateDrawable(mButtonDrawable);
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y)
    {
        super.drawableHotspotChanged(x, y);

        if (mButtonDrawable != null) mButtonDrawable.setHotspot(x, y);
    }

    @Override
    protected boolean verifyDrawable(Drawable who)
    {
        return super.verifyDrawable(who) || who == mButtonDrawable;
    }

    @Override
    public void jumpDrawablesToCurrentState()
    {
        super.jumpDrawablesToCurrentState();

        if (mButtonDrawable != null) mButtonDrawable.jumpToCurrentState();
    }

    @Override
    public void setVisibility(int visibility)
    {
        super.setVisibility(visibility);

        if (mButtonDrawable != null) mButtonDrawable.setVisible(visibility == VISIBLE, false);
    }

    @Override
    public void autofill(AutofillValue value)
    {
        if (!isEnabled() || value == null || !value.isToggle()) return;

        setChecked(value.getToggleValue());
    }

    @Override
    public int getAutofillType()
    {
        return isEnabled() ? AUTOFILL_TYPE_TOGGLE : AUTOFILL_TYPE_NONE;
    }

    @Override
    public AutofillValue getAutofillValue()
    {
        return isEnabled() ? AutofillValue.forToggle(isChecked()) : null;
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

    public interface OnCheckedChangeListener
    {
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
    }
}
