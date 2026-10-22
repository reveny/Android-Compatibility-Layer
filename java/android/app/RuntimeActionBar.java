package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.RuntimeMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class RuntimeActionBar extends ActionBar
{
    private static final int HOME_ITEM_ID = 0x0102002c;
    private static final int DEFAULT_PRIMARY = 0xFF3F51B5;

    private final Context mContext;
    private final Window.Callback mCallback;
    private final Toolbar mToolbar;
    private final LinearLayout mContainer;
    private final float mDensity;
    private final int mAccentColor;
    private final int mTextColor;
    private LinearLayout mTabStrip;

    private int mDisplayOptions = DISPLAY_SHOW_TITLE | DISPLAY_SHOW_HOME;
    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private Drawable mIcon;
    private Drawable mLogo;
    private Drawable mUpIndicator;
    private CharSequence mHomeDescription;
    private View mCustomView;
    private View mListNavigationView;
    private boolean mHomeButtonEnabled;
    private boolean mShowing = true;
    private boolean mHideOnContentScroll;
    private int mHideOffset;
    private float mElevation;

    private int mNavigationMode = NAVIGATION_MODE_STANDARD;
    private final List<TabImpl> mTabs = new ArrayList<>();
    private TabImpl mSelectedTab;
    private int mSavedTabPosition = Tab.INVALID_POSITION;
    private SpinnerAdapter mNavigationAdapter;
    private OnNavigationListener mNavigationListener;
    private int mSelectedNavigationIndex = -1;

    private final List<OnMenuVisibilityListener> mMenuVisibilityListeners = new ArrayList<>();
    private boolean mLastMenuVisibility;

    public RuntimeActionBar(Context context, Window.Callback callback)
    {
        mContext = context;
        mCallback = callback;
        mDensity = context.getResources().getDisplayMetrics().density;
        mAccentColor = themeColor(context, "colorAccent", 0xFFFF4081);
        mTextColor = themeColor(context, "textColorPrimary", 0xDE000000);

        mToolbar = new Toolbar(context);
        mToolbar.setBackgroundColor(themeColor(context, "colorPrimary", DEFAULT_PRIMARY));

        mContainer = new LinearLayout(context);
        mContainer.setOrientation(LinearLayout.VERTICAL);
        int matchParent = ViewGroup.LayoutParams.MATCH_PARENT;
        mContainer.addView(mToolbar, new LinearLayout.LayoutParams(matchParent, ViewGroup.LayoutParams.WRAP_CONTENT));

        attachToolbar();
    }

    public RuntimeActionBar(Toolbar toolbar, CharSequence title, Window.Callback callback)
    {
        mContext = toolbar.getContext();
        mCallback = callback;
        mDensity = mContext.getResources().getDisplayMetrics().density;
        mAccentColor = themeColor(mContext, "colorAccent", 0xFFFF4081);
        mTextColor = themeColor(mContext, "textColorPrimary", 0xDE000000);
        mToolbar = toolbar;
        mContainer = null;
        mTitle = toolbar.getTitle() != null ? toolbar.getTitle() : title;
        mSubtitle = toolbar.getSubtitle();

        if (toolbar.getNavigationIcon() != null) mDisplayOptions |= DISPLAY_HOME_AS_UP;

        attachToolbar();
    }

    private static int themeColor(Context context, String name, int fallback)
    {
        int identifier = context.getResources().getIdentifier(name, "attr", "android");
        if (identifier == 0) return fallback;

        try
        {
            TypedArray array = context.obtainStyledAttributes(new int[] {identifier});
            boolean present = array.hasValue(0);
            int color = array.getColor(0, fallback);
            array.recycle();

            return present ? color : fallback;
        }
        catch (RuntimeException unresolved)
        {
            return fallback;
        }
    }

    private int dp(int value)
    {
        return (int)(value * mDensity + 0.5f);
    }

    private void attachToolbar()
    {
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                return mCallback != null && mCallback.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
            }
        });

        mToolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                dispatchHomeSelected();
            }
        });

        updateToolbar();
    }

    public View getView()
    {
        return mContainer != null ? mContainer : mToolbar;
    }

    public Toolbar getToolbar()
    {
        return mToolbar;
    }

    public Menu getMenu()
    {
        return mToolbar.getMenu();
    }

    public void invalidateOptionsMenu()
    {
        Menu menu = mToolbar.getMenu();
        menu.clear();

        if (mCallback != null && mCallback.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu))
        {
            mCallback.onPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, menu);
        }

        mToolbar.requestLayout();
        mToolbar.invalidate();
    }

    public boolean openOptionsMenu()
    {
        boolean shown = mToolbar.showOverflowMenu();
        if (shown) dispatchMenuVisibilityChanged(true);

        return shown;
    }

    public boolean closeOptionsMenu()
    {
        boolean hidden = mToolbar.hideOverflowMenu();
        if (hidden) dispatchMenuVisibilityChanged(false);

        return hidden;
    }

    public void dispatchMenuVisibilityChanged(boolean isVisible)
    {
        if (isVisible == mLastMenuVisibility) return;

        mLastMenuVisibility = isVisible;

        for (OnMenuVisibilityListener listener : new ArrayList<>(mMenuVisibilityListeners)) listener.onMenuVisibilityChanged(isVisible);
    }

    private boolean dispatchHomeSelected()
    {
        if (mCallback == null) return false;

        RuntimeMenu menu = new RuntimeMenu(mContext);
        MenuItem home = menu.add(0, HOME_ITEM_ID, 0, mTitle);

        return mCallback.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, home);
    }

    private void updateToolbar()
    {
        boolean showTitle = (mDisplayOptions & DISPLAY_SHOW_TITLE) != 0;

        mToolbar.setTitle(showTitle ? mTitle : null);
        mToolbar.setSubtitle(showTitle ? mSubtitle : null);

        if ((mDisplayOptions & DISPLAY_HOME_AS_UP) != 0)
        {
            mToolbar.setNavigationIcon(mUpIndicator != null ? mUpIndicator : new UpIndicatorDrawable(mTextColor, mDensity));
            mToolbar.setNavigationContentDescription(mHomeDescription != null ? mHomeDescription : "Navigate up");
        }
        else
        {
            mToolbar.setNavigationIcon(null);
        }

        if ((mDisplayOptions & DISPLAY_SHOW_HOME) != 0)
        {
            Drawable home = (mDisplayOptions & DISPLAY_USE_LOGO) != 0 && mLogo != null ? mLogo : mIcon;
            mToolbar.setLogo(home);
        }
        else
        {
            mToolbar.setLogo(null);
        }

        boolean showCustom = (mDisplayOptions & DISPLAY_SHOW_CUSTOM) != 0 && mCustomView != null;
        attachToToolbar(mCustomView, showCustom);
        attachToToolbar(mListNavigationView, mNavigationMode == NAVIGATION_MODE_LIST && mListNavigationView != null);

        if (mTabStrip != null) mTabStrip.setVisibility(mNavigationMode == NAVIGATION_MODE_TABS ? View.VISIBLE : View.GONE);
    }

    private void attachToToolbar(View view, boolean attach)
    {
        if (view == null) return;

        if (attach)
        {
            if (view.getParent() == mToolbar) return;
            if (view.getParent() instanceof ViewGroup) ((ViewGroup)view.getParent()).removeView(view);

            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (!(params instanceof ActionBar.LayoutParams))
            {
                params = new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }

            mToolbar.addView(view, params);
            return;
        }

        if (view.getParent() == mToolbar) mToolbar.removeView(view);
    }

    @Override
    public void setCustomView(View view)
    {
        if (view != null && !(view.getLayoutParams() instanceof ActionBar.LayoutParams))
        {
            view.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        replaceCustomView(view);
    }

    @Override
    public void setCustomView(View view, LayoutParams layoutParams)
    {
        if (view != null) view.setLayoutParams(layoutParams);

        replaceCustomView(view);
    }

    @Override
    public void setCustomView(int resId)
    {
        setCustomView(LayoutInflater.from(getThemedContext()).inflate(resId, mToolbar, false));
    }

    private void replaceCustomView(View view)
    {
        if (mCustomView != null && mCustomView.getParent() == mToolbar) mToolbar.removeView(mCustomView);

        mCustomView = view;
        updateToolbar();
    }

    @Override
    public void setIcon(int resId)
    {
        setIcon(resId == 0 ? null : mContext.getResources().getDrawable(resId));
    }

    @Override
    public void setIcon(Drawable icon)
    {
        mIcon = icon;
        updateToolbar();
    }

    @Override
    public void setLogo(int resId)
    {
        setLogo(resId == 0 ? null : mContext.getResources().getDrawable(resId));
    }

    @Override
    public void setLogo(Drawable logo)
    {
        mLogo = logo;
        updateToolbar();
    }

    @Override
    public void setListNavigationCallbacks(SpinnerAdapter adapter, OnNavigationListener callback)
    {
        mNavigationAdapter = adapter;
        mNavigationListener = callback;

        if (mSelectedNavigationIndex < 0 && adapter != null && adapter.getCount() > 0) mSelectedNavigationIndex = 0;

        refreshListNavigationView();
    }

    private void refreshListNavigationView()
    {
        if (mListNavigationView != null && mListNavigationView.getParent() == mToolbar) mToolbar.removeView(mListNavigationView);

        mListNavigationView = null;

        if (mNavigationAdapter != null && mSelectedNavigationIndex >= 0 && mSelectedNavigationIndex < mNavigationAdapter.getCount())
        {
            View selected = mNavigationAdapter.getView(mSelectedNavigationIndex, null, mToolbar);
            selected.setClickable(true);
            selected.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    showNavigationList();
                }
            });

            mListNavigationView = selected;
        }

        updateToolbar();
    }

    private void showNavigationList()
    {
        if (mNavigationAdapter == null) return;

        int count = mNavigationAdapter.getCount();
        CharSequence[] labels = new CharSequence[count];

        for (int index = 0; index < count; index++)
        {
            Object item = mNavigationAdapter.getItem(index);
            labels[index] = item == null ? "" : item.toString();
        }

        try
        {
            new AlertDialog.Builder(mContext).setItems(labels, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                    setSelectedNavigationItem(which);
                }
            }).show();
        }
        catch (RuntimeException unavailable)
        {
            android.util.Log.w("ActionBar", "list navigation could not be shown", unavailable);
        }
    }

    @Override
    public void setSelectedNavigationItem(int position)
    {
        if (mNavigationMode == NAVIGATION_MODE_TABS)
        {
            selectTab(mTabs.get(position));
            return;
        }

        if (mNavigationMode != NAVIGATION_MODE_LIST)
        {
            throw new IllegalStateException("setSelectedNavigationIndex not valid for current navigation mode");
        }

        if (mNavigationAdapter == null || position < 0 || position >= mNavigationAdapter.getCount()) return;

        mSelectedNavigationIndex = position;
        refreshListNavigationView();

        if (mNavigationListener != null) mNavigationListener.onNavigationItemSelected(position, mNavigationAdapter.getItemId(position));
    }

    @Override
    public int getSelectedNavigationIndex()
    {
        if (mNavigationMode == NAVIGATION_MODE_TABS) return mSelectedTab != null ? mSelectedTab.getPosition() : -1;
        if (mNavigationMode == NAVIGATION_MODE_LIST) return mSelectedNavigationIndex;

        return -1;
    }

    @Override
    public int getNavigationItemCount()
    {
        if (mNavigationMode == NAVIGATION_MODE_TABS) return mTabs.size();
        if (mNavigationMode == NAVIGATION_MODE_LIST) return mNavigationAdapter != null ? mNavigationAdapter.getCount() : 0;

        return 0;
    }

    @Override
    public void setTitle(CharSequence title)
    {
        mTitle = title;
        updateToolbar();
    }

    @Override
    public void setTitle(int resId)
    {
        setTitle(resId == 0 ? null : mContext.getText(resId));
    }

    @Override
    public void setSubtitle(CharSequence subtitle)
    {
        mSubtitle = subtitle;
        updateToolbar();
    }

    @Override
    public void setSubtitle(int resId)
    {
        setSubtitle(resId == 0 ? null : mContext.getText(resId));
    }

    @Override
    public void setDisplayOptions(int options)
    {
        mDisplayOptions = options;
        updateToolbar();
    }

    @Override
    public void setDisplayOptions(int options, int mask)
    {
        setDisplayOptions((options & mask) | (mDisplayOptions & ~mask));
    }

    @Override
    public void setDisplayUseLogoEnabled(boolean useLogo)
    {
        setDisplayOptions(useLogo ? DISPLAY_USE_LOGO : 0, DISPLAY_USE_LOGO);
    }

    @Override
    public void setDisplayShowHomeEnabled(boolean showHome)
    {
        setDisplayOptions(showHome ? DISPLAY_SHOW_HOME : 0, DISPLAY_SHOW_HOME);
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp)
    {
        setDisplayOptions(showHomeAsUp ? DISPLAY_HOME_AS_UP : 0, DISPLAY_HOME_AS_UP);
    }

    @Override
    public void setDisplayShowTitleEnabled(boolean showTitle)
    {
        setDisplayOptions(showTitle ? DISPLAY_SHOW_TITLE : 0, DISPLAY_SHOW_TITLE);
    }

    @Override
    public void setDisplayShowCustomEnabled(boolean showCustom)
    {
        setDisplayOptions(showCustom ? DISPLAY_SHOW_CUSTOM : 0, DISPLAY_SHOW_CUSTOM);
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable)
    {
        mToolbar.setBackground(drawable);
    }

    @Override
    public void setStackedBackgroundDrawable(Drawable drawable)
    {
        if (mTabStrip != null) mTabStrip.setBackground(drawable);
    }

    @Override
    public View getCustomView()
    {
        return mCustomView;
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
    public int getNavigationMode()
    {
        return mNavigationMode;
    }

    @Override
    public void setNavigationMode(int mode)
    {
        int oldMode = mNavigationMode;
        if (oldMode == mode) return;

        if (oldMode == NAVIGATION_MODE_TABS)
        {
            mSavedTabPosition = getSelectedNavigationIndex();
            selectTab(null);
        }

        mNavigationMode = mode;

        if (mode == NAVIGATION_MODE_TABS)
        {
            ensureTabStrip();

            if (mSavedTabPosition != Tab.INVALID_POSITION && mSavedTabPosition < mTabs.size())
            {
                setSelectedNavigationItem(mSavedTabPosition);
                mSavedTabPosition = Tab.INVALID_POSITION;
            }
            else if (mSelectedTab == null && !mTabs.isEmpty())
            {
                selectTab(mTabs.get(0));
            }
        }

        updateToolbar();
    }

    @Override
    public int getDisplayOptions()
    {
        return mDisplayOptions;
    }

    @Override
    public Tab newTab()
    {
        return new TabImpl();
    }

    @Override
    public void addTab(Tab tab)
    {
        addTab(tab, mTabs.isEmpty());
    }

    @Override
    public void addTab(Tab tab, boolean setSelected)
    {
        addTab(tab, mTabs.size(), setSelected);
    }

    @Override
    public void addTab(Tab tab, int position)
    {
        addTab(tab, position, mTabs.isEmpty());
    }

    @Override
    public void addTab(Tab tab, int position, boolean setSelected)
    {
        TabImpl implementation = (TabImpl)tab;
        if (implementation.mCallback == null) throw new IllegalStateException("Action Bar Tab must have a Callback");

        mTabs.add(position, implementation);
        renumberTabs();
        rebuildTabStrip();

        if (setSelected) selectTab(tab);
    }

    @Override
    public void removeTab(Tab tab)
    {
        removeTabAt(tab.getPosition());
    }

    @Override
    public void removeTabAt(int position)
    {
        if (position < 0 || position >= mTabs.size()) return;

        int selectedPosition = mSelectedTab != null ? mSelectedTab.getPosition() : mSavedTabPosition;
        TabImpl removed = mTabs.remove(position);
        removed.setPosition(Tab.INVALID_POSITION);

        renumberTabs();
        rebuildTabStrip();

        if (selectedPosition == position) selectTab(mTabs.isEmpty() ? null : mTabs.get(Math.max(0, position - 1)));
    }

    @Override
    public void removeAllTabs()
    {
        if (mSelectedTab != null) selectTab(null);

        for (TabImpl tab : mTabs) tab.setPosition(Tab.INVALID_POSITION);

        mTabs.clear();
        mSavedTabPosition = Tab.INVALID_POSITION;
        rebuildTabStrip();
    }

    @Override
    public void selectTab(Tab tab)
    {
        if (mNavigationMode != NAVIGATION_MODE_TABS)
        {
            mSavedTabPosition = tab != null ? tab.getPosition() : Tab.INVALID_POSITION;
            return;
        }

        FragmentTransaction transaction = beginTransaction();

        if (mSelectedTab == tab)
        {
            if (mSelectedTab != null) mSelectedTab.mCallback.onTabReselected(mSelectedTab, transaction);
        }
        else
        {
            if (mSelectedTab != null) mSelectedTab.mCallback.onTabUnselected(mSelectedTab, transaction);

            mSelectedTab = (TabImpl)tab;

            if (mSelectedTab != null) mSelectedTab.mCallback.onTabSelected(mSelectedTab, transaction);
        }

        commit(transaction);
        rebuildTabStrip();
    }

    private FragmentTransaction beginTransaction()
    {
        if (!(mContext instanceof Activity)) return null;

        try
        {
            FragmentManager manager = ((Activity)mContext).getFragmentManager();
            return manager == null ? null : manager.beginTransaction();
        }
        catch (RuntimeException unavailable)
        {
            return null;
        }
    }

    private static void commit(FragmentTransaction transaction)
    {
        if (transaction == null) return;

        try
        {
            transaction.commit();
        }
        catch (RuntimeException failed)
        {
            android.util.Log.w("ActionBar", "tab fragment transaction failed", failed);
        }
    }

    @Override
    public Tab getSelectedTab()
    {
        return mSelectedTab;
    }

    @Override
    public Tab getTabAt(int index)
    {
        return mTabs.get(index);
    }

    @Override
    public int getTabCount()
    {
        return mTabs.size();
    }

    private void renumberTabs()
    {
        for (int index = 0; index < mTabs.size(); index++) mTabs.get(index).setPosition(index);
    }

    private void ensureTabStrip()
    {
        if (mTabStrip != null || mContainer == null) return;

        mTabStrip = new LinearLayout(mContext);
        mTabStrip.setOrientation(LinearLayout.HORIZONTAL);
        mTabStrip.setBackground(mToolbar.getBackground() != null ? mToolbar.getBackground() : new ColorDrawable(DEFAULT_PRIMARY));
        mContainer.addView(mTabStrip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        rebuildTabStrip();
    }

    private void rebuildTabStrip()
    {
        if (mTabStrip == null) return;

        mTabStrip.removeAllViews();

        for (final TabImpl tab : mTabs)
        {
            View view = tab.getCustomView();

            if (view == null)
            {
                TextView label = new TextView(mContext);
                label.setText(tab.getText());
                label.setPadding(dp(12), dp(14), dp(12), dp(14));
                label.setTextColor(tab == mSelectedTab ? mAccentColor : mTextColor);
                label.setContentDescription(tab.getContentDescription());
                view = label;
            }
            else if (view.getParent() instanceof ViewGroup)
            {
                ((ViewGroup)view.getParent()).removeView(view);
            }

            view.setClickable(true);
            view.setSelected(tab == mSelectedTab);
            view.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View clicked)
                {
                    tab.select();
                }
            });

            mTabStrip.addView(view, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
    }

    @Override
    public int getHeight()
    {
        return mContainer != null ? mContainer.getHeight() : mToolbar.getHeight();
    }

    @Override
    public void show()
    {
        mShowing = true;
        getView().setVisibility(View.VISIBLE);
    }

    @Override
    public void hide()
    {
        mShowing = false;
        getView().setVisibility(View.GONE);
    }

    @Override
    public boolean isShowing()
    {
        return mShowing && getView().getVisibility() == View.VISIBLE;
    }

    @Override
    public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener)
    {
        if (listener != null && !mMenuVisibilityListeners.contains(listener)) mMenuVisibilityListeners.add(listener);
    }

    @Override
    public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener)
    {
        mMenuVisibilityListeners.remove(listener);
    }

    @Override
    public void setHomeButtonEnabled(boolean enabled)
    {
        mHomeButtonEnabled = enabled;
    }

    public boolean isHomeButtonEnabled()
    {
        return mHomeButtonEnabled;
    }

    @Override
    public Context getThemedContext()
    {
        return mToolbar.getContext();
    }

    @Override
    public void setHomeAsUpIndicator(Drawable indicator)
    {
        mUpIndicator = indicator;
        updateToolbar();
    }

    @Override
    public void setHomeAsUpIndicator(int resId)
    {
        setHomeAsUpIndicator(resId == 0 ? null : mContext.getResources().getDrawable(resId));
    }

    @Override
    public void setHomeActionContentDescription(CharSequence description)
    {
        mHomeDescription = description;
        updateToolbar();
    }

    @Override
    public void setHomeActionContentDescription(int resId)
    {
        setHomeActionContentDescription(resId == 0 ? null : mContext.getText(resId));
    }

    @Override
    public void setHideOnContentScrollEnabled(boolean hideOnContentScroll)
    {
        mHideOnContentScroll = hideOnContentScroll;
    }

    @Override
    public boolean isHideOnContentScrollEnabled()
    {
        return mHideOnContentScroll;
    }

    @Override
    public int getHideOffset()
    {
        return mHideOffset;
    }

    @Override
    public void setHideOffset(int offset)
    {
        mHideOffset = Math.max(0, Math.min(offset, getHeight()));
        getView().setTranslationY(-mHideOffset);
    }

    @Override
    public void setElevation(float elevation)
    {
        mElevation = elevation;
        getView().setElevation(elevation);
    }

    @Override
    public float getElevation()
    {
        return mElevation;
    }

    private final class TabImpl extends Tab
    {
        private TabListener mCallback;
        private Object mTag;
        private Drawable mTabIcon;
        private CharSequence mText;
        private CharSequence mContentDescription;
        private int mPosition = INVALID_POSITION;
        private View mTabCustomView;

        @Override
        public int getPosition()
        {
            return mPosition;
        }

        void setPosition(int position)
        {
            mPosition = position;
        }

        @Override
        public Drawable getIcon()
        {
            return mTabIcon;
        }

        @Override
        public CharSequence getText()
        {
            return mText;
        }

        @Override
        public Tab setIcon(Drawable icon)
        {
            mTabIcon = icon;
            if (mPosition >= 0) rebuildTabStrip();
            return this;
        }

        @Override
        public Tab setIcon(int resId)
        {
            return setIcon(resId == 0 ? null : mContext.getResources().getDrawable(resId));
        }

        @Override
        public Tab setText(CharSequence text)
        {
            mText = text;
            if (mPosition >= 0) rebuildTabStrip();
            return this;
        }

        @Override
        public Tab setText(int resId)
        {
            return setText(resId == 0 ? null : mContext.getText(resId));
        }

        @Override
        public Tab setCustomView(View view)
        {
            mTabCustomView = view;
            if (mPosition >= 0) rebuildTabStrip();
            return this;
        }

        @Override
        public Tab setCustomView(int layoutResId)
        {
            return setCustomView(LayoutInflater.from(getThemedContext()).inflate(layoutResId, null));
        }

        @Override
        public View getCustomView()
        {
            return mTabCustomView;
        }

        @Override
        public Tab setTag(Object tag)
        {
            mTag = tag;
            return this;
        }

        @Override
        public Object getTag()
        {
            return mTag;
        }

        @Override
        public Tab setTabListener(TabListener listener)
        {
            mCallback = listener;
            return this;
        }

        @Override
        public void select()
        {
            selectTab(this);
        }

        @Override
        public Tab setContentDescription(int resId)
        {
            return setContentDescription(resId == 0 ? null : mContext.getText(resId));
        }

        @Override
        public Tab setContentDescription(CharSequence contentDescription)
        {
            mContentDescription = contentDescription;
            if (mPosition >= 0) rebuildTabStrip();
            return this;
        }

        @Override
        public CharSequence getContentDescription()
        {
            return mContentDescription;
        }
    }

    private static final class UpIndicatorDrawable extends Drawable
    {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mPath = new Path();
        private final float mDensity;

        UpIndicatorDrawable(int color, float density)
        {
            mDensity = density;
            mPaint.setColor(color);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(2 * density);
            mPaint.setStrokeCap(Paint.Cap.SQUARE);
        }

        @Override
        public void draw(Canvas canvas)
        {
            Rect bounds = getBounds();
            float centreX = bounds.exactCenterX();
            float centreY = bounds.exactCenterY();
            float half = 8 * mDensity;

            mPath.reset();
            mPath.moveTo(centreX + half, centreY);
            mPath.lineTo(centreX - half, centreY);
            mPath.moveTo(centreX - half + 7 * mDensity, centreY - 7 * mDensity);
            mPath.lineTo(centreX - half, centreY);
            mPath.lineTo(centreX - half + 7 * mDensity, centreY + 7 * mDensity);

            canvas.drawPath(mPath, mPaint);
        }

        @Override
        public void setAlpha(int alpha)
        {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter)
        {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity()
        {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth()
        {
            return (int)(24 * mDensity);
        }

        @Override
        public int getIntrinsicHeight()
        {
            return (int)(24 * mDensity);
        }
    }
}
