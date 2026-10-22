package android.graphics;

public class EmbossMaskFilter extends MaskFilter
{
    private final float[] mDirection;
    private final float mAmbient;
    private final float mSpecular;
    private final float mBlurRadius;

    public EmbossMaskFilter(float[] direction, float ambient, float specular, float blurRadius)
    {
        if (direction.length < 3) throw new ArrayIndexOutOfBoundsException();

        mDirection = direction.clone();
        mAmbient = ambient;
        mSpecular = specular;
        mBlurRadius = blurRadius;
    }

    float[] direction()
    {
        return mDirection;
    }

    float ambient()
    {
        return mAmbient;
    }

    float specular()
    {
        return mSpecular;
    }

    float blurRadius()
    {
        return mBlurRadius;
    }
}
