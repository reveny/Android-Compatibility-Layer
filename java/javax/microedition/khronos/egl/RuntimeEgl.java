package javax.microedition.khronos.egl;

import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL;

final class RuntimeEgl implements EGL11
{
    static final RuntimeEgl INSTANCE = new RuntimeEgl();

    private static final int EGL_FALSE = 0;
    private static final int EGL_TRUE = 1;
    private static final int EGL_OPENGL_ES_BIT = 0x0001;
    private static final int EGL_OPENGL_ES2_BIT = 0x0004;
    private static final int EGL_OPENGL_ES3_BIT = 0x0040;
    private static final int EGL_CONFORMANT = 0x3042;
    private static final int EGL_MIN_SWAP_INTERVAL = 0x303B;
    private static final int EGL_MAX_SWAP_INTERVAL = 0x303C;
    private static final int EGL_BIND_TO_TEXTURE_RGB = 0x3039;
    private static final int EGL_BIND_TO_TEXTURE_RGBA = 0x303A;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int EGL_CONTEXT_CLIENT_TYPE = 0x3097;
    private static final int EGL_OPENGL_ES_API = 0x30A0;
    private static final int EGL_BACK_BUFFER = 0x3084;
    private static final int EGL_CLIENT_APIS = 0x308D;
    private static final int EGL_SWAP_BEHAVIOR = 0x3093;
    private static final int EGL_BUFFER_DESTROYED = 0x3095;
    private static final int EGL_MULTISAMPLE_RESOLVE = 0x3099;
    private static final int EGL_MULTISAMPLE_RESOLVE_DEFAULT = 0x309A;
    private static final int EGL_TEXTURE_FORMAT = 0x3080;
    private static final int EGL_TEXTURE_TARGET = 0x3081;
    private static final int EGL_NO_TEXTURE = 0x305C;
    private static final int PIXEL_FORMAT_RGBA_8888 = 1;
    private static final int PIXEL_FORMAT_RGBX_8888 = 2;
    private static final int PIXEL_FORMAT_RGB_565 = 4;
    private static final long FRAME_INTERVAL_NANOS = 16_666_667L;

    private final Display mDisplay = new Display();
    private final List<Config> mConfigs = new ArrayList<>();
    private final ThreadLocal<Current> mCurrent = new ThreadLocal<Current>()
    {
        @Override
        protected Current initialValue()
        {
            return new Current();
        }
    };

    private RuntimeEgl()
    {
        int[][] colors = {{8, 8, 8, 8}, {8, 8, 8, 0}, {5, 6, 5, 0}};
        int[][] depthStencils = {{0, 0}, {16, 0}, {24, 0}, {24, 8}, {0, 8}};
        int[] samples = {0, 4};
        int identifier = 1;

        for (int[] color : colors)
        {
            for (int[] depthStencil : depthStencils)
            {
                for (int sampleCount : samples)
                {
                    mConfigs.add(new Config(identifier++, color, depthStencil[0], depthStencil[1], sampleCount));
                }
            }
        }
    }

    static final class Display extends EGLDisplay
    {
        boolean mInitialized;
    }

    static final class Config extends EGLConfig
    {
        final Map<Integer, Integer> mAttributes = new HashMap<>();

