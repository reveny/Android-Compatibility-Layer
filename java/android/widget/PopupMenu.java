package android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.RuntimeMenu;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class PopupMenu
{
    private static final float MINIMUM_WIDTH_DIP = 112;
    private static final float MAXIMUM_WIDTH_DIP = 320;
    private static final float ITEM_HEIGHT_DIP = 48;
    private static final float ITEM_PADDING_DIP = 16;
    private static final float ICON_SIZE_DIP = 24;

    private final Context mContext;
    private final RuntimeMenu mMenu;
    private final View mAnchor;
    private ListPopupWindow mPopup;
    private int mGravity;
    private boolean mForceShowIcon;
    private boolean mSwitchingMenu;
    private OnMenuItemClickListener mMenuItemClickListener;
    private OnDismissListener mOnDismissListener;
    private View.OnTouchListener mDragListener;

    public PopupMenu(Context context, View anchor)
    {
        this(context, anchor, Gravity.NO_GRAVITY);
    }

    public PopupMenu(Context context, View anchor, int gravity)
    {
        this(context, anchor, gravity, 0, 0);
    }

    public PopupMenu(Context context, View anchor, int gravity, int popupStyleAttr, int popupStyleRes)
    {
        mContext = context;
        mAnchor = anchor;
        mGravity = gravity;
        mMenu = new RuntimeMenu(context);
        mMenu.setCallback(new RuntimeMenu.Callback()
        {
            @Override
            public boolean onMenuItemSelected(RuntimeMenu menu, MenuItem item)
            {
                return mMenuItemClickListener != null && mMenuItemClickListener.onMenuItemClick(item);
            }
        });
    }

    public void setGravity(int gravity)
    {
        mGravity = gravity;
    }

    public int getGravity()
    {
        return mGravity;
    }

    public View.OnTouchListener getDragToOpenListener()
    {
        if (mDragListener == null) mDragListener = popup().createDragToOpenListener(mAnchor);
        return mDragListener;
    }

    public Menu getMenu()
    {
        return mMenu;
    }

    public MenuInflater getMenuInflater()
    {
        return new MenuInflater(mContext);
    }

    public void inflate(int menuRes)
    {
        getMenuInflater().inflate(menuRes, mMenu);
    }

    public void show()
    {
        showMenu(mMenu);
    }

    public void dismiss()
    {
        if (mPopup != null) mPopup.dismiss();
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener)
    {
        mMenuItemClickListener = listener;
    }

    public void setOnDismissListener(OnDismissListener listener)
    {
        mOnDismissListener = listener;
    }

    public void setForceShowIcon(boolean forceShowIcon)
    {
        mForceShowIcon = forceShowIcon;
    }

    private ListPopupWindow popup()
    {
        if (mPopup != null) return mPopup;

        mPopup = new ListPopupWindow(mContext);
        mPopup.setModal(true);
        mPopup.setOnDismissListener(new PopupWindow.OnDismissListener()
        {
            @Override
            public void onDismiss()
            {
                if (!mSwitchingMenu && mOnDismissListener != null) mOnDismissListener.onDismiss(PopupMenu.this);
            }
        });

        return mPopup;
    }

    private void showMenu(RuntimeMenu menu)
    {
        if (mAnchor == null) throw new IllegalStateException("MenuPopupHelper cannot be used without an anchor");

        final MenuAdapter adapter = new MenuAdapter(menu.getVisibleItems(), menuHasIcons(menu));
        ListPopupWindow popup = popup();

        popup.setAdapter(adapter);
        popup.setAnchorView(mAnchor);
        popup.setDropDownGravity(mGravity);
        popup.setContentWidth(measureContentWidth(adapter));
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                onMenuItemChosen(adapter.getItem(position));
            }
        });

        if (popup.isShowing())
        {
            mSwitchingMenu = true;
            popup.dismiss();
            mSwitchingMenu = false;
        }

        popup.show();
    }

    private void onMenuItemChosen(MenuItem item)
    {
        if (item == null || !item.isEnabled()) return;

        mMenu.performItemAction(item);
        SubMenu subMenu = item.getSubMenu();

        if (item.hasSubMenu() && subMenu instanceof RuntimeMenu)
        {
            RuntimeMenu runtimeSubMenu = (RuntimeMenu)subMenu;
            runtimeSubMenu.setCallback(new RuntimeMenu.Callback()
            {
                @Override
                public boolean onMenuItemSelected(RuntimeMenu menu, MenuItem selected)
                {
                    return mMenuItemClickListener != null && mMenuItemClickListener.onMenuItemClick(selected);
                }
            });

            showMenu(runtimeSubMenu);
            return;
        }

        dismiss();
    }

    private boolean menuHasIcons(RuntimeMenu menu)
    {
        if (!mForceShowIcon) return false;

        for (MenuItem item : menu.getVisibleItems())
        {
            if (item.getIcon() != null) return true;
        }

        return false;
    }

    private int measureContentWidth(MenuAdapter adapter)
    {
        float density = mContext.getResources().getDisplayMetrics().density;
        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        int maximum = Math.max(screenWidth / 2, (int)(MAXIMUM_WIDTH_DIP * density));
        int widest = (int)(MINIMUM_WIDTH_DIP * density);

        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        FrameLayout measureParent = new FrameLayout(mContext);
        View convertView = null;

        for (int position = 0; position < adapter.getCount(); position++)
        {
            convertView = adapter.getView(position, convertView, measureParent);
            convertView.measure(widthSpec, heightSpec);

            int width = convertView.getMeasuredWidth();
            if (width >= maximum) return maximum;
            widest = Math.max(widest, width);
        }

        return widest;
    }

    public interface OnMenuItemClickListener
    {
        boolean onMenuItemClick(MenuItem item);
    }

    public interface OnDismissListener
    {
        void onDismiss(PopupMenu menu);
    }

    private final class MenuAdapter extends BaseAdapter
    {
        private final List<MenuItem> mItems;
        private final boolean mShowIcons;

        MenuAdapter(List<MenuItem> items, boolean showIcons)
        {
            mItems = items;
            mShowIcons = showIcons;
        }

        @Override
        public int getCount()
        {
            return mItems.size();
        }

        @Override
        public MenuItem getItem(int position)
        {
            return position >= 0 && position < mItems.size() ? mItems.get(position) : null;
        }

        @Override
        public long getItemId(int position)
        {
            MenuItem item = getItem(position);
            return item == null ? position : item.getItemId();
        }

        @Override
        public boolean areAllItemsEnabled()
        {
            for (MenuItem item : mItems)
            {
                if (!item.isEnabled()) return false;
            }

            return true;
        }

        @Override
        public boolean isEnabled(int position)
        {
            MenuItem item = getItem(position);
            return item != null && item.isEnabled();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            MenuItem item = mItems.get(position);
            float density = mContext.getResources().getDisplayMetrics().density;

            ItemView view = convertView instanceof ItemView ? (ItemView)convertView : new ItemView(mContext, density);
            view.bind(item, mShowIcons, density);
            return view;
        }
    }

    private static final class ItemView extends LinearLayout
    {
        private final ImageView mIcon;
        private final CheckedTextView mTitle;

        ItemView(Context context, float density)
        {
            super(context);

            setOrientation(LinearLayout.HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setMinimumHeight((int)(ITEM_HEIGHT_DIP * density));

            int padding = (int)(ITEM_PADDING_DIP * density);
            setPadding(padding, 0, padding, 0);

            int iconSize = (int)(ICON_SIZE_DIP * density);
            mIcon = new ImageView(context);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconParams.rightMargin = padding;
            addView(mIcon, iconParams);

            mTitle = new CheckedTextView(context);
            mTitle.setTextSize(16);
            mTitle.setSingleLine(true);
            addView(mTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        void bind(MenuItem item, boolean showIcons, float density)
        {
            Drawable icon = item.getIcon();
            mIcon.setImageDrawable(icon);
            mIcon.setVisibility(showIcons && icon != null ? View.VISIBLE : showIcons ? View.INVISIBLE : View.GONE);

            CharSequence title = item.getTitle();
            if (title == null) title = "";
            if (item.isCheckable() && item.isChecked()) title = title + "  ✓";
            if (item.hasSubMenu()) title = title + "  ▸";

            mTitle.setText(title);
            mTitle.setChecked(item.isCheckable() && item.isChecked());
            mTitle.setAlpha(item.isEnabled() ? 1.0f : 0.38f);

            int verticalPadding = (int)(12 * density);
            mTitle.setPadding(0, verticalPadding, 0, verticalPadding);
        }
    }
}
