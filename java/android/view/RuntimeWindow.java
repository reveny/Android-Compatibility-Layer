package android.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;

public class RuntimeWindow extends Window
{
    private static final String VIEWS_TAG = "android:views";
    private static final int USE_DEFAULT_STREAM_TYPE = Integer.MIN_VALUE;

    private final DecorView mDecor;
    private final boolean mFloating;
    private SurfaceHolder.Callback2 mSurfaceCallback;
    private InputQueue.Callback mInputQueueCallback;
    private boolean mTakeKeyEvents;
    private int mTitleColor;
    private int mVolumeControlStream = USE_DEFAULT_STREAM_TYPE;
    private int mStatusBarColor;
    private int mNavigationBarColor;
    private int mDecorCaptionShade = DECOR_CAPTION_SHADE_AUTO;

    public RuntimeWindow(Context context)
    {
        super(context);
        mDecor = new DecorView(context);
        mDecor.attachWindow(this);
        mFloating = themeBoolean("windowIsFloating", false);
        applyWindowBackground(context);
    }

    private void applyWindowBackground(Context context)
    {
        int attribute = context.getResources().getIdentifier("windowBackground", "attr", "android");
        if (attribute == 0) return;

        android.util.TypedValue value = new android.util.TypedValue();
        if (!context.getTheme().resolveAttribute(attribute, value, true)) return;

        if (value.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT && value.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT)
        {
            mDecor.setBackgroundColor(value.data);
            return;
        }

        if (value.resourceId != 0)
        {
            android.graphics.drawable.Drawable background = context.getResources().getDrawable(value.resourceId);
            if (background instanceof android.graphics.drawable.ColorDrawable)
            {
                mDecor.setBackgroundColor(((android.graphics.drawable.ColorDrawable)background).getColor());
            }
        }
    }

    private void notifyContentChanged()
    {
        Callback callback = getCallback();
        if (callback != null) callback.onContentChanged();
    }

    @Override
    public void setContentView(int layoutResID)
    {
        ViewGroup content = mDecor.getContentParent();
        content.removeAllViews();
        getLayoutInflater().inflate(layoutResID, content);

        notifyContentChanged();
    }

    @Override
    public void setContentView(View view)
    {
        setContentView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams layoutParams)
    {
        ViewGroup content = mDecor.getContentParent();
        content.removeAllViews();
        content.addView(view, layoutParams);

        notifyContentChanged();
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params)
    {
        mDecor.getContentParent().addView(view, params);

        notifyContentChanged();
    }

    @Override
    public View getDecorView()
    {
        return mDecor;
    }

