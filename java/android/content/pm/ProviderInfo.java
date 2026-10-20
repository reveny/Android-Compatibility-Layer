package android.content.pm;

public class ProviderInfo extends ComponentInfo
{
    public String authority;
    public boolean multiprocess;
    public boolean grantUriPermissions;
    public int initOrder;

    public ProviderInfo()
    {
    }
}
