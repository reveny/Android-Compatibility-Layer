package android.graphics;

public class SweepGradient extends Shader
{
    private final float mCentreX;
    private final float mCentreY;
    private final int[] mColors;
    private final float[] mPositions;

    public SweepGradient(float centreX, float centreY, int[] colors, float[] positions)
    {
        if (colors == null || colors.length < 2) throw new IllegalArgumentException("needs at least two colors");

        mCentreX = centreX;
        mCentreY = centreY;
        mColors = colors.clone();
        mPositions = positions == null ? evenPositions(colors.length) : positions.clone();
    }

    public SweepGradient(float centreX, float centreY, int startColor, int endColor)
    {
        this(centreX, centreY, new int[] {startColor, endColor}, null);
    }

    @Override
    int kind()
    {
        return KIND_SWEEP;
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
