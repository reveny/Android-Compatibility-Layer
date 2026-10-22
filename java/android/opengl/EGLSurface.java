package android.opengl;

public class EGLSurface extends EGLObjectHandle
{
    EGLSurface(long handle)
    {
        super(handle);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof EGLSurface)) return false;

        return getNativeHandle() == ((EGLSurface) other).getNativeHandle();
    }
}
