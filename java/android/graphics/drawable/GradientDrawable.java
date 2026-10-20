package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class GradientDrawable extends Drawable
{
    public static final int RECTANGLE = 0;
    public static final int OVAL = 1;
    public static final int LINE = 2;
    public static final int RING = 3;

    public static final int LINEAR_GRADIENT = 0;
    public static final int RADIAL_GRADIENT = 1;
    public static final int SWEEP_GRADIENT = 2;

    private static final String[] SHAPE_NAMES = {"rectangle", "oval", "line", "ring"};
    private static final String[] GRADIENT_NAMES = {"linear", "radial", "sweep"};

    private final Paint mPaint = new Paint();
    private final Path mPath = new Path();

    private int mShape = RECTANGLE;
    private int mSolidColor;
    private boolean mHasSolid;
    private int mStrokeColor;
    private float mStrokeWidth;
    private float mRadius;
    private float[] mCornerRadii;
    private int[] mGradientColors;
    private int mGradientKind = LINEAR_GRADIENT;
    private Orientation mOrientation = Orientation.TOP_BOTTOM;
    private float mGradientRadius;
    private int mIntrinsicWidth = -1;
    private int mIntrinsicHeight = -1;

    public GradientDrawable()
    {
        mPaint.setAntiAlias(true);
    }

    public GradientDrawable(Orientation orientation, int[] colors)
    {
        this();
        mOrientation = orientation == null ? Orientation.TOP_BOTTOM : orientation;
        mGradientColors = colors == null ? null : colors.clone();
    }

    public void setShape(int shape)
    {
        mShape = shape;
    }

    public int getShape()
    {
        return mShape;
    }

    public void setColor(int color)
    {
        mSolidColor = color;
        mHasSolid = true;
        mGradientColors = null;
    }

    public void setColors(int[] colors)
    {
        mGradientColors = colors == null ? null : colors.clone();
    }

    public void setCornerRadius(float radius)
    {
        mRadius = radius;
        mCornerRadii = null;
    }

    public void setCornerRadii(float[] radii)
    {
        mCornerRadii = radii == null ? null : radii.clone();
    }

    public void setStroke(int width, int color)
    {
        mStrokeWidth = width;
        mStrokeColor = color;
    }

    public void setGradientType(int kind)
    {
        mGradientKind = kind;
    }

    public void setGradientRadius(float radius)
    {
        mGradientRadius = radius;
    }

    public void setOrientation(Orientation orientation)
    {
        mOrientation = orientation == null ? Orientation.TOP_BOTTOM : orientation;
    }

    public void setSize(int width, int height)
    {
        mIntrinsicWidth = width;
        mIntrinsicHeight = height;
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
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
        mShape = DrawableInflater.enumeration(attributes, "shape", SHAPE_NAMES, RECTANGLE);

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if ("solid".equals(name))
            {
                mSolidColor = DrawableInflater.color(resources, attributes, "color", 0);
                mHasSolid = true;
            }
            else if ("stroke".equals(name))
            {
                mStrokeWidth = DrawableInflater.dimension(resources, attributes, "width", 0);
                mStrokeColor = DrawableInflater.color(resources, attributes, "color", 0);
            }
            else if ("corners".equals(name))
            {
                readCorners(resources, attributes);
            }
            else if ("gradient".equals(name))
            {
                readGradient(resources, attributes);
            }
            else if ("size".equals(name))
            {
                mIntrinsicWidth = Math.round(DrawableInflater.dimension(resources, attributes, "width", -1));
                mIntrinsicHeight = Math.round(DrawableInflater.dimension(resources, attributes, "height", -1));
            }
        }
    }

    private void readCorners(Resources resources, AttributeSet attributes)
    {
        float all = DrawableInflater.dimension(resources, attributes, "radius", 0);

        float topLeft = DrawableInflater.dimension(resources, attributes, "topLeftRadius", all);
        float topRight = DrawableInflater.dimension(resources, attributes, "topRightRadius", all);
        float bottomRight = DrawableInflater.dimension(resources, attributes, "bottomRightRadius", all);
        float bottomLeft = DrawableInflater.dimension(resources, attributes, "bottomLeftRadius", all);

        if (topLeft == topRight && topRight == bottomRight && bottomRight == bottomLeft)
        {
            mRadius = all;
            mCornerRadii = null;
            return;
        }

        mCornerRadii = new float[] {topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft};
    }

    private void readGradient(Resources resources, AttributeSet attributes)
    {
        int start = DrawableInflater.color(resources, attributes, "startColor", 0);
        int centre = DrawableInflater.color(resources, attributes, "centerColor", 0);
        int end = DrawableInflater.color(resources, attributes, "endColor", 0);

        if (centre != 0) mGradientColors = new int[] {start, centre, end};
        else mGradientColors = new int[] {start, end};

        mGradientKind = DrawableInflater.enumeration(attributes, "type", GRADIENT_NAMES, LINEAR_GRADIENT);

        mGradientRadius = DrawableInflater.dimensionOrFloat(resources, attributes, "gradientRadius", 0);

        int angle = Math.round(DrawableInflater.dimensionOrFloat(resources, attributes, "angle", 0));
        mOrientation = orientationForAngle(angle);
    }

    private static Orientation orientationForAngle(int angle)
    {
        int normalised = ((angle % 360) + 360) % 360;

        switch (normalised / 45)
        {
            case 1: return Orientation.BL_TR;
            case 2: return Orientation.BOTTOM_TOP;
            case 3: return Orientation.BR_TL;
            case 4: return Orientation.RIGHT_LEFT;
            case 5: return Orientation.TR_BL;
            case 6: return Orientation.TOP_BOTTOM;
            case 7: return Orientation.TL_BR;
            default: return Orientation.LEFT_RIGHT;
        }
    }

    private Shader buildShader(Rect bounds)
    {
        if (mGradientColors == null || mGradientColors.length < 2) return null;

        if (mGradientKind == RADIAL_GRADIENT)
        {
            float radius = mGradientRadius > 0 ? mGradientRadius : Math.min(bounds.width(), bounds.height()) * 0.5f;
            if (radius <= 0) return null;

            return new RadialGradient(bounds.centerX(), bounds.centerY(), radius, mGradientColors, null,
                                      Shader.TileMode.CLAMP);
        }

        if (mGradientKind == SWEEP_GRADIENT)
        {
            return new SweepGradient(bounds.centerX(), bounds.centerY(), mGradientColors, null);
        }

        float startX = bounds.left;
        float startY = bounds.top;
        float endX = bounds.right;
        float endY = bounds.top;

        switch (mOrientation)
        {
            case TOP_BOTTOM: startX = bounds.left; startY = bounds.top; endX = bounds.left; endY = bounds.bottom; break;
            case BOTTOM_TOP: startX = bounds.left; startY = bounds.bottom; endX = bounds.left; endY = bounds.top; break;
            case RIGHT_LEFT: startX = bounds.right; startY = bounds.top; endX = bounds.left; endY = bounds.top; break;
            case TL_BR: startX = bounds.left; startY = bounds.top; endX = bounds.right; endY = bounds.bottom; break;
            case BR_TL: startX = bounds.right; startY = bounds.bottom; endX = bounds.left; endY = bounds.top; break;
            case TR_BL: startX = bounds.right; startY = bounds.top; endX = bounds.left; endY = bounds.bottom; break;
            case BL_TR: startX = bounds.left; startY = bounds.bottom; endX = bounds.right; endY = bounds.top; break;
            default: break;
        }

        return new LinearGradient(startX, startY, endX, endY, mGradientColors, null, Shader.TileMode.CLAMP);
    }

    private void buildPath(Rect bounds)
    {
        mPath.reset();

        if (mShape == OVAL)
        {
            mPath.addOval(bounds.left, bounds.top, bounds.right, bounds.bottom, Path.Direction.CW);
            return;
        }

        if (mShape == LINE)
        {
            float middle = bounds.centerY();
            mPath.moveTo(bounds.left, middle);
            mPath.lineTo(bounds.right, middle);
            return;
        }

        if (mCornerRadii != null)
        {
            mPath.addRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mCornerRadii, Path.Direction.CW);
            return;
        }

        if (mRadius > 0)
        {
            mPath.addRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mRadius, mRadius, Path.Direction.CW);
            return;
        }

        mPath.addRect(bounds.left, bounds.top, bounds.right, bounds.bottom, Path.Direction.CW);
    }

    @Override
    public void draw(Canvas canvas)
    {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        buildPath(bounds);

        Shader shader = buildShader(bounds);

        if (shader != null || mHasSolid)
        {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShader(shader);
            mPaint.setColor(shader == null ? withAlpha(mSolidColor) : 0xFF000000);

            if (mShape != LINE) canvas.drawPath(mPath, mPaint);
            mPaint.setShader(null);
        }

        if (mStrokeWidth > 0 && mStrokeColor != 0)
        {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mStrokeWidth);
            mPaint.setColor(withAlpha(mStrokeColor));

            canvas.drawPath(mPath, mPaint);
        }
    }

    private int withAlpha(int color)
    {
        int alpha = ((color >>> 24) & 0xFF) * getAlpha() / 255;
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public enum Orientation
    {
        TOP_BOTTOM,
        TR_BL,
        RIGHT_LEFT,
        BR_TL,
        BOTTOM_TOP,
        BL_TR,
        LEFT_RIGHT,
        TL_BR
    }
}
