package android.graphics;

import java.util.Arrays;

public class ColorMatrix
{
    private final float[] mArray = new float[20];

    public ColorMatrix()
    {
        reset();
    }

    public ColorMatrix(float[] source)
    {
        System.arraycopy(source, 0, mArray, 0, 20);
    }

    public ColorMatrix(ColorMatrix source)
    {
        System.arraycopy(source.mArray, 0, mArray, 0, 20);
    }

    public final float[] getArray()
    {
        return mArray;
    }

    public void reset()
    {
        Arrays.fill(mArray, 0);
        mArray[0] = mArray[6] = mArray[12] = mArray[18] = 1;
    }

    public void set(ColorMatrix source)
    {
        System.arraycopy(source.mArray, 0, mArray, 0, 20);
    }

    public void set(float[] source)
    {
        System.arraycopy(source, 0, mArray, 0, 20);
    }

    public void setScale(float redScale, float greenScale, float blueScale, float alphaScale)
    {
        Arrays.fill(mArray, 0);
        mArray[0] = redScale;
        mArray[6] = greenScale;
        mArray[12] = blueScale;
        mArray[18] = alphaScale;
    }

    public void setRotate(int axis, float degrees)
    {
        reset();

        double radians = degrees * Math.PI / 180;
        float cosine = (float)Math.cos(radians);
        float sine = (float)Math.sin(radians);

        switch (axis)
        {
            case 0:
                mArray[6] = mArray[12] = cosine;
                mArray[7] = sine;
                mArray[11] = -sine;
                break;

            case 1:
                mArray[0] = mArray[12] = cosine;
                mArray[2] = -sine;
                mArray[10] = sine;
                break;

            case 2:
                mArray[0] = mArray[6] = cosine;
                mArray[1] = sine;
                mArray[5] = -sine;
                break;

            default:
                throw new RuntimeException();
        }
    }

    public void setConcat(ColorMatrix first, ColorMatrix second)
    {
        float[] product = first == this || second == this ? new float[20] : mArray;
        float[] left = first.mArray;
        float[] right = second.mArray;
        int index = 0;

        for (int row = 0; row < 20; row += 5)
        {
            for (int column = 0; column < 4; column++)
            {
                product[index++] = left[row] * right[column] + left[row + 1] * right[column + 5]
                                   + left[row + 2] * right[column + 10] + left[row + 3] * right[column + 15];
            }

            product[index++] = left[row] * right[4] + left[row + 1] * right[9] + left[row + 2] * right[14]
                               + left[row + 3] * right[19] + left[row + 4];
        }

        if (product != mArray) System.arraycopy(product, 0, mArray, 0, 20);
    }

    public void preConcat(ColorMatrix prematrix)
    {
        setConcat(this, prematrix);
    }

    public void postConcat(ColorMatrix postmatrix)
    {
        setConcat(postmatrix, this);
    }

    public void setSaturation(float saturation)
    {
        reset();

        float inverse = 1 - saturation;
        float red = 0.213f * inverse;
        float green = 0.715f * inverse;
        float blue = 0.072f * inverse;

        mArray[0] = red + saturation;
        mArray[1] = green;
        mArray[2] = blue;
        mArray[5] = red;
        mArray[6] = green + saturation;
        mArray[7] = blue;
        mArray[10] = red;
        mArray[11] = green;
        mArray[12] = blue + saturation;
    }

    public void setRGB2YUV()
    {
        reset();

        mArray[0] = 0.299f;
        mArray[1] = 0.587f;
        mArray[2] = 0.114f;
        mArray[5] = -0.16874f;
        mArray[6] = -0.33126f;
        mArray[7] = 0.5f;
        mArray[10] = 0.5f;
        mArray[11] = -0.41869f;
        mArray[12] = -0.08131f;
    }

    public void setYUV2RGB()
    {
        reset();

        mArray[2] = 1.402f;
        mArray[5] = 1;
        mArray[6] = -0.34414f;
        mArray[7] = -0.71414f;
        mArray[10] = 1;
        mArray[11] = 1.772f;
        mArray[12] = 0;
    }

    static int transform(float[] matrix, int color)
    {
        float red = (color >> 16) & 0xFF;
        float green = (color >> 8) & 0xFF;
        float blue = color & 0xFF;
        float alpha = (color >>> 24) & 0xFF;

        int outRed = clamp(matrix[0] * red + matrix[1] * green + matrix[2] * blue + matrix[3] * alpha + matrix[4]);
        int outGreen = clamp(matrix[5] * red + matrix[6] * green + matrix[7] * blue + matrix[8] * alpha + matrix[9]);
        int outBlue = clamp(matrix[10] * red + matrix[11] * green + matrix[12] * blue + matrix[13] * alpha + matrix[14]);
        int outAlpha = clamp(matrix[15] * red + matrix[16] * green + matrix[17] * blue + matrix[18] * alpha + matrix[19]);

        return (outAlpha << 24) | (outRed << 16) | (outGreen << 8) | outBlue;
    }

    private static int clamp(float value)
    {
        int rounded = Math.round(value);
        return rounded < 0 ? 0 : (rounded > 255 ? 255 : rounded);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ColorMatrix)) return false;

        return Arrays.equals(mArray, ((ColorMatrix) other).mArray);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(mArray);
    }
}
