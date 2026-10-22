package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.NumberFormat;

public class ProgressDialog extends AlertDialog
{
    public static final int STYLE_SPINNER = 0;
    public static final int STYLE_HORIZONTAL = 1;

    private static final float MESSAGE_SIZE_SP = 16f;

    private ProgressBar mProgress;
    private TextView mMessageView;
    private TextView mProgressNumber;
    private TextView mProgressPercent;

    private int mProgressStyle = STYLE_SPINNER;
    private String mProgressNumberFormat = "%1d/%2d";
    private NumberFormat mProgressPercentFormat;

    private int mMax;
    private int mProgressVal;
    private int mSecondaryProgressVal;
    private int mIncrementBy;
    private int mIncrementSecondaryBy;
    private Drawable mProgressDrawable;
    private Drawable mIndeterminateDrawable;
    private CharSequence mMessage;
    private boolean mIndeterminate;

    private final Handler mViewUpdateHandler = new Handler(Looper.getMainLooper());
    private final Runnable mUpdateNumbers = new Runnable()
    {
        @Override
        public void run()
        {
            updateNumbers();
        }
    };

    public ProgressDialog(Context context)
    {
        super(context);
        initFormats();
    }

    public ProgressDialog(Context context, int theme)
    {
        super(context, theme);
        initFormats();
    }

    private void initFormats()
    {
        mProgressPercentFormat = NumberFormat.getPercentInstance();
        mProgressPercentFormat.setMaximumFractionDigits(0);
    }

    public static ProgressDialog show(Context context, CharSequence title, CharSequence message)
    {
        return show(context, title, message, false);
    }

    public static ProgressDialog show(Context context, CharSequence title, CharSequence message, boolean indeterminate)
    {
        return show(context, title, message, indeterminate, false, null);
    }

    public static ProgressDialog show(Context context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable)
    {
        return show(context, title, message, indeterminate, cancelable, null);
    }

    public static ProgressDialog show(Context context, CharSequence title, CharSequence message, boolean indeterminate, boolean cancelable,
                                      DialogInterface.OnCancelListener cancelListener)
    {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();
        return dialog;
    }

