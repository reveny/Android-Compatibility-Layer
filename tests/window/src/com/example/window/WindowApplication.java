package com.example.window;

import android.app.Application;
import android.util.Log;

public class WindowApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i("Pattern", "WindowApplication.onCreate package=" + getPackageName());
    }
}
