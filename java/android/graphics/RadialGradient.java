package android.graphics;

public class RadialGradient extends Shader
{
    private final float mCentreX;
    private final float mCentreY;
    private final float mRadius;
    private final int[] mColors;
    private final float[] mPositions;
    private final int mTileMode;

    public RadialGradient(float centreX, float centreY, float radius, int[] colors, float[] positions, TileMode tileMode)
    {
        if (colors == null || colors.length < 2) throw new IllegalArgumentException("needs at least two colors");
        if (radius <= 0) throw new IllegalArgumentException("radius must be positive");

        mCentreX = centreX;
        mCentreY = centreY;
        mRadius = radius;
        mColors = colors.clone();
        mPositions = positions == null ? evenPositions(colors.length) : positions.clone();
        mTileMode = tileModeId(tileMode);
    }

    public RadialGradient(float centreX, float centreY, float radius, int centreColor, int edgeColor, TileMode tileMode)
    {
        this(centreX, centreY, radius, new int[] {centreColor, edgeColor}, null, tileMode);
    }

    @Override
    int kind()
    {
        return KIND_RADIAL;
    }

    @Override
    int tileModeId()
    {
        return mTileMode;
    }

    @Override
    float startX()
    {
        return mCentreX;
    }

    @Override
    float startY()
    {
        return mCentreY;
    }

    @Override
    float radius()
    {
        return mRadius;
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
