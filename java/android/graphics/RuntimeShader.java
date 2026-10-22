package android.graphics;

import java.util.HashMap;
import java.util.Map;

public class RuntimeShader extends Shader
{
    private final String mShaderSource;
    private final Map<String, float[]> mFloatUniforms = new HashMap<>();
    private final Map<String, int[]> mIntUniforms = new HashMap<>();
    private final Map<String, Shader> mInputShaders = new HashMap<>();

    public RuntimeShader(String shader)
    {
        if (shader == null) throw new NullPointerException("RuntimeShader requires a non-null AGSL string");

        mShaderSource = shader;
    }

    String source()
    {
        return mShaderSource;
    }

    private static void requireName(String uniformName)
    {
        if (uniformName == null) throw new NullPointerException("The uniformName parameter must not be null");
    }

    public void setColorUniform(String uniformName, int color)
    {
        setFloatUniform(uniformName, Color.red(color) / 255.0f, Color.green(color) / 255.0f, Color.blue(color) / 255.0f,
                        Color.alpha(color) / 255.0f);
    }

    public void setColorUniform(String uniformName, long color)
    {
        setFloatUniform(uniformName, Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color));
    }

    public void setColorUniform(String uniformName, Color color)
    {
        if (color == null) throw new NullPointerException("The color parameter must not be null");

        setFloatUniform(uniformName, color.red(), color.green(), color.blue(), color.alpha());
    }

    public void setFloatUniform(String uniformName, float value)
    {
        setFloatUniform(uniformName, new float[] {value});
    }

    public void setFloatUniform(String uniformName, float value1, float value2)
    {
        setFloatUniform(uniformName, new float[] {value1, value2});
    }

    public void setFloatUniform(String uniformName, float value1, float value2, float value3)
    {
        setFloatUniform(uniformName, new float[] {value1, value2, value3});
    }

    public void setFloatUniform(String uniformName, float value1, float value2, float value3, float value4)
    {
        setFloatUniform(uniformName, new float[] {value1, value2, value3, value4});
    }

    public void setFloatUniform(String uniformName, float[] values)
    {
        requireName(uniformName);
        if (values == null) throw new NullPointerException("The uniform values parameter must not be null");

        mFloatUniforms.put(uniformName, values.clone());
    }

    public void setIntUniform(String uniformName, int value)
    {
        setIntUniform(uniformName, new int[] {value});
    }

    public void setIntUniform(String uniformName, int value1, int value2)
    {
        setIntUniform(uniformName, new int[] {value1, value2});
    }

    public void setIntUniform(String uniformName, int value1, int value2, int value3)
    {
        setIntUniform(uniformName, new int[] {value1, value2, value3});
    }

    public void setIntUniform(String uniformName, int value1, int value2, int value3, int value4)
    {
        setIntUniform(uniformName, new int[] {value1, value2, value3, value4});
    }

    public void setIntUniform(String uniformName, int[] values)
    {
        requireName(uniformName);
        if (values == null) throw new NullPointerException("The uniform values parameter must not be null");

        mIntUniforms.put(uniformName, values.clone());
    }

    public void setInputShader(String shaderName, Shader shader)
    {
        if (shaderName == null) throw new NullPointerException("The shaderName parameter must not be null");
        if (shader == null) throw new NullPointerException("The shader parameter must not be null");

        mInputShaders.put(shaderName, shader);
    }

    public void setInputBuffer(String shaderName, BitmapShader shader)
    {
        setInputShader(shaderName, shader);
    }

    @Override
    int kind()
    {
        Shader first = firstBitmapInput();
        return first == null ? KIND_NONE : first.kind();
    }

    @Override
    long bitmapHandle()
    {
        Shader first = firstBitmapInput();
        return first == null ? 0 : first.bitmapHandle();
    }

    @Override
    int tileModeId()
    {
        Shader first = firstBitmapInput();
        return first == null ? 0 : first.tileModeId();
    }

    @Override
    Matrix localMatrix()
    {
        Shader first = firstBitmapInput();
        Matrix own = super.localMatrix();

        if (first == null || first.localMatrix() == null) return own;
        if (own == null) return first.localMatrix();

        Matrix combined = new Matrix(first.localMatrix());
        combined.postConcat(own);
        return combined;
    }

    private Shader firstBitmapInput()
    {
        for (Shader input : mInputShaders.values())
        {
            if (input instanceof BitmapShader) return input;
        }

        return null;
    }
}
