package android.opengl;

public class EGLSync extends EGLObjectHandle
{
    EGLSync(long handle)
    {
        super(handle);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof EGLSync)) return false;

        return getNativeHandle() == ((EGLSync) other).getNativeHandle();
    }
}
