package android.app;

import android.content.Context;
import android.content.DialogInterface;

public class AlertDialog extends Dialog
{
    private CharSequence mMessage;

    protected AlertDialog(Context context)
    {
        super(context);
    }

    public void setMessage(CharSequence message)
    {
        mMessage = message;
    }

    public CharSequence getMessage()
    {
        return mMessage;
    }

    public void setButton(int whichButton, CharSequence text, DialogInterface.OnClickListener listener)
    {
    }

    public static class Builder
    {
        private final Context mContext;
        private CharSequence mTitle;
        private CharSequence mMessage;
        private boolean mCancelable = true;

        private CharSequence mPositiveText;
        private DialogInterface.OnClickListener mPositiveListener;
        private CharSequence mNegativeText;
        private DialogInterface.OnClickListener mNegativeListener;
        private DialogInterface.OnCancelListener mCancelListener;

        public Builder(Context context)
        {
            mContext = context;
        }

        public Builder(Context context, int themeResId)
        {
            mContext = context;
        }

        public Context getContext()
        {
            return mContext;
        }

        public Builder setTitle(CharSequence title)
        {
            mTitle = title;
            return this;
        }

        public Builder setMessage(CharSequence message)
        {
            mMessage = message;
            return this;
        }

        public Builder setCancelable(boolean cancelable)
        {
            mCancelable = cancelable;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener)
        {
            mPositiveText = text;
            mPositiveListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener)
        {
            mNegativeText = text;
            mNegativeListener = listener;
            return this;
        }

        public Builder setOnCancelListener(DialogInterface.OnCancelListener listener)
        {
            mCancelListener = listener;
            return this;
        }

        public AlertDialog create()
        {
            AlertDialog dialog = new AlertDialog(mContext);
            dialog.setTitle(mTitle);
            dialog.setMessage(mMessage);
            dialog.setCancelable(mCancelable);
            dialog.setOnCancelListener(mCancelListener);
            return dialog;
        }

        public AlertDialog show()
        {
            AlertDialog dialog = create();
            dialog.show();
            return dialog;
        }
    }
}
