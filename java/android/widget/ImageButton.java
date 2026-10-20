package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class ImageButton extends ImageView
{
    public ImageButton(Context context)
    {
        this(context, null);
    }

    public ImageButton(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
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
}
