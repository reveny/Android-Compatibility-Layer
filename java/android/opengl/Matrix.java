package android.opengl;

public class Matrix
{
    private static final ThreadLocal<float[]> sTemporary = new ThreadLocal<float[]>()
    {
        @Override
        protected float[] initialValue()
        {
            return new float[32];
        }
    };

    @Deprecated
    public Matrix()
    {
    }

    private static void checkMatrix(float[] matrix, int offset, String name)
    {
        if (matrix == null) throw new IllegalArgumentException(name + " == null");
        if (offset < 0 || offset + 16 > matrix.length) throw new IllegalArgumentException(name + " offset out of bounds");
    }

    private static void checkVector(float[] vector, int offset, String name)
    {
        if (vector == null) throw new IllegalArgumentException(name + " == null");
        if (offset < 0 || offset + 4 > vector.length) throw new IllegalArgumentException(name + " offset out of bounds");
    }

    public static void multiplyMM(float[] result, int resultOffset, float[] lhs, int lhsOffset, float[] rhs, int rhsOffset)
    {
        checkMatrix(result, resultOffset, "result");
        checkMatrix(lhs, lhsOffset, "lhs");
        checkMatrix(rhs, rhsOffset, "rhs");

        float[] product = sTemporary.get();
        for (int column = 0; column < 4; column++)
        {
            float right0 = rhs[rhsOffset + column * 4];
            float right1 = rhs[rhsOffset + column * 4 + 1];
            float right2 = rhs[rhsOffset + column * 4 + 2];
            float right3 = rhs[rhsOffset + column * 4 + 3];
            for (int row = 0; row < 4; row++)
            {
                product[column * 4 + row] = lhs[lhsOffset + row] * right0 + lhs[lhsOffset + 4 + row] * right1
                        + lhs[lhsOffset + 8 + row] * right2 + lhs[lhsOffset + 12 + row] * right3;
            }
        }
        System.arraycopy(product, 0, result, resultOffset, 16);
    }

    public static void multiplyMV(float[] resultVec, int resultVecOffset, float[] lhsMat, int lhsMatOffset, float[] rhsVec,
            int rhsVecOffset)
    {
        checkVector(resultVec, resultVecOffset, "resultVec");
        checkMatrix(lhsMat, lhsMatOffset, "lhsMat");
        checkVector(rhsVec, rhsVecOffset, "rhsVec");

        float x = rhsVec[rhsVecOffset];
        float y = rhsVec[rhsVecOffset + 1];
        float z = rhsVec[rhsVecOffset + 2];
        float w = rhsVec[rhsVecOffset + 3];
        float[] product = sTemporary.get();
        for (int row = 0; row < 4; row++)
        {
            product[row] = lhsMat[lhsMatOffset + row] * x + lhsMat[lhsMatOffset + 4 + row] * y + lhsMat[lhsMatOffset + 8 + row] * z
                    + lhsMat[lhsMatOffset + 12 + row] * w;
        }
        System.arraycopy(product, 0, resultVec, resultVecOffset, 4);
    }

    public static void transposeM(float[] mTrans, int mTransOffset, float[] m, int mOffset)
    {
        float[] transposed = sTemporary.get();
        for (int row = 0; row < 4; row++)
        {
            for (int column = 0; column < 4; column++) transposed[row + column * 4] = m[mOffset + row * 4 + column];
        }
        System.arraycopy(transposed, 0, mTrans, mTransOffset, 16);
    }

