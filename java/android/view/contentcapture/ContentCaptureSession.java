package android.view.contentcapture;

import android.view.autofill.AutofillId;

public abstract class ContentCaptureSession implements AutoCloseable
{
    public AutofillId newAutofillId(AutofillId hostId, long virtualChildId)
    {
        return null;
    }

    public ViewStructure newViewStructure(android.view.View view)
    {
        return null;
    }

    public ViewStructure newVirtualViewStructure(AutofillId parentId, long virtualId)
    {
        return null;
    }

    public void notifyViewAppeared(ViewStructure node)
    {
    }

    public void notifyViewDisappeared(AutofillId id)
    {
    }

    public void notifyViewsDisappeared(AutofillId hostId, long[] virtualIds)
    {
    }

    public void notifyViewTextChanged(AutofillId id, CharSequence text)
    {
    }

    public ContentCaptureSession createContentCaptureSession(ContentCaptureContext context)
    {
        return this;
    }

    public ContentCaptureContext getContentCaptureContext()
    {
        return null;
    }

    public void setContentCaptureContext(ContentCaptureContext context)
    {
    }

    @Override
    public void close()
    {
    }
}
