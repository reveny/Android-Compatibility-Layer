package android.text;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Html
{
    public static final int TO_HTML_PARAGRAPH_LINES_CONSECUTIVE = 0x00000000;
    public static final int TO_HTML_PARAGRAPH_LINES_INDIVIDUAL = 0x00000001;

    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH = 0x00000001;
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_HEADING = 0x00000002;
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM = 0x00000004;
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_LIST = 0x00000008;
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_DIV = 0x00000010;
    public static final int FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE = 0x00000020;
    public static final int FROM_HTML_OPTION_USE_CSS_COLORS = 0x00000100;

    public static final int FROM_HTML_MODE_LEGACY = 0x00000000;
    public static final int FROM_HTML_MODE_COMPACT = FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH | FROM_HTML_SEPARATOR_LINE_BREAK_HEADING
                                                     | FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM | FROM_HTML_SEPARATOR_LINE_BREAK_LIST
                                                     | FROM_HTML_SEPARATOR_LINE_BREAK_DIV
                                                     | FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE;

    public interface ImageGetter
    {
        Drawable getDrawable(String source);
    }

    public interface TagHandler
    {
        void handleTag(boolean opening, String tag, Editable output, org.xml.sax.XMLReader xmlReader);
    }

    private Html()
    {
    }

    @Deprecated
    public static Spanned fromHtml(String source)
    {
        return fromHtml(source, FROM_HTML_MODE_LEGACY, null, null);
    }

    public static Spanned fromHtml(String source, int flags)
    {
        return fromHtml(source, flags, null, null);
    }

    @Deprecated
    public static Spanned fromHtml(String source, ImageGetter imageGetter, TagHandler tagHandler)
    {
        return fromHtml(source, FROM_HTML_MODE_LEGACY, imageGetter, tagHandler);
    }

    public static Spanned fromHtml(String source, int flags, ImageGetter imageGetter, TagHandler tagHandler)
    {
        return new HtmlToSpannedConverter(source == null ? "" : source, imageGetter, tagHandler, flags).convert();
    }

    @Deprecated
    public static String toHtml(Spanned text)
    {
        return toHtml(text, TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
    }

    public static String toHtml(Spanned text, int option)
    {
        StringBuilder out = new StringBuilder();
        withinHtml(out, text, option);
        return out.toString();
    }

    public static String escapeHtml(CharSequence text)
    {
        StringBuilder out = new StringBuilder();
        withinStyle(out, text, 0, text.length());
        return out.toString();
    }

    private static void withinHtml(StringBuilder out, Spanned text, int option)
    {
        if ((option & TO_HTML_PARAGRAPH_LINES_INDIVIDUAL) == TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        {
            encodeTextAlignmentByDiv(out, text, option);
            return;
        }

        withinDiv(out, text, 0, text.length(), option);
    }

    private static void encodeTextAlignmentByDiv(StringBuilder out, Spanned text, int option)
    {
        int length = text.length();

        int next;
        for (int index = 0; index < length; index = next)
        {
            next = text.nextSpanTransition(index, length, ParagraphStyle.class);
            ParagraphStyle[] styles = text.getSpans(index, next, ParagraphStyle.class);
            String elements = " ";
            boolean needDiv = false;

            for (ParagraphStyle style : styles)
            {
                if (style instanceof AlignmentSpan)
                {
                    Layout.Alignment alignment = ((AlignmentSpan)style).getAlignment();
                    needDiv = true;

                    if (alignment == Layout.Alignment.ALIGN_CENTER) elements = "align=\"center\" " + elements;
                    else if (alignment == Layout.Alignment.ALIGN_OPPOSITE) elements = "align=\"right\" " + elements;
                    else elements = "align=\"left\" " + elements;
                }
            }

            if (needDiv) out.append("<div ").append(elements).append(">");

            withinDiv(out, text, index, next, option);

            if (needDiv) out.append("</div>");
        }
    }

    private static void withinDiv(StringBuilder out, Spanned text, int start, int end, int option)
    {
        int next;
        for (int index = start; index < end; index = next)
        {
            next = text.nextSpanTransition(index, end, QuoteSpan.class);
            QuoteSpan[] quotes = text.getSpans(index, next, QuoteSpan.class);

            for (int count = 0; count < quotes.length; count++)
            {
                out.append("<blockquote>");
            }

            withinBlockquote(out, text, index, next, option);

            for (int count = 0; count < quotes.length; count++)
            {
                out.append("</blockquote>\n");
            }
        }
    }

    private static String getTextDirection(Spanned text, int start, int end)
    {
        if (TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(text, start, end - start)) return " dir=\"rtl\"";

        return " dir=\"ltr\"";
    }

    private static String getTextStyles(Spanned text, int start, int end, boolean forceNoVerticalMargin, boolean includeTextAlign)
    {
        String margin = null;
        String textAlign = null;

        if (forceNoVerticalMargin) margin = "margin-top:0; margin-bottom:0;";

        if (includeTextAlign)
        {
            AlignmentSpan[] alignmentSpans = text.getSpans(start, end, AlignmentSpan.class);

            for (int index = alignmentSpans.length - 1; index >= 0; index--)
            {
                AlignmentSpan span = alignmentSpans[index];

                if ((text.getSpanFlags(span) & Spanned.SPAN_PARAGRAPH) == Spanned.SPAN_PARAGRAPH)
                {
                    Layout.Alignment alignment = span.getAlignment();

                    if (alignment == Layout.Alignment.ALIGN_NORMAL) textAlign = "text-align:start;";
                    else if (alignment == Layout.Alignment.ALIGN_CENTER) textAlign = "text-align:center;";
                    else if (alignment == Layout.Alignment.ALIGN_OPPOSITE) textAlign = "text-align:end;";

                    break;
                }
            }
        }

        if (margin == null && textAlign == null) return "";

        StringBuilder style = new StringBuilder(" style=\"");
        if (margin != null && textAlign != null) style.append(margin).append(" ").append(textAlign);
        else if (margin != null) style.append(margin);
        else style.append(textAlign);

        return style.append("\"").toString();
    }

    private static void withinBlockquote(StringBuilder out, Spanned text, int start, int end, int option)
    {
        if ((option & TO_HTML_PARAGRAPH_LINES_INDIVIDUAL) == TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        {
            withinBlockquoteConsecutive(out, text, start, end);
        }
        else
        {
            withinBlockquoteIndividual(out, text, start, end);
        }
    }

    private static void withinBlockquoteIndividual(StringBuilder out, Spanned text, int start, int end)
    {
        boolean isInList = false;

        int next;
        for (int index = start; index <= end; index = next)
        {
            next = TextUtils.indexOf(text, '\n', index, end);
            if (next < 0) next = end;

            if (next == index)
            {
                if (isInList)
                {
                    isInList = false;
                    out.append("</ul>\n");
                }

                out.append("<br>\n");
            }
            else
            {
                boolean isListItem = false;
                ParagraphStyle[] paragraphStyles = text.getSpans(index, next, ParagraphStyle.class);

                for (ParagraphStyle paragraphStyle : paragraphStyles)
                {
                    int spanFlags = text.getSpanFlags(paragraphStyle);
                    if ((spanFlags & Spanned.SPAN_PARAGRAPH) == Spanned.SPAN_PARAGRAPH && paragraphStyle instanceof BulletSpan)
                    {
                        isListItem = true;
                        break;
                    }
                }

                if (isListItem && !isInList)
                {
                    isInList = true;
                    out.append("<ul").append(getTextStyles(text, index, next, true, false)).append(">\n");
                }

                if (isInList && !isListItem)
                {
                    isInList = false;
                    out.append("</ul>\n");
                }

                String tagType = isListItem ? "li" : "p";
                out.append("<").append(tagType).append(getTextDirection(text, index, next))
                    .append(getTextStyles(text, index, next, !isListItem, true)).append(">");

                withinParagraph(out, text, index, next);

                out.append("</").append(tagType).append(">\n");

                if (next == end && isInList)
                {
                    isInList = false;
                    out.append("</ul>\n");
                }
            }

            next++;
        }
    }

    private static void withinBlockquoteConsecutive(StringBuilder out, Spanned text, int start, int end)
    {
        out.append("<p").append(getTextDirection(text, start, end)).append(">");

        int next;
        for (int index = start; index < end; index = next)
        {
            next = TextUtils.indexOf(text, '\n', index, end);
            if (next < 0) next = end;

            int newlines = 0;

            while (next < end && text.charAt(next) == '\n')
            {
                newlines++;
                next++;
            }

            withinParagraph(out, text, index, next - newlines);

            if (newlines == 1)
            {
                out.append("<br>\n");
            }
            else
            {
                for (int count = 2; count < newlines; count++)
                {
                    out.append("<br>");
                }

                if (next != end) out.append("</p>\n").append("<p").append(getTextDirection(text, start, end)).append(">");
            }
        }

        out.append("</p>\n");
    }

    private static void withinParagraph(StringBuilder out, Spanned text, int start, int end)
    {
        int next;
        for (int index = start; index < end; index = next)
        {
            next = text.nextSpanTransition(index, end, CharacterStyle.class);
            CharacterStyle[] style = text.getSpans(index, next, CharacterStyle.class);

            for (CharacterStyle span : style)
            {
                if (span instanceof StyleSpan)
                {
                    int value = ((StyleSpan)span).getStyle();

                    if ((value & Typeface.BOLD) != 0) out.append("<b>");
                    if ((value & Typeface.ITALIC) != 0) out.append("<i>");
                }

                if (span instanceof TypefaceSpan)
                {
                    String face = ((TypefaceSpan)span).getFamily();
                    if ("monospace".equals(face)) out.append("<tt>");
                }

                if (span instanceof SuperscriptSpan) out.append("<sup>");
                if (span instanceof SubscriptSpan) out.append("<sub>");
                if (span instanceof UnderlineSpan) out.append("<u>");
                if (span instanceof StrikethroughSpan) out.append("<span style=\"text-decoration:line-through;\">");
                if (span instanceof URLSpan) out.append("<a href=\"").append(((URLSpan)span).getURL()).append("\">");

                if (span instanceof ImageSpan)
                {
                    out.append("<img src=\"").append(((ImageSpan)span).getSource()).append("\">");
                    index = next;
                }

                if (span instanceof AbsoluteSizeSpan)
                {
                    AbsoluteSizeSpan size = (AbsoluteSizeSpan)span;
                    float sizeDip = size.getSize();

                    if (!size.getDip()) sizeDip /= android.content.res.Resources.getSystem().getDisplayMetrics().density;

                    out.append(String.format(Locale.US, "<span style=\"font-size:%.0fpx\";>", sizeDip));
                }

                if (span instanceof RelativeSizeSpan)
                {
                    float sizeEm = ((RelativeSizeSpan)span).getSizeChange();
                    out.append(String.format(Locale.US, "<span style=\"font-size:%.2fem;\">", sizeEm));
                }

                if (span instanceof ForegroundColorSpan)
                {
                    int color = ((ForegroundColorSpan)span).getForegroundColor();
                    out.append(String.format("<span style=\"color:#%06X;\">", 0xFFFFFF & color));
                }

                if (span instanceof BackgroundColorSpan)
                {
                    int color = ((BackgroundColorSpan)span).getBackgroundColor();
                    out.append(String.format("<span style=\"background-color:#%06X;\">", 0xFFFFFF & color));
                }
            }

            withinStyle(out, text, index, next);

            for (int count = style.length - 1; count >= 0; count--)
            {
                CharacterStyle span = style[count];

                if (span instanceof BackgroundColorSpan) out.append("</span>");
                if (span instanceof ForegroundColorSpan) out.append("</span>");
                if (span instanceof RelativeSizeSpan) out.append("</span>");
                if (span instanceof AbsoluteSizeSpan) out.append("</span>");
                if (span instanceof URLSpan) out.append("</a>");
                if (span instanceof StrikethroughSpan) out.append("</span>");
                if (span instanceof UnderlineSpan) out.append("</u>");
                if (span instanceof SubscriptSpan) out.append("</sub>");
                if (span instanceof SuperscriptSpan) out.append("</sup>");

                if (span instanceof TypefaceSpan)
                {
                    String face = ((TypefaceSpan)span).getFamily();
                    if ("monospace".equals(face)) out.append("</tt>");
                }

                if (span instanceof StyleSpan)
                {
                    int value = ((StyleSpan)span).getStyle();

                    if ((value & Typeface.BOLD) != 0) out.append("</b>");
                    if ((value & Typeface.ITALIC) != 0) out.append("</i>");
                }
            }
        }
    }

    private static void withinStyle(StringBuilder out, CharSequence text, int start, int end)
    {
        for (int index = start; index < end; index++)
        {
            char character = text.charAt(index);

            if (character == '<')
            {
                out.append("&lt;");
            }
            else if (character == '>')
            {
                out.append("&gt;");
            }
            else if (character == '&')
            {
                out.append("&amp;");
            }
            else if (character >= 0xD800 && character <= 0xDFFF)
            {
                if (character < 0xDC00 && index + 1 < end)
                {
                    char low = text.charAt(index + 1);

                    if (low >= 0xDC00 && low <= 0xDFFF)
                    {
                        index++;
                        int codePoint = 0x010000 | (int)character - 0xD800 << 10 | (int)low - 0xDC00;
                        out.append("&#").append(codePoint).append(";");
                    }
                }
            }
            else if (character > 0x7E || character < ' ')
            {
                out.append("&#").append((int)character).append(";");
            }
            else if (character == ' ')
            {
                while (index + 1 < end && text.charAt(index + 1) == ' ')
                {
                    out.append("&nbsp;");
                    index++;
                }

                out.append(' ');
            }
            else
            {
                out.append(character);
            }
        }
    }

    private static final class HtmlToSpannedConverter
    {
        private static final float[] HEADING_SIZES = {1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f};

        private static final Pattern TEXT_ALIGN_PATTERN = Pattern.compile("(?:\\s+|\\A)text-align\\s*:\\s*(\\S*)\\b");
        private static final Pattern FOREGROUND_COLOR_PATTERN = Pattern.compile("(?:\\s+|\\A)color\\s*:\\s*(\\S*)\\b");
        private static final Pattern BACKGROUND_COLOR_PATTERN =
            Pattern.compile("(?:\\s+|\\A)background(?:-color)?\\s*:\\s*(\\S*)\\b");
        private static final Pattern TEXT_DECORATION_PATTERN = Pattern.compile("(?:\\s+|\\A)text-decoration\\s*:\\s*(\\S*)\\b");

        private static final HashMap<String, Integer> COLORS = new HashMap<>();
        private static final HashMap<String, String> ENTITIES = new HashMap<>();

        static
        {
            COLORS.put("aqua", 0x00FFFF);
            COLORS.put("black", 0x000000);
            COLORS.put("blue", 0x0000FF);
            COLORS.put("darkgray", 0xA9A9A9);
            COLORS.put("darkgrey", 0xA9A9A9);
            COLORS.put("fuchsia", 0xFF00FF);
            COLORS.put("gray", 0x808080);
            COLORS.put("grey", 0x808080);
            COLORS.put("green", 0x008000);
            COLORS.put("lightgray", 0xD3D3D3);
            COLORS.put("lightgrey", 0xD3D3D3);
            COLORS.put("lime", 0x00FF00);
            COLORS.put("maroon", 0x800000);
            COLORS.put("navy", 0x000080);
            COLORS.put("olive", 0x808000);
            COLORS.put("purple", 0x800080);
            COLORS.put("red", 0xFF0000);
            COLORS.put("silver", 0xC0C0C0);
            COLORS.put("teal", 0x008080);
            COLORS.put("white", 0xFFFFFF);
            COLORS.put("yellow", 0xFFFF00);
            COLORS.put("cyan", 0x00FFFF);
            COLORS.put("magenta", 0xFF00FF);
            COLORS.put("orange", 0xFFA500);

            ENTITIES.put("amp", "&");
            ENTITIES.put("lt", "<");
            ENTITIES.put("gt", ">");
            ENTITIES.put("quot", "\"");
            ENTITIES.put("apos", "'");
            ENTITIES.put("nbsp", " ");
            ENTITIES.put("copy", "©");
            ENTITIES.put("reg", "®");
            ENTITIES.put("trade", "™");
            ENTITIES.put("hellip", "…");
            ENTITIES.put("mdash", "—");
            ENTITIES.put("ndash", "–");
            ENTITIES.put("lsquo", "‘");
            ENTITIES.put("rsquo", "’");
            ENTITIES.put("ldquo", "“");
            ENTITIES.put("rdquo", "”");
            ENTITIES.put("bull", "•");
            ENTITIES.put("middot", "·");
            ENTITIES.put("euro", "€");
            ENTITIES.put("pound", "£");
            ENTITIES.put("yen", "¥");
            ENTITIES.put("cent", "¢");
            ENTITIES.put("laquo", "«");
            ENTITIES.put("raquo", "»");
            ENTITIES.put("times", "×");
            ENTITIES.put("divide", "÷");
            ENTITIES.put("deg", "°");
            ENTITIES.put("plusmn", "±");
            ENTITIES.put("sect", "§");
            ENTITIES.put("para", "¶");
            ENTITIES.put("iexcl", "¡");
            ENTITIES.put("iquest", "¿");
            ENTITIES.put("shy", "­");
            ENTITIES.put("zwnj", "‌");
            ENTITIES.put("zwj", "‍");
            ENTITIES.put("lrm", "‎");
            ENTITIES.put("rlm", "‏");
            ENTITIES.put("ensp", " ");
            ENTITIES.put("emsp", " ");
            ENTITIES.put("thinsp", " ");
            ENTITIES.put("larr", "←");
            ENTITIES.put("rarr", "→");
            ENTITIES.put("uarr", "↑");
            ENTITIES.put("darr", "↓");
            ENTITIES.put("hearts", "♥");
            ENTITIES.put("star", "☆");
        }

        private final String mSource;
        private final SpannableStringBuilder mSpannableStringBuilder = new SpannableStringBuilder();
        private final ImageGetter mImageGetter;
        private final TagHandler mTagHandler;
        private final int mFlags;
        private final ArrayList<String> mOpenTags = new ArrayList<>();
        private final StringBuilder mPendingText = new StringBuilder();
        private final org.xml.sax.XMLReader mReader = new NullXmlReader();
        private int mSkipDepth;

        HtmlToSpannedConverter(String source, ImageGetter imageGetter, TagHandler tagHandler, int flags)
        {
            mSource = source;
            mImageGetter = imageGetter;
            mTagHandler = tagHandler;
            mFlags = flags;
        }

        Spanned convert()
        {
            parse();
            flushText();

            for (int index = mOpenTags.size() - 1; index >= 0; index--)
            {
                handleEndTag(mOpenTags.get(index));
            }

            mOpenTags.clear();

            Object[] spans = mSpannableStringBuilder.getSpans(0, mSpannableStringBuilder.length(), ParagraphStyle.class);

            for (Object span : spans)
            {
                int start = mSpannableStringBuilder.getSpanStart(span);
                int end = mSpannableStringBuilder.getSpanEnd(span);

                if (end - 2 >= 0 && mSpannableStringBuilder.charAt(end - 1) == '\n' && mSpannableStringBuilder.charAt(end - 2) == '\n')
                {
                    end--;
                }

                if (end == start)
                {
                    mSpannableStringBuilder.removeSpan(span);
                    continue;
                }

                try
                {
                    mSpannableStringBuilder.setSpan(span, start, end, Spanned.SPAN_PARAGRAPH);
                }
                catch (RuntimeException exception)
                {
                    mSpannableStringBuilder.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            return mSpannableStringBuilder;
        }

        private void parse()
        {
            String source = mSource;
            int length = source.length();
            int index = 0;

            while (index < length)
            {
                char character = source.charAt(index);

                if (character != '<')
                {
                    int next = source.indexOf('<', index);
                    if (next < 0) next = length;

                    if (mSkipDepth == 0) mPendingText.append(source, index, next);
                    index = next;
                    continue;
                }

                if (source.startsWith("<!--", index))
                {
                    int close = source.indexOf("-->", index + 4);
                    index = close < 0 ? length : close + 3;
                    continue;
                }

                if (index + 1 < length && (source.charAt(index + 1) == '!' || source.charAt(index + 1) == '?'))
                {
                    int close = source.indexOf('>', index);
                    index = close < 0 ? length : close + 1;
                    continue;
                }

                boolean closing = index + 1 < length && source.charAt(index + 1) == '/';
                int nameStart = index + (closing ? 2 : 1);
                int nameEnd = nameStart;

                while (nameEnd < length && (Character.isLetterOrDigit(source.charAt(nameEnd)) || source.charAt(nameEnd) == '-'
                                            || source.charAt(nameEnd) == ':' || source.charAt(nameEnd) == '_'))
                {
                    nameEnd++;
                }

                if (nameEnd == nameStart)
                {
                    if (mSkipDepth == 0) mPendingText.append('<');
                    index++;
                    continue;
                }

                String name = source.substring(nameStart, nameEnd).toLowerCase(Locale.ROOT);
                HashMap<String, String> attributes = new HashMap<>();
                int position = parseAttributes(source, nameEnd, attributes);
                boolean selfClosing = position > 0 && position <= length && source.charAt(position - 1) == '>' && position - 2 >= nameEnd
                                      && source.charAt(position - 2) == '/';

                index = position;

                if (closing)
                {
                    closeTag(name);
                }
                else
                {
                    openTag(name, attributes);
                    if (selfClosing && !isVoid(name)) closeTag(name);
                }
            }
        }

        private static int parseAttributes(String source, int position, HashMap<String, String> attributes)
        {
            int length = source.length();

            while (position < length)
            {
                char character = source.charAt(position);

                if (character == '>') return position + 1;

                if (Character.isWhitespace(character) || character == '/')
                {
                    position++;
                    continue;
                }

                int nameStart = position;
                while (position < length)
                {
                    char current = source.charAt(position);
                    if (Character.isWhitespace(current) || current == '=' || current == '>' || current == '/') break;
                    position++;
                }

                String name = source.substring(nameStart, position).toLowerCase(Locale.ROOT);

                while (position < length && Character.isWhitespace(source.charAt(position))) position++;

                String value = "";

                if (position < length && source.charAt(position) == '=')
                {
                    position++;
                    while (position < length && Character.isWhitespace(source.charAt(position))) position++;

                    if (position < length && (source.charAt(position) == '"' || source.charAt(position) == '\''))
                    {
                        char quote = source.charAt(position);
                        int close = source.indexOf(quote, position + 1);
                        if (close < 0) close = length;

                        value = source.substring(position + 1, close);
                        position = Math.min(length, close + 1);
                    }
                    else
                    {
                        int valueStart = position;
                        while (position < length && !Character.isWhitespace(source.charAt(position)) && source.charAt(position) != '>')
                        {
                            position++;
                        }

                        value = source.substring(valueStart, position);
                    }
                }

                if (!name.isEmpty()) attributes.put(name, decodeEntities(value));
            }

            return length;
        }

        private static boolean isVoid(String name)
        {
            return name.equals("br") || name.equals("img") || name.equals("hr") || name.equals("input") || name.equals("meta")
                   || name.equals("link") || name.equals("area") || name.equals("base") || name.equals("col") || name.equals("wbr");
        }

        private void openTag(String name, HashMap<String, String> attributes)
        {
            if (name.equals("style") || name.equals("script"))
            {
                flushText();
                mSkipDepth++;
                mOpenTags.add(name);
                return;
            }

            if (mSkipDepth > 0) return;

            flushText();
            handleStartTag(name, attributes);

            if (name.equals("br"))
            {
                handleEndTag(name);
                return;
            }

            if (!isVoid(name)) mOpenTags.add(name);
        }

        private void closeTag(String name)
        {
            int position = mOpenTags.lastIndexOf(name);
            if (position < 0) return;

            if (mSkipDepth == 0) flushText();

            for (int index = mOpenTags.size() - 1; index >= position; index--)
            {
                String open = mOpenTags.remove(index);

                if (open.equals("style") || open.equals("script"))
                {
                    mSkipDepth = Math.max(0, mSkipDepth - 1);
                    mPendingText.setLength(0);
                    continue;
                }

                if (mSkipDepth == 0) handleEndTag(open);
            }
        }

        private void flushText()
        {
            if (mPendingText.length() == 0) return;

            String decoded = decodeEntities(mPendingText.toString());
            mPendingText.setLength(0);

            StringBuilder builder = new StringBuilder();

            for (int index = 0; index < decoded.length(); index++)
            {
                char character = decoded.charAt(index);

                if (character == ' ' || character == '\n' || character == '\t' || character == '\r' || character == '\f')
                {
                    char previous;
                    int length = builder.length();

                    if (length == 0)
                    {
                        length = mSpannableStringBuilder.length();
                        previous = length == 0 ? '\n' : mSpannableStringBuilder.charAt(length - 1);
                    }
                    else
                    {
                        previous = builder.charAt(length - 1);
                    }

                    if (previous != ' ' && previous != '\n') builder.append(' ');
                }
                else
                {
                    builder.append(character);
                }
            }

            mSpannableStringBuilder.append(builder);
        }

        private static String decodeEntities(String text)
        {
            if (text.indexOf('&') < 0) return text;

            StringBuilder builder = new StringBuilder(text.length());
            int length = text.length();

            for (int index = 0; index < length; index++)
            {
                char character = text.charAt(index);

                if (character != '&')
                {
                    builder.append(character);
                    continue;
                }

                int semicolon = text.indexOf(';', index + 1);

                if (semicolon < 0 || semicolon - index > 12)
                {
                    builder.append(character);
                    continue;
                }

                String entity = text.substring(index + 1, semicolon);
                String replacement = null;

                if (entity.startsWith("#"))
                {
                    try
                    {
                        int codePoint = entity.length() > 1 && (entity.charAt(1) == 'x' || entity.charAt(1) == 'X')
                                            ? Integer.parseInt(entity.substring(2), 16)
                                            : Integer.parseInt(entity.substring(1));

                        replacement = new String(Character.toChars(codePoint));
                    }
                    catch (IllegalArgumentException exception)
                    {
                        replacement = null;
                    }
                }
                else
                {
                    replacement = ENTITIES.get(entity);
                    if (replacement == null) replacement = ENTITIES.get(entity.toLowerCase(Locale.ROOT));
                }

                if (replacement == null)
                {
                    builder.append(character);
                    continue;
                }

                builder.append(replacement);
                index = semicolon;
            }

            return builder.toString();
        }

        private void handleStartTag(String tag, HashMap<String, String> attributes)
        {
            SpannableStringBuilder text = mSpannableStringBuilder;

            if (tag.equals("br"))
            {
                return;
            }

            if (tag.equals("p"))
            {
                startBlockElement(text, attributes, getMarginParagraph());
                startCssStyle(text, attributes);
            }
            else if (tag.equals("ul"))
            {
                startBlockElement(text, attributes, getMarginList());
            }
            else if (tag.equals("li"))
            {
                startBlockElement(text, attributes, getMarginListItem());
                start(text, new Bullet());
                startCssStyle(text, attributes);
            }
            else if (tag.equals("div"))
            {
                startBlockElement(text, attributes, getMarginDiv());
            }
            else if (tag.equals("span"))
            {
                startCssStyle(text, attributes);
            }
            else if (tag.equals("strong") || tag.equals("b"))
            {
                start(text, new Bold());
            }
            else if (tag.equals("em") || tag.equals("cite") || tag.equals("dfn") || tag.equals("i"))
            {
                start(text, new Italic());
            }
            else if (tag.equals("big"))
            {
                start(text, new Big());
            }
            else if (tag.equals("small"))
            {
                start(text, new Small());
            }
            else if (tag.equals("font"))
            {
                start(text, new Font(attributes.get("color"), attributes.get("face")));
            }
            else if (tag.equals("blockquote"))
            {
                startBlockElement(text, attributes, getMarginBlockquote());
                start(text, new Blockquote());
            }
            else if (tag.equals("tt"))
            {
                start(text, new Monospace());
            }
            else if (tag.equals("a"))
            {
                start(text, new Href(attributes.get("href")));
            }
            else if (tag.equals("u"))
            {
                start(text, new Underline());
            }
            else if (tag.equals("del") || tag.equals("s") || tag.equals("strike"))
            {
                start(text, new Strikethrough());
            }
            else if (tag.equals("sup"))
            {
                start(text, new Super());
            }
            else if (tag.equals("sub"))
            {
                start(text, new Sub());
            }
            else if (tag.length() == 2 && tag.charAt(0) == 'h' && tag.charAt(1) >= '1' && tag.charAt(1) <= '6')
            {
                startBlockElement(text, attributes, getMarginHeading());
                start(text, new Heading(tag.charAt(1) - '1'));
            }
            else if (tag.equals("img"))
            {
                startImage(text, attributes);
            }
            else if (mTagHandler != null)
            {
                mTagHandler.handleTag(true, tag, text, mReader);
            }
        }

        private void handleEndTag(String tag)
        {
            SpannableStringBuilder text = mSpannableStringBuilder;

            if (tag.equals("br"))
            {
                text.append('\n');
            }
            else if (tag.equals("p"))
            {
                endCssStyle(text);
                endBlockElement(text);
            }
            else if (tag.equals("ul"))
            {
                endBlockElement(text);
            }
            else if (tag.equals("li"))
            {
                endCssStyle(text);
                endBlockElement(text);
                end(text, Bullet.class, new BulletSpan());
            }
            else if (tag.equals("div"))
            {
                endBlockElement(text);
            }
            else if (tag.equals("span"))
            {
                endCssStyle(text);
            }
            else if (tag.equals("strong") || tag.equals("b"))
            {
                end(text, Bold.class, new StyleSpan(Typeface.BOLD));
            }
            else if (tag.equals("em") || tag.equals("cite") || tag.equals("dfn") || tag.equals("i"))
            {
                end(text, Italic.class, new StyleSpan(Typeface.ITALIC));
            }
            else if (tag.equals("big"))
            {
                end(text, Big.class, new RelativeSizeSpan(1.25f));
            }
            else if (tag.equals("small"))
            {
                end(text, Small.class, new RelativeSizeSpan(0.8f));
            }
            else if (tag.equals("font"))
            {
                endFont(text);
            }
            else if (tag.equals("blockquote"))
            {
                endBlockElement(text);
                end(text, Blockquote.class, new QuoteSpan());
            }
            else if (tag.equals("tt"))
            {
                end(text, Monospace.class, new TypefaceSpan("monospace"));
            }
            else if (tag.equals("a"))
            {
                Href href = getLast(text, Href.class);
                if (href != null && href.mHref != null) setSpanFromMark(text, href, new URLSpan(href.mHref));
                else if (href != null) text.removeSpan(href);
            }
            else if (tag.equals("u"))
            {
                end(text, Underline.class, new UnderlineSpan());
            }
            else if (tag.equals("del") || tag.equals("s") || tag.equals("strike"))
            {
                end(text, Strikethrough.class, new StrikethroughSpan());
            }
            else if (tag.equals("sup"))
            {
                end(text, Super.class, new SuperscriptSpan());
            }
            else if (tag.equals("sub"))
            {
                end(text, Sub.class, new SubscriptSpan());
            }
            else if (tag.length() == 2 && tag.charAt(0) == 'h' && tag.charAt(1) >= '1' && tag.charAt(1) <= '6')
            {
                Heading heading = getLast(text, Heading.class);
                if (heading != null)
                {
                    setSpanFromMark(text, heading, new RelativeSizeSpan(HEADING_SIZES[heading.mLevel]), new StyleSpan(Typeface.BOLD));
                }

                endBlockElement(text);
            }
            else if (tag.equals("img"))
            {
                return;
            }
            else if (mTagHandler != null)
            {
                mTagHandler.handleTag(false, tag, text, mReader);
            }
        }

        private int getMarginParagraph()
        {
            return getMargin(FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH);
        }

        private int getMarginHeading()
        {
            return getMargin(FROM_HTML_SEPARATOR_LINE_BREAK_HEADING);
        }

        private int getMarginListItem()
        {
            return getMargin(FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM);
        }

        private int getMarginList()
        {
            return getMargin(FROM_HTML_SEPARATOR_LINE_BREAK_LIST);
        }

        private int getMarginDiv()
        {
            return getMargin(FROM_HTML_SEPARATOR_LINE_BREAK_DIV);
        }

        private int getMarginBlockquote()
        {
            return getMargin(FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE);
        }

        private int getMargin(int flag)
        {
            return (flag & mFlags) != 0 ? 1 : 2;
        }

        private static void appendNewlines(Editable text, int minimumNewlines)
        {
            int length = text.length();
            if (length == 0) return;

            int existing = 0;
            for (int index = length - 1; index >= 0 && text.charAt(index) == '\n'; index--)
            {
                existing++;
            }

            for (int count = existing; count < minimumNewlines; count++)
            {
                text.append("\n");
            }
        }

        private static void startBlockElement(Editable text, HashMap<String, String> attributes, int margin)
        {
            appendNewlines(text, margin);
            start(text, new Newline(margin));

            String style = attributes.get("style");
            if (style == null) return;

            Matcher matcher = TEXT_ALIGN_PATTERN.matcher(style);
            if (!matcher.find()) return;

            String alignment = matcher.group(1);

            if (alignment.equalsIgnoreCase("start")) start(text, new Alignment(Layout.Alignment.ALIGN_NORMAL));
            else if (alignment.equalsIgnoreCase("center")) start(text, new Alignment(Layout.Alignment.ALIGN_CENTER));
            else if (alignment.equalsIgnoreCase("end")) start(text, new Alignment(Layout.Alignment.ALIGN_OPPOSITE));
        }

        private static void endBlockElement(Editable text)
        {
            Newline newline = getLast(text, Newline.class);

            if (newline != null)
            {
                appendNewlines(text, newline.mNumNewlines);
                text.removeSpan(newline);
            }

            Alignment alignment = getLast(text, Alignment.class);
            if (alignment != null) setSpanFromMark(text, alignment, new AlignmentSpan.Standard(alignment.mAlignment));
        }

        private void startCssStyle(Editable text, HashMap<String, String> attributes)
        {
            String style = attributes.get("style");
            if (style == null) return;

            Matcher matcher = FOREGROUND_COLOR_PATTERN.matcher(style);
            if (matcher.find())
            {
                int color = getHtmlColor(matcher.group(1));
                if (color != -1) start(text, new Foreground(color | 0xFF000000));
            }

            matcher = BACKGROUND_COLOR_PATTERN.matcher(style);
            if (matcher.find())
            {
                int color = getHtmlColor(matcher.group(1));
                if (color != -1) start(text, new Background(color | 0xFF000000));
            }

            matcher = TEXT_DECORATION_PATTERN.matcher(style);
            if (matcher.find())
            {
                String decoration = matcher.group(1);
                if (decoration.equalsIgnoreCase("line-through")) start(text, new Strikethrough());
            }
        }

        private static void endCssStyle(Editable text)
        {
            Strikethrough strikethrough = getLast(text, Strikethrough.class);
            if (strikethrough != null) setSpanFromMark(text, strikethrough, new StrikethroughSpan());

            Background background = getLast(text, Background.class);
            if (background != null) setSpanFromMark(text, background, new BackgroundColorSpan(background.mBackgroundColor));

            Foreground foreground = getLast(text, Foreground.class);
            if (foreground != null) setSpanFromMark(text, foreground, new ForegroundColorSpan(foreground.mForegroundColor));
        }

        private void startImage(Editable text, HashMap<String, String> attributes)
        {
            String source = attributes.get("src");
            Drawable drawable = null;

            if (mImageGetter != null) drawable = mImageGetter.getDrawable(source);

            if (drawable == null)
            {
                drawable = new android.graphics.drawable.ColorDrawable(0);
                drawable.setBounds(0, 0, 0, 0);
            }

            int length = text.length();
            text.append("￼");
            text.setSpan(new ImageSpan(drawable, source), length, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private void endFont(Editable text)
        {
            Font font = getLast(text, Font.class);
            if (font == null) return;

            ArrayList<Object> spans = new ArrayList<>();

            if (font.mFace != null && !font.mFace.isEmpty()) spans.add(new TypefaceSpan(font.mFace));

            if (font.mColor != null && !font.mColor.isEmpty())
            {
                int color = getHtmlColor(font.mColor);
                if (color != -1) spans.add(new ForegroundColorSpan(color | 0xFF000000));
            }

            setSpanFromMark(text, font, spans.toArray());
        }

        private int getHtmlColor(String color)
        {
            if (color == null) return -1;

            String value = color.trim();
            if (value.endsWith(";")) value = value.substring(0, value.length() - 1);

            Integer named = COLORS.get(value.toLowerCase(Locale.ROOT));
            if (named != null) return named;

            try
            {
                if (value.startsWith("#"))
                {
                    String hex = value.substring(1);
                    if (hex.length() == 3)
                    {
                        hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
                    }

                    return (int)(Long.parseLong(hex, 16) & 0xFFFFFF);
                }

                if (value.startsWith("0x") || value.startsWith("0X")) return (int)(Long.parseLong(value.substring(2), 16) & 0xFFFFFF);

                if ((mFlags & FROM_HTML_OPTION_USE_CSS_COLORS) == 0) return (int)(Long.parseLong(value) & 0xFFFFFF);
            }
            catch (NumberFormatException exception)
            {
                return -1;
            }

            return -1;
        }

        private static <T> T getLast(Spanned text, Class<T> kind)
        {
            T[] objects = text.getSpans(0, text.length(), kind);
            if (objects.length == 0) return null;

            return objects[objects.length - 1];
        }

        private static void setSpanFromMark(Spannable text, Object mark, Object... spans)
        {
            int where = text.getSpanStart(mark);
            text.removeSpan(mark);

            int length = text.length();
            if (where == length || where < 0) return;

            for (Object span : spans)
            {
                text.setSpan(span, where, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        private static void start(Editable text, Object mark)
        {
            int length = text.length();
            text.setSpan(mark, length, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        private static <T> void end(Editable text, Class<T> kind, Object replacement)
        {
            T object = getLast(text, kind);
            if (object != null) setSpanFromMark(text, object, replacement);
        }

        private static final class Bold
        {
        }

        private static final class Italic
        {
        }

        private static final class Underline
        {
        }

        private static final class Strikethrough
        {
        }

        private static final class Big
        {
        }

        private static final class Small
        {
        }

        private static final class Monospace
        {
        }

        private static final class Blockquote
        {
        }

        private static final class Super
        {
        }

        private static final class Sub
        {
        }

        private static final class Bullet
        {
        }

        private static final class Font
        {
            final String mColor;
            final String mFace;

            Font(String color, String face)
            {
                mColor = color;
                mFace = face;
            }
        }

        private static final class Href
        {
            final String mHref;

            Href(String href)
            {
                mHref = href;
            }
        }

        private static final class Foreground
        {
            final int mForegroundColor;

            Foreground(int foregroundColor)
            {
                mForegroundColor = foregroundColor;
            }
        }

        private static final class Background
        {
            final int mBackgroundColor;

            Background(int backgroundColor)
            {
                mBackgroundColor = backgroundColor;
            }
        }

        private static final class Heading
        {
            final int mLevel;

            Heading(int level)
            {
                mLevel = level;
            }
        }

        private static final class Newline
        {
            final int mNumNewlines;

            Newline(int numNewlines)
            {
                mNumNewlines = numNewlines;
            }
        }

        private static final class Alignment
        {
            final Layout.Alignment mAlignment;

            Alignment(Layout.Alignment alignment)
            {
                mAlignment = alignment;
            }
        }
    }

    private static final class NullXmlReader implements org.xml.sax.XMLReader
    {
        private org.xml.sax.ContentHandler mContentHandler;
        private org.xml.sax.DTDHandler mDtdHandler;
        private org.xml.sax.EntityResolver mEntityResolver;
        private org.xml.sax.ErrorHandler mErrorHandler;

        @Override
        public boolean getFeature(String name)
        {
            return false;
        }

        @Override
        public void setFeature(String name, boolean value)
        {
        }

        @Override
        public Object getProperty(String name)
        {
            return null;
        }

        @Override
        public void setProperty(String name, Object value)
        {
        }

        @Override
        public void setEntityResolver(org.xml.sax.EntityResolver resolver)
        {
            mEntityResolver = resolver;
        }

        @Override
        public org.xml.sax.EntityResolver getEntityResolver()
        {
            return mEntityResolver;
        }

        @Override
        public void setDTDHandler(org.xml.sax.DTDHandler handler)
        {
            mDtdHandler = handler;
        }

        @Override
        public org.xml.sax.DTDHandler getDTDHandler()
        {
            return mDtdHandler;
        }

        @Override
        public void setContentHandler(org.xml.sax.ContentHandler handler)
        {
            mContentHandler = handler;
        }

        @Override
        public org.xml.sax.ContentHandler getContentHandler()
        {
            return mContentHandler;
        }

        @Override
        public void setErrorHandler(org.xml.sax.ErrorHandler handler)
        {
            mErrorHandler = handler;
        }

        @Override
        public org.xml.sax.ErrorHandler getErrorHandler()
        {
            return mErrorHandler;
        }

        @Override
        public void parse(org.xml.sax.InputSource input)
        {
        }

        @Override
        public void parse(String systemId)
        {
        }
    }
}
