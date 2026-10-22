package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.Gravity;

import org.xmlpull.v1.XmlPullParser;

public class BitmapDrawable extends Drawable
{
    private static final String[] TILE_NAMES = {"clamp", "repeat", "mirror"};
    private static final int MAXIMUM_FILTERED_PIXELS = 2048 * 1024;

    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Rect mDestination = new Rect();

    private Bitmap mBitmap;
    private Shader.TileMode mTileX;
    private Shader.TileMode mTileY;
    private int mGravity = Gravity.FILL;
    private int mTargetDensity = 160;
    private boolean mMipMap;
    private boolean mAutoMirrored;

    private Bitmap mFilteredSource;
    private ColorFilter mFilteredBy;
    private Bitmap mFiltered;

    private android.content.res.ColorStateList mTint;
    private android.graphics.BlendMode mTintBlendMode = android.graphics.BlendMode.SRC_IN;
    private ColorFilter mTintFilter;
    private boolean mMutated;

    public BitmapDrawable()
    {
    }

    public BitmapDrawable(Resources resources)
    {
        if (resources != null) mTargetDensity = resources.getDisplayMetrics().densityDpi;
    }

    public BitmapDrawable(Bitmap bitmap)
    {
        mBitmap = bitmap;
    }

    public BitmapDrawable(Resources resources, Bitmap bitmap)
    {
        this(resources);
        mBitmap = bitmap;
    }

    public BitmapDrawable(String path)
    {
        mBitmap = BitmapFactory.decodeFile(path);
    }

    public BitmapDrawable(Resources resources, String path)
    {
        this(resources);
        mBitmap = BitmapFactory.decodeFile(path);
    }

    public BitmapDrawable(java.io.InputStream stream)
    {
        mBitmap = BitmapFactory.decodeStream(stream);
    }

    public BitmapDrawable(Resources resources, java.io.InputStream stream)
    {
        this(resources);
        mBitmap = BitmapFactory.decodeStream(stream);
    }

    public final Paint getPaint()
    {
        return mPaint;
    }

    public final Bitmap getBitmap()
    {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap)
    {
        if (mBitmap == bitmap) return;

        mBitmap = bitmap;
        invalidateSelf();
    }

    public void setTargetDensity(Canvas canvas)
    {
        if (canvas != null) setTargetDensity(canvas.getDensity());
    }

    public void setTargetDensity(android.util.DisplayMetrics metrics)
    {
        if (metrics != null) setTargetDensity(metrics.densityDpi);
    }

    public void setTargetDensity(int density)
    {
        mTargetDensity = density == 0 ? 160 : density;
    }

    public int getGravity()
    {
        return mGravity;
    }

    public void setGravity(int gravity)
    {
        if (mGravity == gravity) return;

        mGravity = gravity;
        invalidateSelf();
    }

    public void setMipMap(boolean mipMap)
    {
        mMipMap = mipMap;
    }

    public boolean hasMipMap()
    {
        return mMipMap;
    }

    public void setAntiAlias(boolean enabled)
    {
        mPaint.setAntiAlias(enabled);
        invalidateSelf();
    }

    public boolean hasAntiAlias()
    {
        return mPaint.isAntiAlias();
    }

    @Override
    public void setFilterBitmap(boolean enabled)
    {
        mPaint.setFilterBitmap(enabled);
        invalidateSelf();
    }

    @Override
    public boolean isFilterBitmap()
    {
        return mPaint.isFilterBitmap();
    }

    @Override
    public void setDither(boolean dither)
    {
        mPaint.setDither(dither);
        invalidateSelf();
    }

    public Shader.TileMode getTileModeX()
    {
        return mTileX;
    }

    public Shader.TileMode getTileModeY()
    {
        return mTileY;
    }

    public void setTileModeX(Shader.TileMode mode)
    {
        setTileModeXY(mode, mTileY);
    }

    public final void setTileModeY(Shader.TileMode mode)
    {
        setTileModeXY(mTileX, mode);
    }

    public void setTileModeXY(Shader.TileMode x, Shader.TileMode y)
    {
        mTileX = x;
        mTileY = y;
        invalidateSelf();
    }

    @Override
    public void setAutoMirrored(boolean mirrored)
    {
        mAutoMirrored = mirrored;
    }

    @Override
    public final boolean isAutoMirrored()
    {
        return mAutoMirrored;
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mBitmap == null ? -1 : mBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mBitmap == null ? -1 : mBitmap.getHeight();
    }

    @Override
    public int getOpacity()
    {
        if (mGravity != Gravity.FILL) return PixelFormat.TRANSLUCENT;
        if (mBitmap == null || mBitmap.hasAlpha() || mPaint.getAlpha() < 255) return PixelFormat.TRANSLUCENT;

        return PixelFormat.OPAQUE;
    }

