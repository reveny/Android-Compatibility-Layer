package android.text.format;

import android.content.Context;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils
{
    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
    public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;
    public static final long YEAR_IN_MILLIS = WEEK_IN_MILLIS * 52;

    public static final int FORMAT_SHOW_TIME = 0x00001;
    public static final int FORMAT_SHOW_WEEKDAY = 0x00002;
    public static final int FORMAT_SHOW_YEAR = 0x00004;
    public static final int FORMAT_NO_YEAR = 0x00008;
    public static final int FORMAT_SHOW_DATE = 0x00010;
    public static final int FORMAT_NO_MONTH_DAY = 0x00020;
    public static final int FORMAT_12HOUR = 0x00040;
    public static final int FORMAT_24HOUR = 0x00080;
    public static final int FORMAT_CAP_AMPM = 0x00100;
    public static final int FORMAT_NO_NOON = 0x00200;
    public static final int FORMAT_CAP_NOON = 0x00400;
    public static final int FORMAT_NO_MIDNIGHT = 0x00800;
    public static final int FORMAT_CAP_MIDNIGHT = 0x01000;
    public static final int FORMAT_UTC = 0x02000;
    public static final int FORMAT_ABBREV_TIME = 0x04000;
    public static final int FORMAT_ABBREV_WEEKDAY = 0x08000;
    public static final int FORMAT_ABBREV_MONTH = 0x10000;
    public static final int FORMAT_NUMERIC_DATE = 0x20000;
    public static final int FORMAT_ABBREV_RELATIVE = 0x40000;
    public static final int FORMAT_ABBREV_ALL = 0x80000;
    public static final int FORMAT_CAP_NOON_MIDNIGHT = FORMAT_CAP_NOON | FORMAT_CAP_MIDNIGHT;
    public static final int FORMAT_NO_NOON_MIDNIGHT = FORMAT_NO_NOON | FORMAT_NO_MIDNIGHT;

    public static final String HOUR_MINUTE_24 = "%H:%M";
    public static final String MONTH_FORMAT = "%B";
    public static final String ABBREV_MONTH_FORMAT = "%b";
    public static final String NUMERIC_MONTH_FORMAT = "%m";
    public static final String MONTH_DAY_FORMAT = "%-d";
    public static final String YEAR_FORMAT = "%Y";
    public static final String YEAR_FORMAT_TWO_DIGITS = "%g";
    public static final String WEEKDAY_FORMAT = "%A";
    public static final String ABBREV_WEEKDAY_FORMAT = "%a";

    public static final int[] sameYearTable = null;
    public static final int[] sameMonthTable = null;

    public static final int LENGTH_LONG = 10;
    public static final int LENGTH_MEDIUM = 20;
    public static final int LENGTH_SHORT = 30;
    public static final int LENGTH_SHORTER = 40;
    public static final int LENGTH_SHORTEST = 50;

    public DateUtils()
    {
    }

    public static String getDayOfWeekString(int dayOfWeek, int abbrev)
    {
        DateFormatSymbols symbols = DateFormatSymbols.getInstance(Locale.getDefault());

        if (abbrev == LENGTH_LONG) return symbols.getWeekdays()[dayOfWeek];

        String shortName = symbols.getShortWeekdays()[dayOfWeek];
        if (abbrev == LENGTH_SHORTEST && !shortName.isEmpty()) return shortName.substring(0, 1);
        if (abbrev == LENGTH_SHORTER && shortName.length() > 2) return shortName.substring(0, 2);

        return shortName;
    }

    public static String getAMPMString(int ampm)
    {
        return DateFormatSymbols.getInstance(Locale.getDefault()).getAmPmStrings()[ampm - Calendar.AM];
    }

    public static String getMonthString(int month, int abbrev)
    {
        DateFormatSymbols symbols = DateFormatSymbols.getInstance(Locale.getDefault());

        if (abbrev == LENGTH_LONG) return symbols.getMonths()[month];

        String shortName = symbols.getShortMonths()[month];
        if (abbrev == LENGTH_SHORTEST && !shortName.isEmpty()) return shortName.substring(0, 1);

        return shortName;
    }

    public static CharSequence getRelativeTimeSpanString(long startTime)
    {
        return getRelativeTimeSpanString(startTime, System.currentTimeMillis(), MINUTE_IN_MILLIS);
    }

    public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolution)
    {
        return getRelativeTimeSpanString(time, now, minResolution, FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_ABBREV_MONTH);
    }

    public static CharSequence getRelativeTimeSpanString(long time, long now, long minResolution, int flags)
    {
        boolean abbreviate = (flags & (FORMAT_ABBREV_RELATIVE | FORMAT_ABBREV_ALL)) != 0;
        boolean past = now >= time;
        long duration = Math.abs(now - time);

        long count;
        String unit;

        if (duration < MINUTE_IN_MILLIS && minResolution < MINUTE_IN_MILLIS)
        {
            count = duration / SECOND_IN_MILLIS;
            unit = abbreviate ? "sec." : count == 1 ? "second" : "seconds";
        }
        else if (duration < HOUR_IN_MILLIS && minResolution < HOUR_IN_MILLIS)
        {
            count = duration / MINUTE_IN_MILLIS;
            unit = abbreviate ? "min." : count == 1 ? "minute" : "minutes";
        }
        else if (duration < DAY_IN_MILLIS && minResolution < DAY_IN_MILLIS)
        {
            count = duration / HOUR_IN_MILLIS;
            unit = abbreviate ? "hr." : count == 1 ? "hour" : "hours";
        }
        else if (duration < WEEK_IN_MILLIS && minResolution < WEEK_IN_MILLIS)
        {
            count = Math.abs(dayDistance(time, now));

            if (count == 0) return abbreviate ? "today" : "Today";
            if (count == 1) return past ? "Yesterday" : "Tomorrow";

            unit = "days";
        }
        else if (minResolution == WEEK_IN_MILLIS)
        {
            count = duration / WEEK_IN_MILLIS;
            unit = abbreviate ? "wk." : count == 1 ? "week" : "weeks";
        }
        else
        {
            return formatDateRange(null, time, time, flags);
        }

        if (count == 0 && !past) return "In 0 " + unit;
        if (count == 0) return "0 " + unit + " ago";

        return past ? count + " " + unit + " ago" : "In " + count + " " + unit;
    }

    private static int dayDistance(long startTime, long endTime)
    {
        return julianDay(endTime) - julianDay(startTime);
    }

    private static int julianDay(long millis)
    {
        long offset = TimeZone.getDefault().getOffset(millis);
        return (int)Math.floorDiv(millis + offset, DAY_IN_MILLIS);
    }

    public static CharSequence getRelativeDateTimeString(Context context, long time, long minResolution, long transitionResolution,
                                                         int flags)
    {
        long now = System.currentTimeMillis();
        long duration = Math.abs(now - time);

        if (transitionResolution > WEEK_IN_MILLIS) transitionResolution = WEEK_IN_MILLIS;
        else if (transitionResolution < DAY_IN_MILLIS) transitionResolution = DAY_IN_MILLIS;

        CharSequence timeClause = formatDateRange(context, time, time, FORMAT_SHOW_TIME);

        if (duration < transitionResolution)
        {
            CharSequence relative = getRelativeTimeSpanString(time, now, minResolution, flags);
            return relative + ", " + timeClause;
        }

        CharSequence dateClause = getRelativeTimeSpanString(context, time, false);
        return dateClause + ", " + timeClause;
    }

    public static String formatElapsedTime(long elapsedSeconds)
    {
        return formatElapsedTime(null, elapsedSeconds);
    }

    public static String formatElapsedTime(StringBuilder recycle, long elapsedSeconds)
    {
        long hours = 0;
        long minutes = 0;
        long seconds = 0;

        if (elapsedSeconds >= 3600)
        {
            hours = elapsedSeconds / 3600;
            elapsedSeconds -= hours * 3600;
        }

        if (elapsedSeconds >= 60)
        {
            minutes = elapsedSeconds / 60;
            elapsedSeconds -= minutes * 60;
        }

        seconds = elapsedSeconds;

        StringBuilder builder = recycle;
        if (builder == null) builder = new StringBuilder(8);
        else builder.setLength(0);

        java.util.Formatter formatter = new java.util.Formatter(builder, Locale.getDefault());

        if (hours > 0) return formatter.format("%1$d:%2$02d:%3$02d", hours, minutes, seconds).toString();

        return formatter.format("%1$02d:%2$02d", minutes, seconds).toString();
    }

    public static final CharSequence formatSameDayTime(long then, long now, int dateStyle, int timeStyle)
    {
        Calendar thenCalendar = new GregorianCalendar();
        thenCalendar.setTimeInMillis(then);
        Calendar nowCalendar = new GregorianCalendar();
        nowCalendar.setTimeInMillis(now);

        java.text.DateFormat format;

        if (thenCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR)
            && thenCalendar.get(Calendar.MONTH) == nowCalendar.get(Calendar.MONTH)
            && thenCalendar.get(Calendar.DAY_OF_MONTH) == nowCalendar.get(Calendar.DAY_OF_MONTH))
        {
            format = java.text.DateFormat.getTimeInstance(timeStyle);
        }
        else
        {
            format = java.text.DateFormat.getDateInstance(dateStyle);
        }

        return format.format(thenCalendar.getTime());
    }

    public static boolean isToday(long when)
    {
        return julianDay(when) == julianDay(System.currentTimeMillis());
    }

    public static String formatDateRange(Context context, long startMillis, long endMillis, int flags)
    {
        java.util.Formatter formatter = new java.util.Formatter(new StringBuilder(50), Locale.getDefault());
        return formatDateRange(context, formatter, startMillis, endMillis, flags).toString();
    }

    public static java.util.Formatter formatDateRange(Context context, java.util.Formatter formatter, long startMillis, long endMillis,
                                                      int flags)
    {
        return formatDateRange(context, formatter, startMillis, endMillis, flags, null);
    }

    public static java.util.Formatter formatDateRange(Context context, java.util.Formatter formatter, long startMillis, long endMillis,
                                                      int flags, String timeZone)
    {
        String range = formatRange(context, startMillis, endMillis, flags, timeZone);

        try
        {
            formatter.out().append(range);
        }
        catch (java.io.IOException exception)
        {
            throw new AssertionError(exception);
        }

        return formatter;
    }

    public static String formatDateTime(Context context, long millis, int flags)
    {
        return formatDateRange(context, millis, millis, flags);
    }

    public static CharSequence getRelativeTimeSpanString(Context context, long millis, boolean withPreposition)
    {
        long now = System.currentTimeMillis();
        long span = Math.abs(now - millis);

        Calendar thenCalendar = new GregorianCalendar();
        thenCalendar.setTimeInMillis(millis);
        Calendar nowCalendar = new GregorianCalendar();
        nowCalendar.setTimeInMillis(now);

        String result;
        String preposition;

        if (span < DAY_IN_MILLIS && julianDay(millis) == julianDay(now))
        {
            result = formatDateRange(context, millis, millis, FORMAT_SHOW_TIME);
            preposition = "at ";
        }
        else if (thenCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR))
        {
            result = formatDateRange(context, millis, millis, FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH);
            preposition = "on ";
        }
        else
        {
            result = formatDateRange(context, millis, millis, FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE);
            preposition = "on ";
        }

        return withPreposition ? preposition + result : result;
    }

    public static CharSequence getRelativeTimeSpanString(Context context, long millis)
    {
        return getRelativeTimeSpanString(context, millis, false);
    }

    private static String formatRange(Context context, long startMillis, long endMillis, int flags, String timeZoneId)
    {
        TimeZone zone;
        if ((flags & FORMAT_UTC) != 0) zone = TimeZone.getTimeZone("UTC");
        else if (timeZoneId != null) zone = TimeZone.getTimeZone(timeZoneId);
        else zone = TimeZone.getDefault();

        Calendar start = new GregorianCalendar(zone);
        start.setTimeInMillis(startMillis);

        Calendar end = new GregorianCalendar(zone);
        end.setTimeInMillis(endMillis);

        boolean showTime = (flags & FORMAT_SHOW_TIME) != 0;

        if (endMillis > startMillis && !showTime && end.get(Calendar.HOUR_OF_DAY) == 0 && end.get(Calendar.MINUTE) == 0
            && end.get(Calendar.SECOND) == 0 && end.get(Calendar.MILLISECOND) == 0)
        {
            end.add(Calendar.DAY_OF_MONTH, -1);
            if (end.before(start)) end.setTimeInMillis(startMillis);
        }

        boolean sameDay = start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
                          && start.get(Calendar.DAY_OF_YEAR) == end.get(Calendar.DAY_OF_YEAR);

        String skeleton = skeletonFor(context, flags, start, end, sameDay);
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);

        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
        format.setTimeZone(zone);

        String first = format.format(start.getTime());

        if (startMillis == endMillis) return first;

        if (sameDay && showTime)
        {
            String timePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), timeSkeleton(context, flags));
            SimpleDateFormat timeFormat = new SimpleDateFormat(timePattern, Locale.getDefault());
            timeFormat.setTimeZone(zone);

            String last = timeFormat.format(end.getTime());
            if (last.equals(timeFormat.format(start.getTime()))) return first;

            return first + " – " + last;
        }

        String last = format.format(end.getTime());
        if (last.equals(first)) return first;

        return first + " – " + last;
    }

    private static String timeSkeleton(Context context, int flags)
    {
        boolean twentyFour;
        if ((flags & FORMAT_24HOUR) != 0) twentyFour = true;
        else if ((flags & FORMAT_12HOUR) != 0) twentyFour = false;
        else twentyFour = DateFormat.is24HourFormat(context);

        return twentyFour ? "Hm" : "hm";
    }

    private static String skeletonFor(Context context, int flags, Calendar start, Calendar end, boolean sameDay)
    {
        boolean showTime = (flags & FORMAT_SHOW_TIME) != 0;
        boolean showWeekday = (flags & FORMAT_SHOW_WEEKDAY) != 0;
        boolean showYear = (flags & FORMAT_SHOW_YEAR) != 0;
        boolean noYear = (flags & FORMAT_NO_YEAR) != 0;
        boolean showDate = (flags & FORMAT_SHOW_DATE) != 0;
        boolean noMonthDay = (flags & FORMAT_NO_MONTH_DAY) != 0;
        boolean numericDate = (flags & FORMAT_NUMERIC_DATE) != 0;
        boolean abbrevAll = (flags & FORMAT_ABBREV_ALL) != 0;
        boolean abbrevMonth = abbrevAll || (flags & FORMAT_ABBREV_MONTH) != 0;
        boolean abbrevWeekday = abbrevAll || (flags & FORMAT_ABBREV_WEEKDAY) != 0;

        if (!showTime && !showWeekday && !showDate && !showYear) showDate = true;
        if (showTime && !sameDay) showDate = true;

        if (showDate && !showYear && !noYear)
        {
            int currentYear = new GregorianCalendar(start.getTimeZone()).get(Calendar.YEAR);
            if (start.get(Calendar.YEAR) != currentYear || end.get(Calendar.YEAR) != currentYear) showYear = true;
        }

        StringBuilder skeleton = new StringBuilder();

        if (showDate || showYear)
        {
            if (numericDate)
            {
                if (showDate) skeleton.append(noMonthDay ? "M" : "Md");
                if (showYear && !noYear) skeleton.append('y');
            }
            else
            {
                if (showDate) skeleton.append(abbrevMonth ? "MMM" : "MMMM");
                if (showDate && !noMonthDay) skeleton.append('d');
                if (showYear && !noYear) skeleton.append('y');
            }
        }

        if (showWeekday) skeleton.append(abbrevWeekday ? "EEE" : "EEEE");
        if (showTime) skeleton.append(timeSkeleton(context, flags));

        return skeleton.toString();
    }
}
