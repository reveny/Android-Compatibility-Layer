package android.content.pm;

import android.os.Bundle;

public class PackageItemInfo
{
    public String name;
    public String packageName;
    public Bundle metaData;
    public int labelRes;
    public int icon;
    public int logo;
    public int banner;
    public CharSequence nonLocalizedLabel;

    public PackageItemInfo()
    {
    }

    public PackageItemInfo(PackageItemInfo original)
    {
        if (original == null) return;

        name = original.name;
        packageName = original.packageName;
        metaData = original.metaData;
        labelRes = original.labelRes;
        icon = original.icon;
        logo = original.logo;
        banner = original.banner;
        nonLocalizedLabel = original.nonLocalizedLabel;
    }

    protected PackageItemInfo(android.os.Parcel source)
    {
        this(source == null ? null : castOrNull(source.readValue(null)));
    }

    private static PackageItemInfo castOrNull(Object value)
    {
        return value instanceof PackageItemInfo ? (PackageItemInfo)value : null;
    }

    public CharSequence loadLabel(PackageManager packageManager)
    {
        if (nonLocalizedLabel != null) return nonLocalizedLabel;

        if (labelRes != 0 && packageManager != null)
        {
            CharSequence label = packageManager.getText(packageName, labelRes, null);
            if (label != null) return label.toString().trim();
        }

        if (name != null) return name;

        return packageName;
    }

    public android.graphics.drawable.Drawable loadIcon(PackageManager packageManager)
    {
        if (packageManager == null) return null;

        if (icon != 0)
        {
            android.graphics.drawable.Drawable drawable = packageManager.getDrawable(packageName, icon, null);
            if (drawable != null) return drawable;
        }

        return packageManager.getDefaultActivityIcon();
    }

    public android.graphics.drawable.Drawable loadUnbadgedIcon(PackageManager packageManager)
    {
        return loadIcon(packageManager);
    }

    public android.graphics.drawable.Drawable loadBanner(PackageManager packageManager)
    {
        if (packageManager == null || banner == 0) return null;

        return packageManager.getDrawable(packageName, banner, null);
    }

    public android.graphics.drawable.Drawable loadLogo(PackageManager packageManager)
    {
        if (packageManager == null || logo == 0) return null;

        return packageManager.getDrawable(packageName, logo, null);
    }

    public android.content.res.XmlResourceParser loadXmlMetaData(PackageManager packageManager, String name)
    {
        if (metaData == null || packageManager == null) return null;

        int resourceId = metaData.getInt(name);
        if (resourceId == 0) return null;

        return packageManager.getXml(packageName, resourceId, null);
    }

    public void writeToParcel(android.os.Parcel dest, int parcelableFlags)
    {
        dest.writeValue(this);
    }
}
