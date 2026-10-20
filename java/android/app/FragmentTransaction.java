package android.app;

public abstract class FragmentTransaction
{
    public abstract FragmentTransaction add(Fragment fragment, String tag);

    public abstract FragmentTransaction add(int containerViewId, Fragment fragment);

    public abstract FragmentTransaction remove(Fragment fragment);

    public abstract FragmentTransaction replace(int containerViewId, Fragment fragment);

    public abstract int commit();

    public abstract int commitAllowingStateLoss();

    public abstract void commitNow();
}
