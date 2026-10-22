package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class SeekBar extends AbsSeekBar
{
    public interface OnSeekBarChangeListener
    {
        void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser);

        void onStartTrackingTouch(SeekBar seekBar);

        void onStopTrackingTouch(SeekBar seekBar);
    }

    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public SeekBar(Context context)
    {
        this(context, null);
    }

    public SeekBar(Context context, AttributeSet attributes)
    {
        this(context, attributes, attributeIdentifier(context, "seekBarStyle"));
    }

    public SeekBar(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public SeekBar(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener)
    {
        mOnSeekBarChangeListener = listener;
    }

    @Override
    void onProgressRefresh(float scale, boolean fromUser, int progress)
    {
        super.onProgressRefresh(scale, fromUser, progress);

        if (mOnSeekBarChangeListener != null) mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
    }

    @Override
    void onStartTrackingTouch()
    {
        super.onStartTrackingTouch();

        if (mOnSeekBarChangeListener != null) mOnSeekBarChangeListener.onStartTrackingTouch(this);
    }

    @Override
    void onStopTrackingTouch()
    {
        super.onStopTrackingTouch();

        if (mOnSeekBarChangeListener != null) mOnSeekBarChangeListener.onStopTrackingTouch(this);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return SeekBar.class.getName();
    }
}
