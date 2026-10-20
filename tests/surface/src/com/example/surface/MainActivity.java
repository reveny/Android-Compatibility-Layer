package com.example.surface;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends Activity implements SurfaceHolder.Callback
{
    private static final String TAG = "SurfaceProbe";

    private int mChangedCount;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        try
        {
            System.loadLibrary("presence");
            Log.i(TAG, "the guest library is loaded, so the surface bridge is installed");
        }
        catch (Throwable missing)
        {
            Log.e(TAG, "the guest library did not load", missing);
        }

        SurfaceView view = new SurfaceView(this);
        view.getHolder().addCallback(this);
        setContentView(view);

        Log.i(TAG, "the content view is a SurfaceView and the callback is registered");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        Log.i(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        mChangedCount += 1;

        Log.i(TAG, "surfaceChanged " + width + "x" + height + " format " + format);

        if (mChangedCount == 1) finish();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        Log.i(TAG, "surfaceDestroyed");
    }
}
