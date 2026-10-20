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
