package android.view;

import android.content.Context;

public abstract class Window
{
    public static final int ID_ANDROID_CONTENT = 0x01020002;

    private final Context mContext;
    private CharSequence mTitle = "";
    private Callback mCallback;
    private boolean mDecorFitsSystemWindows = true;
    private int mStatusBarColor;
    private int mNavigationBarColor;
    private WindowManager.LayoutParams mAttributes = new WindowManager.LayoutParams();

    public Window(Context context)
    {
        mContext = context;
    }

    public final Context getContext()
    {
        return mContext;
    }

    public final CharSequence getTitle()
    {
        return mTitle;
    }

    public void setTitle(CharSequence title)
    {
        mTitle = title == null ? "" : title;
    }

    public final Callback getCallback()
    {
        return mCallback;
    }

    public void setCallback(Callback callback)
    {
        mCallback = callback;
    }

    public abstract void setContentView(View view);

    public abstract void setContentView(View view, ViewGroup.LayoutParams layoutParams);

    public abstract View getDecorView();

    public boolean superDispatchTouchEvent(MotionEvent event)
    {
        View decor = peekDecorView();

        if (View.TRACE_INPUT && event.getActionMasked() == MotionEvent.ACTION_DOWN)
        {
            describeTree(decor, 0);
        }

        return decor != null && decor.dispatchTouchEvent(event);
    }

    private static void describeTree(View view, int depth)
    {
        if (view == null)
        {
            android.util.Log.i("Window", "there is no decor view to dispatch into");
            return;
        }

        StringBuilder indent = new StringBuilder();
        for (int step = 0; step < depth; step++) indent.append("  ");

        android.util.Log.i("Window", indent + view.getClass().getName() + " at " + view.getLeft() + "," + view.getTop() + " size "
                                         + view.getWidth() + "x" + view.getHeight() + " visibility " + view.getVisibility());

        if (!(view instanceof ViewGroup)) return;

        ViewGroup group = (ViewGroup)view;
        for (int index = 0; index < group.getChildCount(); index++) describeTree(group.getChildAt(index), depth + 1);
    }

    public boolean superDispatchKeyEvent(KeyEvent event)
    {
        View decor = peekDecorView();
        return decor != null && decor.dispatchKeyEvent(event);
    }

    public abstract View peekDecorView();

    public abstract View findViewById(int id);

    public void setDecorFitsSystemWindows(boolean decorFitsSystemWindows)
    {
        mDecorFitsSystemWindows = decorFitsSystemWindows;
    }

    public boolean getDecorFitsSystemWindows()
    {
        return mDecorFitsSystemWindows;
    }

    public WindowInsetsController getInsetsController()
    {
        return getDecorView().getWindowInsetsController();
    }

    public void setFormat(int format)
    {
        mFormat = format;
    }

    public int getFormat()
    {
        return mFormat;
    }

    public void takeSurface(SurfaceHolder.Callback2 callback)
    {
        mSurfaceCallback = callback;
    }

    private int mFormat = 0;
    private SurfaceHolder.Callback2 mSurfaceCallback;

    public void setStatusBarColor(int color)
    {
        mStatusBarColor = color;
    }

    public int getStatusBarColor()
    {
        return mStatusBarColor;
    }

    public void setNavigationBarColor(int color)
    {
        mNavigationBarColor = color;
    }

    public int getNavigationBarColor()
    {
        return mNavigationBarColor;
    }

    public void setStatusBarContrastEnforced(boolean enforced)
    {
    }

    public boolean isStatusBarContrastEnforced()
    {
        return false;
    }

    public void setNavigationBarContrastEnforced(boolean enforced)
    {
    }

    public boolean isNavigationBarContrastEnforced()
    {
        return false;
    }

    public WindowManager.LayoutParams getAttributes()
    {
        return mAttributes;
    }

    public void setAttributes(WindowManager.LayoutParams attributes)
    {
        if (attributes != null) mAttributes = attributes;
    }

    public void addFlags(int flags)
    {
        mAttributes.flags |= flags;
    }

    public void clearFlags(int flags)
    {
        mAttributes.flags &= ~flags;
    }

    public void setFlags(int flags, int mask)
    {
        mAttributes.flags = (mAttributes.flags & ~mask) | (flags & mask);
    }

    public boolean hasFeature(int feature)
    {
        return false;
    }

    public boolean requestFeature(int feature)
    {
        return false;
    }

    public void setBackgroundDrawable(android.graphics.drawable.Drawable background)
    {
    }

    public void setWindowAnimations(int resourceId)
    {
    }

    public void setLayout(int width, int height)
    {
    }

    public void setSoftInputMode(int mode)
    {
    }

    public View requireViewById(int id)
    {
        View found = findViewById(id);
        if (found == null) throw new IllegalArgumentException("No view with id 0x" + Integer.toHexString(id));
        return found;
    }

    public interface Callback
    {
        void onContentChanged();

        void onWindowFocusChanged(boolean hasFocus);

        void onAttachedToWindow();

        void onDetachedFromWindow();
    }
}
