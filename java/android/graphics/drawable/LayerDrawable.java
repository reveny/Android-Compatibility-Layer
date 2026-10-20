package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

public class LayerDrawable extends Drawable
{
    private final ArrayList<Layer> mLayers = new ArrayList<>();

    public LayerDrawable()
    {
    }

    public LayerDrawable(Drawable[] layers)
    {
        if (layers == null) return;

        for (Drawable layer : layers)
        {
            if (layer != null) mLayers.add(new Layer(layer));
        }
    }

    public int getNumberOfLayers()
    {
        return mLayers.size();
    }

    public Drawable getDrawable(int index)
    {
        return index < 0 || index >= mLayers.size() ? null : mLayers.get(index).drawable;
    }

    public int getId(int index)
    {
        return index < 0 || index >= mLayers.size() ? 0 : mLayers.get(index).id;
    }

    public Drawable findDrawableByLayerId(int id)
    {
        for (Layer layer : mLayers)
        {
            if (layer.id == id) return layer.drawable;
        }

        return null;
    }

    public void setLayerInset(int index, int left, int top, int right, int bottom)
    {
        if (index < 0 || index >= mLayers.size()) return;

        Layer layer = mLayers.get(index);
        layer.left = left;
        layer.top = top;
        layer.right = right;
        layer.bottom = bottom;
    }

    public void addLayer(Drawable drawable)
    {
        if (drawable != null) mLayers.add(new Layer(drawable));
    }

    @Override
    public int getIntrinsicWidth()
    {
        int widest = -1;

        for (Layer layer : mLayers)
        {
            int width = layer.drawable.getIntrinsicWidth();
            if (width >= 0) widest = Math.max(widest, width + layer.left + layer.right);
        }

        return widest;
    }

    @Override
    public int getIntrinsicHeight()
    {
        int tallest = -1;

        for (Layer layer : mLayers)
        {
            int height = layer.drawable.getIntrinsicHeight();
            if (height >= 0) tallest = Math.max(tallest, height + layer.top + layer.bottom);
        }

        return tallest;
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
            if (!"item".equals(parser.getName())) continue;

            Layer layer = new Layer(null);
            layer.id = DrawableInflater.reference(attributes, "id", 0);
            layer.left = Math.round(DrawableInflater.dimension(resources, attributes, "left", 0));
            layer.top = Math.round(DrawableInflater.dimension(resources, attributes, "top", 0));
            layer.right = Math.round(DrawableInflater.dimension(resources, attributes, "right", 0));
            layer.bottom = Math.round(DrawableInflater.dimension(resources, attributes, "bottom", 0));

            int reference = DrawableInflater.reference(attributes, "drawable", 0);
            if (reference != 0) layer.drawable = resources.getDrawable(reference);

            int itemDepth = parser.getDepth();
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
            {
                if (event == XmlPullParser.END_TAG && parser.getDepth() <= itemDepth) break;
                if (event != XmlPullParser.START_TAG) continue;

                Drawable child = DrawableInflater.inflate(resources, parser, attributes);
                if (child != null) layer.drawable = child;
            }

            if (layer.drawable != null) mLayers.add(layer);
        }
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom)
    {
        super.setBounds(left, top, right, bottom);
        layoutLayers();
    }

    private void layoutLayers()
    {
        Rect bounds = getBounds();

        for (Layer layer : mLayers)
        {
            layer.drawable.setBounds(bounds.left + layer.left, bounds.top + layer.top, bounds.right - layer.right,
                                     bounds.bottom - layer.bottom);
        }
    }

    @Override
    public void draw(Canvas canvas)
    {
        layoutLayers();

        for (Layer layer : mLayers)
        {
            layer.drawable.setAlpha(getAlpha());
            layer.drawable.draw(canvas);
        }
    }

    @Override
    public boolean isStateful()
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable.isStateful()) return true;
        }

        return false;
    }

    @Override
    public boolean setState(int[] stateSet)
    {
        boolean changed = super.setState(stateSet);

        for (Layer layer : mLayers)
        {
            if (layer.drawable.setState(stateSet)) changed = true;
        }

        return changed;
    }

    private static final class Layer
    {
        Drawable drawable;
        int id;
        int left;
        int top;
        int right;
        int bottom;

        Layer(Drawable drawable)
        {
            this.drawable = drawable;
        }
    }
}
