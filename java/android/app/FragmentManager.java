package android.app;

public abstract class FragmentManager
{
    public abstract FragmentTransaction beginTransaction();

    public abstract Fragment findFragmentByTag(String tag);

    public abstract Fragment findFragmentById(int id);

    public abstract boolean executePendingTransactions();

    public abstract int getBackStackEntryCount();

    public abstract void popBackStack();
}
