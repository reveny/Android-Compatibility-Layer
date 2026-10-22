package android.graphics.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class Icon implements Parcelable
{
    public static final int TYPE_BITMAP = 1;
    public static final int TYPE_RESOURCE = 2;
    public static final int TYPE_DATA = 3;
    public static final int TYPE_URI = 4;
    public static final int TYPE_ADAPTIVE_BITMAP = 5;
    public static final int TYPE_URI_ADAPTIVE_BITMAP = 6;

    private int mType;
    private Bitmap mBitmap;
    private String mPackageName;
    private int mResourceId;
    private String mUri;
    private byte[] mData;
    private int mDataOffset;
    private int mDataLength;
    private android.content.res.ColorStateList mTintList;
    private android.graphics.BlendMode mTintBlendMode;

    private Icon(int type)
    {
        mType = type;
    }

    public int getType()
    {
        return mType;
    }

    public Bitmap getBitmap()
    {
        if (mType != TYPE_BITMAP && mType != TYPE_ADAPTIVE_BITMAP)
        {
            throw new IllegalStateException("called getBitmap() on " + this);
        }

        return mBitmap;
    }

    public int getResId()
    {
        if (mType != TYPE_RESOURCE) throw new IllegalStateException("called getResId() on " + this);

        return mResourceId;
    }

    public String getResPackage()
    {
        if (mType != TYPE_RESOURCE) throw new IllegalStateException("called getResPackage() on " + this);

        return mPackageName;
    }

    public Uri getUri()
    {
        if (mType != TYPE_URI && mType != TYPE_URI_ADAPTIVE_BITMAP)
        {
            throw new IllegalStateException("called getUri() on " + this);
        }

        return Uri.parse(mUri);
    }

    public Icon setTint(int tint)
    {
        return setTintList(android.content.res.ColorStateList.valueOf(tint));
    }

    public Icon setTintList(android.content.res.ColorStateList tintList)
    {
        mTintList = tintList;
        return this;
    }

    public android.content.res.ColorStateList getTintList()
    {
        return mTintList;
    }

    public Icon setTintMode(android.graphics.PorterDuff.Mode mode)
    {
        mTintBlendMode = blendModeOf(mode);
        return this;
    }

    private static android.graphics.BlendMode blendModeOf(android.graphics.PorterDuff.Mode mode)
    {
        if (mode == null) return null;
        if (mode == android.graphics.PorterDuff.Mode.ADD) return android.graphics.BlendMode.PLUS;
        if (mode == android.graphics.PorterDuff.Mode.MULTIPLY) return android.graphics.BlendMode.MODULATE;

        return android.graphics.BlendMode.valueOf(mode.name());
    }

    public Icon setTintBlendMode(android.graphics.BlendMode blendMode)
    {
        mTintBlendMode = blendMode;
        return this;
    }

    public Drawable loadDrawable(Context context)
    {
        Drawable drawable = buildDrawable(context);

        if (drawable != null && mTintList != null) drawable.setTintList(mTintList);
        if (drawable != null && mTintBlendMode != null) drawable.setTintBlendMode(mTintBlendMode);

        return drawable;
    }

    private Drawable buildDrawable(Context context)
    {
        if (mType == TYPE_BITMAP || mType == TYPE_ADAPTIVE_BITMAP)
        {
            if (mBitmap == null) return null;

            Resources resources = context == null ? null : context.getResources();
            BitmapDrawable bitmap = new BitmapDrawable(resources, mBitmap);

            if (mType == TYPE_BITMAP) return bitmap;

            return new AdaptiveIconDrawable(null, bitmap);
        }

        if (mType == TYPE_RESOURCE)
        {
            if (context == null) return null;

            if (mPackageName == null || mPackageName.equals(context.getPackageName()))
            {
                return context.getResources().getDrawable(mResourceId, context.getTheme());
            }

            try
            {
                Context other = context.createPackageContext(mPackageName, 0);
                return other.getResources().getDrawable(mResourceId, other.getTheme());
            }
            catch (Exception failure)
            {
                return null;
            }
        }

        if (mType == TYPE_DATA)
        {
            if (mData == null) return null;

            Bitmap decoded = android.graphics.BitmapFactory.decodeByteArray(mData, mDataOffset, mDataLength);
            if (decoded == null) return null;

            return new BitmapDrawable(context == null ? null : context.getResources(), decoded);
        }

        if (mType == TYPE_URI || mType == TYPE_URI_ADAPTIVE_BITMAP)
        {
            Bitmap decoded = decodeUri(context);
            if (decoded == null) return null;

            BitmapDrawable bitmap = new BitmapDrawable(context == null ? null : context.getResources(), decoded);
            return mType == TYPE_URI ? bitmap : new AdaptiveIconDrawable(null, bitmap);
        }

        return null;
    }

    private Bitmap decodeUri(Context context)
    {
        if (mUri == null) return null;

        Uri uri = Uri.parse(mUri);
        String scheme = uri.getScheme();

        if (scheme == null || "file".equals(scheme))
        {
            String path = scheme == null ? mUri : uri.getPath();
            return android.graphics.BitmapFactory.decodeFile(path);
        }

        if (context == null) return null;

        try (java.io.InputStream stream = context.getContentResolver().openInputStream(uri))
        {
            return stream == null ? null : android.graphics.BitmapFactory.decodeStream(stream);
        }
        catch (Exception failure)
        {
            return null;
        }
    }

    public void loadDrawableAsync(Context context, android.os.Message andThen)
    {
        andThen.obj = loadDrawable(context);
        andThen.sendToTarget();
    }

    public void loadDrawableAsync(Context context, OnDrawableLoadedListener listener, android.os.Handler handler)
    {
        final Drawable drawable = loadDrawable(context);

        if (listener == null) return;

        if (handler == null)
        {
            listener.onDrawableLoaded(drawable);
            return;
        }

        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onDrawableLoaded(drawable);
            }
        });
    }

    public interface OnDrawableLoadedListener
    {
        void onDrawableLoaded(Drawable drawable);
    }

    public static Icon createWithBitmap(Bitmap bitmap)
    {
        if (bitmap == null) throw new IllegalArgumentException("bitmap must not be null");

        Icon icon = new Icon(TYPE_BITMAP);
        icon.mBitmap = bitmap;

        return icon;
    }

    public static Icon createWithAdaptiveBitmap(Bitmap bitmap)
    {
        if (bitmap == null) throw new IllegalArgumentException("bitmap must not be null");

        Icon icon = new Icon(TYPE_ADAPTIVE_BITMAP);
        icon.mBitmap = bitmap;

        return icon;
    }

    public static Icon createWithResource(Context context, int resourceId)
    {
        if (context == null) throw new IllegalArgumentException("context must not be null");

        return createWithResource(context.getPackageName(), resourceId);
    }

    public static Icon createWithResource(Resources resources, int resourceId)
    {
        Icon icon = new Icon(TYPE_RESOURCE);
        icon.mResourceId = resourceId;

        return icon;
    }

    public static Icon createWithResource(String packageName, int resourceId)
    {
        Icon icon = new Icon(TYPE_RESOURCE);
        icon.mPackageName = packageName;
        icon.mResourceId = resourceId;

        return icon;
    }

    public static Icon createWithFilePath(String path)
    {
        if (path == null) throw new IllegalArgumentException("Path must not be null.");

        Icon icon = new Icon(TYPE_URI);
        icon.mUri = path;

        return icon;
    }

    public static Icon createWithContentUri(String uri)
    {
        Icon icon = new Icon(TYPE_URI);
        icon.mUri = uri;

        return icon;
    }

    public static Icon createWithContentUri(Uri uri)
    {
        return createWithContentUri(uri == null ? null : uri.toString());
    }

    public static Icon createWithAdaptiveBitmapContentUri(String uri)
    {
        Icon icon = new Icon(TYPE_URI_ADAPTIVE_BITMAP);
        icon.mUri = uri;

        return icon;
    }

    public static Icon createWithAdaptiveBitmapContentUri(Uri uri)
    {
        return createWithAdaptiveBitmapContentUri(uri == null ? null : uri.toString());
    }

    public static Icon createWithData(byte[] data, int offset, int length)
    {
        Icon icon = new Icon(TYPE_DATA);
        icon.mData = data;
        icon.mDataOffset = offset;
        icon.mDataLength = length;

        return icon;
    }

    @Override
    public String toString()
    {
        return "Icon(typ=" + mType + ")";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeValue(Integer.valueOf(mType));
        parcel.writeValue(mPackageName);
        parcel.writeValue(Integer.valueOf(mResourceId));
        parcel.writeValue(mUri);
    }

    public static final Creator<Icon> CREATOR = new Creator<Icon>()
    {
        @Override
        public Icon createFromParcel(Parcel parcel)
        {
            Object type = parcel.readValue(null);
            Icon icon = new Icon(type instanceof Integer ? ((Integer)type).intValue() : TYPE_RESOURCE);

            icon.mPackageName = (String)parcel.readValue(null);

            Object resourceId = parcel.readValue(null);
            icon.mResourceId = resourceId instanceof Integer ? ((Integer)resourceId).intValue() : 0;

            icon.mUri = (String)parcel.readValue(null);

            return icon;
        }

        @Override
        public Icon[] newArray(int size)
        {
            return new Icon[size];
        }
    };
}
