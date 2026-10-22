package android.opengl;

public class EGLConfig extends EGLObjectHandle
{
    EGLConfig(long handle)
    {
        super(handle);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof EGLConfig)) return false;

        return getNativeHandle() == ((EGLConfig) other).getNativeHandle();
    }
}
