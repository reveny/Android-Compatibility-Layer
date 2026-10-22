package android.inputmethodservice;

import android.app.Service;
import android.content.ComponentCallbacks;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodSession;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class AbstractInputMethodService extends Service implements KeyEvent.Callback
{
    private final KeyEvent.DispatcherState mDispatcherState = new KeyEvent.DispatcherState();
    private InputMethod mInputMethod;
    private IBinder mBinder;

    public AbstractInputMethodService()
    {
    }

    public abstract class AbstractInputMethodImpl implements InputMethod
    {
        public AbstractInputMethodImpl()
        {
        }

        @Override
        public void createSession(SessionCallback callback)
        {
            if (callback != null) callback.sessionCreated(onCreateInputMethodSessionInterface());
        }

        @Override
        public void setSessionEnabled(InputMethodSession session, boolean enabled)
        {
            if (session instanceof AbstractInputMethodSessionImpl) ((AbstractInputMethodSessionImpl)session).setEnabled(enabled);
        }

        @Override
        public void revokeSession(InputMethodSession session)
        {
            if (session instanceof AbstractInputMethodSessionImpl) ((AbstractInputMethodSessionImpl)session).revokeSelf();
        }
    }

    public abstract class AbstractInputMethodSessionImpl implements InputMethodSession
    {
        private boolean mEnabled = true;
        private boolean mRevoked;

        public AbstractInputMethodSessionImpl()
        {
        }

        public boolean isEnabled()
        {
            return mEnabled;
        }

        public boolean isRevoked()
        {
            return mRevoked;
        }

        public void setEnabled(boolean enabled)
        {
            if (!mRevoked) mEnabled = enabled;
        }

        public void revokeSelf()
        {
            mRevoked = true;
            mEnabled = false;
        }

        @Override
        public void dispatchKeyEvent(int seq, KeyEvent event, EventCallback callback)
        {
            boolean handled = event.dispatch(AbstractInputMethodService.this, mDispatcherState, this);
            if (callback != null) callback.finishedEvent(seq, handled);
        }

        @Override
        public void dispatchTrackballEvent(int seq, MotionEvent event, EventCallback callback)
        {
            boolean handled = onTrackballEvent(event);
            if (callback != null) callback.finishedEvent(seq, handled);
        }

        @Override
        public void dispatchGenericMotionEvent(int seq, MotionEvent event, EventCallback callback)
        {
            boolean handled = onGenericMotionEvent(event);
            if (callback != null) callback.finishedEvent(seq, handled);
        }

        public boolean onShouldVerifyKeyEvent(KeyEvent event)
        {
            return AbstractInputMethodService.this.onShouldVerifyKeyEvent(event);
        }
    }

    public KeyEvent.DispatcherState getKeyDispatcherState()
    {
        return mDispatcherState;
    }

    public abstract AbstractInputMethodImpl onCreateInputMethodInterface();

    public abstract AbstractInputMethodSessionImpl onCreateInputMethodSessionInterface();

    InputMethod inputMethod()
    {
        if (mInputMethod == null) mInputMethod = onCreateInputMethodInterface();
        return mInputMethod;
    }

    @Override
    protected void dump(FileDescriptor descriptor, PrintWriter writer, String[] arguments)
    {
    }

    @Override
    public final IBinder onBind(Intent intent)
    {
        inputMethod();
        if (mBinder == null) mBinder = new android.os.Binder();
        return mBinder;
    }

    public boolean onTrackballEvent(MotionEvent event)
    {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent event)
    {
        return false;
    }

    public boolean onShouldVerifyKeyEvent(KeyEvent event)
    {
        return false;
    }

    @Override
    public Object getSystemService(String name)
    {
        return super.getSystemService(name);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfiguration)
    {
        super.onConfigurationChanged(newConfiguration);
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void registerComponentCallbacks(ComponentCallbacks callback)
    {
        super.registerComponentCallbacks(callback);
    }

    @Override
    public void unregisterComponentCallbacks(ComponentCallbacks callback)
    {
        super.unregisterComponentCallbacks(callback);
    }
}
