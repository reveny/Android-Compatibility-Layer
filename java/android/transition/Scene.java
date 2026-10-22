package android.transition;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.WeakHashMap;

public final class Scene
{
    private static final WeakHashMap<ViewGroup, Scene> sCurrentScenes = new WeakHashMap<>();
    private static final WeakHashMap<ViewGroup, SparseArray<Scene>> sLayoutScenes = new WeakHashMap<>();

    private Context mContext;
    private int mLayoutId = -1;
    private ViewGroup mSceneRoot;
    private View mLayout;
    private Runnable mEnterAction;
    private Runnable mExitAction;

    public Scene(ViewGroup sceneRoot)
    {
        mSceneRoot = sceneRoot;
    }

    private Scene(ViewGroup sceneRoot, int layoutId, Context context)
    {
        mContext = context;
        mSceneRoot = sceneRoot;
        mLayoutId = layoutId;
    }

    public Scene(ViewGroup sceneRoot, View layout)
    {
        mSceneRoot = sceneRoot;
        mLayout = layout;
    }

    @Deprecated
    public Scene(ViewGroup sceneRoot, ViewGroup layout)
    {
        mSceneRoot = sceneRoot;
        mLayout = layout;
    }

    public static Scene getSceneForLayout(ViewGroup sceneRoot, int layoutId, Context context)
    {
        synchronized (sLayoutScenes)
        {
            SparseArray<Scene> scenes = sLayoutScenes.get(sceneRoot);
            if (scenes == null)
            {
                scenes = new SparseArray<>();
                sLayoutScenes.put(sceneRoot, scenes);
            }

            Scene scene = scenes.get(layoutId);
            if (scene != null) return scene;

            scene = new Scene(sceneRoot, layoutId, context);
            scenes.put(layoutId, scene);
            return scene;
        }
    }

    public ViewGroup getSceneRoot()
    {
        return mSceneRoot;
    }

    public void exit()
    {
        if (getCurrentScene(mSceneRoot) == this && mExitAction != null) mExitAction.run();
    }

    public void enter()
    {
        if (mLayoutId > 0 || mLayout != null)
        {
            getSceneRoot().removeAllViews();

            if (mLayoutId > 0) LayoutInflater.from(mContext).inflate(mLayoutId, mSceneRoot);
            else mSceneRoot.addView(mLayout);
        }

        if (mEnterAction != null) mEnterAction.run();

        setCurrentScene(mSceneRoot, this);
    }

    static void setCurrentScene(ViewGroup sceneRoot, Scene scene)
    {
        if (sceneRoot == null) return;

        synchronized (sCurrentScenes)
        {
            if (scene == null) sCurrentScenes.remove(sceneRoot);
            else sCurrentScenes.put(sceneRoot, scene);
        }
    }

    public static Scene getCurrentScene(ViewGroup sceneRoot)
    {
        if (sceneRoot == null) return null;

        synchronized (sCurrentScenes)
        {
            return sCurrentScenes.get(sceneRoot);
        }
    }

    public void setEnterAction(Runnable action)
    {
        mEnterAction = action;
    }

    public void setExitAction(Runnable action)
    {
        mExitAction = action;
    }

    boolean isCreatedFromLayoutResource()
    {
        return mLayoutId > 0;
    }
}
