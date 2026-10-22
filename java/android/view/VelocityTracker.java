package android.view;

import java.util.ArrayList;

public final class VelocityTracker
{
    private static final int MAX_SAMPLES = 20;
    private static final long HORIZON_MILLIS = 100;
    private static final long ASSUME_STOPPED_MILLIS = 40;
    private static final int AXIS_SCROLL = 26;
    private static final int ACTIVE_POINTER_NONE = -1;

    private static final ArrayList<VelocityTracker> sPool = new ArrayList<>();

    private final ArrayList<Sample> mSamples = new ArrayList<>();
    private final ArrayList<Sample> mScrollSamples = new ArrayList<>();
    private final android.util.SparseArray<float[]> mVelocities = new android.util.SparseArray<>();

    private int mActivePointerId = ACTIVE_POINTER_NONE;
    private float mScrollVelocity;
    private long mLastEventTime;

    private static final class Sample
    {
        final long time;
        final int pointerId;
        final float x;
        final float y;

        Sample(long time, int pointerId, float x, float y)
        {
            this.time = time;
            this.pointerId = pointerId;
            this.x = x;
            this.y = y;
        }
    }

    VelocityTracker()
    {
    }

    public static VelocityTracker obtain()
    {
        synchronized (sPool)
        {
            if (!sPool.isEmpty()) return sPool.remove(sPool.size() - 1);
        }

        return new VelocityTracker();
    }

    public void recycle()
    {
        clear();

        synchronized (sPool)
        {
            if (sPool.size() < 8) sPool.add(this);
        }
    }

    public boolean isAxisSupported(int axis)
    {
        return axis == MotionEvent.AXIS_X || axis == MotionEvent.AXIS_Y || axis == AXIS_SCROLL;
    }

    public void clear()
    {
        mSamples.clear();
        mScrollSamples.clear();
        mVelocities.clear();
        mActivePointerId = ACTIVE_POINTER_NONE;
        mScrollVelocity = 0;
        mLastEventTime = 0;
    }

    public void addMovement(MotionEvent event)
    {
        if (event == null) return;

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_HOVER_ENTER) clear();

        if (action == MotionEvent.ACTION_SCROLL)
        {
            mScrollSamples.add(new Sample(event.getEventTime(), 0, event.getAxisValue(AXIS_SCROLL), 0));
            while (mScrollSamples.size() > MAX_SAMPLES) mScrollSamples.remove(0);
            return;
        }

        if (action == MotionEvent.ACTION_POINTER_UP)
        {
            int liftedId = event.getPointerId(event.getActionIndex());
            removePointer(liftedId);
            if (liftedId == mActivePointerId) mActivePointerId = ACTIVE_POINTER_NONE;
            return;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
        {
            mLastEventTime = event.getEventTime();
            return;
        }

        int history = event.getHistorySize();
        for (int position = 0; position < history; position++)
        {
            long time = event.getHistoricalEventTime(position);
            for (int index = 0; index < event.getPointerCount(); index++)
            {
                mSamples.add(new Sample(time, event.getPointerId(index), event.getHistoricalX(index, position),
                                        event.getHistoricalY(index, position)));
            }
        }

        long time = event.getEventTime();
        for (int index = 0; index < event.getPointerCount(); index++)
        {
            mSamples.add(new Sample(time, event.getPointerId(index), event.getX(index), event.getY(index)));
        }

        mLastEventTime = time;

        if (mActivePointerId == ACTIVE_POINTER_NONE && event.getPointerCount() > 0) mActivePointerId = event.getPointerId(0);

        while (mSamples.size() > MAX_SAMPLES * Math.max(1, event.getPointerCount())) mSamples.remove(0);
    }

    private void removePointer(int pointerId)
    {
        for (int index = mSamples.size() - 1; index >= 0; index--)
        {
            if (mSamples.get(index).pointerId == pointerId) mSamples.remove(index);
        }
    }

    public void computeCurrentVelocity(int units)
    {
        computeCurrentVelocity(units, Float.MAX_VALUE);
    }

