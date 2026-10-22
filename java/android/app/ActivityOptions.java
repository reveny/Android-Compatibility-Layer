package android.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

public class ActivityOptions
{
    public static final String EXTRA_USAGE_TIME_REPORT = "android.activity.usage_time";
    public static final String EXTRA_USAGE_TIME_REPORT_PACKAGES = "android.usage_time_packages";

    public static final int MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED = 0;
    public static final int MODE_BACKGROUND_ACTIVITY_START_ALLOWED = 1;
    public static final int MODE_BACKGROUND_ACTIVITY_START_DENIED = 2;

    private static final int ANIM_NONE = 0;
    private static final int ANIM_CUSTOM = 1;
    private static final int ANIM_SCALE_UP = 2;
    private static final int ANIM_THUMBNAIL_SCALE_UP = 3;
    private static final int ANIM_SCENE_TRANSITION = 5;
    private static final int ANIM_CLIP_REVEAL = 11;

    private static final String KEY_PACKAGE_NAME = "android:activity.packageName";
    private static final String KEY_LAUNCH_BOUNDS = "android:activity.launchBounds";
    private static final String KEY_ANIM_TYPE = "android:activity.animType";
    private static final String KEY_ANIM_ENTER_RES_ID = "android:activity.animEnterRes";
    private static final String KEY_ANIM_EXIT_RES_ID = "android:activity.animExitRes";
    private static final String KEY_ANIM_BACKGROUND_COLOR = "android:activity.backgroundColor";
    private static final String KEY_ANIM_START_X = "android:activity.animStartX";
    private static final String KEY_ANIM_START_Y = "android:activity.animStartY";
    private static final String KEY_ANIM_WIDTH = "android:activity.animWidth";
    private static final String KEY_ANIM_HEIGHT = "android:activity.animHeight";
    private static final String KEY_LAUNCH_DISPLAY_ID = "android.activity.launchDisplayId";
    private static final String KEY_LOCK_TASK_MODE = "android:activity.lockTaskMode";
    private static final String KEY_SPLASH_SCREEN_STYLE = "android.activity.splashScreenStyle";
    private static final String KEY_TASK_LAUNCH_BEHIND = "android:activity.launchTaskBehind";
    private static final String KEY_PENDING_INTENT_BACKGROUND_START_MODE = "android.pendingIntent.backgroundActivityAllowed";
    private static final String KEY_PENDING_INTENT_CREATOR_BACKGROUND_START_MODE = "android.activity.pendingIntentCreatorBackgroundActivityStartMode";
    private static final String KEY_SHARE_IDENTITY = "android:activity.shareIdentity";
    private static final String KEY_ALLOW_PASS_THROUGH_ON_TOUCH_OUTSIDE = "android:activity.allowPassThroughOnTouchOutside";

    private static final int INVALID_DISPLAY = -1;

    private String mPackageName;
    private Rect mLaunchBounds;
    private int mAnimationType = ANIM_NONE;
    private int mCustomEnterResId;
    private int mCustomExitResId;
    private int mCustomBackgroundColor;
    private int mStartX;
    private int mStartY;
    private int mWidth;
    private int mHeight;
    private int mLaunchDisplayId = INVALID_DISPLAY;
    private boolean mLockTaskMode;
    private int mSplashScreenStyle;
    private boolean mTaskLaunchBehind;
    private int mPendingIntentBackgroundStartMode = MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED;
    private int mPendingIntentCreatorBackgroundStartMode = MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED;
    private boolean mShareIdentity;
    private boolean mAllowPassThroughOnTouchOutside;
    private Bundle mAppVerificationBundle;
    private PendingIntent mUsageTimeReport;

    ActivityOptions()
    {
    }

    public static ActivityOptions makeCustomAnimation(Context context, int enterResId, int exitResId)
    {
        return makeCustomAnimation(context, enterResId, exitResId, 0);
    }

    public static ActivityOptions makeCustomAnimation(Context context, int enterResId, int exitResId, int backgroundColor)
    {
        ActivityOptions options = new ActivityOptions();
        options.mPackageName = context == null ? null : context.getPackageName();
        options.mAnimationType = ANIM_CUSTOM;
        options.mCustomEnterResId = enterResId;
        options.mCustomExitResId = exitResId;
        options.mCustomBackgroundColor = backgroundColor;
        return options;
    }

    public static ActivityOptions makeScaleUpAnimation(View source, int startX, int startY, int width, int height)
    {
        return makeSourceAnimation(source, ANIM_SCALE_UP, startX, startY, width, height);
    }

