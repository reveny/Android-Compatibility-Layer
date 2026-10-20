package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class RadioButton extends CompoundButton
{
    public RadioButton(Context context)
    {
        super(context);
    }

    public RadioButton(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public RadioButton(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
    }

    @Override
    public void toggle()
    {
        if (!isChecked()) setChecked(true);
    }
}
