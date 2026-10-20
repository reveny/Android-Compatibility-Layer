package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public final class Space extends View
{
    public Space(Context context)
    {
        this(context, null);
    }

    public Space(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public Space(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public Space(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
        setWillNotDraw(true);
    }

    @Override
    public void draw(Canvas canvas)
    {
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                             getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
}
