package android.graphics;

import java.util.ArrayList;
import java.util.List;

public class PathMeasure
{
    public static final int POSITION_MATRIX_FLAG = 1;
    public static final int TANGENT_MATRIX_FLAG = 2;

    private static final int CURVE_STEPS = 16;

    private final List<Contour> mContours = new ArrayList<>();
    private int mContourIndex;

    public PathMeasure()
    {
    }

    public PathMeasure(Path path, boolean forceClosed)
    {
        setPath(path, forceClosed);
    }

    public void setPath(Path path, boolean forceClosed)
    {
        mContours.clear();
        mContourIndex = 0;

        if (path == null) return;

        byte[] verbs = path.verbs();
        float[] coordinates = path.coordinates();

        ContourBuilder builder = new ContourBuilder(forceClosed);
        int coordinate = 0;

        for (byte verb : verbs)
        {
            switch (verb)
            {
                case Path.VERB_MOVE:
                    builder.finish(mContours, false);
                    builder.moveTo(coordinates[coordinate], coordinates[coordinate + 1]);
                    coordinate += 2;
                    break;
                case Path.VERB_LINE:
                    builder.lineTo(coordinates[coordinate], coordinates[coordinate + 1]);
                    coordinate += 2;
                    break;
                case Path.VERB_QUAD:
                    builder.quadTo(coordinates[coordinate], coordinates[coordinate + 1], coordinates[coordinate + 2],
                                   coordinates[coordinate + 3]);
                    coordinate += 4;
                    break;
                case Path.VERB_CUBIC:
                    builder.cubicTo(coordinates[coordinate], coordinates[coordinate + 1], coordinates[coordinate + 2],
                                    coordinates[coordinate + 3], coordinates[coordinate + 4], coordinates[coordinate + 5]);
                    coordinate += 6;
                    break;
                default:
                    builder.finish(mContours, true);
                    break;
            }
        }

        builder.finish(mContours, false);
    }

    public float getLength()
    {
        Contour contour = currentContour();
        return contour == null ? 0.0f : contour.mLength;
    }

    public boolean isClosed()
    {
        Contour contour = currentContour();
        return contour != null && contour.mClosed;
    }

    public boolean nextContour()
    {
        if (mContourIndex >= mContours.size()) return false;

        mContourIndex++;
        return mContourIndex < mContours.size();
    }

    public boolean getPosTan(float distance, float[] position, float[] tangent)
    {
        Contour contour = currentContour();
        if (contour == null) return false;

        if (position != null && position.length < 2) throw new ArrayIndexOutOfBoundsException();
        if (tangent != null && tangent.length < 2) throw new ArrayIndexOutOfBoundsException();

        float[] point = new float[4];
        contour.sample(distance, point);

        if (position != null)
        {
            position[0] = point[0];
            position[1] = point[1];
        }

        if (tangent != null)
        {
            tangent[0] = point[2];
            tangent[1] = point[3];
        }

        return true;
    }

    public boolean getSegment(float startD, float stopD, Path into, boolean startWithMoveTo)
    {
        Contour contour = currentContour();
        if (contour == null || into == null) return false;

        float start = Math.max(0.0f, startD);
        float stop = Math.min(contour.mLength, stopD);
        if (start > stop || contour.mLength <= 0.0f) return false;

        float[] point = new float[4];
        contour.sample(start, point);

        if (startWithMoveTo) into.moveTo(point[0], point[1]);
        else into.lineTo(point[0], point[1]);

        for (int index = 1; index < contour.mCount - 1; index++)
        {
            float travelled = contour.mDistances[index];
            if (travelled <= start) continue;
            if (travelled >= stop) break;

            into.lineTo(contour.mPoints[index * 2], contour.mPoints[index * 2 + 1]);
        }

        contour.sample(stop, point);
        into.lineTo(point[0], point[1]);

        return true;
    }

    public boolean getMatrix(float distance, Matrix matrix, int flags)
    {
        Contour contour = currentContour();
        if (contour == null || matrix == null) return false;

        float[] point = new float[4];
        contour.sample(distance, point);

        matrix.reset();

        if ((flags & TANGENT_MATRIX_FLAG) != 0) matrix.setSinCos(point[3], point[2]);
        if ((flags & POSITION_MATRIX_FLAG) != 0) matrix.postTranslate(point[0], point[1]);

        return true;
    }

    private Contour currentContour()
    {
        if (mContourIndex >= mContours.size()) return null;

        return mContours.get(mContourIndex);
    }

    private static final class Contour
    {
        final float[] mPoints;
        final float[] mDistances;
        final int mCount;
        final float mLength;
        final boolean mClosed;

