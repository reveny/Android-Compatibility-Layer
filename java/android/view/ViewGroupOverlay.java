package android.view;

import android.content.Context;

public class ViewGroupOverlay extends ViewOverlay
{
    ViewGroupOverlay(Context context, View hostView)
    {
        super(context, hostView);
    }

    public void add(View view)
    {
        mOverlayViewGroup.add(view);
    }

    public void remove(View view)
    {
        mOverlayViewGroup.remove(view);
    }
}
