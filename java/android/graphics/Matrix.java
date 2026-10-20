package android.graphics;

public class Matrix
{
    public static final int MSCALE_X = 0;
    public static final int MSKEW_X = 1;
    public static final int MTRANS_X = 2;
    public static final int MSKEW_Y = 3;
    public static final int MSCALE_Y = 4;
    public static final int MTRANS_Y = 5;
    public static final int MPERSP_0 = 6;
    public static final int MPERSP_1 = 7;
    public static final int MPERSP_2 = 8;

    public static final Matrix IDENTITY_MATRIX = new Matrix();

    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    private static final float NEARLY_ZERO = 1.0f / (1 << 12);

    private final float[] mValues = new float[9];

    public Matrix()
    {
        reset();
    }

    public Matrix(Matrix source)
    {
        set(source);
    }

    public void reset()
    {
        mValues[MSCALE_X] = 1;
        mValues[MSKEW_X] = 0;
        mValues[MTRANS_X] = 0;
        mValues[MSKEW_Y] = 0;
        mValues[MSCALE_Y] = 1;
        mValues[MTRANS_Y] = 0;
        mValues[MPERSP_0] = 0;
        mValues[MPERSP_1] = 0;
        mValues[MPERSP_2] = 1;
    }

    public void set(Matrix source)
    {
        if (source == null) reset();
        else System.arraycopy(source.mValues, 0, mValues, 0, mValues.length);
    }

    public void getValues(float[] values)
    {
        if (values != null) System.arraycopy(mValues, 0, values, 0, Math.min(values.length, mValues.length));
    }

    public void setValues(float[] values)
    {
        if (values != null) System.arraycopy(values, 0, mValues, 0, Math.min(values.length, mValues.length));
    }

    public boolean isIdentity()
    {
        return mValues[MSCALE_X] == 1 && mValues[MSCALE_Y] == 1 && mValues[MPERSP_2] == 1 && mValues[MSKEW_X] == 0
               && mValues[MSKEW_Y] == 0 && mValues[MTRANS_X] == 0 && mValues[MTRANS_Y] == 0 && mValues[MPERSP_0] == 0
               && mValues[MPERSP_1] == 0;
    }

    public boolean isAffine()
    {
        return mValues[MPERSP_0] == 0 && mValues[MPERSP_1] == 0 && mValues[MPERSP_2] == 1;
    }

    public boolean rectStaysRect()
    {
        if (!isAffine()) return false;

        boolean axisAligned = NearlyZero(mValues[MSKEW_X]) && NearlyZero(mValues[MSKEW_Y]);
        boolean quarterTurned = NearlyZero(mValues[MSCALE_X]) && NearlyZero(mValues[MSCALE_Y]);

        return axisAligned || quarterTurned;
    }

    private static boolean NearlyZero(float value)
    {
        return Math.abs(value) < NEARLY_ZERO;
    }

    private static void Multiply(float[] first, float[] second, float[] into)
    {
        float scaleX = first[MSCALE_X] * second[MSCALE_X] + first[MSKEW_X] * second[MSKEW_Y] + first[MTRANS_X] * second[MPERSP_0];
        float skewX = first[MSCALE_X] * second[MSKEW_X] + first[MSKEW_X] * second[MSCALE_Y] + first[MTRANS_X] * second[MPERSP_1];
        float translateX = first[MSCALE_X] * second[MTRANS_X] + first[MSKEW_X] * second[MTRANS_Y] + first[MTRANS_X] * second[MPERSP_2];

        float skewY = first[MSKEW_Y] * second[MSCALE_X] + first[MSCALE_Y] * second[MSKEW_Y] + first[MTRANS_Y] * second[MPERSP_0];
        float scaleY = first[MSKEW_Y] * second[MSKEW_X] + first[MSCALE_Y] * second[MSCALE_Y] + first[MTRANS_Y] * second[MPERSP_1];
        float translateY = first[MSKEW_Y] * second[MTRANS_X] + first[MSCALE_Y] * second[MTRANS_Y] + first[MTRANS_Y] * second[MPERSP_2];

        float perspectiveX = first[MPERSP_0] * second[MSCALE_X] + first[MPERSP_1] * second[MSKEW_Y] + first[MPERSP_2] * second[MPERSP_0];
        float perspectiveY = first[MPERSP_0] * second[MSKEW_X] + first[MPERSP_1] * second[MSCALE_Y] + first[MPERSP_2] * second[MPERSP_1];
        float perspectiveW = first[MPERSP_0] * second[MTRANS_X] + first[MPERSP_1] * second[MTRANS_Y] + first[MPERSP_2] * second[MPERSP_2];

        into[MSCALE_X] = scaleX;
        into[MSKEW_X] = skewX;
        into[MTRANS_X] = translateX;
        into[MSKEW_Y] = skewY;
        into[MSCALE_Y] = scaleY;
        into[MTRANS_Y] = translateY;
        into[MPERSP_0] = perspectiveX;
        into[MPERSP_1] = perspectiveY;
        into[MPERSP_2] = perspectiveW;
    }

