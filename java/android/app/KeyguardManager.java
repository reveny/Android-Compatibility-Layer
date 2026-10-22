package android.app;

import android.content.Context;
import android.content.Intent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class KeyguardManager
{
    private final Context mContext;
    private final Map<KeyguardLockedStateListener, Executor> mLockedStateListeners = new HashMap<>();

    public KeyguardManager(Context context)
    {
        mContext = context;
    }

    public Intent createConfirmDeviceCredentialIntent(CharSequence title, CharSequence description)
    {
        return null;
    }

    public KeyguardLock newKeyguardLock(String tag)
    {
        return new KeyguardLock(tag);
    }

    public boolean isKeyguardLocked()
    {
        return false;
    }

    public boolean isKeyguardSecure()
    {
        return false;
    }

    public boolean inKeyguardRestrictedInputMode()
    {
        return false;
    }

    public boolean isDeviceLocked()
    {
        return false;
    }

    public boolean isDeviceSecure()
    {
        return false;
    }

    public void requestDismissKeyguard(Activity activity, final KeyguardDismissCallback callback)
    {
        if (callback == null) return;

        android.os.Looper main = android.os.Looper.getMainLooper();
        if (main == null)
        {
            callback.onDismissSucceeded();
            return;
        }

        new android.os.Handler(main).post(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onDismissSucceeded();
            }
        });
    }

    public void exitKeyguardSecurely(OnKeyguardExitResult callback)
    {
        if (callback != null) callback.onKeyguardExitResult(true);
    }

    public void addKeyguardLockedStateListener(Executor executor, KeyguardLockedStateListener listener)
    {
        if (executor == null || listener == null) throw new NullPointerException("executor and listener are required");

        synchronized (mLockedStateListeners)
        {
            mLockedStateListeners.put(listener, executor);
        }
    }

    public void removeKeyguardLockedStateListener(KeyguardLockedStateListener listener)
    {
        synchronized (mLockedStateListeners)
        {
            mLockedStateListeners.remove(listener);
        }
    }

    public class KeyguardLock
    {
        private final String mTag;

        KeyguardLock(String tag)
        {
            mTag = tag;
        }

        public void disableKeyguard()
        {
        }

        public void reenableKeyguard()
        {
        }
    }

    public abstract static class KeyguardDismissCallback
    {
        public KeyguardDismissCallback()
        {
        }

        public void onDismissError()
        {
        }

        public void onDismissSucceeded()
        {
        }

        public void onDismissCancelled()
        {
        }
    }

    public interface OnKeyguardExitResult
    {
        void onKeyguardExitResult(boolean success);
    }

    public interface KeyguardLockedStateListener
    {
        void onKeyguardLockedStateChanged(boolean isKeyguardLocked);
    }
}
