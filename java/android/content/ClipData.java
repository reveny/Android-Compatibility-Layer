package android.content;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ClipData implements Parcelable
{
    static final String[] MIMETYPES_TEXT_PLAIN = new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN};
    static final String[] MIMETYPES_TEXT_HTML = new String[] {ClipDescription.MIMETYPE_TEXT_HTML};
    static final String[] MIMETYPES_TEXT_URILIST = new String[] {ClipDescription.MIMETYPE_TEXT_URILIST};
    static final String[] MIMETYPES_TEXT_INTENT = new String[] {ClipDescription.MIMETYPE_TEXT_INTENT};

    public static final Parcelable.Creator<ClipData> CREATOR = new Parcelable.Creator<ClipData>()
    {
        @Override
        public ClipData createFromParcel(Parcel source)
        {
            ClipDescription description = ClipDescription.CREATOR.createFromParcel(source);
            int count = source.readInt();
            ClipData clip = new ClipData(description, (Item)null);

            for (int index = 0; index < count; index++)
            {
                CharSequence text = source.readString();
                String htmlText = source.readString();
                Intent intent = (Intent)source.readValue(null);
                Uri uri = (Uri)source.readValue(null);
                clip.mItems.add(new Item(text, htmlText, intent, uri));
            }

            return clip;
        }

        @Override
        public ClipData[] newArray(int size)
        {
            return new ClipData[size];
        }
    };

    private final ClipDescription mDescription;
    private final List<Item> mItems = new ArrayList<>();

    public static class Item
    {
        private final CharSequence mText;
        private final String mHtmlText;
        private final Intent mIntent;
        private final Uri mUri;
        private android.view.textclassifier.TextLinks mTextLinks;

        public Item(CharSequence text)
        {
            this(text, null, null, null);
        }

        public Item(CharSequence text, String htmlText)
        {
            this(text, htmlText, null, null);
        }

        public Item(Intent intent)
        {
            this(null, null, intent, null);
        }

        public Item(Uri uri)
        {
            this(null, null, null, uri);
        }

        public Item(CharSequence text, Intent intent, Uri uri)
        {
            this(text, null, intent, uri);
        }

        public Item(CharSequence text, String htmlText, Intent intent, Uri uri)
        {
            if (htmlText != null && text == null)
            {
                throw new IllegalArgumentException("Plain text must be supplied if HTML text is supplied");
            }

            mText = text;
            mHtmlText = htmlText;
            mIntent = intent;
            mUri = uri;
        }

        public CharSequence getText()
        {
            return mText;
        }

        public String getHtmlText()
        {
            return mHtmlText;
        }

        public Intent getIntent()
        {
            return mIntent;
        }

        public Uri getUri()
        {
            return mUri;
        }

        public android.view.textclassifier.TextLinks getTextLinks()
        {
            return mTextLinks;
        }

        public void setTextLinks(android.view.textclassifier.TextLinks textLinks)
        {
            mTextLinks = textLinks;
        }

        public CharSequence coerceToText(Context context)
        {
            if (mText != null) return mText;
            if (mUri != null) return readUriText(context);
            if (mIntent != null) return mIntent.toUri(Intent.URI_INTENT_SCHEME);

            return "";
        }

        public CharSequence coerceToStyledText(Context context)
        {
            return coerceToText(context);
        }

        public String coerceToHtmlText(Context context)
        {
            if (mHtmlText != null) return mHtmlText;

            CharSequence text = coerceToText(context);
            return text == null ? null : escapeHtml(text.toString());
        }

        private CharSequence readUriText(Context context)
        {
            if (context != null && ContentResolver.SCHEME_CONTENT.equals(mUri.getScheme()))
            {
                try (java.io.InputStream stream = context.getContentResolver().openInputStream(mUri))
                {
                    java.io.ByteArrayOutputStream contents = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int read;

                    while ((read = stream.read(buffer)) > 0) contents.write(buffer, 0, read);

                    return new String(contents.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                }
                catch (java.io.IOException | RuntimeException unreadable)
                {
                    android.util.Log.w("ClipData", "the clip uri " + mUri + " could not be read", unreadable);
                }
            }

            return mUri.toString();
        }

        private static String escapeHtml(String text)
        {
            StringBuilder escaped = new StringBuilder(text.length());

            for (int index = 0; index < text.length(); index++)
            {
                char character = text.charAt(index);

                if (character == '<') escaped.append("&lt;");
                else if (character == '>') escaped.append("&gt;");
                else if (character == '&') escaped.append("&amp;");
                else if (character == '\n') escaped.append("<br>");
                else escaped.append(character);
            }

            return escaped.toString();
        }

        @Override
        public String toString()
        {
            StringBuilder described = new StringBuilder("ClipData.Item { ");
            if (mHtmlText != null) described.append("H:").append(mHtmlText);
            else if (mText != null) described.append("T:").append(mText);
            else if (mUri != null) described.append("U:").append(mUri);
            else if (mIntent != null) described.append("I:").append(mIntent.toShortString(true, true, true, true));
            else described.append("NULL");

            return described.append(" }").toString();
        }
    }

    public ClipData(CharSequence label, String[] mimeTypes, Item item)
    {
        mDescription = new ClipDescription(label, mimeTypes);
        if (item == null) throw new NullPointerException("item is null");

        mItems.add(item);
    }

    public ClipData(ClipDescription description, Item item)
    {
        mDescription = description;
        if (item != null) mItems.add(item);
    }

    public ClipData(ClipData other)
    {
        mDescription = other.mDescription;
        mItems.addAll(other.mItems);
    }

    public static ClipData newPlainText(CharSequence label, CharSequence text)
    {
        return new ClipData(label, MIMETYPES_TEXT_PLAIN, new Item(text));
    }

    public static ClipData newHtmlText(CharSequence label, CharSequence text, String htmlText)
    {
        return new ClipData(label, MIMETYPES_TEXT_HTML, new Item(text, htmlText));
    }

    public static ClipData newIntent(CharSequence label, Intent intent)
    {
        return new ClipData(label, MIMETYPES_TEXT_INTENT, new Item(intent));
    }

    public static ClipData newUri(ContentResolver resolver, CharSequence label, Uri uri)
    {
        Item item = new Item(uri);
        String[] mimeTypes = null;

        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && resolver != null)
        {
            String realType = resolver.getType(uri);
            String[] streamTypes = resolver.getStreamTypes(uri, "*/*");

            List<String> collected = new ArrayList<>();
            if (realType != null) collected.add(realType);

            if (streamTypes != null)
            {
                for (String streamType : streamTypes)
                {
                    if (!collected.contains(streamType)) collected.add(streamType);
                }
            }

            if (!collected.isEmpty()) mimeTypes = collected.toArray(new String[0]);
        }

        if (mimeTypes == null) mimeTypes = MIMETYPES_TEXT_URILIST;

        return new ClipData(label, mimeTypes, item);
    }

    public static ClipData newRawUri(CharSequence label, Uri uri)
    {
        return new ClipData(label, MIMETYPES_TEXT_URILIST, new Item(uri));
    }

    public ClipDescription getDescription()
    {
        return mDescription;
    }

    public CharSequence getLabel()
    {
        return mDescription == null ? null : mDescription.getLabel();
    }

    public void addItem(Item item)
    {
        if (item == null) throw new NullPointerException("item is null");

        mItems.add(item);
    }

    public void addItem(ContentResolver resolver, Item item)
    {
        addItem(item);
    }

    public int getItemCount()
    {
        return mItems.size();
    }

    public Item getItemAt(int index)
    {
        return mItems.get(index);
    }

    @Override
    public String toString()
    {
        StringBuilder described = new StringBuilder("ClipData { ");
        described.append(mDescription);

        for (Item item : mItems)
        {
            described.append(' ').append(item);
        }

        return described.append(" }").toString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        mDescription.writeToParcel(destination, flags);
        destination.writeInt(mItems.size());

        for (Item item : mItems)
        {
            destination.writeString(item.mText == null ? null : item.mText.toString());
            destination.writeString(item.mHtmlText);
            destination.writeValue(item.mIntent);
            destination.writeValue(item.mUri);
        }
    }
}
