package android.app;

public class NotificationChannel
{
    private final String mId;
    private CharSequence mName;
    private int mImportance;
    private String mDescription;
    private String mGroup;
    private boolean mShowBadge = true;
    private boolean mVibration;
    private long[] mVibrationPattern;

    public NotificationChannel(String id, CharSequence name, int importance)
    {
        mId = id;
        mName = name;
        mImportance = importance;
    }

    public String getId()
    {
        return mId;
    }

    public CharSequence getName()
    {
        return mName;
    }

    public void setName(CharSequence name)
    {
        mName = name;
    }

    public int getImportance()
    {
        return mImportance;
    }

    public void setImportance(int importance)
    {
        mImportance = importance;
    }

    public String getDescription()
    {
        return mDescription;
    }

    public void setDescription(String description)
    {
        mDescription = description;
    }

    public String getGroup()
    {
        return mGroup;
    }

    public void setGroup(String group)
    {
        mGroup = group;
    }

    public boolean canShowBadge()
    {
        return mShowBadge;
    }

    public void setShowBadge(boolean showBadge)
    {
        mShowBadge = showBadge;
    }

    public boolean shouldVibrate()
    {
        return mVibration;
    }

    public void enableVibration(boolean vibration)
    {
        mVibration = vibration;
    }

    public long[] getVibrationPattern()
    {
        return mVibrationPattern;
    }

    public void setVibrationPattern(long[] pattern)
    {
        mVibrationPattern = pattern;
    }

    public void enableLights(boolean lights)
    {
    }

    public void setLightColor(int color)
    {
    }

    public void setSound(android.net.Uri sound, Object attributes)
    {
    }

    public void setLockscreenVisibility(int visibility)
    {
    }
}
