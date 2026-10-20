package android.view;

public interface WindowManager
{
    void addView(View view, ViewGroup.LayoutParams params);

    void updateViewLayout(View view, ViewGroup.LayoutParams params);

    void removeView(View view);

    void removeViewImmediate(View view);

    Display getDefaultDisplay();

    class LayoutParams extends ViewGroup.LayoutParams
    {
        public static final int FLAG_ALLOW_LOCK_WHILE_SCREEN_ON = 0x00000001;
        public static final int FLAG_DIM_BEHIND = 0x00000002;
        public static final int FLAG_NOT_FOCUSABLE = 0x00000008;
        public static final int FLAG_NOT_TOUCHABLE = 0x00000010;
        public static final int FLAG_NOT_TOUCH_MODAL = 0x00000020;
        public static final int FLAG_KEEP_SCREEN_ON = 0x00000080;
        public static final int FLAG_LAYOUT_IN_SCREEN = 0x00000100;
        public static final int FLAG_FULLSCREEN = 0x00000400;
        public static final int FLAG_FORCE_NOT_FULLSCREEN = 0x00000800;
        public static final int FLAG_SECURE = 0x00002000;
        public static final int FLAG_LAYOUT_INSET_DECOR = 0x00010000;
        public static final int FLAG_ALT_FOCUSABLE_IM = 0x00020000;
        public static final int FLAG_SHOW_WHEN_LOCKED = 0x00080000;
        public static final int FLAG_TURN_SCREEN_ON = 0x00200000;
        public static final int FLAG_DISMISS_KEYGUARD = 0x00400000;
        public static final int FLAG_HARDWARE_ACCELERATED = 0x01000000;
        public static final int FLAG_LAYOUT_IN_OVERSCAN = 0x02000000;
        public static final int FLAG_LAYOUT_NO_LIMITS = 0x00000200;
        public static final int FLAG_TRANSLUCENT_STATUS = 0x04000000;
        public static final int FLAG_TRANSLUCENT_NAVIGATION = 0x08000000;
        public static final int FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = 0x80000000;

        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT = 0;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES = 1;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER = 2;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS = 3;

        public int flags;
        public int type;
        public int format;
        public int gravity;
        public int softInputMode;
        public int layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
        public int systemUiVisibility;

        public LayoutParams()
        {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height)
        {
            super(width, height);
        }
    }
}
