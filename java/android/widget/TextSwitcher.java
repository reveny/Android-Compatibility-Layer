package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class TextSwitcher extends ViewSwitcher
{
    public TextSwitcher(Context context)
    {
        super(context);
    }

    public TextSwitcher(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        if (!(child instanceof TextView)) throw new IllegalArgumentException("TextSwitcher children must be instances of TextView");

        super.addView(child, index, params);
    }

    public void setText(CharSequence text)
    {
        TextView next = (TextView)getNextView();
        if (next == null) return;

        next.setText(text);
        showNext();
    }

    public void setCurrentText(CharSequence text)
    {
        TextView current = (TextView)getCurrentView();
        if (current != null) current.setText(text);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return TextSwitcher.class.getName();
    }
}
