package android.widget;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStructure;

public abstract class AdapterView<T extends Adapter> extends ViewGroup
{
    public static final int ITEM_VIEW_TYPE_IGNORE = -1;
    public static final int ITEM_VIEW_TYPE_HEADER_OR_FOOTER = -2;
    public static final int INVALID_POSITION = -1;
    public static final long INVALID_ROW_ID = Long.MIN_VALUE;

    private static final int SOUND_EFFECT_CLICK = 0;

    int mFirstPosition;
    int mItemCount;
    int mOldItemCount;
    int mSelectedPosition = INVALID_POSITION;
    long mSelectedRowId = INVALID_ROW_ID;
    int mNextSelectedPosition = INVALID_POSITION;
    long mNextSelectedRowId = INVALID_ROW_ID;
    int mOldSelectedPosition = INVALID_POSITION;
    long mOldSelectedRowId = INVALID_ROW_ID;
    boolean mDataChanged;
    boolean mInLayout;
    boolean mBlockLayoutRequests;
    int mLayoutHeight;

    OnItemClickListener mOnItemClickListener;
    OnItemLongClickListener mOnItemLongClickListener;
    OnItemSelectedListener mOnItemSelectedListener;

    private View mEmptyView;
    private boolean mDesiredFocusableState = true;
    private boolean mDesiredFocusableInTouchModeState;
    private Runnable mSelectionNotifier;

    public AdapterView(Context context)
    {
        this(context, null);
    }

    public AdapterView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public AdapterView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public AdapterView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    public void setOnItemClickListener(OnItemClickListener listener)
    {
        mOnItemClickListener = listener;
    }

    public final OnItemClickListener getOnItemClickListener()
    {
        return mOnItemClickListener;
    }

    public boolean performItemClick(View view, int position, long id)
    {
        if (mOnItemClickListener == null) return false;

        playSoundEffect(SOUND_EFFECT_CLICK);
        mOnItemClickListener.onItemClick(this, view, position, id);
        return true;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener)
    {
        if (!isLongClickable()) setLongClickable(true);
        mOnItemLongClickListener = listener;
    }

    public final OnItemLongClickListener getOnItemLongClickListener()
    {
        return mOnItemLongClickListener;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener)
    {
        mOnItemSelectedListener = listener;
    }

    public final OnItemSelectedListener getOnItemSelectedListener()
    {
        return mOnItemSelectedListener;
    }

    public abstract T getAdapter();

    public abstract void setAdapter(T adapter);

    @Override
    public void addView(View child)
    {
        throw new UnsupportedOperationException("addView(View) is not supported in AdapterView");
    }

    @Override
    public void addView(View child, int index)
    {
        throw new UnsupportedOperationException("addView(View, int) is not supported in AdapterView");
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params)
    {
        throw new UnsupportedOperationException("addView(View, LayoutParams) is not supported in AdapterView");
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        throw new UnsupportedOperationException("addView(View, int, LayoutParams) is not supported in AdapterView");
    }

    @Override
    public void removeView(View child)
    {
        throw new UnsupportedOperationException("removeView(View) is not supported in AdapterView");
    }

    @Override
    public void removeViewAt(int index)
    {
        throw new UnsupportedOperationException("removeViewAt(int) is not supported in AdapterView");
    }

    @Override
    public void removeAllViews()
    {
        throw new UnsupportedOperationException("removeAllViews() is not supported in AdapterView");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        mLayoutHeight = getHeight();
    }

    public int getSelectedItemPosition()
    {
        return mNextSelectedPosition;
    }

    public long getSelectedItemId()
    {
        return mNextSelectedRowId;
    }

    public abstract View getSelectedView();

    public Object getSelectedItem()
    {
        T adapter = getAdapter();
        int selection = getSelectedItemPosition();

        if (adapter != null && adapter.getCount() > 0 && selection >= 0) return adapter.getItem(selection);
        return null;
    }

    public int getCount()
    {
        return mItemCount;
    }

    public int getPositionForView(View view)
    {
        View listItem = view;

        try
        {
            View parent;
            while ((parent = (View)listItem.getParent()) != null && parent != this) listItem = parent;
            if (parent == null) return INVALID_POSITION;
        }
        catch (ClassCastException notAView)
        {
            return INVALID_POSITION;
        }

        int index = indexOfChild(listItem);
        return index < 0 ? INVALID_POSITION : mFirstPosition + index;
    }

    public int getFirstVisiblePosition()
    {
        return mFirstPosition;
    }

    public int getLastVisiblePosition()
    {
        return mFirstPosition + getChildCount() - 1;
    }

    public abstract void setSelection(int position);

    public void setEmptyView(View emptyView)
    {
        mEmptyView = emptyView;

        T adapter = getAdapter();
        updateEmptyStatus(adapter == null || adapter.isEmpty());
    }

    public View getEmptyView()
    {
        return mEmptyView;
    }

    boolean isInFilterModeInternal()
    {
        return false;
    }

    @Override
    public void setFocusable(int focusable)
    {
        T adapter = getAdapter();
        boolean empty = adapter == null || adapter.getCount() == 0;

        mDesiredFocusableState = focusable != 0;
        super.setFocusable(focusable != 0 && (!empty || isInFilterModeInternal()));
    }

    @Override
    public void setFocusable(boolean focusable)
    {
        setFocusable(focusable ? 1 : 0);
    }

