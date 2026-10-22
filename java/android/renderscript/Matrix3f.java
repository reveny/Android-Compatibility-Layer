package android.renderscript;

public class Matrix3f
{
    final float[] mMat;

    public Matrix3f()
    {
        mMat = new float[9];
        loadIdentity();
    }

    public Matrix3f(float[] dataArray)
    {
        mMat = new float[9];
        System.arraycopy(dataArray, 0, mMat, 0, mMat.length);
    }

    public float[] getArray()
    {
        return mMat;
    }

    public float get(int x, int y)
    {
        return mMat[x * 3 + y];
    }

    public void set(int x, int y, float value)
    {
        mMat[x * 3 + y] = value;
    }

    public void loadIdentity()
    {
        for (int index = 0; index < 9; index++) mMat[index] = index % 4 == 0 ? 1.0f : 0.0f;
    }

    public void load(Matrix3f src)
    {
        System.arraycopy(src.getArray(), 0, mMat, 0, mMat.length);
    }

    public void loadRotate(float rot, float x, float y, float z)
    {
        float[] rotation = new float[16];
        android.opengl.Matrix.setRotateM(rotation, 0, rot, x, y, z);
        for (int column = 0; column < 3; column++)
        {
            for (int row = 0; row < 3; row++) mMat[column * 3 + row] = rotation[column * 4 + row];
        }
    }

    public void loadRotate(float rot)
    {
        loadIdentity();
        double radians = rot * (Math.PI / 180.0f);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        mMat[0] = cosine;
        mMat[1] = -sine;
        mMat[3] = sine;
        mMat[4] = cosine;
    }

    public void loadScale(float x, float y)
    {
        loadIdentity();
        mMat[0] = x;
        mMat[4] = y;
    }

    public void loadScale(float x, float y, float z)
    {
        loadIdentity();
        mMat[0] = x;
        mMat[4] = y;
        mMat[8] = z;
    }

    public void loadTranslate(float x, float y)
    {
        loadIdentity();
        mMat[6] = x;
        mMat[7] = y;
    }

    public void loadMultiply(Matrix3f lhs, Matrix3f rhs)
    {
        float[] result = new float[9];
        for (int column = 0; column < 3; column++)
        {
            for (int row = 0; row < 3; row++)
            {
                float sum = 0;
                for (int index = 0; index < 3; index++) sum += lhs.get(index, row) * rhs.get(column, index);
                result[column * 3 + row] = sum;
            }
        }
        System.arraycopy(result, 0, mMat, 0, 9);
    }

    public void multiply(Matrix3f rhs)
    {
        loadMultiply(this, rhs);
    }

    public void rotate(float rot, float x, float y, float z)
    {
        Matrix3f rotation = new Matrix3f();
        rotation.loadRotate(rot, x, y, z);
        multiply(rotation);
    }

    public void rotate(float rot)
    {
        Matrix3f rotation = new Matrix3f();
        rotation.loadRotate(rot);
        multiply(rotation);
    }

    public void scale(float x, float y)
    {
        Matrix3f scaling = new Matrix3f();
        scaling.loadScale(x, y);
        multiply(scaling);
    }

    public void scale(float x, float y, float z)
    {
        Matrix3f scaling = new Matrix3f();
        scaling.loadScale(x, y, z);
        multiply(scaling);
    }

    public void translate(float x, float y)
    {
        Matrix3f translation = new Matrix3f();
        translation.loadTranslate(x, y);
        multiply(translation);
    }

    public void transpose()
    {
        for (int column = 0; column < 2; column++)
        {
            for (int row = column + 1; row < 3; row++)
            {
                float temporary = mMat[column * 3 + row];
                mMat[column * 3 + row] = mMat[row * 3 + column];
                mMat[row * 3 + column] = temporary;
            }
        }
    }
}
