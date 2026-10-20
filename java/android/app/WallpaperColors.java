package android.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public final class WallpaperColors implements Parcelable
{
    public static final int HINT_SUPPORTS_DARK_TEXT = 1 << 0;
    public static final int HINT_SUPPORTS_DARK_THEME = 1 << 1;

    private final Color mPrimary;
    private final Color mSecondary;
    private final Color mTertiary;
    private final int mHints;

    public WallpaperColors(Color primary, Color secondary, Color tertiary)
    {
        this(primary, secondary, tertiary, 0);
    }

    public WallpaperColors(Color primary, Color secondary, Color tertiary, int hints)
    {
        if (primary == null) throw new IllegalArgumentException("the primary colour is required");

        mPrimary = primary;
        mSecondary = secondary;
        mTertiary = tertiary;
        mHints = hints;
    }

    public static WallpaperColors fromBitmap(Bitmap bitmap)
    {
        if (bitmap == null) throw new IllegalArgumentException("the bitmap is required");

        return new WallpaperColors(Color.valueOf(Color.BLACK), null, null, 0);
    }

    public static WallpaperColors fromDrawable(Drawable drawable)
    {
        if (drawable == null) throw new IllegalArgumentException("the drawable is required");

        return new WallpaperColors(Color.valueOf(Color.BLACK), null, null, 0);
    }

    public Color getPrimaryColor()
    {
        return mPrimary;
    }

    public Color getSecondaryColor()
    {
        return mSecondary;
    }

    public Color getTertiaryColor()
    {
        return mTertiary;
    }

    public int getColorHints()
    {
        return mHints;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeValue(Integer.valueOf(mPrimary.toArgb()));
        parcel.writeValue(mSecondary == null ? null : Integer.valueOf(mSecondary.toArgb()));
        parcel.writeValue(mTertiary == null ? null : Integer.valueOf(mTertiary.toArgb()));
        parcel.writeValue(Integer.valueOf(mHints));
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof WallpaperColors)) return false;

        WallpaperColors colors = (WallpaperColors)other;

        return mHints == colors.mHints && mPrimary.equals(colors.mPrimary);
    }

    @Override
    public int hashCode()
    {
        return mPrimary.hashCode() * 31 + mHints;
    }

    @Override
    public String toString()
    {
        return "WallpaperColors(" + mPrimary + ", hints=" + mHints + ")";
    }

    public static final Creator<WallpaperColors> CREATOR = new Creator<WallpaperColors>()
    {
        @Override
        public WallpaperColors createFromParcel(Parcel parcel)
        {
            Color primary = readColour(parcel);
            Color secondary = readColour(parcel);
            Color tertiary = readColour(parcel);

            Object hints = parcel.readValue(null);

            return new WallpaperColors(primary == null ? Color.valueOf(Color.BLACK) : primary, secondary, tertiary,
                                       hints instanceof Integer ? ((Integer)hints).intValue() : 0);
        }

        private Color readColour(Parcel parcel)
        {
            Object value = parcel.readValue(null);

            return value instanceof Integer ? Color.valueOf(((Integer)value).intValue()) : null;
        }

        @Override
        public WallpaperColors[] newArray(int size)
        {
            return new WallpaperColors[size];
        }
    };
}
