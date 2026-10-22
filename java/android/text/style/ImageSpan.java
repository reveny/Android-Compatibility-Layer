package android.text.style;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

public class ImageSpan extends DynamicDrawableSpan
{
    private Drawable mDrawable;
    private Uri mContentUri;
    private int mResourceId;
    private Context mContext;
    private String mSource;

    public ImageSpan(Bitmap bitmap)
    {
        this(null, bitmap, ALIGN_BOTTOM);
    }

    public ImageSpan(Bitmap bitmap, int verticalAlignment)
    {
        this(null, bitmap, verticalAlignment);
    }

    public ImageSpan(Context context, Bitmap bitmap)
    {
        this(context, bitmap, ALIGN_BOTTOM);
    }

    public ImageSpan(Context context, Bitmap bitmap, int verticalAlignment)
    {
        super(verticalAlignment);
        mContext = context;
        mDrawable = context != null ? new BitmapDrawable(context.getResources(), bitmap) : new BitmapDrawable(bitmap);

        int width = mDrawable.getIntrinsicWidth();
        int height = mDrawable.getIntrinsicHeight();
        mDrawable.setBounds(0, 0, width > 0 ? width : 0, height > 0 ? height : 0);
    }

    public ImageSpan(Drawable drawable)
    {
        this(drawable, ALIGN_BOTTOM);
    }

    public ImageSpan(Drawable drawable, int verticalAlignment)
    {
        super(verticalAlignment);
        mDrawable = drawable;
    }

    public ImageSpan(Drawable drawable, String source)
    {
        this(drawable, source, ALIGN_BOTTOM);
    }

    public ImageSpan(Drawable drawable, String source, int verticalAlignment)
    {
        super(verticalAlignment);
        mDrawable = drawable;
        mSource = source;
    }

    public ImageSpan(Context context, Uri uri)
    {
        this(context, uri, ALIGN_BOTTOM);
    }

    public ImageSpan(Context context, Uri uri, int verticalAlignment)
    {
        super(verticalAlignment);
        mContext = context;
        mContentUri = uri;
        mSource = uri == null ? null : uri.toString();
    }

    public ImageSpan(Context context, int resourceId)
    {
        this(context, resourceId, ALIGN_BOTTOM);
    }

    public ImageSpan(Context context, int resourceId, int verticalAlignment)
    {
        super(verticalAlignment);
        mContext = context;
        mResourceId = resourceId;
    }

    @Override
    public Drawable getDrawable()
    {
        if (mDrawable != null) return mDrawable;

        Drawable drawable = null;

        if (mContentUri != null && mContext != null)
        {
            try
            {
                java.io.InputStream stream = mContext.getContentResolver().openInputStream(mContentUri);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(stream);
                if (stream != null) stream.close();

                if (bitmap != null)
                {
                    drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                }
            }
            catch (Exception exception)
            {
                android.util.Log.e("ImageSpan", "Failed to loaded content " + mContentUri, exception);
            }
        }
        else if (mContext != null && mResourceId != 0)
        {
            try
            {
                drawable = mContext.getDrawable(mResourceId);
                if (drawable != null) drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }
            catch (Exception exception)
            {
                android.util.Log.e("ImageSpan", "Unable to find resource: " + mResourceId);
            }
        }

        return drawable;
    }

    public String getSource()
    {
        return mSource;
    }

    @Override
    public String toString()
    {
        return "ImageSpan{drawable=" + getDrawable() + ", source='" + mSource + "', verticalAlignment=" + getVerticalAlignment() + "}";
    }
}
