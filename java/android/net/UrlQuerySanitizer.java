package android.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

public class UrlQuerySanitizer
{
    public class ParameterValuePair
    {
        public String mParameter;
        public String mValue;

        public ParameterValuePair(String parameter, String value)
        {
            mParameter = parameter;
            mValue = value;
        }
    }

    public interface ValueSanitizer
    {
        String sanitize(String value);
    }

    public static class IllegalCharacterValueSanitizer implements ValueSanitizer
    {
        public static final int SPACE_OK = 1 << 0;
        public static final int OTHER_WHITESPACE_OK = 1 << 1;
        public static final int NON_7_BIT_ASCII_OK = 1 << 2;
        public static final int DQUOTE_OK = 1 << 3;
        public static final int SQUOTE_OK = 1 << 4;
        public static final int LT_OK = 1 << 5;
        public static final int GT_OK = 1 << 6;
        public static final int AMP_OK = 1 << 7;
        public static final int PCT_OK = 1 << 8;
        public static final int NUL_OK = 1 << 9;
        public static final int SCRIPT_URL_OK = 1 << 10;
        public static final int ALL_OK = 0x7ff;
        public static final int ALL_WHITESPACE_OK = SPACE_OK | OTHER_WHITESPACE_OK;
        public static final int ALL_ILLEGAL = 0;
        public static final int ALL_BUT_NUL_LEGAL = ALL_OK & ~NUL_OK;
        public static final int ALL_BUT_WHITESPACE_LEGAL = ALL_OK & ~ALL_WHITESPACE_OK;
        public static final int URL_LEGAL = NON_7_BIT_ASCII_OK | DQUOTE_OK | SQUOTE_OK | AMP_OK | PCT_OK;
        public static final int URL_AND_SPACE_LEGAL = URL_LEGAL | SPACE_OK;
        public static final int AMP_LEGAL = AMP_OK;
        public static final int AMP_AND_SPACE_LEGAL = AMP_OK | SPACE_OK;
        public static final int SPACE_LEGAL = SPACE_OK;
        public static final int ALL_BUT_NUL_AND_ANGLE_BRACKETS_LEGAL = ALL_OK & ~(NUL_OK | LT_OK | GT_OK);

        private static final String JAVASCRIPT_PREFIX = "javascript:";
        private static final String VBSCRIPT_PREFIX = "vbscript:";
        private static final int MIN_SCRIPT_PREFIX_LENGTH = Math.min(JAVASCRIPT_PREFIX.length(), VBSCRIPT_PREFIX.length());

        private final int mFlags;

        public IllegalCharacterValueSanitizer(int flags)
        {
            mFlags = flags;
        }

        @Override
        public String sanitize(String value)
        {
            if (value == null) return null;

            int length = value.length();

            if ((mFlags & SCRIPT_URL_OK) == 0 && length >= MIN_SCRIPT_PREFIX_LENGTH)
            {
                String asLower = value.toLowerCase(Locale.ROOT);
                if (asLower.startsWith(JAVASCRIPT_PREFIX) || asLower.startsWith(VBSCRIPT_PREFIX)) return "";
            }

            if ((mFlags & ALL_WHITESPACE_OK) == 0)
            {
                value = trimWhitespace(value);
                length = value.length();
            }

            StringBuilder builder = new StringBuilder(length);

            for (int index = 0; index < length; index++)
            {
                char character = value.charAt(index);
                if (!characterIsLegal(character))
                {
                    if ((mFlags & SPACE_OK) != 0) character = ' ';
                    else character = '_';
                }
                builder.append(character);
            }

            return builder.toString();
        }

        private String trimWhitespace(String value)
        {
            int start = 0;
            int last = value.length() - 1;
            int end = last;

            while (start <= end && isWhitespace(value.charAt(start)))
            {
                start++;
            }

            while (end >= start && isWhitespace(value.charAt(end)))
            {
                end--;
            }

            if (start == 0 && end == last) return value;

            return value.substring(start, end + 1);
        }

        private boolean isWhitespace(char character)
        {
            switch (character)
            {
                case ' ':
                case '\t':
                case '\f':
                case '\n':
                case '\r':
                case 11:
                    return true;
                default:
                    return false;
            }
        }