    public static boolean invertM(float[] mInv, int mInvOffset, float[] m, int mOffset)
    {
        float m0 = m[mOffset];
        float m1 = m[mOffset + 1];
        float m2 = m[mOffset + 2];
        float m3 = m[mOffset + 3];
        float m4 = m[mOffset + 4];
        float m5 = m[mOffset + 5];
        float m6 = m[mOffset + 6];
        float m7 = m[mOffset + 7];
        float m8 = m[mOffset + 8];
        float m9 = m[mOffset + 9];
        float m10 = m[mOffset + 10];
        float m11 = m[mOffset + 11];
        float m12 = m[mOffset + 12];
        float m13 = m[mOffset + 13];
        float m14 = m[mOffset + 14];
        float m15 = m[mOffset + 15];

        float[] inverse = sTemporary.get();
        inverse[0] = m5 * m10 * m15 - m5 * m11 * m14 - m9 * m6 * m15 + m9 * m7 * m14 + m13 * m6 * m11 - m13 * m7 * m10;
        inverse[4] = -m4 * m10 * m15 + m4 * m11 * m14 + m8 * m6 * m15 - m8 * m7 * m14 - m12 * m6 * m11 + m12 * m7 * m10;
        inverse[8] = m4 * m9 * m15 - m4 * m11 * m13 - m8 * m5 * m15 + m8 * m7 * m13 + m12 * m5 * m11 - m12 * m7 * m9;
        inverse[12] = -m4 * m9 * m14 + m4 * m10 * m13 + m8 * m5 * m14 - m8 * m6 * m13 - m12 * m5 * m10 + m12 * m6 * m9;
        inverse[1] = -m1 * m10 * m15 + m1 * m11 * m14 + m9 * m2 * m15 - m9 * m3 * m14 - m13 * m2 * m11 + m13 * m3 * m10;
        inverse[5] = m0 * m10 * m15 - m0 * m11 * m14 - m8 * m2 * m15 + m8 * m3 * m14 + m12 * m2 * m11 - m12 * m3 * m10;
        inverse[9] = -m0 * m9 * m15 + m0 * m11 * m13 + m8 * m1 * m15 - m8 * m3 * m13 - m12 * m1 * m11 + m12 * m3 * m9;
        inverse[13] = m0 * m9 * m14 - m0 * m10 * m13 - m8 * m1 * m14 + m8 * m2 * m13 + m12 * m1 * m10 - m12 * m2 * m9;
        inverse[2] = m1 * m6 * m15 - m1 * m7 * m14 - m5 * m2 * m15 + m5 * m3 * m14 + m13 * m2 * m7 - m13 * m3 * m6;
        inverse[6] = -m0 * m6 * m15 + m0 * m7 * m14 + m4 * m2 * m15 - m4 * m3 * m14 - m12 * m2 * m7 + m12 * m3 * m6;
        inverse[10] = m0 * m5 * m15 - m0 * m7 * m13 - m4 * m1 * m15 + m4 * m3 * m13 + m12 * m1 * m7 - m12 * m3 * m5;
        inverse[14] = -m0 * m5 * m14 + m0 * m6 * m13 + m4 * m1 * m14 - m4 * m2 * m13 - m12 * m1 * m6 + m12 * m2 * m5;
        inverse[3] = -m1 * m6 * m11 + m1 * m7 * m10 + m5 * m2 * m11 - m5 * m3 * m10 - m9 * m2 * m7 + m9 * m3 * m6;
        inverse[7] = m0 * m6 * m11 - m0 * m7 * m10 - m4 * m2 * m11 + m4 * m3 * m10 + m8 * m2 * m7 - m8 * m3 * m6;
        inverse[11] = -m0 * m5 * m11 + m0 * m7 * m9 + m4 * m1 * m11 - m4 * m3 * m9 - m8 * m1 * m7 + m8 * m3 * m5;
        inverse[15] = m0 * m5 * m10 - m0 * m6 * m9 - m4 * m1 * m10 + m4 * m2 * m9 + m8 * m1 * m6 - m8 * m2 * m5;

        float determinant = m0 * inverse[0] + m1 * inverse[4] + m2 * inverse[8] + m3 * inverse[12];
        if (determinant == 0.0f) return false;

        float reciprocal = 1.0f / determinant;
        for (int index = 0; index < 16; index++) mInv[mInvOffset + index] = inverse[index] * reciprocal;
        return true;
    }

    public static void orthoM(float[] m, int mOffset, float left, float right, float bottom, float top, float near, float far)
    {
        if (left == right) throw new IllegalArgumentException("left == right");
        if (bottom == top) throw new IllegalArgumentException("bottom == top");
        if (near == far) throw new IllegalArgumentException("near == far");

        float reciprocalWidth = 1.0f / (right - left);
        float reciprocalHeight = 1.0f / (top - bottom);
        float reciprocalDepth = 1.0f / (far - near);

        clear(m, mOffset);
        m[mOffset] = 2.0f * reciprocalWidth;
        m[mOffset + 5] = 2.0f * reciprocalHeight;
        m[mOffset + 10] = -2.0f * reciprocalDepth;
        m[mOffset + 12] = -(right + left) * reciprocalWidth;
        m[mOffset + 13] = -(top + bottom) * reciprocalHeight;
        m[mOffset + 14] = -(far + near) * reciprocalDepth;
        m[mOffset + 15] = 1.0f;
    }

