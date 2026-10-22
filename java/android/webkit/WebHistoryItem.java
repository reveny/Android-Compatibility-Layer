package android.webkit;

import android.graphics.Bitmap;

public abstract class WebHistoryItem implements Cloneable
{
    public WebHistoryItem()
    {
    }

    public abstract String getUrl();

    public abstract String getOriginalUrl();

    public abstract String getTitle();

    public abstract Bitmap getFavicon();

    @Override
    protected abstract WebHistoryItem clone();
}
