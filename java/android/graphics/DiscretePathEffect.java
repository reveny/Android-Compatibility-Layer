package android.graphics;

public class DiscretePathEffect extends PathEffect
{
    private final float mSegmentLength;
    private final float mDeviation;

    public DiscretePathEffect(float segmentLength, float deviation)
    {
        mSegmentLength = segmentLength;
        mDeviation = deviation;
    }

    float segmentLength()
    {
        return mSegmentLength;
    }

    float deviation()
    {
        return mDeviation;
    }
}
