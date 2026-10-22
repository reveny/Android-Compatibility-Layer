package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class ResolveInfo implements Parcelable
{
    public static final Parcelable.Creator<ResolveInfo> CREATOR = new Parcelable.Creator<ResolveInfo>()
    {
        @Override
        public ResolveInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof ResolveInfo ? new ResolveInfo((ResolveInfo)value) : new ResolveInfo();
        }

        @Override
        public ResolveInfo[] newArray(int size)
        {
            return new ResolveInfo[size];
        }
    };

    public ActivityInfo activityInfo;
    public ServiceInfo serviceInfo;
    public ProviderInfo providerInfo;
    public android.content.IntentFilter filter;
    public int priority;
    public int preferredOrder;
    public int match;
    public int specificIndex = -1;
    public boolean isDefault;
    public int labelRes;
    public CharSequence nonLocalizedLabel;
    public int icon;
    public String resolvePackageName;
    public boolean isInstantAppAvailable;

    public ResolveInfo()
    {
    }

    public ResolveInfo(ResolveInfo original)
    {
        if (original == null) return;

        activityInfo = original.activityInfo;
        serviceInfo = original.serviceInfo;
        providerInfo = original.providerInfo;
        filter = original.filter;
        priority = original.priority;
        preferredOrder = original.preferredOrder;
        match = original.match;
        specificIndex = original.specificIndex;
        isDefault = original.isDefault;
        labelRes = original.labelRes;
        nonLocalizedLabel = original.nonLocalizedLabel;
        icon = original.icon;
        resolvePackageName = original.resolvePackageName;
        isInstantAppAvailable = original.isInstantAppAvailable;
    }

    private ComponentInfo getComponentInfo()
    {
        if (activityInfo != null) return activityInfo;
        if (serviceInfo != null) return serviceInfo;

        return providerInfo;
    }

    public CharSequence loadLabel(PackageManager packageManager)
    {
        if (nonLocalizedLabel != null) return nonLocalizedLabel;

        if (labelRes != 0 && packageManager != null)
        {
            ComponentInfo component = getComponentInfo();
            String packageName = resolvePackageName != null ? resolvePackageName : component == null ? null : component.packageName;
            CharSequence label = packageManager.getText(packageName, labelRes, null);
            if (label != null) return label.toString().trim();
        }

        ComponentInfo component = getComponentInfo();
        if (component == null) return null;
        if (packageManager == null) return component.name;

        CharSequence label = component.loadLabel(packageManager);
        return label == null ? null : label.toString().trim();
    }

    public android.graphics.drawable.Drawable loadIcon(PackageManager packageManager)
    {
        if (icon != 0 && packageManager != null)
        {
            ComponentInfo component = getComponentInfo();
            String packageName = resolvePackageName != null ? resolvePackageName : component == null ? null : component.packageName;
            android.graphics.drawable.Drawable drawable = packageManager.getDrawable(packageName, icon, null);
            if (drawable != null) return drawable;
        }

        ComponentInfo component = getComponentInfo();
        if (component != null) return component.loadIcon(packageManager);

        return packageManager == null ? null : packageManager.getDefaultActivityIcon();
    }

    public final int getIconResource()
    {
        if (icon != 0) return icon;

        ComponentInfo component = getComponentInfo();
        return component == null ? 0 : component.getIconResource();
    }

    public boolean isCrossProfileIntentForwarderActivity()
    {
        return false;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags)
    {
        dest.writeValue(this);
    }

    @Override
    public String toString()
    {
        ComponentInfo component = getComponentInfo();
        return "ResolveInfo{" + Integer.toHexString(System.identityHashCode(this)) + " "
               + (component == null ? null : component.packageName + "/" + component.name) + " m=0x" + Integer.toHexString(match) + "}";
    }
}
