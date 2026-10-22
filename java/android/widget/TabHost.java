package android.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.util.ArrayList;
import java.util.List;

public class TabHost extends FrameLayout implements ViewTreeObserver.OnTouchModeChangeListener
{
    private static final int ID_TABS = 0x01020013;
    private static final int ID_TAB_CONTENT = 0x01020011;
    private static final int ID_TITLE = 0x01020016;
    private static final int ID_ICON = 0x01020006;

    private static StyleableSet sStyleable;

    private final List<TabSpec> mTabSpecs = new ArrayList<>(2);
    private TabWidget mTabWidget;
    private FrameLayout mTabContent;
    private int mCurrentTab = -1;
    private View mCurrentView;
    private OnTabChangeListener mOnTabChangeListener;
    private OnKeyListener mTabKeyListener;
    private int mTabLayoutId;

    public TabHost(Context context)
    {
        super(context);

        initTabHost();
        mTabLayoutId = defaultTabLayout(context);
    }

    public TabHost(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "tabWidgetStyle"));
    }

    public TabHost(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public TabHost(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes);

        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "tabLayout");

        if (!sStyleable.isEmpty() && (attributes != null || defStyleAttr != 0 || defStyleRes != 0))
        {
            TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);
            int index = sStyleable.indexOf("tabLayout");
            if (index >= 0) mTabLayoutId = array.getResourceId(index, 0);
            array.recycle();
        }

        if (mTabLayoutId == 0) mTabLayoutId = defaultTabLayout(context);

        initTabHost();
    }

    private static int defaultTabLayout(Context context)
    {
        return context.getResources().getIdentifier("tab_indicator_holo", "layout", "android");
    }

    private void initTabHost()
    {
        setFocusableInTouchMode(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        mCurrentTab = -1;
        mCurrentView = null;
    }

    public TabSpec newTabSpec(String tag)
    {
        if (tag == null) throw new IllegalArgumentException("tag must be non-null");

        return new TabSpec(tag);
    }

    public void setup()
    {
        View tabs = findViewById(ID_TABS);
        if (!(tabs instanceof TabWidget))
        {
            throw new RuntimeException("Your TabHost must have a TabWidget whose id attribute is 'android.R.id.tabs'");
        }
        mTabWidget = (TabWidget)tabs;

        mTabKeyListener = new OnKeyListener()
        {
            public boolean onKey(View view, int keyCode, KeyEvent event)
            {
                if (KeyEvent.isModifierKey(keyCode)) return false;

                switch (keyCode)
                {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KeyEvent.KEYCODE_TAB:
                    case KeyEvent.KEYCODE_SPACE:
                    case KeyEvent.KEYCODE_ENTER:
                        return false;
                    default:
                        break;
                }

                mTabContent.requestFocus(View.FOCUS_FORWARD);
                return mTabContent.dispatchKeyEvent(event);
            }
        };

        mTabWidget.setTabSelectionListener(new TabWidget.OnTabSelectionChanged()
        {
            public void onTabSelectionChanged(int tabIndex, boolean clicked)
            {
                setCurrentTab(tabIndex);
                if (clicked) mTabContent.requestFocus(View.FOCUS_FORWARD);
            }
        });

        View content = findViewById(ID_TAB_CONTENT);
        if (!(content instanceof FrameLayout))
        {
            throw new RuntimeException("Your TabHost must have a FrameLayout whose id attribute is 'android.R.id.tabcontent'");
        }
        mTabContent = (FrameLayout)content;
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        getViewTreeObserver().addOnTouchModeChangeListener(this);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        getViewTreeObserver().removeOnTouchModeChangeListener(this);
    }

    public void onTouchModeChanged(boolean isInTouchMode)
    {
    }

    public void addTab(TabSpec tabSpec)
    {
        if (tabSpec.mIndicatorStrategy == null) throw new IllegalArgumentException("you must specify a way to create the tab indicator.");
        if (tabSpec.mContentStrategy == null) throw new IllegalArgumentException("you must specify a way to create the tab content");
        if (mTabWidget == null) throw new IllegalStateException("you must call setup() before adding tabs");

        View tabIndicator = tabSpec.mIndicatorStrategy.createIndicatorView();
        tabIndicator.setOnKeyListener(mTabKeyListener);

        if (tabSpec.mContentStrategy instanceof IntentContentStrategy) mTabWidget.setStripEnabled(false);

        mTabWidget.addView(tabIndicator);
        mTabSpecs.add(tabSpec);

        if (mCurrentTab == -1) setCurrentTab(0);
    }

    public void clearAllTabs()
    {
        if (mTabWidget != null) mTabWidget.removeAllViews();
        initTabHost();
        if (mTabContent != null) mTabContent.removeAllViews();
        mTabSpecs.clear();
        requestLayout();
        invalidate();
    }

    public TabWidget getTabWidget()
    {
        return mTabWidget;
    }

    public int getCurrentTab()
    {
        return mCurrentTab;
    }

    public String getCurrentTabTag()
    {
        if (mCurrentTab >= 0 && mCurrentTab < mTabSpecs.size()) return mTabSpecs.get(mCurrentTab).getTag();

        return null;
    }

    public View getCurrentTabView()
    {
        if (mCurrentTab >= 0 && mCurrentTab < mTabSpecs.size()) return mTabWidget.getChildTabViewAt(mCurrentTab);

        return null;
    }

    public View getCurrentView()
    {
        return mCurrentView;
    }

    public void setCurrentTabByTag(String tag)
    {
        int count = mTabSpecs.size();

        for (int index = 0; index < count; index++)
        {
            if (!mTabSpecs.get(index).getTag().equals(tag)) continue;

            setCurrentTab(index);
            return;
        }
    }

    public FrameLayout getTabContentView()
    {
        return mTabContent;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        boolean handled = super.dispatchKeyEvent(event);
        if (handled || event.getAction() != KeyEvent.ACTION_DOWN || mCurrentView == null || !mCurrentView.hasFocus()) return handled;
        if (event.getKeyCode() != KeyEvent.KEYCODE_DPAD_UP) return false;

        View focused = mCurrentView.findFocus();
        if (focused != null && focused.focusSearch(View.FOCUS_UP) != null) return false;

        View tab = mTabWidget.getChildTabViewAt(mCurrentTab);
        if (tab == null) return false;

        tab.requestFocus();
        playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
        return true;
    }

    @Override
    public void dispatchWindowFocusChanged(boolean hasFocus)
    {
        if (mCurrentView != null) mCurrentView.dispatchWindowFocusChanged(hasFocus);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return TabHost.class.getName();
    }

    public void setCurrentTab(int index)
    {
        if (index < 0 || index >= mTabSpecs.size()) return;
        if (index == mCurrentTab) return;

        if (mCurrentTab != -1) mTabSpecs.get(mCurrentTab).mContentStrategy.tabClosed();

        mCurrentTab = index;
        TabSpec spec = mTabSpecs.get(index);

        mTabWidget.focusCurrentTab(mCurrentTab);

        mCurrentView = spec.mContentStrategy.getContentView();

        if (mCurrentView.getParent() == null)
        {
            mTabContent.addView(mCurrentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                       ViewGroup.LayoutParams.MATCH_PARENT));
        }

        if (!mTabWidget.hasFocus()) mCurrentView.requestFocus();

        if (mOnTabChangeListener != null) mOnTabChangeListener.onTabChanged(getCurrentTabTag());
    }

    public void setOnTabChangedListener(OnTabChangeListener listener)
    {
        mOnTabChangeListener = listener;
    }

    public interface OnTabChangeListener
    {
        void onTabChanged(String tabId);
    }

    public interface TabContentFactory
    {
        View createTabContent(String tag);
    }

    public class TabSpec
    {
        private final String mTag;
        private IndicatorStrategy mIndicatorStrategy;
        private ContentStrategy mContentStrategy;

        TabSpec(String tag)
        {
            mTag = tag;
        }

        public TabSpec setIndicator(CharSequence label)
        {
            mIndicatorStrategy = new LabelIndicatorStrategy(label);
            return this;
        }

        public TabSpec setIndicator(CharSequence label, Drawable icon)
        {
            mIndicatorStrategy = new LabelAndIconIndicatorStrategy(label, icon);
            return this;
        }

        public TabSpec setIndicator(View view)
        {
            mIndicatorStrategy = new ViewIndicatorStrategy(view);
            return this;
        }

        public TabSpec setContent(int viewId)
        {
            mContentStrategy = new ViewIdContentStrategy(viewId);
            return this;
        }

        public TabSpec setContent(TabContentFactory contentFactory)
        {
            mContentStrategy = new FactoryContentStrategy(mTag, contentFactory);
            return this;
        }

        public TabSpec setContent(Intent intent)
        {
            mContentStrategy = new IntentContentStrategy(mTag, intent);
            return this;
        }

        public String getTag()
        {
            return mTag;
        }
    }

    private interface IndicatorStrategy
    {
        View createIndicatorView();
    }

    private interface ContentStrategy
    {
        View getContentView();

        void tabClosed();
    }

    private View inflateIndicator()
    {
        if (mTabLayoutId != 0)
        {
            try
            {
                return LayoutInflater.from(getContext()).inflate(mTabLayoutId, mTabWidget, false);
            }
            catch (RuntimeException missing)
            {
                android.util.Log.w("TabHost", "the tab indicator layout could not be inflated", missing);
            }
        }

        LinearLayout fallback = new LinearLayout(getContext());
        fallback.setOrientation(LinearLayout.VERTICAL);
        fallback.setGravity(android.view.Gravity.CENTER);

        ImageView icon = new ImageView(getContext());
        icon.setId(ID_ICON);
        icon.setVisibility(GONE);
        fallback.addView(icon);

        TextView title = new TextView(getContext());
        title.setId(ID_TITLE);
        title.setGravity(android.view.Gravity.CENTER);
        fallback.addView(title);

        int padding = (int)(8 * getResources().getDisplayMetrics().density);
        fallback.setPadding(padding, padding, padding, padding);
        return fallback;
    }

    private class LabelIndicatorStrategy implements IndicatorStrategy
    {
        private final CharSequence mLabel;

        private LabelIndicatorStrategy(CharSequence label)
        {
            mLabel = label;
        }

        public View createIndicatorView()
        {
            View tabIndicator = inflateIndicator();

            View title = tabIndicator.findViewById(ID_TITLE);
            if (title instanceof TextView) ((TextView)title).setText(mLabel);

            return tabIndicator;
        }
    }

    private class LabelAndIconIndicatorStrategy implements IndicatorStrategy
    {
        private final CharSequence mLabel;
        private final Drawable mIcon;

        private LabelAndIconIndicatorStrategy(CharSequence label, Drawable icon)
        {
            mLabel = label;
            mIcon = icon;
        }

        public View createIndicatorView()
        {
            View tabIndicator = inflateIndicator();

            View title = tabIndicator.findViewById(ID_TITLE);
            View icon = tabIndicator.findViewById(ID_ICON);

            boolean exclusive = icon != null && icon.getVisibility() == GONE;
            boolean bindIcon = !exclusive || TextUtils.isEmpty(mLabel);

            if (title instanceof TextView) ((TextView)title).setText(mLabel);

            if (bindIcon && mIcon != null && icon instanceof ImageView)
            {
                ((ImageView)icon).setImageDrawable(mIcon);
                icon.setVisibility(VISIBLE);
            }

            return tabIndicator;
        }
    }

    private static class ViewIndicatorStrategy implements IndicatorStrategy
    {
        private final View mView;

        private ViewIndicatorStrategy(View view)
        {
            mView = view;
        }

        public View createIndicatorView()
        {
            return mView;
        }
    }

    private class ViewIdContentStrategy implements ContentStrategy
    {
        private final View mView;

        private ViewIdContentStrategy(int viewId)
        {
            mView = mTabContent == null ? null : mTabContent.findViewById(viewId);
            if (mView == null) throw new RuntimeException("Could not create tab content because could not find view with id " + viewId);

            mView.setVisibility(View.GONE);
        }

        public View getContentView()
        {
            mView.setVisibility(View.VISIBLE);
            return mView;
        }

        public void tabClosed()
        {
            mView.setVisibility(View.GONE);
        }
    }

    private static class FactoryContentStrategy implements ContentStrategy
    {
        private View mTabContent;
        private final CharSequence mTag;
        private final TabContentFactory mFactory;

        private FactoryContentStrategy(CharSequence tag, TabContentFactory factory)
        {
            mTag = tag;
            mFactory = factory;
        }

        public View getContentView()
        {
            if (mTabContent == null) mTabContent = mFactory.createTabContent(mTag.toString());

            mTabContent.setVisibility(View.VISIBLE);
            return mTabContent;
        }

        public void tabClosed()
        {
            if (mTabContent != null) mTabContent.setVisibility(View.GONE);
        }
    }

    private static class IntentContentStrategy implements ContentStrategy
    {
        private final String mTag;
        private final Intent mIntent;

        private IntentContentStrategy(String tag, Intent intent)
        {
            mTag = tag;
            mIntent = intent;
        }

        public View getContentView()
        {
            throw new IllegalStateException("Did you forget to call 'public void setup(LocalActivityManager activityGroup)'?");
        }

        public void tabClosed()
        {
        }
    }
}
