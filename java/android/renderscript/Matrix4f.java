package android.renderscript;

public class Matrix4f
{
    final float[] mMat;

    public Matrix4f()
    {
        mMat = new float[16];
        loadIdentity();
    }

    public Matrix4f(float[] dataArray)
    {
        mMat = new float[16];
        System.arraycopy(dataArray, 0, mMat, 0, mMat.length);
    }

    public float[] getArray()
    {
        return mMat;
    }

    public float get(int x, int y)
    {
        return mMat[x * 4 + y];
    }

    public void set(int x, int y, float value)
    {
        mMat[x * 4 + y] = value;
    }

    public void loadIdentity()
    {
        android.opengl.Matrix.setIdentityM(mMat, 0);
    }

    public void load(Matrix4f src)
    {
        System.arraycopy(src.getArray(), 0, mMat, 0, mMat.length);
    }

    public void loadRotate(float rot, float x, float y, float z)
    {
        android.opengl.Matrix.setRotateM(mMat, 0, rot, x, y, z);
    }

    public void loadScale(float x, float y, float z)
    {
        loadIdentity();
        mMat[0] = x;
        mMat[5] = y;
        mMat[10] = z;
    }

    public void loadTranslate(float x, float y, float z)
    {
        loadIdentity();
        mMat[12] = x;
        mMat[13] = y;
        mMat[14] = z;
    }

    public void loadMultiply(Matrix4f lhs, Matrix4f rhs)
    {
        android.opengl.Matrix.multiplyMM(mMat, 0, lhs.mMat, 0, rhs.mMat, 0);
    }

    public void loadOrtho(float left, float right, float bottom, float top, float near, float far)
    {
        loadIdentity();
        mMat[0] = 2 / (right - left);
        mMat[5] = 2 / (top - bottom);
        mMat[10] = -2 / (far - near);
        mMat[12] = -(right + left) / (right - left);
        mMat[13] = -(top + bottom) / (top - bottom);
        mMat[14] = -(far + near) / (far - near);
    }

    public void loadOrthoWindow(int w, int h)
    {
        loadOrtho(0, w, h, 0, -1, 1);
    }

    public void loadFrustum(float left, float right, float bottom, float top, float near, float far)
    {
        loadIdentity();
        mMat[0] = 2 * near / (right - left);
        mMat[5] = 2 * near / (top - bottom);
        mMat[8] = (right + left) / (right - left);
        mMat[9] = (top + bottom) / (top - bottom);
        mMat[10] = -(far + near) / (far - near);
        mMat[11] = -1;
        mMat[14] = -2 * far * near / (far - near);
        mMat[15] = 0;
    }

    public void loadPerspective(float fovy, float aspect, float near, float far)
    {
        float top = near * (float) Math.tan((float) (fovy * Math.PI / 360.0f));
        float bottom = -top;
        float left = bottom * aspect;
        float right = top * aspect;
        loadFrustum(left, right, bottom, top, near, far);
    }

    public void loadProjectionNormalized(int w, int h)
    {
        Matrix4f projection = new Matrix4f();
        Matrix4f step = new Matrix4f();

        if (w > h)
        {
            float aspect = ((float) w) / h;
            projection.loadFrustum(-aspect, aspect, -1, 1, 1, 100);
        }
        else
        {
            float aspect = ((float) h) / w;
            projection.loadFrustum(-1, 1, -aspect, aspect, 1, 100);
        }

        step.loadRotate(180, 0, 1, 0);
        projection.loadMultiply(projection, step);

        step.loadScale(-2, 2, 1);
        projection.loadMultiply(projection, step);

        step.loadTranslate(0, 0, 2);
        projection.loadMultiply(projection, step);

        load(projection);
    }

    public void multiply(Matrix4f rhs)
    {
        loadMultiply(this, rhs);
    }

    public void rotate(float rot, float x, float y, float z)
    {
        Matrix4f rotation = new Matrix4f();
        rotation.loadRotate(rot, x, y, z);
        multiply(rotation);
    }

    public void scale(float x, float y, float z)
    {
        Matrix4f scaling = new Matrix4f();
        scaling.loadScale(x, y, z);
        multiply(scaling);
    }

    public void translate(float x, float y, float z)
    {
        Matrix4f translation = new Matrix4f();
        translation.loadTranslate(x, y, z);
        multiply(translation);
    }

    public boolean inverse()
    {
        float[] result = new float[16];
        if (!android.opengl.Matrix.invertM(result, 0, mMat, 0)) return false;

        System.arraycopy(result, 0, mMat, 0, 16);
        return true;
    }

    public boolean inverseTranspose()
    {
        if (!inverse()) return false;

        transpose();
        return true;
    }

    public void transpose()
    {
        for (int column = 0; column < 3; column++)
        {
            for (int row = column + 1; row < 4; row++)
            {
                float temporary = mMat[column * 4 + row];
                mMat[column * 4 + row] = mMat[row * 4 + column];
                mMat[row * 4 + column] = temporary;
            }
        }
    }
}
