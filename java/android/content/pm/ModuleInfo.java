package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public final class ModuleInfo implements Parcelable
{
    public static final Parcelable.Creator<ModuleInfo> CREATOR = new Parcelable.Creator<ModuleInfo>()
    {
        @Override
        public ModuleInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof ModuleInfo ? (ModuleInfo)value : new ModuleInfo(null, null, false);
        }

        @Override
        public ModuleInfo[] newArray(int size)
        {
            return new ModuleInfo[size];
        }
    };

    private final CharSequence mName;
    private final String mPackageName;
    private final boolean mHidden;

    ModuleInfo(CharSequence name, String packageName, boolean hidden)
    {
        mName = name;
        mPackageName = packageName;
        mHidden = hidden;
    }

    public CharSequence getName()
    {
        return mName;
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    public boolean isHidden()
    {
        return mHidden;
    }

    @Override
    public String toString()
    {
        return "ModuleInfo{" + Integer.toHexString(System.identityHashCode(this)) + " " + mName + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public int hashCode()
    {
        return java.util.Objects.hash(mName == null ? null : mName.toString(), mPackageName, mHidden);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ModuleInfo)) return false;

        ModuleInfo module = (ModuleInfo)other;
        String name = mName == null ? null : mName.toString();
        String otherName = module.mName == null ? null : module.mName.toString();

        return java.util.Objects.equals(name, otherName) && java.util.Objects.equals(mPackageName, module.mPackageName)
               && mHidden == module.mHidden;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(this);
    }
}
