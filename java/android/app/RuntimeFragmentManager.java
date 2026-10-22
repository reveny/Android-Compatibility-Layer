package android.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RuntimeFragmentManager extends FragmentManager
{
    private static final String TAG = "FragmentManager";

    static final String SAVED_FRAGMENTS = "android:fragments";
    private static final String SAVED_VIEW_STATE = "android:view_state";
    private static final String SAVED_USER_VISIBLE_HINT = "android:user_visible_hint";

    private static final int EVENT_PRE_ATTACHED = 0;
    private static final int EVENT_ATTACHED = 1;
    private static final int EVENT_PRE_CREATED = 2;
    private static final int EVENT_CREATED = 3;
    private static final int EVENT_ACTIVITY_CREATED = 4;
    private static final int EVENT_VIEW_CREATED = 5;
    private static final int EVENT_STARTED = 6;
    private static final int EVENT_RESUMED = 7;
    private static final int EVENT_PAUSED = 8;
    private static final int EVENT_STOPPED = 9;
    private static final int EVENT_SAVE_INSTANCE_STATE = 10;
    private static final int EVENT_VIEW_DESTROYED = 11;
    private static final int EVENT_DESTROYED = 12;
    private static final int EVENT_DETACHED = 13;

    private static final int OP_ADD = 1;
    private static final int OP_REPLACE = 2;
    private static final int OP_REMOVE = 3;
    private static final int OP_HIDE = 4;
    private static final int OP_SHOW = 5;
    private static final int OP_DETACH = 6;
    private static final int OP_ATTACH = 7;
    private static final int OP_SET_PRIMARY = 8;

    private final Activity mHost;
    private final Fragment mParent;
    private final ArrayList<Fragment> mAdded = new ArrayList<>();
    private final ArrayList<Fragment> mActive = new ArrayList<>();
    private final ArrayList<Record> mBackStack = new ArrayList<>();
    private final ArrayList<Runnable> mPending = new ArrayList<>();
    private final ArrayList<OnBackStackChangedListener> mBackStackListeners = new ArrayList<>();
    private final ArrayList<Registration> mLifecycleCallbacks = new ArrayList<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private int mState = Fragment.INITIALIZING;
    private int mNextBackStackIndex;
    private boolean mDestroyed;
    private boolean mStateSaved;
    private boolean mExecuting;
    private boolean mScheduled;
    private boolean mBackStackChanged;
    private Fragment mPrimaryNavigation;

    private final Runnable mExecutePending = new Runnable()
    {
        @Override
        public void run()
        {
            mScheduled = false;
            execPendingActions();
        }
    };

    private static final class Registration
    {
        final FragmentLifecycleCallbacks mCallback;
        final boolean mRecursive;

        Registration(FragmentLifecycleCallbacks callback, boolean recursive)
        {
            mCallback = callback;
            mRecursive = recursive;
        }
    }

    private static final class Op
    {
        final int mCommand;
        final Fragment mFragment;
        final ArrayList<Fragment> mRemoved = new ArrayList<>();
        Fragment mPreviousPrimary;

        Op(int command, Fragment fragment)
        {
            mCommand = command;
            mFragment = fragment;
        }
    }

    public RuntimeFragmentManager(Activity host)
    {
        this(host, null);
    }

    RuntimeFragmentManager(Activity host, Fragment parent)
    {
        mHost = host;
        mParent = parent;
    }

    private Activity host()
    {
        if (mHost != null) return mHost;

        return mParent == null ? null : mParent.mActivity;
    }

    @Override
    public FragmentTransaction beginTransaction()
    {
        return new Record();
    }

    @Override
    public boolean executePendingTransactions()
    {
        return execPendingActions();
    }

    @Override
    public Fragment findFragmentById(int id)
    {
        for (int index = mAdded.size() - 1; index >= 0; index--)
        {
            Fragment fragment = mAdded.get(index);
            if (fragment.mId == id) return fragment;
        }

        for (int index = mActive.size() - 1; index >= 0; index--)
        {
            Fragment fragment = mActive.get(index);
            if (fragment.mId == id) return fragment;
        }

        return null;
    }

    @Override
    public Fragment findFragmentByTag(String tag)
    {
        if (tag == null) return null;

        for (int index = mAdded.size() - 1; index >= 0; index--)
        {
            Fragment fragment = mAdded.get(index);
            if (tag.equals(fragment.mTag)) return fragment;
        }

        for (int index = mActive.size() - 1; index >= 0; index--)
        {
            Fragment fragment = mActive.get(index);
            if (tag.equals(fragment.mTag)) return fragment;
        }

        return null;
    }

    @Override
    public void popBackStack()
    {
        enqueuePop(null, -1, 0);
    }

    @Override
    public boolean popBackStackImmediate()
    {
        requireNotSaved();
        execPendingActions();
        return popBackStackState(null, -1, 0);
    }

    @Override
    public void popBackStack(String name, int flags)
    {
        enqueuePop(name, -1, flags);
    }

    @Override
    public boolean popBackStackImmediate(String name, int flags)
    {
        requireNotSaved();
        execPendingActions();
        return popBackStackState(name, -1, flags);
    }

    @Override
    public void popBackStack(int id, int flags)
    {
        if (id < 0) throw new IllegalArgumentException("Bad id: " + id);

        enqueuePop(null, id, flags);
    }

    @Override
    public boolean popBackStackImmediate(int id, int flags)
    {
        requireNotSaved();
        execPendingActions();

        if (id < 0) throw new IllegalArgumentException("Bad id: " + id);

        return popBackStackState(null, id, flags);
    }

    @Override
    public int getBackStackEntryCount()
    {
        return mBackStack.size();
    }

    @Override
    public BackStackEntry getBackStackEntryAt(int index)
    {
        return mBackStack.get(index);
    }

    @Override
    public void addOnBackStackChangedListener(OnBackStackChangedListener listener)
    {
        if (listener != null) mBackStackListeners.add(listener);
    }

    @Override
    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener)
    {
        mBackStackListeners.remove(listener);
    }

    @Override
    public void putFragment(Bundle bundle, String key, Fragment fragment)
    {
        if (fragment.mIndex < 0 || !mActive.contains(fragment))
        {
            throw new IllegalStateException("Fragment " + fragment + " is not currently in the FragmentManager");
        }

        bundle.putInt(key, fragment.mIndex);
    }

    @Override
    public Fragment getFragment(Bundle bundle, String key)
    {
        int index = bundle.getInt(key, -1);
        if (index == -1) return null;

        for (Fragment fragment : mActive)
        {
            if (fragment.mIndex == index) return fragment;
        }

        throw new IllegalStateException("Fragment no longer exists for key " + key + ": index " + index);
    }

    @Override
    public List<Fragment> getFragments()
    {
        return Collections.unmodifiableList(new ArrayList<>(mAdded));
    }

    @Override
    public Fragment.SavedState saveFragmentInstanceState(Fragment fragment)
    {
        if (fragment.mFragmentManager != this)
        {
            throw new IllegalStateException("Fragment " + fragment + " is not currently in the FragmentManager");
        }

        if (fragment.mState <= Fragment.INITIALIZING) return null;

        Bundle state = saveFragmentBasicState(fragment);
        return state == null ? null : new Fragment.SavedState(state);
    }

    @Override
    public boolean isDestroyed()
    {
        return mDestroyed;
    }

    @Override
    public boolean isStateSaved()
    {
        return mStateSaved;
    }

    @Override
    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks callback, boolean recursive)
    {
        if (callback != null) mLifecycleCallbacks.add(new Registration(callback, recursive));
    }

    @Override
    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks callback)
    {
        for (int index = mLifecycleCallbacks.size() - 1; index >= 0; index--)
        {
            if (mLifecycleCallbacks.get(index).mCallback == callback)
            {
                mLifecycleCallbacks.remove(index);
                return;
            }
        }
    }

    @Override
    public Fragment getPrimaryNavigationFragment()
    {
        return mPrimaryNavigation;
    }

    @Override
    public void invalidateOptionsMenu()
    {
        Activity host = host();
        if (host != null) host.invalidateOptionsMenu();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args)
    {
        String inner = prefix + "    ";

        if (!mActive.isEmpty())
        {
            writer.print(prefix);
            writer.println("Active Fragments in " + Integer.toHexString(System.identityHashCode(this)) + ":");

            for (int index = 0; index < mActive.size(); index++)
            {
                Fragment fragment = mActive.get(index);

                writer.print(prefix);
                writer.print("  #");
                writer.print(index);
                writer.print(": ");
                writer.println(fragment);
                fragment.dump(inner, fd, writer, args);
            }
        }

        if (!mAdded.isEmpty())
        {
            writer.print(prefix);
            writer.println("Added Fragments:");

            for (int index = 0; index < mAdded.size(); index++)
            {
                writer.print(prefix);
                writer.print("  #");
                writer.print(index);
                writer.print(": ");
                writer.println(mAdded.get(index));
            }
        }

        if (!mBackStack.isEmpty())
        {
            writer.print(prefix);
            writer.println("Back Stack:");

            for (int index = 0; index < mBackStack.size(); index++)
            {
                writer.print(prefix);
                writer.print("  #");
                writer.print(index);
                writer.print(": ");
                writer.println(mBackStack.get(index));
            }
        }

        writer.print(prefix);
        writer.println("FragmentManager misc state:");
        writer.print(prefix);
        writer.print("  mHost=");
        writer.println(host());

        if (mParent != null)
        {
            writer.print(prefix);
            writer.print("  mParent=");
            writer.println(mParent);
        }

        writer.print(prefix);
        writer.print("  mCurState=");
        writer.print(mState);
        writer.print(" mStateSaved=");
        writer.print(mStateSaved);
        writer.print(" mDestroyed=");
        writer.println(mDestroyed);
    }

    @Override
    public String toString()
    {
        Object owner = mParent != null ? mParent : host();
        return "FragmentManager{" + Integer.toHexString(System.identityHashCode(this)) + " in " + owner + "}";
    }

    void adoptInflated(Fragment fragment)
    {
        fragment.mFragmentManager = this;
        fragment.mParentFragment = mParent;
        fragment.assignIndex();
        fragment.mAdded = true;
        fragment.mRemoving = false;
        fragment.mState = Fragment.ACTIVITY_CREATED;

        if (!mActive.contains(fragment)) mActive.add(fragment);
        if (!mAdded.contains(fragment)) mAdded.add(fragment);

        if (mState <= Fragment.ACTIVITY_CREATED) return;

        final Fragment adopted = fragment;
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (adopted.mFragmentManager == RuntimeFragmentManager.this && adopted.mAdded) moveToState(adopted, mState);
            }
        });
    }

    void dispatchCreate()
    {
        if (mState < Fragment.CREATED) dispatchState(Fragment.CREATED);
    }

    void dispatchActivityCreated()
    {
        dispatchState(Fragment.ACTIVITY_CREATED);
    }

    void dispatchStart()
    {
        dispatchState(Fragment.STARTED);
    }

    void dispatchResume()
    {
        dispatchState(Fragment.RESUMED);
    }

    void dispatchPause()
    {
        dispatchState(Fragment.STARTED);
    }

    void dispatchStop()
    {
        mStateSaved = true;
        dispatchState(Fragment.ACTIVITY_CREATED);
    }

    void dispatchDestroy()
    {
        mDestroyed = true;
        execPendingActions();
        dispatchState(Fragment.INITIALIZING);
        mActive.clear();
        mAdded.clear();
        mPending.clear();
    }

    void dispatchState(int state)
    {
        if (state > Fragment.ACTIVITY_CREATED || (state > mState && state != Fragment.INITIALIZING)) mStateSaved = false;

        mState = state;

        for (Fragment fragment : new ArrayList<>(mActive)) moveToState(fragment, state);

        if (state >= Fragment.CREATED && !mPending.isEmpty() && !mScheduled) schedule();
    }

    void dispatchConfigurationChanged(Configuration configuration)
    {
        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            fragment.onConfigurationChanged(configuration);
            if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchConfigurationChanged(configuration);
        }
    }

    void dispatchLowMemory()
    {
        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            fragment.onLowMemory();
            if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchLowMemory();
        }
    }

    void dispatchTrimMemory(int level)
    {
        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            fragment.onTrimMemory(level);
            if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchTrimMemory(level);
        }
    }

    void dispatchMultiWindowModeChanged(boolean inMultiWindow, Configuration configuration)
    {
        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            fragment.onMultiWindowModeChanged(inMultiWindow, configuration);
            if (fragment.mChildFragmentManager != null)
            {
                fragment.mChildFragmentManager.dispatchMultiWindowModeChanged(inMultiWindow, configuration);
            }
        }
    }

    void dispatchPictureInPictureModeChanged(boolean inPictureInPicture, Configuration configuration)
    {
        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            fragment.onPictureInPictureModeChanged(inPictureInPicture, configuration);
            if (fragment.mChildFragmentManager != null)
            {
                fragment.mChildFragmentManager.dispatchPictureInPictureModeChanged(inPictureInPicture, configuration);
            }
        }
    }

    private static boolean offersMenu(Fragment fragment)
    {
        return !fragment.mHidden && fragment.mHasMenu && fragment.mMenuVisible;
    }

    boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        boolean show = false;

        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            if (fragment.mHidden) continue;

            if (offersMenu(fragment))
            {
                show = true;
                fragment.onCreateOptionsMenu(menu, inflater);
            }

            if (fragment.mChildFragmentManager != null) show |= fragment.mChildFragmentManager.dispatchCreateOptionsMenu(menu, inflater);
        }

        return show;
    }

    boolean dispatchPrepareOptionsMenu(Menu menu)
    {
        boolean show = false;

        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            if (fragment.mHidden) continue;

            if (offersMenu(fragment))
            {
                show = true;
                fragment.onPrepareOptionsMenu(menu);
            }

            if (fragment.mChildFragmentManager != null) show |= fragment.mChildFragmentManager.dispatchPrepareOptionsMenu(menu);
        }

        return show;
    }

    boolean dispatchOptionsItemSelected(MenuItem item)
    {
        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            if (fragment.mHidden) continue;

            if (offersMenu(fragment) && fragment.onOptionsItemSelected(item)) return true;
            if (fragment.mChildFragmentManager != null && fragment.mChildFragmentManager.dispatchOptionsItemSelected(item)) return true;
        }

        return false;
    }

    boolean dispatchContextItemSelected(MenuItem item)
    {
        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            if (fragment.mHidden) continue;

            if (fragment.mUserVisibleHint && fragment.onContextItemSelected(item)) return true;
            if (fragment.mChildFragmentManager != null && fragment.mChildFragmentManager.dispatchContextItemSelected(item)) return true;
        }

        return false;
    }

    void dispatchOptionsMenuClosed(Menu menu)
    {
        for (Fragment fragment : new ArrayList<>(mAdded))
        {
            if (fragment.mHidden) continue;

            if (offersMenu(fragment)) fragment.onOptionsMenuClosed(menu);
            if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchOptionsMenuClosed(menu);
        }
    }

    Bundle saveAllState()
    {
        execPendingActions();
        mStateSaved = true;

        ArrayList<Bundle> saved = new ArrayList<>();

        for (Fragment fragment : mActive)
        {
            if (fragment.mFromLayout) continue;

            Bundle record = new Bundle();
            record.putString("class", fragment.getClass().getName());
            record.putInt("index", fragment.mIndex);
            record.putInt("id", fragment.mId);
            record.putInt("container", fragment.mContainerId);
            record.putString("tag", fragment.mTag);
            record.putBundle("arguments", fragment.mArguments);
            record.putBoolean("added", fragment.mAdded);
            record.putBoolean("hidden", fragment.mHidden);
            record.putBoolean("detached", fragment.mDetached);
            record.putBoolean("retain", fragment.mRetainInstance);
            record.putBoolean("menu", fragment.mHasMenu);
            record.putBoolean("menu_visible", fragment.mMenuVisible);
            record.putInt("target", fragment.mTarget == null ? -1 : fragment.mTarget.mIndex);
            record.putInt("target_request", fragment.mTargetRequestCode);

            Bundle state = fragment.mState > Fragment.INITIALIZING ? saveFragmentBasicState(fragment) : fragment.mSavedFragmentState;
            if (state != null) record.putBundle("state", state);

            saved.add(record);
        }

        if (saved.isEmpty()) return null;

        Bundle all = new Bundle();
        all.putParcelableArrayList("active", saved);
        if (mPrimaryNavigation != null) all.putInt("primary", mPrimaryNavigation.mIndex);

        return all;
    }

    void restoreAllState(Bundle all)
    {
        if (all == null) return;

        Activity host = host();
        ArrayList<Bundle> saved = all.getParcelableArrayList("active");
        if (saved == null) return;

        SparseArray<Fragment> byIndex = new SparseArray<>();
        SparseArray<Integer> targets = new SparseArray<>();

        for (Bundle record : saved)
        {
            Fragment fragment;

            try
            {
                fragment = Fragment.instantiate(host, record.getString("class"), record.getBundle("arguments"));
            }
            catch (RuntimeException failure)
            {
                Log.e(TAG, "could not restore fragment " + record.getString("class"), failure);
                continue;
            }

            fragment.mIndex = record.getInt("index", -1);
            fragment.assignIndex();
            fragment.mId = record.getInt("id");
            fragment.mContainerId = record.getInt("container");
            fragment.mTag = record.getString("tag");
            fragment.mAdded = record.getBoolean("added");
            fragment.mHidden = record.getBoolean("hidden");
            fragment.mDetached = record.getBoolean("detached");
            fragment.mRetainInstance = record.getBoolean("retain");
            fragment.mHasMenu = record.getBoolean("menu");
            fragment.mMenuVisible = record.getBoolean("menu_visible", true);
            fragment.mTargetRequestCode = record.getInt("target_request");
            fragment.mSavedFragmentState = record.getBundle("state");
            if (fragment.mSavedFragmentState != null) fragment.mSavedFragmentState.setClassLoader(fragment.getClass().getClassLoader());
            fragment.mFragmentManager = this;

            mActive.add(fragment);
            if (fragment.mAdded) mAdded.add(fragment);

            byIndex.put(fragment.mIndex, fragment);
            targets.put(fragment.mIndex, record.getInt("target", -1));
        }

        for (int index = 0; index < byIndex.size(); index++)
        {
            Integer target = targets.get(byIndex.keyAt(index));
            if (target != null && target >= 0) byIndex.valueAt(index).mTarget = byIndex.get(target);
        }

        int primary = all.getInt("primary", -1);
        if (primary >= 0) mPrimaryNavigation = byIndex.get(primary);

        Log.i(TAG, "restored " + byIndex.size() + " fragment(s) for " + (mParent != null ? mParent : host));
    }

    private Bundle saveFragmentBasicState(Fragment fragment)
    {
        Bundle state = new Bundle();

        fragment.onSaveInstanceState(state);
        dispatch(EVENT_SAVE_INSTANCE_STATE, fragment, state, null, null, false);

        if (fragment.mChildFragmentManager != null)
        {
            Bundle children = fragment.mChildFragmentManager.saveAllState();
            if (children != null) state.putBundle(SAVED_FRAGMENTS, children);
        }

        if (fragment.mView != null)
        {
            SparseArray<Parcelable> viewState = new SparseArray<>();
            fragment.mView.saveHierarchyState(viewState);
            if (viewState.size() > 0) state.putSparseParcelableArray(SAVED_VIEW_STATE, viewState);
        }

        if (!fragment.mUserVisibleHint) state.putBoolean(SAVED_USER_VISIBLE_HINT, false);

        return state.isEmpty() ? null : state;
    }

    private void requireCalled(Fragment fragment, String method)
    {
        if (!fragment.mCalled)
        {
            throw new android.util.AndroidRuntimeException("Fragment " + fragment + " did not call through to super." + method);
        }
    }

    void moveToState(Fragment fragment, int requested)
    {
        int target = requested;

        if (fragment.mRemoving) target = Fragment.INITIALIZING;
        else if ((fragment.mDetached || !fragment.mAdded) && target > Fragment.CREATED) target = Fragment.CREATED;

        while (fragment.mState < target) moveUp(fragment);
        while (fragment.mState > target) moveDown(fragment);
    }

    private void moveUp(Fragment fragment)
    {
        Activity host = host();
        Bundle saved = fragment.mSavedFragmentState;

        switch (fragment.mState)
        {
            case Fragment.INITIALIZING:
            {
                fragment.mActivity = host;
                if (fragment.mContext == null) fragment.mContext = host;
                fragment.mFragmentManager = this;
                fragment.mParentFragment = mParent;
                fragment.assignIndex();

                if (saved != null) fragment.mUserVisibleHint = saved.getBoolean(SAVED_USER_VISIBLE_HINT, true);

                dispatch(EVENT_PRE_ATTACHED, fragment, null, null, host, false);
                fragment.mCalled = false;
                fragment.onAttach(host);
                requireCalled(fragment, "onAttach()");

                if (mParent == null)
                {
                    if (host != null) host.onAttachFragment(fragment);
                }
                else
                {
                    mParent.onAttachFragment(fragment);
                }

                dispatch(EVENT_ATTACHED, fragment, null, null, host, false);

                dispatch(EVENT_PRE_CREATED, fragment, saved, null, null, false);
                fragment.mCalled = false;
                fragment.onCreate(saved);
                requireCalled(fragment, "onCreate()");

                fragment.mState = Fragment.CREATED;
                if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchCreate();
                dispatch(EVENT_CREATED, fragment, saved, null, null, false);
                break;
            }

            case Fragment.CREATED:
            {
                if (!fragment.mFromLayout) createView(fragment, saved);

                fragment.mCalled = false;
                fragment.onActivityCreated(saved);
                requireCalled(fragment, "onActivityCreated()");

                if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchActivityCreated();

                if (fragment.mView != null)
                {
                    if (saved != null)
                    {
                        SparseArray<Parcelable> viewState = saved.getSparseParcelableArray(SAVED_VIEW_STATE);
                        if (viewState != null) fragment.mView.restoreHierarchyState(viewState);
                    }

                    fragment.mCalled = false;
                    fragment.onViewStateRestored(saved);
                    requireCalled(fragment, "onViewStateRestored()");
                }

                fragment.mSavedFragmentState = null;
                fragment.mState = Fragment.ACTIVITY_CREATED;
                dispatch(EVENT_ACTIVITY_CREATED, fragment, saved, null, null, false);
                break;
            }

            case Fragment.ACTIVITY_CREATED:
            {
                fragment.mCalled = false;
                fragment.onStart();
                requireCalled(fragment, "onStart()");

                if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchStart();
                if (fragment.mLoaderManager != null) fragment.mLoaderManager.doStart();

                fragment.mState = Fragment.STARTED;
                dispatch(EVENT_STARTED, fragment, null, null, null, false);
                break;
            }

            case Fragment.STARTED:
            {
                fragment.mCalled = false;
                fragment.onResume();
                requireCalled(fragment, "onResume()");

                if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchResume();

                fragment.mState = Fragment.RESUMED;
                dispatch(EVENT_RESUMED, fragment, null, null, null, false);
                break;
            }

            default:
                fragment.mState = Fragment.RESUMED;
                break;
        }
    }

    private void createView(Fragment fragment, Bundle saved)
    {
        ViewGroup container = null;

        if (fragment.mContainerId != 0)
        {
            View found = findContainer(fragment.mContainerId);

            if (!(found instanceof ViewGroup))
            {
                String name = "unknown";
                try
                {
                    Activity host = host();
                    if (host != null) name = host.getResources().getResourceName(fragment.mContainerId);
                }
                catch (RuntimeException unnamed)
                {
                    name = "unknown";
                }

                throw new IllegalArgumentException("No view found for id 0x" + Integer.toHexString(fragment.mContainerId) + " ("
                                                   + name + ") for fragment " + fragment);
            }

            container = (ViewGroup)found;
        }

        fragment.mContainer = container;

        LayoutInflater inflater = fragment.getLayoutInflater();
        View view = fragment.onCreateView(inflater, container, saved);
        fragment.mView = view;

        if (view == null) return;

        if (container != null)
        {
            if (view.getParent() == null) container.addView(view);
        }

        if (fragment.mHidden) view.setVisibility(View.GONE);

        fragment.onViewCreated(view, saved);
        dispatch(EVENT_VIEW_CREATED, fragment, saved, view, null, false);
    }

    private View findContainer(int id)
    {
        if (mParent != null) return mParent.mView == null ? null : mParent.mView.findViewById(id);

        Activity host = host();
        return host == null ? null : host.findViewById(id);
    }

    private void moveDown(Fragment fragment)
    {
        switch (fragment.mState)
        {
            case Fragment.RESUMED:
            {
                if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchPause();

                fragment.mCalled = false;
                fragment.onPause();
                requireCalled(fragment, "onPause()");

                fragment.mState = Fragment.STARTED;
                dispatch(EVENT_PAUSED, fragment, null, null, null, false);
                break;
            }

            case Fragment.STARTED:
            {
                if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchStop();

                fragment.mCalled = false;
                fragment.onStop();
                requireCalled(fragment, "onStop()");

                if (fragment.mLoaderManager != null) fragment.mLoaderManager.doStop();

                fragment.mState = Fragment.ACTIVITY_CREATED;
                dispatch(EVENT_STOPPED, fragment, null, null, null, false);
                break;
            }

            case Fragment.ACTIVITY_CREATED:
            {
                if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchState(Fragment.CREATED);

                fragment.mCalled = false;
                fragment.onDestroyView();
                requireCalled(fragment, "onDestroyView()");

                if (fragment.mView != null && fragment.mContainer != null) fragment.mContainer.removeView(fragment.mView);

                fragment.mView = null;
                fragment.mContainer = null;
                fragment.mInLayout = false;
                fragment.mState = Fragment.CREATED;
                dispatch(EVENT_VIEW_DESTROYED, fragment, null, null, null, false);
                break;
            }

            case Fragment.CREATED:
            {
                if (fragment.mChildFragmentManager != null) fragment.mChildFragmentManager.dispatchDestroy();

                fragment.mCalled = false;
                fragment.onDestroy();
                requireCalled(fragment, "onDestroy()");

                if (fragment.mLoaderManager != null)
                {
                    fragment.mLoaderManager.doDestroy();
                    fragment.mLoaderManager = null;
                }

                dispatch(EVENT_DESTROYED, fragment, null, null, null, false);

                fragment.mCalled = false;
                fragment.onDetach();
                requireCalled(fragment, "onDetach()");

                dispatch(EVENT_DETACHED, fragment, null, null, null, false);

                fragment.mState = Fragment.INITIALIZING;
                fragment.mActivity = null;
                fragment.mContext = null;
                fragment.mParentFragment = null;
                fragment.mLayoutInflater = null;
                fragment.mChildFragmentManager = null;
                fragment.mFromLayout = false;

                if (fragment.mRemoving || mDestroyed)
                {
                    mActive.remove(fragment);
                    fragment.mFragmentManager = null;
                }

                break;
            }

            default:
                fragment.mState = Fragment.INITIALIZING;
                break;
        }
    }

    private void dispatch(int event, Fragment fragment, Bundle bundle, View view, Context context, boolean onlyRecursive)
    {
        if (mParent != null && mParent.mFragmentManager != null)
        {
            mParent.mFragmentManager.dispatch(event, fragment, bundle, view, context, true);
        }

        for (Registration registration : new ArrayList<>(mLifecycleCallbacks))
        {
            if (onlyRecursive && !registration.mRecursive) continue;

            FragmentLifecycleCallbacks callback = registration.mCallback;

            switch (event)
            {
                case EVENT_PRE_ATTACHED: callback.onFragmentPreAttached(this, fragment, context); break;
                case EVENT_ATTACHED: callback.onFragmentAttached(this, fragment, context); break;
                case EVENT_PRE_CREATED: callback.onFragmentPreCreated(this, fragment, bundle); break;
                case EVENT_CREATED: callback.onFragmentCreated(this, fragment, bundle); break;
                case EVENT_ACTIVITY_CREATED: callback.onFragmentActivityCreated(this, fragment, bundle); break;
                case EVENT_VIEW_CREATED: callback.onFragmentViewCreated(this, fragment, view, bundle); break;
                case EVENT_STARTED: callback.onFragmentStarted(this, fragment); break;
                case EVENT_RESUMED: callback.onFragmentResumed(this, fragment); break;
                case EVENT_PAUSED: callback.onFragmentPaused(this, fragment); break;
                case EVENT_STOPPED: callback.onFragmentStopped(this, fragment); break;
                case EVENT_SAVE_INSTANCE_STATE: callback.onFragmentSaveInstanceState(this, fragment, bundle); break;
                case EVENT_VIEW_DESTROYED: callback.onFragmentViewDestroyed(this, fragment); break;
                case EVENT_DESTROYED: callback.onFragmentDestroyed(this, fragment); break;
                case EVENT_DETACHED: callback.onFragmentDetached(this, fragment); break;
                default: break;
            }
        }
    }

    private void makeActive(Fragment fragment)
    {
        fragment.assignIndex();
        fragment.mFragmentManager = this;
        if (!mActive.contains(fragment)) mActive.add(fragment);
    }

    private void addFragment(Fragment fragment)
    {
        if (mAdded.contains(fragment)) throw new IllegalStateException("Fragment already added: " + fragment);

        makeActive(fragment);

        if (!fragment.mDetached)
        {
            mAdded.add(fragment);
            fragment.mAdded = true;
        }

        fragment.mRemoving = false;

        if (fragment.mHasMenu && fragment.mMenuVisible) invalidateOptionsMenu();

        moveToState(fragment, mState);
    }

    private void removeFragment(Fragment fragment)
    {
        mAdded.remove(fragment);

        if (fragment.mHasMenu && fragment.mMenuVisible && fragment.mAdded) invalidateOptionsMenu();

        fragment.mAdded = false;
        fragment.mRemoving = true;

        moveToState(fragment, Fragment.INITIALIZING);
        mActive.remove(fragment);
    }

    private void hideFragment(Fragment fragment)
    {
        if (fragment.mHidden) return;

        fragment.mHidden = true;
        if (fragment.mView != null) fragment.mView.setVisibility(View.GONE);
        if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) invalidateOptionsMenu();

        fragment.onHiddenChanged(true);
    }

    private void showFragment(Fragment fragment)
    {
        if (!fragment.mHidden) return;

        fragment.mHidden = false;
        if (fragment.mView != null) fragment.mView.setVisibility(View.VISIBLE);
        if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) invalidateOptionsMenu();

        fragment.onHiddenChanged(false);
    }

    private void detachFragment(Fragment fragment)
    {
        if (fragment.mDetached) return;

        fragment.mDetached = true;

        if (fragment.mAdded)
        {
            mAdded.remove(fragment);
            if (fragment.mHasMenu && fragment.mMenuVisible) invalidateOptionsMenu();
            fragment.mAdded = false;
        }

        moveToState(fragment, Fragment.CREATED);
    }

    private void attachFragment(Fragment fragment)
    {
        if (!fragment.mDetached) return;

        fragment.mDetached = false;

        if (!fragment.mAdded)
        {
            if (mAdded.contains(fragment)) throw new IllegalStateException("Fragment already added: " + fragment);

            mAdded.add(fragment);
            fragment.mAdded = true;
            if (fragment.mHasMenu && fragment.mMenuVisible) invalidateOptionsMenu();
        }

        moveToState(fragment, mState);
    }

    private void requireNotSaved()
    {
        if (mStateSaved) throw new IllegalStateException("Can not perform this action after onSaveInstanceState");
    }

    private void enqueue(Runnable action, boolean allowStateLoss)
    {
        if (!allowStateLoss) requireNotSaved();

        if (mDestroyed)
        {
            if (allowStateLoss) return;

            throw new IllegalStateException("Activity has been destroyed");
        }

        mPending.add(action);
        schedule();
    }

    private void schedule()
    {
        if (mScheduled) return;

        mScheduled = true;
        mHandler.post(mExecutePending);
    }

    private void enqueuePop(final String name, final int id, final int flags)
    {
        enqueue(new Runnable()
        {
            @Override
            public void run()
            {
                popBackStackState(name, id, flags);
            }
        }, false);
    }

    boolean execPendingActions()
    {
        if (mExecuting) return false;

        Looper main = Looper.getMainLooper();
        if (main != null && !main.isCurrentThread()) throw new IllegalStateException("Must be called from main thread of fragment host");

        boolean didSomething = false;
        mExecuting = true;

        try
        {
            while (!mPending.isEmpty())
            {
                ArrayList<Runnable> actions = new ArrayList<>(mPending);
                mPending.clear();

                for (Runnable action : actions) action.run();

                didSomething = true;
            }
        }
        finally
        {
            mExecuting = false;
        }

        reportBackStackChanged();

        return didSomething;
    }

    private void reportBackStackChanged()
    {
        if (!mBackStackChanged) return;

        mBackStackChanged = false;

        for (OnBackStackChangedListener listener : new ArrayList<>(mBackStackListeners)) listener.onBackStackChanged();
    }

    private boolean popBackStackState(String name, int id, int flags)
    {
        if (mBackStack.isEmpty()) return false;

        int last = mBackStack.size() - 1;
        int index;

        if (name == null && id < 0 && (flags & POP_BACK_STACK_INCLUSIVE) == 0)
        {
            index = last - 1;
        }
        else if (name == null && id < 0)
        {
            index = -1;
        }
        else
        {
            index = last;

            while (index >= 0 && !mBackStack.get(index).matches(name, id)) index--;

            if (index < 0) return false;

            if ((flags & POP_BACK_STACK_INCLUSIVE) != 0)
            {
                index--;
                while (index >= 0 && mBackStack.get(index).matches(name, id)) index--;
            }
        }

        if (index == last) return false;

        for (int position = last; position > index; position--)
        {
            Record record = mBackStack.remove(position);
            record.popOps();
        }

        mBackStackChanged = true;
        if (!mExecuting) reportBackStackChanged();

        return true;
    }

    boolean handleBackPressed()
    {
        if (mStateSaved || mDestroyed) return false;

        execPendingActions();

        for (int index = mAdded.size() - 1; index >= 0; index--)
        {
            Fragment fragment = mAdded.get(index);
            if (fragment.mChildFragmentManager != null && fragment.mChildFragmentManager.handleBackPressed()) return true;
        }

        return popBackStackState(null, -1, 0);
    }

    private final class Record extends FragmentTransaction implements BackStackEntry, Runnable
    {
        private final ArrayList<Op> mOps = new ArrayList<>();
        private final ArrayList<Runnable> mCommitRunnables = new ArrayList<>();
        private boolean mAddToBackStack;
        private boolean mAllowAddToBackStack = true;
        private boolean mCommitted;
        private int mIndex = -1;
        private String mName;
        private int mBreadCrumbTitleRes;
        private CharSequence mBreadCrumbTitleText;
        private int mBreadCrumbShortTitleRes;
        private CharSequence mBreadCrumbShortTitleText;

        boolean matches(String name, int id)
        {
            return (name != null && name.equals(mName)) || (id >= 0 && id == mIndex);
        }

        private FragmentTransaction addOp(int containerViewId, Fragment fragment, String tag, int command)
        {
            if (fragment == null) throw new NullPointerException("fragment must not be null");

            if (tag != null)
            {
                if (fragment.mTag != null && !tag.equals(fragment.mTag))
                {
                    throw new IllegalStateException("Can't change tag of fragment " + fragment + ": was " + fragment.mTag + " now " + tag);
                }

                fragment.mTag = tag;
            }

            if (containerViewId != 0)
            {
                if (containerViewId == View.NO_ID)
                {
                    throw new IllegalArgumentException("Can't add fragment " + fragment + " with tag " + tag + " to container view with no id");
                }

                if (fragment.mContainerId != 0 && fragment.mContainerId != containerViewId)
                {
                    throw new IllegalStateException("Can't change container ID of fragment " + fragment + ": was " + fragment.mContainerId
                                                    + " now " + containerViewId);
                }

                fragment.mId = containerViewId;
                fragment.mContainerId = containerViewId;
            }

            mOps.add(new Op(command, fragment));
            return this;
        }

        @Override
        public FragmentTransaction add(Fragment fragment, String tag)
        {
            return addOp(0, fragment, tag, OP_ADD);
        }

        @Override
        public FragmentTransaction add(int containerViewId, Fragment fragment)
        {
            return addOp(containerViewId, fragment, null, OP_ADD);
        }

        @Override
        public FragmentTransaction add(int containerViewId, Fragment fragment, String tag)
        {
            return addOp(containerViewId, fragment, tag, OP_ADD);
        }

        @Override
        public FragmentTransaction replace(int containerViewId, Fragment fragment)
        {
            return replace(containerViewId, fragment, null);
        }

        @Override
        public FragmentTransaction replace(int containerViewId, Fragment fragment, String tag)
        {
            if (containerViewId == 0) throw new IllegalArgumentException("Must use non-zero containerViewId");

            return addOp(containerViewId, fragment, tag, OP_REPLACE);
        }

        @Override
        public FragmentTransaction remove(Fragment fragment)
        {
            mOps.add(new Op(OP_REMOVE, fragment));
            return this;
        }

        @Override
        public FragmentTransaction hide(Fragment fragment)
        {
            mOps.add(new Op(OP_HIDE, fragment));
            return this;
        }

        @Override
        public FragmentTransaction show(Fragment fragment)
        {
            mOps.add(new Op(OP_SHOW, fragment));
            return this;
        }

        @Override
        public FragmentTransaction detach(Fragment fragment)
        {
            mOps.add(new Op(OP_DETACH, fragment));
            return this;
        }

        @Override
        public FragmentTransaction attach(Fragment fragment)
        {
            mOps.add(new Op(OP_ATTACH, fragment));
            return this;
        }

        @Override
        public FragmentTransaction setPrimaryNavigationFragment(Fragment fragment)
        {
            mOps.add(new Op(OP_SET_PRIMARY, fragment));
            return this;
        }

        @Override
        public boolean isEmpty()
        {
            return mOps.isEmpty();
        }

        @Override
        public FragmentTransaction setCustomAnimations(int enter, int exit)
        {
            return this;
        }

        @Override
        public FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit)
        {
            return this;
        }

        @Override
        public FragmentTransaction setTransition(int transit)
        {
            return this;
        }

        @Override
        public FragmentTransaction addSharedElement(View sharedElement, String name)
        {
            return this;
        }

        @Override
        public FragmentTransaction setTransitionStyle(int styleRes)
        {
            return this;
        }

        @Override
        public FragmentTransaction addToBackStack(String name)
        {
            if (!mAllowAddToBackStack) throw new IllegalStateException("This FragmentTransaction is not allowed to be added to the back stack.");

            mAddToBackStack = true;
            mName = name;
            return this;
        }

        @Override
        public boolean isAddToBackStackAllowed()
        {
            return mAllowAddToBackStack;
        }

        @Override
        public FragmentTransaction disallowAddToBackStack()
        {
            if (mAddToBackStack) throw new IllegalStateException("This transaction is already being added to the back stack");

            mAllowAddToBackStack = false;
            return this;
        }

        @Override
        public FragmentTransaction setBreadCrumbTitle(int res)
        {
            mBreadCrumbTitleRes = res;
            mBreadCrumbTitleText = null;
            return this;
        }

        @Override
        public FragmentTransaction setBreadCrumbTitle(CharSequence text)
        {
            mBreadCrumbTitleRes = 0;
            mBreadCrumbTitleText = text;
            return this;
        }

        @Override
        public FragmentTransaction setBreadCrumbShortTitle(int res)
        {
            mBreadCrumbShortTitleRes = res;
            mBreadCrumbShortTitleText = null;
            return this;
        }

        @Override
        public FragmentTransaction setBreadCrumbShortTitle(CharSequence text)
        {
            mBreadCrumbShortTitleRes = 0;
            mBreadCrumbShortTitleText = text;
            return this;
        }

        @Override
        public FragmentTransaction setReorderingAllowed(boolean reorderingAllowed)
        {
            return this;
        }

        @Override
        public FragmentTransaction runOnCommit(Runnable runnable)
        {
            if (runnable == null) throw new IllegalArgumentException("runnable cannot be null");

            disallowAddToBackStack();
            mCommitRunnables.add(runnable);
            return this;
        }

        private int commitInternal(boolean allowStateLoss)
        {
            if (mCommitted) throw new IllegalStateException("commit already called");

            mCommitted = true;
            mIndex = mAddToBackStack ? mNextBackStackIndex++ : -1;
            enqueue(this, allowStateLoss);
            return mIndex;
        }

        @Override
        public int commit()
        {
            return commitInternal(false);
        }

        @Override
        public int commitAllowingStateLoss()
        {
            return commitInternal(true);
        }

        private void commitNowInternal(boolean allowStateLoss)
        {
            disallowAddToBackStack();

            if (mCommitted) throw new IllegalStateException("commit already called");
            if (!allowStateLoss) requireNotSaved();
            if (mDestroyed) throw new IllegalStateException("FragmentManager has been destroyed");

            mCommitted = true;

            if (mExecuting)
            {
                run();
                return;
            }

            execPendingActions();
            mExecuting = true;
            try
            {
                run();
            }
            finally
            {
                mExecuting = false;
            }
        }

        @Override
        public void commitNow()
        {
            commitNowInternal(false);
        }

        @Override
        public void commitNowAllowingStateLoss()
        {
            commitNowInternal(true);
        }

        @Override
        public void run()
        {
            for (Op op : mOps)
            {
                Fragment fragment = op.mFragment;

                switch (op.mCommand)
                {
                    case OP_ADD:
                        addFragment(fragment);
                        break;

                    case OP_REPLACE:
                    {
                        op.mRemoved.clear();

                        for (Fragment existing : new ArrayList<>(mAdded))
                        {
                            if (existing == fragment || existing.mContainerId != fragment.mContainerId) continue;

                            op.mRemoved.add(existing);
                            if (existing == mPrimaryNavigation) mPrimaryNavigation = null;
                            removeFragment(existing);
                        }

                        if (!mAdded.contains(fragment)) addFragment(fragment);
                        break;
                    }

                    case OP_REMOVE:
                        removeFragment(fragment);
                        break;

                    case OP_HIDE:
                        hideFragment(fragment);
                        break;

                    case OP_SHOW:
                        showFragment(fragment);
                        break;

                    case OP_DETACH:
                        detachFragment(fragment);
                        break;

                    case OP_ATTACH:
                        attachFragment(fragment);
                        break;

                    case OP_SET_PRIMARY:
                        op.mPreviousPrimary = mPrimaryNavigation;
                        mPrimaryNavigation = fragment;
                        break;

                    default:
                        break;
                }
            }

            if (mAddToBackStack)
            {
                mBackStack.add(this);
                mBackStackChanged = true;
            }

            for (Runnable runnable : mCommitRunnables) runnable.run();
            mCommitRunnables.clear();
        }

        void popOps()
        {
            for (int index = mOps.size() - 1; index >= 0; index--)
            {
                Op op = mOps.get(index);
                Fragment fragment = op.mFragment;

                switch (op.mCommand)
                {
                    case OP_ADD:
                        removeFragment(fragment);
                        break;

                    case OP_REPLACE:
                        removeFragment(fragment);
                        for (Fragment removed : op.mRemoved) addFragment(removed);
                        break;

                    case OP_REMOVE:
                        addFragment(fragment);
                        break;

                    case OP_HIDE:
                        showFragment(fragment);
                        break;

                    case OP_SHOW:
                        hideFragment(fragment);
                        break;

                    case OP_DETACH:
                        attachFragment(fragment);
                        break;

                    case OP_ATTACH:
                        detachFragment(fragment);
                        break;

                    case OP_SET_PRIMARY:
                        mPrimaryNavigation = op.mPreviousPrimary;
                        break;

                    default:
                        break;
                }
            }
        }

        @Override
        public int getId()
        {
            return mIndex;
        }

        @Override
        public String getName()
        {
            return mName;
        }

        @Override
        public int getBreadCrumbTitleRes()
        {
            return mBreadCrumbTitleRes;
        }

        @Override
        public int getBreadCrumbShortTitleRes()
        {
            return mBreadCrumbShortTitleRes;
        }

        @Override
        public CharSequence getBreadCrumbTitle()
        {
            Activity host = host();
            if (mBreadCrumbTitleRes != 0 && host != null) return host.getText(mBreadCrumbTitleRes);

            return mBreadCrumbTitleText;
        }

        @Override
        public CharSequence getBreadCrumbShortTitle()
        {
            Activity host = host();
            if (mBreadCrumbShortTitleRes != 0 && host != null) return host.getText(mBreadCrumbShortTitleRes);

            return mBreadCrumbShortTitleText;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(128);
            builder.append("BackStackEntry{").append(Integer.toHexString(System.identityHashCode(this)));

            if (mIndex >= 0) builder.append(" #").append(mIndex);
            if (mName != null) builder.append(' ').append(mName);

            return builder.append('}').toString();
        }
    }
}
