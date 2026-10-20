package android.hardware.display;

import android.view.Display;

import java.util.ArrayList;
import java.util.List;

public final class DisplayManager
{
    public static final String DISPLAY_CATEGORY_PRESENTATION = "android.hardware.display.category.PRESENTATION";

    public interface DisplayListener
    {
        void onDisplayAdded(int displayId);

        void onDisplayRemoved(int displayId);

        void onDisplayChanged(int displayId);
    }

    private final android.content.Context mContext;
    private final List<DisplayListener> mListeners = new ArrayList<>();

    private Display mDisplay;

    public DisplayManager(android.content.Context context)
    {
        mContext = context;
    }

    public Display getDisplay(int displayId)
    {
        if (displayId != Display.DEFAULT_DISPLAY) return null;

        if (mDisplay == null)
        {
            runtime.WindowBridge bridge = runtime.WindowBridge.get();

            int width = bridge == null ? 1280 : bridge.getWidth();
            int height = bridge == null ? 720 : bridge.getHeight();
            int densityDpi = android.util.DisplayMetrics.DENSITY_DEFAULT;

            if (mContext != null && mContext.getResources() != null)
            {
                densityDpi = mContext.getResources().getDisplayMetrics().densityDpi;
            }

            mDisplay = new Display(Display.DEFAULT_DISPLAY, "acl", width, height, densityDpi);
        }

        return mDisplay;
    }

    public Display[] getDisplays()
    {
        Display display = getDisplay(Display.DEFAULT_DISPLAY);

        return display == null ? new Display[0] : new Display[] {display};
    }

    public Display[] getDisplays(String category)
    {
        if (DISPLAY_CATEGORY_PRESENTATION.equals(category)) return new Display[0];

        return getDisplays();
    }

    public void registerDisplayListener(DisplayListener listener, android.os.Handler handler)
    {
        if (listener != null) mListeners.add(listener);
    }

    public void unregisterDisplayListener(DisplayListener listener)
    {
        mListeners.remove(listener);
    }
}
