package android.opengl;

import android.hardware.SyncFence;
import android.os.Parcel;

public class EGLExt
{
    public static final int EGL_CONTEXT_MAJOR_VERSION_KHR = 0x3098;
    public static final int EGL_CONTEXT_MINOR_VERSION_KHR = 0x30FB;
    public static final int EGL_CONTEXT_FLAGS_KHR = 0x30FC;
    public static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;
    public static final int EGL_RECORDABLE_ANDROID = 0x3142;
    public static final int EGL_SYNC_NATIVE_FENCE_ANDROID = 0x3144;
    public static final int EGL_SYNC_NATIVE_FENCE_FD_ANDROID = 0x3145;
    public static final int EGL_SYNC_NATIVE_FENCE_SIGNALED_ANDROID = 0x3146;
    public static final int EGL_NO_NATIVE_FENCE_FD_ANDROID = -1;

    public EGLExt()
    {
    }

    public static boolean eglPresentationTimeANDROID(EGLDisplay display, EGLSurface surface, long time)
    {
        return RuntimeEglBridge.surface(surface) != null;
    }

    public static SyncFence eglDupNativeFenceFDANDROID(EGLDisplay display, EGLSync sync)
    {
        Parcel parcel = Parcel.obtain();
        try
        {
            parcel.writeBoolean(false);
            parcel.writeLong(SyncFence.SIGNAL_TIME_INVALID);
            parcel.setDataPosition(0);
            return SyncFence.CREATOR.createFromParcel(parcel);
        }
        finally
        {
            parcel.recycle();
        }
    }
}
