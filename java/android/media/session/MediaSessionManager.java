package android.media.session;

import android.content.ComponentName;
import android.content.Context;
import android.media.Session2Token;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

public final class MediaSessionManager
{
    private static final List<ActiveSessionsListenerRecord> ACTIVE_SESSIONS_LISTENERS = new ArrayList<>();
    private static final List<MediaKeyEventListenerRecord> MEDIA_KEY_EVENT_LISTENERS = new ArrayList<>();
    private static final List<Session2TokensListenerRecord> SESSION2_TOKENS_LISTENERS = new ArrayList<>();
    private static final List<Session2Token> SESSION2_TOKENS = new ArrayList<>();
    private static MediaSession.Token sMediaKeyEventSession;

    private final Context mContext;

    public MediaSessionManager(Context context)
    {
        mContext = context;
    }

    public List<MediaController> getActiveSessions(ComponentName notificationListener)
    {
        List<MediaController> controllers = new ArrayList<>();

        for (MediaSession session : MediaSession.getActiveSessionsSnapshot())
        {
            controllers.add(new MediaController(mContext, session.getSessionToken()));
        }

        return controllers;
    }

    public List<Session2Token> getSession2Tokens()
    {
        synchronized (SESSION2_TOKENS)
        {
            return new ArrayList<>(SESSION2_TOKENS);
        }
    }

    public void notifySession2Created(Session2Token token)
    {
        if (token == null) throw new IllegalArgumentException("token shouldn't be null");
        if (token.getType() != Session2Token.TYPE_SESSION) throw new IllegalArgumentException("token's type should be TYPE_SESSION");

        List<Session2Token> snapshot;

        synchronized (SESSION2_TOKENS)
        {
            if (SESSION2_TOKENS.contains(token)) return;
            SESSION2_TOKENS.add(0, token);
            snapshot = new ArrayList<>(SESSION2_TOKENS);
        }

        List<Session2TokensListenerRecord> records;

        synchronized (SESSION2_TOKENS_LISTENERS)
        {
            records = new ArrayList<>(SESSION2_TOKENS_LISTENERS);
        }

        for (Session2TokensListenerRecord record : records)
        {
            record.mHandler.post(() -> record.mListener.onSession2TokensChanged(new ArrayList<>(snapshot)));
        }
    }

    public boolean isTrustedForMediaControl(RemoteUserInfo userInfo)
    {
        if (userInfo == null) throw new IllegalArgumentException("userInfo may not be null");

        return mContext != null && Objects.equals(mContext.getPackageName(), userInfo.getPackageName());
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener sessionListener, ComponentName notificationListener)
    {
        addOnActiveSessionsChangedListener(sessionListener, notificationListener, null);
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener sessionListener, ComponentName notificationListener,
            Handler handler)
    {
        if (sessionListener == null) throw new IllegalArgumentException("listener may not be null");

        synchronized (ACTIVE_SESSIONS_LISTENERS)
        {
            for (ActiveSessionsListenerRecord record : ACTIVE_SESSIONS_LISTENERS)
            {
                if (record.mListener == sessionListener) return;
            }

            ACTIVE_SESSIONS_LISTENERS.add(new ActiveSessionsListenerRecord(sessionListener, handlerOrDefault(handler), mContext));
        }
    }

    public void removeOnActiveSessionsChangedListener(OnActiveSessionsChangedListener sessionListener)
    {
        if (sessionListener == null) throw new IllegalArgumentException("listener may not be null");

        synchronized (ACTIVE_SESSIONS_LISTENERS)
        {
            ACTIVE_SESSIONS_LISTENERS.removeIf(record -> record.mListener == sessionListener);
        }
    }

    public void addOnSession2TokensChangedListener(OnSession2TokensChangedListener listener)
    {
        addOnSession2TokensChangedListener(listener, null);
    }

    public void addOnSession2TokensChangedListener(OnSession2TokensChangedListener listener, Handler handler)
    {
        if (listener == null) throw new IllegalArgumentException("listener shouldn't be null");

        synchronized (SESSION2_TOKENS_LISTENERS)
        {
            for (Session2TokensListenerRecord record : SESSION2_TOKENS_LISTENERS)
            {
                if (record.mListener == listener) return;
            }

            SESSION2_TOKENS_LISTENERS.add(new Session2TokensListenerRecord(listener, handlerOrDefault(handler)));
        }
    }

    public void removeOnSession2TokensChangedListener(OnSession2TokensChangedListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("listener shouldn't be null");

        synchronized (SESSION2_TOKENS_LISTENERS)
        {
            SESSION2_TOKENS_LISTENERS.removeIf(record -> record.mListener == listener);
        }
    }

    public MediaSession.Token getMediaKeyEventSession()
    {
        synchronized (MEDIA_KEY_EVENT_LISTENERS)
        {
            return sMediaKeyEventSession;
        }
    }

    public String getMediaKeyEventSessionPackageName()
    {
        MediaSession.Token token = getMediaKeyEventSession();
        return token == null || token.mPackageName == null ? "" : token.mPackageName;
    }

