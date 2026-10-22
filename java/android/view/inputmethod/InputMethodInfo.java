package android.view.inputmethod;

import android.content.ComponentName;
import android.content.pm.ServiceInfo;
import android.os.Parcel;
import android.os.Parcelable;

public final class InputMethodInfo implements Parcelable
{
    public static final String ACTION_STYLUS_HANDWRITING_SETTINGS = "android.view.inputmethod.action.STYLUS_HANDWRITING_SETTINGS";

    private final String mId;
    private final String mPackageName;
    private final String mServiceName;
    private final CharSequence mLabel;
    private final String mSettingsActivity;
    private final ServiceInfo mServiceInfo;

    public static final String ACTION_IME_LANGUAGE_SETTINGS = "android.view.inputmethod.action.IME_LANGUAGE_SETTINGS";

    public InputMethodInfo(String packageName, String serviceName, String id)
    {
        this(packageName, serviceName, null, null, id);
    }

    public InputMethodInfo(android.content.Context context, android.content.pm.ResolveInfo service)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        this(service.serviceInfo.packageName, service.serviceInfo.name, service.loadLabel(context.getPackageManager()), null, null);
    }

    public android.content.Intent createImeLanguageSettingsActivityIntent()
    {
        return null;
    }

    public android.content.Intent createStylusHandwritingSettingsActivityIntent()
    {
        return null;
    }

    public boolean supportsConnectionlessStylusHandwriting()
    {
        return false;
    }

    public void dump(android.util.Printer printer, String prefix)
    {
        printer.println(prefix + "mId=" + mId + " mSettingsActivity=" + mSettingsActivity);
        printer.println(prefix + "Service:");
        printer.println(prefix + "  packageName=" + mPackageName + " name=" + mServiceName);
    }

    public InputMethodInfo(String packageName, String className, CharSequence label, String settingsActivity)
    {
        this(packageName, className, label, settingsActivity, null);
    }

    private InputMethodInfo(String packageName, String serviceName, CharSequence label, String settingsActivity, String id)
    {
        mPackageName = packageName;
        mServiceName = serviceName;
        mLabel = label;
        mSettingsActivity = settingsActivity;
        mId = id != null ? id : packageName + "/" + serviceName;

        mServiceInfo = new ServiceInfo();
        mServiceInfo.packageName = packageName;
        mServiceInfo.name = serviceName;
        mServiceInfo.nonLocalizedLabel = label;
        mServiceInfo.exported = true;
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

    public ServiceInfo getServiceInfo()
    {
        return mServiceInfo;
    }

    public ComponentName getComponent()
    {
        return new ComponentName(mPackageName, mServiceName);
    }

    public CharSequence loadLabel(android.content.pm.PackageManager packageManager)
    {
        return mLabel != null ? mLabel : mServiceName;
    }

    public android.graphics.drawable.Drawable loadIcon(android.content.pm.PackageManager packageManager)
    {
        return null;
    }

    public String getSettingsActivity()
    {
        return mSettingsActivity;
    }

    public int getSubtypeCount()
    {
        return 0;
    }

    public InputMethodSubtype getSubtypeAt(int index)
    {
        throw new IndexOutOfBoundsException("index=" + index);
    }

    public int getIsDefaultResourceId()
    {
        return 0;
    }

    public int getConfigChanges()
    {
        return 0;
    }

    public boolean supportsStylusHandwriting()
    {
        return false;
    }

    public boolean suppressesSpellChecker()
    {
        return false;
    }

    public boolean shouldShowInInputMethodPicker()
    {
        return true;
    }

    public boolean isDefault(android.content.Context context)
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "InputMethodInfo{" + mId + ", settings: " + mSettingsActivity + "}";
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof InputMethodInfo)) return false;

        return mId.equals(((InputMethodInfo)other).mId);
    }

    @Override
    public int hashCode()
    {
        return mId.hashCode();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mPackageName);
        dest.writeString(mServiceName);
        dest.writeValue(mLabel);
        dest.writeString(mSettingsActivity);
        dest.writeString(mId);
    }

    public static final Creator<InputMethodInfo> CREATOR = new Creator<InputMethodInfo>()
    {
        @Override
        public InputMethodInfo createFromParcel(Parcel source)
        {
            String packageName = source.readString();
            String serviceName = source.readString();
            CharSequence label = (CharSequence)source.readValue(null);
            String settingsActivity = source.readString();
            String id = source.readString();
            return new InputMethodInfo(packageName, serviceName, label, settingsActivity, id);
        }

        @Override
        public InputMethodInfo[] newArray(int size)
        {
            return new InputMethodInfo[size];
        }
    };
}
