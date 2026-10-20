package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;

public final class ViewStub extends View
{
    public interface OnInflateListener
    {
        void onInflate(ViewStub stub, View inflated);
    }

    private static StyleableSet sStyleable;

    private int mLayoutResource;
    private int mInflatedId = NO_ID;
    private View mInflatedView;
    private OnInflateListener mListener;

    public ViewStub(Context context)
    {
        this(context, null);
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

        android.content.res.TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), 0, 0);

        int layoutIndex = sStyleable.indexOf("layout");
        if (layoutIndex >= 0) mLayoutResource = array.getResourceId(layoutIndex, 0);

        int idIndex = sStyleable.indexOf("inflatedId");
        if (idIndex >= 0) mInflatedId = array.getResourceId(idIndex, NO_ID);

        array.recycle();
    }

    public int getLayoutResource()
    {
        return mLayoutResource;
    }

    public void setLayoutResource(int layoutResource)
    {
        mLayoutResource = layoutResource;
    }

    public int getInflatedId()
    {
        return mInflatedId;
    }

    public void setInflatedId(int inflatedId)
    {
        mInflatedId = inflatedId;
    }

    public void setOnInflateListener(OnInflateListener listener)
    {
        mListener = listener;
    }

    @Override
    public void setVisibility(int visibility)
    {
        if (mInflatedView != null)
        {
            mInflatedView.setVisibility(visibility);
            return;
        }

        super.setVisibility(visibility);

        if (visibility == VISIBLE || visibility == INVISIBLE) inflate();
    }

    public View inflate()
    {
        if (mInflatedView != null) return mInflatedView;

        ViewGroup parent = (ViewGroup)getParent();
        if (parent == null || mLayoutResource == 0) return null;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View inflated = inflater.inflate(mLayoutResource, parent, false);

        if (mInflatedId != NO_ID) inflated.setId(mInflatedId);

        int index = parent.indexOfChild(this);
        parent.removeViewAt(index);

        ViewGroup.LayoutParams params = getLayoutParams();
        if (params != null) parent.addView(inflated, index, params);
        else parent.addView(inflated, index);

        mInflatedView = inflated;

        if (mListener != null) mListener.onInflate(this, inflated);

        return inflated;
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
    protected void onDraw(Canvas canvas)
    {
    }
}
