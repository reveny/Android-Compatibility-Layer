package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class PaintDrawable extends ShapeDrawable
{
    public PaintDrawable()
    {
    }

    public PaintDrawable(int color)
    {
        getPaint().setColor(color);
    }

    public void setCornerRadius(float radius)
    {
        float[] radii = null;

        if (radius > 0)
        {
            radii = new float[8];
            java.util.Arrays.fill(radii, radius);
        }

        setCornerRadii(radii);
    }

    public void setCornerRadii(float[] radii)
    {
        if (radii == null)
        {
            if (getShape() != null) setShape(null);
        }
        else
        {
            setShape(new RoundRectShape(radii, null, null));
        }

        invalidateSelf();
    }

    @Override
    protected boolean inflateTag(String name, Resources resources, XmlPullParser parser, AttributeSet attributes)
    {
        if (!"corners".equals(name)) return super.inflateTag(name, resources, parser, attributes);

        float radius = DrawableInflater.dimension(resources, attributes, "radius", 0);
        setCornerRadius(radius);

        float topLeft = DrawableInflater.dimension(resources, attributes, "topLeftRadius", radius);
        float topRight = DrawableInflater.dimension(resources, attributes, "topRightRadius", radius);
        float bottomLeft = DrawableInflater.dimension(resources, attributes, "bottomLeftRadius", radius);
        float bottomRight = DrawableInflater.dimension(resources, attributes, "bottomRightRadius", radius);

        if (topLeft != radius || topRight != radius || bottomLeft != radius || bottomRight != radius)
        {
            setCornerRadii(new float[] {topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft});
        }

        return true;
    }
}
