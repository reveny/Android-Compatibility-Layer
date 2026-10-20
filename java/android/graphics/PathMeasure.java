package android.graphics;

public class PathMeasure
{
    private final RectF mBounds = new RectF();
    private boolean mClosed;
    private boolean mHasPath;

    public PathMeasure()
    {
    }

    public PathMeasure(Path path, boolean forceClosed)
    {
        setPath(path, forceClosed);
    }

    public void setPath(Path path, boolean forceClosed)
    {
        mClosed = forceClosed;
        mHasPath = path != null && !path.isEmpty();

        if (mHasPath) path.computeBounds(mBounds, true);
        else mBounds.set(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public float getLength()
    {
        if (!mHasPath) return 0.0f;

        return 2.0f * ((mBounds.right - mBounds.left) + (mBounds.bottom - mBounds.top));
    }

    public boolean isClosed()
    {
        return mClosed;
    }

    public boolean nextContour()
    {
        return false;
    }

    public boolean getPosTan(float distance, float[] position, float[] tangent)
    {
        if (!mHasPath) return false;

        float width = mBounds.right - mBounds.left;
        float height = mBounds.bottom - mBounds.top;
        float perimeter = 2.0f * (width + height);

        float walked = perimeter <= 0.0f ? 0.0f : Math.max(0.0f, Math.min(distance, perimeter));

        float x;
        float y;
        float tangentX;
        float tangentY;

        if (walked <= width)
        {
            x = mBounds.left + walked;
            y = mBounds.top;
            tangentX = 1.0f;
            tangentY = 0.0f;
        }
        else if (walked <= width + height)
        {
            x = mBounds.right;
            y = mBounds.top + (walked - width);
            tangentX = 0.0f;
            tangentY = 1.0f;
        }
        else if (walked <= 2.0f * width + height)
        {
            x = mBounds.right - (walked - width - height);
            y = mBounds.bottom;
            tangentX = -1.0f;
            tangentY = 0.0f;
        }
        else
        {
            x = mBounds.left;
            y = mBounds.bottom - (walked - 2.0f * width - height);
            tangentX = 0.0f;
            tangentY = -1.0f;
        }

        if (position != null && position.length >= 2)
        {
            position[0] = x;
            position[1] = y;
        }

        if (tangent != null && tangent.length >= 2)
        {
            tangent[0] = tangentX;
            tangent[1] = tangentY;
        }

        return true;
    }

    public boolean getSegment(float startD, float stopD, Path into, boolean startWithMoveTo)
    {
        if (!mHasPath || into == null || stopD <= startD) return false;

        float[] start = new float[2];
        float[] stop = new float[2];

        getPosTan(startD, start, null);
        getPosTan(stopD, stop, null);

        if (startWithMoveTo) into.moveTo(start[0], start[1]);
        into.lineTo(stop[0], stop[1]);

        return true;
    }

    public boolean getMatrix(float distance, Matrix matrix, int flags)
    {
        if (!mHasPath || matrix == null) return false;

        float[] position = new float[2];
        getPosTan(distance, position, null);

        matrix.reset();
        matrix.postTranslate(position[0], position[1]);

        return true;
    }
}
