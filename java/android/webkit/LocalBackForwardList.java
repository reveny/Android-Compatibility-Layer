package android.webkit;

import java.util.ArrayList;
import java.util.List;

final class LocalBackForwardList extends WebBackForwardList
{
    private final List<LocalHistoryItem> mItems = new ArrayList<>();
    private int mCurrentIndex = -1;

    synchronized void push(LocalHistoryItem item)
    {
        while (mItems.size() > mCurrentIndex + 1) mItems.remove(mItems.size() - 1);
        mItems.add(item);
        mCurrentIndex = mItems.size() - 1;
    }

    synchronized void replaceCurrent(LocalHistoryItem item)
    {
        if (mCurrentIndex < 0)
        {
            push(item);
            return;
        }
        mItems.set(mCurrentIndex, item);
    }

    synchronized boolean canGoBackOrForward(int steps)
    {
        int target = mCurrentIndex + steps;
        return mCurrentIndex >= 0 && target >= 0 && target < mItems.size();
    }

    synchronized LocalHistoryItem goBackOrForward(int steps)
    {
        if (!canGoBackOrForward(steps)) return null;
        mCurrentIndex += steps;
        return mItems.get(mCurrentIndex);
    }

    synchronized void clearAllButCurrent()
    {
        if (mCurrentIndex < 0)
        {
            mItems.clear();
            return;
        }
        LocalHistoryItem current = mItems.get(mCurrentIndex);
        mItems.clear();
        mItems.add(current);
        mCurrentIndex = 0;
    }

    synchronized void clear()
    {
        mItems.clear();
        mCurrentIndex = -1;
    }

    @Override
    public synchronized LocalHistoryItem getCurrentItem()
    {
        return mCurrentIndex < 0 ? null : mItems.get(mCurrentIndex);
    }

    @Override
    public synchronized int getCurrentIndex()
    {
        return mCurrentIndex;
    }

    @Override
    public synchronized LocalHistoryItem getItemAtIndex(int index)
    {
        if (index < 0 || index >= mItems.size()) return null;
        return mItems.get(index);
    }

    @Override
    public synchronized int getSize()
    {
        return mItems.size();
    }

    @Override
    protected synchronized LocalBackForwardList clone()
    {
        LocalBackForwardList copy = new LocalBackForwardList();
        for (LocalHistoryItem item : mItems) copy.mItems.add(item.clone());
        copy.mCurrentIndex = mCurrentIndex;
        return copy;
    }
}
