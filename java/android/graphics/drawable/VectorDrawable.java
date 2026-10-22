package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.PathParser;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

public class VectorDrawable extends Drawable
{
    private static final String[] FILL_TYPE_NAMES = {"nonZero", "evenOdd"};

    private final Paint mPaint = new Paint();

    private Group mRoot = new Group();
    private int mResourceId;
    private float mViewportWidth;
    private float mViewportHeight;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;
    private int mRootAlpha = 255;
    private boolean mAutoMirrored;
    private Insets mOpticalInsets = Insets.NONE;
    private ColorStateList mTint;
    private BlendMode mTintBlendMode = BlendMode.SRC_IN;
    private int mTintColor;
    private ColorFilter mColorFilter;
    private boolean mMutated;

    public VectorDrawable()
    {
        this(0);
    }

    public VectorDrawable(int resourceId)
    {
        mResourceId = resourceId;
        mPaint.setAntiAlias(true);
    }

    public int getResourceId()
    {
        return mResourceId;
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
    public Insets getOpticalInsets()
    {
        return mOpticalInsets;
    }

    @Override
    public int getAlpha()
    {
        return mRootAlpha;
    }

    @Override
    public void setAlpha(int alpha)
    {
        if (mRootAlpha == alpha) return;

        mRootAlpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter filter)
    {
        mColorFilter = filter;
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter()
    {
        return mColorFilter;
    }

    @Override
    public void setTintList(ColorStateList tint)
    {
        mTint = tint;
        updateTintColor(getState());
        invalidateSelf();
    }

    @Override
    public void setTintBlendMode(BlendMode blendMode)
    {
        mTintBlendMode = blendMode == null ? BlendMode.SRC_IN : blendMode;
        invalidateSelf();
    }

    private void updateTintColor(int[] stateSet)
    {
        if (mTint != null) mTintColor = mTint.getColorForState(stateSet, mTint.getDefaultColor());
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
    protected boolean onStateChange(int[] stateSet)
    {
        if (mTint == null || !mTint.isStateful()) return false;

        int previous = mTintColor;
        updateTintColor(stateSet);

        if (previous == mTintColor) return false;

        invalidateSelf();
        return true;
    }

    @Override
    public int getOpacity()
    {
        return mRootAlpha == 0 ? PixelFormat.TRANSPARENT : PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean canApplyTheme()
    {
        return false;
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations();
    }

    @Override
    public void setAutoMirrored(boolean mirrored)
    {
        if (mAutoMirrored == mirrored) return;

        mAutoMirrored = mirrored;
        invalidateSelf();
    }

    @Override
    public boolean isAutoMirrored()
    {
        return mAutoMirrored;
    }

    public boolean isEmpty()
    {
        return mRoot.children.isEmpty();
    }

    @Override
    public Drawable mutate()
    {
        mMutated = true;
        return this;
    }

    @Override
    public ConstantState getConstantState()
    {
        return new VectorState(this);
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        super.inflate(resources, parser, attributes, theme);
        readVector(resources, parser, attributes);
    }

    void readVector(Resources resources, XmlPullParser parser, AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
        mRoot = new Group();
        mViewportWidth = DrawableInflater.dimensionOrFloat(resources, attributes, "viewportWidth", 0);
        mViewportHeight = DrawableInflater.dimensionOrFloat(resources, attributes, "viewportHeight", 0);
        mIntrinsicWidth = Math.round(DrawableInflater.dimension(resources, attributes, "width", 0));
        mIntrinsicHeight = Math.round(DrawableInflater.dimension(resources, attributes, "height", 0));
        mAutoMirrored = DrawableInflater.flag(attributes, "autoMirrored", mAutoMirrored);

        float alpha = DrawableInflater.dimensionOrFloat(resources, attributes, "alpha", mRootAlpha / 255.0f);
        mRootAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255)));

        ColorStateList tint = DrawableInflater.colorStateList(resources, attributes, "tint");
        if (tint != null) mTint = tint;
        mTintBlendMode = DrawableInflater.tintMode(attributes, "tintMode", mTintBlendMode);
        updateTintColor(getState());

        int insetLeft = Math.round(DrawableInflater.dimension(resources, attributes, "opticalInsetLeft", 0));
        int insetTop = Math.round(DrawableInflater.dimension(resources, attributes, "opticalInsetTop", 0));
        int insetRight = Math.round(DrawableInflater.dimension(resources, attributes, "opticalInsetRight", 0));
        int insetBottom = Math.round(DrawableInflater.dimension(resources, attributes, "opticalInsetBottom", 0));
        mOpticalInsets = Insets.of(insetLeft, insetTop, insetRight, insetBottom);

        readChildren(resources, parser, attributes, mRoot, parser.getDepth());

        if (mIntrinsicWidth <= 0) mIntrinsicWidth = Math.round(mViewportWidth);
        if (mIntrinsicHeight <= 0) mIntrinsicHeight = Math.round(mViewportHeight);
    }

    private void readChildren(Resources resources, XmlPullParser parser, AttributeSet attributes, Group into, int outerDepth)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if ("path".equals(name))
            {
                Shape shape = readShape(resources, attributes);
                if (shape != null) into.children.add(shape);
            }
            else if ("clip-path".equals(name))
            {
                String data = attributes.getAttributeValue(DrawableInflater.ANDROID_NAMESPACE, "pathData");
                Path clip = data == null ? null : PathParser.createPathFromPathData(data);

                if (clip != null) into.clip = clip;
            }
            else if ("group".equals(name))
            {
                Group group = readGroup(resources, attributes);
                into.children.add(group);
                readChildren(resources, parser, attributes, group, parser.getDepth());
            }
        }
    }

    private Group readGroup(Resources resources, AttributeSet attributes)
    {
        Group group = new Group();

        float pivotX = DrawableInflater.dimensionOrFloat(resources, attributes, "pivotX", 0);
        float pivotY = DrawableInflater.dimensionOrFloat(resources, attributes, "pivotY", 0);
        float rotation = DrawableInflater.dimensionOrFloat(resources, attributes, "rotation", 0);
        float scaleX = DrawableInflater.dimensionOrFloat(resources, attributes, "scaleX", 1);
        float scaleY = DrawableInflater.dimensionOrFloat(resources, attributes, "scaleY", 1);
        float translateX = DrawableInflater.dimensionOrFloat(resources, attributes, "translateX", 0);
        float translateY = DrawableInflater.dimensionOrFloat(resources, attributes, "translateY", 0);

        group.transform.postTranslate(-pivotX, -pivotY);
        group.transform.postScale(scaleX, scaleY);
        group.transform.postRotate(rotation);
        group.transform.postTranslate(pivotX + translateX, pivotY + translateY);

        return group;
    }

    private Shape readShape(Resources resources, AttributeSet attributes)
    {
        String data = attributes.getAttributeValue(DrawableInflater.ANDROID_NAMESPACE, "pathData");
        if (data == null) return null;

        Path path = PathParser.createPathFromPathData(data);
        if (path == null) return null;

        Shape shape = new Shape();
        shape.path = path;
        shape.fillColor = DrawableInflater.color(resources, attributes, "fillColor", 0);
        shape.strokeColor = DrawableInflater.color(resources, attributes, "strokeColor", 0);
        shape.strokeWidth = DrawableInflater.dimensionOrFloat(resources, attributes, "strokeWidth", 0);
        shape.fillAlpha = DrawableInflater.dimensionOrFloat(resources, attributes, "fillAlpha", 1);
        shape.strokeAlpha = DrawableInflater.dimensionOrFloat(resources, attributes, "strokeAlpha", 1);

        shape.evenOdd = DrawableInflater.enumeration(attributes, "fillType", FILL_TYPE_NAMES, 0) == 1;

        return shape;
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mRoot.children.isEmpty()) return;

        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        if (mViewportWidth <= 0 || mViewportHeight <= 0) return;

        int saveCount = canvas.save();
        canvas.translate(bounds.left, bounds.top);

        if (mAutoMirrored && getLayoutDirection() == 1)
        {
            canvas.translate(bounds.width(), 0);
            canvas.scale(-1.0f, 1.0f);
        }

        canvas.scale(bounds.width() / mViewportWidth, bounds.height() / mViewportHeight);

        ColorFilter filter = mColorFilter;
        boolean replaceColor = filter == null && mTint != null && mTintBlendMode == BlendMode.SRC_IN;
        if (filter == null && mTint != null && !replaceColor) filter = tintFilterFor(mTint, mTintBlendMode, getState());

        mPaint.setColorFilter(filter);
        drawGroup(canvas, mRoot, replaceColor);
        canvas.restoreToCount(saveCount);
    }

    private void drawGroup(Canvas canvas, Group group, boolean replaceColor)
    {
        int saveCount = canvas.save();

        if (!group.transform.isIdentity()) canvas.concat(group.transform);
        if (group.clip != null) canvas.clipPath(group.clip);

        for (int index = 0; index < group.children.size(); index++)
        {
            Object child = group.children.get(index);

            if (child instanceof Group) drawGroup(canvas, (Group) child, replaceColor);
            else drawShape(canvas, (Shape) child, replaceColor);
        }

        canvas.restoreToCount(saveCount);
    }

    private void drawShape(Canvas canvas, Shape shape, boolean replaceColor)
    {
        float alpha = mRootAlpha / 255.0f;

        if (shape.fillColor != 0)
        {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(scaleAlpha(tinted(shape.fillColor, replaceColor), shape.fillAlpha * alpha));
            shape.path.setFillType(shape.evenOdd ? Path.FillType.EVEN_ODD : Path.FillType.WINDING);

            canvas.drawPath(shape.path, mPaint);
        }

        if (shape.strokeColor != 0 && shape.strokeWidth > 0)
        {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(shape.strokeWidth);
            mPaint.setColor(scaleAlpha(tinted(shape.strokeColor, replaceColor), shape.strokeAlpha * alpha));

            canvas.drawPath(shape.path, mPaint);
        }
    }

    private int tinted(int color, boolean replaceColor)
    {
        if (!replaceColor) return color;

        int alpha = ((color >>> 24) * (mTintColor >>> 24) + 127) / 255;
        return (alpha << 24) | (mTintColor & 0x00FFFFFF);
    }

    private static int scaleAlpha(int color, float amount)
    {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, amount)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static final class VectorState extends ConstantState
    {
        private final Group mRoot;
        private final int mResourceId;
        private final float mViewportWidth;
        private final float mViewportHeight;
        private final int mIntrinsicWidth;
        private final int mIntrinsicHeight;
        private final int mRootAlpha;
        private final boolean mAutoMirrored;
        private final Insets mOpticalInsets;
        private final ColorStateList mTint;
        private final BlendMode mTintBlendMode;
        private final int mChangingConfigurations;

        VectorState(VectorDrawable source)
        {
            mRoot = source.mRoot;
            mResourceId = source.mResourceId;
            mViewportWidth = source.mViewportWidth;
            mViewportHeight = source.mViewportHeight;
            mIntrinsicWidth = source.mIntrinsicWidth;
            mIntrinsicHeight = source.mIntrinsicHeight;
            mRootAlpha = source.mRootAlpha;
            mAutoMirrored = source.mAutoMirrored;
            mOpticalInsets = source.mOpticalInsets;
            mTint = source.mTint;
            mTintBlendMode = source.mTintBlendMode;
            mChangingConfigurations = source.getChangingConfigurations();
        }

        @Override
        public Drawable newDrawable()
        {
            VectorDrawable drawable = new VectorDrawable(mResourceId);
            drawable.mRoot = mRoot;
            drawable.mViewportWidth = mViewportWidth;
            drawable.mViewportHeight = mViewportHeight;
            drawable.mIntrinsicWidth = mIntrinsicWidth;
            drawable.mIntrinsicHeight = mIntrinsicHeight;
            drawable.mRootAlpha = mRootAlpha;
            drawable.mAutoMirrored = mAutoMirrored;
            drawable.mOpticalInsets = mOpticalInsets;
            drawable.mTint = mTint;
            drawable.mTintBlendMode = mTintBlendMode;
            drawable.updateTintColor(drawable.getState());
            drawable.setChangingConfigurations(mChangingConfigurations);
            return drawable;
        }

        @Override
        public int getChangingConfigurations()
        {
            return mChangingConfigurations;
        }
    }

    private static final class Group
    {
        final ArrayList<Object> children = new ArrayList<>();
        final Matrix transform = new Matrix();
        Path clip;
    }

    private static final class Shape
    {
        Path path;
        int fillColor;
        int strokeColor;
        float strokeWidth;
        float fillAlpha = 1;
        float strokeAlpha = 1;
        boolean evenOdd;
    }
}
