package javax.microedition.khronos.opengles;

public interface GL10 extends GL
{
    int GL_DEPTH_BUFFER_BIT = 0x00000100;
    int GL_STENCIL_BUFFER_BIT = 0x00000400;
    int GL_COLOR_BUFFER_BIT = 0x00004000;

    int GL_FALSE = 0;
    int GL_TRUE = 1;

    int GL_NO_ERROR = 0;

    int GL_VENDOR = 0x1F00;
    int GL_RENDERER = 0x1F01;
    int GL_VERSION = 0x1F02;
    int GL_EXTENSIONS = 0x1F03;

    int GL_MAX_TEXTURE_SIZE = 0x0D33;

    void glClear(int mask);
    void glClearColor(float red, float green, float blue, float alpha);
    void glViewport(int x, int y, int width, int height);
    void glFinish();
    void glFlush();
    int glGetError();
    String glGetString(int name);
}