    public static ActivityOptions makeClipRevealAnimation(View source, int startX, int startY, int width, int height)
    {
        return makeSourceAnimation(source, ANIM_CLIP_REVEAL, startX, startY, width, height);
    }

    public static ActivityOptions makeThumbnailScaleUpAnimation(View source, Bitmap thumbnail, int startX, int startY)
    {
        int width = thumbnail == null ? 0 : thumbnail.getWidth();
        int height = thumbnail == null ? 0 : thumbnail.getHeight();
        return makeSourceAnimation(source, ANIM_THUMBNAIL_SCALE_UP, startX, startY, width, height);
    }

    public static ActivityOptions makeSceneTransitionAnimation(Activity activity, View sharedElement, String sharedElementName)
    {
        ActivityOptions options = new ActivityOptions();
        options.mAnimationType = ANIM_SCENE_TRANSITION;
        return options;
    }

    @SafeVarargs
    public static ActivityOptions makeSceneTransitionAnimation(Activity activity, Pair<View, String>... sharedElements)
    {
        ActivityOptions options = new ActivityOptions();
        options.mAnimationType = ANIM_SCENE_TRANSITION;
        return options;
    }

    public static ActivityOptions makeTaskLaunchBehind()
    {
        ActivityOptions options = new ActivityOptions();
        options.mTaskLaunchBehind = true;
        return options;
    }

    public static ActivityOptions makeBasic()
    {
        return new ActivityOptions();
    }

    private static ActivityOptions makeSourceAnimation(View source, int type, int startX, int startY, int width, int height)
    {
        ActivityOptions options = new ActivityOptions();
        options.mPackageName = source == null || source.getContext() == null ? null : source.getContext().getPackageName();
        options.mAnimationType = type;
        options.mStartX = startX;
        options.mStartY = startY;
        options.mWidth = width;
        options.mHeight = height;
        return options;
    }

    public ActivityOptions setLaunchBounds(Rect screenSpacePixelRect)
    {
        mLaunchBounds = screenSpacePixelRect == null ? null : new Rect(screenSpacePixelRect);
        return this;
    }

    public Rect getLaunchBounds()
    {
        return mLaunchBounds;
    }

    public boolean getLockTaskMode()
    {
        return mLockTaskMode;
    }

    public boolean isShareIdentityEnabled()
    {
        return mShareIdentity;
    }

    public int getSplashScreenStyle()
    {
        return mSplashScreenStyle;
    }

    public ActivityOptions setSplashScreenStyle(int style)
    {
        mSplashScreenStyle = style;
        return this;
    }

    public ActivityOptions setLockTaskEnabled(boolean lockTaskMode)
    {
        mLockTaskMode = lockTaskMode;
        return this;
    }

    public ActivityOptions setShareIdentityEnabled(boolean shareIdentity)
    {
        mShareIdentity = shareIdentity;
        return this;
    }

    public void setAllowPassThroughOnTouchOutside(boolean allowed)
    {
        mAllowPassThroughOnTouchOutside = allowed;
    }

    public boolean isAllowPassThroughOnTouchOutside()
    {
        return mAllowPassThroughOnTouchOutside;
    }

    public int getLaunchDisplayId()
    {
        return mLaunchDisplayId;
    }

    public ActivityOptions setLaunchDisplayId(int launchDisplayId)
    {
        mLaunchDisplayId = launchDisplayId;
        return this;
    }

    public ActivityOptions setPendingIntentCreatorBackgroundActivityStartMode(int mode)
    {
        mPendingIntentCreatorBackgroundStartMode = mode;
        return this;
    }

    public int getPendingIntentCreatorBackgroundActivityStartMode()
    {
        return mPendingIntentCreatorBackgroundStartMode;
    }

    public void update(ActivityOptions otherOptions)
    {
        if (otherOptions == null) return;

        if (otherOptions.mPackageName != null) mPackageName = otherOptions.mPackageName;
        if (otherOptions.mLaunchBounds != null) mLaunchBounds = otherOptions.mLaunchBounds;
        if (otherOptions.mAnimationType != ANIM_NONE)
        {
            mAnimationType = otherOptions.mAnimationType;
            mCustomEnterResId = otherOptions.mCustomEnterResId;
            mCustomExitResId = otherOptions.mCustomExitResId;
            mCustomBackgroundColor = otherOptions.mCustomBackgroundColor;
            mStartX = otherOptions.mStartX;
            mStartY = otherOptions.mStartY;
            mWidth = otherOptions.mWidth;
            mHeight = otherOptions.mHeight;
        }
        if (otherOptions.mUsageTimeReport != null) mUsageTimeReport = otherOptions.mUsageTimeReport;
    }

