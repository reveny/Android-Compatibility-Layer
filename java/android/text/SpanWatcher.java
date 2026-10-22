package android.text;

public interface SpanWatcher extends NoCopySpan
{
    void onSpanAdded(Spannable text, Object what, int start, int end);

    void onSpanRemoved(Spannable text, Object what, int start, int end);

    void onSpanChanged(Spannable text, Object what, int start, int end, int oldStart, int oldEnd);
}
