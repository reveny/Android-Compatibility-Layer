package android.view.inputmethod;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public final class InputBinding implements Parcelable
{
    private final InputConnection mConnection;
    private final IBinder mConnectionToken;
    private final int mUid;
    private final int mPid;

    public InputBinding(InputConnection connection, IBinder connectionToken, int uid, int pid)
    {
        mConnection = connection;
        mConnectionToken = connectionToken;
        mUid = uid;
        mPid = pid;
    }

    public InputBinding(InputConnection connection, InputBinding binding)
    {
        mConnection = connection;
        mConnectionToken = binding.getConnectionToken();
        mUid = binding.getUid();
        mPid = binding.getPid();
    }

    private InputBinding(Parcel source)
    {
        mConnection = null;
        mConnectionToken = source.readStrongBinder();
        mUid = source.readInt();
        mPid = source.readInt();
    }

    public InputConnection getConnection()
    {
        return mConnection;
    }

    public IBinder getConnectionToken()
    {
        return mConnectionToken;
    }

    public int getUid()
    {
        return mUid;
    }

    public int getPid()
    {
        return mPid;
    }

    @Override
    public String toString()
    {
        return "InputBinding{" + mConnectionToken + " / uid " + mUid + " / pid " + mPid + "}";
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeStrongBinder(mConnectionToken);
        out.writeInt(mUid);
        out.writeInt(mPid);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Parcelable.Creator<InputBinding> CREATOR = new Parcelable.Creator<InputBinding>()
    {
        @Override
        public InputBinding createFromParcel(Parcel source)
        {
            return new InputBinding(source);
        }

        @Override
        public InputBinding[] newArray(int size)
        {
            return new InputBinding[size];
        }
    };
}
