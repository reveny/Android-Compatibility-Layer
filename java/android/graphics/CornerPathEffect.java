package android.graphics;

public class CornerPathEffect extends PathEffect
{
    private final float mRadius;

    public CornerPathEffect(float radius)
    {
        mRadius = radius;
    }

    float radius()
    {
        return mRadius;
    }
}
