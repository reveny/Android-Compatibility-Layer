package android.text.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Linkify
{
    public static final int WEB_URLS = 0x01;
    public static final int EMAIL_ADDRESSES = 0x02;
    public static final int PHONE_NUMBERS = 0x04;
    public static final int MAP_ADDRESSES = 0x08;
    public static final int ALL = WEB_URLS | EMAIL_ADDRESSES | PHONE_NUMBERS | MAP_ADDRESSES;

    private static final int PHONE_NUMBER_MINIMUM_DIGITS = 5;

    private static final Function<String, URLSpan> DEFAULT_SPAN_FACTORY = URLSpan::new;

    public static final MatchFilter sUrlMatchFilter = new MatchFilter()
    {
        @Override
        public final boolean acceptMatch(CharSequence text, int start, int end)
        {
            if (start == 0) return true;

            return text.charAt(start - 1) != '@';
        }
    };

    public static final MatchFilter sPhoneNumberMatchFilter = new MatchFilter()
    {
        @Override
        public final boolean acceptMatch(CharSequence text, int start, int end)
        {
            int digitCount = 0;

            for (int index = start; index < end; index++)
            {
                if (Character.isDigit(text.charAt(index)))
                {
                    digitCount++;
                    if (digitCount >= PHONE_NUMBER_MINIMUM_DIGITS) return true;
                }
            }

            return false;
        }
    };

    public static final TransformFilter sPhoneNumberTransformFilter = new TransformFilter()
    {
        @Override
        public final String transformUrl(Matcher match, String url)
        {
            return Patterns.digitsAndPlusOnly(match);
        }
    };

    public interface MatchFilter
    {
        boolean acceptMatch(CharSequence text, int start, int end);
    }

    public interface TransformFilter
    {
        String transformUrl(Matcher match, String url);
    }

    public Linkify()
    {
    }

    public static final boolean addLinks(Spannable text, int mask)
    {
        return addLinks(text, mask, null);
    }

    public static final boolean addLinks(Spannable text, int mask, Function<String, URLSpan> urlSpanFactory)
    {
        if (text == null || mask == 0) return false;

        URLSpan[] old = text.getSpans(0, text.length(), URLSpan.class);
        for (int index = old.length - 1; index >= 0; index--)
        {
            text.removeSpan(old[index]);
        }

        ArrayList<LinkSpec> links = new ArrayList<>();

        if ((mask & WEB_URLS) != 0)
        {
            gatherLinks(links, text, Patterns.AUTOLINK_WEB_URL, new String[] {"http://", "https://", "rtsp://", "ftp://"},
                        sUrlMatchFilter, null);
        }

        if ((mask & EMAIL_ADDRESSES) != 0)
        {
            gatherLinks(links, text, Patterns.AUTOLINK_EMAIL_ADDRESS, new String[] {"mailto:"}, null, null);
        }

        if ((mask & PHONE_NUMBERS) != 0)
        {
            gatherLinks(links, text, Patterns.PHONE, new String[] {"tel:"}, sPhoneNumberMatchFilter, sPhoneNumberTransformFilter);
        }

        pruneOverlaps(links);

        if (links.isEmpty()) return false;

        for (LinkSpec link : links)
        {
            applyLink(link.url, link.start, link.end, text, urlSpanFactory);
        }

        return true;
    }

    public static final boolean addLinks(TextView text, int mask)
    {
        if (text == null || mask == 0) return false;

        CharSequence content = text.getText();

        if (content instanceof Spannable)
        {
            if (!addLinks((Spannable)content, mask)) return false;

            addLinkMovementMethod(text);
            return true;
        }

        SpannableString spannable = SpannableString.valueOf(content);
        if (!addLinks(spannable, mask)) return false;

        addLinkMovementMethod(text);
        text.setText(spannable);
        return true;
    }

    private static void addLinkMovementMethod(TextView text)
    {
        MovementMethod method = text.getMovementMethod();

        if (method instanceof LinkMovementMethod) return;
        if (text.getLinksClickable()) text.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static final void addLinks(TextView text, Pattern pattern, String scheme)
    {
        addLinks(text, pattern, scheme, null, null, null);
    }

    public static final void addLinks(TextView text, Pattern pattern, String scheme, MatchFilter matchFilter,
                                      TransformFilter transformFilter)
    {
        addLinks(text, pattern, scheme, null, matchFilter, transformFilter);
    }

    public static final void addLinks(TextView text, Pattern pattern, String defaultScheme, String[] schemes, MatchFilter matchFilter,
                                      TransformFilter transformFilter)
    {
        SpannableString spannable = SpannableString.valueOf(text.getText());

        if (addLinks(spannable, pattern, defaultScheme, schemes, matchFilter, transformFilter))
        {
            text.setText(spannable);
            addLinkMovementMethod(text);
        }
    }

    public static final boolean addLinks(Spannable text, Pattern pattern, String scheme)
    {
        return addLinks(text, pattern, scheme, null, null, null);
    }

    public static final boolean addLinks(Spannable spannable, Pattern pattern, String scheme, MatchFilter matchFilter,
                                         TransformFilter transformFilter)
    {
        return addLinks(spannable, pattern, scheme, null, matchFilter, transformFilter);
    }

    public static final boolean addLinks(Spannable spannable, Pattern pattern, String defaultScheme, String[] schemes,
                                         MatchFilter matchFilter, TransformFilter transformFilter)
    {
        return addLinks(spannable, pattern, defaultScheme, schemes, matchFilter, transformFilter, null);
    }

    public static final boolean addLinks(Spannable spannable, Pattern pattern, String defaultScheme, String[] schemes,
                                         MatchFilter matchFilter, TransformFilter transformFilter,
                                         Function<String, URLSpan> urlSpanFactory)
    {
        if (spannable == null || pattern == null) return false;

        if (defaultScheme == null) defaultScheme = "";
        if (schemes == null || schemes.length < 1) schemes = new String[0];

        String[] schemesCopy = new String[schemes.length + 1];
        schemesCopy[0] = defaultScheme.toLowerCase(Locale.ROOT);

        for (int index = 0; index < schemes.length; index++)
        {
            String scheme = schemes[index];
            schemesCopy[index + 1] = scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
        }

        boolean hasMatches = false;
        Matcher matcher = pattern.matcher(spannable);

        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();

            if (matchFilter != null && !matchFilter.acceptMatch(spannable, start, end)) continue;

            String url = makeUrl(matcher.group(0), schemesCopy, matcher, transformFilter);
            applyLink(url, start, end, spannable, urlSpanFactory);
            hasMatches = true;
        }

        return hasMatches;
    }

    private static void applyLink(String url, int start, int end, Spannable text, Function<String, URLSpan> urlSpanFactory)
    {
        if (urlSpanFactory == null) urlSpanFactory = DEFAULT_SPAN_FACTORY;

        URLSpan span = urlSpanFactory.apply(url);
        text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static String makeUrl(String url, String[] prefixes, Matcher matcher, TransformFilter filter)
    {
        if (filter != null) url = filter.transformUrl(matcher, url);

        boolean hasPrefix = false;

        for (String prefix : prefixes)
        {
            if (url.regionMatches(true, 0, prefix, 0, prefix.length()))
            {
                hasPrefix = true;

                if (!url.regionMatches(false, 0, prefix, 0, prefix.length())) url = prefix + url.substring(prefix.length());
                break;
            }
        }

        if (!hasPrefix && prefixes.length > 0) url = prefixes[0] + url;

        return url;
    }

    private static void gatherLinks(ArrayList<LinkSpec> links, Spannable text, Pattern pattern, String[] schemes,
                                    MatchFilter matchFilter, TransformFilter transformFilter)
    {
        Matcher matcher = pattern.matcher(text);

        while (matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();

            if (matchFilter != null && !matchFilter.acceptMatch(text, start, end)) continue;

            LinkSpec spec = new LinkSpec();
            spec.url = makeUrl(matcher.group(0), schemes, matcher, transformFilter);
            spec.start = start;
            spec.end = end;
            links.add(spec);
        }
    }

    private static void pruneOverlaps(ArrayList<LinkSpec> links)
    {
        Comparator<LinkSpec> comparator = new Comparator<LinkSpec>()
        {
            @Override
            public int compare(LinkSpec first, LinkSpec second)
            {
                if (first.start < second.start) return -1;
                if (first.start > second.start) return 1;
                if (first.end < second.end) return 1;
                if (first.end > second.end) return -1;

                return 0;
            }
        };

        Collections.sort(links, comparator);

        int length = links.size();
        int index = 0;

        while (index < length - 1)
        {
            LinkSpec first = links.get(index);
            LinkSpec second = links.get(index + 1);
            int remove = -1;

            if (first.start <= second.start && first.end > second.start)
            {
                if (second.end <= first.end) remove = index + 1;
                else if (first.end - first.start > second.end - second.start) remove = index + 1;
                else if (first.end - first.start < second.end - second.start) remove = index;

                if (remove != -1)
                {
                    links.remove(remove);
                    length--;
                    continue;
                }
            }

            index++;
        }
    }

    private static final class LinkSpec
    {
        String url;
        int start;
        int end;
    }
}
