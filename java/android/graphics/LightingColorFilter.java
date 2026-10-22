package android.graphics;

public class LightingColorFilter extends ColorFilter
{
    private final int mMultiply;
    private final int mAdd;

    public LightingColorFilter(int multiply, int add)
    {
        mMultiply = multiply;
        mAdd = add;
    }

    public int getColorMultiply()
    {
        return mMultiply;
    }

    public int getColorAdd()
    {
        return mAdd;
    }

    @Override
    int apply(int source)
    {
        int red = Math.min(255, Color.red(source) * Color.red(mMultiply) / 255 + Color.red(mAdd));
        int green = Math.min(255, Color.green(source) * Color.green(mMultiply) / 255 + Color.green(mAdd));
        int blue = Math.min(255, Color.blue(source) * Color.blue(mMultiply) / 255 + Color.blue(mAdd));

        return Color.argb(Color.alpha(source), red, green, blue);
    }

    @Override
    boolean tintsImages()
    {
        return (mMultiply & 0x00FFFFFF) == 0;
    }

    @Override
    int imageTint()
    {
        return 0xFF000000 | (mAdd & 0x00FFFFFF);
    }
}
