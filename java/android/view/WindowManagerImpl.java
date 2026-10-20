package android.view;

import android.content.Context;
import java.util.ArrayList;

public final class WindowManagerImpl implements WindowManager
{
    private final Context mContext;
    private final ArrayList<View> mViews = new ArrayList<>();

    public WindowManagerImpl(Context context)
    {
        mContext = context;
    }

    @Override
    public Display getDefaultDisplay()
    {
        if (mDisplay == null)
        {
            runtime.WindowBridge bridge = runtime.WindowBridge.get();
            int width = bridge == null ? 1280 : bridge.getWidth();
            int height = bridge == null ? 720 : bridge.getHeight();
            mDisplay = new Display(Display.DEFAULT_DISPLAY, "acl", width, height, densityDpi());
        }

        return mDisplay;
    }

    private static int densityDpi()
    {
        String value = runtime.Settings.get("density");

        if (value != null && !value.isEmpty())
        {
            try
            {
                int parsed = Integer.parseInt(value.trim());
                if (parsed > 0) return parsed;
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        return android.util.DisplayMetrics.DENSITY_DEFAULT;
    }

    private Display mDisplay;

    public Context getContext()
    {
        return mContext;
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams params)
    {
        if (view == null) throw new IllegalArgumentException("view must not be null");
        if (mViews.contains(view)) throw new IllegalStateException("View has already been added to the window manager.");

        view.setLayoutParams(params);
        mViews.add(view);
    }

    @Override
    public void updateViewLayout(View view, ViewGroup.LayoutParams params)
    {
        if (view == null) throw new IllegalArgumentException("view must not be null");
        if (!mViews.contains(view)) throw new IllegalArgumentException("View not attached to window manager");

        view.setLayoutParams(params);
        view.requestLayout();
    }

    @Override
    public void removeView(View view)
    {
        mViews.remove(view);
    }

    @Override
    public void removeViewImmediate(View view)
    {
        removeView(view);
    }
}
