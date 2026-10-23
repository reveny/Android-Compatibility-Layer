package android.os;

public final class MessageQueue
{
    private final boolean mQuitAllowed;
    private Message mMessages;
    private boolean mQuitting;
    private runtime.Poller mPoller;
    private boolean mBlocked;
    private boolean mPendingWake;

    MessageQueue(boolean quitAllowed)
    {
        mQuitAllowed = quitAllowed;
    }

    public void setPoller(runtime.Poller poller)
    {
        synchronized (this)
        {
            mPoller = poller;
        }
    }

    public boolean isIdle()
    {
        synchronized (this)
        {
            return mMessages == null || SystemClock.uptimeMillis() < mMessages.when;
        }
    }

    public interface IdleHandler
    {
        boolean queueIdle();
    }

    private final java.util.ArrayList<IdleHandler> mIdleHandlers = new java.util.ArrayList<>();

    public void addIdleHandler(IdleHandler handler)
    {
        if (handler == null) throw new NullPointerException("the idle handler is required");

        synchronized (this)
        {
            mIdleHandlers.add(handler);
        }
    }

    public void removeIdleHandler(IdleHandler handler)
    {
        synchronized (this)
        {
            mIdleHandlers.remove(handler);
        }
    }

    private void runIdleHandlers()
    {
        IdleHandler[] pending;

        synchronized (this)
        {
            if (mIdleHandlers.isEmpty()) return;

            pending = mIdleHandlers.toArray(new IdleHandler[0]);
        }

        for (IdleHandler handler : pending)
        {
            boolean keep;

            try
            {
                keep = handler.queueIdle();
            }
            catch (Throwable failure)
            {
                android.util.Log.e("MessageQueue", "an idle handler threw", failure);
                keep = false;
            }

            if (!keep) removeIdleHandler(handler);
        }
    }

    boolean enqueueMessage(Message message, long when)
    {
        if (message.target == null) throw new IllegalArgumentException("Message must have a target");
        if (message.isInUse()) throw new IllegalStateException(message + " This message is already in use.");

        synchronized (this)
        {
            if (mQuitting)
            {
                message.recycleUnchecked();
                return false;
            }

            message.inUse = true;
            message.when = when;

            Message cursor = mMessages;
            if (cursor == null || when < cursor.when)
            {
                message.next = cursor;
                mMessages = message;
            }
            else
            {
                Message previous;
                do
                {
                    previous = cursor;
                    cursor = cursor.next;
                }
                while (cursor != null && when >= cursor.when);

                message.next = cursor;
                previous.next = message;
            }

            wakeLocked();
            return true;
        }
    }

    private void wakeLocked()
    {
        mPendingWake = true;
        notifyAll();

        if (mPoller != null && mBlocked) mPoller.wake();
    }

    Message next()
    {
        boolean idleHandlersRan = false;

        for (;;)
        {
            long waitMillis;
            runtime.Poller poller;
            boolean idle;

            synchronized (this)
            {
                long now = SystemClock.uptimeMillis();
                if (mMessages != null && now >= mMessages.when)
                {
                    Message message = mMessages;
                    mMessages = message.next;
                    message.next = null;
                    message.inUse = true;
                    return message;
                }

                if (mQuitting) return null;

                waitMillis = mMessages == null ? -1 : mMessages.when - now;
                poller = mPoller;
                idle = !idleHandlersRan && !mIdleHandlers.isEmpty();

                if (poller == null && !idle)
                {
                    awaitLocked(waitMillis);
                    continue;
                }

                mPendingWake = false;
                mBlocked = true;
            }

            if (idle)
            {
                runIdleHandlers();
                idleHandlersRan = true;

                synchronized (this)
                {
                    mBlocked = false;
                }

                continue;
            }

            poller.poll(waitMillis);

            synchronized (this)
            {
                mBlocked = false;
            }
        }
    }

    private void awaitLocked(long waitMillis)
    {
        if (mPendingWake)
        {
            mPendingWake = false;
            return;
        }

        try
        {
            wait(waitMillis < 0 ? 0 : waitMillis);
        }
        catch (InterruptedException interrupted)
        {
            Thread.currentThread().interrupt();
            mPendingWake = false;
            throw new IllegalStateException("looper thread interrupted while waiting for messages", interrupted);
        }

        mPendingWake = false;
    }

    boolean hasMessages(Handler handler, Runnable callback, Object token, boolean matchWhat, int what)
    {
        synchronized (this)
        {
            for (Message cursor = mMessages; cursor != null; cursor = cursor.next)
            {
                if (matches(cursor, handler, callback, token, matchWhat, what)) return true;
            }
            return false;
        }
    }

    void removeCallbacksAndMessages(Handler handler, Runnable callback, Object token)
    {
        remove(handler, callback, token, false, 0);
    }

    void removeMessages(Handler handler, int what, Object token)
    {
        remove(handler, null, token, true, what);
    }

    private void remove(Handler handler, Runnable callback, Object token, boolean matchWhat, int what)
    {
        synchronized (this)
        {
            while (mMessages != null && matches(mMessages, handler, callback, token, matchWhat, what))
            {
                Message removed = mMessages;
                mMessages = removed.next;
                removed.recycleUnchecked();
            }

            Message cursor = mMessages;
            while (cursor != null)
            {
                Message following = cursor.next;
                if (following != null && matches(following, handler, callback, token, matchWhat, what))
                {
                    cursor.next = following.next;
                    following.recycleUnchecked();
                    continue;
                }
                cursor = following;
            }
        }
    }

    private static boolean matches(Message message, Handler handler, Runnable callback, Object token, boolean matchWhat, int what)
    {
        if (message.target != handler) return false;
        if (callback != null && message.callback != callback) return false;
        if (token != null && message.obj != token) return false;
        if (matchWhat && message.what != what) return false;
        return true;
    }

    void quit(boolean flush)
    {
        if (!mQuitAllowed) throw new IllegalStateException("Main thread not allowed to quit");
        forceQuit(flush);
    }

    void forceQuit(boolean flush)
    {
        synchronized (this)
        {
            if (mQuitting) return;
            mQuitting = true;

            if (flush)
            {
                removeFutureMessages(SystemClock.uptimeMillis());
            }
            else
            {
                removeAllMessages();
            }

            wakeLocked();
        }
    }

    private void removeAllMessages()
    {
        while (mMessages != null)
        {
            Message removed = mMessages;
            mMessages = removed.next;
            removed.recycleUnchecked();
        }
    }

    private void removeFutureMessages(long now)
    {
        while (mMessages != null && mMessages.when > now)
        {
            Message removed = mMessages;
            mMessages = removed.next;
            removed.recycleUnchecked();
        }

        Message cursor = mMessages;
        while (cursor != null)
        {
            Message following = cursor.next;
            if (following != null && following.when > now)
            {
                cursor.next = following.next;
                following.recycleUnchecked();
                continue;
            }
            cursor = following;
        }
    }
}
