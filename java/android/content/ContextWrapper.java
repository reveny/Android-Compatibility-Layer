package android.content;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

public class ContextWrapper extends Context
{
    private Context mBase;

    public ContextWrapper(Context base)
    {
        mBase = base;
    }

    protected void attachBaseContext(Context base)
    {
        if (mBase != null) throw new IllegalStateException("Base context already set");
        mBase = base;
    }

    void replaceBaseContext(Context base)
    {
        mBase = base;
    }

    public Context getBaseContext()
    {
        return mBase;
    }

    @Override
    public Context getApplicationContext()
    {
        return mBase.getApplicationContext();
    }

    @Override
    public String getPackageName()
    {
        return mBase.getPackageName();
    }

    @Override
    public PackageManager getPackageManager()
    {
        return mBase.getPackageManager();
    }

    @Override
    public ContentResolver getContentResolver()
    {
        return mBase.getContentResolver();
    }

    @Override
    public ApplicationInfo getApplicationInfo()
    {
        return mBase.getApplicationInfo();
    }

    @Override
    public Resources getResources()
    {
        return mBase.getResources();
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return mBase.getClassLoader();
    }

    @Override
    public String getString(int resourceId)
    {
        return mBase.getString(resourceId);
    }

    @Override
    public java.io.File getFilesDir()
    {
        return mBase.getFilesDir();
    }

    @Override
    public java.io.File getCacheDir()
    {
        return mBase.getCacheDir();
    }

    @Override
    public java.io.File getObbDir()
    {
        return mBase.getObbDir();
    }

    @Override
    public java.io.File getExternalFilesDir(String type)
    {
        return mBase.getExternalFilesDir(type);
    }

    @Override
    public java.io.File getExternalCacheDir()
    {
        return mBase.getExternalCacheDir();
    }

    @Override
    public java.io.File getCodeCacheDir()
    {
        return mBase.getCodeCacheDir();
    }

    @Override
    public java.io.File getNoBackupFilesDir()
    {
        return mBase.getNoBackupFilesDir();
    }

    @Override
    public java.io.File getDataDir()
    {
        return mBase.getDataDir();
    }

    @Override
    public android.content.res.AssetManager getAssets()
    {
        return mBase.getAssets();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode)
    {
        return mBase.getSharedPreferences(name, mode);
    }

    @Override
    public java.io.FileInputStream openFileInput(String name) throws java.io.FileNotFoundException
    {
        return mBase.openFileInput(name);
    }

    @Override
    public java.io.FileOutputStream openFileOutput(String name, int mode) throws java.io.FileNotFoundException
    {
        return mBase.openFileOutput(name, mode);
    }

    @Override
    public boolean deleteFile(String name)
    {
        return mBase.deleteFile(name);
    }

    @Override
    public java.io.File getFileStreamPath(String name)
    {
        return mBase.getFileStreamPath(name);
    }

    @Override
    public String[] fileList()
    {
        return mBase.fileList();
    }

    @Override
    public java.io.File getDatabasePath(String name)
    {
        return mBase.getDatabasePath(name);
    }

    @Override
    public java.io.File getDir(String name, int mode)
    {
        return mBase.getDir(name, mode);
    }

    @Override
    public Resources.Theme getTheme()
    {
        return mBase.getTheme();
    }

    @Override
    public void setTheme(int resourceId)
    {
        mBase.setTheme(resourceId);
    }

    @Override
    public Object getSystemService(String name)
    {
        return mBase.getSystemService(name);
    }

    @Override
    public Context createDeviceProtectedStorageContext()
    {
        return mBase.createDeviceProtectedStorageContext();
    }

    public Context createConfigurationContext(android.content.res.Configuration overrideConfiguration)
    {
        return mBase.createConfigurationContext(overrideConfiguration);
    }

    @Override
    public Context createPackageContext(String packageName, int flags)
    {
        return mBase.createPackageContext(packageName, flags);
    }

    @Override
    public Context createWindowContext(int type, android.os.Bundle options)
    {
        return mBase.createWindowContext(type, options);
    }

    @Override
    public Context createWindowContext(android.view.Display display, int type, android.os.Bundle options)
    {
        return mBase.createWindowContext(display, type, options);
    }

    @Override
    public String getSystemServiceName(Class<?> serviceClass)
    {
        return mBase.getSystemServiceName(serviceClass);
    }
}
