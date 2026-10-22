package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class CheckBox extends CompoundButton
{
    public CheckBox(Context context)
    {
        this(context, null);
    }

    public CheckBox(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "checkboxStyle"));
    }

    public CheckBox(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public CheckBox(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return CheckBox.class.getName();
    }
}
