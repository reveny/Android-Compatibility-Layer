package android.text.method;

import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;

public class DigitsKeyListener extends NumberKeyListener
{
    private static final int SIGN = 1;
    private static final int DECIMAL = 2;

    private static final char[][] COMPATIBILITY_CHARACTERS = {
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'},
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+'},
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'},
        {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+', '.'},
    };

    private static final DigitsKeyListener[] sInstance = new DigitsKeyListener[4];
    private static final HashMap<String, DigitsKeyListener> sStringInstances = new HashMap<>();
    private static final HashMap<Locale, DigitsKeyListener[]> sLocaleInstances = new HashMap<>();

    private char[] mAccepted;
    private final boolean mSign;
    private final boolean mDecimal;
    private boolean mStringMode;
    private String mDecimalPointChars = ".";
    private String mSignChars = "-+";

    public DigitsKeyListener()
    {
        this(null, false, false);
    }

    public DigitsKeyListener(boolean sign, boolean decimal)
    {
        this(null, sign, decimal);
    }

    public DigitsKeyListener(Locale locale)
    {
        this(locale, false, false);
    }

    public DigitsKeyListener(Locale locale, boolean sign, boolean decimal)
    {
        mSign = sign;
        mDecimal = decimal;

        if (locale == null)
        {
            mAccepted = COMPATIBILITY_CHARACTERS[(sign ? SIGN : 0) | (decimal ? DECIMAL : 0)];
            return;
        }

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        StringBuilder accepted = new StringBuilder();
        char zero = symbols.getZeroDigit();

        for (int index = 0; index < 10; index++) accepted.append((char)(zero + index));

        if (zero != '0')
        {
            for (int index = 0; index < 10; index++) accepted.append((char)('0' + index));
        }

        if (sign)
        {
            char minus = symbols.getMinusSign();
            mSignChars = minus == '-' ? "-+" : "-+" + minus;
            accepted.append(mSignChars);
        }

        if (decimal)
        {
            char separator = symbols.getDecimalSeparator();
            mDecimalPointChars = separator == '.' ? "." : "." + separator;
            accepted.append(mDecimalPointChars);
        }

        mAccepted = accepted.toString().toCharArray();
    }

    private DigitsKeyListener(String accepted)
    {
        mSign = false;
        mDecimal = false;
        mStringMode = true;
        mAccepted = accepted.toCharArray();
    }

    @Override
    protected char[] getAcceptedChars()
    {
        return mAccepted;
    }

    public static DigitsKeyListener getInstance()
    {
        return getInstance(false, false);
    }

    public static DigitsKeyListener getInstance(boolean sign, boolean decimal)
    {
        int kind = (sign ? SIGN : 0) | (decimal ? DECIMAL : 0);

        synchronized (sInstance)
        {
            if (sInstance[kind] == null) sInstance[kind] = new DigitsKeyListener(sign, decimal);

            return sInstance[kind];
        }
    }

    public static DigitsKeyListener getInstance(Locale locale)
    {
        return getInstance(locale, false, false);
    }

    public static DigitsKeyListener getInstance(Locale locale, boolean sign, boolean decimal)
    {
        if (locale == null) return getInstance(sign, decimal);

        int kind = (sign ? SIGN : 0) | (decimal ? DECIMAL : 0);

        synchronized (sLocaleInstances)
        {
            DigitsKeyListener[] cached = sLocaleInstances.get(locale);

            if (cached == null)
            {
                cached = new DigitsKeyListener[4];
                sLocaleInstances.put(locale, cached);
            }

            if (cached[kind] == null) cached[kind] = new DigitsKeyListener(locale, sign, decimal);

            return cached[kind];
        }
    }

    public static DigitsKeyListener getInstance(String accepted)
    {
        synchronized (sStringInstances)
        {
            DigitsKeyListener result = sStringInstances.get(accepted);

            if (result == null)
            {
                result = new DigitsKeyListener(accepted);
                sStringInstances.put(accepted, result);
            }

            return result;
        }
    }

    @Override
    public int getInputType()
    {
        if (mStringMode)
        {
            boolean numeric = true;
            boolean hasSign = false;
            boolean hasDecimal = false;

            for (char character : mAccepted)
            {
                if (character >= '0' && character <= '9') continue;

                if (character == '-' || character == '+') hasSign = true;
                else if (character == '.') hasDecimal = true;
                else numeric = false;
            }

            if (!numeric) return InputType.TYPE_CLASS_TEXT;

            int type = InputType.TYPE_CLASS_NUMBER;
            if (hasSign) type |= InputType.TYPE_NUMBER_FLAG_SIGNED;
            if (hasDecimal) type |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
            return type;
        }

        int type = InputType.TYPE_CLASS_NUMBER;
        if (mSign) type |= InputType.TYPE_NUMBER_FLAG_SIGNED;
        if (mDecimal) type |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
        return type;
    }

    private boolean isSignChar(char character)
    {
        return mSignChars.indexOf(character) != -1;
    }

    private boolean isDecimalPointChar(char character)
    {
        return mDecimalPointChars.indexOf(character) != -1;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destinationStart, int destinationEnd)
    {
        CharSequence out = super.filter(source, start, end, dest, destinationStart, destinationEnd);

        if (!mSign && !mDecimal) return out;

        if (out != null)
        {
            source = out;
            start = 0;
            end = out.length();
        }

        int sign = -1;
        int decimal = -1;
        int destinationLength = dest.length();

        for (int index = 0; index < destinationStart; index++)
        {
            char character = dest.charAt(index);

            if (isSignChar(character)) sign = index;
            else if (isDecimalPointChar(character)) decimal = index;
        }

        for (int index = destinationEnd; index < destinationLength; index++)
        {
            char character = dest.charAt(index);

            if (isSignChar(character)) return "";
            if (isDecimalPointChar(character)) decimal = index;
        }

        SpannableStringBuilder stripped = null;

        for (int index = end - 1; index >= start; index--)
        {
            char character = source.charAt(index);
            boolean strip = false;

            if (isSignChar(character))
            {
                if (index != start || destinationStart != 0) strip = true;
                else if (sign >= 0) strip = true;
                else sign = index;
            }
            else if (isDecimalPointChar(character))
            {
                if (decimal >= 0) strip = true;
                else decimal = index;
            }

            if (strip)
            {
                if (end == start + 1) return "";
                if (stripped == null) stripped = new SpannableStringBuilder(source, start, end);

                stripped.delete(index - start, index + 1 - start);
            }
        }

        if (stripped != null) return stripped;

        return out;
    }
}
