package android.content.pm;

public class ResolveInfo
{
    public ActivityInfo activityInfo;
    public ServiceInfo serviceInfo;
    public ProviderInfo providerInfo;
    public android.content.IntentFilter filter;
    public int priority;
    public int match;
    public String resolvePackageName;

    public ResolveInfo()
    {
    }

    public String loadLabel(PackageManager packageManager)
    {
        if (activityInfo != null) return activityInfo.name;
        if (serviceInfo != null) return serviceInfo.name;

        return null;
    }

    @Override
    public String toString()
    {
        return "ResolveInfo{" + loadLabel(null) + "}";
    }
}
