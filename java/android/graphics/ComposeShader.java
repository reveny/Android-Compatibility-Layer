package android.graphics;

public class ComposeShader extends Shader
{
    private final Shader mShaderA;
    private final Shader mShaderB;
    private final BlendMode mBlendMode;

    public ComposeShader(Shader shaderA, Shader shaderB, Xfermode mode)
    {
        this(shaderA, shaderB, mode instanceof PorterDuffXfermode ? ((PorterDuffXfermode) mode).blendMode() : BlendMode.SRC_OVER);
    }

    public ComposeShader(Shader shaderA, Shader shaderB, PorterDuff.Mode mode)
    {
        this(shaderA, shaderB, BlendMode.fromPorterDuff(mode));
    }

    public ComposeShader(Shader shaderA, Shader shaderB, BlendMode mode)
    {
        if (shaderA == null || shaderB == null) throw new IllegalArgumentException("Shader parameters must not be null");

        mShaderA = shaderA;
        mShaderB = shaderB;
        mBlendMode = mode == null ? BlendMode.SRC_OVER : mode;
    }

    private Shader visible()
    {
        switch (mBlendMode)
        {
            case SRC:
            case SRC_OVER:
            case SRC_IN:
            case SRC_OUT:
            case SCREEN:
            case PLUS:
            case LIGHTEN:
                return mShaderB;

            default:
                return mShaderA;
        }
    }

    @Override
    Matrix localMatrix()
    {
        Matrix own = super.localMatrix();
        Matrix inner = visible().localMatrix();

        if (own == null) return inner;
        if (inner == null) return own;

        Matrix combined = new Matrix();
        combined.setConcat(own, inner);
        return combined;
    }

    @Override
    int kind()
    {
        return visible().kind();
    }

    @Override
    int tileModeId()
    {
        return visible().tileModeId();
    }

    @Override
    float startX()
    {
        return visible().startX();
    }

    @Override
    float startY()
    {
        return visible().startY();
    }

    @Override
    float endX()
    {
        return visible().endX();
    }

    @Override
    float endY()
    {
        return visible().endY();
    }

    @Override
    float radius()
    {
        return visible().radius();
    }

    @Override
    int[] stopColors()
    {
        return visible().stopColors();
    }

    @Override
    float[] stopPositions()
    {
        return visible().stopPositions();
    }

    @Override
    long bitmapHandle()
    {
        return visible().bitmapHandle();
    }
}
