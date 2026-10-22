package android.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class RuntimeMenu implements Menu
{
    public interface Callback
    {
        boolean onMenuItemSelected(RuntimeMenu menu, MenuItem item);
    }

    private final Context mContext;
    private final List<RuntimeMenuItem> mItems = new ArrayList<>();
    private Callback mCallback;
    private boolean mQwertyMode = true;
    private boolean mGroupDividerEnabled;

    public RuntimeMenu(Context context)
    {
        mContext = context;
    }

    public void setCallback(Callback callback)
    {
        mCallback = callback;
    }

    public Context getContext()
    {
        return mContext;
    }

    boolean dispatchItemSelected(MenuItem item)
    {
        return mCallback != null && mCallback.onMenuItemSelected(this, item);
    }

    void uncheckGroup(int groupId, MenuItem except)
    {
        for (RuntimeMenuItem item : mItems)
        {
            if (item.getGroupId() == groupId && item != except && item.isCheckable()) item.setChecked(false);
        }
    }

    private RuntimeMenuItem addInternal(int groupId, int itemId, int order, CharSequence title)
    {
        RuntimeMenuItem item = new RuntimeMenuItem(mContext, this, groupId, itemId, order, title);

        int position = mItems.size();
        for (int index = 0; index < mItems.size(); index++)
        {
            if (mItems.get(index).getOrder() > order)
            {
                position = index;
                break;
            }
        }

        mItems.add(position, item);
        return item;
    }

    private CharSequence text(int titleRes)
    {
        return titleRes == 0 ? null : mContext.getResources().getText(titleRes);
    }

    @Override
    public MenuItem add(CharSequence title)
    {
        return addInternal(NONE, NONE, NONE, title);
    }

    @Override
    public MenuItem add(int titleRes)
    {
        return addInternal(NONE, NONE, NONE, text(titleRes));
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, CharSequence title)
    {
        return addInternal(groupId, itemId, order, title);
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, int titleRes)
    {
        return addInternal(groupId, itemId, order, text(titleRes));
    }

    @Override
    public SubMenu addSubMenu(CharSequence title)
    {
        return addSubMenu(NONE, NONE, NONE, title);
    }

    @Override
    public SubMenu addSubMenu(int titleRes)
    {
        return addSubMenu(NONE, NONE, NONE, text(titleRes));
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title)
    {
        RuntimeMenuItem item = addInternal(groupId, itemId, order, title);
        RuntimeSubMenu subMenu = new RuntimeSubMenu(mContext, this, item);
        item.setSubMenu(subMenu);
        return subMenu;
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes)
    {
        return addSubMenu(groupId, itemId, order, text(titleRes));
    }

    @Override
    public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics, Intent intent, int flags,
                                MenuItem[] outSpecificItems)
    {
        if ((flags & FLAG_APPEND_TO_GROUP) == 0) removeGroup(groupId);
        return 0;
    }

    @Override
    public void removeItem(int id)
    {
        for (int index = 0; index < mItems.size(); index++)
        {
            if (mItems.get(index).getItemId() != id) continue;

            mItems.remove(index);
            return;
        }
    }

    @Override
    public void removeGroup(int groupId)
    {
        for (int index = mItems.size() - 1; index >= 0; index--)
        {
            if (mItems.get(index).getGroupId() == groupId) mItems.remove(index);
        }
    }

    @Override
    public void clear()
    {
        mItems.clear();
    }

    @Override
    public void setGroupCheckable(int group, boolean checkable, boolean exclusive)
    {
        for (RuntimeMenuItem item : mItems)
        {
            if (item.getGroupId() != group) continue;

            item.setExclusiveCheckable(exclusive);
            item.setCheckable(checkable);
        }
    }

    @Override
    public void setGroupVisible(int group, boolean visible)
    {
        for (RuntimeMenuItem item : mItems)
        {
            if (item.getGroupId() == group) item.setVisible(visible);
        }
    }

    @Override
    public void setGroupEnabled(int group, boolean enabled)
    {
        for (RuntimeMenuItem item : mItems)
        {
            if (item.getGroupId() == group) item.setEnabled(enabled);
        }
    }

    @Override
    public boolean hasVisibleItems()
    {
        for (RuntimeMenuItem item : mItems)
        {
            if (item.isVisible()) return true;
        }

        return false;
    }

    @Override
    public MenuItem findItem(int id)
    {
        for (RuntimeMenuItem item : mItems)
        {
            if (item.getItemId() == id) return item;

            if (item.hasSubMenu())
            {
                MenuItem nested = item.getSubMenu().findItem(id);
                if (nested != null) return nested;
            }
        }

        return null;
    }

    @Override
    public int size()
    {
        return mItems.size();
    }

    @Override
    public MenuItem getItem(int index)
    {
        return mItems.get(index);
    }

    @Override
    public void close()
    {
    }

    @Override
    public boolean performShortcut(int keyCode, KeyEvent event, int flags)
    {
        MenuItem item = findShortcut(event);
        return item instanceof RuntimeMenuItem && ((RuntimeMenuItem)item).invoke();
    }

    @Override
    public boolean isShortcutKey(int keyCode, KeyEvent event)
    {
        return findShortcut(event) != null;
    }

    private MenuItem findShortcut(KeyEvent event)
    {
        if (event == null) return null;

        int unicode = event.getUnicodeChar();
        if (unicode == 0) return null;

        char wanted = Character.toLowerCase((char)unicode);

        for (RuntimeMenuItem item : mItems)
        {
            char shortcut = mQwertyMode ? item.getAlphabeticShortcut() : item.getNumericShortcut();
            if (shortcut != 0 && shortcut == wanted && item.isEnabled()) return item;
        }

        return null;
    }

    @Override
    public boolean performIdentifierAction(int id, int flags)
    {
        MenuItem item = findItem(id);
        return item instanceof RuntimeMenuItem && ((RuntimeMenuItem)item).invoke();
    }

    @Override
    public void setQwertyMode(boolean isQwerty)
    {
        mQwertyMode = isQwerty;
    }

    @Override
    public void setGroupDividerEnabled(boolean groupDividerEnabled)
    {
        mGroupDividerEnabled = groupDividerEnabled;
    }

    public List<MenuItem> getVisibleItems()
    {
        List<MenuItem> visible = new ArrayList<>();
        for (RuntimeMenuItem item : mItems)
        {
            if (item.isVisible()) visible.add(item);
        }

        return visible;
    }

    public boolean performItemAction(MenuItem item)
    {
        return item instanceof RuntimeMenuItem && ((RuntimeMenuItem)item).invoke();
    }
}
