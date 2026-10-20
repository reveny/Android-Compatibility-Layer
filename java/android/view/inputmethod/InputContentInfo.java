package android.view.inputmethod;

public class InputContentInfo
{
    private final Object mContentUri;
    private final Object mDescription;
    private final Object mLinkUri;

    public InputContentInfo(Object contentUri, Object description)
    {
        this(contentUri, description, null);
    }

    public InputContentInfo(Object contentUri, Object description, Object linkUri)
    {
        mContentUri = contentUri;
        mDescription = description;
        mLinkUri = linkUri;
    }

    public android.net.Uri getContentUri()
    {
        return (android.net.Uri)mContentUri;
    }

    public android.content.ClipDescription getDescription()
    {
        return (android.content.ClipDescription)mDescription;
    }

    public android.net.Uri getLinkUri()
    {
        return (android.net.Uri)mLinkUri;
    }

    public void requestPermission()
    {
    }

    public void releasePermission()
    {
    }
}
