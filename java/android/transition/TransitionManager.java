package android.transition;

import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

public class TransitionManager
{
    private static final Transition sDefaultTransition = new AutoTransition();
    private static final ArrayList<ViewGroup> sPendingTransitions = new ArrayList<>();
    private static Handler sHandler;

    private final HashMap<Scene, Transition> mSceneTransitions = new HashMap<>();
    private final HashMap<Scene, HashMap<Scene, Transition>> mScenePairTransitions = new HashMap<>();

    public TransitionManager()
    {
    }

    public void setTransition(Scene scene, Transition transition)
    {
        mSceneTransitions.put(scene, transition);
    }

    public void setTransition(Scene fromScene, Scene toScene, Transition transition)
    {
        HashMap<Scene, Transition> sceneTransitionMap = mScenePairTransitions.get(toScene);
        if (sceneTransitionMap == null)
        {
            sceneTransitionMap = new HashMap<>();
            mScenePairTransitions.put(toScene, sceneTransitionMap);
        }

        sceneTransitionMap.put(fromScene, transition);
    }

    private Transition getTransition(Scene scene)
    {
        ViewGroup sceneRoot = scene.getSceneRoot();

        if (sceneRoot != null)
        {
            Scene currentScene = Scene.getCurrentScene(sceneRoot);
            if (currentScene != null)
            {
                HashMap<Scene, Transition> sceneTransitionMap = mScenePairTransitions.get(scene);
                if (sceneTransitionMap != null)
                {
                    Transition transition = sceneTransitionMap.get(currentScene);
                    if (transition != null) return transition;
                }
            }
        }

        Transition transition = mSceneTransitions.get(scene);
        return transition != null ? transition : sDefaultTransition;
    }

    public void transitionTo(Scene scene)
    {
        changeScene(scene, getTransition(scene));
    }

    public static void go(Scene scene)
    {
        changeScene(scene, sDefaultTransition);
    }

    public static void go(Scene scene, Transition transition)
    {
        changeScene(scene, transition);
    }

    private static void changeScene(Scene scene, Transition transition)
    {
        if (scene == null) return;

        ViewGroup sceneRoot = scene.getSceneRoot();
        if (sceneRoot != null && sPendingTransitions.contains(sceneRoot)) return;

        Transition transitionClone = transition == null ? null : transition.clone();

        Scene previousScene = Scene.getCurrentScene(sceneRoot);
        if (previousScene != null) previousScene.exit();

        if (transitionClone != null) transitionClone.playStart();

        scene.enter();

        if (transitionClone != null) finishLater(sceneRoot, transitionClone);
    }

    public static void beginDelayedTransition(ViewGroup sceneRoot)
    {
        beginDelayedTransition(sceneRoot, null);
    }

    public static void beginDelayedTransition(ViewGroup sceneRoot, Transition transition)
    {
        if (sceneRoot == null || sPendingTransitions.contains(sceneRoot)) return;

        sPendingTransitions.add(sceneRoot);

        if (transition == null) transition = sDefaultTransition;
        Transition transitionClone = transition.clone();

        Scene.setCurrentScene(sceneRoot, null);
        transitionClone.playStart();
        finishLater(sceneRoot, transitionClone);
    }

    private static void finishLater(final ViewGroup sceneRoot, final Transition transition)
    {
        Runnable finish = new Runnable()
        {
            @Override
            public void run()
            {
                if (sceneRoot != null) sPendingTransitions.remove(sceneRoot);

                transition.playEnd();
            }
        };

        if (sceneRoot != null && sceneRoot.post(finish)) return;

        Looper looper = Looper.getMainLooper();
        if (looper == null)
        {
            finish.run();
            return;
        }

        if (sHandler == null) sHandler = new Handler(looper);
        sHandler.post(finish);
    }

    public static void endTransitions(ViewGroup sceneRoot)
    {
        if (sceneRoot == null) return;

        sPendingTransitions.remove(sceneRoot);
    }
}
