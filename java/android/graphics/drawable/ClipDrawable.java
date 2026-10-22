package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;

import org.xmlpull.v1.XmlPullParser;

public class ClipDrawable extends DrawableWrapper
{
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;

    private static final int MAX_LEVEL = 10000;

    private final Rect mClipRect = new Rect();

    private int mOrientation = HORIZONTAL;
    private int mGravity = Gravity.LEFT;

    ClipDrawable()
    {
    }

    public ClipDrawable(Drawable drawable, int gravity, int orientation)
    {
        super(drawable);
        mGravity = gravity;
        mOrientation = orientation;
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    DrawableWrapper copyWrapper()
    {
        return new ClipDrawable(null, mGravity, mOrientation);
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        Drawable inner = getDrawable();
        if (inner != null) inner.setLevel(level);

        invalidateSelf();
        return true;
    }

    @Override
    public int getOpacity()
    {
        Drawable inner = getDrawable();
        if (inner == null || getLevel() == 0) return PixelFormat.TRANSPARENT;

        int opacity = inner.getOpacity();
        if (opacity == PixelFormat.TRANSPARENT || getLevel() >= MAX_LEVEL) return opacity;

        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void draw(Canvas canvas)
    {
        Drawable inner = getDrawable();
        if (inner == null || getLevel() == 0) return;

        Rect bounds = getBounds();
        int level = getLevel();
        int width = bounds.width();
        int height = bounds.height();

        if ((mOrientation & HORIZONTAL) != 0) width -= width * (MAX_LEVEL - level) / MAX_LEVEL;
        if ((mOrientation & VERTICAL) != 0) height -= height * (MAX_LEVEL - level) / MAX_LEVEL;

        Gravity.apply(mGravity, width, height, bounds, mClipRect, getLayoutDirection());

        if (width <= 0 || height <= 0) return;

        int saveCount = canvas.save();
        canvas.clipRect(mClipRect);
        inner.setAlpha(getAlpha());
        inner.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        mOrientation = DrawableInflater.integer(attributes, "clipOrientation", mOrientation);

        String orientation = DrawableInflater.text(attributes, "clipOrientation");
        if ("vertical".equals(orientation)) mOrientation = VERTICAL;
        else if ("horizontal".equals(orientation)) mOrientation = HORIZONTAL;

        mGravity = DrawableInflater.gravity(attributes, "gravity", mGravity);

        int reference = DrawableInflater.reference(attributes, "drawable", 0);
        if (reference != 0) setDrawable(resources.getDrawable(reference));

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            Drawable child = DrawableInflater.inflate(resources, parser, attributes, theme);
            if (child != null) setDrawable(child);
        }

        Drawable inner = getDrawable();
        if (inner != null) inner.setLevel(getLevel());
    }
}
