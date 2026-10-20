package android.os;

public final class Looper
{
    private static final ThreadLocal<Looper> THREAD_LOOPER = new ThreadLocal<>();
    private static Looper sMainLooper;

    final MessageQueue mQueue;
    final Thread mThread;

    private Looper(boolean quitAllowed)
    {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }

    public static void prepare()
    {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed)
    {
        if (THREAD_LOOPER.get() != null) throw new RuntimeException("Only one Looper may be created per thread");
        THREAD_LOOPER.set(new Looper(quitAllowed));
    }

    public static void prepareMainLooper()
    {
        prepare(false);
        synchronized (Looper.class)
        {
            if (sMainLooper != null) throw new IllegalStateException("The main Looper has already been prepared");
            sMainLooper = myLooper();
        }
    }

    public static Looper getMainLooper()
    {
        synchronized (Looper.class)
        {
            return sMainLooper;
        }
    }

    public static Looper myLooper()
    {
        return THREAD_LOOPER.get();
    }

    public static MessageQueue myQueue()
    {
        Looper looper = myLooper();
        if (looper == null) throw new IllegalStateException("No Looper on this thread");
        return looper.mQueue;
    }

    public static void loop()
    {
        Looper looper = myLooper();
        if (looper == null) throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread");

        for (;;)
        {
            Message message = looper.mQueue.next();
            if (message == null) return;

            message.target.dispatchMessage(message);
            message.recycleUnchecked();
        }
    }

    public MessageQueue getQueue()
    {
        return mQueue;
    }

    public Thread getThread()
    {
        return mThread;
    }

    public boolean isCurrentThread()
    {
        return Thread.currentThread() == mThread;
    }

    public static void quitMainLooper()
    {
        Looper main = getMainLooper();
        if (main != null) main.mQueue.forceQuit(true);
    }

    public void quit()
    {
        mQueue.quit(false);
    }

    public void quitSafely()
    {
        mQueue.quit(true);
    }

    @Override
    public String toString()
    {
        return "Looper{" + mThread.getName() + '}';
    }
}
