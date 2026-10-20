package android.app;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class NativeActivity extends Activity
{
    private static final String TAG = "NativeActivity";
    private static final String LIB_NAME_META_DATA = "android.app.lib_name";
    private static final String DEFAULT_LIB_NAME = "main";

    private static final int CALLBACK_START = 0;
    private static final int CALLBACK_RESUME = 1;
    private static final int CALLBACK_PAUSE = 3;
    private static final int CALLBACK_STOP = 4;
    private static final int CALLBACK_DESTROY = 5;

    private long mHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String libraryName = resolveLibraryName();
        Log.i(TAG, "native library: " + libraryName);

        mHandle = nativeCreate(libraryName, getFilesDir().getAbsolutePath());
        if (mHandle == 0) throw new IllegalStateException("the native activity library " + libraryName + " did not start");
    }

    private String resolveLibraryName()
    {
        ComponentName component = new ComponentName(getPackageName(), getClass().getName());

        ActivityInfo info;
        try
        {
            info = getPackageManager().getActivityInfo(component, PackageManager.GET_META_DATA);
        }
        catch (PackageManager.NameNotFoundException missing)
        {
            Log.w(TAG, "no manifest entry for " + component.getClassName() + "; falling back to " + DEFAULT_LIB_NAME);
            return DEFAULT_LIB_NAME;
        }

        if (info.metaData == null) return DEFAULT_LIB_NAME;

        String name = info.metaData.getString(LIB_NAME_META_DATA);
        return name != null && !name.isEmpty() ? name : DEFAULT_LIB_NAME;
    }

    public void attachWindow(int width, int height)
    {
        if (mHandle == 0) return;

        long window = nativeCreateWindow(width, height);
        if (window == 0)
        {
            Log.e(TAG, "the native activity did not get a window");
            return;
        }

        nativeSetWindow(mHandle, window);
    }

    public void detachWindow()
    {
        if (mHandle != 0) nativeSetWindow(mHandle, 0);
    }

    public boolean isFinishRequested()
    {
        return nativeFinishRequested();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        dispatch(CALLBACK_START);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        dispatch(CALLBACK_RESUME);
    }

    @Override
    protected void onPause()
    {
        dispatch(CALLBACK_PAUSE);
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        dispatch(CALLBACK_STOP);
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        if (mHandle != 0)
        {
            nativeDispatch(mHandle, CALLBACK_DESTROY);
            nativeDestroy(mHandle);
            mHandle = 0;
        }
        super.onDestroy();
    }

    private void dispatch(int callback)
    {
        if (mHandle != 0) nativeDispatch(mHandle, callback);
    }

    private native long nativeCreate(String libraryName, String internalDataPath);

    private native void nativeDispatch(long handle, int callback);

    private native void nativeSetWindow(long handle, long window);

    private native long nativeCreateWindow(int width, int height);

    private native boolean nativeFinishRequested();

    private native void nativeDestroy(long handle);
}
