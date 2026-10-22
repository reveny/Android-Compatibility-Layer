package android.media;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.Settings;

import java.io.FileNotFoundException;

public class RingtoneManager
{
    public static final String ACTION_RINGTONE_PICKER = "android.intent.action.RINGTONE_PICKER";
    public static final String EXTRA_RINGTONE_DEFAULT_URI = "android.intent.extra.ringtone.DEFAULT_URI";
    public static final String EXTRA_RINGTONE_EXISTING_URI = "android.intent.extra.ringtone.EXISTING_URI";
    public static final String EXTRA_RINGTONE_INCLUDE_DRM = "android.intent.extra.ringtone.INCLUDE_DRM";
    public static final String EXTRA_RINGTONE_PICKED_URI = "android.intent.extra.ringtone.PICKED_URI";
    public static final String EXTRA_RINGTONE_SHOW_DEFAULT = "android.intent.extra.ringtone.SHOW_DEFAULT";
    public static final String EXTRA_RINGTONE_SHOW_SILENT = "android.intent.extra.ringtone.SHOW_SILENT";
    public static final String EXTRA_RINGTONE_TITLE = "android.intent.extra.ringtone.TITLE";
    public static final String EXTRA_RINGTONE_TYPE = "android.intent.extra.ringtone.TYPE";
    public static final int ID_COLUMN_INDEX = 0;
    public static final int TITLE_COLUMN_INDEX = 1;
    public static final int TYPE_ALARM = 4;
    public static final int TYPE_ALL = 7;
    public static final int TYPE_NOTIFICATION = 2;
    public static final int TYPE_RINGTONE = 1;
    public static final int URI_COLUMN_INDEX = 2;

    private static final String[] COLUMNS = {"_id", "title", "uri", "title_key"};

    private final Context mContext;
    private int mType = TYPE_RINGTONE;
    private boolean mIncludeDrm;
    private boolean mStopPreviousRingtone = true;
    private Ringtone mPreviousRingtone;

    public RingtoneManager(Activity activity)
    {
        mContext = activity;
    }

    public RingtoneManager(Context context)
    {
        mContext = context;
    }

    public static Uri getActualDefaultRingtoneUri(Context context, int type)
    {
        String setting = settingForType(type);
        if (setting == null || context == null) return null;

        String value = Settings.System.getString(context.getContentResolver(), setting);
        return value == null ? null : Uri.parse(value);
    }

    public Cursor getCursor()
    {
        return new MatrixCursor(COLUMNS);
    }

    public static int getDefaultType(Uri defaultRingtoneUri)
    {
        if (defaultRingtoneUri == null) return -1;
        if (defaultRingtoneUri.equals(Settings.System.DEFAULT_RINGTONE_URI)) return TYPE_RINGTONE;
        if (defaultRingtoneUri.equals(Settings.System.DEFAULT_NOTIFICATION_URI)) return TYPE_NOTIFICATION;
        if (defaultRingtoneUri.equals(Settings.System.DEFAULT_ALARM_ALERT_URI)) return TYPE_ALARM;
        return -1;
    }

    public static Uri getDefaultUri(int type)
    {
        if ((type & TYPE_RINGTONE) != 0) return Settings.System.DEFAULT_RINGTONE_URI;
        if ((type & TYPE_NOTIFICATION) != 0) return Settings.System.DEFAULT_NOTIFICATION_URI;
        if ((type & TYPE_ALARM) != 0) return Settings.System.DEFAULT_ALARM_ALERT_URI;
        return null;
    }

    public boolean getIncludeDrm()
    {
        return mIncludeDrm;
    }

    public static Ringtone getRingtone(Context context, Uri ringtoneUri)
    {
        return new Ringtone(ringtoneUri);
    }

    public Ringtone getRingtone(int position)
    {
        if (mStopPreviousRingtone && mPreviousRingtone != null) mPreviousRingtone.stop();

        Uri uri = getRingtoneUri(position);
        if (uri == null) return null;

        mPreviousRingtone = new Ringtone(uri);
        return mPreviousRingtone;
    }

    public int getRingtonePosition(Uri ringtoneUri)
    {
        return -1;
    }

    public Uri getRingtoneUri(int position)
    {
        return null;
    }

    public boolean getStopPreviousRingtone()
    {
        return mStopPreviousRingtone;
    }

    public static Uri getValidRingtoneUri(Context context)
    {
        return null;
    }

    public static boolean hasHapticChannels(Context context, Uri ringtoneUri)
    {
        return false;
    }

    public static boolean hasHapticChannels(Uri ringtoneUri)
    {
        return false;
    }

    public boolean hasHapticChannels(int position)
    {
        return false;
    }

    public int inferStreamType()
    {
        switch (mType)
        {
            case TYPE_ALARM:
                return AudioManager.STREAM_ALARM;
            case TYPE_NOTIFICATION:
                return AudioManager.STREAM_NOTIFICATION;
            default:
                return AudioManager.STREAM_RING;
        }
    }

    public static boolean isDefault(Uri ringtoneUri)
    {
        return getDefaultType(ringtoneUri) != -1;
    }

    public static AssetFileDescriptor openDefaultRingtoneUri(Context context, Uri uri) throws FileNotFoundException
    {
        int type = getDefaultType(uri);
        Uri actual = getActualDefaultRingtoneUri(context, type);
        if (actual == null) throw new FileNotFoundException("No default ringtone set for " + uri);

        return context.getContentResolver().openAssetFileDescriptor(actual, "r");
    }

    public static void setActualDefaultRingtoneUri(Context context, int type, Uri ringtoneUri)
    {
        String setting = settingForType(type);
        if (setting == null || context == null) return;

        Settings.System.putString(context.getContentResolver(), setting, ringtoneUri == null ? null : ringtoneUri.toString());
    }

    public void setIncludeDrm(boolean includeDrm)
    {
        mIncludeDrm = includeDrm;
    }

    public void setStopPreviousRingtone(boolean stopPreviousRingtone)
    {
        mStopPreviousRingtone = stopPreviousRingtone;
    }

    public void setType(int type)
    {
        mType = type;
    }

    public void stopPreviousRingtone()
    {
        if (mPreviousRingtone != null) mPreviousRingtone.stop();
    }

    private static String settingForType(int type)
    {
        if ((type & TYPE_RINGTONE) != 0) return Settings.System.RINGTONE;
        if ((type & TYPE_NOTIFICATION) != 0) return Settings.System.NOTIFICATION_SOUND;
        if ((type & TYPE_ALARM) != 0) return Settings.System.ALARM_ALERT;
        return null;
    }
}
