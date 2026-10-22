package android.graphics;

public class ComposePathEffect extends PathEffect
{
    private final PathEffect mOuter;
    private final PathEffect mInner;

    public ComposePathEffect(PathEffect outerpe, PathEffect innerpe)
    {
        mOuter = outerpe;
        mInner = innerpe;
    }

    PathEffect dashSource()
    {
        return containsDash(mOuter) ? mOuter : mInner;
    }

    static boolean containsDash(PathEffect effect)
    {
        if (effect instanceof DashPathEffect) return true;
        if (effect instanceof ComposePathEffect) return containsDash(((ComposePathEffect) effect).dashSource());
        if (effect instanceof SumPathEffect) return containsDash(((SumPathEffect) effect).dashSource());

        return false;
    }
}
