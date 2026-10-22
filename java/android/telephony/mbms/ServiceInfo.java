package android.telephony.mbms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class ServiceInfo
{
    private final Map<Locale, String> mNames;
    private final String mClassName;
    private final List<Locale> mLocales;
    private final String mServiceId;
    private final Date mSessionStartTime;
    private final Date mSessionEndTime;

    ServiceInfo(Map<Locale, String> names, String className, List<Locale> locales, String serviceId, Date start, Date end)
    {
        mNames = new HashMap<>(names == null ? new HashMap<Locale, String>() : names);
        mClassName = className;
        mLocales = new ArrayList<>(locales == null ? new ArrayList<Locale>() : locales);
        mServiceId = serviceId;
        mSessionStartTime = start == null ? null : (Date)start.clone();
        mSessionEndTime = end == null ? null : (Date)end.clone();
    }

    public CharSequence getNameForLocale(Locale locale)
    {
        if (!mNames.containsKey(locale)) throw new NoSuchElementException("Locale not supported");
        return mNames.get(locale);
    }

    public Set<Locale> getNamedContentLocales()
    {
        return Collections.unmodifiableSet(mNames.keySet());
    }

    public String getServiceClassName()
    {
        return mClassName;
    }

    public List<Locale> getLocales()
    {
        return mLocales;
    }

    public String getServiceId()
    {
        return mServiceId;
    }

    public Date getSessionStartTime()
    {
        return mSessionStartTime;
    }

    public Date getSessionEndTime()
    {
        return mSessionEndTime;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ServiceInfo)) return false;
        ServiceInfo info = (ServiceInfo)other;
        return Objects.equals(mNames, info.mNames) && Objects.equals(mClassName, info.mClassName) && Objects.equals(mLocales, info.mLocales)
            && Objects.equals(mServiceId, info.mServiceId) && Objects.equals(mSessionStartTime, info.mSessionStartTime)
            && Objects.equals(mSessionEndTime, info.mSessionEndTime);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mNames, mClassName, mLocales, mServiceId, mSessionStartTime, mSessionEndTime);
    }
}
