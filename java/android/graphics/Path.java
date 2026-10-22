package android.graphics;

public class Path
{
    static final int VERB_MOVE = 0;
    static final int VERB_LINE = 1;
    static final int VERB_QUAD = 2;
    static final int VERB_CUBIC = 3;
    static final int VERB_CLOSE = 4;

    private static final float CIRCLE_KAPPA = 0.5522847498f;
    private static final int MAXIMUM_FLATTEN_STEPS = 1024;

    private final float[] mBoundsScratch = new float[4];
    private final float[] mMatrixScratch = new float[9];

    private long mNativePath;
    private FillType mFillType = FillType.WINDING;

    private byte[] mVerbs = new byte[16];
    private int mVerbCount;
    private float[] mCoordinates = new float[32];
    private int mCoordinateCount;
    private float mContourStartX;
    private float mContourStartY;
    private boolean mContourOpen;
    private int mGenerationId;

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
        if (source == this) return;

        reset();
        if (source == null) return;

        mFillType = source.mFillType;
        addPath(source);
    }

    public void reset()
    {
        nativeReset(mNativePath);
        mVerbCount = 0;
        mCoordinateCount = 0;
        mContourOpen = false;
        mContourStartX = 0;
        mContourStartY = 0;
        mGenerationId++;
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
        mGenerationId++;
    }

    public boolean isInverseFillType()
    {
        return mFillType == FillType.INVERSE_WINDING || mFillType == FillType.INVERSE_EVEN_ODD;
    }

    public void toggleInverseFillType()
    {
        switch (mFillType)
        {
            case WINDING:
                setFillType(FillType.INVERSE_WINDING);
                break;
            case EVEN_ODD:
                setFillType(FillType.INVERSE_EVEN_ODD);
                break;
            case INVERSE_WINDING:
                setFillType(FillType.WINDING);
                break;
            default:
                setFillType(FillType.EVEN_ODD);
                break;
        }
    }

    public void incReserve(int extraPointCount)
    {
        if (extraPointCount > 0) ensureCoordinates(extraPointCount * 2);
    }

    public int getGenerationId()
    {
        return mGenerationId;
    }

    public PathIterator getPathIterator()
    {
        return new PathIterator(this);
    }

    public void moveTo(float x, float y)
    {
        nativeMoveTo(mNativePath, x, y);
        recordMove(x, y);
    }

    public void lineTo(float x, float y)
    {
        nativeLineTo(mNativePath, x, y);
        recordLine(x, y);
    }

    public void quadTo(float controlX, float controlY, float x, float y)
    {
        nativeQuadTo(mNativePath, controlX, controlY, x, y);
        recordQuad(controlX, controlY, x, y);
    }

    public void conicTo(float controlX, float controlY, float x, float y, float weight)
    {
        if (weight == 1.0f || !(weight > 0.0f) || Float.isInfinite(weight))
        {
            if (weight > 0.0f && !Float.isInfinite(weight)) quadTo(controlX, controlY, x, y);
            else lineTo(x, y);
            return;
        }

        float[] start = lastPoint();
        float scale = 2.0f * weight / (1.0f + weight);
        float firstX = start[0] + (controlX - start[0]) * scale;
        float firstY = start[1] + (controlY - start[1]) * scale;
        float secondX = x + (controlX - x) * scale;
        float secondY = y + (controlY - y) * scale;

        cubicTo(firstX, firstY, secondX, secondY, x, y);
    }

    public void cubicTo(float firstX, float firstY, float secondX, float secondY, float x, float y)
    {
        nativeCubicTo(mNativePath, firstX, firstY, secondX, secondY, x, y);
        recordCubic(firstX, firstY, secondX, secondY, x, y);
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

    public void rConicTo(float controlDx, float controlDy, float dx, float dy, float weight)
    {
        float[] current = currentPoint();
        conicTo(current[0] + controlDx, current[1] + controlDy, current[0] + dx, current[1] + dy, weight);
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
        recordClose();
    }

    public void addRect(float left, float top, float right, float bottom, Direction direction)
    {
        nativeAddRect(mNativePath, left, top, right, bottom);

        recordMove(left, top);

        if (direction == Direction.CCW)
        {
            recordLine(left, bottom);
            recordLine(right, bottom);
            recordLine(right, top);
        }
        else
        {
            recordLine(right, top);
            recordLine(right, bottom);
            recordLine(left, bottom);
        }

        recordClose();
    }

    public void addRect(RectF rect, Direction direction)
    {
        if (rect != null) addRect(rect.left, rect.top, rect.right, rect.bottom, direction);
    }

    public void addRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY,
                             Direction direction)
    {
        nativeAddRoundRect(mNativePath, left, top, right, bottom, radiusX, radiusY);
        recordRoundRect(left, top, right, bottom, radiusX, radiusY);
    }

    public void addRoundRect(RectF rect, float radiusX, float radiusY, Direction direction)
    {
        if (rect != null) addRoundRect(rect.left, rect.top, rect.right, rect.bottom, radiusX, radiusY, direction);
    }

    public void addRoundRect(RectF rect, float[] radii, Direction direction)
    {
        if (rect == null) return;

        addRoundRect(rect.left, rect.top, rect.right, rect.bottom, radii, direction);
    }

    public void addOval(float left, float top, float right, float bottom, Direction direction)
    {
        nativeAddOval(mNativePath, left, top, right, bottom);
        recordOval(left, top, right, bottom);
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
        recordArc(left, top, right, bottom, startAngle, sweepAngle, true);
    }

    public void addArc(RectF oval, float startAngle, float sweepAngle)
    {
        if (oval != null) addArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle);
    }

    public void arcTo(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                      boolean forceMoveTo)
    {
        nativeAddArc(mNativePath, left, top, right, bottom, startAngle, sweepAngle, forceMoveTo);
        recordArc(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo);
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

        byte[] verbs = java.util.Arrays.copyOf(source.mVerbs, source.mVerbCount);
        float[] coordinates = java.util.Arrays.copyOf(source.mCoordinates, source.mCoordinateCount);

        if (matrix != null && !matrix.isIdentity() && coordinates.length > 0) matrix.mapPoints(coordinates);

        appendRecord(verbs, coordinates);
    }

    public void transform(Matrix matrix)
    {
        if (matrix == null) return;

        matrix.getValues(mMatrixScratch);
        nativeTransform(mNativePath, mMatrixScratch);

        if (mCoordinateCount > 0)
        {
            float[] mapped = java.util.Arrays.copyOf(mCoordinates, mCoordinateCount);
            matrix.mapPoints(mapped);
            System.arraycopy(mapped, 0, mCoordinates, 0, mCoordinateCount);
        }

        float[] start = {mContourStartX, mContourStartY};
        matrix.mapPoints(start);
        mContourStartX = start[0];
        mContourStartY = start[1];
        mGenerationId++;
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

    public boolean isRect(RectF rect)
    {
        if (mVerbCount < 4 || mVerbs[0] != VERB_MOVE) return false;

        float[] corners = new float[10];
        int cornerCount = 1;
        corners[0] = mCoordinates[0];
        corners[1] = mCoordinates[1];

        int coordinate = 2;
        int verb = 1;

        for (; verb < mVerbCount; verb++)
        {
            if (mVerbs[verb] == VERB_CLOSE) break;
            if (mVerbs[verb] != VERB_LINE || cornerCount >= 5) return false;

            corners[cornerCount * 2] = mCoordinates[coordinate];
            corners[cornerCount * 2 + 1] = mCoordinates[coordinate + 1];
            cornerCount++;
            coordinate += 2;
        }

        if (verb < mVerbCount - 1) return false;

        if (cornerCount == 5)
        {
            if (corners[8] != corners[0] || corners[9] != corners[1]) return false;
            cornerCount = 4;
        }

        if (cornerCount != 4) return false;

        for (int index = 0; index < 4; index++)
        {
            int next = (index + 1) % 4;
            boolean horizontal = corners[index * 2 + 1] == corners[next * 2 + 1];
            boolean vertical = corners[index * 2] == corners[next * 2];

            if (horizontal == vertical) return false;

            int after = (index + 2) % 4;
            boolean nextHorizontal = corners[next * 2 + 1] == corners[after * 2 + 1];
            if (nextHorizontal == horizontal) return false;
        }

        if (rect != null)
        {
            float left = Math.min(Math.min(corners[0], corners[2]), Math.min(corners[4], corners[6]));
            float top = Math.min(Math.min(corners[1], corners[3]), Math.min(corners[5], corners[7]));
            float right = Math.max(Math.max(corners[0], corners[2]), Math.max(corners[4], corners[6]));
            float bottom = Math.max(Math.max(corners[1], corners[3]), Math.max(corners[5], corners[7]));
            rect.set(left, top, right, bottom);
        }

        return true;
    }

    public boolean isConvex()
    {
        if (mVerbCount == 0) return true;

        int moves = 0;
        for (int index = 0; index < mVerbCount; index++)
        {
            if (mVerbs[index] == VERB_MOVE && index + 1 < mVerbCount && mVerbs[index + 1] != VERB_MOVE) moves++;
        }

        if (moves > 1) return false;

        float[] points = new float[mCoordinateCount];
        int pointCount = 0;

        for (int index = 0; index + 1 < mCoordinateCount; index += 2)
        {
            float x = mCoordinates[index];
            float y = mCoordinates[index + 1];

            if (pointCount > 0 && points[pointCount * 2 - 2] == x && points[pointCount * 2 - 1] == y) continue;

            points[pointCount * 2] = x;
            points[pointCount * 2 + 1] = y;
            pointCount++;
        }

        if (pointCount > 1 && points[0] == points[pointCount * 2 - 2] && points[1] == points[pointCount * 2 - 1]) pointCount--;
        if (pointCount < 3) return true;

        int sign = 0;
        double turning = 0;

        for (int index = 0; index < pointCount; index++)
        {
            int middle = (index + 1) % pointCount;
            int last = (index + 2) % pointCount;

            float firstX = points[middle * 2] - points[index * 2];
            float firstY = points[middle * 2 + 1] - points[index * 2 + 1];
            float secondX = points[last * 2] - points[middle * 2];
            float secondY = points[last * 2 + 1] - points[middle * 2 + 1];

            float cross = firstX * secondY - firstY * secondX;
            float dot = firstX * secondX + firstY * secondY;

            if (cross != 0)
            {
                int current = cross > 0 ? 1 : -1;
                if (sign != 0 && current != sign) return false;
                sign = current;
            }

            turning += Math.atan2(cross, dot);
        }

        return Math.abs(Math.abs(turning) - 2.0 * Math.PI) < 0.01 || sign == 0;
    }

    public float[] approximate(float acceptableError)
    {
        if (acceptableError < 0) throw new IllegalArgumentException("AcceptableError must be greater than or equal to 0");

        float tolerance = Math.max(acceptableError, 0.0001f);

        float[] flat = new float[64];
        boolean[] jumps = new boolean[32];
        int flatCount = 0;

        int coordinate = 0;
        float currentX = 0;
        float currentY = 0;
        float startX = 0;
        float startY = 0;

        for (int verb = 0; verb < mVerbCount; verb++)
        {
            int kind = mVerbs[verb];

            if (kind == VERB_MOVE)
            {
                currentX = mCoordinates[coordinate];
                currentY = mCoordinates[coordinate + 1];
                startX = currentX;
                startY = currentY;
                coordinate += 2;

                if (flatCount * 2 + 2 > flat.length)
                {
                    flat = java.util.Arrays.copyOf(flat, flat.length * 2);
                    jumps = java.util.Arrays.copyOf(jumps, jumps.length * 2);
                }

                flat[flatCount * 2] = currentX;
                flat[flatCount * 2 + 1] = currentY;
                jumps[flatCount] = true;
                flatCount++;
                continue;
            }

            float[] segment;

            if (kind == VERB_LINE)
            {
                segment = new float[] {mCoordinates[coordinate], mCoordinates[coordinate + 1]};
                coordinate += 2;
            }
            else if (kind == VERB_QUAD)
            {
                segment = flattenQuad(currentX, currentY, mCoordinates[coordinate], mCoordinates[coordinate + 1],
                                      mCoordinates[coordinate + 2], mCoordinates[coordinate + 3], tolerance);
                coordinate += 4;
            }
            else if (kind == VERB_CUBIC)
            {
                segment = flattenCubic(currentX, currentY, mCoordinates[coordinate], mCoordinates[coordinate + 1],
                                       mCoordinates[coordinate + 2], mCoordinates[coordinate + 3], mCoordinates[coordinate + 4],
                                       mCoordinates[coordinate + 5], tolerance);
                coordinate += 6;
            }
            else
            {
                if (currentX == startX && currentY == startY) continue;
                segment = new float[] {startX, startY};
            }

            if (flatCount == 0)
            {
                flat[0] = currentX;
                flat[1] = currentY;
                jumps[0] = true;
                flatCount = 1;
            }

            int added = segment.length / 2;
            while ((flatCount + added) * 2 > flat.length)
            {
                flat = java.util.Arrays.copyOf(flat, flat.length * 2);
                jumps = java.util.Arrays.copyOf(jumps, jumps.length * 2);
            }

            System.arraycopy(segment, 0, flat, flatCount * 2, segment.length);
            flatCount += added;

            currentX = segment[segment.length - 2];
            currentY = segment[segment.length - 1];
        }

        if (flatCount == 0) return new float[] {0, 0, 0, 1, 0, 0};

        float[] distances = new float[flatCount];
        float total = 0;

        for (int index = 1; index < flatCount; index++)
        {
            if (!jumps[index])
            {
                float dx = flat[index * 2] - flat[index * 2 - 2];
                float dy = flat[index * 2 + 1] - flat[index * 2 - 1];
                total += (float) Math.sqrt(dx * dx + dy * dy);
            }

            distances[index] = total;
        }

        int outputCount = flatCount == 1 ? 2 : flatCount;
        float[] result = new float[outputCount * 3];

        for (int index = 0; index < outputCount; index++)
        {
            int source = Math.min(index, flatCount - 1);
            float fraction = total > 0 ? distances[source] / total : (index == outputCount - 1 ? 1 : 0);

            result[index * 3] = index == outputCount - 1 ? 1 : fraction;
            result[index * 3 + 1] = flat[source * 2];
            result[index * 3 + 2] = flat[source * 2 + 1];
        }

        return result;
    }

    public boolean isInterpolatable(Path otherPath)
    {
        if (otherPath == null || otherPath.mVerbCount != mVerbCount || otherPath.mCoordinateCount != mCoordinateCount) return false;

        for (int index = 0; index < mVerbCount; index++)
        {
            if (mVerbs[index] != otherPath.mVerbs[index]) return false;
        }

        return true;
    }

    public boolean interpolate(Path otherPath, float t, Path interpolatedPath)
    {
        if (interpolatedPath == null || !isInterpolatable(otherPath)) return false;

        float[] blended = new float[mCoordinateCount];
        for (int index = 0; index < mCoordinateCount; index++)
        {
            blended[index] = otherPath.mCoordinates[index] + (mCoordinates[index] - otherPath.mCoordinates[index]) * t;
        }

        byte[] verbs = java.util.Arrays.copyOf(mVerbs, mVerbCount);

        interpolatedPath.reset();
        interpolatedPath.setFillType(mFillType);
        interpolatedPath.replay(verbs, blended);

        return true;
    }

    int verbCount()
    {
        return mVerbCount;
    }

    byte[] verbs()
    {
        return java.util.Arrays.copyOf(mVerbs, mVerbCount);
    }

    float[] coordinates()
    {
        return java.util.Arrays.copyOf(mCoordinates, mCoordinateCount);
    }

    private void replay(byte[] verbs, float[] coordinates)
    {
        int coordinate = 0;

        for (byte verb : verbs)
        {
            switch (verb)
            {
                case VERB_MOVE:
                    moveTo(coordinates[coordinate], coordinates[coordinate + 1]);
                    coordinate += 2;
                    break;
                case VERB_LINE:
                    lineTo(coordinates[coordinate], coordinates[coordinate + 1]);
                    coordinate += 2;
                    break;
                case VERB_QUAD:
                    quadTo(coordinates[coordinate], coordinates[coordinate + 1], coordinates[coordinate + 2],
                           coordinates[coordinate + 3]);
                    coordinate += 4;
                    break;
                case VERB_CUBIC:
                    cubicTo(coordinates[coordinate], coordinates[coordinate + 1], coordinates[coordinate + 2],
                            coordinates[coordinate + 3], coordinates[coordinate + 4], coordinates[coordinate + 5]);
                    coordinate += 6;
                    break;
                default:
                    close();
                    break;
            }
        }
    }

    private static int flattenSteps(float deviation, float tolerance)
    {
        int steps = (int) Math.ceil(Math.sqrt(deviation / tolerance));
        return Math.max(1, Math.min(MAXIMUM_FLATTEN_STEPS, steps));
    }

    private static float[] flattenQuad(float startX, float startY, float controlX, float controlY, float endX, float endY,
                                       float tolerance)
    {
        float deviationX = startX - 2 * controlX + endX;
        float deviationY = startY - 2 * controlY + endY;
        int steps = flattenSteps((float) Math.sqrt(deviationX * deviationX + deviationY * deviationY) * 0.25f, tolerance);

        float[] points = new float[steps * 2];
        for (int step = 1; step <= steps; step++)
        {
            float t = step / (float) steps;
            float inverse = 1 - t;
            points[(step - 1) * 2] = inverse * inverse * startX + 2 * inverse * t * controlX + t * t * endX;
            points[(step - 1) * 2 + 1] = inverse * inverse * startY + 2 * inverse * t * controlY + t * t * endY;
        }

        return points;
    }

    private static float[] flattenCubic(float startX, float startY, float firstX, float firstY, float secondX, float secondY,
                                        float endX, float endY, float tolerance)
    {
        float firstDeviationX = startX - 2 * firstX + secondX;
        float firstDeviationY = startY - 2 * firstY + secondY;
        float secondDeviationX = firstX - 2 * secondX + endX;
        float secondDeviationY = firstY - 2 * secondY + endY;
        float deviation = (float) Math.max(Math.sqrt(firstDeviationX * firstDeviationX + firstDeviationY * firstDeviationY),
                                           Math.sqrt(secondDeviationX * secondDeviationX + secondDeviationY * secondDeviationY));
        int steps = flattenSteps(deviation * 0.75f, tolerance);

        float[] points = new float[steps * 2];
        for (int step = 1; step <= steps; step++)
        {
            float t = step / (float) steps;
            float inverse = 1 - t;
            float a = inverse * inverse * inverse;
            float b = 3 * inverse * inverse * t;
            float c = 3 * inverse * t * t;
            float d = t * t * t;
            points[(step - 1) * 2] = a * startX + b * firstX + c * secondX + d * endX;
            points[(step - 1) * 2 + 1] = a * startY + b * firstY + c * secondY + d * endY;
        }

        return points;
    }

    private float[] lastPoint()
    {
        if (mCoordinateCount < 2) return new float[] {0, 0};

        if (mVerbCount > 0 && mVerbs[mVerbCount - 1] == VERB_CLOSE) return new float[] {mContourStartX, mContourStartY};

        return new float[] {mCoordinates[mCoordinateCount - 2], mCoordinates[mCoordinateCount - 1]};
    }

    private void ensureVerbs(int extra)
    {
        if (mVerbCount + extra > mVerbs.length) mVerbs = java.util.Arrays.copyOf(mVerbs, Math.max(mVerbs.length * 2, mVerbCount + extra));
    }

    private void ensureCoordinates(int extra)
    {
        if (mCoordinateCount + extra > mCoordinates.length)
        {
            mCoordinates = java.util.Arrays.copyOf(mCoordinates, Math.max(mCoordinates.length * 2, mCoordinateCount + extra));
        }
    }

    private void pushVerb(int verb)
    {
        ensureVerbs(1);
        mVerbs[mVerbCount++] = (byte) verb;
        mGenerationId++;
    }

    private void pushPoint(float x, float y)
    {
        ensureCoordinates(2);
        mCoordinates[mCoordinateCount++] = x;
        mCoordinates[mCoordinateCount++] = y;
    }

    private void ensureContour()
    {
        if (mContourOpen) return;

        float[] start = lastPoint();
        recordMove(start[0], start[1]);
    }

    private void recordMove(float x, float y)
    {
        pushVerb(VERB_MOVE);
        pushPoint(x, y);
        mContourStartX = x;
        mContourStartY = y;
        mContourOpen = true;
    }

    private void recordLine(float x, float y)
    {
        ensureContour();
        pushVerb(VERB_LINE);
        pushPoint(x, y);
    }

    private void recordQuad(float controlX, float controlY, float x, float y)
    {
        ensureContour();
        pushVerb(VERB_QUAD);
        pushPoint(controlX, controlY);
        pushPoint(x, y);
    }

    private void recordCubic(float firstX, float firstY, float secondX, float secondY, float x, float y)
    {
        ensureContour();
        pushVerb(VERB_CUBIC);
        pushPoint(firstX, firstY);
        pushPoint(secondX, secondY);
        pushPoint(x, y);
    }

    private void recordClose()
    {
        if (!mContourOpen) return;

        pushVerb(VERB_CLOSE);
        mContourOpen = false;
    }

    private void recordOval(float left, float top, float right, float bottom)
    {
        float centreX = (left + right) * 0.5f;
        float centreY = (top + bottom) * 0.5f;
        float radiusX = (right - left) * 0.5f;
        float radiusY = (bottom - top) * 0.5f;
        float handleX = radiusX * CIRCLE_KAPPA;
        float handleY = radiusY * CIRCLE_KAPPA;

        recordMove(right, centreY);
        recordCubic(right, centreY + handleY, centreX + handleX, bottom, centreX, bottom);
        recordCubic(centreX - handleX, bottom, left, centreY + handleY, left, centreY);
        recordCubic(left, centreY - handleY, centreX - handleX, top, centreX, top);
        recordCubic(centreX + handleX, top, right, centreY - handleY, right, centreY);
        recordClose();
    }

    private void recordRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY)
    {
        float width = Math.abs(right - left);
        float height = Math.abs(bottom - top);
        float rx = Math.max(0, Math.min(radiusX, width * 0.5f));
        float ry = Math.max(0, Math.min(radiusY, height * 0.5f));

        if (rx <= 0 || ry <= 0)
        {
            recordMove(left, top);
            recordLine(right, top);
            recordLine(right, bottom);
            recordLine(left, bottom);
            recordClose();
            return;
        }

        float handleX = rx * (1 - CIRCLE_KAPPA);
        float handleY = ry * (1 - CIRCLE_KAPPA);

        recordMove(left + rx, top);
        recordLine(right - rx, top);
        recordCubic(right - handleX, top, right, top + handleY, right, top + ry);
        recordLine(right, bottom - ry);
        recordCubic(right, bottom - handleY, right - handleX, bottom, right - rx, bottom);
        recordLine(left + rx, bottom);
        recordCubic(left + handleX, bottom, left, bottom - handleY, left, bottom - ry);
        recordLine(left, top + ry);
        recordCubic(left, top + handleY, left + handleX, top, left + rx, top);
        recordClose();
    }

    private void recordArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                           boolean forceMoveTo)
    {
        float centreX = (left + right) * 0.5f;
        float centreY = (top + bottom) * 0.5f;
        float radiusX = (right - left) * 0.5f;
        float radiusY = (bottom - top) * 0.5f;

        float sweep = Math.max(-360, Math.min(360, sweepAngle));
        double start = Math.toRadians(startAngle);
        float startX = centreX + radiusX * (float) Math.cos(start);
        float startY = centreY + radiusY * (float) Math.sin(start);

        if (forceMoveTo || !mContourOpen) recordMove(startX, startY);
        else recordLine(startX, startY);

        int segments = Math.max(1, (int) Math.ceil(Math.abs(sweep) / 90.0f));
        double step = Math.toRadians(sweep) / segments;
        double handle = 4.0 / 3.0 * Math.tan(step / 4.0);

        double angle = start;
        for (int segment = 0; segment < segments; segment++)
        {
            double next = angle + step;
            double cosStart = Math.cos(angle);
            double sinStart = Math.sin(angle);
            double cosEnd = Math.cos(next);
            double sinEnd = Math.sin(next);

            recordCubic((float) (centreX + radiusX * (cosStart - handle * sinStart)),
                        (float) (centreY + radiusY * (sinStart + handle * cosStart)),
                        (float) (centreX + radiusX * (cosEnd + handle * sinEnd)),
                        (float) (centreY + radiusY * (sinEnd - handle * cosEnd)), (float) (centreX + radiusX * cosEnd),
                        (float) (centreY + radiusY * sinEnd));

            angle = next;
        }
    }

    private void appendRecord(byte[] verbs, float[] coordinates)
    {
        int coordinate = 0;

        for (byte verb : verbs)
        {
            switch (verb)
            {
                case VERB_MOVE:
                    recordMove(coordinates[coordinate], coordinates[coordinate + 1]);
                    coordinate += 2;
                    break;
                case VERB_LINE:
                    recordLine(coordinates[coordinate], coordinates[coordinate + 1]);
                    coordinate += 2;
                    break;
                case VERB_QUAD:
                    recordQuad(coordinates[coordinate], coordinates[coordinate + 1], coordinates[coordinate + 2],
                               coordinates[coordinate + 3]);
                    coordinate += 4;
                    break;
                case VERB_CUBIC:
                    recordCubic(coordinates[coordinate], coordinates[coordinate + 1], coordinates[coordinate + 2],
                                coordinates[coordinate + 3], coordinates[coordinate + 4], coordinates[coordinate + 5]);
                    coordinate += 6;
                    break;
                default:
                    recordClose();
                    break;
            }
        }
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
