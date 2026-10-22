package android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public final class PointerIcon implements Parcelable
{
    public static final int TYPE_NULL = 0;
    public static final int TYPE_ARROW = 1000;
    public static final int TYPE_DEFAULT = TYPE_ARROW;
    public static final int TYPE_CONTEXT_MENU = 1001;
    public static final int TYPE_HAND = 1002;
    public static final int TYPE_HELP = 1003;
    public static final int TYPE_WAIT = 1004;
    public static final int TYPE_CELL = 1006;
    public static final int TYPE_CROSSHAIR = 1007;
    public static final int TYPE_TEXT = 1008;
    public static final int TYPE_VERTICAL_TEXT = 1009;
    public static final int TYPE_ALIAS = 1010;
    public static final int TYPE_COPY = 1011;
    public static final int TYPE_NO_DROP = 1012;
    public static final int TYPE_ALL_SCROLL = 1013;
    public static final int TYPE_HORIZONTAL_DOUBLE_ARROW = 1014;
    public static final int TYPE_VERTICAL_DOUBLE_ARROW = 1015;
    public static final int TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW = 1016;
    public static final int TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW = 1017;
    public static final int TYPE_ZOOM_IN = 1018;
    public static final int TYPE_ZOOM_OUT = 1019;
    public static final int TYPE_GRAB = 1020;
    public static final int TYPE_GRABBING = 1021;
    public static final int TYPE_HANDWRITING = 1022;

    private static final int TYPE_CUSTOM = -1;

    public static final Parcelable.Creator<PointerIcon> CREATOR = new Parcelable.Creator<PointerIcon>()
    {
        @Override
        public PointerIcon createFromParcel(Parcel source)
        {
            int type = source.readInt();
            float hotSpotX = source.readFloat();
            float hotSpotY = source.readFloat();
            return new PointerIcon(type, null, hotSpotX, hotSpotY);
        }

        @Override
        public PointerIcon[] newArray(int size)
        {
            return new PointerIcon[size];
        }
    };

    private static final android.util.SparseArray<PointerIcon> sSystemIcons = new android.util.SparseArray<>();

    private final int mType;
    private final Bitmap mBitmap;
    private final float mHotSpotX;
    private final float mHotSpotY;

    private PointerIcon(int type, Bitmap bitmap, float hotSpotX, float hotSpotY)
    {
        mType = type;
        mBitmap = bitmap;
        mHotSpotX = hotSpotX;
        mHotSpotY = hotSpotY;
    }

    public static PointerIcon getSystemIcon(Context context, int type)
    {
        if (context == null) throw new IllegalArgumentException("context must not be null");

        synchronized (sSystemIcons)
        {
            PointerIcon icon = sSystemIcons.get(type);
            if (icon == null)
            {
                icon = new PointerIcon(type, null, 0, 0);
                sSystemIcons.put(type, icon);
            }
            return icon;
        }
    }

    public static PointerIcon create(Bitmap bitmap, float hotSpotX, float hotSpotY)
    {
        if (bitmap == null) throw new IllegalArgumentException("bitmap must not be null");
        if (hotSpotX < 0 || hotSpotX >= bitmap.getWidth()) throw new IllegalArgumentException("x hotspot lies outside of the bitmap area");
        if (hotSpotY < 0 || hotSpotY >= bitmap.getHeight()) throw new IllegalArgumentException("y hotspot lies outside of the bitmap area");

        return new PointerIcon(TYPE_CUSTOM, bitmap, hotSpotX, hotSpotY);
    }

    public static PointerIcon load(android.content.res.Resources resources, int resourceId)
    {
        if (resources == null) throw new IllegalArgumentException("resources must not be null");
        return new PointerIcon(TYPE_CUSTOM, null, 0, 0);
    }

    int getType()
    {
        return mType;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mType);
        dest.writeFloat(mHotSpotX);
        dest.writeFloat(mHotSpotY);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PointerIcon)) return false;

        PointerIcon icon = (PointerIcon)other;
        if (mType != icon.mType) return false;
        if (mType != TYPE_CUSTOM) return true;

        return mBitmap == icon.mBitmap && mHotSpotX == icon.mHotSpotX && mHotSpotY == icon.mHotSpotY;
    }

    @Override
    public int hashCode()
    {
        return mType;
    }

    @Override
    public String toString()
    {
        return "PointerIcon{type=" + mType + ", hotspotX=" + mHotSpotX + ", hotspotY=" + mHotSpotY + "}";
    }
}