    public boolean setConcat(Matrix first, Matrix second)
    {
        if (first == null || second == null) return false;

        Multiply(first.mValues, second.mValues, mValues);
        return true;
    }

    public boolean preConcat(Matrix other)
    {
        if (other == null) return false;

        Multiply(mValues, other.mValues, mValues);
        return true;
    }

    public boolean postConcat(Matrix other)
    {
        if (other == null) return false;

        Multiply(other.mValues, mValues, mValues);
        return true;
    }

    public void setTranslate(float dx, float dy)
    {
        reset();
        mValues[MTRANS_X] = dx;
        mValues[MTRANS_Y] = dy;
    }

    public void setScale(float sx, float sy)
    {
        reset();
        mValues[MSCALE_X] = sx;
        mValues[MSCALE_Y] = sy;
    }

    public void setScale(float sx, float sy, float pivotX, float pivotY)
    {
        reset();
        mValues[MSCALE_X] = sx;
        mValues[MSCALE_Y] = sy;
        mValues[MTRANS_X] = pivotX - sx * pivotX;
        mValues[MTRANS_Y] = pivotY - sy * pivotY;
    }

    public void setRotate(float degrees)
    {
        float radians = degrees * DEGREES_TO_RADIANS;

        setSinCos((float) Math.sin(radians), (float) Math.cos(radians));
    }

    public void setRotate(float degrees, float pivotX, float pivotY)
    {
        setRotate(degrees);
        mValues[MTRANS_X] = pivotX - mValues[MSCALE_X] * pivotX - mValues[MSKEW_X] * pivotY;
        mValues[MTRANS_Y] = pivotY - mValues[MSKEW_Y] * pivotX - mValues[MSCALE_Y] * pivotY;
    }

    public void setSkew(float kx, float ky)
    {
        reset();
        mValues[MSKEW_X] = kx;
        mValues[MSKEW_Y] = ky;
    }

    public void setSkew(float kx, float ky, float pivotX, float pivotY)
    {
        setSkew(kx, ky);
        mValues[MTRANS_X] = -kx * pivotY;
        mValues[MTRANS_Y] = -ky * pivotX;
    }

    public void setSinCos(float sinValue, float cosValue)
    {
        reset();
        mValues[MSCALE_X] = cosValue;
        mValues[MSKEW_X] = -sinValue;
        mValues[MSKEW_Y] = sinValue;
        mValues[MSCALE_Y] = cosValue;
    }

    public void setSinCos(float sinValue, float cosValue, float pivotX, float pivotY)
    {
        setSinCos(sinValue, cosValue);
        mValues[MTRANS_X] = pivotX - cosValue * pivotX + sinValue * pivotY;
        mValues[MTRANS_Y] = pivotY - sinValue * pivotX - cosValue * pivotY;
    }

    private boolean preOperation(Matrix operation)
    {
        Multiply(mValues, operation.mValues, mValues);
        return true;
    }

    private boolean postOperation(Matrix operation)
    {
        Multiply(operation.mValues, mValues, mValues);
        return true;
    }

    public boolean preTranslate(float dx, float dy)
    {
        Matrix step = new Matrix();
        step.setTranslate(dx, dy);

        return preOperation(step);
    }

    public boolean postTranslate(float dx, float dy)
    {
        Matrix step = new Matrix();
        step.setTranslate(dx, dy);

        return postOperation(step);
    }

    public boolean preScale(float sx, float sy)
    {
        Matrix step = new Matrix();
        step.setScale(sx, sy);

        return preOperation(step);
    }

    public boolean preScale(float sx, float sy, float pivotX, float pivotY)
    {
        Matrix step = new Matrix();
        step.setScale(sx, sy, pivotX, pivotY);

        return preOperation(step);
    }

    public boolean postScale(float sx, float sy)
    {
        Matrix step = new Matrix();
        step.setScale(sx, sy);

        return postOperation(step);
    }

    public boolean postScale(float sx, float sy, float pivotX, float pivotY)
    {
        Matrix step = new Matrix();
        step.setScale(sx, sy, pivotX, pivotY);

        return postOperation(step);
    }

