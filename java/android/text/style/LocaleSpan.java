package android.text.style;

import android.os.LocaleList;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;
import java.util.Locale;

public class LocaleSpan extends MetricAffectingSpan implements ParcelableSpan
{
    private final LocaleList mLocales;

    public LocaleSpan(Locale locale)
    {
        mLocales = locale == null ? LocaleList.getEmptyLocaleList() : new LocaleList(locale);
    }

    public LocaleSpan(LocaleList locales)
    {
        if (locales == null) throw new NullPointerException("locales cannot be null");

        mLocales = locales;
    }

    public LocaleSpan(Parcel source)
    {
        mLocales = LocaleList.forLanguageTags(source.readString());
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.LOCALE_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mLocales.toLanguageTags());
    }

    public Locale getLocale()
    {
        return mLocales.size() == 0 ? null : mLocales.get(0);
    }

    public LocaleList getLocales()
    {
        return mLocales;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setTextLocales(mLocales);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        if (paint != null) paint.setTextLocales(mLocales);
    }

    @Override
    public String toString()
    {
        return "LocaleSpan{locales=" + mLocales + "}";
    }
}