    @Override
    public void setAlpha(int alpha)
    {
        super.setAlpha(alpha);
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha()
    {
        return mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter filter)
    {
        super.setColorFilter(filter);
        mPaint.setColorFilter(filter);
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter()
    {
        return mPaint.getColorFilter();
    }

    @Override
    public void getOutline(Outline outline)
    {
        outline.setRect(getBounds());
        outline.setAlpha(mBitmap != null && !mBitmap.hasAlpha() ? getAlpha() / 255.0f : 0.0f);
    }

    @Override
    public android.graphics.Insets getOpticalInsets()
    {
        return android.graphics.Insets.NONE;
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations();
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        super.onBoundsChange(bounds);
    }

    @Override
    public void setTintList(android.content.res.ColorStateList tint)
    {
        mTint = tint;
        mTintFilter = tintFilterFor(mTint, mTintBlendMode, getState());
        invalidateSelf();
    }

    @Override
    public void setTintBlendMode(android.graphics.BlendMode blendMode)
    {
        mTintBlendMode = blendMode == null ? android.graphics.BlendMode.SRC_IN : blendMode;
        mTintFilter = tintFilterFor(mTint, mTintBlendMode, getState());
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        if (mTint == null) return false;

        mTintFilter = tintFilterFor(mTint, mTintBlendMode, stateSet);
        return true;
    }

    @Override
    public boolean isStateful()
    {
        return mTint != null && mTint.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        return hasFocusState(mTint);
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    public boolean canApplyTheme()
    {
        return false;
    }

    @Override
    public Drawable mutate()
    {
        mMutated = true;
        return this;
    }

    @Override
    public final ConstantState getConstantState()
    {
        return new BitmapState(this);
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        super.inflate(resources, parser, attributes, theme);

        if (resources != null) mTargetDensity = resources.getDisplayMetrics().densityDpi;

        int source = DrawableInflater.reference(attributes, "src", 0);
        if (source != 0) mBitmap = resources.getBitmap(source);

        int tileMode = DrawableInflater.enumeration(attributes, "tileMode", TILE_NAMES, -1);

        if (tileMode == 0) setTileModeXY(Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        else if (tileMode == 1) setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        else if (tileMode == 2) setTileModeXY(Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);

        mGravity = DrawableInflater.gravity(attributes, "gravity", mGravity);
        mMipMap = DrawableInflater.flag(attributes, "mipMap", mMipMap);
        mAutoMirrored = DrawableInflater.flag(attributes, "autoMirrored", mAutoMirrored);
        mPaint.setFilterBitmap(DrawableInflater.flag(attributes, "filter", true));
        mPaint.setAntiAlias(DrawableInflater.flag(attributes, "antialias", true));
        mPaint.setDither(DrawableInflater.flag(attributes, "dither", true));

        int alpha = Math.round(DrawableInflater.dimensionOrFloat(resources, attributes, "alpha", 1.0f) * 255);
        mPaint.setAlpha(alpha);

        android.content.res.ColorStateList tint = DrawableInflater.colorStateList(resources, attributes, "tint");
        mTintBlendMode = DrawableInflater.tintMode(attributes, "tintMode", mTintBlendMode);
        if (tint != null) mTint = tint;
        mTintFilter = tintFilterFor(mTint, mTintBlendMode, getState());
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mBitmap == null) return;

        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        ColorFilter filter = mPaint.getColorFilter();
        boolean tinted = filter == null && mTintFilter != null;
        if (tinted) mPaint.setColorFilter(mTintFilter);

        Bitmap bitmap = filtered(mBitmap, filter);
        if (bitmap != mBitmap) mPaint.setColorFilter(null);

        if (mTileX != null || mTileY != null)
        {
            Shader.TileMode x = mTileX == null ? Shader.TileMode.CLAMP : mTileX;
            Shader.TileMode y = mTileY == null ? Shader.TileMode.CLAMP : mTileY;

            mPaint.setShader(new BitmapShader(bitmap, x, y));
            mPaint.setStyle(Paint.Style.FILL);

            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);
            mPaint.setShader(null);
        }
        else
        {
            if (mGravity == Gravity.FILL) mDestination.set(bounds);
            else Gravity.apply(mGravity, bitmap.getWidth(), bitmap.getHeight(), bounds, mDestination, getLayoutDirection());

            canvas.drawBitmap(bitmap, null, new RectF(mDestination), mPaint);
        }

        if (bitmap != mBitmap || tinted) mPaint.setColorFilter(filter);
    }

    static final class BitmapState extends ConstantState
    {
        final Bitmap mBitmap;
        final Paint mPaint;
        final Shader.TileMode mTileX;
        final Shader.TileMode mTileY;
        final int mGravity;
        final int mTargetDensity;
        final boolean mMipMap;
        final boolean mAutoMirrored;
        final android.content.res.ColorStateList mTint;
        final android.graphics.BlendMode mTintBlendMode;
        final int mChangingConfigurations;

        BitmapState(BitmapDrawable source)
        {
            mBitmap = source.mBitmap;
            mPaint = new Paint(source.mPaint);
            mTileX = source.mTileX;
            mTileY = source.mTileY;
            mGravity = source.mGravity;
            mTargetDensity = source.mTargetDensity;
            mMipMap = source.mMipMap;
            mAutoMirrored = source.mAutoMirrored;
            mTint = source.mTint;
            mTintBlendMode = source.mTintBlendMode;
            mChangingConfigurations = source.getChangingConfigurations();
        }

        @Override
        public Drawable newDrawable()
        {
            return newDrawable(null);
        }

        @Override
        public Drawable newDrawable(Resources resources)
        {
            BitmapDrawable drawable = new BitmapDrawable();
            drawable.mBitmap = mBitmap;
            drawable.mPaint.set(mPaint);
            drawable.mTileX = mTileX;
            drawable.mTileY = mTileY;
            drawable.mGravity = mGravity;
            drawable.mTargetDensity = resources != null ? resources.getDisplayMetrics().densityDpi : mTargetDensity;
            drawable.mMipMap = mMipMap;
            drawable.mAutoMirrored = mAutoMirrored;
            drawable.mTint = mTint;
            drawable.mTintBlendMode = mTintBlendMode;
            drawable.mTintFilter = tintFilterFor(mTint, mTintBlendMode, drawable.getState());
            drawable.setChangingConfigurations(mChangingConfigurations);
            if (mPaint.getColorFilter() != null) drawable.setColorFilter(mPaint.getColorFilter());
            drawable.setAlpha(mPaint.getAlpha());
            return drawable;
        }

        @Override
        public int getChangingConfigurations()
        {
            return mChangingConfigurations;
        }
    }

    private Bitmap filtered(Bitmap source, ColorFilter filter)
    {
        float[] matrix = matrixOf(filter);
        if (matrix == null) return source;

        if (mFiltered != null && mFilteredSource == source && mFilteredBy == filter) return mFiltered;

        int width = source.getWidth();
        int height = source.getHeight();
        if ((long)width * height > MAXIMUM_FILTERED_PIXELS) return source;

        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int index = 0; index < pixels.length; index++)
        {
            int pixel = pixels[index];
            if (pixel == 0 && matrix[19] <= 0) continue;

            pixels[index] = transform(matrix, pixel);
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height);

        mFilteredSource = source;
        mFilteredBy = filter;
        mFiltered = result;
        return result;
    }

    private static float[] matrixOf(ColorFilter filter)
    {
        if (filter instanceof ColorMatrixColorFilter)
        {
            ColorMatrix matrix = new ColorMatrix();
            ((ColorMatrixColorFilter) filter).getColorMatrix(matrix);
            return matrix.getArray();
        }

        if (filter instanceof LightingColorFilter)
        {
            LightingColorFilter lighting = (LightingColorFilter) filter;
            int multiply = lighting.getColorMultiply();
            int add = lighting.getColorAdd();

            float[] matrix = new float[20];
            matrix[0] = ((multiply >> 16) & 0xFF) / 255.0f;
            matrix[6] = ((multiply >> 8) & 0xFF) / 255.0f;
            matrix[12] = (multiply & 0xFF) / 255.0f;
            matrix[18] = 1;
            matrix[4] = (add >> 16) & 0xFF;
            matrix[9] = (add >> 8) & 0xFF;
            matrix[14] = add & 0xFF;
            return matrix;
        }

        return null;
    }

    private static int transform(float[] matrix, int color)
    {
        float red = (color >> 16) & 0xFF;
        float green = (color >> 8) & 0xFF;
        float blue = color & 0xFF;
        float alpha = (color >>> 24) & 0xFF;

        int outRed = clamp(matrix[0] * red + matrix[1] * green + matrix[2] * blue + matrix[3] * alpha + matrix[4]);
        int outGreen = clamp(matrix[5] * red + matrix[6] * green + matrix[7] * blue + matrix[8] * alpha + matrix[9]);
        int outBlue = clamp(matrix[10] * red + matrix[11] * green + matrix[12] * blue + matrix[13] * alpha + matrix[14]);
        int outAlpha = clamp(matrix[15] * red + matrix[16] * green + matrix[17] * blue + matrix[18] * alpha + matrix[19]);

        return (outAlpha << 24) | (outRed << 16) | (outGreen << 8) | outBlue;
    }

    private static int clamp(float value)
    {
        int rounded = Math.round(value);
        return rounded < 0 ? 0 : (rounded > 255 ? 255 : rounded);
    }
}
