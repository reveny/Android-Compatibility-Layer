package android.os;

public class ConditionVariable
{
    private volatile boolean mCondition;

    public ConditionVariable()
    {
        mCondition = false;
    }

    public ConditionVariable(boolean state)
    {
        mCondition = state;
    }

    public void open()
    {
        synchronized (this)
        {
            boolean old = mCondition;
            mCondition = true;
            if (!old) notifyAll();
        }
    }

    public void close()
    {
        synchronized (this)
        {
            mCondition = false;
        }
    }

    public void block()
    {
        synchronized (this)
        {
            while (!mCondition)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException ignored)
                {
                    continue;
                }
            }
        }
    }

    public boolean block(long timeoutMillis)
    {
        if (timeoutMillis == 0)
        {
            block();
            return true;
        }

        synchronized (this)
        {
            long now = SystemClock.elapsedRealtime();
            long end = now + timeoutMillis;
            while (!mCondition && now < end)
            {
                try
                {
                    wait(end - now);
                }
                catch (InterruptedException ignored)
                {
                    now = SystemClock.elapsedRealtime();
                    continue;
                }
                now = SystemClock.elapsedRealtime();
            }
            return mCondition;
        }
    }
}
