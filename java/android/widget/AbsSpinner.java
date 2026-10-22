package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.AbsSavedState;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillValue;

public abstract class AbsSpinner extends AdapterView<SpinnerAdapter>
{
    private static final int AUTOFILL_TYPE_LIST = 3;

    private static StyleableSet sStyleable;

    SpinnerAdapter mAdapter;
    int mHeightMeasureSpec;
    int mWidthMeasureSpec;
    final Rect mSpinnerPadding = new Rect();
    final SparseArray<View> mRecycledViews = new SparseArray<View>();
    private DataSetObserver mDataSetObserver;

    public AbsSpinner(Context context)
    {
        this(context, null);
    }

    public AbsSpinner(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public AbsSpinner(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public AbsSpinner(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        setFocusable(true);
        setWillNotDraw(false);

        if (attributes != null) readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "entries");
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int entriesIndex = sStyleable.indexOf("entries");
        CharSequence[] entries = entriesIndex >= 0 && array.hasValue(entriesIndex) ? array.getTextArray(entriesIndex) : null;
        array.recycle();

        if (entries == null) return;

        android.content.res.Resources resources = context.getResources();
        int itemLayout = resources.getIdentifier("simple_spinner_item", "layout", "android");
        int dropDownLayout = resources.getIdentifier("simple_spinner_dropdown_item", "layout", "android");

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context, itemLayout, entries);
        adapter.setDropDownViewResource(dropDownLayout);
        setAdapter(adapter);
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter)
    {
        if (mAdapter != null && mDataSetObserver != null)
        {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            resetList();
        }

        mAdapter = adapter;

        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;

        if (mAdapter != null)
        {
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
            checkFocus();

            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            int position = mItemCount > 0 ? 0 : INVALID_POSITION;
            setSelectedPositionInt(position);
            setNextSelectedPositionInt(position);

            if (mItemCount == 0) checkSelectionChanged();
        }
        else
        {
            checkFocus();
            resetList();
            checkSelectionChanged();
        }

        requestLayout();
    }

    void resetList()
    {
        mDataChanged = false;

        removeAllViewsInLayout();
        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;

        setSelectedPositionInt(INVALID_POSITION);
        setNextSelectedPositionInt(INVALID_POSITION);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        mSpinnerPadding.set(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());

        if (mDataChanged) handleDataChanged();

        int preferredHeight = 0;
        int preferredWidth = 0;
        boolean needsMeasuring = true;

        int selectedPosition = getSelectedItemPosition();
        if (selectedPosition >= 0 && mAdapter != null && selectedPosition < mAdapter.getCount())
        {
            View view = currentSelectedChild(selectedPosition);
            if (view == null) view = mRecycledViews.get(selectedPosition);

            if (view == null)
            {
                view = mAdapter.getView(selectedPosition, null, this);
                if (view != null) mRecycledViews.put(selectedPosition, view);
            }

            if (view != null)
            {
                if (view.getLayoutParams() == null)
                {
                    mBlockLayoutRequests = true;
                    view.setLayoutParams(generateDefaultLayoutParams());
                    mBlockLayoutRequests = false;
                }

                measureChild(view, widthMeasureSpec, heightMeasureSpec);

                preferredHeight = view.getMeasuredHeight() + mSpinnerPadding.top + mSpinnerPadding.bottom;
                preferredWidth = view.getMeasuredWidth() + mSpinnerPadding.left + mSpinnerPadding.right;
                needsMeasuring = false;
            }
        }

        if (needsMeasuring)
        {
            preferredHeight = mSpinnerPadding.top + mSpinnerPadding.bottom;
            if (widthMode == MeasureSpec.UNSPECIFIED) preferredWidth = mSpinnerPadding.left + mSpinnerPadding.right;
        }

        preferredHeight = Math.max(preferredHeight, getSuggestedMinimumHeight());
        preferredWidth = Math.max(preferredWidth, getSuggestedMinimumWidth());

        int heightSize = resolveSizeAndState(preferredHeight, heightMeasureSpec, 0);
        int widthSize = resolveSizeAndState(preferredWidth, widthMeasureSpec, 0);

        setMeasuredDimension(widthSize, heightSize);
        mHeightMeasureSpec = heightMeasureSpec;
        mWidthMeasureSpec = widthMeasureSpec;
    }

