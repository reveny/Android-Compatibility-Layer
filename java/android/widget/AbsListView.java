package android.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.util.SparseBooleanArray;
import android.view.AbsSavedState;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.RuntimeMenu;
import android.view.StyleableSet;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStructure;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbsListView extends AdapterView<ListAdapter>
    implements TextWatcher, ViewTreeObserver.OnGlobalLayoutListener, Filter.FilterListener, ViewTreeObserver.OnTouchModeChangeListener
{
    public static final int TRANSCRIPT_MODE_DISABLED = 0;
    public static final int TRANSCRIPT_MODE_NORMAL = 1;
    public static final int TRANSCRIPT_MODE_ALWAYS_SCROLL = 2;

    public static final int CHOICE_MODE_NONE = 0;
    public static final int CHOICE_MODE_SINGLE = 1;
    public static final int CHOICE_MODE_MULTIPLE = 2;
    public static final int CHOICE_MODE_MULTIPLE_MODAL = 3;

    static final int TOUCH_MODE_REST = -1;
    static final int TOUCH_MODE_DOWN = 0;
    static final int TOUCH_MODE_TAP = 1;
    static final int TOUCH_MODE_DONE_WAITING = 2;
    static final int TOUCH_MODE_SCROLL = 3;
    static final int TOUCH_MODE_FLING = 4;

    private static final int STATE_PRESSED = 0x010100a7;
    private static final int DEFAULT_SELECTOR_COLOR = 0x1F000000;
    private static final int DEFAULT_SMOOTH_SCROLL_DURATION = 250;
    private static final int HAPTIC_LONG_PRESS = 0;

    private static StyleableSet sStyleable;

    ListAdapter mAdapter;
    AdapterDataSetObserver mDataSetObserver;
    final RecycleBin mRecycler = new RecycleBin();
    final Rect mListPadding = new Rect();
    int mWidthMeasureSpec;

    int mSyncPosition = INVALID_POSITION;
    int mSpecificTop;
    int mResurrectToPosition = INVALID_POSITION;

    int mChoiceMode = CHOICE_MODE_NONE;
    SparseBooleanArray mCheckStates;
    LongSparseArray<Integer> mCheckedIdStates;
    int mCheckedItemCount;
    private MultiChoiceModeListener mMultiChoiceModeListener;
    private ChoiceActionMode mChoiceActionMode;

    private OnScrollListener mOnScrollListener;
    private int mLastScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    private RecyclerListener mRecyclerListener;

    private Drawable mSelector;
    private boolean mDrawSelectorOnTop;
    private final Rect mSelectorRect = new Rect();
    private int mSelectorPosition = INVALID_POSITION;
    private boolean mSelectedChildViewEnabled;

    int mTouchMode = TOUCH_MODE_REST;
    private int mMotionPosition = INVALID_POSITION;
    private int mMotionX;
    private int mMotionY;
    private int mLastY;
    private final int mTouchSlop;
    private final int mMinimumVelocity;
    private final int mMaximumVelocity;
    private final float mVerticalScrollFactor;
    private float mVelocityScale = 1.0f;
    private VelocityTracker mVelocityTracker;
    private final OverScroller mScroller;
    private int mLastFlingY;
    private boolean mFlinging;
    private boolean mTouchOwnedBySelf;
    private Runnable mPendingCheckForTap;
    private Runnable mPendingCheckForLongPress;
    private Runnable mTouchModeReset;
    private PositionScroller mPositionScroller;

    private boolean mStackFromBottom;
    private int mTranscriptMode = TRANSCRIPT_MODE_DISABLED;
    private int mCacheColorHint;
    private boolean mSmoothScrollbarEnabled = true;
    private boolean mScrollingCacheEnabled;
    private boolean mTextFilterEnabled;
    private boolean mFastScrollEnabled;
    private boolean mFastScrollAlwaysVisible;
    private int mFastScrollStyle;
    private int mVerticalScrollbarPosition;
    private int mTopEdgeEffectColor;
    private int mBottomEdgeEffectColor;
    private CharSequence mFilterText;
    private boolean mFiltered;
    private View mScrollUp;
    private View mScrollDown;
    private ContextMenu.ContextMenuInfo mContextMenuInfo;

    public AbsListView(Context context)
    {
        this(context, null);
    }

    public AbsListView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public AbsListView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public AbsListView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mVerticalScrollFactor = configuration.getScaledVerticalScrollFactor();
        mScroller = new OverScroller(context);

        setClickable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
        setClipToPadding(true);

        if (attributes != null) readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "listSelector", "drawSelectorOnTop", "stackFromBottom", "scrollingCache",
                                         "textFilterEnabled", "transcriptMode", "cacheColorHint", "fastScrollEnabled", "smoothScrollbar",
                                         "choiceMode", "fastScrollAlwaysVisible");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int selectorIndex = sStyleable.indexOf("listSelector");
        if (selectorIndex >= 0 && array.hasValue(selectorIndex))
        {
            try
            {
                Drawable selector = array.getDrawable(selectorIndex);
                if (selector != null) setSelector(selector);
            }
            catch (RuntimeException unsupported)
            {
                android.util.Log.w("AbsListView", "the list selector could not be loaded, so the default one is used", unsupported);
            }
        }

        mDrawSelectorOnTop = readBoolean(array, "drawSelectorOnTop", false);
        mStackFromBottom = readBoolean(array, "stackFromBottom", false);
        mScrollingCacheEnabled = readBoolean(array, "scrollingCache", true);
        mTextFilterEnabled = readBoolean(array, "textFilterEnabled", false);
        mTranscriptMode = readInt(array, "transcriptMode", TRANSCRIPT_MODE_DISABLED);
        mCacheColorHint = readColor(array, "cacheColorHint", 0);
        mFastScrollEnabled = readBoolean(array, "fastScrollEnabled", false);
        mSmoothScrollbarEnabled = readBoolean(array, "smoothScrollbar", true);
        mFastScrollAlwaysVisible = readBoolean(array, "fastScrollAlwaysVisible", false);
        setChoiceMode(readInt(array, "choiceMode", CHOICE_MODE_NONE));

        array.recycle();
    }

    private static boolean readBoolean(TypedArray array, String name, boolean fallback)
    {
        int index = sStyleable.indexOf(name);
        return index < 0 ? fallback : array.getBoolean(index, fallback);
    }

    private static int readInt(TypedArray array, String name, int fallback)
    {
        int index = sStyleable.indexOf(name);
        return index < 0 ? fallback : array.getInt(index, fallback);
    }

    private static int readColor(TypedArray array, String name, int fallback)
    {
        int index = sStyleable.indexOf(name);
        if (index < 0 || !array.hasValue(index)) return fallback;
        return array.getColor(index, fallback);
    }

    @Override
    public void setAdapter(ListAdapter adapter)
    {
        if (adapter != null && mChoiceMode != CHOICE_MODE_NONE && adapter.hasStableIds() && mCheckedIdStates == null)
        {
            mCheckedIdStates = new LongSparseArray<Integer>();
        }

        clearChoices();
    }

    public int getCheckedItemCount()
    {
        return mCheckedItemCount;
    }

    public boolean isItemChecked(int position)
    {
        if (mChoiceMode != CHOICE_MODE_NONE && mCheckStates != null) return mCheckStates.get(position);
        return false;
    }

    public int getCheckedItemPosition()
    {
        if (mChoiceMode == CHOICE_MODE_SINGLE && mCheckStates != null && mCheckStates.size() == 1) return mCheckStates.keyAt(0);
        return INVALID_POSITION;
    }

    public SparseBooleanArray getCheckedItemPositions()
    {
        if (mChoiceMode != CHOICE_MODE_NONE) return mCheckStates;
        return null;
    }

    public long[] getCheckedItemIds()
    {
        if (mChoiceMode == CHOICE_MODE_NONE || mCheckedIdStates == null || mAdapter == null) return new long[0];

        LongSparseArray<Integer> states = mCheckedIdStates;
        long[] ids = new long[states.size()];
        for (int index = 0; index < ids.length; index++) ids[index] = states.keyAt(index);

        return ids;
    }

    public void clearChoices()
    {
        if (mCheckStates != null) mCheckStates.clear();
        if (mCheckedIdStates != null) mCheckedIdStates.clear();
        mCheckedItemCount = 0;

        updateOnScreenCheckedViews();
    }

    public void setItemChecked(int position, boolean value)
    {
        if (mChoiceMode == CHOICE_MODE_NONE) return;

        if (value && mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode == null)
        {
            if (mMultiChoiceModeListener == null)
            {
                throw new IllegalStateException("AbsListView: attempted to start selection mode for CHOICE_MODE_MULTIPLE_MODAL but no "
                                                + "choice mode callback was supplied. Call setMultiChoiceModeListener to set a callback.");
            }

            startChoiceActionMode();
        }

        boolean itemCheckChanged;
        if (mChoiceMode == CHOICE_MODE_MULTIPLE || mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL)
        {
            boolean oldValue = mCheckStates.get(position);
            mCheckStates.put(position, value);

            if (mCheckedIdStates != null && mAdapter != null && mAdapter.hasStableIds())
            {
                if (value) mCheckedIdStates.put(mAdapter.getItemId(position), position);
                else mCheckedIdStates.delete(mAdapter.getItemId(position));
            }

            itemCheckChanged = oldValue != value;
            if (itemCheckChanged)
            {
                if (value) mCheckedItemCount++;
                else mCheckedItemCount--;
            }

            if (mChoiceActionMode != null)
            {
                long id = mAdapter == null ? INVALID_ROW_ID : mAdapter.getItemId(position);
                mMultiChoiceModeListener.onItemCheckedStateChanged(mChoiceActionMode, position, id, value);
                if (mCheckedItemCount == 0) mChoiceActionMode.finish();
            }
        }
        else
        {
            boolean updateIds = mCheckedIdStates != null && mAdapter != null && mAdapter.hasStableIds();
            itemCheckChanged = isItemChecked(position) != value;

            if (value || isItemChecked(position))
            {
                mCheckStates.clear();
                if (updateIds) mCheckedIdStates.clear();
            }

            if (value)
            {
                mCheckStates.put(position, true);
                if (updateIds) mCheckedIdStates.put(mAdapter.getItemId(position), position);
                mCheckedItemCount = 1;
            }
            else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0))
            {
                mCheckedItemCount = 0;
            }
        }

        if (itemCheckChanged) updateOnScreenCheckedViews();
    }

    @Override
    public boolean performItemClick(View view, int position, long id)
    {
        boolean handled = false;
        boolean dispatchItemClick = true;

        if (mChoiceMode != CHOICE_MODE_NONE)
        {
            handled = true;
            boolean checkedStateChanged = false;

            if (mChoiceMode == CHOICE_MODE_MULTIPLE || (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode != null))
            {
                boolean checked = !mCheckStates.get(position, false);
                mCheckStates.put(position, checked);

                if (mCheckedIdStates != null && mAdapter.hasStableIds())
                {
                    if (checked) mCheckedIdStates.put(mAdapter.getItemId(position), position);
                    else mCheckedIdStates.delete(mAdapter.getItemId(position));
                }

                if (checked) mCheckedItemCount++;
                else mCheckedItemCount--;

                if (mChoiceActionMode != null)
                {
                    mMultiChoiceModeListener.onItemCheckedStateChanged(mChoiceActionMode, position, id, checked);
                    dispatchItemClick = false;
                    if (mCheckedItemCount == 0) mChoiceActionMode.finish();
                }

                checkedStateChanged = true;
            }
            else if (mChoiceMode == CHOICE_MODE_SINGLE)
            {
                boolean checked = !mCheckStates.get(position, false);
                if (checked)
                {
                    mCheckStates.clear();
                    mCheckStates.put(position, true);

                    if (mCheckedIdStates != null && mAdapter.hasStableIds())
                    {
                        mCheckedIdStates.clear();
                        mCheckedIdStates.put(mAdapter.getItemId(position), position);
                    }

                    mCheckedItemCount = 1;
                }
                else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0))
                {
                    mCheckedItemCount = 0;
                }

                checkedStateChanged = true;
            }

            if (checkedStateChanged) updateOnScreenCheckedViews();
        }

        if (dispatchItemClick) handled |= super.performItemClick(view, position, id);

        return handled;
    }

    void updateOnScreenCheckedViews()
    {
        int count = getChildCount();
        for (int index = 0; index < count; index++) applyCheckedState(getChildAt(index), mFirstPosition + index);
    }

    void applyCheckedState(View child, int position)
    {
        if (child == null || mCheckStates == null || mChoiceMode == CHOICE_MODE_NONE) return;

        boolean checked = mCheckStates.get(position);
        if (child instanceof Checkable) ((Checkable)child).setChecked(checked);
        else child.setActivated(checked);
    }

    public int getChoiceMode()
    {
        return mChoiceMode;
    }

    public void setChoiceMode(int choiceMode)
    {
        mChoiceMode = choiceMode;

        if (mChoiceActionMode != null) mChoiceActionMode.finish();

        if (mChoiceMode == CHOICE_MODE_NONE) return;

        if (mCheckStates == null) mCheckStates = new SparseBooleanArray(0);
        if (mCheckedIdStates == null && mAdapter != null && mAdapter.hasStableIds()) mCheckedIdStates = new LongSparseArray<Integer>(0);
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL)
        {
            clearChoices();
            setLongClickable(true);
        }
    }

    public void setMultiChoiceModeListener(MultiChoiceModeListener listener)
    {
        mMultiChoiceModeListener = listener;
    }

    private boolean startChoiceActionMode()
    {
        if (mMultiChoiceModeListener == null || mChoiceActionMode != null) return mChoiceActionMode != null;

        ChoiceActionMode mode = new ChoiceActionMode();
        if (!mMultiChoiceModeListener.onCreateActionMode(mode, mode.getMenu())) return false;

        mChoiceActionMode = mode;
        mMultiChoiceModeListener.onPrepareActionMode(mode, mode.getMenu());
        return true;
    }

    public void setFastScrollEnabled(boolean enabled)
    {
        mFastScrollEnabled = enabled;
    }

    public void setFastScrollStyle(int styleResId)
    {
        mFastScrollStyle = styleResId;
    }

    public void setFastScrollAlwaysVisible(boolean alwaysShow)
    {
        mFastScrollAlwaysVisible = alwaysShow;
        if (alwaysShow) mFastScrollEnabled = true;
    }

    public boolean isFastScrollAlwaysVisible()
    {
        return mFastScrollEnabled && mFastScrollAlwaysVisible;
    }

    public int getVerticalScrollbarWidth()
    {
        return ViewConfiguration.get(getContext()).getScaledScrollBarSize();
    }

    public boolean isFastScrollEnabled()
    {
        return mFastScrollEnabled;
    }

    public void setVerticalScrollbarPosition(int position)
    {
        mVerticalScrollbarPosition = position;
    }

    @Override
    public void setScrollBarStyle(int style)
    {
        super.setScrollBarStyle(style);
    }

    public void setSmoothScrollbarEnabled(boolean enabled)
    {
        mSmoothScrollbarEnabled = enabled;
    }

    public boolean isSmoothScrollbarEnabled()
    {
        return mSmoothScrollbarEnabled;
    }

    public void setOnScrollListener(OnScrollListener listener)
    {
        mOnScrollListener = listener;
        invokeOnItemScrollListener();
    }

    void invokeOnItemScrollListener()
    {
        if (mOnScrollListener != null) mOnScrollListener.onScroll(this, mFirstPosition, getChildCount(), mItemCount);
        updateScrollIndicators();
    }

    void reportScrollStateChange(int newState)
    {
        if (newState == mLastScrollState) return;

        mLastScrollState = newState;
        if (mOnScrollListener != null) mOnScrollListener.onScrollStateChanged(this, newState);
    }

    private void updateScrollIndicators()
    {
        if (mScrollUp != null) mScrollUp.setVisibility(canScrollList(-1) ? View.VISIBLE : View.INVISIBLE);
        if (mScrollDown != null) mScrollDown.setVisibility(canScrollList(1) ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return AbsListView.class.getName();
    }

    public boolean isScrollingCacheEnabled()
    {
        return mScrollingCacheEnabled;
    }

    public void setScrollingCacheEnabled(boolean enabled)
    {
        mScrollingCacheEnabled = enabled;
    }

    public void setTextFilterEnabled(boolean textFilterEnabled)
    {
        mTextFilterEnabled = textFilterEnabled;
    }

    public boolean isTextFilterEnabled()
    {
        return mTextFilterEnabled;
    }

    public void getFocusedRect(Rect rect)
    {
        View view = getSelectedView();
        if (view != null && view.getParent() == this)
        {
            rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            return;
        }

        rect.set(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
    }

    public boolean isStackFromBottom()
    {
        return mStackFromBottom;
    }

    public void setStackFromBottom(boolean stackFromBottom)
    {
        if (mStackFromBottom == stackFromBottom) return;

        mStackFromBottom = stackFromBottom;
        requestLayoutIfNecessary();
    }

    void requestLayoutIfNecessary()
    {
        if (getChildCount() == 0) return;

        requestLayout();
        invalidate();
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.firstPosition = mFirstPosition;
        state.viewTop = getChildCount() > 0 ? getChildAt(0).getTop() - mListPadding.top : 0;
        state.checkedItemCount = mCheckedItemCount;
        state.filter = mFilterText == null ? null : mFilterText.toString();

        if (mCheckStates != null)
        {
            state.checkedPositions = new int[mCheckStates.size()];
            state.checkedValues = new boolean[mCheckStates.size()];
            for (int index = 0; index < mCheckStates.size(); index++)
            {
                state.checkedPositions[index] = mCheckStates.keyAt(index);
                state.checkedValues[index] = mCheckStates.valueAt(index);
            }
        }

        if (mCheckedIdStates != null)
        {
            state.checkedIds = new long[mCheckedIdStates.size()];
            state.checkedIdPositions = new int[mCheckedIdStates.size()];
            for (int index = 0; index < mCheckedIdStates.size(); index++)
            {
                state.checkedIds[index] = mCheckedIdStates.keyAt(index);
                state.checkedIdPositions[index] = mCheckedIdStates.valueAt(index);
            }
        }

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
        mDataChanged = true;

        if (state.firstPosition >= 0)
        {
            mSyncPosition = state.firstPosition;
            mSpecificTop = state.viewTop;
        }

        if (state.checkedPositions != null)
        {
            if (mCheckStates == null) mCheckStates = new SparseBooleanArray();
            mCheckStates.clear();
            for (int index = 0; index < state.checkedPositions.length; index++)
            {
                mCheckStates.put(state.checkedPositions[index], state.checkedValues[index]);
            }
        }

        if (state.checkedIds != null)
        {
            if (mCheckedIdStates == null) mCheckedIdStates = new LongSparseArray<Integer>();
            mCheckedIdStates.clear();
            for (int index = 0; index < state.checkedIds.length; index++)
            {
                mCheckedIdStates.put(state.checkedIds[index], state.checkedIdPositions[index]);
            }
        }

        mCheckedItemCount = state.checkedItemCount;
        if (state.filter != null) setFilterText(state.filter);

        requestLayout();
    }

    public void setFilterText(String filterText)
    {
        if (!mTextFilterEnabled || filterText == null || filterText.isEmpty()) return;

        mFilterText = filterText;
        if (!(mAdapter instanceof Filterable)) return;

        Filter filter = ((Filterable)mAdapter).getFilter();
        if (filter == null) return;

        mFiltered = true;
        filter.filter(filterText, this);
    }

    public CharSequence getTextFilter()
    {
        return mTextFilterEnabled ? mFilterText : null;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
    {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus && mSelectedPosition < 0 && !isInTouchMode()) requestLayout();
    }

    @Override
    public void requestLayout()
    {
        if (!mBlockLayoutRequests && !mInLayout) super.requestLayout();
    }

    void resetList()
    {
        removeAllViewsInLayout();
        mRecycler.clear();
        mFirstPosition = 0;
        mDataChanged = false;
        mSyncPosition = INVALID_POSITION;
        mOldSelectedPosition = INVALID_POSITION;
        mOldSelectedRowId = INVALID_ROW_ID;
        setSelectedPositionInt(INVALID_POSITION);
        setNextSelectedPositionInt(INVALID_POSITION);
        mSelectorPosition = INVALID_POSITION;
        mSelectorRect.setEmpty();
        invalidate();
    }

    @Override
    protected int computeVerticalScrollExtent()
    {
        int count = getChildCount();
        if (count == 0) return 0;
        if (!mSmoothScrollbarEnabled) return 1;

        int extent = count * 100;

        View first = getChildAt(0);
        int top = first.getTop();
        int height = first.getHeight();
        if (height > 0) extent += (top * 100) / height;

        View last = getChildAt(count - 1);
        int bottom = last.getBottom();
        height = last.getHeight();
        if (height > 0) extent -= ((bottom - getHeight()) * 100) / height;

        return extent;
    }

    @Override
    protected int computeVerticalScrollOffset()
    {
        int count = getChildCount();
        if (mFirstPosition < 0 || count == 0) return 0;

        if (mSmoothScrollbarEnabled)
        {
            View first = getChildAt(0);
            int height = first.getHeight();
            if (height <= 0) return 0;

            int rows = Math.max(1, columnCount());
            return Math.max((mFirstPosition / rows) * 100 - (first.getTop() * 100) / height, 0);
        }

        int itemCount = mItemCount;
        int index;
        if (mFirstPosition == 0) index = 0;
        else if (mFirstPosition + count == itemCount) index = itemCount;
        else index = mFirstPosition + count / 2;

        return (int)(mFirstPosition + count * (index / (float)itemCount));
    }

    @Override
    protected int computeVerticalScrollRange()
    {
        if (!mSmoothScrollbarEnabled) return mItemCount;

        int rows = Math.max(1, columnCount());
        return Math.max(((mItemCount + rows - 1) / rows) * 100, 0);
    }

    @Override
    protected float getTopFadingEdgeStrength()
    {
        if (getChildCount() == 0) return 0.0f;
        if (mFirstPosition > 0) return 1.0f;

        int top = getChildAt(0).getTop();
        int fadeLength = getVerticalFadingEdgeLength();
        if (fadeLength <= 0) return 0.0f;

        return top < getPaddingTop() ? (float)-(top - getPaddingTop()) / fadeLength : 0.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength()
    {
        int count = getChildCount();
        if (count == 0) return 0.0f;
        if (mFirstPosition + count - 1 < mItemCount - 1) return 1.0f;

        int bottom = getChildAt(count - 1).getBottom();
        int height = getHeight();
        int fadeLength = getVerticalFadingEdgeLength();
        if (fadeLength <= 0) return 0.0f;

        return bottom > height - getPaddingBottom() ? (float)(bottom - height + getPaddingBottom()) / fadeLength : 0.0f;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        updateListPadding();
        mWidthMeasureSpec = widthMeasureSpec;

        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                             getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    void updateListPadding()
    {
        mListPadding.set(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        updateListPadding();
        mInLayout = true;

        try
        {
            if (mDataChanged && mAdapter != null)
            {
                mOldItemCount = mItemCount;
                mItemCount = mAdapter.getCount();
            }

            layoutChildren();
        }
        finally
        {
            mInLayout = false;
        }

        checkSelectionChanged();
    }

    protected void layoutChildren()
    {
    }

    int columnCount()
    {
        return 1;
    }

    int rowSpacing()
    {
        return 0;
    }

    void measureItem(View child, int column)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        int widthSpec = mWidthMeasureSpec;
        if (widthSpec == 0) widthSpec = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY);

        int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec, mListPadding.left + mListPadding.right, params.width);
        int childHeightSpec = params.height > 0 ? MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY)
                                                : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        child.measure(childWidthSpec, childHeightSpec);
    }

    int itemLeft(int column, View child)
    {
        return mListPadding.left;
    }

    int rowStart(int position)
    {
        int columns = Math.max(1, columnCount());
        return position - position % columns;
    }

    int fillTopLimit()
    {
        return getClipToPadding() ? mListPadding.top : 0;
    }

    int fillBottomLimit()
    {
        return getClipToPadding() ? getHeight() - mListPadding.bottom : getHeight();
    }

    void layoutRows()
    {
        if (mBlockLayoutRequests) return;

        mBlockLayoutRequests = true;

        try
        {
            invalidate();

            if (mAdapter == null || mItemCount == 0)
            {
                if (mAdapter == null) mItemCount = 0;

                recycleAllChildren();
                mRecycler.flushTransient();
                mFirstPosition = 0;
                mDataChanged = false;
                mSyncPosition = INVALID_POSITION;
                invokeOnItemScrollListener();
                return;
            }

            if (mItemCount != mAdapter.getCount())
            {
                android.util.Log.w("AbsListView", "The content of the adapter has changed but " + getClass().getName()
                                                  + " did not receive a notification, so the list is rebuilt from "
                                                  + mAdapter.getClass().getName());
                mOldItemCount = mItemCount;
                mItemCount = mAdapter.getCount();
                mDataChanged = true;
            }

            int listTop = mListPadding.top;
            int listBottom = getHeight() - mListPadding.bottom;
            int childCount = getChildCount();

            if (!mDataChanged && mSyncPosition < 0 && childCount > 0 && mFirstPosition + childCount <= mItemCount)
            {
                relayoutInPlace(firstRowTop());
                return;
            }

            int anchorPosition;
            int anchorTop;
            boolean fromBottom = false;

            if (mSyncPosition >= 0)
            {
                anchorPosition = mSyncPosition;
                anchorTop = listTop + mSpecificTop;
            }
            else if (shouldStickToBottom())
            {
                anchorPosition = mItemCount - 1;
                anchorTop = listBottom;
                fromBottom = true;
            }
            else if (childCount > 0)
            {
                anchorPosition = mFirstPosition;
                anchorTop = getChildAt(0).getTop();
            }
            else if (mStackFromBottom)
            {
                anchorPosition = mItemCount - 1;
                anchorTop = listBottom;
                fromBottom = true;
            }
            else
            {
                anchorPosition = 0;
                anchorTop = listTop;
            }

            if (anchorPosition >= mItemCount)
            {
                anchorPosition = mItemCount - 1;
                anchorTop = listBottom;
                fromBottom = true;
            }

            anchorPosition = rowStart(Math.max(0, anchorPosition));

            if (mDataChanged) recycleAllChildren();
            else mRecycler.fillActiveViews(mFirstPosition);

            mFirstPosition = anchorPosition;

            if (fromBottom)
            {
                fillUp(anchorPosition, anchorTop);
                fillDown(mFirstPosition + getChildCount(), lastRowBottom() + rowSpacing());
            }
            else
            {
                fillDown(anchorPosition, anchorTop);
                fillUp(anchorPosition - Math.max(1, columnCount()), anchorTop - rowSpacing());
            }

            correctGaps();

            mRecycler.scrapActiveViews();
            mRecycler.flushTransient();
            mRecycler.prune(getChildCount());

            mDataChanged = false;
            mSyncPosition = INVALID_POSITION;
            mResurrectToPosition = INVALID_POSITION;

            if (mSelectorPosition >= 0) positionSelector(mSelectorPosition);
            updateOnScreenCheckedViews();
            invokeOnItemScrollListener();
        }
        finally
        {
            mBlockLayoutRequests = false;
        }
    }

    private void relayoutInPlace(int top)
    {
        int columns = Math.max(1, columnCount());
        int childCount = getChildCount();
        int nextTop = top;

        for (int rowIndex = 0; rowIndex < childCount; rowIndex += columns)
        {
            int rowEnd = Math.min(childCount, rowIndex + columns);
            int rowHeight = 0;

            for (int index = rowIndex; index < rowEnd; index++)
            {
                View child = getChildAt(index);
                listParamsFor(child);
                measureItem(child, index - rowIndex);
                rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
            }

            for (int index = rowIndex; index < rowEnd; index++)
            {
                View child = getChildAt(index);
                int left = itemLeft(index - rowIndex, child);
                child.layout(left, nextTop, left + child.getMeasuredWidth(), nextTop + child.getMeasuredHeight());
            }

            nextTop += rowHeight + rowSpacing();
        }

        trimBottom();
        fillDown(mFirstPosition + getChildCount(), lastRowBottom() + rowSpacing());
        correctGaps();

        mRecycler.flushTransient();
        mRecycler.prune(getChildCount());

        if (mSelectorPosition >= 0) positionSelector(mSelectorPosition);
        updateOnScreenCheckedViews();
        invokeOnItemScrollListener();
    }

    private boolean shouldStickToBottom()
    {
        if (!mDataChanged || mTranscriptMode == TRANSCRIPT_MODE_DISABLED || mItemCount <= mOldItemCount) return false;
        if (mTranscriptMode == TRANSCRIPT_MODE_ALWAYS_SCROLL) return true;

        int childCount = getChildCount();
        if (childCount == 0) return mStackFromBottom;

        int lastPosition = mFirstPosition + childCount - 1;
        return lastPosition >= mOldItemCount - 1 && lastRowBottom() <= getHeight() - mListPadding.bottom;
    }

    private void recycleAllChildren()
    {
        int childCount = getChildCount();
        if (childCount == 0) return;

        View[] children = new View[childCount];
        for (int index = 0; index < childCount; index++) children[index] = getChildAt(index);

        detachAllViewsFromParent();

        for (int index = 0; index < childCount; index++) mRecycler.addScrapView(children[index], mFirstPosition + index);
    }

    void fillDown(int position, int nextTop)
    {
        int end = fillBottomLimit();
        int columns = Math.max(1, columnCount());

        while (nextTop < end && position < mItemCount)
        {
            int bottom = makeRow(position, nextTop, true);
            nextTop = bottom + rowSpacing();
            position += columns;
        }
    }

    void fillUp(int position, int nextBottom)
    {
        int end = fillTopLimit();
        int columns = Math.max(1, columnCount());

        while (nextBottom > end && position >= 0)
        {
            int top = makeRow(position, nextBottom, false);
            mFirstPosition = position;
            nextBottom = top - rowSpacing();
            position -= columns;
        }
    }

    int makeRow(int startPosition, int edge, boolean flowDown)
    {
        int columns = Math.max(1, columnCount());
        int end = Math.min(startPosition + columns, mItemCount);
        int count = end - startPosition;
        View[] row = new View[count];
        int rowHeight = 0;
        int insertIndex = flowDown ? getChildCount() : 0;

        for (int column = 0; column < count; column++)
        {
            int position = startPosition + column;
            View child = obtainView(position);
            attachChild(child, insertIndex + column, position);
            measureItem(child, column);

            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
            row[column] = child;
        }

        int top = flowDown ? edge : edge - rowHeight;

        for (int column = 0; column < count; column++)
        {
            View child = row[column];
            int left = itemLeft(column, child);
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }

        return flowDown ? top + rowHeight : top;
    }

    View obtainView(int position)
    {
        View active = mRecycler.takeActiveView(position);
        if (active != null) return active;

        int viewType = mAdapter.getItemViewType(position);
        View scrap = mRecycler.shouldRecycleViewType(viewType) ? mRecycler.takeScrapView(viewType) : null;

        View child = mAdapter.getView(position, scrap, this);
        if (child == null) throw new NullPointerException("Adapter.getView returned null for position " + position);

        if (scrap != null && child != scrap) mRecycler.addScrapView(scrap, position);

        LayoutParams params = listParamsFor(child);
        params.viewType = viewType;
        params.itemId = mAdapter.getItemId(position);
        params.recycledHeaderFooter = viewType == ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
        return child;
    }

    LayoutParams listParamsFor(View child)
    {
        ViewGroup.LayoutParams current = child.getLayoutParams();
        if (current instanceof LayoutParams) return (LayoutParams)current;

        LayoutParams params = current == null ? (LayoutParams)generateDefaultLayoutParams() : new LayoutParams(current);
        child.setLayoutParams(params);
        return params;
    }

    void attachChild(View child, int index, int position)
    {
        LayoutParams params = listParamsFor(child);

        if (child.getParent() == this)
        {
            if (indexOfChild(child) >= 0) detachViewFromParent(child);

            mRecycler.forget(child);
            attachViewToParent(child, index, params);
        }
        else
        {
            mRecycler.forget(child);
            addViewInLayout(child, index, params, true);
        }

        applyCheckedState(child, position);
    }

    int lastRowBottom()
    {
        int childCount = getChildCount();
        if (childCount == 0) return mListPadding.top;

        int lastRowIndex = rowStart(mFirstPosition + childCount - 1) - mFirstPosition;
        int bottom = Integer.MIN_VALUE;
        for (int index = Math.max(0, lastRowIndex); index < childCount; index++) bottom = Math.max(bottom, getChildAt(index).getBottom());

        return bottom;
    }

    int firstRowTop()
    {
        if (getChildCount() == 0) return mListPadding.top;

        int columns = Math.max(1, columnCount());
        int top = Integer.MAX_VALUE;
        for (int index = 0; index < Math.min(columns, getChildCount()); index++) top = Math.min(top, getChildAt(index).getTop());

        return top;
    }

    private void correctGaps()
    {
        if (getChildCount() == 0) return;

        int listTop = mListPadding.top;
        int listBottom = getHeight() - mListPadding.bottom;
        int columns = Math.max(1, columnCount());

        int firstTop = firstRowTop();
        if (mFirstPosition == 0 && firstTop > listTop && !mStackFromBottom)
        {
            offsetChildren(listTop - firstTop);
            fillDown(mFirstPosition + getChildCount(), lastRowBottom() + rowSpacing());
        }

        firstTop = firstRowTop();
        int lastBottom = lastRowBottom();
        int lastPosition = mFirstPosition + getChildCount() - 1;

        if (lastPosition >= mItemCount - 1 && lastBottom < listBottom)
        {
            int shift = listBottom - lastBottom;
            if (mFirstPosition == 0) shift = Math.min(shift, Math.max(0, listTop - firstTop));
            if (mStackFromBottom && mFirstPosition == 0) shift = listBottom - lastBottom;

            if (shift > 0)
            {
                offsetChildren(shift);
                fillUp(mFirstPosition - columns, firstRowTop() - rowSpacing());
            }
        }
    }

    void offsetChildren(int offset)
    {
        for (int index = 0; index < getChildCount(); index++) getChildAt(index).offsetTopAndBottom(offset);
    }

    boolean trackMotionScroll(int deltaY)
    {
        int childCount = getChildCount();
        if (childCount == 0 || mAdapter == null) return true;

        int height = getHeight() - mListPadding.top - mListPadding.bottom;
        if (height > 1) deltaY = Math.max(-(height - 1), Math.min(height - 1, deltaY));

        int listTop = mListPadding.top;
        int listBottom = getHeight() - mListPadding.bottom;
        int firstTop = firstRowTop();
        int lastBottom = lastRowBottom();
        int lastPosition = mFirstPosition + childCount - 1;
        boolean atEdge = false;

        if (deltaY > 0 && mFirstPosition == 0)
        {
            int room = listTop - firstTop;
            if (deltaY >= room)
            {
                deltaY = Math.max(0, room);
                atEdge = true;
            }
        }

        if (deltaY < 0 && lastPosition >= mItemCount - 1)
        {
            int room = listBottom - lastBottom;
            if (deltaY <= room)
            {
                deltaY = Math.min(0, room);
                atEdge = true;
            }
        }

        if (deltaY == 0) return atEdge;

        offsetChildren(deltaY);

        if (deltaY < 0) trimTop();
        else trimBottom();

        if (deltaY < 0) fillDown(mFirstPosition + getChildCount(), lastRowBottom() + rowSpacing());
        else fillUp(mFirstPosition - Math.max(1, columnCount()), firstRowTop() - rowSpacing());

        correctGaps();

        if (deltaY < 0 && mFirstPosition + getChildCount() - 1 >= mItemCount - 1 && lastRowBottom() <= listBottom) atEdge = true;
        if (deltaY > 0 && mFirstPosition == 0 && firstRowTop() >= listTop) atEdge = true;

        mRecycler.flushTransient();
        mRecycler.prune(getChildCount());

        if (mSelectorPosition >= 0) positionSelector(mSelectorPosition);

        invalidate();
        invokeOnItemScrollListener();
        awakenScrollBars();

        return atEdge;
    }

    private void trimTop()
    {
        int columns = Math.max(1, columnCount());
        int limit = fillTopLimit();

        while (getChildCount() > columns)
        {
            int rowSize = Math.min(columns, getChildCount());
            int rowBottom = Integer.MIN_VALUE;
            for (int index = 0; index < rowSize; index++) rowBottom = Math.max(rowBottom, getChildAt(index).getBottom());

            if (rowBottom >= limit) return;

            for (int index = 0; index < rowSize; index++)
            {
                View child = getChildAt(0);
                detachViewFromParent(child);
                mRecycler.addScrapView(child, mFirstPosition);
                mFirstPosition++;
            }
        }
    }

    private void trimBottom()
    {
        int limit = fillBottomLimit();

        while (getChildCount() > 1)
        {
            int childCount = getChildCount();
            int lastRowIndex = Math.max(0, rowStart(mFirstPosition + childCount - 1) - mFirstPosition);
            if (lastRowIndex == 0) return;

            int rowTop = Integer.MAX_VALUE;
            for (int index = lastRowIndex; index < childCount; index++) rowTop = Math.min(rowTop, getChildAt(index).getTop());

            if (rowTop <= limit) return;

            for (int index = childCount - 1; index >= lastRowIndex; index--)
            {
                View child = getChildAt(index);
                detachViewFromParent(child);
                mRecycler.addScrapView(child, mFirstPosition + index);
            }
        }
    }

    @Override
    public View getSelectedView()
    {
        if (mItemCount > 0 && mSelectedPosition >= 0) return getChildAt(mSelectedPosition - mFirstPosition);
        return null;
    }

    public int getListPaddingTop()
    {
        return mListPadding.top;
    }

    public int getListPaddingBottom()
    {
        return mListPadding.bottom;
    }

    public int getListPaddingLeft()
    {
        return mListPadding.left;
    }

    public int getListPaddingRight()
    {
        return mListPadding.right;
    }

    public void onInitializeAccessibilityNodeInfoForItem(View view, int position, AccessibilityNodeInfo info)
    {
    }

    public boolean isSelectedChildViewEnabled()
    {
        return mSelectedChildViewEnabled;
    }

    public void setSelectedChildViewEnabled(boolean selectedChildViewEnabled)
    {
        mSelectedChildViewEnabled = selectedChildViewEnabled;
    }

    @Override
    protected void dispatchDraw(Canvas canvas)
    {
        int saveCount = canvas.save();
        if (getClipToPadding())
        {
            canvas.clipRect(getScrollX() + getPaddingLeft(), getScrollY() + getPaddingTop(), getScrollX() + getWidth() - getPaddingRight(),
                            getScrollY() + getHeight() - getPaddingBottom());
        }

        if (!mDrawSelectorOnTop) drawSelector(canvas);
        super.dispatchDraw(canvas);
        if (mDrawSelectorOnTop) drawSelector(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void drawSelector(Canvas canvas)
    {
        if (mSelectorRect.isEmpty() || mSelectorPosition < 0) return;
        if (mTouchMode != TOUCH_MODE_TAP && mTouchMode != TOUCH_MODE_DONE_WAITING && !isPressed()) return;

        Drawable selector = getSelector();
        selector.setBounds(mSelectorRect);
        selector.draw(canvas);
    }

    protected boolean isPaddingOffsetRequired()
    {
        return !getClipToPadding();
    }

    protected int getLeftPaddingOffset()
    {
        return getClipToPadding() ? 0 : -getPaddingLeft();
    }

    protected int getTopPaddingOffset()
    {
        return getClipToPadding() ? 0 : -getPaddingTop();
    }

    protected int getRightPaddingOffset()
    {
        return getClipToPadding() ? 0 : getPaddingRight();
    }

    protected int getBottomPaddingOffset()
    {
        return getClipToPadding() ? 0 : getPaddingBottom();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (getChildCount() > 0 && height != oldHeight) invokeOnItemScrollListener();
    }

    public void setDrawSelectorOnTop(boolean onTop)
    {
        mDrawSelectorOnTop = onTop;
    }

    public boolean isDrawSelectorOnTop()
    {
        return mDrawSelectorOnTop;
    }

    public void setSelector(int resID)
    {
        setSelector(getContext().getDrawable(resID));
    }

    public void setSelector(Drawable selector)
    {
        if (mSelector != null) mSelector.setCallback(null);

        mSelector = selector;
        if (selector != null) selector.setCallback(this);

        invalidate();
    }

    public Drawable getSelector()
    {
        if (mSelector == null)
        {
            mSelector = new ColorDrawable(DEFAULT_SELECTOR_COLOR);
            mSelector.setCallback(this);
        }

        return mSelector;
    }

    void positionSelector(int position)
    {
        mSelectorPosition = position;

        View child = getChildAt(position - mFirstPosition);
        if (child == null)
        {
            mSelectorRect.setEmpty();
            return;
        }

        mSelectorRect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        if (child instanceof SelectionBoundsAdjuster) ((SelectionBoundsAdjuster)child).adjustListItemSelectionBounds(mSelectorRect);
    }

    private void clearSelector()
    {
        mSelectorPosition = INVALID_POSITION;
        mSelectorRect.setEmpty();
        invalidate();
    }

    public void setScrollIndicators(View up, View down)
    {
        mScrollUp = up;
        mScrollDown = down;
        updateScrollIndicators();
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();
        updateSelectorState();
    }

    private void updateSelectorState()
    {
        if (mSelector == null) return;

        boolean pressed = isPressed() || mTouchMode == TOUCH_MODE_TAP || mTouchMode == TOUCH_MODE_DONE_WAITING;
        mSelector.setState(pressed ? new int[] {STATE_PRESSED} : EMPTY_STATE_SET);
    }

    @Override
    public boolean verifyDrawable(Drawable drawable)
    {
        return mSelector == drawable || super.verifyDrawable(drawable);
    }

    public void jumpDrawablesToCurrentState()
    {
        if (mSelector != null) mSelector.jumpToCurrentState();
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if (mAdapter != null && mDataSetObserver == null)
        {
            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        stopFling();
        if (mPositionScroller != null) mPositionScroller.stop();
        if (mPendingCheckForTap != null) removeCallbacks(mPendingCheckForTap);
        if (mPendingCheckForLongPress != null) removeCallbacks(mPendingCheckForLongPress);
        if (mTouchModeReset != null)
        {
            removeCallbacks(mTouchModeReset);
            mTouchModeReset.run();
        }

        mRecycler.clear();

        if (mAdapter != null && mDataSetObserver != null)
        {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus)
    {
        super.onWindowFocusChanged(hasWindowFocus);

        if (!hasWindowFocus)
        {
            if (mTouchMode == TOUCH_MODE_FLING) stopFling();
            setPressed(false);
        }
    }

    public void onRtlPropertiesChanged(int layoutDirection)
    {
        requestLayoutIfNecessary();
    }

    public void onCancelPendingInputEvents()
    {
        if (mPendingCheckForTap != null) removeCallbacks(mPendingCheckForTap);
        if (mPendingCheckForLongPress != null) removeCallbacks(mPendingCheckForLongPress);
    }

    protected ContextMenu.ContextMenuInfo getContextMenuInfo()
    {
        return mContextMenuInfo;
    }

    @Override
    public boolean showContextMenu()
    {
        return super.showContextMenu();
    }

    @Override
    public boolean showContextMenu(float x, float y)
    {
        int position = pointToPosition((int)x, (int)y);
        if (position != INVALID_POSITION)
        {
            long id = mAdapter.getItemId(position);
            View child = getChildAt(position - mFirstPosition);
            if (child != null)
            {
                mContextMenuInfo = new AdapterContextMenuInfo(child, position, id);
                return super.showContextMenu();
            }
        }

        return super.showContextMenu(x, y);
    }

    public boolean showContextMenuForChild(View originalView)
    {
        int position = getPositionForView(originalView);
        if (position < 0) return false;

        long id = mAdapter.getItemId(position);
        boolean handled = false;
        if (mOnItemLongClickListener != null) handled = mOnItemLongClickListener.onItemLongClick(this, originalView, position, id);

        if (!handled)
        {
            mContextMenuInfo = new AdapterContextMenuInfo(getChildAt(position - mFirstPosition), position, id);
            handled = super.showContextMenu();
        }

        return handled;
    }

    public boolean showContextMenuForChild(View originalView, float x, float y)
    {
        return showContextMenuForChild(originalView);
    }

    boolean performLongPress(View child, int position, long id)
    {
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL)
        {
            if (mChoiceActionMode == null && startChoiceActionMode())
            {
                setItemChecked(position, true);
                performHapticFeedback(HAPTIC_LONG_PRESS);
            }

            return true;
        }

        boolean handled = false;
        if (mOnItemLongClickListener != null) handled = mOnItemLongClickListener.onItemLongClick(this, child, position, id);

        if (!handled)
        {
            mContextMenuInfo = new AdapterContextMenuInfo(child, position, id);
            handled = super.showContextMenu();
        }

        if (handled) performHapticFeedback(HAPTIC_LONG_PRESS);
        return handled;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (mAdapter == null || mItemCount == 0) return super.onKeyDown(keyCode, event);

        int page = Math.max(1, getHeight() - mListPadding.top - mListPadding.bottom);
        int row = getChildCount() > 0 ? Math.max(1, getChildAt(0).getHeight() + rowSpacing()) : page / 4;

        switch (keyCode)
        {
            case KeyEvent.KEYCODE_DPAD_UP:
                smoothScrollBy(-row, DEFAULT_SMOOTH_SCROLL_DURATION);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                smoothScrollBy(row, DEFAULT_SMOOTH_SCROLL_DURATION);
                return true;
            case KeyEvent.KEYCODE_PAGE_UP:
                smoothScrollBy(-page, DEFAULT_SMOOTH_SCROLL_DURATION);
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                smoothScrollBy(page, DEFAULT_SMOOTH_SCROLL_DURATION);
                return true;
            case KeyEvent.KEYCODE_MOVE_HOME:
                setSelection(0);
                return true;
            case KeyEvent.KEYCODE_MOVE_END:
                setSelection(mItemCount - 1);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
        {
            if (!isEnabled()) return true;

            int position = mSelectedPosition;
            if (isClickable() && isPressed() && position >= 0 && mAdapter != null && position < mAdapter.getCount())
            {
                View view = getChildAt(position - mFirstPosition);
                if (view != null)
                {
                    performItemClick(view, position, mSelectedRowId);
                    view.setPressed(false);
                }

                setPressed(false);
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    protected void dispatchSetPressed(boolean pressed)
    {
    }

    public void dispatchDrawableHotspotChanged(float x, float y)
    {
    }

    public int pointToPosition(int x, int y)
    {
        for (int index = getChildCount() - 1; index >= 0; index--)
        {
            View child = getChildAt(index);
            if (child.getVisibility() != View.VISIBLE) continue;

            if (x >= child.getLeft() && x < child.getRight() && y >= child.getTop() && y < child.getBottom()) return mFirstPosition + index;
        }

        return INVALID_POSITION;
    }

    public long pointToRowId(int x, int y)
    {
        int position = pointToPosition(x, y);
        if (position >= 0 && mAdapter != null) return mAdapter.getItemId(position);
        return INVALID_ROW_ID;
    }

    @Override
    public void onTouchModeChanged(boolean isInTouchMode)
    {
        if (isInTouchMode) clearSelector();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        int action = event.getActionMasked();

        if (mPositionScroller != null) mPositionScroller.stop();
        if (!isEnabled()) return false;
        if (action != MotionEvent.ACTION_DOWN && mTouchOwnedBySelf) return false;

        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
            {
                mTouchOwnedBySelf = false;
                int touchMode = mTouchMode;
                int x = (int)event.getX();
                int y = (int)event.getY();

                mMotionX = x;
                mMotionY = y;
                mLastY = y;
                mMotionPosition = pointToPosition(x, y);
                obtainVelocityTracker().clear();
                mVelocityTracker.addMovement(event);

                if (touchMode == TOUCH_MODE_FLING)
                {
                    stopFling();
                    mTouchMode = TOUCH_MODE_SCROLL;
                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    return true;
                }

                mTouchMode = TOUCH_MODE_DOWN;
                return false;
            }
            case MotionEvent.ACTION_MOVE:
            {
                if (mTouchMode == TOUCH_MODE_SCROLL) return true;
                if (mVelocityTracker != null) mVelocityTracker.addMovement(event);

                if (mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_TAP || mTouchMode == TOUCH_MODE_DONE_WAITING)
                {
                    return startScrollIfNeeded((int)event.getY());
                }

                return false;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchMode = TOUCH_MODE_REST;
                recycleVelocityTracker();
                reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                return false;
            default:
                return false;
        }
    }

    private boolean startScrollIfNeeded(int y)
    {
        int distance = y - mMotionY;
        if (Math.abs(distance) <= mTouchSlop) return false;
        if (!canScrollList(distance > 0 ? -1 : 1)) return false;

        mTouchMode = TOUCH_MODE_SCROLL;
        mLastY = distance > 0 ? mMotionY + mTouchSlop : mMotionY - mTouchSlop;

        cancelPendingPress();
        setPressed(false);
        clearSelector();

        View child = getChildAt(mMotionPosition - mFirstPosition);
        if (child != null) child.setPressed(false);

        android.view.ViewParent parent = getParent();
        if (parent != null) parent.requestDisallowInterceptTouchEvent(true);

        reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
        scrollByTouch(y);
        return true;
    }

    private void scrollByTouch(int y)
    {
        int incremental = y - mLastY;
        mLastY = y;

        if (incremental != 0) trackMotionScroll(incremental);
    }

    private void cancelPendingPress()
    {
        if (mPendingCheckForTap != null) removeCallbacks(mPendingCheckForTap);
        if (mPendingCheckForLongPress != null) removeCallbacks(mPendingCheckForLongPress);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (!isEnabled()) return isClickable() || isLongClickable();

        if (mPositionScroller != null) mPositionScroller.stop();

        int action = event.getActionMasked();
        int x = (int)event.getX();
        int y = (int)event.getY();

        obtainVelocityTracker().addMovement(event);

        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                mTouchOwnedBySelf = true;
                onTouchDown(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(y);
                return true;
            case MotionEvent.ACTION_UP:
                mTouchOwnedBySelf = false;
                onTouchUp(x, y);
                return true;
            case MotionEvent.ACTION_CANCEL:
                mTouchOwnedBySelf = false;
                onTouchCancel();
                return true;
            default:
                return true;
        }
    }

    private void onTouchDown(int x, int y)
    {
        if (mTouchMode == TOUCH_MODE_SCROLL) return;

        mMotionX = x;
        mMotionY = y;
        mLastY = y;

        if (mTouchMode == TOUCH_MODE_FLING || !mScroller.isFinished())
        {
            stopFling();
            mTouchMode = TOUCH_MODE_SCROLL;
            mMotionPosition = pointToPosition(x, y);
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            return;
        }

        mMotionPosition = pointToPosition(x, y);
        mTouchMode = TOUCH_MODE_DOWN;

        if (mMotionPosition < 0 || mDataChanged || mAdapter == null) return;

        if (mPendingCheckForTap == null)
        {
            mPendingCheckForTap = new Runnable()
            {
                @Override
                public void run()
                {
                    onTapTimeout();
                }
            };
        }

        postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
    }

    private void onTapTimeout()
    {
        if (mTouchMode != TOUCH_MODE_DOWN) return;

        mTouchMode = TOUCH_MODE_TAP;
        View child = getChildAt(mMotionPosition - mFirstPosition);
        if (child == null) return;

        if (!mDataChanged && mAdapter != null && mMotionPosition < mAdapter.getCount() && mAdapter.isEnabled(mMotionPosition))
        {
            child.setPressed(true);
            setPressed(true);
            positionSelector(mMotionPosition);
            updateSelectorState();
            invalidate();
        }

        if (!isLongClickable())
        {
            mTouchMode = TOUCH_MODE_DONE_WAITING;
            return;
        }

        if (mPendingCheckForLongPress == null)
        {
            mPendingCheckForLongPress = new Runnable()
            {
                @Override
                public void run()
                {
                    onLongPressTimeout();
                }
            };
        }

        int delay = Math.max(0, ViewConfiguration.getLongPressTimeout() - ViewConfiguration.getTapTimeout());
        postDelayed(mPendingCheckForLongPress, delay);
    }

    private void onLongPressTimeout()
    {
        if (mTouchMode != TOUCH_MODE_TAP) return;

        int position = mMotionPosition;
        View child = getChildAt(position - mFirstPosition);
        if (child == null || mDataChanged || mAdapter == null || position >= mAdapter.getCount())
        {
            mTouchMode = TOUCH_MODE_DONE_WAITING;
            return;
        }

        boolean handled = performLongPress(child, position, mAdapter.getItemId(position));
        if (!handled)
        {
            mTouchMode = TOUCH_MODE_DONE_WAITING;
            return;
        }

        mTouchMode = TOUCH_MODE_REST;
        setPressed(false);
        child.setPressed(false);
        clearSelector();
    }

    private void onTouchMove(int y)
    {
        switch (mTouchMode)
        {
            case TOUCH_MODE_DOWN:
            case TOUCH_MODE_TAP:
            case TOUCH_MODE_DONE_WAITING:
                if (startScrollIfNeeded(y)) return;
                if (Math.abs(y - mMotionY) > mTouchSlop)
                {
                    cancelPendingPress();
                    setPressed(false);
                    View child = getChildAt(mMotionPosition - mFirstPosition);
                    if (child != null) child.setPressed(false);
                    clearSelector();
                    mTouchMode = TOUCH_MODE_DONE_WAITING;
                    mMotionPosition = INVALID_POSITION;
                }
                return;
            case TOUCH_MODE_SCROLL:
                scrollByTouch(y);
                return;
            default:
                return;
        }
    }

    private void onTouchUp(int x, int y)
    {
        switch (mTouchMode)
        {
            case TOUCH_MODE_DOWN:
            case TOUCH_MODE_TAP:
            case TOUCH_MODE_DONE_WAITING:
                performTouchClick(x, y);
                break;
            case TOUCH_MODE_SCROLL:
            {
                int velocity = 0;
                if (mVelocityTracker != null)
                {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    velocity = (int)(mVelocityTracker.getYVelocity() * mVelocityScale);
                }

                if (Math.abs(velocity) > mMinimumVelocity && getChildCount() > 0) startFling(velocity);
                else
                {
                    mTouchMode = TOUCH_MODE_REST;
                    reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                }
                break;
            }
            default:
                mTouchMode = TOUCH_MODE_REST;
                break;
        }

        setPressed(false);
        recycleVelocityTracker();

        android.view.ViewParent parent = getParent();
        if (parent != null) parent.requestDisallowInterceptTouchEvent(false);

        invalidate();
    }

    private void performTouchClick(int x, int y)
    {
        cancelPendingPress();

        final int position = mMotionPosition;
        final View child = getChildAt(position - mFirstPosition);
        boolean clickable = child != null && position >= 0 && !mDataChanged && mAdapter != null && position < mAdapter.getCount()
                            && mAdapter.isEnabled(position) && x >= 0 && x < getWidth();

        if (!clickable)
        {
            mTouchMode = TOUCH_MODE_REST;
            clearSelector();
            return;
        }

        final long id = mAdapter.getItemId(position);
        child.setPressed(true);
        positionSelector(position);
        mTouchMode = TOUCH_MODE_TAP;
        updateSelectorState();
        invalidate();

        if (mTouchModeReset != null) removeCallbacks(mTouchModeReset);

        mTouchModeReset = new Runnable()
        {
            @Override
            public void run()
            {
                mTouchModeReset = null;
                mTouchMode = TOUCH_MODE_REST;
                child.setPressed(false);
                setPressed(false);
                clearSelector();

                if (!mDataChanged && isAttachedToWindow() && mAdapter != null && position < mAdapter.getCount())
                {
                    performItemClick(child, position, id);
                }
            }
        };

        postDelayed(mTouchModeReset, ViewConfiguration.getPressedStateDuration());
    }

    private void onTouchCancel()
    {
        cancelPendingPress();
        mTouchMode = TOUCH_MODE_REST;
        setPressed(false);

        View child = getChildAt(mMotionPosition - mFirstPosition);
        if (child != null) child.setPressed(false);

        clearSelector();
        recycleVelocityTracker();
        reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
    }

    private VelocityTracker obtainVelocityTracker()
    {
        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
        return mVelocityTracker;
    }

    private void recycleVelocityTracker()
    {
        if (mVelocityTracker == null) return;

        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY)
    {
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (event.getActionMasked() == MotionEvent.ACTION_SCROLL && mTouchMode == TOUCH_MODE_REST)
        {
            float vertical = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            int delta = Math.round(vertical * mVerticalScrollFactor);

            if (delta != 0)
            {
                if (mPositionScroller != null) mPositionScroller.stop();
                stopFling();

                if (!trackMotionScroll(delta)) return true;
            }
        }

        return super.onGenericMotionEvent(event);
    }

    public void fling(int velocityY)
    {
        startFling(-velocityY);
    }

    void startFling(int velocity)
    {
        if (getChildCount() == 0) return;

        mScroller.fling(0, 0, 0, velocity, 0, 0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
        mLastFlingY = 0;
        mFlinging = true;
        mTouchMode = TOUCH_MODE_FLING;
        reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
        invalidate();
    }

    void startScroll(int distance, int duration)
    {
        if (getChildCount() == 0 || distance == 0) return;

        mScroller.startScroll(0, 0, 0, -distance, duration);
        mLastFlingY = 0;
        mFlinging = true;
        mTouchMode = TOUCH_MODE_FLING;
        reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
        invalidate();
    }

    void stopFling()
    {
        if (!mFlinging && mScroller.isFinished()) return;

        mScroller.abortAnimation();
        mFlinging = false;
        if (mTouchMode == TOUCH_MODE_FLING) mTouchMode = TOUCH_MODE_REST;
        reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
    }

    @Override
    public void computeScroll()
    {
        if (!mFlinging) return;

        if (!mScroller.computeScrollOffset())
        {
            mFlinging = false;
            if (mTouchMode == TOUCH_MODE_FLING) mTouchMode = TOUCH_MODE_REST;
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            return;
        }

        int y = mScroller.getCurrY();
        int delta = y - mLastFlingY;
        mLastFlingY = y;

        if (delta != 0 && trackMotionScroll(delta))
        {
            stopFling();
            return;
        }

        if (mScroller.isFinished())
        {
            mFlinging = false;
            if (mTouchMode == TOUCH_MODE_FLING) mTouchMode = TOUCH_MODE_REST;
            reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            return;
        }

        invalidate();
    }

    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes)
    {
        return false;
    }

    public void onNestedScrollAccepted(View child, View target, int axes)
    {
    }

    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed)
    {
        if (dyUnconsumed != 0) trackMotionScroll(-dyUnconsumed);
    }

    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed)
    {
        if (consumed) return false;

        fling((int)velocityY);
        return true;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept)
    {
        if (disallowIntercept) recycleVelocityTracker();
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    public boolean onInterceptHoverEvent(MotionEvent event)
    {
        return false;
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex)
    {
        return super.onResolvePointerIcon(event, pointerIndex);
    }

    @Override
    public void onProvideContentCaptureStructure(ViewStructure structure, int flags)
    {
        super.onProvideContentCaptureStructure(structure, flags);
    }

    public void addTouchables(ArrayList<View> views)
    {
        if (mAdapter == null) return;

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (mAdapter.isEnabled(mFirstPosition + index)) views.add(child);
        }
    }

    public void setFriction(float friction)
    {
    }

    public void setVelocityScale(float scale)
    {
        mVelocityScale = scale;
    }

    public void smoothScrollToPosition(int position)
    {
        positionScroller().start(position, INVALID_POSITION, 0, false, DEFAULT_SMOOTH_SCROLL_DURATION);
    }

    public void smoothScrollToPositionFromTop(int position, int offset, int duration)
    {
        positionScroller().start(position, INVALID_POSITION, offset, true, duration);
    }

    public void smoothScrollToPositionFromTop(int position, int offset)
    {
        positionScroller().start(position, INVALID_POSITION, offset, true, DEFAULT_SMOOTH_SCROLL_DURATION);
    }

    public void smoothScrollToPosition(int position, int boundPosition)
    {
        positionScroller().start(position, boundPosition, 0, false, DEFAULT_SMOOTH_SCROLL_DURATION);
    }

    public void smoothScrollBy(int distance, int duration)
    {
        if (mPositionScroller != null) mPositionScroller.stop();
        stopFling();
        startScroll(distance, duration);
    }

    private PositionScroller positionScroller()
    {
        if (mPositionScroller == null) mPositionScroller = new PositionScroller();
        return mPositionScroller;
    }

    public void scrollListBy(int y)
    {
        trackMotionScroll(-y);
    }

    public boolean canScrollList(int direction)
    {
        int childCount = getChildCount();
        if (childCount == 0) return false;

        if (direction > 0)
        {
            int lastPosition = mFirstPosition + childCount - 1;
            return lastPosition < mItemCount - 1 || lastRowBottom() > getHeight() - mListPadding.bottom;
        }

        return mFirstPosition > 0 || firstRowTop() < mListPadding.top;
    }

    @Override
    public boolean canScrollVertically(int direction)
    {
        return canScrollList(direction);
    }

    public void invalidateViews()
    {
        mDataChanged = true;
        if (getChildCount() > 0)
        {
            mSyncPosition = mFirstPosition;
            mSpecificTop = getChildAt(0).getTop() - mListPadding.top;
        }

        requestLayout();
        invalidate();
    }

    protected void handleDataChanged()
    {
        int count = mItemCount;
        if (count <= 0)
        {
            setSelectedPositionInt(INVALID_POSITION);
            setNextSelectedPositionInt(INVALID_POSITION);
            return;
        }

        if (mSelectedPosition >= count) setSelectedPositionInt(count - 1);
        if (mNextSelectedPosition >= count) setNextSelectedPositionInt(count - 1);
    }

    protected void onDisplayHint(int hint)
    {
    }

    protected boolean isInFilterMode()
    {
        return mFiltered;
    }

    @Override
    boolean isInFilterModeInternal()
    {
        return mFiltered;
    }

    @Override
    public android.view.inputmethod.InputConnection onCreateInputConnection(android.view.inputmethod.EditorInfo outAttributes)
    {
        return null;
    }

    public boolean checkInputConnectionProxy(View view)
    {
        return false;
    }

    public void clearTextFilter()
    {
        if (!mFiltered) return;

        mFiltered = false;
        mFilterText = null;

        if (mAdapter instanceof Filterable)
        {
            Filter filter = ((Filterable)mAdapter).getFilter();
            if (filter != null) filter.filter(null);
        }
    }

    public boolean hasTextFilter()
    {
        return mFiltered;
    }

    @Override
    public void onGlobalLayout()
    {
    }

    @Override
    public void beforeTextChanged(CharSequence text, int start, int count, int after)
    {
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count)
    {
        if (!mTextFilterEnabled || !(mAdapter instanceof Filterable)) return;

        Filter filter = ((Filterable)mAdapter).getFilter();
        if (filter == null) throw new IllegalStateException("You cannot call onTextChanged with a non filterable adapter");

        mFiltered = text != null && text.length() > 0;
        mFilterText = text;
        filter.filter(text, this);
    }

    @Override
    public void afterTextChanged(Editable text)
    {
    }

    @Override
    public void onFilterComplete(int count)
    {
        if (mSelectedPosition < 0 && count > 0) mResurrectToPosition = INVALID_POSITION;
        requestLayout();
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams()
    {
        return new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams params)
    {
        return new LayoutParams(params);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new AbsListView.LayoutParams(getContext(), attributes);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams params)
    {
        return params instanceof AbsListView.LayoutParams;
    }

    public void setTranscriptMode(int mode)
    {
        mTranscriptMode = mode;
    }

    public int getTranscriptMode()
    {
        return mTranscriptMode;
    }

    public int getSolidColor()
    {
        return mCacheColorHint;
    }

    public void setCacheColorHint(int color)
    {
        mCacheColorHint = color;
    }

    public int getCacheColorHint()
    {
        return mCacheColorHint;
    }

    public void reclaimViews(List<View> views)
    {
        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            ViewGroup.LayoutParams params = child.getLayoutParams();
            if (params instanceof LayoutParams && mRecycler.shouldRecycleViewType(((LayoutParams)params).viewType)) views.add(child);
        }

        mRecycler.reclaimScrapViews(views);
        removeAllViewsInLayout();
    }

    public void setRemoteViewsAdapter(Intent intent)
    {
        android.util.Log.w("AbsListView", "remote views adapters are not supported, so the list stays empty");
    }

    public void deferNotifyDataSetChanged()
    {
    }

    public boolean onRemoteAdapterConnected()
    {
        return false;
    }

    public void onRemoteAdapterDisconnected()
    {
    }

    public void setEdgeEffectColor(int color)
    {
        setTopEdgeEffectColor(color);
        setBottomEdgeEffectColor(color);
    }

    public void setBottomEdgeEffectColor(int color)
    {
        mBottomEdgeEffectColor = color;
    }

    public void setTopEdgeEffectColor(int color)
    {
        mTopEdgeEffectColor = color;
    }

    public int getTopEdgeEffectColor()
    {
        return mTopEdgeEffectColor;
    }

    public int getBottomEdgeEffectColor()
    {
        return mBottomEdgeEffectColor;
    }

    public void setRecyclerListener(RecyclerListener listener)
    {
        mRecyclerListener = listener;
    }

    public void setSelectionFromTop(int position, int y)
    {
        if (mAdapter == null) return;

        if (!isInTouchMode())
        {
            if (position >= 0 && position < mItemCount) setNextSelectedPositionInt(position);
        }
        else
        {
            mResurrectToPosition = position;
        }

        if (position < 0) return;

        if (mPositionScroller != null) mPositionScroller.stop();
        stopFling();

        mSyncPosition = position;
        mSpecificTop = y;
        requestLayout();
    }

    public interface OnScrollListener
    {
        int SCROLL_STATE_IDLE = 0;
        int SCROLL_STATE_TOUCH_SCROLL = 1;
        int SCROLL_STATE_FLING = 2;

        void onScrollStateChanged(AbsListView view, int scrollState);

        void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount);
    }

    public interface SelectionBoundsAdjuster
    {
        void adjustListItemSelectionBounds(Rect bounds);
    }

    public interface RecyclerListener
    {
        void onMovedToScrapHeap(View view);
    }

    public interface MultiChoiceModeListener extends ActionMode.Callback
    {
        void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams
    {
        int viewType;
        boolean recycledHeaderFooter;
        boolean forceAdd;
        int scrappedFromPosition;
        long itemId = -1;

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);
        }

        public LayoutParams(int width, int height)
        {
            super(width, height);
        }

        public LayoutParams(int width, int height, int viewType)
        {
            super(width, height);
            this.viewType = viewType;
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }
    }

    final class RecycleBin
    {
        private final HashMap<Integer, View> mActiveViews = new HashMap<>();
        private final ArrayList<View> mTransientViews = new ArrayList<>();
        private ArrayList<View>[] mScrapViews = newScrapArray(1);

        @SuppressWarnings("unchecked")
        private ArrayList<View>[] newScrapArray(int count)
        {
            ArrayList<View>[] scrap = new ArrayList[Math.max(1, count)];
            for (int index = 0; index < scrap.length; index++) scrap[index] = new ArrayList<View>();
            return scrap;
        }

        void setViewTypeCount(int viewTypeCount)
        {
            if (viewTypeCount < 1) throw new IllegalArgumentException("Can't have a viewTypeCount < 1");

            clear();
            mScrapViews = newScrapArray(viewTypeCount);
        }

        boolean shouldRecycleViewType(int viewType)
        {
            return viewType >= 0 && viewType < mScrapViews.length;
        }

        void fillActiveViews(int firstPosition)
        {
            int childCount = getChildCount();
            View[] children = new View[childCount];
            for (int index = 0; index < childCount; index++) children[index] = getChildAt(index);

            detachAllViewsFromParent();

            for (int index = 0; index < childCount; index++) mActiveViews.put(firstPosition + index, children[index]);
        }

        View takeActiveView(int position)
        {
            return mActiveViews.remove(position);
        }

        View takeScrapView(int viewType)
        {
            ArrayList<View> scrap = mScrapViews[viewType];
            if (scrap.isEmpty()) return null;

            return scrap.remove(scrap.size() - 1);
        }

        void addScrapView(View scrap, int position)
        {
            ViewGroup.LayoutParams raw = scrap.getLayoutParams();
            int viewType = raw instanceof LayoutParams ? ((LayoutParams)raw).viewType : ITEM_VIEW_TYPE_IGNORE;

            if (raw instanceof LayoutParams) ((LayoutParams)raw).scrappedFromPosition = position;

            scrap.setPressed(false);

            if (!shouldRecycleViewType(viewType))
            {
                if (!mTransientViews.contains(scrap)) mTransientViews.add(scrap);
                return;
            }

            ArrayList<View> list = mScrapViews[viewType];
            if (!list.contains(scrap)) list.add(scrap);

            if (mRecyclerListener != null) mRecyclerListener.onMovedToScrapHeap(scrap);
        }

        void forget(View view)
        {
            mTransientViews.remove(view);

            for (ArrayList<View> list : mScrapViews) list.remove(view);

            java.util.Iterator<java.util.Map.Entry<Integer, View>> iterator = mActiveViews.entrySet().iterator();
            while (iterator.hasNext())
            {
                if (iterator.next().getValue() == view) iterator.remove();
            }
        }

        void scrapActiveViews()
        {
            if (mActiveViews.isEmpty()) return;

            ArrayList<java.util.Map.Entry<Integer, View>> leftovers = new ArrayList<>(mActiveViews.entrySet());
            mActiveViews.clear();

            for (java.util.Map.Entry<Integer, View> entry : leftovers) addScrapView(entry.getValue(), entry.getKey());
        }

        void flushTransient()
        {
            if (mTransientViews.isEmpty()) return;

            ArrayList<View> leftovers = new ArrayList<>(mTransientViews);
            mTransientViews.clear();

            for (View view : leftovers) release(view);
        }

        void prune(int visibleCount)
        {
            int limit = Math.max(4, visibleCount * 2 + 2);

            for (ArrayList<View> list : mScrapViews)
            {
                while (list.size() > limit) release(list.remove(0));
            }
        }

        void clear()
        {
            ArrayList<View> leftovers = new ArrayList<>(mTransientViews);
            leftovers.addAll(mActiveViews.values());
            for (ArrayList<View> list : mScrapViews)
            {
                leftovers.addAll(list);
                list.clear();
            }

            mTransientViews.clear();
            mActiveViews.clear();

            for (View view : leftovers) release(view);
        }

        void reclaimScrapViews(List<View> views)
        {
            for (ArrayList<View> list : mScrapViews)
            {
                views.addAll(list);
                for (View view : list) release(view);
                list.clear();
            }
        }

        private void release(View view)
        {
            if (view.getParent() == AbsListView.this && indexOfChild(view) < 0) removeDetachedView(view, false);
        }
    }

    private final class PositionScroller implements Runnable
    {
        private int mTargetPosition;
        private int mBoundPosition;
        private int mOffset;
        private boolean mAlignTop;
        private int mDuration;

        void start(int position, int boundPosition, int offset, boolean alignTop, int duration)
        {
            stop();
            stopFling();

            if (mAdapter == null || mItemCount == 0) return;

            mTargetPosition = Math.max(0, Math.min(mItemCount - 1, position));
            mBoundPosition = boundPosition;
            mOffset = offset;
            mAlignTop = alignTop;
            mDuration = duration;

            if (getChildCount() == 0 || isLayoutRequested() && !isLaidOut())
            {
                setSelectionFromTop(mTargetPosition, mOffset);
                return;
            }

            postOnAnimation(this);
        }

        void stop()
        {
            removeCallbacks(this);
        }

        @Override
        public void run()
        {
            int childCount = getChildCount();
            if (childCount == 0 || mAdapter == null) return;

            int first = mFirstPosition;
            int last = first + childCount - 1;
            int listTop = mListPadding.top;
            int listBottom = getHeight() - mListPadding.bottom;
            int viewport = Math.max(1, listBottom - listTop);

            if (mTargetPosition >= first && mTargetPosition <= last)
            {
                View child = getChildAt(mTargetPosition - first);
                int distance;

                if (mAlignTop) distance = child.getTop() - (listTop + mOffset);
                else if (child.getTop() < listTop) distance = child.getTop() - listTop;
                else if (child.getBottom() > listBottom) distance = Math.min(child.getBottom() - listBottom, child.getTop() - listTop);
                else distance = 0;

                if (!mAlignTop && mBoundPosition >= first && mBoundPosition <= last && distance > 0)
                {
                    View bound = getChildAt(mBoundPosition - first);
                    distance = Math.min(distance, Math.max(0, bound.getTop() - listTop));
                }

                if (distance != 0) startScroll(distance, Math.max(1, mDuration));
                return;
            }

            int step = Math.max(viewport / 3, 1);
            boolean stuck = mTargetPosition > last ? trackMotionScroll(-step) : trackMotionScroll(step);
            if (stuck) return;

            postOnAnimation(this);
        }
    }

    private final class ChoiceActionMode extends ActionMode
    {
        private final RuntimeMenu mMenu = new RuntimeMenu(getContext());
        private CharSequence mTitle;
        private CharSequence mSubtitle;
        private View mCustomView;
        private boolean mFinished;

        @Override
        public void setTitle(CharSequence title)
        {
            mTitle = title;
        }

        @Override
        public void setTitle(int resId)
        {
            mTitle = getContext().getText(resId);
        }

        @Override
        public void setSubtitle(CharSequence subtitle)
        {
            mSubtitle = subtitle;
        }

        @Override
        public void setSubtitle(int resId)
        {
            mSubtitle = getContext().getText(resId);
        }

        @Override
        public void setCustomView(View view)
        {
            mCustomView = view;
        }

        @Override
        public void invalidate()
        {
            if (mMultiChoiceModeListener != null) mMultiChoiceModeListener.onPrepareActionMode(this, mMenu);
        }

        @Override
        public void finish()
        {
            if (mFinished) return;

            mFinished = true;
            if (mMultiChoiceModeListener != null) mMultiChoiceModeListener.onDestroyActionMode(this);
            if (mChoiceActionMode == this) mChoiceActionMode = null;

            clearChoices();
            mDataChanged = true;
            requestLayout();
            setLongClickable(true);
        }

        @Override
        public Menu getMenu()
        {
            return mMenu;
        }

        @Override
        public CharSequence getTitle()
        {
            return mTitle;
        }

        @Override
        public CharSequence getSubtitle()
        {
            return mSubtitle;
        }

        @Override
        public View getCustomView()
        {
            return mCustomView;
        }

        @Override
        public MenuInflater getMenuInflater()
        {
            return new MenuInflater(getContext());
        }
    }

    static class SavedState extends AbsSavedState
    {
        int firstPosition;
        int viewTop;
        int checkedItemCount;
        String filter;
        int[] checkedPositions;
        boolean[] checkedValues;
        long[] checkedIds;
        int[] checkedIdPositions;

        SavedState(Parcelable superState)
        {
            super(superState == null ? AbsSavedState.EMPTY_STATE : superState);
        }

        private SavedState(Parcel source)
        {
            super(source);
            firstPosition = source.readInt();
            viewTop = source.readInt();
            checkedItemCount = source.readInt();
            filter = source.readString();

            int checkedCount = source.readInt();
            if (checkedCount >= 0)
            {
                checkedPositions = new int[checkedCount];
                checkedValues = new boolean[checkedCount];
                for (int index = 0; index < checkedCount; index++)
                {
                    checkedPositions[index] = source.readInt();
                    checkedValues[index] = source.readInt() != 0;
                }
            }

            int idCount = source.readInt();
            if (idCount >= 0)
            {
                checkedIds = new long[idCount];
                checkedIdPositions = new int[idCount];
                for (int index = 0; index < idCount; index++)
                {
                    checkedIds[index] = source.readLong();
                    checkedIdPositions[index] = source.readInt();
                }
            }
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeInt(firstPosition);
            out.writeInt(viewTop);
            out.writeInt(checkedItemCount);
            out.writeString(filter);

            out.writeInt(checkedPositions == null ? -1 : checkedPositions.length);
            if (checkedPositions != null)
            {
                for (int index = 0; index < checkedPositions.length; index++)
                {
                    out.writeInt(checkedPositions[index]);
                    out.writeInt(checkedValues[index] ? 1 : 0);
                }
            }

            out.writeInt(checkedIds == null ? -1 : checkedIds.length);
            if (checkedIds != null)
            {
                for (int index = 0; index < checkedIds.length; index++)
                {
                    out.writeLong(checkedIds[index]);
                    out.writeInt(checkedIdPositions[index]);
                }
            }
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
