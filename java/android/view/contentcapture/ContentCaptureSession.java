package android.view.contentcapture;

import android.view.RuntimeViewStructure;
import android.view.ViewStructure;
import android.view.autofill.AutofillId;

import java.util.List;

public abstract class ContentCaptureSession implements AutoCloseable
{
    private static final java.util.concurrent.atomic.AtomicInteger sNextSessionId = new java.util.concurrent.atomic.AtomicInteger(1);

    private final ContentCaptureSessionId mId = new ContentCaptureSessionId(sNextSessionId.getAndIncrement());
    private ContentCaptureContext mContext;
    private boolean mDestroyed;

    public final ContentCaptureSessionId getContentCaptureSessionId()
    {
        return mId;
    }

    public final ContentCaptureSession createContentCaptureSession(ContentCaptureContext context)
    {
        RuntimeContentCaptureSession child = new RuntimeContentCaptureSession();
        child.setContentCaptureContext(context);
        return child;
    }

    public final void setContentCaptureContext(ContentCaptureContext context)
    {
        mContext = context;
    }

    public final ContentCaptureContext getContentCaptureContext()
    {
        return mContext;
    }

    public final void destroy()
    {
        mDestroyed = true;
    }

    public final boolean isDestroyed()
    {
        return mDestroyed;
    }

    @Override
    public void close()
    {
        destroy();
    }

    public final void notifyViewAppeared(ViewStructure node)
    {
    }

    public final void notifyViewDisappeared(AutofillId id)
    {
    }

    public final void notifyViewsAppeared(List<ViewStructure> appearedNodes)
    {
    }

    public final void notifyViewsDisappeared(AutofillId hostId, long[] virtualIds)
    {
    }

    public final void notifyViewTextChanged(AutofillId id, CharSequence text)
    {
    }

    public final void notifyViewInsetsChanged(android.graphics.Insets viewInsets)
    {
    }

    public void flush()
    {
    }

    public final void notifySessionResumed()
    {
    }

    public final void notifySessionPaused()
    {
    }

    public final ViewStructure newViewStructure(android.view.View view)
    {
        RuntimeViewStructure structure = new RuntimeViewStructure();
        if (view != null) structure.setAutofillId(view.getAutofillId());
        return structure;
    }

    public AutofillId newAutofillId(AutofillId hostId, long virtualChildId)
    {
        return new AutofillId(hostId, virtualChildId, mId.getValue());
    }

    public final ViewStructure newVirtualViewStructure(AutofillId parentId, long virtualId)
    {
        RuntimeViewStructure structure = new RuntimeViewStructure();
        structure.setAutofillId(new AutofillId(parentId, virtualId, mId.getValue()));
        return structure;
    }

    @Override
    public String toString()
    {
        return "ContentCaptureSession[id=" + mId + "]";
    }
}