    @Override
    public View peekDecorView()
    {
        return mDecor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends View> T findViewById(int id)
    {
        return (T)mDecor.findViewById(id);
    }

    @Override
    public View getCurrentFocus()
    {
        return mDecor.findFocus();
    }

    @Override
    public LayoutInflater getLayoutInflater()
    {
        return LayoutInflater.from(getContext());
    }

    @Override
    public void setTitle(CharSequence title)
    {
        storeTitle(title);
        getAttributes().setTitle(title);
    }

    @Override
    public void setTitleColor(int textColor)
    {
        mTitleColor = textColor;
    }

    @Override
    public boolean isFloating()
    {
        return mFloating;
    }

    @Override
    public void takeSurface(SurfaceHolder.Callback2 callback)
    {
        mSurfaceCallback = callback;
    }

    @Override
    public void takeInputQueue(InputQueue.Callback callback)
    {
        mInputQueueCallback = callback;
    }

    @Override
    public void takeKeyEvents(boolean get)
    {
        mTakeKeyEvents = get;
    }

    @Override
    public boolean superDispatchTouchEvent(MotionEvent event)
    {
        if (View.TRACE_INPUT && event.getActionMasked() == MotionEvent.ACTION_DOWN) describeTree(mDecor, 0);

        return mDecor.dispatchTouchEvent(event);
    }

    private static void describeTree(View view, int depth)
    {
        StringBuilder indent = new StringBuilder();
        for (int step = 0; step < depth; step++) indent.append("  ");

        android.util.Log.i("Window", indent + view.getClass().getName() + " at " + view.getLeft() + "," + view.getTop() + " size "
                                         + view.getWidth() + "x" + view.getHeight() + " visibility " + view.getVisibility());

        if (!(view instanceof ViewGroup)) return;

        ViewGroup group = (ViewGroup)view;
        for (int index = 0; index < group.getChildCount(); index++) describeTree(group.getChildAt(index), depth + 1);
    }

    @Override
    public boolean superDispatchKeyEvent(KeyEvent event)
    {
        return mDecor.dispatchKeyEvent(event);
    }

    @Override
    public boolean superDispatchKeyShortcutEvent(KeyEvent event)
    {
        return mDecor.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean superDispatchGenericMotionEvent(MotionEvent event)
    {
        return mDecor.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean superDispatchTrackballEvent(MotionEvent event)
    {
        return mDecor.dispatchTrackballEvent(event);
    }

    @Override
    public void openPanel(int featureId, KeyEvent event)
    {
    }

    @Override
    public void closePanel(int featureId)
    {
    }

    @Override
    public void togglePanel(int featureId, KeyEvent event)
    {
    }

    @Override
    public void invalidatePanelMenu(int featureId)
    {
    }

    @Override
    public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags)
    {
        return false;
    }

    @Override
    public boolean performPanelIdentifierAction(int featureId, int id, int flags)
    {
        return false;
    }

    @Override
    public void closeAllPanels()
    {
    }

    @Override
    public boolean performContextMenuIdentifierAction(int id, int flags)
    {
        return false;
    }

    @Override
    public boolean isShortcutKey(int keyCode, KeyEvent event)
    {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        mDecor.requestLayout();
        mDecor.invalidate();
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable)
    {
        mDecor.setBackground(drawable);
    }

    @Override
    public void setFeatureDrawableResource(int featureId, int resId)
    {
    }

    @Override
    public void setFeatureDrawableUri(int featureId, Uri uri)
    {
    }

    @Override
    public void setFeatureDrawable(int featureId, Drawable drawable)
    {
    }

    @Override
    public void setFeatureDrawableAlpha(int featureId, int alpha)
    {
    }

    @Override
    public void setFeatureInt(int featureId, int value)
    {
    }

    @Override
    public void setChildDrawable(int featureId, Drawable drawable)
    {
    }

    @Override
    public void setChildInt(int featureId, int value)
    {
    }

    @Override
    protected void onActive()
    {
    }

    @Override
    public Bundle saveHierarchyState()
    {
        Bundle outState = new Bundle();
        SparseArray<Parcelable> states = new SparseArray<>();
        mDecor.saveHierarchyState(states);
        outState.putSparseParcelableArray(VIEWS_TAG, states);
        return outState;
    }

    @Override
    public void restoreHierarchyState(Bundle savedInstanceState)
    {
        if (savedInstanceState == null) return;

        SparseArray<Parcelable> states = savedInstanceState.getSparseParcelableArray(VIEWS_TAG);
        if (states != null) mDecor.restoreHierarchyState(states);
    }

    @Override
    public void setVolumeControlStream(int streamType)
    {
        mVolumeControlStream = streamType;
    }

    @Override
    public int getVolumeControlStream()
    {
        return mVolumeControlStream;
    }

    @Override
    public int getStatusBarColor()
    {
        return mStatusBarColor;
    }

    @Override
    public void setStatusBarColor(int color)
    {
        mStatusBarColor = color;
    }

    @Override
    public int getNavigationBarColor()
    {
        return mNavigationBarColor;
    }

    @Override
    public void setNavigationBarColor(int color)
    {
        mNavigationBarColor = color;
    }

    @Override
    public void setDecorCaptionShade(int decorCaptionShade)
    {
        mDecorCaptionShade = decorCaptionShade;
    }

    @Override
    public void setResizingCaptionDrawable(Drawable drawable)
    {
    }
}
