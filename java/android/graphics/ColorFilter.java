package android.graphics;

public class ColorFilter
{
    public ColorFilter()
    {
    }

    int apply(int source)
    {
        return source;
    }

    boolean tintsImages()
    {
        return false;
    }

    int imageTint()
    {
        return 0;
    }
}
