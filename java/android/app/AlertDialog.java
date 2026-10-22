package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class AlertDialog extends Dialog implements DialogInterface
{
    public static final int THEME_TRADITIONAL = 1;
    public static final int THEME_HOLO_DARK = 2;
    public static final int THEME_HOLO_LIGHT = 3;
    public static final int THEME_DEVICE_DEFAULT_DARK = 4;
    public static final int THEME_DEVICE_DEFAULT_LIGHT = 5;

    private static final int PADDING_DP = 24;
    private static final int BUTTON_GAP_DP = 8;
    private static final float TITLE_SIZE_SP = 20f;
    private static final float MESSAGE_SIZE_SP = 16f;

    private CharSequence mTitle;
    private CharSequence mMessage;
    private Drawable mIcon;
    private View mView;
    private CharSequence[] mItems;
    private boolean[] mCheckedItems;
    private int mCheckedItem = -1;
    private int mChoiceMode;
    private DialogInterface.OnClickListener mItemListener;
    private DialogInterface.OnMultiChoiceClickListener mMultiChoiceListener;
    private ListAdapter mAdapter;
    private ListView mListView;
    private AdapterView.OnItemSelectedListener mOnItemSelectedListener;

    private final CharSequence[] mButtonText = new CharSequence[3];
    private final DialogInterface.OnClickListener[] mButtonListeners = new DialogInterface.OnClickListener[3];
    private final Button[] mButtons = new Button[3];

    private TextView mMessageView;
    private TextView mTitleView;

    protected AlertDialog(Context context)
    {
        this(context, 0);
    }

    protected AlertDialog(Context context, boolean cancelable, OnCancelListener cancelListener)
    {
        this(context, 0);
        setCancelable(cancelable);
        setOnCancelListener(cancelListener);
    }

    protected AlertDialog(Context context, int themeResId)
    {
        super(context, resolveAlertTheme(context, themeResId));
    }

    private static int resolveAlertTheme(Context context, int themeResId)
    {
        if (themeResId >= 0x01000000) return themeResId;

        int attribute = context.getResources().getIdentifier("alertDialogTheme", "attr", "android");
        if (attribute == 0) return 0;

        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(attribute, value, true) ? value.resourceId : 0;
    }

    private static int slot(int whichButton)
    {
        switch (whichButton)
        {
            case BUTTON_POSITIVE: return 0;
            case BUTTON_NEGATIVE: return 1;
            case BUTTON_NEUTRAL: return 2;
            default: throw new IllegalArgumentException("Button does not exist");
        }
    }

    private int pixels(float dp)
    {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }

    public Button getButton(int whichButton)
    {
        return mButtons[slot(whichButton)];
    }

    @Override
    public void setTitle(CharSequence title)
    {
        super.setTitle(title);
        mTitle = title;
        if (mTitleView != null) mTitleView.setText(title);
    }

    public void setCustomTitle(View customTitleView)
    {
        mView = customTitleView;
    }

    public void setMessage(CharSequence message)
    {
        mMessage = message;
        if (mMessageView != null) mMessageView.setText(message);
    }

    public CharSequence getMessage()
    {
        return mMessage;
    }

    public void setView(View view)
    {
        mView = view;
    }

    public void setView(View view, int viewSpacingLeft, int viewSpacingTop, int viewSpacingRight, int viewSpacingBottom)
    {
        mView = view;
        if (view != null) view.setPadding(viewSpacingLeft, viewSpacingTop, viewSpacingRight, viewSpacingBottom);
    }

    public void setButton(int whichButton, CharSequence text, DialogInterface.OnClickListener listener)
    {
        int index = slot(whichButton);
        mButtonText[index] = text;
        mButtonListeners[index] = listener;
    }

    public void setButton(int whichButton, CharSequence text, android.os.Message message)
    {
        final android.os.Message pending = message;
        setButton(whichButton, text, pending == null ? null : new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                android.os.Message.obtain(pending).sendToTarget();
            }
        });
    }

    public void setButton(int whichButton, CharSequence text, Drawable icon, DialogInterface.OnClickListener listener)
    {
        setButton(whichButton, text, listener);
    }

    public void setIcon(int resId)
    {
        mIcon = resId == 0 ? null : getContext().getResources().getDrawable(resId);
    }

    public void setIcon(Drawable icon)
    {
        mIcon = icon;
    }

    public void setIconAttribute(int attrId)
    {
    }

    public void setInverseBackgroundForced(boolean forceInverseBackground)
    {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        int padding = pixels(PADDING_DP);

        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(padding, padding, padding, pixels(BUTTON_GAP_DP));

        if (mTitle != null && mTitle.length() > 0)
        {
            mTitleView = new TextView(context);
            mTitleView.setText(mTitle);
            mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SIZE_SP);
            mTitleView.setPadding(0, 0, 0, pixels(BUTTON_GAP_DP * 2));
            panel.addView(mTitleView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (mMessage != null)
        {
            ScrollView scroller = new ScrollView(context);
            mMessageView = new TextView(context);
            mMessageView.setText(mMessage);
            mMessageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_SIZE_SP);
            scroller.addView(mMessageView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            panel.addView(scroller, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (mItems != null || mAdapter != null)
        {
            panel.addView(buildList(context), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                            ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (mView != null)
        {
            ViewGroup.LayoutParams params = mView.getLayoutParams();
            if (params == null) params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            panel.addView(mView, params);
        }

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        buttons.setPadding(0, pixels(BUTTON_GAP_DP * 2), 0, 0);

        int[] order = {BUTTON_NEUTRAL, BUTTON_NEGATIVE, BUTTON_POSITIVE};
        boolean anyButton = false;

        for (final int which : order)
        {
            int index = slot(which);
            if (mButtonText[index] == null) continue;

            Button button = new Button(context);
            button.setText(mButtonText[index]);

            final DialogInterface.OnClickListener listener = mButtonListeners[index];
            button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (listener != null) listener.onClick(AlertDialog.this, which);
                    dismiss();
                }
            });

            mButtons[index] = button;
            buttons.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            anyButton = true;
        }

        if (anyButton) panel.addView(buttons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(panel, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public ListView getListView()
    {
        return mListView;
    }

    private View buildList(final Context context)
    {
        final ListAdapter adapter = mAdapter != null ? mAdapter : new BaseAdapter()
        {
            @Override
            public int getCount()
            {
                return mItems == null ? 0 : mItems.length;
            }

            @Override
            public Object getItem(int position)
            {
                return mItems[position];
            }

            @Override
            public long getItemId(int position)
            {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                TextView row = convertView instanceof TextView ? (TextView)convertView : null;

                if (row == null)
                {
                    row = new TextView(context);
                    row.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_SIZE_SP);
                    row.setPadding(0, pixels(BUTTON_GAP_DP + 4), 0, pixels(BUTTON_GAP_DP + 4));
                }

                row.setText(describeItem(position));
                return row;
            }
        };

        mListView = new ListView(context);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                onItemClicked(position);
                if (adapter instanceof BaseAdapter) ((BaseAdapter)adapter).notifyDataSetChanged();
            }
        });

        if (mOnItemSelectedListener != null) mListView.setOnItemSelectedListener(mOnItemSelectedListener);

        if (mAdapter != null && mChoiceMode == 1)
        {
            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            if (mCheckedItem >= 0) mListView.setItemChecked(mCheckedItem, true);
        }

        return mListView;
    }

    private CharSequence describeItem(int index)
    {
        if (mChoiceMode == 1) return (index == mCheckedItem ? "◉  " : "○  ") + mItems[index];
        if (mChoiceMode == 2) return (mCheckedItems != null && mCheckedItems[index] ? "☑  " : "☐  ") + mItems[index];

        return mItems[index];
    }

    private void onItemClicked(int position)
    {
        if (mChoiceMode == 2)
        {
            if (mCheckedItems == null) mCheckedItems = new boolean[mItems.length];
            mCheckedItems[position] = !mCheckedItems[position];
            if (mMultiChoiceListener != null) mMultiChoiceListener.onClick(this, position, mCheckedItems[position]);
            return;
        }

        if (mChoiceMode == 1) mCheckedItem = position;
        if (mItemListener != null) mItemListener.onClick(this, position);
        if (mChoiceMode == 0) dismiss();
    }

    public static class Builder
    {
        private final Context mContext;
        private final int mTheme;
        private CharSequence mTitle;
        private View mCustomTitle;
        private CharSequence mMessage;
        private Drawable mIcon;
        private int mIconId;
        private View mView;
        private int mViewLayoutResId;
        private boolean mCancelable = true;
        private CharSequence[] mItems;
        private boolean[] mCheckedItems;
        private int mCheckedItem = -1;
        private int mChoiceMode;
        private DialogInterface.OnClickListener mItemListener;
        private DialogInterface.OnMultiChoiceClickListener mMultiChoiceListener;
        private ListAdapter mAdapter;
        private AdapterView.OnItemSelectedListener mOnItemSelectedListener;

        private final CharSequence[] mButtonText = new CharSequence[3];
        private final DialogInterface.OnClickListener[] mButtonListeners = new DialogInterface.OnClickListener[3];
        private DialogInterface.OnCancelListener mCancelListener;
        private DialogInterface.OnDismissListener mDismissListener;
        private DialogInterface.OnKeyListener mKeyListener;

        public Builder(Context context)
        {
            this(context, 0);
        }

        public Builder(Context context, int themeResId)
        {
            mContext = context;
            mTheme = themeResId;
        }

        public Context getContext()
        {
            return mContext;
        }

        private CharSequence text(int resId)
        {
            return resId == 0 ? null : mContext.getText(resId);
        }

        public Builder setTitle(int titleId)
        {
            return setTitle(text(titleId));
        }

        public Builder setTitle(CharSequence title)
        {
            mTitle = title;
            return this;
        }

        public Builder setCustomTitle(View customTitleView)
        {
            mCustomTitle = customTitleView;
            return this;
        }

        public Builder setMessage(int messageId)
        {
            return setMessage(text(messageId));
        }

        public Builder setMessage(CharSequence message)
        {
            mMessage = message;
            return this;
        }

        public Builder setIcon(int iconId)
        {
            mIconId = iconId;
            return this;
        }

        public Builder setIcon(Drawable icon)
        {
            mIcon = icon;
            return this;
        }

        public Builder setIconAttribute(int attrId)
        {
            return this;
        }

        public Builder setPositiveButton(int textId, DialogInterface.OnClickListener listener)
        {
            return setPositiveButton(text(textId), listener);
        }

        public Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener)
        {
            mButtonText[0] = text;
            mButtonListeners[0] = listener;
            return this;
        }

        public Builder setPositiveButtonIcon(Drawable icon)
        {
            return this;
        }

        public Builder setNegativeButton(int textId, DialogInterface.OnClickListener listener)
        {
            return setNegativeButton(text(textId), listener);
        }

        public Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener)
        {
            mButtonText[1] = text;
            mButtonListeners[1] = listener;
            return this;
        }

        public Builder setNegativeButtonIcon(Drawable icon)
        {
            return this;
        }

        public Builder setNeutralButton(int textId, DialogInterface.OnClickListener listener)
        {
            return setNeutralButton(text(textId), listener);
        }

        public Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener)
        {
            mButtonText[2] = text;
            mButtonListeners[2] = listener;
            return this;
        }

        public Builder setNeutralButtonIcon(Drawable icon)
        {
            return this;
        }

        public Builder setCancelable(boolean cancelable)
        {
            mCancelable = cancelable;
            return this;
        }

        public Builder setOnCancelListener(DialogInterface.OnCancelListener listener)
        {
            mCancelListener = listener;
            return this;
        }

        public Builder setOnDismissListener(DialogInterface.OnDismissListener listener)
        {
            mDismissListener = listener;
            return this;
        }

        public Builder setOnKeyListener(DialogInterface.OnKeyListener listener)
        {
            mKeyListener = listener;
            return this;
        }

        public Builder setItems(int itemsId, DialogInterface.OnClickListener listener)
        {
            return setItems(mContext.getResources().getTextArray(itemsId), listener);
        }

        public Builder setItems(CharSequence[] items, DialogInterface.OnClickListener listener)
        {
            mItems = items;
            mItemListener = listener;
            mChoiceMode = 0;
            return this;
        }

        public Builder setMultiChoiceItems(int itemsId, boolean[] checkedItems, DialogInterface.OnMultiChoiceClickListener listener)
        {
            return setMultiChoiceItems(mContext.getResources().getTextArray(itemsId), checkedItems, listener);
        }

        public Builder setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems, DialogInterface.OnMultiChoiceClickListener listener)
        {
            mItems = items;
            mCheckedItems = checkedItems;
            mMultiChoiceListener = listener;
            mChoiceMode = 2;
            return this;
        }

        public Builder setSingleChoiceItems(int itemsId, int checkedItem, DialogInterface.OnClickListener listener)
        {
            return setSingleChoiceItems(mContext.getResources().getTextArray(itemsId), checkedItem, listener);
        }

        public Builder setSingleChoiceItems(CharSequence[] items, int checkedItem, DialogInterface.OnClickListener listener)
        {
            mItems = items;
            mCheckedItem = checkedItem;
            mItemListener = listener;
            mChoiceMode = 1;
            return this;
        }

        public Builder setAdapter(ListAdapter adapter, DialogInterface.OnClickListener listener)
        {
            mAdapter = adapter;
            mItems = null;
            mItemListener = listener;
            mChoiceMode = 0;
            return this;
        }

        public Builder setSingleChoiceItems(ListAdapter adapter, int checkedItem, DialogInterface.OnClickListener listener)
        {
            mAdapter = adapter;
            mItems = null;
            mCheckedItem = checkedItem;
            mItemListener = listener;
            mChoiceMode = 1;
            return this;
        }

        public Builder setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener)
        {
            mOnItemSelectedListener = listener;
            return this;
        }

        public Builder setView(int layoutResId)
        {
            mViewLayoutResId = layoutResId;
            mView = null;
            return this;
        }

        public Builder setView(View view)
        {
            mView = view;
            mViewLayoutResId = 0;
            return this;
        }

        @Deprecated
        public Builder setInverseBackgroundForced(boolean useInverseBackground)
        {
            return this;
        }

        public Builder setRecycleOnMeasureEnabled(boolean enabled)
        {
            return this;
        }

        public AlertDialog create()
        {
            AlertDialog dialog = new AlertDialog(mContext, mTheme);

            dialog.setTitle(mTitle);
            if (mCustomTitle != null) dialog.setCustomTitle(mCustomTitle);
            dialog.setMessage(mMessage);
            if (mIcon != null) dialog.setIcon(mIcon);
            if (mIconId != 0) dialog.setIcon(mIconId);

            for (int index = 0; index < 3; index++)
            {
                if (mButtonText[index] == null) continue;

                int which = index == 0 ? BUTTON_POSITIVE : index == 1 ? BUTTON_NEGATIVE : BUTTON_NEUTRAL;
                dialog.setButton(which, mButtonText[index], mButtonListeners[index]);
            }

            dialog.mItems = mItems;
            dialog.mCheckedItems = mCheckedItems;
            dialog.mCheckedItem = mCheckedItem;
            dialog.mChoiceMode = mChoiceMode;
            dialog.mItemListener = mItemListener;
            dialog.mMultiChoiceListener = mMultiChoiceListener;
            dialog.mAdapter = mAdapter;
            dialog.mOnItemSelectedListener = mOnItemSelectedListener;

            if (mView != null) dialog.setView(mView);
            else if (mViewLayoutResId != 0) dialog.setView(android.view.LayoutInflater.from(dialog.getContext()).inflate(mViewLayoutResId, null));

            dialog.setCancelable(mCancelable);
            if (mCancelable) dialog.setCanceledOnTouchOutside(true);
            dialog.setOnCancelListener(mCancelListener);
            dialog.setOnDismissListener(mDismissListener);
            if (mKeyListener != null) dialog.setOnKeyListener(mKeyListener);
            return dialog;
        }

        public AlertDialog show()
        {
            AlertDialog dialog = create();
            dialog.show();
            return dialog;
        }
    }
}
