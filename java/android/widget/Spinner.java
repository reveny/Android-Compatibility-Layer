package android.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

public class Spinner extends AbsSpinner implements DialogInterface.OnClickListener
{
    public static final int MODE_DIALOG = 0;
    public static final int MODE_DROPDOWN = 1;

    private static final int MODE_THEME = -1;
    private static final int MAX_ITEMS_MEASURED = 15;

    private static StyleableSet sStyleable;

    private final Context mPopupContext;
    private SpinnerPopup mPopup;
    private SpinnerAdapter mTempAdapter;
    private int mDropDownWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
    private int mGravity = Gravity.CENTER;
    private boolean mDisableChildrenWhenDisabled;
    private final Rect mTempRect = new Rect();

    public Spinner(Context context)
    {
        this(context, null);
    }

    public Spinner(Context context, int mode)
    {
        this(context, null, 0, 0, mode);
    }

    public Spinner(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public Spinner(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0, MODE_THEME);
    }

    public Spinner(Context context, AttributeSet attributes, int defStyleAttr, int mode)
    {
        this(context, attributes, defStyleAttr, 0, mode);
    }

    public Spinner(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes, int mode)
    {
        this(context, attributes, defStyleAttr, defStyleRes, mode, null);
    }

    public Spinner(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes, int mode, Resources.Theme popupTheme)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mPopupContext = popupTheme != null ? new ContextThemeWrapper(context, popupTheme) : context;
        setClickable(true);

        int resolvedMode = mode;
        CharSequence prompt = null;
        Drawable popupBackground = null;
        int verticalOffset = 0;
        int horizontalOffset = 0;
        boolean verticalOffsetSet = false;

        if (attributes != null)
        {
            if (sStyleable == null)
            {
                sStyleable = StyleableSet.of(context.getResources(), "spinnerMode", "prompt", "dropDownWidth", "dropDownVerticalOffset",
                                             "dropDownHorizontalOffset", "popupBackground", "gravity", "disableChildrenWhenDisabled");
            }

            if (!sStyleable.isEmpty())
            {
                TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

                int index = sStyleable.indexOf("spinnerMode");
                if (resolvedMode == MODE_THEME && index >= 0) resolvedMode = array.getInt(index, MODE_DROPDOWN);

                index = sStyleable.indexOf("prompt");
                if (index >= 0) prompt = array.getText(index);

                index = sStyleable.indexOf("dropDownWidth");
                if (index >= 0 && array.hasValue(index))
                {
                    mDropDownWidth = array.getLayoutDimension(index, ViewGroup.LayoutParams.WRAP_CONTENT);
                }

                index = sStyleable.indexOf("dropDownVerticalOffset");
                if (index >= 0 && array.hasValue(index))
                {
                    verticalOffset = array.getDimensionPixelOffset(index, 0);
                    verticalOffsetSet = true;
                }

                index = sStyleable.indexOf("dropDownHorizontalOffset");
                if (index >= 0) horizontalOffset = array.getDimensionPixelOffset(index, 0);

                index = sStyleable.indexOf("popupBackground");
                if (index >= 0 && array.hasValue(index))
                {
                    try
                    {
                        popupBackground = array.getDrawable(index);
                    }
                    catch (RuntimeException unsupported)
                    {
                        popupBackground = null;
                    }
                }

                index = sStyleable.indexOf("gravity");
                if (index >= 0) mGravity = array.getInt(index, Gravity.CENTER);

                index = sStyleable.indexOf("disableChildrenWhenDisabled");
                if (index >= 0) mDisableChildrenWhenDisabled = array.getBoolean(index, false);

                array.recycle();
            }
        }

        if (resolvedMode == MODE_THEME) resolvedMode = MODE_DROPDOWN;

        if (resolvedMode == MODE_DIALOG)
        {
            mPopup = new DialogPopup();
        }
        else
        {
            DropdownPopup popup = new DropdownPopup(mPopupContext);
            if (popupBackground != null) popup.setBackgroundDrawable(popupBackground);
            if (verticalOffsetSet) popup.setVerticalOffset(verticalOffset);
            popup.setHorizontalOffset(horizontalOffset);
            mPopup = popup;
        }

