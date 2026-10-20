package android.view;

import android.graphics.Outline;

public abstract class ViewOutlineProvider
{
    public static final ViewOutlineProvider BACKGROUND = new ViewOutlineProvider()
    {
        @Override
        public void getOutline(View view, Outline outline)
        {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };

    public static final ViewOutlineProvider BOUNDS = new ViewOutlineProvider()
    {
        @Override
        public void getOutline(View view, Outline outline)
        {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
        }
    };

    public static final ViewOutlineProvider PADDED_BOUNDS = new ViewOutlineProvider()
    {
        @Override
        public void getOutline(View view, Outline outline)
        {
            outline.setRect(view.getPaddingLeft(), view.getPaddingTop(), view.getWidth() - view.getPaddingRight(),
                            view.getHeight() - view.getPaddingBottom());
        }
    };

    public abstract void getOutline(View view, Outline outline);
}