    private View currentSelectedChild(int position)
    {
        if (mDataChanged || getChildCount() == 0 || mFirstPosition != position) return null;
        return getChildAt(0);
    }

    void handleDataChanged()
    {
        if (mAdapter != null) mItemCount = mAdapter.getCount();

        if (mItemCount <= 0)
        {
            setSelectedPositionInt(INVALID_POSITION);
            setNextSelectedPositionInt(INVALID_POSITION);
            return;
        }

        int selected = mNextSelectedPosition;
        if (selected < 0) selected = 0;
        if (selected >= mItemCount) selected = mItemCount - 1;

        setNextSelectedPositionInt(selected);
        setSelectedPositionInt(selected);
    }

    int getChildHeight(View child)
    {
        return child.getMeasuredHeight();
    }

    int getChildWidth(View child)
    {
        return child.getMeasuredWidth();
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams()
    {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    void recycleAllViews()
    {
        int childCount = getChildCount();
        int position = mFirstPosition;

        for (int index = 0; index < childCount; index++)
        {
            View child = getChildAt(index);
            mRecycledViews.put(position + index, child);
        }
    }

    public void setSelection(int position, boolean animate)
    {
        boolean shouldAnimate = animate && mFirstPosition <= position && position <= mFirstPosition + getChildCount() - 1;
        setSelectionInt(position, shouldAnimate);
    }

    @Override
    public void setSelection(int position)
    {
        setNextSelectedPositionInt(position);
        requestLayout();
        invalidate();
    }

    void setSelectionInt(int position, boolean animate)
    {
        if (position == mOldSelectedPosition) return;

        mBlockLayoutRequests = true;
        int delta = position - mSelectedPosition;
        setNextSelectedPositionInt(position);
        layout(delta, animate);
        mBlockLayoutRequests = false;
    }

    abstract void layout(int delta, boolean animate);

    @Override
    public View getSelectedView()
    {
        if (mItemCount > 0 && mSelectedPosition >= 0) return getChildAt(mSelectedPosition - mFirstPosition);
        return null;
    }

    @Override
    public void requestLayout()
    {
        if (!mBlockLayoutRequests) super.requestLayout();
    }

    @Override
    public SpinnerAdapter getAdapter()
    {
        return mAdapter;
    }

    @Override
    public int getCount()
    {
        return mItemCount;
    }

    public int pointToPosition(int x, int y)
    {
        Rect frame = new Rect();

        for (int index = getChildCount() - 1; index >= 0; index--)
        {
            View child = getChildAt(index);
            if (child.getVisibility() != VISIBLE) continue;

            child.getHitRect(frame);
            if (frame.contains(x, y)) return index + mFirstPosition;
        }

        return INVALID_POSITION;
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container)
    {
        super.dispatchRestoreInstanceState(container);
        handleDataChanged();
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.selectedId = getSelectedItemId();
        state.position = state.selectedId >= 0 ? getSelectedItemPosition() : INVALID_POSITION;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable)
    {
        if (!(parcelable instanceof SavedState))
        {
            super.onRestoreInstanceState(parcelable);
            return;
        }

        SavedState state = (SavedState)parcelable;
        super.onRestoreInstanceState(state.getSuperState());
        if (state.selectedId < 0 || state.position < 0) return;

        mDataChanged = true;
        setNextSelectedPositionInt(state.position);
        requestLayout();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return AbsSpinner.class.getName();
    }

    @Override
    public void autofill(AutofillValue value)
    {
        if (!isEnabled() || value == null) return;

        if (!value.isList())
        {
            android.util.Log.w("AbsSpinner", value + " could not be autofilled into " + this);
            return;
        }

        setSelection(value.getListValue());
    }

    @Override
    public int getAutofillType()
    {
        return isEnabled() ? AUTOFILL_TYPE_LIST : 0;
    }

    @Override
    public AutofillValue getAutofillValue()
    {
        return isEnabled() ? AutofillValue.forList(getSelectedItemPosition()) : null;
    }

    static class SavedState extends AbsSavedState
    {
        long selectedId;
        int position;

        SavedState(Parcelable superState)
        {
            super(superState == null ? AbsSavedState.EMPTY_STATE : superState);
        }

        private SavedState(Parcel source)
        {
            super(source);
            selectedId = source.readLong();
            position = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeLong(selectedId);
            out.writeInt(position);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
        {
            @Override
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }
}
