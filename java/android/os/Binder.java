package android.os;

public class Binder implements IBinder
{
    private String mDescriptor;
    private IInterface mOwner;

    public Binder()
    {
    }

    public Binder(String descriptor)
    {
        mDescriptor = descriptor;
    }

    public void attachInterface(IInterface owner, String descriptor)
    {
        mOwner = owner;
        mDescriptor = descriptor;
    }

    public IInterface queryLocalInterface(String descriptor)
    {
        return descriptor != null && descriptor.equals(mDescriptor) ? mOwner : null;
    }

    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
    {
        if (code == INTERFACE_TRANSACTION)
        {
            if (reply != null) reply.writeString(getInterfaceDescriptor());
            return true;
        }

        return false;
    }

    public final boolean transact(int code, Parcel data, Parcel reply, int flags)
    {
        if (data != null) data.setDataPosition(0);

        boolean handled = onTransact(code, data, reply, flags);
        if (reply != null) reply.setDataPosition(0);

        return handled;
    }

    public void linkToDeath(DeathRecipient recipient, int flags)
    {
    }

    public boolean unlinkToDeath(DeathRecipient recipient, int flags)
    {
        return true;
    }

    public void dump(java.io.FileDescriptor target, String[] arguments)
    {
    }

    @Override
    public String getInterfaceDescriptor()
    {
        return mDescriptor;
    }

    @Override
    public boolean pingBinder()
    {
        return true;
    }

    @Override
    public boolean isBinderAlive()
    {
        return true;
    }

    public static final int getCallingPid()
    {
        return android.os.Process.myPid();
    }

    public static final int getCallingUid()
    {
        return android.os.Process.myUid();
    }

    public static final int getCallingUidOrThrow()
    {
        return android.os.Process.myUid();
    }

    public static final UserHandle getCallingUserHandle()
    {
        return android.os.Process.myUserHandle();
    }

    public static final long clearCallingIdentity()
    {
        return ((long)getCallingUid() << 32) | (getCallingPid() & 0xffffffffL);
    }

    public static final void restoreCallingIdentity(long token)
    {
    }

    public static final void flushPendingCommands()
    {
    }

    public static final void joinThreadPool()
    {
    }

    public void dumpAsync(final java.io.FileDescriptor target, final String[] arguments)
    {
        Thread worker = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                dump(target, arguments);
            }
        }, "Binder.dumpAsync");
        worker.start();
    }
}
