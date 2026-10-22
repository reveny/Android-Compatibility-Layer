package android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class RuntimeSubMenu extends RuntimeMenu implements SubMenu
{
    private final RuntimeMenu mParent;
    private final RuntimeMenuItem mItem;
    private CharSequence mHeaderTitle;
    private Drawable mHeaderIcon;
    private View mHeaderView;

    RuntimeSubMenu(Context context, RuntimeMenu parent, RuntimeMenuItem item)
    {
        super(context);
        mParent = parent;
        mItem = item;
    }

    @Override
    boolean dispatchItemSelected(MenuItem item)
    {
        return super.dispatchItemSelected(item) || mParent.dispatchItemSelected(item);
    }

    @Override
    public SubMenu setHeaderTitle(int titleRes)
    {
        mHeaderTitle = getContext().getResources().getText(titleRes);
        return this;
    }

    @Override
    public SubMenu setHeaderTitle(CharSequence title)
    {
        mHeaderTitle = title;
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(int iconRes)
    {
        mHeaderIcon = iconRes == 0 ? null : getContext().getResources().getDrawable(iconRes);
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(Drawable icon)
    {
        mHeaderIcon = icon;
        return this;
    }

    @Override
    public SubMenu setHeaderView(View view)
    {
        mHeaderView = view;
        return this;
    }

    @Override
    public void clearHeader()
    {
        mHeaderTitle = null;
        mHeaderIcon = null;
        mHeaderView = null;
    }

    @Override
    public SubMenu setIcon(int iconRes)
    {
        mItem.setIcon(iconRes);
        return this;
    }

    @Override
    public SubMenu setIcon(Drawable icon)
    {
        mItem.setIcon(icon);
        return this;
    }

    @Override
    public MenuItem getItem()
    {
        return mItem;
    }
}
