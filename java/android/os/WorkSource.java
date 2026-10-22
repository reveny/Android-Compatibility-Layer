package android.os;

import java.util.ArrayList;
import java.util.List;

public class WorkSource implements Parcelable
{
    public static final Parcelable.Creator<WorkSource> CREATOR = new Parcelable.Creator<WorkSource>()
    {
        @Override
        public WorkSource createFromParcel(Parcel source)
        {
            WorkSource workSource = new WorkSource();
            int count = source.readInt();
            for (int index = 0; index < count; index++)
            {
                int uid = source.readInt();
                workSource.addEntry(uid, source.readString());
            }
            return workSource;
        }

        @Override
        public WorkSource[] newArray(int size)
        {
            return new WorkSource[size];
        }
    };

    private final List<Integer> mUids = new ArrayList<>();
    private final List<String> mNames = new ArrayList<>();

    public WorkSource()
    {
    }

    public WorkSource(WorkSource original)
    {
        if (original == null) return;

        mUids.addAll(original.mUids);
        mNames.addAll(original.mNames);
    }

    public WorkSource(int uid)
    {
        addEntry(uid, null);
    }

    public WorkSource(int uid, String packageName)
    {
        addEntry(uid, packageName);
    }

    public int size()
    {
        return mUids.size();
    }

    public int getUid(int index)
    {
        return mUids.get(index);
    }

    public String getPackageName(int index)
    {
        return mNames.get(index);
    }

    public boolean isEmpty()
    {
        return mUids.isEmpty();
    }

    public void clear()
    {
        mUids.clear();
        mNames.clear();
    }

    public boolean diff(WorkSource other)
    {
        return !equals(other);
    }

    public void set(WorkSource other)
    {
        clear();
        if (other == null) return;

        mUids.addAll(other.mUids);
        mNames.addAll(other.mNames);
    }

    public boolean add(WorkSource other)
    {
        if (other == null) return false;

        boolean changed = false;
        for (int index = 0; index < other.mUids.size(); index++)
        {
            changed |= addEntry(other.mUids.get(index), other.mNames.get(index));
        }
        return changed;
    }

    public boolean add(int uid)
    {
        return addEntry(uid, null);
    }

    public boolean add(int uid, String packageName)
    {
        return addEntry(uid, packageName);
    }

    public boolean remove(WorkSource other)
    {
        if (other == null) return false;

        boolean changed = false;
        for (int index = 0; index < other.mUids.size(); index++)
        {
            int position = indexOf(other.mUids.get(index), other.mNames.get(index));
            if (position < 0) continue;

            mUids.remove(position);
            mNames.remove(position);
            changed = true;
        }
        return changed;
    }

    private boolean addEntry(int uid, String packageName)
    {
        if (indexOf(uid, packageName) >= 0) return false;

        mUids.add(uid);
        mNames.add(packageName);
        return true;
    }

    private int indexOf(int uid, String packageName)
    {
        for (int index = 0; index < mUids.size(); index++)
        {
            if (mUids.get(index) != uid) continue;

            String name = mNames.get(index);
            if (name == null ? packageName == null : name.equals(packageName)) return index;
        }
        return -1;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof WorkSource)) return false;

        WorkSource workSource = (WorkSource)other;
        if (workSource.mUids.size() != mUids.size()) return false;

        for (int index = 0; index < mUids.size(); index++)
        {
            if (workSource.indexOf(mUids.get(index), mNames.get(index)) < 0) return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 0;
        for (int index = 0; index < mUids.size(); index++)
        {
            String name = mNames.get(index);
            hash += mUids.get(index) * 31 + (name == null ? 0 : name.hashCode());
        }
        return hash;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mUids.size());
        for (int index = 0; index < mUids.size(); index++)
        {
            dest.writeInt(mUids.get(index));
            dest.writeString(mNames.get(index));
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("WorkSource{");
        for (int index = 0; index < mUids.size(); index++)
        {
            if (index > 0) builder.append(", ");
            builder.append(mUids.get(index));
            if (mNames.get(index) != null) builder.append(' ').append(mNames.get(index));
        }
        return builder.append('}').toString();
    }
}
