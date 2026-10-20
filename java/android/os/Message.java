package android.os;

public final class Message
{
    private static final Object POOL_LOCK = new Object();
    private static final int MAX_POOL_SIZE = 50;

    private static Message sPool;
    private static int sPoolSize;

    public int what;
    public int arg1;
    public int arg2;
    public Object obj;

    Handler target;
    Runnable callback;
    long when;
    Message next;
    boolean inUse;

    public Message()
    {
    }

    public static Message obtain()
    {
        synchronized (POOL_LOCK)
        {
            if (sPool != null)
            {
                Message message = sPool;
                sPool = message.next;
                message.next = null;
                message.inUse = false;
                sPoolSize--;
                return message;
            }
        }
        return new Message();
    }

    public boolean isInUse()
    {
        return inUse;
    }

    public static Message obtain(Handler target)
    {
        Message message = obtain();
        message.target = target;
        return message;
    }

    public static Message obtain(Handler target, int what)
    {
        Message message = obtain(target);
        message.what = what;
        return message;
    }

    public static Message obtain(Handler target, Runnable callback)
    {
        Message message = obtain(target);
        message.callback = callback;
        return message;
    }

    public static Message obtain(Handler target, int what, Object object)
    {
        Message message = obtain(target, what);
        message.obj = object;
        return message;
    }

    public static Message obtain(Handler target, int what, int arg1, int arg2)
    {
        Message message = obtain(target, what);
        message.arg1 = arg1;
        message.arg2 = arg2;
        return message;
    }

    public static Message obtain(Handler target, int what, int arg1, int arg2, Object object)
    {
        Message message = obtain(target, what, arg1, arg2);
        message.obj = object;
        return message;
    }

    public static Message obtain(Message other)
    {
        Message message = obtain();

        if (other == null) return message;

        message.what = other.what;
        message.arg1 = other.arg1;
        message.arg2 = other.arg2;
        message.obj = other.obj;
        message.callback = other.callback;
        message.setTarget(other.getTarget());

        return message;
    }

    public Handler getTarget()
    {
        return target;
    }

    private Bundle mData;

    public void setData(Bundle data)
    {
        mData = data;
    }

    public Bundle getData()
    {
        if (mData == null) mData = new Bundle();

        return mData;
    }

    public Bundle peekData()
    {
        return mData;
    }

    private boolean mAsynchronous;

    public void setAsynchronous(boolean asynchronous)
    {
        mAsynchronous = asynchronous;
    }

    public boolean isAsynchronous()
    {
        return mAsynchronous;
    }

    public void setTarget(Handler target)
    {
        this.target = target;
    }

    public Runnable getCallback()
    {
        return callback;
    }

    public long getWhen()
    {
        return when;
    }

    public void sendToTarget()
    {
        target.sendMessage(this);
    }

    public void recycle()
    {
        if (inUse) throw new IllegalStateException("This message cannot be recycled because it is still in use.");
        recycleUnchecked();
    }

    void recycleUnchecked()
    {
        what = 0;
        arg1 = 0;
        arg2 = 0;
        obj = null;
        target = null;
        callback = null;
        when = 0;
        next = null;
        inUse = true;

        synchronized (POOL_LOCK)
        {
            if (sPoolSize >= MAX_POOL_SIZE) return;
            next = sPool;
            sPool = this;
            sPoolSize++;
        }
    }

    @Override
    public String toString()
    {
        return "Message{what=" + what + " when=" + when + '}';
    }
}
