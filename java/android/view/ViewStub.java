package android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;

import java.lang.ref.WeakReference;

public final class ViewStub extends View
{
    public interface OnInflateListener
    {
        void onInflate(ViewStub stub, View inflated);
    }

    private static StyleableSet sStyleable;

    private int mInflatedId = NO_ID;
    private int mLayoutResource;
    private WeakReference<View> mInflatedViewRef;
    private LayoutInflater mInflater;
    private OnInflateListener mInflateListener;

    public ViewStub(Context context)
    {
        this(context, 0);
    }

    public ViewStub(Context context, int layoutResource)
    {
        this(context, null);

        mLayoutResource = layoutResource;
    }

    public ViewStub(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public ViewStub(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public ViewStub(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        setVisibility(GONE);
        setWillNotDraw(true);

        if (attributes == null) return;

        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "layout", "inflatedId");
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int layoutIndex = sStyleable.indexOf("layout");
        if (layoutIndex >= 0) mLayoutResource = array.getResourceId(layoutIndex, 0);

        int idIndex = sStyleable.indexOf("inflatedId");
        if (idIndex >= 0) mInflatedId = array.getResourceId(idIndex, NO_ID);

        array.recycle();
    }

    public int getInflatedId()
    {
        return mInflatedId;
    }

    public void setInflatedId(int inflatedId)
    {
        mInflatedId = inflatedId;
    }

    public int getLayoutResource()
    {
        return mLayoutResource;
    }

    public void setLayoutResource(int layoutResource)
    {
        mLayoutResource = layoutResource;
    }

    public void setLayoutInflater(LayoutInflater inflater)
    {
        mInflater = inflater;
    }

    public LayoutInflater getLayoutInflater()
    {
        return mInflater;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        setMeasuredDimension(0, 0);
    }

    @Override
    public void draw(Canvas canvas)
    {
    }

    @Override
    protected void dispatchDraw(Canvas canvas)
    {
    }

    @Override
    public void setVisibility(int visibility)
    {
        if (mInflatedViewRef != null)
        {
            View view = mInflatedViewRef.get();
            if (view == null) throw new IllegalStateException("setVisibility called on un-referenced view");

            view.setVisibility(visibility);
            return;
        }

        super.setVisibility(visibility);

        if (visibility == VISIBLE || visibility == INVISIBLE) inflate();
    }

    private View inflateViewNoAdd(ViewGroup parent)
    {
        LayoutInflater inflater = mInflater != null ? mInflater : LayoutInflater.from(getContext());
        View view = inflater.inflate(mLayoutResource, parent, false);

        if (mInflatedId != NO_ID) view.setId(mInflatedId);

        return view;
    }

    private void replaceSelfWithView(View view, ViewGroup parent)
    {
        int index = parent.indexOfChild(this);
        parent.removeViewInLayout(this);

        ViewGroup.LayoutParams params = getLayoutParams();
        if (params != null) parent.addView(view, index, params);
        else parent.addView(view, index);
    }

    public View inflate()
    {
        ViewParent viewParent = getParent();

        if (!(viewParent instanceof ViewGroup)) throw new IllegalStateException("ViewStub must have a non-null ViewGroup viewParent");
        if (mLayoutResource == 0) throw new IllegalArgumentException("ViewStub must have a valid layoutResource");

        ViewGroup parent = (ViewGroup)viewParent;
        View view = inflateViewNoAdd(parent);

        replaceSelfWithView(view, parent);

        mInflatedViewRef = new WeakReference<>(view);

        if (mInflateListener != null) mInflateListener.onInflate(this, view);

        return view;
    }

    public void setOnInflateListener(OnInflateListener inflateListener)
    {
        mInflateListener = inflateListener;
    }
}
