package android.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

public abstract class ActionBar
{
    public static final int NAVIGATION_MODE_STANDARD = 0;
    public static final int NAVIGATION_MODE_LIST = 1;
    public static final int NAVIGATION_MODE_TABS = 2;

    public static final int DISPLAY_USE_LOGO = 0x1;
    public static final int DISPLAY_SHOW_HOME = 0x2;
    public static final int DISPLAY_HOME_AS_UP = 0x4;
    public static final int DISPLAY_SHOW_TITLE = 0x8;
    public static final int DISPLAY_SHOW_CUSTOM = 0x10;

    public ActionBar()
    {
    }

    public abstract void setCustomView(View view);

    public abstract void setCustomView(View view, LayoutParams layoutParams);

    public abstract void setCustomView(int resId);

    public abstract void setIcon(int resId);

    public abstract void setIcon(Drawable icon);

    public abstract void setLogo(int resId);

    public abstract void setLogo(Drawable logo);

    public abstract void setListNavigationCallbacks(SpinnerAdapter adapter, OnNavigationListener callback);

    public abstract void setSelectedNavigationItem(int position);

    public abstract int getSelectedNavigationIndex();

    public abstract int getNavigationItemCount();

    public abstract void setTitle(CharSequence title);

    public abstract void setTitle(int resId);

    public abstract void setSubtitle(CharSequence subtitle);

    public abstract void setSubtitle(int resId);

    public abstract void setDisplayOptions(int options);

    public abstract void setDisplayOptions(int options, int mask);

    public abstract void setDisplayUseLogoEnabled(boolean useLogo);

    public abstract void setDisplayShowHomeEnabled(boolean showHome);

    public abstract void setDisplayHomeAsUpEnabled(boolean showHomeAsUp);

    public abstract void setDisplayShowTitleEnabled(boolean showTitle);

    public abstract void setDisplayShowCustomEnabled(boolean showCustom);

    public abstract void setBackgroundDrawable(Drawable drawable);

    public void setStackedBackgroundDrawable(Drawable drawable)
    {
    }

    public void setSplitBackgroundDrawable(Drawable drawable)
    {
    }

    public abstract View getCustomView();

    public abstract CharSequence getTitle();

    public abstract CharSequence getSubtitle();

    public abstract int getNavigationMode();

    public abstract void setNavigationMode(int mode);

    public abstract int getDisplayOptions();

    public abstract Tab newTab();

    public abstract void addTab(Tab tab);

    public abstract void addTab(Tab tab, boolean setSelected);

    public abstract void addTab(Tab tab, int position);

    public abstract void addTab(Tab tab, int position, boolean setSelected);

    public abstract void removeTab(Tab tab);

    public abstract void removeTabAt(int position);

    public abstract void removeAllTabs();

    public abstract void selectTab(Tab tab);

    public abstract Tab getSelectedTab();

    public abstract Tab getTabAt(int index);

    public abstract int getTabCount();

    public abstract int getHeight();

    public abstract void show();

    public abstract void hide();

    public abstract boolean isShowing();

    public abstract void addOnMenuVisibilityListener(OnMenuVisibilityListener listener);

    public abstract void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener);

    public void setHomeButtonEnabled(boolean enabled)
    {
    }

    public Context getThemedContext()
    {
        return null;
    }

    public void setHomeAsUpIndicator(Drawable indicator)
    {
    }

    public void setHomeAsUpIndicator(int resId)
    {
    }

    public void setHomeActionContentDescription(CharSequence description)
    {
    }

    public void setHomeActionContentDescription(int resId)
    {
    }

    public void setHideOnContentScrollEnabled(boolean hideOnContentScroll)
    {
        if (hideOnContentScroll) throw new UnsupportedOperationException("Hide on content scroll is not supported by this action bar");
    }

    public boolean isHideOnContentScrollEnabled()
    {
        return false;
    }

    public int getHideOffset()
    {
        return 0;
    }

    public void setHideOffset(int offset)
    {
        if (offset != 0) throw new UnsupportedOperationException("An explicit hide offset is not supported by this action bar");
    }

    public void setElevation(float elevation)
    {
        if (elevation != 0) throw new UnsupportedOperationException("A non-zero elevation is not supported by this action bar");
    }

    public float getElevation()
    {
        return 0;
    }

    public interface OnNavigationListener
    {
        boolean onNavigationItemSelected(int itemPosition, long itemId);
    }

    public interface OnMenuVisibilityListener
    {
        void onMenuVisibilityChanged(boolean isVisible);
    }

    public abstract static class Tab
    {
        public static final int INVALID_POSITION = -1;

        public Tab()
        {
        }

        public abstract int getPosition();

        public abstract Drawable getIcon();

        public abstract CharSequence getText();

        public abstract Tab setIcon(Drawable icon);

        public abstract Tab setIcon(int resId);

        public abstract Tab setText(CharSequence text);

        public abstract Tab setText(int resId);

        public abstract Tab setCustomView(View view);

        public abstract Tab setCustomView(int layoutResId);

        public abstract View getCustomView();

        public abstract Tab setTag(Object obj);

        public abstract Object getTag();

        public abstract Tab setTabListener(TabListener listener);

        public abstract void select();

        public abstract Tab setContentDescription(int resId);

        public abstract Tab setContentDescription(CharSequence contentDesc);

        public abstract CharSequence getContentDescription();
    }

    public interface TabListener
    {
        void onTabSelected(Tab tab, FragmentTransaction transaction);

        void onTabUnselected(Tab tab, FragmentTransaction transaction);

        void onTabReselected(Tab tab, FragmentTransaction transaction);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams
    {
        private static StyleableSet sGravityStyleable;

        public int gravity = Gravity.NO_GRAVITY;

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);

            if (attributes == null) return;

            if (sGravityStyleable == null) sGravityStyleable = StyleableSet.of(context.getResources(), "layout_gravity");
            if (sGravityStyleable.isEmpty()) return;

            android.content.res.TypedArray array = context.obtainStyledAttributes(attributes, sGravityStyleable.attributes(), 0, 0);
            gravity = array.getInt(0, Gravity.NO_GRAVITY);
            array.recycle();
        }

        public LayoutParams(int width, int height)
        {
            super(width, height);
            gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        }

        public LayoutParams(int width, int height, int gravity)
        {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(int gravity)
        {
            this(WRAP_CONTENT, MATCH_PARENT, gravity);
        }

        public LayoutParams(LayoutParams source)
        {
            super(source);
            gravity = source.gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }
    }
}
