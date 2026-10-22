package android.view;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.HardwareBuffer;
import android.hardware.SyncFence;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class SurfaceControl implements Parcelable
{
    public static final int BUFFER_TRANSFORM_IDENTITY = 0;
    public static final int BUFFER_TRANSFORM_MIRROR_HORIZONTAL = 1;
    public static final int BUFFER_TRANSFORM_MIRROR_VERTICAL = 2;
    public static final int BUFFER_TRANSFORM_ROTATE_180 = 3;
    public static final int BUFFER_TRANSFORM_ROTATE_90 = 4;
    public static final int BUFFER_TRANSFORM_ROTATE_270 = 7;

    public static final Parcelable.Creator<SurfaceControl> CREATOR = new Parcelable.Creator<SurfaceControl>()
    {
        @Override
        public SurfaceControl createFromParcel(Parcel source)
        {
            SurfaceControl control = new SurfaceControl();
            control.readFromParcel(source);
            return control;
        }

        @Override
        public SurfaceControl[] newArray(int size)
        {
            return new SurfaceControl[size];
        }
    };

    private String mName;
    private boolean mValid;
    private int mWidth;
    private int mHeight;
    private int mFormat;
    private boolean mOpaque;
    private boolean mVisible = true;
    private SurfaceControl mParent;
    private float mPositionX;
    private float mPositionY;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private float mAlpha = 1.0f;
    private int mLayer;
    private final Rect mCrop = new Rect();
    private int mBufferTransform;
    private int mDataSpace;
    private float mFrameRate;
    private float mDesiredHdrHeadroom;
    private HardwareBuffer mBuffer;
    private Consumer<SyncFence> mBufferRelease;
    private Bitmap mSoftwareBuffer;
    private boolean mSoftwarePosted;
    private Surface.BufferProducer mProducer;

    SurfaceControl()
    {
    }

    SurfaceControl(String name, int width, int height, int format, boolean opaque, boolean hidden, SurfaceControl parent)
    {
        mName = name;
        mWidth = width;
        mHeight = height;
        mFormat = format;
        mOpaque = opaque;
        mVisible = !hidden;
        mParent = parent;
        mValid = true;
    }

    String getName()
    {
        return mName;
    }

    synchronized void setBufferSize(int width, int height)
    {
        mWidth = width;
        mHeight = height;
    }

    synchronized Surface.BufferProducer bufferProducer()
    {
        if (mProducer != null) return mProducer;

        mProducer = new Surface.BufferProducer()
        {
            @Override
            Bitmap lockBuffer()
            {
                synchronized (SurfaceControl.this)
                {
                    if (!mValid || mWidth <= 0 || mHeight <= 0) return null;

                    if (mSoftwareBuffer == null || mSoftwareBuffer.getWidth() != mWidth || mSoftwareBuffer.getHeight() != mHeight)
                    {
                        mSoftwareBuffer = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                    }

                    return mSoftwareBuffer;
                }
            }

            @Override
            void unlockAndPost(Bitmap buffer)
            {
                synchronized (SurfaceControl.this)
                {
                    mSoftwarePosted = true;
                }
            }

            @Override
            Bitmap peekFrontBuffer()
            {
                synchronized (SurfaceControl.this)
                {
                    return mSoftwarePosted ? mSoftwareBuffer : null;
                }
            }
        };

        return mProducer;
    }

    public synchronized boolean isValid()
    {
        return mValid;
    }

    public void release()
    {
        Consumer<SyncFence> release;

        synchronized (this)
        {
            mValid = false;
            mSoftwareBuffer = null;
            release = mBufferRelease;
            mBufferRelease = null;
            mBuffer = null;
        }

        if (release != null) release.accept(null);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public synchronized void writeToParcel(Parcel out, int flags)
    {
        out.writeString(mName);
        out.writeInt(mValid ? 1 : 0);
        out.writeInt(mWidth);
        out.writeInt(mHeight);
        out.writeInt(mFormat);
        out.writeInt(mOpaque ? 1 : 0);
        out.writeInt(mVisible ? 1 : 0);

        if ((flags & PARCELABLE_WRITE_RETURN_VALUE) != 0) mValid = false;
    }

    public synchronized void readFromParcel(Parcel source)
    {
        if (source == null) throw new IllegalArgumentException("source must not be null");

        mName = source.readString();
        mValid = source.readInt() != 0;
        mWidth = source.readInt();
        mHeight = source.readInt();
        mFormat = source.readInt();
        mOpaque = source.readInt() != 0;
        mVisible = source.readInt() != 0;
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            release();
        }
        finally
        {
            super.finalize();
        }
    }

    @Override
    public String toString()
    {
        return "Surface(name=" + mName + ")/@0x" + Integer.toHexString(System.identityHashCode(this));
    }

    public static class Builder
    {
        private String mName;
        private int mWidth;
        private int mHeight;
        private int mFormat = android.graphics.PixelFormat.OPAQUE;
        private boolean mOpaque;
        private boolean mHidden;
        private SurfaceControl mParent;

        public Builder()
        {
        }

        public SurfaceControl build()
        {
            if (mWidth < 0 || mHeight < 0) throw new IllegalStateException("width and height must be positive or unset");
            if (mName == null) throw new IllegalStateException("name must be set");

            return new SurfaceControl(mName, mWidth, mHeight, mFormat, mOpaque, mHidden, mParent);
        }

        public Builder setName(String name)
        {
            if (name == null) throw new IllegalArgumentException("name must not be null");

            mName = name;
            return this;
        }

        public Builder setBufferSize(int width, int height)
        {
            if (width < 0 || height < 0) throw new IllegalArgumentException("width and height must be positive");

            mWidth = width;
            mHeight = height;
            return this;
        }

        public Builder setFormat(int format)
        {
            mFormat = format;
            return this;
        }

        public Builder setOpaque(boolean opaque)
        {
            mOpaque = opaque;
            return this;
        }

        public Builder setHidden(boolean hidden)
        {
            mHidden = hidden;
            return this;
        }

        public Builder setParent(SurfaceControl parent)
        {
            mParent = parent;
            return this;
        }
    }

    public interface TransactionCommittedListener
    {
        void onTransactionCommitted();
    }

    public static final class TrustedPresentationThresholds
    {
        private final float mMinAlpha;
        private final float mMinFractionRendered;
        private final int mStabilityRequirementMs;

        public TrustedPresentationThresholds(float minAlpha, float minFractionRendered, int stabilityRequirementMs)
        {
            if (minAlpha <= 0 || minAlpha > 1 || minFractionRendered <= 0 || minFractionRendered > 1 || stabilityRequirementMs < 1)
            {
                throw new IllegalArgumentException("TrustedPresentationThresholds values are invalid");
            }

            mMinAlpha = minAlpha;
            mMinFractionRendered = minFractionRendered;
            mStabilityRequirementMs = stabilityRequirementMs;
        }
    }

    public static class JankData
    {
        public static final int JANK_NONE = 0;
        public static final int JANK_COMPOSER = 1;
        public static final int JANK_APPLICATION = 2;
        public static final int JANK_OTHER = 4;

        private final long mVsyncId;
        private final int mJankType;
        private final long mScheduledAppFrameTimeNanos;
        private final long mActualAppFrameTimeNanos;

        JankData(long vsyncId, int jankType, long scheduledAppFrameTimeNanos, long actualAppFrameTimeNanos)
        {
            mVsyncId = vsyncId;
            mJankType = jankType;
            mScheduledAppFrameTimeNanos = scheduledAppFrameTimeNanos;
            mActualAppFrameTimeNanos = actualAppFrameTimeNanos;
        }

        public long getVsyncId()
        {
            return mVsyncId;
        }

        public int getJankType()
        {
            return mJankType;
        }

        public long getScheduledAppFrameTimeNanos()
        {
            return mScheduledAppFrameTimeNanos;
        }

        public long getActualAppFrameTimeNanos()
        {
            return mActualAppFrameTimeNanos;
        }

        @Override
        public String toString()
        {
            return "JankData{vsync=" + mVsyncId + ", jankType=0x" + Integer.toHexString(mJankType) + ", scheduledAppFrameTime="
                   + mScheduledAppFrameTimeNanos + ", actualAppFrameTime=" + mActualAppFrameTimeNanos + "}";
        }
    }

    public interface OnJankDataListener
    {
        void onJankDataAvailable(List<JankData> jankData);
    }

    public static class OnJankDataListenerRegistration
    {
        private boolean mRemoved;

        OnJankDataListenerRegistration()
        {
        }

        public void flush()
        {
        }

        public void removeAfter(long afterVsync)
        {
            mRemoved = true;
        }
    }

    private interface Operation
    {
        void apply();
    }

    public static class Transaction implements Closeable, Parcelable
    {
        public static final Parcelable.Creator<Transaction> CREATOR = new Parcelable.Creator<Transaction>()
        {
            @Override
            public Transaction createFromParcel(Parcel source)
            {
                source.readInt();
                return new Transaction();
            }

            @Override
            public Transaction[] newArray(int size)
            {
                return new Transaction[size];
            }
        };

        private final ArrayList<Operation> mOperations = new ArrayList<>();
        private final ArrayList<Runnable> mCommittedCallbacks = new ArrayList<>();

        public Transaction()
        {
        }

        private Transaction add(SurfaceControl control, Operation operation)
        {
            if (control == null) throw new IllegalArgumentException("the surface control must not be null");

            synchronized (this)
            {
                mOperations.add(operation);
            }

            return this;
        }

        public void apply()
        {
            ArrayList<Operation> operations;
            ArrayList<Runnable> callbacks;

            synchronized (this)
            {
                operations = new ArrayList<>(mOperations);
                callbacks = new ArrayList<>(mCommittedCallbacks);
                mOperations.clear();
                mCommittedCallbacks.clear();
            }

            for (Operation operation : operations)
            {
                operation.apply();
            }

            for (Runnable callback : callbacks)
            {
                callback.run();
            }
        }

        @Override
        public void close()
        {
            synchronized (this)
            {
                mOperations.clear();
                mCommittedCallbacks.clear();
            }
        }

        public Transaction merge(Transaction other)
        {
            if (other == null || other == this) return this;

            ArrayList<Operation> operations;
            ArrayList<Runnable> callbacks;

            synchronized (other)
            {
                operations = new ArrayList<>(other.mOperations);
                callbacks = new ArrayList<>(other.mCommittedCallbacks);
                other.mOperations.clear();
                other.mCommittedCallbacks.clear();
            }

            synchronized (this)
            {
                mOperations.addAll(operations);
                mCommittedCallbacks.addAll(callbacks);
            }

            return this;
        }

        public Transaction addTransactionCommittedListener(Executor executor, TransactionCommittedListener listener)
        {
            if (executor == null || listener == null) throw new IllegalArgumentException("executor and listener must not be null");

            synchronized (this)
            {
                mCommittedCallbacks.add(() -> executor.execute(listener::onTransactionCommitted));
            }

            return this;
        }

        public Transaction setVisibility(SurfaceControl control, boolean visible)
        {
            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mVisible = visible;
                }
            });
        }

        public Transaction setPosition(SurfaceControl control, float x, float y)
        {
            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mPositionX = x;
                    control.mPositionY = y;
                }
            });
        }

        public Transaction setScale(SurfaceControl control, float scaleX, float scaleY)
        {
            if (scaleX < 0 || scaleY < 0) throw new IllegalArgumentException("negative scale");

            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mScaleX = scaleX;
                    control.mScaleY = scaleY;
                }
            });
        }

        public Transaction setBufferSize(SurfaceControl control, int width, int height)
        {
            if (width < 0 || height < 0) throw new IllegalArgumentException("negative buffer size");

            return add(control, () -> control.setBufferSize(width, height));
        }

        public Transaction setGeometry(SurfaceControl control, Rect sourceCrop, Rect destFrame, int orientation)
        {
            Rect crop = sourceCrop == null ? null : new Rect(sourceCrop);
            Rect frame = destFrame == null ? null : new Rect(destFrame);

            return add(control, () ->
            {
                synchronized (control)
                {
                    if (crop != null) control.mCrop.set(crop);
                    else control.mCrop.setEmpty();

                    if (frame != null)
                    {
                        control.mPositionX = frame.left;
                        control.mPositionY = frame.top;
                    }

                    control.mBufferTransform = orientation;
                }
            });
        }

        public Transaction setCrop(SurfaceControl control, Rect crop)
        {
            if (crop != null && (crop.width() < 0 || crop.height() < 0)) throw new IllegalArgumentException("crop must be a valid rect");

            Rect copy = crop == null ? null : new Rect(crop);

            return add(control, () ->
            {
                synchronized (control)
                {
                    if (copy == null) control.mCrop.setEmpty();
                    else control.mCrop.set(copy);
                }
            });
        }

        public Transaction setAlpha(SurfaceControl control, float alpha)
        {
            if (alpha < 0 || alpha > 1) throw new IllegalArgumentException("alpha must be between 0 and 1");

            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mAlpha = alpha;
                }
            });
        }

        public Transaction setLayer(SurfaceControl control, int z)
        {
            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mLayer = z;
                }
            });
        }

        public Transaction setOpaque(SurfaceControl control, boolean isOpaque)
        {
            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mOpaque = isOpaque;
                }
            });
        }

        public Transaction setBufferTransform(SurfaceControl control, int transform)
        {
            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mBufferTransform = transform;
                }
            });
        }

        public Transaction setDataSpace(SurfaceControl control, int dataSpace)
        {
            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mDataSpace = dataSpace;
                }
            });
        }

        public Transaction setDamageRegion(SurfaceControl control, Region region)
        {
            return add(control, () ->
            {
            });
        }

        public Transaction setContentPriority(SurfaceControl control, int priority)
        {
            return add(control, () ->
            {
            });
        }

        public Transaction setDesiredPresentTimeNanos(long desiredPresentTimeNanos)
        {
            return this;
        }

        public Transaction setFrameTimeline(long vsyncId)
        {
            return this;
        }

        public Transaction setExtendedRangeBrightness(SurfaceControl control, float currentBufferRatio, float desiredRatio)
        {
            if (!Float.isFinite(currentBufferRatio) || currentBufferRatio < 1.0f)
            {
                throw new IllegalArgumentException("currentBufferRatio must be finite and >= 1.0f");
            }

            if (!Float.isFinite(desiredRatio) || desiredRatio < 1.0f)
            {
                throw new IllegalArgumentException("desiredRatio must be finite and >= 1.0f");
            }

            return add(control, () ->
            {
            });
        }

        public Transaction setDesiredHdrHeadroom(SurfaceControl control, float desiredRatio)
        {
            if (!Float.isFinite(desiredRatio) || (desiredRatio != 0 && desiredRatio < 1.0f))
            {
                throw new IllegalArgumentException("desiredRatio must be finite and either 0 or >= 1.0f");
            }

            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mDesiredHdrHeadroom = desiredRatio;
                }
            });
        }

        public Transaction setFrameRate(SurfaceControl control, float frameRate, int compatibility)
        {
            return setFrameRate(control, frameRate, compatibility, Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS);
        }

        public Transaction setFrameRate(SurfaceControl control, float frameRate, int compatibility, int changeFrameRateStrategy)
        {
            if (Float.isNaN(frameRate) || Float.isInfinite(frameRate) || frameRate < 0)
            {
                throw new IllegalArgumentException("invalid frame rate " + frameRate);
            }

            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mFrameRate = frameRate;
                }
            });
        }

        public Transaction clearFrameRate(SurfaceControl control)
        {
            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mFrameRate = 0;
                }
            });
        }

        public Transaction setBuffer(SurfaceControl control, HardwareBuffer buffer)
        {
            return setBuffer(control, buffer, null);
        }

        public Transaction setBuffer(SurfaceControl control, HardwareBuffer buffer, SyncFence fence)
        {
            return setBuffer(control, buffer, fence, null);
        }

        public Transaction setBuffer(SurfaceControl control, HardwareBuffer buffer, SyncFence fence, Consumer<SyncFence> releaseCallback)
        {
            return add(control, () ->
            {
                Consumer<SyncFence> previous;

                synchronized (control)
                {
                    previous = control.mBufferRelease;
                    control.mBuffer = buffer;
                    control.mBufferRelease = releaseCallback;
                }

                if (previous != null) previous.accept(null);
            });
        }

        public Transaction reparent(SurfaceControl control, SurfaceControl newParent)
        {
            return add(control, () ->
            {
                synchronized (control)
                {
                    control.mParent = newParent;
                }
            });
        }

        public Transaction setTrustedPresentationCallback(SurfaceControl control, TrustedPresentationThresholds thresholds,
                                                          Executor executor, Consumer<Boolean> listener)
        {
            if (thresholds == null || executor == null || listener == null) throw new IllegalArgumentException("arguments must not be null");

            return add(control, () ->
            {
            });
        }

        public Transaction clearTrustedPresentationCallback(SurfaceControl control)
        {
            return add(control, () ->
            {
            });
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            out.writeInt(0);
        }
    }
}
