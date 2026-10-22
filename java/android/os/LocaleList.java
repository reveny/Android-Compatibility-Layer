package android.os;

import java.util.Locale;

public final class LocaleList
{
    private static final LocaleList EMPTY = new LocaleList();
    private static LocaleList sDefault = new LocaleList(Locale.US);

    private final Locale[] mList;

    public LocaleList(Locale... list)
    {
        mList = list == null ? new Locale[0] : list.clone();
    }

    public Locale get(int index)
    {
        return index >= 0 && index < mList.length ? mList[index] : null;
    }

    public boolean isEmpty()
    {
        return mList.length == 0;
    }

    public int size()
    {
        return mList.length;
    }

    public int indexOf(Locale locale)
    {
        for (int index = 0; index < mList.length; index++)
        {
            if (mList[index].equals(locale)) return index;
        }
        return -1;
    }

    public static LocaleList getEmptyLocaleList()
    {
        return EMPTY;
    }

    public static LocaleList getDefault()
    {
        return sDefault;
    }

    public static LocaleList getAdjustedDefault()
    {
        return sDefault;
    }

    public static void setDefault(LocaleList locales)
    {
        if (locales == null || locales.isEmpty()) return;

        sDefault = locales;
        Locale.setDefault(locales.get(0));
    }

    public String toLanguageTags()
    {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < mList.length; index++)
        {
            if (index > 0) builder.append(',');
            builder.append(mList[index].toLanguageTag());
        }
        return builder.toString();
    }

    public static boolean matchesLanguageAndScript(Locale supported, Locale desired)
    {
        if (supported == null || desired == null) return false;
        if (supported.equals(desired)) return true;
        if (!supported.getLanguage().equals(desired.getLanguage())) return false;

        return likelyScript(supported).equals(likelyScript(desired));
    }

    private static String likelyScript(Locale locale)
    {
        String script = locale.getScript();
        if (!script.isEmpty()) return script;

        String language = locale.getLanguage();
        String country = locale.getCountry();

        if ("zh".equals(language))
        {
            return "TW".equals(country) || "HK".equals(country) || "MO".equals(country) ? "Hant" : "Hans";
        }
        if ("sr".equals(language)) return "Cyrl";
        if ("ru".equals(language) || "uk".equals(language) || "bg".equals(language) || "be".equals(language)) return "Cyrl";
        if ("ar".equals(language) || "fa".equals(language) || "ur".equals(language)) return "Arab";
        if ("ja".equals(language)) return "Jpan";
        if ("ko".equals(language)) return "Kore";
        if ("el".equals(language)) return "Grek";
        if ("he".equals(language) || "iw".equals(language) || "yi".equals(language)) return "Hebr";
        if ("hi".equals(language) || "mr".equals(language) || "ne".equals(language)) return "Deva";
        if ("th".equals(language)) return "Thai";
        return "Latn";
    }

    public Locale getFirstMatch(String[] supportedLocales)
    {
        if (supportedLocales == null || supportedLocales.length == 0) return mList.length == 0 ? null : mList[0];

        Locale[] supported = new Locale[supportedLocales.length];
        for (int index = 0; index < supportedLocales.length; index++)
        {
            supported[index] = Locale.forLanguageTag(supportedLocales[index]);
        }

        for (Locale desired : mList)
        {
            for (Locale candidate : supported)
            {
                if (candidate.equals(desired)) return candidate;
            }

            for (Locale candidate : supported)
            {
                if (matchesLanguageAndScript(candidate, desired)) return candidate;
            }
        }

        return supported[0];
    }

    public static LocaleList forLanguageTags(String tags)
    {
        if (tags == null || tags.isEmpty()) return EMPTY;

        String[] parts = tags.split(",");
        Locale[] locales = new Locale[parts.length];
        for (int index = 0; index < parts.length; index++)
        {
            locales[index] = Locale.forLanguageTag(parts[index]);
        }
        return new LocaleList(locales);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof LocaleList)) return false;

        return java.util.Arrays.equals(mList, ((LocaleList)other).mList);
    }

    @Override
    public int hashCode()
    {
        return java.util.Arrays.hashCode(mList);
    }

    @Override
    public String toString()
    {
        return "LocaleList[" + toLanguageTags() + "]";
    }
}
