package android.graphics;

public final class RenderEffect
{
    static final int KIND_OFFSET = 0;
    static final int KIND_BLUR = 1;
    static final int KIND_BITMAP = 2;
    static final int KIND_COLOR_FILTER = 3;
    static final int KIND_BLEND_MODE = 4;
    static final int KIND_CHAIN = 5;
    static final int KIND_SHADER = 6;
    static final int KIND_RUNTIME_SHADER = 7;

    final int kind;
    final float first;
    final float second;
    final RenderEffect input;
    final RenderEffect other;
    final Object payload;

    RenderEffect()
    {
        this(KIND_OFFSET, 0f, 0f, null, null, null);
    }

    private RenderEffect(int kind, float first, float second, RenderEffect input, RenderEffect other, Object payload)
    {
        this.kind = kind;
        this.first = first;
        this.second = second;
        this.input = input;
        this.other = other;
        this.payload = payload;
    }

    public static RenderEffect createOffsetEffect(float offsetX, float offsetY)
    {
        return new RenderEffect(KIND_OFFSET, offsetX, offsetY, null, null, null);
    }

    public static RenderEffect createOffsetEffect(float offsetX, float offsetY, RenderEffect input)
    {
        return new RenderEffect(KIND_OFFSET, offsetX, offsetY, input, null, null);
    }

    public static RenderEffect createBlurEffect(float radiusX, float radiusY, RenderEffect inputEffect, Shader.TileMode edgeTreatment)
    {
        return new RenderEffect(KIND_BLUR, radiusX, radiusY, inputEffect, null, edgeTreatment);
    }

    public static RenderEffect createBlurEffect(float radiusX, float radiusY, Shader.TileMode edgeTreatment)
    {
        return new RenderEffect(KIND_BLUR, radiusX, radiusY, null, null, edgeTreatment);
    }

    public static RenderEffect createBitmapEffect(Bitmap bitmap)
    {
        return new RenderEffect(KIND_BITMAP, 0f, 0f, null, null, bitmap);
    }

    public static RenderEffect createBitmapEffect(Bitmap bitmap, Rect src, Rect dst)
    {
        return new RenderEffect(KIND_BITMAP, 0f, 0f, null, null, bitmap);
    }

    public static RenderEffect createColorFilterEffect(ColorFilter colorFilter, RenderEffect renderEffect)
    {
        return new RenderEffect(KIND_COLOR_FILTER, 0f, 0f, renderEffect, null, colorFilter);
    }

    public static RenderEffect createColorFilterEffect(ColorFilter colorFilter)
    {
        return new RenderEffect(KIND_COLOR_FILTER, 0f, 0f, null, null, colorFilter);
    }

    public static RenderEffect createBlendModeEffect(RenderEffect dst, RenderEffect src, BlendMode blendMode)
    {
        return new RenderEffect(KIND_BLEND_MODE, 0f, 0f, dst, src, blendMode);
    }

    public static RenderEffect createChainEffect(RenderEffect outer, RenderEffect inner)
    {
        return new RenderEffect(KIND_CHAIN, 0f, 0f, inner, outer, null);
    }

    public static RenderEffect createShaderEffect(Shader shader)
    {
        return new RenderEffect(KIND_SHADER, 0f, 0f, null, null, shader);
    }

    public static RenderEffect createRuntimeShaderEffect(RuntimeShader shader, String uniformShaderName)
    {
        if (shader == null) throw new NullPointerException("The shader parameter must not be null");
        if (uniformShaderName == null) throw new NullPointerException("The uniformShaderName parameter must not be null");

        return new RenderEffect(KIND_RUNTIME_SHADER, 0f, 0f, null, null, shader);
    }
}
