package android.os;

public final class Messenger implements Parcelable
{
    public static final Parcelable.Creator<Messenger> CREATOR = new Parcelable.Creator<Messenger>()
    {
        @Override
        public Messenger createFromParcel(Parcel source)
        {
            return readMessengerOrNullFromParcel(source);
        }

        @Override
        public Messenger[] newArray(int size)
        {
            return new Messenger[size];
        }
    };

    private static final class HandlerBinder extends Binder
    {
        final Handler handler;

        HandlerBinder(Handler handler)
        {
            this.handler = handler;
        }
    }

    private final IBinder mTarget;

    public Messenger(Handler target)
    {
        mTarget = new HandlerBinder(target);
    }

    public Messenger(IBinder target)
    {
        mTarget = target;
    }

    public void send(Message message) throws RemoteException
    {
        if (!(mTarget instanceof HandlerBinder)) throw new DeadObjectException("the messenger's target lives in another process");

        Handler handler = ((HandlerBinder)mTarget).handler;
        if (handler == null || !handler.sendMessage(message)) throw new DeadObjectException("the messenger's handler is gone");
    }

    public IBinder getBinder()
    {
        return mTarget;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof Messenger && mTarget.equals(((Messenger)other).mTarget);
    }

    @Override
    public int hashCode()
    {
        return mTarget.hashCode();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeValue(this);
    }

    public static void writeMessengerOrNullToParcel(Messenger messenger, Parcel out)
    {
        out.writeValue(messenger);
    }

    public static Messenger readMessengerOrNullFromParcel(Parcel in)
    {
        Object value = in == null ? null : in.readValue(null);
        return value instanceof Messenger ? (Messenger)value : null;
    }
}