        Config(int identifier, int[] color, int depth, int stencil, int samples)
        {
            int bufferSize = color[0] + color[1] + color[2] + color[3];
            int format = color[3] > 0 ? PIXEL_FORMAT_RGBA_8888 : (color[0] == 5 ? PIXEL_FORMAT_RGB_565 : PIXEL_FORMAT_RGBX_8888);

            mAttributes.put(EGL_CONFIG_ID, identifier);
            mAttributes.put(EGL_BUFFER_SIZE, bufferSize);
            mAttributes.put(EGL_RED_SIZE, color[0]);
            mAttributes.put(EGL_GREEN_SIZE, color[1]);
            mAttributes.put(EGL_BLUE_SIZE, color[2]);
            mAttributes.put(EGL_ALPHA_SIZE, color[3]);
            mAttributes.put(EGL_DEPTH_SIZE, depth);
            mAttributes.put(EGL_STENCIL_SIZE, stencil);
            mAttributes.put(EGL_SAMPLE_BUFFERS, samples > 0 ? 1 : 0);
            mAttributes.put(EGL_SAMPLES, samples);
            mAttributes.put(EGL_CONFIG_CAVEAT, EGL_NONE);
            mAttributes.put(EGL_LEVEL, 0);
            mAttributes.put(EGL_MAX_PBUFFER_WIDTH, 8192);
            mAttributes.put(EGL_MAX_PBUFFER_HEIGHT, 8192);
            mAttributes.put(EGL_MAX_PBUFFER_PIXELS, 8192 * 8192);
            mAttributes.put(EGL_NATIVE_RENDERABLE, EGL_TRUE);
            mAttributes.put(EGL_NATIVE_VISUAL_ID, format);
            mAttributes.put(EGL_NATIVE_VISUAL_TYPE, 0);
            mAttributes.put(EGL_SURFACE_TYPE, EGL_WINDOW_BIT | EGL_PBUFFER_BIT);
            mAttributes.put(EGL_RENDERABLE_TYPE, EGL_OPENGL_ES_BIT | EGL_OPENGL_ES2_BIT | EGL_OPENGL_ES3_BIT);
            mAttributes.put(EGL_CONFORMANT, EGL_OPENGL_ES_BIT | EGL_OPENGL_ES2_BIT | EGL_OPENGL_ES3_BIT);
            mAttributes.put(EGL_TRANSPARENT_TYPE, EGL_NONE);
            mAttributes.put(EGL_TRANSPARENT_RED_VALUE, 0);
            mAttributes.put(EGL_TRANSPARENT_GREEN_VALUE, 0);
            mAttributes.put(EGL_TRANSPARENT_BLUE_VALUE, 0);
            mAttributes.put(EGL_LUMINANCE_SIZE, 0);
            mAttributes.put(EGL_ALPHA_MASK_SIZE, 0);
            mAttributes.put(EGL_COLOR_BUFFER_TYPE, EGL_RGB_BUFFER);
            mAttributes.put(EGL_MIN_SWAP_INTERVAL, 0);
            mAttributes.put(EGL_MAX_SWAP_INTERVAL, 1);
            mAttributes.put(EGL_BIND_TO_TEXTURE_RGB, EGL_FALSE);
            mAttributes.put(EGL_BIND_TO_TEXTURE_RGBA, EGL_FALSE);
        }

        int get(int attribute)
        {
            Integer value = mAttributes.get(attribute);
            return value == null ? 0 : value;
        }
    }

    static final class Context extends EGLContext
    {
        final Config mConfig;
        final int mClientVersion;

        Context(Config config, int clientVersion)
        {
            mConfig = config;
            mClientVersion = clientVersion;
        }

        @Override
        public GL getGL()
        {
            return RuntimeGl.INSTANCE;
        }
    }

    static final class Surface extends EGLSurface
    {
        final Config mConfig;
        final Object mNativeWindow;
        final int mWidth;
        final int mHeight;
        long mLastSwapNanos;

        Surface(Config config, Object nativeWindow, int width, int height)
        {
            mConfig = config;
            mNativeWindow = nativeWindow;
            mWidth = width;
            mHeight = height;
        }

        int width()
        {
            Rect frame = frame();
            return frame != null ? frame.width() : mWidth;
        }

        int height()
        {
            Rect frame = frame();
            return frame != null ? frame.height() : mHeight;
        }

        private Rect frame()
        {
            SurfaceHolder holder = null;
            if (mNativeWindow instanceof SurfaceHolder) holder = (SurfaceHolder) mNativeWindow;
            else if (mNativeWindow instanceof SurfaceView) holder = ((SurfaceView) mNativeWindow).getHolder();
            if (holder == null) return null;

            Rect frame = holder.getSurfaceFrame();
            return frame == null || frame.isEmpty() ? null : frame;
        }
    }

