package android.view;

import android.graphics.Insets;

public interface WindowInsetsAnimationController
{
    Insets getHiddenStateInsets();

    Insets getShownStateInsets();

    Insets getCurrentInsets();

    float getCurrentFraction();

    float getCurrentAlpha();

    int getTypes();

    void setInsetsAndAlpha(Insets insets, float alpha, float fraction);

    void finish(boolean shown);

    boolean isFinished();

    boolean isCancelled();

    default boolean isReady()
    {
        return !isFinished() && !isCancelled();
    }
}
