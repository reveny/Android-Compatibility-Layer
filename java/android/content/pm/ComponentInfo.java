package android.content.pm;

public class ComponentInfo extends PackageItemInfo
{
    public ApplicationInfo applicationInfo;
    public boolean exported;
    public boolean enabled = true;
    public boolean directBootAware;
    public String processName;

    public ComponentInfo()
    {
    }
}
