package android.text;

public abstract class SegmentFinder
{
    public static final int DONE = -1;

    public SegmentFinder()
    {
    }

    public abstract int previousStartBoundary(int offset);

    public abstract int previousEndBoundary(int offset);

    public abstract int nextStartBoundary(int offset);

    public abstract int nextEndBoundary(int offset);
}
