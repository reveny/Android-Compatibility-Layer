package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class InsetDrawable extends Drawable
{
    private Drawable mInner;
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;

    public InsetDrawable()
    {
    }

    public InsetDrawable(Drawable inner, int inset)
    {
        this(inner, inset, inset, inset, inset);
    }

    public InsetDrawable(Drawable inner, int left, int top, int right, int bottom)
    {
        mInner = inner;
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
    }

    public Drawable getDrawable()
    {
        return mInner;
    }

    public void setDrawable(Drawable inner)
    {
        mInner = inner;
    }

    @Override
    public int getIntrinsicWidth()
    {
        if (mInner == null) return -1;

        int width = mInner.getIntrinsicWidth();
        return width < 0 ? -1 : width + mLeft + mRight;
    }

    @Override
    public int getIntrinsicHeight()
    {
        if (mInner == null) return -1;

        int height = mInner.getIntrinsicHeight();
        return height < 0 ? -1 : height + mTop + mBottom;
    }

    @Override
    public boolean getPadding(Rect padding)
    {
        if (padding == null) return false;

        padding.set(mLeft, mTop, mRight, mBottom);
        return mLeft != 0 || mTop != 0 || mRight != 0 || mBottom != 0;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
        int all = Math.round(DrawableInflater.dimension(resources, attributes, "inset", 0));

        mLeft = Math.round(DrawableInflater.dimension(resources, attributes, "insetLeft", all));
        mTop = Math.round(DrawableInflater.dimension(resources, attributes, "insetTop", all));
        mRight = Math.round(DrawableInflater.dimension(resources, attributes, "insetRight", all));
        mBottom = Math.round(DrawableInflater.dimension(resources, attributes, "insetBottom", all));

        int reference = DrawableInflater.reference(attributes, "drawable", 0);
        if (reference != 0) mInner = resources.getDrawable(reference);

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            Drawable child = DrawableInflater.inflate(resources, parser, attributes);
            if (child != null) mInner = child;
        }
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mInner == null) return;

        Rect bounds = getBounds();
        mInner.setBounds(bounds.left + mLeft, bounds.top + mTop, bounds.right - mRight, bounds.bottom - mBottom);
        mInner.setAlpha(getAlpha());
        mInner.draw(canvas);
    }

    @Override
    public boolean isStateful()
    {
        return mInner != null && mInner.isStateful();
    }

    @Override
    public boolean setState(int[] stateSet)
    {
        boolean changed = super.setState(stateSet);
        if (mInner != null && mInner.setState(stateSet)) changed = true;

        return changed;
    }
}