    public Bundle toBundle()
    {
        Bundle bundle = new Bundle();

        if (mPackageName != null) bundle.putString(KEY_PACKAGE_NAME, mPackageName);
        if (mLaunchBounds != null) bundle.putParcelable(KEY_LAUNCH_BOUNDS, mLaunchBounds);
        if (mAnimationType != ANIM_NONE) bundle.putInt(KEY_ANIM_TYPE, mAnimationType);

        if (mAnimationType == ANIM_CUSTOM)
        {
            bundle.putInt(KEY_ANIM_ENTER_RES_ID, mCustomEnterResId);
            bundle.putInt(KEY_ANIM_EXIT_RES_ID, mCustomExitResId);
            bundle.putInt(KEY_ANIM_BACKGROUND_COLOR, mCustomBackgroundColor);
        }
        else if (mAnimationType == ANIM_SCALE_UP || mAnimationType == ANIM_CLIP_REVEAL || mAnimationType == ANIM_THUMBNAIL_SCALE_UP)
        {
            bundle.putInt(KEY_ANIM_START_X, mStartX);
            bundle.putInt(KEY_ANIM_START_Y, mStartY);
            bundle.putInt(KEY_ANIM_WIDTH, mWidth);
            bundle.putInt(KEY_ANIM_HEIGHT, mHeight);
        }

        if (mLaunchDisplayId != INVALID_DISPLAY) bundle.putInt(KEY_LAUNCH_DISPLAY_ID, mLaunchDisplayId);
        if (mLockTaskMode) bundle.putBoolean(KEY_LOCK_TASK_MODE, true);
        if (mSplashScreenStyle != 0) bundle.putInt(KEY_SPLASH_SCREEN_STYLE, mSplashScreenStyle);
        if (mTaskLaunchBehind) bundle.putBoolean(KEY_TASK_LAUNCH_BEHIND, true);
        if (mShareIdentity) bundle.putBoolean(KEY_SHARE_IDENTITY, true);
        if (mAllowPassThroughOnTouchOutside) bundle.putBoolean(KEY_ALLOW_PASS_THROUGH_ON_TOUCH_OUTSIDE, true);

        if (mPendingIntentBackgroundStartMode != MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED)
        {
            bundle.putInt(KEY_PENDING_INTENT_BACKGROUND_START_MODE, mPendingIntentBackgroundStartMode);
        }

        if (mPendingIntentCreatorBackgroundStartMode != MODE_BACKGROUND_ACTIVITY_START_SYSTEM_DEFINED)
        {
            bundle.putInt(KEY_PENDING_INTENT_CREATOR_BACKGROUND_START_MODE, mPendingIntentCreatorBackgroundStartMode);
        }

        if (mAppVerificationBundle != null) bundle.putBundle("android:activity.appVerificationBundle", mAppVerificationBundle);

        return bundle;
    }

    public void requestUsageTimeReport(PendingIntent receiver)
    {
        mUsageTimeReport = receiver;
    }

    public ActivityOptions setAppVerificationBundle(Bundle bundle)
    {
        mAppVerificationBundle = bundle;
        return this;
    }

    public ActivityOptions setPendingIntentBackgroundActivityStartMode(int state)
    {
        mPendingIntentBackgroundStartMode = state;
        return this;
    }

    public int getPendingIntentBackgroundActivityStartMode()
    {
        return mPendingIntentBackgroundStartMode;
    }

    public void setPendingIntentBackgroundActivityLaunchAllowed(boolean allowed)
    {
        mPendingIntentBackgroundStartMode = allowed ? MODE_BACKGROUND_ACTIVITY_START_ALLOWED : MODE_BACKGROUND_ACTIVITY_START_DENIED;
    }

    public boolean isPendingIntentBackgroundActivityLaunchAllowed()
    {
        return mPendingIntentBackgroundStartMode != MODE_BACKGROUND_ACTIVITY_START_DENIED;
    }

    @Override
    public String toString()
    {
        return "ActivityOptions(" + hashCode() + "), mPackageName=" + mPackageName + ", mAnimationType=" + mAnimationType
               + ", mStartX=" + mStartX + ", mStartY=" + mStartY + ", mWidth=" + mWidth + ", mHeight=" + mHeight
               + ", mLaunchDisplayId=" + mLaunchDisplayId;
    }
}
