package android.text.format;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateFormat
{
    private static final char QUOTE = '\'';

    public DateFormat()
    {
    }

    public static boolean is24HourFormat(Context context)
    {
        String value = null;

        try
        {
            if (context != null) value = android.provider.Settings.System.getString(context.getContentResolver(),
                                                                                  android.provider.Settings.System.TIME_12_24);
        }
        catch (RuntimeException exception)
        {
            value = null;
        }

        if (value != null) return "24".equals(value);

        return is24HourLocale(Locale.getDefault());
    }

    private static boolean is24HourLocale(Locale locale)
    {
        java.text.DateFormat format = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale);
        if (!(format instanceof SimpleDateFormat)) return false;

        String pattern = ((SimpleDateFormat)format).toPattern();
        return pattern.indexOf('H') >= 0 || pattern.indexOf('k') >= 0;
    }

    public static String getBestDateTimePattern(Locale locale, String skeleton)
    {
        if (locale == null) locale = Locale.getDefault();
        if (skeleton == null) throw new NullPointerException("skeleton == null");

        String pattern = icuBestPattern(locale, skeleton);
        if (pattern != null) return pattern;

        return fallbackBestPattern(locale, skeleton);
    }

    private static String icuBestPattern(Locale locale, String skeleton)
    {
        try
        {
            Class<?> generatorClass = Class.forName("android.icu.text.DateTimePatternGenerator");
            Object generator = generatorClass.getMethod("getInstance", Locale.class).invoke(null, locale);
            Object pattern = generatorClass.getMethod("getBestPattern", String.class).invoke(generator, skeleton);
            return pattern instanceof String ? (String)pattern : null;
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception)
        {
            return null;
        }
    }

    private static int countOf(String skeleton, String letters)
    {
        int count = 0;

        for (int index = 0; index < skeleton.length(); index++)
        {
            if (letters.indexOf(skeleton.charAt(index)) >= 0) count++;
        }

        return count;
    }

    private static String repeat(char character, int count)
    {
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) builder.append(character);
        return builder.toString();
    }

    private static String fallbackBestPattern(Locale locale, String skeleton)
    {
        int years = countOf(skeleton, "yY");
        int months = countOf(skeleton, "ML");
        int days = countOf(skeleton, "d");
        int weekdays = countOf(skeleton, "Ec");
        int hours12 = countOf(skeleton, "hK");
        int hours24 = countOf(skeleton, "Hk");
        int hoursLocale = countOf(skeleton, "j");
        int minutes = countOf(skeleton, "m");
        int seconds = countOf(skeleton, "s");
        int zones = countOf(skeleton, "zZv");

        StringBuilder date = new StringBuilder();
        boolean textualMonth = months >= 3;
        char[] order = dateOrder(locale);

        for (char field : order)
        {
            String part = null;

            if (field == 'y' && years > 0) part = years == 2 ? "yy" : "y";
            else if (field == 'M' && months > 0) part = repeat('M', Math.min(months, 4));
            else if (field == 'd' && days > 0) part = days >= 2 ? "dd" : "d";

            if (part == null) continue;

            if (date.length() > 0)
            {
                if (textualMonth) date.append(field == 'y' && order[0] == 'M' ? ", " : " ");
                else date.append(dateSeparator(locale));
            }

            date.append(part);
        }

        if (weekdays > 0)
        {
            String weekday = repeat('E', Math.min(weekdays, 4));
            date.insert(0, date.length() > 0 ? weekday + ", " : weekday);
        }

        StringBuilder time = new StringBuilder();
        boolean twelveHour = hours12 > 0 || (hoursLocale > 0 && !is24HourLocale(locale));

        if (hours12 > 0 || hours24 > 0 || hoursLocale > 0)
        {
            int hourCount = Math.max(hours12, Math.max(hours24, hoursLocale));
            time.append(twelveHour ? repeat('h', Math.min(hourCount, 2)) : repeat('H', Math.min(hourCount, 2)));
        }

        if (minutes > 0)
        {
            if (time.length() > 0) time.append(':');
            time.append("mm");
        }

        if (seconds > 0)
        {
            if (time.length() > 0) time.append(':');
            time.append("ss");
        }

        if (twelveHour && time.length() > 0) time.append(" a");
        if (zones > 0 && time.length() > 0) time.append(" z");

        if (date.length() == 0) return time.toString();
        if (time.length() == 0) return date.toString();

        return date + ", " + time;
    }

    private static String dateSeparator(Locale locale)
    {
        java.text.DateFormat format = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale);
        if (!(format instanceof SimpleDateFormat)) return "/";

        String pattern = ((SimpleDateFormat)format).toPattern();

        for (int index = 0; index < pattern.length(); index++)
        {
            char character = pattern.charAt(index);
            if (!Character.isLetter(character) && character != QUOTE) return String.valueOf(character);
        }

        return "/";
    }

    private static char[] dateOrder(Locale locale)
    {
        java.text.DateFormat format = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale);
        String pattern = format instanceof SimpleDateFormat ? ((SimpleDateFormat)format).toPattern() : "M/d/y";

        char[] result = new char[3];
        int found = 0;
        boolean quoted = false;

        for (int index = 0; index < pattern.length() && found < 3; index++)
        {
            char character = pattern.charAt(index);

            if (character == QUOTE)
            {
                quoted = !quoted;
                continue;
            }

            if (quoted) continue;

            if (character == 'L') character = 'M';
            if (character != 'd' && character != 'M' && character != 'y') continue;

            boolean seen = false;
            for (int other = 0; other < found; other++)
            {
                if (result[other] == character) seen = true;
            }

            if (!seen) result[found++] = character;
        }

        String defaults = "Mdy";
        for (int index = 0; index < defaults.length() && found < 3; index++)
        {
            char character = defaults.charAt(index);
            boolean seen = false;
            for (int other = 0; other < found; other++)
            {
                if (result[other] == character) seen = true;
            }

            if (!seen) result[found++] = character;
        }

        return result;
    }

    public static java.text.DateFormat getTimeFormat(Context context)
    {
        return new SimpleDateFormat(is24HourFormat(context) ? "HH:mm" : "h:mm a", Locale.getDefault());
    }

    public static java.text.DateFormat getDateFormat(Context context)
    {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.getDefault());
    }

    public static java.text.DateFormat getLongDateFormat(Context context)
    {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG, Locale.getDefault());
    }

    public static java.text.DateFormat getMediumDateFormat(Context context)
    {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault());
    }

    public static char[] getDateFormatOrder(Context context)
    {
        return dateOrder(Locale.getDefault());
    }

    public static CharSequence format(CharSequence inFormat, long inTimeInMillis)
    {
        return format(inFormat, new Date(inTimeInMillis));
    }

    public static CharSequence format(CharSequence inFormat, Date inDate)
    {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(inDate);
        return format(inFormat, calendar);
    }

    public static CharSequence format(CharSequence inFormat, Calendar inDate)
    {
        SpannableStringBuilder builder = new SpannableStringBuilder(inFormat);
        DateFormatSymbols symbols = DateFormatSymbols.getInstance(Locale.getDefault());
        int length = builder.length();
        int count;

        for (int index = 0; index < length; index += count)
        {
            count = 1;
            char character = builder.charAt(index);

            if (character == QUOTE)
            {
                count = appendQuotedText(builder, index);
                length = builder.length();
                continue;
            }

            while (index + count < length && builder.charAt(index + count) == character) count++;

            String replacement;

            switch (character)
            {
                case 'A':
                case 'a':
                    replacement = symbols.getAmPmStrings()[inDate.get(Calendar.AM_PM) - Calendar.AM];
                    break;
                case 'd':
                    replacement = zeroPad(inDate.get(Calendar.DATE), count);
                    break;
                case 'c':
                case 'E':
                    replacement = getDayOfWeekString(symbols, inDate.get(Calendar.DAY_OF_WEEK), count);
                    break;
                case 'K':
                case 'h':
                {
                    int hour = inDate.get(Calendar.HOUR);
                    if (character == 'h' && hour == 0) hour = 12;
                    replacement = zeroPad(hour, count);
                    break;
                }
                case 'H':
                case 'k':
                {
                    int hour = inDate.get(Calendar.HOUR_OF_DAY);
                    if (character == 'k' && hour == 0) hour = 24;
                    replacement = zeroPad(hour, count);
                    break;
                }
                case 'L':
                case 'M':
                    replacement = getMonthString(symbols, inDate.get(Calendar.MONTH), count);
                    break;
                case 'm':
                    replacement = zeroPad(inDate.get(Calendar.MINUTE), count);
                    break;
                case 's':
                    replacement = zeroPad(inDate.get(Calendar.SECOND), count);
                    break;
                case 'y':
                    replacement = getYearString(inDate.get(Calendar.YEAR), count);
                    break;
                case 'z':
                    replacement = getTimeZoneString(inDate, count);
                    break;
                default:
                    replacement = null;
                    break;
            }

            if (replacement != null)
            {
                builder.replace(index, index + count, replacement);
                count = replacement.length();
                length = builder.length();
            }
        }

        if (inFormat instanceof Spanned) return builder;

        return builder.toString();
    }

    private static String getDayOfWeekString(DateFormatSymbols symbols, int day, int count)
    {
        if (count == 4) return symbols.getWeekdays()[day];

        String shortName = symbols.getShortWeekdays()[day];
        if (count == 5) return shortName.isEmpty() ? shortName : shortName.substring(0, 1);

        return shortName;
    }

    private static String getMonthString(DateFormatSymbols symbols, int month, int count)
    {
        if (count == 4) return symbols.getMonths()[month];

        if (count == 5)
        {
            String name = symbols.getShortMonths()[month];
            return name.isEmpty() ? name : name.substring(0, 1);
        }

        if (count == 3) return symbols.getShortMonths()[month];

        return zeroPad(month + 1, count);
    }

    private static String getTimeZoneString(Calendar inDate, int count)
    {
        TimeZone zone = inDate.getTimeZone();

        if (count < 2) return formatZoneOffset(inDate.get(Calendar.DST_OFFSET) + inDate.get(Calendar.ZONE_OFFSET));

        boolean daylight = inDate.get(Calendar.DST_OFFSET) != 0;
        return zone.getDisplayName(daylight, TimeZone.SHORT);
    }

    private static String formatZoneOffset(int offset)
    {
        offset /= 1000;

        StringBuilder builder = new StringBuilder();

        if (offset < 0)
        {
            builder.insert(0, "-");
            offset = -offset;
        }
        else
        {
            builder.insert(0, "+");
        }

        int hours = offset / 3600;
        int minutes = (offset % 3600) / 60;

        builder.append(zeroPad(hours, 2));
        builder.append(zeroPad(minutes, 2));
        return builder.toString();
    }

    private static String getYearString(int year, int count)
    {
        return count <= 2 ? zeroPad(year % 100, 2) : String.format(Locale.getDefault(), "%d", year);
    }

    private static int appendQuotedText(SpannableStringBuilder builder, int index)
    {
        int length = builder.length();

        if (index + 1 < length && builder.charAt(index + 1) == QUOTE)
        {
            builder.delete(index, index + 1);
            return 1;
        }

        int count = 0;

        builder.delete(index, index + 1);
        length--;

        while (index < length)
        {
            char character = builder.charAt(index);

            if (character == QUOTE)
            {
                if (index + 1 < length && builder.charAt(index + 1) == QUOTE)
                {
                    builder.delete(index, index + 1);
                    length--;
                    count++;
                    index++;
                }
                else
                {
                    builder.delete(index, index + 1);
                    break;
                }
            }
            else
            {
                index++;
                count++;
            }
        }

        return count;
    }

    private static String zeroPad(int value, int minimumDigits)
    {
        return String.format(Locale.getDefault(), "%0" + minimumDigits + "d", value);
    }
}
