package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.PointerIcon;

public class ImageButton extends ImageView
{
    public ImageButton(Context context)
    {
        this(context, null);
    }

    public ImageButton(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "imageButtonStyle"));
    }

    public ImageButton(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public ImageButton(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected boolean onSetAlpha(int alpha)
    {
        return false;
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ImageButton.class.getName();
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex)
    {
        if (getPointerIcon() == null && isClickable() && isEnabled()) return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND);

        return super.onResolvePointerIcon(event, pointerIndex);
    }
}
