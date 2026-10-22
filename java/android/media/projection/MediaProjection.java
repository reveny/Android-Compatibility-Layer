package android.media.projection;

import android.os.Handler;
import android.os.Looper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MediaProjection
{
    private final Map<Callback, Handler> mCallbacks = new ConcurrentHashMap<>();
    private boolean mStopped;

    MediaProjection()
    {
    }

    public void registerCallback(Callback callback, Handler handler)
    {
        if (callback == null) throw new NullPointerException("callback must not be null");

        mCallbacks.put(callback, handler != null ? handler : new Handler(Looper.getMainLooper()));
    }

    public void stop()
    {
        synchronized (this)
        {
            if (mStopped) return;

            mStopped = true;
        }

        for (Map.Entry<Callback, Handler> entry : mCallbacks.entrySet())
        {
            final Callback callback = entry.getKey();
            entry.getValue().post(new Runnable()
            {
                @Override
                public void run()
                {
                    callback.onStop();
                }
            });
        }
    }

    public void unregisterCallback(Callback callback)
    {
        if (callback == null) throw new NullPointerException("callback must not be null");

        mCallbacks.remove(callback);
    }

    public abstract static class Callback
    {
        public Callback()
        {
        }

        public void onCapturedContentResize(int width, int height)
        {
        }

        public void onCapturedContentVisibilityChanged(boolean isVisible)
        {
        }

        public void onStop()
        {
        }
    }
}
