package android.content;

public final class ComponentName
{
    private final String mPackage;
    private final String mClass;

    public ComponentName(String packageName, String className)
    {
        mPackage = packageName;
        mClass = className;
    }

    public ComponentName(Context context, String className)
    {
        mPackage = context == null ? "" : context.getPackageName();
        mClass = className;
    }

    public ComponentName(Context context, Class<?> subject)
    {
        mPackage = context == null ? "" : context.getPackageName();
        mClass = subject.getName();
    }

    public String getPackageName()
    {
        return mPackage;
    }

    public String getClassName()
    {
        return mClass;
    }

    public String getShortClassName()
    {
        if (mClass == null || mPackage == null) return mClass;
        if (!mClass.startsWith(mPackage)) return mClass;

        int at = mPackage.length();
        if (at < mClass.length() && mClass.charAt(at) == '.') return mClass.substring(at);

        return mClass;
    }

    public String flattenToString()
    {
        return mPackage + "/" + mClass;
    }

    public String flattenToShortString()
    {
        return mPackage + "/" + getShortClassName();
    }

    @Override
    public String toString()
    {
        return "ComponentName{" + flattenToString() + "}";
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ComponentName)) return false;

        ComponentName compared = (ComponentName)other;
        return java.util.Objects.equals(mPackage, compared.mPackage) && java.util.Objects.equals(mClass, compared.mClass);
    }

    @Override
    public int hashCode()
    {
        return java.util.Objects.hash(mPackage, mClass);
    }
}
