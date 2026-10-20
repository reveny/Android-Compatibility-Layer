package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public abstract class CompoundButton extends Button implements Checkable
{
    private OnCheckedChangeListener mListener;
    private boolean mChecked;
    private boolean mBroadcasting;

    public CompoundButton(Context context)
    {
        super(context);
    }

    public CompoundButton(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public CompoundButton(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener)
    {
        mListener = listener;
    }

    @Override
    public boolean isChecked()
    {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked)
    {
        if (mChecked == checked) return;

        mChecked = checked;
        if (mBroadcasting) return;

        mBroadcasting = true;
        if (mListener != null) mListener.onCheckedChanged(this, mChecked);
        mBroadcasting = false;
    }

    @Override
    public void toggle()
    {
        setChecked(!mChecked);
    }

    @Override
    public boolean performClick()
    {
        toggle();
        return super.performClick();
    }

    public void setButtonDrawable(android.graphics.drawable.Drawable drawable)
    {
    }

    public void setButtonDrawable(int resourceId)
    {
    }

    public interface OnCheckedChangeListener
    {
        void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
    }
}
