package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;

public final class AccessibilityManager
{
    public static final int FLAG_CONTENT_ICONS = 1;
    public static final int FLAG_CONTENT_TEXT = 2;
    public static final int FLAG_CONTENT_CONTROLS = 4;

    private static final int DEFAULT_FOCUS_STROKE_WIDTH = 4;
    private static final int DEFAULT_FOCUS_COLOR = 0xbf39b500;

    private static final AccessibilityManager INSTANCE = new AccessibilityManager();

    private final List<AccessibilityStateChangeListener> mStateListeners = new ArrayList<>();
    private final List<TouchExplorationStateChangeListener> mTouchListeners = new ArrayList<>();
    private final List<AccessibilityServicesStateChangeListener> mServicesListeners = new ArrayList<>();
    private final List<AudioDescriptionRequestedChangeListener> mAudioDescriptionListeners = new ArrayList<>();
    private final List<HighContrastTextStateChangeListener> mHighContrastListeners = new ArrayList<>();
    private final List<AccessibilityRequestPreparer> mRequestPreparers = new ArrayList<>();

    private AccessibilityManager()
    {
    }

    public static AccessibilityManager getInstance()
    {
        return INSTANCE;
    }

    public static AccessibilityManager getInstance(android.content.Context context)
    {
        return INSTANCE;
    }

    public static boolean isAccessibilityButtonSupported()
    {
        return false;
    }

    public boolean isEnabled()
    {
        return false;
    }

    public boolean isTouchExplorationEnabled()
    {
        return false;
    }

    public boolean isHighTextContrastEnabled()
    {
        return false;
    }

    public void sendAccessibilityEvent(AccessibilityEvent event)
    {
    }

    public void interrupt()
    {
    }

    public List<android.content.pm.ServiceInfo> getAccessibilityServiceList()
    {
        return new ArrayList<>();
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

    public void addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener, android.os.Handler handler)
    {
        addAccessibilityStateChangeListener(listener);
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

    public void addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener, android.os.Handler handler)
    {
        addTouchExplorationStateChangeListener(listener);
    }

    public boolean removeTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener)
    {
        return mTouchListeners.remove(listener);
    }

    public void addAccessibilityServicesStateChangeListener(java.util.concurrent.Executor executor,
                                                            AccessibilityServicesStateChangeListener listener)
    {
        addAccessibilityServicesStateChangeListener(listener);
    }

    public void addAccessibilityServicesStateChangeListener(AccessibilityServicesStateChangeListener listener)
    {
        if (listener != null && !mServicesListeners.contains(listener)) mServicesListeners.add(listener);
    }

    public boolean removeAccessibilityServicesStateChangeListener(AccessibilityServicesStateChangeListener listener)
    {
        return mServicesListeners.remove(listener);
    }

    public boolean isRequestFromAccessibilityTool()
    {
        return false;
    }

    public int getRecommendedTimeoutMillis(int originalTimeout, int uiContentFlags)
    {
        return originalTimeout;
    }

    public int getAccessibilityFocusStrokeWidth()
    {
        return DEFAULT_FOCUS_STROKE_WIDTH;
    }

    public int getAccessibilityFocusColor()
    {
        return DEFAULT_FOCUS_COLOR;
    }

    public void addAudioDescriptionRequestedChangeListener(java.util.concurrent.Executor executor,
                                                           AudioDescriptionRequestedChangeListener listener)
    {
        if (listener != null && !mAudioDescriptionListeners.contains(listener)) mAudioDescriptionListeners.add(listener);
    }

    public boolean removeAudioDescriptionRequestedChangeListener(AudioDescriptionRequestedChangeListener listener)
    {
        return mAudioDescriptionListeners.remove(listener);
    }

    public boolean isAudioDescriptionRequested()
    {
        return false;
    }

    public boolean isHighContrastTextEnabled()
    {
        return isHighTextContrastEnabled();
    }

    public void addHighContrastTextStateChangeListener(java.util.concurrent.Executor executor, HighContrastTextStateChangeListener listener)
    {
        if (executor == null || listener == null) throw new NullPointerException();

        synchronized (mHighContrastListeners)
        {
            if (!mHighContrastListeners.contains(listener)) mHighContrastListeners.add(listener);
        }
    }

    public void removeHighContrastTextStateChangeListener(HighContrastTextStateChangeListener listener)
    {
        synchronized (mHighContrastListeners)
        {
            mHighContrastListeners.remove(listener);
        }
    }

    public void addAccessibilityRequestPreparer(AccessibilityRequestPreparer preparer)
    {
        if (preparer == null) return;

        synchronized (mRequestPreparers)
        {
            if (!mRequestPreparers.contains(preparer)) mRequestPreparers.add(preparer);
        }
    }

    public void removeAccessibilityRequestPreparer(AccessibilityRequestPreparer preparer)
    {
        synchronized (mRequestPreparers)
        {
            mRequestPreparers.remove(preparer);
        }
    }

    public interface HighContrastTextStateChangeListener
    {
        void onHighContrastTextStateChanged(boolean enabled);
    }

    public interface AccessibilityStateChangeListener
    {
        void onAccessibilityStateChanged(boolean enabled);
    }

    public interface TouchExplorationStateChangeListener
    {
        void onTouchExplorationStateChanged(boolean enabled);
    }

    public interface AccessibilityServicesStateChangeListener
    {
        void onAccessibilityServicesStateChanged(AccessibilityManager manager);
    }

    public interface AudioDescriptionRequestedChangeListener
    {
        void onAudioDescriptionRequestedChanged(boolean enabled);
    }
}
