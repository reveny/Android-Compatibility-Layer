package android.transition;

import android.graphics.Rect;
import android.view.ViewGroup;

public class CircularPropagation extends VisibilityPropagation
{
    private float mPropagationSpeed = 3.0f;

    public CircularPropagation()
    {
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
        TransitionValues positionValues;

        if (endValues == null || getViewVisibility(startValues) == android.view.View.VISIBLE)
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

        Rect epicenter = transition.getEpicenter();
        int epicenterX;
        int epicenterY;

        if (epicenter != null)
        {
            epicenterX = epicenter.centerX();
            epicenterY = epicenter.centerY();
        }
        else
        {
            int[] location = new int[2];
            sceneRoot.getLocationOnScreen(location);
            epicenterX = Math.round(location[0] + (sceneRoot.getWidth() / 2) + sceneRoot.getTranslationX());
            epicenterY = Math.round(location[1] + (sceneRoot.getHeight() / 2) + sceneRoot.getTranslationY());
        }

        double distance = distance(viewCenterX, viewCenterY, epicenterX, epicenterY);
        double maxDistance = distance(0, 0, sceneRoot.getWidth(), sceneRoot.getHeight());
        double distanceFraction = maxDistance == 0 ? 0 : distance / maxDistance;

        long duration = transition.getDuration();
        if (duration < 0) duration = 300;

        return Math.round(duration * directionMultiplier / mPropagationSpeed * distanceFraction);
    }

    private static double distance(float x1, float y1, float x2, float y2)
    {
        double x = x2 - x1;
        double y = y2 - y1;
        return Math.hypot(x, y);
    }
}
