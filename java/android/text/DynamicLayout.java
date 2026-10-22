package android.text;

import java.lang.ref.WeakReference;

public class DynamicLayout extends Layout
{
    private final CharSequence mBase;
    private final CharSequence mDisplay;
    private final TextPaint mTextPaint;
    private final int mLayoutWidth;
    private final Alignment mLayoutAlignment;
    private final TextDirectionHeuristic mTextDirection;
    private final float mLayoutSpacingMultiplier;
    private final float mLayoutSpacingAdd;
    private final boolean mIncludePad;
    private final boolean mFallbackLineSpacing;
    private final TextUtils.TruncateAt mEllipsizeAt;
    private final int mEllipsizedWidth;
    private final int mBreakStrategy;
    private final int mHyphenationFrequency;
    private final int mJustificationMode;
    private final ChangeWatcher mWatcher;

    private StaticLayout mLayout;
    private boolean mDirty = true;
    private String mSnapshot;

    public DynamicLayout(CharSequence base, TextPaint paint, int width, Alignment alignment, float spacingMultiplier, float spacingAdd,
                         boolean includePad)
    {
        this(base, base, paint, width, alignment, spacingMultiplier, spacingAdd, includePad);
    }

    public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment alignment,
                         float spacingMultiplier, float spacingAdd, boolean includePad)
    {
        this(base, display, paint, width, alignment, spacingMultiplier, spacingAdd, includePad, null, 0);
    }

    public DynamicLayout(CharSequence base, CharSequence display, TextPaint paint, int width, Alignment alignment,
                         float spacingMultiplier, float spacingAdd, boolean includePad, TextUtils.TruncateAt ellipsize,
                         int ellipsizedWidth)
    {
        this(Builder.obtain(base, paint, width).setDisplayText(display).setAlignment(alignment)
                 .setLineSpacing(spacingAdd, spacingMultiplier).setIncludePad(includePad).setEllipsize(ellipsize)
                 .setEllipsizedWidth(ellipsizedWidth));
    }

    private DynamicLayout(Builder builder)
    {
        super(builder.mDisplay, builder.mPaint, builder.mWidth, builder.mAlignment, builder.mSpacingMultiplier, builder.mSpacingAdd);

        mBase = builder.mBase;
        mDisplay = builder.mDisplay;
        mTextPaint = builder.mPaint;
        mLayoutWidth = builder.mWidth;
        mLayoutAlignment = builder.mAlignment;
        mTextDirection = builder.mTextDirection;
        mLayoutSpacingMultiplier = builder.mSpacingMultiplier;
        mLayoutSpacingAdd = builder.mSpacingAdd;
        mIncludePad = builder.mIncludePad;
        mFallbackLineSpacing = builder.mFallbackLineSpacing;
        mEllipsizeAt = builder.mEllipsize;
        mEllipsizedWidth = builder.mEllipsize == null ? builder.mWidth : builder.mEllipsizedWidth;
        mBreakStrategy = builder.mBreakStrategy;
        mHyphenationFrequency = builder.mHyphenationFrequency;
        mJustificationMode = builder.mJustificationMode;

        if (mBase instanceof Spannable)
        {
            mWatcher = new ChangeWatcher(this);
            Spannable spannable = (Spannable)mBase;

            ChangeWatcher[] existing = spannable.getSpans(0, spannable.length(), ChangeWatcher.class);
            for (ChangeWatcher watcher : existing)
            {
                if (watcher.mLayout.get() == null) spannable.removeSpan(watcher);
            }

            spannable.setSpan(mWatcher, 0, mBase.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE | (100 << Spanned.SPAN_PRIORITY_SHIFT));
        }
        else
        {
            mWatcher = null;
        }
    }

    private StaticLayout layout()
    {
        if (!mDirty && mWatcher == null && mSnapshot != null && !mSnapshot.contentEquals(mDisplay)) mDirty = true;

        if (mDirty || mLayout == null)
        {
            mLayout = StaticLayout.Builder.obtain(mDisplay, 0, mDisplay.length(), mTextPaint, mLayoutWidth)
                .setAlignment(mLayoutAlignment).setTextDirection(mTextDirection)
                .setLineSpacing(mLayoutSpacingAdd, mLayoutSpacingMultiplier).setIncludePad(mIncludePad)
                .setUseLineSpacingFromFallbacks(mFallbackLineSpacing).setEllipsize(mEllipsizeAt)
                .setEllipsizedWidth(mEllipsizedWidth).setBreakStrategy(mBreakStrategy)
                .setHyphenationFrequency(mHyphenationFrequency).setJustificationMode(mJustificationMode).build();

            mDirty = false;
            if (mWatcher == null) mSnapshot = mDisplay.toString();
        }

        return mLayout;
    }

    void invalidate()
    {
        mDirty = true;
    }

    @Override
    public int getLineCount()
    {
        return layout().getLineCount();
    }

    @Override
    public int getLineTop(int line)
    {
        return layout().getLineTop(line);
    }

    @Override
    public int getLineDescent(int line)
    {
        return layout().getLineDescent(line);
    }

    @Override
    public int getLineStart(int line)
    {
        return layout().getLineStart(line);
    }

    @Override
    public boolean getLineContainsTab(int line)
    {
        return layout().getLineContainsTab(line);
    }

    @Override
    public int getParagraphDirection(int line)
    {
        return layout().getParagraphDirection(line);
    }

    @Override
    public final Directions getLineDirections(int line)
    {
        return layout().getLineDirections(line);
    }

    @Override
    public int getTopPadding()
    {
        return layout().getTopPadding();
    }

    @Override
    public int getBottomPadding()
    {
        return layout().getBottomPadding();
    }

    @Override
    public int getEllipsizedWidth()
    {
        return mEllipsizedWidth;
    }

    @Override
    public int getEllipsisStart(int line)
    {
        return layout().getEllipsisStart(line);
    }

    @Override
    public int getEllipsisCount(int line)
    {
        return layout().getEllipsisCount(line);
    }

    private static final class ChangeWatcher implements TextWatcher, SpanWatcher
    {
        private final WeakReference<DynamicLayout> mLayout;

        ChangeWatcher(DynamicLayout layout)
        {
            mLayout = new WeakReference<>(layout);
        }

        private void reflow()
        {
            DynamicLayout layout = mLayout.get();
            if (layout != null) layout.invalidate();
        }

        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after)
        {
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count)
        {
            reflow();
        }

        @Override
        public void afterTextChanged(Editable text)
        {
        }

        @Override
        public void onSpanAdded(Spannable text, Object what, int start, int end)
        {
            if (what instanceof android.text.style.UpdateLayout) reflow();
        }

        @Override
        public void onSpanRemoved(Spannable text, Object what, int start, int end)
        {
            if (what instanceof android.text.style.UpdateLayout) reflow();
        }

        @Override
        public void onSpanChanged(Spannable text, Object what, int start, int end, int oldStart, int oldEnd)
        {
            if (what instanceof android.text.style.UpdateLayout) reflow();
        }
    }

    public static final class Builder
    {
        private CharSequence mBase;
        private CharSequence mDisplay;
        private TextPaint mPaint;
        private int mWidth;
        private Alignment mAlignment = Alignment.ALIGN_NORMAL;
        private TextDirectionHeuristic mTextDirection = TextDirectionHeuristics.FIRSTSTRONG_LTR;
        private float mSpacingMultiplier = DEFAULT_LINESPACING_MULTIPLIER;
        private float mSpacingAdd = DEFAULT_LINESPACING_ADDITION;
        private boolean mIncludePad = true;
        private boolean mFallbackLineSpacing;
        private TextUtils.TruncateAt mEllipsize;
        private int mEllipsizedWidth;
        private int mBreakStrategy;
        private int mHyphenationFrequency;
        private int mJustificationMode;

        private Builder()
        {
        }

        public static Builder obtain(CharSequence base, TextPaint paint, int width)
        {
            Builder builder = new Builder();
            builder.mBase = base;
            builder.mDisplay = base;
            builder.mPaint = paint;
            builder.mWidth = width;
            builder.mEllipsizedWidth = width;
            return builder;
        }

        public Builder setDisplayText(CharSequence display)
        {
            mDisplay = display;
            return this;
        }

        public Builder setAlignment(Alignment alignment)
        {
            mAlignment = alignment;
            return this;
        }

        public Builder setTextDirection(TextDirectionHeuristic textDirection)
        {
            mTextDirection = textDirection;
            return this;
        }

        public Builder setLineSpacing(float spacingAdd, float spacingMultiplier)
        {
            mSpacingAdd = spacingAdd;
            mSpacingMultiplier = spacingMultiplier;
            return this;
        }

        public Builder setIncludePad(boolean includePad)
        {
            mIncludePad = includePad;
            return this;
        }

        public Builder setUseLineSpacingFromFallbacks(boolean useLineSpacingFromFallbacks)
        {
            mFallbackLineSpacing = useLineSpacingFromFallbacks;
            return this;
        }

        public Builder setEllipsizedWidth(int ellipsizedWidth)
        {
            mEllipsizedWidth = ellipsizedWidth;
            return this;
        }

        public Builder setEllipsize(TextUtils.TruncateAt ellipsize)
        {
            mEllipsize = ellipsize;
            return this;
        }

        public Builder setBreakStrategy(int breakStrategy)
        {
            mBreakStrategy = breakStrategy;
            return this;
        }

        public Builder setHyphenationFrequency(int hyphenationFrequency)
        {
            mHyphenationFrequency = hyphenationFrequency;
            return this;
        }

        public Builder setJustificationMode(int justificationMode)
        {
            mJustificationMode = justificationMode;
            return this;
        }

        public DynamicLayout build()
        {
            if (mDisplay == null) mDisplay = mBase == null ? "" : mBase;
            if (mBase == null) mBase = mDisplay;

            return new DynamicLayout(this);
        }
    }
}
