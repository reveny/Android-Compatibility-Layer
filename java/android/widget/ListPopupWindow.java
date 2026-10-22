package android.widget;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public class ListPopupWindow
{
    public static final int POSITION_PROMPT_ABOVE = 0;
    public static final int POSITION_PROMPT_BELOW = 1;
    public static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    public static final int INPUT_METHOD_FROM_FOCUSABLE = PopupWindow.INPUT_METHOD_FROM_FOCUSABLE;
    public static final int INPUT_METHOD_NEEDED = PopupWindow.INPUT_METHOD_NEEDED;
    public static final int INPUT_METHOD_NOT_NEEDED = PopupWindow.INPUT_METHOD_NOT_NEEDED;

    private final Context mContext;
    private final PopupWindow mPopup;
    private ListAdapter mAdapter;
    private ListView mDropDownList;
    private View mContentView;

    private int mDropDownHeight = WRAP_CONTENT;
    private int mDropDownWidth = WRAP_CONTENT;
    private int mDropDownHorizontalOffset;
    private int mDropDownVerticalOffset;
    private int mDropDownWindowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL;
    private int mDropDownGravity = Gravity.NO_GRAVITY;
    private boolean mDropDownAlwaysVisible;
    private boolean mForceIgnoreOutsideTouch;
    private boolean mModal;

    private View mPromptView;
    private int mPromptPosition = POSITION_PROMPT_ABOVE;
    private View mDropDownAnchorView;
    private Drawable mDropDownListHighlight;
    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemSelectedListener mItemSelectedListener;
    private int mSelectedPosition = AdapterView.INVALID_POSITION;
    private Rect mEpicenterBounds;
    private DataSetObserver mObserver;

    public ListPopupWindow(Context context)
    {
        this(context, null, 0, 0);
    }

    public ListPopupWindow(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0, 0);
    }

    public ListPopupWindow(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public ListPopupWindow(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        mContext = context;
        mPopup = new PopupWindow(context, attributes, defStyleAttr, defStyleRes);
        mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

        if (mPopup.getBackground() == null) mPopup.setBackgroundDrawable(PopupWindow.defaultPopupBackground(context));

        if (mPopup.getElevation() == 0) mPopup.setElevation(8 * context.getResources().getDisplayMetrics().density);
    }

    public void setAdapter(ListAdapter adapter)
    {
        if (mObserver == null)
        {
            mObserver = new DataSetObserver()
            {
                @Override
                public void onChanged()
                {
                    if (isShowing()) show();
                }

                @Override
                public void onInvalidated()
                {
                    dismiss();
                }
            };
        }
        else if (mAdapter != null)
        {
            mAdapter.unregisterDataSetObserver(mObserver);
        }

        mAdapter = adapter;
        if (adapter != null) adapter.registerDataSetObserver(mObserver);

        if (mDropDownList != null) mDropDownList.setAdapter(mAdapter);
    }

    public void setPromptPosition(int position)
    {
        mPromptPosition = position;
    }

    public int getPromptPosition()
    {
        return mPromptPosition;
    }

    public void setModal(boolean modal)
    {
        mModal = modal;
        mPopup.setFocusable(modal);
    }

    public boolean isModal()
    {
        return mModal;
    }

    public void setForceIgnoreOutsideTouch(boolean forceIgnoreOutsideTouch)
    {
        mForceIgnoreOutsideTouch = forceIgnoreOutsideTouch;
    }

    public void setDropDownAlwaysVisible(boolean dropDownAlwaysVisible)
    {
        mDropDownAlwaysVisible = dropDownAlwaysVisible;
    }

    public boolean isDropDownAlwaysVisible()
    {
        return mDropDownAlwaysVisible;
    }

    public void setSoftInputMode(int mode)
    {
        mPopup.setSoftInputMode(mode);
    }

    public int getSoftInputMode()
    {
        return mPopup.getSoftInputMode();
    }

    public void setListSelector(Drawable selector)
    {
        mDropDownListHighlight = selector;
        if (mDropDownList != null && selector != null) mDropDownList.setSelector(selector);
    }

    public Drawable getBackground()
    {
        return mPopup.getBackground();
    }

    public void setBackgroundDrawable(Drawable drawable)
    {
        mPopup.setBackgroundDrawable(drawable);
    }

    public void setAnimationStyle(int animationStyle)
    {
        mPopup.setAnimationStyle(animationStyle);
    }

    public int getAnimationStyle()
    {
        return mPopup.getAnimationStyle();
    }

    public View getAnchorView()
    {
        return mDropDownAnchorView;
    }

    public void setAnchorView(View anchor)
    {
        mDropDownAnchorView = anchor;
    }

    public int getHorizontalOffset()
    {
        return mDropDownHorizontalOffset;
    }

    public void setHorizontalOffset(int offset)
    {
        mDropDownHorizontalOffset = offset;
    }

    public int getVerticalOffset()
    {
        return mDropDownVerticalOffset;
    }

    public void setVerticalOffset(int offset)
    {
        mDropDownVerticalOffset = offset;
    }

    public void setEpicenterBounds(Rect bounds)
    {
        mEpicenterBounds = bounds == null ? null : new Rect(bounds);
    }

    public Rect getEpicenterBounds()
    {
        return mEpicenterBounds == null ? null : new Rect(mEpicenterBounds);
    }

    public void setDropDownGravity(int gravity)
    {
        mDropDownGravity = gravity;
    }

    public int getWidth()
    {
        return mDropDownWidth;
    }

    public void setWidth(int width)
    {
        mDropDownWidth = width;
    }

    public void setContentWidth(int width)
    {
        Drawable background = mPopup.getBackground();
        if (background != null)
        {
            Rect padding = new Rect();
            background.getPadding(padding);
            mDropDownWidth = padding.left + padding.right + width;
            return;
        }

        setWidth(width);
    }

    public int getHeight()
    {
        return mDropDownHeight;
    }

    public void setHeight(int height)
    {
        if (height < 0 && height != WRAP_CONTENT && height != MATCH_PARENT)
        {
            throw new IllegalArgumentException("Invalid height. Must be a positive value, MATCH_PARENT, or WRAP_CONTENT.");
        }

        mDropDownHeight = height;
    }

    public void setWindowLayoutType(int layoutType)
    {
        mDropDownWindowLayoutType = layoutType;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener)
    {
        mItemClickListener = listener;
        if (mDropDownList != null) mDropDownList.setOnItemClickListener(listener);
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener selectedListener)
    {
        mItemSelectedListener = selectedListener;
        if (mDropDownList != null) mDropDownList.setOnItemSelectedListener(selectedListener);
    }

    public void setPromptView(View prompt)
    {
        boolean showing = isShowing();
        if (showing) removePromptView();

        mPromptView = prompt;
        mContentView = null;

        if (showing) show();
    }

    private void removePromptView()
    {
        if (mPromptView == null) return;

        android.view.ViewParent parent = mPromptView.getParent();
        if (parent instanceof ViewGroup) ((ViewGroup)parent).removeView(mPromptView);
    }

    public void postShow()
    {
        View anchor = getAnchorView();
        if (anchor == null)
        {
            show();
            return;
        }

        anchor.post(new Runnable()
        {
            @Override
            public void run()
            {
                show();
            }
        });
    }

    public void show()
    {
        View anchor = getAnchorView();
        if (anchor == null) return;

        int height = buildDropDown();
        boolean noInputMethod = isInputMethodNotNeeded();

        mPopup.setWindowLayoutType(mDropDownWindowLayoutType);
        mPopup.setEpicenterBounds(mEpicenterBounds);

        int widthSpec;
        if (mDropDownWidth == MATCH_PARENT) widthSpec = MATCH_PARENT;
        else if (mDropDownWidth == WRAP_CONTENT) widthSpec = anchor.getWidth();
        else widthSpec = mDropDownWidth;

        int heightSpec;
        if (mDropDownHeight == MATCH_PARENT) heightSpec = noInputMethod ? height : MATCH_PARENT;
        else if (mDropDownHeight == WRAP_CONTENT) heightSpec = height;
        else heightSpec = mDropDownHeight;

        if (mPopup.isShowing())
        {
            mPopup.setOutsideTouchable(!mForceIgnoreOutsideTouch && !mDropDownAlwaysVisible);
            if (widthSpec == MATCH_PARENT) mPopup.setWidth(MATCH_PARENT);
            if (heightSpec == MATCH_PARENT) mPopup.setHeight(MATCH_PARENT);

            mPopup.update(anchor, mDropDownHorizontalOffset, mDropDownVerticalOffset, widthSpec < 0 ? -1 : widthSpec,
                          heightSpec < 0 ? -1 : heightSpec);
            return;
        }

        mPopup.setWidth(widthSpec);
        mPopup.setHeight(heightSpec);
        mPopup.setOutsideTouchable(!mForceIgnoreOutsideTouch && !mDropDownAlwaysVisible);
        mPopup.setTouchModal(mModal);
        mPopup.showAsDropDown(anchor, mDropDownHorizontalOffset, mDropDownVerticalOffset, mDropDownGravity);

        if (!mModal || mDropDownList.isInTouchMode()) clearListSelection();
        if (mSelectedPosition >= 0) mDropDownList.setSelection(mSelectedPosition);
    }

    public void dismiss()
    {
        mPopup.dismiss();
        removePromptView();
        mPopup.setContentView(null);
        mContentView = null;
        mDropDownList = null;
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener listener)
    {
        mPopup.setOnDismissListener(listener);
    }

    public void setInputMethodMode(int mode)
    {
        mPopup.setInputMethodMode(mode);
    }

    public int getInputMethodMode()
    {
        return mPopup.getInputMethodMode();
    }

    public void setSelection(int position)
    {
        mSelectedPosition = position;

        ListView list = mDropDownList;
        if (!isShowing() || list == null) return;

        if (position >= 0 && position < list.getCount())
        {
            int firstVisible = list.getFirstVisiblePosition();
            int lastVisible = list.getLastVisiblePosition();
            if (position < firstVisible || position > lastVisible || list.getChildCount() == 0) list.setSelection(position);
        }

        highlightSelection();
    }

    private void highlightSelection()
    {
        ListView list = mDropDownList;
        if (list == null) return;

        for (int index = 0; index < list.getChildCount(); index++)
        {
            list.getChildAt(index).setSelected(list.getFirstVisiblePosition() + index == mSelectedPosition);
        }
    }

    public void clearListSelection()
    {
        mSelectedPosition = AdapterView.INVALID_POSITION;
        highlightSelection();
    }

    public boolean isShowing()
    {
        return mPopup.isShowing();
    }

    public boolean isInputMethodNotNeeded()
    {
        return mPopup.getInputMethodMode() == INPUT_METHOD_NOT_NEEDED;
    }

    public boolean performItemClick(int position)
    {
        if (!isShowing() || mDropDownList == null || mAdapter == null) return false;
        if (position < 0 || position >= mAdapter.getCount()) return false;

        if (mItemClickListener != null)
        {
            View child = mDropDownList.getChildAt(position - mDropDownList.getFirstVisiblePosition());
            mItemClickListener.onItemClick(mDropDownList, child, position, mAdapter.getItemId(position));
        }

        return true;
    }

    public Object getSelectedItem()
    {
        if (!isShowing() || mAdapter == null || mSelectedPosition < 0 || mSelectedPosition >= mAdapter.getCount()) return null;
        return mAdapter.getItem(mSelectedPosition);
    }

    public int getSelectedItemPosition()
    {
        if (!isShowing()) return ListView.INVALID_POSITION;
        return mSelectedPosition;
    }

    public long getSelectedItemId()
    {
        if (getSelectedItem() == null) return ListView.INVALID_ROW_ID;
        return mAdapter.getItemId(mSelectedPosition);
    }

    public View getSelectedView()
    {
        if (!isShowing() || mDropDownList == null || mSelectedPosition < 0) return null;
        return mDropDownList.getChildAt(mSelectedPosition - mDropDownList.getFirstVisiblePosition());
    }

    public ListView getListView()
    {
        return mDropDownList;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (!isShowing() || mAdapter == null) return false;

        int count = mAdapter.getCount();
        if (count == 0) return false;

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP)
        {
            boolean down = keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
            int next = mSelectedPosition < 0 ? (down ? 0 : count - 1) : mSelectedPosition + (down ? 1 : -1);

            while (next >= 0 && next < count && !mAdapter.isEnabled(next)) next += down ? 1 : -1;

            if (next < 0 || next >= count)
            {
                clearListSelection();
                return true;
            }

            setSelection(next);
            return true;
        }

        return keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
               ? mSelectedPosition >= 0 : false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (!isShowing() || mSelectedPosition < 0) return false;

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
        {
            boolean consumed = performItemClick(mSelectedPosition);
            if (consumed) dismiss();
            return consumed;
        }

        return false;
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) && isShowing())
        {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) dismiss();
            return true;
        }

        return false;
    }

    public View.OnTouchListener createDragToOpenListener(View src)
    {
        return new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent event)
            {
                return false;
            }
        };
    }

    private int buildDropDown()
    {
        int otherHeights = 0;

        if (mContentView == null || mDropDownList == null)
        {
            mDropDownList = new ListView(mContext);
            mDropDownList.setFocusable(true);
            mDropDownList.setFocusableInTouchMode(true);
            if (mDropDownListHighlight != null) mDropDownList.setSelector(mDropDownListHighlight);
            mDropDownList.setAdapter(mAdapter);
            mDropDownList.setOnItemClickListener(mItemClickListener);
            if (mItemSelectedListener != null) mDropDownList.setOnItemSelectedListener(mItemSelectedListener);

            View content = mDropDownList;

            if (mPromptView != null)
            {
                removePromptView();

                LinearLayout layout = new LinearLayout(mContext);
                layout.setOrientation(LinearLayout.VERTICAL);

                LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
                LinearLayout.LayoutParams promptParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                                       ViewGroup.LayoutParams.WRAP_CONTENT);

                if (mPromptPosition == POSITION_PROMPT_BELOW)
                {
                    layout.addView(mDropDownList, listParams);
                    layout.addView(mPromptView, promptParams);
                }
                else
                {
                    layout.addView(mPromptView, promptParams);
                    layout.addView(mDropDownList, listParams);
                }

                int promptWidthSpec = mDropDownWidth >= 0 ? View.MeasureSpec.makeMeasureSpec(mDropDownWidth, View.MeasureSpec.AT_MOST)
                                                          : View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                mPromptView.measure(promptWidthSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                otherHeights = mPromptView.getMeasuredHeight() + promptParams.topMargin + promptParams.bottomMargin;

                content = layout;
            }

            mContentView = content;
            mPopup.setContentView(content);
        }
        else if (mPromptView != null)
        {
            otherHeights = mPromptView.getMeasuredHeight();
        }

        int padding = 0;
        Rect backgroundPadding = new Rect();
        Drawable background = mPopup.getBackground();
        if (background != null && background.getPadding(backgroundPadding)) padding = backgroundPadding.top + backgroundPadding.bottom;

        View anchor = getAnchorView();
        boolean ignoreBottomDecorations = mPopup.getInputMethodMode() == PopupWindow.INPUT_METHOD_NOT_NEEDED;
        int maxHeight = mPopup.getMaxAvailableHeight(anchor, mDropDownVerticalOffset, ignoreBottomDecorations);

        if (mDropDownAlwaysVisible || mDropDownHeight == MATCH_PARENT) return maxHeight + padding;

        int displayWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int outerWidth;
        if (mDropDownWidth == WRAP_CONTENT) outerWidth = anchor != null && anchor.getWidth() > 0 ? anchor.getWidth() : displayWidth;
        else if (mDropDownWidth == MATCH_PARENT) outerWidth = displayWidth;
        else outerWidth = mDropDownWidth;

        int innerWidth = Math.max(0, outerWidth - backgroundPadding.left - backgroundPadding.right);
        int childWidthSpec = View.MeasureSpec.makeMeasureSpec(innerWidth, View.MeasureSpec.EXACTLY);

        int listContent = mDropDownList.measureHeightOfChildren(childWidthSpec, 0, ListView.NO_POSITION, maxHeight - otherHeights, -1);
        if (listContent > 0) otherHeights += padding;

        return listContent + otherHeights;
    }
}
