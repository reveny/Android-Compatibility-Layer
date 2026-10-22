package android.animation;

import android.graphics.PointF;

public class PointFEvaluator implements TypeEvaluator<PointF>
{
    private PointF mPoint;

    public PointFEvaluator()
    {
    }

    public PointFEvaluator(PointF reuse)
    {
        mPoint = reuse;
    }

    @Override
    public PointF evaluate(float fraction, PointF startValue, PointF endValue)
    {
        float x = startValue.x + (fraction * (endValue.x - startValue.x));
        float y = startValue.y + (fraction * (endValue.y - startValue.y));

        if (mPoint == null) return new PointF(x, y);

        mPoint.set(x, y);
        return mPoint;
    }
}
