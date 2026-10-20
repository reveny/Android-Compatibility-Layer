package android.view;

public interface WindowInsetsController
{
    int APPEARANCE_OPAQUE_STATUS_BARS = 1 << 0;
    int APPEARANCE_OPAQUE_NAVIGATION_BARS = 1 << 1;
    int APPEARANCE_LOW_PROFILE_BARS = 1 << 2;
    int APPEARANCE_LIGHT_STATUS_BARS = 1 << 3;
    int APPEARANCE_LIGHT_NAVIGATION_BARS = 1 << 4;

    int BEHAVIOR_DEFAULT = 1;
    int BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2;

    void setSystemBarsAppearance(int appearance, int mask);

    int getSystemBarsAppearance();

    void setSystemBarsBehavior(int behavior);

    int getSystemBarsBehavior();

    void show(int types);

    void hide(int types);
}
