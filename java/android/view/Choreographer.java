package android.view;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import java.util.ArrayList;

public final class Choreographer
{
    public static final int CALLBACK_INPUT = 0;
    public static final int CALLBACK_ANIMATION = 1;
    public static final int CALLBACK_INSETS_ANIMATION = 2;
    public static final int CALLBACK_TRAVERSAL = 3;
    public static final int CALLBACK_COMMIT = 4;

    private static final int CALLBACK_TYPE_COUNT = 5;
    private static final long FRAME_INTERVAL_MILLIS = 16;
    private static final long BACKSTOP_INTERVAL_MILLIS = 50;

    private static final ThreadLocal<Choreographer> THREAD_INSTANCE = new ThreadLocal<Choreographer>()
    {
        @Override
        protected Choreographer initialValue()
        {
            Looper looper = Looper.myLooper();
            if (looper == null) throw new IllegalStateException("The current thread must have a looper!");
            return new Choreographer(looper);
        }
    };

    private static final int FRAME_TIMELINE_COUNT = 3;

    private final Handler mHandler;
    private final ArrayList<Object> mFrameCallbacks = new ArrayList<>();
    private long mVsyncId;
    private final ArrayList<TypedCallback>[] mTypedCallbacks = newQueues();
    private final ArrayList<DelayedTypedCallback> mPendingDelayed = new ArrayList<>();
    private long mLastFrameTimeNanos;
    private boolean mTimerArmed;
    private final Runnable mTimerFrame = new TimerFrame(this);

