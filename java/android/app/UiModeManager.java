package android.app;

import android.content.Context;
import android.content.res.Configuration;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class UiModeManager
{
    public static String ACTION_ENTER_CAR_MODE = "android.app.action.ENTER_CAR_MODE";
    public static String ACTION_ENTER_DESK_MODE = "android.app.action.ENTER_DESK_MODE";
    public static String ACTION_EXIT_CAR_MODE = "android.app.action.EXIT_CAR_MODE";
    public static String ACTION_EXIT_DESK_MODE = "android.app.action.EXIT_DESK_MODE";

    public static final int DISABLE_CAR_MODE_GO_HOME = 1;
    public static final int ENABLE_CAR_MODE_ALLOW_SLEEP = 2;
    public static final int ENABLE_CAR_MODE_GO_CAR_HOME = 1;

    public static final int MODE_NIGHT_AUTO = 0;
    public static final int MODE_NIGHT_NO = 1;
    public static final int MODE_NIGHT_YES = 2;
    public static final int MODE_NIGHT_CUSTOM = 3;

    private static final int MODE_NIGHT_UNSET = -1;

    private final Context mContext;
    private final Map<ContrastChangeListener, Executor> mContrastListeners = new HashMap<>();
    private int mNightMode = MODE_NIGHT_UNSET;
    private LocalTime mCustomNightStart = LocalTime.of(22, 0);
    private LocalTime mCustomNightEnd = LocalTime.of(6, 0);

    public UiModeManager(Context context)
    {
        mContext = context;
    }

    public void enableCarMode(int flags)
    {
    }

    public void disableCarMode(int flags)
    {
    }

    public int getCurrentModeType()
    {
        int type = configurationUiMode() & Configuration.UI_MODE_TYPE_MASK;
        return type == Configuration.UI_MODE_TYPE_UNDEFINED ? Configuration.UI_MODE_TYPE_NORMAL : type;
    }

    public void setNightMode(int mode)
    {
        mNightMode = mode;
    }

    public void setApplicationNightMode(int mode)
    {
        mNightMode = mode;
    }

    public int getNightMode()
    {
        if (mNightMode != MODE_NIGHT_UNSET) return mNightMode;

        int night = configurationUiMode() & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES ? MODE_NIGHT_YES : MODE_NIGHT_NO;
    }

    public LocalTime getCustomNightModeStart()
    {
        return mCustomNightStart;
    }

    public void setCustomNightModeStart(LocalTime time)
    {
        if (time != null) mCustomNightStart = time;
    }

    public LocalTime getCustomNightModeEnd()
    {
        return mCustomNightEnd;
    }

    public void setCustomNightModeEnd(LocalTime time)
    {
        if (time != null) mCustomNightEnd = time;
    }

    public float getContrast()
    {
        return 0.0f;
    }

    public void addContrastChangeListener(Executor executor, ContrastChangeListener listener)
    {
        if (executor == null || listener == null) throw new NullPointerException("executor and listener are required");

        synchronized (mContrastListeners)
        {
            mContrastListeners.put(listener, executor);
        }
    }

    public void removeContrastChangeListener(ContrastChangeListener listener)
    {
        synchronized (mContrastListeners)
        {
            mContrastListeners.remove(listener);
        }
    }

    private int configurationUiMode()
    {
        if (mContext == null || mContext.getResources() == null) return Configuration.UI_MODE_TYPE_NORMAL | Configuration.UI_MODE_NIGHT_NO;

        Configuration configuration = mContext.getResources().getConfiguration();
        return configuration == null ? Configuration.UI_MODE_TYPE_NORMAL | Configuration.UI_MODE_NIGHT_NO : configuration.uiMode;
    }

    public interface ContrastChangeListener
    {
        void onContrastChanged(float contrast);
    }
}
