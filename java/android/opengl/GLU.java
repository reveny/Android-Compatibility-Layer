package android.opengl;

import javax.microedition.khronos.opengles.GL10;

public class GLU
{
    private static final int PRODUCT_OFFSET = 0;
    private static final int INVERSE_OFFSET = 16;
    private static final int VECTOR_OFFSET = 32;
    private static final int RESULT_OFFSET = 36;

    private static final ThreadLocal<float[]> sScratch = new ThreadLocal<float[]>()
    {
        @Override
        protected float[] initialValue()
        {
            return new float[40];
        }
    };

    public GLU()
    {
    }

    public static String gluErrorString(int error)
    {
        switch (error)
        {
            case GL10.GL_NO_ERROR:
                return "no error";
            case GL10.GL_INVALID_ENUM:
                return "invalid enum";
            case GL10.GL_INVALID_VALUE:
                return "invalid value";
            case GL10.GL_INVALID_OPERATION:
                return "invalid operation";
            case GL10.GL_STACK_OVERFLOW:
                return "stack overflow";
            case GL10.GL_STACK_UNDERFLOW:
                return "stack underflow";
            case GL10.GL_OUT_OF_MEMORY:
                return "out of memory";
            default:
                return null;
        }
    }

    public static void gluLookAt(GL10 gl, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX,
            float upY, float upZ)
    {
        float[] scratch = sScratch.get();
        Matrix.setLookAtM(scratch, PRODUCT_OFFSET, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        gl.glMultMatrixf(scratch, PRODUCT_OFFSET);
    }

    public static void gluOrtho2D(GL10 gl, float left, float right, float bottom, float top)
    {
        gl.glOrthof(left, right, bottom, top, -1.0f, 1.0f);
    }

    public static void gluPerspective(GL10 gl, float fovy, float aspect, float zNear, float zFar)
    {
        float top = zNear * (float) Math.tan(fovy * (Math.PI / 360.0));
        float bottom = -top;
        float left = bottom * aspect;
        float right = top * aspect;
        gl.glFrustumf(left, right, bottom, top, zNear, zFar);
    }

    public static int gluProject(float objX, float objY, float objZ, float[] model, int modelOffset, float[] project, int projectOffset,
            int[] view, int viewOffset, float[] win, int winOffset)
    {
        float[] scratch = sScratch.get();
        Matrix.multiplyMM(scratch, PRODUCT_OFFSET, project, projectOffset, model, modelOffset);

        scratch[VECTOR_OFFSET] = objX;
        scratch[VECTOR_OFFSET + 1] = objY;
        scratch[VECTOR_OFFSET + 2] = objZ;
        scratch[VECTOR_OFFSET + 3] = 1.0f;
        Matrix.multiplyMV(scratch, RESULT_OFFSET, scratch, PRODUCT_OFFSET, scratch, VECTOR_OFFSET);

        float w = scratch[RESULT_OFFSET + 3];
        if (w == 0.0f) return GL10.GL_FALSE;

        float reciprocal = 1.0f / w;
        win[winOffset] = view[viewOffset] + view[viewOffset + 2] * (scratch[RESULT_OFFSET] * reciprocal + 1.0f) * 0.5f;
        win[winOffset + 1] = view[viewOffset + 1] + view[viewOffset + 3] * (scratch[RESULT_OFFSET + 1] * reciprocal + 1.0f) * 0.5f;
        win[winOffset + 2] = (scratch[RESULT_OFFSET + 2] * reciprocal + 1.0f) * 0.5f;
        return GL10.GL_TRUE;
    }

    public static int gluUnProject(float winX, float winY, float winZ, float[] model, int modelOffset, float[] project,
            int projectOffset, int[] view, int viewOffset, float[] obj, int objOffset)
    {
        float[] scratch = sScratch.get();
        Matrix.multiplyMM(scratch, PRODUCT_OFFSET, project, projectOffset, model, modelOffset);
        if (!Matrix.invertM(scratch, INVERSE_OFFSET, scratch, PRODUCT_OFFSET)) return GL10.GL_FALSE;

        scratch[VECTOR_OFFSET] = 2.0f * (winX - view[viewOffset]) / view[viewOffset + 2] - 1.0f;
        scratch[VECTOR_OFFSET + 1] = 2.0f * (winY - view[viewOffset + 1]) / view[viewOffset + 3] - 1.0f;
        scratch[VECTOR_OFFSET + 2] = 2.0f * winZ - 1.0f;
        scratch[VECTOR_OFFSET + 3] = 1.0f;
        Matrix.multiplyMV(obj, objOffset, scratch, INVERSE_OFFSET, scratch, VECTOR_OFFSET);
        return GL10.GL_TRUE;
    }
}
