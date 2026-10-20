package com.example.jni;

import android.app.Application;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class JniApplication extends Application
{
    private static final String TAG = "Jni";

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i(TAG, "JniApplication.onCreate package=" + getPackageName());
        Log.i(TAG, "nativeLibraryDir=" + getApplicationInfo().nativeLibraryDir);

        System.loadLibrary("acljni");
        Log.i(TAG, "loadLibrary acljni returned");

        Log.i(TAG, "registered upcall = " + NativeBridge.registeredUpcall(21));
        Log.i(TAG, "mangled upcall = " + NativeBridge.manglingUpcall(50));
        Log.i(TAG, "string round trip = " + NativeBridge.stringRoundTrip("acl"));

        NativeBridge target = new NativeBridge(7);
        Log.i(TAG, "instance upcall = " + NativeBridge.instanceUpcall(target, 6));

        byte[] buffer = new byte[] {1, 2, 3, 4};
        int sum = NativeBridge.mutateArray(buffer);
        Log.i(TAG, "array sum = " + sum + " mutated = " + Arrays.toString(buffer));

        Log.i(TAG, "field bump = " + NativeBridge.bumpField(target, 5) + " field = " + target.seed);
        Log.i(TAG, "caught java exception = " + NativeBridge.catchJavaException(9));

        try
        {
            NativeBridge.throwFromNative(3);
            Log.e(TAG, "throwFromNative did not throw");
        }
        catch (IllegalArgumentException expected)
        {
            Log.i(TAG, "caught native exception = " + expected.getMessage());
        }

        Log.i(TAG, "attached thread upcall = " + NativeBridge.attachedThreadUpcall(11));

        exerciseDescriptors();

        Log.i(TAG, "conformance complete");
    }

    private void exerciseDescriptors()
    {
        try (AssetFileDescriptor stored = getAssets().openFd("stored.bin"))
        {
            byte[] buffer = new byte[64];
            int got;
            try (InputStream stream = stored.createInputStream())
            {
                got = stream.read(buffer);
            }

            Log.i(TAG, "openFd stored.bin length=" + stored.getLength() + " offset=" + (stored.getStartOffset() > 0) + " read=" + got + " text="
                    + new String(buffer, 0, got, "UTF-8"));
        }
        catch (IOException failure)
        {
            Log.e(TAG, "openFd stored.bin failed", failure);
        }

        try
        {
            getAssets().openFd("deflated.txt");
            Log.e(TAG, "openFd deflated.txt unexpectedly succeeded");
        }
        catch (FileNotFoundException expected)
        {
            Log.i(TAG, "openFd deflated.txt refused: " + expected.getMessage());
        }
        catch (IOException failure)
        {
            Log.e(TAG, "openFd deflated.txt failed with the wrong exception", failure);
        }

        try
        {
            getAssets().openFd("absent.bin");
            Log.e(TAG, "openFd absent.bin unexpectedly succeeded");
        }
        catch (FileNotFoundException expected)
        {
            Log.i(TAG, "openFd absent.bin refused");
        }
        catch (IOException failure)
        {
            Log.e(TAG, "openFd absent.bin failed with the wrong exception", failure);
        }

        Log.i(TAG, "descriptor fd balance = " + descriptorFdBalance());
    }

    private int descriptorFdBalance()
    {
        int before = openDescriptorCount();

        for (int round = 0; round < 32; round++)
        {
            try (AssetFileDescriptor descriptor = getAssets().openFd("stored.bin"))
            {
                try (InputStream stream = descriptor.createInputStream())
                {
                    stream.read(new byte[8]);
                }
            }
            catch (IOException failure)
            {
                Log.e(TAG, "descriptor round " + round + " failed", failure);
                return -1;
            }
        }

        return openDescriptorCount() - before;
    }

    private int openDescriptorCount()
    {
        String[] entries = new java.io.File("/proc/self/fd").list();
        return entries == null ? -1 : entries.length;
    }
}
