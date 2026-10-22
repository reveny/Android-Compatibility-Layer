package android.view;

public interface WindowManager extends ViewManager
{
    int COMPAT_SMALL_COVER_SCREEN_OPT_IN = 1;
    int SCREEN_RECORDING_STATE_NOT_VISIBLE = 0;
    int SCREEN_RECORDING_STATE_VISIBLE = 1;

    String PROPERTY_ACTIVITY_EMBEDDING_ALLOW_SYSTEM_OVERRIDE = "android.window.PROPERTY_ACTIVITY_EMBEDDING_ALLOW_SYSTEM_OVERRIDE";
    String PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED = "android.window.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED";
    String PROPERTY_CAMERA_COMPAT_ALLOW_FORCE_ROTATION = "android.window.PROPERTY_CAMERA_COMPAT_ALLOW_FORCE_ROTATION";
    String PROPERTY_CAMERA_COMPAT_ALLOW_REFRESH = "android.window.PROPERTY_CAMERA_COMPAT_ALLOW_REFRESH";
    String PROPERTY_CAMERA_COMPAT_ENABLE_REFRESH_VIA_PAUSE = "android.window.PROPERTY_CAMERA_COMPAT_ENABLE_REFRESH_VIA_PAUSE";
    String PROPERTY_COMPAT_ALLOW_DISPLAY_ORIENTATION_OVERRIDE = "android.window.PROPERTY_COMPAT_ALLOW_DISPLAY_ORIENTATION_OVERRIDE";
    String PROPERTY_COMPAT_ALLOW_IGNORING_ORIENTATION_REQUEST_WHEN_LOOP_DETECTED =
        "android.window.PROPERTY_COMPAT_ALLOW_IGNORING_ORIENTATION_REQUEST_WHEN_LOOP_DETECTED";
    String PROPERTY_COMPAT_ALLOW_MIN_ASPECT_RATIO_OVERRIDE = "android.window.PROPERTY_COMPAT_ALLOW_MIN_ASPECT_RATIO_OVERRIDE";
    String PROPERTY_COMPAT_ALLOW_ORIENTATION_OVERRIDE = "android.window.PROPERTY_COMPAT_ALLOW_ORIENTATION_OVERRIDE";
    String PROPERTY_COMPAT_ALLOW_RESIZEABLE_ACTIVITY_OVERRIDES = "android.window.PROPERTY_COMPAT_ALLOW_RESIZEABLE_ACTIVITY_OVERRIDES";
    String PROPERTY_COMPAT_ALLOW_SANDBOXING_VIEW_BOUNDS_APIS = "android.window.PROPERTY_COMPAT_ALLOW_SANDBOXING_VIEW_BOUNDS_APIS";
    String PROPERTY_COMPAT_ALLOW_SMALL_COVER_SCREEN = "android.window.PROPERTY_COMPAT_ALLOW_SMALL_COVER_SCREEN";
    String PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_FULLSCREEN_OVERRIDE =
        "android.window.PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_FULLSCREEN_OVERRIDE";
    String PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE = "android.window.PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE";
    String PROPERTY_COMPAT_ENABLE_FAKE_FOCUS = "android.window.PROPERTY_COMPAT_ENABLE_FAKE_FOCUS";
    String PROPERTY_COMPAT_IGNORE_REQUESTED_ORIENTATION = "android.window.PROPERTY_COMPAT_IGNORE_REQUESTED_ORIENTATION";
    String PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI = "android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI";

    void addView(View view, ViewGroup.LayoutParams params);

    void updateViewLayout(View view, ViewGroup.LayoutParams params);

    void removeView(View view);

    void removeViewImmediate(View view);

    Display getDefaultDisplay();

    default WindowMetrics getCurrentWindowMetrics()
    {
        throw new UnsupportedOperationException();
    }

    default WindowMetrics getMaximumWindowMetrics()
    {
        throw new UnsupportedOperationException();
    }

    default boolean isCrossWindowBlurEnabled()
    {
        return false;
    }

    default void addCrossWindowBlurEnabledListener(java.util.function.Consumer<Boolean> listener)
    {
        addCrossWindowBlurEnabledListener(Runnable::run, listener);
    }

    default void addCrossWindowBlurEnabledListener(java.util.concurrent.Executor executor, java.util.function.Consumer<Boolean> listener)
    {
        if (executor == null || listener == null) throw new NullPointerException();

        executor.execute(() -> listener.accept(isCrossWindowBlurEnabled()));
    }

