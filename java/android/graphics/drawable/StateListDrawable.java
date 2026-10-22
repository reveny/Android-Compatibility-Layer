package android.graphics.drawable;

import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.StateSet;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

public class StateListDrawable extends DrawableContainer
{
    private StateListState mStateListState;
    private boolean mMutated;

    public StateListDrawable()
    {
        this(null, null);
    }

    StateListDrawable(StateListState state)
    {
        if (state != null) setConstantState(state);
    }

    private StateListDrawable(StateListState state, Resources resources)
    {
        setConstantState(new StateListState(state, this, resources));
        onStateChange(getState());
    }

    public void addState(int[] stateSet, Drawable drawable)
    {
        if (drawable == null) return;

        mStateListState.addStateSet(stateSet == null ? StateSet.WILD_CARD : stateSet, drawable);
        onStateChange(getState());
    }

    @Override
    public boolean isStateful()
    {
        return true;
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        return mStateListState.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        boolean changed = super.onStateChange(stateSet);

        int index = mStateListState.indexOfStateSet(stateSet);
        if (index < 0) index = mStateListState.indexOfStateSet(StateSet.WILD_CARD);

        return selectDrawable(index) || changed;
    }

    public int getStateCount()
    {
        return mStateListState.getChildCount();
    }

    public int[] getStateSet(int index)
    {
        return index < 0 || index >= mStateListState.getChildCount() ? null : mStateListState.mStateSets[index];
    }

    public Drawable getStateDrawable(int index)
    {
        return mStateListState.getChild(index);
    }

    public int findStateDrawableIndex(int[] stateSet)
    {
        return mStateListState.indexOfStateSet(stateSet);
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            mStateListState.mutate();
            mMutated = true;
        }

        return this;
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
        onStateChange(getState());
    }

    @Override
    protected void setConstantState(DrawableContainerState state)
    {
        super.setConstantState(state);

        if (state instanceof StateListState) mStateListState = (StateListState) state;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        super.inflate(resources, parser, attributes, theme);

        mStateListState.setVariablePadding(DrawableInflater.flag(attributes, "variablePadding", false));
        mStateListState.setConstantSize(DrawableInflater.flag(attributes, "constantSize", false));
        mStateListState.setEnterFadeDuration(DrawableInflater.integer(attributes, "enterFadeDuration", 0));
        mStateListState.setExitFadeDuration(DrawableInflater.integer(attributes, "exitFadeDuration", 0));
        setDither(DrawableInflater.flag(attributes, "dither", true));
        setAutoMirrored(DrawableInflater.flag(attributes, "autoMirrored", false));

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
            if (reference != 0) drawable = resources.getDrawable(reference, theme);

            int itemDepth = parser.getDepth();
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
            {
                if (event == XmlPullParser.END_TAG && parser.getDepth() <= itemDepth) break;
                if (event != XmlPullParser.START_TAG) continue;

                Drawable child = DrawableInflater.inflate(resources, parser, attributes, theme);
                if (child != null) drawable = child;
            }

            if (drawable == null) continue;

            mStateListState.addStateSet(stateSet, drawable);
        }

        onStateChange(getState());
    }

    static int[] readStateSet(AttributeSet attributes)
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

    static class StateListState extends DrawableContainerState
    {
        private static final int FOCUSED_STATE = 0x0101009c;

        int[][] mStateSets;

        StateListState(StateListState original, StateListDrawable owner, Resources resources)
        {
            super(original, owner, resources);

            if (original != null)
            {
                mStateSets = new int[mDrawables.length][];
                for (int index = 0; index < original.mNumChildren && index < original.mStateSets.length; index++)
                {
                    mStateSets[index] = original.mStateSets[index];
                }
            }
            else
            {
                mStateSets = new int[mDrawables.length][];
            }
        }

        void mutate()
        {
            int[][] copies = new int[mStateSets.length][];

            for (int index = 0; index < mStateSets.length; index++)
            {
                copies[index] = mStateSets[index] == null ? null : mStateSets[index].clone();
            }

            mStateSets = copies;
        }

        int addStateSet(int[] stateSet, Drawable drawable)
        {
            int position = addChild(drawable);
            mStateSets[position] = stateSet;
            return position;
        }

        int indexOfStateSet(int[] stateSet)
        {
            for (int index = 0; index < mNumChildren; index++)
            {
                if (StateSet.stateSetMatches(mStateSets[index], stateSet)) return index;
            }

            return -1;
        }

        boolean hasFocusStateSpecified()
        {
            for (int index = 0; index < mNumChildren; index++)
            {
                int[] stateSet = mStateSets[index];
                if (stateSet == null) continue;

                for (int state : stateSet)
                {
                    if (state == FOCUSED_STATE) return true;
                }
            }

            return false;
        }

        @Override
        public Drawable newDrawable()
        {
            return new StateListDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources resources)
        {
            return new StateListDrawable(this, resources);
        }

        @Override
        public boolean canApplyTheme()
        {
            return super.canApplyTheme();
        }

        @Override
        public void growArray(int oldSize, int newSize)
        {
            super.growArray(oldSize, newSize);

            int[][] grown = new int[newSize][];
            System.arraycopy(mStateSets, 0, grown, 0, Math.min(oldSize, mStateSets.length));
            mStateSets = grown;
        }
    }
}
