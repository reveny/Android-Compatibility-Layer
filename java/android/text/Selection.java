package android.text;

public class Selection
{
    public static final Object SELECTION_START = new StartSelectionObject();
    public static final Object SELECTION_END = new EndSelectionObject();

    private static final Object SELECTION_MEMORY = new MemoryTextWatcher();

    private Selection()
    {
    }

    public static final int getSelectionStart(CharSequence text)
    {
        if (!(text instanceof Spanned)) return -1;

        return ((Spanned)text).getSpanStart(SELECTION_START);
    }

    public static final int getSelectionEnd(CharSequence text)
    {
        if (!(text instanceof Spanned)) return -1;

        return ((Spanned)text).getSpanStart(SELECTION_END);
    }

    private static int getSelectionMemory(CharSequence text)
    {
        if (!(text instanceof Spanned)) return -1;

        return ((Spanned)text).getSpanStart(SELECTION_MEMORY);
    }

    public static void setSelection(Spannable text, int start, int stop)
    {
        setSelection(text, start, stop, -1);
    }

    private static void setSelection(Spannable text, int start, int stop, int memory)
    {
        if (text == null) return;

        int length = text.length();
        start = Math.max(0, Math.min(start, length));
        stop = Math.max(0, Math.min(stop, length));

        int oldStart = getSelectionStart(text);
        int oldEnd = getSelectionEnd(text);

        if (oldStart != start || oldEnd != stop)
        {
            text.setSpan(SELECTION_START, start, start, Spanned.SPAN_POINT_POINT | Spanned.SPAN_INTERMEDIATE);
            text.setSpan(SELECTION_END, stop, stop, Spanned.SPAN_POINT_POINT);
            updateMemory(text, memory);
        }
    }

    private static void updateMemory(Spannable text, int memory)
    {
        if (memory > -1)
        {
            if (memory != getSelectionMemory(text)) text.setSpan(SELECTION_MEMORY, memory, memory, Spanned.SPAN_POINT_POINT);
        }
        else
        {
            removeMemory(text);
        }
    }

    private static void removeMemory(Spannable text)
    {
        text.removeSpan(SELECTION_MEMORY);
    }

    public static final void setSelection(Spannable text, int index)
    {
        setSelection(text, index, index);
    }

    public static final void selectAll(Spannable text)
    {
        if (text != null) setSelection(text, 0, text.length());
    }

    public static final void extendSelection(Spannable text, int index)
    {
        extendSelection(text, index, -1);
    }

    private static void extendSelection(Spannable text, int index, int memory)
    {
        if (text == null) return;

        int length = text.length();
        index = Math.max(0, Math.min(index, length));

        if (text.getSpanStart(SELECTION_END) != index) text.setSpan(SELECTION_END, index, index, Spanned.SPAN_POINT_POINT);

        updateMemory(text, memory);
    }

    public static final void removeSelection(Spannable text)
    {
        if (text == null) return;

        text.removeSpan(SELECTION_START);
        text.removeSpan(SELECTION_END);
        removeMemory(text);
    }

    public static boolean moveUp(Spannable text, Layout layout)
    {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);

        if (start != end)
        {
            int min = Math.min(start, end);
            int max = Math.max(start, end);

            setSelection(text, min);

            return !(min == 0 && max == text.length());
        }

        int line = layout.getLineForOffset(end);

        if (line > 0)
        {
            setSelectionAndMemory(text, layout, line, end, -1, false);
            return true;
        }

        if (end != 0)
        {
            setSelection(text, 0);
            return true;
        }

