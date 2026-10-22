package android.opengl;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import javax.microedition.khronos.egl.EGL10;

public class EGL14
{
    public static final int EGL_DEFAULT_DISPLAY = 0;
    public static EGLContext EGL_NO_CONTEXT = new EGLContext(0L);
    public static EGLDisplay EGL_NO_DISPLAY = new EGLDisplay(0L);
    public static EGLSurface EGL_NO_SURFACE = new EGLSurface(0L);

    public static final int EGL_FALSE = 0;
    public static final int EGL_TRUE = 1;
    public static final int EGL_SUCCESS = 0x3000;
    public static final int EGL_NOT_INITIALIZED = 0x3001;
    public static final int EGL_BAD_ACCESS = 0x3002;
    public static final int EGL_BAD_ALLOC = 0x3003;
    public static final int EGL_BAD_ATTRIBUTE = 0x3004;
    public static final int EGL_BAD_CONFIG = 0x3005;
    public static final int EGL_BAD_CONTEXT = 0x3006;
    public static final int EGL_BAD_CURRENT_SURFACE = 0x3007;
    public static final int EGL_BAD_DISPLAY = 0x3008;
    public static final int EGL_BAD_MATCH = 0x3009;
    public static final int EGL_BAD_NATIVE_PIXMAP = 0x300A;
    public static final int EGL_BAD_NATIVE_WINDOW = 0x300B;
    public static final int EGL_BAD_PARAMETER = 0x300C;
    public static final int EGL_BAD_SURFACE = 0x300D;
    public static final int EGL_CONTEXT_LOST = 0x300E;
    public static final int EGL_BUFFER_SIZE = 0x3020;
    public static final int EGL_ALPHA_SIZE = 0x3021;
    public static final int EGL_BLUE_SIZE = 0x3022;
    public static final int EGL_GREEN_SIZE = 0x3023;
    public static final int EGL_RED_SIZE = 0x3024;
    public static final int EGL_DEPTH_SIZE = 0x3025;
    public static final int EGL_STENCIL_SIZE = 0x3026;
    public static final int EGL_CONFIG_CAVEAT = 0x3027;
    public static final int EGL_CONFIG_ID = 0x3028;
    public static final int EGL_LEVEL = 0x3029;
    public static final int EGL_MAX_PBUFFER_HEIGHT = 0x302A;
    public static final int EGL_MAX_PBUFFER_PIXELS = 0x302B;
    public static final int EGL_MAX_PBUFFER_WIDTH = 0x302C;
    public static final int EGL_NATIVE_RENDERABLE = 0x302D;
    public static final int EGL_NATIVE_VISUAL_ID = 0x302E;
    public static final int EGL_NATIVE_VISUAL_TYPE = 0x302F;
    public static final int EGL_SAMPLES = 0x3031;
    public static final int EGL_SAMPLE_BUFFERS = 0x3032;
    public static final int EGL_SURFACE_TYPE = 0x3033;
    public static final int EGL_TRANSPARENT_TYPE = 0x3034;
    public static final int EGL_TRANSPARENT_BLUE_VALUE = 0x3035;
    public static final int EGL_TRANSPARENT_GREEN_VALUE = 0x3036;
    public static final int EGL_TRANSPARENT_RED_VALUE = 0x3037;
    public static final int EGL_NONE = 0x3038;
    public static final int EGL_BIND_TO_TEXTURE_RGB = 0x3039;
    public static final int EGL_BIND_TO_TEXTURE_RGBA = 0x303A;
    public static final int EGL_MIN_SWAP_INTERVAL = 0x303B;
    public static final int EGL_MAX_SWAP_INTERVAL = 0x303C;
    public static final int EGL_LUMINANCE_SIZE = 0x303D;
    public static final int EGL_ALPHA_MASK_SIZE = 0x303E;
    public static final int EGL_COLOR_BUFFER_TYPE = 0x303F;
    public static final int EGL_RENDERABLE_TYPE = 0x3040;
    public static final int EGL_MATCH_NATIVE_PIXMAP = 0x3041;
    public static final int EGL_CONFORMANT = 0x3042;
    public static final int EGL_SLOW_CONFIG = 0x3050;
    public static final int EGL_NON_CONFORMANT_CONFIG = 0x3051;
    public static final int EGL_TRANSPARENT_RGB = 0x3052;
    public static final int EGL_RGB_BUFFER = 0x308E;
    public static final int EGL_LUMINANCE_BUFFER = 0x308F;
    public static final int EGL_NO_TEXTURE = 0x305C;
    public static final int EGL_TEXTURE_RGB = 0x305D;
    public static final int EGL_TEXTURE_RGBA = 0x305E;
    public static final int EGL_TEXTURE_2D = 0x305F;
    public static final int EGL_PBUFFER_BIT = 0x0001;
    public static final int EGL_PIXMAP_BIT = 0x0002;
    public static final int EGL_WINDOW_BIT = 0x0004;
    public static final int EGL_VG_COLORSPACE_LINEAR_BIT = 0x0020;
    public static final int EGL_VG_ALPHA_FORMAT_PRE_BIT = 0x0040;
    public static final int EGL_MULTISAMPLE_RESOLVE_BOX_BIT = 0x0200;
    public static final int EGL_SWAP_BEHAVIOR_PRESERVED_BIT = 0x0400;
    public static final int EGL_OPENGL_ES_BIT = 0x0001;
    public static final int EGL_OPENVG_BIT = 0x0002;
    public static final int EGL_OPENGL_ES2_BIT = 0x0004;
    public static final int EGL_OPENGL_BIT = 0x0008;
    public static final int EGL_VENDOR = 0x3053;
    public static final int EGL_VERSION = 0x3054;
    public static final int EGL_EXTENSIONS = 0x3055;
    public static final int EGL_CLIENT_APIS = 0x308D;
    public static final int EGL_HEIGHT = 0x3056;
    public static final int EGL_WIDTH = 0x3057;
    public static final int EGL_LARGEST_PBUFFER = 0x3058;
    public static final int EGL_TEXTURE_FORMAT = 0x3080;
    public static final int EGL_TEXTURE_TARGET = 0x3081;
    public static final int EGL_MIPMAP_TEXTURE = 0x3082;
    public static final int EGL_MIPMAP_LEVEL = 0x3083;
    public static final int EGL_RENDER_BUFFER = 0x3086;
    public static final int EGL_VG_COLORSPACE = 0x3087;
    public static final int EGL_VG_ALPHA_FORMAT = 0x3088;
    public static final int EGL_HORIZONTAL_RESOLUTION = 0x3090;
    public static final int EGL_VERTICAL_RESOLUTION = 0x3091;
    public static final int EGL_PIXEL_ASPECT_RATIO = 0x3092;
    public static final int EGL_SWAP_BEHAVIOR = 0x3093;
    public static final int EGL_MULTISAMPLE_RESOLVE = 0x3099;
    public static final int EGL_BACK_BUFFER = 0x3084;
    public static final int EGL_SINGLE_BUFFER = 0x3085;
    public static final int EGL_VG_COLORSPACE_sRGB = 0x3089;
    public static final int EGL_VG_COLORSPACE_LINEAR = 0x308A;
    public static final int EGL_VG_ALPHA_FORMAT_NONPRE = 0x308B;
    public static final int EGL_VG_ALPHA_FORMAT_PRE = 0x308C;
    public static final int EGL_DISPLAY_SCALING = 10000;
    public static final int EGL_BUFFER_PRESERVED = 0x3094;
    public static final int EGL_BUFFER_DESTROYED = 0x3095;
    public static final int EGL_OPENVG_IMAGE = 0x3096;
    public static final int EGL_CONTEXT_CLIENT_TYPE = 0x3097;
    public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    public static final int EGL_MULTISAMPLE_RESOLVE_DEFAULT = 0x309A;
    public static final int EGL_MULTISAMPLE_RESOLVE_BOX = 0x309B;
    public static final int EGL_OPENGL_ES_API = 0x30A0;
    public static final int EGL_OPENVG_API = 0x30A1;
    public static final int EGL_OPENGL_API = 0x30A2;
    public static final int EGL_DRAW = 0x3059;
    public static final int EGL_READ = 0x305A;
    public static final int EGL_CORE_NATIVE_ENGINE = 0x305B;

