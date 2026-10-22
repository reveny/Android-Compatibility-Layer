package android.renderscript;

public class Matrix2f
{
    final float[] mMat;

    public Matrix2f()
    {
        mMat = new float[4];
        loadIdentity();
    }

    public Matrix2f(float[] dataArray)
    {
        mMat = new float[4];
        System.arraycopy(dataArray, 0, mMat, 0, mMat.length);
    }

    public float[] getArray()
    {
        return mMat;
    }

    public float get(int x, int y)
    {
        return mMat[x * 2 + y];
    }

    public void set(int x, int y, float value)
    {
        mMat[x * 2 + y] = value;
    }

    public void loadIdentity()
    {
        mMat[0] = 1;
        mMat[1] = 0;
        mMat[2] = 0;
        mMat[3] = 1;
    }

    public void load(Matrix2f src)
    {
        System.arraycopy(src.getArray(), 0, mMat, 0, mMat.length);
    }

    public void loadRotate(float rot)
    {
        double radians = rot * (Math.PI / 180.0f);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        mMat[0] = cosine;
        mMat[1] = -sine;
        mMat[2] = sine;
        mMat[3] = cosine;
    }

    public void loadScale(float x, float y)
    {
        loadIdentity();
        mMat[0] = x;
        mMat[3] = y;
    }

    public void loadMultiply(Matrix2f lhs, Matrix2f rhs)
    {
        float[] result = new float[4];
        for (int column = 0; column < 2; column++)
        {
            for (int row = 0; row < 2; row++)
            {
                result[column * 2 + row] = lhs.get(0, row) * rhs.get(column, 0) + lhs.get(1, row) * rhs.get(column, 1);
            }
        }
        System.arraycopy(result, 0, mMat, 0, 4);
    }

    public void multiply(Matrix2f rhs)
    {
        loadMultiply(this, rhs);
    }

    public void rotate(float rot)
    {
        Matrix2f rotation = new Matrix2f();
        rotation.loadRotate(rot);
        multiply(rotation);
    }

    public void scale(float x, float y)
    {
        Matrix2f scaling = new Matrix2f();
        scaling.loadScale(x, y);
        multiply(scaling);
    }

    public void transpose()
    {
        float temporary = mMat[1];
        mMat[1] = mMat[2];
        mMat[2] = temporary;
    }
}
