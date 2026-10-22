package android.view;

import java.util.ArrayList;
import java.util.List;

public final class WindowManagerGlobal
{
    private static WindowManagerGlobal sDefaultWindowManager;

    private WindowManagerGlobal()
    {
    }

    public static WindowManagerGlobal getInstance()
    {
        synchronized (WindowManagerGlobal.class)
        {
            if (sDefaultWindowManager == null) sDefaultWindowManager = new WindowManagerGlobal();

            return sDefaultWindowManager;
        }
    }

    public static void initialize()
    {
        getInstance();
    }

    private static List<View> windowViews()
    {
        ViewRootImpl root = ViewRootImpl.active();
        return root == null ? new ArrayList<View>() : root.windowViews();
    }

    private static String nameOf(View view, int index)
    {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        CharSequence title = params instanceof WindowManager.LayoutParams ? ((WindowManager.LayoutParams)params).getTitle() : null;

        String name = title == null || title.length() == 0 ? view.getClass().getName() : title.toString();
        return name + "/" + Integer.toHexString(System.identityHashCode(view)) + "#" + index;
    }

    public String[] getViewRootNames()
    {
        List<View> views = windowViews();
        String[] names = new String[views.size()];

        for (int index = 0; index < names.length; index++)
        {
            names[index] = nameOf(views.get(index), index);
        }

        return names;
    }

    public View getRootView(String name)
    {
        List<View> views = windowViews();

        for (int index = 0; index < views.size(); index++)
        {
            if (nameOf(views.get(index), index).equals(name)) return views.get(index);
        }

        return null;
    }

    public ArrayList<View> getWindowViews()
    {
        return new ArrayList<>(windowViews());
    }

    public ArrayList<ViewRootImpl> getRootViews()
    {
        ArrayList<ViewRootImpl> roots = new ArrayList<>();
        ViewRootImpl root = ViewRootImpl.active();
        if (root != null) roots.add(root);
        return roots;
    }

    public ArrayList<WindowManager.LayoutParams> getWindowParams()
    {
        ViewRootImpl root = ViewRootImpl.active();
        return root == null ? new ArrayList<WindowManager.LayoutParams>() : new ArrayList<>(root.windowParams());
    }

    public void trimMemory(int level)
    {
    }

    public void setStoppedState(android.os.IBinder token, boolean stopped)
    {
    }

    public void closeAll(android.os.IBinder token, String who, String what)
    {
    }
}
