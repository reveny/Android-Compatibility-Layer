package android.content.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ShortcutInfo implements Parcelable
{
    public static final int DISABLED_REASON_NOT_DISABLED = 0;
    public static final int DISABLED_REASON_BY_APP = 1;
    public static final int DISABLED_REASON_APP_CHANGED = 2;
    public static final int DISABLED_REASON_UNKNOWN = 3;
    public static final int DISABLED_REASON_VERSION_LOWER = 100;
    public static final int DISABLED_REASON_BACKUP_NOT_SUPPORTED = 101;
    public static final int DISABLED_REASON_SIGNATURE_MISMATCH = 102;
    public static final int DISABLED_REASON_OTHER_RESTORE_ISSUE = 103;

    public static final String SHORTCUT_CATEGORY_CONVERSATION = "android.shortcut.conversation";
    public static final int SURFACE_LAUNCHER = 1;

    public static final Parcelable.Creator<ShortcutInfo> CREATOR = new Parcelable.Creator<ShortcutInfo>()
    {
        @Override
        public ShortcutInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof ShortcutInfo ? (ShortcutInfo)value : new ShortcutInfo(null, null);
        }

        @Override
        public ShortcutInfo[] newArray(int size)
        {
            return new ShortcutInfo[size];
        }
    };

    private final String mId;
    private final String mPackageName;
    private ComponentName mActivity;
    private CharSequence mShortLabel;
    private CharSequence mLongLabel;
    private CharSequence mDisabledMessage;
    private android.graphics.drawable.Icon mIcon;
    private android.content.LocusId mLocusId;
    private Set<String> mCategories;
    private Intent[] mIntents;
    private android.app.Person[] mPersons;
    private PersistableBundle mExtras;
    private int mRank;
    private int mStartingTheme;
    private int mExcludedSurfaces;
    private boolean mLongLived;
    private boolean mDynamic;
    private boolean mPinned;
    private boolean mEnabled = true;
    private int mDisabledReason = DISABLED_REASON_NOT_DISABLED;
    private long mLastChangedTimestamp;

    ShortcutInfo(String id, String packageName)
    {
        mId = id;
        mPackageName = packageName;
        mLastChangedTimestamp = System.currentTimeMillis();
    }

    public String getId()
    {
        return mId;
    }

    public android.content.LocusId getLocusId()
    {
        return mLocusId;
    }

    public String getPackage()
    {
        return mPackageName;
    }

    public ComponentName getActivity()
    {
        return mActivity;
    }

    public CharSequence getShortLabel()
    {
        return mShortLabel;
    }

    public CharSequence getLongLabel()
    {
        return mLongLabel;
    }

    public CharSequence getDisabledMessage()
    {
        return mDisabledMessage;
    }

    public int getDisabledReason()
    {
        return mDisabledReason;
    }

    public Set<String> getCategories()
    {
        return mCategories;
    }

    public Intent getIntent()
    {
        if (mIntents == null || mIntents.length == 0) return null;

        return new Intent(mIntents[mIntents.length - 1]);
    }

    public Intent[] getIntents()
    {
        if (mIntents == null) return null;

        Intent[] copies = new Intent[mIntents.length];
        for (int index = 0; index < mIntents.length; index++) copies[index] = new Intent(mIntents[index]);

        return copies;
    }

    public int getRank()
    {
        return mRank;
    }

    public PersistableBundle getExtras()
    {
        return mExtras;
    }

    public UserHandle getUserHandle()
    {
        return android.os.Process.myUserHandle();
    }

    public long getLastChangedTimestamp()
    {
        return mLastChangedTimestamp;
    }

    public boolean isCached()
    {
        return false;
    }

    public boolean isDynamic()
    {
        return mDynamic;
    }

    public boolean isPinned()
    {
        return mPinned;
    }

    public boolean isDeclaredInManifest()
    {
        return false;
    }

    public boolean isImmutable()
    {
        return false;
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    public boolean hasKeyFieldsOnly()
    {
        return false;
    }

    public boolean isExcludedFromSurfaces(int surface)
    {
        return (mExcludedSurfaces & surface) != 0;
    }

    public int getExcludedFromSurfaces()
    {
        return mExcludedSurfaces;
    }

    void setDynamic(boolean dynamic)
    {
        mDynamic = dynamic;
        mLastChangedTimestamp = System.currentTimeMillis();
    }

    void setEnabled(boolean enabled, CharSequence disabledMessage)
    {
        mEnabled = enabled;
        mDisabledReason = enabled ? DISABLED_REASON_NOT_DISABLED : DISABLED_REASON_BY_APP;
        if (disabledMessage != null) mDisabledMessage = disabledMessage;
        mLastChangedTimestamp = System.currentTimeMillis();
    }

    void copyNonNullFieldsFrom(ShortcutInfo source)
    {
        if (source.mActivity != null) mActivity = source.mActivity;
        if (source.mShortLabel != null) mShortLabel = source.mShortLabel;
        if (source.mLongLabel != null) mLongLabel = source.mLongLabel;
        if (source.mDisabledMessage != null) mDisabledMessage = source.mDisabledMessage;
        if (source.mIcon != null) mIcon = source.mIcon;
        if (source.mLocusId != null) mLocusId = source.mLocusId;
        if (source.mCategories != null) mCategories = source.mCategories;
        if (source.mIntents != null) mIntents = source.mIntents;
        if (source.mPersons != null) mPersons = source.mPersons;
        if (source.mExtras != null) mExtras = source.mExtras;
        if (source.mRank != 0) mRank = source.mRank;
        mLastChangedTimestamp = System.currentTimeMillis();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(this);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "ShortcutInfo {id=" + mId + ", packageName=" + mPackageName + ", shortLabel=" + mShortLabel + "}";
    }

    public static class Builder
    {
        private final ShortcutInfo mShortcut;

        public Builder(android.content.Context context, String id)
        {
            if (id == null || id.isEmpty()) throw new IllegalArgumentException("id cannot be empty");

            mShortcut = new ShortcutInfo(id, context == null ? null : context.getPackageName());
        }

        public Builder setLocusId(android.content.LocusId locusId)
        {
            if (locusId == null) throw new NullPointerException("locusId cannot be null");

            mShortcut.mLocusId = locusId;
            return this;
        }

        public Builder setActivity(ComponentName activity)
        {
            if (activity == null) throw new NullPointerException("activity cannot be null");

            mShortcut.mActivity = activity;
            return this;
        }

        public Builder setIcon(android.graphics.drawable.Icon icon)
        {
            mShortcut.mIcon = icon;
            return this;
        }

        public Builder setStartingTheme(int themeResourceId)
        {
            mShortcut.mStartingTheme = themeResourceId;
            return this;
        }

        public Builder setShortLabel(CharSequence shortLabel)
        {
            if (shortLabel == null || shortLabel.length() == 0) throw new IllegalArgumentException("shortLabel cannot be empty");

            mShortcut.mShortLabel = shortLabel;
            return this;
        }

        public Builder setLongLabel(CharSequence longLabel)
        {
            if (longLabel == null || longLabel.length() == 0) throw new IllegalArgumentException("longLabel cannot be empty");

            mShortcut.mLongLabel = longLabel;
            return this;
        }

        public Builder setDisabledMessage(CharSequence disabledMessage)
        {
            if (disabledMessage == null || disabledMessage.length() == 0)
            {
                throw new IllegalArgumentException("disabledMessage cannot be empty");
            }

            mShortcut.mDisabledMessage = disabledMessage;
            return this;
        }

        public Builder setCategories(Set<String> categories)
        {
            mShortcut.mCategories = categories == null ? null : new HashSet<>(categories);
            return this;
        }

        public Builder setIntent(Intent intent)
        {
            return setIntents(new Intent[] {intent});
        }

        public Builder setIntents(Intent[] intents)
        {
            if (intents == null || intents.length == 0) throw new IllegalArgumentException("intents cannot be empty");

            List<Intent> copies = new ArrayList<>();

            for (Intent intent : intents)
            {
                if (intent == null) throw new NullPointerException("intents cannot contain null");
                if (intent.getAction() == null) throw new NullPointerException("intent's action must be set");

                copies.add(new Intent(intent));
            }

            mShortcut.mIntents = copies.toArray(new Intent[0]);
            return this;
        }

        public Builder setPerson(android.app.Person person)
        {
            return setPersons(new android.app.Person[] {person});
        }

        public Builder setPersons(android.app.Person[] persons)
        {
            mShortcut.mPersons = persons == null ? null : persons.clone();
            return this;
        }

        public Builder setLongLived(boolean longLived)
        {
            mShortcut.mLongLived = longLived;
            return this;
        }

        public Builder setRank(int rank)
        {
            if (rank < 0) throw new IllegalArgumentException("Rank cannot be negative");

            mShortcut.mRank = rank;
            return this;
        }

        public Builder setExtras(PersistableBundle extras)
        {
            mShortcut.mExtras = extras;
            return this;
        }

        public Builder setExcludedFromSurfaces(int surfaces)
        {
            mShortcut.mExcludedSurfaces = surfaces;
            return this;
        }

        public ShortcutInfo build()
        {
            return mShortcut;
        }
    }
}
