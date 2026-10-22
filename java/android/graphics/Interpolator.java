package android.graphics;

public class Interpolator
{
    private static final float[] LINEAR_BLEND = {1.0f / 3, 1.0f / 3, 2.0f / 3, 2.0f / 3};

    private int mValueCount;
    private int mFrameCount;
    private int[] mTimes;
    private float[][] mValues;
    private float[][] mBlends;
    private float mRepeat = 1;
    private boolean mMirror;

    public Interpolator(int valueCount)
    {
        this(valueCount, 2);
    }

    public Interpolator(int valueCount, int frameCount)
    {
        reset(valueCount, frameCount);
    }

    public void reset(int valueCount)
    {
        reset(valueCount, 2);
    }

    public void reset(int valueCount, int frameCount)
    {
        mValueCount = valueCount;
        mFrameCount = frameCount;
        mTimes = new int[frameCount];
        mValues = new float[frameCount][valueCount];
        mBlends = new float[frameCount][];
        mRepeat = 1;
        mMirror = false;

        for (int index = 0; index < frameCount; index++) mBlends[index] = LINEAR_BLEND;
    }

    public final int getKeyFrameCount()
    {
        return mFrameCount;
    }

    public final int getValueCount()
    {
        return mValueCount;
    }

    public void setKeyFrame(int index, int msec, float[] values)
    {
        setKeyFrame(index, msec, values, null);
    }

    public void setKeyFrame(int index, int msec, float[] values, float[] blend)
    {
        if (index < 0 || index >= mFrameCount) throw new IndexOutOfBoundsException();
        if (values.length < mValueCount) throw new ArrayStoreException();
        if (blend != null && blend.length < 4) throw new ArrayStoreException();

        mTimes[index] = msec;
        System.arraycopy(values, 0, mValues[index], 0, mValueCount);
        mBlends[index] = blend == null ? LINEAR_BLEND : blend.clone();
    }

    public void setRepeatMirror(float repeatCount, boolean mirror)
    {
        if (repeatCount < 0) return;

        mRepeat = repeatCount;
        mMirror = mirror;
    }

    public Result timeToValues(float[] values)
    {
        return timeToValues((int)android.os.SystemClock.uptimeMillis(), values);
    }

    public Result timeToValues(int msec, float[] values)
    {
        if (values != null && values.length < mValueCount) throw new ArrayStoreException();
        if (mFrameCount == 0) return Result.FREEZE_START;

        Result result = Result.NORMAL;
        int time = msec;

        if (mRepeat != 1)
        {
            int startTime = mTimes[0];
            int totalTime = mTimes[mFrameCount - 1] - startTime;
            int offsetTime = time - startTime;
            int endTime = (int)Math.floor(mRepeat * totalTime);

            if (offsetTime >= endTime)
            {
                float fraction = mRepeat - (float)Math.floor(mRepeat);
                offsetTime = fraction == 0 && mRepeat > 0 ? totalTime : (int)Math.floor(fraction * totalTime);
                result = Result.FREEZE_END;
            }
            else if (totalTime > 0)
            {
                int period = mMirror ? totalTime * 2 : totalTime;
                offsetTime = offsetTime % period;
                if (offsetTime > totalTime) offsetTime = totalTime * 2 - offsetTime;
            }

            time = offsetTime + startTime;
        }

        int index = search(time);
        boolean exact = true;

        if (index < 0)
        {
            index = ~index;

            if (index == 0)
            {
                result = Result.FREEZE_START;
            }
            else if (index == mFrameCount)
            {
                index -= 1;
                result = Result.FREEZE_END;
            }
            else
            {
                exact = false;
            }
        }

        if (values == null) return result;

        if (exact)
        {
            System.arraycopy(mValues[index], 0, values, 0, mValueCount);
            return result;
        }

        int previousTime = mTimes[index - 1];
        float fraction = (time - previousTime) / (float)(mTimes[index] - previousTime);
        float[] blend = mBlends[index - 1];
        float eased = cubic(fraction, blend[0], blend[1], blend[2], blend[3]);

        float[] previous = mValues[index - 1];
        float[] next = mValues[index];

        for (int value = 0; value < mValueCount; value++)
        {
            values[value] = previous[value] + (next[value] - previous[value]) * eased;
        }

        return result;
    }

    private int search(int time)
    {
        int low = 0;
        int high = mFrameCount - 1;

        while (low <= high)
        {
            int middle = (low + high) >>> 1;

            if (mTimes[middle] < time) low = middle + 1;
            else if (mTimes[middle] > time) high = middle - 1;
            else return middle;
        }

        return ~low;
    }

    private static float cubic(float x, float firstX, float firstY, float secondX, float secondY)
    {
        float low = 0;
        float high = 1;
        float t = x;

        for (int iteration = 0; iteration < 24; iteration++)
        {
            t = (low + high) * 0.5f;
            float estimate = bezier(t, firstX, secondX);

            if (estimate < x) low = t;
            else high = t;
        }

        return bezier(t, firstY, secondY);
    }

    private static float bezier(float t, float first, float second)
    {
        float inverse = 1 - t;
        return 3 * inverse * inverse * t * first + 3 * inverse * t * t * second + t * t * t;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    public enum Result
    {
        NORMAL,
        FREEZE_START,
        FREEZE_END
    }
}