    default void removeCrossWindowBlurEnabledListener(java.util.function.Consumer<Boolean> listener)
    {
    }

    default void addProposedRotationListener(java.util.concurrent.Executor executor, java.util.function.IntConsumer listener)
    {
    }

    default void removeProposedRotationListener(java.util.function.IntConsumer listener)
    {
    }

    default int addScreenRecordingCallback(java.util.concurrent.Executor executor, java.util.function.Consumer<Integer> callback)
    {
        return 0;
    }

    default void removeScreenRecordingCallback(java.util.function.Consumer<Integer> callback)
    {
    }

    class BadTokenException extends RuntimeException
    {
        public BadTokenException()
        {
        }

        public BadTokenException(String name)
        {
            super(name);
        }
    }

    class InvalidDisplayException extends RuntimeException
    {
        public InvalidDisplayException()
        {
        }

        public InvalidDisplayException(String name)
        {
            super(name);
        }
    }

    class LayoutParams extends ViewGroup.LayoutParams implements android.os.Parcelable
    {
        public static final int FIRST_APPLICATION_WINDOW = 1;
        public static final int TYPE_BASE_APPLICATION = 1;
        public static final int TYPE_APPLICATION = 2;
        public static final int TYPE_APPLICATION_STARTING = 3;
        public static final int TYPE_DRAWN_APPLICATION = 4;
        public static final int LAST_APPLICATION_WINDOW = 99;
        public static final int FIRST_SUB_WINDOW = 1000;
        public static final int TYPE_APPLICATION_PANEL = 1000;
        public static final int TYPE_APPLICATION_MEDIA = 1001;
        public static final int TYPE_APPLICATION_SUB_PANEL = 1002;
        public static final int TYPE_APPLICATION_ATTACHED_DIALOG = 1003;
        public static final int LAST_SUB_WINDOW = 1999;
        public static final int FIRST_SYSTEM_WINDOW = 2000;
        public static final int TYPE_STATUS_BAR = 2000;
        public static final int TYPE_SEARCH_BAR = 2001;
        public static final int TYPE_PHONE = 2002;
        public static final int TYPE_SYSTEM_ALERT = 2003;
        public static final int TYPE_TOAST = 2005;
        public static final int TYPE_SYSTEM_OVERLAY = 2006;
        public static final int TYPE_PRIORITY_PHONE = 2007;
        public static final int TYPE_SYSTEM_DIALOG = 2008;
        public static final int TYPE_KEYGUARD_DIALOG = 2009;
        public static final int TYPE_SYSTEM_ERROR = 2010;
        public static final int TYPE_INPUT_METHOD = 2011;
        public static final int TYPE_INPUT_METHOD_DIALOG = 2012;
        public static final int TYPE_WALLPAPER = 2013;
        public static final int TYPE_PRIVATE_PRESENTATION = 2030;
        public static final int TYPE_ACCESSIBILITY_OVERLAY = 2032;
        public static final int TYPE_APPLICATION_OVERLAY = 2038;
        public static final int LAST_SYSTEM_WINDOW = 2999;

