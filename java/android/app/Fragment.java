package android.app;

import android.animation.Animator;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.transition.Transition;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class Fragment implements ComponentCallbacks2, View.OnCreateContextMenuListener
{
    static final int INITIALIZING = 0;
    static final int CREATED = 1;
    static final int ACTIVITY_CREATED = 2;
    static final int STARTED = 3;
    static final int RESUMED = 4;

    private static int sNextIndex;

    int mState = INITIALIZING;
    int mIndex = -1;
    Activity mActivity;
    Context mContext;
    View mView;
    ViewGroup mContainer;
    Bundle mArguments;
    Bundle mSavedFragmentState;
    String mTag;
    int mId;
    int mContainerId;
    boolean mAdded;
    boolean mDetached;
    boolean mHidden;
    boolean mRemoving;
    boolean mInLayout;
    boolean mFromLayout;
    boolean mRetainInstance;
    boolean mHasMenu;
    boolean mMenuVisible = true;
    boolean mUserVisibleHint = true;
    boolean mCalled;
    Fragment mTarget;
    int mTargetRequestCode;
    Fragment mParentFragment;
    RuntimeFragmentManager mFragmentManager;
    RuntimeFragmentManager mChildFragmentManager;
    RuntimeLoaderManager mLoaderManager;
    LayoutInflater mLayoutInflater;

    private Transition mEnterTransition;
    private Transition mReturnTransition;
    private Transition mExitTransition;
    private Transition mReenterTransition;
    private Transition mSharedElementEnterTransition;
    private Transition mSharedElementReturnTransition;
    private boolean mReturnTransitionSet;
    private boolean mReenterTransitionSet;
    private boolean mSharedElementReturnTransitionSet;
    private Boolean mAllowEnterTransitionOverlap;
    private Boolean mAllowReturnTransitionOverlap;
    private SharedElementCallback mEnterSharedElementCallback;
    private SharedElementCallback mExitSharedElementCallback;

    public static class SavedState implements Parcelable
    {
        final Bundle mState;

        SavedState(Bundle state)
        {
            mState = state;
        }

        SavedState(Parcel in, ClassLoader loader)
        {
            mState = in.readBundle(loader);
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeBundle(mState);
        }

        public static final Parcelable.ClassLoaderCreator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>()
        {
            @Override
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in, null);
            }

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader)
            {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }

    public static class InstantiationException extends android.util.AndroidRuntimeException
    {
        public InstantiationException(String message, Exception cause)
        {
            super(message, cause);
        }

        InstantiationException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    public Fragment()
    {
    }

    public static Fragment instantiate(Context context, String className)
    {
        return instantiate(context, className, null);
    }

    public static Fragment instantiate(Context context, String className, Bundle arguments)
    {
        try
        {
            ClassLoader loader = context == null ? Fragment.class.getClassLoader() : context.getClassLoader();
            Class<?> type = loader.loadClass(className);

            if (!Fragment.class.isAssignableFrom(type))
            {
                throw new InstantiationException("Trying to instantiate a class " + className + " that is not a Fragment",
                                                 new ClassCastException());
            }

            Fragment fragment = (Fragment)type.getConstructor().newInstance();

            if (arguments != null)
            {
                arguments.setClassLoader(fragment.getClass().getClassLoader());
                fragment.setArguments(arguments);
            }

            return fragment;
        }
        catch (ReflectiveOperationException failure)
        {
            throw new InstantiationException("Unable to instantiate fragment " + className
                                                 + ": make sure class name exists, is public, and has an empty constructor that is public",
                                             failure);
        }
    }

    void assignIndex()
    {
        if (mIndex < 0) mIndex = ++sNextIndex;
    }

    @Override
    public final boolean equals(Object other)
    {
        return super.equals(other);
    }

    @Override
    public final int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);
        builder.append(getClass().getSimpleName()).append('{').append(Integer.toHexString(System.identityHashCode(this)));

        if (mIndex >= 0) builder.append(" #").append(mIndex);
        if (mId != 0) builder.append(" id=0x").append(Integer.toHexString(mId));
        if (mTag != null) builder.append(' ').append(mTag);

        return builder.append('}').toString();
    }

    public final int getId()
    {
        return mId;
    }

    public void setId(int id)
    {
        mId = id;
    }

    public final String getTag()
    {
        return mTag;
    }

    public void setTag(String tag)
    {
        mTag = tag;
    }

    public void setArguments(Bundle arguments)
    {
        if (mIndex >= 0 && isStateSaved()) throw new IllegalStateException("Fragment already active");

        mArguments = arguments;
    }

    public final Bundle getArguments()
    {
        return mArguments;
    }

    public final boolean isStateSaved()
    {
        return mFragmentManager != null && mFragmentManager.isStateSaved();
    }

    public void setInitialSavedState(SavedState state)
    {
        if (mIndex >= 0) throw new IllegalStateException("Fragment already active");

        mSavedFragmentState = state != null && state.mState != null ? state.mState : null;
    }

    public void setTargetFragment(Fragment fragment, int requestCode)
    {
        if (fragment != null)
        {
            for (Fragment check = fragment; check != null; check = check.mTarget)
            {
                if (check == this)
                {
                    throw new IllegalArgumentException("Setting " + fragment + " as the target of " + this
                                                       + " would create a target cycle");
                }
            }
        }

        mTarget = fragment;
        mTargetRequestCode = requestCode;
    }

    public final Fragment getTargetFragment()
    {
        return mTarget;
    }

    public final int getTargetRequestCode()
    {
        return mTargetRequestCode;
    }

    public Context getContext()
    {
        return mContext != null ? mContext : mActivity;
    }

    public final Activity getActivity()
    {
        return mActivity;
    }

    public final Object getHost()
    {
        return mActivity;
    }

    public final android.content.res.Resources getResources()
    {
        Context context = getContext();

        if (context == null) throw new IllegalStateException("Fragment " + this + " not attached to Activity");

        return context.getResources();
    }

    public final CharSequence getText(int resourceId)
    {
        return getResources().getText(resourceId);
    }

    public final String getString(int resourceId)
    {
        return getResources().getString(resourceId);
    }

    public final String getString(int resourceId, Object... formatArgs)
    {
        return getResources().getString(resourceId, formatArgs);
    }

    public final FragmentManager getFragmentManager()
    {
        return mFragmentManager;
    }

    public final FragmentManager getChildFragmentManager()
    {
        if (mChildFragmentManager == null)
        {
            mChildFragmentManager = new RuntimeFragmentManager(mActivity, this);
            mChildFragmentManager.dispatchState(mState);
        }

        return mChildFragmentManager;
    }

    public final Fragment getParentFragment()
    {
        return mParentFragment;
    }

    public final boolean isAdded()
    {
        return mActivity != null && mAdded;
    }

    public final boolean isDetached()
    {
        return mDetached;
    }

    public final boolean isRemoving()
    {
        return mRemoving;
    }

    public final boolean isInLayout()
    {
        return mInLayout;
    }

    public final boolean isResumed()
    {
        return mState >= RESUMED;
    }

    public final boolean isVisible()
    {
        return isAdded() && !isHidden() && mView != null && mView.getVisibility() == View.VISIBLE;
    }

    public final boolean isHidden()
    {
        return mHidden;
    }

    public void onHiddenChanged(boolean hidden)
    {
    }

    public void setRetainInstance(boolean retain)
    {
        if (retain && mParentFragment != null) throw new IllegalStateException("Can't retain fragments that are nested in other fragments");

        mRetainInstance = retain;
    }

    public final boolean getRetainInstance()
    {
        return mRetainInstance;
    }

    public void setHasOptionsMenu(boolean hasMenu)
    {
        if (mHasMenu == hasMenu) return;

        mHasMenu = hasMenu;
        if (isAdded() && !isHidden() && mFragmentManager != null) mFragmentManager.invalidateOptionsMenu();
    }

    public void setMenuVisibility(boolean menuVisible)
    {
        if (mMenuVisible == menuVisible) return;

        mMenuVisible = menuVisible;
        if (mHasMenu && isAdded() && !isHidden() && mFragmentManager != null) mFragmentManager.invalidateOptionsMenu();
    }

    public void setUserVisibleHint(boolean isVisibleToUser)
    {
        mUserVisibleHint = isVisibleToUser;
    }

    public boolean getUserVisibleHint()
    {
        return mUserVisibleHint;
    }

    public LoaderManager getLoaderManager()
    {
        if (mLoaderManager == null) mLoaderManager = new RuntimeLoaderManager(toString(), mState >= STARTED);

        return mLoaderManager;
    }

    private Activity requireHost()
    {
        if (mActivity == null) throw new IllegalStateException("Fragment " + this + " not attached to Activity");

        return mActivity;
    }

    public void startActivity(Intent intent)
    {
        startActivity(intent, null);
    }

    public void startActivity(Intent intent, Bundle options)
    {
        requireHost().startActivityFromFragment(this, intent, -1, options);
    }

    public void startActivityForResult(Intent intent, int requestCode)
    {
        startActivityForResult(intent, requestCode, null);
    }

    public void startActivityForResult(Intent intent, int requestCode, Bundle options)
    {
        requireHost().startActivityFromFragment(this, intent, requestCode, options);
    }

    public void startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues,
                                           int extraFlags, Bundle options) throws IntentSender.SendIntentException
    {
        requireHost().startIntentSenderFromFragment(this, intent, requestCode, fillInIntent, flagsMask, flagsValues);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    }

    public final void requestPermissions(String[] permissions, int requestCode)
    {
        requireHost().requestPermissionsFromFragment(this, permissions, requestCode);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
    }

    public boolean shouldShowRequestPermissionRationale(String permission)
    {
        return mActivity != null && mActivity.shouldShowRequestPermissionRationale(permission);
    }

    public LayoutInflater onGetLayoutInflater(Bundle savedInstanceState)
    {
        return requireHost().getLayoutInflater().cloneInContext(mActivity);
    }

    public final LayoutInflater getLayoutInflater()
    {
        if (mLayoutInflater == null) mLayoutInflater = onGetLayoutInflater(null);

        return mLayoutInflater;
    }

    public void onInflate(AttributeSet attrs, Bundle savedInstanceState)
    {
        mCalled = true;
    }

    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState)
    {
        onInflate(attrs, savedInstanceState);
        mCalled = true;

        if (mActivity != null) onInflate(mActivity, attrs, savedInstanceState);
    }

    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState)
    {
        mCalled = true;
    }

    public void onAttachFragment(Fragment childFragment)
    {
    }

    public void onAttach(Context context)
    {
        mCalled = true;

        if (mActivity != null)
        {
            mCalled = false;
            onAttach(mActivity);
        }
    }

    public void onAttach(Activity activity)
    {
        mCalled = true;
    }

    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim)
    {
        return null;
    }

    public void onCreate(Bundle savedInstanceState)
    {
        mCalled = true;

        Bundle children = savedInstanceState == null ? null : savedInstanceState.getBundle(RuntimeFragmentManager.SAVED_FRAGMENTS);
        if (children == null) return;

        getChildFragmentManager();
        mChildFragmentManager.restoreAllState(children);
        mChildFragmentManager.dispatchCreate();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return null;
    }

    public void onViewCreated(View view, Bundle savedInstanceState)
    {
    }

    public View getView()
    {
        return mView;
    }

    public void setView(View view)
    {
        mView = view;
    }

    public void onActivityCreated(Bundle savedInstanceState)
    {
        mCalled = true;
    }

    public void onViewStateRestored(Bundle savedInstanceState)
    {
        mCalled = true;
    }

    public void onStart()
    {
        mCalled = true;
    }

    public void onResume()
    {
        mCalled = true;
    }

    public void onSaveInstanceState(Bundle outState)
    {
    }

    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig)
    {
        onMultiWindowModeChanged(isInMultiWindowMode);
    }

    public void onMultiWindowModeChanged(boolean isInMultiWindowMode)
    {
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig)
    {
        onPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode)
    {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        mCalled = true;
    }

    public void onPause()
    {
        mCalled = true;
    }

    public void onStop()
    {
        mCalled = true;
    }

    @Override
    public void onLowMemory()
    {
        mCalled = true;
    }

    @Override
    public void onTrimMemory(int level)
    {
        mCalled = true;
    }

    public void onDestroyView()
    {
        mCalled = true;
    }

    public void onDestroy()
    {
        mCalled = true;
    }

    public void onDetach()
    {
        mCalled = true;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    }

    public void onPrepareOptionsMenu(Menu menu)
    {
    }

    public void onDestroyOptionsMenu()
    {
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        return false;
    }

    public void onOptionsMenuClosed(Menu menu)
    {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        if (mActivity != null) mActivity.onCreateContextMenu(menu, view, menuInfo);
    }

    public void registerForContextMenu(View view)
    {
        view.setOnCreateContextMenuListener(this);
    }

    public void unregisterForContextMenu(View view)
    {
        view.setOnCreateContextMenuListener(null);
    }

    public boolean onContextItemSelected(MenuItem item)
    {
        return false;
    }

    public void setEnterSharedElementCallback(SharedElementCallback callback)
    {
        mEnterSharedElementCallback = callback;
    }

    public void setExitSharedElementCallback(SharedElementCallback callback)
    {
        mExitSharedElementCallback = callback;
    }

    public void setEnterTransition(Transition transition)
    {
        mEnterTransition = transition;
    }

    public Transition getEnterTransition()
    {
        return mEnterTransition;
    }

    public void setReturnTransition(Transition transition)
    {
        mReturnTransition = transition;
        mReturnTransitionSet = true;
    }

    public Transition getReturnTransition()
    {
        return mReturnTransitionSet ? mReturnTransition : getEnterTransition();
    }

    public void setExitTransition(Transition transition)
    {
        mExitTransition = transition;
    }

    public Transition getExitTransition()
    {
        return mExitTransition;
    }

    public void setReenterTransition(Transition transition)
    {
        mReenterTransition = transition;
        mReenterTransitionSet = true;
    }

    public Transition getReenterTransition()
    {
        return mReenterTransitionSet ? mReenterTransition : getExitTransition();
    }

    public void setSharedElementEnterTransition(Transition transition)
    {
        mSharedElementEnterTransition = transition;
    }

    public Transition getSharedElementEnterTransition()
    {
        return mSharedElementEnterTransition;
    }

    public void setSharedElementReturnTransition(Transition transition)
    {
        mSharedElementReturnTransition = transition;
        mSharedElementReturnTransitionSet = true;
    }

    public Transition getSharedElementReturnTransition()
    {
        return mSharedElementReturnTransitionSet ? mSharedElementReturnTransition : getSharedElementEnterTransition();
    }

    public void setAllowEnterTransitionOverlap(boolean allow)
    {
        mAllowEnterTransitionOverlap = allow;
    }

    public boolean getAllowEnterTransitionOverlap()
    {
        return mAllowEnterTransitionOverlap == null || mAllowEnterTransitionOverlap;
    }

    public void setAllowReturnTransitionOverlap(boolean allow)
    {
        mAllowReturnTransitionOverlap = allow;
    }

    public boolean getAllowReturnTransitionOverlap()
    {
        return mAllowReturnTransitionOverlap == null || mAllowReturnTransitionOverlap;
    }

    public void postponeEnterTransition()
    {
    }

    public void startPostponedEnterTransition()
    {
    }

    public void attachTo(Activity activity, Context context)
    {
        mActivity = activity;
        mContext = context != null ? context : activity;
        mInLayout = true;
        mFromLayout = true;

        if (activity != null && activity.getFragmentManager() instanceof RuntimeFragmentManager)
        {
            ((RuntimeFragmentManager)activity.getFragmentManager()).adoptInflated(this);
        }
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args)
    {
        writer.print(prefix);
        writer.print("mFragmentId=#");
        writer.print(Integer.toHexString(mId));
        writer.print(" mContainerId=#");
        writer.print(Integer.toHexString(mContainerId));
        writer.print(" mTag=");
        writer.println(mTag);

        writer.print(prefix);
        writer.print("mState=");
        writer.print(mState);
        writer.print(" mIndex=");
        writer.print(mIndex);
        writer.print(" mAdded=");
        writer.print(mAdded);
        writer.print(" mRemoving=");
        writer.print(mRemoving);
        writer.print(" mFromLayout=");
        writer.print(mFromLayout);
        writer.print(" mInLayout=");
        writer.println(mInLayout);

        writer.print(prefix);
        writer.print("mHidden=");
        writer.print(mHidden);
        writer.print(" mDetached=");
        writer.print(mDetached);
        writer.print(" mMenuVisible=");
        writer.print(mMenuVisible);
        writer.print(" mHasMenu=");
        writer.println(mHasMenu);

        writer.print(prefix);
        writer.print("mRetainInstance=");
        writer.print(mRetainInstance);
        writer.print(" mUserVisibleHint=");
        writer.println(mUserVisibleHint);

        if (mArguments != null)
        {
            writer.print(prefix);
            writer.print("mArguments=");
            writer.println(mArguments);
        }

        if (mTarget != null)
        {
            writer.print(prefix);
            writer.print("mTarget=");
            writer.print(mTarget);
            writer.print(" mTargetRequestCode=");
            writer.println(mTargetRequestCode);
        }

        if (mView != null)
        {
            writer.print(prefix);
            writer.print("mView=");
            writer.println(mView);
        }

        if (mLoaderManager != null)
        {
            writer.print(prefix);
            writer.println("Loader Manager:");
            mLoaderManager.dump(prefix + "  ", fd, writer, args);
        }

        if (mChildFragmentManager != null)
        {
            writer.print(prefix);
            writer.println("Child " + mChildFragmentManager + ":");
            mChildFragmentManager.dump(prefix + "  ", fd, writer, args);
        }
    }

    SharedElementCallback enterSharedElementCallback()
    {
        return mEnterSharedElementCallback;
    }

    SharedElementCallback exitSharedElementCallback()
    {
        return mExitSharedElementCallback;
    }
}
