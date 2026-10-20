package android.view.autofill;

public final class AutofillId
{
    private final int mId;

    public AutofillId(int id)
    {
        mId = id;
    }

    public int hashCode()
    {
        return mId;
    }

    public boolean equals(Object other)
    {
        return other instanceof AutofillId && ((AutofillId)other).mId == mId;
    }

    public String toString()
    {
        return Integer.toString(mId);
    }
}