        public static final int FLAG_ALLOW_LOCK_WHILE_SCREEN_ON = 0x00000001;
        public static final int FLAG_DIM_BEHIND = 0x00000002;
        @Deprecated
        public static final int FLAG_BLUR_BEHIND = 0x00000004;
        @Deprecated
        public static final int FLAG_TOUCHABLE_WHEN_WAKING = 0x00000040;
        @Deprecated
        public static final int FLAG_DITHER = 0x00001000;
        public static final int DISPLAY_FLAG_DISABLE_HDR_CONVERSION = 1;
        @Deprecated
        public static final int MEMORY_TYPE_NORMAL = 0;
        @Deprecated
        public static final int MEMORY_TYPE_HARDWARE = 1;
        @Deprecated
        public static final int MEMORY_TYPE_GPU = 2;
        @Deprecated
        public static final int MEMORY_TYPE_PUSH_BUFFERS = 3;
        public static final int FLAG_NOT_FOCUSABLE = 0x00000008;
        public static final int FLAG_NOT_TOUCHABLE = 0x00000010;
        public static final int FLAG_NOT_TOUCH_MODAL = 0x00000020;
        public static final int FLAG_KEEP_SCREEN_ON = 0x00000080;
        public static final int FLAG_LAYOUT_IN_SCREEN = 0x00000100;
        public static final int FLAG_LAYOUT_NO_LIMITS = 0x00000200;
        public static final int FLAG_FULLSCREEN = 0x00000400;
        public static final int FLAG_FORCE_NOT_FULLSCREEN = 0x00000800;
        public static final int FLAG_SECURE = 0x00002000;
        public static final int FLAG_SCALED = 0x00004000;
        public static final int FLAG_IGNORE_CHEEK_PRESSES = 0x00008000;
        public static final int FLAG_LAYOUT_INSET_DECOR = 0x00010000;
        public static final int FLAG_ALT_FOCUSABLE_IM = 0x00020000;
        public static final int FLAG_WATCH_OUTSIDE_TOUCH = 0x00040000;
        public static final int FLAG_SHOW_WHEN_LOCKED = 0x00080000;
        public static final int FLAG_SHOW_WALLPAPER = 0x00100000;
        public static final int FLAG_TURN_SCREEN_ON = 0x00200000;
        public static final int FLAG_DISMISS_KEYGUARD = 0x00400000;
        public static final int FLAG_SPLIT_TOUCH = 0x00800000;
        public static final int FLAG_HARDWARE_ACCELERATED = 0x01000000;
        public static final int FLAG_LAYOUT_IN_OVERSCAN = 0x02000000;
        public static final int FLAG_TRANSLUCENT_STATUS = 0x04000000;
        public static final int FLAG_TRANSLUCENT_NAVIGATION = 0x08000000;
        public static final int FLAG_LOCAL_FOCUS_MODE = 0x10000000;
        public static final int FLAG_LAYOUT_ATTACHED_IN_DECOR = 0x40000000;
        public static final int FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = 0x80000000;

        public static final int SOFT_INPUT_STATE_UNSPECIFIED = 0;
        public static final int SOFT_INPUT_STATE_UNCHANGED = 1;
        public static final int SOFT_INPUT_STATE_HIDDEN = 2;
        public static final int SOFT_INPUT_STATE_ALWAYS_HIDDEN = 3;
        public static final int SOFT_INPUT_STATE_VISIBLE = 4;
        public static final int SOFT_INPUT_STATE_ALWAYS_VISIBLE = 5;
        public static final int SOFT_INPUT_MASK_STATE = 0x0f;
        public static final int SOFT_INPUT_ADJUST_UNSPECIFIED = 0x00;
        public static final int SOFT_INPUT_ADJUST_RESIZE = 0x10;
        public static final int SOFT_INPUT_ADJUST_PAN = 0x20;
        public static final int SOFT_INPUT_ADJUST_NOTHING = 0x30;
        public static final int SOFT_INPUT_MASK_ADJUST = 0xf0;
        public static final int SOFT_INPUT_IS_FORWARD_NAVIGATION = 0x100;
        public static final int SOFT_INPUT_MODE_CHANGED = 0x200;

        public static final int ROTATION_ANIMATION_ROTATE = 0;
        public static final int ROTATION_ANIMATION_CROSSFADE = 1;
        public static final int ROTATION_ANIMATION_JUMPCUT = 2;
        public static final int ROTATION_ANIMATION_SEAMLESS = 3;
        public static final int ROTATION_ANIMATION_CHANGED = 0x1000;

        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT = 0;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES = 1;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER = 2;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS = 3;

        public static final int LAYOUT_CHANGED = 1;
        public static final int TYPE_CHANGED = 2;
        public static final int FLAGS_CHANGED = 4;
        public static final int FORMAT_CHANGED = 8;
        public static final int ANIMATION_CHANGED = 16;
        public static final int DIM_AMOUNT_CHANGED = 32;
        public static final int TITLE_CHANGED = 64;
        public static final int ALPHA_CHANGED = 128;
        public static final int MEMORY_TYPE_CHANGED = 256;
        public static final int SOFT_INPUT_MODE_CHANGED_FLAG = 512;
        public static final int SCREEN_ORIENTATION_CHANGED = 1024;
        public static final int SCREEN_BRIGHTNESS_CHANGED = 2048;

        public static final float BRIGHTNESS_OVERRIDE_NONE = -1.0f;
        public static final float BRIGHTNESS_OVERRIDE_OFF = 0.0f;
        public static final float BRIGHTNESS_OVERRIDE_FULL = 1.0f;

