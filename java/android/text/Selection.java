package android.text;

public class Selection
{
    public static final Object SELECTION_START = new NoCopySpan.Concrete();
    public static final Object SELECTION_END = new NoCopySpan.Concrete();

    private Selection()
    {
    }

    public static int getSelectionStart(CharSequence text)
    {
        if (!(text instanceof Spanned)) return -1;

        return ((Spanned)text).getSpanStart(SELECTION_START);
    }

    public static int getSelectionEnd(CharSequence text)
    {
        if (!(text instanceof Spanned)) return -1;

        return ((Spanned)text).getSpanStart(SELECTION_END);
    }

    public static void setSelection(Spannable text, int start, int stop)
    {
        if (text == null) return;

        int length = text.length();
        int from = Math.max(0, Math.min(start, length));
        int to = Math.max(0, Math.min(stop, length));

        text.setSpan(SELECTION_START, from, from, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(SELECTION_END, to, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static void setSelection(Spannable text, int index)
    {
        setSelection(text, index, index);
    }

    public static void selectAll(Spannable text)
    {
        if (text != null) setSelection(text, 0, text.length());
    }

    public static void removeSelection(Spannable text)
    {
        if (text == null) return;

        text.removeSpan(SELECTION_START);
        text.removeSpan(SELECTION_END);
    }

    public static boolean moveLeft(Spannable text, Layout layout)
    {
        int start = getSelectionStart(text);

        if (start <= 0) return false;

        setSelection(text, start - 1);
        return true;
    }

    public static boolean moveRight(Spannable text, Layout layout)
    {
        int end = getSelectionEnd(text);

        if (text == null || end >= text.length()) return false;

        setSelection(text, end + 1);
        return true;
    }

    public static boolean extendSelection(Spannable text, int index)
    {
        if (text == null) return false;

        setSelection(text, getSelectionStart(text), index);
        return true;
    }
}
