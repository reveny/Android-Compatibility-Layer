package android.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class PixelCopy
{
    public static final int SUCCESS = 0;
    public static final int ERROR_UNKNOWN = 1;
    public static final int ERROR_TIMEOUT = 2;
    public static final int ERROR_SOURCE_NO_DATA = 3;
    public static final int ERROR_SOURCE_INVALID = 4;
    public static final int ERROR_DESTINATION_INVALID = 5;

    private PixelCopy()
    {
    }

    public interface OnPixelCopyFinishedListener
    {
        void onPixelCopyFinished(int copyResult);
    }

    public static void request(SurfaceView source, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread)
    {
        request(source, null, dest, listener, listenerThread);
    }

    public static void request(SurfaceView source, Rect srcRect, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread)
    {
        request(source.getHolder().getSurface(), srcRect, dest, listener, listenerThread);
    }

    public static void request(Surface source, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread)
    {
        request(source, null, dest, listener, listenerThread);
    }

    public static void request(Surface source, Rect srcRect, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread)
    {
        validateBitmapDest(dest);
        if (source == null || !source.isValid()) throw new IllegalArgumentException("Surface isn't valid, source.isValid() == false");
        if (srcRect != null && srcRect.isEmpty()) throw new IllegalArgumentException("sourceRect is empty");

        int result = copySurface(source, srcRect, dest);
        deliver(result, listener, listenerThread);
    }

    public static void request(Window source, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread)
    {
        request(source, null, dest, listener, listenerThread);
    }

    public static void request(Window source, Rect srcRect, Bitmap dest, OnPixelCopyFinishedListener listener, Handler listenerThread)
    {
        validateBitmapDest(dest);
        if (source == null) throw new IllegalArgumentException("source is null");
        if (source.peekDecorView() == null) throw new IllegalArgumentException("Only able to copy windows with decor views");
        if (srcRect != null && srcRect.isEmpty()) throw new IllegalArgumentException("sourceRect is empty");

        int result = copyView(source.peekDecorView(), srcRect, dest);
        deliver(result, listener, listenerThread);
    }

    public static void request(Request request, Executor callbackExecutor, Consumer<Result> listener)
    {
        if (request == null || callbackExecutor == null || listener == null) throw new NullPointerException();

        Bitmap destination = request.mDestination;
        int status;

        if (request.mSurface != null && !request.mSurface.isValid())
        {
            status = ERROR_SOURCE_INVALID;
        }
        else
        {
            if (destination == null)
            {
                Rect bounds = request.sourceBounds();
                if (bounds == null || bounds.isEmpty())
                {
                    callbackExecutor.execute(() -> listener.accept(new Result(ERROR_SOURCE_INVALID, null)));
                    return;
                }

                destination = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
            }

            if (request.mSurface != null) status = copySurface(request.mSurface, request.mSourceRect, destination);
            else status = copyView(request.mView, request.mSourceRect, destination);
        }

        Result result = new Result(status, status == SUCCESS ? destination : null);
        callbackExecutor.execute(() -> listener.accept(result));
    }

    private static void validateBitmapDest(Bitmap bitmap)
    {
        if (bitmap == null) throw new IllegalArgumentException("Bitmap cannot be null");
        if (bitmap.isRecycled()) throw new IllegalArgumentException("Bitmap is recycled");
        if (!bitmap.isMutable()) throw new IllegalArgumentException("Bitmap is immutable");
    }

    private static void deliver(int result, OnPixelCopyFinishedListener listener, Handler handler)
    {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");

        Handler target = handler == null ? new Handler(android.os.Looper.getMainLooper()) : handler;
        if (!target.post(() -> listener.onPixelCopyFinished(result))) listener.onPixelCopyFinished(result);
    }

    private static int copySurface(Surface source, Rect sourceRect, Bitmap dest)
    {
        Bitmap front = source.peekFrontBuffer();
        if (front == null || front.isRecycled()) return ERROR_SOURCE_NO_DATA;

        Rect from = sourceRect == null ? new Rect(0, 0, front.getWidth(), front.getHeight()) : new Rect(sourceRect);
        if (!from.intersect(0, 0, front.getWidth(), front.getHeight())) return ERROR_SOURCE_INVALID;

        try
        {
            new Canvas(dest).drawBitmap(front, from, new Rect(0, 0, dest.getWidth(), dest.getHeight()), new Paint(Paint.FILTER_BITMAP_FLAG));
            return SUCCESS;
        }
        catch (RuntimeException failure)
        {
            return ERROR_DESTINATION_INVALID;
        }
    }

    private static int copyView(View view, Rect sourceRect, Bitmap dest)
    {
        if (view == null) return ERROR_SOURCE_INVALID;
        if (view.getWidth() <= 0 || view.getHeight() <= 0) return ERROR_SOURCE_NO_DATA;

        Rect from = sourceRect == null ? new Rect(0, 0, view.getWidth(), view.getHeight()) : new Rect(sourceRect);
        if (!from.intersect(0, 0, view.getWidth(), view.getHeight())) return ERROR_SOURCE_INVALID;

        try
        {
            Canvas canvas = new Canvas(dest);
            canvas.scale(dest.getWidth() / (float)from.width(), dest.getHeight() / (float)from.height());
            canvas.translate(-from.left, -from.top);
            view.draw(canvas);
            return SUCCESS;
        }
        catch (RuntimeException failure)
        {
            android.util.Log.w("PixelCopy", "the window could not be copied", failure);
            return ERROR_UNKNOWN;
        }
    }

    public static final class Result
    {
        private final int mStatus;
        private final Bitmap mBitmap;

        Result(int status, Bitmap bitmap)
        {
            mStatus = status;
            mBitmap = bitmap;
        }

        public int getStatus()
        {
            return mStatus;
        }

        public Bitmap getBitmap()
        {
            if (mStatus != SUCCESS) throw new IllegalStateException("getBitmap() is not valid for a failed copy, status " + mStatus);

            return mBitmap;
        }
    }

    public static final class Request
    {
        private final Surface mSurface;
        private final View mView;
        private final Rect mSourceRect;
        private final Bitmap mDestination;

        Request(Surface surface, View view, Rect sourceRect, Bitmap destination)
        {
            mSurface = surface;
            mView = view;
            mSourceRect = sourceRect;
            mDestination = destination;
        }

        Rect sourceBounds()
        {
            if (mSourceRect != null) return new Rect(mSourceRect);
            if (mView != null) return new Rect(0, 0, mView.getWidth(), mView.getHeight());

            Bitmap front = mSurface == null ? null : mSurface.peekFrontBuffer();
            return front == null ? null : new Rect(0, 0, front.getWidth(), front.getHeight());
        }

        public Rect getSourceRect()
        {
            return mSourceRect;
        }

        public Bitmap getDestinationBitmap()
        {
            return mDestination;
        }

        public static final class Builder
        {
            private final Surface mSurface;
            private final View mView;
            private Rect mSourceRect;
            private Bitmap mDestination;

            private Builder(Surface surface, View view)
            {
                mSurface = surface;
                mView = view;
            }

            public static Builder ofSurface(Surface source)
            {
                if (source == null || !source.isValid()) throw new IllegalArgumentException("Source must not be null & must be valid");

                return new Builder(source, null);
            }

            public static Builder ofSurface(SurfaceView source)
            {
                return ofSurface(source.getHolder().getSurface());
            }

            public static Builder ofWindow(Window source)
            {
                if (source == null || source.peekDecorView() == null)
                {
                    throw new IllegalArgumentException("Only able to copy windows with decor views");
                }

                return new Builder(null, source.peekDecorView());
            }

            public static Builder ofWindow(View source)
            {
                if (source == null || !source.isAttachedToWindow())
                {
                    throw new IllegalArgumentException("View must not be null & must be attached to window");
                }

                return new Builder(null, source.getRootView());
            }

            public Builder setSourceRect(Rect srcRect)
            {
                mSourceRect = srcRect == null ? null : new Rect(srcRect);
                return this;
            }

            public Builder setDestinationBitmap(Bitmap destination)
            {
                if (destination != null) validateBitmapDest(destination);

                mDestination = destination;
                return this;
            }

            public Request build()
            {
                return new Request(mSurface, mView, mSourceRect, mDestination);
            }
        }
    }
}
