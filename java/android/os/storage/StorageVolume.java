package android.os.storage;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.io.File;
import java.util.UUID;

public final class StorageVolume implements Parcelable
{
    public static final String EXTRA_STORAGE_VOLUME = "android.os.storage.extra.STORAGE_VOLUME";

    private static final String PRIMARY_ID = "emulated;0";
    private static final String PRIMARY_DESCRIPTION = "Internal shared storage";
    private static final String MEDIA_STORE_VOLUME_PRIMARY = "external_primary";
    private static final String ACTION_OPEN_DOCUMENT_TREE = "android.intent.action.OPEN_DOCUMENT_TREE";

    public static final Parcelable.Creator<StorageVolume> CREATOR = new Parcelable.Creator<StorageVolume>()
    {
        @Override
        public StorageVolume createFromParcel(Parcel source)
        {
            String id = source.readString();
            String path = source.readString();
            String description = source.readString();
            boolean primary = source.readBoolean();
            boolean removable = source.readBoolean();
            boolean emulated = source.readBoolean();
            String uuid = source.readString();
            String state = source.readString();
            return new StorageVolume(id, new File(path), description, primary, removable, emulated, uuid, state,
                                     android.os.Process.myUserHandle());
        }

        @Override
        public StorageVolume[] newArray(int size)
        {
            return new StorageVolume[size];
        }
    };

    private final String mId;
    private final File mPath;
    private final String mDescription;
    private final boolean mPrimary;
    private final boolean mRemovable;
    private final boolean mEmulated;
    private final String mUuid;
    private final String mState;
    private final UserHandle mOwner;

    StorageVolume(String id, File path, String description, boolean primary, boolean removable, boolean emulated, String uuid,
                  String state, UserHandle owner)
    {
        mId = id;
        mPath = path;
        mDescription = description;
        mPrimary = primary;
        mRemovable = removable;
        mEmulated = emulated;
        mUuid = uuid;
        mState = state;
        mOwner = owner;
    }

    static StorageVolume primary(File path)
    {
        return new StorageVolume(PRIMARY_ID, path, PRIMARY_DESCRIPTION, true, false, true, null, Environment.MEDIA_MOUNTED,
                                 android.os.Process.myUserHandle());
    }

    public String getId()
    {
        return mId;
    }

    public String getPath()
    {
        return mPath.getPath();
    }

    public File getPathFile()
    {
        return mPath;
    }

    public File getDirectory()
    {
        return Environment.MEDIA_MOUNTED.equals(mState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(mState) ? mPath : null;
    }

    public String getDescription(Context context)
    {
        return mDescription;
    }

    public boolean isPrimary()
    {
        return mPrimary;
    }

    public boolean isRemovable()
    {
        return mRemovable;
    }

    public boolean isEmulated()
    {
        return mEmulated;
    }

    public UserHandle getOwner()
    {
        return mOwner;
    }

    public UUID getStorageUuid()
    {
        if (mPrimary) return StorageManager.UUID_DEFAULT;
        if (mUuid == null) return null;

        try
        {
            return UUID.fromString(mUuid);
        }
        catch (IllegalArgumentException notUuid)
        {
            return null;
        }
    }

    public String getUuid()
    {
        return mUuid;
    }

    public String getMediaStoreVolumeName()
    {
        if (mPrimary) return MEDIA_STORE_VOLUME_PRIMARY;
        return mUuid == null ? null : mUuid.toLowerCase(java.util.Locale.ROOT);
    }

    public String getState()
    {
        return mState;
    }

    public Intent createAccessIntent(String directoryName)
    {
        return null;
    }

    public Intent createOpenDocumentTreeIntent()
    {
        return new Intent(ACTION_OPEN_DOCUMENT_TREE);
    }

    @Override
    public boolean equals(Object object)
    {
        if (!(object instanceof StorageVolume)) return false;

        StorageVolume other = (StorageVolume)object;
        return mId.equals(other.mId) && mPath.equals(other.mPath);
    }

    @Override
    public int hashCode()
    {
        return mPath.hashCode();
    }

    @Override
    public String toString()
    {
        return "StorageVolume: " + mDescription + (mUuid == null ? "" : " (" + mUuid + ")");
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mId);
        dest.writeString(mPath.getPath());
        dest.writeString(mDescription);
        dest.writeBoolean(mPrimary);
        dest.writeBoolean(mRemovable);
        dest.writeBoolean(mEmulated);
        dest.writeString(mUuid);
        dest.writeString(mState);
    }
}
