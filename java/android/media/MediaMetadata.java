package android.media;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MediaMetadata implements Parcelable
{
    public static final String METADATA_KEY_ALBUM = "android.media.metadata.ALBUM";
    public static final String METADATA_KEY_ALBUM_ART = "android.media.metadata.ALBUM_ART";
    public static final String METADATA_KEY_ALBUM_ARTIST = "android.media.metadata.ALBUM_ARTIST";
    public static final String METADATA_KEY_ALBUM_ART_URI = "android.media.metadata.ALBUM_ART_URI";
    public static final String METADATA_KEY_ART = "android.media.metadata.ART";
    public static final String METADATA_KEY_ARTIST = "android.media.metadata.ARTIST";
    public static final String METADATA_KEY_ART_URI = "android.media.metadata.ART_URI";
    public static final String METADATA_KEY_AUTHOR = "android.media.metadata.AUTHOR";
    public static final String METADATA_KEY_BT_FOLDER_TYPE = "android.media.metadata.BT_FOLDER_TYPE";
    public static final String METADATA_KEY_COMPILATION = "android.media.metadata.COMPILATION";
    public static final String METADATA_KEY_COMPOSER = "android.media.metadata.COMPOSER";
    public static final String METADATA_KEY_DATE = "android.media.metadata.DATE";
    public static final String METADATA_KEY_DISC_NUMBER = "android.media.metadata.DISC_NUMBER";
    public static final String METADATA_KEY_DISPLAY_DESCRIPTION = "android.media.metadata.DISPLAY_DESCRIPTION";
    public static final String METADATA_KEY_DISPLAY_ICON = "android.media.metadata.DISPLAY_ICON";
    public static final String METADATA_KEY_DISPLAY_ICON_URI = "android.media.metadata.DISPLAY_ICON_URI";
    public static final String METADATA_KEY_DISPLAY_SUBTITLE = "android.media.metadata.DISPLAY_SUBTITLE";
    public static final String METADATA_KEY_DISPLAY_TITLE = "android.media.metadata.DISPLAY_TITLE";
    public static final String METADATA_KEY_DURATION = "android.media.metadata.DURATION";
    public static final String METADATA_KEY_GENRE = "android.media.metadata.GENRE";
    public static final String METADATA_KEY_MEDIA_ID = "android.media.metadata.MEDIA_ID";
    public static final String METADATA_KEY_MEDIA_URI = "android.media.metadata.MEDIA_URI";
    public static final String METADATA_KEY_NUM_TRACKS = "android.media.metadata.NUM_TRACKS";
    public static final String METADATA_KEY_RATING = "android.media.metadata.RATING";
    public static final String METADATA_KEY_TITLE = "android.media.metadata.TITLE";
    public static final String METADATA_KEY_TRACK_NUMBER = "android.media.metadata.TRACK_NUMBER";
    public static final String METADATA_KEY_USER_RATING = "android.media.metadata.USER_RATING";
    public static final String METADATA_KEY_WRITER = "android.media.metadata.WRITER";
    public static final String METADATA_KEY_YEAR = "android.media.metadata.YEAR";

    static final int METADATA_TYPE_LONG = 0;
    static final int METADATA_TYPE_TEXT = 1;
    static final int METADATA_TYPE_BITMAP = 2;
    static final int METADATA_TYPE_RATING = 3;

    static final Map<String, Integer> METADATA_KEYS_TYPE = new HashMap<>();

    private static final String[] PREFERRED_DESCRIPTION_ORDER = {METADATA_KEY_TITLE, METADATA_KEY_ARTIST, METADATA_KEY_ALBUM,
            METADATA_KEY_ALBUM_ARTIST, METADATA_KEY_WRITER, METADATA_KEY_AUTHOR, METADATA_KEY_COMPOSER};
    private static final String[] PREFERRED_BITMAP_ORDER = {METADATA_KEY_DISPLAY_ICON, METADATA_KEY_ART, METADATA_KEY_ALBUM_ART};
    private static final String[] PREFERRED_URI_ORDER = {METADATA_KEY_DISPLAY_ICON_URI, METADATA_KEY_ART_URI,
            METADATA_KEY_ALBUM_ART_URI};

    static
    {
        METADATA_KEYS_TYPE.put(METADATA_KEY_TITLE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ARTIST, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DURATION, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ALBUM, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_AUTHOR, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_WRITER, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_COMPOSER, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_COMPILATION, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DATE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_YEAR, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_GENRE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_TRACK_NUMBER, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_NUM_TRACKS, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISC_NUMBER, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ALBUM_ARTIST, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ART, METADATA_TYPE_BITMAP);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ART_URI, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ALBUM_ART, METADATA_TYPE_BITMAP);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ALBUM_ART_URI, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_USER_RATING, METADATA_TYPE_RATING);
        METADATA_KEYS_TYPE.put(METADATA_KEY_RATING, METADATA_TYPE_RATING);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_TITLE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_SUBTITLE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_DESCRIPTION, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_ICON, METADATA_TYPE_BITMAP);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_ICON_URI, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_BT_FOLDER_TYPE, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_MEDIA_ID, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_MEDIA_URI, METADATA_TYPE_TEXT);
    }

    public static final Parcelable.Creator<MediaMetadata> CREATOR = new Parcelable.Creator<MediaMetadata>()
    {
        @Override
        public MediaMetadata createFromParcel(Parcel source)
        {
            Bundle bundle = source.readBundle();
            int limit = source.readInt();
            return new MediaMetadata(bundle == null ? new Bundle() : bundle, limit);
        }

        @Override
        public MediaMetadata[] newArray(int size)
        {
            return new MediaMetadata[size];
        }
    };

    private final Bundle mBundle;
    private final int mBitmapDimensionLimit;
    private MediaDescription mDescription;

    private MediaMetadata(Bundle bundle, int bitmapDimensionLimit)
    {
        mBundle = new Bundle(bundle);
        mBitmapDimensionLimit = bitmapDimensionLimit;
    }

    public boolean containsKey(String key)
    {
        return mBundle.containsKey(key);
    }

    public CharSequence getText(String key)
    {
        return mBundle.getCharSequence(key);
    }

    public String getString(String key)
    {
        CharSequence text = getText(key);
        return text == null ? null : text.toString();
    }

    public long getLong(String key)
    {
        return mBundle.getLong(key, 0);
    }

    public Rating getRating(String key)
    {
        return mBundle.getParcelable(key);
    }

    public Bitmap getBitmap(String key)
    {
        return mBundle.getParcelable(key);
    }

    public int getBitmapDimensionLimit()
    {
        return mBitmapDimensionLimit;
    }

    public int size()
    {
        return mBundle.size();
    }

    public Set<String> keySet()
    {
        return mBundle.keySet();
    }

    public MediaDescription getDescription()
    {
        if (mDescription != null) return mDescription;

        String mediaId = getString(METADATA_KEY_MEDIA_ID);
        CharSequence[] text = new CharSequence[3];
        Bitmap icon = null;
        Uri iconUri = null;

        CharSequence displayText = getText(METADATA_KEY_DISPLAY_TITLE);

        if (!TextUtils.isEmpty(displayText))
        {
            text[0] = displayText;
            text[1] = getText(METADATA_KEY_DISPLAY_SUBTITLE);
            text[2] = getText(METADATA_KEY_DISPLAY_DESCRIPTION);
        }
        else
        {
            int textIndex = 0;
            int keyIndex = 0;

            while (textIndex < text.length && keyIndex < PREFERRED_DESCRIPTION_ORDER.length)
            {
                CharSequence next = getText(PREFERRED_DESCRIPTION_ORDER[keyIndex++]);
                if (!TextUtils.isEmpty(next)) text[textIndex++] = next;
            }
        }

        for (String key : PREFERRED_BITMAP_ORDER)
        {
            Bitmap next = getBitmap(key);

            if (next != null)
            {
                icon = next;
                break;
            }
        }

        for (String key : PREFERRED_URI_ORDER)
        {
            String next = getString(key);

            if (!TextUtils.isEmpty(next))
            {
                iconUri = Uri.parse(next);
                break;
            }
        }

        Uri mediaUri = null;
        String mediaUriText = getString(METADATA_KEY_MEDIA_URI);
        if (!TextUtils.isEmpty(mediaUriText)) mediaUri = Uri.parse(mediaUriText);

        MediaDescription.Builder builder = new MediaDescription.Builder();
        builder.setMediaId(mediaId);
        builder.setTitle(text[0]);
        builder.setSubtitle(text[1]);
        builder.setDescription(text[2]);
        builder.setIconBitmap(icon);
        builder.setIconUri(iconUri);
        builder.setMediaUri(mediaUri);

        if (mBundle.containsKey(METADATA_KEY_BT_FOLDER_TYPE))
        {
            Bundle extras = new Bundle();
            extras.putLong(MediaDescription.EXTRA_BT_FOLDER_TYPE, getLong(METADATA_KEY_BT_FOLDER_TYPE));
            builder.setExtras(extras);
        }

        mDescription = builder.build();
        return mDescription;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeBundle(mBundle);
        dest.writeInt(mBitmapDimensionLimit);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) return true;
        if (!(object instanceof MediaMetadata)) return false;

        MediaMetadata other = (MediaMetadata)object;

        for (Map.Entry<String, Integer> entry : METADATA_KEYS_TYPE.entrySet())
        {
            String key = entry.getKey();

            switch (entry.getValue())
            {
                case METADATA_TYPE_TEXT:
                    if (!Objects.equals(getString(key), other.getString(key))) return false;
                    break;
                case METADATA_TYPE_LONG:
                    if (getLong(key) != other.getLong(key)) return false;
                    break;
                default:
                    break;
            }
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int hashCode = 17;

        for (Map.Entry<String, Integer> entry : METADATA_KEYS_TYPE.entrySet())
        {
            String key = entry.getKey();

            switch (entry.getValue())
            {
                case METADATA_TYPE_TEXT:
                    hashCode = 31 * hashCode + Objects.hashCode(getString(key));
                    break;
                case METADATA_TYPE_LONG:
                    hashCode = 31 * hashCode + Long.hashCode(getLong(key));
                    break;
                default:
                    break;
            }
        }

        return hashCode;
    }

    public static final class Builder
    {
        private final Bundle mBundle;
        private int mBitmapDimensionLimit = Integer.MAX_VALUE;

        public Builder()
        {
            mBundle = new Bundle();
        }

        public Builder(MediaMetadata source)
        {
            mBundle = new Bundle(source.mBundle);
            mBitmapDimensionLimit = source.mBitmapDimensionLimit;
        }

        public Builder putText(String key, CharSequence value)
        {
            checkType(key, METADATA_TYPE_TEXT, "CharSequence");
            mBundle.putCharSequence(key, value);
            return this;
        }

        public Builder putString(String key, String value)
        {
            checkType(key, METADATA_TYPE_TEXT, "String");
            mBundle.putCharSequence(key, value);
            return this;
        }

        public Builder putLong(String key, long value)
        {
            checkType(key, METADATA_TYPE_LONG, "long");
            mBundle.putLong(key, value);
            return this;
        }

        public Builder putRating(String key, Rating value)
        {
            checkType(key, METADATA_TYPE_RATING, "Rating");
            mBundle.putParcelable(key, value);
            return this;
        }

        public Builder putBitmap(String key, Bitmap value)
        {
            checkType(key, METADATA_TYPE_BITMAP, "Bitmap");
            mBundle.putParcelable(key, value);
            return this;
        }

        public Builder setBitmapDimensionLimit(int bitmapDimensionLimit)
        {
            mBitmapDimensionLimit = bitmapDimensionLimit > 0 ? bitmapDimensionLimit : Integer.MAX_VALUE;
            return this;
        }

        public MediaMetadata build()
        {
            if (mBitmapDimensionLimit != Integer.MAX_VALUE)
            {
                for (String key : new java.util.ArrayList<>(mBundle.keySet()))
                {
                    Object value = mBundle.get(key);

                    if (value instanceof Bitmap)
                    {
                        mBundle.putParcelable(key, scaleBitmap((Bitmap)value, mBitmapDimensionLimit));
                    }
                }
            }

            return new MediaMetadata(mBundle, mBitmapDimensionLimit);
        }

        private static void checkType(String key, int type, String typeName)
        {
            Integer expected = METADATA_KEYS_TYPE.get(key);

            if (expected != null && expected != type)
            {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a " + typeName);
            }
        }

        private static Bitmap scaleBitmap(Bitmap bitmap, int maxDimension)
        {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            if (width <= maxDimension && height <= maxDimension) return bitmap;

            float scale = Math.min((float)maxDimension / width, (float)maxDimension / height);
            int scaledWidth = Math.max(1, Math.round(width * scale));
            int scaledHeight = Math.max(1, Math.round(height * scale));

            return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        }
    }
}
