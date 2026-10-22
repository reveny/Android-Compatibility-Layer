package android.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.BlendMode;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public class RuntimeMenuItem implements MenuItem
{
    private final Context mContext;
    private final RuntimeMenu mOwner;
    private final int mGroupId;
    private final int mItemId;
    private final int mOrder;

    private CharSequence mTitle;
    private CharSequence mTitleCondensed;
    private CharSequence mContentDescription;
    private CharSequence mTooltipText;
    private Drawable mIcon;
    private ColorStateList mIconTintList;
    private PorterDuff.Mode mIconTintMode;
    private BlendMode mIconTintBlendMode;
    private Intent mIntent;
    private char mNumericShortcut;
    private char mAlphabeticShortcut;
    private int mNumericModifiers = KeyEvent.META_CTRL_ON;
    private int mAlphabeticModifiers = KeyEvent.META_CTRL_ON;
    private boolean mCheckable;
    private boolean mChecked;
    private boolean mExclusive;
    private boolean mVisible = true;
    private boolean mEnabled = true;
    private RuntimeSubMenu mSubMenu;
    private OnMenuItemClickListener mClickListener;
    private OnActionExpandListener mExpandListener;
    private int mShowAsAction;
    private View mActionView;
    private ActionProvider mActionProvider;
    private boolean mActionViewExpanded;

    RuntimeMenuItem(Context context, RuntimeMenu owner, int groupId, int itemId, int order, CharSequence title)
    {
        mContext = context;
        mOwner = owner;
        mGroupId = groupId;
        mItemId = itemId;
        mOrder = order;
        mTitle = title;
    }

    boolean invoke()
    {
        if (!mEnabled) return false;
        if (mClickListener != null && mClickListener.onMenuItemClick(this)) return true;
        if (mOwner != null && mOwner.dispatchItemSelected(this)) return true;

        if (mIntent != null && mContext != null)
        {
            try
            {
                mContext.startActivity(mIntent);
                return true;
            }
            catch (RuntimeException ignored)
            {
                return false;
            }
        }

        return mActionProvider != null && mActionProvider.onPerformDefaultAction();
    }

    void setSubMenu(RuntimeSubMenu subMenu)
    {
        mSubMenu = subMenu;
    }

    void setExclusiveCheckable(boolean exclusive)
    {
        mExclusive = exclusive;
    }

    @Override
    public int getItemId()
    {
        return mItemId;
    }

    @Override
    public int getGroupId()
    {
        return mGroupId;
    }

    @Override
    public int getOrder()
    {
        return mOrder;
    }

    @Override
    public MenuItem setTitle(CharSequence title)
    {
        mTitle = title;
        return this;
    }

    @Override
    public MenuItem setTitle(int title)
    {
        return setTitle(mContext.getResources().getText(title));
    }

    @Override
    public CharSequence getTitle()
    {
        return mTitle;
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title)
    {
        mTitleCondensed = title;
        return this;
    }

    @Override
    public CharSequence getTitleCondensed()
    {
        return mTitleCondensed != null ? mTitleCondensed : mTitle;
    }

    @Override
    public MenuItem setIcon(Drawable icon)
    {
        mIcon = icon;
        return this;
    }

    @Override
    public MenuItem setIcon(int iconRes)
    {
        mIcon = iconRes == 0 ? null : mContext.getResources().getDrawable(iconRes);
        return this;
    }

    @Override
    public Drawable getIcon()
    {
        return mIcon;
    }

    @Override
    public MenuItem setIconTintList(ColorStateList tint)
    {
        mIconTintList = tint;
        if (mIcon != null) mIcon.setTintList(tint);
        return this;
    }

    @Override
    public ColorStateList getIconTintList()
    {
        return mIconTintList;
    }

    @Override
    public MenuItem setIconTintMode(PorterDuff.Mode tintMode)
    {
        mIconTintMode = tintMode;
        return this;
    }

    @Override
    public MenuItem setIconTintBlendMode(BlendMode blendMode)
    {
        mIconTintBlendMode = blendMode;
        return this;
    }

    @Override
    public PorterDuff.Mode getIconTintMode()
    {
        return mIconTintMode;
    }

    @Override
    public BlendMode getIconTintBlendMode()
    {
        return mIconTintBlendMode;
    }

    @Override
    public MenuItem setIntent(Intent intent)
    {
        mIntent = intent;
        return this;
    }

    @Override
    public Intent getIntent()
    {
        return mIntent;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar)
    {
        mNumericShortcut = numericChar;
        mAlphabeticShortcut = Character.toLowerCase(alphaChar);
        return this;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar, int numericModifiers, int alphaModifiers)
    {
        mNumericModifiers = numericModifiers;
        mAlphabeticModifiers = alphaModifiers;
        return setShortcut(numericChar, alphaChar);
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar)
    {
        mNumericShortcut = numericChar;
        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar, int numericModifiers)
    {
        mNumericModifiers = numericModifiers;
        return setNumericShortcut(numericChar);
    }

    @Override
    public char getNumericShortcut()
    {
        return mNumericShortcut;
    }

    @Override
    public int getNumericModifiers()
    {
        return mNumericModifiers;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar)
    {
        mAlphabeticShortcut = Character.toLowerCase(alphaChar);
        return this;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar, int alphaModifiers)
    {
        mAlphabeticModifiers = alphaModifiers;
        return setAlphabeticShortcut(alphaChar);
    }

    @Override
    public char getAlphabeticShortcut()
    {
        return mAlphabeticShortcut;
    }

    @Override
    public int getAlphabeticModifiers()
    {
        return mAlphabeticModifiers;
    }

    @Override
    public MenuItem setCheckable(boolean checkable)
    {
        mCheckable = checkable;
        return this;
    }

    @Override
    public boolean isCheckable()
    {
        return mCheckable;
    }

    @Override
    public MenuItem setChecked(boolean checked)
    {
        if (checked && mExclusive && mOwner != null) mOwner.uncheckGroup(mGroupId, this);

        mChecked = checked;
        return this;
    }

    @Override
    public boolean isChecked()
    {
        return mChecked;
    }

    @Override
    public MenuItem setVisible(boolean visible)
    {
        mVisible = visible;
        return this;
    }

    @Override
    public boolean isVisible()
    {
        return mVisible;
    }

    @Override
    public MenuItem setEnabled(boolean enabled)
    {
        mEnabled = enabled;
        return this;
    }

    @Override
    public boolean isEnabled()
    {
        return mEnabled;
    }

    @Override
    public boolean hasSubMenu()
    {
        return mSubMenu != null;
    }

    @Override
    public SubMenu getSubMenu()
    {
        return mSubMenu;
    }

    @Override
    public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener)
    {
        mClickListener = menuItemClickListener;
        return this;
    }

    @Override
    public ContextMenu.ContextMenuInfo getMenuInfo()
    {
        return null;
    }

    @Override
    public void setShowAsAction(int actionEnum)
    {
        mShowAsAction = actionEnum;
    }

    @Override
    public MenuItem setShowAsActionFlags(int actionEnum)
    {
        mShowAsAction = actionEnum;
        return this;
    }

    public int getShowAsAction()
    {
        return mShowAsAction;
    }

    @Override
    public MenuItem setActionView(View view)
    {
        mActionView = view;
        return this;
    }

    @Override
    public MenuItem setActionView(int resId)
    {
        mActionView = LayoutInflater.from(mContext).inflate(resId, null);
        return this;
    }

    @Override
    public View getActionView()
    {
        if (mActionView == null && mActionProvider != null) mActionView = mActionProvider.onCreateActionView(this);
        return mActionView;
    }

    @Override
    public MenuItem setActionProvider(ActionProvider actionProvider)
    {
        mActionProvider = actionProvider;
        return this;
    }

    @Override
    public ActionProvider getActionProvider()
    {
        return mActionProvider;
    }

    @Override
    public boolean expandActionView()
    {
        if (getActionView() == null || mActionViewExpanded) return false;
        if (mExpandListener != null && !mExpandListener.onMenuItemActionExpand(this)) return false;

        mActionViewExpanded = true;
        if (mActionView instanceof CollapsibleActionView) ((CollapsibleActionView)mActionView).onActionViewExpanded();
        return true;
    }

    @Override
    public boolean collapseActionView()
    {
        if (!mActionViewExpanded) return false;
        if (mExpandListener != null && !mExpandListener.onMenuItemActionCollapse(this)) return false;

        mActionViewExpanded = false;
        if (mActionView instanceof CollapsibleActionView) ((CollapsibleActionView)mActionView).onActionViewCollapsed();
        return true;
    }

    @Override
    public boolean isActionViewExpanded()
    {
        return mActionViewExpanded;
    }

    @Override
    public MenuItem setOnActionExpandListener(OnActionExpandListener listener)
    {
        mExpandListener = listener;
        return this;
    }

    @Override
    public MenuItem setContentDescription(CharSequence contentDescription)
    {
        mContentDescription = contentDescription;
        return this;
    }

    @Override
    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    @Override
    public MenuItem setTooltipText(CharSequence tooltipText)
    {
        mTooltipText = tooltipText;
        return this;
    }

    @Override
    public CharSequence getTooltipText()
    {
        return mTooltipText;
    }

    @Override
    public String toString()
    {
        return mTitle == null ? "" : mTitle.toString();
    }
}
