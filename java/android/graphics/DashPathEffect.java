package android.graphics;

public class DashPathEffect extends PathEffect
{
    private final float[] mIntervals;
    private final float mPhase;

    public DashPathEffect(float[] intervals, float phase)
    {
        if (intervals == null || intervals.length < 2) throw new IllegalArgumentException("needs at least two intervals");

        mIntervals = intervals.clone();
        mPhase = phase;
    }

    float[] intervals()
    {
        return mIntervals;
    }

    float phase()
    {
        return mPhase;
    }
}
