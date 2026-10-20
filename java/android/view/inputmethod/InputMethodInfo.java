package android.view.inputmethod;

public final class InputMethodInfo
{
    private final String mId;
    private final String mPackageName;
    private final String mServiceName;

    public InputMethodInfo(String packageName, String serviceName, String id)
    {
        mPackageName = packageName;
        mServiceName = serviceName;
        mId = id;
    }

    public String getId()
    {
        return mId;
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    public String getServiceName()
    {
        return mServiceName;
    }

    public int getSubtypeCount()
    {
        return 0;
    }

    public boolean isDefault(android.content.Context context)
    {
        return false;
    }
}
