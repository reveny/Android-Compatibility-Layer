package android.app;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public abstract class FragmentManager
{
    public static final int POP_BACK_STACK_INCLUSIVE = 1;

    static boolean sDebug;

    public interface BackStackEntry
    {
        int getId();

        String getName();

        int getBreadCrumbTitleRes();

        int getBreadCrumbShortTitleRes();

        CharSequence getBreadCrumbTitle();

        CharSequence getBreadCrumbShortTitle();
    }

    public interface OnBackStackChangedListener
    {
        void onBackStackChanged();
    }

    public abstract static class FragmentLifecycleCallbacks
    {
        public FragmentLifecycleCallbacks()
        {
        }

        public void onFragmentPreAttached(FragmentManager manager, Fragment fragment, Context context)
        {
        }

        public void onFragmentAttached(FragmentManager manager, Fragment fragment, Context context)
        {
        }

        public void onFragmentPreCreated(FragmentManager manager, Fragment fragment, Bundle savedInstanceState)
        {
        }

        public void onFragmentCreated(FragmentManager manager, Fragment fragment, Bundle savedInstanceState)
        {
        }

        public void onFragmentActivityCreated(FragmentManager manager, Fragment fragment, Bundle savedInstanceState)
        {
        }

        public void onFragmentViewCreated(FragmentManager manager, Fragment fragment, View view, Bundle savedInstanceState)
        {
        }

        public void onFragmentStarted(FragmentManager manager, Fragment fragment)
        {
        }

        public void onFragmentResumed(FragmentManager manager, Fragment fragment)
        {
        }

        public void onFragmentPaused(FragmentManager manager, Fragment fragment)
        {
        }

        public void onFragmentStopped(FragmentManager manager, Fragment fragment)
        {
        }

        public void onFragmentSaveInstanceState(FragmentManager manager, Fragment fragment, Bundle outState)
        {
        }

        public void onFragmentViewDestroyed(FragmentManager manager, Fragment fragment)
        {
        }

        public void onFragmentDestroyed(FragmentManager manager, Fragment fragment)
        {
        }

        public void onFragmentDetached(FragmentManager manager, Fragment fragment)
        {
        }
    }

    public FragmentManager()
    {
    }

    public abstract FragmentTransaction beginTransaction();

    public abstract boolean executePendingTransactions();

    public abstract Fragment findFragmentById(int id);

    public abstract Fragment findFragmentByTag(String tag);

    public abstract void popBackStack();

    public abstract boolean popBackStackImmediate();

    public abstract void popBackStack(String name, int flags);

    public abstract boolean popBackStackImmediate(String name, int flags);

    public abstract void popBackStack(int id, int flags);

    public abstract boolean popBackStackImmediate(int id, int flags);

    public abstract int getBackStackEntryCount();

    public abstract BackStackEntry getBackStackEntryAt(int index);

    public abstract void addOnBackStackChangedListener(OnBackStackChangedListener listener);

    public abstract void removeOnBackStackChangedListener(OnBackStackChangedListener listener);

    public abstract void putFragment(Bundle bundle, String key, Fragment fragment);

    public abstract Fragment getFragment(Bundle bundle, String key);

    public abstract List<Fragment> getFragments();

    public abstract Fragment.SavedState saveFragmentInstanceState(Fragment fragment);

    public abstract boolean isDestroyed();

    public abstract void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks callback, boolean recursive);

    public abstract void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks callback);

    public abstract Fragment getPrimaryNavigationFragment();

    public abstract void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args);

    public static void enableDebugLogging(boolean enabled)
    {
        sDebug = enabled;
    }

    public void invalidateOptionsMenu()
    {
    }

    public abstract boolean isStateSaved();
}
