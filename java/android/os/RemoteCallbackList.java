package android.os;

import android.util.ArrayMap;

public class RemoteCallbackList<E extends IInterface>
{
    private final ArrayMap<IBinder, Callback> mCallbacks = new ArrayMap<>();
    private Object[] mActiveBroadcast;
    private int mBroadcastCount = -1;
    private boolean mKilled = false;

    private final class Callback implements IBinder.DeathRecipient
    {
        final E mCallback;
        final Object mCookie;

        Callback(E callback, Object cookie)
        {
            mCallback = callback;
            mCookie = cookie;
        }

        @Override
        public void binderDied()
        {
            synchronized (mCallbacks)
            {
                mCallbacks.remove(mCallback.asBinder());
            }
            onCallbackDied(mCallback, mCookie);
        }
    }

    public RemoteCallbackList()
    {
    }

    public boolean register(E callback)
    {
        return register(callback, null);
    }

    public boolean register(E callback, Object cookie)
    {
        synchronized (mCallbacks)
        {
            if (mKilled) return false;

            IBinder binder = callback.asBinder();
            Callback registered = new Callback(callback, cookie);
            Callback previous = mCallbacks.put(binder, registered);
            if (previous != null && binder != null) binder.unlinkToDeath(previous, 0);
            if (binder != null) binder.linkToDeath(registered, 0);
            return true;
        }
    }

    public boolean unregister(E callback)
    {
        synchronized (mCallbacks)
        {
            Callback registered = mCallbacks.remove(callback.asBinder());
            if (registered == null) return false;

            IBinder binder = registered.mCallback.asBinder();
            if (binder != null) binder.unlinkToDeath(registered, 0);
            return true;
        }
    }

    public void kill()
    {
        synchronized (mCallbacks)
        {
            for (int index = mCallbacks.size() - 1; index >= 0; index--)
            {
                Callback registered = mCallbacks.valueAt(index);
                IBinder binder = registered.mCallback.asBinder();
                if (binder != null) binder.unlinkToDeath(registered, 0);
            }
            mCallbacks.clear();
            mKilled = true;
        }
    }

    public void onCallbackDied(E callback)
    {
    }

    public void onCallbackDied(E callback, Object cookie)
    {
        onCallbackDied(callback);
    }

    public int beginBroadcast()
    {
        synchronized (mCallbacks)
        {
            if (mBroadcastCount > 0) throw new IllegalStateException("beginBroadcast() called while already in a broadcast");

            final int count = mCallbacks.size();
            mBroadcastCount = count;
            if (count <= 0) return 0;

            Object[] active = mActiveBroadcast;
            if (active == null || active.length < count) mActiveBroadcast = active = new Object[count];
            for (int index = 0; index < count; index++) active[index] = mCallbacks.valueAt(index);
            return count;
        }
    }

    @SuppressWarnings("unchecked")
    public E getBroadcastItem(int index)
    {
        return ((Callback)mActiveBroadcast[index]).mCallback;
    }

    @SuppressWarnings("unchecked")
    public Object getBroadcastCookie(int index)
    {
        return ((Callback)mActiveBroadcast[index]).mCookie;
    }

    public void finishBroadcast()
    {
        synchronized (mCallbacks)
        {
            if (mBroadcastCount < 0) throw new IllegalStateException("finishBroadcast() called outside of a broadcast");

            Object[] active = mActiveBroadcast;
            if (active != null)
            {
                final int count = mBroadcastCount;
                for (int index = 0; index < count; index++) active[index] = null;
            }
            mBroadcastCount = -1;
        }
    }

    public int getRegisteredCallbackCount()
    {
        synchronized (mCallbacks)
        {
            if (mKilled) return 0;
            return mCallbacks.size();
        }
    }

    public E getRegisteredCallbackItem(int index)
    {
        synchronized (mCallbacks)
        {
            if (mKilled) return null;
            return mCallbacks.valueAt(index).mCallback;
        }
    }

    public Object getRegisteredCallbackCookie(int index)
    {
        synchronized (mCallbacks)
        {
            if (mKilled) return null;
            return mCallbacks.valueAt(index).mCookie;
        }
    }
}
