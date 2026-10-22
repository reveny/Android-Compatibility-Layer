package android.renderscript;

public class BaseObj
{
    final RenderScript mRS;

    private final long mID;

    private String mName;
    private boolean mDestroyed;

    BaseObj(RenderScript rs)
    {
        if (rs != null) rs.validate();

        mRS = rs;
        mID = RenderScript.nextIdentifier();
    }

    long getID()
    {
        if (mDestroyed) throw new RSInvalidStateException("using a destroyed object.");
        return mID;
    }

    void checkValid()
    {
        if (mDestroyed) throw new RSInvalidStateException("using a destroyed object.");
        if (mRS != null) mRS.validate();
    }

    public void setName(String name)
    {
        if (name == null) throw new RSIllegalArgumentException("setName requires a string of non-zero length.");
        if (name.length() < 1) throw new RSIllegalArgumentException("setName does not accept a zero length string.");
        if (mName != null) throw new RSIllegalArgumentException("setName object already has a name.");

        mName = name;
    }

    public String getName()
    {
        return mName;
    }

    @Override
    protected void finalize() throws Throwable
    {
        mDestroyed = true;
        super.finalize();
    }

    public void destroy()
    {
        if (mDestroyed) throw new RSInvalidStateException("Object already destroyed.");
        mDestroyed = true;
    }

    @Override
    public int hashCode()
    {
        return (int) ((mID & 0xFFFFFFFL) ^ (mID >> 32));
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        return mID == ((BaseObj) other).mID;
    }
}
