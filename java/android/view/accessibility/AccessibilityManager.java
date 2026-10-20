package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;

public final class AccessibilityManager
{
    private static final AccessibilityManager INSTANCE = new AccessibilityManager();

    private final List<AccessibilityStateChangeListener> mStateListeners = new ArrayList<>();
    private final List<TouchExplorationStateChangeListener> mTouchListeners = new ArrayList<>();

    private AccessibilityManager()
    {
    }

    public static AccessibilityManager getInstance()
    {
        return INSTANCE;
    }

    public boolean isEnabled()
    {
        return false;
    }

    public boolean isTouchExplorationEnabled()
    {
        return false;
    }

    public void sendAccessibilityEvent(AccessibilityEvent event)
    {
    }

    public void interrupt()
    {
    }

    public List<Object> getEnabledAccessibilityServiceList(int feedbackTypeFlags)
    {
        return new ArrayList<>();
    }

    public List<Object> getInstalledAccessibilityServiceList()
    {
        return new ArrayList<>();
    }

    public boolean addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener)
    {
        if (listener == null || mStateListeners.contains(listener)) return false;

        mStateListeners.add(listener);
        return true;
    }

    public boolean removeAccessibilityStateChangeListener(AccessibilityStateChangeListener listener)
    {
        return mStateListeners.remove(listener);
    }

    public boolean addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener)
    {
        if (listener == null || mTouchListeners.contains(listener)) return false;

        mTouchListeners.add(listener);
        return true;
    }

    public boolean removeTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener)
    {
        return mTouchListeners.remove(listener);
    }

    public interface AccessibilityStateChangeListener
    {
        void onAccessibilityStateChanged(boolean enabled);
    }

    public interface TouchExplorationStateChangeListener
    {
        void onTouchExplorationStateChanged(boolean enabled);
    }
}