    private static final class Current
    {
        Display mDisplay;
        Surface mDraw;
        Surface mRead;
        Context mContext;
        int mError = EGL_SUCCESS;
    }

    private boolean fail(int error)
    {
        mCurrent.get().mError = error;
        return false;
    }

    private boolean succeed()
    {
        mCurrent.get().mError = EGL_SUCCESS;
        return true;
    }

    private boolean checkDisplay(EGLDisplay display)
    {
        if (display != mDisplay) return fail(EGL_BAD_DISPLAY);
        if (!mDisplay.mInitialized) return fail(EGL_NOT_INITIALIZED);
        return true;
    }

    @Override
    public boolean eglChooseConfig(EGLDisplay display, int[] attributeList, EGLConfig[] configs, int configSize, int[] numConfig)
    {
        if (!checkDisplay(display)) return false;
        if (numConfig == null || numConfig.length < 1) throw new IllegalArgumentException("numConfig");
        if (configs != null && configs.length < configSize) throw new IllegalArgumentException("length - offset < configSize");

        Map<Integer, Integer> requested = new HashMap<>();
        if (attributeList != null)
        {
            for (int index = 0; index + 1 < attributeList.length && attributeList[index] != EGL_NONE; index += 2)
            {
                requested.put(attributeList[index], attributeList[index + 1]);
            }
        }

        List<Config> matches = new ArrayList<>();
        for (Config config : mConfigs)
        {
            if (matches(config, requested)) matches.add(config);
        }

        final Map<Integer, Integer> wanted = requested;
        Collections.sort(matches, new Comparator<Config>()
        {
            @Override
            public int compare(Config first, Config second)
            {
                int colorDifference = requestedColorBits(second, wanted) - requestedColorBits(first, wanted);
                if (colorDifference != 0) return colorDifference;

                int[] keys = {EGL_BUFFER_SIZE, EGL_SAMPLE_BUFFERS, EGL_SAMPLES, EGL_DEPTH_SIZE, EGL_STENCIL_SIZE, EGL_CONFIG_ID};
                for (int key : keys)
                {
                    int difference = first.get(key) - second.get(key);
                    if (difference != 0) return difference;
                }
                return 0;
            }
        });

        if (configs == null)
        {
            numConfig[0] = matches.size();
            return succeed();
        }

        int count = Math.min(configSize, matches.size());
        for (int index = 0; index < count; index++) configs[index] = matches.get(index);
        numConfig[0] = count;
        return succeed();
    }

    private static int requestedColorBits(Config config, Map<Integer, Integer> requested)
    {
        int bits = 0;
        int[] components = {EGL_RED_SIZE, EGL_GREEN_SIZE, EGL_BLUE_SIZE, EGL_ALPHA_SIZE};
        for (int component : components)
        {
            Integer value = requested.get(component);
            if (value != null && value != 0 && value != EGL_DONT_CARE) bits += config.get(component);
        }
        return bits;
    }

