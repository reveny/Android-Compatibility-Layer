package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.PointerIcon;

public class Button extends TextView
{
    public Button(Context context)
    {
        this(context, null);
    }

    public Button(Context context, AttributeSet attributes)
    {
        this(context, attributes, attribute(context, "buttonStyle"));
    }

    public Button(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public Button(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    private static int attribute(Context context, String name)
    {
        if (context == null) return 0;

        return context.getResources().getIdentifier(name, "attr", "android");
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return Button.class.getName();
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex)
    {
        if (getPointerIcon() == null && isClickable() && isEnabled()) return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND);

        return super.onResolvePointerIcon(event, pointerIndex);
    }
}