    public static void frustumM(float[] m, int offset, float left, float right, float bottom, float top, float near, float far)
    {
        if (left == right) throw new IllegalArgumentException("left == right");
        if (top == bottom) throw new IllegalArgumentException("top == bottom");
        if (near == far) throw new IllegalArgumentException("near == far");
        if (near <= 0.0f) throw new IllegalArgumentException("near <= 0.0f");
        if (far <= 0.0f) throw new IllegalArgumentException("far <= 0.0f");

        float reciprocalWidth = 1.0f / (right - left);
        float reciprocalHeight = 1.0f / (top - bottom);
        float reciprocalDepth = 1.0f / (near - far);

        clear(m, offset);
        m[offset] = 2.0f * (near * reciprocalWidth);
        m[offset + 5] = 2.0f * (near * reciprocalHeight);
        m[offset + 8] = (right + left) * reciprocalWidth;
        m[offset + 9] = (top + bottom) * reciprocalHeight;
        m[offset + 10] = (far + near) * reciprocalDepth;
        m[offset + 11] = -1.0f;
        m[offset + 14] = 2.0f * (far * near * reciprocalDepth);
    }

    public static void perspectiveM(float[] m, int offset, float fovy, float aspect, float zNear, float zFar)
    {
        float focal = 1.0f / (float) Math.tan(fovy * (Math.PI / 360.0));
        float rangeReciprocal = 1.0f / (zNear - zFar);

        clear(m, offset);
        m[offset] = focal / aspect;
        m[offset + 5] = focal;
        m[offset + 10] = (zFar + zNear) * rangeReciprocal;
        m[offset + 11] = -1.0f;
        m[offset + 14] = 2.0f * zFar * zNear * rangeReciprocal;
    }

