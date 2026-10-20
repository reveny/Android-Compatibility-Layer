package android.text;

public interface Spanned extends CharSequence
{
    int SPAN_INCLUSIVE_EXCLUSIVE = 0x11;
    int SPAN_EXCLUSIVE_EXCLUSIVE = 0x21;

    <T> T[] getSpans(int start, int end, Class<T> type);

    int getSpanStart(Object span);

    int getSpanEnd(Object span);

    int getSpanFlags(Object span);

    int nextSpanTransition(int start, int limit, Class<?> type);
}
