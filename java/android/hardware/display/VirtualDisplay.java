package android.hardware.display;

import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Surface;

public final class VirtualDisplay
{
    private static final int CALLBACK_PAUSED = 0;
    private static final int CALLBACK_RESUMED = 1;
    private static final int CALLBACK_STOPPED = 2;

    private final DisplayManager mDisplayManager;
    private final int mFlags;
    private final Callback mCallback;
    private final Handler mHandler;
    private Display mDisplay;
    private Surface mSurface;
    private boolean mReleased;
    private int mRotation;

    VirtualDisplay(DisplayManager displayManager, Display display, Surface surface, int flags, Callback callback, Handler handler)
    {
        mDisplayManager = displayManager;
        mDisplay = display;
        mSurface = surface;
        mFlags = flags;
        mCallback = callback;
        mHandler = handler != null ? handler : new Handler(Looper.getMainLooper());

        if (surface != null) dispatch(CALLBACK_RESUMED);
    }

    private void dispatch(int event)
    {
        if (mCallback == null) return;

        mHandler.post(() ->
        {
            if (event == CALLBACK_PAUSED) mCallback.onPaused();
            else if (event == CALLBACK_RESUMED) mCallback.onResumed();
            else mCallback.onStopped();
        });
    }

    public Display getDisplay()
    {
        return mDisplay;
    }

    public Surface getSurface()
    {
        return mSurface;
    }

    public void setSurface(Surface surface)
    {
        if (mReleased || surface == mSurface) return;

        boolean hadSurface = mSurface != null;
        mSurface = surface;
        if (hadSurface && surface == null) dispatch(CALLBACK_PAUSED);
        else if (!hadSurface && surface != null) dispatch(CALLBACK_RESUMED);
    }

    public void resize(int width, int height, int densityDpi)
    {
        if (mReleased) return;

        mDisplay = new Display(mDisplay.getDisplayId(), mDisplay.getName(), width, height, densityDpi);
        mDisplayManager.onVirtualDisplayChanged(this);
    }

    public void setRotation(int rotation)
    {
        mRotation = rotation;
    }

    int getFlags()
    {
        return mFlags;
    }

    public void release()
    {
        if (mReleased) return;

        mReleased = true;
        mDisplayManager.onVirtualDisplayReleased(this);
        dispatch(CALLBACK_STOPPED);
    }

    @Override
    public String toString()
    {
        return "VirtualDisplay{display=" + mDisplay.getDisplayId() + ", name=" + mDisplay.getName() + ", surface=" + mSurface
            + ", rotation=" + mRotation + "}";
    }

    public abstract static class Callback
    {
        public Callback()
        {
        }

        public void onPaused()
        {
        }

        public void onResumed()
        {
        }

        public void onStopped()
        {
        }
    }
}
