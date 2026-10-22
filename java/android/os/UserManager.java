package android.os;

import java.util.ArrayList;
import java.util.List;

public class UserManager
{
    private final android.content.Context mContext;

    public UserManager(android.content.Context context)
    {
        mContext = context;
    }

    public boolean isUserUnlocked()
    {
        return true;
    }

    public boolean isUserUnlocked(UserHandle user)
    {
        return true;
    }

    public boolean isQuietModeEnabled(UserHandle user)
    {
        return false;
    }

    public boolean requestQuietModeEnabled(boolean enableQuietMode, UserHandle user)
    {
        return false;
    }

    public boolean isUserRunning(UserHandle user)
    {
        return true;
    }

    public boolean isUserRunningOrStopping(UserHandle user)
    {
        return true;
    }

    public boolean isSystemUser()
    {
        return true;
    }

    public boolean isPrimaryUser()
    {
        return true;
    }

    public boolean isDemoUser()
    {
        return false;
    }

    public boolean isManagedProfile()
    {
        return false;
    }

    public boolean isUserAGoat()
    {
        return false;
    }

    public String getUserName()
    {
        return "Owner";
    }

    public UserHandle getUserForSerialNumber(long serialNumber)
    {
        return Process.myUserHandle();
    }

    public long getSerialNumberForUser(UserHandle user)
    {
        return 0L;
    }

    public List<UserHandle> getUserProfiles()
    {
        List<UserHandle> profiles = new ArrayList<>();
        profiles.add(Process.myUserHandle());

        return profiles;
    }

    public boolean hasUserRestriction(String restrictionKey)
    {
        return false;
    }

    public Bundle getUserRestrictions()
    {
        return new Bundle();
    }

    public Bundle getUserRestrictions(UserHandle user)
    {
        return new Bundle();
    }

    public boolean hasUserRestriction(String restrictionKey, UserHandle user)
    {
        return false;
    }

    public Bundle getApplicationRestrictions(String packageName)
    {
        return new Bundle();
    }

    public boolean isUserForeground()
    {
        return true;
    }

    public boolean isProfile()
    {
        return false;
    }

    public boolean isAdminUser()
    {
        return true;
    }

    public long getUserCreationTime(UserHandle user)
    {
        return 0L;
    }

    public int getUserCount()
    {
        return 1;
    }

    public static boolean supportsMultipleUsers()
    {
        return false;
    }
}
