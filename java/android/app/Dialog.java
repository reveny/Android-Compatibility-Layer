package android.app;

import android.content.Context;
import android.content.DialogInterface;

public class Dialog implements DialogInterface
{
    private final Context mContext;
    private boolean mShowing;
    private CharSequence mTitle;

    private OnCancelListener mCancelListener;
    private OnDismissListener mDismissListener;

    public Dialog(Context context)
    {
        mContext = context;
    }

    public Context getContext()
    {
        return mContext;
    }

    public void setTitle(CharSequence title)
    {
        mTitle = title;
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    public void show()
    {
        mShowing = true;
        System.out.println("[framework] a dialog was shown: " + mTitle);
    }

    public void hide()
    {
        mShowing = false;
    }

    public boolean isShowing()
    {
        return mShowing;
    }

    public void setCancelable(boolean cancelable)
    {
    }

    public void setCanceledOnTouchOutside(boolean cancel)
    {
    }

    public void setOnCancelListener(OnCancelListener listener)
    {
        mCancelListener = listener;
    }

    public void setOnDismissListener(OnDismissListener listener)
    {
        mDismissListener = listener;
    }

    @Override
    public void cancel()
    {
        if (mCancelListener != null) mCancelListener.onCancel(this);
        dismiss();
    }

    @Override
    public void dismiss()
    {
        mShowing = false;
        if (mDismissListener != null) mDismissListener.onDismiss(this);
    }
}
