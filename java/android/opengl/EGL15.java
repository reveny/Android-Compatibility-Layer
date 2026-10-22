package android.opengl;

import java.nio.Buffer;

public final class EGL15
{
    public static final int EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT = 0x00000001;
    public static final int EGL_CONTEXT_OPENGL_COMPATIBILITY_PROFILE_BIT = 0x00000002;
    public static final int EGL_OPENGL_ES3_BIT = 0x00000040;
    public static final int EGL_SYNC_FLUSH_COMMANDS_BIT = 0x0001;
    public static final int EGL_GL_COLORSPACE_SRGB = 0x3089;
    public static final int EGL_GL_COLORSPACE_LINEAR = 0x308A;
    public static final int EGL_CONTEXT_MAJOR_VERSION = 0x3098;
    public static final int EGL_CL_EVENT_HANDLE = 0x309C;
    public static final int EGL_GL_COLORSPACE = 0x309D;
    public static final int EGL_GL_TEXTURE_2D = 0x30B1;
    public static final int EGL_GL_TEXTURE_3D = 0x30B2;
    public static final int EGL_GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x30B3;
    public static final int EGL_GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x30B4;
    public static final int EGL_GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x30B5;
    public static final int EGL_GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x30B6;
    public static final int EGL_GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x30B7;
    public static final int EGL_GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x30B8;
    public static final int EGL_GL_RENDERBUFFER = 0x30B9;
    public static final int EGL_GL_TEXTURE_LEVEL = 0x30BC;
    public static final int EGL_GL_TEXTURE_ZOFFSET = 0x30BD;
    public static final int EGL_IMAGE_PRESERVED = 0x30D2;
    public static final int EGL_SYNC_PRIOR_COMMANDS_COMPLETE = 0x30F0;
    public static final int EGL_SYNC_STATUS = 0x30F1;
    public static final int EGL_SIGNALED = 0x30F2;
    public static final int EGL_UNSIGNALED = 0x30F3;
    public static final int EGL_TIMEOUT_EXPIRED = 0x30F5;
    public static final int EGL_CONDITION_SATISFIED = 0x30F6;
    public static final int EGL_SYNC_TYPE = 0x30F7;
    public static final int EGL_SYNC_CONDITION = 0x30F8;
    public static final int EGL_SYNC_FENCE = 0x30F9;
    public static final int EGL_CONTEXT_MINOR_VERSION = 0x30FB;
    public static final int EGL_CONTEXT_OPENGL_PROFILE_MASK = 0x30FD;
    public static final int EGL_SYNC_CL_EVENT = 0x30FE;
    public static final int EGL_SYNC_CL_EVENT_COMPLETE = 0x30FF;
    public static final int EGL_CONTEXT_OPENGL_DEBUG = 0x31B0;
    public static final int EGL_CONTEXT_OPENGL_FORWARD_COMPATIBLE = 0x31B1;
    public static final int EGL_CONTEXT_OPENGL_ROBUST_ACCESS = 0x31B2;
    public static final int EGL_CONTEXT_OPENGL_RESET_NOTIFICATION_STRATEGY = 0x31BD;
    public static final int EGL_NO_RESET_NOTIFICATION = 0x31BE;
    public static final int EGL_LOSE_CONTEXT_ON_RESET = 0x31BF;
    public static final int EGL_PLATFORM_ANDROID_KHR = 0x3141;
    public static final long EGL_FOREVER = -1L;
    public static final EGLImage EGL_NO_IMAGE = null;
    public static final EGLSync EGL_NO_SYNC = null;
    public static final EGLContext EGL_NO_CONTEXT = new EGLContext(0L);
    public static final EGLDisplay EGL_NO_DISPLAY = new EGLDisplay(0L);
    public static final EGLSurface EGL_NO_SURFACE = new EGLSurface(0L);

    EGL15()
    {
    }

    public static EGLSync eglCreateSync(EGLDisplay display, int type, long[] attributeList, int offset)
    {
        if (RuntimeEglBridge.display(display) == null) return EGL_NO_SYNC;
        return new EGLSync(RuntimeEglBridge.newHandle());
    }

    public static boolean eglGetSyncAttrib(EGLDisplay display, EGLSync sync, int attribute, long[] value, int offset)
    {
        RuntimeEglBridge.checkOutput(value, offset, 1, "value");
        if (sync == null) return false;

        switch (attribute)
        {
            case EGL_SYNC_STATUS:
                value[offset] = EGL_SIGNALED;
                return true;
            case EGL_SYNC_TYPE:
                value[offset] = EGL_SYNC_FENCE;
                return true;
            case EGL_SYNC_CONDITION:
                value[offset] = EGL_SYNC_PRIOR_COMMANDS_COMPLETE;
                return true;
            default:
                return false;
        }
    }

    public static boolean eglDestroySync(EGLDisplay display, EGLSync sync)
    {
        return sync != null;
    }

    public static int eglClientWaitSync(EGLDisplay display, EGLSync sync, int flags, long timeout)
    {
        return sync == null ? EGL14.EGL_FALSE : EGL_CONDITION_SATISFIED;
    }

    public static EGLDisplay eglGetPlatformDisplay(int platform, long nativeDisplay, long[] attributeList, int offset)
    {
        if (platform != EGL_PLATFORM_ANDROID_KHR) return EGL_NO_DISPLAY;
        return EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
    }

    public static EGLSurface eglCreatePlatformWindowSurface(EGLDisplay display, EGLConfig config, Buffer nativeWindow,
            long[] attributeList, int offset)
    {
        return EGL_NO_SURFACE;
    }

    public static EGLSurface eglCreatePlatformPixmapSurface(EGLDisplay display, EGLConfig config, Buffer nativePixmap,
            long[] attributeList, int offset)
    {
        return EGL_NO_SURFACE;
    }

    public static boolean eglWaitSync(EGLDisplay display, EGLSync sync, int flags)
    {
        return sync != null;
    }

    public static EGLImage eglCreateImage(EGLDisplay display, EGLContext context, int target, long buffer, long[] attributeList,
            int offset)
    {
        if (RuntimeEglBridge.display(display) == null) return EGL_NO_IMAGE;
        return new EGLImage(RuntimeEglBridge.newHandle());
    }

    public static boolean eglDestroyImage(EGLDisplay display, EGLImage image)
    {
        return image != null;
    }
}
