package android.content.pm;

public class ComponentInfo extends PackageItemInfo
{
    public ApplicationInfo applicationInfo;
    public boolean exported;
    public boolean enabled = true;
    public boolean directBootAware;
    public String processName;
    public String splitName;
    public String[] attributionTags;
    public int descriptionRes;

    public ComponentInfo()
    {
    }

    public ComponentInfo(ComponentInfo original)
    {
        super(original);
        if (original == null) return;

        applicationInfo = original.applicationInfo;
        exported = original.exported;
        enabled = original.enabled;
        directBootAware = original.directBootAware;
        processName = original.processName;
        splitName = original.splitName;
        attributionTags = original.attributionTags;
        descriptionRes = original.descriptionRes;
    }

    @Override
    public CharSequence loadLabel(PackageManager packageManager)
    {
        if (nonLocalizedLabel != null) return nonLocalizedLabel;
        if (labelRes == 0 && applicationInfo != null) return applicationInfo.loadLabel(packageManager);

        return super.loadLabel(packageManager);
    }

    @Override
    public android.graphics.drawable.Drawable loadIcon(PackageManager packageManager)
    {
        if (icon == 0 && applicationInfo != null) return applicationInfo.loadIcon(packageManager);

        return super.loadIcon(packageManager);
    }

    public boolean isEnabled()
    {
        return enabled && (applicationInfo == null || applicationInfo.enabled);
    }

    public final int getIconResource()
    {
        if (icon != 0) return icon;

        return applicationInfo == null ? 0 : applicationInfo.icon;
    }

    public final int getBannerResource()
    {
        if (banner != 0) return banner;

        return applicationInfo == null ? 0 : applicationInfo.banner;
    }

    public final int getLogoResource()
    {
        if (logo != 0) return logo;

        return applicationInfo == null ? 0 : applicationInfo.logo;
    }
}
