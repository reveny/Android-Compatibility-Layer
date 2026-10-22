package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class ShapeDrawable extends Drawable
{
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Shape mShape;
    private Rect mPadding;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;
    private int mAlpha = 255;
    private ShaderFactory mShaderFactory;
    private ColorStateList mTint;
    private BlendMode mTintBlendMode = BlendMode.SRC_IN;
    private ColorFilter mTintFilter;

    public ShapeDrawable()
    {
        this(null);
    }

    public ShapeDrawable(Shape shape)
    {
        mShape = shape;
    }

    public Shape getShape()
    {
        return mShape;
    }

    public void setShape(Shape shape)
    {
        mShape = shape;
        updateShape();
    }

    public void setShaderFactory(ShaderFactory factory)
    {
        mShaderFactory = factory;
    }

    public ShaderFactory getShaderFactory()
    {
        return mShaderFactory;
    }

    public Paint getPaint()
    {
        return mPaint;
    }

    public void setPadding(int left, int top, int right, int bottom)
    {
        if ((left | top | right | bottom) == 0)
        {
            mPadding = null;
        }
        else
        {
            if (mPadding == null) mPadding = new Rect();
            mPadding.set(left, top, right, bottom);
        }

        invalidateSelf();
    }

    public void setPadding(Rect padding)
    {
        if (padding == null)
        {
            mPadding = null;
        }
        else
        {
            if (mPadding == null) mPadding = new Rect();
            mPadding.set(padding);
        }

        invalidateSelf();
    }

    public void setIntrinsicWidth(int width)
    {
        mIntrinsicWidth = width;
        invalidateSelf();
    }

    public void setIntrinsicHeight(int height)
    {
        mIntrinsicHeight = height;
        invalidateSelf();
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mIntrinsicHeight;
    }

    @Override
    public boolean getPadding(Rect padding)
    {
        if (mPadding == null) return super.getPadding(padding);

        padding.set(mPadding);
        return true;
    }

    protected void onDraw(Shape shape, Canvas canvas, Paint paint)
    {
        shape.draw(canvas, paint);
    }

    @Override
    public void draw(Canvas canvas)
    {
        Rect bounds = getBounds();
        Paint paint = mPaint;
        int previousAlpha = paint.getAlpha();

        paint.setAlpha(modulateAlpha(previousAlpha, mAlpha));

        if (paint.getAlpha() != 0 || paint.getXfermode() != null || paint.getShadowLayerRadius() > 0)
        {
            boolean clearFilter = false;

            if (mTintFilter != null && paint.getColorFilter() == null)
            {
                paint.setColorFilter(mTintFilter);
                clearFilter = true;
            }

            if (mShape != null)
            {
                int saveCount = canvas.save();
                canvas.translate(bounds.left, bounds.top);
                onDraw(mShape, canvas, paint);
                canvas.restoreToCount(saveCount);
            }
            else
            {
                canvas.drawRect(bounds, paint);
            }

            if (clearFilter) paint.setColorFilter(null);
        }

        paint.setAlpha(previousAlpha);
    }

    private static int modulateAlpha(int paintAlpha, int alpha)
    {
        int scale = alpha + (alpha >>> 7);
        return paintAlpha * scale >>> 8;
    }

    @Override
    public void setAlpha(int alpha)
    {
        mAlpha = alpha;
        invalidateSelf();
    }

    @Override
    public int getAlpha()
    {
        return mAlpha;
    }

    @Override
    public void setTintList(ColorStateList tint)
    {
        mTint = tint;
        mTintFilter = updateTintFilter();
        invalidateSelf();
    }

    @Override
    public void setTintBlendMode(BlendMode blendMode)
    {
        mTintBlendMode = blendMode == null ? BlendMode.SRC_IN : blendMode;
        mTintFilter = updateTintFilter();
        invalidateSelf();
    }

    @Override
    public void setTintMode(android.graphics.PorterDuff.Mode tintMode)
    {
        if (tintMode == null) return;

        setTintBlendMode(BlendMode.valueOf(tintMode == android.graphics.PorterDuff.Mode.ADD ? "PLUS" : tintMode.name()));
    }

    private ColorFilter updateTintFilter()
    {
        if (mTint == null) return null;

        return new BlendModeColorFilter(mTint.getColorForState(getState(), mTint.getDefaultColor()), mTintBlendMode);
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
    public int getOpacity()
    {
        if (mShape != null) return PixelFormat.TRANSLUCENT;
        if (mPaint.getXfermode() != null) return PixelFormat.TRANSLUCENT;

        int alpha = mPaint.getAlpha();
        if (alpha == 0) return PixelFormat.TRANSPARENT;
        if (alpha == 255) return PixelFormat.OPAQUE;

        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setDither(boolean dither)
    {
        mPaint.setDither(dither);
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        super.onBoundsChange(bounds);
        updateShape();
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        if (mTint == null) return false;

        mTintFilter = updateTintFilter();
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
        return false;
    }

    protected boolean inflateTag(String name, Resources resources, XmlPullParser parser, AttributeSet attributes)
    {
        if (!"padding".equals(name)) return false;

        setPadding(Math.round(DrawableInflater.dimension(resources, attributes, "left", 0)),
                   Math.round(DrawableInflater.dimension(resources, attributes, "top", 0)),
                   Math.round(DrawableInflater.dimension(resources, attributes, "right", 0)),
                   Math.round(DrawableInflater.dimension(resources, attributes, "bottom", 0)));
        return true;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        mPaint.setColor(DrawableInflater.color(resources, attributes, "color", mPaint.getColor()));
        mPaint.setDither(DrawableInflater.flag(attributes, "dither", mPaint.isDither()));
        mIntrinsicWidth = Math.round(DrawableInflater.dimension(resources, attributes, "width", mIntrinsicWidth));
        mIntrinsicHeight = Math.round(DrawableInflater.dimension(resources, attributes, "height", mIntrinsicHeight));

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if (!inflateTag(name, resources, parser, attributes))
            {
                android.util.Log.w("drawable", "Unknown element: " + name + " for ShapeDrawable " + this);
            }

            DrawableInflater.skipElement(parser);
        }

        updateShape();
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
    }

    @Override
    public void getOutline(Outline outline)
    {
        if (mShape == null) return;

        mShape.getOutline(outline);
        outline.setAlpha(getAlpha() / 255.0f);
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations();
    }

    @Override
    public ConstantState getConstantState()
    {
        return new ShapeState(this);
    }

    @Override
    public Drawable mutate()
    {
        return this;
    }

    private void updateShape()
    {
        if (mShape != null)
        {
            Rect bounds = getBounds();
            int width = bounds.width();
            int height = bounds.height();

            mShape.resize(width, height);
            if (mShaderFactory != null) mPaint.setShader(mShaderFactory.resize(width, height));
        }

        invalidateSelf();
    }

    private static final class ShapeState extends ConstantState
    {
        private final Shape mShape;
        private final Paint mPaint;
        private final Rect mPadding;
        private final int mIntrinsicWidth;
        private final int mIntrinsicHeight;
        private final int mAlpha;
        private final ShaderFactory mShaderFactory;
        private final ColorStateList mTint;
        private final BlendMode mTintBlendMode;
        private final boolean mPaintDrawable;
        private final int mChangingConfigurations;

        ShapeState(ShapeDrawable source)
        {
            Shape shape = source.mShape;

            try
            {
                shape = shape == null ? null : shape.clone();
            }
            catch (CloneNotSupportedException failure)
            {
                shape = source.mShape;
            }

            mShape = shape;
            mPaint = new Paint(source.mPaint);
            mPadding = source.mPadding == null ? null : new Rect(source.mPadding);
            mIntrinsicWidth = source.mIntrinsicWidth;
            mIntrinsicHeight = source.mIntrinsicHeight;
            mAlpha = source.mAlpha;
            mShaderFactory = source.mShaderFactory;
            mTint = source.mTint;
            mTintBlendMode = source.mTintBlendMode;
            mPaintDrawable = source.getClass() == PaintDrawable.class;
            mChangingConfigurations = source.getChangingConfigurations();
        }

        @Override
        public Drawable newDrawable()
        {
            ShapeDrawable drawable = mPaintDrawable ? new PaintDrawable() : new ShapeDrawable();
            drawable.mShape = copyOf(mShape);
            drawable.setChangingConfigurations(mChangingConfigurations);
            drawable.mPaint.set(mPaint);
            drawable.mPadding = mPadding == null ? null : new Rect(mPadding);
            drawable.mIntrinsicWidth = mIntrinsicWidth;
            drawable.mIntrinsicHeight = mIntrinsicHeight;
            drawable.mAlpha = mAlpha;
            drawable.mShaderFactory = mShaderFactory;
            drawable.mTint = mTint;
            drawable.mTintBlendMode = mTintBlendMode;
            drawable.mTintFilter = drawable.updateTintFilter();
            return drawable;
        }

        @Override
        public int getChangingConfigurations()
        {
            return mChangingConfigurations;
        }

        private static Shape copyOf(Shape shape)
        {
            if (shape == null) return null;

            try
            {
                return shape.clone();
            }
            catch (CloneNotSupportedException failure)
            {
                return shape;
            }
        }
    }

    public abstract static class ShaderFactory
    {
        public ShaderFactory()
        {
        }

        public abstract Shader resize(int width, int height);
    }
}
