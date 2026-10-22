package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

public class LinearLayout extends ViewGroup
{
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public static final int SHOW_DIVIDER_NONE = 0;
    public static final int SHOW_DIVIDER_BEGINNING = 1;
    public static final int SHOW_DIVIDER_MIDDLE = 2;
    public static final int SHOW_DIVIDER_END = 4;

    private static final int VERTICAL_GRAVITY_COUNT = 4;
    private static final int INDEX_CENTER_VERTICAL = 0;
    private static final int INDEX_TOP = 1;
    private static final int INDEX_BOTTOM = 2;
    private static final int INDEX_FILL = 3;

    private static StyleableSet sStyleable;

    private int mOrientation = HORIZONTAL;
    private int mGravity = Gravity.START | Gravity.TOP;
    private boolean mBaselineAligned = true;
    private int mBaselineAlignedChildIndex = -1;
    private int mBaselineChildTop;
    private float mWeightSum = -1f;
    private boolean mUseLargestChild;
    private int mTotalLength;
    private int[] mMaxAscent;
    private int[] mMaxDescent;

    private Drawable mDivider;
    private int mDividerWidth;
    private int mDividerHeight;
    private int mShowDividers = SHOW_DIVIDER_NONE;
    private int mDividerPadding;
    private int mLayoutDirection = -1;

    public LinearLayout(Context context)
    {
        this(context, null);
    }

