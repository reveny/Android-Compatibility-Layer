package android.app;

import android.os.LocaleList;

public final class LocaleManager
{
    private LocaleList mApplicationLocales = LocaleList.getEmptyLocaleList();

    public LocaleList getApplicationLocales()
    {
        return mApplicationLocales;
    }

    public LocaleList getApplicationLocales(String packageName)
    {
        return mApplicationLocales;
    }

    public void setApplicationLocales(LocaleList locales)
    {
        mApplicationLocales = locales == null ? LocaleList.getEmptyLocaleList() : locales;
    }

    public LocaleList getSystemLocales()
    {
        return LocaleList.getDefault();
    }
}
