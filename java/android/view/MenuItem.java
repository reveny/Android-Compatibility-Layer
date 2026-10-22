package android.view;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.BlendMode;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public interface MenuItem
{
    int SHOW_AS_ACTION_NEVER = 0;
    int SHOW_AS_ACTION_IF_ROOM = 1;
    int SHOW_AS_ACTION_ALWAYS = 2;
    int SHOW_AS_ACTION_WITH_TEXT = 4;
    int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = 8;

    interface OnMenuItemClickListener
    {
        boolean onMenuItemClick(MenuItem item);
    }

    interface OnActionExpandListener
    {
        boolean onMenuItemActionExpand(MenuItem item);

        boolean onMenuItemActionCollapse(MenuItem item);
    }

    int getItemId();

    int getGroupId();

    int getOrder();

    MenuItem setTitle(CharSequence title);

    MenuItem setTitle(int title);

    CharSequence getTitle();

    MenuItem setTitleCondensed(CharSequence title);

    CharSequence getTitleCondensed();

    MenuItem setIcon(Drawable icon);

    MenuItem setIcon(int iconRes);

    Drawable getIcon();

    default MenuItem setIconTintList(ColorStateList tint)
    {
        return this;
    }

    default ColorStateList getIconTintList()
    {
        return null;
    }

    default MenuItem setIconTintMode(PorterDuff.Mode tintMode)
    {
        return this;
    }

    default MenuItem setIconTintBlendMode(BlendMode blendMode)
    {
        return this;
    }

    default PorterDuff.Mode getIconTintMode()
    {
        return null;
    }

    default BlendMode getIconTintBlendMode()
    {
        return null;
    }

    MenuItem setIntent(Intent intent);

    Intent getIntent();

    MenuItem setShortcut(char numericChar, char alphaChar);

    default MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers, int alphaModifiers)
    {
        return setShortcut(numericChar, alphaChar);
    }

    MenuItem setNumericShortcut(char numericChar);

    default MenuItem setNumericShortcut(char numericChar, int numericModifiers)
    {
        return setNumericShortcut(numericChar);
    }

    char getNumericShortcut();

    default int getNumericModifiers()
    {
        return KeyEvent.META_CTRL_ON;
    }

    MenuItem setAlphabeticShortcut(char alphaChar);

    default MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers)
    {
        return setAlphabeticShortcut(alphaChar);
    }

    char getAlphabeticShortcut();

    default int getAlphabeticModifiers()
    {
        return KeyEvent.META_CTRL_ON;
    }

    MenuItem setCheckable(boolean checkable);

    boolean isCheckable();

    MenuItem setChecked(boolean checked);

    boolean isChecked();

    MenuItem setVisible(boolean visible);

    boolean isVisible();

    MenuItem setEnabled(boolean enabled);

    boolean isEnabled();

    boolean hasSubMenu();

    SubMenu getSubMenu();

    MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener);

    ContextMenu.ContextMenuInfo getMenuInfo();

    void setShowAsAction(int actionEnum);

    MenuItem setShowAsActionFlags(int actionEnum);

    MenuItem setActionView(View view);

    MenuItem setActionView(int resId);

    View getActionView();

    MenuItem setActionProvider(ActionProvider actionProvider);

    ActionProvider getActionProvider();

    boolean expandActionView();

    boolean collapseActionView();

    boolean isActionViewExpanded();

    MenuItem setOnActionExpandListener(OnActionExpandListener listener);

    default MenuItem setContentDescription(CharSequence contentDescription)
    {
        return this;
    }

    default CharSequence getContentDescription()
    {
        return null;
    }

    default MenuItem setTooltipText(CharSequence tooltipText)
    {
        return this;
    }

    default CharSequence getTooltipText()
    {
        return null;
    }
}
