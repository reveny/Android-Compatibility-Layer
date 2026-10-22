package android.widget;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.RuntimeMenu;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class Toolbar extends ViewGroup
{
    public interface OnMenuItemClickListener
    {
        boolean onMenuItemClick(MenuItem item);
    }

    private static StyleableSet sStyleable;
    private static StyleableSet sAppearanceStyleable;

    private ImageButton mNavButtonView;
    private ImageView mLogoView;
    private TextView mTitleTextView;
    private TextView mSubtitleTextView;
    private LinearLayout mMenuView;
    private ImageButton mOverflowButton;
    private RuntimeMenu mMenu;
    private AlertDialog mOverflowDialog;

    private Drawable mNavigationIcon;
    private CharSequence mNavigationContentDescription;
    private Drawable mCollapseIcon;
    private CharSequence mCollapseContentDescription;
    private Drawable mLogo;
    private CharSequence mLogoDescription;
    private Drawable mOverflowIcon;

    private CharSequence mTitleText;
    private CharSequence mSubtitleText;
    private int mTitleTextColor;
    private int mSubtitleTextColor;
    private boolean mTitleColorSet;
    private boolean mSubtitleColorSet;
    private int mTitleTextAppearance;
    private int mSubtitleTextAppearance;

    private int mPopupTheme;
    private int mGravity = Gravity.START | Gravity.CENTER_VERTICAL;
    private int mMaxButtonHeight;
    private int mBarHeight;

    private int mTitleMarginStart;
    private int mTitleMarginEnd;
    private int mTitleMarginTop;
    private int mTitleMarginBottom;

    private int mContentInsetStart;
    private int mContentInsetEnd;
    private int mContentInsetLeft = Integer.MIN_VALUE;
    private int mContentInsetRight = Integer.MIN_VALUE;
    private boolean mExplicitInsetsAbsolute;
    private int mContentInsetStartWithNavigation;
    private int mContentInsetEndWithActions;

    private OnMenuItemClickListener mOnMenuItemClickListener;
    private final List<Object> mMenuSignature = new ArrayList<>();
    private final List<MenuItem> mOverflowItems = new ArrayList<>();
    private final float mDensity;

    public Toolbar(Context context)
    {
        this(context, null);
    }

    public Toolbar(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "toolbarStyle"));
    }

    public Toolbar(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public Toolbar(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mDensity = context.getResources().getDisplayMetrics().density;
        mContentInsetStart = dp(16);
        mContentInsetStartWithNavigation = dp(72);
        mTitleMarginStart = dp(4);
        mTitleMarginEnd = dp(4);
        mBarHeight = resolveThemeDimension(context, "actionBarSize", dp(56));
        mTitleTextColor = ProgressBar.resolveThemeColor(context, 0xDE000000, "textColorPrimary");
        mSubtitleTextColor = ProgressBar.resolveThemeColor(context, 0x8A000000, "textColorSecondary");

        StyleableSet styleable = styleable(context);
        if (styleable.isEmpty() || (attributes == null && defStyleAttr == 0 && defStyleRes == 0)) return;

        TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);
        readAttributes(context, array, styleable);
        array.recycle();
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "title", "subtitle", "titleTextAppearance", "subtitleTextAppearance",
                                         "titleTextColor", "subtitleTextColor", "navigationIcon", "navigationContentDescription", "logo",
                                         "logoDescription", "contentInsetStart", "contentInsetEnd", "contentInsetLeft", "contentInsetRight",
                                         "contentInsetStartWithNavigation", "contentInsetEndWithActions", "titleMargin", "titleMarginStart",
                                         "titleMarginEnd", "titleMarginTop", "titleMarginBottom", "maxButtonHeight", "popupTheme",
                                         "gravity", "collapseIcon", "collapseContentDescription", "minHeight");
        }
        return sStyleable;
    }

    private void readAttributes(Context context, TypedArray array, StyleableSet styleable)
    {
        int index = styleable.indexOf("titleTextAppearance");
        if (index >= 0) mTitleTextAppearance = array.getResourceId(index, 0);

        index = styleable.indexOf("subtitleTextAppearance");
        if (index >= 0) mSubtitleTextAppearance = array.getResourceId(index, 0);

        index = styleable.indexOf("gravity");
        if (index >= 0) mGravity = array.getInt(index, mGravity);

        index = styleable.indexOf("titleMargin");
        if (index >= 0 && array.hasValue(index))
        {
            int margin = array.getDimensionPixelOffset(index, 0);
            mTitleMarginStart = margin;
            mTitleMarginEnd = margin;
            mTitleMarginTop = margin;
            mTitleMarginBottom = margin;
        }

        mTitleMarginStart = readOffset(array, styleable, "titleMarginStart", mTitleMarginStart);
        mTitleMarginEnd = readOffset(array, styleable, "titleMarginEnd", mTitleMarginEnd);
        mTitleMarginTop = readOffset(array, styleable, "titleMarginTop", mTitleMarginTop);
        mTitleMarginBottom = readOffset(array, styleable, "titleMarginBottom", mTitleMarginBottom);

        index = styleable.indexOf("maxButtonHeight");
        if (index >= 0) mMaxButtonHeight = array.getDimensionPixelSize(index, 0);

        mContentInsetStart = readOffset(array, styleable, "contentInsetStart", mContentInsetStart);
        mContentInsetEnd = readOffset(array, styleable, "contentInsetEnd", mContentInsetEnd);
        mContentInsetStartWithNavigation =
            readOffset(array, styleable, "contentInsetStartWithNavigation", mContentInsetStartWithNavigation);
        mContentInsetEndWithActions = readOffset(array, styleable, "contentInsetEndWithActions", mContentInsetEndWithActions);

        index = styleable.indexOf("contentInsetLeft");
        if (index >= 0 && array.hasValue(index))
        {
            mContentInsetLeft = array.getDimensionPixelSize(index, 0);
            mExplicitInsetsAbsolute = true;
        }

        index = styleable.indexOf("contentInsetRight");
        if (index >= 0 && array.hasValue(index))
        {
            mContentInsetRight = array.getDimensionPixelSize(index, 0);
            mExplicitInsetsAbsolute = true;
        }

        index = styleable.indexOf("minHeight");
        if (index >= 0 && array.hasValue(index))
        {
            int minimum = array.getDimensionPixelSize(index, 0);
            if (minimum > 0) setMinimumHeight(minimum);
        }

        index = styleable.indexOf("collapseIcon");
        if (index >= 0 && array.hasValue(index)) mCollapseIcon = loadDrawable(array, index);

        index = styleable.indexOf("collapseContentDescription");
        if (index >= 0) mCollapseContentDescription = array.getText(index);

        index = styleable.indexOf("popupTheme");
        if (index >= 0) mPopupTheme = array.getResourceId(index, 0);

        index = styleable.indexOf("titleTextColor");
        if (index >= 0 && array.hasValue(index)) setTitleTextColor(array.getColor(index, mTitleTextColor));

        index = styleable.indexOf("subtitleTextColor");
        if (index >= 0 && array.hasValue(index)) setSubtitleTextColor(array.getColor(index, mSubtitleTextColor));

        index = styleable.indexOf("title");
        if (index >= 0)
        {
            CharSequence title = array.getText(index);
            if (!TextUtils.isEmpty(title)) setTitle(title);
        }

        index = styleable.indexOf("subtitle");
        if (index >= 0)
        {
            CharSequence subtitle = array.getText(index);
            if (!TextUtils.isEmpty(subtitle)) setSubtitle(subtitle);
        }

        index = styleable.indexOf("navigationIcon");
        if (index >= 0 && array.hasValue(index))
        {
            Drawable icon = loadDrawable(array, index);
            if (icon != null) setNavigationIcon(icon);
        }

        index = styleable.indexOf("navigationContentDescription");
        if (index >= 0)
        {
            CharSequence description = array.getText(index);
            if (!TextUtils.isEmpty(description)) setNavigationContentDescription(description);
        }

        index = styleable.indexOf("logo");
        if (index >= 0 && array.hasValue(index))
        {
            Drawable logo = loadDrawable(array, index);
            if (logo != null) setLogo(logo);
        }

        index = styleable.indexOf("logoDescription");
        if (index >= 0)
        {
            CharSequence description = array.getText(index);
            if (!TextUtils.isEmpty(description)) setLogoDescription(description);
        }
    }

    private static int readOffset(TypedArray array, StyleableSet styleable, String name, int fallback)
    {
        int index = styleable.indexOf(name);
        if (index < 0 || !array.hasValue(index)) return fallback;

        return array.getDimensionPixelOffset(index, fallback);
    }

    private static Drawable loadDrawable(TypedArray array, int index)
    {
        try
        {
            return array.getDrawable(index);
        }
        catch (Throwable unsupported)
        {
            return null;
        }
    }

    private static int resolveThemeDimension(Context context, String name, int fallback)
    {
        int identifier = ProgressBar.attributeIdentifier(context, name);
        if (identifier == 0) return fallback;

        try
        {
            TypedArray array = context.obtainStyledAttributes(new int[] {identifier});
            int value = array.getDimensionPixelSize(0, fallback);
            array.recycle();

            return value > 0 ? value : fallback;
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

    private Drawable resolveDrawable(int resourceId)
    {
        if (resourceId == 0) return null;

        try
        {
            return getContext().getResources().getDrawable(resourceId);
        }
        catch (Throwable missing)
        {
            android.util.Log.w("Toolbar", "drawable 0x" + Integer.toHexString(resourceId) + " did not load", missing);
            return null;
        }
    }

    private CharSequence resolveText(int resourceId)
    {
        return resourceId == 0 ? null : getContext().getText(resourceId);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
    }

    public void setPopupTheme(int resId)
    {
        mPopupTheme = resId;
    }

    public int getPopupTheme()
    {
        return mPopupTheme;
    }

    public void setTitleMargin(int start, int top, int end, int bottom)
    {
        mTitleMarginStart = start;
        mTitleMarginTop = top;
        mTitleMarginEnd = end;
        mTitleMarginBottom = bottom;

        requestLayout();
    }

    public int getTitleMarginStart()
    {
        return mTitleMarginStart;
    }

    public void setTitleMarginStart(int margin)
    {
        mTitleMarginStart = margin;
        requestLayout();
    }

    public int getTitleMarginTop()
    {
        return mTitleMarginTop;
    }

    public void setTitleMarginTop(int margin)
    {
        mTitleMarginTop = margin;
        requestLayout();
    }

    public int getTitleMarginEnd()
    {
        return mTitleMarginEnd;
    }

    public void setTitleMarginEnd(int margin)
    {
        mTitleMarginEnd = margin;
        requestLayout();
    }

    public int getTitleMarginBottom()
    {
        return mTitleMarginBottom;
    }

    public void setTitleMarginBottom(int margin)
    {
        mTitleMarginBottom = margin;
        requestLayout();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection)
    {
        super.onRtlPropertiesChanged(layoutDirection);
        requestLayout();
    }

    public void setLogo(int resId)
    {
        setLogo(resolveDrawable(resId));
    }

    public boolean isOverflowMenuShowing()
    {
        return mOverflowDialog != null && mOverflowDialog.isShowing();
    }

    public boolean showOverflowMenu()
    {
        syncMenuView();
        if (mOverflowItems.isEmpty()) return false;

        CharSequence[] titles = new CharSequence[mOverflowItems.size()];
        for (int index = 0; index < titles.length; index++) titles[index] = mOverflowItems.get(index).getTitle();

        final List<MenuItem> items = new ArrayList<>(mOverflowItems);

        try
        {
            mOverflowDialog = new AlertDialog.Builder(getContext())
                                  .setItems(titles, new DialogInterface.OnClickListener()
                                  {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which)
                                      {
                                          dialog.dismiss();
                                          if (which >= 0 && which < items.size()) performMenuItem(items.get(which));
                                      }
                                  })
                                  .show();
            return true;
        }
        catch (RuntimeException unavailable)
        {
            android.util.Log.w("Toolbar", "overflow menu could not be shown", unavailable);
            mOverflowDialog = null;
            return false;
        }
    }

    public boolean hideOverflowMenu()
    {
        if (!isOverflowMenuShowing()) return false;

        mOverflowDialog.dismiss();
        mOverflowDialog = null;
        return true;
    }

    public void dismissPopupMenus()
    {
        hideOverflowMenu();
    }

    public void setLogo(Drawable drawable)
    {
        mLogo = drawable;

        if (drawable != null)
        {
            ensureLogoView();
            if (mLogoView.getParent() != this) addSystemView(mLogoView);
        }
        else if (mLogoView != null && mLogoView.getParent() == this)
        {
            removeView(mLogoView);
        }

        if (mLogoView != null) mLogoView.setImageDrawable(drawable);
    }

    public Drawable getLogo()
    {
        return mLogo;
    }

    public void setLogoDescription(int resId)
    {
        setLogoDescription(resolveText(resId));
    }

    public void setLogoDescription(CharSequence description)
    {
        mLogoDescription = description;

        if (!TextUtils.isEmpty(description)) ensureLogoView();
        if (mLogoView != null) mLogoView.setContentDescription(description);
    }

    public CharSequence getLogoDescription()
    {
        return mLogoDescription;
    }

    private void ensureLogoView()
    {
        if (mLogoView != null) return;

        mLogoView = new ImageView(getContext());
        mLogoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mLogoView.setAdjustViewBounds(true);
    }

    public boolean hasExpandedActionView()
    {
        return false;
    }

    public void collapseActionView()
    {
        if (mMenu == null) return;

        for (int index = 0; index < mMenu.size(); index++)
        {
            MenuItem item = mMenu.getItem(index);
            if (item.isActionViewExpanded()) item.collapseActionView();
        }
    }

    public CharSequence getTitle()
    {
        return mTitleText;
    }

    public void setTitle(int resId)
    {
        setTitle(resolveText(resId));
    }

    public void setTitle(CharSequence title)
    {
        if (!TextUtils.isEmpty(title))
        {
            if (mTitleTextView == null)
            {
                mTitleTextView = createLabel(20, mTitleTextAppearance, true);
                if (mTitleColorSet || mTitleTextAppearance == 0) mTitleTextView.setTextColor(mTitleTextColor);
            }

            if (mTitleTextView.getParent() != this) addSystemView(mTitleTextView);
        }
        else if (mTitleTextView != null && mTitleTextView.getParent() == this)
        {
            removeView(mTitleTextView);
        }

        if (mTitleTextView != null) mTitleTextView.setText(title);

        mTitleText = title;
    }

    public CharSequence getSubtitle()
    {
        return mSubtitleText;
    }

    public void setSubtitle(int resId)
    {
        setSubtitle(resolveText(resId));
    }

    public void setSubtitle(CharSequence subtitle)
    {
        if (!TextUtils.isEmpty(subtitle))
        {
            if (mSubtitleTextView == null)
            {
                mSubtitleTextView = createLabel(14, mSubtitleTextAppearance, false);
                if (mSubtitleColorSet || mSubtitleTextAppearance == 0) mSubtitleTextView.setTextColor(mSubtitleTextColor);
            }

            if (mSubtitleTextView.getParent() != this) addSystemView(mSubtitleTextView);
        }
        else if (mSubtitleTextView != null && mSubtitleTextView.getParent() == this)
        {
            removeView(mSubtitleTextView);
        }

        if (mSubtitleTextView != null) mSubtitleTextView.setText(subtitle);

        mSubtitleText = subtitle;
    }

    private TextView createLabel(int textSize, int appearance, boolean title)
    {
        TextView label = new TextView(getContext());
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        label.setTextColor(title ? mTitleTextColor : mSubtitleTextColor);
        if (title) label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        if (appearance != 0) applyTextAppearance(label, getContext(), appearance);

        return label;
    }

    private static void applyTextAppearance(TextView label, Context context, int resId)
    {
        if (sAppearanceStyleable == null)
        {
            sAppearanceStyleable = StyleableSet.of(context.getResources(), "textSize", "textColor", "textStyle", "fontFamily");
        }
        if (sAppearanceStyleable.isEmpty()) return;

        TypedArray array;

        try
        {
            array = context.obtainStyledAttributes(resId, sAppearanceStyleable.attributes());
        }
        catch (RuntimeException unresolved)
        {
            return;
        }

        int index = sAppearanceStyleable.indexOf("textSize");
        if (index >= 0 && array.hasValue(index))
        {
            int size = array.getDimensionPixelSize(index, 0);
            if (size > 0) label.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }

        index = sAppearanceStyleable.indexOf("textColor");
        if (index >= 0 && array.hasValue(index))
        {
            try
            {
                ColorStateList colors = array.getColorStateList(index);
                if (colors != null) label.setTextColor(colors);
            }
            catch (RuntimeException unresolved)
            {
                label.setTextColor(array.getColor(index, label.getCurrentTextColor()));
            }
        }

        int style = Typeface.NORMAL;
        index = sAppearanceStyleable.indexOf("textStyle");
        if (index >= 0) style = array.getInt(index, Typeface.NORMAL);

        String family = null;
        index = sAppearanceStyleable.indexOf("fontFamily");
        if (index >= 0) family = array.getString(index);

        if (family != null || style != Typeface.NORMAL) label.setTypeface(Typeface.create(family == null ? "sans-serif" : family, style));

        array.recycle();
    }

    public void setTitleTextAppearance(Context context, int resId)
    {
        mTitleTextAppearance = resId;
        if (mTitleTextView != null) applyTextAppearance(mTitleTextView, context, resId);
    }

    public void setSubtitleTextAppearance(Context context, int resId)
    {
        mSubtitleTextAppearance = resId;
        if (mSubtitleTextView != null) applyTextAppearance(mSubtitleTextView, context, resId);
    }

    public void setTitleTextColor(int color)
    {
        mTitleTextColor = color;
        mTitleColorSet = true;
        if (mTitleTextView != null) mTitleTextView.setTextColor(color);
    }

    public void setSubtitleTextColor(int color)
    {
        mSubtitleTextColor = color;
        mSubtitleColorSet = true;
        if (mSubtitleTextView != null) mSubtitleTextView.setTextColor(color);
    }

    public CharSequence getNavigationContentDescription()
    {
        return mNavigationContentDescription;
    }

    public void setNavigationContentDescription(int resId)
    {
        setNavigationContentDescription(resolveText(resId));
    }

    public void setNavigationContentDescription(CharSequence description)
    {
        mNavigationContentDescription = description;

        if (!TextUtils.isEmpty(description)) ensureNavButtonView();
        if (mNavButtonView != null) mNavButtonView.setContentDescription(description);
    }

    public void setNavigationIcon(int resId)
    {
        setNavigationIcon(resolveDrawable(resId));
    }

    public void setNavigationIcon(Drawable icon)
    {
        mNavigationIcon = icon;

        if (icon != null)
        {
            ensureNavButtonView();
            if (mNavButtonView.getParent() != this) addSystemView(mNavButtonView);
        }
        else if (mNavButtonView != null && mNavButtonView.getParent() == this)
        {
            removeView(mNavButtonView);
        }

        if (mNavButtonView != null) mNavButtonView.setImageDrawable(icon);
    }

    public Drawable getNavigationIcon()
    {
        return mNavigationIcon;
    }

    public void setNavigationOnClickListener(OnClickListener listener)
    {
        ensureNavButtonView();
        mNavButtonView.setOnClickListener(listener);
    }

    private void ensureNavButtonView()
    {
        if (mNavButtonView != null) return;

        mNavButtonView = new ImageButton(getContext());
        mNavButtonView.setScaleType(ImageView.ScaleType.CENTER);
        mNavButtonView.setContentDescription(mNavigationContentDescription);
    }

    public CharSequence getCollapseContentDescription()
    {
        return mCollapseContentDescription;
    }

    public void setCollapseContentDescription(int resId)
    {
        setCollapseContentDescription(resolveText(resId));
    }

    public void setCollapseContentDescription(CharSequence description)
    {
        mCollapseContentDescription = description;
    }

    public Drawable getCollapseIcon()
    {
        return mCollapseIcon;
    }

    public void setCollapseIcon(int resId)
    {
        setCollapseIcon(resolveDrawable(resId));
    }

    public void setCollapseIcon(Drawable icon)
    {
        mCollapseIcon = icon;
    }

    public Menu getMenu()
    {
        ensureMenu();
        return mMenu;
    }

    private void ensureMenu()
    {
        if (mMenu != null) return;

        mMenu = new RuntimeMenu(getContext());
        mMenu.setCallback(new RuntimeMenu.Callback()
        {
            @Override
            public boolean onMenuItemSelected(RuntimeMenu menu, MenuItem item)
            {
                return mOnMenuItemClickListener != null && mOnMenuItemClickListener.onMenuItemClick(item);
            }
        });
    }

    public void setOverflowIcon(Drawable icon)
    {
        mOverflowIcon = icon;
        if (mOverflowButton != null) mOverflowButton.setImageDrawable(icon != null ? icon : new OverflowDrawable());
    }

    public Drawable getOverflowIcon()
    {
        return mOverflowIcon;
    }

    public void inflateMenu(int resId)
    {
        new MenuInflater(getContext()).inflate(resId, getMenu());
        syncMenuView();
        requestLayout();
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener)
    {
        mOnMenuItemClickListener = listener;
    }

    private boolean performMenuItem(MenuItem item)
    {
        return mMenu != null && mMenu.performItemAction(item);
    }

    private void syncMenuView()
    {
        List<Object> signature = new ArrayList<>();

        if (mMenu != null)
        {
            for (MenuItem item : mMenu.getVisibleItems())
            {
                signature.add(item);
                signature.add(item.getIcon());
                signature.add(item.getTitle());
                signature.add(item.getActionView());
                signature.add(item.isEnabled());
            }
        }

        if (signature.equals(mMenuSignature) && (mMenuView != null || signature.isEmpty())) return;

        mMenuSignature.clear();
        mMenuSignature.addAll(signature);
        mOverflowItems.clear();

        if (mMenuView == null)
        {
            mMenuView = new LinearLayout(getContext());
            mMenuView.setOrientation(LinearLayout.HORIZONTAL);
        }

        mMenuView.removeAllViews();

        if (mMenu != null)
        {
            for (MenuItem item : mMenu.getVisibleItems())
            {
                View actionView = item.getActionView();

                if (actionView != null)
                {
                    if (actionView.getParent() instanceof ViewGroup) ((ViewGroup)actionView.getParent()).removeView(actionView);

                    mMenuView.addView(actionView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
                    continue;
                }

                if (item.getIcon() == null)
                {
                    mOverflowItems.add(item);
                    continue;
                }

                mMenuView.addView(createActionButton(item), new LinearLayout.LayoutParams(dp(48), LayoutParams.MATCH_PARENT));
            }
        }

        if (!mOverflowItems.isEmpty())
        {
            if (mOverflowButton == null)
            {
                mOverflowButton = new ImageButton(getContext());
                mOverflowButton.setScaleType(ImageView.ScaleType.CENTER);
                mOverflowButton.setImageDrawable(mOverflowIcon != null ? mOverflowIcon : new OverflowDrawable());
                mOverflowButton.setContentDescription("More options");
                mOverflowButton.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        showOverflowMenu();
                    }
                });
            }

            mMenuView.addView(mOverflowButton, new LinearLayout.LayoutParams(dp(48), LayoutParams.MATCH_PARENT));
        }

        boolean hasContent = mMenuView.getChildCount() > 0;

        if (hasContent && mMenuView.getParent() != this) addSystemView(mMenuView);
        else if (!hasContent && mMenuView.getParent() == this) removeView(mMenuView);
    }

    private ImageButton createActionButton(final MenuItem item)
    {
        ImageButton button = new ImageButton(getContext());
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setImageDrawable(item.getIcon());
        button.setEnabled(item.isEnabled());
        button.setContentDescription(item.getContentDescription() != null ? item.getContentDescription() : item.getTitle());
        button.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                performMenuItem(item);
            }
        });

        return button;
    }

    private void addSystemView(View view)
    {
        ViewGroup.LayoutParams current = view.getLayoutParams();
        LayoutParams params;

        if (current == null) params = generateDefaultLayoutParams();
        else if (!checkLayoutParams(current)) params = generateLayoutParams(current);
        else params = (LayoutParams)current;

        params.mViewType = LayoutParams.SYSTEM;
        addView(view, params);
    }

    public void setContentInsetsRelative(int contentInsetStart, int contentInsetEnd)
    {
        mContentInsetStart = contentInsetStart;
        mContentInsetEnd = contentInsetEnd;
        mExplicitInsetsAbsolute = false;
        requestLayout();
    }

    public int getContentInsetStart()
    {
        return mContentInsetStart;
    }

    public int getContentInsetEnd()
    {
        return mContentInsetEnd;
    }

    public void setContentInsetsAbsolute(int contentInsetLeft, int contentInsetRight)
    {
        mContentInsetLeft = contentInsetLeft;
        mContentInsetRight = contentInsetRight;
        mExplicitInsetsAbsolute = true;
        requestLayout();
    }

    public int getContentInsetLeft()
    {
        if (mExplicitInsetsAbsolute && mContentInsetLeft != Integer.MIN_VALUE) return mContentInsetLeft;

        return isRtl() ? mContentInsetEnd : mContentInsetStart;
    }

    public int getContentInsetRight()
    {
        if (mExplicitInsetsAbsolute && mContentInsetRight != Integer.MIN_VALUE) return mContentInsetRight;

        return isRtl() ? mContentInsetStart : mContentInsetEnd;
    }

    public int getContentInsetStartWithNavigation()
    {
        return mContentInsetStartWithNavigation;
    }

    public void setContentInsetStartWithNavigation(int insetStartWithNavigation)
    {
        mContentInsetStartWithNavigation = Math.max(0, insetStartWithNavigation);
        requestLayout();
    }

    public int getContentInsetEndWithActions()
    {
        return mContentInsetEndWithActions;
    }

    public void setContentInsetEndWithActions(int insetEndWithActions)
    {
        mContentInsetEndWithActions = Math.max(0, insetEndWithActions);
        requestLayout();
    }

    private int startInset()
    {
        return isRtl() ? getContentInsetRight() : getContentInsetLeft();
    }

    private int endInset()
    {
        return isRtl() ? getContentInsetLeft() : getContentInsetRight();
    }

    public int getCurrentContentInsetStart()
    {
        if (shouldLayout(mNavButtonView)) return Math.max(startInset(), mContentInsetStartWithNavigation);

        return startInset();
    }

    public int getCurrentContentInsetEnd()
    {
        if (shouldLayout(mMenuView)) return Math.max(endInset(), mContentInsetEndWithActions);

        return endInset();
    }

    public int getCurrentContentInsetLeft()
    {
        return isRtl() ? getCurrentContentInsetEnd() : getCurrentContentInsetStart();
    }

    public int getCurrentContentInsetRight()
    {
        return isRtl() ? getCurrentContentInsetStart() : getCurrentContentInsetEnd();
    }

    private boolean isRtl()
    {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    private boolean shouldLayout(View view)
    {
        return view != null && view.getParent() == this && view.getVisibility() != GONE;
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.mOverflowShowing = isOverflowMenuShowing();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        if (!(state instanceof SavedState))
        {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState saved = (SavedState)state;
        super.onRestoreInstanceState(saved.getSuperState());

        if (saved.mOverflowShowing)
        {
            post(new Runnable()
            {
                @Override
                public void run()
                {
                    showOverflowMenu();
                }
            });
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        hideOverflowMenu();

        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        super.onTouchEvent(event);
        return true;
    }

    private int contentHeight(int heightMeasureSpec)
    {
        int verticalPadding = getPaddingTop() + getPaddingBottom();

        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)
        {
            return Math.max(0, MeasureSpec.getSize(heightMeasureSpec) - verticalPadding);
        }

        int target = Math.max(getSuggestedMinimumHeight(), mBarHeight) - verticalPadding;

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.AT_MOST) target = Math.min(target, MeasureSpec.getSize(heightMeasureSpec));

        return Math.max(0, target);
    }

    private int buttonHeight(int contentHeight)
    {
        if (mMaxButtonHeight > 0) return Math.min(contentHeight, mMaxButtonHeight);

        return contentHeight;
    }

    private int horizontalMargins(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (!(params instanceof MarginLayoutParams)) return 0;

        return ((MarginLayoutParams)params).leftMargin + ((MarginLayoutParams)params).rightMargin;
    }

    private int verticalMargins(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (!(params instanceof MarginLayoutParams)) return 0;

        return ((MarginLayoutParams)params).topMargin + ((MarginLayoutParams)params).bottomMargin;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        syncMenuView();

        int content = contentHeight(heightMeasureSpec);
        int buttons = buttonHeight(content);
        int unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int tallest = content;
        int width = 0;

        if (shouldLayout(mNavButtonView))
        {
            int navigationWidth = MeasureSpec.makeMeasureSpec(dp(56), MeasureSpec.EXACTLY);
            mNavButtonView.measure(navigationWidth, MeasureSpec.makeMeasureSpec(buttons, MeasureSpec.EXACTLY));
            width += mNavButtonView.getMeasuredWidth();
        }

        width = Math.max(getCurrentContentInsetStart(), width);

        int endWidth = 0;

        if (shouldLayout(mMenuView))
        {
            mMenuView.measure(unspecified, MeasureSpec.makeMeasureSpec(buttons, MeasureSpec.EXACTLY));
            endWidth = mMenuView.getMeasuredWidth();
        }

        endWidth = Math.max(getCurrentContentInsetEnd(), endWidth);

        if (shouldLayout(mLogoView))
        {
            mLogoView.measure(unspecified, MeasureSpec.makeMeasureSpec(buttons, MeasureSpec.AT_MOST));
            width += mLogoView.getMeasuredWidth();
        }

        int available = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED ? Integer.MAX_VALUE / 2
                                                                                           : MeasureSpec.getSize(widthMeasureSpec);

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (!isCustomView(child) || child.getVisibility() == GONE) continue;

            measureChildWithMargins(child, MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST), width + endWidth,
                                    MeasureSpec.makeMeasureSpec(content + getPaddingTop() + getPaddingBottom(), MeasureSpec.AT_MOST), 0);

            width += child.getMeasuredWidth() + horizontalMargins(child);
            tallest = Math.max(tallest, child.getMeasuredHeight() + verticalMargins(child));
        }

        int titleWidth = 0;
        int titleHeight = 0;
        int labelSpec = MeasureSpec.makeMeasureSpec(content, MeasureSpec.AT_MOST);

        if (shouldLayout(mTitleTextView))
        {
            mTitleTextView.measure(unspecified, labelSpec);
            titleWidth = mTitleTextView.getMeasuredWidth();
            titleHeight = mTitleTextView.getMeasuredHeight();
        }

        if (shouldLayout(mSubtitleTextView))
        {
            mSubtitleTextView.measure(unspecified, labelSpec);
            titleWidth = Math.max(titleWidth, mSubtitleTextView.getMeasuredWidth());
            titleHeight += mSubtitleTextView.getMeasuredHeight();
        }

        if (titleWidth > 0)
        {
            titleWidth += mTitleMarginStart + mTitleMarginEnd;
            tallest = Math.max(tallest, titleHeight + mTitleMarginTop + mTitleMarginBottom);
        }

        int measuredWidth = width + titleWidth + endWidth + getPaddingLeft() + getPaddingRight();
        int measuredHeight = Math.max(tallest + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight());

        setMeasuredDimension(resolveSizeAndState(Math.max(measuredWidth, getSuggestedMinimumWidth()), widthMeasureSpec, 0),
                             resolveSizeAndState(measuredHeight, heightMeasureSpec, 0));
    }

    private boolean isCustomView(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        return !(params instanceof LayoutParams) || ((LayoutParams)params).mViewType == LayoutParams.CUSTOM;
    }

    private int childTop(View child, int innerTop, int innerBottom)
    {
        int height = child.getMeasuredHeight();
        int gravity = Gravity.CENTER_VERTICAL;

        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof ActionBar.LayoutParams) gravity = ((ActionBar.LayoutParams)params).gravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity != Gravity.TOP && gravity != Gravity.BOTTOM && gravity != Gravity.CENTER_VERTICAL)
        {
            gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        }

        int topMargin = 0;
        int bottomMargin = 0;

        if (params instanceof MarginLayoutParams)
        {
            topMargin = ((MarginLayoutParams)params).topMargin;
            bottomMargin = ((MarginLayoutParams)params).bottomMargin;
        }

        if (gravity == Gravity.TOP) return innerTop + topMargin;
        if (gravity == Gravity.BOTTOM) return innerBottom - bottomMargin - height;

        return innerTop + Math.max(0, (innerBottom - innerTop - height) / 2) + topMargin - bottomMargin;
    }

    private int horizontalGravity(View child)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        int gravity = params instanceof ActionBar.LayoutParams ? ((ActionBar.LayoutParams)params).gravity : Gravity.NO_GRAVITY;
        int absolute = Gravity.getAbsoluteGravity(gravity, getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;

        if (absolute == Gravity.LEFT || absolute == Gravity.RIGHT || absolute == Gravity.CENTER_HORIZONTAL) return absolute;

        return isRtl() ? Gravity.RIGHT : Gravity.LEFT;
    }

    private int placeLeft(View child, int left, int innerTop, int innerBottom)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        int leftMargin = params instanceof MarginLayoutParams ? ((MarginLayoutParams)params).leftMargin : 0;
        int rightMargin = params instanceof MarginLayoutParams ? ((MarginLayoutParams)params).rightMargin : 0;

        int childLeft = left + leftMargin;
        int top = childTop(child, innerTop, innerBottom);

        child.layout(childLeft, top, childLeft + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        return childLeft + child.getMeasuredWidth() + rightMargin;
    }

    private int placeRight(View child, int right, int innerTop, int innerBottom)
    {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        int leftMargin = params instanceof MarginLayoutParams ? ((MarginLayoutParams)params).leftMargin : 0;
        int rightMargin = params instanceof MarginLayoutParams ? ((MarginLayoutParams)params).rightMargin : 0;

        int childRight = right - rightMargin;
        int top = childTop(child, innerTop, innerBottom);

        child.layout(childRight - child.getMeasuredWidth(), top, childRight, top + child.getMeasuredHeight());
        return childRight - child.getMeasuredWidth() - leftMargin;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        boolean rtl = isRtl();
        int width = r - l;
        int height = b - t;
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int innerTop = getPaddingTop();
        int innerBottom = height - getPaddingBottom();

        int left = paddingLeft;
        int right = width - paddingRight;

        if (shouldLayout(mNavButtonView))
        {
            if (rtl) right = placeRight(mNavButtonView, right, innerTop, innerBottom);
            else left = placeLeft(mNavButtonView, left, innerTop, innerBottom);
        }

        if (shouldLayout(mMenuView))
        {
            if (rtl) left = placeLeft(mMenuView, left, innerTop, innerBottom);
            else right = placeRight(mMenuView, right, innerTop, innerBottom);
        }

        left = Math.max(left, paddingLeft + getCurrentContentInsetLeft());
        right = Math.min(right, width - paddingRight - getCurrentContentInsetRight());

        if (shouldLayout(mLogoView))
        {
            if (rtl) right = placeRight(mLogoView, right, innerTop, innerBottom);
            else left = placeLeft(mLogoView, left, innerTop, innerBottom);
        }

        boolean layoutTitle = shouldLayout(mTitleTextView);
        boolean layoutSubtitle = shouldLayout(mSubtitleTextView);

        if (layoutTitle || layoutSubtitle)
        {
            int titleHeight = layoutTitle ? mTitleTextView.getMeasuredHeight() : 0;
            int subtitleHeight = layoutSubtitle ? mSubtitleTextView.getMeasuredHeight() : 0;
            int labelWidth = Math.max(layoutTitle ? mTitleTextView.getMeasuredWidth() : 0,
                                      layoutSubtitle ? mSubtitleTextView.getMeasuredWidth() : 0);

            int space = Math.max(0, right - left - mTitleMarginStart - mTitleMarginEnd);
            labelWidth = Math.min(labelWidth, space);

            int blockHeight = titleHeight + subtitleHeight;
            int vertical = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
            int top;

            if (vertical == Gravity.TOP) top = innerTop + mTitleMarginTop;
            else if (vertical == Gravity.BOTTOM) top = innerBottom - mTitleMarginBottom - blockHeight;
            else top = innerTop + (innerBottom - innerTop - blockHeight) / 2 + mTitleMarginTop - mTitleMarginBottom;

            int labelLeft = rtl ? right - mTitleMarginStart - labelWidth : left + mTitleMarginStart;

            if (layoutTitle)
            {
                mTitleTextView.layout(labelLeft, top, labelLeft + labelWidth, top + titleHeight);
                top += titleHeight;
            }

            if (layoutSubtitle) mSubtitleTextView.layout(labelLeft, top, labelLeft + labelWidth, top + subtitleHeight);

            if (labelWidth > 0)
            {
                if (rtl) right = labelLeft - mTitleMarginEnd;
                else left = labelLeft + labelWidth + mTitleMarginEnd;
            }
        }

        List<View> centred = new ArrayList<>();

        for (int index = 0; index < getChildCount(); index++)
        {
            View child = getChildAt(index);
            if (!isCustomView(child) || child.getVisibility() == GONE) continue;

            int gravity = horizontalGravity(child);

            if (gravity == Gravity.CENTER_HORIZONTAL) centred.add(child);
            else if (gravity == Gravity.RIGHT) right = placeRight(child, right, innerTop, innerBottom);
            else left = placeLeft(child, left, innerTop, innerBottom);
        }

        for (View child : centred)
        {
            int childLeft = Math.max(left, Math.min(right - child.getMeasuredWidth(), (width - child.getMeasuredWidth()) / 2));
            int top = childTop(child, innerTop, innerBottom);

            child.layout(childLeft, top, childLeft + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new LayoutParams(getContext(), attributes);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams source)
    {
        if (source instanceof LayoutParams) return new LayoutParams((LayoutParams)source);
        if (source instanceof ActionBar.LayoutParams) return new LayoutParams((ActionBar.LayoutParams)source);
        if (source instanceof MarginLayoutParams) return new LayoutParams((MarginLayoutParams)source);

        return new LayoutParams(source);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams)
    {
        return super.checkLayoutParams(layoutParams) && layoutParams instanceof LayoutParams;
    }

    private final class OverflowDrawable extends Drawable
    {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        OverflowDrawable()
        {
            mPaint.setColor(mTitleTextColor);
        }

        @Override
        public void draw(Canvas canvas)
        {
            android.graphics.Rect bounds = getBounds();
            float radius = 2 * mDensity;
            float spacing = 6 * mDensity;
            float centreX = bounds.exactCenterX();
            float centreY = bounds.exactCenterY();

            mPaint.setColor(mTitleTextColor);

            for (int index = -1; index <= 1; index++) canvas.drawCircle(centreX, centreY + index * spacing, radius, mPaint);
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
            return dp(24);
        }

        @Override
        public int getIntrinsicHeight()
        {
            return dp(24);
        }
    }

    static class SavedState extends BaseSavedState
    {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
        {
            @Override
            public SavedState createFromParcel(Parcel source)
            {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };

        boolean mOverflowShowing;

        SavedState(Parcelable superState)
        {
            super(superState);
        }

        private SavedState(Parcel source)
        {
            super(source);
            mOverflowShowing = source.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeInt(mOverflowShowing ? 1 : 0);
        }
    }

    public static class LayoutParams extends ActionBar.LayoutParams
    {
        static final int CUSTOM = 0;
        static final int SYSTEM = 1;
        static final int EXPANDED = 2;

        int mViewType = CUSTOM;

        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);
        }

        public LayoutParams(int width, int height)
        {
            super(width, height);
            this.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
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
            super((ActionBar.LayoutParams)source);
            mViewType = source.mViewType;
        }

        public LayoutParams(ActionBar.LayoutParams source)
        {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source)
        {
            super((ViewGroup.LayoutParams)source);
            setMargins(source.leftMargin, source.topMargin, source.rightMargin, source.bottomMargin);
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }
    }
}
