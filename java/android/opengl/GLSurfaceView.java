package android.opengl;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class GLSurfaceView extends SurfaceView implements SurfaceHolder.Callback2
{
    public static final int RENDERMODE_WHEN_DIRTY = 0;
    public static final int RENDERMODE_CONTINUOUSLY = 1;

    public static final int DEBUG_CHECK_GL_ERROR = 1;
    public static final int DEBUG_LOG_GL_CALLS = 2;

    private static final String TAG = "GLSurfaceView";
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int EGL_OPENGL_ES2_BIT = 0x0004;
    private static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;

    private static final GLThreadManager sGLThreadManager = new GLThreadManager();

    private final WeakReference<GLSurfaceView> mThisWeakRef = new WeakReference<>(this);

    private GLThread mGLThread;
    private Renderer mRenderer;
    private boolean mDetached;
    private EGLConfigChooser mEGLConfigChooser;
    private EGLContextFactory mEGLContextFactory;
    private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private GLWrapper mGLWrapper;
    private int mDebugFlags;
    private int mEGLContextClientVersion;
    private boolean mPreserveEGLContextOnPause;

    public interface Renderer
    {
        void onSurfaceCreated(GL10 gl, EGLConfig config);

        void onSurfaceChanged(GL10 gl, int width, int height);

        void onDrawFrame(GL10 gl);
    }

    public interface EGLConfigChooser
    {
        EGLConfig chooseConfig(EGL10 egl, EGLDisplay display);
    }

    public interface EGLContextFactory
    {
        EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig);

        void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context);
    }

    public interface EGLWindowSurfaceFactory
    {
        EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow);

        void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface);
    }

    public interface GLWrapper
    {
        GL wrap(GL gl);
    }

    public GLSurfaceView(Context context)
    {
        super(context);
        initialize();
    }

    public GLSurfaceView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
        initialize();
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            if (mGLThread != null) mGLThread.requestExitAndWait();
        }
        finally
        {
            super.finalize();
        }
    }

    private void initialize()
    {
        getHolder().addCallback(this);
    }

    public void setGLWrapper(GLWrapper glWrapper)
    {
        mGLWrapper = glWrapper;
    }

    public void setDebugFlags(int debugFlags)
    {
        mDebugFlags = debugFlags;
    }

    public int getDebugFlags()
    {
        return mDebugFlags;
    }

    public void setPreserveEGLContextOnPause(boolean preserveOnPause)
    {
        mPreserveEGLContextOnPause = preserveOnPause;
    }

    public boolean getPreserveEGLContextOnPause()
    {
        return mPreserveEGLContextOnPause;
    }

    public void setRenderer(Renderer renderer)
    {
        checkRenderThreadState();
        if (mEGLConfigChooser == null) mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        if (mEGLContextFactory == null) mEGLContextFactory = new DefaultContextFactory();
        if (mEGLWindowSurfaceFactory == null) mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();

        mRenderer = renderer;
        mGLThread = new GLThread(mThisWeakRef);
        mGLThread.start();
    }

    public void setEGLContextFactory(EGLContextFactory factory)
    {
        checkRenderThreadState();
        mEGLContextFactory = factory;
    }

    public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory)
    {
        checkRenderThreadState();
        mEGLWindowSurfaceFactory = factory;
    }

    public void setEGLConfigChooser(EGLConfigChooser configChooser)
    {
        checkRenderThreadState();
        mEGLConfigChooser = configChooser;
    }

    public void setEGLConfigChooser(boolean needDepth)
    {
        setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
    }

    public void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize)
    {
        setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize));
    }

    public void setEGLContextClientVersion(int version)
    {
        checkRenderThreadState();
        mEGLContextClientVersion = version;
    }

    public void setRenderMode(int renderMode)
    {
        mGLThread.setRenderMode(renderMode);
    }

    public int getRenderMode()
    {
        return mGLThread.getRenderMode();
    }

    public void requestRender()
    {
        mGLThread.requestRender();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        mGLThread.surfaceCreated();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        mGLThread.surfaceDestroyed();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        mGLThread.onWindowResize(width, height);
    }

    @Override
    public void surfaceRedrawNeededAsync(SurfaceHolder holder, Runnable finishDrawing)
    {
        if (mGLThread != null) mGLThread.requestRenderAndNotify(finishDrawing);
    }

    @Deprecated
    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder)
    {
    }

    public void onPause()
    {
        mGLThread.onPause();
    }

    public void onResume()
    {
        mGLThread.onResume();
    }

    public void queueEvent(Runnable runnable)
    {
        mGLThread.queueEvent(runnable);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if (mDetached && mRenderer != null)
        {
            int renderMode = RENDERMODE_CONTINUOUSLY;
            if (mGLThread != null) renderMode = mGLThread.getRenderMode();

            mGLThread = new GLThread(mThisWeakRef);
            if (renderMode != RENDERMODE_CONTINUOUSLY) mGLThread.setRenderMode(renderMode);
            mGLThread.start();
        }

        mDetached = false;
    }

    @Override
    protected void onDetachedFromWindow()
    {
        if (mGLThread != null) mGLThread.requestExitAndWait();

        mDetached = true;
        super.onDetachedFromWindow();
    }

    private void checkRenderThreadState()
    {
        if (mGLThread != null) throw new IllegalStateException("setRenderer has already been called for this instance.");
    }

    private class DefaultContextFactory implements EGLContextFactory
    {
        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config)
        {
            int[] attributes = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion, EGL10.EGL_NONE};
            return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, mEGLContextClientVersion != 0 ? attributes : null);
        }

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context)
        {
            if (!egl.eglDestroyContext(display, context))
            {
                Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
                EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
            }
        }
    }

    private static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory
    {
        @Override
        public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow)
        {
            try
            {
                return egl.eglCreateWindowSurface(display, config, nativeWindow, null);
            }
            catch (IllegalArgumentException exception)
            {
                Log.e(TAG, "eglCreateWindowSurface", exception);
                return null;
            }
        }

        @Override
        public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface)
        {
            egl.eglDestroySurface(display, surface);
        }
    }

    private abstract class BaseConfigChooser implements EGLConfigChooser
    {
        protected int[] mConfigSpec;

        BaseConfigChooser(int[] configSpec)
        {
            mConfigSpec = filterConfigSpec(configSpec);
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display)
        {
            int[] configCount = new int[1];
            if (!egl.eglChooseConfig(display, mConfigSpec, null, 0, configCount))
            {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int configTotal = configCount[0];
            if (configTotal <= 0) throw new IllegalArgumentException("No configs match configSpec");

            EGLConfig[] configs = new EGLConfig[configTotal];
            if (!egl.eglChooseConfig(display, mConfigSpec, configs, configTotal, configCount))
            {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }

            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) throw new IllegalArgumentException("No config chosen");
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs);

        private int[] filterConfigSpec(int[] configSpec)
        {
            if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) return configSpec;

            int length = configSpec.length;
            int[] filtered = new int[length + 2];
            System.arraycopy(configSpec, 0, filtered, 0, length - 1);
            filtered[length - 1] = EGL10.EGL_RENDERABLE_TYPE;
            filtered[length] = mEGLContextClientVersion == 2 ? EGL_OPENGL_ES2_BIT : EGL_OPENGL_ES3_BIT_KHR;
            filtered[length + 1] = EGL10.EGL_NONE;
            return filtered;
        }
    }

    private class ComponentSizeChooser extends BaseConfigChooser
    {
        private final int[] mValue = new int[1];

        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;

        ComponentSizeChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize)
        {
            super(new int[] {
                EGL10.EGL_RED_SIZE, redSize, EGL10.EGL_GREEN_SIZE, greenSize, EGL10.EGL_BLUE_SIZE, blueSize, EGL10.EGL_ALPHA_SIZE,
                alphaSize, EGL10.EGL_DEPTH_SIZE, depthSize, EGL10.EGL_STENCIL_SIZE, stencilSize, EGL10.EGL_NONE
            });
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
        }

        @Override
        EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs)
        {
            for (EGLConfig config : configs)
            {
                int depth = findConfigAttribute(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
                int stencil = findConfigAttribute(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
                if (depth < mDepthSize || stencil < mStencilSize) continue;

                int red = findConfigAttribute(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                int green = findConfigAttribute(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                int blue = findConfigAttribute(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                int alpha = findConfigAttribute(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
                if (red == mRedSize && green == mGreenSize && blue == mBlueSize && alpha == mAlphaSize) return config;
            }
            return null;
        }

        private int findConfigAttribute(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue)
        {
            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) return mValue[0];
            return defaultValue;
        }
    }

    private class SimpleEGLConfigChooser extends ComponentSizeChooser
    {
        SimpleEGLConfigChooser(boolean withDepthBuffer)
        {
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

    private static class EglHelper
    {
        private final WeakReference<GLSurfaceView> mGLSurfaceViewWeakRef;

        EGL10 mEgl;
        EGLDisplay mEglDisplay;
        EGLSurface mEglSurface;
        EGLConfig mEglConfig;
        EGLContext mEglContext;

        EglHelper(WeakReference<GLSurfaceView> glSurfaceViewWeakRef)
        {
            mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        void start()
        {
            mEgl = (EGL10) EGLContext.getEGL();
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (mEglDisplay == EGL10.EGL_NO_DISPLAY) throw new RuntimeException("eglGetDisplay failed");

            int[] version = new int[2];
            if (!mEgl.eglInitialize(mEglDisplay, version)) throw new RuntimeException("eglInitialize failed");

            GLSurfaceView view = mGLSurfaceViewWeakRef.get();
            if (view == null)
            {
                mEglConfig = null;
                mEglContext = null;
            }
            else
            {
                mEglConfig = view.mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);
                mEglContext = view.mEGLContextFactory.createContext(mEgl, mEglDisplay, mEglConfig);
            }

            if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT)
            {
                mEglContext = null;
                throwEglException("createContext");
            }

            mEglSurface = null;
        }

        boolean createSurface()
        {
            if (mEgl == null) throw new RuntimeException("egl not initialized");
            if (mEglDisplay == null) throw new RuntimeException("eglDisplay not initialized");
            if (mEglConfig == null) throw new RuntimeException("mEglConfig not initialized");

            destroySurfaceImplementation();

            GLSurfaceView view = mGLSurfaceViewWeakRef.get();
            if (view != null)
            {
                mEglSurface = view.mEGLWindowSurfaceFactory.createWindowSurface(mEgl, mEglDisplay, mEglConfig, view.getHolder());
            }
            else
            {
                mEglSurface = null;
            }

            if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE)
            {
                int error = mEgl.eglGetError();
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                return false;
            }

            if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext))
            {
                logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", mEgl.eglGetError());
                return false;
            }

            return true;
        }

        GL createGL()
        {
            GL gl = mEglContext.getGL();
            GLSurfaceView view = mGLSurfaceViewWeakRef.get();
            if (view == null) return gl;

            if (view.mGLWrapper != null) gl = view.mGLWrapper.wrap(gl);

            if ((view.mDebugFlags & (DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS)) != 0)
            {
                int configFlags = 0;
                Writer log = null;
                if ((view.mDebugFlags & DEBUG_CHECK_GL_ERROR) != 0) configFlags |= GLDebugHelper.CONFIG_CHECK_GL_ERROR;
                if ((view.mDebugFlags & DEBUG_LOG_GL_CALLS) != 0) log = new LogWriter();
                gl = GLDebugHelper.wrap(gl, configFlags, log);
            }

            return gl;
        }

        int swap()
        {
            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) return mEgl.eglGetError();
            return EGL10.EGL_SUCCESS;
        }

        void destroySurface()
        {
            destroySurfaceImplementation();
        }

        private void destroySurfaceImplementation()
        {
            if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) return;

            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            GLSurfaceView view = mGLSurfaceViewWeakRef.get();
            if (view != null) view.mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
            mEglSurface = null;
        }

        void finish()
        {
            if (mEglContext != null)
            {
                GLSurfaceView view = mGLSurfaceViewWeakRef.get();
                if (view != null) view.mEGLContextFactory.destroyContext(mEgl, mEglDisplay, mEglContext);
                mEglContext = null;
            }

            if (mEglDisplay != null)
            {
                mEgl.eglTerminate(mEglDisplay);
                mEglDisplay = null;
            }
        }

        private void throwEglException(String function)
        {
            throwEglException(function, mEgl.eglGetError());
        }

        static void throwEglException(String function, int error)
        {
            throw new RuntimeException(formatEglError(function, error));
        }

        static void logEglErrorAsWarning(String tag, String function, int error)
        {
            Log.w(tag, formatEglError(function, error));
        }

        static String formatEglError(String function, int error)
        {
            return function + " failed: " + GLUtils.getEGLErrorString(error);
        }
    }

    static class GLThread extends Thread
    {
        private final ArrayList<Runnable> mEventQueue = new ArrayList<>();
        private final WeakReference<GLSurfaceView> mGLSurfaceViewWeakRef;

        private boolean mShouldExit;
        private boolean mExited;
        private boolean mRequestPaused;
        private boolean mPaused;
        private boolean mHasSurface;
        private boolean mSurfaceIsBad;
        private boolean mWaitingForSurface;
        private boolean mHaveEglContext;
        private boolean mHaveEglSurface;
        private boolean mFinishedCreatingEglSurface;
        private boolean mShouldReleaseEglContext;
        private int mWidth;
        private int mHeight;
        private int mRenderMode;
        private boolean mRequestRender;
        private boolean mWantRenderNotification;
        private boolean mRenderComplete;
        private boolean mSizeChanged = true;
        private Runnable mFinishDrawingRunnable;
        private EglHelper mEglHelper;

        GLThread(WeakReference<GLSurfaceView> glSurfaceViewWeakRef)
        {
            super();
            mWidth = 0;
            mHeight = 0;
            mRequestRender = true;
            mRenderMode = RENDERMODE_CONTINUOUSLY;
            mWantRenderNotification = false;
            mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        @Override
        public void run()
        {
            setName("GLThread " + getId());

            try
            {
                guardedRun();
            }
            catch (InterruptedException exception)
            {
                Thread.currentThread().interrupt();
            }
            finally
            {
                sGLThreadManager.threadExiting(this);
            }
        }

        private void stopEglSurfaceLocked()
        {
            if (!mHaveEglSurface) return;

            mHaveEglSurface = false;
            mEglHelper.destroySurface();
        }

        private void stopEglContextLocked()
        {
            if (!mHaveEglContext) return;

            mEglHelper.finish();
            mHaveEglContext = false;
            sGLThreadManager.releaseEglContextLocked(this);
        }

        private void guardedRun() throws InterruptedException
        {
            mEglHelper = new EglHelper(mGLSurfaceViewWeakRef);
            mHaveEglContext = false;
            mHaveEglSurface = false;
            mWantRenderNotification = false;

            try
            {
                GL10 gl = null;
                boolean createEglContext = false;
                boolean createEglSurface = false;
                boolean createGlInterface = false;
                boolean lostEglContext = false;
                boolean sizeChanged = false;
                boolean wantRenderNotification = false;
                boolean doRenderNotification = false;
                boolean askedToReleaseEglContext = false;
                int width = 0;
                int height = 0;
                Runnable event = null;
                Runnable finishDrawingRunnable = null;

                while (true)
                {
                    synchronized (sGLThreadManager)
                    {
                        while (true)
                        {
                            if (mShouldExit) return;

                            if (!mEventQueue.isEmpty())
                            {
                                event = mEventQueue.remove(0);
                                break;
                            }

                            boolean pausing = false;
                            if (mPaused != mRequestPaused)
                            {
                                pausing = mRequestPaused;
                                mPaused = mRequestPaused;
                                sGLThreadManager.notifyAll();
                            }

                            if (mShouldReleaseEglContext)
                            {
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                mShouldReleaseEglContext = false;
                                askedToReleaseEglContext = true;
                            }

                            if (lostEglContext)
                            {
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                lostEglContext = false;
                            }

                            if (pausing && mHaveEglSurface) stopEglSurfaceLocked();

                            if (pausing && mHaveEglContext)
                            {
                                GLSurfaceView view = mGLSurfaceViewWeakRef.get();
                                boolean preserveEglContextOnPause = view != null && view.mPreserveEGLContextOnPause;
                                if (!preserveEglContextOnPause) stopEglContextLocked();
                            }

                            if (!mHasSurface && !mWaitingForSurface)
                            {
                                if (mHaveEglSurface) stopEglSurfaceLocked();
                                mWaitingForSurface = true;
                                mSurfaceIsBad = false;
                                sGLThreadManager.notifyAll();
                            }

                            if (mHasSurface && mWaitingForSurface)
                            {
                                mWaitingForSurface = false;
                                sGLThreadManager.notifyAll();
                            }

                            if (doRenderNotification)
                            {
                                mWantRenderNotification = false;
                                doRenderNotification = false;
                                mRenderComplete = true;
                                sGLThreadManager.notifyAll();
                            }

                            if (mFinishDrawingRunnable != null)
                            {
                                finishDrawingRunnable = mFinishDrawingRunnable;
                                mFinishDrawingRunnable = null;
                            }

                            if (readyToDraw())
                            {
                                if (!mHaveEglContext)
                                {
                                    if (askedToReleaseEglContext)
                                    {
                                        askedToReleaseEglContext = false;
                                    }
                                    else
                                    {
                                        try
                                        {
                                            mEglHelper.start();
                                        }
                                        catch (RuntimeException exception)
                                        {
                                            sGLThreadManager.releaseEglContextLocked(this);
                                            throw exception;
                                        }
                                        mHaveEglContext = true;
                                        createEglContext = true;
                                        sGLThreadManager.notifyAll();
                                    }
                                }

                                if (mHaveEglContext && !mHaveEglSurface)
                                {
                                    mHaveEglSurface = true;
                                    createEglSurface = true;
                                    createGlInterface = true;
                                    sizeChanged = true;
                                }

                                if (mHaveEglSurface)
                                {
                                    if (mSizeChanged)
                                    {
                                        sizeChanged = true;
                                        width = mWidth;
                                        height = mHeight;
                                        mWantRenderNotification = true;
                                        createEglSurface = true;
                                        mSizeChanged = false;
                                    }
                                    mRequestRender = false;
                                    sGLThreadManager.notifyAll();
                                    if (mWantRenderNotification) wantRenderNotification = true;
                                    break;
                                }
                            }
                            else if (finishDrawingRunnable != null)
                            {
                                finishDrawingRunnable.run();
                                finishDrawingRunnable = null;
                            }

                            sGLThreadManager.wait();
                        }
                    }

                    if (event != null)
                    {
                        event.run();
                        event = null;
                        continue;
                    }

                    if (createEglSurface)
                    {
                        if (mEglHelper.createSurface())
                        {
                            synchronized (sGLThreadManager)
                            {
                                mFinishedCreatingEglSurface = true;
                                sGLThreadManager.notifyAll();
                            }
                        }
                        else
                        {
                            synchronized (sGLThreadManager)
                            {
                                mFinishedCreatingEglSurface = true;
                                mSurfaceIsBad = true;
                                sGLThreadManager.notifyAll();
                            }
                            continue;
                        }
                        createEglSurface = false;
                    }

                    if (createGlInterface)
                    {
                        gl = (GL10) mEglHelper.createGL();
                        createGlInterface = false;
                    }

                    if (createEglContext)
                    {
                        GLSurfaceView view = mGLSurfaceViewWeakRef.get();
                        if (view != null) view.mRenderer.onSurfaceCreated(gl, mEglHelper.mEglConfig);
                        createEglContext = false;
                    }

                    if (sizeChanged)
                    {
                        GLSurfaceView view = mGLSurfaceViewWeakRef.get();
                        if (view != null) view.mRenderer.onSurfaceChanged(gl, width, height);
                        sizeChanged = false;
                    }

                    GLSurfaceView view = mGLSurfaceViewWeakRef.get();
                    if (view != null)
                    {
                        view.mRenderer.onDrawFrame(gl);
                        if (finishDrawingRunnable != null)
                        {
                            finishDrawingRunnable.run();
                            finishDrawingRunnable = null;
                        }
                    }

                    int swapError = mEglHelper.swap();
                    switch (swapError)
                    {
                        case EGL10.EGL_SUCCESS:
                            break;
                        case EGL11.EGL_CONTEXT_LOST:
                            lostEglContext = true;
                            break;
                        default:
                            EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);
                            synchronized (sGLThreadManager)
                            {
                                mSurfaceIsBad = true;
                                sGLThreadManager.notifyAll();
                            }
                            break;
                    }

                    if (wantRenderNotification)
                    {
                        doRenderNotification = true;
                        wantRenderNotification = false;
                    }
                }
            }
            finally
            {
                synchronized (sGLThreadManager)
                {
                    stopEglSurfaceLocked();
                    stopEglContextLocked();
                }
            }
        }

        public boolean ableToDraw()
        {
            return mHaveEglContext && mHaveEglSurface && readyToDraw();
        }

        private boolean readyToDraw()
        {
            return !mPaused && mHasSurface && !mSurfaceIsBad && mWidth > 0 && mHeight > 0
                    && (mRequestRender || mRenderMode == RENDERMODE_CONTINUOUSLY);
        }

        public void setRenderMode(int renderMode)
        {
            if (renderMode < RENDERMODE_WHEN_DIRTY || renderMode > RENDERMODE_CONTINUOUSLY)
            {
                throw new IllegalArgumentException("renderMode");
            }

            synchronized (sGLThreadManager)
            {
                mRenderMode = renderMode;
                sGLThreadManager.notifyAll();
            }
        }

        public int getRenderMode()
        {
            synchronized (sGLThreadManager)
            {
                return mRenderMode;
            }
        }

        public void requestRender()
        {
            synchronized (sGLThreadManager)
            {
                mRequestRender = true;
                sGLThreadManager.notifyAll();
            }
        }

        public void requestRenderAndNotify(Runnable finishDrawing)
        {
            synchronized (sGLThreadManager)
            {
                if (Thread.currentThread() == this) return;

                mWantRenderNotification = true;
                mRequestRender = true;
                mRenderComplete = false;
                mFinishDrawingRunnable = finishDrawing;
                sGLThreadManager.notifyAll();
            }
        }

        public void surfaceCreated()
        {
            synchronized (sGLThreadManager)
            {
                mHasSurface = true;
                mFinishedCreatingEglSurface = false;
                sGLThreadManager.notifyAll();
                while (mWaitingForSurface && !mFinishedCreatingEglSurface && !mExited) waitForManager();
            }
        }

        public void surfaceDestroyed()
        {
            synchronized (sGLThreadManager)
            {
                mHasSurface = false;
                sGLThreadManager.notifyAll();
                while (!mWaitingForSurface && !mExited) waitForManager();
            }
        }

        public void onPause()
        {
            synchronized (sGLThreadManager)
            {
                mRequestPaused = true;
                sGLThreadManager.notifyAll();
                while (!mExited && !mPaused) waitForManager();
            }
        }

        public void onResume()
        {
            synchronized (sGLThreadManager)
            {
                mRequestPaused = false;
                mRequestRender = true;
                mRenderComplete = false;
                sGLThreadManager.notifyAll();
                while (!mExited && mPaused && !mRenderComplete) waitForManager();
            }
        }

        public void onWindowResize(int width, int height)
        {
            synchronized (sGLThreadManager)
            {
                mWidth = width;
                mHeight = height;
                mSizeChanged = true;
                mRequestRender = true;
                mRenderComplete = false;

                if (Thread.currentThread() == this) return;

                sGLThreadManager.notifyAll();
                while (!mExited && !mPaused && !mRenderComplete && ableToDraw()) waitForManager();
            }
        }

        public void requestExitAndWait()
        {
            synchronized (sGLThreadManager)
            {
                mShouldExit = true;
                sGLThreadManager.notifyAll();
                while (!mExited) waitForManager();
            }
        }

        public void requestReleaseEglContextLocked()
        {
            mShouldReleaseEglContext = true;
            sGLThreadManager.notifyAll();
        }

        public void queueEvent(Runnable runnable)
        {
            if (runnable == null) throw new IllegalArgumentException("r must not be null");

            synchronized (sGLThreadManager)
            {
                mEventQueue.add(runnable);
                sGLThreadManager.notifyAll();
            }
        }

        private void waitForManager()
        {
            try
            {
                sGLThreadManager.wait();
            }
            catch (InterruptedException exception)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class LogWriter extends Writer
    {
        private final StringBuilder mBuilder = new StringBuilder();

        @Override
        public void close()
        {
            flushBuilder();
        }

        @Override
        public void flush()
        {
            flushBuilder();
        }

        @Override
        public void write(char[] buffer, int offset, int count)
        {
            for (int index = 0; index < count; index++)
            {
                char character = buffer[offset + index];
                if (character == '\n') flushBuilder();
                else mBuilder.append(character);
            }
        }

        private void flushBuilder()
        {
            if (mBuilder.length() == 0) return;

            Log.v("GLSurfaceView", mBuilder.toString());
            mBuilder.delete(0, mBuilder.length());
        }
    }

    private static class GLThreadManager
    {
        public synchronized void threadExiting(GLThread thread)
        {
            thread.mExited = true;
            notifyAll();
        }

        public void releaseEglContextLocked(GLThread thread)
        {
            notifyAll();
        }
    }
}
