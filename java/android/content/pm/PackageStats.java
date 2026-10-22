package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class PackageStats implements Parcelable
{
    public static final Parcelable.Creator<PackageStats> CREATOR = new Parcelable.Creator<PackageStats>()
    {
        @Override
        public PackageStats createFromParcel(Parcel source)
        {
            return new PackageStats(source);
        }

        @Override
        public PackageStats[] newArray(int size)
        {
            return new PackageStats[size];
        }
    };

    public String packageName;
    public long codeSize;
    public long dataSize;
    public long cacheSize;
    public long externalCodeSize;
    public long externalDataSize;
    public long externalCacheSize;
    public long externalMediaSize;
    public long externalObbSize;

    public PackageStats(String packageName)
    {
        this.packageName = packageName;
    }

    public PackageStats(Parcel source)
    {
        Object value = source == null ? null : source.readValue(null);
        if (value instanceof PackageStats) copyFrom((PackageStats)value);
    }

    public PackageStats(PackageStats original)
    {
        if (original != null) copyFrom(original);
    }

    private void copyFrom(PackageStats original)
    {
        packageName = original.packageName;
        codeSize = original.codeSize;
        dataSize = original.dataSize;
        cacheSize = original.cacheSize;
        externalCodeSize = original.externalCodeSize;
        externalDataSize = original.externalDataSize;
        externalCacheSize = original.externalCacheSize;
        externalMediaSize = original.externalMediaSize;
        externalObbSize = original.externalObbSize;
    }

    @Override
    public String toString()
    {
        return "PackageStats{" + Integer.toHexString(System.identityHashCode(this)) + " " + packageName + " code=" + codeSize + ",data="
               + dataSize + ",cache=" + cacheSize + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags)
    {
        dest.writeValue(new PackageStats(this));
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof PackageStats)) return false;

        PackageStats stats = (PackageStats)other;
        return java.util.Objects.equals(packageName, stats.packageName) && codeSize == stats.codeSize && dataSize == stats.dataSize
               && cacheSize == stats.cacheSize && externalCodeSize == stats.externalCodeSize
               && externalDataSize == stats.externalDataSize && externalCacheSize == stats.externalCacheSize
               && externalMediaSize == stats.externalMediaSize && externalObbSize == stats.externalObbSize;
    }

    @Override
    public int hashCode()
    {
        return java.util.Objects.hash(packageName, codeSize, dataSize, cacheSize, externalCodeSize, externalDataSize, externalCacheSize,
                                      externalMediaSize, externalObbSize);
    }
}