    private static final ThreadLocal<int[]> sBoundApi = new ThreadLocal<int[]>()
    {
        @Override
        protected int[] initialValue()
        {
            return new int[] {EGL_OPENGL_ES_API};
        }
    };

    public EGL14()
    {
    }

    public static int eglGetError()
    {
        return RuntimeEglBridge.EGL.eglGetError();
    }

    public static EGLDisplay eglGetDisplay(int displayId)
    {
        if (displayId != EGL_DEFAULT_DISPLAY) return EGL_NO_DISPLAY;
        return RuntimeEglBridge.wrap(RuntimeEglBridge.EGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY));
    }

    public static boolean eglInitialize(EGLDisplay display, int[] major, int majorOffset, int[] minor, int minorOffset)
    {
        if (major != null) RuntimeEglBridge.checkOutput(major, majorOffset, 1, "major");
        if (minor != null) RuntimeEglBridge.checkOutput(minor, minorOffset, 1, "minor");

        int[] version = new int[2];
        if (!RuntimeEglBridge.EGL.eglInitialize(RuntimeEglBridge.display(display), version)) return false;

        if (major != null) major[majorOffset] = version[0];
        if (minor != null) minor[minorOffset] = version[1];
        return true;
    }

    public static boolean eglTerminate(EGLDisplay display)
    {
        return RuntimeEglBridge.EGL.eglTerminate(RuntimeEglBridge.display(display));
    }

    public static String eglQueryString(EGLDisplay display, int name)
    {
        return RuntimeEglBridge.EGL.eglQueryString(RuntimeEglBridge.display(display), name);
    }

    public static boolean eglGetConfigs(EGLDisplay display, EGLConfig[] configs, int configsOffset, int configSize, int[] numConfig,
            int numConfigOffset)
    {
        RuntimeEglBridge.checkOutput(numConfig, numConfigOffset, 1, "numConfig");
        if (configs != null) RuntimeEglBridge.checkOutput(configs, configsOffset, configSize, "configs");

        javax.microedition.khronos.egl.EGLConfig[] found = configs == null ? null : RuntimeEglBridge.configArray(configSize);
        int[] count = new int[1];
        if (!RuntimeEglBridge.EGL.eglGetConfigs(RuntimeEglBridge.display(display), found, configSize, count)) return false;

        numConfig[numConfigOffset] = count[0];
        if (configs != null)
        {
            for (int index = 0; index < count[0]; index++) configs[configsOffset + index] = RuntimeEglBridge.wrap(found[index]);
        }
        return true;
    }

    public static boolean eglChooseConfig(EGLDisplay display, int[] attributeList, int attributeListOffset, EGLConfig[] configs,
            int configsOffset, int configSize, int[] numConfig, int numConfigOffset)
    {
        RuntimeEglBridge.checkOutput(numConfig, numConfigOffset, 1, "numConfig");
        if (configs != null) RuntimeEglBridge.checkOutput(configs, configsOffset, configSize, "configs");

        int[] attributes = RuntimeEglBridge.attributes(attributeList, attributeListOffset);
        javax.microedition.khronos.egl.EGLConfig[] found = configs == null ? null : RuntimeEglBridge.configArray(configSize);
        int[] count = new int[1];
        if (!RuntimeEglBridge.EGL.eglChooseConfig(RuntimeEglBridge.display(display), attributes, found, configSize, count))
        {
            return false;
        }

        numConfig[numConfigOffset] = count[0];
        if (configs != null)
        {
            for (int index = 0; index < count[0]; index++) configs[configsOffset + index] = RuntimeEglBridge.wrap(found[index]);
        }
        return true;
    }

    public static boolean eglGetConfigAttrib(EGLDisplay display, EGLConfig config, int attribute, int[] value, int offset)
    {
        RuntimeEglBridge.checkOutput(value, offset, 1, "value");

        int[] result = new int[1];
        if (!RuntimeEglBridge.EGL.eglGetConfigAttrib(RuntimeEglBridge.display(display), RuntimeEglBridge.config(config), attribute,
                result))
        {
            return false;
        }

        value[offset] = result[0];
        return true;
    }

    public static EGLSurface eglCreateWindowSurface(EGLDisplay display, EGLConfig config, Object window, int[] attributeList,
            int offset)
    {
        if (!(window instanceof Surface) && !(window instanceof SurfaceView) && !(window instanceof SurfaceHolder)
                && !(window instanceof SurfaceTexture))
        {
            throw new UnsupportedOperationException("eglCreateWindowSurface() can only be called with an instance of Surface, "
                    + "SurfaceView, SurfaceTexture or SurfaceHolder at the moment, this will be fixed later.");
        }

        int[] attributes = RuntimeEglBridge.attributes(attributeList, offset);
        return RuntimeEglBridge.wrap(RuntimeEglBridge.EGL.eglCreateWindowSurface(RuntimeEglBridge.display(display),
                RuntimeEglBridge.config(config), window, attributes));
    }

    public static EGLSurface eglCreatePbufferSurface(EGLDisplay display, EGLConfig config, int[] attributeList, int offset)
    {
        int[] attributes = RuntimeEglBridge.attributes(attributeList, offset);
        return RuntimeEglBridge.wrap(RuntimeEglBridge.EGL.eglCreatePbufferSurface(RuntimeEglBridge.display(display),
                RuntimeEglBridge.config(config), attributes));
    }

    @Deprecated
    public static EGLSurface eglCreatePixmapSurface(EGLDisplay display, EGLConfig config, int pixmap, int[] attributeList, int offset)
    {
        throw new UnsupportedOperationException("eglCreatePixmapSurface");
    }

    public static boolean eglDestroySurface(EGLDisplay display, EGLSurface surface)
    {
        boolean destroyed = RuntimeEglBridge.EGL.eglDestroySurface(RuntimeEglBridge.display(display),
                RuntimeEglBridge.surface(surface));
        if (destroyed) RuntimeEglBridge.forget(surface);
        return destroyed;
    }

    public static boolean eglQuerySurface(EGLDisplay display, EGLSurface surface, int attribute, int[] value, int offset)
    {
        RuntimeEglBridge.checkOutput(value, offset, 1, "value");

        int[] result = new int[1];
        if (!RuntimeEglBridge.EGL.eglQuerySurface(RuntimeEglBridge.display(display), RuntimeEglBridge.surface(surface), attribute,
                result))
        {
            return false;
        }

        value[offset] = result[0];
        return true;
    }

    public static boolean eglBindAPI(int api)
    {
        if (api != EGL_OPENGL_ES_API) return false;

        sBoundApi.get()[0] = api;
        return true;
    }

    public static int eglQueryAPI()
    {
        return sBoundApi.get()[0];
    }

    public static boolean eglWaitClient()
    {
        return RuntimeEglBridge.EGL.eglWaitGL();
    }

    public static boolean eglReleaseThread()
    {
        return RuntimeEglBridge.EGL.eglMakeCurrent(EGL10.EGL_NO_DISPLAY, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
    }

    public static EGLSurface eglCreatePbufferFromClientBuffer(EGLDisplay display, int bufferType, int buffer, EGLConfig config,
            int[] attributeList, int offset)
    {
        return EGL_NO_SURFACE;
    }

    public static boolean eglSurfaceAttrib(EGLDisplay display, EGLSurface surface, int attribute, int value)
    {
        return RuntimeEglBridge.surface(surface) != null;
    }

    public static boolean eglBindTexImage(EGLDisplay display, EGLSurface surface, int buffer)
    {
        return RuntimeEglBridge.surface(surface) != null;
    }

    public static boolean eglReleaseTexImage(EGLDisplay display, EGLSurface surface, int buffer)
    {
        return RuntimeEglBridge.surface(surface) != null;
    }

    public static boolean eglSwapInterval(EGLDisplay display, int interval)
    {
        return RuntimeEglBridge.display(display) != null;
    }

    public static EGLContext eglCreateContext(EGLDisplay display, EGLConfig config, EGLContext shareContext, int[] attributeList,
            int offset)
    {
        int[] attributes = RuntimeEglBridge.attributes(attributeList, offset);
        return RuntimeEglBridge.wrap(RuntimeEglBridge.EGL.eglCreateContext(RuntimeEglBridge.display(display),
                RuntimeEglBridge.config(config), RuntimeEglBridge.context(shareContext), attributes));
    }

    public static boolean eglDestroyContext(EGLDisplay display, EGLContext context)
    {
        boolean destroyed = RuntimeEglBridge.EGL.eglDestroyContext(RuntimeEglBridge.display(display),
                RuntimeEglBridge.context(context));
        if (destroyed) RuntimeEglBridge.forget(context);
        return destroyed;
    }

    public static boolean eglMakeCurrent(EGLDisplay display, EGLSurface draw, EGLSurface read, EGLContext context)
    {
        return RuntimeEglBridge.EGL.eglMakeCurrent(RuntimeEglBridge.display(display), RuntimeEglBridge.surface(draw),
                RuntimeEglBridge.surface(read), RuntimeEglBridge.context(context));
    }

    public static EGLContext eglGetCurrentContext()
    {
        return RuntimeEglBridge.wrap(RuntimeEglBridge.EGL.eglGetCurrentContext());
    }

    public static EGLSurface eglGetCurrentSurface(int readdraw)
    {
        return RuntimeEglBridge.wrap(RuntimeEglBridge.EGL.eglGetCurrentSurface(readdraw));
    }

    public static EGLDisplay eglGetCurrentDisplay()
    {
        return RuntimeEglBridge.wrap(RuntimeEglBridge.EGL.eglGetCurrentDisplay());
    }

    public static boolean eglQueryContext(EGLDisplay display, EGLContext context, int attribute, int[] value, int offset)
    {
        RuntimeEglBridge.checkOutput(value, offset, 1, "value");

        int[] result = new int[1];
        if (!RuntimeEglBridge.EGL.eglQueryContext(RuntimeEglBridge.display(display), RuntimeEglBridge.context(context), attribute,
                result))
        {
            return false;
        }

        value[offset] = result[0];
        return true;
    }

    public static boolean eglWaitGL()
    {
        return RuntimeEglBridge.EGL.eglWaitGL();
    }

    public static boolean eglWaitNative(int engine)
    {
        return RuntimeEglBridge.EGL.eglWaitNative(engine, null);
    }

    public static boolean eglSwapBuffers(EGLDisplay display, EGLSurface surface)
    {
        return RuntimeEglBridge.EGL.eglSwapBuffers(RuntimeEglBridge.display(display), RuntimeEglBridge.surface(surface));
    }

    public static boolean eglCopyBuffers(EGLDisplay display, EGLSurface surface, int target)
    {
        return RuntimeEglBridge.EGL.eglCopyBuffers(RuntimeEglBridge.display(display), RuntimeEglBridge.surface(surface), null);
    }
}
