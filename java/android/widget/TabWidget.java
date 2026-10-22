package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

public class TabWidget extends LinearLayout implements View.OnFocusChangeListener
{
    private static StyleableSet sStyleable;

    private final Rect mBounds = new Rect();

    private OnTabSelectionChanged mSelectionChangedListener;
    private int mSelectedTab = -1;
    private Drawable mLeftStrip;
    private Drawable mRightStrip;
    private boolean mDrawBottomStrips = true;
    private boolean mStripMoved;

    public TabWidget(Context context)
    {
        this(context, null);
    }

    public TabWidget(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "tabWidgetStyle"));
    }

    public TabWidget(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public TabWidget(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        if (attributes != null || defStyleAttr != 0 || defStyleRes != 0) readAttributes(context, attributes, defStyleAttr, defStyleRes);

        setChildrenDrawingOrderEnabled(true);
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "tabStripEnabled", "tabStripLeft", "tabStripRight", "divider");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int enabledIndex = sStyleable.indexOf("tabStripEnabled");
        if (enabledIndex >= 0) mDrawBottomStrips = array.getBoolean(enabledIndex, true);

        mLeftStrip = readDrawable(array, sStyleable.indexOf("tabStripLeft"));
        mRightStrip = readDrawable(array, sStyleable.indexOf("tabStripRight"));

        Drawable divider = readDrawable(array, sStyleable.indexOf("divider"));
        if (divider != null) setDividerDrawable(divider);

        array.recycle();
    }

    private static Drawable readDrawable(TypedArray array, int index)
    {
        if (index < 0 || !array.hasValue(index)) return null;

        try
        {
            return array.getDrawable(index);
        }
        catch (RuntimeException unsupported)
        {
            return null;
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        mStripMoved = true;

        super.onSizeChanged(width, height, oldWidth, oldHeight);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int drawingPosition)
    {
        if (mSelectedTab == -1 || mSelectedTab >= childCount) return drawingPosition;
        if (drawingPosition == childCount - 1) return mSelectedTab;
        if (drawingPosition >= mSelectedTab) return drawingPosition + 1;

        return drawingPosition;
    }

    public View getChildTabViewAt(int index)
    {
        return getChildAt(index);
    }

    public int getTabCount()
    {
        return getChildCount();
    }

    @Override
    public void setDividerDrawable(Drawable drawable)
    {
        super.setDividerDrawable(drawable);
    }

    public void setDividerDrawable(int resId)
    {
        setDividerDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public void setLeftStripDrawable(Drawable drawable)
    {
        mLeftStrip = drawable;
        requestLayout();
        invalidate();
    }

    public void setLeftStripDrawable(int resId)
    {
        setLeftStripDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public Drawable getLeftStripDrawable()
    {
        return mLeftStrip;
    }

    public void setRightStripDrawable(Drawable drawable)
    {
        mRightStrip = drawable;
        requestLayout();
        invalidate();
    }

    public void setRightStripDrawable(int resId)
    {
        setRightStripDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public Drawable getRightStripDrawable()
    {
        return mRightStrip;
    }

    public void setStripEnabled(boolean stripEnabled)
    {
        mDrawBottomStrips = stripEnabled;
        invalidate();
    }

    public boolean isStripEnabled()
    {
        return mDrawBottomStrips;
    }

    @Override
    public void childDrawableStateChanged(View child)
    {
        if (getTabCount() > 0 && child == getChildTabViewAt(mSelectedTab)) invalidate();

        super.childDrawableStateChanged(child);
    }

    @Override
    public void dispatchDraw(Canvas canvas)
    {
        super.dispatchDraw(canvas);

        if (getTabCount() == 0 || !mDrawBottomStrips) return;

        View selected = getChildTabViewAt(mSelectedTab);
        if (selected == null) return;

        Drawable leftStrip = mLeftStrip;
        Drawable rightStrip = mRightStrip;
        int[] state = selected.getDrawableState();

        if (leftStrip != null) leftStrip.setState(state);
        if (rightStrip != null) rightStrip.setState(state);

        if (mStripMoved)
        {
            mBounds.left = selected.getLeft();
            mBounds.right = selected.getRight();
            int height = getHeight();

            if (leftStrip != null)
            {
                leftStrip.setBounds(Math.min(0, mBounds.left - leftStrip.getIntrinsicWidth()), height - leftStrip.getIntrinsicHeight(),
                                    mBounds.left, height);
            }

            if (rightStrip != null)
            {
                rightStrip.setBounds(mBounds.right, height - rightStrip.getIntrinsicHeight(),
                                     Math.max(getWidth(), mBounds.right + rightStrip.getIntrinsicWidth()), height);
            }

            mStripMoved = false;
        }

        if (leftStrip != null) leftStrip.draw(canvas);
        if (rightStrip != null) rightStrip.draw(canvas);
    }

    public void setCurrentTab(int index)
    {
        if (index < 0 || index >= getTabCount() || index == mSelectedTab) return;

        if (mSelectedTab != -1 && mSelectedTab < getTabCount()) getChildTabViewAt(mSelectedTab).setSelected(false);

        mSelectedTab = index;
        getChildTabViewAt(mSelectedTab).setSelected(true);
        mStripMoved = true;
        invalidate();
    }

    public void focusCurrentTab(int index)
    {
        int oldTab = mSelectedTab;

        setCurrentTab(index);

        if (oldTab != index && index >= 0 && index < getTabCount()) getChildTabViewAt(index).requestFocus();
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);

        int count = getTabCount();
        for (int index = 0; index < count; index++) getChildTabViewAt(index).setEnabled(enabled);
    }

    @Override
    public void addView(View child)
    {
        if (child.getLayoutParams() == null)
        {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            params.setMargins(0, 0, 0, 0);
            child.setLayoutParams(params);
        }

        child.setFocusable(true);
        child.setClickable(true);

        if (child.getPointerIcon() == null)
        {
            child.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND));
        }

        super.addView(child);

        child.setOnClickListener(new TabClickListener(getTabCount() - 1));
    }

    @Override
    public void removeAllViews()
    {
        super.removeAllViews();

        mSelectedTab = -1;
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex)
    {
        if (!isEnabled()) return null;

        return super.onResolvePointerIcon(event, pointerIndex);
    }

    void setTabSelectionListener(OnTabSelectionChanged listener)
    {
        mSelectionChangedListener = listener;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus)
    {
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return TabWidget.class.getName();
    }

    private class TabClickListener implements OnClickListener
    {
        private final int mTabIndex;

        private TabClickListener(int tabIndex)
        {
            mTabIndex = tabIndex;
        }

        public void onClick(View view)
        {
            if (mSelectionChangedListener != null) mSelectionChangedListener.onTabSelectionChanged(mTabIndex, true);
        }
    }

    interface OnTabSelectionChanged
    {
        void onTabSelectionChanged(int tabIndex, boolean clicked);
    }
}
