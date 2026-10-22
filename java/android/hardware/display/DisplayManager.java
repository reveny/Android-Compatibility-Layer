package android.hardware.display;

import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public final class DisplayManager
{
    public static final String DISPLAY_CATEGORY_PRESENTATION = "android.hardware.display.category.PRESENTATION";

    public static final long EVENT_TYPE_DISPLAY_ADDED = 1L;
    public static final long EVENT_TYPE_DISPLAY_REMOVED = 2L;
    public static final long EVENT_TYPE_DISPLAY_CHANGED = 4L;
    public static final long EVENT_TYPE_DISPLAY_REFRESH_RATE = 8L;
    public static final long EVENT_TYPE_DISPLAY_STATE = 16L;

    public static final int MATCH_CONTENT_FRAMERATE_UNKNOWN = -1;
    public static final int MATCH_CONTENT_FRAMERATE_NEVER = 0;
    public static final int MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY = 1;
    public static final int MATCH_CONTENT_FRAMERATE_ALWAYS = 2;

    public static final int VIRTUAL_DISPLAY_FLAG_PUBLIC = 1;
    public static final int VIRTUAL_DISPLAY_FLAG_PRESENTATION = 2;
    public static final int VIRTUAL_DISPLAY_FLAG_SECURE = 4;
    public static final int VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY = 8;
    public static final int VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR = 16;

    public interface DisplayListener
    {
        void onDisplayAdded(int displayId);

        void onDisplayRemoved(int displayId);

        void onDisplayChanged(int displayId);
    }

    private static final List<VirtualDisplay> sVirtualDisplays = new ArrayList<>();
    private static final List<ListenerRecord> sListeners = new ArrayList<>();
    private static int sNextVirtualDisplayId = 2;

    private final android.content.Context mContext;

    private Display mDisplay;

    public DisplayManager(android.content.Context context)
    {
        mContext = context;
    }

    public Display getDisplay(int displayId)
    {
        if (displayId != Display.DEFAULT_DISPLAY)
        {
            synchronized (sVirtualDisplays)
            {
                for (VirtualDisplay virtualDisplay : sVirtualDisplays)
                {
                    if (virtualDisplay.getDisplay().getDisplayId() == displayId) return virtualDisplay.getDisplay();
                }
            }
            return null;
        }

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
        return getDisplays(null);
    }

    public Display[] getDisplays(String category)
    {
        boolean presentation = DISPLAY_CATEGORY_PRESENTATION.equals(category);
        List<Display> displays = new ArrayList<>();

        Display display = getDisplay(Display.DEFAULT_DISPLAY);
        if (display != null && !presentation) displays.add(display);

        synchronized (sVirtualDisplays)
        {
            for (VirtualDisplay virtualDisplay : sVirtualDisplays)
            {
                if (presentation && (virtualDisplay.getFlags() & VIRTUAL_DISPLAY_FLAG_PRESENTATION) == 0) continue;
                displays.add(virtualDisplay.getDisplay());
            }
        }

        return displays.toArray(new Display[0]);
    }

    public VirtualDisplay createVirtualDisplay(String name, int width, int height, int densityDpi, Surface surface, int flags)
    {
        return createVirtualDisplay(name, width, height, densityDpi, surface, flags, null, null);
    }

    public VirtualDisplay createVirtualDisplay(String name, int width, int height, int densityDpi, Surface surface, int flags,
        VirtualDisplay.Callback callback, Handler handler)
    {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name must be non-null and non-empty");
        if (width <= 0 || height <= 0 || densityDpi <= 0)
        {
            throw new IllegalArgumentException("width, height, and densityDpi must be greater than 0");
        }

        VirtualDisplay virtualDisplay;
        synchronized (sVirtualDisplays)
        {
            Display display = new Display(sNextVirtualDisplayId++, name, width, height, densityDpi);
            virtualDisplay = new VirtualDisplay(this, display, surface, flags, callback, handler);
            sVirtualDisplays.add(virtualDisplay);
        }

        notifyListeners(virtualDisplay.getDisplay().getDisplayId(), EVENT_TYPE_DISPLAY_ADDED);
        return virtualDisplay;
    }

    void onVirtualDisplayChanged(VirtualDisplay virtualDisplay)
    {
        notifyListeners(virtualDisplay.getDisplay().getDisplayId(), EVENT_TYPE_DISPLAY_CHANGED);
    }

    void onVirtualDisplayReleased(VirtualDisplay virtualDisplay)
    {
        synchronized (sVirtualDisplays)
        {
            sVirtualDisplays.remove(virtualDisplay);
        }

        notifyListeners(virtualDisplay.getDisplay().getDisplayId(), EVENT_TYPE_DISPLAY_REMOVED);
    }

    private static void notifyListeners(int displayId, long eventType)
    {
        List<ListenerRecord> records;
        synchronized (sListeners)
        {
            records = new ArrayList<>(sListeners);
        }

        for (ListenerRecord record : records)
        {
            if ((record.mEventMask & eventType) == 0) continue;

            Runnable notification = () ->
            {
                if (eventType == EVENT_TYPE_DISPLAY_ADDED) record.mListener.onDisplayAdded(displayId);
                else if (eventType == EVENT_TYPE_DISPLAY_REMOVED) record.mListener.onDisplayRemoved(displayId);
                else record.mListener.onDisplayChanged(displayId);
            };

            if (record.mExecutor != null) record.mExecutor.execute(notification);
            else if (record.mHandler != null) record.mHandler.post(notification);
            else new Handler(Looper.getMainLooper()).post(notification);
        }
    }

    public int getMatchContentFrameRateUserPreference()
    {
        return MATCH_CONTENT_FRAMERATE_SEAMLESSS_ONLY;
    }

    public void registerDisplayListener(DisplayListener listener, Handler handler)
    {
        registerListener(listener, handler, null, EVENT_TYPE_DISPLAY_ADDED | EVENT_TYPE_DISPLAY_REMOVED | EVENT_TYPE_DISPLAY_CHANGED);
    }

    public void registerDisplayListener(Executor executor, long eventFilter, DisplayListener listener)
    {
        if (executor == null) throw new IllegalArgumentException("executor must not be null");
        registerListener(listener, null, executor, eventFilter);
    }

    private void registerListener(DisplayListener listener, Handler handler, Executor executor, long eventMask)
    {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");

        synchronized (sListeners)
        {
            for (ListenerRecord record : sListeners) if (record.mListener == listener) return;
            sListeners.add(new ListenerRecord(listener, handler, executor, eventMask));
        }
    }

    public void unregisterDisplayListener(DisplayListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");

        synchronized (sListeners)
        {
            for (int index = sListeners.size() - 1; index >= 0; index--)
            {
                if (sListeners.get(index).mListener == listener) sListeners.remove(index);
            }
        }
    }

    private static final class ListenerRecord
    {
        final DisplayListener mListener;
        final Handler mHandler;
        final Executor mExecutor;
        final long mEventMask;

        ListenerRecord(DisplayListener listener, Handler handler, Executor executor, long eventMask)
        {
            mListener = listener;
            mHandler = handler;
            mExecutor = executor;
            mEventMask = eventMask;
        }
    }
}
