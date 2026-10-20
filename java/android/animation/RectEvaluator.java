package android.animation;

import android.graphics.Rect;

public class RectEvaluator implements TypeEvaluator<Rect>
{
    private Rect mReuse;

    public RectEvaluator()
    {
    }

    public RectEvaluator(Rect reuse)
    {
        mReuse = reuse;
    }

    @Override
    public Rect evaluate(float fraction, Rect startValue, Rect endValue)
    {
        if (startValue == null || endValue == null) return endValue;

        int left = startValue.left + (int)((endValue.left - startValue.left) * fraction);
        int top = startValue.top + (int)((endValue.top - startValue.top) * fraction);
        int right = startValue.right + (int)((endValue.right - startValue.right) * fraction);
        int bottom = startValue.bottom + (int)((endValue.bottom - startValue.bottom) * fraction);

        if (mReuse == null) return new Rect(left, top, right, bottom);

        mReuse.set(left, top, right, bottom);
        return mReuse;
    }
}
