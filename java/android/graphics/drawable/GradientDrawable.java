package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Insets;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;

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
    private static final float DEFAULT_INNER_RADIUS_RATIO = 3.0f;
    private static final float DEFAULT_THICKNESS_RATIO = 9.0f;

    private final Paint mPaint = new Paint();
    private final Paint mStrokePaint = new Paint();
    private final Path mPath = new Path();
    private final RectF mRect = new RectF();

    private int mShape = RECTANGLE;
    private ColorStateList mSolidColors;
    private ColorStateList mStrokeColors;
    private float mStrokeWidth;
    private float mStrokeDashWidth;
    private float mStrokeDashGap;
    private float mRadius;
    private float[] mCornerRadii;
    private int[] mGradientColors;
    private float[] mPositions;
    private int mGradientKind = LINEAR_GRADIENT;
    private Orientation mOrientation = Orientation.TOP_BOTTOM;
    private float mGradientRadius;
    private boolean mGradientRadiusFraction;
    private float mCenterX = 0.5f;
    private float mCenterY = 0.5f;
    private boolean mUseLevel;
    private boolean mUseLevelForShape = true;
    private int mInnerRadius = -1;
    private float mInnerRadiusRatio = DEFAULT_INNER_RADIUS_RATIO;
    private int mThickness = -1;
    private float mThicknessRatio = DEFAULT_THICKNESS_RATIO;
    private Rect mPadding;
    private int mIntrinsicWidth = -1;
    private int mIntrinsicHeight = -1;
    private int mAlpha = 255;
    private boolean mDither;
    private ColorFilter mColorFilter;
    private ColorStateList mTint;
    private BlendMode mTintBlendMode = BlendMode.SRC_IN;
    private ColorFilter mTintFilter;
    private boolean mMutated;

    public GradientDrawable()
    {
        mPaint.setAntiAlias(true);
        mStrokePaint.setAntiAlias(true);
        mStrokePaint.setStyle(Paint.Style.STROKE);
    }

    public GradientDrawable(Orientation orientation, int[] colors)
    {
        this();
        mOrientation = orientation == null ? Orientation.TOP_BOTTOM : orientation;
        mGradientColors = colors == null ? null : colors.clone();
    }

    private GradientDrawable(GradientDrawable source)
    {
        this();
        mShape = source.mShape;
        mSolidColors = source.mSolidColors;
        mStrokeColors = source.mStrokeColors;
        mStrokeWidth = source.mStrokeWidth;
        mStrokeDashWidth = source.mStrokeDashWidth;
        mStrokeDashGap = source.mStrokeDashGap;
        mRadius = source.mRadius;
        mCornerRadii = source.mCornerRadii == null ? null : source.mCornerRadii.clone();
        mGradientColors = source.mGradientColors == null ? null : source.mGradientColors.clone();
        mPositions = source.mPositions == null ? null : source.mPositions.clone();
        mGradientKind = source.mGradientKind;
        mOrientation = source.mOrientation;
        mGradientRadius = source.mGradientRadius;
        mGradientRadiusFraction = source.mGradientRadiusFraction;
        mCenterX = source.mCenterX;
        mCenterY = source.mCenterY;
        mUseLevel = source.mUseLevel;
        mUseLevelForShape = source.mUseLevelForShape;
        mInnerRadius = source.mInnerRadius;
        mInnerRadiusRatio = source.mInnerRadiusRatio;
        mThickness = source.mThickness;
        mThicknessRatio = source.mThicknessRatio;
        mPadding = source.mPadding == null ? null : new Rect(source.mPadding);
        mIntrinsicWidth = source.mIntrinsicWidth;
        mIntrinsicHeight = source.mIntrinsicHeight;
        mTint = source.mTint;
        mTintBlendMode = source.mTintBlendMode;
        mTintFilter = tintFilter();
    }

    @Override
    public boolean getPadding(Rect padding)
    {
        if (mPadding == null) return super.getPadding(padding);

        padding.set(mPadding);
        return true;
    }

    public void setPadding(int left, int top, int right, int bottom)
    {
        if (mPadding == null) mPadding = new Rect();
        mPadding.set(left, top, right, bottom);
        invalidateSelf();
    }

    public void setCornerRadii(float[] radii)
    {
        mCornerRadii = radii == null ? null : radii.clone();
        if (radii == null) mRadius = 0;
        invalidateSelf();
    }

    public float[] getCornerRadii()
    {
        return mCornerRadii == null ? null : mCornerRadii.clone();
    }

    public void setCornerRadius(float radius)
    {
        mRadius = radius < 0 ? 0 : radius;
        mCornerRadii = null;
        invalidateSelf();
    }

    public float getCornerRadius()
    {
        return mRadius;
    }

    public void setStroke(int width, int color)
    {
        setStroke(width, color, 0, 0);
    }

    public void setStroke(int width, ColorStateList colorStateList)
    {
        setStroke(width, colorStateList, 0, 0);
    }

    public void setStroke(int width, int color, float dashWidth, float dashGap)
    {
        setStroke(width, ColorStateList.valueOf(color), dashWidth, dashGap);
    }

    public void setStroke(int width, ColorStateList colorStateList, float dashWidth, float dashGap)
    {
        mStrokeWidth = width;
        mStrokeColors = colorStateList;
        mStrokeDashWidth = dashWidth;
        mStrokeDashGap = dashGap;
        invalidateSelf();
    }

    public void setSize(int width, int height)
    {
        mIntrinsicWidth = width;
        mIntrinsicHeight = height;
        invalidateSelf();
    }

    public void setShape(int shape)
    {
        mShape = shape;
        invalidateSelf();
    }

    public int getShape()
    {
        return mShape;
    }

    public void setGradientType(int gradient)
    {
        mGradientKind = gradient;
        invalidateSelf();
    }

    public int getGradientType()
    {
        return mGradientKind;
    }

    public void setGradientCenter(float x, float y)
    {
        mCenterX = x;
        mCenterY = y;
        invalidateSelf();
    }

    public float getGradientCenterX()
    {
        return mCenterX;
    }

    public float getGradientCenterY()
    {
        return mCenterY;
    }

    public void setGradientRadius(float gradientRadius)
    {
        mGradientRadius = gradientRadius;
        mGradientRadiusFraction = false;
        invalidateSelf();
    }

    public float getGradientRadius()
    {
        if (mGradientKind != RADIAL_GRADIENT) throw new IllegalStateException();
        return mGradientRadius;
    }

    public void setUseLevel(boolean useLevel)
    {
        mUseLevel = useLevel;
        invalidateSelf();
    }

    public boolean getUseLevel()
    {
        return mUseLevel;
    }

    public Orientation getOrientation()
    {
        return mOrientation;
    }

    public void setOrientation(Orientation orientation)
    {
        mOrientation = orientation == null ? Orientation.TOP_BOTTOM : orientation;
        invalidateSelf();
    }

    public void setColors(int[] colors)
    {
        setColors(colors, null);
    }

    public void setColors(int[] colors, float[] offsets)
    {
        mGradientColors = colors == null ? null : colors.clone();
        mPositions = offsets == null ? null : offsets.clone();
        if (colors != null) mSolidColors = null;
        invalidateSelf();
    }

    public int[] getColors()
    {
        return mGradientColors == null ? null : mGradientColors.clone();
    }

    public void setInnerRadiusRatio(float innerRadiusRatio)
    {
        if (innerRadiusRatio <= 0) throw new IllegalArgumentException("Ratio must be greater than zero");
        mInnerRadiusRatio = innerRadiusRatio;
        invalidateSelf();
    }

    public float getInnerRadiusRatio()
    {
        return mInnerRadiusRatio;
    }

    public void setInnerRadius(int innerRadius)
    {
        mInnerRadius = innerRadius;
        invalidateSelf();
    }

    public int getInnerRadius()
    {
        return mInnerRadius;
    }

    public void setThicknessRatio(float thicknessRatio)
    {
        if (thicknessRatio <= 0) throw new IllegalArgumentException("Ratio must be greater than zero");
        mThicknessRatio = thicknessRatio;
        invalidateSelf();
    }

    public float getThicknessRatio()
    {
        return mThicknessRatio;
    }

    public void setThickness(int thickness)
    {
        mThickness = thickness;
        invalidateSelf();
    }

    public int getThickness()
    {
        return mThickness;
    }

    public void setColor(int argb)
    {
        setColor(ColorStateList.valueOf(argb));
    }

    public void setColor(ColorStateList colorStateList)
    {
        mSolidColors = colorStateList;
        mGradientColors = null;
        mPositions = null;
        invalidateSelf();
    }

    public ColorStateList getColor()
    {
        return mSolidColors;
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        boolean changed = false;
        if (mSolidColors != null && mSolidColors.isStateful()) changed = true;
        if (mStrokeColors != null && mStrokeColors.isStateful()) changed = true;

        if (mTint != null)
        {
            mTintFilter = tintFilter();
            changed = true;
        }

        if (changed) invalidateSelf();
        return changed;
    }

    @Override
    public boolean isStateful()
    {
        return (mSolidColors != null && mSolidColors.isStateful()) || (mStrokeColors != null && mStrokeColors.isStateful())
            || (mTint != null && mTint.isStateful());
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        return (mSolidColors != null && mSolidColors.hasFocusStateSpecified())
            || (mStrokeColors != null && mStrokeColors.hasFocusStateSpecified())
            || (mTint != null && mTint.hasFocusStateSpecified());
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations();
    }

    @Override
    public void setAlpha(int alpha)
    {
        if (alpha == mAlpha) return;

        mAlpha = alpha;
        super.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha()
    {
        return mAlpha;
    }

    @Override
    public void setDither(boolean dither)
    {
        if (dither == mDither) return;

        mDither = dither;
        mPaint.setDither(dither);
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter()
    {
        return mColorFilter;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter)
    {
        mColorFilter = colorFilter;
        super.setColorFilter(colorFilter);
        invalidateSelf();
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
    public int getOpacity()
    {
        if (mAlpha == 255 && mColorFilter == null && mTintFilter == null && isOpaqueContent()) return PixelFormat.OPAQUE;
        return PixelFormat.TRANSLUCENT;
    }

    private boolean isOpaqueContent()
    {
        if (mShape != RECTANGLE || mRadius > 0 || mCornerRadii != null) return false;
        if (mStrokeWidth > 0 && mStrokeColors != null && !mStrokeColors.isOpaque()) return false;

        return isOpaqueFill();
    }

    private boolean isOpaqueFill()
    {
        if (mGradientColors != null)
        {
            for (int color : mGradientColors)
            {
                if ((color >>> 24) != 0xFF) return false;
            }
            return true;
        }

        return mSolidColors != null && mSolidColors.isOpaque();
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        super.onBoundsChange(bounds);
        invalidateSelf();
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        super.onLevelChange(level);
        invalidateSelf();
        return true;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        super.inflate(resources, parser, attributes, theme);

        mShape = DrawableInflater.enumeration(attributes, "shape", SHAPE_NAMES, RECTANGLE);
        mDither = DrawableInflater.flag(attributes, "dither", false);

        if (mShape == RING)
        {
            mInnerRadius = Math.round(DrawableInflater.dimension(resources, attributes, "innerRadius", -1));
            mInnerRadiusRatio = DrawableInflater.dimensionOrFloat(resources, attributes, "innerRadiusRatio", DEFAULT_INNER_RADIUS_RATIO);
            mThickness = Math.round(DrawableInflater.dimension(resources, attributes, "thickness", -1));
            mThicknessRatio = DrawableInflater.dimensionOrFloat(resources, attributes, "thicknessRatio", DEFAULT_THICKNESS_RATIO);
            mUseLevelForShape = DrawableInflater.flag(attributes, "useLevel", true);
        }

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if ("solid".equals(name))
            {
                mSolidColors = colorList(resources, attributes, "color", 0);
            }
            else if ("stroke".equals(name))
            {
                mStrokeWidth = DrawableInflater.dimension(resources, attributes, "width", 0);
                mStrokeColors = colorList(resources, attributes, "color", 0);
                mStrokeDashWidth = DrawableInflater.dimension(resources, attributes, "dashWidth", 0);
                mStrokeDashGap = DrawableInflater.dimension(resources, attributes, "dashGap", 0);
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
            else if ("padding".equals(name))
            {
                mPadding = new Rect(Math.round(DrawableInflater.dimension(resources, attributes, "left", 0)),
                                    Math.round(DrawableInflater.dimension(resources, attributes, "top", 0)),
                                    Math.round(DrawableInflater.dimension(resources, attributes, "right", 0)),
                                    Math.round(DrawableInflater.dimension(resources, attributes, "bottom", 0)));
            }
        }
    }

    private static ColorStateList colorList(Resources resources, AttributeSet attributes, String name, int fallback)
    {
        int[] packed = DrawableInflater.typedValue(attributes, name);

        if (packed != null && packed[0] == TypedValue.TYPE_REFERENCE && packed[1] != 0)
        {
            try
            {
                return resources.getColorStateList(packed[1]);
            }
            catch (RuntimeException failure)
            {
                return ColorStateList.valueOf(fallback);
            }
        }

        return ColorStateList.valueOf(DrawableInflater.color(resources, attributes, name, fallback));
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
        boolean hasCentre = DrawableInflater.text(attributes, "centerColor") != null;
        int centre = DrawableInflater.color(resources, attributes, "centerColor", 0);
        int end = DrawableInflater.color(resources, attributes, "endColor", 0);

        mCenterX = gradientCentre(attributes, "centerX");
        mCenterY = gradientCentre(attributes, "centerY");

        if (hasCentre)
        {
            mGradientColors = new int[] {start, centre, end};
            mPositions = new float[] {0.0f, mCenterX != 0.5f ? mCenterX : mCenterY, 1.0f};
        }
        else
        {
            mGradientColors = new int[] {start, end};
            mPositions = null;
        }

        mGradientKind = DrawableInflater.enumeration(attributes, "type", GRADIENT_NAMES, LINEAR_GRADIENT);
        mUseLevel = DrawableInflater.flag(attributes, "useLevel", false);

        mGradientRadiusFraction = DrawableInflater.isFraction(attributes, "gradientRadius");
        if (mGradientRadiusFraction)
        {
            mGradientRadius = DrawableInflater.fraction(attributes, "gradientRadius", 0.5f);
        }
        else
        {
            mGradientRadius = DrawableInflater.dimensionOrFloat(resources, attributes, "gradientRadius", 0);
        }

        int angle = Math.round(DrawableInflater.dimensionOrFloat(resources, attributes, "angle", 0));
        mOrientation = orientationForAngle(angle);
    }

    private static float gradientCentre(AttributeSet attributes, String name)
    {
        return DrawableInflater.fraction(attributes, name, 0.5f);
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

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    public boolean canApplyTheme()
    {
        return (mSolidColors != null && mSolidColors.canApplyTheme()) || (mStrokeColors != null && mStrokeColors.canApplyTheme())
            || (mTint != null && mTint.canApplyTheme()) || super.canApplyTheme();
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
        return Insets.NONE;
    }

    @Override
    public ConstantState getConstantState()
    {
        final GradientDrawable snapshot = new GradientDrawable(this);
        final int changing = getChangingConfigurations();

        return new ConstantState()
        {
            @Override
            public Drawable newDrawable()
            {
                return new GradientDrawable(snapshot);
            }

            @Override
            public int getChangingConfigurations()
            {
                return changing;
            }
        };
    }

    @Override
    public void getOutline(Outline outline)
    {
        Rect bounds = getBounds();
        outline.setAlpha(isOpaqueFill() ? mAlpha / 255.0f : 0.0f);

        switch (mShape)
        {
            case RECTANGLE:
                if (mCornerRadii != null)
                {
                    buildPath(bounds);
                    outline.setConvexPath(mPath);
                    return;
                }

                float radius = Math.min(mRadius, Math.min(bounds.width(), bounds.height()) * 0.5f);
                outline.setRoundRect(bounds, radius);
                return;
            case OVAL:
                outline.setOval(bounds);
                return;
            case LINE:
                float halfStroke = mStrokeWidth * 0.5f;
                float centreY = bounds.centerY();
                int top = (int)Math.floor(centreY - halfStroke);
                int bottom = (int)Math.ceil(centreY + halfStroke);
                outline.setRect(bounds.left, top, bounds.right, bottom);
                return;
            default:
                return;
        }
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            mCornerRadii = mCornerRadii == null ? null : mCornerRadii.clone();
            mGradientColors = mGradientColors == null ? null : mGradientColors.clone();
            mPositions = mPositions == null ? null : mPositions.clone();
            mMutated = true;
        }
        return this;
    }

    private float levelFraction()
    {
        return getLevel() / 10000.0f;
    }

    private Shader buildShader(Rect bounds)
    {
        if (mGradientColors == null || mGradientColors.length < 2) return null;

        float[] positions = mPositions != null && mPositions.length == mGradientColors.length ? mPositions : null;

        if (mGradientKind == RADIAL_GRADIENT)
        {
            float radius = mGradientRadius;
            if (mGradientRadiusFraction) radius = radius * Math.min(bounds.width(), bounds.height());
            else if (radius <= 0) radius = Math.min(bounds.width(), bounds.height()) * 0.5f;
            if (mUseLevel) radius *= levelFraction();
            if (radius <= 0) radius = 0.001f;

            float x = bounds.left + bounds.width() * mCenterX;
            float y = bounds.top + bounds.height() * mCenterY;
            return new RadialGradient(x, y, radius, mGradientColors, positions, Shader.TileMode.CLAMP);
        }

        if (mGradientKind == SWEEP_GRADIENT)
        {
            float x = bounds.left + bounds.width() * mCenterX;
            float y = bounds.top + bounds.height() * mCenterY;
            return new SweepGradient(x, y, mGradientColors, positions);
        }

        float startX = bounds.left;
        float startY = bounds.top;
        float endX = bounds.right;
        float endY = bounds.top;
        float level = mUseLevel ? levelFraction() : 1.0f;

        switch (mOrientation)
        {
            case TOP_BOTTOM: endX = startX; endY = bounds.top + bounds.height() * level; break;
            case BOTTOM_TOP: startY = bounds.bottom; endX = startX; endY = bounds.bottom - bounds.height() * level; break;
            case RIGHT_LEFT: startX = bounds.right; endX = bounds.right - bounds.width() * level; break;
            case TL_BR: endX = bounds.left + bounds.width() * level; endY = bounds.top + bounds.height() * level; break;
            case BR_TL:
                startX = bounds.right;
                startY = bounds.bottom;
                endX = bounds.right - bounds.width() * level;
                endY = bounds.bottom - bounds.height() * level;
                break;
            case TR_BL: startX = bounds.right; endX = bounds.right - bounds.width() * level; endY = bounds.top + bounds.height() * level; break;
            case BL_TR: startY = bounds.bottom; endX = bounds.left + bounds.width() * level; endY = bounds.bottom - bounds.height() * level; break;
            default: endX = bounds.left + bounds.width() * level; break;
        }

        return new LinearGradient(startX, startY, endX, endY, mGradientColors, positions, Shader.TileMode.CLAMP);
    }

    private void buildPath(Rect bounds)
    {
        mPath.reset();

        float inset = mStrokeWidth > 0 ? mStrokeWidth * 0.5f : 0;
        mRect.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);

        if (mShape == OVAL)
        {
            mPath.addOval(mRect, Path.Direction.CW);
            return;
        }

        if (mShape == LINE)
        {
            float middle = mRect.centerY();
            mPath.moveTo(mRect.left, middle);
            mPath.lineTo(mRect.right, middle);
            return;
        }

        if (mShape == RING)
        {
            buildRing(bounds);
            return;
        }

        if (mCornerRadii != null)
        {
            mPath.addRoundRect(mRect, mCornerRadii, Path.Direction.CW);
            return;
        }

        if (mRadius > 0)
        {
            float radius = Math.min(mRadius, Math.min(mRect.width(), mRect.height()) * 0.5f);
            mPath.addRoundRect(mRect, radius, radius, Path.Direction.CW);
            return;
        }

        mPath.addRect(mRect, Path.Direction.CW);
    }

    private void buildRing(Rect bounds)
    {
        float thickness = mThickness != -1 ? mThickness : bounds.width() / mThicknessRatio;
        float innerRadius = mInnerRadius != -1 ? mInnerRadius : bounds.width() / mInnerRadiusRatio;
        float sweep = mUseLevelForShape ? 360.0f * levelFraction() : 360.0f;
        float centreX = bounds.exactCenterX();
        float centreY = bounds.exactCenterY();

        RectF inner = new RectF(centreX - innerRadius, centreY - innerRadius, centreX + innerRadius, centreY + innerRadius);
        float outerRadius = innerRadius + thickness;
        RectF outer = new RectF(centreX - outerRadius, centreY - outerRadius, centreX + outerRadius, centreY + outerRadius);

        if (sweep >= 360.0f || sweep <= -360.0f)
        {
            mPath.addOval(outer, Path.Direction.CW);
            mPath.addOval(inner, Path.Direction.CCW);
            mPath.setFillType(Path.FillType.EVEN_ODD);
            return;
        }

        mPath.setFillType(Path.FillType.WINDING);
        mPath.moveTo(centreX + innerRadius, centreY);
        mPath.lineTo(centreX + outerRadius, centreY);
        mPath.arcTo(outer, 0.0f, sweep, false);
        mPath.arcTo(inner, sweep, -sweep, false);
        mPath.close();
    }

    @Override
    public void draw(Canvas canvas)
    {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        buildPath(bounds);

        ColorFilter filter = mColorFilter != null ? mColorFilter : mTintFilter;
        mPaint.setColorFilter(filter);
        mStrokePaint.setColorFilter(filter);

        Shader shader = buildShader(bounds);
        int[] state = getState();

        if (shader != null || mSolidColors != null)
        {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShader(shader);
            mPaint.setColor(shader == null ? withAlpha(mSolidColors.getColorForState(state, mSolidColors.getDefaultColor())) : 0xFF000000);
            if (shader != null) mPaint.setAlpha(mAlpha);

            if (mShape != LINE) canvas.drawPath(mPath, mPaint);
            mPaint.setShader(null);
        }

        if (mStrokeWidth > 0 && mStrokeColors != null)
        {
            int strokeColor = mStrokeColors.getColorForState(state, mStrokeColors.getDefaultColor());
            if (strokeColor == 0) return;

            mStrokePaint.setStrokeWidth(mStrokeWidth);
            mStrokePaint.setColor(withAlpha(strokeColor));
            mStrokePaint.setPathEffect(mStrokeDashWidth > 0 ? new DashPathEffect(new float[] {mStrokeDashWidth, mStrokeDashGap}, 0) : null);

            canvas.drawPath(mPath, mStrokePaint);
        }
    }

    private int withAlpha(int color)
    {
        int alpha = ((color >>> 24) & 0xFF) * mAlpha / 255;
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
