package android.window;

import android.util.Log;
import android.view.AttachedSurfaceControl;
import android.view.SurfaceControl;

import java.util.ArrayList;

public final class SurfaceSyncGroup
{
    private static final String TAG = "SurfaceSyncGroup";

    private final String mName;
    private final ArrayList<SurfaceControl.Transaction> mTransactions = new ArrayList<>();
    private boolean mSyncReady;
    private boolean mFinished;

    public SurfaceSyncGroup(String name)
    {
        mName = name;
    }

    public boolean add(AttachedSurfaceControl attachedSurfaceControl, Runnable runnable)
    {
        synchronized (this)
        {
            if (mSyncReady || mFinished)
            {
                Log.w(TAG, "the sync group " + mName + " is already ready, so nothing more can be added");
                return false;
            }
        }

        if (attachedSurfaceControl == null) return false;

        if (runnable != null) runnable.run();
        return true;
    }

    public void addTransaction(SurfaceControl.Transaction transaction)
    {
        if (transaction == null) return;

        synchronized (this)
        {
            if (!mFinished)
            {
                mTransactions.add(transaction);
                return;
            }
        }

        transaction.apply();
    }

    public void markSyncReady()
    {
        ArrayList<SurfaceControl.Transaction> pending;

        synchronized (this)
        {
            if (mSyncReady) return;

            mSyncReady = true;
            mFinished = true;
            pending = new ArrayList<>(mTransactions);
            mTransactions.clear();
        }

        for (SurfaceControl.Transaction transaction : pending) transaction.apply();
    }

    @Override
    public String toString()
    {
        return "SurfaceSyncGroup{" + mName + (mFinished ? " finished" : "") + "}";
    }
}