    public static float length(float x, float y, float z)
    {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static void setIdentityM(float[] sm, int smOffset)
    {
        clear(sm, smOffset);
        for (int index = 0; index < 16; index += 5) sm[smOffset + index] = 1.0f;
    }

    public static void scaleM(float[] sm, int smOffset, float[] m, int mOffset, float x, float y, float z)
    {
        for (int index = 0; index < 4; index++)
        {
            sm[smOffset + index] = m[mOffset + index] * x;
            sm[smOffset + 4 + index] = m[mOffset + 4 + index] * y;
            sm[smOffset + 8 + index] = m[mOffset + 8 + index] * z;
            sm[smOffset + 12 + index] = m[mOffset + 12 + index];
        }
    }

    public static void scaleM(float[] m, int mOffset, float x, float y, float z)
    {
        for (int index = 0; index < 4; index++)
        {
            m[mOffset + index] *= x;
            m[mOffset + 4 + index] *= y;
            m[mOffset + 8 + index] *= z;
        }
    }

    public static void translateM(float[] tm, int tmOffset, float[] m, int mOffset, float x, float y, float z)
    {
        float[] translated = sTemporary.get();
        for (int index = 0; index < 12; index++) translated[index] = m[mOffset + index];
        for (int index = 0; index < 4; index++)
        {
            translated[12 + index] = m[mOffset + index] * x + m[mOffset + 4 + index] * y + m[mOffset + 8 + index] * z
                    + m[mOffset + 12 + index];
        }
        System.arraycopy(translated, 0, tm, tmOffset, 16);
    }

    public static void translateM(float[] m, int mOffset, float x, float y, float z)
    {
        for (int index = 0; index < 4; index++)
        {
            m[mOffset + 12 + index] += m[mOffset + index] * x + m[mOffset + 4 + index] * y + m[mOffset + 8 + index] * z;
        }
    }

    public static void rotateM(float[] rm, int rmOffset, float[] m, int mOffset, float a, float x, float y, float z)
    {
        float[] rotation = new float[16];
        setRotateM(rotation, 0, a, x, y, z);
        multiplyMM(rm, rmOffset, m, mOffset, rotation, 0);
    }

    public static void rotateM(float[] m, int mOffset, float a, float x, float y, float z)
    {
        float[] rotation = new float[16];
        setRotateM(rotation, 0, a, x, y, z);
        multiplyMM(m, mOffset, m, mOffset, rotation, 0);
    }

    public static void setRotateM(float[] rm, int rmOffset, float a, float x, float y, float z)
    {
        rm[rmOffset + 3] = 0.0f;
        rm[rmOffset + 7] = 0.0f;
        rm[rmOffset + 11] = 0.0f;
        rm[rmOffset + 12] = 0.0f;
        rm[rmOffset + 13] = 0.0f;
        rm[rmOffset + 14] = 0.0f;
        rm[rmOffset + 15] = 1.0f;

        double radians = a * (Math.PI / 180.0);
        float sine = (float) Math.sin(radians);
        float cosine = (float) Math.cos(radians);

        if (x == 1.0f && y == 0.0f && z == 0.0f)
        {
            rm[rmOffset] = 1.0f;
            rm[rmOffset + 1] = 0.0f;
            rm[rmOffset + 2] = 0.0f;
            rm[rmOffset + 4] = 0.0f;
            rm[rmOffset + 5] = cosine;
            rm[rmOffset + 6] = sine;
            rm[rmOffset + 8] = 0.0f;
            rm[rmOffset + 9] = -sine;
            rm[rmOffset + 10] = cosine;
        }
        else if (x == 0.0f && y == 1.0f && z == 0.0f)
        {
            rm[rmOffset] = cosine;
            rm[rmOffset + 1] = 0.0f;
            rm[rmOffset + 2] = -sine;
            rm[rmOffset + 4] = 0.0f;
            rm[rmOffset + 5] = 1.0f;
            rm[rmOffset + 6] = 0.0f;
            rm[rmOffset + 8] = sine;
            rm[rmOffset + 9] = 0.0f;
            rm[rmOffset + 10] = cosine;
        }
        else if (x == 0.0f && y == 0.0f && z == 1.0f)
        {
            rm[rmOffset] = cosine;
            rm[rmOffset + 1] = sine;
            rm[rmOffset + 2] = 0.0f;
            rm[rmOffset + 4] = -sine;
            rm[rmOffset + 5] = cosine;
            rm[rmOffset + 6] = 0.0f;
            rm[rmOffset + 8] = 0.0f;
            rm[rmOffset + 9] = 0.0f;
            rm[rmOffset + 10] = 1.0f;
        }
        else
        {
            float length = length(x, y, z);
            if (length != 1.0f)
            {
                float reciprocal = 1.0f / length;
                x *= reciprocal;
                y *= reciprocal;
                z *= reciprocal;
            }

            float inverseCosine = 1.0f - cosine;
            float xy = x * y;
            float yz = y * z;
            float zx = z * x;
            float xSine = x * sine;
            float ySine = y * sine;
            float zSine = z * sine;

            rm[rmOffset] = x * x * inverseCosine + cosine;
            rm[rmOffset + 4] = xy * inverseCosine - zSine;
            rm[rmOffset + 8] = zx * inverseCosine + ySine;
            rm[rmOffset + 1] = xy * inverseCosine + zSine;
            rm[rmOffset + 5] = y * y * inverseCosine + cosine;
            rm[rmOffset + 9] = yz * inverseCosine - xSine;
            rm[rmOffset + 2] = zx * inverseCosine - ySine;
            rm[rmOffset + 6] = yz * inverseCosine + xSine;
            rm[rmOffset + 10] = z * z * inverseCosine + cosine;
        }
    }

    @Deprecated
    public static void setRotateEulerM(float[] rm, int rmOffset, float x, float y, float z)
    {
        double scale = Math.PI / 180.0;
        float cosineX = (float) Math.cos(x * scale);
        float sineX = (float) Math.sin(x * scale);
        float cosineY = (float) Math.cos(y * scale);
        float sineY = (float) Math.sin(y * scale);
        float cosineZ = (float) Math.cos(z * scale);
        float sineZ = (float) Math.sin(z * scale);
        float cosineXSineY = cosineX * sineY;
        float sineXSineY = sineX * sineY;

        rm[rmOffset] = cosineY * cosineZ;
        rm[rmOffset + 1] = -cosineY * sineZ;
        rm[rmOffset + 2] = sineY;
        rm[rmOffset + 3] = 0.0f;

        rm[rmOffset + 4] = cosineXSineY * cosineZ + cosineX * sineZ;
        rm[rmOffset + 5] = -cosineXSineY * sineZ + cosineX * cosineZ;
        rm[rmOffset + 6] = -sineX * cosineY;
        rm[rmOffset + 7] = 0.0f;

        rm[rmOffset + 8] = -sineXSineY * cosineZ + sineX * sineZ;
        rm[rmOffset + 9] = sineXSineY * sineZ + sineX * cosineZ;
        rm[rmOffset + 10] = cosineX * cosineY;
        rm[rmOffset + 11] = 0.0f;

        rm[rmOffset + 12] = 0.0f;
        rm[rmOffset + 13] = 0.0f;
        rm[rmOffset + 14] = 0.0f;
        rm[rmOffset + 15] = 1.0f;
    }

    public static void setRotateEulerM2(float[] rm, int rmOffset, float x, float y, float z)
    {
        if (rm == null) throw new IllegalArgumentException("rm == null");
        if (rmOffset < 0) throw new IllegalArgumentException("rmOffset < 0");
        if (rm.length < rmOffset + 16) throw new IllegalArgumentException("rm.length < rmOffset + 16");

        double scale = Math.PI / 180.0;
        float cosineX = (float) Math.cos(x * scale);
        float sineX = (float) Math.sin(x * scale);
        float cosineY = (float) Math.cos(y * scale);
        float sineY = (float) Math.sin(y * scale);
        float cosineZ = (float) Math.cos(z * scale);
        float sineZ = (float) Math.sin(z * scale);
        float cosineXSineY = cosineX * sineY;
        float sineXSineY = sineX * sineY;

        rm[rmOffset] = cosineY * cosineZ;
        rm[rmOffset + 1] = cosineY * sineZ;
        rm[rmOffset + 2] = -sineY;
        rm[rmOffset + 3] = 0.0f;

        rm[rmOffset + 4] = sineXSineY * cosineZ - cosineX * sineZ;
        rm[rmOffset + 5] = sineXSineY * sineZ + cosineX * cosineZ;
        rm[rmOffset + 6] = sineX * cosineY;
        rm[rmOffset + 7] = 0.0f;

        rm[rmOffset + 8] = cosineXSineY * cosineZ + sineX * sineZ;
        rm[rmOffset + 9] = cosineXSineY * sineZ - sineX * cosineZ;
        rm[rmOffset + 10] = cosineX * cosineY;
        rm[rmOffset + 11] = 0.0f;

        rm[rmOffset + 12] = 0.0f;
        rm[rmOffset + 13] = 0.0f;
        rm[rmOffset + 14] = 0.0f;
        rm[rmOffset + 15] = 1.0f;
    }

    public static void setLookAtM(float[] rm, int rmOffset, float eyeX, float eyeY, float eyeZ, float centerX, float centerY,
            float centerZ, float upX, float upY, float upZ)
    {
        float forwardX = centerX - eyeX;
        float forwardY = centerY - eyeY;
        float forwardZ = centerZ - eyeZ;

        float reciprocalForward = 1.0f / length(forwardX, forwardY, forwardZ);
        forwardX *= reciprocalForward;
        forwardY *= reciprocalForward;
        forwardZ *= reciprocalForward;

        float sideX = forwardY * upZ - forwardZ * upY;
        float sideY = forwardZ * upX - forwardX * upZ;
        float sideZ = forwardX * upY - forwardY * upX;

        float reciprocalSide = 1.0f / length(sideX, sideY, sideZ);
        sideX *= reciprocalSide;
        sideY *= reciprocalSide;
        sideZ *= reciprocalSide;

        float upwardX = sideY * forwardZ - sideZ * forwardY;
        float upwardY = sideZ * forwardX - sideX * forwardZ;
        float upwardZ = sideX * forwardY - sideY * forwardX;

        rm[rmOffset] = sideX;
        rm[rmOffset + 1] = upwardX;
        rm[rmOffset + 2] = -forwardX;
        rm[rmOffset + 3] = 0.0f;

        rm[rmOffset + 4] = sideY;
        rm[rmOffset + 5] = upwardY;
        rm[rmOffset + 6] = -forwardY;
        rm[rmOffset + 7] = 0.0f;

        rm[rmOffset + 8] = sideZ;
        rm[rmOffset + 9] = upwardZ;
        rm[rmOffset + 10] = -forwardZ;
        rm[rmOffset + 11] = 0.0f;

        rm[rmOffset + 12] = 0.0f;
        rm[rmOffset + 13] = 0.0f;
        rm[rmOffset + 14] = 0.0f;
        rm[rmOffset + 15] = 1.0f;

        translateM(rm, rmOffset, -eyeX, -eyeY, -eyeZ);
    }

    private static void clear(float[] m, int offset)
    {
        for (int index = 0; index < 16; index++) m[offset + index] = 0.0f;
    }
}
