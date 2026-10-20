package android.content;

public class Intent implements android.os.Parcelable
{
    public static final Creator<Intent> CREATOR = new Creator<Intent>()
    {
        @Override
        public Intent createFromParcel(android.os.Parcel source)
        {
            return new Intent();
        }

        @Override
        public Intent[] newArray(int size)
        {
            return new Intent[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel destination, int flags)
    {
    }

    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String ACTION_EDIT = "android.intent.action.EDIT";
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
    public static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    public static final String ACTION_TIME_TICK = "android.intent.action.TIME_TICK";
    public static final String ACTION_LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED";
    public static final String ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";

    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_HOME = "android.intent.category.HOME";

    public static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    public static final String ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED";
    public static final String ACTION_PACKAGE_CHANGED = "android.intent.action.PACKAGE_CHANGED";
    public static final String ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final String CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE";

    public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
    public static final String EXTRA_SUBJECT = "android.intent.extra.SUBJECT";

    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    public static final int FLAG_ACTIVITY_NO_HISTORY = 0x40000000;
    public static final int FLAG_RECEIVER_FOREGROUND = 0x10000000;
    public static final int FLAG_GRANT_READ_URI_PERMISSION = 0x00000001;
    public static final int FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002;

    private String mAction;
    private String mType;
    private ComponentName mComponent;
    private android.os.Bundle mExtras;
    private android.net.Uri mData;
    private int mFlags;
    private final java.util.LinkedHashSet<String> mCategories = new java.util.LinkedHashSet<>();
    private String mPackage;

    public Intent()
    {
    }

    public Intent(String action)
    {
        mAction = action;
    }

    public Intent(String action, android.net.Uri data)
    {
        mAction = action;
        mData = data;
    }

    public Intent(Intent other)
    {
        mAction = other.mAction;
        mType = other.mType;
        mComponent = other.mComponent;
        mData = other.mData;
        mFlags = other.mFlags;
        mPackage = other.mPackage;
        mCategories.addAll(other.mCategories);
        if (other.mExtras != null) mExtras = new android.os.Bundle(other.mExtras);
    }

    public Intent(Context packageContext, Class<?> target)
    {
        mComponent = new ComponentName(packageContext == null ? "" : packageContext.getPackageName(), target.getName());
    }

    public Intent setClass(Context packageContext, Class<?> target)
    {
        mComponent = new ComponentName(packageContext == null ? "" : packageContext.getPackageName(), target.getName());
        return this;
    }

    public android.net.Uri getData()
    {
        return mData;
    }

    public String getDataString()
    {
        return mData == null ? null : mData.toString();
    }

    public String getScheme()
    {
        return mData == null ? null : mData.getScheme();
    }

    public Intent setData(android.net.Uri data)
    {
        mData = data;
        return this;
    }

    public Intent setDataAndType(android.net.Uri data, String type)
    {
        mData = data;
        mType = type;
        return this;
    }

    public Intent addCategory(String category)
    {
        if (category != null) mCategories.add(category);
        return this;
    }

    public Intent removeCategory(String category)
    {
        mCategories.remove(category);
        return this;
    }

    public boolean hasCategory(String category)
    {
        return mCategories.contains(category);
    }

    public java.util.Set<String> getCategories()
    {
        return mCategories.isEmpty() ? null : mCategories;
    }

    public int getFlags()
    {
        return mFlags;
    }

    public Intent setFlags(int flags)
    {
        mFlags = flags;
        return this;
    }

    public Intent addFlags(int flags)
    {
        mFlags |= flags;
        return this;
    }

    public String getPackage()
    {
        return mPackage;
    }

    public Intent setPackage(String packageName)
    {
        mPackage = packageName;
        return this;
    }

    public Intent putExtras(android.os.Bundle extras)
    {
        if (extras != null) ensureExtras().putAll(extras);
        return this;
    }

    public Intent putExtra(String name, android.os.Bundle value)
    {
        ensureExtras().putBundle(name, value);
        return this;
    }

    public android.os.Bundle getBundleExtra(String name)
    {
        return mExtras == null ? null : mExtras.getBundle(name);
    }

    public Intent removeExtra(String name)
    {
        if (mExtras != null) mExtras.remove(name);
        return this;
    }

    @Override
    public String toString()
    {
        StringBuilder described = new StringBuilder("Intent { ");
        if (mAction != null) described.append("act=").append(mAction).append(' ');
        if (!mCategories.isEmpty()) described.append("cat=").append(mCategories).append(' ');
        if (mData != null) described.append("dat=").append(mData).append(' ');
        if (mType != null) described.append("typ=").append(mType).append(' ');
        if (mFlags != 0) described.append("flg=0x").append(Integer.toHexString(mFlags)).append(' ');
        if (mComponent != null) described.append("cmp=").append(mComponent.flattenToShortString()).append(' ');
        return described.append('}').toString();
    }

    public String getAction()
    {
        return mAction;
    }

    public Intent setAction(String action)
    {
        mAction = action;
        return this;
    }

    public ComponentName getComponent()
    {
        return mComponent;
    }

    public Intent setComponent(ComponentName component)
    {
        mComponent = component;
        return this;
    }

    public Intent setClassName(String packageName, String className)
    {
        mComponent = new ComponentName(packageName, className);
        return this;
    }

    public String getType()
    {
        return mType;
    }

    public Intent setType(String type)
    {
        mType = type;
        return this;
    }

    public android.os.Bundle getExtras()
    {
        return mExtras == null ? null : new android.os.Bundle(mExtras);
    }

    public Intent replaceExtras(android.os.Bundle extras)
    {
        mExtras = extras == null ? null : new android.os.Bundle(extras);
        return this;
    }

    public boolean hasExtra(String name)
    {
        return mExtras != null && mExtras.containsKey(name);
    }

    public String getStringExtra(String name)
    {
        return mExtras == null ? null : mExtras.getString(name);
    }

    public boolean getBooleanExtra(String name, boolean defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getBoolean(name, defaultValue);
    }

    public int getIntExtra(String name, int defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getInt(name, defaultValue);
    }

    public long getLongExtra(String name, long defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getLong(name, defaultValue);
    }

    public float getFloatExtra(String name, float defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getFloat(name, defaultValue);
    }

    public Intent putExtra(String name, String value)
    {
        ensureExtras().putString(name, value);
        return this;
    }

    public Intent putExtra(String name, boolean value)
    {
        ensureExtras().putBoolean(name, value);
        return this;
    }

    public Intent putExtra(String name, int value)
    {
        ensureExtras().putInt(name, value);
        return this;
    }

    public Intent putExtra(String name, long value)
    {
        ensureExtras().putLong(name, value);
        return this;
    }

    public Intent putExtra(String name, float value)
    {
        ensureExtras().putFloat(name, value);
        return this;
    }

    private android.os.Bundle ensureExtras()
    {
        if (mExtras == null) mExtras = new android.os.Bundle();

        return mExtras;
    }
}