    public boolean preRotate(float degrees)
    {
        Matrix step = new Matrix();
        step.setRotate(degrees);

        return preOperation(step);
    }

    public boolean preRotate(float degrees, float pivotX, float pivotY)
    {
        Matrix step = new Matrix();
        step.setRotate(degrees, pivotX, pivotY);

        return preOperation(step);
    }

    public boolean postRotate(float degrees)
    {
        Matrix step = new Matrix();
        step.setRotate(degrees);

        return postOperation(step);
    }

    public boolean postRotate(float degrees, float pivotX, float pivotY)
    {
        Matrix step = new Matrix();
        step.setRotate(degrees, pivotX, pivotY);

        return postOperation(step);
    }

    public boolean preSkew(float kx, float ky)
    {
        Matrix step = new Matrix();
        step.setSkew(kx, ky);

        return preOperation(step);
    }

    public boolean preSkew(float kx, float ky, float pivotX, float pivotY)
    {
        Matrix step = new Matrix();
        step.setSkew(kx, ky, pivotX, pivotY);

        return preOperation(step);
    }

    public boolean postSkew(float kx, float ky)
    {
        Matrix step = new Matrix();
        step.setSkew(kx, ky);

        return postOperation(step);
    }

    public boolean postSkew(float kx, float ky, float pivotX, float pivotY)
    {
        Matrix step = new Matrix();
        step.setSkew(kx, ky, pivotX, pivotY);

        return postOperation(step);
    }

    public boolean invert(Matrix inverse)
    {
        if (inverse == null) return false;

        float scaleX = mValues[MSCALE_X];
        float skewX = mValues[MSKEW_X];
        float translateX = mValues[MTRANS_X];
        float skewY = mValues[MSKEW_Y];
        float scaleY = mValues[MSCALE_Y];
        float translateY = mValues[MTRANS_Y];
        float perspectiveX = mValues[MPERSP_0];
        float perspectiveY = mValues[MPERSP_1];
        float perspectiveW = mValues[MPERSP_2];

        float cofactorA = scaleY * perspectiveW - translateY * perspectiveY;
        float cofactorB = translateY * perspectiveX - skewY * perspectiveW;
        float cofactorC = skewY * perspectiveY - scaleY * perspectiveX;

        float determinant = scaleX * cofactorA + skewX * cofactorB + translateX * cofactorC;
        if (determinant == 0 || Float.isNaN(determinant) || Float.isInfinite(determinant)) return false;

        float reciprocal = 1.0f / determinant;

        float[] result = new float[9];
        result[MSCALE_X] = cofactorA * reciprocal;
        result[MSKEW_X] = (translateX * perspectiveY - skewX * perspectiveW) * reciprocal;
        result[MTRANS_X] = (skewX * translateY - translateX * scaleY) * reciprocal;
        result[MSKEW_Y] = cofactorB * reciprocal;
        result[MSCALE_Y] = (scaleX * perspectiveW - translateX * perspectiveX) * reciprocal;
        result[MTRANS_Y] = (translateX * skewY - scaleX * translateY) * reciprocal;
        result[MPERSP_0] = cofactorC * reciprocal;
        result[MPERSP_1] = (skewX * perspectiveX - scaleX * perspectiveY) * reciprocal;
        result[MPERSP_2] = (scaleX * scaleY - skewX * skewY) * reciprocal;

        System.arraycopy(result, 0, inverse.mValues, 0, result.length);
        return true;
    }

    public void mapPoints(float[] points)
    {
        if (points != null) mapPoints(points, 0, points, 0, points.length / 2);
    }

    public void mapPoints(float[] destination, float[] source)
    {
        if (destination == null || source == null) return;

        mapPoints(destination, 0, source, 0, Math.min(destination.length, source.length) / 2);
    }

    public void mapPoints(float[] destination, int destinationIndex, float[] source, int sourceIndex, int pointCount)
    {
        if (destination == null || source == null) return;

        for (int index = 0; index < pointCount; index++)
        {
            float x = source[sourceIndex + index * 2];
            float y = source[sourceIndex + index * 2 + 1];

            float mappedX = mValues[MSCALE_X] * x + mValues[MSKEW_X] * y + mValues[MTRANS_X];
            float mappedY = mValues[MSKEW_Y] * x + mValues[MSCALE_Y] * y + mValues[MTRANS_Y];
            float mappedW = mValues[MPERSP_0] * x + mValues[MPERSP_1] * y + mValues[MPERSP_2];

            if (mappedW != 0 && mappedW != 1)
            {
                mappedX /= mappedW;
                mappedY /= mappedW;
            }

            destination[destinationIndex + index * 2] = mappedX;
            destination[destinationIndex + index * 2 + 1] = mappedY;
        }
    }

