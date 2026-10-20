package android.graphics;

public class PorterDuffXfermode extends Xfermode
{
    private final PorterDuff.Mode mMode;

    public PorterDuffXfermode(PorterDuff.Mode mode)
    {
        mMode = mode;
    }

    public PorterDuff.Mode getMode()
    {
        return mMode;
    }

    BlendMode blendMode()
    {
        return BlendMode.fromPorterDuff(mMode);
    }
}
