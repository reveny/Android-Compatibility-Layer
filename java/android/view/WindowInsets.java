package android.view;

import android.graphics.Insets;
import android.graphics.Rect;
import android.util.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class WindowInsets
{
    private static final int TYPE_COUNT = 9;

    private static final WindowInsets EMPTY = new Builder().build();

    public static final WindowInsets CONSUMED = new WindowInsets(null, null, null, false, null, null, null, 0, 0, null, null, true, true, true);

    private final Insets[] mTypeInsets;
    private final Insets[] mTypeMaxInsets;
    private final boolean[] mTypeVisibility;
    private final boolean mIsRound;
    private final DisplayCutout mDisplayCutout;
    private final RoundedCorner[] mRoundedCorners;
    private final Rect mPrivacyIndicatorBounds;
    private final int mFrameWidth;
    private final int mFrameHeight;
    private final List<Rect>[] mTypeBoundingRects;
    private final List<Rect>[] mTypeMaxBoundingRects;
    private final boolean mSystemWindowInsetsConsumed;
    private final boolean mStableInsetsConsumed;
    private final boolean mDisplayCutoutConsumed;
    private DisplayShape mDisplayShape;

    private WindowInsets(Insets[] typeInsets, Insets[] typeMaxInsets, boolean[] typeVisibility, boolean isRound, DisplayCutout displayCutout,
                         RoundedCorner[] roundedCorners, Rect privacyIndicatorBounds, int frameWidth, int frameHeight,
                         List<Rect>[] typeBoundingRects, List<Rect>[] typeMaxBoundingRects, boolean systemWindowInsetsConsumed,
                         boolean stableInsetsConsumed, boolean displayCutoutConsumed)
    {
        mSystemWindowInsetsConsumed = systemWindowInsetsConsumed;
        mStableInsetsConsumed = stableInsetsConsumed;
        mDisplayCutoutConsumed = displayCutoutConsumed;

        mTypeInsets = systemWindowInsetsConsumed ? filledInsets() : copyInsets(typeInsets);
        mTypeMaxInsets = stableInsetsConsumed ? filledInsets() : copyInsets(typeMaxInsets);
        mTypeVisibility = typeVisibility == null ? new boolean[TYPE_COUNT] : typeVisibility.clone();
        mIsRound = isRound;
        mDisplayCutout = displayCutoutConsumed ? null : displayCutout;
        mRoundedCorners = roundedCorners == null ? new RoundedCorner[4] : roundedCorners.clone();
        mPrivacyIndicatorBounds = privacyIndicatorBounds == null ? null : new Rect(privacyIndicatorBounds);
        mFrameWidth = frameWidth;
        mFrameHeight = frameHeight;
        mTypeBoundingRects = copyRects(typeBoundingRects);
        mTypeMaxBoundingRects = copyRects(typeMaxBoundingRects);
    }

    public WindowInsets(WindowInsets source)
    {
        this(source.mTypeInsets, source.mTypeMaxInsets, source.mTypeVisibility, source.mIsRound, source.mDisplayCutout,
             source.mRoundedCorners, source.mPrivacyIndicatorBounds, source.mFrameWidth, source.mFrameHeight, source.mTypeBoundingRects,
             source.mTypeMaxBoundingRects, source.mSystemWindowInsetsConsumed, source.mStableInsetsConsumed, source.mDisplayCutoutConsumed);
        mDisplayShape = source.mDisplayShape;
    }

    private static Insets[] filledInsets()
    {
        Insets[] insets = new Insets[TYPE_COUNT];
        Arrays.fill(insets, Insets.NONE);
        return insets;
    }

    private static Insets[] copyInsets(Insets[] source)
    {
        Insets[] insets = filledInsets();
        if (source == null) return insets;

        for (int index = 0; index < TYPE_COUNT; index++)
        {
            if (source[index] != null) insets[index] = source[index];
        }

        return insets;
    }

    @SuppressWarnings("unchecked")
    private static List<Rect>[] copyRects(List<Rect>[] source)
    {
        List<Rect>[] rects = new List[TYPE_COUNT];

        for (int index = 0; index < TYPE_COUNT; index++)
        {
            ArrayList<Rect> copy = new ArrayList<>();
            if (source != null && source[index] != null)
            {
                for (Rect rect : source[index])
                {
                    if (rect != null) copy.add(new Rect(rect));
                }
            }
            rects[index] = copy;
        }

        return rects;
    }

    public static WindowInsets empty()
    {
        return EMPTY;
    }

    private static Insets union(Insets[] insets, int typeMask)
    {
        Insets result = Insets.NONE;

        for (int index = 0; index < TYPE_COUNT; index++)
        {
            if ((typeMask & (1 << index)) == 0) continue;
            result = Insets.max(result, insets[index]);
        }

        return result;
    }

    private static int compatTypes()
    {
        return Type.systemBars() | Type.ime() | Type.displayCutout();
    }

    public Insets getInsets(int typeMask)
    {
        return union(mTypeInsets, typeMask);
    }

    public Insets getInsetsIgnoringVisibility(int typeMask)
    {
        if ((typeMask & Type.ime()) != 0) throw new IllegalArgumentException("Unable to query the maximum insets for IME");

        return union(mTypeMaxInsets, typeMask);
    }

    public boolean isVisible(int typeMask)
    {
        for (int index = 0; index < TYPE_COUNT; index++)
        {
            if ((typeMask & (1 << index)) != 0 && !mTypeVisibility[index]) return false;
        }

        return true;
    }

    public List<Rect> getBoundingRects(int typeMask)
    {
        return collectRects(mTypeBoundingRects, typeMask);
    }

    public List<Rect> getBoundingRectsIgnoringVisibility(int typeMask)
    {
        if ((typeMask & Type.ime()) != 0) throw new IllegalArgumentException("Unable to query the bounding rects for IME");

        return collectRects(mTypeMaxBoundingRects, typeMask);
    }

    private static List<Rect> collectRects(List<Rect>[] rects, int typeMask)
    {
        ArrayList<Rect> result = new ArrayList<>();

        for (int index = 0; index < TYPE_COUNT; index++)
        {
            if ((typeMask & (1 << index)) == 0) continue;

            for (Rect rect : rects[index])
            {
                if (!result.contains(rect)) result.add(new Rect(rect));
            }
        }

        return Collections.unmodifiableList(result);
    }

    public Size getFrame()
    {
        return new Size(mFrameWidth, mFrameHeight);
    }

    public Insets getSystemWindowInsets()
    {
        return getInsets(compatTypes());
    }

    public int getSystemWindowInsetLeft()
    {
        return getSystemWindowInsets().left;
    }

    public int getSystemWindowInsetTop()
    {
        return getSystemWindowInsets().top;
    }

    public int getSystemWindowInsetRight()
    {
        return getSystemWindowInsets().right;
    }

    public int getSystemWindowInsetBottom()
    {
        return getSystemWindowInsets().bottom;
    }

    public boolean hasSystemWindowInsets()
    {
        return !Insets.NONE.equals(getSystemWindowInsets());
    }

    public boolean hasInsets()
    {
        for (int index = 0; index < TYPE_COUNT; index++)
        {
            if (!Insets.NONE.equals(mTypeInsets[index])) return true;
        }

        return mDisplayCutout != null || hasRoundedCorners();
    }

    private boolean hasRoundedCorners()
    {
        for (RoundedCorner corner : mRoundedCorners)
        {
            if (corner != null) return true;
        }

        return false;
    }

    public DisplayCutout getDisplayCutout()
    {
        return mDisplayCutout;
    }

    public RoundedCorner getRoundedCorner(int position)
    {
        if (position < RoundedCorner.POSITION_TOP_LEFT || position > RoundedCorner.POSITION_BOTTOM_LEFT) return null;

        return mRoundedCorners[position];
    }

    public Rect getPrivacyIndicatorBounds()
    {
        return mPrivacyIndicatorBounds == null ? null : new Rect(mPrivacyIndicatorBounds);
    }

    public WindowInsets consumeDisplayCutout()
    {
        return new WindowInsets(mTypeInsets, mTypeMaxInsets, mTypeVisibility, mIsRound, null, mRoundedCorners, mPrivacyIndicatorBounds,
                                mFrameWidth, mFrameHeight, mTypeBoundingRects, mTypeMaxBoundingRects, mSystemWindowInsetsConsumed,
                                mStableInsetsConsumed, true).shaped(mDisplayShape);
    }

    public boolean isConsumed()
    {
        return mSystemWindowInsetsConsumed && mStableInsetsConsumed && mDisplayCutoutConsumed;
    }

    public boolean isRound()
    {
        return mIsRound;
    }

    public WindowInsets consumeSystemWindowInsets()
    {
        return new WindowInsets(null, mTypeMaxInsets, mTypeVisibility, mIsRound, mDisplayCutout, mRoundedCorners, mPrivacyIndicatorBounds,
                                mFrameWidth, mFrameHeight, mTypeBoundingRects, mTypeMaxBoundingRects, true, mStableInsetsConsumed,
                                mDisplayCutoutConsumed).shaped(mDisplayShape);
    }

    public WindowInsets consumeStableInsets()
    {
        return new WindowInsets(mTypeInsets, null, mTypeVisibility, mIsRound, mDisplayCutout, mRoundedCorners, mPrivacyIndicatorBounds,
                                mFrameWidth, mFrameHeight, mTypeBoundingRects, mTypeMaxBoundingRects, mSystemWindowInsetsConsumed, true,
                                mDisplayCutoutConsumed).shaped(mDisplayShape);
    }

    public WindowInsets replaceSystemWindowInsets(int left, int top, int right, int bottom)
    {
        if (left == getSystemWindowInsetLeft() && top == getSystemWindowInsetTop() && right == getSystemWindowInsetRight()
            && bottom == getSystemWindowInsetBottom())
        {
            return this;
        }

        return new Builder(this).setSystemWindowInsets(Insets.of(left, top, right, bottom)).build();
    }

    public WindowInsets replaceSystemWindowInsets(Rect systemWindowInsets)
    {
        if (systemWindowInsets == null) return this;

        return replaceSystemWindowInsets(systemWindowInsets.left, systemWindowInsets.top, systemWindowInsets.right,
                                         systemWindowInsets.bottom);
    }

    public Insets getStableInsets()
    {
        return getInsetsIgnoringVisibility(Type.systemBars());
    }

    public int getStableInsetTop()
    {
        return getStableInsets().top;
    }

    public int getStableInsetLeft()
    {
        return getStableInsets().left;
    }

    public int getStableInsetRight()
    {
        return getStableInsets().right;
    }

    public int getStableInsetBottom()
    {
        return getStableInsets().bottom;
    }

    public boolean hasStableInsets()
    {
        return !Insets.NONE.equals(getStableInsets());
    }

    public Insets getSystemGestureInsets()
    {
        return getInsets(Type.systemGestures());
    }

    public Insets getMandatorySystemGestureInsets()
    {
        return getInsets(Type.mandatorySystemGestures());
    }

    public Insets getTappableElementInsets()
    {
        return getInsets(Type.tappableElement());
    }

    public WindowInsets inset(Insets insets)
    {
        if (insets == null) throw new NullPointerException("insets");

        return inset(insets.left, insets.top, insets.right, insets.bottom);
    }

    public WindowInsets inset(int left, int top, int right, int bottom)
    {
        if (left < 0 || top < 0 || right < 0 || bottom < 0) throw new IllegalArgumentException("insets must be non-negative");

        Insets[] typeInsets = new Insets[TYPE_COUNT];
        Insets[] typeMaxInsets = new Insets[TYPE_COUNT];

        for (int index = 0; index < TYPE_COUNT; index++)
        {
            typeInsets[index] = insetInsets(mTypeInsets[index], left, top, right, bottom);
            typeMaxInsets[index] = insetInsets(mTypeMaxInsets[index], left, top, right, bottom);
        }

        Rect privacy = null;
        if (mPrivacyIndicatorBounds != null)
        {
            privacy = new Rect(mPrivacyIndicatorBounds);
            privacy.offset(-left, -top);
        }

        int frameWidth = Math.max(0, mFrameWidth - left - right);
        int frameHeight = Math.max(0, mFrameHeight - top - bottom);

        return new WindowInsets(mSystemWindowInsetsConsumed ? null : typeInsets, mStableInsetsConsumed ? null : typeMaxInsets,
                                mTypeVisibility, mIsRound, mDisplayCutoutConsumed ? null : mDisplayCutout, mRoundedCorners, privacy,
                                frameWidth, frameHeight, mTypeBoundingRects, mTypeMaxBoundingRects, mSystemWindowInsetsConsumed,
                                mStableInsetsConsumed, mDisplayCutoutConsumed)
                   .shaped(mDisplayShape == null ? null : mDisplayShape.offset(-left, -top));
    }

    private WindowInsets shaped(DisplayShape shape)
    {
        mDisplayShape = shape;
        return this;
    }

    public DisplayShape getDisplayShape()
    {
        return mDisplayShape;
    }

    private static Insets insetInsets(Insets insets, int left, int top, int right, int bottom)
    {
        return Insets.of(Math.max(0, insets.left - left), Math.max(0, insets.top - top), Math.max(0, insets.right - right),
                         Math.max(0, insets.bottom - bottom));
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof WindowInsets)) return false;

        WindowInsets that = (WindowInsets)other;
        return mIsRound == that.mIsRound && mSystemWindowInsetsConsumed == that.mSystemWindowInsetsConsumed
               && mStableInsetsConsumed == that.mStableInsetsConsumed && mDisplayCutoutConsumed == that.mDisplayCutoutConsumed
               && Arrays.equals(mTypeInsets, that.mTypeInsets) && Arrays.equals(mTypeMaxInsets, that.mTypeMaxInsets)
               && Arrays.equals(mTypeVisibility, that.mTypeVisibility) && Objects.equals(mDisplayCutout, that.mDisplayCutout)
               && Arrays.equals(mRoundedCorners, that.mRoundedCorners) && Objects.equals(mPrivacyIndicatorBounds, that.mPrivacyIndicatorBounds)
               && mFrameWidth == that.mFrameWidth && mFrameHeight == that.mFrameHeight
               && Arrays.equals(mTypeBoundingRects, that.mTypeBoundingRects) && Arrays.equals(mTypeMaxBoundingRects, that.mTypeMaxBoundingRects);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(Arrays.hashCode(mTypeInsets), Arrays.hashCode(mTypeMaxInsets), Arrays.hashCode(mTypeVisibility), mIsRound,
                            mDisplayCutout, Arrays.hashCode(mRoundedCorners), mPrivacyIndicatorBounds, mFrameWidth, mFrameHeight,
                            mSystemWindowInsetsConsumed, mStableInsetsConsumed, mDisplayCutoutConsumed);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("WindowInsets{");

        for (int index = 0; index < TYPE_COUNT; index++)
        {
            if (Insets.NONE.equals(mTypeInsets[index])) continue;
            builder.append(Type.toString(1 << index)).append('=').append(mTypeInsets[index]).append(' ');
        }

        if (mDisplayCutout != null) builder.append(mDisplayCutout).append(' ');
        if (mPrivacyIndicatorBounds != null) builder.append("privacyIndicatorBounds=").append(mPrivacyIndicatorBounds).append(' ');
        if (mIsRound) builder.append("round ");
        if (isConsumed()) builder.append("consumed ");

        return builder.append('}').toString();
    }

    public static final class Builder
    {
        private final Insets[] mTypeInsets;
        private final Insets[] mTypeMaxInsets;
        private final boolean[] mTypeVisibility;
        private boolean mSystemInsetsConsumed = true;
        private boolean mStableInsetsConsumed = true;
        private DisplayCutout mDisplayCutout;
        private final RoundedCorner[] mRoundedCorners;
        private Rect mPrivacyIndicatorBounds;
        private boolean mIsRound;
        private int mFrameWidth;
        private int mFrameHeight;
        private final List<Rect>[] mTypeBoundingRects;
        private final List<Rect>[] mTypeMaxBoundingRects;
        private DisplayShape mDisplayShape;

        public Builder()
        {
            mTypeInsets = filledInsets();
            mTypeMaxInsets = filledInsets();
            mTypeVisibility = new boolean[TYPE_COUNT];
            mRoundedCorners = new RoundedCorner[4];
            mTypeBoundingRects = copyRects(null);
            mTypeMaxBoundingRects = copyRects(null);
        }

        public Builder(WindowInsets insets)
        {
            mTypeInsets = insets.mTypeInsets.clone();
            mTypeMaxInsets = insets.mTypeMaxInsets.clone();
            mTypeVisibility = insets.mTypeVisibility.clone();
            mSystemInsetsConsumed = insets.mSystemWindowInsetsConsumed;
            mStableInsetsConsumed = insets.mStableInsetsConsumed;
            mDisplayCutout = insets.mDisplayCutout;
            mRoundedCorners = insets.mRoundedCorners.clone();
            mPrivacyIndicatorBounds = insets.mPrivacyIndicatorBounds;
            mIsRound = insets.mIsRound;
            mFrameWidth = insets.mFrameWidth;
            mFrameHeight = insets.mFrameHeight;
            mTypeBoundingRects = copyRects(insets.mTypeBoundingRects);
            mTypeMaxBoundingRects = copyRects(insets.mTypeMaxBoundingRects);
            mDisplayShape = insets.mDisplayShape;
        }

        @Deprecated
        public Builder setSystemWindowInsets(Insets systemWindowInsets)
        {
            if (systemWindowInsets == null) throw new NullPointerException("systemWindowInsets");

            Arrays.fill(mTypeInsets, Insets.NONE);
            mTypeInsets[0] = systemWindowInsets;
            mSystemInsetsConsumed = false;
            return this;
        }

        @Deprecated
        public Builder setSystemGestureInsets(Insets insets)
        {
            return setInsets(Type.systemGestures(), insets);
        }

        @Deprecated
        public Builder setMandatorySystemGestureInsets(Insets insets)
        {
            return setInsets(Type.mandatorySystemGestures(), insets);
        }

        @Deprecated
        public Builder setTappableElementInsets(Insets insets)
        {
            return setInsets(Type.tappableElement(), insets);
        }

        public Builder setInsets(int typeMask, Insets insets)
        {
            if (insets == null) throw new NullPointerException("insets");

            for (int index = 0; index < TYPE_COUNT; index++)
            {
                if ((typeMask & (1 << index)) != 0) mTypeInsets[index] = insets;
            }

            mSystemInsetsConsumed = false;
            return this;
        }

        public Builder setInsetsIgnoringVisibility(int typeMask, Insets insets)
        {
            if (insets == null) throw new NullPointerException("insets");
            if ((typeMask & Type.ime()) != 0) throw new IllegalArgumentException("Maximum inset not available for IME");

            for (int index = 0; index < TYPE_COUNT; index++)
            {
                if ((typeMask & (1 << index)) != 0) mTypeMaxInsets[index] = insets;
            }

            mStableInsetsConsumed = false;
            return this;
        }

        public Builder setVisible(int typeMask, boolean visible)
        {
            for (int index = 0; index < TYPE_COUNT; index++)
            {
                if ((typeMask & (1 << index)) != 0) mTypeVisibility[index] = visible;
            }

            return this;
        }

        @Deprecated
        public Builder setStableInsets(Insets stableInsets)
        {
            if (stableInsets == null) throw new NullPointerException("stableInsets");

            Arrays.fill(mTypeMaxInsets, Insets.NONE);
            mTypeMaxInsets[0] = stableInsets;
            mStableInsetsConsumed = false;
            return this;
        }

        public Builder setDisplayCutout(DisplayCutout displayCutout)
        {
            mDisplayCutout = displayCutout;
            return this;
        }

        public Builder setRoundedCorner(int position, RoundedCorner roundedCorner)
        {
            if (position < RoundedCorner.POSITION_TOP_LEFT || position > RoundedCorner.POSITION_BOTTOM_LEFT) return this;

            mRoundedCorners[position] = roundedCorner;
            return this;
        }

        public Builder setPrivacyIndicatorBounds(Rect bounds)
        {
            mPrivacyIndicatorBounds = bounds == null ? null : new Rect(bounds);
            return this;
        }

        public Builder setBoundingRects(int typeMask, List<Rect> rects)
        {
            for (int index = 0; index < TYPE_COUNT; index++)
            {
                if ((typeMask & (1 << index)) == 0) continue;
                mTypeBoundingRects[index] = rects == null ? new ArrayList<>() : new ArrayList<>(rects);
            }

            return this;
        }

        public Builder setBoundingRectsIgnoringVisibility(int typeMask, List<Rect> rects)
        {
            if ((typeMask & Type.ime()) != 0) throw new IllegalArgumentException("Maximum bounding rects not available for IME");

            for (int index = 0; index < TYPE_COUNT; index++)
            {
                if ((typeMask & (1 << index)) == 0) continue;
                mTypeMaxBoundingRects[index] = rects == null ? new ArrayList<>() : new ArrayList<>(rects);
            }

            return this;
        }

        public Builder setFrame(int width, int height)
        {
            mFrameWidth = width;
            mFrameHeight = height;
            return this;
        }

        public Builder setRound(boolean round)
        {
            mIsRound = round;
            return this;
        }

        public WindowInsets build()
        {
            return new WindowInsets(mSystemInsetsConsumed ? null : mTypeInsets, mStableInsetsConsumed ? null : mTypeMaxInsets,
                                    mTypeVisibility, mIsRound, mDisplayCutout, mRoundedCorners, mPrivacyIndicatorBounds, mFrameWidth,
                                    mFrameHeight, mTypeBoundingRects, mTypeMaxBoundingRects, mSystemInsetsConsumed, mStableInsetsConsumed,
                                    mDisplayCutout == null).shaped(mDisplayShape);
        }

        public Builder setDisplayShape(DisplayShape displayShape)
        {
            mDisplayShape = displayShape;
            return this;
        }
    }

    public static final class Type
    {
        static String toString(int type)
        {
            switch (type)
            {
                case 1:
                    return "statusBars";
                case 1 << 1:
                    return "navigationBars";
                case 1 << 2:
                    return "captionBar";
                case 1 << 3:
                    return "ime";
                case 1 << 4:
                    return "systemGestures";
                case 1 << 5:
                    return "mandatorySystemGestures";
                case 1 << 6:
                    return "tappableElement";
                case 1 << 7:
                    return "displayCutout";
                default:
                    return "systemOverlays";
            }
        }

        public static int statusBars()
        {
            return 1;
        }

        public static int navigationBars()
        {
            return 1 << 1;
        }

        public static int captionBar()
        {
            return 1 << 2;
        }

        public static int ime()
        {
            return 1 << 3;
        }

        public static int systemGestures()
        {
            return 1 << 4;
        }

        public static int mandatorySystemGestures()
        {
            return 1 << 5;
        }

        public static int tappableElement()
        {
            return 1 << 6;
        }

        public static int displayCutout()
        {
            return 1 << 7;
        }

        public static int systemOverlays()
        {
            return 1 << 8;
        }

        public static int systemBars()
        {
            return statusBars() | navigationBars() | captionBar() | systemOverlays();
        }

        static int all()
        {
            return (1 << TYPE_COUNT) - 1;
        }

        private Type()
        {
        }
    }

    public static final class Side
    {
        public static final int LEFT = 1;
        public static final int TOP = 1 << 1;
        public static final int RIGHT = 1 << 2;
        public static final int BOTTOM = 1 << 3;

        public static int all()
        {
            return LEFT | TOP | RIGHT | BOTTOM;
        }

        private Side()
        {
        }
    }
}
