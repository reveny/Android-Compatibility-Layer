package android.renderscript;

import android.util.SparseArray;

public class Script extends BaseObj
{
    private final SparseArray<KernelID> mKernelIDs = new SparseArray<>();
    private final SparseArray<InvokeID> mInvokeIDs = new SparseArray<>();
    private final SparseArray<FieldID> mFieldIDs = new SparseArray<>();
    private final SparseArray<Object> mVariables = new SparseArray<>();

    private String mTimeZone;

    Script(RenderScript rs)
    {
        super(rs);
    }

    public static final class KernelID extends BaseObj
    {
        final Script mScript;
        final int mSlot;
        final int mSignature;

        KernelID(RenderScript rs, Script script, int slot, int signature)
        {
            super(rs);
            mScript = script;
            mSlot = slot;
            mSignature = signature;
        }
    }

    public static final class InvokeID extends BaseObj
    {
        final Script mScript;
        final int mSlot;

        InvokeID(RenderScript rs, Script script, int slot)
        {
            super(rs);
            mScript = script;
            mSlot = slot;
        }
    }

    public static final class FieldID extends BaseObj
    {
        final Script mScript;
        final int mSlot;

        FieldID(RenderScript rs, Script script, int slot)
        {
            super(rs);
            mScript = script;
            mSlot = slot;
        }
    }

    public static final class LaunchOptions
    {
        private int mXStart;
        private int mXEnd;
        private int mYStart;
        private int mYEnd;
        private int mZStart;
        private int mZEnd;

        public LaunchOptions()
        {
        }

        public LaunchOptions setX(int xstartArg, int xendArg)
        {
            if (xstartArg < 0 || xendArg <= xstartArg) throw new RSIllegalArgumentException("Invalid dimensions");
            mXStart = xstartArg;
            mXEnd = xendArg;
            return this;
        }

        public LaunchOptions setY(int ystartArg, int yendArg)
        {
            if (ystartArg < 0 || yendArg <= ystartArg) throw new RSIllegalArgumentException("Invalid dimensions");
            mYStart = ystartArg;
            mYEnd = yendArg;
            return this;
        }

        public LaunchOptions setZ(int zstartArg, int zendArg)
        {
            if (zstartArg < 0 || zendArg <= zstartArg) throw new RSIllegalArgumentException("Invalid dimensions");
            mZStart = zstartArg;
            mZEnd = zendArg;
            return this;
        }

        public int getXStart()
        {
            return mXStart;
        }

        public int getXEnd()
        {
            return mXEnd;
        }

        public int getYStart()
        {
            return mYStart;
        }

        public int getYEnd()
        {
            return mYEnd;
        }

        public int getZStart()
        {
            return mZStart;
        }

        public int getZEnd()
        {
            return mZEnd;
        }
    }

    public static class Builder
    {
        final RenderScript mRS;

        Builder(RenderScript rs)
        {
            mRS = rs;
        }
    }

    public static class FieldBase
    {
        protected Element mElement;
        protected Allocation mAllocation;

        protected FieldBase()
        {
        }

        protected void init(RenderScript rs, int dimx)
        {
            mAllocation = Allocation.createSized(rs, mElement, dimx, Allocation.USAGE_SCRIPT);
        }

        protected void init(RenderScript rs, int dimx, int usages)
        {
            mAllocation = Allocation.createSized(rs, mElement, dimx, Allocation.USAGE_SCRIPT | usages);
        }

        public Element getElement()
        {
            return mElement;
        }

        public Type getType()
        {
            return mAllocation.getType();
        }

        public Allocation getAllocation()
        {
            return mAllocation;
        }

        public void updateAllocation()
        {
        }
    }

    protected KernelID createKernelID(int slot, int sig, Element ein, Element eout)
    {
        KernelID kernelId = mKernelIDs.get(slot);
        if (kernelId != null) return kernelId;

        kernelId = new KernelID(mRS, this, slot, sig);
        mKernelIDs.put(slot, kernelId);
        return kernelId;
    }

