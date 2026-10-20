package com.example.minimal;

import android.app.Application;
import android.util.Log;

public class MinimalApplication extends Application
{
    private static final String TAG = "Minimal";

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i(TAG, "MinimalApplication.onCreate package=" + getPackageName());
        Log.i(TAG, "applicationContext=" + (getApplicationContext() != null));
        Log.i(TAG, "classLoader=" + (getClassLoader() != null));
        Log.i(TAG, "hello from the guest");
    }
}
