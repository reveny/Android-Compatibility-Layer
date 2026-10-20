package android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;

import java.util.ArrayList;

public class SurfaceView extends View
{
    private final Surface mSurface = new Surface();
    private final Holder mHolder = new Holder();
    private final ArrayList<SurfaceHolder.Callback> mCallbacks = new ArrayList<>();

    private boolean mCreated;
    private boolean mCreating;
    private boolean mLaidOut;
    private int mFormat = 4;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mRequestedWidth;
    private int mRequestedHeight;

    public SurfaceView(Context context)
    {
        super(context);
    }

    public SurfaceView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public SurfaceView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        super(context, attributes, defStyleAttr);
    }

    public SurfaceView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    public SurfaceHolder getHolder()
    {
        return mHolder;
    }

    public void setZOrderOnTop(boolean onTop)
    {
    }

    public void setZOrderMediaOverlay(boolean isMediaOverlay)
    {
    }

    public void setSecure(boolean secure)
    {
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);
        updateSurface(right - left, bottom - top);
    }

    private void updateSurface(int width, int height)
    {
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

        mSurfaceWidth = targetWidth;
        mSurfaceHeight = targetHeight;
        mSurface.attach(runtime.SurfaceBridge.acquireNativeWindow(targetWidth, targetHeight));

        if (!mCreated)
        {
            mCreated = true;
            mCreating = true;

            for (SurfaceHolder.Callback callback : new ArrayList<>(mCallbacks)) callback.surfaceCreated(mHolder);

            mCreating = false;
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

        mCreated = false;

        for (SurfaceHolder.Callback callback : new ArrayList<>(mCallbacks)) callback.surfaceDestroyed(mHolder);

        mSurface.release();
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
            mRequestedWidth = width;
            mRequestedHeight = height;
            updateSurface(width, height);
        }

        @Override
        public void setSizeFromLayout()
        {
            mRequestedWidth = 0;
            mRequestedHeight = 0;
        }

        @Override
        public void setFormat(int format)
        {
            mFormat = format;
        }

        @Override
        public void setKeepScreenOn(boolean screenOn)
        {
        }

        @Override
        public Canvas lockCanvas()
        {
            return mSurface.lockCanvas(null);
        }

        @Override
        public Canvas lockCanvas(Rect dirty)
        {
            return mSurface.lockCanvas(dirty);
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