    public void addOnMediaKeyEventSessionChangedListener(Executor executor, OnMediaKeyEventSessionChangedListener listener)
    {
        if (executor == null) throw new NullPointerException("executor shouldn't be null");
        if (listener == null) throw new NullPointerException("listener shouldn't be null");

        synchronized (MEDIA_KEY_EVENT_LISTENERS)
        {
            for (MediaKeyEventListenerRecord record : MEDIA_KEY_EVENT_LISTENERS)
            {
                if (record.mListener == listener) return;
            }

            MEDIA_KEY_EVENT_LISTENERS.add(new MediaKeyEventListenerRecord(listener, executor));
        }
    }

    public void removeOnMediaKeyEventSessionChangedListener(OnMediaKeyEventSessionChangedListener listener)
    {
        if (listener == null) throw new NullPointerException("listener shouldn't be null");

        synchronized (MEDIA_KEY_EVENT_LISTENERS)
        {
            MEDIA_KEY_EVENT_LISTENERS.removeIf(record -> record.mListener == listener);
        }
    }

    static void dispatchActiveSessionsChanged()
    {
        List<MediaSession> sessions = MediaSession.getActiveSessionsSnapshot();
        List<ActiveSessionsListenerRecord> records;

        synchronized (ACTIVE_SESSIONS_LISTENERS)
        {
            records = new ArrayList<>(ACTIVE_SESSIONS_LISTENERS);
        }

        for (ActiveSessionsListenerRecord record : records)
        {
            List<MediaController> controllers = new ArrayList<>();

            for (MediaSession session : sessions)
            {
                controllers.add(new MediaController(record.mContext, session.getSessionToken()));
            }

            record.mHandler.post(() -> record.mListener.onActiveSessionsChanged(controllers));
        }

        MediaSession.Token mediaKeyToken = null;

        for (MediaSession session : sessions)
        {
            if ((session.getFlags() & MediaSession.FLAG_HANDLES_MEDIA_BUTTONS) != 0 || mediaKeyToken == null)
            {
                mediaKeyToken = session.getSessionToken();
                if ((session.getFlags() & MediaSession.FLAG_HANDLES_MEDIA_BUTTONS) != 0) break;
            }
        }

        List<MediaKeyEventListenerRecord> keyRecords;

        synchronized (MEDIA_KEY_EVENT_LISTENERS)
        {
            if (Objects.equals(sMediaKeyEventSession, mediaKeyToken)) return;

            sMediaKeyEventSession = mediaKeyToken;
            keyRecords = new ArrayList<>(MEDIA_KEY_EVENT_LISTENERS);
        }

        MediaSession.Token token = mediaKeyToken;
        String packageName = token == null || token.mPackageName == null ? "" : token.mPackageName;

        for (MediaKeyEventListenerRecord record : keyRecords)
        {
            record.mExecutor.execute(() -> record.mListener.onMediaKeyEventSessionChanged(packageName, token));
        }
    }

    private static Handler handlerOrDefault(Handler handler)
    {
        if (handler != null) return handler;

        Looper looper = Looper.myLooper();
        return new Handler(looper != null ? looper : Looper.getMainLooper());
    }

    private static final class ActiveSessionsListenerRecord
    {
        final OnActiveSessionsChangedListener mListener;
        final Handler mHandler;
        final Context mContext;

        ActiveSessionsListenerRecord(OnActiveSessionsChangedListener listener, Handler handler, Context context)
        {
            mListener = listener;
            mHandler = handler;
            mContext = context;
        }
    }

    private static final class MediaKeyEventListenerRecord
    {
        final OnMediaKeyEventSessionChangedListener mListener;
        final Executor mExecutor;

        MediaKeyEventListenerRecord(OnMediaKeyEventSessionChangedListener listener, Executor executor)
        {
            mListener = listener;
            mExecutor = executor;
        }
    }

    private static final class Session2TokensListenerRecord
    {
        final OnSession2TokensChangedListener mListener;
        final Handler mHandler;

        Session2TokensListenerRecord(OnSession2TokensChangedListener listener, Handler handler)
        {
            mListener = listener;
            mHandler = handler;
        }
    }

    public static final class RemoteUserInfo
    {
        private final String mPackageName;
        private final int mPid;
        private final int mUid;

        public RemoteUserInfo(String packageName, int pid, int uid)
        {
            mPackageName = packageName;
            mPid = pid;
            mUid = uid;
        }

        public String getPackageName()
        {
            return mPackageName;
        }

        public int getPid()
        {
            return mPid;
        }

        public int getUid()
        {
            return mUid;
        }

        @Override
        public boolean equals(Object object)
        {
            if (object == this) return true;
            if (!(object instanceof RemoteUserInfo)) return false;

            RemoteUserInfo other = (RemoteUserInfo)object;

            return Objects.equals(mPackageName, other.mPackageName) && mPid == other.mPid && mUid == other.mUid;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mPackageName, mPid, mUid);
        }
    }

    public interface OnActiveSessionsChangedListener
    {
        void onActiveSessionsChanged(List<MediaController> controllers);
    }

    public interface OnSession2TokensChangedListener
    {
        void onSession2TokensChanged(List<Session2Token> tokens);
    }

    public interface OnMediaKeyEventSessionChangedListener
    {
        void onMediaKeyEventSessionChanged(String packageName, MediaSession.Token sessionToken);
    }
}
