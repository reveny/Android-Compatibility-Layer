package android.app;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class WallpaperManager
{
    public static final int FLAG_SYSTEM = 1 << 0;
    public static final int FLAG_LOCK = 1 << 1;

    private final Context mContext;

    private WallpaperManager(Context context)
    {
        mContext = context;
    }

    public static WallpaperManager getInstance(Context context)
    {
        return new WallpaperManager(context);
    }

    public Drawable getDrawable()
    {
        return null;
    }

    public Drawable peekDrawable()
    {
        return null;
    }

    public Drawable getFastDrawable()
    {
        return null;
    }

    public Drawable peekFastDrawable()
    {
        return null;
    }

    public int getDesiredMinimumWidth()
    {
        return mContext == null ? 0 : mContext.getResources().getDisplayMetrics().widthPixels;
    }

    public int getDesiredMinimumHeight()
    {
        return mContext == null ? 0 : mContext.getResources().getDisplayMetrics().heightPixels;
    }

    public void suggestDesiredDimensions(int width, int height)
    {
    }

    public void setWallpaperOffsets(android.os.IBinder windowToken, float x, float y)
    {
    }

    public void setWallpaperOffsetSteps(float x, float y)
    {
    }

    public void sendWallpaperCommand(android.os.IBinder windowToken, String action, int x, int y, int z, android.os.Bundle extras)
    {
    }

    public android.app.WallpaperInfo getWallpaperInfo()
    {
        return null;
    }

    public boolean isWallpaperSupported()
    {
        return false;
    }

    public boolean isSetWallpaperAllowed()
    {
        return false;
    }

    public int getWallpaperId(int which)
    {
        return -1;
    }

    public interface OnColorsChangedListener
    {
        void onColorsChanged(WallpaperColors colors, int which);
    }

    private final java.util.ArrayList<OnColorsChangedListener> mColorListeners = new java.util.ArrayList<>();

    public WallpaperColors getWallpaperColors(int which)
    {
        return null;
    }

    public void addOnColorsChangedListener(OnColorsChangedListener listener, android.os.Handler handler)
    {
        if (listener != null) mColorListeners.add(listener);
    }

    public void removeOnColorsChangedListener(OnColorsChangedListener listener)
    {
        mColorListeners.remove(listener);
    }

    public void clear() throws java.io.IOException
    {
    }

    public void clear(int which) throws java.io.IOException
    {
    }
}
