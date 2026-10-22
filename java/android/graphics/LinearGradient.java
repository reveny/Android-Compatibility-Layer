package android.graphics;

public class LinearGradient extends Shader
{
    private final float mStartX;
    private final float mStartY;
    private final float mEndX;
    private final float mEndY;
    private final int[] mColors;
    private final float[] mPositions;
    private final int mTileMode;

    public LinearGradient(float startX, float startY, float endX, float endY, int[] colors, float[] positions,
                          TileMode tileMode)
    {
        if (colors == null || colors.length < 2) throw new IllegalArgumentException("needs at least two colors");

        mStartX = startX;
        mStartY = startY;
        mEndX = endX;
        mEndY = endY;
        mColors = colors.clone();
        mPositions = positions == null ? evenPositions(colors.length) : positions.clone();
        mTileMode = tileModeId(tileMode);
    }

    public LinearGradient(float startX, float startY, float endX, float endY, int startColor, int endColor,
                          TileMode tileMode)
    {
        this(startX, startY, endX, endY, new int[] {startColor, endColor}, null, tileMode);
    }

    public LinearGradient(float startX, float startY, float endX, float endY, long[] colors, float[] positions,
                          TileMode tileMode)
    {
        this(startX, startY, endX, endY, argbColors(colors), positions, tileMode);
    }

    public LinearGradient(float startX, float startY, float endX, float endY, long startColor, long endColor,
                          TileMode tileMode)
    {
        this(startX, startY, endX, endY, new long[] {startColor, endColor}, null, tileMode);
    }

    @Override
    int kind()
    {
        return KIND_LINEAR;
    }

    @Override
    int tileModeId()
    {
        return mTileMode;
    }

    @Override
    float startX()
    {
        return mStartX;
    }

    @Override
    float startY()
    {
        return mStartY;
    }

    @Override
    float endX()
    {
        return mEndX;
    }

    @Override
    float endY()
    {
        return mEndY;
    }

    @Override
    int[] stopColors()
    {
        return mColors;
    }

    @Override
    float[] stopPositions()
    {
        return mPositions;
    }
}