        public static final android.os.Parcelable.Creator<LayoutParams> CREATOR = new android.os.Parcelable.Creator<LayoutParams>()
        {
            @Override
            public LayoutParams createFromParcel(android.os.Parcel source)
            {
                return new LayoutParams(source);
            }

            @Override
            public LayoutParams[] newArray(int size)
            {
                return new LayoutParams[size];
            }
        };

        public int x;
        public int y;
        public float horizontalWeight;
        public float verticalWeight;
        public int type;
        public int memoryType;
        public int flags;
        public int softInputMode;
        public int gravity;
        public float horizontalMargin;
        public float verticalMargin;
        public int format;
        public int windowAnimations;
        public float alpha = 1.0f;
        public float dimAmount = 1.0f;
        public float screenBrightness = BRIGHTNESS_OVERRIDE_NONE;
        public float buttonBrightness = BRIGHTNESS_OVERRIDE_NONE;
        public int rotationAnimation = ROTATION_ANIMATION_ROTATE;
        public android.os.IBinder token;
        public String packageName;
        public int screenOrientation = -1;
        public float preferredRefreshRate;
        public int preferredDisplayModeId;
        public int systemUiVisibility;
        public int layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
        public boolean preferMinimalPostProcessing;

        private int mColorMode;
        private float mDesiredHdrHeadroom;
        private int mBlurBehindRadius;
        private boolean mWallpaperTouchEventsEnabled = true;
        private boolean mCanPlayMoveAnimation = true;
        private boolean mFrameRateBoostOnTouch = true;
        private boolean mFrameRatePowerSavingsBalanced = true;
        private boolean mHdrConversionEnabled = true;
        private CharSequence mTitle = "";
        private boolean mFitInsetsIgnoringVisibility;
        private int mFitInsetsTypes = WindowInsets.Type.systemBars();
        private int mFitInsetsSides = WindowInsets.Side.all();

        public LayoutParams()
        {
            super(MATCH_PARENT, MATCH_PARENT);
            type = TYPE_APPLICATION;
            format = android.graphics.PixelFormat.OPAQUE;
        }

        public LayoutParams(int type)
        {
            this();
            this.type = type;
        }

        public LayoutParams(int type, int flags)
        {
            this(type);
            this.flags = flags;
        }

        public LayoutParams(int type, int flags, int format)
        {
            this(type, flags);
            this.format = format;
        }

        public LayoutParams(int width, int height, int type, int flags, int format)
        {
            super(width, height);
            this.type = type;
            this.flags = flags;
            this.format = format;
        }

        public LayoutParams(int width, int height, int x, int y, int type, int flags, int format)
        {
            this(width, height, type, flags, format);
            this.x = x;
            this.y = y;
        }

        public LayoutParams(android.os.Parcel in)
        {
            this();
            if (in == null) return;

            width = in.readInt();
            height = in.readInt();
            x = in.readInt();
            y = in.readInt();
            type = in.readInt();
            flags = in.readInt();
            softInputMode = in.readInt();
            layoutInDisplayCutoutMode = in.readInt();
            gravity = in.readInt();
            horizontalMargin = in.readFloat();
            verticalMargin = in.readFloat();
            horizontalWeight = in.readFloat();
            verticalWeight = in.readFloat();
            format = in.readInt();
            windowAnimations = in.readInt();
            alpha = in.readFloat();
            dimAmount = in.readFloat();
            screenBrightness = in.readFloat();
            buttonBrightness = in.readFloat();
            rotationAnimation = in.readInt();
            token = in.readStrongBinder();
            packageName = in.readString();
            String title = in.readString();
            mTitle = title == null ? "" : title;
            screenOrientation = in.readInt();
            preferredRefreshRate = in.readFloat();
            preferredDisplayModeId = in.readInt();
            systemUiVisibility = in.readInt();
            memoryType = in.readInt();
            mFitInsetsTypes = in.readInt();
            mFitInsetsSides = in.readInt();
            mFitInsetsIgnoringVisibility = in.readInt() != 0;
            preferMinimalPostProcessing = in.readInt() != 0;
            mColorMode = in.readInt();
            mDesiredHdrHeadroom = in.readFloat();
            mBlurBehindRadius = in.readInt();
            mWallpaperTouchEventsEnabled = in.readInt() != 0;
            mCanPlayMoveAnimation = in.readInt() != 0;
            mFrameRateBoostOnTouch = in.readInt() != 0;
            mFrameRatePowerSavingsBalanced = in.readInt() != 0;
            mHdrConversionEnabled = in.readInt() != 0;
        }

