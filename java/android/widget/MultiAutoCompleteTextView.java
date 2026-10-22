package android.widget;

import android.content.Context;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;

public class MultiAutoCompleteTextView extends AutoCompleteTextView
{
    private Tokenizer mTokenizer;

    public MultiAutoCompleteTextView(Context context)
    {
        this(context, null);
    }

    public MultiAutoCompleteTextView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public MultiAutoCompleteTextView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public MultiAutoCompleteTextView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    public void setTokenizer(Tokenizer tokenizer)
    {
        mTokenizer = tokenizer;
    }

    private int cursorPosition()
    {
        Editable text = getText();
        int length = text == null ? 0 : text.length();
        int end = getSelectionEnd();

        if (end < 0 || end > length) end = length;
        return end;
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode)
    {
        if (enoughToFilter())
        {
            int end = cursorPosition();
            int start = mTokenizer.findTokenStart(text, end);
            performFiltering(text, start, end, keyCode);
            return;
        }

        dismissDropDown();

        Filter filter = getFilter();
        if (filter != null) filter.filter(null);
    }

    @Override
    public boolean enoughToFilter()
    {
        Editable text = getText();
        if (text == null || mTokenizer == null) return false;

        int end = cursorPosition();
        if (end < 0) return false;

        int start = mTokenizer.findTokenStart(text, end);
        return end - start >= getThreshold();
    }

    @Override
    public void performValidation()
    {
        Validator validator = getValidator();
        if (validator == null || mTokenizer == null) return;

        Editable editable = getText();
        int index = editable.length();

        while (index > 0)
        {
            int start = mTokenizer.findTokenStart(editable, index);
            int end = mTokenizer.findTokenEnd(editable, start);

            CharSequence token = editable.subSequence(start, end);
            if (TextUtils.isEmpty(token)) editable.replace(start, index, "");
            else if (!validator.isValid(token)) editable.replace(start, index, mTokenizer.terminateToken(validator.fixText(token)));

            index = start;
        }
    }

    protected void performFiltering(CharSequence text, int start, int end, int keyCode)
    {
        Filter filter = getFilter();
        if (filter != null) filter.filter(text.subSequence(start, end), this);
    }

    @Override
    protected void replaceText(CharSequence text)
    {
        if (mTokenizer == null)
        {
            super.replaceText(text);
            return;
        }

        clearComposingText();

        Editable editable = getText();
        int end = cursorPosition();
        int start = mTokenizer.findTokenStart(editable, end);

        editable.replace(start, end, mTokenizer.terminateToken(text));
    }

    public CharSequence getAccessibilityClassName()
    {
        return MultiAutoCompleteTextView.class.getName();
    }

    public interface Tokenizer
    {
        int findTokenStart(CharSequence text, int cursor);

        int findTokenEnd(CharSequence text, int cursor);

        CharSequence terminateToken(CharSequence text);
    }

    public static class CommaTokenizer implements Tokenizer
    {
        public CommaTokenizer()
        {
        }

        @Override
        public int findTokenStart(CharSequence text, int cursor)
        {
            int index = cursor;

            while (index > 0 && text.charAt(index - 1) != ',') index--;
            while (index < cursor && text.charAt(index) == ' ') index++;

            return index;
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor)
        {
            int length = text.length();

            for (int index = cursor; index < length; index++)
            {
                if (text.charAt(index) == ',') return index;
            }

            return length;
        }

        @Override
        public CharSequence terminateToken(CharSequence text)
        {
            int index = text.length();

            while (index > 0 && text.charAt(index - 1) == ' ') index--;

            if (index > 0 && text.charAt(index - 1) == ',') return text;

            if (text instanceof Spanned)
            {
                SpannableString terminated = new SpannableString(text + ", ");
                TextUtils.copySpansFrom((Spanned)text, 0, text.length(), Object.class, terminated, 0);
                return terminated;
            }

            return text + ", ";
        }
    }
}
