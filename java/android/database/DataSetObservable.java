package android.database;

public class DataSetObservable extends Observable<DataSetObserver>
{
    public void notifyChanged()
    {
        synchronized (mObservers)
        {
            for (int index = mObservers.size() - 1; index >= 0; index--) mObservers.get(index).onChanged();
        }
    }

    public void notifyInvalidated()
    {
        synchronized (mObservers)
        {
            for (int index = mObservers.size() - 1; index >= 0; index--) mObservers.get(index).onInvalidated();
        }
    }
}
