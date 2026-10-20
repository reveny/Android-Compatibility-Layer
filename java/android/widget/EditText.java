package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public class EditText extends TextView
{
    private final android.text.SpannableStringBuilder mEditable = new android.text.SpannableStringBuilder();

    private int mSelection;

    public EditText(Context context)
    {
        super(context);
        setEditable(true);
    }

    public EditText(Context context, AttributeSet attributes)
    {
        super(context, attributes);
        setEditable(true);
    }

    public EditText(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
        setEditable(true);
    }

    public EditText(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
        setEditable(true);
    }

    @Override
    public android.text.Editable getText()
    {
        return mEditable;
    }

    @Override
    public void setText(CharSequence text)
    {
        if (mEditable == null)
        {
            super.setText(text);
            return;
        }

        if (text == mEditable)
        {
            super.setText(mEditable);
            return;
        }

        mEditable.clear();
        if (text != null) mEditable.append(text);

        super.setText(mEditable);
    }

    public void setSelection(int index)
    {
        mSelection = Math.max(0, Math.min(index, mEditable == null ? 0 : mEditable.length()));
    }

    public int getSelectionStart()
    {
        return mSelection;
    }

    public int getSelectionEnd()
    {
        return mSelection;
    }

    public void selectAll()
    {
        mSelection = mEditable == null ? 0 : mEditable.length();
    }
}
