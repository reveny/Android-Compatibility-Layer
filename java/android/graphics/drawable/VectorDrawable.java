package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.PathParser;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

public class VectorDrawable extends Drawable
{
    private static final String[] FILL_TYPE_NAMES = {"nonZero", "evenOdd"};

    private final Group mRoot = new Group();
    private final Paint mPaint = new Paint();

    private int mResourceId;
    private float mViewportWidth;
    private float mViewportHeight;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;
    private int mTint;
    private boolean mTinted;

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
    public void setTint(int tint)
    {
        mTint = tint;
        mTinted = true;
    }

    @Override
    public void setTintList(android.content.res.ColorStateList tintList)
    {
        if (tintList != null) setTint(tintList.getDefaultColor());
    }

    public boolean isEmpty()
    {
        return mRoot.children.isEmpty();
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
        readVector(resources, parser, attributes);
    }

    void readVector(Resources resources, XmlPullParser parser, AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
        mViewportWidth = DrawableInflater.dimensionOrFloat(resources, attributes, "viewportWidth", 0);
        mViewportHeight = DrawableInflater.dimensionOrFloat(resources, attributes, "viewportHeight", 0);
        mIntrinsicWidth = Math.round(DrawableInflater.dimension(resources, attributes, "width", 0));
        mIntrinsicHeight = Math.round(DrawableInflater.dimension(resources, attributes, "height", 0));

        int tint = DrawableInflater.color(resources, attributes, "tint", 0);
        if (tint != 0) setTint(tint);

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
        canvas.scale(bounds.width() / mViewportWidth, bounds.height() / mViewportHeight);

        drawGroup(canvas, mRoot);
        canvas.restoreToCount(saveCount);
    }

    private void drawGroup(Canvas canvas, Group group)
    {
        int saveCount = canvas.save();

        if (!group.transform.isIdentity()) canvas.concat(group.transform);
        if (group.clip != null) canvas.clipPath(group.clip);

        for (int index = 0; index < group.children.size(); index++)
        {
            Object child = group.children.get(index);

            if (child instanceof Group) drawGroup(canvas, (Group) child);
            else drawShape(canvas, (Shape) child);
        }

        canvas.restoreToCount(saveCount);
    }

    private void drawShape(Canvas canvas, Shape shape)
    {
        int alpha = getAlpha();

        if (shape.fillColor != 0)
        {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(scaleAlpha(tinted(shape.fillColor), shape.fillAlpha * alpha / 255.0f));
            shape.path.setFillType(shape.evenOdd ? Path.FillType.EVEN_ODD : Path.FillType.WINDING);

            canvas.drawPath(shape.path, mPaint);
        }

        if (shape.strokeColor != 0 && shape.strokeWidth > 0)
        {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(shape.strokeWidth);
            mPaint.setColor(scaleAlpha(tinted(shape.strokeColor), shape.strokeAlpha * alpha / 255.0f));

            canvas.drawPath(shape.path, mPaint);
        }
    }

    private int tinted(int color)
    {
        if (!mTinted) return color;

        return (color & 0xFF000000) | (mTint & 0x00FFFFFF);
    }

    private static int scaleAlpha(int color, float amount)
    {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, amount)));
        return (alpha << 24) | (color & 0x00FFFFFF);
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
