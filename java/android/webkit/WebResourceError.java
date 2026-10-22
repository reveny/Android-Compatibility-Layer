package android.webkit;

public abstract class WebResourceError
{
    WebResourceError()
    {
    }

    public abstract int getErrorCode();

    public abstract CharSequence getDescription();
}
