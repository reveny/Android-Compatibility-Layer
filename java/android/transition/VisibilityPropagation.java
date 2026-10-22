package android.transition;

import android.view.View;

public abstract class VisibilityPropagation extends TransitionPropagation
{
    private static final String PROPNAME_VISIBILITY = "android:visibilityPropagation:visibility";
    private static final String PROPNAME_VIEW_CENTER = "android:visibilityPropagation:center";

    private static final String[] VISIBILITY_PROPAGATION_VALUES = { PROPNAME_VISIBILITY, PROPNAME_VIEW_CENTER };

    public VisibilityPropagation()
    {
    }

    @Override
    public void captureValues(TransitionValues values)
    {
        View view = values.view;

        Integer visibility = (Integer)values.values.get(Visibility.PROPNAME_VISIBILITY);
        if (visibility == null) visibility = view.getVisibility();
        values.values.put(PROPNAME_VISIBILITY, visibility);

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        location[0] += Math.round(view.getTranslationX());
        location[0] += view.getWidth() / 2;
        location[1] += Math.round(view.getTranslationY());
        location[1] += view.getHeight() / 2;
        values.values.put(PROPNAME_VIEW_CENTER, location);
    }

    @Override
    public String[] getPropagationProperties()
    {
        return VISIBILITY_PROPAGATION_VALUES;
    }

    public int getViewVisibility(TransitionValues values)
    {
        if (values == null) return View.GONE;

        Integer visibility = (Integer)values.values.get(PROPNAME_VISIBILITY);
        if (visibility == null) return View.GONE;

        return visibility;
    }

    public int getViewX(TransitionValues values)
    {
        return getViewCoordinate(values, 0);
    }

    public int getViewY(TransitionValues values)
    {
        return getViewCoordinate(values, 1);
    }

    private static int getViewCoordinate(TransitionValues values, int coordinateIndex)
    {
        if (values == null) return -1;

        int[] coordinates = (int[])values.values.get(PROPNAME_VIEW_CENTER);
        if (coordinates == null) return -1;

        return coordinates[coordinateIndex];
    }
}
