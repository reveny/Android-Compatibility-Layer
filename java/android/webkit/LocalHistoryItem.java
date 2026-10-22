package android.webkit;

import android.graphics.Bitmap;

final class LocalHistoryItem extends WebHistoryItem
{
    private final String mUrl;
    private final String mOriginalUrl;
    private String mTitle;
    private Bitmap mFavicon;

    LocalHistoryItem(String url, String originalUrl, String title)
    {
        mUrl = url;
        mOriginalUrl = originalUrl;
        mTitle = title;
    }

    @Override
    public String getUrl()
    {
        return mUrl;
    }

    @Override
    public String getOriginalUrl()
    {
        return mOriginalUrl;
    }

    @Override
    public String getTitle()
    {
        return mTitle;
    }

    void setTitle(String title)
    {
        mTitle = title;
    }

    @Override
    public Bitmap getFavicon()
    {
        return mFavicon;
    }

    @Override
    protected LocalHistoryItem clone()
    {
        LocalHistoryItem copy = new LocalHistoryItem(mUrl, mOriginalUrl, mTitle);
        copy.mFavicon = mFavicon;
        return copy;
    }
}
