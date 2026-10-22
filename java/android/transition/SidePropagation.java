package android.transition;

import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

public class SidePropagation extends VisibilityPropagation
{
    private float mPropagationSpeed = 3.0f;
    private int mSide = Gravity.BOTTOM;

    public SidePropagation()
    {
    }

    public void setSide(int side)
    {
        mSide = side;
    }

    public void setPropagationSpeed(float propagationSpeed)
    {
        if (propagationSpeed == 0) throw new IllegalArgumentException("propagationSpeed may not be 0");

        mPropagationSpeed = propagationSpeed;
    }

    @Override
    public long getStartDelay(ViewGroup sceneRoot, Transition transition, TransitionValues startValues, TransitionValues endValues)
    {
        if (startValues == null && endValues == null) return 0;

        int directionMultiplier = 1;
        Rect epicenter = transition.getEpicenter();
        TransitionValues positionValues;

        if (endValues == null || getViewVisibility(startValues) == View.VISIBLE)
        {
            positionValues = startValues;
            directionMultiplier = -1;
        }
        else
        {
            positionValues = endValues;
        }

        int viewCenterX = getViewX(positionValues);
        int viewCenterY = getViewY(positionValues);

        int[] location = new int[2];
        sceneRoot.getLocationOnScreen(location);
        int left = location[0] + Math.round(sceneRoot.getTranslationX());
        int top = location[1] + Math.round(sceneRoot.getTranslationY());
        int right = left + sceneRoot.getWidth();
        int bottom = top + sceneRoot.getHeight();

        int epicenterX;
        int epicenterY;

        if (epicenter != null)
        {
            epicenterX = epicenter.centerX();
            epicenterY = epicenter.centerY();
        }
        else
        {
            epicenterX = (left + right) / 2;
            epicenterY = (top + bottom) / 2;
        }

        float distance = distance(sceneRoot, viewCenterX, viewCenterY, epicenterX, epicenterY, left, top, right, bottom);
        float maxDistance = getMaxDistance(sceneRoot);
        float distanceFraction = maxDistance == 0 ? 0 : distance / maxDistance;

        long duration = transition.getDuration();
        if (duration < 0) duration = 300;

        return Math.round(duration * directionMultiplier / mPropagationSpeed * distanceFraction);
    }

    private int distance(View sceneRoot, int viewX, int viewY, int epicenterX, int epicenterY, int left, int top, int right,
                         int bottom)
    {
        int side;

        if (mSide == Gravity.START)
        {
            boolean isRtl = sceneRoot.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            side = isRtl ? Gravity.RIGHT : Gravity.LEFT;
        }
        else if (mSide == Gravity.END)
        {
            boolean isRtl = sceneRoot.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            side = isRtl ? Gravity.LEFT : Gravity.RIGHT;
        }
        else
        {
            side = mSide;
        }

        switch (side)
        {
            case Gravity.LEFT: return right - viewX + Math.abs(epicenterY - viewY);
            case Gravity.TOP: return bottom - viewY + Math.abs(epicenterX - viewX);
            case Gravity.RIGHT: return viewX - left + Math.abs(epicenterY - viewY);
            case Gravity.BOTTOM: return viewY - top + Math.abs(epicenterX - viewX);
            default: return 0;
        }
    }

    private int getMaxDistance(ViewGroup sceneRoot)
    {
        switch (mSide)
        {
            case Gravity.LEFT:
            case Gravity.RIGHT:
            case Gravity.START:
            case Gravity.END:
                return sceneRoot.getWidth();
            default:
                return sceneRoot.getHeight();
        }
    }
}
