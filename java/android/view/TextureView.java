package android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;

public class TextureView extends View
{
    private SurfaceTexture mSurface;
    private boolean mSurfaceProvidedByApplication;
    private SurfaceTextureListener mListener;
    private boolean mOpaque = true;
    private final Matrix mMatrix = new Matrix();
    private boolean mMatrixChanged;
    private Bitmap mBuffer;
    private Canvas mLockedCanvas;
    private Paint mLayerPaint;
    private final Paint mBitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private static final java.util.WeakHashMap<SurfaceTexture, java.lang.ref.WeakReference<TextureView>> sConsumers =
        new java.util.WeakHashMap<>();

    private final Surface.BufferProducer mProducer = new Surface.BufferProducer()
    {
        @Override
        Bitmap lockBuffer()
        {
            return mBuffer;
        }

        @Override
        void unlockAndPost(Bitmap buffer)
        {
            postInvalidate();
            post(mUpdateNotifier);
        }

        @Override
        Bitmap peekFrontBuffer()
        {
            return mBuffer;
        }
    };

    private final Runnable mUpdateNotifier = new Runnable()
    {
        @Override
        public void run()
        {
            if (mListener != null && mSurface != null) mListener.onSurfaceTextureUpdated(mSurface);
        }
    };

    static Surface.BufferProducer producerFor(SurfaceTexture texture)
    {
        TextureView view;

        synchronized (sConsumers)
        {
            java.lang.ref.WeakReference<TextureView> reference = sConsumers.get(texture);
            view = reference == null ? null : reference.get();
        }

        return view == null ? null : view.mProducer;
    }

    private void registerConsumer(SurfaceTexture texture)
    {
        synchronized (sConsumers)
        {
            sConsumers.put(texture, new java.lang.ref.WeakReference<>(this));
        }
    }

    private void unregisterConsumer(SurfaceTexture texture)
    {
        synchronized (sConsumers)
        {
            java.lang.ref.WeakReference<TextureView> reference = sConsumers.get(texture);
            if (reference != null && reference.get() == this) sConsumers.remove(texture);
        }
    }

    public TextureView(Context context)
    {
        super(context);
    }

