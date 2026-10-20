import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

public class StaticLayoutTest
{
    private static int failures = 0;

    private static void check(boolean condition, String what)
    {
        System.out.println((condition ? "PASS " : "FAIL ") + what);
        if (!condition) failures++;
    }

    private static TextPaint paintAt(float textSize)
    {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        return paint;
    }

    private static StaticLayout layout(String text, float textSize, int width)
    {
        return StaticLayout.Builder.obtain(text, 0, text.length(), paintAt(textSize), width).build();
    }

    private static void testSeamIsPureJava()
    {
        runtime.TextMeasurers.install(new runtime.FixedAdvanceTextMeasurer());

        check(!runtime.TextMeasurers.isNativeBacked(),
              "the host JVM uses the fixed-advance measurer, so no native is required");
        check(runtime.TextMeasurers.get() != null, "a measurer is always available");
    }

    private static void testSingleLineWhenItFits()
    {
        StaticLayout wide = layout("Hello World!", 14f, 10000);
        check(wide.getLineCount() == 1, "text that fits stays on one line");
        check(wide.getLineStart(0) == 0, "the first line starts at zero");
        check(wide.getLineEnd(0) == "Hello World!".length(), "the only line covers the whole string");
    }

    private static void testHardBreaksAlwaysSplit()
    {
        StaticLayout broken = layout("a\nb\nc", 14f, 10000);
        check(broken.getLineCount() == 3, "newlines split lines even when everything fits");

        StaticLayout trailing = layout("ab\n", 14f, 10000);
        check(trailing.getLineCount() == 2, "a trailing newline produces an empty final line");
    }

    private static void testWrappingHappensAtWordBoundaries()
    {
        StaticLayout wrapped = layout("aaa bbb ccc ddd", 14f, 40);
        check(wrapped.getLineCount() > 1, "a narrow width forces wrapping");

        boolean breaksMidWord = false;
        for (int line = 0; line < wrapped.getLineCount(); line++)
        {
            int start = wrapped.getLineStart(line);
            if (start > 0 && wrapped.getText().charAt(start - 1) != ' ') breaksMidWord = true;
        }
        check(!breaksMidWord, "wrapping lands on word boundaries, not mid-word");
    }

    private static void testHeightGrowsWithLines()
    {
        StaticLayout one = layout("aaa", 14f, 10000);
        StaticLayout many = layout("aaa bbb ccc ddd eee fff", 14f, 40);

        check(many.getLineCount() > one.getLineCount(), "the wrapped layout has more lines");
        check(many.getHeight() > one.getHeight(), "height grows with line count");
        check(one.getHeight() > 0, "a single line still has positive height");
    }

    private static void testLineGeometryIsMonotonic()
    {
        StaticLayout wrapped = layout("aaa bbb ccc ddd eee", 14f, 40);

        boolean monotonic = true;
        for (int line = 1; line < wrapped.getLineCount(); line++)
        {
            if (wrapped.getLineTop(line) < wrapped.getLineTop(line - 1)) monotonic = false;
            if (wrapped.getLineStart(line) < wrapped.getLineStart(line - 1)) monotonic = false;
        }

        check(monotonic, "line tops and starts increase monotonically");
        check(wrapped.getLineBottom(wrapped.getLineCount() - 1) == wrapped.getHeight(),
              "the last line's bottom is the layout height");
    }

    private static void testEmptyTextIsOneLine()
    {
        StaticLayout empty = layout("", 14f, 100);
        check(empty.getLineCount() == 1, "empty text still reports one line");
        check(empty.getHeight() > 0, "empty text still has a line height");
    }

    private static void testDesiredWidthTracksTheLongestLine()
    {
        TextPaint paint = paintAt(14f);
        float single = Layout.getDesiredWidth("aaaa", paint);
        float multi = Layout.getDesiredWidth("aa\naaaa\na", paint);

        check(single > 0f, "desired width is positive");
        check(Math.abs(multi - single) < 0.001f, "desired width is the widest line, not the total");
    }

    private static void testStyleKeyTracksTextSize()
    {
        Paint paint = new Paint();
        paint.setTextSize(14f);
        int first = paint.styleId();

        paint.setTextSize(28f);
        int second = paint.styleId();

        check(first != second, "changing the text size invalidates the style key");
        check(paint.measureText("aaaa") > 0f, "measurement still works after the size change");
    }

    private static void testUnbreakableTokenStillWraps()
    {
        StaticLayout layout = layout("supercalifragilistic", 14f, 20);

        check(layout.getLineCount() > 1, "a single unbreakable word wraps by character rather than overflowing");

        float widest = 0f;
        for (int line = 0; line < layout.getLineCount(); line++)
        {
            widest = Math.max(widest, layout.getLineWidth(line));
        }
        check(widest <= 20f, "no line of a character-wrapped word exceeds the requested width");
    }

    private static void testZeroWidthIsNotInfinite()
    {
        StaticLayout narrow = layout("aaa bbb ccc ddd", 14f, 1);
        StaticLayout zero = layout("aaa bbb ccc ddd", 14f, 0);

        check(zero.getLineCount() > 1, "width 0 wraps rather than meaning infinitely wide");
        check(zero.getLineCount() >= narrow.getLineCount(), "width 0 wraps at least as hard as width 1");
    }

    private static void testHardBreakIsNotMeasured()
    {
        StaticLayout broken = layout("ab\ncd", 14f, 10000);
        StaticLayout plain = layout("ab", 14f, 10000);

        check(broken.getLineWidth(0) == plain.getLineWidth(0),
              "a hard-broken line measures its visible text, not the newline that ended it");
    }

    private static void testTrailingSpaceDoesNotForceAnEarlyWrap()
    {
        TextPaint paint = paintAt(14f);
        float twoWords = paint.measureText("aaa bbb");

        StaticLayout layout = StaticLayout.Builder.obtain("aaa bbb ccc", 0, 11, paint, (int)twoWords).build();
        check(layout.getLineEnd(0) > 4, "trailing whitespace is not counted when deciding whether a line fits");
    }

    public static void main(String[] args)
    {
        testSeamIsPureJava();
        testSingleLineWhenItFits();
        testHardBreaksAlwaysSplit();
        testWrappingHappensAtWordBoundaries();
        testHeightGrowsWithLines();
        testLineGeometryIsMonotonic();
        testEmptyTextIsOneLine();
        testDesiredWidthTracksTheLongestLine();
        testStyleKeyTracksTextSize();
        testUnbreakableTokenStillWraps();
        testZeroWidthIsNotInfinite();
        testHardBreakIsNotMeasured();
        testTrailingSpaceDoesNotForceAnEarlyWrap();

        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        if (failures != 0) System.exit(1);
    }
}
