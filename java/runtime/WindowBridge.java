package runtime;

import android.graphics.Canvas;
import android.os.Looper;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.MotionEvent;

public final class WindowBridge implements Poller
{
    private static WindowBridge sInstance;

    private long mHandle;
    private static final int GUEST_SERVICE_INTERVAL_MILLIS = 2;

    private static final int KIND_TOUCH = 0;
    private static final int INPUT_EVENT_WORDS = 8;

    private final long[] mInputWords = new long[INPUT_EVENT_WORDS];
    private static final boolean TRACE_INPUT = Settings.tracing("input");

    private final PointerTracker mTracker = new PointerTracker();

    private final PointerTracker.Sink mMotionSink = new PointerTracker.Sink()
    {
        @Override
        public void onMotion(MotionEvent event)
        {
            if (TRACE_INPUT)
            {
                android.util.Log.i("WindowBridge", "touch action=" + event.getActionMasked() + " pointers="
                                                   + event.getPointerCount() + " at " + event.getX() + "," + event.getY());
            }

            if (mInputSink != null) mInputSink.onTouch(event);
        }
    };

    private InputSink mInputSink;
    private long mDownTime;
    private boolean mSinkWarned;

    private boolean mPollFailed;
    private boolean mGuestPresents;
    private boolean mGuestWatchesDescriptors;
    private final int mWidth;
    private final int mHeight;
    private final Canvas mCanvas = new Canvas();

    private WindowBridge(long handle, int width, int height)
    {
        mHandle = handle;
        mWidth = width;
        mHeight = height;
    }

    public static WindowBridge open(int width, int height, String title)
    {
        if (sInstance != null) return sInstance;

        long handle = nativeOpen(width, height, title);
        if (handle == 0) return null;

        sInstance = new WindowBridge(handle, width, height);
        Looper main = Looper.getMainLooper();
        if (main != null) main.getQueue().setPoller(sInstance);

        return sInstance;
    }

