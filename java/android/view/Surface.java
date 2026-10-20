package android.view;

import android.graphics.Canvas;
import android.graphics.Rect;

public class Surface
{
    public static final int ROTATION_0 = 0;
    public static final int ROTATION_90 = 1;
    public static final int ROTATION_180 = 2;
    public static final int ROTATION_270 = 3;

    private long mNativeWindow;
    private boolean mValid;

    public Surface()
    {
        mNativeWindow = 0;
        mValid = false;
    }

    Surface(long nativeWindow)
    {
        mNativeWindow = nativeWindow;
        mValid = nativeWindow != 0;
    }

    public boolean isValid()
    {
        return mValid && mNativeWindow != 0;
    }

    public void release()
    {
        mNativeWindow = 0;
        mValid = false;
    }

    public Canvas lockCanvas(Rect dirty)
    {
        throw new IllegalStateException("Surface.lockCanvas is not available on this surface");
    }

    public void unlockCanvasAndPost(Canvas canvas)
    {
        throw new IllegalStateException("Surface.unlockCanvasAndPost is not available on this surface");
    }

    long getNativeWindow()
    {
        return mNativeWindow;
    }

    void attach(long nativeWindow)
    {
        mNativeWindow = nativeWindow;
        mValid = nativeWindow != 0;
    }

    @Override
    public String toString()
    {
        return "Surface(native=0x" + Long.toHexString(mNativeWindow) + ")";
    }
}
