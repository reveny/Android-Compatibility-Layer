package android.view.autofill;

import java.util.Arrays;

public final class VirtualViewFillInfo
{
    private final String[] mAutofillHints;

    private VirtualViewFillInfo(String[] autofillHints)
    {
        mAutofillHints = autofillHints;
    }

    public String[] getAutofillHints()
    {
        return mAutofillHints;
    }

    @Override
    public String toString()
    {
        return "VirtualViewFillInfo { autofillHints = " + Arrays.toString(mAutofillHints) + " }";
    }

    public static final class Builder
    {
        private String[] mAutofillHints;
        private boolean mBuilt;

        public Builder()
        {
        }

        public Builder setAutofillHints(String... value)
        {
            if (mBuilt) throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");

            mAutofillHints = value;
            return this;
        }

        public VirtualViewFillInfo build()
        {
            if (mBuilt) throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");

            mBuilt = true;
            return new VirtualViewFillInfo(mAutofillHints);
        }
    }
}
