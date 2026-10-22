package android.media;

import android.graphics.Bitmap;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public abstract class MediaMetadataEditor
{
    public static final int BITMAP_KEY_ARTWORK = 100;
    public static final int RATING_KEY_BY_OTHERS = 101;
    public static final int RATING_KEY_BY_USER = 0x10000001;

    static final int KEY_EDITABLE_MASK = 0x1FFFFFFF;

    private static final int[] LONG_KEYS = {0, 8, 9, 14};
    private static final int[] STRING_KEYS = {1, 2, 3, 4, 5, 6, 7, 11, 13, 15};

    final Bundle mEditorMetadata = new Bundle();
    final List<Integer> mEditableKeys = new ArrayList<>();
    Bitmap mEditorArtwork;
    boolean mMetadataChanged;
    boolean mArtworkChanged;
    boolean mApplied;

    MediaMetadataEditor()
    {
    }

    public abstract void apply();

    public synchronized void addEditableKey(int key)
    {
        if (mApplied) return;

        if (key != RATING_KEY_BY_USER) throw new IllegalArgumentException("Invalid type 'int' for key " + key);

        if (!mEditableKeys.contains(key)) mEditableKeys.add(key);

        mMetadataChanged = true;
    }

    public synchronized void removeEditableKeys()
    {
        if (mApplied) return;

        if (!mEditableKeys.isEmpty())
        {
            mEditableKeys.clear();
            mMetadataChanged = true;
        }
    }

    public synchronized int[] getEditableKeys()
    {
        if (mEditableKeys.isEmpty()) return null;

        int[] keys = new int[mEditableKeys.size()];
        for (int index = 0; index < keys.length; index++) keys[index] = mEditableKeys.get(index);

        return keys;
    }

    public synchronized MediaMetadataEditor putString(int key, String value) throws IllegalArgumentException
    {
        if (mApplied) return this;

        if (!contains(STRING_KEYS, key)) throw new IllegalArgumentException("Invalid type 'String' for key " + key);

        mEditorMetadata.putString(String.valueOf(key), value);
        mMetadataChanged = true;
        return this;
    }

    public synchronized MediaMetadataEditor putLong(int key, long value) throws IllegalArgumentException
    {
        if (mApplied) return this;

        if (!contains(LONG_KEYS, key)) throw new IllegalArgumentException("Invalid type 'long' for key " + key);

        mEditorMetadata.putLong(String.valueOf(key), value);
        mMetadataChanged = true;
        return this;
    }

    public synchronized MediaMetadataEditor putBitmap(int key, Bitmap bitmap) throws IllegalArgumentException
    {
        if (mApplied) return this;

        if (key != BITMAP_KEY_ARTWORK) throw new IllegalArgumentException("Invalid type 'Bitmap' for key " + key);

        mEditorArtwork = bitmap;
        mArtworkChanged = true;
        return this;
    }

    public synchronized MediaMetadataEditor putObject(int key, Object value) throws IllegalArgumentException
    {
        if (mApplied) return this;

        if (key == RATING_KEY_BY_OTHERS || key == RATING_KEY_BY_USER)
        {
            if (value != null && !(value instanceof Rating))
            {
                throw new IllegalArgumentException("Not a non-null Rating Object for key " + key);
            }

            mEditorMetadata.putParcelable(String.valueOf(key), (Rating)value);
            mMetadataChanged = true;
            return this;
        }

        throw new IllegalArgumentException("Invalid key " + key);
    }

    public synchronized long getLong(int key, long defaultValue) throws IllegalArgumentException
    {
        if (!contains(LONG_KEYS, key)) throw new IllegalArgumentException("Invalid type 'long' for key " + key);

        return mEditorMetadata.getLong(String.valueOf(key), defaultValue);
    }

    public synchronized String getString(int key, String defaultValue) throws IllegalArgumentException
    {
        if (!contains(STRING_KEYS, key)) throw new IllegalArgumentException("Invalid type 'String' for key " + key);

        return mEditorMetadata.getString(String.valueOf(key), defaultValue);
    }

    public synchronized Bitmap getBitmap(int key, Bitmap defaultValue) throws IllegalArgumentException
    {
        if (key != BITMAP_KEY_ARTWORK) throw new IllegalArgumentException("Invalid type 'Bitmap' for key " + key);

        return mEditorArtwork != null ? mEditorArtwork : defaultValue;
    }

    public synchronized Object getObject(int key, Object defaultValue) throws IllegalArgumentException
    {
        if (key == RATING_KEY_BY_OTHERS || key == RATING_KEY_BY_USER)
        {
            Object value = mEditorMetadata.getParcelable(String.valueOf(key));
            return value != null ? value : defaultValue;
        }

        throw new IllegalArgumentException("Invalid key " + key);
    }

    public synchronized void clear()
    {
        if (mApplied) return;

        mEditorMetadata.clear();
        mEditorArtwork = null;
        mMetadataChanged = true;
        mArtworkChanged = true;
    }

    MediaMetadata toMediaMetadata()
    {
        MediaMetadata.Builder builder = new MediaMetadata.Builder();

        putMetadataString(builder, 1, MediaMetadata.METADATA_KEY_ALBUM);
        putMetadataString(builder, 2, MediaMetadata.METADATA_KEY_ARTIST);
        putMetadataString(builder, 3, MediaMetadata.METADATA_KEY_AUTHOR);
        putMetadataString(builder, 4, MediaMetadata.METADATA_KEY_COMPOSER);
        putMetadataString(builder, 5, MediaMetadata.METADATA_KEY_DATE);
        putMetadataString(builder, 6, MediaMetadata.METADATA_KEY_GENRE);
        putMetadataString(builder, 7, MediaMetadata.METADATA_KEY_TITLE);
        putMetadataString(builder, 11, MediaMetadata.METADATA_KEY_WRITER);
        putMetadataString(builder, 13, MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        putMetadataString(builder, 15, MediaMetadata.METADATA_KEY_COMPILATION);
        putMetadataLong(builder, 0, MediaMetadata.METADATA_KEY_TRACK_NUMBER);
        putMetadataLong(builder, 8, MediaMetadata.METADATA_KEY_YEAR);
        putMetadataLong(builder, 9, MediaMetadata.METADATA_KEY_DURATION);
        putMetadataLong(builder, 14, MediaMetadata.METADATA_KEY_DISC_NUMBER);

        Rating rating = mEditorMetadata.getParcelable(String.valueOf(RATING_KEY_BY_OTHERS));
        if (rating != null) builder.putRating(MediaMetadata.METADATA_KEY_RATING, rating);

        Rating userRating = mEditorMetadata.getParcelable(String.valueOf(RATING_KEY_BY_USER));
        if (userRating != null) builder.putRating(MediaMetadata.METADATA_KEY_USER_RATING, userRating);

        if (mEditorArtwork != null) builder.putBitmap(MediaMetadata.METADATA_KEY_ART, mEditorArtwork);

        return builder.build();
    }

    private void putMetadataString(MediaMetadata.Builder builder, int key, String metadataKey)
    {
        String name = String.valueOf(key);
        if (mEditorMetadata.containsKey(name)) builder.putString(metadataKey, mEditorMetadata.getString(name));
    }

    private void putMetadataLong(MediaMetadata.Builder builder, int key, String metadataKey)
    {
        String name = String.valueOf(key);
        if (mEditorMetadata.containsKey(name)) builder.putLong(metadataKey, mEditorMetadata.getLong(name));
    }

    private static boolean contains(int[] keys, int key)
    {
        for (int candidate : keys)
        {
            if (candidate == key) return true;
        }

        return false;
    }
}
