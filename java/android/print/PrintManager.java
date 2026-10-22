package android.print;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PrintManager
{
    private static final List<Session> sSessions = new ArrayList<>();

    private final Context mContext;
    private final Handler mHandler;

    public PrintManager(Context context)
    {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public List<PrintJob> getPrintJobs()
    {
        List<PrintJob> jobs = new ArrayList<>();
        synchronized (sSessions)
        {
            for (Session session : sSessions) jobs.add(session.mJob);
        }
        return jobs;
    }

    public boolean isPrintServiceEnabled(ComponentName service)
    {
        return false;
    }

    public PrintJob print(String printJobName, PrintDocumentAdapter documentAdapter, PrintAttributes attributes)
    {
        if (printJobName == null || printJobName.isEmpty()) throw new IllegalArgumentException("printJobName cannot be empty");
        if (documentAdapter == null) throw new IllegalArgumentException("documentAdapter cannot be null");

        PrintJobInfo info = new PrintJobInfo();
        info.setId(new PrintJobId());
        info.setLabel(printJobName);
        info.setCreationTime(System.currentTimeMillis());
        info.setCopies(1);
        info.setState(PrintJobInfo.STATE_CREATED);
        info.setAttributes(completeAttributes(attributes));

        PrintJob job = new PrintJob(info, this);
        Session session = new Session(job, documentAdapter);
        synchronized (sSessions)
        {
            sSessions.add(session);
        }

        mHandler.post(session::start);
        return job;
    }

    void cancelPrintJob(PrintJob job)
    {
        Session session = findSession(job);
        if (session != null) mHandler.post(session::cancel);
    }

    void restartPrintJob(PrintJob job)
    {
        Session session = findSession(job);
        if (session == null) return;

        job.setState(PrintJobInfo.STATE_CREATED);
        mHandler.post(session::start);
    }

    private Session findSession(PrintJob job)
    {
        synchronized (sSessions)
        {
            for (Session session : sSessions) if (session.mJob.equals(job)) return session;
        }
        return null;
    }

    private static PrintAttributes completeAttributes(PrintAttributes attributes)
    {
        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        String country = Locale.getDefault().getCountry();
        boolean letter = "US".equals(country) || "CA".equals(country) || "MX".equals(country);

        PrintAttributes.MediaSize mediaSize = attributes != null ? attributes.getMediaSize() : null;
        PrintAttributes.Resolution resolution = attributes != null ? attributes.getResolution() : null;
        PrintAttributes.Margins margins = attributes != null ? attributes.getMinMargins() : null;
        int colorMode = attributes != null ? attributes.getColorMode() : 0;
        int duplexMode = attributes != null ? attributes.getDuplexMode() : 0;

        PrintAttributes.MediaSize defaultMediaSize = letter ? PrintAttributes.MediaSize.NA_LETTER : PrintAttributes.MediaSize.ISO_A4;
        PrintAttributes.Resolution defaultResolution = new PrintAttributes.Resolution("PDF resolution", "PDF resolution", 300, 300);

        builder.setMediaSize(mediaSize != null ? mediaSize : defaultMediaSize);
        builder.setResolution(resolution != null ? resolution : defaultResolution);
        builder.setMinMargins(margins != null ? margins : PrintAttributes.Margins.NO_MARGINS);
        builder.setColorMode(colorMode != 0 ? colorMode : PrintAttributes.COLOR_MODE_COLOR);
        builder.setDuplexMode(duplexMode != 0 ? duplexMode : PrintAttributes.DUPLEX_MODE_NONE);
        return builder.build();
    }

    private File outputFile(PrintJob job)
    {
        File base = mContext != null ? mContext.getCacheDir() : null;
        if (base == null) base = new File(System.getProperty("java.io.tmpdir"));

        File directory = new File(base, "print");
        if (!directory.isDirectory()) directory.mkdirs();
        return new File(directory, job.getId() + ".pdf");
    }

    private final class Session
    {
        final PrintJob mJob;
        final PrintDocumentAdapter mAdapter;
        CancellationSignal mCancellation;
        ParcelFileDescriptor mDestination;
        boolean mActive;

        Session(PrintJob job, PrintDocumentAdapter adapter)
        {
            mJob = job;
            mAdapter = adapter;
        }

        void start()
        {
            if (mActive || mJob.isCancelled()) return;

            mActive = true;
            mCancellation = new CancellationSignal();
            mAdapter.onStart();

            Bundle extras = new Bundle();
            extras.putBoolean(PrintDocumentAdapter.EXTRA_PRINT_PREVIEW, false);

            final CancellationSignal cancellation = mCancellation;
            mAdapter.onLayout(null, mJob.getMutableInfo().getAttributes(), cancellation, new PrintDocumentAdapter.LayoutResultCallback()
            {
                @Override
                public void onLayoutFinished(PrintDocumentInfo info, boolean changed)
                {
                    mHandler.post(() -> layoutFinished(cancellation));
                }

                @Override
                public void onLayoutFailed(CharSequence error)
                {
                    mHandler.post(() -> finish(PrintJobInfo.STATE_FAILED));
                }

                @Override
                public void onLayoutCancelled()
                {
                    mHandler.post(() -> finish(PrintJobInfo.STATE_CANCELED));
                }
            }, extras);
        }

        void layoutFinished(CancellationSignal cancellation)
        {
            if (!mActive || cancellation.isCanceled()) return;

            mJob.setState(PrintJobInfo.STATE_QUEUED);

            try
            {
                mDestination = ParcelFileDescriptor.open(outputFile(mJob),
                    ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
            }
            catch (IOException exception)
            {
                finish(PrintJobInfo.STATE_FAILED);
                return;
            }

            mJob.setState(PrintJobInfo.STATE_STARTED);
            mAdapter.onWrite(new PageRange[] {PageRange.ALL_PAGES}, mDestination, cancellation,
                new PrintDocumentAdapter.WriteResultCallback()
                {
                    @Override
                    public void onWriteFinished(PageRange[] pages)
                    {
                        mHandler.post(() ->
                        {
                            synchronized (mJob.getMutableInfo())
                            {
                                mJob.getMutableInfo().setPages(pages);
                            }
                            finish(PrintJobInfo.STATE_COMPLETED);
                        });
                    }

                    @Override
                    public void onWriteFailed(CharSequence error)
                    {
                        mHandler.post(() -> finish(PrintJobInfo.STATE_FAILED));
                    }

                    @Override
                    public void onWriteCancelled()
                    {
                        mHandler.post(() -> finish(PrintJobInfo.STATE_CANCELED));
                    }
                });
        }

        void cancel()
        {
            if (!mActive)
            {
                mJob.setState(PrintJobInfo.STATE_CANCELED);
                return;
            }

            mCancellation.cancel();
            finish(PrintJobInfo.STATE_CANCELED);
        }

        void finish(int state)
        {
            if (!mActive) return;

            mActive = false;
            if (mDestination != null)
            {
                try
                {
                    mDestination.close();
                }
                catch (IOException exception)
                {
                }
                mDestination = null;
            }

            mJob.setState(state);
            mAdapter.onFinish();
        }
    }
}
