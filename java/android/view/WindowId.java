package android.view;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;

public class WindowId implements Parcelable
{
    public static final Parcelable.Creator<WindowId> CREATOR = new Parcelable.Creator<WindowId>()
    {
        @Override
        public WindowId createFromParcel(Parcel source)
        {
            return new WindowId(source.readStrongBinder());
        }

        @Override
        public WindowId[] newArray(int size)
        {
            return new WindowId[size];
        }
    };

    private final android.os.IBinder mToken;
    private final ArrayList<FocusObserver> mObservers = new ArrayList<>();
    private boolean mFocused = true;

    WindowId(android.os.IBinder token)
    {
        mToken = token;
    }

    public boolean isFocused()
    {
        return mFocused;
    }

    void setFocused(boolean focused)
    {
        if (mFocused == focused) return;

        mFocused = focused;

        for (FocusObserver observer : new ArrayList<>(mObservers))
        {
            if (focused) observer.onFocusGained(this);
            else observer.onFocusLost(this);
        }
    }

    public void registerFocusObserver(FocusObserver observer)
    {
        if (observer != null && !mObservers.contains(observer)) mObservers.add(observer);
    }

    public void unregisterFocusObserver(FocusObserver observer)
    {
        mObservers.remove(observer);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof WindowId)) return false;
        return mToken == ((WindowId)other).mToken;
    }

    @Override
    public int hashCode()
    {
        return mToken == null ? 0 : System.identityHashCode(mToken);
    }

    @Override
    public String toString()
    {
        return "WindowId{" + Integer.toHexString(System.identityHashCode(this)) + ": " + mToken + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeStrongBinder(mToken);
    }

    public static abstract class FocusObserver
    {
        public FocusObserver()
        {
        }

        public abstract void onFocusGained(WindowId token);

        public abstract void onFocusLost(WindowId token);
    }
}