        Contour(float[] points, int count, boolean closed)
        {
            mPoints = points;
            mCount = count;
            mClosed = closed;
            mDistances = new float[count];

            float length = 0.0f;
            for (int index = 1; index < count; index++)
            {
                float deltaX = points[index * 2] - points[index * 2 - 2];
                float deltaY = points[index * 2 + 1] - points[index * 2 - 1];
                length += (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                mDistances[index] = length;
            }

            mLength = length;
        }

        void sample(float distance, float[] out)
        {
            float clamped = Math.max(0.0f, Math.min(distance, mLength));

            int segment = 1;
            while (segment < mCount - 1 && mDistances[segment] < clamped) segment++;

            if (mCount < 2)
            {
                out[0] = mPoints[0];
                out[1] = mPoints[1];
                out[2] = 1.0f;
                out[3] = 0.0f;
                return;
            }

            while (segment > 1 && mDistances[segment] - mDistances[segment - 1] <= 0.0f && mDistances[segment - 1] >= clamped)
            {
                segment--;
            }

            float startX = mPoints[segment * 2 - 2];
            float startY = mPoints[segment * 2 - 1];
            float endX = mPoints[segment * 2];
            float endY = mPoints[segment * 2 + 1];
            float span = mDistances[segment] - mDistances[segment - 1];
            float fraction = span <= 0.0f ? 0.0f : (clamped - mDistances[segment - 1]) / span;

            out[0] = startX + (endX - startX) * fraction;
            out[1] = startY + (endY - startY) * fraction;

            float deltaX = endX - startX;
            float deltaY = endY - startY;
            float magnitude = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            if (magnitude > 0.0f)
            {
                out[2] = deltaX / magnitude;
                out[3] = deltaY / magnitude;
            }
            else
            {
                out[2] = 1.0f;
                out[3] = 0.0f;
            }
        }
    }

    private static final class ContourBuilder
    {
        private final boolean mForceClosed;
        private float[] mPoints = new float[32];
        private int mCount;
        private float mStartX;
        private float mStartY;
        private float mLastX;
        private float mLastY;

        ContourBuilder(boolean forceClosed)
        {
            mForceClosed = forceClosed;
        }

        void moveTo(float x, float y)
        {
            mStartX = x;
            mStartY = y;
            mLastX = x;
            mLastY = y;
            mCount = 0;
            append(x, y);
        }

        void lineTo(float x, float y)
        {
            ensureStarted();
            append(x, y);
        }

        void quadTo(float controlX, float controlY, float endX, float endY)
        {
            ensureStarted();

            float startX = mLastX;
            float startY = mLastY;

            for (int step = 1; step <= CURVE_STEPS; step++)
            {
                float t = step / (float) CURVE_STEPS;
                float inverse = 1.0f - t;
                append(inverse * inverse * startX + 2 * inverse * t * controlX + t * t * endX,
                       inverse * inverse * startY + 2 * inverse * t * controlY + t * t * endY);
            }
        }

        void cubicTo(float firstX, float firstY, float secondX, float secondY, float endX, float endY)
        {
            ensureStarted();

            float startX = mLastX;
            float startY = mLastY;

            for (int step = 1; step <= CURVE_STEPS; step++)
            {
                float t = step / (float) CURVE_STEPS;
                float inverse = 1.0f - t;
                float a = inverse * inverse * inverse;
                float b = 3 * inverse * inverse * t;
                float c = 3 * inverse * t * t;
                float d = t * t * t;
                append(a * startX + b * firstX + c * secondX + d * endX, a * startY + b * firstY + c * secondY + d * endY);
            }
        }

        void finish(List<Contour> contours, boolean closed)
        {
            if (mCount > 1)
            {
                boolean isClosed = closed || mForceClosed;
                if (isClosed && (mLastX != mStartX || mLastY != mStartY)) append(mStartX, mStartY);

                contours.add(new Contour(java.util.Arrays.copyOf(mPoints, mCount * 2), mCount, isClosed));
            }

            mCount = 0;
            mLastX = mStartX;
            mLastY = mStartY;
        }

        private void ensureStarted()
        {
            if (mCount == 0)
            {
                mStartX = mLastX;
                mStartY = mLastY;
                append(mLastX, mLastY);
            }
        }

        private void append(float x, float y)
        {
            if (mCount * 2 + 2 > mPoints.length) mPoints = java.util.Arrays.copyOf(mPoints, mPoints.length * 2);

            mPoints[mCount * 2] = x;
            mPoints[mCount * 2 + 1] = y;
            mCount++;
            mLastX = x;
            mLastY = y;
        }
    }
}
