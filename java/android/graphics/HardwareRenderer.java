package android.graphics;

import android.view.Surface;

import java.util.concurrent.Executor;

public class HardwareRenderer
{
    public static final int SYNC_OK = 0;
    public static final int SYNC_REDRAW_REQUESTED = 1;
    public static final int SYNC_LOST_SURFACE_REWARD_IF_FOUND = 2;
    public static final int SYNC_CONTEXT_IS_STOPPED = 4;
    public static final int SYNC_FRAME_DROPPED = 8;

    private static volatile boolean sDrawingEnabled = true;

    private final FrameRenderRequest mRenderRequest = new FrameRenderRequest();

    private String mName;
    private RenderNode mContentRoot;
    private Surface mSurface;
    private boolean mOpaque = true;
    private boolean mStopped;
    private boolean mDestroyed;
    private float mLightX;
    private float mLightY;
    private float mLightZ;
    private float mLightRadius;
    private float mAmbientShadowAlpha;
    private float mSpotShadowAlpha;

    public HardwareRenderer()
    {
    }

    public void destroy()
    {
        mDestroyed = true;
        mSurface = null;
        mContentRoot = null;
    }

    public void setName(String name)
    {
        mName = name;
    }

    public void setLightSourceGeometry(float lightX, float lightY, float lightZ, float lightRadius)
    {
        mLightX = lightX;
        mLightY = lightY;
        mLightZ = lightZ;
        mLightRadius = lightRadius;
    }

    public void setLightSourceAlpha(float ambientShadowAlpha, float spotShadowAlpha)
    {
        mAmbientShadowAlpha = ambientShadowAlpha;
        mSpotShadowAlpha = spotShadowAlpha;
    }

    public void setContentRoot(RenderNode content)
    {
        mContentRoot = content;
    }

    public void setSurface(Surface surface)
    {
        if (surface != null && !surface.isValid()) throw new IllegalArgumentException("Surface is invalid. surface.isValid() == false.");

        mSurface = surface;
    }

    public FrameRenderRequest createRenderRequest()
    {
        mRenderRequest.reset();
        return mRenderRequest;
    }

    public void stop()
    {
        mStopped = true;
    }

    public void start()
    {
        mStopped = false;
    }

    public void clearContent()
    {
        mContentRoot = null;
    }

    public void notifyFramePending()
    {
    }

    public void setOpaque(boolean opaque)
    {
        mOpaque = opaque;
    }

    public boolean isOpaque()
    {
        return mOpaque;
    }

    public static boolean isDrawingEnabled()
    {
        return sDrawingEnabled;
    }

    public static void setDrawingEnabled(boolean drawingEnabled)
    {
        sDrawingEnabled = drawingEnabled;
    }

    private int drawFrame()
    {
        if (mDestroyed || mStopped) return SYNC_CONTEXT_IS_STOPPED;

        Surface surface = mSurface;
        if (surface == null || !surface.isValid()) return SYNC_LOST_SURFACE_REWARD_IF_FOUND;
        if (!sDrawingEnabled || mContentRoot == null) return SYNC_OK;

        Canvas canvas;
        try
        {
            canvas = surface.lockCanvas(null);
        }
        catch (IllegalStateException | IllegalArgumentException exception)
        {
            return SYNC_FRAME_DROPPED;
        }

        if (canvas == null) return SYNC_FRAME_DROPPED;

        try
        {
            if (!mOpaque) canvas.drawColor(0, BlendMode.CLEAR);
            canvas.drawRenderNode(mContentRoot);
        }
        finally
        {
            surface.unlockCanvasAndPost(canvas);
        }

        return SYNC_OK;
    }

    public final class FrameRenderRequest
    {
        private long mVsyncTime;
        private Executor mCommitExecutor;
        private Runnable mCommitCallback;
        private boolean mWaitForPresent;

        FrameRenderRequest()
        {
        }

        void reset()
        {
            mVsyncTime = System.nanoTime();
            mCommitExecutor = null;
            mCommitCallback = null;
            mWaitForPresent = false;
        }

        public FrameRenderRequest setVsyncTime(long vsyncTime)
        {
            mVsyncTime = vsyncTime;
            return this;
        }

        public FrameRenderRequest setFrameCommitCallback(Executor executor, Runnable frameCommitCallback)
        {
            mCommitExecutor = executor;
            mCommitCallback = frameCommitCallback;
            return this;
        }

        public FrameRenderRequest setWaitForPresent(boolean shouldWait)
        {
            mWaitForPresent = shouldWait;
            return this;
        }

        public int syncAndDraw()
        {
            int result = drawFrame();

            Executor executor = mCommitExecutor;
            Runnable callback = mCommitCallback;
            reset();

            if (callback != null)
            {
                if (executor != null) executor.execute(callback);
                else callback.run();
            }

            return result;
        }
    }
}