        return false;
    }

    private static void setSelectionAndMemory(Spannable text, Layout layout, int line, int end, int direction, boolean extend)
    {
        int memory = getSelectionMemory(text);
        int origin = memory > -1 ? memory : end;
        float horizontal = layout.getPrimaryHorizontal(origin);
        int target = layout.getOffsetForHorizontal(line + direction, horizontal);

        if (extend) extendSelection(text, target, origin);
        else setSelection(text, target, target, origin);
    }

    public static boolean moveDown(Spannable text, Layout layout)
    {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);

        if (start != end)
        {
            int min = Math.min(start, end);
            int max = Math.max(start, end);

            setSelection(text, max);

            return !(min == 0 && max == text.length());
        }

        int line = layout.getLineForOffset(end);

        if (line < layout.getLineCount() - 1)
        {
            setSelectionAndMemory(text, layout, line, end, 1, false);
            return true;
        }

        if (end != text.length())
        {
            setSelection(text, text.length());
            return true;
        }

        return false;
    }

    public static boolean moveLeft(Spannable text, Layout layout)
    {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);

        if (start != end)
        {
            setSelection(text, Math.min(start, end));
            return true;
        }

        int target = layout == null ? TextUtils.getOffsetBefore(text, end) : layout.getOffsetToLeftOf(end);

        if (target != end)
        {
            setSelection(text, target);
            return true;
        }

        return false;
    }

    public static boolean moveRight(Spannable text, Layout layout)
    {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);

        if (start != end)
        {
            setSelection(text, Math.max(start, end));
            return true;
        }

        int target = layout == null ? TextUtils.getOffsetAfter(text, end) : layout.getOffsetToRightOf(end);

        if (target != end)
        {
            setSelection(text, target);
            return true;
        }

        return false;
    }

    public static boolean moveToParagraphStart(Spannable text, Layout layout)
    {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);
        if (start != end) return false;

        int target = paragraphStart(text, Math.min(start, end));
        setSelection(text, target);
        return true;
    }

    public static boolean moveToParagraphEnd(Spannable text, Layout layout)
    {
        int start = getSelectionStart(text);
        int end = getSelectionEnd(text);
        if (start != end) return false;

        int target = paragraphEnd(text, Math.max(start, end));
        setSelection(text, target);
        return true;
    }

    public static boolean extendToParagraphStart(Spannable text)
    {
        int end = getSelectionEnd(text);
        extendSelection(text, paragraphStart(text, end));
        return true;
    }

    public static boolean extendToParagraphEnd(Spannable text)
    {
        int end = getSelectionEnd(text);
        extendSelection(text, paragraphEnd(text, end));
        return true;
    }

    private static int paragraphStart(CharSequence text, int offset)
    {
        int index = Math.max(0, Math.min(offset, text.length()));
        while (index > 0 && text.charAt(index - 1) != '\n') index--;
        return index;
    }

    private static int paragraphEnd(CharSequence text, int offset)
    {
        int index = Math.max(0, Math.min(offset, text.length()));
        while (index < text.length() && text.charAt(index) != '\n') index++;
        return index;
    }

    public static boolean extendUp(Spannable text, Layout layout)
    {
        int end = getSelectionEnd(text);
        int line = layout.getLineForOffset(end);

        if (line > 0)
        {
            setSelectionAndMemory(text, layout, line, end, -1, true);
            return true;
        }

        if (end != 0)
        {
            extendSelection(text, 0);
            return true;
        }

        return true;
    }

    public static boolean extendDown(Spannable text, Layout layout)
    {
        int end = getSelectionEnd(text);
        int line = layout.getLineForOffset(end);

        if (line < layout.getLineCount() - 1)
        {
            setSelectionAndMemory(text, layout, line, end, 1, true);
            return true;
        }

        if (end != text.length())
        {
            extendSelection(text, text.length(), -1);
            return true;
        }

        return true;
    }

    public static boolean extendLeft(Spannable text, Layout layout)
    {
        int end = getSelectionEnd(text);
        int target = layout == null ? TextUtils.getOffsetBefore(text, end) : layout.getOffsetToLeftOf(end);

        if (target != end) extendSelection(text, target);

        return true;
    }

    public static boolean extendRight(Spannable text, Layout layout)
    {
        int end = getSelectionEnd(text);
        int target = layout == null ? TextUtils.getOffsetAfter(text, end) : layout.getOffsetToRightOf(end);

        if (target != end) extendSelection(text, target);

        return true;
    }

    public static boolean extendToLeftEdge(Spannable text, Layout layout)
    {
        int where = findEdge(text, layout, -1);
        extendSelection(text, where);
        return true;
    }

    public static boolean extendToRightEdge(Spannable text, Layout layout)
    {
        int where = findEdge(text, layout, 1);
        extendSelection(text, where);
        return true;
    }

    public static boolean moveToLeftEdge(Spannable text, Layout layout)
    {
        int where = findEdge(text, layout, -1);
        setSelection(text, where);
        return true;
    }

    public static boolean moveToRightEdge(Spannable text, Layout layout)
    {
        int where = findEdge(text, layout, 1);
        setSelection(text, where);
        return true;
    }

    private static int findEdge(Spannable text, Layout layout, int direction)
    {
        int point = Math.max(0, getSelectionEnd(text));
        int line = layout.getLineForOffset(point);

        if (direction < 0) return layout.getLineStart(line);

        int end = layout.getLineEnd(line);
        if (line == layout.getLineCount() - 1) return end;

        return Math.max(layout.getLineStart(line), end - 1);
    }

    private static final class StartSelectionObject implements NoCopySpan
    {
    }

    private static final class EndSelectionObject implements NoCopySpan
    {
    }

    private static final class MemoryTextWatcher implements NoCopySpan
    {
    }
}