    public TextureView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public TextureView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
    }

    public TextureView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return TextureView.class.getName();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility)
    {
        super.onVisibilityChanged(changedView, visibility);

        if (mSurface != null && visibility == VISIBLE) invalidate();
    }

    @Override
    public boolean isOpaque()
    {
        return mOpaque;
    }

    public void setOpaque(boolean opaque)
    {
        if (mOpaque == opaque) return;

        mOpaque = opaque;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        ensureSurface();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        destroySurface();
    }

    private void ensureSurface()
    {
        if (mSurface != null || getWidth() <= 0 || getHeight() <= 0) return;

        mSurface = new SurfaceTexture(false);
        mSurface.setDefaultBufferSize(getWidth(), getHeight());
        mSurfaceProvidedByApplication = false;
        allocateBuffer(getWidth(), getHeight());
        registerConsumer(mSurface);

        if (mListener != null) mListener.onSurfaceTextureAvailable(mSurface, getWidth(), getHeight());
    }

    private void destroySurface()
    {
        if (mSurface == null) return;

        unregisterConsumer(mSurface);

        boolean shouldRelease = true;
        if (mListener != null) shouldRelease = mListener.onSurfaceTextureDestroyed(mSurface);

        if (shouldRelease && !mSurfaceProvidedByApplication) mSurface.release();

        mSurface = null;
        mBuffer = null;
    }

    private void allocateBuffer(int width, int height)
    {
        if (width <= 0 || height <= 0)
        {
            mBuffer = null;
            return;
        }

        if (mBuffer != null && mBuffer.getWidth() == width && mBuffer.getHeight() == height) return;

        mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    @Override
    public void setLayerType(int layerType, Paint paint)
    {
        setLayerPaint(paint);
    }

    @Override
    public void setLayerPaint(Paint paint)
    {
        if (paint == mLayerPaint) return;

        mLayerPaint = paint;
        invalidate();
    }

    @Override
    public int getLayerType()
    {
        return LAYER_TYPE_HARDWARE;
    }

    @Override
    public void buildLayer()
    {
    }

    @Override
    public void setForeground(android.graphics.drawable.Drawable foreground)
    {
    }

    @Override
    public void setBackgroundDrawable(android.graphics.drawable.Drawable background)
    {
    }

    @Override
    public final void draw(Canvas canvas)
    {
        if (getVisibility() != VISIBLE) return;

        if (mBuffer == null) return;

        int saveCount = canvas.save();
        if (mMatrixChanged) canvas.concat(mMatrix);
        canvas.drawBitmap(mBuffer, 0, 0, mLayerPaint != null ? mLayerPaint : mBitmapPaint);
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected final void onDraw(Canvas canvas)
    {
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        if (mSurface == null)
        {
            if (isAttachedToWindow()) ensureSurface();
            return;
        }

        mSurface.setDefaultBufferSize(width, height);
        allocateBuffer(width, height);

        if (mListener != null) mListener.onSurfaceTextureSizeChanged(mSurface, width, height);
    }

    public void setTransform(Matrix transform)
    {
        mMatrix.set(transform);
        mMatrixChanged = !mMatrix.isIdentity();
        invalidate();
    }

    public Matrix getTransform(Matrix transform)
    {
        if (transform == null) transform = new Matrix();

        transform.set(mMatrix);
        return transform;
    }

    public Bitmap getBitmap()
    {
        return getBitmap(getWidth(), getHeight());
    }

    public Bitmap getBitmap(int width, int height)
    {
        if (!isAvailable() || width <= 0 || height <= 0) return null;

        return getBitmap(Bitmap.createBitmap(getResources().getDisplayMetrics(), width, height, Bitmap.Config.ARGB_8888));
    }

    public Bitmap getBitmap(Bitmap bitmap)
    {
        if (bitmap == null || !isAvailable()) return bitmap;

        Canvas canvas = new Canvas(bitmap);
        if (mBuffer != null)
        {
            canvas.drawBitmap(mBuffer, new Rect(0, 0, mBuffer.getWidth(), mBuffer.getHeight()),
                              new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), mBitmapPaint);
        }

        return bitmap;
    }

    public boolean isAvailable()
    {
        return mSurface != null;
    }

    public Canvas lockCanvas()
    {
        return lockCanvas(null);
    }

    public Canvas lockCanvas(Rect dirty)
    {
        if (!isAvailable() || mBuffer == null || mLockedCanvas != null) return null;

        mLockedCanvas = new Canvas(mBuffer);
        if (dirty != null) mLockedCanvas.clipRect(dirty);
        return mLockedCanvas;
    }

    public void unlockCanvasAndPost(Canvas canvas)
    {
        if (canvas == null || canvas != mLockedCanvas) return;

        mLockedCanvas = null;
        postInvalidate();

        if (mListener != null && mSurface != null) mListener.onSurfaceTextureUpdated(mSurface);
    }

    public SurfaceTexture getSurfaceTexture()
    {
        return mSurface;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture)
    {
        if (surfaceTexture == null) throw new NullPointerException("surfaceTexture must not be null");
        if (surfaceTexture == mSurface)
        {
            throw new IllegalArgumentException("Trying to setSurfaceTexture to the same SurfaceTexture that's already set.");
        }

        if (surfaceTexture.isReleased()) throw new IllegalArgumentException("Cannot setSurfaceTexture to a released SurfaceTexture");

        if (mSurface != null)
        {
            unregisterConsumer(mSurface);
            if (!mSurfaceProvidedByApplication) mSurface.release();
        }

        mSurface = surfaceTexture;
        mSurfaceProvidedByApplication = true;
        allocateBuffer(getWidth(), getHeight());
        registerConsumer(surfaceTexture);
        invalidate();
    }

    public SurfaceTextureListener getSurfaceTextureListener()
    {
        return mListener;
    }

    public void setSurfaceTextureListener(SurfaceTextureListener listener)
    {
        mListener = listener;
    }

    public interface SurfaceTextureListener
    {
        void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height);

        void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height);

        boolean onSurfaceTextureDestroyed(SurfaceTexture surface);

        void onSurfaceTextureUpdated(SurfaceTexture surface);
    }
}
