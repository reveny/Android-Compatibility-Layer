package android.content;

import android.os.Parcel;
import android.os.Parcelable;

public final class ComponentName implements Parcelable, Cloneable, Comparable<ComponentName>
{
    public static final Parcelable.Creator<ComponentName> CREATOR = new Parcelable.Creator<ComponentName>()
    {
        @Override
        public ComponentName createFromParcel(Parcel source)
        {
            return new ComponentName(source);
        }

        @Override
        public ComponentName[] newArray(int size)
        {
            return new ComponentName[size];
        }
    };

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

    public ComponentName(Parcel source)
    {
        String packageName = source.readString();
        String className = source.readString();

        mPackage = packageName == null ? "" : packageName;
        mClass = className == null ? "" : className;
    }

    public static ComponentName createRelative(String packageName, String className)
    {
        if (className == null || className.isEmpty()) throw new IllegalArgumentException("class name cannot be empty");

        String fullName = className.charAt(0) == '.' ? packageName + className : className;
        return new ComponentName(packageName, fullName);
    }

    public static ComponentName createRelative(Context packageContext, String className)
    {
        return createRelative(packageContext.getPackageName(), className);
    }

    @Override
    public ComponentName clone()
    {
        return new ComponentName(mPackage, mClass);
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

    public static ComponentName unflattenFromString(String text)
    {
        if (text == null) return null;

        int separator = text.indexOf('/');
        if (separator < 0 || separator + 1 >= text.length()) return null;

        String packageName = text.substring(0, separator);
        String className = text.substring(separator + 1);
        if (className.length() > 0 && className.charAt(0) == '.') className = packageName + className;

        return new ComponentName(packageName, className);
    }

    public String toShortString()
    {
        return "{" + mPackage + "/" + mClass + "}";
    }

    @Override
    public String toString()
    {
        return "ComponentInfo{" + mPackage + "/" + mClass + "}";
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
        return (mPackage == null ? 0 : mPackage.hashCode()) + (mClass == null ? 0 : mClass.hashCode());
    }

    @Override
    public int compareTo(ComponentName other)
    {
        int compared = String.valueOf(mPackage).compareTo(String.valueOf(other.mPackage));
        if (compared != 0) return compared;

        return String.valueOf(mClass).compareTo(String.valueOf(other.mClass));
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mPackage);
        destination.writeString(mClass);
    }

    public static void writeToParcel(ComponentName component, Parcel destination)
    {
        if (component != null)
        {
            component.writeToParcel(destination, 0);
            return;
        }

        destination.writeString(null);
    }

    public static ComponentName readFromParcel(Parcel source)
    {
        String packageName = source.readString();
        if (packageName == null) return null;

        String className = source.readString();
        return new ComponentName(packageName, className == null ? "" : className);
    }
}
