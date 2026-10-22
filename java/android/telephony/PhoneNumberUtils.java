package android.telephony;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TtsSpan;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberUtils
{
    public static final char PAUSE = ',';
    public static final char WAIT = ';';
    public static final char WILD = 'N';

    public static final int TOA_International = 0x91;
    public static final int TOA_Unknown = 0x81;

    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_NANP = 1;
    public static final int FORMAT_JAPAN = 2;

    public static final int BCD_EXTENDED_TYPE_EF_ADN = 1;
    public static final int BCD_EXTENDED_TYPE_CALLED_PARTY = 2;

    private static final int MIN_MATCH = 7;
    private static final String CLIR_ON = "*31#";
    private static final String CLIR_OFF = "#31#";

    private static final Pattern GLOBAL_PHONE_NUMBER_PATTERN = Pattern.compile("[\\+]?[0-9.-]+");
    private static final Pattern MMI_PREFIX_PATTERN = Pattern.compile("^([*#]+[0-9]*[*#])(.*)$");

    private static final Set<String> NANP_COUNTRIES = new HashSet<>(Arrays.asList("US", "CA", "AS", "AI", "AG", "BS", "BB", "BM", "VG",
        "KY", "DM", "DO", "GD", "GU", "JM", "PR", "MS", "MP", "KN", "LC", "VC", "TT", "TC", "VI"));

    private static final Set<String> DEFAULT_EMERGENCY_NUMBERS = new HashSet<>(Arrays.asList("112", "911", "000", "08", "110", "118",
        "119", "999"));

    private static final Map<String, Integer> COUNTRY_CALLING_CODES = new HashMap<>();

    private static final int NANP_STATE_DIGIT = 1;
    private static final int NANP_STATE_PLUS = 2;
    private static final int NANP_STATE_ONE = 3;
    private static final int NANP_STATE_DASH = 4;

    static
    {
        for (String country : NANP_COUNTRIES) COUNTRY_CALLING_CODES.put(country, 1);
        String[] codes = {
            "RU", "7", "KZ", "7", "EG", "20", "ZA", "27", "GR", "30", "NL", "31", "BE", "32", "FR", "33", "ES", "34", "PT", "351", "IE", "353",
            "IS", "354", "FI", "358", "BG", "359", "HU", "36", "LT", "370", "LV", "371", "EE", "372", "UA", "380", "RS", "381", "HR", "385",
            "SI", "386", "IT", "39", "RO", "40", "CH", "41", "CZ", "420", "SK", "421", "AT", "43", "GB", "44", "DK", "45", "SE", "46",
            "NO", "47", "PL", "48", "DE", "49", "PE", "51", "MX", "52", "AR", "54", "BR", "55", "CL", "56", "CO", "57", "VE", "58",
            "MY", "60", "AU", "61", "ID", "62", "PH", "63", "NZ", "64", "SG", "65", "TH", "66", "JP", "81", "KR", "82", "VN", "84",
            "CN", "86", "HK", "852", "TW", "886", "TR", "90", "IN", "91", "PK", "92", "AF", "93", "LK", "94", "IR", "98", "MA", "212",
            "DZ", "213", "TN", "216", "NG", "234", "KE", "254", "IL", "972", "AE", "971", "SA", "966", "QA", "974", "BD", "880", "LU", "352",
            "CY", "357", "MT", "356", "BY", "375", "GE", "995", "AM", "374", "AZ", "994", "UZ", "998"
        };
        for (int index = 0; index < codes.length; index += 2) COUNTRY_CALLING_CODES.put(codes[index], Integer.parseInt(codes[index + 1]));
    }

    public PhoneNumberUtils()
    {
    }

    public static boolean isISODigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    public static final boolean is12Key(char c)
    {
        return (c >= '0' && c <= '9') || c == '*' || c == '#';
    }

    public static final boolean isDialable(char c)
    {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == WILD;
    }

    public static final boolean isReallyDialable(char c)
    {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+';
    }

    public static final boolean isNonSeparator(char c)
    {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == WILD || c == WAIT || c == PAUSE;
    }

    public static final boolean isStartsPostDial(char c)
    {
        return c == PAUSE || c == WAIT;
    }

    static boolean isSeparator(char c)
    {
        return !isDialable(c) && !(('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z'));
    }

    public static String getNumberFromIntent(Intent intent, Context context)
    {
        if (intent == null) return null;
        Uri uri = intent.getData();
        if (uri == null) return null;
        String scheme = uri.getScheme();
        if ("tel".equals(scheme) || "sip".equals(scheme)) return uri.getSchemeSpecificPart();
        return null;
    }

    public static String extractNetworkPortion(String phoneNumber)
    {
        if (phoneNumber == null) return null;
        int length = phoneNumber.length();
        StringBuilder result = new StringBuilder(length);
        for (int index = 0; index < length; index++)
        {
            char c = phoneNumber.charAt(index);
            int digit = Character.digit(c, 10);
            if (digit != -1)
            {
                result.append(digit);
            }
            else if (c == '+')
            {
                String prefix = result.toString();
                if (prefix.length() == 0 || prefix.equals(CLIR_ON) || prefix.equals(CLIR_OFF)) result.append(c);
            }
            else if (isDialable(c))
            {
                result.append(c);
            }
            else if (isStartsPostDial(c))
            {
                break;
            }
        }
        return result.toString();
    }

    private static int indexOfLastNetworkChar(String phoneNumber)
    {
        int length = phoneNumber.length();
        int pauseIndex = minPositive(phoneNumber.indexOf(PAUSE), phoneNumber.indexOf(WAIT));
        return pauseIndex < 0 ? length - 1 : pauseIndex - 1;
    }

    private static int minPositive(int first, int second)
    {
        if (first >= 0 && second >= 0) return Math.min(first, second);
        if (first >= 0) return first;
        return second;
    }

    public static String extractPostDialPortion(String phoneNumber)
    {
        if (phoneNumber == null) return null;
        StringBuilder result = new StringBuilder();
        int trimIndex = indexOfLastNetworkChar(phoneNumber);
        for (int index = trimIndex + 1; index < phoneNumber.length(); index++)
        {
            char c = phoneNumber.charAt(index);
            if (isNonSeparator(c)) result.append(c);
        }
        return result.toString();
    }

    public static String stripSeparators(String phoneNumber)
    {
        if (phoneNumber == null) return null;
        int length = phoneNumber.length();
        StringBuilder result = new StringBuilder(length);
        for (int index = 0; index < length; index++)
        {
            char c = phoneNumber.charAt(index);
            int digit = Character.digit(c, 10);
            if (digit != -1)
            {
                result.append(digit);
            }
            else if (isNonSeparator(c))
            {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String convertKeypadLettersToDigits(String input)
    {
        if (input == null) return input;
        int length = input.length();
        if (length == 0) return input;
        char[] out = input.toCharArray();
        for (int index = 0; index < length; index++)
        {
            out[index] = keypadDigit(out[index]);
        }
        return new String(out);
    }

    private static char keypadDigit(char c)
    {
        char upper = Character.toUpperCase(c);
        if (upper < 'A' || upper > 'Z') return c;
        if (upper <= 'C') return '2';
        if (upper <= 'F') return '3';
        if (upper <= 'I') return '4';
        if (upper <= 'L') return '5';
        if (upper <= 'O') return '6';
        if (upper <= 'S') return '7';
        if (upper <= 'V') return '8';
        return '9';
    }

    public static String normalizeNumber(String phoneNumber)
    {
        if (TextUtils.isEmpty(phoneNumber)) return "";
        StringBuilder result = new StringBuilder();
        int length = phoneNumber.length();
        for (int index = 0; index < length; index++)
        {
            char c = phoneNumber.charAt(index);
            int digit = Character.digit(c, 10);
            if (digit != -1)
            {
                result.append(digit);
            }
            else if (result.length() == 0 && c == '+')
            {
                result.append(c);
            }
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
            {
                return normalizeNumber(convertKeypadLettersToDigits(phoneNumber));
            }
        }
        return result.toString();
    }

    public static String replaceUnicodeDigits(String number)
    {
        StringBuilder normalizedDigits = new StringBuilder(number.length());
        for (char c : number.toCharArray())
        {
            int digit = Character.digit(c, 10);
            if (digit != -1)
            {
                normalizedDigits.append(digit);
            }
            else
            {
                normalizedDigits.append(c);
            }
        }
        return normalizedDigits.toString();
    }

    @Deprecated
    public static boolean compare(String first, String second)
    {
        return compareLoosely(first, second);
    }

    public static boolean compare(Context context, String first, String second)
    {
        return compareLoosely(first, second);
    }

    private static boolean compareLoosely(String first, String second)
    {
        if (first == null || second == null) return first == second;
        if (first.length() == 0 || second.length() == 0) return false;
        String firstDigits = dialableDigits(first);
        String secondDigits = dialableDigits(second);
        if (firstDigits.length() == 0 || secondDigits.length() == 0) return false;
        int firstIndex = firstDigits.length() - 1;
        int secondIndex = secondDigits.length() - 1;
        int matched = 0;
        while (firstIndex >= 0 && secondIndex >= 0)
        {
            if (firstDigits.charAt(firstIndex) != secondDigits.charAt(secondIndex)) break;
            matched++;
            firstIndex--;
            secondIndex--;
        }
        if (matched >= MIN_MATCH) return true;
        return firstIndex < 0 && secondIndex < 0;
    }

    private static String dialableDigits(String number)
    {
        String networkPortion = extractNetworkPortion(number);
        StringBuilder result = new StringBuilder(networkPortion.length());
        for (int index = 0; index < networkPortion.length(); index++)
        {
            char c = networkPortion.charAt(index);
            if (c != '+') result.append(c);
        }
        return result.toString();
    }

    public static boolean areSamePhoneNumber(String number1, String number2, String defaultCountryIso)
    {
        if (number1 == null || number2 == null) return false;
        String e1 = formatNumberToE164(number1, defaultCountryIso);
        String e2 = formatNumberToE164(number2, defaultCountryIso);
        if (e1 != null && e2 != null) return e1.equals(e2);
        return compareLoosely(number1, number2);
    }

    public static String toCallerIDMinMatch(String phoneNumber)
    {
        String networkPortion = extractNetworkPortion(phoneNumber);
        return internalGetStrippedReversed(networkPortion, MIN_MATCH);
    }

    public static String getStrippedReversed(String phoneNumber)
    {
        String networkPortion = extractNetworkPortion(phoneNumber);
        if (networkPortion == null) return null;
        return internalGetStrippedReversed(networkPortion, networkPortion.length());
    }

    private static String internalGetStrippedReversed(String networkPortion, int length)
    {
        if (networkPortion == null) return null;
        StringBuilder result = new StringBuilder(length);
        int size = networkPortion.length();
        for (int index = size - 1, count = length; index >= 0 && count > 0; index--, count--)
        {
            result.append(networkPortion.charAt(index));
        }
        return result.toString();
    }

    public static String stringFromStringAndTOA(String phoneNumber, int toa)
    {
        if (phoneNumber == null) return null;
        if (toa == TOA_International && phoneNumber.length() > 0 && phoneNumber.charAt(0) != '+') return "+" + phoneNumber;
        return phoneNumber;
    }

    public static int toaFromString(String phoneNumber)
    {
        if (phoneNumber != null && phoneNumber.length() > 0 && phoneNumber.charAt(0) == '+') return TOA_International;
        return TOA_Unknown;
    }

    public static boolean isGlobalPhoneNumber(String phoneNumber)
    {
        if (TextUtils.isEmpty(phoneNumber)) return false;
        return GLOBAL_PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches();
    }

    private static boolean isDialable(String address)
    {
        for (int index = 0, count = address.length(); index < count; index++)
        {
            if (!isDialable(address.charAt(index))) return false;
        }
        return true;
    }

    public static boolean isWellFormedSmsAddress(String address)
    {
        String networkPortion = extractNetworkPortion(address);
        return !(networkPortion == null || networkPortion.equals("+") || TextUtils.isEmpty(networkPortion)) && isDialable(networkPortion);
    }

    @Deprecated
    public static boolean isEmergencyNumber(String number)
    {
        if (number == null) return false;
        if (number.indexOf('@') >= 0 || number.contains("%40")) return false;
        String networkPortion = extractNetworkPortion(number);
        return DEFAULT_EMERGENCY_NUMBERS.contains(networkPortion);
    }

    @Deprecated
    public static boolean isLocalEmergencyNumber(Context context, String number)
    {
        return isEmergencyNumber(number);
    }

    public static boolean isVoiceMailNumber(String number)
    {
        return false;
    }

    public static boolean isWpsCallNumber(String number)
    {
        return number != null && (number.startsWith("*272") || number.startsWith("*31#*272"));
    }

    public static int getFormatTypeForLocale(Locale locale)
    {
        if (locale == null) return FORMAT_UNKNOWN;
        return getFormatTypeFromCountryCode(locale.getCountry());
    }

    private static int getFormatTypeFromCountryCode(String country)
    {
        if (country == null) return FORMAT_UNKNOWN;
        String upper = country.toUpperCase(Locale.ROOT);
        if (NANP_COUNTRIES.contains(upper)) return FORMAT_NANP;
        if ("JP".equals(upper)) return FORMAT_JAPAN;
        return FORMAT_UNKNOWN;
    }

    @Deprecated
    public static String formatNumber(String source)
    {
        SpannableStringBuilder text = new SpannableStringBuilder(source);
        formatNumber(text, getFormatTypeForLocale(Locale.getDefault()));
        return text.toString();
    }

    @Deprecated
    public static void formatNumber(Editable text, int defaultFormattingType)
    {
        int formatType = defaultFormattingType;
        if (text.length() > 2 && text.charAt(0) == '+')
        {
            if (text.charAt(1) == '1')
            {
                formatType = FORMAT_NANP;
            }
            else if (text.length() >= 3 && text.charAt(1) == '8' && text.charAt(2) == '1')
            {
                formatType = FORMAT_JAPAN;
            }
            else
            {
                formatType = FORMAT_UNKNOWN;
            }
        }

        switch (formatType)
        {
            case FORMAT_NANP:
                formatNanpNumber(text);
                return;
            case FORMAT_JAPAN:
                formatJapaneseNumber(text);
                return;
            default:
                removeDashes(text);
        }
    }

    @Deprecated
    public static void formatNanpNumber(Editable text)
    {
        int length = text.length();
        if (length > "+1-nnn-nnn-nnnn".length()) return;
        if (length <= 5) return;

        CharSequence saved = text.subSequence(0, length);
        removeDashes(text);
        length = text.length();

        int[] breaks = new int[3];
        int numBreaks = 0;
        int state = NANP_STATE_DIGIT;
        int numDigits = 0;
        for (int index = 0; index < length; index++)
        {
            char c = text.charAt(index);
            if (c == '1' && (numDigits == 0 || state == NANP_STATE_PLUS))
            {
                state = NANP_STATE_ONE;
                continue;
            }
            if (c >= '0' && c <= '9')
            {
                if (state == NANP_STATE_PLUS)
                {
                    text.replace(0, length, saved);
                    return;
                }
                if (state == NANP_STATE_ONE)
                {
                    breaks[numBreaks++] = index;
                }
                else if (state != NANP_STATE_DASH && (numDigits == 3 || numDigits == 6))
                {
                    breaks[numBreaks++] = index;
                }
                state = NANP_STATE_DIGIT;
                numDigits++;
                continue;
            }
            if (c == '-')
            {
                state = NANP_STATE_DASH;
                continue;
            }
            if (c == '+' && index == 0)
            {
                state = NANP_STATE_PLUS;
                continue;
            }
            text.replace(0, length, saved);
            return;
        }

        if (numDigits == 7) numBreaks--;

        for (int index = 0; index < numBreaks; index++)
        {
            int position = breaks[index];
            text.replace(position + index, position + index, "-");
        }

        int newLength = text.length();
        while (newLength > 0 && text.charAt(newLength - 1) == '-')
        {
            text.delete(newLength - 1, newLength);
            newLength--;
        }
    }

    @Deprecated
    public static void formatJapaneseNumber(Editable text)
    {
        String digits = text.toString();
        if (digits.indexOf('-') >= 0 || digits.length() < 6) return;
        boolean international = digits.startsWith("+81");
        String national = international ? digits.substring(3) : digits;
        for (int index = 0; index < national.length(); index++)
        {
            if (!isISODigit(national.charAt(index))) return;
        }
        if (!international && !national.startsWith("0")) return;
        String body = international ? national : national.substring(1);
        String formatted = body.startsWith("3") || body.startsWith("6") ? group(body, 1, 4) : group(body, 2, 4);
        if (formatted == null) return;
        String result = international ? "+81-" + formatted : "0" + formatted;
        text.replace(0, text.length(), result);
    }

    private static String group(String body, int first, int second)
    {
        if (body.length() <= first + second) return null;
        return body.substring(0, first) + "-" + body.substring(first, first + second) + "-" + body.substring(first + second);
    }

    private static void removeDashes(Editable text)
    {
        int position = 0;
        while (position < text.length())
        {
            if (text.charAt(position) == '-')
            {
                text.delete(position, position + 1);
            }
            else
            {
                position++;
            }
        }
    }

    public static String formatNumber(String phoneNumber, String defaultCountryIso)
    {
        if (phoneNumber == null) return null;
        if (phoneNumber.startsWith("#") || phoneNumber.startsWith("*")) return phoneNumber;
        String normalized = normalizeNumber(phoneNumber);
        if (normalized.length() == 0) return null;
        String country = defaultCountryIso == null ? "" : defaultCountryIso.toUpperCase(Locale.ROOT);

        if (normalized.startsWith("+"))
        {
            String e164 = normalized;
            if (e164.startsWith("+1") && e164.length() == 12 && isValidNanpNational(e164.substring(2)))
            {
                String national = e164.substring(2);
                return "+1 " + national.substring(0, 3) + "-" + national.substring(3, 6) + "-" + national.substring(6);
            }
            return e164.length() >= 4 && e164.length() <= 16 ? phoneNumber : null;
        }

        if (NANP_COUNTRIES.contains(country))
        {
            String national = normalized;
            boolean trunk = national.length() == 11 && national.charAt(0) == '1';
            if (trunk) national = national.substring(1);
            if (national.length() == 10 && isValidNanpNational(national))
            {
                String formatted = "(" + national.substring(0, 3) + ") " + national.substring(3, 6) + "-" + national.substring(6);
                return trunk ? "1 " + formatted : formatted;
            }
            if (national.length() == 7) return national.substring(0, 3) + "-" + national.substring(3);
            return phoneNumber;
        }

        return COUNTRY_CALLING_CODES.containsKey(country) ? phoneNumber : null;
    }

    public static String formatNumber(String phoneNumber, String phoneNumberE164, String defaultCountryIso)
    {
        if (phoneNumber == null) return null;
        int length = phoneNumber.length();
        for (int index = 0; index < length; index++)
        {
            if (!isDialable(phoneNumber.charAt(index))) return phoneNumber;
        }
        String country = defaultCountryIso;
        if (phoneNumberE164 != null && phoneNumberE164.length() >= 2 && phoneNumberE164.charAt(0) == '+')
        {
            String region = regionForE164(phoneNumberE164);
            if (region != null && normalizeNumber(phoneNumber).indexOf(phoneNumberE164.substring(1)) <= 0) country = region;
        }
        String result = formatNumber(phoneNumber, country);
        return result != null ? result : phoneNumber;
    }

    private static String regionForE164(String e164)
    {
        String digits = e164.substring(1);
        for (int length = 1; length <= 3 && length <= digits.length(); length++)
        {
            int code;
            try
            {
                code = Integer.parseInt(digits.substring(0, length));
            }
            catch (NumberFormatException exception)
            {
                return null;
            }
            if (code == 1) return "US";
            for (Map.Entry<String, Integer> entry : COUNTRY_CALLING_CODES.entrySet())
            {
                if (entry.getValue() == code && !NANP_COUNTRIES.contains(entry.getKey())) return entry.getKey();
            }
        }
        return null;
    }

    private static boolean isValidNanpNational(String national)
    {
        if (national.length() != 10) return false;
        for (int index = 0; index < 10; index++)
        {
            if (!isISODigit(national.charAt(index))) return false;
        }
        return national.charAt(0) >= '2' && national.charAt(3) >= '2';
    }

    public static String formatNumberToE164(String phoneNumber, String defaultCountryIso)
    {
        if (phoneNumber == null) return null;
        String normalized = normalizeNumber(phoneNumber);
        if (normalized.length() == 0) return null;
        if (normalized.startsWith("+"))
        {
            String digits = normalized.substring(1);
            if (digits.length() < 7 || digits.length() > 15 || digits.charAt(0) == '0') return null;
            if (digits.charAt(0) == '1' && !isValidNanpNational(digits.substring(1))) return null;
            return normalized;
        }
        if (defaultCountryIso == null) return null;
        String country = defaultCountryIso.toUpperCase(Locale.ROOT);
        Integer code = COUNTRY_CALLING_CODES.get(country);
        if (code == null) return null;
        String national = normalized;
        if (code == 1)
        {
            if (national.length() == 11 && national.charAt(0) == '1') national = national.substring(1);
            return isValidNanpNational(national) ? "+1" + national : null;
        }
        String international = String.valueOf(code);
        if (national.startsWith("00"))
        {
            String digits = national.substring(2);
            return digits.length() >= 7 && digits.length() <= 15 ? "+" + digits : null;
        }
        if (national.startsWith("0") && !"IT".equals(country)) national = national.substring(1);
        if (national.length() < 4 || national.length() + international.length() > 15) return null;
        return "+" + international + national;
    }

    public static String formatNumberToRFC3966(String phoneNumber, String defaultCountryIso)
    {
        String e164 = formatNumberToE164(phoneNumber, defaultCountryIso);
        if (e164 == null) return null;
        if (e164.startsWith("+1") && e164.length() == 12)
        {
            String national = e164.substring(2);
            return "tel:+1-" + national.substring(0, 3) + "-" + national.substring(3, 6) + "-" + national.substring(6);
        }
        String region = regionForE164(e164);
        Integer code = region == null ? null : COUNTRY_CALLING_CODES.get(region);
        if (code == null) return "tel:" + e164;
        String international = String.valueOf(code);
        return "tel:+" + international + "-" + e164.substring(1 + international.length());
    }

    public static void addTtsSpan(Spannable s, int start, int endExclusive)
    {
        s.setSpan(createTtsSpan(s.subSequence(start, endExclusive).toString()), start, endExclusive, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static CharSequence createTtsSpannable(CharSequence phoneNumber)
    {
        if (phoneNumber == null) return null;
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(phoneNumber);
        addTtsSpan(spannable, 0, spannable.length());
        return spannable;
    }

    public static TtsSpan createTtsSpan(String phoneNumber)
    {
        if (phoneNumber == null) return null;
        TtsSpan.Builder<?> builder = new TtsSpan.Builder<>(TtsSpan.TYPE_TELEPHONE);
        builder.setStringArgument(TtsSpan.ARG_NUMBER_PARTS, splitAtNonNumerics(phoneNumber));
        return builder.build();
    }

    private static String splitAtNonNumerics(CharSequence number)
    {
        StringBuilder result = new StringBuilder(number.length());
        for (int index = 0; index < number.length(); index++)
        {
            char c = number.charAt(index);
            result.append(is12Key(c) ? c : ' ');
        }
        return result.toString().replaceAll(" +", " ").trim();
    }

    public static String calledPartyBCDToString(byte[] bytes, int offset, int length)
    {
        return calledPartyBCDToString(bytes, offset, length, BCD_EXTENDED_TYPE_EF_ADN);
    }

    public static String calledPartyBCDToString(byte[] bytes, int offset, int length, int bcdExtType)
    {
        if (length < 2) return "";
        boolean prependPlus = (bytes[offset] & 0xf0) == (TOA_International & 0xf0);
        StringBuilder result = new StringBuilder(1 + length * 2);
        internalCalledPartyBCDFragmentToString(result, bytes, offset + 1, length - 1, bcdExtType);
        if (prependPlus && result.length() == 0) return "";
        if (!prependPlus) return result.toString();
        String digits = result.toString();
        Matcher matcher = MMI_PREFIX_PATTERN.matcher(digits);
        if (matcher.matches() && matcher.group(2).length() > 0) return matcher.group(1) + "+" + matcher.group(2);
        return "+" + digits;
    }

    public static String calledPartyBCDFragmentToString(byte[] bytes, int offset, int length)
    {
        return calledPartyBCDFragmentToString(bytes, offset, length, BCD_EXTENDED_TYPE_EF_ADN);
    }

    public static String calledPartyBCDFragmentToString(byte[] bytes, int offset, int length, int bcdExtType)
    {
        StringBuilder result = new StringBuilder(length * 2);
        internalCalledPartyBCDFragmentToString(result, bytes, offset, length, bcdExtType);
        return result.toString();
    }

    private static void internalCalledPartyBCDFragmentToString(StringBuilder result, byte[] bytes, int offset, int length, int bcdExtType)
    {
        for (int index = offset; index < length + offset; index++)
        {
            char c = bcdToChar((byte)(bytes[index] & 0xf), bcdExtType);
            if (c == 0) return;
            result.append(c);
            byte high = (byte)((bytes[index] >> 4) & 0xf);
            if (high == 0xf && index + 1 == length + offset) break;
            c = bcdToChar(high, bcdExtType);
            if (c == 0) return;
            result.append(c);
        }
    }

    private static char bcdToChar(byte b, int bcdExtType)
    {
        if (b < 0xa) return (char)('0' + b);
        String extended = bcdExtType == BCD_EXTENDED_TYPE_CALLED_PARTY ? "*#abc" : "*#" + PAUSE + WILD + WAIT;
        int index = b - 0xa;
        return index < extended.length() ? extended.charAt(index) : 0;
    }

    private static int charToBcd(char c, int bcdExtType)
    {
        if ('0' <= c && c <= '9') return c - '0';
        String extended = bcdExtType == BCD_EXTENDED_TYPE_CALLED_PARTY ? "*#abc" : "*#" + PAUSE + WILD + WAIT;
        int index = extended.indexOf(c);
        if (index == -1) throw new RuntimeException("invalid char for BCD " + c);
        return 0xa + index;
    }

    public static byte[] networkPortionToCalledPartyBCD(String s)
    {
        return numberToCalledPartyBCDHelper(extractNetworkPortion(s), false, BCD_EXTENDED_TYPE_EF_ADN);
    }

    public static byte[] networkPortionToCalledPartyBCDWithLength(String s)
    {
        return numberToCalledPartyBCDHelper(extractNetworkPortion(s), true, BCD_EXTENDED_TYPE_EF_ADN);
    }

    @Deprecated
    public static byte[] numberToCalledPartyBCD(String number)
    {
        return numberToCalledPartyBCD(number, BCD_EXTENDED_TYPE_EF_ADN);
    }

    public static byte[] numberToCalledPartyBCD(String number, int bcdExtType)
    {
        return numberToCalledPartyBCDHelper(number, false, bcdExtType);
    }

    private static byte[] numberToCalledPartyBCDHelper(String number, boolean includeLength, int bcdExtType)
    {
        if (number == null) return null;
        int effectiveLength = number.length();
        boolean hasPlus = number.indexOf('+') != -1;
        if (hasPlus) effectiveLength--;
        if (effectiveLength == 0) return null;

        int extraBytes = includeLength ? 2 : 1;
        int resultLength = (effectiveLength + 1) / 2 + extraBytes;
        byte[] result = new byte[resultLength];

        int digitCount = 0;
        for (int index = 0; index < number.length(); index++)
        {
            char c = number.charAt(index);
            if (c == '+') continue;
            int shift = (digitCount & 0x01) == 1 ? 4 : 0;
            result[extraBytes + (digitCount >> 1)] |= (byte)((charToBcd(c, bcdExtType) & 0x0F) << shift);
            digitCount++;
        }

        if ((digitCount & 0x01) == 1) result[extraBytes + (digitCount >> 1)] |= (byte)0xF0;

        int offset = 0;
        if (includeLength) result[offset++] = (byte)(resultLength - 1);
        result[offset] = (byte)(hasPlus ? TOA_International : TOA_Unknown);
        return result;
    }

    static boolean isNanpCountry(String country)
    {
        return country != null && NANP_COUNTRIES.contains(country.toUpperCase(Locale.ROOT));
    }
}
