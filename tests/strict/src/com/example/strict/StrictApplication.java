package com.example.strict;

import android.app.Application;
import android.util.Log;

public class StrictApplication extends Application
{
    private static final String TAG = "Strict";

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i(TAG, "StrictApplication.onCreate package=" + getPackageName());

        describePermissions();
        describeSignature();
        describeSplits();

        Object service = getSystemService("no-such-service-at-all");
        Log.i(TAG, "survived the guarded call, service=" + service);
    }

    private void describePermissions()
    {
        Log.i(TAG, "INTERNET is " + describe("android.permission.INTERNET"));
        Log.i(TAG, "CAMERA is " + describe("android.permission.CAMERA"));
        Log.i(TAG, "READ_SMS is " + describe("android.permission.READ_SMS"));

        try
        {
            android.content.pm.PackageInfo info =
                getPackageManager().getPackageInfo(getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS);

            Log.i(TAG, "the package reports version " + info.versionName + " code " + info.versionCode + " with "
                       + (info.requestedPermissions == null ? -1 : info.requestedPermissions.length) + " requested permission(s)");
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the package could not describe itself", cause);
        }

        Log.i(TAG, "a touchscreen is " + getPackageManager().hasSystemFeature("android.hardware.touchscreen")
                   + " and a telephone is " + getPackageManager().hasSystemFeature("android.hardware.telephony"));
    }

    private void describeSplits()
    {
        android.content.pm.ApplicationInfo info = getApplicationInfo();
        String[] names = info.splitNames;

        Log.i(TAG, "the package is installed with " + (names == null ? 0 : names.length) + " split(s)");

        if (names != null)
        {
            for (String name : names) Log.i(TAG, "split: " + name);
        }

        try
        {
            java.io.InputStream stream = getAssets().open("from-the-split.txt");
            byte[] buffer = new byte[64];
            int read = stream.read(buffer);
            stream.close();

            Log.i(TAG, "an asset that only the split carries reads back as " + new String(buffer, 0, Math.max(read, 0)).trim());
        }
        catch (Throwable cause)
        {
            Log.i(TAG, "no asset from a split: " + cause.getClass().getSimpleName());
        }

        int identifier = getResources().getIdentifier("split_greeting", "string", getPackageName());

        if (identifier == 0)
        {
            Log.i(TAG, "no resource from a split");
            return;
        }

        Log.i(TAG, "a string that only the split carries resolves to " + getResources().getString(identifier));
    }

    private void describeSignature()
    {
        try
        {
            android.content.pm.PackageInfo info =
                getPackageManager().getPackageInfo(getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);

            if (info.signatures == null || info.signatures.length == 0)
            {
                Log.i(TAG, "the package carries no signature");
                return;
            }

            android.content.pm.Signature signature = info.signatures[0];
            byte[] certificate = signature.toByteArray();

            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] fingerprint = digest.digest(certificate);

            StringBuilder text = new StringBuilder();
            for (byte value : fingerprint) text.append(String.format("%02x", value));

            Log.i(TAG, "the package is signed by " + info.signatures.length + " signer(s), the first with a "
                       + certificate.length + " byte certificate");
            Log.i(TAG, "the signing certificate fingerprint is " + text);
            Log.i(TAG, "the signature reads back as " + signature.toCharsString().length() + " hex characters and "
                       + (signature.equals(new android.content.pm.Signature(signature.toCharsString())) ? "round trips"
                                                                                                        : "does not round trip"));
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "the package could not describe its signature", cause);
        }
    }

    private String describe(String permission)
    {
        return checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED ? "granted" : "denied";
    }
}
