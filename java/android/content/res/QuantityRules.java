package android.content.res;

import java.util.Locale;

final class QuantityRules
{
    static final int OTHER = 0;
    static final int ZERO = 1;
    static final int ONE = 2;
    static final int TWO = 3;
    static final int FEW = 4;
    static final int MANY = 5;

    private static final int[] NONE = {};
    private static final int[] ONE_ONLY = {ONE};
    private static final int[] ONE_FEW = {ONE, FEW};
    private static final int[] ONE_TWO = {ONE, TWO};
    private static final int[] ONE_FEW_MANY = {ONE, FEW, MANY};
    private static final int[] ONE_MANY = {ONE, MANY};
    private static final int[] ALL = {ZERO, ONE, TWO, FEW, MANY};

    private QuantityRules()
    {
    }

    static int[] categoriesFor(Locale locale)
    {
        String language = locale == null ? "en" : locale.getLanguage();

        switch (language)
        {
            case "ja":
            case "ko":
            case "zh":
            case "vi":
            case "th":
            case "id":
            case "in":
            case "ms":
            case "my":
            case "lo":
            case "km":
                return NONE;
            case "ru":
            case "uk":
            case "be":
            case "pl":
            case "lt":
            case "cs":
            case "sk":
                return ONE_FEW_MANY;
            case "ro":
            case "hr":
            case "sr":
            case "bs":
                return ONE_FEW;
            case "he":
            case "iw":
                return ONE_TWO;
            case "ar":
            case "cy":
                return ALL;
            case "fr":
            case "it":
            case "es":
            case "pt":
                return ONE_MANY;
            default:
                return ONE_ONLY;
        }
    }

    static int categoryOf(Locale locale, int quantity)
    {
        String language = locale == null ? "en" : locale.getLanguage();
        int absolute = Math.abs(quantity);
        int lastDigit = absolute % 10;
        int lastTwo = absolute % 100;

        switch (language)
        {
            case "ja":
            case "ko":
            case "zh":
            case "vi":
            case "th":
            case "id":
            case "in":
            case "ms":
            case "my":
            case "lo":
            case "km":
                return OTHER;
            case "ru":
            case "uk":
            case "be":
                if (lastDigit == 1 && lastTwo != 11) return ONE;
                if (lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14)) return FEW;
                return MANY;
            case "pl":
                if (absolute == 1) return ONE;
                if (lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14)) return FEW;
                return MANY;
            case "lt":
                if (lastDigit == 1 && (lastTwo < 11 || lastTwo > 19)) return ONE;
                if (lastDigit >= 2 && (lastTwo < 11 || lastTwo > 19)) return FEW;
                return OTHER;
            case "cs":
            case "sk":
                if (absolute == 1) return ONE;
                if (absolute >= 2 && absolute <= 4) return FEW;
                return OTHER;
            case "ro":
                if (absolute == 1) return ONE;
                if (absolute == 0 || (lastTwo >= 2 && lastTwo <= 19)) return FEW;
                return OTHER;
            case "hr":
            case "sr":
            case "bs":
                if (lastDigit == 1 && lastTwo != 11) return ONE;
                if (lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14)) return FEW;
                return OTHER;
            case "he":
            case "iw":
                if (absolute == 1) return ONE;
                if (absolute == 2) return TWO;
                return OTHER;
            case "ar":
                if (absolute == 0) return ZERO;
                if (absolute == 1) return ONE;
                if (absolute == 2) return TWO;
                if (lastTwo >= 3 && lastTwo <= 10) return FEW;
                if (lastTwo >= 11) return MANY;
                return OTHER;
            case "cy":
                if (absolute == 0) return ZERO;
                if (absolute == 1) return ONE;
                if (absolute == 2) return TWO;
                if (absolute == 3) return FEW;
                if (absolute == 6) return MANY;
                return OTHER;
            case "fr":
                if (absolute == 0 || absolute == 1) return ONE;
                if (absolute != 0 && absolute % 1000000 == 0) return MANY;
                return OTHER;
            case "pt":
                if (absolute == 0 || absolute == 1) return ONE;
                if (absolute != 0 && absolute % 1000000 == 0) return MANY;
                return OTHER;
            case "it":
            case "es":
                if (absolute == 1) return ONE;
                if (absolute != 0 && absolute % 1000000 == 0) return MANY;
                return OTHER;
            case "hi":
            case "bn":
            case "fa":
            case "am":
            case "zu":
                return absolute <= 1 ? ONE : OTHER;
            default:
                return absolute == 1 ? ONE : OTHER;
        }
    }

    static int entryIndex(Locale locale, int quantity, int entryCount)
    {
        if (entryCount <= 1) return 0;

        int category = categoryOf(locale, quantity);
        if (category == OTHER) return 0;

        int[] categories = categoriesFor(locale);
        if (categories.length + 1 != entryCount)
        {
            if (entryCount == 2 && category == ONE) return 1;
            return 0;
        }

        for (int index = 0; index < categories.length; index++)
        {
            if (categories[index] == category) return index + 1;
        }

        return 0;
    }
}
