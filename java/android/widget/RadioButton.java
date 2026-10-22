package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeInfo;

public class RadioButton extends CompoundButton
{
    public RadioButton(Context context)
    {
        this(context, null);
    }

    public RadioButton(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "radioButtonStyle"));
    }

    public RadioButton(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public RadioButton(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    @Override
    public void toggle()
    {
        if (!isChecked()) super.toggle();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return RadioButton.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info)
    {
        super.onInitializeAccessibilityNodeInfo(info);

        info.setCheckable(true);
        info.setChecked(isChecked());
    }
}
