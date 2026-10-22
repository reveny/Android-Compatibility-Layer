package android.opengl;

public class EGLImage extends EGLObjectHandle
{
    EGLImage(long handle)
    {
        super(handle);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof EGLImage)) return false;

        return getNativeHandle() == ((EGLImage) other).getNativeHandle();
    }
}
