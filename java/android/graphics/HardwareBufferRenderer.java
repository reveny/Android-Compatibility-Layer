package android.graphics;

import android.hardware.HardwareBuffer;
import android.hardware.SyncFence;
import android.os.Parcel;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class HardwareBufferRenderer implements AutoCloseable
{
    private final HardwareBuffer mHardwareBuffer;
    private final RenderRequest mRenderRequest = new RenderRequest();

    private RenderNode mContentRoot;
    private boolean mClosed;
    private float mLightX;
    private float mLightY;
    private float mLightZ;
    private float mLightRadius;
    private float mAmbientShadowAlpha;
    private float mSpotShadowAlpha;

    public HardwareBufferRenderer(HardwareBuffer buffer)
    {
        if (buffer == null) throw new NullPointerException("buffer must not be null");

        mHardwareBuffer = buffer;
    }

    public void setContentRoot(RenderNode content)
    {
        mContentRoot = content;
    }

    public RenderRequest obtainRenderRequest()
    {
        mRenderRequest.reset();
        return mRenderRequest;
    }

    public boolean isClosed()
    {
        return mClosed;
    }

    @Override
    public void close()
    {
        mClosed = true;
        mContentRoot = null;
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

    private static SyncFence signalledFence()
    {
        Parcel parcel = Parcel.obtain();

        try
        {
            parcel.writeBoolean(true);
            parcel.writeLong(System.nanoTime());
            parcel.setDataPosition(0);
            return SyncFence.CREATOR.createFromParcel(parcel);
        }
        finally
        {
            parcel.recycle();
        }
    }

    public final class RenderRequest
    {
        private ColorSpace mColorSpace;
        private int mTransform;

        RenderRequest()
        {
        }

        void reset()
        {
            mColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
            mTransform = 0;
        }

        public void draw(Executor executor, Consumer<RenderResult> renderCallback)
        {
            if (executor == null || renderCallback == null) throw new NullPointerException("executor and callback must not be null");
            if (mClosed) throw new IllegalStateException("Attempt to draw with a HardwareBufferRenderer that has already been closed");

            int status = mHardwareBuffer.isClosed() ? RenderResult.ERROR_UNKNOWN : RenderResult.SUCCESS;
            final RenderResult result = new RenderResult(signalledFence(), status);
            executor.execute(() -> renderCallback.accept(result));
        }

        public RenderRequest setColorSpace(ColorSpace colorSpace)
        {
            mColorSpace = colorSpace == null ? ColorSpace.get(ColorSpace.Named.SRGB) : colorSpace;
            return this;
        }

        public RenderRequest setBufferTransform(int bufferTransform)
        {
            mTransform = bufferTransform;
            return this;
        }
    }

    public static final class RenderResult
    {
        public static final int SUCCESS = 0;
        public static final int ERROR_UNKNOWN = 1;

        private final SyncFence mFence;
        private final int mStatus;

        RenderResult()
        {
            this(signalledFence(), SUCCESS);
        }

        RenderResult(SyncFence fence, int status)
        {
            mFence = fence;
            mStatus = status;
        }

        public SyncFence getFence()
        {
            return mFence;
        }

        public int getStatus()
        {
            return mStatus;
        }
    }
}
