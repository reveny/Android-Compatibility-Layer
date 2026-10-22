package android.graphics;

public class ColorMatrixColorFilter extends ColorFilter
{
    private final ColorMatrix mMatrix = new ColorMatrix();

    public ColorMatrixColorFilter(ColorMatrix matrix)
    {
        if (matrix != null) mMatrix.set(matrix);
    }

    public ColorMatrixColorFilter(float[] array)
    {
        if (array.length < 20) throw new ArrayIndexOutOfBoundsException();

        mMatrix.set(array);
    }

    public void getColorMatrix(ColorMatrix colorMatrix)
    {
        if (colorMatrix != null) colorMatrix.set(mMatrix);
    }

    @Override
    int apply(int source)
    {
        return ColorMatrix.transform(mMatrix.getArray(), source);
    }

    @Override
    boolean tintsImages()
    {
        float[] matrix = mMatrix.getArray();

        for (int row = 0; row < 15; row += 5)
        {
            if (matrix[row] != 0 || matrix[row + 1] != 0 || matrix[row + 2] != 0 || matrix[row + 3] != 0) return false;
        }

        return matrix[15] == 0 && matrix[16] == 0 && matrix[17] == 0 && matrix[19] == 0;
    }

    @Override
    int imageTint()
    {
        float[] matrix = mMatrix.getArray();
        int alpha = clamp(matrix[18] * 255);

        return (alpha << 24) | (clamp(matrix[4]) << 16) | (clamp(matrix[9]) << 8) | clamp(matrix[14]);
    }

    private static int clamp(float value)
    {
        int rounded = Math.round(value);
        return rounded < 0 ? 0 : (rounded > 255 ? 255 : rounded);
    }
}
