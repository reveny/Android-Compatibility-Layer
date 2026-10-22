package android.opengl;

import java.io.Writer;

import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.opengles.GL;

public class GLDebugHelper
{
    public static final int CONFIG_CHECK_GL_ERROR = 1;
    public static final int CONFIG_CHECK_THREAD = 2;
    public static final int CONFIG_LOG_ARGUMENT_NAMES = 4;
    public static final int ERROR_WRONG_THREAD = 0x7000;

    public GLDebugHelper()
    {
    }

    public static GL wrap(GL gl, int configFlags, Writer log)
    {
        return gl;
    }

    public static EGL wrap(EGL egl, int configFlags, Writer log)
    {
        return egl;
    }
}
