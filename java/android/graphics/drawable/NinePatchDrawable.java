package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.NinePatch;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class NinePatchDrawable extends Drawable
{
    private static final int CHUNK_HEADER_SIZE = 32;
    private static final int PNG_SIGNATURE_SIZE = 8;

    private NinePatch mNinePatch;
    private Rect mPadding;
    private Insets mOpticalInsets = Insets.NONE;
    private Paint mPaint;
    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
    private int mBitmapWidth = -1;
    private int mBitmapHeight = -1;
    private boolean mAutoMirrored;
    private boolean mDither;
    private boolean mFilterBitmap = true;
    private ColorStateList mTint;
    private BlendMode mTintBlendMode = BlendMode.SRC_IN;
    private ColorFilter mTintFilter;
    private boolean mMutated;

    NinePatchDrawable()
    {
    }

    public NinePatchDrawable(Bitmap bitmap, byte[] chunk, Rect padding, String srcName)
    {
        this(new NinePatch(bitmap, chunk, srcName), padding != null ? padding : paddingFromChunk(chunk), null);
    }

    public NinePatchDrawable(Resources resources, Bitmap bitmap, byte[] chunk, Rect padding, String srcName)
    {
        this(new NinePatch(bitmap, chunk, srcName), padding != null ? padding : paddingFromChunk(chunk), resources);
    }

    public NinePatchDrawable(NinePatch patch)
    {
        this(patch, null, null);
    }

    public NinePatchDrawable(Resources resources, NinePatch patch)
    {
        this(patch, null, resources);
    }

    private NinePatchDrawable(NinePatch patch, Rect padding, Resources resources)
    {
        mNinePatch = patch;
        mPadding = padding == null ? null : new Rect(padding);
        if (resources != null) mTargetDensity = resources.getDisplayMetrics().densityDpi;
        computeBitmapSize();
    }

    static byte[] extractChunk(byte[] png)
    {
        if (png == null || png.length < PNG_SIGNATURE_SIZE + 12) return null;
        if ((png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') return null;

        int offset = PNG_SIGNATURE_SIZE;

        while (offset + 12 <= png.length)
        {
            int length = ((png[offset] & 0xFF) << 24) | ((png[offset + 1] & 0xFF) << 16) | ((png[offset + 2] & 0xFF) << 8)
                | (png[offset + 3] & 0xFF);
            if (length < 0 || offset + 12 + length > png.length) return null;

            if (png[offset + 4] == 'n' && png[offset + 5] == 'p' && png[offset + 6] == 'T' && png[offset + 7] == 'c')
            {
                byte[] chunk = new byte[length];
                System.arraycopy(png, offset + 8, chunk, 0, length);
                return chunk;
            }

            if (png[offset + 4] == 'I' && png[offset + 5] == 'D' && png[offset + 6] == 'A' && png[offset + 7] == 'T') return null;

            offset += 12 + length;
        }

        return null;
    }

    static Rect paddingFromChunk(byte[] chunk)
    {
        if (chunk == null || chunk.length < CHUNK_HEADER_SIZE) return null;

        boolean littleEndian = true;
        for (int index = 0; index < 4; index++)
        {
            int value = readInt(chunk, 12 + index * 4, true);
            if (value < -1 || value > 0xFFFF) littleEndian = false;
        }

        int left = readInt(chunk, 12, littleEndian);
        int right = readInt(chunk, 16, littleEndian);
        int top = readInt(chunk, 20, littleEndian);
        int bottom = readInt(chunk, 24, littleEndian);

        if (left < 0 || right < 0 || top < 0 || bottom < 0) return null;
        return new Rect(left, top, right, bottom);
    }

    private static int readInt(byte[] data, int offset, boolean littleEndian)
    {
        int first = data[offset] & 0xFF;
        int second = data[offset + 1] & 0xFF;
        int third = data[offset + 2] & 0xFF;
        int fourth = data[offset + 3] & 0xFF;

        if (littleEndian) return first | (second << 8) | (third << 16) | (fourth << 24);
        return (first << 24) | (second << 16) | (third << 8) | fourth;
    }

    private void computeBitmapSize()
    {
        if (mNinePatch == null || mNinePatch.getBitmap() == null)
        {
            mBitmapWidth = -1;
            mBitmapHeight = -1;
            return;
        }

        int sourceDensity = mNinePatch.getDensity();
        mBitmapWidth = scale(mNinePatch.getWidth(), sourceDensity);
        mBitmapHeight = scale(mNinePatch.getHeight(), sourceDensity);
    }

    private int scale(int size, int sourceDensity)
    {
        if (sourceDensity <= 0 || mTargetDensity <= 0 || sourceDensity == mTargetDensity) return size;
        return (size * mTargetDensity + (sourceDensity >> 1)) / sourceDensity;
    }

    private Rect scaledPadding()
    {
        if (mPadding == null || mNinePatch == null) return mPadding;

        int sourceDensity = mNinePatch.getDensity();
        if (sourceDensity <= 0 || sourceDensity == mTargetDensity) return mPadding;

        return new Rect(scale(mPadding.left, sourceDensity), scale(mPadding.top, sourceDensity), scale(mPadding.right, sourceDensity),
                        scale(mPadding.bottom, sourceDensity));
    }

    public void setTargetDensity(Canvas canvas)
    {
        setTargetDensity(canvas.getDensity());
    }

    public void setTargetDensity(DisplayMetrics metrics)
    {
        setTargetDensity(metrics.densityDpi);
    }

    public void setTargetDensity(int density)
    {
        if (density == 0) density = DisplayMetrics.DENSITY_DEFAULT;
        if (mTargetDensity == density) return;

        mTargetDensity = density;
        computeBitmapSize();
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mNinePatch == null) return;

        Rect bounds = getBounds();
        Paint paint = getPaint();
        ColorFilter previous = paint.getColorFilter();
        boolean tinted = previous == null && mTintFilter != null;
        if (tinted) paint.setColorFilter(mTintFilter);

        boolean mirrored = needsMirroring();
        int saved = -1;
        if (mirrored)
        {
            saved = canvas.save();
            canvas.translate(bounds.right + bounds.left, 0);
            canvas.scale(-1.0f, 1.0f);
        }

        mNinePatch.draw(canvas, bounds, paint);

        if (saved >= 0) canvas.restoreToCount(saved);
        if (tinted) paint.setColorFilter(previous);
    }

    private boolean needsMirroring()
    {
        return mAutoMirrored && getLayoutDirection() == 1;
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations();
    }

    @Override
    public boolean getPadding(Rect padding)
    {
        Rect scaled = scaledPadding();
        if (scaled == null) return super.getPadding(padding);

        padding.set(scaled);
        if (needsMirroring())
        {
            int left = padding.right;
            padding.right = padding.left;
            padding.left = left;
        }
        return (padding.left | padding.top | padding.right | padding.bottom) != 0;
    }

    @Override
    public void getOutline(Outline outline)
    {
        super.getOutline(outline);
    }

    @Override
    public Insets getOpticalInsets()
    {
        return mOpticalInsets;
    }

    @Override
    public void setAlpha(int alpha)
    {
        if (mPaint == null && alpha == 0xFF) return;

        getPaint().setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha()
    {
        return mPaint == null ? 0xFF : mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter)
    {
        if (mPaint == null && colorFilter == null) return;

        getPaint().setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter()
    {
        return mPaint == null ? null : mPaint.getColorFilter();
    }

    @Override
    public void setTintList(ColorStateList tint)
    {
        mTint = tint;
        mTintFilter = tintFilter();
        invalidateSelf();
    }

    @Override
    public void setTintBlendMode(BlendMode blendMode)
    {
        mTintBlendMode = blendMode == null ? BlendMode.SRC_IN : blendMode;
        mTintFilter = tintFilter();
        invalidateSelf();
    }

    private ColorFilter tintFilter()
    {
        if (mTint == null) return null;
        return new BlendModeColorFilter(mTint.getColorForState(getState(), mTint.getDefaultColor()), mTintBlendMode);
    }

    @Override
    public void setDither(boolean dither)
    {
        if (mPaint == null && !dither) return;

        mDither = dither;
        getPaint().setDither(dither);
        invalidateSelf();
    }

    @Override
    public void setAutoMirrored(boolean mirrored)
    {
        mAutoMirrored = mirrored;
    }

    @Override
    public boolean isAutoMirrored()
    {
        return mAutoMirrored;
    }

    @Override
    public void setFilterBitmap(boolean filter)
    {
        mFilterBitmap = filter;
        getPaint().setFilterBitmap(filter);
        invalidateSelf();
    }

    @Override
    public boolean isFilterBitmap()
    {
        return mFilterBitmap;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, XmlPullParserException
    {
        super.inflate(resources, parser, attributes, theme);

        int source = DrawableInflater.reference(attributes, "src", 0);
        if (source == 0) throw new XmlPullParserException(parser.getPositionDescription() + ": <nine-patch> requires a valid src attribute");

        byte[] data;
        try (java.io.InputStream stream = resources.openRawResource(source))
        {
            data = readAll(stream);
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap == null) throw new XmlPullParserException(parser.getPositionDescription() + ": <nine-patch> requires a valid src attribute");

        byte[] chunk = extractChunk(data);
        mNinePatch = new NinePatch(bitmap, chunk, null);
        mPadding = paddingFromChunk(chunk);
        mTargetDensity = resources.getDisplayMetrics().densityDpi;
        mDither = DrawableInflater.flag(attributes, "dither", false);
        mAutoMirrored = DrawableInflater.flag(attributes, "autoMirrored", false);
        getPaint().setDither(mDither);

        int alphaPercent = Math.round(DrawableInflater.dimensionOrFloat(resources, attributes, "alpha", 1.0f) * 255);
        if (alphaPercent != 255) getPaint().setAlpha(Math.max(0, Math.min(255, alphaPercent)));

        computeBitmapSize();
    }

    private static byte[] readAll(java.io.InputStream stream) throws java.io.IOException
    {
        java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[16384];
        int got;

        while ((got = stream.read(chunk)) > 0) collected.write(chunk, 0, got);

        return collected.toByteArray();
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    public boolean canApplyTheme()
    {
        return mTint != null && mTint.canApplyTheme();
    }

    public Paint getPaint()
    {
        if (mPaint == null)
        {
            mPaint = new Paint();
            mPaint.setDither(mDither);
            mPaint.setFilterBitmap(mFilterBitmap);
        }
        return mPaint;
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mBitmapHeight;
    }

    @Override
    public int getOpacity()
    {
        if (mNinePatch == null || mNinePatch.hasAlpha() || (mPaint != null && mPaint.getAlpha() < 255)) return PixelFormat.TRANSLUCENT;
        return PixelFormat.OPAQUE;
    }

    @Override
    public Region getTransparentRegion()
    {
        Rect bounds = getBounds();
        return mNinePatch == null ? null : mNinePatch.getTransparentRegion(bounds);
    }

    @Override
    public ConstantState getConstantState()
    {
        final NinePatch patch = mNinePatch;
        final Rect padding = mPadding == null ? null : new Rect(mPadding);
        final ColorStateList tint = mTint;
        final BlendMode blendMode = mTintBlendMode;
        final boolean autoMirrored = mAutoMirrored;
        final boolean dither = mDither;
        final int alpha = getAlpha();
        final int changing = getChangingConfigurations();

        return new ConstantState()
        {
            @Override
            public Drawable newDrawable()
            {
                return newDrawable(null);
            }

            @Override
            public Drawable newDrawable(Resources resources)
            {
                NinePatchDrawable copy = new NinePatchDrawable(patch, padding, resources);
                copy.mTint = tint;
                copy.mTintBlendMode = blendMode;
                copy.mTintFilter = copy.tintFilter();
                copy.mAutoMirrored = autoMirrored;
                copy.mDither = dither;
                if (alpha != 0xFF) copy.getPaint().setAlpha(alpha);
                return copy;
            }

            @Override
            public int getChangingConfigurations()
            {
                return changing;
            }
        };
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            if (mPadding != null) mPadding = new Rect(mPadding);
            if (mPaint != null) mPaint = new Paint(mPaint);
            mMutated = true;
        }
        return this;
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        if (mTint == null) return false;

        mTintFilter = tintFilter();
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
        return mTint != null && mTint.hasFocusStateSpecified();
    }
}
