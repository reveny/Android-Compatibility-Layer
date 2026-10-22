package android.opengl;

public class EGLContext extends EGLObjectHandle
{
    EGLContext(long handle)
    {
        super(handle);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof EGLContext)) return false;

        return getNativeHandle() == ((EGLContext) other).getNativeHandle();
    }
}
