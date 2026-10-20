package android.animation;

public class ArgbEvaluator implements TypeEvaluator<Object>
{
    private static final ArgbEvaluator INSTANCE = new ArgbEvaluator();

    public static ArgbEvaluator getInstance()
    {
        return INSTANCE;
    }

    @Override
    public Object evaluate(float fraction, Object startValue, Object endValue)
    {
        int start = startValue instanceof Integer ? ((Integer)startValue).intValue() : 0;
        int end = endValue instanceof Integer ? ((Integer)endValue).intValue() : 0;

        int startAlpha = (start >> 24) & 0xFF;
        int startRed = (start >> 16) & 0xFF;
        int startGreen = (start >> 8) & 0xFF;
        int startBlue = start & 0xFF;

        int endAlpha = (end >> 24) & 0xFF;
        int endRed = (end >> 16) & 0xFF;
        int endGreen = (end >> 8) & 0xFF;
        int endBlue = end & 0xFF;

        int alpha = startAlpha + (int)(fraction * (endAlpha - startAlpha));
        int red = startRed + (int)(fraction * (endRed - startRed));
        int green = startGreen + (int)(fraction * (endGreen - startGreen));
        int blue = startBlue + (int)(fraction * (endBlue - startBlue));

        return Integer.valueOf((alpha << 24) | (red << 16) | (green << 8) | blue);
    }
}
