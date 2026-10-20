package android.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Fragment
{
    private Activity mActivity;
    private Context mContext;
    private View mView;
    private Bundle mArguments;
    private String mTag;
    private int mId;
    private boolean mAdded;
    private boolean mDetached;

    public Fragment()
    {
    }

    public void onAttach(Context context)
    {
        mContext = context;
        mAdded = true;
    }

    public void onCreate(Bundle savedInstanceState)
    {
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return null;
    }

    public void onViewCreated(View view, Bundle savedInstanceState)
    {
    }

    public void onActivityCreated(Bundle savedInstanceState)
    {
    }

    public void onStart()
    {
    }

    public void onResume()
    {
    }

    public void onPause()
    {
    }

    public void onStop()
    {
    }

    public void onDestroyView()
    {
    }

    public void onDestroy()
    {
    }

    public void onDetach()
    {
        mAdded = false;
        mContext = null;
        mActivity = null;
    }

    public void onSaveInstanceState(Bundle outState)
    {
    }

    public void onConfigurationChanged(android.content.res.Configuration configuration)
    {
    }

    public Activity getActivity()
    {
        return mActivity;
    }

    public Context getContext()
    {
        return mContext != null ? mContext : mActivity;
    }

    public void attachTo(Activity activity, Context context)
    {
        mActivity = activity;
        mContext = context != null ? context : activity;
    }

    public View getView()
    {
        return mView;
    }

    public void setView(View view)
    {
        mView = view;
    }

    public Bundle getArguments()
    {
        return mArguments;
    }

    public void setArguments(Bundle arguments)
    {
        mArguments = arguments;
    }

    public String getTag()
    {
        return mTag;
    }

    public void setTag(String tag)
    {
        mTag = tag;
    }

    public int getId()
    {
        return mId;
    }

    public void setId(int id)
    {
        mId = id;
    }

    public boolean isAdded()
    {
        return mAdded && mActivity != null;
    }

    public boolean isDetached()
    {
        return mDetached;
    }

    public boolean isVisible()
    {
        return mAdded && mView != null && mView.getVisibility() == View.VISIBLE;
    }

    public boolean isRemoving()
    {
        return false;
    }

    public boolean isResumed()
    {
        return mAdded;
    }

    public final android.content.res.Resources getResources()
    {
        Context context = getContext();

        if (context == null) throw new IllegalStateException("this fragment is not attached to a context");

        return context.getResources();
    }

    public final String getString(int resourceId)
    {
        return getResources().getString(resourceId);
    }

    public static Fragment instantiate(Context context, String className)
    {
        return instantiate(context, className, null);
    }

    public static Fragment instantiate(Context context, String className, Bundle arguments)
    {
        try
        {
            Class<?> type = context.getClassLoader().loadClass(className);
            Fragment fragment = (Fragment)type.getConstructor().newInstance();

            if (arguments != null) fragment.setArguments(arguments);

            return fragment;
        }
        catch (ReflectiveOperationException failure)
        {
            throw new InstantiationException("could not instantiate fragment " + className, failure);
        }
    }

    public static class InstantiationException extends RuntimeException
    {
        public InstantiationException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

}
