package android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;

public class AutoCompleteTextView extends EditText implements Filter.FilterListener
{
    private static final int DEFAULT_THRESHOLD = 2;
    private static final int DISPLAY_HINT_INVISIBLE = 4;

    private static StyleableSet sStyleable;

    private CharSequence mHintText;
    private TextView mHintView;
    private ListAdapter mAdapter;
    private Filter mFilter;
    private int mThreshold = DEFAULT_THRESHOLD;
    private ListPopupWindow mPopup;
    private int mDropDownAnchorId = View.NO_ID;
    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemSelectedListener mItemSelectedListener;
    private boolean mDropDownDismissedOnCompletion = true;
    private int mLastKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private Validator mValidator;
    private boolean mBlockCompletion;
    private boolean mPopupCanBeUpdated = true;
    private boolean mOpenBefore;
    private DataSetObserver mObserver;
    private OnDismissListener mOnDismissListener;

    public AutoCompleteTextView(Context context)
    {
        this(context, null);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        this(context, attributes, defStyleAttr, defStyleRes, null);
    }

    public AutoCompleteTextView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes, Resources.Theme popupTheme)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        Context popupContext = popupTheme != null ? new ContextThemeWrapper(context, popupTheme) : context;

        mPopup = new ListPopupWindow(popupContext, attributes, defStyleAttr, defStyleRes);
        mPopup.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        mPopup.setPromptPosition(ListPopupWindow.POSITION_PROMPT_BELOW);
        mPopup.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                performCompletion(view, position, id);
            }
        });
        mPopup.setOnDismissListener(new PopupWindow.OnDismissListener()
        {
            @Override
            public void onDismiss()
            {
                if (mOnDismissListener != null) mOnDismissListener.onDismiss();
            }
        });

        addTextChangedListener(new CompletionWatcher());

        if (attributes != null) readAttributes(context, attributes, defStyleAttr, defStyleRes);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "completionThreshold", "completionHint", "dropDownWidth", "dropDownHeight",
                                         "dropDownAnchor", "dropDownSelector", "popupBackground");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int index = sStyleable.indexOf("completionThreshold");
        if (index >= 0) mThreshold = array.getInt(index, DEFAULT_THRESHOLD);

        index = sStyleable.indexOf("completionHint");
        if (index >= 0 && array.hasValue(index)) setCompletionHint(array.getText(index));

        index = sStyleable.indexOf("dropDownWidth");
        if (index >= 0 && array.hasValue(index)) mPopup.setWidth(array.getLayoutDimension(index, ViewGroup.LayoutParams.WRAP_CONTENT));

        index = sStyleable.indexOf("dropDownHeight");
        if (index >= 0 && array.hasValue(index)) mPopup.setHeight(array.getLayoutDimension(index, ViewGroup.LayoutParams.WRAP_CONTENT));

        index = sStyleable.indexOf("dropDownAnchor");
        if (index >= 0) mDropDownAnchorId = array.getResourceId(index, View.NO_ID);

        index = sStyleable.indexOf("dropDownSelector");
        if (index >= 0 && array.hasValue(index))
        {
            Drawable selector = safeDrawable(array, index);
            if (selector != null) mPopup.setListSelector(selector);
        }

        index = sStyleable.indexOf("popupBackground");
        if (index >= 0 && array.hasValue(index))
        {
            Drawable background = safeDrawable(array, index);
            if (background != null) mPopup.setBackgroundDrawable(background);
        }

        array.recycle();
    }

    private static Drawable safeDrawable(TypedArray array, int index)
    {
        try
        {
            return array.getDrawable(index);
        }
        catch (RuntimeException unsupported)
        {
            return null;
        }
    }

    @Override
    public void setOnClickListener(View.OnClickListener listener)
    {
        super.setOnClickListener(listener);
    }

    public void setCompletionHint(CharSequence hint)
    {
        mHintText = hint;

        if (hint == null)
        {
            mPopup.setPromptView(null);
            mHintView = null;
            return;
        }

        if (mHintView == null)
        {
            float density = getContext().getResources().getDisplayMetrics().density;
            int padding = (int)(10 * density);

            mHintView = new TextView(getContext());
            mHintView.setPadding(padding, padding, padding, padding);
            mHintView.setTextSize(14);
        }

        mHintView.setText(hint);
        mPopup.setPromptView(mHintView);
    }

    public CharSequence getCompletionHint()
    {
        return mHintText;
    }

    public int getDropDownWidth()
    {
        return mPopup.getWidth();
    }

    public void setDropDownWidth(int width)
    {
        mPopup.setWidth(width);
    }

    public int getDropDownHeight()
    {
        return mPopup.getHeight();
    }

    public void setDropDownHeight(int height)
    {
        mPopup.setHeight(height);
    }

    public int getDropDownAnchor()
    {
        return mDropDownAnchorId;
    }

    public void setDropDownAnchor(int id)
    {
        mDropDownAnchorId = id;
        mPopup.setAnchorView(null);
    }

    public Drawable getDropDownBackground()
    {
        return mPopup.getBackground();
    }

    public void setDropDownBackgroundDrawable(Drawable drawable)
    {
        mPopup.setBackgroundDrawable(drawable);
    }

    public void setDropDownBackgroundResource(int id)
    {
        mPopup.setBackgroundDrawable(getContext().getDrawable(id));
    }

    public void setDropDownVerticalOffset(int offset)
    {
        mPopup.setVerticalOffset(offset);
    }

    public int getDropDownVerticalOffset()
    {
        return mPopup.getVerticalOffset();
    }

    public void setDropDownHorizontalOffset(int offset)
    {
        mPopup.setHorizontalOffset(offset);
    }

    public int getDropDownHorizontalOffset()
    {
        return mPopup.getHorizontalOffset();
    }

    public int getThreshold()
    {
        return mThreshold;
    }

    public void setThreshold(int threshold)
    {
        mThreshold = threshold <= 0 ? 1 : threshold;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener)
    {
        mItemClickListener = listener;
    }

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener)
    {
        mItemSelectedListener = listener;
        mPopup.setOnItemSelectedListener(listener);
    }

    @Deprecated
    public AdapterView.OnItemClickListener getItemClickListener()
    {
        return mItemClickListener;
    }

    @Deprecated
    public AdapterView.OnItemSelectedListener getItemSelectedListener()
    {
        return mItemSelectedListener;
    }

    public AdapterView.OnItemClickListener getOnItemClickListener()
    {
        return mItemClickListener;
    }

    public AdapterView.OnItemSelectedListener getOnItemSelectedListener()
    {
        return mItemSelectedListener;
    }

    public void setOnDismissListener(OnDismissListener dismissListener)
    {
        mOnDismissListener = dismissListener;
    }

    public ListAdapter getAdapter()
    {
        return mAdapter;
    }

    public <T extends ListAdapter & Filterable> void setAdapter(T adapter)
    {
        if (mObserver == null)
        {
            mObserver = new DataSetObserver()
            {
                @Override
                public void onChanged()
                {
                    post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (mAdapter != null) updateDropDownForFilter(mAdapter.getCount());
                        }
                    });
                }
            };
        }
        else if (mAdapter != null)
        {
            mAdapter.unregisterDataSetObserver(mObserver);
        }

        mAdapter = adapter;

        if (mAdapter != null)
        {
            mFilter = ((Filterable)mAdapter).getFilter();
            adapter.registerDataSetObserver(mObserver);
        }
        else
        {
            mFilter = null;
        }

        mPopup.setAdapter(mAdapter);
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        boolean dismissKey = keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE;
        if (dismissKey && isPopupShowing() && !mPopup.isDropDownAlwaysVisible())
        {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) dismissDropDown();
            return true;
        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        boolean consumed = mPopup.onKeyUp(keyCode, event);
        if (consumed)
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_TAB:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    performCompletion();
                    return true;
                default:
                    break;
            }
        }

        if (isPopupShowing() && keyCode == KeyEvent.KEYCODE_TAB && event.getModifiers() == 0)
        {
            performCompletion();
            return true;
        }

        if (onKeyPreIme(keyCode, event)) return true;

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (mPopup.onKeyDown(keyCode, event)) return true;

        if (!isPopupShowing() && keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.getModifiers() == 0)
        {
            performValidation();
        }

        if (isPopupShowing() && keyCode == KeyEvent.KEYCODE_TAB && event.getModifiers() == 0) return true;

        if (onKeyPreIme(keyCode, event)) return true;

        mLastKeyCode = keyCode;
        boolean handled = super.onKeyDown(keyCode, event);
        mLastKeyCode = KeyEvent.KEYCODE_UNKNOWN;

        if (handled && isPopupShowing()) clearListSelection();

        return handled;
    }

    public boolean enoughToFilter()
    {
        return getText().length() >= mThreshold;
    }

    private void doAfterTextChanged()
    {
        if (mBlockCompletion) return;
        if (mOpenBefore && !isPopupShowing()) return;

        refreshAutoCompleteResults();
    }

    public final void refreshAutoCompleteResults()
    {
        if (enoughToFilter())
        {
            if (mFilter == null) return;

            mPopupCanBeUpdated = true;
            performFiltering(getText(), mLastKeyCode);
            return;
        }

        if (!mPopup.isDropDownAlwaysVisible()) dismissDropDown();
        if (mFilter != null) mFilter.filter(null);
    }

    public boolean isPopupShowing()
    {
        return mPopup != null && mPopup.isShowing();
    }

    protected CharSequence convertSelectionToString(Object selectedItem)
    {
        if (mFilter == null) return selectedItem == null ? "" : selectedItem.toString();
        return mFilter.convertResultToString(selectedItem);
    }

    public void clearListSelection()
    {
        mPopup.clearListSelection();
    }

    public void setListSelection(int position)
    {
        mPopup.setSelection(position);
    }

    public int getListSelection()
    {
        return mPopup.getSelectedItemPosition();
    }

    protected void performFiltering(CharSequence text, int keyCode)
    {
        mFilter.filter(text, this);
    }

    public void performCompletion()
    {
        performCompletion(null, -1, -1);
    }

    public void onCommitCompletion(CompletionInfo completion)
    {
        if (isPopupShowing() && completion != null) mPopup.performItemClick(completion.getPosition());
    }

    private void performCompletion(View selectedView, int position, long id)
    {
        if (isPopupShowing())
        {
            Object selectedItem;
            if (position < 0) selectedItem = mPopup.getSelectedItem();
            else selectedItem = mAdapter == null ? null : mAdapter.getItem(position);

            if (selectedItem == null)
            {
                android.util.Log.w("AutoCompleteTextView", "performCompletion: no selected item");
                return;
            }

            mBlockCompletion = true;
            replaceText(convertSelectionToString(selectedItem));
            mBlockCompletion = false;

            if (mItemClickListener != null)
            {
                ListPopupWindow list = mPopup;
                if (selectedView == null || position < 0)
                {
                    selectedView = list.getSelectedView();
                    position = list.getSelectedItemPosition();
                    id = list.getSelectedItemId();
                }

                mItemClickListener.onItemClick(list.getListView(), selectedView, position, id);
            }
        }

        if (mDropDownDismissedOnCompletion && !mPopup.isDropDownAlwaysVisible()) dismissDropDown();
    }

    public boolean isPerformingCompletion()
    {
        return mBlockCompletion;
    }

    public void setText(CharSequence text, boolean filter)
    {
        if (filter)
        {
            setText(text);
            return;
        }

        mBlockCompletion = true;
        setText(text);
        mBlockCompletion = false;
    }

    protected void replaceText(CharSequence text)
    {
        clearComposingText();
        setText(text);
        setSelection(getText().length());
    }

    @Override
    public void onFilterComplete(int count)
    {
        updateDropDownForFilter(count);
    }

    private void updateDropDownForFilter(int count)
    {
        if (getWindowVisibility() == View.GONE) return;

        boolean dropDownAlwaysVisible = mPopup.isDropDownAlwaysVisible();
        boolean enoughToFilter = enoughToFilter();

        if ((count > 0 || dropDownAlwaysVisible) && enoughToFilter)
        {
            if (hasFocus() && hasWindowFocus() && mPopupCanBeUpdated) showDropDown();
            return;
        }

        if (!dropDownAlwaysVisible && isPopupShowing())
        {
            dismissDropDown();
            mPopupCanBeUpdated = true;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus)
    {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus && !mPopup.isDropDownAlwaysVisible()) dismissDropDown();
    }

    protected void onDisplayHint(int hint)
    {
        if (hint == DISPLAY_HINT_INVISIBLE && !mPopup.isDropDownAlwaysVisible()) dismissDropDown();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect)
    {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (focused) return;

        performValidation();
        if (!mPopup.isDropDownAlwaysVisible()) dismissDropDown();
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        dismissDropDown();
        super.onDetachedFromWindow();
    }

    public void dismissDropDown()
    {
        if (mPopup == null) return;

        mPopup.dismiss();
        mPopupCanBeUpdated = false;
    }

    protected boolean setFrame(int left, int top, int right, int bottom)
    {
        boolean changed = super.setFrame(left, top, right, bottom);
        if (changed && isPopupShowing()) showDropDown();
        return changed;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (isPopupShowing()) showDropDown();
    }

    public int getInputMethodMode()
    {
        return mPopup.getInputMethodMode();
    }

    public void setInputMethodMode(int mode)
    {
        mPopup.setInputMethodMode(mode);
    }

    public void showDropDown()
    {
        if (mPopup.getAnchorView() == null)
        {
            View anchor = null;
            if (mDropDownAnchorId != View.NO_ID) anchor = getRootView().findViewById(mDropDownAnchorId);
            mPopup.setAnchorView(anchor != null ? anchor : this);
        }

        if (!isPopupShowing()) mPopup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NEEDED);

        mPopup.show();
    }

    public void setValidator(Validator validator)
    {
        mValidator = validator;
    }

    public Validator getValidator()
    {
        return mValidator;
    }

    public void performValidation()
    {
        if (mValidator == null) return;

        CharSequence text = getText();
        if (!TextUtils.isEmpty(text) && !mValidator.isValid(text)) setText(mValidator.fixText(text));
    }

    protected Filter getFilter()
    {
        return mFilter;
    }

    public CharSequence getAccessibilityClassName()
    {
        return AutoCompleteTextView.class.getName();
    }

    private final class CompletionWatcher implements TextWatcher
    {
        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after)
        {
            if (mBlockCompletion) return;

            mOpenBefore = isPopupShowing();
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count)
        {
        }

        @Override
        public void afterTextChanged(Editable text)
        {
            doAfterTextChanged();
        }
    }

    public interface Validator
    {
        boolean isValid(CharSequence text);

        CharSequence fixText(CharSequence invalidText);
    }

    public interface OnDismissListener
    {
        void onDismiss();
    }
}
