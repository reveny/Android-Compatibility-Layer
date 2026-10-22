package android.graphics;

import java.util.ArrayDeque;

public class Camera
{
    private static final float POINTS_PER_INCH = 72.0f;

    private final ArrayDeque<float[]> mSaved = new ArrayDeque<>();

    private float[] mTransform = identity();
    private float mLocationX;
    private float mLocationY;
    private float mLocationZ = -8 * POINTS_PER_INCH;
    private float mObserverZ = -8 * POINTS_PER_INCH;

    public Camera()
    {
    }

    public void save()
    {
        mSaved.push(mTransform.clone());
    }

    public void restore()
    {
        if (!mSaved.isEmpty()) mTransform = mSaved.pop();
    }

    public void translate(float x, float y, float z)
    {
        float[] step = identity();
        step[3] = x;
        step[7] = y;
        step[11] = z;

        mTransform = multiply(mTransform, step);
    }

    public void rotateX(float degrees)
    {
        mTransform = multiply(mTransform, rotation(1, 0, 0, degrees));
    }

    public void rotateY(float degrees)
    {
        mTransform = multiply(mTransform, rotation(0, -1, 0, degrees));
    }

    public void rotateZ(float degrees)
    {
        mTransform = multiply(mTransform, rotation(0, 0, 1, degrees));
    }

    public void rotate(float x, float y, float z)
    {
        rotateX(x);
        rotateY(y);
        rotateZ(z);
    }

    public float getLocationX()
    {
        return mLocationX / POINTS_PER_INCH;
    }

    public float getLocationY()
    {
        return mLocationY / POINTS_PER_INCH;
    }

    public float getLocationZ()
    {
        return mLocationZ / POINTS_PER_INCH;
    }

    public void setLocation(float x, float y, float z)
    {
        mLocationX = x * POINTS_PER_INCH;
        mLocationY = y * POINTS_PER_INCH;
        mLocationZ = z * POINTS_PER_INCH;
        mObserverZ = mLocationZ;
    }

    public void getMatrix(Matrix matrix)
    {
        if (matrix == null) return;

        float[] values = new float[9];
        computeMatrix(values);
        matrix.setValues(values);
    }

    public void applyToCanvas(Canvas canvas)
    {
        if (canvas == null) return;

        Matrix matrix = new Matrix();
        getMatrix(matrix);
        canvas.concat(matrix);
    }

    public float dotWithNormal(float dx, float dy, float dz)
    {
        float[] matrix = mTransform;

        float ux = matrix[0];
        float uy = matrix[4];
        float uz = matrix[8];
        float vx = -matrix[1];
        float vy = -matrix[5];
        float vz = -matrix[9];

        float cx = uy * vz - uz * vy;
        float cy = uz * vx - ux * vy;
        float cz = ux * vy - uy * vx;

        return cx * dx + cy * dy + cz * dz;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    private void computeMatrix(float[] out)
    {
        float[] matrix = mTransform;

        float[] u = {matrix[0], matrix[4], matrix[8]};
        float[] v = {-matrix[1], -matrix[5], -matrix[9]};
        float[] difference = {matrix[3] - mLocationX, matrix[7] - mLocationY, matrix[11] - mLocationZ};

        float[] orientation = orientation();
        float dot = difference[0] * orientation[6] + difference[1] * orientation[7] + difference[2] * orientation[8];

        out[Matrix.MSCALE_X] = dotDivide(u, orientation, 0, dot);
        out[Matrix.MSKEW_Y] = dotDivide(u, orientation, 3, dot);
        out[Matrix.MPERSP_0] = dotDivide(u, orientation, 6, dot);
        out[Matrix.MSKEW_X] = dotDivide(v, orientation, 0, dot);
        out[Matrix.MSCALE_Y] = dotDivide(v, orientation, 3, dot);
        out[Matrix.MPERSP_1] = dotDivide(v, orientation, 6, dot);
        out[Matrix.MTRANS_X] = dotDivide(difference, orientation, 0, dot);
        out[Matrix.MTRANS_Y] = dotDivide(difference, orientation, 3, dot);
        out[Matrix.MPERSP_2] = 1;
    }

    private float[] orientation()
    {
        float axisX = 0;
        float axisY = 0;
        float axisZ = 1;

        float zenithX = 0;
        float zenithY = -1;
        float zenithZ = 0;

        float crossX = axisY * zenithZ - axisZ * zenithY;
        float crossY = axisZ * zenithX - axisX * zenithZ;
        float crossZ = axisX * zenithY - axisY * zenithX;

        float observerX = 0;
        float observerY = 0;
        float observerZ = mObserverZ;

        return new float[] {
            observerX * axisX - observerZ * crossX, observerX * axisY - observerZ * crossY, observerX * axisZ - observerZ * crossZ,
            observerY * axisX - observerZ * zenithX, observerY * axisY - observerZ * zenithY, observerY * axisZ - observerZ * zenithZ,
            axisX, axisY, axisZ
        };
    }

    private static float dotDivide(float[] vector, float[] orientation, int offset, float denominator)
    {
        float sum = vector[0] * orientation[offset] + vector[1] * orientation[offset + 1] + vector[2] * orientation[offset + 2];
        return denominator == 0 ? 0 : sum / denominator;
    }

    private static float[] identity()
    {
        float[] matrix = new float[16];
        matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1;
        return matrix;
    }

    private static float[] rotation(float x, float y, float z, float degrees)
    {
        double radians = degrees * Math.PI / 180;
        float sine = (float)Math.sin(radians);
        float cosine = (float)Math.cos(radians);
        float complement = 1 - cosine;

        return new float[] {
            complement * x * x + cosine, complement * x * y - sine * z, complement * x * z + sine * y, 0,
            complement * x * y + sine * z, complement * y * y + cosine, complement * y * z - sine * x, 0,
            complement * x * z - sine * y, complement * y * z + sine * x, complement * z * z + cosine, 0,
            0, 0, 0, 1
        };
    }

    private static float[] multiply(float[] left, float[] right)
    {
        float[] product = new float[16];

        for (int row = 0; row < 4; row++)
        {
            for (int column = 0; column < 4; column++)
            {
                float sum = 0;
                for (int index = 0; index < 4; index++) sum += left[row * 4 + index] * right[index * 4 + column];

                product[row * 4 + column] = sum;
            }
        }

        return product;
    }
}
