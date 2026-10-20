package android.graphics;

public class Path
{
    private final float[] mBoundsScratch = new float[4];
    private final float[] mMatrixScratch = new float[9];

    private long mNativePath;
    private FillType mFillType = FillType.WINDING;

    public Path()
    {
        mNativePath = nativeCreate();
    }

    public Path(Path source)
    {
        mNativePath = nativeCreate();
        set(source);
    }

    public long nativeHandle()
    {
        return mNativePath;
    }

    public void set(Path source)
    {
        reset();
        if (source == null) return;

        mFillType = source.mFillType;
        addPath(source);
    }

    public void reset()
    {
        nativeReset(mNativePath);
    }

    public void rewind()
    {
        reset();
    }

    public boolean isEmpty()
    {
        return nativeIsEmpty(mNativePath);
    }

    public FillType getFillType()
    {
        return mFillType;
    }

    public void setFillType(FillType type)
    {
        mFillType = type == null ? FillType.WINDING : type;
    }

    public boolean isInverseFillType()
    {
        return mFillType == FillType.INVERSE_WINDING || mFillType == FillType.INVERSE_EVEN_ODD;
    }

    public void moveTo(float x, float y)
    {
        nativeMoveTo(mNativePath, x, y);
    }

    public void lineTo(float x, float y)
    {
        nativeLineTo(mNativePath, x, y);
    }

    public void quadTo(float controlX, float controlY, float x, float y)
    {
        nativeQuadTo(mNativePath, controlX, controlY, x, y);
    }

    public void cubicTo(float firstX, float firstY, float secondX, float secondY, float x, float y)
    {
        nativeCubicTo(mNativePath, firstX, firstY, secondX, secondY, x, y);
    }

    public void rMoveTo(float dx, float dy)
    {
        float[] current = currentPoint();
        moveTo(current[0] + dx, current[1] + dy);
    }

    public void rLineTo(float dx, float dy)
    {
        float[] current = currentPoint();
        lineTo(current[0] + dx, current[1] + dy);
    }

    public void rQuadTo(float controlDx, float controlDy, float dx, float dy)
    {
        float[] current = currentPoint();
        quadTo(current[0] + controlDx, current[1] + controlDy, current[0] + dx, current[1] + dy);
    }

    public void rCubicTo(float firstDx, float firstDy, float secondDx, float secondDy, float dx, float dy)
    {
        float[] current = currentPoint();
        cubicTo(current[0] + firstDx, current[1] + firstDy, current[0] + secondDx, current[1] + secondDy,
                current[0] + dx, current[1] + dy);
    }

    private float[] currentPoint()
    {
        float[] point = new float[2];
        nativeCurrentPoint(mNativePath, point);

        return point;
    }

    public void close()
    {
        nativeClose(mNativePath);
    }

    public void addRect(float left, float top, float right, float bottom, Direction direction)
    {
        nativeAddRect(mNativePath, left, top, right, bottom);
    }

    public void addRect(RectF rect, Direction direction)
    {
        if (rect != null) addRect(rect.left, rect.top, rect.right, rect.bottom, direction);
    }

