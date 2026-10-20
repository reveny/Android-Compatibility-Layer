package android.content;

public class ClipDescription
{
    public static final String MIMETYPE_TEXT_PLAIN = "text/plain";
    public static final String MIMETYPE_TEXT_HTML = "text/html";
    public static final String MIMETYPE_TEXT_INTENT = "text/vnd.android.intent";

    private final CharSequence mLabel;
    private final String[] mMimeTypes;

    public ClipDescription(CharSequence label, String[] mimeTypes)
    {
        mLabel = label;
        mMimeTypes = mimeTypes == null ? new String[0] : mimeTypes;
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public int getMimeTypeCount()
    {
        return mMimeTypes.length;
    }

    public String getMimeType(int index)
    {
        return index >= 0 && index < mMimeTypes.length ? mMimeTypes[index] : null;
    }

    public boolean hasMimeType(String mimeType)
    {
        for (String candidate : mMimeTypes)
        {
            if (candidate != null && candidate.equals(mimeType)) return true;
        }

        return false;
    }
}
