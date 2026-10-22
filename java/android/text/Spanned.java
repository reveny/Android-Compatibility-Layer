package android.text;

public interface Spanned extends CharSequence
{
    int SPAN_POINT_MARK_MASK = 0x33;
    int SPAN_MARK_MARK = 0x11;
    int SPAN_MARK_POINT = 0x12;
    int SPAN_POINT_MARK = 0x21;
    int SPAN_POINT_POINT = 0x22;
    int SPAN_PARAGRAPH = 0x33;
    int SPAN_INCLUSIVE_EXCLUSIVE = SPAN_MARK_MARK;
    int SPAN_INCLUSIVE_INCLUSIVE = SPAN_MARK_POINT;
    int SPAN_EXCLUSIVE_EXCLUSIVE = SPAN_POINT_MARK;
    int SPAN_EXCLUSIVE_INCLUSIVE = SPAN_POINT_POINT;
    int SPAN_COMPOSING = 0x100;
    int SPAN_INTERMEDIATE = 0x200;
    int SPAN_USER_SHIFT = 24;
    int SPAN_USER = 0xFFFFFFFF << SPAN_USER_SHIFT;
    int SPAN_PRIORITY_SHIFT = 16;
    int SPAN_PRIORITY = 0xFF << SPAN_PRIORITY_SHIFT;

    <T> T[] getSpans(int start, int end, Class<T> type);

    int getSpanStart(Object span);

    int getSpanEnd(Object span);

    int getSpanFlags(Object span);

    int nextSpanTransition(int start, int limit, Class type);
}
