package android.view.accessibility;

import android.graphics.Rect;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityNodeInfo
{
    public static final int ACTION_FOCUS = 0x00000001;
    public static final int ACTION_CLEAR_FOCUS = 0x00000002;
    public static final int ACTION_SELECT = 0x00000004;
    public static final int ACTION_CLICK = 0x00000010;
    public static final int ACTION_LONG_CLICK = 0x00000020;
    public static final int ACTION_ACCESSIBILITY_FOCUS = 0x00000040;

    private final List<Integer> mChildIds = new ArrayList<>();
    private final Rect mBoundsInParent = new Rect();
    private final Rect mBoundsInScreen = new Rect();

    private CharSequence mClassName;
    private CharSequence mPackageName;
    private CharSequence mText;
    private CharSequence mContentDescription;
    private int mParentId = View.NO_ID;
    private boolean mEnabled = true;
    private boolean mVisibleToUser = true;
    private boolean mFocusable;
    private boolean mFocused;
    private boolean mClickable;

    public static AccessibilityNodeInfo obtain()
    {
        return new AccessibilityNodeInfo();
    }

    public static AccessibilityNodeInfo obtain(View source)
    {
        AccessibilityNodeInfo info = new AccessibilityNodeInfo();
        if (source != null) info.setSource(source);
        return info;
    }

    public static AccessibilityNodeInfo obtain(View root, int virtualDescendantId)
    {
        AccessibilityNodeInfo info = new AccessibilityNodeInfo();
        info.mParentId = virtualDescendantId;
        return info;
    }

    public static AccessibilityNodeInfo obtain(AccessibilityNodeInfo other)
    {
        return other == null ? new AccessibilityNodeInfo() : other;
    }

    public void recycle()
    {
    }

    public void setSource(View source)
    {
    }

    public void setSource(View root, int virtualDescendantId)
    {
    }

    public void setParent(View parent)
    {
        mParentId = parent == null ? View.NO_ID : parent.getId();
    }

    public void setParent(View root, int virtualDescendantId)
    {
        mParentId = virtualDescendantId;
    }

    public void addChild(View child)
    {
        if (child != null) mChildIds.add(child.getId());
    }

    public void addChild(View root, int virtualDescendantId)
    {
        mChildIds.add(virtualDescendantId);
    }

    public int getChildCount()
    {
        return mChildIds.size();
    }

    private final java.util.ArrayList<AccessibilityAction> mActions = new java.util.ArrayList<>();
    private int mActionMask;

    public void addAction(int action)
    {
        mActionMask |= action;
    }

    public void addAction(AccessibilityAction action)
    {
        if (action == null || mActions.contains(action)) return;

        mActions.add(action);
        mActionMask |= action.getId();
    }

    public void removeAction(int action)
    {
        mActionMask &= ~action;
    }

    public boolean removeAction(AccessibilityAction action)
    {
        if (action == null) return false;

        mActionMask &= ~action.getId();
        return mActions.remove(action);
    }

    public int getActions()
    {
        return mActionMask;
    }

    public java.util.List<AccessibilityAction> getActionList()
    {
        return new java.util.ArrayList<>(mActions);
    }

    public static final class AccessibilityAction
    {
        public static final AccessibilityAction ACTION_FOCUS = new AccessibilityAction(0x00000001, null);
        public static final AccessibilityAction ACTION_CLEAR_FOCUS = new AccessibilityAction(0x00000002, null);
        public static final AccessibilityAction ACTION_SELECT = new AccessibilityAction(0x00000004, null);
        public static final AccessibilityAction ACTION_CLEAR_SELECTION = new AccessibilityAction(0x00000008, null);
        public static final AccessibilityAction ACTION_CLICK = new AccessibilityAction(0x00000010, null);
        public static final AccessibilityAction ACTION_LONG_CLICK = new AccessibilityAction(0x00000020, null);
        public static final AccessibilityAction ACTION_ACCESSIBILITY_FOCUS = new AccessibilityAction(0x00000040, null);
        public static final AccessibilityAction ACTION_CLEAR_ACCESSIBILITY_FOCUS = new AccessibilityAction(0x00000080, null);
        public static final AccessibilityAction ACTION_SCROLL_FORWARD = new AccessibilityAction(0x00001000, null);
        public static final AccessibilityAction ACTION_SCROLL_BACKWARD = new AccessibilityAction(0x00002000, null);
        public static final AccessibilityAction ACTION_SCROLL_UP = new AccessibilityAction(0x01000000, null);
        public static final AccessibilityAction ACTION_SCROLL_LEFT = new AccessibilityAction(0x02000000, null);
        public static final AccessibilityAction ACTION_SCROLL_DOWN = new AccessibilityAction(0x04000000, null);
        public static final AccessibilityAction ACTION_SCROLL_RIGHT = new AccessibilityAction(0x08000000, null);
        public static final AccessibilityAction ACTION_MOVE_WINDOW = new AccessibilityAction(0x00800000, null);
        public static final AccessibilityAction ACTION_DISMISS = new AccessibilityAction(0x00400000, null);

        private final int mId;
        private final CharSequence mLabel;

        public AccessibilityAction(int id, CharSequence label)
        {
            mId = id;
            mLabel = label;
        }

        public int getId()
        {
            return mId;
        }

        public CharSequence getLabel()
        {
            return mLabel;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof AccessibilityAction)) return false;

            return mId == ((AccessibilityAction)other).mId;
        }

        @Override
        public int hashCode()
        {
            return mId;
        }

        @Override
        public String toString()
        {
            return "AccessibilityAction(0x" + Integer.toHexString(mId) + ")";
        }
    }

    public void getBoundsInParent(Rect out)
    {
        if (out != null) out.set(mBoundsInParent);
    }

    public void setBoundsInParent(Rect bounds)
    {
        if (bounds != null) mBoundsInParent.set(bounds);
    }

    public void getBoundsInScreen(Rect out)
    {
        if (out != null) out.set(mBoundsInScreen);
    }

    public void setBoundsInScreen(Rect bounds)
    {
        if (bounds != null) mBoundsInScreen.set(bounds);
    }

    public CharSequence getClassName()
    {
        return mClassName;
    }

    public void setClassName(CharSequence className)
    {
        mClassName = className;
    }

    public CharSequence getPackageName()
    {
        return mPackageName;
    }

    public void setPackageName(CharSequence packageName)
    {
        mPackageName = packageName;
    }

    public CharSequence getText()
    {
        return mText;
    }

    public void setText(CharSequence text)
    {
        mText = text;
    }

    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    public void setContentDescription(CharSequence description)
    {
        mContentDescription = description;
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    public boolean isVisibleToUser()
    {
        return mVisibleToUser;
    }

    public void setVisibleToUser(boolean visible)
    {
        mVisibleToUser = visible;
    }

    public boolean isFocusable()
    {
        return mFocusable;
    }

    public void setFocusable(boolean focusable)
    {
        mFocusable = focusable;
    }

    public boolean isFocused()
    {
        return mFocused;
    }

    public void setFocused(boolean focused)
    {
        mFocused = focused;
    }

    public boolean isClickable()
    {
        return mClickable;
    }

    public void setClickable(boolean clickable)
    {
        mClickable = clickable;
    }
}
