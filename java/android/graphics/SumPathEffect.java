package android.graphics;

public class SumPathEffect extends PathEffect
{
    private final PathEffect mFirst;
    private final PathEffect mSecond;

    public SumPathEffect(PathEffect first, PathEffect second)
    {
        mFirst = first;
        mSecond = second;
    }

    PathEffect dashSource()
    {
        return ComposePathEffect.containsDash(mFirst) ? mFirst : mSecond;
    }
}
