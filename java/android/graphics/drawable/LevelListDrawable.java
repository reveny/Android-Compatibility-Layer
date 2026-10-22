package android.graphics.drawable;

import android.content.res.Resources;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class LevelListDrawable extends DrawableContainer
{
    private LevelListState mLevelListState;
    private boolean mMutated;

    public LevelListDrawable()
    {
        this(null, null);
    }

    private LevelListDrawable(LevelListState state, Resources resources)
    {
        setConstantState(new LevelListState(state, this, resources));
        onLevelChange(getLevel());
    }

    public void addLevel(int low, int high, Drawable drawable)
    {
        if (drawable == null) return;

        mLevelListState.addLevel(low, high, drawable);
        onLevelChange(getLevel());
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        if (selectDrawable(mLevelListState.indexOfLevel(level))) return true;

        return super.onLevelChange(level);
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            mLevelListState.mutate();
            mMutated = true;
        }

        return this;
    }

    @Override
    protected void setConstantState(DrawableContainerState state)
    {
        super.setConstantState(state);

        if (state instanceof LevelListState) mLevelListState = (LevelListState) state;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        super.inflate(resources, parser, attributes, theme);

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            if (!"item".equals(parser.getName()))
            {
                DrawableInflater.skipElement(parser);
                continue;
            }

            int low = DrawableInflater.integer(attributes, "minLevel", 0);
            int high = DrawableInflater.integer(attributes, "maxLevel", 0);

            Drawable drawable = DrawableInflater.itemDrawable(resources, parser, attributes, theme);
            if (drawable == null) continue;

            mLevelListState.addLevel(low, high, drawable);
        }

        onLevelChange(getLevel());
    }

    private static final class LevelListState extends DrawableContainerState
    {
        private int[] mLows;
        private int[] mHighs;

        LevelListState(LevelListState original, LevelListDrawable owner, Resources resources)
        {
            super(original, owner, resources);

            mLows = new int[mDrawables.length];
            mHighs = new int[mDrawables.length];

            if (original != null)
            {
                System.arraycopy(original.mLows, 0, mLows, 0, Math.min(original.mLows.length, mLows.length));
                System.arraycopy(original.mHighs, 0, mHighs, 0, Math.min(original.mHighs.length, mHighs.length));
            }
        }

        void mutate()
        {
            mLows = mLows.clone();
            mHighs = mHighs.clone();
        }

        void addLevel(int low, int high, Drawable drawable)
        {
            int position = addChild(drawable);
            mLows[position] = low;
            mHighs[position] = high;
        }

        int indexOfLevel(int level)
        {
            for (int index = 0; index < mNumChildren; index++)
            {
                if (level >= mLows[index] && level <= mHighs[index]) return index;
            }

            return -1;
        }

        @Override
        public Drawable newDrawable()
        {
            return new LevelListDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources resources)
        {
            return new LevelListDrawable(this, resources);
        }

        @Override
        public void growArray(int oldSize, int newSize)
        {
            super.growArray(oldSize, newSize);

            int[] lows = new int[newSize];
            int[] highs = new int[newSize];
            System.arraycopy(mLows, 0, lows, 0, Math.min(oldSize, mLows.length));
            System.arraycopy(mHighs, 0, highs, 0, Math.min(oldSize, mHighs.length));
            mLows = lows;
            mHighs = highs;
        }
    }
}
