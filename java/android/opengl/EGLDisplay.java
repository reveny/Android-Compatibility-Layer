package android.opengl;

public class EGLDisplay extends EGLObjectHandle
{
    EGLDisplay(long handle)
    {
        super(handle);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof EGLDisplay)) return false;

        return getNativeHandle() == ((EGLDisplay) other).getNativeHandle();
    }
}
