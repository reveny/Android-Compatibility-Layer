package android.text;

public interface Editable extends CharSequence, GetChars, Spannable, Appendable
{
    Editable replace(int start, int end, CharSequence source, int sourceStart, int sourceEnd);

    Editable replace(int start, int end, CharSequence text);

    Editable insert(int where, CharSequence text, int start, int end);

    Editable insert(int where, CharSequence text);

    Editable delete(int start, int end);

    Editable append(CharSequence text);

    Editable append(CharSequence text, int start, int end);

    Editable append(char text);

    void clear();

    void clearSpans();

    void setFilters(InputFilter[] filters);

    InputFilter[] getFilters();

    interface Factory
    {
        Editable newEditable(CharSequence source);
    }
}