    @Override
    public void setFocusableInTouchMode(boolean focusable)
    {
        T adapter = getAdapter();
        boolean empty = adapter == null || adapter.getCount() == 0;

        mDesiredFocusableInTouchModeState = focusable;
        super.setFocusableInTouchMode(focusable && (!empty || isInFilterModeInternal()));
    }

    void checkFocus()
    {
        T adapter = getAdapter();
        boolean empty = adapter == null || adapter.getCount() == 0;
        boolean focusable = !empty || isInFilterModeInternal();

        super.setFocusableInTouchMode(focusable && mDesiredFocusableInTouchModeState);
        super.setFocusable(focusable && mDesiredFocusableState);

        if (mEmptyView != null) updateEmptyStatus(adapter == null || adapter.isEmpty());
    }

    void updateEmptyStatus(boolean empty)
    {
        if (isInFilterModeInternal()) empty = false;

        if (empty)
        {
            if (mEmptyView != null)
            {
                mEmptyView.setVisibility(View.VISIBLE);
                setVisibility(View.GONE);
            }
            else
            {
                setVisibility(View.VISIBLE);
            }

            if (mDataChanged) onLayout(false, getLeft(), getTop(), getRight(), getBottom());
            return;
        }

        if (mEmptyView != null) mEmptyView.setVisibility(View.GONE);
        setVisibility(View.VISIBLE);
    }

    public Object getItemAtPosition(int position)
    {
        T adapter = getAdapter();
        return adapter == null || position < 0 ? null : adapter.getItem(position);
    }

    public long getItemIdAtPosition(int position)
    {
        T adapter = getAdapter();
        return adapter == null || position < 0 ? INVALID_ROW_ID : adapter.getItemId(position);
    }

    @Override
    public void setOnClickListener(View.OnClickListener listener)
    {
        throw new RuntimeException("Don't call setOnClickListener for an AdapterView. You probably want setOnItemClickListener instead");
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container)
    {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container)
    {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        if (mSelectionNotifier != null) removeCallbacks(mSelectionNotifier);
    }

    public CharSequence getAccessibilityClassName()
    {
        return AdapterView.class.getName();
    }

    protected boolean canAnimate()
    {
        return mItemCount > 0;
    }

    @Override
    public void onProvideAutofillStructure(ViewStructure structure, int flags)
    {
        super.onProvideAutofillStructure(structure, flags);

        T adapter = getAdapter();
        if (adapter == null) return;

        CharSequence[] options = adapter.getAutofillOptions();
        if (options != null) structure.setAutofillOptions(options);
    }

    void selectionChanged()
    {
        if (mOnItemSelectedListener == null) return;

        if (!mInLayout && !mBlockLayoutRequests)
        {
            fireOnSelected();
            return;
        }

        if (mSelectionNotifier == null)
        {
            mSelectionNotifier = new Runnable()
            {
                @Override
                public void run()
                {
                    if (mDataChanged && getAdapter() != null)
                    {
                        post(this);
                        return;
                    }

                    fireOnSelected();
                }
            };
        }
        else
        {
            removeCallbacks(mSelectionNotifier);
        }

        post(mSelectionNotifier);
    }

    private void fireOnSelected()
    {
        if (mOnItemSelectedListener == null) return;

        int selection = getSelectedItemPosition();
        if (selection >= 0)
        {
            View view = getSelectedView();
            T adapter = getAdapter();
            long id = adapter == null ? INVALID_ROW_ID : adapter.getItemId(selection);
            mOnItemSelectedListener.onItemSelected(this, view, selection, id);
            return;
        }

        mOnItemSelectedListener.onNothingSelected(this);
    }

    void checkSelectionChanged()
    {
        if (mSelectedPosition == mOldSelectedPosition && mSelectedRowId == mOldSelectedRowId) return;

        selectionChanged();
        mOldSelectedPosition = mSelectedPosition;
        mOldSelectedRowId = mSelectedRowId;
    }

    void setSelectedPositionInt(int position)
    {
        mSelectedPosition = position;
        mSelectedRowId = getItemIdAtPosition(position);
    }

    void setNextSelectedPositionInt(int position)
    {
        mNextSelectedPosition = position;
        mNextSelectedRowId = getItemIdAtPosition(position);
    }

    class AdapterDataSetObserver extends DataSetObserver
    {
        @Override
        public void onChanged()
        {
            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = getAdapter() == null ? 0 : getAdapter().getCount();

            checkFocus();
            requestLayout();
        }

        @Override
        public void onInvalidated()
        {
            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = 0;

            mSelectedPosition = INVALID_POSITION;
            mSelectedRowId = INVALID_ROW_ID;
            mNextSelectedPosition = INVALID_POSITION;
            mNextSelectedRowId = INVALID_ROW_ID;

            checkFocus();
            requestLayout();
        }
    }

    public interface OnItemClickListener
    {
        void onItemClick(AdapterView<?> parent, View view, int position, long id);
    }

    public interface OnItemLongClickListener
    {
        boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id);
    }

    public interface OnItemSelectedListener
    {
        void onItemSelected(AdapterView<?> parent, View view, int position, long id);

        void onNothingSelected(AdapterView<?> parent);
    }

    public static class AdapterContextMenuInfo implements ContextMenu.ContextMenuInfo
    {
        public View targetView;
        public int position;
        public long id;

        public AdapterContextMenuInfo(View targetView, int position, long id)
        {
            this.targetView = targetView;
            this.position = position;
            this.id = id;
        }
    }
}
