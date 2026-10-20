package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.StateSet;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

public class StateListDrawable extends Drawable
{
    private final ArrayList<Entry> mEntries = new ArrayList<>();
    private Drawable mCurrent;

    public void addState(int[] stateSet, Drawable drawable)
    {
        if (drawable == null) return;

        mEntries.add(new Entry(stateSet == null ? new int[0] : stateSet, drawable));
        if (mCurrent == null) select(getState());
    }

    public int getStateCount()
    {
        return mEntries.size();
    }

    public Drawable getStateDrawable(int index)
    {
        return index < 0 || index >= mEntries.size() ? null : mEntries.get(index).drawable;
    }

    public int[] getStateSet(int index)
    {
        return index < 0 || index >= mEntries.size() ? null : mEntries.get(index).stateSet;
    }

    @Override
    public Drawable getCurrent()
    {
        return mCurrent;
    }

    @Override
    public boolean isStateful()
    {
        return true;
    }

    @Override
    public boolean setState(int[] stateSet)
    {
        super.setState(stateSet);
        return select(stateSet);
    }

    private boolean select(int[] stateSet)
    {
        Drawable wanted = null;

        for (Entry entry : mEntries)
        {
            if (!StateSet.stateSetMatches(entry.stateSet, stateSet)) continue;

            wanted = entry.drawable;
            break;
        }

        if (wanted == mCurrent) return false;

        mCurrent = wanted;
        return true;
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mCurrent == null ? -1 : mCurrent.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mCurrent == null ? -1 : mCurrent.getIntrinsicHeight();
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

            int[] stateSet = readStateSet(attributes);
            Drawable drawable = null;

            int reference = DrawableInflater.reference(attributes, "drawable", 0);
            if (reference != 0) drawable = resources.getDrawable(reference);

            int itemDepth = parser.getDepth();
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
            {
                if (event == XmlPullParser.END_TAG && parser.getDepth() <= itemDepth) break;
                if (event != XmlPullParser.START_TAG) continue;

                Drawable child = DrawableInflater.inflate(resources, parser, attributes);
                if (child != null) drawable = child;
            }

            if (drawable != null) mEntries.add(new Entry(stateSet, drawable));
        }

        select(getState());
    }

    private static int[] readStateSet(AttributeSet attributes)
    {
        ArrayList<Integer> collected = new ArrayList<>();

        for (int index = 0; index < attributes.getAttributeCount(); index++)
        {
            int name = attributes.getAttributeNameResource(index);
            if (name == 0) continue;

            if (!isBoolean(attributes, index)) continue;

            boolean wanted = attributes.getAttributeBooleanValue(index, false);
            collected.add(wanted ? name : -name);
        }

        int[] stateSet = new int[collected.size()];
        for (int index = 0; index < stateSet.length; index++) stateSet[index] = collected.get(index);

        return stateSet;
    }

    private static boolean isBoolean(AttributeSet attributes, int index)
    {
        if (attributes instanceof android.content.res.XmlBlock.Parser)
        {
            int[] packed = ((android.content.res.XmlBlock.Parser) attributes).getAttributeTypedValue(index);
            return packed != null && packed[0] == android.util.TypedValue.TYPE_INT_BOOLEAN;
        }

        String value = attributes.getAttributeValue(index);
        return "true".equals(value) || "false".equals(value);
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mCurrent == null) return;

        mCurrent.setBounds(getBounds());
        mCurrent.setAlpha(getAlpha());
        mCurrent.draw(canvas);
    }

    private static final class Entry
    {
        final int[] stateSet;
        final Drawable drawable;

        Entry(int[] stateSet, Drawable drawable)
        {
            this.stateSet = stateSet;
            this.drawable = drawable;
        }
    }
}
