package android.telecom;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;

import java.util.Objects;

public final class PhoneAccountHandle implements Parcelable
{
    public static final Parcelable.Creator<PhoneAccountHandle> CREATOR = new Parcelable.Creator<PhoneAccountHandle>()
    {
        @Override
        public PhoneAccountHandle createFromParcel(Parcel source)
        {
            ComponentName component = ComponentName.readFromParcel(source);
            String id = source.readString();
            UserHandle user = UserHandle.CREATOR.createFromParcel(source);
            return new PhoneAccountHandle(component, id, user);
        }

        @Override
        public PhoneAccountHandle[] newArray(int size)
        {
            return new PhoneAccountHandle[size];
        }
    };

    private final ComponentName mComponentName;
    private final String mId;
    private final UserHandle mUserHandle;

    public PhoneAccountHandle(ComponentName componentName, String id)
    {
        this(componentName, id, UserHandle.getUserHandleForUid(Process.myUid()));
    }

    public PhoneAccountHandle(ComponentName componentName, String id, UserHandle userHandle)
    {
        if (componentName == null) throw new NullPointerException("componentName");
        if (id == null) throw new NullPointerException("id");
        if (userHandle == null) throw new NullPointerException("userHandle");
        mComponentName = componentName;
        mId = id;
        mUserHandle = userHandle;
    }

    public ComponentName getComponentName()
    {
        return mComponentName;
    }

    public String getId()
    {
        return mId;
    }

    public UserHandle getUserHandle()
    {
        return mUserHandle;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        ComponentName.writeToParcel(mComponentName, dest);
        dest.writeString(mId);
        mUserHandle.writeToParcel(dest, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof PhoneAccountHandle)) return false;
        PhoneAccountHandle handle = (PhoneAccountHandle)other;
        return mComponentName.equals(handle.mComponentName) && mId.equals(handle.mId) && mUserHandle.equals(handle.mUserHandle);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mComponentName, mId, mUserHandle);
    }

    @Override
    public String toString()
    {
        return mComponentName + ", " + mId + ", " + mUserHandle;
    }
}
