package android.os;

public final class Message implements Parcelable
{
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>()
    {
        @Override
        public Message createFromParcel(Parcel source)
        {
            Message message = Message.obtain();
            message.what = source.readInt();
            message.arg1 = source.readInt();
            message.arg2 = source.readInt();
            message.obj = source.readValue(null);
            message.when = source.readLong();
            message.mData = source.readBundle();
            message.replyTo = Messenger.readMessengerOrNullFromParcel(source);
            message.sendingUid = source.readInt();
            return message;
        }

        @Override
        public Message[] newArray(int size)
        {
            return new Message[size];
        }
    };

    private static final Object POOL_LOCK = new Object();
    private static final int MAX_POOL_SIZE = 50;

    private static Message sPool;
    private static int sPoolSize;

    public int what;
    public int arg1;
    public int arg2;
    public Object obj;
    public Messenger replyTo;
    public int sendingUid = -1;

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
        message.replyTo = other.replyTo;
        message.sendingUid = other.sendingUid;
        if (other.mData != null) message.mData = new Bundle(other.mData);
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
        replyTo = null;
        sendingUid = -1;
        mData = null;
        mAsynchronous = false;
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

    public void copyFrom(Message other)
    {
        what = other.what;
        arg1 = other.arg1;
        arg2 = other.arg2;
        obj = other.obj;
        replyTo = other.replyTo;
        sendingUid = other.sendingUid;
        mData = other.mData == null ? null : new Bundle(other.mData);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        if (callback != null) throw new RuntimeException("Can't marshal callbacks across processes.");

        dest.writeInt(what);
        dest.writeInt(arg1);
        dest.writeInt(arg2);
        dest.writeValue(obj);
        dest.writeLong(when);
        dest.writeBundle(mData);
        Messenger.writeMessengerOrNullToParcel(replyTo, dest);
        dest.writeInt(sendingUid);
    }

    @Override
    public String toString()
    {
        return "Message{what=" + what + " when=" + when + '}';
    }
}
