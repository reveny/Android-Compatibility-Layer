package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;

public abstract class AccessibilityNodeProvider
{
    public static final int HOST_VIEW_ID = -1;

    public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId)
    {
        return null;
    }

    public void addExtraDataToAccessibilityNodeInfo(int virtualViewId, AccessibilityNodeInfo info, String extraDataKey,
                                                    android.os.Bundle arguments)
    {
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String text, int virtualViewId)
    {
        return new ArrayList<>();
    }

    public AccessibilityNodeInfo findFocus(int focus)
    {
        return null;
    }

    public boolean performAction(int virtualViewId, int action, android.os.Bundle arguments)
    {
        return false;
    }
}