        if (prompt != null) mPopup.setPromptText(prompt);

        if (mTempAdapter != null)
        {
            setAdapter(mTempAdapter);
            mTempAdapter = null;
        }
    }

    public Context getPopupContext()
    {
        return mPopupContext;
    }

    public void setPopupBackgroundDrawable(Drawable background)
    {
        if (!(mPopup instanceof DropdownPopup))
        {
            android.util.Log.e("Spinner", "setPopupBackgroundDrawable: incompatible spinner mode; ignoring...");
            return;
        }

        mPopup.setBackgroundDrawable(background);
    }

    public void setPopupBackgroundResource(int resId)
    {
        setPopupBackgroundDrawable(getPopupContext().getDrawable(resId));
    }

    public Drawable getPopupBackground()
    {
        return mPopup.getBackground();
    }

    public void setDropDownVerticalOffset(int pixels)
    {
        mPopup.setVerticalOffset(pixels);
    }

    public int getDropDownVerticalOffset()
    {
        return mPopup.getVerticalOffset();
    }

    public void setDropDownHorizontalOffset(int pixels)
    {
        mPopup.setHorizontalOffset(pixels);
    }

    public int getDropDownHorizontalOffset()
    {
        return mPopup.getHorizontalOffset();
    }

    public void setDropDownWidth(int pixels)
    {
        if (!(mPopup instanceof DropdownPopup))
        {
            android.util.Log.e("Spinner", "Cannot set dropdown width for MODE_DIALOG, ignoring");
            return;
        }

        mDropDownWidth = pixels;
    }

    public int getDropDownWidth()
    {
        return mDropDownWidth;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);

        if (!mDisableChildrenWhenDisabled) return;

        for (int index = 0; index < getChildCount(); index++) getChildAt(index).setEnabled(enabled);
    }

    public void setGravity(int gravity)
    {
        if (mGravity == gravity) return;

        if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == 0) gravity |= Gravity.START;
        mGravity = gravity;
        requestLayout();
    }

    public int getGravity()
    {
        return mGravity;
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter)
    {
        if (mPopup == null)
        {
            mTempAdapter = adapter;
            return;
        }

        super.setAdapter(adapter);
        mRecycledViews.clear();

        if (adapter != null && adapter.getViewTypeCount() != 1)
        {
            throw new IllegalArgumentException("Spinner adapter view type count must be 1");
        }

        mPopup.setAdapter(new DropDownAdapter(adapter, mPopupContext.getTheme()));
    }

    @Override
    public int getBaseline()
    {
        View child = null;

        if (getChildCount() > 0) child = getChildAt(0);
        else if (mAdapter != null && mAdapter.getCount() > 0)
        {
            child = makeView(0, false);
            mRecycledViews.put(0, child);
        }

        if (child == null) return -1;

        int childBaseline = child.getBaseline();
        return childBaseline >= 0 ? child.getTop() + childBaseline : -1;
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        if (mPopup != null && mPopup.isShowing()) mPopup.dismiss();
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener)
    {
        throw new RuntimeException("setOnItemClickListener cannot be used with a spinner.");
    }

    void setOnItemClickListenerInt(OnItemClickListener listener)
    {
        super.setOnItemClickListener(listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mPopup != null && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST)
        {
            int measuredWidth = getMeasuredWidth();
            int contentWidth = measureContentWidth(getAdapter(), getBackground());
            int width = Math.min(Math.max(measuredWidth, contentWidth), MeasureSpec.getSize(widthMeasureSpec));
            setMeasuredDimension(width, getMeasuredHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        mInLayout = true;
        layout(0, false);
        mInLayout = false;
    }

    @Override
    void layout(int delta, boolean animate)
    {
        int childrenLeft = mSpinnerPadding.left;
        int childrenWidth = getRight() - getLeft() - mSpinnerPadding.left - mSpinnerPadding.right;

        if (mDataChanged) handleDataChanged();

        if (mItemCount == 0 || mAdapter == null)
        {
            resetList();
            return;
        }

        if (mNextSelectedPosition >= 0) setSelectedPositionInt(mNextSelectedPosition);

        recycleAllViews();
        removeAllViewsInLayout();

        mFirstPosition = mSelectedPosition;

        View selected = makeView(mSelectedPosition, true);
        int width = selected.getMeasuredWidth();
        int selectedOffset = childrenLeft;

        int horizontal = Gravity.getAbsoluteGravity(mGravity, getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (horizontal == Gravity.CENTER_HORIZONTAL) selectedOffset = childrenLeft + (childrenWidth / 2) - (width / 2);
        else if (horizontal == Gravity.RIGHT) selectedOffset = childrenLeft + childrenWidth - width;

        selected.offsetLeftAndRight(selectedOffset);

        mRecycledViews.clear();
        invalidate();
        checkSelectionChanged();

        mDataChanged = false;
        setNextSelectedPositionInt(mSelectedPosition);
    }

    private View makeView(int position, boolean addChild)
    {
        View child;

        if (!mDataChanged)
        {
            child = mRecycledViews.get(position);
            if (child != null)
            {
                mRecycledViews.remove(position);
                setUpChild(child, addChild);
                return child;
            }
        }

        child = mAdapter.getView(position, null, this);
        setUpChild(child, addChild);
        return child;
    }

    private void setUpChild(View child, boolean addChild)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) params = generateDefaultLayoutParams();

        if (addChild)
        {
            if (child.getParent() instanceof ViewGroup && child.getParent() != this) ((ViewGroup)child.getParent()).removeView(child);
            if (child.getParent() == null) addViewInLayout(child, 0, params);
        }

        child.setSelected(hasFocus());
        if (mDisableChildrenWhenDisabled) child.setEnabled(isEnabled());

        int verticalPadding = mSpinnerPadding.top + mSpinnerPadding.bottom;
        int horizontalPadding = mSpinnerPadding.left + mSpinnerPadding.right;

        int childHeightSpec = ViewGroup.getChildMeasureSpec(mHeightMeasureSpec, verticalPadding, params.height);
        int childWidthSpec = ViewGroup.getChildMeasureSpec(mWidthMeasureSpec, horizontalPadding, params.width);
        child.measure(childWidthSpec, childHeightSpec);

        int childTop = mSpinnerPadding.top + ((getMeasuredHeight() - verticalPadding - child.getMeasuredHeight()) / 2);
        int childBottom = childTop + child.getMeasuredHeight();
        child.layout(0, childTop, child.getMeasuredWidth(), childBottom);
    }

    @Override
    public boolean performClick()
    {
        boolean handled = super.performClick();

        if (!handled)
        {
            handled = true;
            if (mPopup != null && !mPopup.isShowing() && isEnabled()) mPopup.show();
        }

        return handled;
    }

    @Override
    public void onClick(DialogInterface dialog, int which)
    {
        setSelection(which);
        dialog.dismiss();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return Spinner.class.getName();
    }

    public void setPrompt(CharSequence prompt)
    {
        mPopup.setPromptText(prompt);
    }

    public void setPromptId(int promptId)
    {
        setPrompt(getContext().getText(promptId));
    }

    public CharSequence getPrompt()
    {
        return mPopup.getHintText();
    }

    int measureContentWidth(SpinnerAdapter adapter, Drawable background)
    {
        if (adapter == null) return 0;

        int width = 0;
        View itemView = null;
        int itemType = 0;
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.UNSPECIFIED);

        int start = Math.max(0, getSelectedItemPosition());
        int end = Math.min(adapter.getCount(), start + MAX_ITEMS_MEASURED);
        int count = end - start;
        start = Math.max(0, start - (MAX_ITEMS_MEASURED - count));

        for (int position = start; position < end; position++)
        {
            int positionType = adapter.getItemViewType(position);
            if (positionType != itemType)
            {
                itemType = positionType;
                itemView = null;
            }

            itemView = adapter.getView(position, itemView, this);
            if (itemView.getLayoutParams() == null) itemView.setLayoutParams(generateDefaultLayoutParams());

            itemView.measure(widthMeasureSpec, heightMeasureSpec);
            width = Math.max(width, itemView.getMeasuredWidth());
        }

        if (background != null && background.getPadding(mTempRect)) width += mTempRect.left + mTempRect.right;

        return width + getPaddingLeft() + getPaddingRight();
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        return super.onSaveInstanceState();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        super.onRestoreInstanceState(state);
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex)
    {
        if (getPointerIcon() == null && isClickable() && isEnabled()) return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND);
        return super.onResolvePointerIcon(event, pointerIndex);
    }

    private void onPopupItemChosen(View view, int position)
    {
        setSelection(position);

        if (mOnItemClickListener != null && mAdapter != null) performItemClick(view, position, mAdapter.getItemId(position));

        mPopup.dismiss();
    }

    private interface SpinnerPopup
    {
        void setAdapter(ListAdapter adapter);

        void show();

        void dismiss();

        boolean isShowing();

        void setPromptText(CharSequence hintText);

        CharSequence getHintText();

        void setBackgroundDrawable(Drawable background);

        void setVerticalOffset(int pixels);

        void setHorizontalOffset(int pixels);

        Drawable getBackground();

        int getVerticalOffset();

        int getHorizontalOffset();
    }

    private static class DropDownAdapter implements ListAdapter, SpinnerAdapter
    {
        private final SpinnerAdapter mAdapter;
        private final ListAdapter mListAdapter;

        DropDownAdapter(SpinnerAdapter adapter, Resources.Theme dropDownTheme)
        {
            mAdapter = adapter;
            mListAdapter = adapter instanceof ListAdapter ? (ListAdapter)adapter : null;

            if (dropDownTheme != null && adapter instanceof ThemedSpinnerAdapter)
            {
                ThemedSpinnerAdapter themed = (ThemedSpinnerAdapter)adapter;
                if (themed.getDropDownViewTheme() == null) themed.setDropDownViewTheme(dropDownTheme);
            }
        }

        @Override
        public int getCount()
        {
            return mAdapter == null ? 0 : mAdapter.getCount();
        }

        @Override
        public Object getItem(int position)
        {
            return mAdapter == null ? null : mAdapter.getItem(position);
        }

        @Override
        public long getItemId(int position)
        {
            return mAdapter == null ? -1 : mAdapter.getItemId(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            return getDropDownView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent)
        {
            return mAdapter == null ? null : mAdapter.getDropDownView(position, convertView, parent);
        }

        @Override
        public boolean hasStableIds()
        {
            return mAdapter != null && mAdapter.hasStableIds();
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer)
        {
            if (mAdapter != null) mAdapter.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer)
        {
            if (mAdapter != null) mAdapter.unregisterDataSetObserver(observer);
        }

        @Override
        public boolean areAllItemsEnabled()
        {
            return mListAdapter == null || mListAdapter.areAllItemsEnabled();
        }

        @Override
        public boolean isEnabled(int position)
        {
            return mListAdapter == null || mListAdapter.isEnabled(position);
        }

        @Override
        public int getItemViewType(int position)
        {
            return 0;
        }

        @Override
        public int getViewTypeCount()
        {
            return 1;
        }

        @Override
        public boolean isEmpty()
        {
            return getCount() == 0;
        }
    }

    private class DialogPopup implements SpinnerPopup
    {
        private AlertDialog mDialog;
        private ListAdapter mListAdapter;
        private CharSequence mPrompt;

        @Override
        public void dismiss()
        {
            if (mDialog == null) return;

            mDialog.dismiss();
            mDialog = null;
        }

        @Override
        public boolean isShowing()
        {
            return mDialog != null && mDialog.isShowing();
        }

        @Override
        public void setAdapter(ListAdapter adapter)
        {
            mListAdapter = adapter;
        }

        @Override
        public void setPromptText(CharSequence hintText)
        {
            mPrompt = hintText;
        }

        @Override
        public CharSequence getHintText()
        {
            return mPrompt;
        }

        @Override
        public void show()
        {
            if (mListAdapter == null) return;

            ListView list = new ListView(getPopupContext());
            list.setAdapter(mListAdapter);
            list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

            int selected = getSelectedItemPosition();
            if (selected >= 0)
            {
                list.setItemChecked(selected, true);
                list.setSelection(selected);
            }

            list.setOnItemClickListener(new OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    onPopupItemChosen(view, position);
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(getPopupContext());
            if (mPrompt != null) builder.setTitle(mPrompt);
            builder.setView(list);

            mDialog = builder.create();
            mDialog.show();
        }

        @Override
        public void setBackgroundDrawable(Drawable background)
        {
            android.util.Log.e("Spinner", "Cannot set popup background for MODE_DIALOG, ignoring");
        }

        @Override
        public void setVerticalOffset(int pixels)
        {
            android.util.Log.e("Spinner", "Cannot set vertical offset for MODE_DIALOG, ignoring");
        }

        @Override
        public void setHorizontalOffset(int pixels)
        {
            android.util.Log.e("Spinner", "Cannot set horizontal offset for MODE_DIALOG, ignoring");
        }

        @Override
        public Drawable getBackground()
        {
            return null;
        }

        @Override
        public int getVerticalOffset()
        {
            return 0;
        }

        @Override
        public int getHorizontalOffset()
        {
            return 0;
        }
    }

    private class DropdownPopup extends ListPopupWindow implements SpinnerPopup
    {
        private CharSequence mHintText;
        private ListAdapter mListAdapter;

        DropdownPopup(Context context)
        {
            super(context);

            setAnchorView(Spinner.this);
            setModal(true);
            setPromptPosition(POSITION_PROMPT_ABOVE);
            setOnItemClickListener(new OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    onPopupItemChosen(view, position);
                }
            });
        }

        @Override
        public void setAdapter(ListAdapter adapter)
        {
            super.setAdapter(adapter);
            mListAdapter = adapter;
        }

        @Override
        public CharSequence getHintText()
        {
            return mHintText;
        }

        @Override
        public void setPromptText(CharSequence hintText)
        {
            mHintText = hintText;
        }

        void computeContentWidth()
        {
            Drawable background = getBackground();
            int horizontalOffset = 0;
            if (background != null)
            {
                background.getPadding(mTempRect);
                horizontalOffset = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? mTempRect.right : -mTempRect.left;
            }
            else
            {
                mTempRect.set(0, 0, 0, 0);
            }

            int spinnerPaddingLeft = Spinner.this.getPaddingLeft();
            int spinnerPaddingRight = Spinner.this.getPaddingRight();
            int spinnerWidth = Spinner.this.getWidth();

            if (mDropDownWidth == WRAP_CONTENT)
            {
                int contentWidth = measureContentWidth((SpinnerAdapter)mListAdapter, getBackground());
                int contentWidthLimit = getContext().getResources().getDisplayMetrics().widthPixels - mTempRect.left - mTempRect.right;
                if (contentWidth > contentWidthLimit) contentWidth = contentWidthLimit;

                setContentWidth(Math.max(contentWidth, spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight));
            }
            else if (mDropDownWidth == MATCH_PARENT)
            {
                setContentWidth(spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight);
            }
            else
            {
                setContentWidth(mDropDownWidth);
            }

            if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) horizontalOffset += spinnerWidth - spinnerPaddingRight - getWidth();
            else horizontalOffset += spinnerPaddingLeft;

            setHorizontalOffset(horizontalOffset);
        }

        @Override
        public void show()
        {
            computeContentWidth();
            setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);

            super.show();

            ListView list = getListView();
            if (list == null) return;

            list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

            int selected = Spinner.this.getSelectedItemPosition();
            if (selected >= 0)
            {
                list.setItemChecked(selected, true);
                list.setSelection(selected);
            }
        }
    }
}
