package android.view.accessibility;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

public final class AccessibilityEvent
{
    public static final int TYPE_VIEW_CLICKED = 0x00000001;
    public static final int TYPE_VIEW_FOCUSED = 0x00000008;
    public static final int TYPE_VIEW_TEXT_CHANGED = 0x00000010;
    public static final int TYPE_WINDOW_STATE_CHANGED = 0x00000020;
    public static final int TYPE_WINDOW_CONTENT_CHANGED = 0x00000800;
    public static final int TYPE_VIEW_TEXT_SELECTION_CHANGED = 0x00002000;
    public static final int TYPE_VIEW_SCROLLED = 0x00001000;

    public static final int CONTENT_CHANGE_TYPE_UNDEFINED = 0x00000000;
    public static final int CONTENT_CHANGE_TYPE_SUBTREE = 0x00000001;
    public static final int CONTENT_CHANGE_TYPE_TEXT = 0x00000002;
    public static final int CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION = 0x00000004;

    private final List<CharSequence> mText = new ArrayList<>();

    private int mEventType;
    private int mContentChangeTypes;
    private int mSourceId = View.NO_ID;
    private boolean mEnabled = true;
    private CharSequence mClassName;
    private CharSequence mPackageName;
    private CharSequence mContentDescription;

    private AccessibilityEvent(int eventType)
    {
        mEventType = eventType;
    }

    public static AccessibilityEvent obtain()
    {
        return new AccessibilityEvent(0);
    }

    public static AccessibilityEvent obtain(int eventType)
    {
        return new AccessibilityEvent(eventType);
    }

    public void recycle()
    {
    }

    public List<CharSequence> getText()
    {
        return mText;
    }

    public int getEventType()
    {
        return mEventType;
    }

    public void setEventType(int eventType)
    {
        mEventType = eventType;
    }

    public int getContentChangeTypes()
    {
        return mContentChangeTypes;
    }

    public void setContentChangeTypes(int types)
    {
        mContentChangeTypes = types;
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    public void setSource(View source)
    {
        mSourceId = source == null ? View.NO_ID : source.getId();
    }

    public void setSource(View root, int virtualDescendantId)
    {
        mSourceId = virtualDescendantId;
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

    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    public void setContentDescription(CharSequence description)
    {
        mContentDescription = description;
    }
}
