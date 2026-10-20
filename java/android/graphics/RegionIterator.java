package android.graphics;

public class RegionIterator
{
    private final Rect[] mParts;
    private int mIndex;

    public RegionIterator(Region region)
    {
        mParts = region == null ? new Rect[0] : region.parts();
    }

    public final boolean next(Rect into)
    {
        if (into == null || mIndex >= mParts.length) return false;

        into.set(mParts[mIndex]);
        mIndex++;

        return true;
    }
}
