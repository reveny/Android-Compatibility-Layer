package android.text;

public interface TextWatcher extends android.text.NoCopySpan
{
    void beforeTextChanged(CharSequence text, int start, int count, int after);

    void onTextChanged(CharSequence text, int start, int before, int count);

    void afterTextChanged(Editable text);
}