        public final void setTitle(CharSequence title)
        {
            mTitle = title == null ? "" : title;
        }

        public final CharSequence getTitle()
        {
            return mTitle;
        }

        public void setFitInsetsTypes(int types)
        {
            mFitInsetsTypes = types;
        }

        public void setFitInsetsSides(int sides)
        {
            mFitInsetsSides = sides;
        }

        public void setFitInsetsIgnoringVisibility(boolean ignore)
        {
            mFitInsetsIgnoringVisibility = ignore;
        }

        public int getFitInsetsTypes()
        {
            return mFitInsetsTypes;
        }

        public int getFitInsetsSides()
        {
            return mFitInsetsSides;
        }

        public boolean isFitInsetsIgnoringVisibility()
        {
            return mFitInsetsIgnoringVisibility;
        }

        public static boolean mayUseInputMethod(int flags)
        {
            return ((flags & FLAG_NOT_FOCUSABLE) == 0) != ((flags & FLAG_ALT_FOCUSABLE_IM) != 0);
        }

        public final int copyFrom(LayoutParams other)
        {
            int changes = 0;

            if (width != other.width || height != other.height) changes |= LAYOUT_CHANGED;
            width = other.width;
            height = other.height;

            if (x != other.x || y != other.y || gravity != other.gravity) changes |= LAYOUT_CHANGED;
            x = other.x;
            y = other.y;
            horizontalWeight = other.horizontalWeight;
            verticalWeight = other.verticalWeight;
            horizontalMargin = other.horizontalMargin;
            verticalMargin = other.verticalMargin;
            gravity = other.gravity;

            if (type != other.type) changes |= TYPE_CHANGED;
            type = other.type;

            if (flags != other.flags) changes |= FLAGS_CHANGED;
            flags = other.flags;

            if (format != other.format) changes |= FORMAT_CHANGED;
            format = other.format;

            if (windowAnimations != other.windowAnimations) changes |= ANIMATION_CHANGED;
            windowAnimations = other.windowAnimations;

            if (dimAmount != other.dimAmount) changes |= DIM_AMOUNT_CHANGED;
            dimAmount = other.dimAmount;

            if (alpha != other.alpha) changes |= ALPHA_CHANGED;
            alpha = other.alpha;

            if (softInputMode != other.softInputMode) changes |= SOFT_INPUT_MODE_CHANGED_FLAG;
            softInputMode = other.softInputMode;

            if (!mTitle.equals(other.mTitle)) changes |= TITLE_CHANGED;
            mTitle = other.mTitle;

            memoryType = other.memoryType;
            screenBrightness = other.screenBrightness;
            buttonBrightness = other.buttonBrightness;
            rotationAnimation = other.rotationAnimation;
            token = other.token != null ? other.token : token;
            packageName = other.packageName != null ? other.packageName : packageName;
            screenOrientation = other.screenOrientation;
            preferredRefreshRate = other.preferredRefreshRate;
            preferredDisplayModeId = other.preferredDisplayModeId;
            systemUiVisibility = other.systemUiVisibility;
            layoutInDisplayCutoutMode = other.layoutInDisplayCutoutMode;
            mFitInsetsTypes = other.mFitInsetsTypes;
            mFitInsetsSides = other.mFitInsetsSides;
            mFitInsetsIgnoringVisibility = other.mFitInsetsIgnoringVisibility;
            preferMinimalPostProcessing = other.preferMinimalPostProcessing;
            mColorMode = other.mColorMode;
            mDesiredHdrHeadroom = other.mDesiredHdrHeadroom;
            mBlurBehindRadius = other.mBlurBehindRadius;
            mWallpaperTouchEventsEnabled = other.mWallpaperTouchEventsEnabled;
            mCanPlayMoveAnimation = other.mCanPlayMoveAnimation;
            mFrameRateBoostOnTouch = other.mFrameRateBoostOnTouch;
            mFrameRatePowerSavingsBalanced = other.mFrameRatePowerSavingsBalanced;
            mHdrConversionEnabled = other.mHdrConversionEnabled;

            return changes;
        }

        public void setColorMode(int colorMode)
        {
            mColorMode = colorMode;
        }