    public void computeCurrentVelocity(int units, float maxVelocity)
    {
        mVelocities.clear();

        java.util.HashSet<Integer> pointers = new java.util.HashSet<>();
        for (Sample sample : mSamples) pointers.add(sample.pointerId);

        for (Integer pointerId : pointers)
        {
            float[] velocity = estimate(mSamples, pointerId, units, maxVelocity);
            mVelocities.put(pointerId, velocity);
        }

        mScrollVelocity = estimateScroll(units, maxVelocity);
    }

    private float estimateScroll(int units, float maxVelocity)
    {
        if (mScrollSamples.size() < 2) return 0;

        Sample newest = mScrollSamples.get(mScrollSamples.size() - 1);
        long oldestTime = newest.time;
        double total = 0;

        for (int index = mScrollSamples.size() - 1; index >= 0; index--)
        {
            Sample sample = mScrollSamples.get(index);
            if (newest.time - sample.time > HORIZON_MILLIS) break;

            total += sample.x;
            oldestTime = sample.time;
        }

        long span = newest.time - oldestTime;
        if (span <= 0) return 0;

        return clampMagnitude((float)(total / (span / 1000.0) * (units / 1000.0)), maxVelocity);
    }

    private float[] estimate(ArrayList<Sample> samples, int pointerId, int units, float maxVelocity)
    {
        float[] result = new float[2];

        Sample newest = null;
        for (int index = samples.size() - 1; index >= 0; index--)
        {
            if (samples.get(index).pointerId != pointerId) continue;
            newest = samples.get(index);
            break;
        }

        if (newest == null) return result;
        if (mLastEventTime - newest.time > ASSUME_STOPPED_MILLIS) return result;

        double sumTime = 0;
        double sumX = 0;
        double sumY = 0;
        double sumTimeTime = 0;
        double sumTimeX = 0;
        double sumTimeY = 0;
        int count = 0;
        Sample previous = newest;

        for (int index = samples.size() - 1; index >= 0; index--)
        {
            Sample sample = samples.get(index);
            if (sample.pointerId != pointerId) continue;
            if (newest.time - sample.time > HORIZON_MILLIS) break;
            if (previous.time - sample.time > ASSUME_STOPPED_MILLIS) break;

            double time = (sample.time - newest.time) / 1000.0;
            sumTime += time;
            sumX += sample.x;
            sumY += sample.y;
            sumTimeTime += time * time;
            sumTimeX += time * sample.x;
            sumTimeY += time * sample.y;
            count++;
            previous = sample;
        }

        if (count < 2) return result;

        double denominator = count * sumTimeTime - sumTime * sumTime;
        if (Math.abs(denominator) < 1e-12) return result;

        double scale = units / 1000.0;
        double velocityX = (count * sumTimeX - sumTime * sumX) / denominator * scale;
        double velocityY = (count * sumTimeY - sumTime * sumY) / denominator * scale;

        result[0] = clampMagnitude((float)velocityX, maxVelocity);
        result[1] = clampMagnitude((float)velocityY, maxVelocity);
        return result;
    }

    private static float clampMagnitude(float value, float limit)
    {
        if (Float.isNaN(value)) return 0;
        if (value > limit) return limit;
        if (value < -limit) return -limit;

        return value;
    }

    private float velocityFor(int pointerId, int component)
    {
        float[] velocity = mVelocities.get(pointerId);
        return velocity == null ? 0 : velocity[component];
    }

    private int activePointer()
    {
        if (mActivePointerId != ACTIVE_POINTER_NONE) return mActivePointerId;
        return mSamples.isEmpty() ? 0 : mSamples.get(mSamples.size() - 1).pointerId;
    }

    public float getXVelocity()
    {
        return velocityFor(activePointer(), 0);
    }

    public float getYVelocity()
    {
        return velocityFor(activePointer(), 1);
    }

    public float getXVelocity(int pointerId)
    {
        return velocityFor(pointerId, 0);
    }

    public float getYVelocity(int pointerId)
    {
        return velocityFor(pointerId, 1);
    }

    public float getAxisVelocity(int axis, int pointerId)
    {
        if (axis == MotionEvent.AXIS_X) return velocityFor(pointerId, 0);
        if (axis == MotionEvent.AXIS_Y) return velocityFor(pointerId, 1);
        if (axis == AXIS_SCROLL) return mScrollVelocity;

        return 0;
    }

    public float getAxisVelocity(int axis)
    {
        return getAxisVelocity(axis, activePointer());
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }
}
