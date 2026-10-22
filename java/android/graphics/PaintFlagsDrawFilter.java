package android.graphics;

public class PaintFlagsDrawFilter extends DrawFilter
{
    private final int mClearBits;
    private final int mSetBits;

    public PaintFlagsDrawFilter(int clearBits, int setBits)
    {
        mClearBits = clearBits;
        mSetBits = setBits;
    }

    @Override
    Paint filter(Paint paint)
    {
        if (paint == null) return null;

        int flags = (paint.getFlags() & ~mClearBits) | mSetBits;
        if (flags == paint.getFlags()) return paint;

        Paint filtered = new Paint(paint);
        filtered.setFlags(flags);
        return filtered;
    }
}