    private int pixels(float dp)
    {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Context context = getContext();

        int horizontalStyle = context.getResources().getIdentifier("progressBarStyleHorizontal", "attr", "android");

        if (mProgressStyle == STYLE_HORIZONTAL)
        {
            LinearLayout column = new LinearLayout(context);
            column.setOrientation(LinearLayout.VERTICAL);

            mMessageView = new TextView(context);
            mMessageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_SIZE_SP);
            column.addView(mMessageView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            mProgress = new ProgressBar(context, null, horizontalStyle);
            mProgress.setIndeterminate(false);
            column.addView(mProgress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pixels(16)));

            LinearLayout numbers = new LinearLayout(context);
            numbers.setOrientation(LinearLayout.HORIZONTAL);

            mProgressPercent = new TextView(context);
            numbers.addView(mProgressPercent, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            mProgressNumber = new TextView(context);
            mProgressNumber.setGravity(Gravity.END);
            numbers.addView(mProgressNumber, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            column.addView(numbers, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            setView(column);
        }
        else
        {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            mProgress = new ProgressBar(context);
            row.addView(mProgress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            mMessageView = new TextView(context);
            mMessageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_SIZE_SP);
            mMessageView.setPadding(pixels(16), 0, 0, 0);
            row.addView(mMessageView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            setView(row);
        }

        if (mMax > 0) setMax(mMax);
        if (mProgressVal > 0) mProgress.setProgress(mProgressVal);
        if (mSecondaryProgressVal > 0) setSecondaryProgress(mSecondaryProgressVal);
        if (mIncrementBy > 0) incrementProgressBy(mIncrementBy);
        if (mIncrementSecondaryBy > 0) incrementSecondaryProgressBy(mIncrementSecondaryBy);
        if (mProgressDrawable != null) setProgressDrawable(mProgressDrawable);
        if (mIndeterminateDrawable != null) setIndeterminateDrawable(mIndeterminateDrawable);
        if (mMessage != null) setMessage(mMessage);
        setIndeterminate(mIndeterminate);

        updateNumbers();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        updateNumbers();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mViewUpdateHandler.removeCallbacks(mUpdateNumbers);
    }

    public void setProgress(int value)
    {
        if (mProgress != null)
        {
            mProgress.setProgress(value);
            onProgressChanged();
            return;
        }

        mProgressVal = value;
    }

    public void setSecondaryProgress(int secondaryProgress)
    {
        if (mProgress != null)
        {
            mProgress.setSecondaryProgress(secondaryProgress);
            onProgressChanged();
            return;
        }

        mSecondaryProgressVal = secondaryProgress;
    }

    public int getProgress()
    {
        return mProgress != null ? mProgress.getProgress() : mProgressVal;
    }

    public int getSecondaryProgress()
    {
        return mProgress != null ? mProgress.getSecondaryProgress() : mSecondaryProgressVal;
    }

    public int getMax()
    {
        return mProgress != null ? mProgress.getMax() : mMax;
    }

    public void setMax(int max)
    {
        if (mProgress != null)
        {
            mProgress.setMax(max);
            onProgressChanged();
            return;
        }

        mMax = max;
    }

    public void incrementProgressBy(int diff)
    {
        if (mProgress != null)
        {
            mProgress.incrementProgressBy(diff);
            onProgressChanged();
            return;
        }

        mIncrementBy += diff;
    }

    public void incrementSecondaryProgressBy(int diff)
    {
        if (mProgress != null)
        {
            mProgress.setSecondaryProgress(mProgress.getSecondaryProgress() + diff);
            onProgressChanged();
            return;
        }

        mIncrementSecondaryBy += diff;
    }

    public void setProgressDrawable(Drawable drawable)
    {
        if (mProgress != null) mProgress.setProgressDrawable(drawable);
        else mProgressDrawable = drawable;
    }

    public void setIndeterminateDrawable(Drawable drawable)
    {
        if (mProgress != null) mProgress.setIndeterminateDrawable(drawable);
        else mIndeterminateDrawable = drawable;
    }

    public void setIndeterminate(boolean indeterminate)
    {
        if (mProgress != null) mProgress.setIndeterminate(indeterminate);
        else mIndeterminate = indeterminate;

        mIndeterminate = indeterminate;
        updateNumbers();
    }

    public boolean isIndeterminate()
    {
        return mProgress != null ? mProgress.isIndeterminate() : mIndeterminate;
    }

    @Override
    public void setMessage(CharSequence message)
    {
        mMessage = message;
        if (mMessageView != null) mMessageView.setText(message);
    }

    public void setProgressStyle(int style)
    {
        mProgressStyle = style;
    }

    public void setProgressNumberFormat(String format)
    {
        mProgressNumberFormat = format;
        onProgressChanged();
    }

    public void setProgressPercentFormat(NumberFormat format)
    {
        mProgressPercentFormat = format;
        onProgressChanged();
    }

    private void onProgressChanged()
    {
        if (mProgressStyle != STYLE_HORIZONTAL) return;

        mViewUpdateHandler.removeCallbacks(mUpdateNumbers);
        mViewUpdateHandler.post(mUpdateNumbers);
    }

    private void updateNumbers()
    {
        if (mProgress == null || mProgressStyle != STYLE_HORIZONTAL) return;

        boolean hidden = mProgress.isIndeterminate();
        int progress = mProgress.getProgress();
        int max = mProgress.getMax();

        if (mProgressNumber != null)
        {
            if (mProgressNumberFormat != null && !hidden)
            {
                mProgressNumber.setText(String.format(mProgressNumberFormat, progress, max));
                mProgressNumber.setVisibility(View.VISIBLE);
            }
            else
            {
                mProgressNumber.setText("");
                mProgressNumber.setVisibility(hidden ? View.GONE : View.VISIBLE);
            }
        }

        if (mProgressPercent != null)
        {
            if (mProgressPercentFormat != null && !hidden && max > 0)
            {
                double percent = (double)progress / (double)max;
                SpannableString text = new SpannableString(mProgressPercentFormat.format(percent));
                text.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                mProgressPercent.setText(text);
                mProgressPercent.setVisibility(View.VISIBLE);
            }
            else
            {
                mProgressPercent.setText("");
                mProgressPercent.setVisibility(hidden ? View.GONE : View.VISIBLE);
            }
        }
    }
}
