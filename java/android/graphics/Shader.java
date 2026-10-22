package android.graphics;

public class Shader
{
    static final int KIND_NONE = 0;
    static final int KIND_LINEAR = 1;
    static final int KIND_RADIAL = 2;
    static final int KIND_SWEEP = 3;
    static final int KIND_BITMAP = 4;

    private Matrix mLocalMatrix;

    public boolean getLocalMatrix(Matrix into)
    {
        if (into == null || mLocalMatrix == null) return false;

        into.set(mLocalMatrix);
        return true;
    }

    public void setLocalMatrix(Matrix matrix)
    {
        mLocalMatrix = matrix == null ? null : new Matrix(matrix);
    }

    Matrix localMatrix()
    {
        return mLocalMatrix;
    }

    int kind()
    {
        return KIND_NONE;
    }

    int tileModeId()
    {
        return 0;
    }

    float startX()
    {
        return 0;
    }

    float startY()
    {
        return 0;
    }

    float endX()
    {
        return 0;
    }

    float endY()
    {
        return 0;
    }

    float radius()
    {
        return 0;
    }

    int[] stopColors()
    {
        return null;
    }

    float[] stopPositions()
    {
        return null;
    }

    long bitmapHandle()
    {
        return 0;
    }

    static int tileModeId(TileMode mode)
    {
        if (mode == TileMode.REPEAT) return 1;
        if (mode == TileMode.MIRROR) return 2;

        return 0;
    }

    static float[] evenPositions(int count)
    {
        float[] positions = new float[count];

        for (int index = 0; index < count; index++)
        {
            positions[index] = count == 1 ? 0 : index / (float) (count - 1);
        }

        return positions;
    }

    static int[] argbColors(long[] colors)
    {
        if (colors == null) throw new NullPointerException("colors must not be null");

        int[] converted = new int[colors.length];
        for (int index = 0; index < colors.length; index++) converted[index] = Color.toArgb(colors[index]);

        return converted;
    }

    public enum TileMode
    {
        CLAMP,
        REPEAT,
        MIRROR,
        DECAL
    }
}
