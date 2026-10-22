package android.graphics;

public class DrawFilter
{
    public DrawFilter()
    {
    }

    Paint filter(Paint paint)
    {
        return paint;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }
}