    protected InvokeID createInvokeID(int slot)
    {
        InvokeID invokeId = mInvokeIDs.get(slot);
        if (invokeId != null) return invokeId;

        invokeId = new InvokeID(mRS, this, slot);
        mInvokeIDs.put(slot, invokeId);
        return invokeId;
    }

    protected FieldID createFieldID(int slot, Element e)
    {
        FieldID fieldId = mFieldIDs.get(slot);
        if (fieldId != null) return fieldId;

        fieldId = new FieldID(mRS, this, slot);
        mFieldIDs.put(slot, fieldId);
        return fieldId;
    }

    protected void invoke(int slot)
    {
        checkValid();
    }

    protected void invoke(int slot, FieldPacker v)
    {
        checkValid();
    }

    protected void forEach(int slot, Allocation ain, Allocation aout, FieldPacker v)
    {
        forEach(slot, ain, aout, v, null);
    }

    protected void forEach(int slot, Allocation ain, Allocation aout, FieldPacker v, LaunchOptions sc)
    {
        checkValid();
        if (ain == null && aout == null && sc == null)
        {
            throw new RSIllegalArgumentException(
                    "At least one of input allocation, output allocation, or LaunchOptions is required to be non-null.");
        }
    }

    protected void forEach(int slot, Allocation[] ains, Allocation aout, FieldPacker v)
    {
        forEach(slot, ains, aout, v, null);
    }

    protected void forEach(int slot, Allocation[] ains, Allocation aout, FieldPacker v, LaunchOptions sc)
    {
        checkValid();
        if (ains == null && aout == null) throw new RSIllegalArgumentException("At least one of ain or aout is required to be non-null.");
    }

    protected void reduce(int slot, Allocation[] ains, Allocation aout, LaunchOptions sc)
    {
        checkValid();
        if (ains == null || ains.length < 1) throw new RSIllegalArgumentException("At least one input is required.");
        if (aout == null) throw new RSIllegalArgumentException("aout is required to be non-null.");
    }

    public void bindAllocation(Allocation va, int slot)
    {
        checkValid();
        mVariables.put(slot, va);
    }

    public void setVar(int index, float v)
    {
        mVariables.put(index, v);
    }

    public float getVarF(int index)
    {
        Object value = mVariables.get(index);
        return value instanceof Number ? ((Number) value).floatValue() : 0;
    }

    public void setVar(int index, double v)
    {
        mVariables.put(index, v);
    }

    public double getVarD(int index)
    {
        Object value = mVariables.get(index);
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }

    public void setVar(int index, int v)
    {
        mVariables.put(index, v);
    }

    public int getVarI(int index)
    {
        Object value = mVariables.get(index);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public void setVar(int index, long v)
    {
        mVariables.put(index, v);
    }

    public long getVarJ(int index)
    {
        Object value = mVariables.get(index);
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    public void setVar(int index, boolean v)
    {
        mVariables.put(index, v);
    }

    public boolean getVarB(int index)
    {
        Object value = mVariables.get(index);
        return value instanceof Boolean && (Boolean) value;
    }

    public void setVar(int index, BaseObj o)
    {
        mVariables.put(index, o);
    }

    public void setVar(int index, FieldPacker v)
    {
        mVariables.put(index, v.getData().clone());
    }

    public void setVar(int index, FieldPacker v, Element e, int[] dims)
    {
        if (e == null || dims == null) throw new RSIllegalArgumentException("Element and dims must be non-null");
        mVariables.put(index, v.getData().clone());
    }

    public void getVarV(int index, FieldPacker v)
    {
        Object value = mVariables.get(index);
        if (!(value instanceof byte[])) return;

        byte[] stored = (byte[]) value;
        byte[] target = v.getData();
        System.arraycopy(stored, 0, target, 0, Math.min(stored.length, target.length));
    }

    public void setTimeZone(String timeZone)
    {
        checkValid();
        if (timeZone == null) throw new RSIllegalArgumentException("timeZone must be non-null");
        mTimeZone = timeZone;
    }
}