        private boolean characterIsLegal(char character)
        {
            switch (character)
            {
                case ' ':
                    return (mFlags & SPACE_OK) != 0;
                case '\t':
                case '\f':
                case '\n':
                case '\r':
                case 11:
                    return (mFlags & OTHER_WHITESPACE_OK) != 0;
                case '\"':
                    return (mFlags & DQUOTE_OK) != 0;
                case '\'':
                    return (mFlags & SQUOTE_OK) != 0;
                case '<':
                    return (mFlags & LT_OK) != 0;
                case '>':
                    return (mFlags & GT_OK) != 0;
                case '&':
                    return (mFlags & AMP_OK) != 0;
                case '%':
                    return (mFlags & PCT_OK) != 0;
                case '\0':
                    return (mFlags & NUL_OK) != 0;
                default:
                    return (character >= 32 && character < 127) || ((character >= 128) && ((mFlags & NON_7_BIT_ASCII_OK) != 0));
            }
        }
    }

    private static final ValueSanitizer ALL_ILLEGAL = new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.ALL_ILLEGAL);
    private static final ValueSanitizer ALL_BUT_NUL_LEGAL =
        new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.ALL_BUT_NUL_LEGAL);
    private static final ValueSanitizer ALL_BUT_WHITESPACE_LEGAL =
        new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.ALL_BUT_WHITESPACE_LEGAL);
    private static final ValueSanitizer URL_LEGAL = new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.URL_LEGAL);
    private static final ValueSanitizer URL_AND_SPACE_LEGAL =
        new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.URL_AND_SPACE_LEGAL);
    private static final ValueSanitizer AMP_LEGAL = new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.AMP_LEGAL);
    private static final ValueSanitizer AMP_AND_SPACE_LEGAL =
        new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.AMP_AND_SPACE_LEGAL);
    private static final ValueSanitizer SPACE_LEGAL = new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.SPACE_LEGAL);
    private static final ValueSanitizer ALL_BUT_NUL_AND_ANGLE_BRACKETS_LEGAL =
        new IllegalCharacterValueSanitizer(IllegalCharacterValueSanitizer.ALL_BUT_NUL_AND_ANGLE_BRACKETS_LEGAL);

    private final HashMap<String, ValueSanitizer> mSanitizers = new HashMap<>();
    private final HashMap<String, String> mEntries = new HashMap<>();
    private final ArrayList<ParameterValuePair> mEntriesList = new ArrayList<>();
    private boolean mAllowUnregisteredParamaters;
    private boolean mPreferFirstRepeatedParameter;
    private ValueSanitizer mUnregisteredParameterValueSanitizer = getAllIllegal();

    public UrlQuerySanitizer()
    {
    }

    public UrlQuerySanitizer(String url)
    {
        setAllowUnregisteredParamaters(true);
        parseUrl(url);
    }

    public ValueSanitizer getUnregisteredParameterValueSanitizer()
    {
        return mUnregisteredParameterValueSanitizer;
    }

    public void setUnregisteredParameterValueSanitizer(ValueSanitizer sanitizer)
    {
        mUnregisteredParameterValueSanitizer = sanitizer;
    }

    public static final ValueSanitizer getAllIllegal()
    {
        return ALL_ILLEGAL;
    }

    public static final ValueSanitizer getAllButNulLegal()
    {
        return ALL_BUT_NUL_LEGAL;
    }

    public static final ValueSanitizer getAllButWhitespaceLegal()
    {
        return ALL_BUT_WHITESPACE_LEGAL;
    }

    public static final ValueSanitizer getUrlLegal()
    {
        return URL_LEGAL;
    }

    public static final ValueSanitizer getUrlAndSpaceLegal()
    {
        return URL_AND_SPACE_LEGAL;
    }

    public static final ValueSanitizer getAmpLegal()
    {
        return AMP_LEGAL;
    }

    public static final ValueSanitizer getAmpAndSpaceLegal()
    {
        return AMP_AND_SPACE_LEGAL;
    }

    public static final ValueSanitizer getSpaceLegal()
    {
        return SPACE_LEGAL;
    }

    public static final ValueSanitizer getAllButNulAndAngleBracketsLegal()
    {
        return ALL_BUT_NUL_AND_ANGLE_BRACKETS_LEGAL;
    }

    public void parseUrl(String url)
    {
        int queryIndex = url.indexOf('?');
        String query;

        if (queryIndex >= 0) query = url.substring(queryIndex + 1);
        else query = "";

        parseQuery(query);
    }

    public void parseQuery(String query)
    {
        clear();

        StringTokenizer tokenizer = new StringTokenizer(query, "&");

        while (tokenizer.hasMoreElements())
        {
            String attributeValuePair = tokenizer.nextToken();

            if (attributeValuePair.length() > 0)
            {
                int assignmentIndex = attributeValuePair.indexOf('=');

                if (assignmentIndex < 0)
                {
                    parseEntry(attributeValuePair, "");
                }
                else
                {
                    parseEntry(attributeValuePair.substring(0, assignmentIndex), attributeValuePair.substring(assignmentIndex + 1));
                }
            }
        }
    }

    public Set<String> getParameterSet()
    {
        return mEntries.keySet();
    }

    public List<ParameterValuePair> getParameterList()
    {
        return mEntriesList;
    }

    public boolean hasParameter(String parameter)
    {
        return mEntries.containsKey(parameter);
    }

    public String getValue(String parameter)
    {
        return mEntries.get(parameter);
    }

    public void registerParameter(String parameter, ValueSanitizer valueSanitizer)
    {
        if (valueSanitizer == null) mSanitizers.remove(parameter);

        mSanitizers.put(parameter, valueSanitizer);
    }

    public void registerParameters(String[] parameters, ValueSanitizer valueSanitizer)
    {
        for (String parameter : parameters)
        {
            mSanitizers.put(parameter, valueSanitizer);
        }
    }

    public void setAllowUnregisteredParamaters(boolean allowUnregisteredParamaters)
    {
        mAllowUnregisteredParamaters = allowUnregisteredParamaters;
    }

    public boolean getAllowUnregisteredParamaters()
    {
        return mAllowUnregisteredParamaters;
    }

    public void setPreferFirstRepeatedParameter(boolean preferFirstRepeatedParameter)
    {
        mPreferFirstRepeatedParameter = preferFirstRepeatedParameter;
    }

    public boolean getPreferFirstRepeatedParameter()
    {
        return mPreferFirstRepeatedParameter;
    }

    protected void parseEntry(String parameter, String value)
    {
        String unescapedParameter = unescape(parameter);
        ValueSanitizer valueSanitizer = getEffectiveValueSanitizer(unescapedParameter);

        if (valueSanitizer == null) return;

        String unescapedValue = unescape(value);
        String sanitizedValue = valueSanitizer.sanitize(unescapedValue);
        addSanitizedEntry(unescapedParameter, sanitizedValue);
    }

    protected void addSanitizedEntry(String parameter, String value)
    {
        mEntriesList.add(new ParameterValuePair(parameter, value));

        if (mPreferFirstRepeatedParameter && mEntries.containsKey(parameter)) return;

        mEntries.put(parameter, value);
    }

    public ValueSanitizer getValueSanitizer(String parameter)
    {
        return mSanitizers.get(parameter);
    }

    public ValueSanitizer getEffectiveValueSanitizer(String parameter)
    {
        ValueSanitizer sanitizer = getValueSanitizer(parameter);

        if (sanitizer == null && mAllowUnregisteredParamaters) sanitizer = getUnregisteredParameterValueSanitizer();

        return sanitizer;
    }

    public String unescape(String string)
    {
        int firstEscape = -1;

        for (int index = 0; index < string.length(); index++)
        {
            char character = string.charAt(index);
            if (character == '+' || character == '%')
            {
                firstEscape = index;
                break;
            }
        }

        if (firstEscape == -1) return string;

        int length = string.length();
        StringBuilder builder = new StringBuilder(length);
        builder.append(string, 0, firstEscape);

        for (int index = firstEscape; index < length; index++)
        {
            char character = string.charAt(index);

            if (character == '+')
            {
                character = ' ';
            }
            else if (character == '%' && index + 2 < length)
            {
                char first = string.charAt(index + 1);
                char second = string.charAt(index + 2);

                if (isHexDigit(first) && isHexDigit(second))
                {
                    character = (char)(decodeHexDigit(first) * 16 + decodeHexDigit(second));
                    index += 2;
                }
            }

            builder.append(character);
        }

        return builder.toString();
    }

    protected boolean isHexDigit(char character)
    {
        return decodeHexDigit(character) >= 0;
    }

    protected int decodeHexDigit(char character)
    {
        if (character >= '0' && character <= '9') return character - '0';
        if (character >= 'A' && character <= 'F') return character - 'A' + 10;
        if (character >= 'a' && character <= 'f') return character - 'a' + 10;

        return -1;
    }

    protected void clear()
    {
        mEntries.clear();
        mEntriesList.clear();
    }
}
