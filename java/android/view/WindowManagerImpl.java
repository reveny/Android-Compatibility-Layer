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

    @Override
    public WindowMetrics getCurrentWindowMetrics()
    {
        ViewRootImpl root = ViewRootImpl.active();
        android.graphics.Rect bounds;
        WindowInsets insets;

        if (root != null)
        {
            bounds = new android.graphics.Rect(0, 0, root.getWindowWidth(), root.getWindowHeight());
            insets = root.getWindowInsets();
        }
        else
        {
            Display display = getDefaultDisplay();
            bounds = new android.graphics.Rect(0, 0, display.getWidth(), display.getHeight());
            insets = new WindowInsets.Builder().setFrame(bounds.width(), bounds.height()).build();
        }

        return new WindowMetrics(bounds, insets, densityDpi() / (float)android.util.DisplayMetrics.DENSITY_DEFAULT);
    }

    @Override
    public WindowMetrics getMaximumWindowMetrics()
    {
        WindowMetrics current = getCurrentWindowMetrics();
        Display display = getDefaultDisplay();

        android.graphics.Rect bounds = new android.graphics.Rect(current.getBounds());
        bounds.union(0, 0, display.getWidth(), display.getHeight());

        return new WindowMetrics(bounds, current.getWindowInsets(), current.getDensity());
    }

    public static final String DENSITY_SETTING = "density";

    private static int densityDpi()
    {
        String value = runtime.Settings.get(DENSITY_SETTING);

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

    private static WindowManager.LayoutParams windowParams(ViewGroup.LayoutParams params)
    {
        if (!(params instanceof WindowManager.LayoutParams)) throw new IllegalArgumentException("Params must be WindowManager.LayoutParams");

        return (WindowManager.LayoutParams)params;
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams params)
    {
        if (view == null) throw new IllegalArgumentException("view must not be null");
        if (mViews.contains(view)) throw new IllegalStateException("View has already been added to the window manager.");

        WindowManager.LayoutParams windowParams = windowParams(params);
        ViewRootImpl root = ViewRootImpl.active();

        mViews.add(view);

        if (root == null)
        {
            android.util.Log.w("WindowManager", "a " + view.getClass().getName() + " window was added before the activity had one, so it is not shown");
            view.setLayoutParams(windowParams);
            return;
        }

        root.addPanel(view, windowParams);
    }

    @Override
    public void updateViewLayout(View view, ViewGroup.LayoutParams params)
    {
        if (view == null) throw new IllegalArgumentException("view must not be null");

        WindowManager.LayoutParams windowParams = windowParams(params);
        ViewRootImpl root = ViewRootImpl.active();

        if (root != null && root.hasPanel(view))
        {
            root.updatePanel(view, windowParams);
            return;
        }

        if (!mViews.contains(view)) throw new IllegalArgumentException("View not attached to window manager");

        view.setLayoutParams(windowParams);
        view.requestLayout();
    }

    @Override
    public void removeView(View view)
    {
        mViews.remove(view);

        ViewRootImpl root = ViewRootImpl.active();
        if (root != null) root.removePanel(view);
    }

    @Override
    public void removeViewImmediate(View view)
    {
        removeView(view);
    }
}
