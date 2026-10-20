package android.text;

public interface Spannable extends Spanned
{
    void setSpan(Object what, int start, int end, int flags);

    void removeSpan(Object what);

    interface Factory
    {
        Spannable newSpannable(CharSequence source);
    }
}
