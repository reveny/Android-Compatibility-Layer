package android.os;

public class Handler
{
    private final Looper mLooper;
    private final MessageQueue mQueue;
    private final Callback mCallback;

    public Handler()
    {
        this(null, null);
    }

    public Handler(Callback callback)
    {
        this(null, callback);
    }

    public Handler(Looper looper)
    {
        this(looper, null);
    }

    public Handler(Looper looper, Callback callback)
    {
        Looper resolved = looper;
        if (resolved == null)
        {
            resolved = Looper.myLooper();
            if (resolved == null)
            {
                throw new RuntimeException("Can't create handler inside thread " + Thread.currentThread()
                        + " that has not called Looper.prepare()");
            }
        }

        mLooper = resolved;
        mQueue = resolved.mQueue;
        mCallback = callback;
    }

    public static Handler createAsync(Looper looper)
    {
        return new Handler(looper);
    }

    public final Looper getLooper()
    {
        return mLooper;
    }

    public void handleMessage(Message message)
    {
    }

    public void dispatchMessage(Message message)
    {
        if (message.callback != null)
        {
            message.callback.run();
            return;
        }

        if (mCallback != null && mCallback.handleMessage(message)) return;

        handleMessage(message);
    }

    public final boolean post(Runnable runnable)
    {
        return sendMessageDelayed(Message.obtain(this, runnable), 0);
    }

    public final boolean postDelayed(Runnable runnable, long delayMillis)
    {
        return sendMessageDelayed(Message.obtain(this, runnable), delayMillis);
    }

    public final boolean postAtTime(Runnable runnable, long uptimeMillis)
    {
        return sendMessageAtTime(Message.obtain(this, runnable), uptimeMillis);
    }

    public final boolean postAtFrontOfQueue(Runnable runnable)
    {
        return sendMessageAtTime(Message.obtain(this, runnable), 0);
    }

    public final Message obtainMessage()
    {
        return Message.obtain(this);
    }

    public final Message obtainMessage(int what)
    {
        return Message.obtain(this, what);
    }

    public final Message obtainMessage(int what, Object object)
    {
        return Message.obtain(this, what, object);
    }

    public final Message obtainMessage(int what, int arg1, int arg2)
    {
        return Message.obtain(this, what, arg1, arg2);
    }

    public final Message obtainMessage(int what, int arg1, int arg2, Object object)
    {
        return Message.obtain(this, what, arg1, arg2, object);
    }

    public final boolean sendMessage(Message message)
    {
        return sendMessageDelayed(message, 0);
    }

    public final boolean sendEmptyMessage(int what)
    {
        return sendEmptyMessageDelayed(what, 0);
    }

    public final boolean sendEmptyMessageDelayed(int what, long delayMillis)
    {
        return sendMessageDelayed(Message.obtain(this, what), delayMillis);
    }

    public final boolean sendMessageDelayed(Message message, long delayMillis)
    {
        long delay = delayMillis < 0 ? 0 : delayMillis;
        return sendMessageAtTime(message, SystemClock.uptimeMillis() + delay);
    }

    public boolean sendMessageAtTime(Message message, long uptimeMillis)
    {
        message.target = this;
        return mQueue.enqueueMessage(message, uptimeMillis);
    }

    public final void removeCallbacks(Runnable runnable)
    {
        mQueue.removeCallbacksAndMessages(this, runnable, null);
    }

    public final void removeCallbacks(Runnable runnable, Object token)
    {
        mQueue.removeCallbacksAndMessages(this, runnable, token);
    }

    public final void removeCallbacksAndMessages(Object token)
    {
        mQueue.removeCallbacksAndMessages(this, null, token);
    }

    public final void removeMessages(int what)
    {
        mQueue.removeMessages(this, what, null);
    }

    public final void removeMessages(int what, Object token)
    {
        mQueue.removeMessages(this, what, token);
    }

    public interface Callback
    {
        boolean handleMessage(Message message);
    }
}