    public static WindowBridge get()
    {
        return sInstance;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public Canvas beginFrame()
    {
        if (mHandle == 0) return null;

        long canvasHandle = nativeBeginFrame(mHandle);
        if (canvasHandle == 0) return null;

        mCanvas.setNativeCanvas(canvasHandle, mWidth, mHeight);
        return mCanvas;
    }

    public void endFrame()
    {
        if (mHandle == 0) return;
        nativeEndFrame(mHandle);
    }

    public void requestFrame()
    {
        if (mHandle == 0 || mGuestPresents) return;
        nativeRequestFrame(mHandle);
    }

    public void setGuestPresents(boolean guestPresents)
    {
        mGuestPresents = guestPresents;
        if (mHandle != 0) nativeSetGuestPresents(mHandle, guestPresents);
    }

    public void setFullscreen(boolean fullscreen)
    {
        if (mHandle == 0) return;
        nativeSetFullscreen(mHandle, fullscreen);
    }

    public static final float FALLBACK_REFRESH_RATE = 60.0f;

    private static final int DISPLAY_MODE_FIELDS = 6;
    private static final int DISPLAY_MODE_REFRESH = 2;

    private static int[] sDisplayMode;
    private static boolean sDisplayModeAsked;

    public static int[] displayMode()
    {
        if (sDisplayModeAsked) return sDisplayMode;

        sDisplayModeAsked = true;

        WindowBridge instance = sInstance;
        if (instance == null || instance.mHandle == 0) return null;

        int[] values = new int[DISPLAY_MODE_FIELDS];
        if (!nativeDisplayMode(instance.mHandle, values)) return null;

        sDisplayMode = values;

        android.util.Log.i("WindowBridge", "the output reports " + values[0] + "x" + values[1] + " at " + (values[2] / 1000.0f)
                                               + " Hz, scale " + values[3] + ", " + values[4] + "x" + values[5] + " mm");

        return sDisplayMode;
    }

    public static float refreshRate()
    {
        int[] mode = displayMode();
        if (mode == null || mode[DISPLAY_MODE_REFRESH] <= 0) return FALLBACK_REFRESH_RATE;

        return mode[DISPLAY_MODE_REFRESH] / 1000.0f;
    }

    public interface ResizeListener
    {
        void onWindowResized(int width, int height);
    }

    private final int[] mResizeSize = new int[2];
    private ResizeListener mResizeListener;

    public void setResizeListener(ResizeListener listener)
    {
        mResizeListener = listener;
    }

    public boolean isCloseRequested()
    {
        return mPollFailed || (mHandle != 0 && nativeIsCloseRequested(mHandle));
    }

    public void close()
    {
        if (mHandle == 0) return;

        Looper main = Looper.getMainLooper();
        if (main != null) main.getQueue().setPoller(null);

        nativeClose(mHandle);
        mHandle = 0;
        sInstance = null;
    }

    @Override
    public void poll(long timeoutMillis)
    {
        if (mHandle == 0) return;

        int timeout = timeoutMillis < 0 ? -1 : (int)Math.min(timeoutMillis, Integer.MAX_VALUE);

        if ((mGuestPresents || mGuestWatchesDescriptors) && (timeout < 0 || timeout > GUEST_SERVICE_INTERVAL_MILLIS))
        {
            timeout = GUEST_SERVICE_INTERVAL_MILLIS;
        }
        if (!nativePoll(mHandle, timeout))
        {
            android.util.Log.i("WindowBridge", "the window connection ended; closing the window");
            mPollFailed = true;
            Looper.quitMainLooper();
            return;
        }

        mGuestWatchesDescriptors = SurfaceBridge.serviceLooper();

        deliverInput();
        deliverResize();

        if (nativeConsumeFrame(mHandle)) Choreographer.getInstance().doFrame(android.os.SystemClock.uptimeNanos());
    }

    @Override
    public void wake()
    {
        if (mHandle == 0) return;
        nativeWake(mHandle);
    }

    public interface InputSink
    {
        void onTouch(MotionEvent event);

        void onKey(KeyEvent event);
    }

    public void setInputSink(InputSink sink)
    {
        mInputSink = sink;
    }

    private void deliverResize()
    {
        if (mHandle == 0 || mResizeListener == null) return;
        if (!nativeConsumeResize(mHandle, mResizeSize)) return;

        mResizeListener.onWindowResized(mResizeSize[0], mResizeSize[1]);
    }

    private void deliverInput()
    {
        if (mInputSink == null)
        {
            if (mHandle != 0 && nativeNextInputEvent(mHandle, mInputWords) && !mSinkWarned)
            {
                mSinkWarned = true;
                android.util.Log.w("WindowBridge", "an input event arrived before anything was listening for it");
            }

            return;
        }

        while (nativeNextInputEvent(mHandle, mInputWords))
        {
            long packed = mInputWords[7];
            float x = Float.intBitsToFloat((int)(packed >>> 32));
            float y = Float.intBitsToFloat((int)packed);

            int action = (int)mInputWords[1];
            int pointerId = (int)mInputWords[2];
            long eventTime = mInputWords[6] / 1000000L;

            if (mInputWords[0] == KIND_TOUCH)
            {
                deliverTouch(action, pointerId, x, y, eventTime, (int)mInputWords[5]);
                continue;
            }

            KeyEvent event = new KeyEvent(mDownTime, eventTime, action, (int)mInputWords[3], 0, (int)mInputWords[5]);
            mInputSink.onKey(event);
        }
    }

    private void deliverTouch(int action, int pointerId, float x, float y, long eventTime, int metaState)
    {
        mTracker.track(action, pointerId, x, y, eventTime, metaState, mMotionSink);
    }

    public void injectTap(float x, float y)
    {
        long now = android.os.SystemClock.uptimeMillis();

        deliverTouch(MotionEvent.ACTION_DOWN, 0, x, y, now, 0);
        deliverTouch(MotionEvent.ACTION_UP, 0, x, y, now + 16, 0);
    }

    private static native boolean nativeNextInputEvent(long handle, long[] words);

    private static native long nativeOpen(int width, int height, String title);

    private static native void nativeSetGuestPresents(long handle, boolean guestPresents);

    private static native boolean nativeConsumeResize(long handle, int[] sizeOut);

    private static native boolean nativeDisplayMode(long handle, int[] modeOut);

    private static native void nativeSetFullscreen(long handle, boolean fullscreen);

    private static native void nativeClose(long handle);

    private static native long nativeBeginFrame(long handle);

    private static native void nativeEndFrame(long handle);

    private static native void nativeRequestFrame(long handle);

    private static native boolean nativePoll(long handle, int timeoutMillis);

    private static native void nativeWake(long handle);

    private static native boolean nativeConsumeFrame(long handle);

    private static native long nativeLastFrameTimeNanos(long handle);

    private static native boolean nativeIsCloseRequested(long handle);
}
