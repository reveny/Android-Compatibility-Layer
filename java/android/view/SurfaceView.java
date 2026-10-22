package android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.IBinder;
import android.util.AttributeSet;

import java.util.ArrayList;

public class SurfaceView extends View
{
    public static final int SURFACE_LIFECYCLE_DEFAULT = 0;
    public static final int SURFACE_LIFECYCLE_FOLLOWS_VISIBILITY = 1;
    public static final int SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT = 2;

    private static final int MEDIA_SUBLAYER = -2;
    private static final int MEDIA_OVERLAY_SUBLAYER = -1;
    private static final int APPLICATION_PANEL_SUBLAYER = 1;

    private final Surface mSurface = new Surface();
    private final Holder mHolder = new Holder();
    private final ArrayList<SurfaceHolder.Callback> mCallbacks = new ArrayList<>();
    private final Object mBufferLock = new Object();
    private final Paint mBufferPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private boolean mCreated;
    private boolean mCreating;
    private boolean mLaidOut;
    private int mFormat = 4;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mRequestedWidth = -1;
    private int mRequestedHeight = -1;
    private int mSurfaceLifecycle = SURFACE_LIFECYCLE_DEFAULT;
    private int mSubLayer = MEDIA_SUBLAYER;
    private boolean mSecure;
    private float mDesiredHdrHeadroom;
    private SurfaceControl mSurfaceControl;
    private Bitmap mFrontBuffer;
    private Bitmap mBackBuffer;
    private boolean mSoftwarePosted;

    private final Surface.BufferProducer mProducer = new Surface.BufferProducer()
    {
        @Override
        Bitmap lockBuffer()
        {
            synchronized (mBufferLock)
            {
                if (!mCreated || mSurfaceWidth <= 0 || mSurfaceHeight <= 0) return null;

                if (mBackBuffer == null || mBackBuffer.getWidth() != mSurfaceWidth || mBackBuffer.getHeight() != mSurfaceHeight)
                {
                    mBackBuffer = Bitmap.createBitmap(mSurfaceWidth, mSurfaceHeight, Bitmap.Config.ARGB_8888);
                }

                if (mFrontBuffer != null && mFrontBuffer.getWidth() == mSurfaceWidth && mFrontBuffer.getHeight() == mSurfaceHeight)
                {
                    new Canvas(mBackBuffer).drawBitmap(mFrontBuffer, 0, 0, null);
                }

                return mBackBuffer;
            }
        }

        @Override
        void unlockAndPost(Bitmap buffer)
        {
            synchronized (mBufferLock)
            {
                if (buffer == mBackBuffer)
                {
                    mBackBuffer = mFrontBuffer;
                    mFrontBuffer = buffer;
                }

                mSoftwarePosted = true;
            }

            postInvalidate();
        }

        @Override
        Bitmap peekFrontBuffer()
        {
            synchronized (mBufferLock)
            {
                return mSoftwarePosted ? mFrontBuffer : null;
            }
        }
    };

    public SurfaceView(Context context)
    {
        this(context, null);
    }

