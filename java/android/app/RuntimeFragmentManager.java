package android.app;

import java.util.ArrayList;
import java.util.HashMap;

public final class RuntimeFragmentManager extends FragmentManager
{
    private final HashMap<String, Fragment> mByTag = new HashMap<>();
    private final ArrayList<Fragment> mFragments = new ArrayList<>();

    @Override
    public FragmentTransaction beginTransaction()
    {
        return new Transaction();
    }

    @Override
    public Fragment findFragmentByTag(String tag)
    {
        return mByTag.get(tag);
    }

    @Override
    public Fragment findFragmentById(int id)
    {
        return null;
    }

    @Override
    public boolean executePendingTransactions()
    {
        return false;
    }

    @Override
    public int getBackStackEntryCount()
    {
        return 0;
    }

    @Override
    public void popBackStack()
    {
    }

    private final class Transaction extends FragmentTransaction
    {
        @Override
        public FragmentTransaction add(Fragment fragment, String tag)
        {
            if (fragment == null) return this;

            mFragments.add(fragment);
            if (tag != null) mByTag.put(tag, fragment);
            return this;
        }

        @Override
        public FragmentTransaction add(int containerViewId, Fragment fragment)
        {
            return add(fragment, null);
        }

        @Override
        public FragmentTransaction remove(Fragment fragment)
        {
            mFragments.remove(fragment);
            mByTag.values().remove(fragment);
            return this;
        }

        @Override
        public FragmentTransaction replace(int containerViewId, Fragment fragment)
        {
            return add(fragment, null);
        }

        @Override
        public int commit()
        {
            dispatchCreate();
            return 0;
        }

        @Override
        public int commitAllowingStateLoss()
        {
            return commit();
        }

        @Override
        public void commitNow()
        {
            commit();
        }

        private void dispatchCreate()
        {
            for (Fragment fragment : new ArrayList<>(mFragments))
            {
                fragment.onCreate(null);
                fragment.onStart();
                fragment.onResume();
            }
        }
    }
}
