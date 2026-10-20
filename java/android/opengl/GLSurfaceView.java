package android.opengl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
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
        EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config);

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

    private final List<Runnable> mQueuedEvents = new ArrayList<>();

    private final android.os.Handler mHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final EGLConfig mConfig = new RuntimeEglConfig();

    private Renderer mRenderer;
    private EGLConfigChooser mConfigChooser;
    private EGLContextFactory mContextFactory;
    private EGLWindowSurfaceFactory mWindowSurfaceFactory;
    private GLWrapper mWrapper;
    private int mRenderMode = RENDERMODE_CONTINUOUSLY;
    private int mDebugFlags;
    private int mContextClientVersion;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private boolean mPreserveContextOnPause;
    private boolean mDetachedFromWindow;
    private boolean mSurfaceReported;
    private boolean mPaused;

    public GLSurfaceView(Context context)
    {
        super(context);
        getHolder().addCallback(this);
    }

    public GLSurfaceView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
        getHolder().addCallback(this);
    }

    public void setRenderer(Renderer renderer)
    {
        if (mRenderer != null) throw new IllegalStateException("setRenderer has already been called for this instance.");
        if (renderer == null) throw new IllegalArgumentException("renderer must not be null");

        mRenderer = renderer;

        android.util.Log.w(TAG, "the renderer will be called, but no driver stands behind it, so nothing it draws reaches the screen");

        if (mSurfaceWidth > 0 && mSurfaceHeight > 0) startRenderer();
    }

    public void setGLWrapper(GLWrapper wrapper)
    {
        mWrapper = wrapper;
    }

    public void setEGLConfigChooser(EGLConfigChooser chooser)
    {
        checkRendererNotSet();
        mConfigChooser = chooser;
    }

    public void setEGLConfigChooser(boolean needDepth)
    {
        checkRendererNotSet();
    }

    public void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize)
    {
        checkRendererNotSet();
    }

    public void setEGLContextFactory(EGLContextFactory factory)
    {
        checkRendererNotSet();
        mContextFactory = factory;
    }

    public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory)
    {
        checkRendererNotSet();
        mWindowSurfaceFactory = factory;
    }

    public void setEGLContextClientVersion(int version)
    {
        checkRendererNotSet();
        mContextClientVersion = version;
    }

    public void setDebugFlags(int debugFlags)
    {
        mDebugFlags = debugFlags;
    }

    public int getDebugFlags()
    {
        return mDebugFlags;
    }

    public void setPreserveEGLContextOnPause(boolean preserve)
    {
        mPreserveContextOnPause = preserve;
    }

    public boolean getPreserveEGLContextOnPause()
    {
        return mPreserveContextOnPause;
    }

    public void setRenderMode(int renderMode)
    {
        if (renderMode != RENDERMODE_WHEN_DIRTY && renderMode != RENDERMODE_CONTINUOUSLY)
            throw new IllegalArgumentException("renderMode");

        mRenderMode = renderMode;
    }

    public int getRenderMode()
    {
        return mRenderMode;
    }

    public void requestRender()
    {
        if (mRenderer == null || !mSurfaceReported || mPaused) return;

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                drainQueuedEvents();
                if (mRenderer != null && !mPaused) mRenderer.onDrawFrame(RuntimeGl10.INSTANCE);
            }
        });
    }

    public void queueEvent(Runnable event)
    {
        if (event == null) return;

        synchronized (mQueuedEvents)
        {
            mQueuedEvents.add(event);
        }
    }

    public void onPause()
    {
        mPaused = true;
    }

    public void onResume()
    {
        mPaused = false;
        requestRender();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        if (width <= 0 || height <= 0) return;

        mSurfaceWidth = width;
        mSurfaceHeight = height;

        startRenderer();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        mSurfaceWidth = 0;
        mSurfaceHeight = 0;
        mSurfaceReported = false;
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder)
    {
    }

    private void startRenderer()
    {
        if (mRenderer == null || mSurfaceReported || mDetachedFromWindow) return;
        if (mSurfaceWidth <= 0 || mSurfaceHeight <= 0) return;

        mSurfaceReported = true;

        final int width = mSurfaceWidth;
        final int height = mSurfaceHeight;

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Renderer renderer = mRenderer;
                if (renderer == null || mDetachedFromWindow) return;

                renderer.onSurfaceCreated(RuntimeGl10.INSTANCE, mConfig);
                renderer.onSurfaceChanged(RuntimeGl10.INSTANCE, width, height);

                drainQueuedEvents();

                if (!mPaused) renderer.onDrawFrame(RuntimeGl10.INSTANCE);
            }
        });
    }

    private void drainQueuedEvents()
    {
        List<Runnable> pending;

        synchronized (mQueuedEvents)
        {
            if (mQueuedEvents.isEmpty()) return;

            pending = new ArrayList<>(mQueuedEvents);
            mQueuedEvents.clear();
        }

        for (Runnable event : pending) event.run();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        mDetachedFromWindow = true;
        super.onDetachedFromWindow();
    }

    private void checkRendererNotSet()
    {
        if (mRenderer != null) throw new IllegalStateException("setRenderer has already been called for this instance.");
    }

    private static final class RuntimeEglConfig extends EGLConfig
    {
    }
}