        public int getColorMode()
        {
            return mColorMode;
        }

        public void setDesiredHdrHeadroom(float desiredHeadroom)
        {
            if (!Float.isFinite(desiredHeadroom) || (desiredHeadroom != 0 && (desiredHeadroom < 1.0f || desiredHeadroom > 10000.0f)))
            {
                throw new IllegalArgumentException("desiredHeadroom must be 0 or within [1.0, 10000.0], received: " + desiredHeadroom);
            }

            mDesiredHdrHeadroom = desiredHeadroom;
        }

        public float getDesiredHdrHeadroom()
        {
            return mDesiredHdrHeadroom;
        }

        public void setBlurBehindRadius(int blurBehindRadius)
        {
            mBlurBehindRadius = blurBehindRadius;
        }

        public int getBlurBehindRadius()
        {
            return mBlurBehindRadius;
        }

        public void setWallpaperTouchEventsEnabled(boolean enable)
        {
            mWallpaperTouchEventsEnabled = enable;
        }

        public boolean areWallpaperTouchEventsEnabled()
        {
            return mWallpaperTouchEventsEnabled;
        }

        public void setCanPlayMoveAnimation(boolean enable)
        {
            mCanPlayMoveAnimation = enable;
        }

        public boolean canPlayMoveAnimation()
        {
            return mCanPlayMoveAnimation;
        }

        public void setFrameRateBoostOnTouchEnabled(boolean enabled)
        {
            mFrameRateBoostOnTouch = enabled;
        }

        public boolean getFrameRateBoostOnTouchEnabled()
        {
            return mFrameRateBoostOnTouch;
        }

        public void setFrameRatePowerSavingsBalanced(boolean enabled)
        {
            mFrameRatePowerSavingsBalanced = enabled;
        }

        public boolean isFrameRatePowerSavingsBalanced()
        {
            return mFrameRatePowerSavingsBalanced;
        }

        public void setHdrConversionEnabled(boolean enabled)
        {
            mHdrConversionEnabled = enabled;
        }

        public boolean isHdrConversionEnabled()
        {
            return mHdrConversionEnabled;
        }

        public String debug(String output)
        {
            return output + "Contents of " + this + ":";
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(android.os.Parcel out, int parcelableFlags)
        {
            out.writeInt(width);
            out.writeInt(height);
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(type);
            out.writeInt(flags);
            out.writeInt(softInputMode);
            out.writeInt(layoutInDisplayCutoutMode);
            out.writeInt(gravity);
            out.writeFloat(horizontalMargin);
            out.writeFloat(verticalMargin);
            out.writeFloat(horizontalWeight);
            out.writeFloat(verticalWeight);
            out.writeInt(format);
            out.writeInt(windowAnimations);
            out.writeFloat(alpha);
            out.writeFloat(dimAmount);
            out.writeFloat(screenBrightness);
            out.writeFloat(buttonBrightness);
            out.writeInt(rotationAnimation);
            out.writeStrongBinder(token);
            out.writeString(packageName);
            out.writeString(mTitle.toString());
            out.writeInt(screenOrientation);
            out.writeFloat(preferredRefreshRate);
            out.writeInt(preferredDisplayModeId);
            out.writeInt(systemUiVisibility);
            out.writeInt(memoryType);
            out.writeInt(mFitInsetsTypes);
            out.writeInt(mFitInsetsSides);
            out.writeInt(mFitInsetsIgnoringVisibility ? 1 : 0);
            out.writeInt(preferMinimalPostProcessing ? 1 : 0);
            out.writeInt(mColorMode);
            out.writeFloat(mDesiredHdrHeadroom);
            out.writeInt(mBlurBehindRadius);
            out.writeInt(mWallpaperTouchEventsEnabled ? 1 : 0);
            out.writeInt(mCanPlayMoveAnimation ? 1 : 0);
            out.writeInt(mFrameRateBoostOnTouch ? 1 : 0);
            out.writeInt(mFrameRatePowerSavingsBalanced ? 1 : 0);
            out.writeInt(mHdrConversionEnabled ? 1 : 0);
        }

        @Override
        public String toString()
        {
            return "WM.LayoutParams{(" + x + "," + y + ")(" + width + "x" + height + ") gravity=" + gravity + " type=" + type
                   + " flags=0x" + Integer.toHexString(flags) + "}";
        }
    }
}