    public LinearLayout(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0, 0);
    }

    public LinearLayout(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public LinearLayout(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        if (attributes == null) return;

        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "orientation", "gravity", "baselineAligned", "weightSum",
                                         "baselineAlignedChildIndex", "measureWithLargestChild", "divider", "showDividers",
                                         "dividerPadding");
        }
        if (sStyleable.isEmpty()) return;

        android.content.res.TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int orientationIndex = sStyleable.indexOf("orientation");
        if (orientationIndex >= 0) mOrientation = array.getInt(orientationIndex, HORIZONTAL);

        int gravityIndex = sStyleable.indexOf("gravity");
        if (gravityIndex >= 0)
        {
            int gravity = array.getInt(gravityIndex, -1);
            if (gravity >= 0) setGravity(gravity);
        }

        int baselineIndex = sStyleable.indexOf("baselineAligned");
        if (baselineIndex >= 0) mBaselineAligned = array.getBoolean(baselineIndex, true);

        int weightSumIndex = sStyleable.indexOf("weightSum");
        if (weightSumIndex >= 0) mWeightSum = array.getFloat(weightSumIndex, -1f);

        int childIndex = sStyleable.indexOf("baselineAlignedChildIndex");
        if (childIndex >= 0) mBaselineAlignedChildIndex = array.getInt(childIndex, -1);

        int largestIndex = sStyleable.indexOf("measureWithLargestChild");
        if (largestIndex >= 0) mUseLargestChild = array.getBoolean(largestIndex, false);

        int dividerIndex = sStyleable.indexOf("divider");
        if (dividerIndex >= 0 && array.hasValue(dividerIndex)) setDividerDrawable(array.getDrawable(dividerIndex));

        int showIndex = sStyleable.indexOf("showDividers");
        if (showIndex >= 0) mShowDividers = array.getInt(showIndex, SHOW_DIVIDER_NONE);

        int paddingIndex = sStyleable.indexOf("dividerPadding");
        if (paddingIndex >= 0) mDividerPadding = array.getDimensionPixelSize(paddingIndex, 0);

        array.recycle();
    }

    public void setShowDividers(int showDividers)
    {
        if (showDividers == mShowDividers) return;

        mShowDividers = showDividers;
        setWillNotDraw(!isShowingDividers());
        requestLayout();
    }

    public int getShowDividers()
    {
        return mShowDividers;
    }

    public Drawable getDividerDrawable()
    {
        return mDivider;
    }

    public void setDividerDrawable(Drawable divider)
    {
        if (divider == mDivider) return;

        mDivider = divider;

        if (divider != null)
        {
            mDividerWidth = divider.getIntrinsicWidth();
            mDividerHeight = divider.getIntrinsicHeight();
        }
        else
        {
            mDividerWidth = 0;
            mDividerHeight = 0;
        }

        setWillNotDraw(!isShowingDividers());
        requestLayout();
    }

    public void setDividerPadding(int padding)
    {
        if (padding == mDividerPadding) return;

        mDividerPadding = padding;
        if (isShowingDividers())
        {
            requestLayout();
            invalidate();
        }
    }

    public int getDividerPadding()
    {
        return mDividerPadding;
    }

    public int getDividerWidth()
    {
        return mDividerWidth;
    }

    private boolean isShowingDividers()
    {
        return mShowDividers != SHOW_DIVIDER_NONE && mDivider != null;
    }

    @Override
    public boolean shouldDelayChildPressedState()
    {
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if (mDivider == null) return;

        if (mOrientation == VERTICAL) drawDividersVertical(canvas);
        else drawDividersHorizontal(canvas);
    }

    void drawDividersVertical(Canvas canvas)
    {
        int count = getVirtualChildCount();

        for (int index = 0; index < count; index++)
        {
            View child = getVirtualChildAt(index);
            if (child == null || child.getVisibility() == GONE) continue;

            if (hasDividerBeforeChildAt(index))
            {
                LayoutParams params = (LayoutParams)child.getLayoutParams();
                drawHorizontalDivider(canvas, child.getTop() - params.topMargin - mDividerHeight);
            }
        }

        if (hasDividerBeforeChildAt(count))
        {
            View child = getLastNonGoneChild();
            int bottom;

            if (child == null)
            {
                bottom = getHeight() - getPaddingBottom() - mDividerHeight;
            }
            else
            {
                LayoutParams params = (LayoutParams)child.getLayoutParams();
                bottom = child.getBottom() + params.bottomMargin;
            }

            drawHorizontalDivider(canvas, bottom);
        }
    }

    private View getLastNonGoneChild()
    {
        for (int index = getVirtualChildCount() - 1; index >= 0; index--)
        {
            View child = getVirtualChildAt(index);
            if (child != null && child.getVisibility() != GONE) return child;
        }

        return null;
    }

    void drawDividersHorizontal(Canvas canvas)
    {
        int count = getVirtualChildCount();
        boolean rtl = isRtl();

        for (int index = 0; index < count; index++)
        {
            View child = getVirtualChildAt(index);
            if (child == null || child.getVisibility() == GONE) continue;

            if (hasDividerBeforeChildAt(index))
            {
                LayoutParams params = (LayoutParams)child.getLayoutParams();
                int position = rtl ? child.getRight() + params.rightMargin : child.getLeft() - params.leftMargin - mDividerWidth;
                drawVerticalDivider(canvas, position);
            }
        }

        if (hasDividerBeforeChildAt(count))
        {
            View child = getLastNonGoneChild();
            int position;

            if (child == null)
            {
                position = rtl ? getPaddingLeft() : getWidth() - getPaddingRight() - mDividerWidth;
            }
            else
            {
                LayoutParams params = (LayoutParams)child.getLayoutParams();
                position = rtl ? child.getLeft() - params.leftMargin - mDividerWidth : child.getRight() + params.rightMargin;
            }

            drawVerticalDivider(canvas, position);
        }
    }

    void drawHorizontalDivider(Canvas canvas, int top)
    {
        mDivider.setBounds(getPaddingLeft() + mDividerPadding, top, getWidth() - getPaddingRight() - mDividerPadding, top + mDividerHeight);
        mDivider.draw(canvas);
    }

    void drawVerticalDivider(Canvas canvas, int left)
    {
        mDivider.setBounds(left, getPaddingTop() + mDividerPadding, left + mDividerWidth, getHeight() - getPaddingBottom() - mDividerPadding);
        mDivider.draw(canvas);
    }

    public boolean isBaselineAligned()
    {
        return mBaselineAligned;
    }

    public void setBaselineAligned(boolean baselineAligned)
    {
        mBaselineAligned = baselineAligned;
    }

    public boolean isMeasureWithLargestChildEnabled()
    {
        return mUseLargestChild;
    }

    public void setMeasureWithLargestChildEnabled(boolean enabled)
    {
        mUseLargestChild = enabled;
    }

    @Override
    public int getBaseline()
    {
        if (mBaselineAlignedChildIndex < 0) return super.getBaseline();

        if (getChildCount() <= mBaselineAlignedChildIndex)
        {
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout set to an index that is out of bounds.");
        }

        View child = getChildAt(mBaselineAlignedChildIndex);
        int childBaseline = child.getBaseline();

        if (childBaseline == -1)
        {
            if (mBaselineAlignedChildIndex == 0) return -1;

            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout points to a View that doesn't know how to get its baseline.");
        }

        int childTop = mBaselineChildTop;

        if (mOrientation == VERTICAL)
        {
            int majorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

            if (majorGravity == Gravity.BOTTOM)
            {
                childTop = getBottom() - getTop() - getPaddingBottom() - mTotalLength;
            }
            else if (majorGravity == Gravity.CENTER_VERTICAL)
            {
                childTop += ((getBottom() - getTop() - getPaddingTop() - getPaddingBottom()) - mTotalLength) / 2;
            }
        }

        LayoutParams params = (LayoutParams)child.getLayoutParams();
        return childTop + params.topMargin + childBaseline;
    }

    public int getBaselineAlignedChildIndex()
    {
        return mBaselineAlignedChildIndex;
    }

    public void setBaselineAlignedChildIndex(int index)
    {
        if (index < 0 || index >= getChildCount())
        {
            throw new IllegalArgumentException("base aligned child index out of range (0, " + getChildCount() + ")");
        }

        mBaselineAlignedChildIndex = index;
    }

    View getVirtualChildAt(int index)
    {
        return getChildAt(index);
    }

    int getVirtualChildCount()
    {
        return getChildCount();
    }

    public float getWeightSum()
    {
        return mWeightSum;
    }

    public void setWeightSum(float weightSum)
    {
        mWeightSum = Math.max(0.0f, weightSum);
    }

    public int getOrientation()
    {
        return mOrientation;
    }

    public void setOrientation(int orientation)
    {
        if (mOrientation == orientation) return;

        mOrientation = orientation;
        requestLayout();
    }

    public void setGravity(int gravity)
    {
        if (mGravity == gravity) return;

        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) gravity |= Gravity.START;
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) gravity |= Gravity.TOP;

        mGravity = gravity;
        requestLayout();
    }

    public int getGravity()
    {
        return mGravity;
    }

    public void setHorizontalGravity(int horizontalGravity)
    {
        int gravity = horizontalGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if ((mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == gravity) return;

        mGravity = (mGravity & ~Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) | gravity;
        requestLayout();
    }

    public void setVerticalGravity(int verticalGravity)
    {
        int gravity = verticalGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == gravity) return;

        mGravity = (mGravity & ~Gravity.VERTICAL_GRAVITY_MASK) | gravity;
        requestLayout();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection)
    {
        super.onRtlPropertiesChanged(layoutDirection);

        if (layoutDirection == mLayoutDirection) return;

        mLayoutDirection = layoutDirection;
        if (mOrientation == HORIZONTAL) requestLayout();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return LinearLayout.class.getName();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new LayoutParams(getContext(), attributes);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams()
    {
        if (mOrientation == VERTICAL) return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams source)
    {
        if (source instanceof LayoutParams) return new LayoutParams((LayoutParams)source);
        if (source instanceof MarginLayoutParams) return new LayoutParams((MarginLayoutParams)source);
        return new LayoutParams(source);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams)
    {
        return layoutParams instanceof LayoutParams;
    }

    private boolean isRtl()
    {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    protected boolean hasDividerBeforeChildAt(int childIndex)
    {
        if (childIndex == getVirtualChildCount()) return (mShowDividers & SHOW_DIVIDER_END) != 0;

        if (allViewsAreGoneBefore(childIndex)) return (mShowDividers & SHOW_DIVIDER_BEGINNING) != 0;

        return (mShowDividers & SHOW_DIVIDER_MIDDLE) != 0;
    }

    private boolean allViewsAreGoneBefore(int childIndex)
    {
        for (int index = childIndex - 1; index >= 0; index--)
        {
            View child = getVirtualChildAt(index);
            if (child != null && child.getVisibility() != GONE) return false;
        }

        return true;
    }

    private LayoutParams paramsFor(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof LayoutParams) return (LayoutParams)params;

        LayoutParams converted = generateLayoutParams(params == null ? generateDefaultLayoutParams() : params);
        child.setLayoutParams(converted);
        return converted;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if (mOrientation == VERTICAL) measureVertical(widthMeasureSpec, heightMeasureSpec);
        else measureHorizontal(widthMeasureSpec, heightMeasureSpec);
    }

    int measureNullChild(int childIndex)
    {
        return 0;
    }

    int getChildrenSkipCount(View child, int index)
    {
        return 0;
    }

    int getLocationOffset(View child)
    {
        return 0;
    }

    int getNextLocationOffset(View child)
    {
        return 0;
    }

    void measureChildBeforeLayout(View child, int childIndex, int widthMeasureSpec, int totalWidth, int heightMeasureSpec,
                                  int totalHeight)
    {
        measureChildWithMargins(child, widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
    }

    void measureVertical(int widthMeasureSpec, int heightMeasureSpec)
    {
        mTotalLength = 0;

        int maxWidth = 0;
        int childState = 0;
        int alternativeMaxWidth = 0;
        int weightedMaxWidth = 0;
        boolean allFillParent = true;
        float totalWeight = 0;

        int count = getVirtualChildCount();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        boolean matchWidth = false;
        boolean skippedMeasure = false;

        int baselineChildIndex = mBaselineAlignedChildIndex;
        boolean useLargestChild = mUseLargestChild;

        int largestChildHeight = Integer.MIN_VALUE;
        int consumedExcessSpace = 0;
        int nonSkippedChildCount = 0;

        for (int index = 0; index < count; ++index)
        {
            View child = getVirtualChildAt(index);

            if (child == null)
            {
                mTotalLength += measureNullChild(index);
                continue;
            }

            if (child.getVisibility() == View.GONE)
            {
                index += getChildrenSkipCount(child, index);
                continue;
            }

            nonSkippedChildCount++;
            if (hasDividerBeforeChildAt(index)) mTotalLength += mDividerHeight;

            LayoutParams params = paramsFor(child);
            totalWeight += params.weight;

            boolean useExcessSpace = params.height == 0 && params.weight > 0;

            if (heightMode == MeasureSpec.EXACTLY && useExcessSpace)
            {
                int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + params.topMargin + params.bottomMargin);
                skippedMeasure = true;
            }
            else
            {
                if (useExcessSpace) params.height = LayoutParams.WRAP_CONTENT;

                int usedHeight = totalWeight == 0 ? mTotalLength : 0;
                measureChildBeforeLayout(child, index, widthMeasureSpec, 0, heightMeasureSpec, usedHeight);

                int childHeight = child.getMeasuredHeight();

                if (useExcessSpace)
                {
                    params.height = 0;
                    consumedExcessSpace += childHeight;
                }

                int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + childHeight + params.topMargin + params.bottomMargin
                                        + getNextLocationOffset(child));

                if (useLargestChild) largestChildHeight = Math.max(childHeight, largestChildHeight);
            }

            if (baselineChildIndex >= 0 && baselineChildIndex == index + 1) mBaselineChildTop = mTotalLength;

            if (index < baselineChildIndex && params.weight > 0)
            {
                throw new RuntimeException("A child of LinearLayout with index less than mBaselineAlignedChildIndex has weight > 0, "
                                           + "which won't work.  Either remove the weight, or don't set "
                                           + "mBaselineAlignedChildIndex.");
            }

            boolean matchWidthLocally = false;

            if (widthMode != MeasureSpec.EXACTLY && params.width == LayoutParams.MATCH_PARENT)
            {
                matchWidth = true;
                matchWidthLocally = true;
            }

            int margin = params.leftMargin + params.rightMargin;
            int measuredWidth = child.getMeasuredWidth() + margin;

            maxWidth = Math.max(maxWidth, measuredWidth);
            childState = combineMeasuredStates(childState, child.getMeasuredState());

            allFillParent = allFillParent && params.width == LayoutParams.MATCH_PARENT;

            if (params.weight > 0)
            {
                weightedMaxWidth = Math.max(weightedMaxWidth, matchWidthLocally ? margin : measuredWidth);
            }
            else
            {
                alternativeMaxWidth = Math.max(alternativeMaxWidth, matchWidthLocally ? margin : measuredWidth);
            }

            index += getChildrenSkipCount(child, index);
        }

        if (nonSkippedChildCount > 0 && hasDividerBeforeChildAt(count)) mTotalLength += mDividerHeight;

        if (useLargestChild && (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED))
        {
            mTotalLength = 0;

            for (int index = 0; index < count; ++index)
            {
                View child = getVirtualChildAt(index);

                if (child == null)
                {
                    mTotalLength += measureNullChild(index);
                    continue;
                }

                if (child.getVisibility() == GONE)
                {
                    index += getChildrenSkipCount(child, index);
                    continue;
                }

                LayoutParams params = (LayoutParams)child.getLayoutParams();
                int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + largestChildHeight + params.topMargin + params.bottomMargin
                                        + getNextLocationOffset(child));
            }
        }

        mTotalLength += getPaddingTop() + getPaddingBottom();

        int heightSize = mTotalLength;
        heightSize = Math.max(heightSize, getSuggestedMinimumHeight());

        int heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0);
        heightSize = heightSizeAndState & MEASURED_SIZE_MASK;

        int remainingExcess = heightSize - mTotalLength + consumedExcessSpace;

        if (skippedMeasure || (remainingExcess != 0 && totalWeight > 0.0f))
        {
            float remainingWeightSum = mWeightSum > 0.0f ? mWeightSum : totalWeight;

            mTotalLength = 0;

            for (int index = 0; index < count; ++index)
            {
                View child = getVirtualChildAt(index);
                if (child == null || child.getVisibility() == View.GONE) continue;

                LayoutParams params = (LayoutParams)child.getLayoutParams();
                float childWeight = params.weight;

                if (childWeight > 0)
                {
                    int share = (int)(childWeight * remainingExcess / remainingWeightSum);
                    remainingExcess -= share;
                    remainingWeightSum -= childWeight;

                    int childHeight;
                    if (mUseLargestChild && heightMode != MeasureSpec.EXACTLY) childHeight = largestChildHeight;
                    else if (params.height == 0) childHeight = share;
                    else childHeight = child.getMeasuredHeight() + share;

                    int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(0, childHeight), MeasureSpec.EXACTLY);
                    int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight()
                                                                    + params.leftMargin + params.rightMargin, params.width);
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

                    childState = combineMeasuredStates(childState, child.getMeasuredState() & (MEASURED_STATE_MASK >> MEASURED_HEIGHT_STATE_SHIFT));
                }

                int margin = params.leftMargin + params.rightMargin;
                int measuredWidth = child.getMeasuredWidth() + margin;
                maxWidth = Math.max(maxWidth, measuredWidth);

                boolean matchWidthLocally = widthMode != MeasureSpec.EXACTLY && params.width == LayoutParams.MATCH_PARENT;

                alternativeMaxWidth = Math.max(alternativeMaxWidth, matchWidthLocally ? margin : measuredWidth);

                allFillParent = allFillParent && params.width == LayoutParams.MATCH_PARENT;

                int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + child.getMeasuredHeight() + params.topMargin
                                        + params.bottomMargin + getNextLocationOffset(child));
            }

            mTotalLength += getPaddingTop() + getPaddingBottom();
        }
        else
        {
            alternativeMaxWidth = Math.max(alternativeMaxWidth, weightedMaxWidth);

            if (useLargestChild && heightMode != MeasureSpec.EXACTLY)
            {
                for (int index = 0; index < count; index++)
                {
                    View child = getVirtualChildAt(index);
                    if (child == null || child.getVisibility() == View.GONE) continue;

                    LayoutParams params = (LayoutParams)child.getLayoutParams();
                    if (params.weight > 0)
                    {
                        child.measure(MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(), MeasureSpec.EXACTLY),
                                      MeasureSpec.makeMeasureSpec(largestChildHeight, MeasureSpec.EXACTLY));
                    }
                }
            }
        }

        if (!allFillParent && widthMode != MeasureSpec.EXACTLY) maxWidth = alternativeMaxWidth;

        maxWidth += getPaddingLeft() + getPaddingRight();
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState), heightSizeAndState);

        if (matchWidth) forceUniformWidth(count, heightMeasureSpec);
    }

    private void forceUniformWidth(int count, int heightMeasureSpec)
    {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);

        for (int index = 0; index < count; ++index)
        {
            View child = getVirtualChildAt(index);
            if (child == null || child.getVisibility() == GONE) continue;

            LayoutParams params = (LayoutParams)child.getLayoutParams();
            if (params.width != LayoutParams.MATCH_PARENT) continue;

            int oldHeight = params.height;
            params.height = child.getMeasuredHeight();

            measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasureSpec, 0);
            params.height = oldHeight;
        }
    }

    private static int gravityIndex(int gravity)
    {
        return (((gravity & Gravity.VERTICAL_GRAVITY_MASK) >> Gravity.AXIS_Y_SHIFT) & ~Gravity.AXIS_SPECIFIED) >> 1;
    }

    void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec)
    {
        mTotalLength = 0;

        int maxHeight = 0;
        int childState = 0;
        int alternativeMaxHeight = 0;
        int weightedMaxHeight = 0;
        boolean allFillParent = true;
        float totalWeight = 0;

        int count = getVirtualChildCount();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        boolean matchHeight = false;
        boolean skippedMeasure = false;

        if (mMaxAscent == null || mMaxDescent == null)
        {
            mMaxAscent = new int[VERTICAL_GRAVITY_COUNT];
            mMaxDescent = new int[VERTICAL_GRAVITY_COUNT];
        }

        int[] maxAscent = mMaxAscent;
        int[] maxDescent = mMaxDescent;

        for (int index = 0; index < VERTICAL_GRAVITY_COUNT; index++)
        {
            maxAscent[index] = -1;
            maxDescent[index] = -1;
        }

        boolean baselineAligned = mBaselineAligned;
        boolean useLargestChild = mUseLargestChild;
        boolean isExactly = widthMode == MeasureSpec.EXACTLY;

        int largestChildWidth = Integer.MIN_VALUE;
        int usedExcessSpace = 0;
        int nonSkippedChildCount = 0;

        for (int index = 0; index < count; ++index)
        {
            View child = getVirtualChildAt(index);

            if (child == null)
            {
                mTotalLength += measureNullChild(index);
                continue;
            }

            if (child.getVisibility() == GONE)
            {
                index += getChildrenSkipCount(child, index);
                continue;
            }

            nonSkippedChildCount++;
            if (hasDividerBeforeChildAt(index)) mTotalLength += mDividerWidth;

            LayoutParams params = paramsFor(child);
            totalWeight += params.weight;

            boolean useExcessSpace = params.width == 0 && params.weight > 0;

            if (widthMode == MeasureSpec.EXACTLY && useExcessSpace)
            {
                if (isExactly)
                {
                    mTotalLength += params.leftMargin + params.rightMargin;
                }
                else
                {
                    int totalLength = mTotalLength;
                    mTotalLength = Math.max(totalLength, totalLength + params.leftMargin + params.rightMargin);
                }

                if (baselineAligned)
                {
                    int freeWidthSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.UNSPECIFIED);
                    int freeHeightSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.UNSPECIFIED);
                    child.measure(freeWidthSpec, freeHeightSpec);
                }
                else
                {
                    skippedMeasure = true;
                }
            }
            else
            {
                if (useExcessSpace) params.width = LayoutParams.WRAP_CONTENT;

                int usedWidth = totalWeight == 0 ? mTotalLength : 0;
                measureChildBeforeLayout(child, index, widthMeasureSpec, usedWidth, heightMeasureSpec, 0);

                int childWidth = child.getMeasuredWidth();

                if (useExcessSpace)
                {
                    params.width = 0;
                    usedExcessSpace += childWidth;
                }

                if (isExactly)
                {
                    mTotalLength += childWidth + params.leftMargin + params.rightMargin + getNextLocationOffset(child);
                }
                else
                {
                    int totalLength = mTotalLength;
                    mTotalLength = Math.max(totalLength, totalLength + childWidth + params.leftMargin + params.rightMargin
                                            + getNextLocationOffset(child));
                }

                if (useLargestChild) largestChildWidth = Math.max(childWidth, largestChildWidth);
            }

            boolean matchHeightLocally = false;

            if (heightMode != MeasureSpec.EXACTLY && params.height == LayoutParams.MATCH_PARENT)
            {
                matchHeight = true;
                matchHeightLocally = true;
            }

            int margin = params.topMargin + params.bottomMargin;
            int childHeight = child.getMeasuredHeight() + margin;
            childState = combineMeasuredStates(childState, child.getMeasuredState());

            if (baselineAligned)
            {
                int childBaseline = child.getBaseline();

                if (childBaseline != -1)
                {
                    int gravity = (params.gravity < 0 ? mGravity : params.gravity) & Gravity.VERTICAL_GRAVITY_MASK;
                    int position = gravityIndex(gravity);

                    maxAscent[position] = Math.max(maxAscent[position], childBaseline);
                    maxDescent[position] = Math.max(maxDescent[position], childHeight - childBaseline);
                }
            }

            maxHeight = Math.max(maxHeight, childHeight);

            allFillParent = allFillParent && params.height == LayoutParams.MATCH_PARENT;

            if (params.weight > 0)
            {
                weightedMaxHeight = Math.max(weightedMaxHeight, matchHeightLocally ? margin : childHeight);
            }
            else
            {
                alternativeMaxHeight = Math.max(alternativeMaxHeight, matchHeightLocally ? margin : childHeight);
            }

            index += getChildrenSkipCount(child, index);
        }

        if (nonSkippedChildCount > 0 && hasDividerBeforeChildAt(count)) mTotalLength += mDividerWidth;

        if (maxAscent[INDEX_TOP] != -1 || maxAscent[INDEX_CENTER_VERTICAL] != -1 || maxAscent[INDEX_BOTTOM] != -1
            || maxAscent[INDEX_FILL] != -1)
        {
            int ascent = Math.max(maxAscent[INDEX_FILL], Math.max(maxAscent[INDEX_CENTER_VERTICAL],
                                                                  Math.max(maxAscent[INDEX_TOP], maxAscent[INDEX_BOTTOM])));
            int descent = Math.max(maxDescent[INDEX_FILL], Math.max(maxDescent[INDEX_CENTER_VERTICAL],
                                                                    Math.max(maxDescent[INDEX_TOP], maxDescent[INDEX_BOTTOM])));
            maxHeight = Math.max(maxHeight, ascent + descent);
        }

        if (useLargestChild && (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED))
        {
            mTotalLength = 0;

            for (int index = 0; index < count; ++index)
            {
                View child = getVirtualChildAt(index);

                if (child == null)
                {
                    mTotalLength += measureNullChild(index);
                    continue;
                }

                if (child.getVisibility() == GONE)
                {
                    index += getChildrenSkipCount(child, index);
                    continue;
                }

                LayoutParams params = (LayoutParams)child.getLayoutParams();

                if (isExactly)
                {
                    mTotalLength += largestChildWidth + params.leftMargin + params.rightMargin + getNextLocationOffset(child);
                }
                else
                {
                    int totalLength = mTotalLength;
                    mTotalLength = Math.max(totalLength, totalLength + largestChildWidth + params.leftMargin + params.rightMargin
                                            + getNextLocationOffset(child));
                }
            }
        }

        mTotalLength += getPaddingLeft() + getPaddingRight();

        int widthSize = mTotalLength;
        widthSize = Math.max(widthSize, getSuggestedMinimumWidth());

        int widthSizeAndState = resolveSizeAndState(widthSize, widthMeasureSpec, 0);
        widthSize = widthSizeAndState & MEASURED_SIZE_MASK;

        int remainingExcess = widthSize - mTotalLength + usedExcessSpace;

        if (skippedMeasure || (remainingExcess != 0 && totalWeight > 0.0f))
        {
            float remainingWeightSum = mWeightSum > 0.0f ? mWeightSum : totalWeight;

            for (int index = 0; index < VERTICAL_GRAVITY_COUNT; index++)
            {
                maxAscent[index] = -1;
                maxDescent[index] = -1;
            }

            maxHeight = -1;
            mTotalLength = 0;

            for (int index = 0; index < count; ++index)
            {
                View child = getVirtualChildAt(index);
                if (child == null || child.getVisibility() == View.GONE) continue;

                LayoutParams params = (LayoutParams)child.getLayoutParams();
                float childWeight = params.weight;

                if (childWeight > 0)
                {
                    int share = (int)(childWeight * remainingExcess / remainingWeightSum);
                    remainingExcess -= share;
                    remainingWeightSum -= childWeight;

                    int childWidth;
                    if (mUseLargestChild && widthMode != MeasureSpec.EXACTLY) childWidth = largestChildWidth;
                    else if (params.width == 0) childWidth = share;
                    else childWidth = child.getMeasuredWidth() + share;

                    int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(0, childWidth), MeasureSpec.EXACTLY);
                    int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom()
                                                                     + params.topMargin + params.bottomMargin, params.height);
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

                    childState = combineMeasuredStates(childState, child.getMeasuredState() & MEASURED_STATE_MASK);
                }

                if (isExactly)
                {
                    mTotalLength += child.getMeasuredWidth() + params.leftMargin + params.rightMargin + getNextLocationOffset(child);
                }
                else
                {
                    int totalLength = mTotalLength;
                    mTotalLength = Math.max(totalLength, totalLength + child.getMeasuredWidth() + params.leftMargin
                                            + params.rightMargin + getNextLocationOffset(child));
                }

                boolean matchHeightLocally = heightMode != MeasureSpec.EXACTLY && params.height == LayoutParams.MATCH_PARENT;

                int margin = params.topMargin + params.bottomMargin;
                int childHeight = child.getMeasuredHeight() + margin;
                maxHeight = Math.max(maxHeight, childHeight);
                alternativeMaxHeight = Math.max(alternativeMaxHeight, matchHeightLocally ? margin : childHeight);

                allFillParent = allFillParent && params.height == LayoutParams.MATCH_PARENT;

                if (baselineAligned)
                {
                    int childBaseline = child.getBaseline();

                    if (childBaseline != -1)
                    {
                        int gravity = (params.gravity < 0 ? mGravity : params.gravity) & Gravity.VERTICAL_GRAVITY_MASK;
                        int position = gravityIndex(gravity);

                        maxAscent[position] = Math.max(maxAscent[position], childBaseline);
                        maxDescent[position] = Math.max(maxDescent[position], childHeight - childBaseline);
                    }
                }
            }

            mTotalLength += getPaddingLeft() + getPaddingRight();

            if (maxAscent[INDEX_TOP] != -1 || maxAscent[INDEX_CENTER_VERTICAL] != -1 || maxAscent[INDEX_BOTTOM] != -1
                || maxAscent[INDEX_FILL] != -1)
            {
                int ascent = Math.max(maxAscent[INDEX_FILL], Math.max(maxAscent[INDEX_CENTER_VERTICAL],
                                                                      Math.max(maxAscent[INDEX_TOP], maxAscent[INDEX_BOTTOM])));
                int descent = Math.max(maxDescent[INDEX_FILL], Math.max(maxDescent[INDEX_CENTER_VERTICAL],
                                                                        Math.max(maxDescent[INDEX_TOP], maxDescent[INDEX_BOTTOM])));
                maxHeight = Math.max(maxHeight, ascent + descent);
            }
        }
        else
        {
            alternativeMaxHeight = Math.max(alternativeMaxHeight, weightedMaxHeight);

            if (useLargestChild && widthMode != MeasureSpec.EXACTLY)
            {
                for (int index = 0; index < count; index++)
                {
                    View child = getVirtualChildAt(index);
                    if (child == null || child.getVisibility() == View.GONE) continue;

                    LayoutParams params = (LayoutParams)child.getLayoutParams();
                    if (params.weight > 0)
                    {
                        child.measure(MeasureSpec.makeMeasureSpec(largestChildWidth, MeasureSpec.EXACTLY),
                                      MeasureSpec.makeMeasureSpec(child.getMeasuredHeight(), MeasureSpec.EXACTLY));
                    }
                }
            }
        }

        if (!allFillParent && heightMode != MeasureSpec.EXACTLY) maxHeight = alternativeMaxHeight;

        maxHeight += getPaddingTop() + getPaddingBottom();
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());

        setMeasuredDimension(widthSizeAndState | (childState & MEASURED_STATE_MASK),
                             resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));

        if (matchHeight) forceUniformHeight(count, widthMeasureSpec);
    }

    private void forceUniformHeight(int count, int widthMeasureSpec)
    {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);

        for (int index = 0; index < count; ++index)
        {
            View child = getVirtualChildAt(index);
            if (child == null || child.getVisibility() == GONE) continue;

            LayoutParams params = (LayoutParams)child.getLayoutParams();
            if (params.height != LayoutParams.MATCH_PARENT) continue;

            int oldWidth = params.width;
            params.width = child.getMeasuredWidth();

            measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0);
            params.width = oldWidth;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        if (mOrientation == VERTICAL) layoutVertical(left, top, right, bottom);
        else layoutHorizontal(left, top, right, bottom);
    }

    void layoutVertical(int left, int top, int right, int bottom)
    {
        int paddingLeft = getPaddingLeft();

        int width = right - left;
        int childRight = width - getPaddingRight();
        int childSpace = width - paddingLeft - getPaddingRight();

        int count = getVirtualChildCount();

        int majorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        int minorGravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;

        int childTop;

        if (majorGravity == Gravity.BOTTOM) childTop = getPaddingTop() + bottom - top - mTotalLength;
        else if (majorGravity == Gravity.CENTER_VERTICAL) childTop = getPaddingTop() + (bottom - top - mTotalLength) / 2;
        else childTop = getPaddingTop();

        for (int index = 0; index < count; index++)
        {
            View child = getVirtualChildAt(index);

            if (child == null)
            {
                childTop += measureNullChild(index);
                continue;
            }

            if (child.getVisibility() == GONE) continue;

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            LayoutParams params = paramsFor(child);

            int gravity = params.gravity < 0 ? minorGravity : params.gravity;
            int absoluteGravity = Gravity.getAbsoluteGravity(gravity, getLayoutDirection());

            int childLeft;
            int horizontal = absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK;

            if (horizontal == Gravity.CENTER_HORIZONTAL)
            {
                childLeft = paddingLeft + ((childSpace - childWidth) / 2) + params.leftMargin - params.rightMargin;
            }
            else if (horizontal == Gravity.RIGHT)
            {
                childLeft = childRight - childWidth - params.rightMargin;
            }
            else
            {
                childLeft = paddingLeft + params.leftMargin;
            }

            if (hasDividerBeforeChildAt(index)) childTop += mDividerHeight;

            childTop += params.topMargin;
            int offsetTop = childTop + getLocationOffset(child);
            child.layout(childLeft, offsetTop, childLeft + childWidth, offsetTop + childHeight);
            childTop += childHeight + params.bottomMargin + getNextLocationOffset(child);

            index += getChildrenSkipCount(child, index);
        }
    }

    void layoutHorizontal(int left, int top, int right, int bottom)
    {
        boolean rtl = isRtl();
        int paddingTop = getPaddingTop();

        int height = bottom - top;
        int childBottom = height - getPaddingBottom();
        int childSpace = height - paddingTop - getPaddingBottom();

        int count = getVirtualChildCount();

        int majorGravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        int minorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        boolean baselineAligned = mBaselineAligned;

        int[] maxAscent = mMaxAscent;
        int[] maxDescent = mMaxDescent;

        int childLeft;
        int absoluteMajor = Gravity.getAbsoluteGravity(majorGravity, getLayoutDirection());

        if (absoluteMajor == Gravity.RIGHT) childLeft = getPaddingLeft() + right - left - mTotalLength;
        else if (absoluteMajor == Gravity.CENTER_HORIZONTAL) childLeft = getPaddingLeft() + (right - left - mTotalLength) / 2;
        else childLeft = getPaddingLeft();

        int start = 0;
        int direction = 1;

        if (rtl)
        {
            start = count - 1;
            direction = -1;
        }

        for (int step = 0; step < count; step++)
        {
            int childIndex = start + direction * step;
            View child = getVirtualChildAt(childIndex);

            if (child == null)
            {
                childLeft += measureNullChild(childIndex);
                continue;
            }

            if (child.getVisibility() == GONE) continue;

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            int childBaseline = -1;

            LayoutParams params = paramsFor(child);

            if (baselineAligned && params.height != LayoutParams.MATCH_PARENT) childBaseline = child.getBaseline();

            int gravity = params.gravity < 0 ? minorGravity : params.gravity;
            int vertical = gravity & Gravity.VERTICAL_GRAVITY_MASK;
            int childTop;

            if (vertical == Gravity.CENTER_VERTICAL)
            {
                childTop = paddingTop + ((childSpace - childHeight) / 2) + params.topMargin - params.bottomMargin;
            }
            else if (vertical == Gravity.BOTTOM)
            {
                childTop = childBottom - childHeight - params.bottomMargin;

                if (childBaseline != -1 && maxDescent != null)
                {
                    int descent = child.getMeasuredHeight() - childBaseline;
                    childTop -= (maxDescent[INDEX_BOTTOM] - descent);
                }
            }
            else
            {
                childTop = paddingTop + params.topMargin;

                if (childBaseline != -1 && maxAscent != null) childTop += maxAscent[INDEX_TOP] - childBaseline;
            }

            if (hasDividerBeforeChildAt(childIndex)) childLeft += mDividerWidth;

            childLeft += params.leftMargin;
            int offsetLeft = childLeft + getLocationOffset(child);
            child.layout(offsetLeft, childTop, offsetLeft + childWidth, childTop + childHeight);
            childLeft += childWidth + params.rightMargin + getNextLocationOffset(child);

            step += getChildrenSkipCount(child, childIndex);
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams
    {
        private static StyleableSet sChildStyleable;

        public float weight;
        public int gravity = -1;

        public LayoutParams(int width, int height)
        {
            super(width, height);
        }

        public LayoutParams(int width, int height, float weight)
        {
            super(width, height);
            this.weight = weight;
        }

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);
            if (attributes != null) readChildAttributes(context, attributes);
        }

        private void readChildAttributes(Context context, AttributeSet attributes)
        {
            if (sChildStyleable == null)
            {
                sChildStyleable = StyleableSet.of(context.getResources(), "layout_weight", "layout_gravity");
            }
            if (sChildStyleable.isEmpty()) return;

            android.content.res.TypedArray array =
                context.obtainStyledAttributes(attributes, sChildStyleable.attributes(), 0, 0);

            int weightIndex = sChildStyleable.indexOf("layout_weight");
            int gravityIndex = sChildStyleable.indexOf("layout_gravity");

            if (weightIndex >= 0) weight = array.getFloat(weightIndex, weight);
            if (gravityIndex >= 0) gravity = array.getInt(gravityIndex, gravity);

            array.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source)
        {
            super(source);
        }

        public LayoutParams(LayoutParams source)
        {
            super(source);

            this.weight = source.weight;
            this.gravity = source.gravity;
        }

        public String debug(String output)
        {
            return output + "LinearLayout.LayoutParams={width=" + sizeToString(width) + ", height=" + sizeToString(height)
                + " weight=" + weight + "}";
        }

        static String sizeToString(int size)
        {
            if (size == WRAP_CONTENT) return "wrap-content";
            if (size == MATCH_PARENT) return "match-parent";
            return String.valueOf(size);
        }
    }
}
