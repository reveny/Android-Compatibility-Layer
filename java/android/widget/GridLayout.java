package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class GridLayout extends ViewGroup
{
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;
    public static final int UNDEFINED = Integer.MIN_VALUE;
    public static final int ALIGN_BOUNDS = 0;
    public static final int ALIGN_MARGINS = 1;

    static final int MAX_SIZE = 100000;
    static final int DEFAULT_CONTAINER_MARGIN = 0;

    private static final int DEFAULT_ORIENTATION = HORIZONTAL;
    private static final int DEFAULT_COUNT = UNDEFINED;
    private static final boolean DEFAULT_USE_DEFAULT_MARGINS = false;
    private static final boolean DEFAULT_ORDER_PRESERVED = true;
    private static final int DEFAULT_ALIGNMENT_MODE = ALIGN_MARGINS;

    static final int INFLEXIBLE = 0;
    static final int CAN_STRETCH = 2;

    private static StyleableSet sStyleable;

    private final Axis mHorizontalAxis = new Axis(true);
    private final Axis mVerticalAxis = new Axis(false);
    private int mOrientation = DEFAULT_ORIENTATION;
    private boolean mUseDefaultMargins = DEFAULT_USE_DEFAULT_MARGINS;
    private int mAlignmentMode = DEFAULT_ALIGNMENT_MODE;
    private int mDefaultGap;

    public GridLayout(Context context)
    {
        this(context, null);
    }

    public GridLayout(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public GridLayout(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public GridLayout(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        float density = context.getResources().getDisplayMetrics().density;
        mDefaultGap = Math.round(8 * density);

        if (attributes == null) return;

        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "orientation", "rowCount", "columnCount", "useDefaultMargins",
                                         "alignmentMode", "rowOrderPreserved", "columnOrderPreserved");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int index = sStyleable.indexOf("rowCount");
        if (index >= 0) setRowCount(array.getInt(index, DEFAULT_COUNT));

        index = sStyleable.indexOf("columnCount");
        if (index >= 0) setColumnCount(array.getInt(index, DEFAULT_COUNT));

        index = sStyleable.indexOf("orientation");
        if (index >= 0) setOrientation(array.getInt(index, DEFAULT_ORIENTATION));

        index = sStyleable.indexOf("useDefaultMargins");
        if (index >= 0) setUseDefaultMargins(array.getBoolean(index, DEFAULT_USE_DEFAULT_MARGINS));

        index = sStyleable.indexOf("alignmentMode");
        if (index >= 0) setAlignmentMode(array.getInt(index, DEFAULT_ALIGNMENT_MODE));

        index = sStyleable.indexOf("rowOrderPreserved");
        if (index >= 0) setRowOrderPreserved(array.getBoolean(index, DEFAULT_ORDER_PRESERVED));

        index = sStyleable.indexOf("columnOrderPreserved");
        if (index >= 0) setColumnOrderPreserved(array.getBoolean(index, DEFAULT_ORDER_PRESERVED));

        array.recycle();
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

    public int getRowCount()
    {
        return mVerticalAxis.getCount();
    }

    public void setRowCount(int rowCount)
    {
        mVerticalAxis.setCount(rowCount);
        requestLayout();
    }

    public int getColumnCount()
    {
        return mHorizontalAxis.getCount();
    }

    public void setColumnCount(int columnCount)
    {
        mHorizontalAxis.setCount(columnCount);
        requestLayout();
    }

    public boolean getUseDefaultMargins()
    {
        return mUseDefaultMargins;
    }

    public void setUseDefaultMargins(boolean useDefaultMargins)
    {
        mUseDefaultMargins = useDefaultMargins;
        requestLayout();
    }

    public int getAlignmentMode()
    {
        return mAlignmentMode;
    }

    public void setAlignmentMode(int alignmentMode)
    {
        mAlignmentMode = alignmentMode;
        requestLayout();
    }

    public boolean isRowOrderPreserved()
    {
        return mVerticalAxis.mOrderPreserved;
    }

    public void setRowOrderPreserved(boolean rowOrderPreserved)
    {
        mVerticalAxis.mOrderPreserved = rowOrderPreserved;
        requestLayout();
    }

    public boolean isColumnOrderPreserved()
    {
        return mHorizontalAxis.mOrderPreserved;
    }

    public void setColumnOrderPreserved(boolean columnOrderPreserved)
    {
        mHorizontalAxis.mOrderPreserved = columnOrderPreserved;
        requestLayout();
    }

    @Override
    public void onViewAdded(View child)
    {
        super.onViewAdded(child);
    }

    @Override
    public void onViewRemoved(View child)
    {
        super.onViewRemoved(child);
    }

    @Override
    public void requestLayout()
    {
        super.requestLayout();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return GridLayout.class.getName();
    }

    private boolean isRtl()
    {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    static boolean isViewRtl(View view)
    {
        return view.getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    private int getDefaultMargin(View child, LayoutParams params, boolean horizontal, boolean leading)
    {
        if (!mUseDefaultMargins) return 0;
        if (child.getClass() == Space.class) return 0;

        return mDefaultGap / 2;
    }

    private int getMargin1(View child, boolean horizontal, boolean leading)
    {
        LayoutParams params = getLayoutParams(child);
        int margin = horizontal ? (leading ? params.leftMargin : params.rightMargin) : (leading ? params.topMargin : params.bottomMargin);

        return margin == UNDEFINED ? getDefaultMargin(child, params, horizontal, leading) : margin;
    }

    private int getMargin(View child, boolean horizontal, boolean leading)
    {
        if (mAlignmentMode == ALIGN_MARGINS) return getMargin1(child, horizontal, leading);

        Axis axis = horizontal ? mHorizontalAxis : mVerticalAxis;
        int[] margins = leading ? axis.getLeadingMargins() : axis.getTrailingMargins();
        LayoutParams params = getLayoutParams(child);
        Spec spec = horizontal ? params.columnSpec : params.rowSpec;
        int index = leading ? spec.mSpan.min : spec.mSpan.max;

        return index >= 0 && index < margins.length ? margins[index] : 0;
    }

    private int getTotalMargin(View child, boolean horizontal)
    {
        return getMargin(child, horizontal, true) + getMargin(child, horizontal, false);
    }

    private static boolean fits(int[] array, int value, int start, int end)
    {
        if (end > array.length) return false;

        for (int index = start; index < end; index++)
        {
            if (array[index] > value) return false;
        }

        return true;
    }

    private static void procrusteanFill(int[] array, int start, int end, int value)
    {
        int length = array.length;
        Arrays.fill(array, Math.min(start, length), Math.min(end, length), value);
    }

    private static void setCellGroup(LayoutParams params, int row, int rowSpan, int column, int columnSpan)
    {
        params.setRowSpecSpan(new Interval(row, row + rowSpan));
        params.setColumnSpecSpan(new Interval(column, column + columnSpan));
    }

    private static int clip(Interval minorRange, boolean minorWasDefined, int count)
    {
        int size = minorRange.size();
        if (count == 0) return size;

        int minimum = minorWasDefined ? Math.min(minorRange.min, count) : 0;
        return Math.min(size, count - minimum);
    }

    private void validateLayoutParams()
    {
        boolean horizontal = mOrientation == HORIZONTAL;
        Axis axis = horizontal ? mHorizontalAxis : mVerticalAxis;
        int count = axis.mDefinedCount != UNDEFINED ? axis.mDefinedCount : 0;

        int major = 0;
        int minor = 0;
        int[] maxSizes = new int[count];

        for (int index = 0, total = getChildCount(); index < total; index++)
        {
            LayoutParams params = getLayoutParams(getChildAt(index));

            Spec majorSpec = horizontal ? params.rowSpec : params.columnSpec;
            Interval majorRange = majorSpec.mSpan;
            boolean majorWasDefined = majorSpec.mStartDefined;
            int majorSpan = majorRange.size();
            if (majorWasDefined) major = majorRange.min;

            Spec minorSpec = horizontal ? params.columnSpec : params.rowSpec;
            Interval minorRange = minorSpec.mSpan;
            boolean minorWasDefined = minorSpec.mStartDefined;
            int minorSpan = clip(minorRange, minorWasDefined, count);
            if (minorWasDefined) minor = minorRange.min;

            if (count != 0)
            {
                if (!majorWasDefined || !minorWasDefined)
                {
                    while (!fits(maxSizes, major, minor, minor + minorSpan))
                    {
                        if (minorWasDefined)
                        {
                            major++;
                        }
                        else if (minor + minorSpan <= count)
                        {
                            minor++;
                        }
                        else
                        {
                            minor = 0;
                            major++;
                        }
                    }
                }

                procrusteanFill(maxSizes, minor, minor + minorSpan, major + majorSpan);
            }

            if (horizontal) setCellGroup(params, major, majorSpan, minor, minorSpan);
            else setCellGroup(params, minor, minorSpan, major, majorSpan);

            minor = minor + minorSpan;
        }
    }

    final LayoutParams getLayoutParams(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof LayoutParams) return (LayoutParams)params;

        LayoutParams converted = generateLayoutParams(params == null ? generateDefaultLayoutParams() : params);
        child.setLayoutParams(converted);
        return converted;
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams)
    {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new LayoutParams(getContext(), attributes);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams source)
    {
        if (source instanceof LayoutParams) return new LayoutParams((LayoutParams)source);
        if (source instanceof MarginLayoutParams) return new LayoutParams((MarginLayoutParams)source);
        return new LayoutParams(source);
    }

    private void measureChildWithMargins2(View child, int parentWidthSpec, int parentHeightSpec, int childWidth, int childHeight)
    {
        int childWidthSpec = getChildMeasureSpec(parentWidthSpec, getTotalMargin(child, true), childWidth);
        int childHeightSpec = getChildMeasureSpec(parentHeightSpec, getTotalMargin(child, false), childHeight);
        child.measure(childWidthSpec, childHeightSpec);
    }

    private void measureChildrenWithMargins(int widthSpec, int heightSpec, boolean firstPass)
    {
        for (int index = 0, count = getChildCount(); index < count; index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == View.GONE) continue;

            LayoutParams params = getLayoutParams(child);

            if (firstPass)
            {
                measureChildWithMargins2(child, widthSpec, heightSpec, params.width, params.height);
                continue;
            }

            boolean horizontal = mOrientation == HORIZONTAL;
            Spec spec = horizontal ? params.columnSpec : params.rowSpec;
            if (spec.getAbsoluteAlignment(horizontal) != FILL) continue;

            Interval span = spec.mSpan;
            Axis axis = horizontal ? mHorizontalAxis : mVerticalAxis;
            int[] locations = axis.getLocations();
            int cellSize = locations[span.max] - locations[span.min];
            int viewSize = cellSize - getTotalMargin(child, horizontal);

            if (horizontal) measureChildWithMargins2(child, widthSpec, heightSpec, viewSize, params.height);
            else measureChildWithMargins2(child, widthSpec, heightSpec, params.width, viewSize);
        }
    }

    static int adjust(int measureSpec, int delta)
    {
        return MeasureSpec.makeMeasureSpec(Math.max(0, MeasureSpec.getSize(measureSpec) + delta), MeasureSpec.getMode(measureSpec));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec)
    {
        validateLayoutParams();
        mHorizontalAxis.invalidateValues();
        mVerticalAxis.invalidateValues();

        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();

        int widthSpecSansPadding = adjust(widthSpec, -horizontalPadding);
        int heightSpecSansPadding = adjust(heightSpec, -verticalPadding);

        measureChildrenWithMargins(widthSpecSansPadding, heightSpecSansPadding, true);

        int widthSansPadding;
        int heightSansPadding;

        if (mOrientation == HORIZONTAL)
        {
            widthSansPadding = mHorizontalAxis.getMeasure(widthSpecSansPadding);
            measureChildrenWithMargins(widthSpecSansPadding, heightSpecSansPadding, false);
            heightSansPadding = mVerticalAxis.getMeasure(heightSpecSansPadding);
        }
        else
        {
            heightSansPadding = mVerticalAxis.getMeasure(heightSpecSansPadding);
            measureChildrenWithMargins(widthSpecSansPadding, heightSpecSansPadding, false);
            widthSansPadding = mHorizontalAxis.getMeasure(widthSpecSansPadding);
        }

        int measuredWidth = Math.max(widthSansPadding + horizontalPadding, getSuggestedMinimumWidth());
        int measuredHeight = Math.max(heightSansPadding + verticalPadding, getSuggestedMinimumHeight());

        setMeasuredDimension(resolveSizeAndState(measuredWidth, widthSpec, 0), resolveSizeAndState(measuredHeight, heightSpec, 0));
    }

    private int getMeasurement(View child, boolean horizontal)
    {
        return horizontal ? child.getMeasuredWidth() : child.getMeasuredHeight();
    }

    final int getMeasurementIncludingMargin(View child, boolean horizontal)
    {
        if (child.getVisibility() == View.GONE) return 0;

        return getMeasurement(child, horizontal) + getTotalMargin(child, horizontal);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        validateLayoutParams();

        int targetWidth = right - left;
        int targetHeight = bottom - top;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        mHorizontalAxis.invalidateValues();
        mVerticalAxis.invalidateValues();
        mHorizontalAxis.layout(targetWidth - paddingLeft - paddingRight);
        mVerticalAxis.layout(targetHeight - paddingTop - paddingBottom);

        int[] horizontalLocations = mHorizontalAxis.getLocations();
        int[] verticalLocations = mVerticalAxis.getLocations();
        Bounds[] horizontalBounds = mHorizontalAxis.getChildBounds();
        Bounds[] verticalBounds = mVerticalAxis.getChildBounds();

        for (int index = 0, count = getChildCount(); index < count; index++)
        {
            View child = getChildAt(index);
            if (child.getVisibility() == View.GONE) continue;

            LayoutParams params = getLayoutParams(child);
            Spec columnSpec = params.columnSpec;
            Spec rowSpec = params.rowSpec;

            Interval columnSpan = columnSpec.mSpan;
            Interval rowSpan = rowSpec.mSpan;

            int x1 = horizontalLocations[columnSpan.min];
            int y1 = verticalLocations[rowSpan.min];

            int x2 = horizontalLocations[columnSpan.max];
            int y2 = verticalLocations[rowSpan.max];

            int cellWidth = x2 - x1;
            int cellHeight = y2 - y1;

            int measuredWidth = getMeasurement(child, true);
            int measuredHeight = getMeasurement(child, false);

            Alignment horizontalAlignment = columnSpec.getAbsoluteAlignment(true);
            Alignment verticalAlignment = rowSpec.getAbsoluteAlignment(false);

            Bounds boundsX = horizontalBounds[index];
            Bounds boundsY = verticalBounds[index];

            int gravityOffsetX = horizontalAlignment.getGravityOffset(child, cellWidth - boundsX.size(true));
            int gravityOffsetY = verticalAlignment.getGravityOffset(child, cellHeight - boundsY.size(true));

            int leftMargin = getMargin(child, true, true);
            int topMargin = getMargin(child, false, true);
            int rightMargin = getMargin(child, true, false);
            int bottomMargin = getMargin(child, false, false);

            int sumMarginsX = leftMargin + rightMargin;
            int sumMarginsY = topMargin + bottomMargin;

            int alignmentOffsetX = boundsX.getOffset(child, horizontalAlignment, measuredWidth + sumMarginsX);
            int alignmentOffsetY = boundsY.getOffset(child, verticalAlignment, measuredHeight + sumMarginsY);

            int width = horizontalAlignment.getSizeInCell(child, measuredWidth, cellWidth - sumMarginsX);
            int height = verticalAlignment.getSizeInCell(child, measuredHeight, cellHeight - sumMarginsY);

            int dx = x1 + gravityOffsetX + alignmentOffsetX;

            int childLeft = !isRtl() ? paddingLeft + leftMargin + dx : targetWidth - width - paddingRight - rightMargin - dx;
            int childTop = paddingTop + y1 + gravityOffsetY + alignmentOffsetY + topMargin;

            if (width != child.getMeasuredWidth() || height != child.getMeasuredHeight())
            {
                child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }

            child.layout(childLeft, childTop, childLeft + width, childTop + height);
        }
    }

    final class Axis
    {
        final boolean mHorizontal;
        int mDefinedCount = UNDEFINED;
        boolean mOrderPreserved = DEFAULT_ORDER_PRESERVED;

        private int mParentMin;
        private int mParentMax = MAX_SIZE;
        private int[] mLocations;
        private int[] mLeadingMargins;
        private int[] mTrailingMargins;
        private int[] mDeltas;
        private Bounds[] mChildBounds;

        Axis(boolean horizontal)
        {
            mHorizontal = horizontal;
        }

        private int getMaxIndex()
        {
            int result = -1;

            for (int index = 0, count = getChildCount(); index < count; index++)
            {
                View child = getChildAt(index);
                LayoutParams params = getLayoutParams(child);
                Spec spec = mHorizontal ? params.columnSpec : params.rowSpec;
                Interval span = spec.mSpan;

                result = Math.max(result, span.min);
                result = Math.max(result, span.max);
                result = Math.max(result, span.size());
            }

            return result == -1 ? UNDEFINED : result;
        }

        int getCount()
        {
            return Math.max(mDefinedCount, getMaxIndex());
        }

        private int getSafeCount()
        {
            return Math.max(0, getCount());
        }

        void setCount(int count)
        {
            if (count != UNDEFINED && count < getMaxIndex())
            {
                throw new IllegalArgumentException((mHorizontal ? "column" : "row")
                                                   + "Count must be greater than or equal to the maximum of all grid indices "
                                                   + "(and spans) defined in the LayoutParams of each child");
            }

            mDefinedCount = count;
        }

        void invalidateValues()
        {
            mLocations = null;
            mLeadingMargins = null;
            mTrailingMargins = null;
            mChildBounds = null;
        }

        int[] getLeadingMargins()
        {
            if (mLeadingMargins == null) mLeadingMargins = computeMargins(true);
            return mLeadingMargins;
        }

        int[] getTrailingMargins()
        {
            if (mTrailingMargins == null) mTrailingMargins = computeMargins(false);
            return mTrailingMargins;
        }

        private int[] computeMargins(boolean leading)
        {
            int[] margins = new int[getSafeCount() + 1];

            for (int index = 0, count = getChildCount(); index < count; index++)
            {
                View child = getChildAt(index);
                if (child.getVisibility() == View.GONE) continue;

                LayoutParams params = getLayoutParams(child);
                Spec spec = mHorizontal ? params.columnSpec : params.rowSpec;
                int position = leading ? spec.mSpan.min : spec.mSpan.max;
                if (position < 0 || position >= margins.length) continue;

                margins[position] = Math.max(margins[position], getMargin1(child, mHorizontal, leading));
            }

            return margins;
        }

        private int[] getDeltas()
        {
            if (mDeltas == null || mDeltas.length != getChildCount()) mDeltas = new int[getChildCount()];
            return mDeltas;
        }

        Bounds[] getChildBounds()
        {
            if (mChildBounds == null) computeBounds();
            return mChildBounds;
        }

        private void computeBounds()
        {
            int count = getChildCount();
            int[] deltas = getDeltas();
            HashMap<Spec, Bounds> groups = new HashMap<>();
            Bounds[] childBounds = new Bounds[count];

            for (int index = 0; index < count; index++)
            {
                View child = getChildAt(index);
                LayoutParams params = getLayoutParams(child);
                Spec spec = mHorizontal ? params.columnSpec : params.rowSpec;

                Bounds bounds = groups.get(spec);
                if (bounds == null)
                {
                    bounds = spec.getAbsoluteAlignment(mHorizontal).getBounds();
                    groups.put(spec, bounds);
                }

                int size = getMeasurementIncludingMargin(child, mHorizontal) + (spec.mWeight == 0 ? 0 : deltas[index]);
                bounds.include(child, spec, mHorizontal, size);
                childBounds[index] = bounds;
            }

            mChildBounds = childBounds;
        }

        private ArrayList<int[]> createArcs(boolean withBackward)
        {
            int count = getSafeCount();
            ArrayList<int[]> arcs = new ArrayList<>();

            if (mOrderPreserved)
            {
                for (int index = 0; index < count; index++)
                {
                    arcs.add(new int[] {index, index + 1, 0});
                }
            }

            HashMap<Long, int[]> spans = new HashMap<>();
            Bounds[] childBounds = getChildBounds();
            ArrayList<Bounds> seen = new ArrayList<>();

            for (int index = 0; index < childBounds.length; index++)
            {
                Bounds bounds = childBounds[index];
                if (seen.contains(bounds)) continue;
                seen.add(bounds);

                LayoutParams params = getLayoutParams(getChildAt(index));
                Interval span = (mHorizontal ? params.columnSpec : params.rowSpec).mSpan;
                long key = ((long)span.min << 32) | (span.max & 0xffffffffL);

                int[] link = spans.get(key);
                if (link == null)
                {
                    link = new int[] {span.min, span.max, 0, 0};
                    spans.put(key, link);
                }

                link[2] = Math.max(link[2], bounds.size(true));
                link[3] = Math.max(link[3], bounds.size(false));
            }

            for (int[] link : spans.values())
            {
                arcs.add(new int[] {link[0], link[1], link[2]});
                if (withBackward && link[3] < MAX_SIZE) arcs.add(new int[] {link[1], link[0], -link[3]});
            }

            arcs.add(new int[] {0, count, mParentMin});
            if (withBackward) arcs.add(new int[] {count, 0, -mParentMax});

            return arcs;
        }

        private boolean solve(ArrayList<int[]> arcs, int[] locations)
        {
            Arrays.fill(locations, 0);

            int nodes = locations.length;

            for (int iteration = 0; iteration < nodes; iteration++)
            {
                boolean changed = false;

                for (int[] arc : arcs)
                {
                    int from = arc[0];
                    int to = arc[1];
                    if (from < 0 || to < 0 || from >= nodes || to >= nodes) continue;

                    int candidate = locations[from] + arc[2];

                    if (candidate > locations[to])
                    {
                        locations[to] = candidate;
                        changed = true;
                    }
                }

                if (!changed) return true;
            }

            return false;
        }

        private boolean solveAll(int[] locations)
        {
            mChildBounds = null;

            if (solve(createArcs(true), locations)) return true;

            solve(createArcs(false), locations);
            return false;
        }

        private float calculateTotalWeight()
        {
            float totalWeight = 0f;

            for (int index = 0, count = getChildCount(); index < count; index++)
            {
                View child = getChildAt(index);
                if (child.getVisibility() == View.GONE) continue;

                LayoutParams params = getLayoutParams(child);
                Spec spec = mHorizontal ? params.columnSpec : params.rowSpec;
                totalWeight += spec.mWeight;
            }

            return totalWeight;
        }

        private boolean hasWeights()
        {
            for (int index = 0, count = getChildCount(); index < count; index++)
            {
                View child = getChildAt(index);
                if (child.getVisibility() == View.GONE) continue;

                LayoutParams params = getLayoutParams(child);
                Spec spec = mHorizontal ? params.columnSpec : params.rowSpec;
                if (spec.mWeight != 0) return true;
            }

            return false;
        }

        private void shareOutDelta(int totalDelta, float totalWeight)
        {
            int[] deltas = getDeltas();
            Arrays.fill(deltas, 0);

            for (int index = 0, count = getChildCount(); index < count; index++)
            {
                View child = getChildAt(index);
                if (child.getVisibility() == View.GONE) continue;

                LayoutParams params = getLayoutParams(child);
                Spec spec = mHorizontal ? params.columnSpec : params.rowSpec;
                float weight = spec.mWeight;
                if (weight == 0) continue;

                int delta = Math.round(weight * totalDelta / totalWeight);
                deltas[index] = delta;
                totalDelta -= delta;
                totalWeight -= weight;
            }
        }

        private void computeLocations(int[] locations)
        {
            Arrays.fill(getDeltas(), 0);

            if (!hasWeights())
            {
                solveAll(locations);
            }
            else
            {
                solveAll(locations);

                int deltaMax = mParentMin * getChildCount() + 1;

                if (deltaMax >= 2)
                {
                    int deltaMin = 0;
                    float totalWeight = calculateTotalWeight();
                    int validDelta = -1;
                    boolean validSolution = true;

                    while (deltaMin < deltaMax)
                    {
                        int delta = (int)(((long)deltaMin + deltaMax) / 2);
                        shareOutDelta(delta, totalWeight);
                        mChildBounds = null;
                        validSolution = solve(createArcs(true), locations);

                        if (validSolution)
                        {
                            validDelta = delta;
                            deltaMin = delta + 1;
                        }
                        else
                        {
                            deltaMax = delta;
                        }
                    }

                    if (validDelta > 0 && !validSolution)
                    {
                        shareOutDelta(validDelta, totalWeight);
                        solveAll(locations);
                    }
                    else if (validDelta <= 0)
                    {
                        Arrays.fill(getDeltas(), 0);
                        solveAll(locations);
                    }
                }
            }

            int origin = locations[0];
            if (origin != 0)
            {
                for (int index = 0; index < locations.length; index++)
                {
                    locations[index] -= origin;
                }
            }

            mChildBounds = null;
            getChildBounds();
        }

        int[] getLocations()
        {
            if (mLocations == null)
            {
                int[] locations = new int[getSafeCount() + 1];
                computeLocations(locations);
                mLocations = locations;
            }

            return mLocations;
        }

        private int size(int minimum, int maximum)
        {
            mParentMin = minimum;
            mParentMax = maximum;
            mLocations = null;

            return getLocations()[getSafeCount()];
        }

        int getMeasure(int measureSpec)
        {
            int mode = MeasureSpec.getMode(measureSpec);
            int specSize = MeasureSpec.getSize(measureSpec);

            if (mode == MeasureSpec.UNSPECIFIED) return size(0, MAX_SIZE);
            if (mode == MeasureSpec.EXACTLY) return size(specSize, specSize);

            return size(0, specSize);
        }

        void layout(int size)
        {
            mParentMin = size;
            mParentMax = size;
            mLocations = null;
            getLocations();
        }
    }

    static class Bounds
    {
        int mBefore;
        int mAfter;
        int mFlexibility;

        Bounds()
        {
            reset();
        }

        void reset()
        {
            mBefore = Integer.MIN_VALUE;
            mAfter = Integer.MIN_VALUE;
            mFlexibility = CAN_STRETCH;
        }

        void include(int before, int after)
        {
            mBefore = Math.max(mBefore, before);
            mAfter = Math.max(mAfter, after);
        }

        int size(boolean minimum)
        {
            if (!minimum && (mFlexibility & CAN_STRETCH) != 0) return MAX_SIZE;

            return mBefore + mAfter;
        }

        int getOffset(View child, Alignment alignment, int size)
        {
            return mBefore - alignment.getAlignmentValue(child, size);
        }

        final void include(View child, Spec spec, boolean horizontal, int size)
        {
            mFlexibility &= spec.getFlexibility();

            Alignment alignment = spec.getAbsoluteAlignment(horizontal);
            int before = alignment.getAlignmentValue(child, size);
            include(before, size - before);
        }
    }

    static final class Interval
    {
        final int min;
        final int max;

        Interval(int min, int max)
        {
            this.min = min;
            this.max = max;
        }

        int size()
        {
            return max - min;
        }

        @Override
        public boolean equals(Object that)
        {
            if (this == that) return true;
            if (!(that instanceof Interval)) return false;

            Interval interval = (Interval)that;
            return max == interval.max && min == interval.min;
        }

        @Override
        public int hashCode()
        {
            return 31 * min + max;
        }

        @Override
        public String toString()
        {
            return "[" + min + ", " + max + "]";
        }
    }

    public static class Spec
    {
        static final Spec UNDEFINED = spec(GridLayout.UNDEFINED);
        static final float DEFAULT_WEIGHT = 0;

        final boolean mStartDefined;
        final Interval mSpan;
        final Alignment mAlignment;
        final float mWeight;

        Spec()
        {
            this(false, new Interval(GridLayout.UNDEFINED, GridLayout.UNDEFINED + 1), UNDEFINED_ALIGNMENT, DEFAULT_WEIGHT);
        }

        private Spec(boolean startDefined, Interval span, Alignment alignment, float weight)
        {
            mStartDefined = startDefined;
            mSpan = span;
            mAlignment = alignment;
            mWeight = weight;
        }

        Spec(boolean startDefined, int start, int size, Alignment alignment, float weight)
        {
            this(startDefined, new Interval(start, start + size), alignment, weight);
        }

        Alignment getAbsoluteAlignment(boolean horizontal)
        {
            if (mAlignment != UNDEFINED_ALIGNMENT) return mAlignment;
            if (mWeight == 0f) return horizontal ? START : BASELINE;

            return FILL;
        }

        final Spec copyWriteSpan(Interval span)
        {
            return new Spec(mStartDefined, span, mAlignment, mWeight);
        }

        final Spec copyWriteAlignment(Alignment alignment)
        {
            return new Spec(mStartDefined, mSpan, alignment, mWeight);
        }

        final int getFlexibility()
        {
            return (mAlignment == UNDEFINED_ALIGNMENT && mWeight == 0) ? INFLEXIBLE : CAN_STRETCH;
        }

        @Override
        public boolean equals(Object that)
        {
            if (this == that) return true;
            if (that == null || getClass() != that.getClass()) return false;

            Spec spec = (Spec)that;
            return mAlignment.equals(spec.mAlignment) && mSpan.equals(spec.mSpan);
        }

        @Override
        public int hashCode()
        {
            int result = mSpan.hashCode();
            result = 31 * result + mAlignment.hashCode();
            return result;
        }
    }

    public static Spec spec(int start, int size, Alignment alignment, float weight)
    {
        return new Spec(start != UNDEFINED, start, size, alignment, weight);
    }

    public static Spec spec(int start, Alignment alignment, float weight)
    {
        return spec(start, 1, alignment, weight);
    }

    public static Spec spec(int start, int size, float weight)
    {
        return spec(start, size, UNDEFINED_ALIGNMENT, weight);
    }

    public static Spec spec(int start, float weight)
    {
        return spec(start, 1, weight);
    }

    public static Spec spec(int start, int size, Alignment alignment)
    {
        return spec(start, size, alignment, Spec.DEFAULT_WEIGHT);
    }

    public static Spec spec(int start, Alignment alignment)
    {
        return spec(start, 1, alignment);
    }

    public static Spec spec(int start, int size)
    {
        return spec(start, size, UNDEFINED_ALIGNMENT);
    }

    public static Spec spec(int start)
    {
        return spec(start, 1);
    }

    public abstract static class Alignment
    {
        Alignment()
        {
        }

        abstract int getGravityOffset(View view, int cellDelta);

        abstract int getAlignmentValue(View view, int viewSize);

        int getSizeInCell(View view, int viewSize, int cellSize)
        {
            return viewSize;
        }

        Bounds getBounds()
        {
            return new Bounds();
        }
    }

    static final Alignment UNDEFINED_ALIGNMENT = new Alignment()
    {
        @Override
        int getGravityOffset(View view, int cellDelta)
        {
            return UNDEFINED;
        }

        @Override
        int getAlignmentValue(View view, int viewSize)
        {
            return UNDEFINED;
        }
    };

    private static final Alignment LEADING = new Alignment()
    {
        @Override
        int getGravityOffset(View view, int cellDelta)
        {
            return 0;
        }

        @Override
        int getAlignmentValue(View view, int viewSize)
        {
            return 0;
        }
    };

    private static final Alignment TRAILING = new Alignment()
    {
        @Override
        int getGravityOffset(View view, int cellDelta)
        {
            return cellDelta;
        }

        @Override
        int getAlignmentValue(View view, int viewSize)
        {
            return viewSize;
        }
    };

    public static final Alignment TOP = LEADING;
    public static final Alignment BOTTOM = TRAILING;
    public static final Alignment START = LEADING;
    public static final Alignment END = TRAILING;

    private static Alignment createSwitchingAlignment(final Alignment leftToRight, final Alignment rightToLeft)
    {
        return new Alignment()
        {
            @Override
            int getGravityOffset(View view, int cellDelta)
            {
                return (!isViewRtl(view) ? leftToRight : rightToLeft).getGravityOffset(view, cellDelta);
            }

            @Override
            int getAlignmentValue(View view, int viewSize)
            {
                return (!isViewRtl(view) ? leftToRight : rightToLeft).getAlignmentValue(view, viewSize);
            }
        };
    }

    public static final Alignment LEFT = createSwitchingAlignment(START, END);
    public static final Alignment RIGHT = createSwitchingAlignment(END, START);

    public static final Alignment CENTER = new Alignment()
    {
        @Override
        int getGravityOffset(View view, int cellDelta)
        {
            return cellDelta >> 1;
        }

        @Override
        int getAlignmentValue(View view, int viewSize)
        {
            return viewSize >> 1;
        }
    };

    public static final Alignment BASELINE = new Alignment()
    {
        @Override
        int getGravityOffset(View view, int cellDelta)
        {
            return 0;
        }

        @Override
        int getAlignmentValue(View view, int viewSize)
        {
            if (view.getVisibility() == GONE) return 0;

            int baseline = view.getBaseline();
            return baseline == -1 ? UNDEFINED : baseline;
        }

        @Override
        Bounds getBounds()
        {
            return new Bounds()
            {
                private int mSize;

                @Override
                void reset()
                {
                    super.reset();
                    mSize = Integer.MIN_VALUE;
                }

                @Override
                void include(int before, int after)
                {
                    super.include(before, after);
                    mSize = Math.max(mSize, before + after);
                }

                @Override
                int size(boolean minimum)
                {
                    return Math.max(super.size(minimum), mSize);
                }

                @Override
                int getOffset(View child, Alignment alignment, int size)
                {
                    return Math.max(0, super.getOffset(child, alignment, size));
                }
            };
        }
    };

    public static final Alignment FILL = new Alignment()
    {
        @Override
        int getGravityOffset(View view, int cellDelta)
        {
            return 0;
        }

        @Override
        int getAlignmentValue(View view, int viewSize)
        {
            return UNDEFINED;
        }

        @Override
        int getSizeInCell(View view, int viewSize, int cellSize)
        {
            return cellSize;
        }
    };

    static Alignment getAlignment(int gravity, boolean horizontal)
    {
        if (horizontal)
        {
            int relative = gravity & Gravity.RELATIVE_LAYOUT_DIRECTION;
            int flags = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;

            if (flags == (Gravity.AXIS_SPECIFIED | Gravity.AXIS_PULL_BEFORE)) return relative != 0 ? START : LEFT;
            if (flags == (Gravity.AXIS_SPECIFIED | Gravity.AXIS_PULL_AFTER)) return relative != 0 ? END : RIGHT;
            if (flags == (Gravity.AXIS_SPECIFIED | Gravity.AXIS_PULL_BEFORE | Gravity.AXIS_PULL_AFTER)) return FILL;
            if (flags == Gravity.AXIS_SPECIFIED) return CENTER;

            return UNDEFINED_ALIGNMENT;
        }

        int flags = (gravity & Gravity.VERTICAL_GRAVITY_MASK) >> Gravity.AXIS_Y_SHIFT;

        if (flags == (Gravity.AXIS_SPECIFIED | Gravity.AXIS_PULL_BEFORE)) return TOP;
        if (flags == (Gravity.AXIS_SPECIFIED | Gravity.AXIS_PULL_AFTER)) return BOTTOM;
        if (flags == (Gravity.AXIS_SPECIFIED | Gravity.AXIS_PULL_BEFORE | Gravity.AXIS_PULL_AFTER)) return FILL;
        if (flags == Gravity.AXIS_SPECIFIED) return CENTER;

        return UNDEFINED_ALIGNMENT;
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams
    {
        private static final int DEFAULT_WIDTH = WRAP_CONTENT;
        private static final int DEFAULT_HEIGHT = WRAP_CONTENT;
        private static final int DEFAULT_MARGIN = UNDEFINED;
        private static final int DEFAULT_ROW = UNDEFINED;
        private static final int DEFAULT_COLUMN = UNDEFINED;
        private static final int DEFAULT_SPAN_SIZE = 1;

        private static StyleableSet sStyleable;

        public Spec rowSpec = Spec.UNDEFINED;
        public Spec columnSpec = Spec.UNDEFINED;

        private LayoutParams(int width, int height, int left, int top, int right, int bottom, Spec rowSpec, Spec columnSpec)
        {
            super(width, height);
            setMargins(left, top, right, bottom);
            this.rowSpec = rowSpec;
            this.columnSpec = columnSpec;
        }

        public LayoutParams(Spec rowSpec, Spec columnSpec)
        {
            this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, rowSpec, columnSpec);
        }

        public LayoutParams()
        {
            this(Spec.UNDEFINED, Spec.UNDEFINED);
        }

        public LayoutParams(ViewGroup.LayoutParams params)
        {
            super(params);
        }

        public LayoutParams(MarginLayoutParams params)
        {
            super(params);
        }

        public LayoutParams(LayoutParams source)
        {
            super(source);

            this.rowSpec = source.rowSpec;
            this.columnSpec = source.columnSpec;
        }

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);

            if (attributes == null) return;

            if (sStyleable == null)
            {
                sStyleable = StyleableSet.of(context.getResources(), "layout_margin", "layout_marginLeft", "layout_marginTop",
                                             "layout_marginRight", "layout_marginBottom", "layout_row", "layout_rowSpan",
                                             "layout_rowWeight", "layout_column", "layout_columnSpan", "layout_columnWeight",
                                             "layout_gravity");
            }
            if (sStyleable.isEmpty()) return;

            TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), 0, 0);

            int margin = dimension(array, "layout_margin", DEFAULT_MARGIN);

            leftMargin = dimension(array, "layout_marginLeft", margin);
            topMargin = dimension(array, "layout_marginTop", margin);
            rightMargin = dimension(array, "layout_marginRight", margin);
            bottomMargin = dimension(array, "layout_marginBottom", margin);

            int gravity = integer(array, "layout_gravity", Gravity.NO_GRAVITY);

            int column = integer(array, "layout_column", DEFAULT_COLUMN);
            int columnSpan = integer(array, "layout_columnSpan", DEFAULT_SPAN_SIZE);
            float columnWeight = decimal(array, "layout_columnWeight", Spec.DEFAULT_WEIGHT);
            this.columnSpec = spec(column, columnSpan, getAlignment(gravity, true), columnWeight);

            int row = integer(array, "layout_row", DEFAULT_ROW);
            int rowSpan = integer(array, "layout_rowSpan", DEFAULT_SPAN_SIZE);
            float rowWeight = decimal(array, "layout_rowWeight", Spec.DEFAULT_WEIGHT);
            this.rowSpec = spec(row, rowSpan, getAlignment(gravity, false), rowWeight);

            array.recycle();
        }

        private static int dimension(TypedArray array, String name, int fallback)
        {
            int index = sStyleable.indexOf(name);
            if (index < 0 || !array.hasValue(index)) return fallback;

            return array.getDimensionPixelSize(index, fallback);
        }

        private static int integer(TypedArray array, String name, int fallback)
        {
            int index = sStyleable.indexOf(name);
            if (index < 0) return fallback;

            return array.getInt(index, fallback);
        }

        private static float decimal(TypedArray array, String name, float fallback)
        {
            int index = sStyleable.indexOf(name);
            if (index < 0) return fallback;

            return array.getFloat(index, fallback);
        }

        public void setGravity(int gravity)
        {
            rowSpec = rowSpec.copyWriteAlignment(getAlignment(gravity, false));
            columnSpec = columnSpec.copyWriteAlignment(getAlignment(gravity, true));
        }

        @Override
        protected void setBaseAttributes(TypedArray attributes, int widthAttr, int heightAttr)
        {
            this.width = attributes.getLayoutDimension(widthAttr, DEFAULT_WIDTH);
            this.height = attributes.getLayoutDimension(heightAttr, DEFAULT_HEIGHT);
        }

        final void setRowSpecSpan(Interval span)
        {
            rowSpec = rowSpec.copyWriteSpan(span);
        }

        final void setColumnSpecSpan(Interval span)
        {
            columnSpec = columnSpec.copyWriteSpan(span);
        }

        @Override
        public boolean equals(Object that)
        {
            if (this == that) return true;
            if (that == null || getClass() != that.getClass()) return false;

            LayoutParams params = (LayoutParams)that;
            return columnSpec.equals(params.columnSpec) && rowSpec.equals(params.rowSpec);
        }

        @Override
        public int hashCode()
        {
            int result = rowSpec.hashCode();
            result = 31 * result + columnSpec.hashCode();
            return result;
        }
    }
}
