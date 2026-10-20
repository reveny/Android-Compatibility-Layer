package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class AdaptiveIconDrawable extends Drawable
{
    private static final float SAFE_ZONE_SCALE = 72.0f / 108.0f;

    private final Path mMask = new Path();

    private Drawable mBackground;
    private Drawable mForeground;

    public AdaptiveIconDrawable()
    {
    }

    public AdaptiveIconDrawable(Drawable background, Drawable foreground)
    {
        mBackground = background;
        mForeground = foreground;
    }

    public Drawable getBackground()
    {
        return mBackground;
    }

    public Drawable getForeground()
    {
        return mForeground;
    }

    public static float getExtraInsetFraction()
    {
        return 0.25f;
    }

    public Path getIconMask()
    {
        buildMask(getBounds());
        return mMask;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            String name = parser.getName();
            boolean isBackground = "background".equals(name);
            if (!isBackground && !"foreground".equals(name)) continue;

            Drawable layer = null;

            int reference = DrawableInflater.reference(attributes, "drawable", 0);
            if (reference != 0) layer = resources.getDrawable(reference);

            int itemDepth = parser.getDepth();
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
            {
                if (event == XmlPullParser.END_TAG && parser.getDepth() <= itemDepth) break;
                if (event != XmlPullParser.START_TAG) continue;

                Drawable child = DrawableInflater.inflate(resources, parser, attributes);
                if (child != null) layer = child;
            }

            if (isBackground) mBackground = layer;
            else mForeground = layer;
        }
    }

    private void buildMask(Rect bounds)
    {
        mMask.reset();

        float radius = Math.min(bounds.width(), bounds.height()) * 0.25f;
        mMask.addRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, radius, radius, Path.Direction.CW);
    }

    @Override
    public void draw(Canvas canvas)
    {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        buildMask(bounds);

        int saveCount = canvas.save();
        canvas.clipPath(mMask);

        int overdrawX = Math.round(bounds.width() * (1 - SAFE_ZONE_SCALE) * 0.5f);
        int overdrawY = Math.round(bounds.height() * (1 - SAFE_ZONE_SCALE) * 0.5f);

        Rect layerBounds = new Rect(bounds.left - overdrawX, bounds.top - overdrawY, bounds.right + overdrawX,
                                    bounds.bottom + overdrawY);

        if (mBackground != null)
        {
            mBackground.setBounds(layerBounds);
            mBackground.setAlpha(getAlpha());
            mBackground.draw(canvas);
        }

        if (mForeground != null)
        {
            mForeground.setBounds(layerBounds);
            mForeground.setAlpha(getAlpha());
            mForeground.draw(canvas);
        }

        canvas.restoreToCount(saveCount);
    }
}
