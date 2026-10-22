package android.graphics;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PathIterator implements Iterator<PathIterator.Segment>
{
    public static final int VERB_MOVE = 0;
    public static final int VERB_LINE = 1;
    public static final int VERB_QUAD = 2;
    public static final int VERB_CONIC = 3;
    public static final int VERB_CUBIC = 4;
    public static final int VERB_CLOSE = 5;
    public static final int VERB_DONE = 6;

    private static final int POINT_ARRAY_SIZE = 8;

    private final Path mPath;
    private final int mGenerationId;
    private final byte[] mVerbs;
    private final float[] mCoordinates;
    private int mVerbIndex;
    private int mCoordinateIndex;
    private float mCurrentX;
    private float mCurrentY;
    private float mStartX;
    private float mStartY;

    PathIterator()
    {
        this(new Path());
    }

    PathIterator(Path path)
    {
        mPath = path;
        mGenerationId = path.getGenerationId();
        mVerbs = path.verbs();
        mCoordinates = path.coordinates();
    }

    public int next(float[] points, int offset)
    {
        if (points.length < offset + POINT_ARRAY_SIZE) throw new ArrayIndexOutOfBoundsException("points must hold at least 8 entries");
        checkUnmodified();
        if (mVerbIndex >= mVerbs.length) return VERB_DONE;

        int verb = mVerbs[mVerbIndex++];

        switch (verb)
        {
            case Path.VERB_MOVE:
                mCurrentX = mCoordinates[mCoordinateIndex++];
                mCurrentY = mCoordinates[mCoordinateIndex++];
                mStartX = mCurrentX;
                mStartY = mCurrentY;
                points[offset] = mCurrentX;
                points[offset + 1] = mCurrentY;
                return VERB_MOVE;
            case Path.VERB_LINE:
                return emit(points, offset, 1, VERB_LINE);
            case Path.VERB_QUAD:
                return emit(points, offset, 2, VERB_QUAD);
            case Path.VERB_CUBIC:
                return emit(points, offset, 3, VERB_CUBIC);
            default:
                points[offset] = mCurrentX;
                points[offset + 1] = mCurrentY;
                points[offset + 2] = mStartX;
                points[offset + 3] = mStartY;
                mCurrentX = mStartX;
                mCurrentY = mStartY;
                return VERB_CLOSE;
        }
    }

    private void checkUnmodified()
    {
        if (mPath.getGenerationId() != mGenerationId) throw new ConcurrentModificationException("Iterator cannot be used on modified Path");
    }

    private int emit(float[] points, int offset, int count, int verb)
    {
        points[offset] = mCurrentX;
        points[offset + 1] = mCurrentY;

        for (int point = 0; point < count; point++)
        {
            points[offset + 2 + point * 2] = mCoordinates[mCoordinateIndex++];
            points[offset + 3 + point * 2] = mCoordinates[mCoordinateIndex++];
        }

        mCurrentX = points[offset + count * 2];
        mCurrentY = points[offset + count * 2 + 1];
        return verb;
    }

    @Override
    public boolean hasNext()
    {
        return mVerbIndex < mVerbs.length;
    }

    public int peek()
    {
        checkUnmodified();
        if (mVerbIndex >= mVerbs.length) return VERB_DONE;

        switch (mVerbs[mVerbIndex])
        {
            case Path.VERB_MOVE:
                return VERB_MOVE;
            case Path.VERB_LINE:
                return VERB_LINE;
            case Path.VERB_QUAD:
                return VERB_QUAD;
            case Path.VERB_CUBIC:
                return VERB_CUBIC;
            default:
                return VERB_CLOSE;
        }
    }

    @Override
    public Segment next()
    {
        float[] points = new float[POINT_ARRAY_SIZE];
        int verb = next(points, 0);

        if (verb == VERB_DONE) throw new NoSuchElementException("No more path segments to iterate");

        return new Segment(verb, points, 0.0f);
    }

    public static class Segment
    {
        private final int mVerb;
        private final float[] mPoints;
        private final float mConicWeight;

        Segment()
        {
            this(VERB_DONE, new float[POINT_ARRAY_SIZE], 0.0f);
        }

        Segment(int verb, float[] points, float conicWeight)
        {
            mVerb = verb;
            mPoints = points;
            mConicWeight = conicWeight;
        }

        public int getVerb()
        {
            return mVerb;
        }

        public float[] getPoints()
        {
            return mPoints;
        }

        public float getConicWeight()
        {
            return mConicWeight;
        }
    }
}