    public void mapVectors(float[] vectors)
    {
        if (vectors != null) mapVectors(vectors, 0, vectors, 0, vectors.length / 2);
    }

    public void mapVectors(float[] destination, float[] source)
    {
        if (destination == null || source == null) return;

        mapVectors(destination, 0, source, 0, Math.min(destination.length, source.length) / 2);
    }

    public String toShortString()
    {
        return "[" + mValues[MSCALE_X] + ", " + mValues[MSKEW_X] + ", " + mValues[MTRANS_X] + "]["
               + mValues[MSKEW_Y] + ", " + mValues[MSCALE_Y] + ", " + mValues[MTRANS_Y] + "]["
               + mValues[MPERSP_0] + ", " + mValues[MPERSP_1] + ", " + mValues[MPERSP_2] + "]";
    }

    public final void dump(java.io.PrintWriter writer)
    {
        if (writer != null) writer.print(toShortString());
    }

    public void mapVectors(float[] destination, int destinationIndex, float[] source, int sourceIndex, int vectorCount)
    {
        if (destination == null || source == null) return;

        for (int index = 0; index < vectorCount; index++)
        {
            float x = source[sourceIndex + index * 2];
            float y = source[sourceIndex + index * 2 + 1];

            destination[destinationIndex + index * 2] = mValues[MSCALE_X] * x + mValues[MSKEW_X] * y;
            destination[destinationIndex + index * 2 + 1] = mValues[MSKEW_Y] * x + mValues[MSCALE_Y] * y;
        }
    }

    public boolean mapRect(RectF rect)
    {
        return mapRect(rect, rect);
    }

    public boolean mapRect(RectF destination, RectF source)
    {
        if (destination == null || source == null) return false;

        float[] corners = {source.left, source.top, source.right, source.top,
                           source.right, source.bottom, source.left, source.bottom};
        mapPoints(corners);

        float left = corners[0];
        float top = corners[1];
        float right = corners[0];
        float bottom = corners[1];

        for (int index = 2; index < corners.length; index += 2)
        {
            left = Math.min(left, corners[index]);
            right = Math.max(right, corners[index]);
            top = Math.min(top, corners[index + 1]);
            bottom = Math.max(bottom, corners[index + 1]);
        }

        destination.set(left, top, right, bottom);
        return rectStaysRect();
    }

    public float mapRadius(float radius)
    {
        float[] vectors = {radius, 0, 0, radius};
        mapVectors(vectors);

        float first = (float) Math.hypot(vectors[0], vectors[1]);
        float second = (float) Math.hypot(vectors[2], vectors[3]);

        return (float) Math.sqrt(first * second);
    }

    public boolean setRectToRect(RectF source, RectF destination, ScaleToFit fit)
    {
        if (source == null || destination == null) return false;

        if (source.isEmpty())
        {
            reset();
            return false;
        }

        if (destination.isEmpty())
        {
            setScale(0, 0);
            return true;
        }

        float scaleX = destination.width() / source.width();
        float scaleY = destination.height() / source.height();
        float translateX = destination.left - source.left * scaleX;
        float translateY = destination.top - source.top * scaleY;

        if (fit != null && fit != ScaleToFit.FILL)
        {
            float uniform = Math.min(scaleX, scaleY);
            float spareX = destination.width() - source.width() * uniform;
            float spareY = destination.height() - source.height() * uniform;

            float offsetX = fit == ScaleToFit.START ? 0 : (fit == ScaleToFit.END ? spareX : spareX * 0.5f);
            float offsetY = fit == ScaleToFit.START ? 0 : (fit == ScaleToFit.END ? spareY : spareY * 0.5f);

            scaleX = uniform;
            scaleY = uniform;
            translateX = destination.left - source.left * uniform + offsetX;
            translateY = destination.top - source.top * uniform + offsetY;
        }

        reset();
        mValues[MSCALE_X] = scaleX;
        mValues[MSCALE_Y] = scaleY;
        mValues[MTRANS_X] = translateX;
        mValues[MTRANS_Y] = translateY;

        return true;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Matrix)) return false;

        return java.util.Arrays.equals(mValues, ((Matrix) other).mValues);
    }

    @Override
    public int hashCode()
    {
        return java.util.Arrays.hashCode(mValues);
    }

    @Override
    public String toString()
    {
        return "Matrix{" + java.util.Arrays.toString(mValues) + "}";
    }

    public enum ScaleToFit
    {
        FILL,
        START,
        CENTER,
        END
    }
}
