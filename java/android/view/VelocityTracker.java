package android.view;

import java.util.ArrayList;

public final class VelocityTracker
{
    private static final int MAX_SAMPLES = 20;
    private static final long HORIZON_MILLIS = 200;

    private static final ArrayList<VelocityTracker> sPool = new ArrayList<>();

    private final ArrayList<Sample> mSamples = new ArrayList<>();

    private float mVelocityX;
    private float mVelocityY;

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

    private VelocityTracker()
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

    public void clear()
    {
        mSamples.clear();
        mVelocityX = 0;
        mVelocityY = 0;
    }

    public void addMovement(MotionEvent event)
    {
        if (event == null) return;

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) clear();

        long time = event.getEventTime();

        for (int index = 0; index < event.getPointerCount(); index++)
        {
            mSamples.add(new Sample(time, event.getPointerId(index), event.getX(index), event.getY(index)));
        }

        while (mSamples.size() > MAX_SAMPLES) mSamples.remove(0);
    }

    public void computeCurrentVelocity(int units)
    {
        computeCurrentVelocity(units, Float.MAX_VALUE);
    }

    public void computeCurrentVelocity(int units, float maxVelocity)
    {
        mVelocityX = 0;
        mVelocityY = 0;

        if (mSamples.size() < 2) return;

        int pointerId = mSamples.get(mSamples.size() - 1).pointerId;
        Sample newest = null;
        Sample oldest = null;

        for (int index = mSamples.size() - 1; index >= 0; index--)
        {
            Sample sample = mSamples.get(index);
            if (sample.pointerId != pointerId) continue;

            if (newest == null)
            {
                newest = sample;
                oldest = sample;
                continue;
            }

            if (newest.time - sample.time > HORIZON_MILLIS) break;

            oldest = sample;
        }

        if (newest == null || oldest == null || newest == oldest) return;

        float seconds = (newest.time - oldest.time) / 1000.0f;
        if (seconds <= 0) return;

        mVelocityX = clampMagnitude((newest.x - oldest.x) / seconds * (units / 1000.0f), maxVelocity);
        mVelocityY = clampMagnitude((newest.y - oldest.y) / seconds * (units / 1000.0f), maxVelocity);
    }

    private static float clampMagnitude(float value, float limit)
    {
        if (value > limit) return limit;
        if (value < -limit) return -limit;

        return value;
    }

    public float getXVelocity()
    {
        return mVelocityX;
    }

    public float getYVelocity()
    {
        return mVelocityY;
    }

    public float getXVelocity(int pointerId)
    {
        return mVelocityX;
    }

    public float getYVelocity(int pointerId)
    {
        return mVelocityY;
    }
}