    private static boolean matches(Config config, Map<Integer, Integer> requested)
    {
        for (Map.Entry<Integer, Integer> entry : requested.entrySet())
        {
            int attribute = entry.getKey();
            int value = entry.getValue();
            if (value == EGL_DONT_CARE) continue;

            switch (attribute)
            {
                case EGL_BUFFER_SIZE:
                case EGL_RED_SIZE:
                case EGL_GREEN_SIZE:
                case EGL_BLUE_SIZE:
                case EGL_ALPHA_SIZE:
                case EGL_DEPTH_SIZE:
                case EGL_STENCIL_SIZE:
                case EGL_SAMPLE_BUFFERS:
                case EGL_SAMPLES:
                case EGL_LUMINANCE_SIZE:
                case EGL_ALPHA_MASK_SIZE:
                    if (config.get(attribute) < value) return false;
                    break;
                case EGL_SURFACE_TYPE:
                case EGL_RENDERABLE_TYPE:
                case EGL_CONFORMANT:
                    if ((config.get(attribute) & value) != value) return false;
                    break;
                case EGL_CONFIG_ID:
                case EGL_CONFIG_CAVEAT:
                case EGL_LEVEL:
                case EGL_COLOR_BUFFER_TYPE:
                case EGL_TRANSPARENT_TYPE:
                case EGL_NATIVE_RENDERABLE:
                    if (config.get(attribute) != value) return false;
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean eglCopyBuffers(EGLDisplay display, EGLSurface surface, Object nativePixmap)
    {
        if (!checkDisplay(display)) return false;
        return fail(EGL_BAD_NATIVE_PIXMAP);
    }

    @Override
    public EGLContext eglCreateContext(EGLDisplay display, EGLConfig config, EGLContext shareContext, int[] attributeList)
    {
        if (!checkDisplay(display)) return EGL_NO_CONTEXT;
        if (!(config instanceof Config))
        {
            fail(EGL_BAD_CONFIG);
            return EGL_NO_CONTEXT;
        }

        int clientVersion = 1;
        if (attributeList != null)
        {
            for (int index = 0; index + 1 < attributeList.length && attributeList[index] != EGL_NONE; index += 2)
            {
                if (attributeList[index] == EGL_CONTEXT_CLIENT_VERSION) clientVersion = attributeList[index + 1];
            }
        }

        succeed();
        return new Context((Config) config, clientVersion);
    }

    @Override
    public EGLSurface eglCreatePbufferSurface(EGLDisplay display, EGLConfig config, int[] attributeList)
    {
        if (!checkDisplay(display)) return EGL_NO_SURFACE;
        if (!(config instanceof Config))
        {
            fail(EGL_BAD_CONFIG);
            return EGL_NO_SURFACE;
        }

        int width = 0;
        int height = 0;
        if (attributeList != null)
        {
            for (int index = 0; index + 1 < attributeList.length && attributeList[index] != EGL_NONE; index += 2)
            {
                if (attributeList[index] == EGL_WIDTH) width = attributeList[index + 1];
                else if (attributeList[index] == EGL_HEIGHT) height = attributeList[index + 1];
            }
        }

        succeed();
        return new Surface((Config) config, null, width, height);
    }

    @Override
    public EGLSurface eglCreatePixmapSurface(EGLDisplay display, EGLConfig config, Object nativePixmap, int[] attributeList)
    {
        if (!checkDisplay(display)) return EGL_NO_SURFACE;
        fail(EGL_BAD_NATIVE_PIXMAP);
        return EGL_NO_SURFACE;
    }

    @Override
    public EGLSurface eglCreateWindowSurface(EGLDisplay display, EGLConfig config, Object nativeWindow, int[] attributeList)
    {
        if (!checkDisplay(display)) return EGL_NO_SURFACE;
        if (!(config instanceof Config))
        {
            fail(EGL_BAD_CONFIG);
            return EGL_NO_SURFACE;
        }
        if (nativeWindow == null)
        {
            fail(EGL_BAD_NATIVE_WINDOW);
            return EGL_NO_SURFACE;
        }

        succeed();
        return new Surface((Config) config, nativeWindow, 0, 0);
    }

    @Override
    public boolean eglDestroyContext(EGLDisplay display, EGLContext context)
    {
        if (!checkDisplay(display)) return false;
        if (!(context instanceof Context)) return fail(EGL_BAD_CONTEXT);
        return succeed();
    }

    @Override
    public boolean eglDestroySurface(EGLDisplay display, EGLSurface surface)
    {
        if (!checkDisplay(display)) return false;
        if (!(surface instanceof Surface)) return fail(EGL_BAD_SURFACE);
        return succeed();
    }

    @Override
    public boolean eglGetConfigAttrib(EGLDisplay display, EGLConfig config, int attribute, int[] value)
    {
        if (!checkDisplay(display)) return false;
        if (!(config instanceof Config)) return fail(EGL_BAD_CONFIG);
        if (value == null || value.length < 1) throw new IllegalArgumentException("value");

        Config runtimeConfig = (Config) config;
        if (!runtimeConfig.mAttributes.containsKey(attribute)) return fail(EGL_BAD_ATTRIBUTE);

        value[0] = runtimeConfig.get(attribute);
        return succeed();
    }

    @Override
    public boolean eglGetConfigs(EGLDisplay display, EGLConfig[] configs, int configSize, int[] numConfig)
    {
        if (!checkDisplay(display)) return false;
        if (numConfig == null || numConfig.length < 1) throw new IllegalArgumentException("numConfig");

        if (configs == null)
        {
            numConfig[0] = mConfigs.size();
            return succeed();
        }

        int count = Math.min(Math.min(configSize, configs.length), mConfigs.size());
        for (int index = 0; index < count; index++) configs[index] = mConfigs.get(index);
        numConfig[0] = count;
        return succeed();
    }

    @Override
    public EGLContext eglGetCurrentContext()
    {
        Context context = mCurrent.get().mContext;
        return context == null ? EGL_NO_CONTEXT : context;
    }

    @Override
    public EGLDisplay eglGetCurrentDisplay()
    {
        Display display = mCurrent.get().mDisplay;
        return display == null ? EGL_NO_DISPLAY : display;
    }

    @Override
    public EGLSurface eglGetCurrentSurface(int readdraw)
    {
        Current current = mCurrent.get();
        Surface surface = readdraw == EGL_READ ? current.mRead : current.mDraw;
        return surface == null ? EGL_NO_SURFACE : surface;
    }

    @Override
    public EGLDisplay eglGetDisplay(Object nativeDisplay)
    {
        succeed();
        return mDisplay;
    }

    @Override
    public int eglGetError()
    {
        Current current = mCurrent.get();
        int error = current.mError;
        current.mError = EGL_SUCCESS;
        return error;
    }

    @Override
    public boolean eglInitialize(EGLDisplay display, int[] majorMinor)
    {
        if (display != mDisplay) return fail(EGL_BAD_DISPLAY);
        if (majorMinor != null && majorMinor.length < 2) throw new IllegalArgumentException("length < 2");

        mDisplay.mInitialized = true;
        if (majorMinor != null)
        {
            majorMinor[0] = 1;
            majorMinor[1] = 4;
        }
        return succeed();
    }

    @Override
    public boolean eglMakeCurrent(EGLDisplay display, EGLSurface draw, EGLSurface read, EGLContext context)
    {
        Current current = mCurrent.get();

        if (display == EGL_NO_DISPLAY && context == EGL_NO_CONTEXT)
        {
            current.mDisplay = null;
            current.mDraw = null;
            current.mRead = null;
            current.mContext = null;
            return succeed();
        }

        if (!checkDisplay(display)) return false;

        if (context == EGL_NO_CONTEXT)
        {
            if (draw != EGL_NO_SURFACE || read != EGL_NO_SURFACE) return fail(EGL_BAD_MATCH);
            current.mDisplay = null;
            current.mDraw = null;
            current.mRead = null;
            current.mContext = null;
            return succeed();
        }

        if (!(context instanceof Context)) return fail(EGL_BAD_CONTEXT);
        if (draw != EGL_NO_SURFACE && !(draw instanceof Surface)) return fail(EGL_BAD_SURFACE);
        if (read != EGL_NO_SURFACE && !(read instanceof Surface)) return fail(EGL_BAD_SURFACE);

        current.mDisplay = mDisplay;
        current.mDraw = (Surface) draw;
        current.mRead = (Surface) read;
        current.mContext = (Context) context;
        return succeed();
    }

    @Override
    public boolean eglQueryContext(EGLDisplay display, EGLContext context, int attribute, int[] value)
    {
        if (!checkDisplay(display)) return false;
        if (!(context instanceof Context)) return fail(EGL_BAD_CONTEXT);
        if (value == null || value.length < 1) throw new IllegalArgumentException("value");

        Context runtimeContext = (Context) context;
        switch (attribute)
        {
            case EGL_CONFIG_ID:
                value[0] = runtimeContext.mConfig.get(EGL_CONFIG_ID);
                return succeed();
            case EGL_CONTEXT_CLIENT_VERSION:
                value[0] = runtimeContext.mClientVersion;
                return succeed();
            case EGL_CONTEXT_CLIENT_TYPE:
                value[0] = EGL_OPENGL_ES_API;
                return succeed();
            case EGL_RENDER_BUFFER:
                value[0] = EGL_BACK_BUFFER;
                return succeed();
            default:
                return fail(EGL_BAD_ATTRIBUTE);
        }
    }

    @Override
    public String eglQueryString(EGLDisplay display, int name)
    {
        if (!checkDisplay(display)) return null;

        succeed();
        switch (name)
        {
            case EGL_VENDOR:
                return runtime.PlatformConfig.GPU_VENDOR;
            case EGL_VERSION:
                return "1.4 " + runtime.PlatformConfig.GPU_RENDERER;
            case EGL_EXTENSIONS:
                return "";
            case EGL_CLIENT_APIS:
                return "OpenGL_ES";
            default:
                fail(EGL_BAD_PARAMETER);
                return null;
        }
    }

    @Override
    public boolean eglQuerySurface(EGLDisplay display, EGLSurface surface, int attribute, int[] value)
    {
        if (!checkDisplay(display)) return false;
        if (!(surface instanceof Surface)) return fail(EGL_BAD_SURFACE);
        if (value == null || value.length < 1) throw new IllegalArgumentException("value");

        Surface runtimeSurface = (Surface) surface;
        switch (attribute)
        {
            case EGL_WIDTH:
                value[0] = runtimeSurface.width();
                return succeed();
            case EGL_HEIGHT:
                value[0] = runtimeSurface.height();
                return succeed();
            case EGL_CONFIG_ID:
                value[0] = runtimeSurface.mConfig.get(EGL_CONFIG_ID);
                return succeed();
            case EGL_RENDER_BUFFER:
                value[0] = EGL_BACK_BUFFER;
                return succeed();
            case EGL_SWAP_BEHAVIOR:
                value[0] = EGL_BUFFER_DESTROYED;
                return succeed();
            case EGL_MULTISAMPLE_RESOLVE:
                value[0] = EGL_MULTISAMPLE_RESOLVE_DEFAULT;
                return succeed();
            case EGL_TEXTURE_FORMAT:
            case EGL_TEXTURE_TARGET:
                value[0] = EGL_NO_TEXTURE;
                return succeed();
            case EGL_LARGEST_PBUFFER:
            case EGL_HORIZONTAL_RESOLUTION:
            case EGL_VERTICAL_RESOLUTION:
            case EGL_PIXEL_ASPECT_RATIO:
                value[0] = 0;
                return succeed();
            default:
                return fail(EGL_BAD_ATTRIBUTE);
        }
    }

    @Override
    public boolean eglSwapBuffers(EGLDisplay display, EGLSurface surface)
    {
        if (!checkDisplay(display)) return false;
        if (!(surface instanceof Surface)) return fail(EGL_BAD_SURFACE);

        Surface runtimeSurface = (Surface) surface;
        long now = System.nanoTime();
        long wait = runtimeSurface.mLastSwapNanos + FRAME_INTERVAL_NANOS - now;
        if (wait > 0)
        {
            try
            {
                Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
            }
            catch (InterruptedException exception)
            {
                Thread.currentThread().interrupt();
            }
        }
        runtimeSurface.mLastSwapNanos = System.nanoTime();
        return succeed();
    }

    @Override
    public boolean eglTerminate(EGLDisplay display)
    {
        if (display != mDisplay) return fail(EGL_BAD_DISPLAY);

        mDisplay.mInitialized = false;
        return succeed();
    }

    @Override
    public boolean eglWaitGL()
    {
        return succeed();
    }

    @Override
    public boolean eglWaitNative(int engine, Object bindTarget)
    {
        return succeed();
    }
}
