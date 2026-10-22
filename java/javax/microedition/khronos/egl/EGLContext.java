package javax.microedition.khronos.egl;

import javax.microedition.khronos.opengles.GL;

public abstract class EGLContext
{
    public EGLContext()
    {
    }

    public static EGL getEGL()
    {
        return RuntimeEgl.INSTANCE;
    }

    public abstract GL getGL();
}
