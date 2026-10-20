package android.opengl;

import javax.microedition.khronos.opengles.GL10;

final class RuntimeGl10 implements GL10
{
    static final RuntimeGl10 INSTANCE = new RuntimeGl10();

    private RuntimeGl10()
    {
    }

    @Override
    public void glClear(int mask)
    {
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha)
    {
    }

    @Override
    public void glViewport(int x, int y, int width, int height)
    {
    }

    @Override
    public void glFinish()
    {
    }

    @Override
    public void glFlush()
    {
    }

    @Override
    public int glGetError()
    {
        return GL_NO_ERROR;
    }

    @Override
    public String glGetString(int name)
    {
        switch (name)
        {
            case GL_VENDOR:
                return runtime.PlatformConfig.GPU_VENDOR;
            case GL_RENDERER:
                return runtime.PlatformConfig.GPU_RENDERER;
            case GL_VERSION:
                return "OpenGL ES " + runtime.PlatformConfig.GL_ES_VERSION;
            case GL_EXTENSIONS:
                return "";
            default:
                return null;
        }
    }
}