    private Choreographer(Looper looper)
    {
        mHandler = new Handler(looper);
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<TypedCallback>[] newQueues()
    {
        ArrayList<TypedCallback>[] queues = new ArrayList[CALLBACK_TYPE_COUNT];
        for (int index = 0; index < CALLBACK_TYPE_COUNT; index++)
        {
            queues[index] = new ArrayList<>();
        }
        return queues;
    }

    public static Choreographer getInstance()
    {
        return THREAD_INSTANCE.get();
    }

    public void postFrameCallback(FrameCallback callback)
    {
        postFrameCallbackDelayed(callback, 0);
    }

    public void postFrameCallbackDelayed(FrameCallback callback, long delayMillis)
    {
        if (callback == null) throw new IllegalArgumentException("callback must not be null");

        if (delayMillis > 0)
        {
            Message message = Message.obtain(mHandler, new DelayedFrameCallback(this, callback));
            message.obj = callback;
            mHandler.sendMessageDelayed(message, delayMillis);
            return;
        }

        synchronized (mFrameCallbacks)
        {
            mFrameCallbacks.add(callback);
        }

        scheduleFrame();
    }

    public void removeFrameCallback(FrameCallback callback)
    {
        if (callback == null) return;

        synchronized (mFrameCallbacks)
        {
            mFrameCallbacks.remove(callback);
        }
        mHandler.removeCallbacksAndMessages(callback);
    }

    public void postVsyncCallback(VsyncCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("callback must not be null");

        synchronized (mFrameCallbacks)
        {
            mFrameCallbacks.add(callback);
        }

        scheduleFrame();
    }

    public void removeVsyncCallback(VsyncCallback callback)
    {
        if (callback == null) return;

        synchronized (mFrameCallbacks)
        {
            mFrameCallbacks.remove(callback);
        }
    }

    private static long frameIntervalNanos()
    {
        float refreshRate = runtime.WindowBridge.refreshRate();
        if (refreshRate <= 0) refreshRate = runtime.WindowBridge.FALLBACK_REFRESH_RATE;

        return (long)(1000000000L / refreshRate);
    }

    public void doFrame(long frameTimeNanos)
    {
        mLastFrameTimeNanos = frameTimeNanos;

        if (mTimerArmed)
        {
            mTimerArmed = false;
            mHandler.removeCallbacks(mTimerFrame);
        }

        runTyped(CALLBACK_INPUT);
        runTyped(CALLBACK_ANIMATION);
        runTyped(CALLBACK_INSETS_ANIMATION);

        runFrameCallbacks(frameTimeNanos);

        runTyped(CALLBACK_TRAVERSAL);
        runTyped(CALLBACK_COMMIT);
    }

    private void runTyped(int type)
    {
        TypedCallback[] due;

        synchronized (mTypedCallbacks)
        {
            if (mTypedCallbacks[type].isEmpty()) return;

            due = mTypedCallbacks[type].toArray(new TypedCallback[0]);
            mTypedCallbacks[type].clear();
        }

        for (TypedCallback entry : due)
        {
            entry.action.run();
        }
    }

    private void runFrameCallbacks(long frameTimeNanos)
    {
        Object[] pending;

        synchronized (mFrameCallbacks)
        {
            if (mFrameCallbacks.isEmpty()) return;

            pending = mFrameCallbacks.toArray();
            mFrameCallbacks.clear();
        }

        FrameData frameData = null;

        for (Object callback : pending)
        {
            if (callback instanceof FrameCallback)
            {
                ((FrameCallback)callback).doFrame(frameTimeNanos);
                continue;
            }

            if (frameData == null) frameData = newFrameData(frameTimeNanos);
            ((VsyncCallback)callback).onVsync(frameData);
        }
    }

    private FrameData newFrameData(long frameTimeNanos)
    {
        long interval = frameIntervalNanos();
        FrameTimeline[] timelines = new FrameTimeline[FRAME_TIMELINE_COUNT];

        for (int index = 0; index < FRAME_TIMELINE_COUNT; index++)
        {
            long presentation = frameTimeNanos + interval * (index + 1);
            timelines[index] = new FrameTimeline(++mVsyncId, presentation, presentation - interval / 2);
        }

        return new FrameData(frameTimeNanos, timelines, 0);
    }

    public void postCallback(int callbackType, Runnable action, Object token)
    {
        postCallbackDelayed(callbackType, action, token, 0);
    }

    public void postCallbackDelayed(int callbackType, Runnable action, Object token, long delayMillis)
    {
        if (action == null) throw new IllegalArgumentException("action must not be null");
        if (callbackType < 0 || callbackType >= CALLBACK_TYPE_COUNT)
        {
            throw new IllegalArgumentException("unknown callback type " + callbackType);
        }

        if (delayMillis > 0)
        {
            DelayedTypedCallback pending = new DelayedTypedCallback(this, callbackType, action, token);
            synchronized (mPendingDelayed)
            {
                mPendingDelayed.add(pending);
            }

            Message message = Message.obtain(mHandler, pending);
            message.obj = token;
            mHandler.sendMessageDelayed(message, delayMillis);
            return;
        }

        synchronized (mTypedCallbacks)
        {
            mTypedCallbacks[callbackType].add(new TypedCallback(action, token));
        }

        scheduleFrame();
    }

    public void removeCallbacks(int callbackType, Runnable action, Object token)
    {
        if (callbackType < 0 || callbackType >= CALLBACK_TYPE_COUNT) return;

        DelayedTypedCallback[] pending;
        synchronized (mPendingDelayed)
        {
            pending = mPendingDelayed.toArray(new DelayedTypedCallback[0]);
        }

        for (DelayedTypedCallback entry : pending)
        {
            if (entry.mType != callbackType) continue;
            if (action != null && entry.mAction != action) continue;
            if (token != null && entry.mToken != token) continue;

            mHandler.removeCallbacks(entry, entry.mToken);
            synchronized (mPendingDelayed)
            {
                mPendingDelayed.remove(entry);
            }
        }

        synchronized (mTypedCallbacks)
        {
            ArrayList<TypedCallback> queue = mTypedCallbacks[callbackType];
            for (int index = queue.size() - 1; index >= 0; index--)
            {
                TypedCallback entry = queue.get(index);
                if (action != null && entry.action != action) continue;
                if (token != null && entry.token != token) continue;
                queue.remove(index);
            }
        }

        mHandler.removeCallbacks(action, token);
    }

    private void scheduleFrame()
    {
        runtime.WindowBridge window = runtime.WindowBridge.get();
        if (window != null) window.requestFrame();

        if (mTimerArmed) return;

        mTimerArmed = true;
        mHandler.postDelayed(mTimerFrame, window != null ? BACKSTOP_INTERVAL_MILLIS : FRAME_INTERVAL_MILLIS);
    }

    public long getFrameTime()
    {
        return getFrameTimeNanos() / 1000000L;
    }

    public long getFrameTimeNanos()
    {
        return mLastFrameTimeNanos != 0 ? mLastFrameTimeNanos : SystemClock.uptimeNanos();
    }

    public interface FrameCallback
    {
        void doFrame(long frameTimeNanos);
    }

    public interface VsyncCallback
    {
        void onVsync(FrameData data);
    }

    public static class FrameData
    {
        private final long mFrameTimeNanos;
        private final FrameTimeline[] mFrameTimelines;
        private final int mPreferredIndex;

        FrameData(long frameTimeNanos, FrameTimeline[] frameTimelines, int preferredIndex)
        {
            mFrameTimeNanos = frameTimeNanos;
            mFrameTimelines = frameTimelines;
            mPreferredIndex = preferredIndex;
        }

        public long getFrameTimeNanos()
        {
            return mFrameTimeNanos;
        }

        public FrameTimeline[] getFrameTimelines()
        {
            return mFrameTimelines.clone();
        }

        public FrameTimeline getPreferredFrameTimeline()
        {
            return mFrameTimelines[mPreferredIndex];
        }
    }

    public static class FrameTimeline
    {
        private final long mVsyncId;
        private final long mExpectedPresentationTimeNanos;
        private final long mDeadlineNanos;

        FrameTimeline(long vsyncId, long expectedPresentationTimeNanos, long deadlineNanos)
        {
            mVsyncId = vsyncId;
            mExpectedPresentationTimeNanos = expectedPresentationTimeNanos;
            mDeadlineNanos = deadlineNanos;
        }

        public long getVsyncId()
        {
            return mVsyncId;
        }

        public long getExpectedPresentationTimeNanos()
        {
            return mExpectedPresentationTimeNanos;
        }

        public long getDeadlineNanos()
        {
            return mDeadlineNanos;
        }
    }

    private static final class TypedCallback
    {
        final Runnable action;
        final Object token;

        TypedCallback(Runnable action, Object token)
        {
            this.action = action;
            this.token = token;
        }
    }

    private static final class TimerFrame implements Runnable
    {
        private final Choreographer mChoreographer;

        TimerFrame(Choreographer choreographer)
        {
            mChoreographer = choreographer;
        }

        @Override
        public void run()
        {
            mChoreographer.doFrame(SystemClock.uptimeNanos());
        }
    }

    private static final class DelayedFrameCallback implements Runnable
    {
        private final Choreographer mChoreographer;
        private final FrameCallback mCallback;

        DelayedFrameCallback(Choreographer choreographer, FrameCallback callback)
        {
            mChoreographer = choreographer;
            mCallback = callback;
        }

        @Override
        public void run()
        {
            mChoreographer.postFrameCallback(mCallback);
        }
    }

    private static final class DelayedTypedCallback implements Runnable
    {
        private final Choreographer mChoreographer;
        private final int mType;
        private final Runnable mAction;
        private final Object mToken;

        DelayedTypedCallback(Choreographer choreographer, int type, Runnable action, Object token)
        {
            mChoreographer = choreographer;
            mType = type;
            mAction = action;
            mToken = token;
        }

        @Override
        public void run()
        {
            synchronized (mChoreographer.mPendingDelayed)
            {
                mChoreographer.mPendingDelayed.remove(this);
            }
            mChoreographer.postCallback(mType, mAction, mToken);
        }
    }
}
