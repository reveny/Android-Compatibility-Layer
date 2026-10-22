package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.StyleableSet;

public class ToggleButton extends CompoundButton
{
    private static StyleableSet sStyleable;

    private CharSequence mTextOn = "ON";
    private CharSequence mTextOff = "OFF";
    private float mDisabledAlpha = 0.5f;
    private final Paint mIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mAccentColor;
    private final float mDensity;

    public ToggleButton(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mDensity = context.getResources().getDisplayMetrics().density;
        mAccentColor = ProgressBar.resolveThemeColor(context, 0xFF009688, "colorControlActivated", "colorAccent");

        setClickable(true);
        setFocusable(true);

        StyleableSet styleable = styleable(context);
        if (!styleable.isEmpty() && (attributes != null || defStyleAttr != 0 || defStyleRes != 0))
        {
            TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);

            int index = styleable.indexOf("textOn");
            if (index >= 0)
            {
                CharSequence text = array.getText(index);
                if (text != null) mTextOn = text;
            }

            index = styleable.indexOf("textOff");
            if (index >= 0)
            {
                CharSequence text = array.getText(index);
                if (text != null) mTextOff = text;
            }

            index = styleable.indexOf("disabledAlpha");
            if (index >= 0) mDisabledAlpha = array.getFloat(index, mDisabledAlpha);

            array.recycle();
        }

        syncTextState();
    }

    public ToggleButton(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public ToggleButton(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "buttonStyleToggle"));
    }

    public ToggleButton(Context context)
    {
        this(context, null);
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "textOn", "textOff", "disabledAlpha");
        return sStyleable;
    }

    @Override
    public void setChecked(boolean checked)
    {
        super.setChecked(checked);

        syncTextState();
        refreshDrawableState();
    }

    private void syncTextState()
    {
        boolean checked = isChecked();

        if (checked && mTextOn != null) setText(mTextOn);
        else if (!checked && mTextOff != null) setText(mTextOff);
    }

    public CharSequence getTextOn()
    {
        return mTextOn;
    }

    public void setTextOn(CharSequence textOn)
    {
        mTextOn = textOn;
        syncTextState();
    }

    public CharSequence getTextOff()
    {
        return mTextOff;
    }

    public void setTextOff(CharSequence textOff)
    {
        mTextOff = textOff;
        syncTextState();
    }

    public float getDisabledAlpha()
    {
        return mDisabledAlpha;
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();

        syncTextState();
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable)
    {
        super.setBackgroundDrawable(drawable);
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (getBackground() != null) return;

        float inset = 8 * mDensity;
        float thickness = 2 * mDensity;
        float bottom = getHeight() - getPaddingBottom() * 0.5f;

        int colour = isChecked() ? mAccentColor : 0x42000000;
        if (!isEnabled()) colour = (colour & 0x00FFFFFF) | ((int)(((colour >>> 24) & 0xFF) * mDisabledAlpha) << 24);

        mIndicatorPaint.setStyle(Paint.Style.FILL);
        mIndicatorPaint.setColor(colour);
        canvas.drawRect(inset, bottom - thickness, getWidth() - inset, bottom, mIndicatorPaint);
    }

    public CharSequence getAccessibilityClassName()
    {
        return ToggleButton.class.getName();
    }
}
