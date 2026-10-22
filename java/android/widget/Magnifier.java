package android.widget;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

public final class Magnifier
{
    public static final int SOURCE_BOUND_MAX_IN_SURFACE = 0;
    public static final int SOURCE_BOUND_MAX_VISIBLE = 1;

    private final View mView;
    private final int mWidth;
    private final int mHeight;
    private float mZoom;
    private int mSourceWidth;
    private int mSourceHeight;
    private final float mElevation;
    private final float mCornerRadius;
    private final Drawable mOverlay;
    private final int mHorizontalOffset;
    private final int mVerticalOffset;
    private final boolean mClippingEnabled;
    private final int mLeftBound;
    private final int mTopBound;
    private final int mRightBound;
    private final int mBottomBound;
    private boolean mShowing;
    private float mSourceCenterX;
    private float mSourceCenterY;
    private float mMagnifierCenterX;
    private float mMagnifierCenterY;

    public Magnifier(View view)
    {
        this(new Builder(view));
    }

    private Magnifier(Builder builder)
    {
        mView = builder.mView;
        mWidth = builder.mWidth;
        mHeight = builder.mHeight;
        mZoom = builder.mZoom;
        mElevation = builder.mElevation;
        mCornerRadius = builder.mCornerRadius;
        mOverlay = builder.mOverlay;
        mHorizontalOffset = builder.mHorizontalOffset;
        mVerticalOffset = builder.mVerticalOffset;
        mClippingEnabled = builder.mClippingEnabled;
        mLeftBound = builder.mLeftBound;
        mTopBound = builder.mTopBound;
        mRightBound = builder.mRightBound;
        mBottomBound = builder.mBottomBound;
        updateSourceSize();
    }

    private void updateSourceSize()
    {
        mSourceWidth = Math.round(mWidth / mZoom);
        mSourceHeight = Math.round(mHeight / mZoom);
    }

    public void show(float sourceCenterX, float sourceCenterY)
    {
        show(sourceCenterX, sourceCenterY, sourceCenterX + mHorizontalOffset, sourceCenterY + mVerticalOffset);
    }

    public void show(float sourceCenterX, float sourceCenterY, float magnifierCenterX, float magnifierCenterY)
    {
        mSourceCenterX = clamp(sourceCenterX, 0, mView.getWidth());
        mSourceCenterY = clamp(sourceCenterY, 0, mView.getHeight());
        mMagnifierCenterX = magnifierCenterX;
        mMagnifierCenterY = magnifierCenterY;
        mShowing = true;
    }

    private static float clamp(float value, float minimum, float maximum)
    {
        if (maximum < minimum) return minimum;

        return Math.max(minimum, Math.min(maximum, value));
    }

    public void dismiss()
    {
        mShowing = false;
    }

    public void update()
    {
        if (mShowing && mOverlay != null) mOverlay.invalidateSelf();
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public int getSourceWidth()
    {
        return mSourceWidth;
    }

    public int getSourceHeight()
    {
        return mSourceHeight;
    }

    public void setZoom(float zoom)
    {
        if (zoom <= 0) throw new IllegalArgumentException("Zoom should be positive");

        mZoom = zoom;
        updateSourceSize();
    }

    public float getZoom()
    {
        return mZoom;
    }

    public float getElevation()
    {
        return mElevation;
    }

    public float getCornerRadius()
    {
        return mCornerRadius;
    }

    public int getDefaultHorizontalSourceToMagnifierOffset()
    {
        return mHorizontalOffset;
    }

    public int getDefaultVerticalSourceToMagnifierOffset()
    {
        return mVerticalOffset;
    }

    public Drawable getOverlay()
    {
        return mOverlay;
    }

    public boolean isClippingEnabled()
    {
        return mClippingEnabled;
    }

    public Point getPosition()
    {
        if (!mShowing) return null;

        int[] location = new int[2];
        mView.getLocationInWindow(location);

        int left = Math.round(location[0] + mMagnifierCenterX - mWidth / 2f);
        int top = Math.round(location[1] + mMagnifierCenterY - mHeight / 2f);
        return new Point(left, top);
    }

    public Point getSourcePosition()
    {
        if (!mShowing) return null;

        int[] location = new int[2];
        mView.getLocationInWindow(location);

        float left = location[0] + mSourceCenterX - mSourceWidth / 2f;
        float top = location[1] + mSourceCenterY - mSourceHeight / 2f;

        View root = mView.getRootView();
        float minimumLeft = mLeftBound == SOURCE_BOUND_MAX_IN_SURFACE ? 0 : location[0];
        float minimumTop = mTopBound == SOURCE_BOUND_MAX_IN_SURFACE ? 0 : location[1];
        float maximumRight = mRightBound == SOURCE_BOUND_MAX_IN_SURFACE ? root.getWidth() : location[0] + mView.getWidth();
        float maximumBottom = mBottomBound == SOURCE_BOUND_MAX_IN_SURFACE ? root.getHeight() : location[1] + mView.getHeight();

        left = clamp(left, minimumLeft, maximumRight - mSourceWidth);
        top = clamp(top, minimumTop, maximumBottom - mSourceHeight);
        return new Point(Math.round(left), Math.round(top));
    }

    public static final class Builder
    {
        private final View mView;
        private int mWidth;
        private int mHeight;
        private float mZoom = 1.25f;
        private float mElevation;
        private float mCornerRadius;
        private Drawable mOverlay;
        private int mHorizontalOffset;
        private int mVerticalOffset;
        private boolean mClippingEnabled = true;
        private int mLeftBound = SOURCE_BOUND_MAX_VISIBLE;
        private int mTopBound = SOURCE_BOUND_MAX_IN_SURFACE;
        private int mRightBound = SOURCE_BOUND_MAX_VISIBLE;
        private int mBottomBound = SOURCE_BOUND_MAX_IN_SURFACE;

        public Builder(View view)
        {
            if (view == null) throw new NullPointerException("view");

            mView = view;

            android.util.DisplayMetrics metrics = view.getResources().getDisplayMetrics();
            mWidth = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, metrics));
            mHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, metrics));
            mElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, metrics);
            mCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics);
            mVerticalOffset = -Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, metrics));
        }

        public Builder setSize(int width, int height)
        {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("The size must be positive");

            mWidth = width;
            mHeight = height;
            return this;
        }

        public Builder setInitialZoom(float zoom)
        {
            if (zoom <= 0) throw new IllegalArgumentException("Zoom should be positive");

            mZoom = zoom;
            return this;
        }

        public Builder setElevation(float elevation)
        {
            if (elevation < 0) throw new IllegalArgumentException("Elevation should not be negative");

            mElevation = elevation;
            return this;
        }

        public Builder setCornerRadius(float cornerRadius)
        {
            if (cornerRadius < 0) throw new IllegalArgumentException("Corner radius should not be negative");

            mCornerRadius = cornerRadius;
            return this;
        }

        public Builder setOverlay(Drawable overlay)
        {
            mOverlay = overlay;
            return this;
        }

        public Builder setDefaultSourceToMagnifierOffset(int horizontalOffset, int verticalOffset)
        {
            mHorizontalOffset = horizontalOffset;
            mVerticalOffset = verticalOffset;
            return this;
        }

        public Builder setClippingEnabled(boolean clip)
        {
            mClippingEnabled = clip;
            return this;
        }

        public Builder setSourceBounds(int left, int top, int right, int bottom)
        {
            mLeftBound = left;
            mTopBound = top;
            mRightBound = right;
            mBottomBound = bottom;
            return this;
        }

        public Magnifier build()
        {
            return new Magnifier(this);
        }
    }
}
