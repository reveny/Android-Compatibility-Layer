package android.os;

public class HandlerThread extends Thread
{
    private final int mPriority;
    private Looper mLooper;
    private int mThreadId = -1;

    public HandlerThread(String name)
    {
        this(name, Process.THREAD_PRIORITY_DEFAULT);
    }

    public HandlerThread(String name, int priority)
    {
        super(name);
        mPriority = priority;
    }

    protected void onLooperPrepared()
    {
    }

    @Override
    public void run()
    {
        mThreadId = Process.myTid();
        Looper.prepare();

        synchronized (this)
        {
            mLooper = Looper.myLooper();
            notifyAll();
        }

        Process.setThreadPriority(mPriority);
        onLooperPrepared();
        Looper.loop();

        mThreadId = -1;
    }

    public Looper getLooper()
    {
        if (!isAlive()) return null;

        synchronized (this)
        {
            while (isAlive() && mLooper == null)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException interrupted)
                {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        return mLooper;
    }

    public boolean quit()
    {
        Looper looper = getLooper();
        if (looper == null) return false;

        looper.quit();
        return true;
    }

    public boolean quitSafely()
    {
        return quit();
    }

    public int getThreadId()
    {
        return mThreadId;
    }
}
