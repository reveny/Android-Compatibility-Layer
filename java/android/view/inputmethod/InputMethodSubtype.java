package android.view.inputmethod;

public final class InputMethodSubtype
{
    private final int mSubtypeId;
    private final String mMode;
    private final String mLocale;

    public InputMethodSubtype(int nameId, int iconId, String locale, String mode, String extraValue, boolean isAuxiliary,
                              boolean overridesImplicitlyEnabledSubtype)
    {
        mSubtypeId = nameId;
        mMode = mode;
        mLocale = locale;
    }

    public int hashCode()
    {
        return mSubtypeId;
    }

    public String getMode()
    {
        return mMode;
    }

    public String getLocale()
    {
        return mLocale;
    }

    public String getLanguageTag()
    {
        return mLocale == null ? "" : mLocale;
    }

    public boolean isAuxiliary()
    {
        return false;
    }

    public String getExtraValue()
    {
        return "";
    }
}