    public void addRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY,
                             Direction direction)
    {
        nativeAddRoundRect(mNativePath, left, top, right, bottom, radiusX, radiusY);
    }

    public void addRoundRect(RectF rect, float radiusX, float radiusY, Direction direction)
    {
        if (rect != null) addRoundRect(rect.left, rect.top, rect.right, rect.bottom, radiusX, radiusY, direction);
    }

    public void addRoundRect(RectF rect, float[] radii, Direction direction)
    {
        if (rect == null) return;

        float radiusX = radii != null && radii.length > 0 ? radii[0] : 0;
        float radiusY = radii != null && radii.length > 1 ? radii[1] : radiusX;

        addRoundRect(rect, radiusX, radiusY, direction);
    }

    public void addOval(float left, float top, float right, float bottom, Direction direction)
    {
        nativeAddOval(mNativePath, left, top, right, bottom);
    }

    public void addOval(RectF rect, Direction direction)
    {
        if (rect != null) addOval(rect.left, rect.top, rect.right, rect.bottom, direction);
    }

    public void addCircle(float x, float y, float radius, Direction direction)
    {
        addOval(x - radius, y - radius, x + radius, y + radius, direction);
    }

    public void addArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle)
    {
        nativeAddArc(mNativePath, left, top, right, bottom, startAngle, sweepAngle, true);
    }

    public void addArc(RectF oval, float startAngle, float sweepAngle)
    {
        if (oval != null) addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle);
    }

    public void arcTo(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                      boolean forceMoveTo)
    {
        nativeAddArc(mNativePath, left, top, right, bottom, startAngle, sweepAngle, forceMoveTo);
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle)
    {
        arcTo(oval, startAngle, sweepAngle, false);
    }

    public void arcTo(RectF oval, float startAngle, float sweepAngle, boolean forceMoveTo)
    {
        if (oval != null) arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, forceMoveTo);
    }

    public void addPath(Path source)
    {
        addPath(source, null);
    }

    public void addPath(Path source, float dx, float dy)
    {
        Matrix move = new Matrix();
        move.setTranslate(dx, dy);

        addPath(source, move);
    }

    public void addPath(Path source, Matrix matrix)
    {
        if (source == null) return;

        if (matrix == null) new Matrix().getValues(mMatrixScratch);
        else matrix.getValues(mMatrixScratch);

        nativeAddPath(mNativePath, source.mNativePath, mMatrixScratch);
    }

    public void transform(Matrix matrix)
    {
        if (matrix == null) return;

        matrix.getValues(mMatrixScratch);
        nativeTransform(mNativePath, mMatrixScratch);
    }

    public void transform(Matrix matrix, Path destination)
    {
        if (destination == null)
        {
            transform(matrix);
            return;
        }

        destination.reset();
        destination.addPath(this, matrix);
    }

    public void offset(float dx, float dy)
    {
        Matrix move = new Matrix();
        move.setTranslate(dx, dy);

        transform(move);
    }

    public void offset(float dx, float dy, Path destination)
    {
        if (destination == null)
        {
            offset(dx, dy);
            return;
        }

        destination.reset();
        destination.addPath(this, dx, dy);
    }

    public void setLastPoint(float x, float y)
    {
        moveTo(x, y);
    }

    public void addRoundRect(float left, float top, float right, float bottom, float[] radii, Direction direction)
    {
        float radiusX = radii != null && radii.length > 0 ? radii[0] : 0;
        float radiusY = radii != null && radii.length > 1 ? radii[1] : radiusX;

        addRoundRect(left, top, right, bottom, radiusX, radiusY, direction);
    }

    public void computeBounds(RectF into, boolean exact)
    {
        if (into == null) return;

        if (!nativeComputeBounds(mNativePath, mBoundsScratch))
        {
            into.setEmpty();
            return;
        }

        into.set(mBoundsScratch[0], mBoundsScratch[1], mBoundsScratch[2], mBoundsScratch[3]);
    }

    public void computeBounds(RectF into)
    {
        computeBounds(into, true);
    }

    public void dispose()
    {
        if (mNativePath == 0) return;

        nativeDestroy(mNativePath);
        mNativePath = 0;
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            dispose();
        }
        finally
        {
            super.finalize();
        }
    }

    private static native long nativeCreate();

    private static native void nativeDestroy(long path);

    private static native void nativeReset(long path);

    private static native boolean nativeIsEmpty(long path);

    private static native void nativeMoveTo(long path, float x, float y);

    private static native void nativeLineTo(long path, float x, float y);

    private static native void nativeQuadTo(long path, float controlX, float controlY, float x, float y);

    private static native void nativeCubicTo(long path, float firstX, float firstY, float secondX, float secondY, float x,
                                             float y);

    private static native void nativeClose(long path);

    private static native void nativeAddRect(long path, float left, float top, float right, float bottom);

    private static native void nativeAddRoundRect(long path, float left, float top, float right, float bottom,
                                                  float radiusX, float radiusY);

    private static native void nativeAddOval(long path, float left, float top, float right, float bottom);

    private static native void nativeAddArc(long path, float left, float top, float right, float bottom, float start,
                                            float sweep, boolean moveToStart);

    private static native void nativeAddPath(long path, long other, float[] matrix);

    private static native void nativeTransform(long path, float[] matrix);

    private static native boolean nativeComputeBounds(long path, float[] into);

    private static native void nativeCurrentPoint(long path, float[] into);

    public enum FillType
    {
        WINDING,
        EVEN_ODD,
        INVERSE_WINDING,
        INVERSE_EVEN_ODD
    }

    public enum Direction
    {
        CW,
        CCW
    }

    public enum Op
    {
        DIFFERENCE,
        INTERSECT,
        UNION,
        XOR,
        REVERSE_DIFFERENCE
    }

    public boolean op(Path other, Op operation)
    {
        return op(this, other, operation);
    }

    public boolean op(Path first, Path second, Op operation)
    {
        if (first == null || second == null) return false;

        RectF left = new RectF();
        RectF right = new RectF();

        first.computeBounds(left, true);
        second.computeBounds(right, true);

        RectF result = new RectF();

        if (operation == Op.INTERSECT)
        {
            result.set(Math.max(left.left, right.left), Math.max(left.top, right.top), Math.min(left.right, right.right),
                       Math.min(left.bottom, right.bottom));

            if (result.right <= result.left || result.bottom <= result.top)
            {
                reset();
                return true;
            }
        }
        else if (operation == Op.REVERSE_DIFFERENCE)
        {
            result.set(right);
        }
        else if (operation == Op.DIFFERENCE)
        {
            result.set(left);
        }
        else
        {
            result.set(Math.min(left.left, right.left), Math.min(left.top, right.top), Math.max(left.right, right.right),
                       Math.max(left.bottom, right.bottom));
        }

        reset();

        if (result.right > result.left && result.bottom > result.top) addRect(result, Direction.CW);

        return true;
    }
}