    public SurfaceView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public SurfaceView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public SurfaceView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mSurface.setBufferProducer(mProducer);
    }

    public SurfaceHolder getHolder()
    {
        return mHolder;
    }

    boolean presentsThroughFramework()
    {
        synchronized (mBufferLock)
        {
            return mSoftwarePosted;
        }
    }

    public void setZOrderOnTop(boolean onTop)
    {
        mSubLayer = onTop ? APPLICATION_PANEL_SUBLAYER : MEDIA_SUBLAYER;
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay)
    {
        mSubLayer = isMediaOverlay ? MEDIA_OVERLAY_SUBLAYER : MEDIA_SUBLAYER;
    }

    public void setCompositionOrder(int compositionOrder)
    {
        mSubLayer = compositionOrder;
    }

    public int getCompositionOrder()
    {
        return mSubLayer;
    }

    public void setSecure(boolean isSecure)
    {
        mSecure = isSecure;
    }

    public void setSurfaceLifecycle(int lifecycleStrategy)
    {
        if (lifecycleStrategy < SURFACE_LIFECYCLE_DEFAULT || lifecycleStrategy > SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT) return;

        mSurfaceLifecycle = lifecycleStrategy;
        if (isAttachedToWindow() && !shouldKeepSurface()) destroySurface();
    }

    public void setDesiredHdrHeadroom(float desiredHeadroom)
    {
        if (!Float.isFinite(desiredHeadroom) || (desiredHeadroom != 0 && (desiredHeadroom < 1.0f || desiredHeadroom > 10000.0f)))
        {
            throw new IllegalArgumentException("desiredHeadroom must be 0 or within [1.0, 10000.0], received: " + desiredHeadroom);
        }

        mDesiredHdrHeadroom = desiredHeadroom;
    }

    public SurfaceControl getSurfaceControl()
    {
        return mSurfaceControl;
    }

    public IBinder getHostToken()
    {
        return null;
    }

    public void applyTransactionToFrame(SurfaceControl.Transaction transaction)
    {
        if (transaction == null) return;

        AttachedSurfaceControl root = getRootSurfaceControl();
        if (root == null || !root.applyTransactionOnDraw(transaction)) transaction.apply();
    }

    public void clearChildSurfacePackage()
    {
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return SurfaceView.class.getName();
    }

    @Override
    public int getImportantForAccessibility()
    {
        return super.getImportantForAccessibility();
    }

    @Override
    public boolean hasOverlappingRendering()
    {
        return false;
    }

    public boolean gatherTransparentRegion(Region region)
    {
        return true;
    }

    @Override
    protected boolean onSetAlpha(int alpha)
    {
        return false;
    }

    @Override
    public void setAlpha(float alpha)
    {
        super.setAlpha(alpha);
    }

    @Override
    public void setClipBounds(Rect clipBounds)
    {
        super.setClipBounds(clipBounds);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int width = mRequestedWidth >= 0 ? resolveSizeAndState(mRequestedWidth, widthMeasureSpec, 0) : getDefaultSize(0, widthMeasureSpec);
        int height = mRequestedHeight >= 0 ? resolveSizeAndState(mRequestedHeight, heightMeasureSpec, 0)
                                           : getDefaultSize(0, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
    {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        mLaidOut = false;
        requestLayout();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        destroySurface();

        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility)
    {
        super.onWindowVisibilityChanged(visibility);

        refreshSurfaceForVisibility();
    }

    @Override
    public void setVisibility(int visibility)
    {
        super.setVisibility(visibility);

        refreshSurfaceForVisibility();
    }

    private boolean shouldKeepSurface()
    {
        if (!isAttachedToWindow()) return false;
        if (mSurfaceLifecycle == SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT) return true;

        return getVisibility() == VISIBLE && getWindowVisibility() == VISIBLE;
    }

    private void refreshSurfaceForVisibility()
    {
        if (!isAttachedToWindow()) return;

        if (!shouldKeepSurface())
        {
            destroySurface();
            return;
        }

        if (!mCreated) requestLayout();
    }

    @Override
    public void draw(Canvas canvas)
    {
        drawSoftwareFrame(canvas);

        super.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas)
    {
        super.dispatchDraw(canvas);
    }

    private void drawSoftwareFrame(Canvas canvas)
    {
        Bitmap front;

        synchronized (mBufferLock)
        {
            front = mSoftwarePosted ? mFrontBuffer : null;
        }

        if (front == null || front.isRecycled()) return;

        canvas.drawBitmap(front, new Rect(0, 0, front.getWidth(), front.getHeight()), new Rect(0, 0, getWidth(), getHeight()),
                          mBufferPaint);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);
        updateSurface(right - left, bottom - top);
    }

    private void updateSurface(int width, int height)
    {
        if (!shouldKeepSurface()) return;

        int targetWidth = mRequestedWidth > 0 ? mRequestedWidth : width;
        int targetHeight = mRequestedHeight > 0 ? mRequestedHeight : height;

        if (targetWidth <= 0 || targetHeight <= 0) return;
        if (mCreated && targetWidth == mSurfaceWidth && targetHeight == mSurfaceHeight) return;

        if (!mLaidOut)
        {
            mLaidOut = true;
            requestLayout();
            return;
        }

        synchronized (mBufferLock)
        {
            mSurfaceWidth = targetWidth;
            mSurfaceHeight = targetHeight;
        }

        mSurface.attach(runtime.SurfaceBridge.acquireNativeWindow(targetWidth, targetHeight));

        if (!mCreated)
        {
            mSurfaceControl = new SurfaceControl.Builder().setName("SurfaceView[" + Integer.toHexString(System.identityHashCode(this)) + "]")
                                                          .setBufferSize(targetWidth, targetHeight)
                                                          .build();

            synchronized (mBufferLock)
            {
                mCreated = true;
            }

            mCreating = true;

            for (SurfaceHolder.Callback callback : new ArrayList<>(mCallbacks)) callback.surfaceCreated(mHolder);

            mCreating = false;
        }
        else if (mSurfaceControl != null)
        {
            mSurfaceControl.setBufferSize(targetWidth, targetHeight);
        }

        for (SurfaceHolder.Callback callback : new ArrayList<>(mCallbacks))
        {
            callback.surfaceChanged(mHolder, mFormat, mSurfaceWidth, mSurfaceHeight);
        }

        for (SurfaceHolder.Callback callback : new ArrayList<>(mCallbacks))
        {
            if (callback instanceof SurfaceHolder.Callback2) ((SurfaceHolder.Callback2)callback).surfaceRedrawNeeded(mHolder);
        }
    }

    void destroySurface()
    {
        if (!mCreated) return;

        for (SurfaceHolder.Callback callback : new ArrayList<>(mCallbacks)) callback.surfaceDestroyed(mHolder);

        synchronized (mBufferLock)
        {
            mCreated = false;
            mSoftwarePosted = false;
            mFrontBuffer = null;
            mBackBuffer = null;
        }

        mLaidOut = false;
        mSurface.release();

        if (mSurfaceControl != null)
        {
            mSurfaceControl.release();
            mSurfaceControl = null;
        }
    }

    private final class Holder implements SurfaceHolder
    {
        @Override
        public void addCallback(Callback callback)
        {
            if (callback == null || mCallbacks.contains(callback)) return;

            mCallbacks.add(callback);
            if (mCreated) callback.surfaceCreated(this);
            if (mCreated) callback.surfaceChanged(this, mFormat, mSurfaceWidth, mSurfaceHeight);
        }

        @Override
        public void removeCallback(Callback callback)
        {
            mCallbacks.remove(callback);
        }

        @Override
        public boolean isCreating()
        {
            return mCreating;
        }

        @Override
        public void setType(int type)
        {
        }

        @Override
        public void setFixedSize(int width, int height)
        {
            if (mRequestedWidth == width && mRequestedHeight == height) return;

            mRequestedWidth = width;
            mRequestedHeight = height;
            requestLayout();
        }

        @Override
        public void setSizeFromLayout()
        {
            if (mRequestedWidth == -1 && mRequestedHeight == -1) return;

            mRequestedWidth = -1;
            mRequestedHeight = -1;
            requestLayout();
        }

        @Override
        public void setFormat(int format)
        {
            if (format == android.graphics.PixelFormat.OPAQUE) format = android.graphics.PixelFormat.RGB_565;

            mFormat = format;
        }

        @Override
        public void setKeepScreenOn(boolean screenOn)
        {
            SurfaceView.this.setKeepScreenOn(screenOn);
        }

        @Override
        public Canvas lockCanvas()
        {
            return lockCanvas(null);
        }

        @Override
        public Canvas lockCanvas(Rect dirty)
        {
            if (!mSurface.isValid()) return null;

            try
            {
                return mSurface.lockCanvas(dirty);
            }
            catch (RuntimeException unavailable)
            {
                android.util.Log.w("SurfaceView", "the surface could not be locked", unavailable);
                return null;
            }
        }

        @Override
        public Canvas lockHardwareCanvas()
        {
            return lockCanvas(null);
        }

        @Override
        public void unlockCanvasAndPost(Canvas canvas)
        {
            mSurface.unlockCanvasAndPost(canvas);
        }

        @Override
        public Rect getSurfaceFrame()
        {
            return new Rect(0, 0, mSurfaceWidth, mSurfaceHeight);
        }

        @Override
        public Surface getSurface()
        {
            return mSurface;
        }
    }
}
