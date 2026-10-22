package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.autofill.AutofillValue;

public class RadioGroup extends LinearLayout
{
    private static StyleableSet sStyleable;

    private int mCheckedId = View.NO_ID;
    private CompoundButton.OnCheckedChangeListener mChildOnCheckedChangeListener;
    private boolean mProtectFromCheckedChange;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private PassThroughHierarchyChangeListener mPassThroughListener;

    public RadioGroup(Context context)
    {
        super(context);
        setOrientation(VERTICAL);
        init();
    }

    public RadioGroup(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        int orientation = VERTICAL;

        if (attributes != null)
        {
            if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "checkedButton", "orientation");

            if (!sStyleable.isEmpty())
            {
                TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), 0, 0);

                int checkedIndex = sStyleable.indexOf("checkedButton");
                if (checkedIndex >= 0)
                {
                    int value = array.getResourceId(checkedIndex, View.NO_ID);
                    if (value != View.NO_ID) mCheckedId = value;
                }

                int orientationIndex = sStyleable.indexOf("orientation");
                if (orientationIndex >= 0) orientation = array.getInt(orientationIndex, VERTICAL);

                array.recycle();
            }
        }

        setOrientation(orientation);
        init();
    }

    private void init()
    {
        mChildOnCheckedChangeListener = new CheckedStateTracker();
        mPassThroughListener = new PassThroughHierarchyChangeListener();
        super.setOnHierarchyChangeListener(mPassThroughListener);
    }

    @Override
    public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener)
    {
        mPassThroughListener.mOnHierarchyChangeListener = listener;
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();

        if (mCheckedId == View.NO_ID) return;

        mProtectFromCheckedChange = true;
        setCheckedStateForView(mCheckedId, true);
        mProtectFromCheckedChange = false;
        setCheckedId(mCheckedId);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        if (child instanceof RadioButton)
        {
            RadioButton button = (RadioButton)child;

            if (button.isChecked())
            {
                mProtectFromCheckedChange = true;
                if (mCheckedId != View.NO_ID) setCheckedStateForView(mCheckedId, false);
                mProtectFromCheckedChange = false;
                setCheckedId(button.getId());
            }
        }

        super.addView(child, index, params);
    }

    public void check(int id)
    {
        if (id != View.NO_ID && id == mCheckedId) return;

        if (mCheckedId != View.NO_ID) setCheckedStateForView(mCheckedId, false);
        if (id != View.NO_ID) setCheckedStateForView(id, true);

        setCheckedId(id);
    }

    private void setCheckedId(int id)
    {
        mCheckedId = id;

        if (mOnCheckedChangeListener != null) mOnCheckedChangeListener.onCheckedChanged(this, mCheckedId);
    }

    private void setCheckedStateForView(int viewId, boolean checked)
    {
        View checkedView = findViewById(viewId);
        if (checkedView instanceof RadioButton) ((RadioButton)checkedView).setChecked(checked);
    }

    public int getCheckedRadioButtonId()
    {
        return mCheckedId;
    }

    public void clearCheck()
    {
        check(View.NO_ID);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener)
    {
        mOnCheckedChangeListener = listener;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributes)
    {
        return new RadioGroup.LayoutParams(getContext(), attributes);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams)
    {
        return layoutParams instanceof RadioGroup.LayoutParams;
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams source)
    {
        if (source instanceof RadioGroup.LayoutParams) return (RadioGroup.LayoutParams)source;
        if (source instanceof MarginLayoutParams) return new LayoutParams((MarginLayoutParams)source);
        return new LayoutParams(source);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return RadioGroup.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info)
    {
        super.onInitializeAccessibilityNodeInfo(info);
    }

    @Override
    public void autofill(AutofillValue value)
    {
        if (!isEnabled() || value == null) return;

        if (!value.isList()) return;

        int index = value.getListValue();
        View child = getChildAt(index);
        if (child == null) return;

        check(child.getId());
    }

    @Override
    public int getAutofillType()
    {
        return isEnabled() ? AUTOFILL_TYPE_LIST : AUTOFILL_TYPE_NONE;
    }

    @Override
    public AutofillValue getAutofillValue()
    {
        if (!isEnabled()) return null;

        int count = getChildCount();

        for (int index = 0; index < count; index++)
        {
            View child = getChildAt(index);
            if (child.getId() == mCheckedId) return AutofillValue.forList(index);
        }

        return null;
    }

    public static class LayoutParams extends LinearLayout.LayoutParams
    {
        public LayoutParams(Context context, AttributeSet attributes)
        {
            super(context, attributes);
        }

        public LayoutParams(int width, int height)
        {
            super(width, height);
        }

        public LayoutParams(int width, int height, float initWeight)
        {
            super(width, height, initWeight);
        }

        public LayoutParams(ViewGroup.LayoutParams source)
        {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source)
        {
            super(source);
        }

        @Override
        protected void setBaseAttributes(TypedArray array, int widthAttr, int heightAttr)
        {
            width = array.hasValue(widthAttr) ? array.getLayoutDimension(widthAttr, "layout_width") : WRAP_CONTENT;
            height = array.hasValue(heightAttr) ? array.getLayoutDimension(heightAttr, "layout_height") : WRAP_CONTENT;
        }
    }

    public interface OnCheckedChangeListener
    {
        void onCheckedChanged(RadioGroup group, int checkedId);
    }

    private class CheckedStateTracker implements CompoundButton.OnCheckedChangeListener
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
            if (mProtectFromCheckedChange) return;

            mProtectFromCheckedChange = true;
            if (mCheckedId != View.NO_ID) setCheckedStateForView(mCheckedId, false);
            mProtectFromCheckedChange = false;

            setCheckedId(buttonView.getId());
        }
    }

    private class PassThroughHierarchyChangeListener implements ViewGroup.OnHierarchyChangeListener
    {
        private ViewGroup.OnHierarchyChangeListener mOnHierarchyChangeListener;

        @Override
        public void onChildViewAdded(View parent, View child)
        {
            if (parent == RadioGroup.this && child instanceof RadioButton)
            {
                int id = child.getId();

                if (id == View.NO_ID)
                {
                    id = View.generateViewId();
                    child.setId(id);
                }

                ((RadioButton)child).setOnCheckedChangeWidgetListener(mChildOnCheckedChangeListener);
            }

            if (mOnHierarchyChangeListener != null) mOnHierarchyChangeListener.onChildViewAdded(parent, child);
        }

        @Override
        public void onChildViewRemoved(View parent, View child)
        {
            if (parent == RadioGroup.this && child instanceof RadioButton)
            {
                ((RadioButton)child).setOnCheckedChangeWidgetListener(null);
            }

            if (mOnHierarchyChangeListener != null) mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
        }
    }
}
