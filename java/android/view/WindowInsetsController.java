package android.view;

import android.os.CancellationSignal;
import android.view.animation.Interpolator;

public interface WindowInsetsController
{
    int APPEARANCE_OPAQUE_STATUS_BARS = 1 << 0;
    int APPEARANCE_OPAQUE_NAVIGATION_BARS = 1 << 1;
    int APPEARANCE_LOW_PROFILE_BARS = 1 << 2;
    int APPEARANCE_LIGHT_STATUS_BARS = 1 << 3;
    int APPEARANCE_LIGHT_NAVIGATION_BARS = 1 << 4;
    int APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND = 1 << 7;
    int APPEARANCE_LIGHT_CAPTION_BARS = 1 << 8;

    int BEHAVIOR_SHOW_BARS_BY_TOUCH = 0;
    int BEHAVIOR_DEFAULT = 1;
    int BEHAVIOR_SHOW_BARS_BY_SWIPE = 1;
    int BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2;

    void setSystemBarsAppearance(int appearance, int mask);

    int getSystemBarsAppearance();

    void setSystemBarsBehavior(int behavior);

    int getSystemBarsBehavior();

    void show(int types);

    void hide(int types);

    void controlWindowInsetsAnimation(int types, long durationMillis, Interpolator interpolator, CancellationSignal cancellationSignal,
                                      WindowInsetsAnimationControlListener listener);

    void addOnControllableInsetsChangedListener(OnControllableInsetsChangedListener listener);

    void removeOnControllableInsetsChangedListener(OnControllableInsetsChangedListener listener);

    interface OnControllableInsetsChangedListener
    {
        void onControllableInsetsChanged(WindowInsetsController controller, int typeMask);
    }
}
