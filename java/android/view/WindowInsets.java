package android.view;

import android.graphics.Insets;

public final class WindowInsets
{
    private static final WindowInsets EMPTY = new WindowInsets(Insets.NONE);

    public static final WindowInsets CONSUMED = EMPTY;

    private final Insets mSystemWindowInsets;

    private WindowInsets(Insets systemWindowInsets)
    {
        mSystemWindowInsets = systemWindowInsets == null ? Insets.NONE : systemWindowInsets;
    }

    public static WindowInsets empty()
    {
        return EMPTY;
    }

    public Insets getInsets(int typeMask)
    {
        return mSystemWindowInsets;
    }

    public Insets getInsetsIgnoringVisibility(int typeMask)
    {
        return mSystemWindowInsets;
    }

    public Insets getSystemWindowInsets()
    {
        return mSystemWindowInsets;
    }

    public int getSystemWindowInsetLeft()
    {
        return mSystemWindowInsets.left;
    }

    public int getSystemWindowInsetTop()
    {
        return mSystemWindowInsets.top;
    }

    public int getSystemWindowInsetRight()
    {
        return mSystemWindowInsets.right;
    }

    public int getSystemWindowInsetBottom()
    {
        return mSystemWindowInsets.bottom;
    }

    public boolean isConsumed()
    {
        return false;
    }

    public boolean isVisible(int typeMask)
    {
        return (typeMask & Type.ime()) == 0;
    }

    public WindowInsets consumeSystemWindowInsets()
    {
        return EMPTY;
    }

    public WindowInsets consumeStableInsets()
    {
        return EMPTY;
    }

    public WindowInsets consumeDisplayCutout()
    {
        return EMPTY;
    }

    public DisplayCutout getDisplayCutout()
    {
        return null;
    }

    public Insets getStableInsets()
    {
        return Insets.NONE;
    }

    public int getStableInsetLeft()
    {
        return 0;
    }

    public int getStableInsetTop()
    {
        return 0;
    }

    public int getStableInsetRight()
    {
        return 0;
    }

    public int getStableInsetBottom()
    {
        return 0;
    }

    public Insets getSystemGestureInsets()
    {
        return mSystemWindowInsets;
    }

    public Insets getMandatorySystemGestureInsets()
    {
        return mSystemWindowInsets;
    }

    public Insets getTappableElementInsets()
    {
        return mSystemWindowInsets;
    }

    public boolean hasInsets()
    {
        return !Insets.NONE.equals(mSystemWindowInsets);
    }

    public boolean hasSystemWindowInsets()
    {
        return !Insets.NONE.equals(mSystemWindowInsets);
    }

    public boolean hasStableInsets()
    {
        return false;
    }

    public boolean isRound()
    {
        return false;
    }

    public WindowInsets inset(int left, int top, int right, int bottom)
    {
        return this;
    }

    public WindowInsets inset(Insets insets)
    {
        return this;
    }

    public WindowInsets replaceSystemWindowInsets(int left, int top, int right, int bottom)
    {
        return new WindowInsets(Insets.of(left, top, right, bottom));
    }

    public WindowInsets replaceSystemWindowInsets(android.graphics.Rect systemInsets)
    {
        if (systemInsets == null) return this;
        return new WindowInsets(Insets.of(systemInsets.left, systemInsets.top, systemInsets.right, systemInsets.bottom));
    }

    public static final class Builder
    {
        private Insets mInsets = Insets.NONE;

        public Builder()
        {
        }

        public Builder(WindowInsets insets)
        {
            if (insets != null) mInsets = insets.mSystemWindowInsets;
        }

        public Builder setInsets(int typeMask, Insets insets)
        {
            mInsets = insets == null ? Insets.NONE : insets;
            return this;
        }

        public Builder setSystemWindowInsets(Insets insets)
        {
            return setInsets(0, insets);
        }

        public WindowInsets build()
        {
            return new WindowInsets(mInsets);
        }
    }

    public static final class Type
    {
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

        public static int systemBars()
        {
            return statusBars() | navigationBars() | captionBar();
        }

        private Type()
        {
        }
    }
}
