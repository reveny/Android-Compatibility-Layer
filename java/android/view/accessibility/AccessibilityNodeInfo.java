package android.view.accessibility;

import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccessibilityNodeInfo implements Parcelable
{
    public static final int ACTION_FOCUS = 0x00000001;
    public static final int ACTION_CLEAR_FOCUS = 0x00000002;
    public static final int ACTION_SELECT = 0x00000004;
    public static final int ACTION_CLEAR_SELECTION = 0x00000008;
    public static final int ACTION_CLICK = 0x00000010;
    public static final int ACTION_LONG_CLICK = 0x00000020;
    public static final int ACTION_ACCESSIBILITY_FOCUS = 0x00000040;
    public static final int ACTION_CLEAR_ACCESSIBILITY_FOCUS = 0x00000080;
    public static final int ACTION_NEXT_AT_MOVEMENT_GRANULARITY = 0x00000100;
    public static final int ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY = 0x00000200;
    public static final int ACTION_NEXT_HTML_ELEMENT = 0x00000400;
    public static final int ACTION_PREVIOUS_HTML_ELEMENT = 0x00000800;
    public static final int ACTION_SCROLL_FORWARD = 0x00001000;
    public static final int ACTION_SCROLL_BACKWARD = 0x00002000;
    public static final int ACTION_COPY = 0x00004000;
    public static final int ACTION_PASTE = 0x00008000;
    public static final int ACTION_CUT = 0x00010000;
    public static final int ACTION_SET_SELECTION = 0x00020000;
    public static final int ACTION_EXPAND = 0x00040000;
    public static final int ACTION_COLLAPSE = 0x00080000;
    public static final int ACTION_DISMISS = 0x00100000;
    public static final int ACTION_SET_TEXT = 0x00200000;

    public static final String ACTION_ARGUMENT_COLUMN_INT = "android.view.accessibility.action.ARGUMENT_COLUMN_INT";
    public static final String ACTION_ARGUMENT_DIRECTION_INT = "android.view.accessibility.action.ARGUMENT_DIRECTION_INT";
    public static final String ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN = "ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN";
    public static final String ACTION_ARGUMENT_HTML_ELEMENT_STRING = "ACTION_ARGUMENT_HTML_ELEMENT_STRING";
    public static final String ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT = "ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT";
    public static final String ACTION_ARGUMENT_MOVE_WINDOW_X = "ACTION_ARGUMENT_MOVE_WINDOW_X";
    public static final String ACTION_ARGUMENT_MOVE_WINDOW_Y = "ACTION_ARGUMENT_MOVE_WINDOW_Y";
    public static final String ACTION_ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT =
        "android.view.accessibility.action.ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT";
    public static final String ACTION_ARGUMENT_PROGRESS_VALUE = "android.view.accessibility.action.ARGUMENT_PROGRESS_VALUE";
    public static final String ACTION_ARGUMENT_ROW_INT = "android.view.accessibility.action.ARGUMENT_ROW_INT";
    public static final String ACTION_ARGUMENT_SELECTION_END_INT = "ACTION_ARGUMENT_SELECTION_END_INT";
    public static final String ACTION_ARGUMENT_SELECTION_START_INT = "ACTION_ARGUMENT_SELECTION_START_INT";
    public static final String ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE = "ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE";

    public static final String ACTION_ARGUMENT_SCROLL_AMOUNT_FLOAT = "android.view.accessibility.action.ARGUMENT_SCROLL_AMOUNT_FLOAT";
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_IN_WINDOW_KEY =
        "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_IN_WINDOW_KEY";

    public static final String EXTRA_DATA_RENDERING_INFO_KEY = "android.view.accessibility.extra.DATA_RENDERING_INFO_KEY";
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH =
        "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH";
    public static final int EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_MAX_LENGTH = 20000;
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX =
        "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX";
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY = "android.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_KEY";

    public static final int FLAG_PREFETCH_ANCESTORS = 1;
    public static final int FLAG_PREFETCH_SIBLINGS = 2;
    public static final int FLAG_PREFETCH_DESCENDANTS_HYBRID = 4;
    public static final int FLAG_PREFETCH_DESCENDANTS_DEPTH_FIRST = 8;
    public static final int FLAG_PREFETCH_DESCENDANTS_BREADTH_FIRST = 16;
    public static final int FLAG_PREFETCH_UNINTERRUPTIBLE = 32;

    public static final int FOCUS_INPUT = 1;
    public static final int FOCUS_ACCESSIBILITY = 2;

    public static final int MAX_NUMBER_OF_PREFETCHED_NODES = 50;

    public static final int MOVEMENT_GRANULARITY_CHARACTER = 1;
    public static final int MOVEMENT_GRANULARITY_WORD = 2;
    public static final int MOVEMENT_GRANULARITY_LINE = 4;
    public static final int MOVEMENT_GRANULARITY_PARAGRAPH = 8;
    public static final int MOVEMENT_GRANULARITY_PAGE = 16;

    public static final int CHECKED_STATE_FALSE = 0;
    public static final int CHECKED_STATE_TRUE = 1;
    public static final int CHECKED_STATE_PARTIAL = 2;

    public static final int EXPANDED_STATE_UNDEFINED = 0;
    public static final int EXPANDED_STATE_COLLAPSED = 1;
    public static final int EXPANDED_STATE_PARTIAL = 2;
    public static final int EXPANDED_STATE_FULL = 3;

    private static final int UNDEFINED_ID = -1;
    private static final int FIND_VIEWS_WITH_TEXT = 1;
    private static final int FIND_VIEWS_WITH_CONTENT_DESCRIPTION = 2;

    public static final Parcelable.Creator<AccessibilityNodeInfo> CREATOR = new Parcelable.Creator<AccessibilityNodeInfo>()
    {
        @Override
        public AccessibilityNodeInfo createFromParcel(Parcel source)
        {
            AccessibilityNodeInfo info = new AccessibilityNodeInfo();
            info.mClassName = source.readString();
            info.mPackageName = source.readString();
            info.mText = source.readString();
            info.mContentDescription = source.readString();
            info.mBooleanProperties = source.readInt();
            info.mActionMask = source.readInt();
            return info;
        }

        @Override
        public AccessibilityNodeInfo[] newArray(int size)
        {
            return new AccessibilityNodeInfo[size];
        }
    };

    private static final int PROPERTY_CHECKABLE = 1;
    private static final int PROPERTY_CHECKED = 1 << 1;
    private static final int PROPERTY_FOCUSABLE = 1 << 2;
    private static final int PROPERTY_FOCUSED = 1 << 3;
    private static final int PROPERTY_SELECTED = 1 << 4;
    private static final int PROPERTY_CLICKABLE = 1 << 5;
    private static final int PROPERTY_LONG_CLICKABLE = 1 << 6;
    private static final int PROPERTY_ENABLED = 1 << 7;
    private static final int PROPERTY_PASSWORD = 1 << 8;
    private static final int PROPERTY_SCROLLABLE = 1 << 9;
    private static final int PROPERTY_ACCESSIBILITY_FOCUSED = 1 << 10;
    private static final int PROPERTY_VISIBLE_TO_USER = 1 << 11;
    private static final int PROPERTY_EDITABLE = 1 << 12;
    private static final int PROPERTY_OPENS_POPUP = 1 << 13;
    private static final int PROPERTY_DISMISSABLE = 1 << 14;
    private static final int PROPERTY_MULTI_LINE = 1 << 15;
    private static final int PROPERTY_CONTENT_INVALID = 1 << 16;
    private static final int PROPERTY_CONTEXT_CLICKABLE = 1 << 17;
    private static final int PROPERTY_IMPORTANCE = 1 << 18;
    private static final int PROPERTY_SCREEN_READER_FOCUSABLE = 1 << 19;
    private static final int PROPERTY_SHOWING_HINT_TEXT = 1 << 20;
    private static final int PROPERTY_HEADING = 1 << 21;
    private static final int PROPERTY_TEXT_ENTRY_KEY = 1 << 22;
    private static final int PROPERTY_TEXT_SELECTABLE = 1 << 23;
    private static final int PROPERTY_REQUEST_INITIAL_ACCESSIBILITY_FOCUS = 1 << 24;
    private static final int PROPERTY_ACCESSIBILITY_DATA_SENSITIVE = 1 << 25;
    private static final int PROPERTY_FIELD_REQUIRED = 1 << 26;

    private final ArrayList<View> mChildViews = new ArrayList<>();
    private final ArrayList<Integer> mChildVirtualIds = new ArrayList<>();
    private final ArrayList<AccessibilityAction> mActions = new ArrayList<>();
    private final Rect mBoundsInParent = new Rect();
    private final Rect mBoundsInScreen = new Rect();
    private final Rect mBoundsInWindow = new Rect();

    private View mSource;
    private int mVirtualDescendantId = UNDEFINED_ID;
    private View mParentView;
    private int mParentVirtualId = UNDEFINED_ID;
    private View mLabelForView;
    private int mLabelForVirtualId = UNDEFINED_ID;
    private View mLabeledByView;
    private int mLabeledByVirtualId = UNDEFINED_ID;
    private final java.util.ArrayList<View> mExtraLabeledByViews = new java.util.ArrayList<>();
    private final java.util.ArrayList<Integer> mExtraLabeledByVirtualIds = new java.util.ArrayList<>();
    private boolean mGranularScrollingSupported;
    private View mTraversalBeforeView;
    private int mTraversalBeforeVirtualId = UNDEFINED_ID;
    private View mTraversalAfterView;
    private int mTraversalAfterVirtualId = UNDEFINED_ID;

    private CharSequence mClassName;
    private CharSequence mPackageName;
    private CharSequence mText;
    private CharSequence mContentDescription;
    private CharSequence mHintText;
    private CharSequence mError;
    private CharSequence mStateDescription;
    private CharSequence mTooltipText;
    private CharSequence mPaneTitle;
    private CharSequence mContainerTitle;
    private CharSequence mSupplementalDescription;
    private String mViewIdResourceName;
    private String mUniqueId;
    private int mBooleanProperties = PROPERTY_ENABLED | PROPERTY_VISIBLE_TO_USER;
    private int mActionMask;
    private int mWindowId = UNDEFINED_ID;
    private int mMaxTextLength = -1;
    private int mMovementGranularities;
    private int mTextSelectionStart = UNDEFINED_ID;
    private int mTextSelectionEnd = UNDEFINED_ID;
    private int mInputType;
    private int mLiveRegion;
    private int mDrawingOrder;
    private int mChecked = CHECKED_STATE_FALSE;
    private int mExpandedState = EXPANDED_STATE_UNDEFINED;
    private java.time.Duration mMinDurationBetweenContentChanges = java.time.Duration.ZERO;
    private List<String> mExtraDataKeys;
    private Bundle mExtras;
    private CollectionInfo mCollectionInfo;
    private CollectionItemInfo mCollectionItemInfo;
    private RangeInfo mRangeInfo;
    private ExtraRenderingInfo mExtraRenderingInfo;
    private TouchDelegateInfo mTouchDelegateInfo;

    public AccessibilityNodeInfo()
    {
    }

    public AccessibilityNodeInfo(View source)
    {
        setSource(source);
    }

    public AccessibilityNodeInfo(View root, int virtualDescendantId)
    {
        setSource(root, virtualDescendantId);
    }

    public AccessibilityNodeInfo(AccessibilityNodeInfo info)
    {
        if (info != null) copyFrom(info);
    }

    private void copyFrom(AccessibilityNodeInfo other)
    {
        mChildViews.addAll(other.mChildViews);
        mChildVirtualIds.addAll(other.mChildVirtualIds);
        mActions.addAll(other.mActions);
        mBoundsInParent.set(other.mBoundsInParent);
        mBoundsInScreen.set(other.mBoundsInScreen);
        mBoundsInWindow.set(other.mBoundsInWindow);
        mSource = other.mSource;
        mVirtualDescendantId = other.mVirtualDescendantId;
        mParentView = other.mParentView;
        mParentVirtualId = other.mParentVirtualId;
        mLabelForView = other.mLabelForView;
        mLabelForVirtualId = other.mLabelForVirtualId;
        mLabeledByView = other.mLabeledByView;
        mLabeledByVirtualId = other.mLabeledByVirtualId;
        mExtraLabeledByViews.clear();
        mExtraLabeledByViews.addAll(other.mExtraLabeledByViews);
        mExtraLabeledByVirtualIds.clear();
        mExtraLabeledByVirtualIds.addAll(other.mExtraLabeledByVirtualIds);
        mGranularScrollingSupported = other.mGranularScrollingSupported;
        mTraversalBeforeView = other.mTraversalBeforeView;
        mTraversalBeforeVirtualId = other.mTraversalBeforeVirtualId;
        mTraversalAfterView = other.mTraversalAfterView;
        mTraversalAfterVirtualId = other.mTraversalAfterVirtualId;
        mClassName = other.mClassName;
        mPackageName = other.mPackageName;
        mText = other.mText;
        mContentDescription = other.mContentDescription;
        mHintText = other.mHintText;
        mError = other.mError;
        mStateDescription = other.mStateDescription;
        mTooltipText = other.mTooltipText;
        mPaneTitle = other.mPaneTitle;
        mContainerTitle = other.mContainerTitle;
        mSupplementalDescription = other.mSupplementalDescription;
        mViewIdResourceName = other.mViewIdResourceName;
        mUniqueId = other.mUniqueId;
        mBooleanProperties = other.mBooleanProperties;
        mActionMask = other.mActionMask;
        mWindowId = other.mWindowId;
        mMaxTextLength = other.mMaxTextLength;
        mMovementGranularities = other.mMovementGranularities;
        mTextSelectionStart = other.mTextSelectionStart;
        mTextSelectionEnd = other.mTextSelectionEnd;
        mInputType = other.mInputType;
        mLiveRegion = other.mLiveRegion;
        mDrawingOrder = other.mDrawingOrder;
        mChecked = other.mChecked;
        mExpandedState = other.mExpandedState;
        mMinDurationBetweenContentChanges = other.mMinDurationBetweenContentChanges;
        mExtraDataKeys = other.mExtraDataKeys == null ? null : new ArrayList<>(other.mExtraDataKeys);
        mExtras = other.mExtras == null ? null : new Bundle(other.mExtras);
        mCollectionInfo = other.mCollectionInfo;
        mCollectionItemInfo = other.mCollectionItemInfo;
        mRangeInfo = other.mRangeInfo;
        mExtraRenderingInfo = other.mExtraRenderingInfo;
        mTouchDelegateInfo = other.mTouchDelegateInfo;
    }

    public static AccessibilityNodeInfo obtain()
    {
        return new AccessibilityNodeInfo();
    }

    public static AccessibilityNodeInfo obtain(View source)
    {
        return new AccessibilityNodeInfo(source);
    }

    public static AccessibilityNodeInfo obtain(View root, int virtualDescendantId)
    {
        return new AccessibilityNodeInfo(root, virtualDescendantId);
    }

    public static AccessibilityNodeInfo obtain(AccessibilityNodeInfo info)
    {
        return new AccessibilityNodeInfo(info);
    }

    public void recycle()
    {
    }

    public void setSource(View source)
    {
        setSource(source, UNDEFINED_ID);
    }

    public void setSource(View root, int virtualDescendantId)
    {
        mSource = root;
        mVirtualDescendantId = virtualDescendantId;
    }

    public AccessibilityNodeInfo findFocus(int focus)
    {
        if (mSource == null) return null;

        if (focus == FOCUS_INPUT)
        {
            View focused = mSource.findFocus();
            return focused == null ? null : createInfo(focused, UNDEFINED_ID);
        }

        return null;
    }

    public AccessibilityNodeInfo focusSearch(int direction)
    {
        return null;
    }

    public int getWindowId()
    {
        return mWindowId;
    }

    public boolean refresh()
    {
        return mSource != null;
    }

    public boolean refreshWithExtraData(String extraDataKey, Bundle args)
    {
        return refresh();
    }

    private static AccessibilityNodeInfo createInfo(View view, int virtualId)
    {
        if (view == null) return null;

        if (virtualId != UNDEFINED_ID)
        {
            AccessibilityNodeProvider provider = view.getAccessibilityNodeProvider();
            if (provider != null) return provider.createAccessibilityNodeInfo(virtualId);
            return null;
        }

        return view.createAccessibilityNodeInfo();
    }

    public int getChildCount()
    {
        return mChildViews.size();
    }

    public AccessibilityNodeInfo getChild(int index)
    {
        if (index < 0 || index >= mChildViews.size()) return null;

        return createInfo(mChildViews.get(index), mChildVirtualIds.get(index));
    }

    public AccessibilityNodeInfo getChild(int index, int prefetchingStrategy)
    {
        return getChild(index);
    }

    public void addChild(View child)
    {
        addChild(child, UNDEFINED_ID);
    }

    public void addChild(View root, int virtualDescendantId)
    {
        if (root == null) return;

        mChildViews.add(root);
        mChildVirtualIds.add(virtualDescendantId);
    }

    public boolean removeChild(View child)
    {
        return removeChild(child, UNDEFINED_ID);
    }

    public boolean removeChild(View root, int virtualDescendantId)
    {
        for (int index = 0; index < mChildViews.size(); index++)
        {
            if (mChildViews.get(index) != root || mChildVirtualIds.get(index) != virtualDescendantId) continue;

            mChildViews.remove(index);
            mChildVirtualIds.remove(index);
            return true;
        }

        return false;
    }

    public void addAction(int action)
    {
        mActionMask |= action;
    }

    public void addAction(AccessibilityAction action)
    {
        if (action == null) return;

        mActions.remove(action);
        mActions.add(action);

        if (Integer.bitCount(action.getId()) == 1 && action.getId() <= ACTION_SET_TEXT) mActionMask |= action.getId();
    }

    public void removeAction(int action)
    {
        mActionMask &= ~action;
    }

    public boolean removeAction(AccessibilityAction action)
    {
        if (action == null) return false;

        if (Integer.bitCount(action.getId()) == 1 && action.getId() <= ACTION_SET_TEXT) mActionMask &= ~action.getId();
        return mActions.remove(action);
    }

    public int getActions()
    {
        int mask = mActionMask;

        for (AccessibilityAction action : mActions)
        {
            if (Integer.bitCount(action.getId()) == 1 && action.getId() <= ACTION_SET_TEXT) mask |= action.getId();
        }

        return mask;
    }

    public List<AccessibilityAction> getActionList()
    {
        ArrayList<AccessibilityAction> list = new ArrayList<>(mActions);

        for (int bit = 0; bit < 32; bit++)
        {
            int id = 1 << bit;
            if ((mActionMask & id) == 0) continue;

            AccessibilityAction action = new AccessibilityAction(id, null);
            if (!list.contains(action)) list.add(action);
        }

        return list;
    }

    public AccessibilityNodeInfo getTraversalBefore()
    {
        return createInfo(mTraversalBeforeView, mTraversalBeforeVirtualId);
    }

    public void setTraversalBefore(View view)
    {
        setTraversalBefore(view, UNDEFINED_ID);
    }

    public void setTraversalBefore(View root, int virtualDescendantId)
    {
        mTraversalBeforeView = root;
        mTraversalBeforeVirtualId = virtualDescendantId;
    }

    public AccessibilityNodeInfo getTraversalAfter()
    {
        return createInfo(mTraversalAfterView, mTraversalAfterVirtualId);
    }

    public void setTraversalAfter(View view)
    {
        setTraversalAfter(view, UNDEFINED_ID);
    }

    public void setTraversalAfter(View root, int virtualDescendantId)
    {
        mTraversalAfterView = root;
        mTraversalAfterVirtualId = virtualDescendantId;
    }

    public List<String> getAvailableExtraData()
    {
        return mExtraDataKeys == null ? new ArrayList<String>() : new ArrayList<>(mExtraDataKeys);
    }

    public void setAvailableExtraData(List<String> extraDataKeys)
    {
        mExtraDataKeys = extraDataKeys == null ? null : new ArrayList<>(extraDataKeys);
    }

    public void setMaxTextLength(int max)
    {
        mMaxTextLength = max;
    }

    public int getMaxTextLength()
    {
        return mMaxTextLength;
    }

    public void setMovementGranularities(int granularities)
    {
        mMovementGranularities = granularities;
    }

    public int getMovementGranularities()
    {
        return mMovementGranularities;
    }

    public void setMinDurationBetweenContentChanges(java.time.Duration duration)
    {
        mMinDurationBetweenContentChanges = duration == null ? java.time.Duration.ZERO : duration;
    }

    public java.time.Duration getMinDurationBetweenContentChanges()
    {
        return mMinDurationBetweenContentChanges;
    }

    public boolean performAction(int action)
    {
        return performAction(action, null);
    }

    public boolean performAction(int action, Bundle arguments)
    {
        if (mSource == null) return false;

        if (mVirtualDescendantId != UNDEFINED_ID)
        {
            AccessibilityNodeProvider provider = mSource.getAccessibilityNodeProvider();
            return provider != null && provider.performAction(mVirtualDescendantId, action, arguments);
        }

        return mSource.performAccessibilityAction(action, arguments);
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(String text)
    {
        ArrayList<AccessibilityNodeInfo> result = new ArrayList<>();
        if (mSource == null || text == null) return result;

        ArrayList<View> found = new ArrayList<>();
        mSource.findViewsWithText(found, text, FIND_VIEWS_WITH_TEXT | FIND_VIEWS_WITH_CONTENT_DESCRIPTION);

        for (View view : found)
        {
            AccessibilityNodeInfo info = createInfo(view, UNDEFINED_ID);
            if (info != null) result.add(info);
        }

        return result;
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId(String viewId)
    {
        ArrayList<AccessibilityNodeInfo> result = new ArrayList<>();
        if (mSource == null || viewId == null) return result;

        int id = mSource.getResources().getIdentifier(viewId, null, null);
        if (id == 0) return result;

        View view = mSource.findViewById(id);
        AccessibilityNodeInfo info = createInfo(view, UNDEFINED_ID);
        if (info != null) result.add(info);

        return result;
    }

    public AccessibilityWindowInfo getWindow()
    {
        return null;
    }

    public AccessibilityNodeInfo getParent()
    {
        if (mParentView != null) return createInfo(mParentView, mParentVirtualId);
        if (mSource == null || mVirtualDescendantId != UNDEFINED_ID) return null;

        android.view.ViewParent parent = mSource.getParentForAccessibility();
        return parent instanceof View ? createInfo((View)parent, UNDEFINED_ID) : null;
    }

    public AccessibilityNodeInfo getParent(int prefetchingStrategy)
    {
        return getParent();
    }

    public void setParent(View parent)
    {
        setParent(parent, UNDEFINED_ID);
    }

    public void setParent(View root, int virtualDescendantId)
    {
        mParentView = root;
        mParentVirtualId = virtualDescendantId;
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

    public void getBoundsInWindow(Rect out)
    {
        if (out != null) out.set(mBoundsInWindow);
    }

    public void setBoundsInWindow(Rect bounds)
    {
        if (bounds != null) mBoundsInWindow.set(bounds);
    }

    private boolean getProperty(int property)
    {
        return (mBooleanProperties & property) != 0;
    }

    private void setProperty(int property, boolean value)
    {
        if (value) mBooleanProperties |= property;
        else mBooleanProperties &= ~property;
    }

    public boolean isCheckable()
    {
        return getProperty(PROPERTY_CHECKABLE);
    }

    public void setCheckable(boolean checkable)
    {
        setProperty(PROPERTY_CHECKABLE, checkable);
    }

    public boolean isChecked()
    {
        return getProperty(PROPERTY_CHECKED);
    }

    public void setChecked(boolean checked)
    {
        setProperty(PROPERTY_CHECKED, checked);
        mChecked = checked ? CHECKED_STATE_TRUE : CHECKED_STATE_FALSE;
    }

    public int getChecked()
    {
        return mChecked;
    }

    public void setChecked(int checked)
    {
        mChecked = checked;
        setProperty(PROPERTY_CHECKED, checked == CHECKED_STATE_TRUE);
    }

    public int getExpandedState()
    {
        return mExpandedState;
    }

    public void setExpandedState(int state)
    {
        mExpandedState = state;
    }

    public boolean isFocusable()
    {
        return getProperty(PROPERTY_FOCUSABLE);
    }

    public void setFocusable(boolean focusable)
    {
        setProperty(PROPERTY_FOCUSABLE, focusable);
    }

    public boolean isFocused()
    {
        return getProperty(PROPERTY_FOCUSED);
    }

    public void setFocused(boolean focused)
    {
        setProperty(PROPERTY_FOCUSED, focused);
    }

    public boolean isVisibleToUser()
    {
        return getProperty(PROPERTY_VISIBLE_TO_USER);
    }

    public void setVisibleToUser(boolean visibleToUser)
    {
        setProperty(PROPERTY_VISIBLE_TO_USER, visibleToUser);
    }

    public boolean isAccessibilityFocused()
    {
        return getProperty(PROPERTY_ACCESSIBILITY_FOCUSED);
    }

    public void setAccessibilityFocused(boolean focused)
    {
        setProperty(PROPERTY_ACCESSIBILITY_FOCUSED, focused);
    }

    public boolean isSelected()
    {
        return getProperty(PROPERTY_SELECTED);
    }

    public void setSelected(boolean selected)
    {
        setProperty(PROPERTY_SELECTED, selected);
    }

    public boolean isClickable()
    {
        return getProperty(PROPERTY_CLICKABLE);
    }

    public void setClickable(boolean clickable)
    {
        setProperty(PROPERTY_CLICKABLE, clickable);
    }

    public boolean isLongClickable()
    {
        return getProperty(PROPERTY_LONG_CLICKABLE);
    }

    public void setLongClickable(boolean longClickable)
    {
        setProperty(PROPERTY_LONG_CLICKABLE, longClickable);
    }

    public boolean isEnabled()
    {
        return getProperty(PROPERTY_ENABLED);
    }

    public void setEnabled(boolean enabled)
    {
        setProperty(PROPERTY_ENABLED, enabled);
    }

    public boolean isPassword()
    {
        return getProperty(PROPERTY_PASSWORD);
    }

    public void setPassword(boolean password)
    {
        setProperty(PROPERTY_PASSWORD, password);
    }

    public boolean isScrollable()
    {
        return getProperty(PROPERTY_SCROLLABLE);
    }

    public void setScrollable(boolean scrollable)
    {
        setProperty(PROPERTY_SCROLLABLE, scrollable);
    }

    public boolean isTextSelectable()
    {
        return getProperty(PROPERTY_TEXT_SELECTABLE);
    }

    public void setTextSelectable(boolean selectableText)
    {
        setProperty(PROPERTY_TEXT_SELECTABLE, selectableText);
    }

    public boolean hasRequestInitialAccessibilityFocus()
    {
        return getProperty(PROPERTY_REQUEST_INITIAL_ACCESSIBILITY_FOCUS);
    }

    public void setRequestInitialAccessibilityFocus(boolean request)
    {
        setProperty(PROPERTY_REQUEST_INITIAL_ACCESSIBILITY_FOCUS, request);
    }

    public boolean isEditable()
    {
        return getProperty(PROPERTY_EDITABLE);
    }

    public void setEditable(boolean editable)
    {
        setProperty(PROPERTY_EDITABLE, editable);
    }

    public boolean isAccessibilityDataSensitive()
    {
        return getProperty(PROPERTY_ACCESSIBILITY_DATA_SENSITIVE);
    }

    public void setAccessibilityDataSensitive(boolean sensitive)
    {
        setProperty(PROPERTY_ACCESSIBILITY_DATA_SENSITIVE, sensitive);
    }

    public boolean isFieldRequired()
    {
        return getProperty(PROPERTY_FIELD_REQUIRED);
    }

    public void setFieldRequired(boolean required)
    {
        setProperty(PROPERTY_FIELD_REQUIRED, required);
    }

    public void setPaneTitle(CharSequence paneTitle)
    {
        mPaneTitle = paneTitle;
    }

    public CharSequence getPaneTitle()
    {
        return mPaneTitle;
    }

    public int getDrawingOrder()
    {
        return mDrawingOrder;
    }

    public void setDrawingOrder(int drawingOrderInParent)
    {
        mDrawingOrder = drawingOrderInParent;
    }

    public CollectionInfo getCollectionInfo()
    {
        return mCollectionInfo;
    }

    public void setCollectionInfo(CollectionInfo collectionInfo)
    {
        mCollectionInfo = collectionInfo;
    }

    public CollectionItemInfo getCollectionItemInfo()
    {
        return mCollectionItemInfo;
    }

    public void setCollectionItemInfo(CollectionItemInfo collectionItemInfo)
    {
        mCollectionItemInfo = collectionItemInfo;
    }

    public RangeInfo getRangeInfo()
    {
        return mRangeInfo;
    }

    public void setRangeInfo(RangeInfo rangeInfo)
    {
        mRangeInfo = rangeInfo;
    }

    public ExtraRenderingInfo getExtraRenderingInfo()
    {
        return mExtraRenderingInfo;
    }

    public boolean isContentInvalid()
    {
        return getProperty(PROPERTY_CONTENT_INVALID);
    }

    public void setContentInvalid(boolean contentInvalid)
    {
        setProperty(PROPERTY_CONTENT_INVALID, contentInvalid);
    }

    public boolean isContextClickable()
    {
        return getProperty(PROPERTY_CONTEXT_CLICKABLE);
    }

    public void setContextClickable(boolean contextClickable)
    {
        setProperty(PROPERTY_CONTEXT_CLICKABLE, contextClickable);
    }

    public int getLiveRegion()
    {
        return mLiveRegion;
    }

    public void setLiveRegion(int mode)
    {
        mLiveRegion = mode;
    }

    public boolean isMultiLine()
    {
        return getProperty(PROPERTY_MULTI_LINE);
    }

    public void setMultiLine(boolean multiLine)
    {
        setProperty(PROPERTY_MULTI_LINE, multiLine);
    }

    public boolean canOpenPopup()
    {
        return getProperty(PROPERTY_OPENS_POPUP);
    }

    public void setCanOpenPopup(boolean opensPopup)
    {
        setProperty(PROPERTY_OPENS_POPUP, opensPopup);
    }

    public boolean isDismissable()
    {
        return getProperty(PROPERTY_DISMISSABLE);
    }

    public void setDismissable(boolean dismissable)
    {
        setProperty(PROPERTY_DISMISSABLE, dismissable);
    }

    public boolean isImportantForAccessibility()
    {
        return getProperty(PROPERTY_IMPORTANCE);
    }

    public void setImportantForAccessibility(boolean important)
    {
        setProperty(PROPERTY_IMPORTANCE, important);
    }

    public boolean isScreenReaderFocusable()
    {
        return getProperty(PROPERTY_SCREEN_READER_FOCUSABLE);
    }

    public void setScreenReaderFocusable(boolean screenReaderFocusable)
    {
        setProperty(PROPERTY_SCREEN_READER_FOCUSABLE, screenReaderFocusable);
    }

    public boolean isShowingHintText()
    {
        return getProperty(PROPERTY_SHOWING_HINT_TEXT);
    }

    public void setShowingHintText(boolean showingHintText)
    {
        setProperty(PROPERTY_SHOWING_HINT_TEXT, showingHintText);
    }

    public boolean isHeading()
    {
        return getProperty(PROPERTY_HEADING);
    }

    public void setHeading(boolean isHeading)
    {
        setProperty(PROPERTY_HEADING, isHeading);
    }

    public boolean isTextEntryKey()
    {
        return getProperty(PROPERTY_TEXT_ENTRY_KEY);
    }

    public void setTextEntryKey(boolean isTextEntryKey)
    {
        setProperty(PROPERTY_TEXT_ENTRY_KEY, isTextEntryKey);
    }

    public CharSequence getPackageName()
    {
        return mPackageName;
    }

    public void setPackageName(CharSequence packageName)
    {
        mPackageName = packageName;
    }

    public CharSequence getClassName()
    {
        return mClassName;
    }

    public void setClassName(CharSequence className)
    {
        mClassName = className;
    }

    public CharSequence getText()
    {
        return mText;
    }

    public void setText(CharSequence text)
    {
        mText = text;
    }

    public CharSequence getHintText()
    {
        return mHintText;
    }

    public void setHintText(CharSequence hintText)
    {
        mHintText = hintText;
    }

    public void setError(CharSequence error)
    {
        mError = error;
    }

    public CharSequence getError()
    {
        return mError;
    }

    public CharSequence getStateDescription()
    {
        return mStateDescription;
    }

    public void setStateDescription(CharSequence stateDescription)
    {
        mStateDescription = stateDescription;
    }

    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    public void setContentDescription(CharSequence contentDescription)
    {
        mContentDescription = contentDescription;
    }

    public CharSequence getSupplementalDescription()
    {
        return mSupplementalDescription;
    }

    public void setSupplementalDescription(CharSequence supplementalDescription)
    {
        mSupplementalDescription = supplementalDescription;
    }

    public CharSequence getTooltipText()
    {
        return mTooltipText;
    }

    public void setTooltipText(CharSequence tooltipText)
    {
        mTooltipText = tooltipText;
    }

    public void setLabelFor(View labeled)
    {
        setLabelFor(labeled, UNDEFINED_ID);
    }

    public void setLabelFor(View root, int virtualDescendantId)
    {
        mLabelForView = root;
        mLabelForVirtualId = virtualDescendantId;
    }

    public AccessibilityNodeInfo getLabelFor()
    {
        return createInfo(mLabelForView, mLabelForVirtualId);
    }

    public void setLabeledBy(View label)
    {
        setLabeledBy(label, UNDEFINED_ID);
    }

    public void setLabeledBy(View root, int virtualDescendantId)
    {
        mLabeledByView = root;
        mLabeledByVirtualId = virtualDescendantId;
        mExtraLabeledByViews.clear();
        mExtraLabeledByVirtualIds.clear();
    }

    public AccessibilityNodeInfo getLabeledBy()
    {
        return createInfo(mLabeledByView, mLabeledByVirtualId);
    }

    public void addLabeledBy(View label)
    {
        addLabeledBy(label, UNDEFINED_ID);
    }

    public void addLabeledBy(View root, int virtualDescendantId)
    {
        if (root == null) return;

        if (mLabeledByView == null)
        {
            mLabeledByView = root;
            mLabeledByVirtualId = virtualDescendantId;
            return;
        }

        if (mLabeledByView == root && mLabeledByVirtualId == virtualDescendantId) return;

        for (int index = 0; index < mExtraLabeledByViews.size(); index++)
        {
            if (mExtraLabeledByViews.get(index) == root && mExtraLabeledByVirtualIds.get(index) == virtualDescendantId) return;
        }

        mExtraLabeledByViews.add(root);
        mExtraLabeledByVirtualIds.add(virtualDescendantId);
    }

    public boolean removeLabeledBy(View label)
    {
        return removeLabeledBy(label, UNDEFINED_ID);
    }

    public boolean removeLabeledBy(View root, int virtualDescendantId)
    {
        if (root == null) return false;

        if (mLabeledByView == root && mLabeledByVirtualId == virtualDescendantId)
        {
            if (mExtraLabeledByViews.isEmpty())
            {
                mLabeledByView = null;
                mLabeledByVirtualId = UNDEFINED_ID;
            }
            else
            {
                mLabeledByView = mExtraLabeledByViews.remove(0);
                mLabeledByVirtualId = mExtraLabeledByVirtualIds.remove(0);
            }

            return true;
        }

        for (int index = 0; index < mExtraLabeledByViews.size(); index++)
        {
            if (mExtraLabeledByViews.get(index) != root || mExtraLabeledByVirtualIds.get(index) != virtualDescendantId) continue;

            mExtraLabeledByViews.remove(index);
            mExtraLabeledByVirtualIds.remove(index);
            return true;
        }

        return false;
    }

    public java.util.List<AccessibilityNodeInfo> getLabeledByList()
    {
        java.util.ArrayList<AccessibilityNodeInfo> labels = new java.util.ArrayList<>();
        if (mLabeledByView == null) return labels;

        AccessibilityNodeInfo first = createInfo(mLabeledByView, mLabeledByVirtualId);
        if (first != null) labels.add(first);

        for (int index = 0; index < mExtraLabeledByViews.size(); index++)
        {
            AccessibilityNodeInfo label = createInfo(mExtraLabeledByViews.get(index), mExtraLabeledByVirtualIds.get(index));
            if (label != null) labels.add(label);
        }

        return labels;
    }

    public void setGranularScrollingSupported(boolean granularScrollingSupported)
    {
        mGranularScrollingSupported = granularScrollingSupported;
    }

    public boolean isGranularScrollingSupported()
    {
        return mGranularScrollingSupported;
    }

    public void setViewIdResourceName(String viewIdResName)
    {
        mViewIdResourceName = viewIdResName;
    }

    public String getViewIdResourceName()
    {
        return mViewIdResourceName;
    }

    public int getTextSelectionStart()
    {
        return mTextSelectionStart;
    }

    public int getTextSelectionEnd()
    {
        return mTextSelectionEnd;
    }

    public void setTextSelection(int start, int end)
    {
        mTextSelectionStart = start;
        mTextSelectionEnd = end;
    }

    public int getInputType()
    {
        return mInputType;
    }

    public void setInputType(int inputType)
    {
        mInputType = inputType;
    }

    public Bundle getExtras()
    {
        if (mExtras == null) mExtras = new Bundle();
        return mExtras;
    }

    public TouchDelegateInfo getTouchDelegateInfo()
    {
        return mTouchDelegateInfo;
    }

    public void setTouchDelegateInfo(TouchDelegateInfo delegatedInfo)
    {
        mTouchDelegateInfo = delegatedInfo;
    }

    public void setUniqueId(String uniqueId)
    {
        mUniqueId = uniqueId;
    }

    public String getUniqueId()
    {
        return mUniqueId;
    }

    public void setContainerTitle(CharSequence containerTitle)
    {
        mContainerTitle = containerTitle;
    }

    public CharSequence getContainerTitle()
    {
        return mContainerTitle;
    }

    public void setQueryFromAppProcessEnabled(View view, boolean enabled)
    {
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeString(mClassName == null ? null : mClassName.toString());
        parcel.writeString(mPackageName == null ? null : mPackageName.toString());
        parcel.writeString(mText == null ? null : mText.toString());
        parcel.writeString(mContentDescription == null ? null : mContentDescription.toString());
        parcel.writeInt(mBooleanProperties);
        parcel.writeInt(mActionMask);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof AccessibilityNodeInfo)) return false;

        AccessibilityNodeInfo info = (AccessibilityNodeInfo)other;
        return mSource != null && mSource == info.mSource && mVirtualDescendantId == info.mVirtualDescendantId;
    }

    @Override
    public int hashCode()
    {
        return (mSource == null ? 0 : System.identityHashCode(mSource)) * 31 + mVirtualDescendantId;
    }

    @Override
    public String toString()
    {
        return "AccessibilityNodeInfo{className=" + mClassName + ", text=" + mText + ", contentDescription=" + mContentDescription
               + ", viewId=" + mViewIdResourceName + ", boundsInScreen=" + mBoundsInScreen + "}";
    }

    public static final class AccessibilityAction implements Parcelable
    {
        public static final AccessibilityAction ACTION_FOCUS = new AccessibilityAction(AccessibilityNodeInfo.ACTION_FOCUS, null);
        public static final AccessibilityAction ACTION_CLEAR_FOCUS =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS, null);
        public static final AccessibilityAction ACTION_SELECT = new AccessibilityAction(AccessibilityNodeInfo.ACTION_SELECT, null);
        public static final AccessibilityAction ACTION_CLEAR_SELECTION =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_SELECTION, null);
        public static final AccessibilityAction ACTION_CLICK = new AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null);
        public static final AccessibilityAction ACTION_LONG_CLICK = new AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, null);
        public static final AccessibilityAction ACTION_ACCESSIBILITY_FOCUS =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
        public static final AccessibilityAction ACTION_CLEAR_ACCESSIBILITY_FOCUS =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
        public static final AccessibilityAction ACTION_NEXT_AT_MOVEMENT_GRANULARITY =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, null);
        public static final AccessibilityAction ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, null);
        public static final AccessibilityAction ACTION_NEXT_HTML_ELEMENT =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT, null);
        public static final AccessibilityAction ACTION_PREVIOUS_HTML_ELEMENT =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT, null);
        public static final AccessibilityAction ACTION_SCROLL_FORWARD =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null);
        public static final AccessibilityAction ACTION_SCROLL_BACKWARD =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null);
        public static final AccessibilityAction ACTION_COPY = new AccessibilityAction(AccessibilityNodeInfo.ACTION_COPY, null);
        public static final AccessibilityAction ACTION_PASTE = new AccessibilityAction(AccessibilityNodeInfo.ACTION_PASTE, null);
        public static final AccessibilityAction ACTION_CUT = new AccessibilityAction(AccessibilityNodeInfo.ACTION_CUT, null);
        public static final AccessibilityAction ACTION_SET_SELECTION =
            new AccessibilityAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, null);
        public static final AccessibilityAction ACTION_EXPAND = new AccessibilityAction(AccessibilityNodeInfo.ACTION_EXPAND, null);
        public static final AccessibilityAction ACTION_COLLAPSE = new AccessibilityAction(AccessibilityNodeInfo.ACTION_COLLAPSE, null);
        public static final AccessibilityAction ACTION_DISMISS = new AccessibilityAction(AccessibilityNodeInfo.ACTION_DISMISS, null);
        public static final AccessibilityAction ACTION_SET_TEXT = new AccessibilityAction(AccessibilityNodeInfo.ACTION_SET_TEXT, null);
        public static final AccessibilityAction ACTION_SHOW_ON_SCREEN = new AccessibilityAction(0x01020036, null);
        public static final AccessibilityAction ACTION_SCROLL_TO_POSITION = new AccessibilityAction(0x01020037, null);
        public static final AccessibilityAction ACTION_SCROLL_UP = new AccessibilityAction(0x01020038, null);
        public static final AccessibilityAction ACTION_SCROLL_LEFT = new AccessibilityAction(0x01020039, null);
        public static final AccessibilityAction ACTION_SCROLL_DOWN = new AccessibilityAction(0x0102003a, null);
        public static final AccessibilityAction ACTION_SCROLL_RIGHT = new AccessibilityAction(0x0102003b, null);
        public static final AccessibilityAction ACTION_CONTEXT_CLICK = new AccessibilityAction(0x0102003c, null);
        public static final AccessibilityAction ACTION_SET_PROGRESS = new AccessibilityAction(0x0102003d, null);
        public static final AccessibilityAction ACTION_MOVE_WINDOW = new AccessibilityAction(0x01020042, null);
        public static final AccessibilityAction ACTION_SHOW_TOOLTIP = new AccessibilityAction(0x01020044, null);
        public static final AccessibilityAction ACTION_HIDE_TOOLTIP = new AccessibilityAction(0x01020045, null);
        public static final AccessibilityAction ACTION_PAGE_UP = new AccessibilityAction(0x01020046, null);
        public static final AccessibilityAction ACTION_PAGE_DOWN = new AccessibilityAction(0x01020047, null);
        public static final AccessibilityAction ACTION_PAGE_LEFT = new AccessibilityAction(0x01020048, null);
        public static final AccessibilityAction ACTION_PAGE_RIGHT = new AccessibilityAction(0x01020049, null);
        public static final AccessibilityAction ACTION_PRESS_AND_HOLD = new AccessibilityAction(0x0102004a, null);
        public static final AccessibilityAction ACTION_IME_ENTER = new AccessibilityAction(0x01020054, null);
        public static final AccessibilityAction ACTION_DRAG_START = new AccessibilityAction(0x01020055, null);
        public static final AccessibilityAction ACTION_DRAG_DROP = new AccessibilityAction(0x01020056, null);
        public static final AccessibilityAction ACTION_DRAG_CANCEL = new AccessibilityAction(0x01020057, null);
        public static final AccessibilityAction ACTION_SHOW_TEXT_SUGGESTIONS = new AccessibilityAction(0x01020058, null);
        public static final AccessibilityAction ACTION_SCROLL_IN_DIRECTION = new AccessibilityAction(0x0102005e, null);

        public static final Parcelable.Creator<AccessibilityAction> CREATOR = new Parcelable.Creator<AccessibilityAction>()
        {
            @Override
            public AccessibilityAction createFromParcel(Parcel source)
            {
                int id = source.readInt();
                return new AccessibilityAction(id, source.readString());
            }

            @Override
            public AccessibilityAction[] newArray(int size)
            {
                return new AccessibilityAction[size];
            }
        };

        private final int mId;
        private final CharSequence mLabel;

        public AccessibilityAction(int actionId, CharSequence label)
        {
            mId = actionId;
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
            return "AccessibilityAction: 0x" + Integer.toHexString(mId) + " - " + mLabel;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags)
        {
            parcel.writeInt(mId);
            parcel.writeString(mLabel == null ? null : mLabel.toString());
        }
    }

    public static final class CollectionInfo
    {
        public static final int SELECTION_MODE_NONE = 0;
        public static final int SELECTION_MODE_SINGLE = 1;
        public static final int SELECTION_MODE_MULTIPLE = 2;
        public static final int UNDEFINED = -1;

        private final int mRowCount;
        private final int mColumnCount;
        private final boolean mHierarchical;
        private final int mSelectionMode;
        private int mItemCount = UNDEFINED;
        private int mImportantForAccessibilityItemCount = UNDEFINED;

        public CollectionInfo(int rowCount, int columnCount, boolean hierarchical)
        {
            this(rowCount, columnCount, hierarchical, SELECTION_MODE_NONE);
        }

        public CollectionInfo(int rowCount, int columnCount, boolean hierarchical, int selectionMode)
        {
            mRowCount = rowCount;
            mColumnCount = columnCount;
            mHierarchical = hierarchical;
            mSelectionMode = selectionMode;
        }

        public static CollectionInfo obtain(int rowCount, int columnCount, boolean hierarchical)
        {
            return new CollectionInfo(rowCount, columnCount, hierarchical);
        }

        public static CollectionInfo obtain(int rowCount, int columnCount, boolean hierarchical, int selectionMode)
        {
            return new CollectionInfo(rowCount, columnCount, hierarchical, selectionMode);
        }

        public int getRowCount()
        {
            return mRowCount;
        }

        public int getColumnCount()
        {
            return mColumnCount;
        }

        public boolean isHierarchical()
        {
            return mHierarchical;
        }

        public int getSelectionMode()
        {
            return mSelectionMode;
        }

        public int getItemCount()
        {
            return mItemCount;
        }

        public int getImportantForAccessibilityItemCount()
        {
            return mImportantForAccessibilityItemCount;
        }

        public static final class Builder
        {
            private int mRowCount;
            private int mColumnCount;
            private boolean mHierarchical;
            private int mSelectionMode;
            private int mItemCount = UNDEFINED;
            private int mImportantForAccessibilityItemCount = UNDEFINED;

            public Builder()
            {
            }

            public Builder setRowCount(int rowCount)
            {
                mRowCount = rowCount;
                return this;
            }

            public Builder setColumnCount(int columnCount)
            {
                mColumnCount = columnCount;
                return this;
            }

            public Builder setHierarchical(boolean hierarchical)
            {
                mHierarchical = hierarchical;
                return this;
            }

            public Builder setSelectionMode(int selectionMode)
            {
                mSelectionMode = selectionMode;
                return this;
            }

            public Builder setItemCount(int itemCount)
            {
                mItemCount = itemCount;
                return this;
            }

            public Builder setImportantForAccessibilityItemCount(int count)
            {
                mImportantForAccessibilityItemCount = count;
                return this;
            }

            public CollectionInfo build()
            {
                CollectionInfo info = new CollectionInfo(mRowCount, mColumnCount, mHierarchical, mSelectionMode);
                info.mItemCount = mItemCount;
                info.mImportantForAccessibilityItemCount = mImportantForAccessibilityItemCount;
                return info;
            }
        }
    }

    public static final class CollectionItemInfo
    {
        private int mRowIndex;
        private int mRowSpan;
        private int mColumnIndex;
        private int mColumnSpan;
        private boolean mHeading;
        private boolean mSelected;
        private String mRowTitle;
        private String mColumnTitle;

        public CollectionItemInfo(int rowIndex, int rowSpan, int columnIndex, int columnSpan, boolean heading)
        {
            this(rowIndex, rowSpan, columnIndex, columnSpan, heading, false);
        }

        public CollectionItemInfo(int rowIndex, int rowSpan, int columnIndex, int columnSpan, boolean heading, boolean selected)
        {
            mRowIndex = rowIndex;
            mRowSpan = rowSpan;
            mColumnIndex = columnIndex;
            mColumnSpan = columnSpan;
            mHeading = heading;
            mSelected = selected;
        }

        public static CollectionItemInfo obtain(int rowIndex, int rowSpan, int columnIndex, int columnSpan, boolean heading)
        {
            return new CollectionItemInfo(rowIndex, rowSpan, columnIndex, columnSpan, heading);
        }

        public static CollectionItemInfo obtain(int rowIndex, int rowSpan, int columnIndex, int columnSpan, boolean heading,
                                                boolean selected)
        {
            return new CollectionItemInfo(rowIndex, rowSpan, columnIndex, columnSpan, heading, selected);
        }

        public int getColumnIndex()
        {
            return mColumnIndex;
        }

        public int getRowIndex()
        {
            return mRowIndex;
        }

        public int getColumnSpan()
        {
            return mColumnSpan;
        }

        public int getRowSpan()
        {
            return mRowSpan;
        }

        public boolean isHeading()
        {
            return mHeading;
        }

        public boolean isSelected()
        {
            return mSelected;
        }

        public String getRowTitle()
        {
            return mRowTitle;
        }

        public String getColumnTitle()
        {
            return mColumnTitle;
        }

        public static final class Builder
        {
            private boolean mHeading;
            private int mColumnIndex;
            private int mRowIndex;
            private int mColumnSpan;
            private int mRowSpan;
            private boolean mSelected;
            private String mRowTitle;
            private String mColumnTitle;

            public Builder()
            {
            }

            public Builder setHeading(boolean heading)
            {
                mHeading = heading;
                return this;
            }

            public Builder setColumnIndex(int columnIndex)
            {
                mColumnIndex = columnIndex;
                return this;
            }

            public Builder setRowIndex(int rowIndex)
            {
                mRowIndex = rowIndex;
                return this;
            }

            public Builder setColumnSpan(int columnSpan)
            {
                mColumnSpan = columnSpan;
                return this;
            }

            public Builder setRowSpan(int rowSpan)
            {
                mRowSpan = rowSpan;
                return this;
            }

            public Builder setSelected(boolean selected)
            {
                mSelected = selected;
                return this;
            }

            public Builder setRowTitle(String rowTitle)
            {
                mRowTitle = rowTitle;
                return this;
            }

            public Builder setColumnTitle(String columnTitle)
            {
                mColumnTitle = columnTitle;
                return this;
            }

            public CollectionItemInfo build()
            {
                CollectionItemInfo info = new CollectionItemInfo(mRowIndex, mRowSpan, mColumnIndex, mColumnSpan, mHeading, mSelected);
                info.mRowTitle = mRowTitle;
                info.mColumnTitle = mColumnTitle;
                return info;
            }
        }
    }

    public static final class RangeInfo
    {
        public static final int RANGE_TYPE_INT = 0;
        public static final int RANGE_TYPE_FLOAT = 1;
        public static final int RANGE_TYPE_PERCENT = 2;
        public static final int RANGE_TYPE_INDETERMINATE = 3;

        public static final RangeInfo INDETERMINATE = new RangeInfo(RANGE_TYPE_INDETERMINATE, Float.NEGATIVE_INFINITY,
                                                                    Float.POSITIVE_INFINITY, 0);

        private final int mType;
        private final float mMin;
        private final float mMax;
        private final float mCurrent;

        public RangeInfo(int type, float min, float max, float current)
        {
            mType = type;
            mMin = min;
            mMax = max;
            mCurrent = current;
        }

        public static RangeInfo obtain(int type, float min, float max, float current)
        {
            return new RangeInfo(type, min, max, current);
        }

        public int getType()
        {
            return mType;
        }

        public float getMin()
        {
            return mMin;
        }

        public float getMax()
        {
            return mMax;
        }

        public float getCurrent()
        {
            return mCurrent;
        }
    }

    public static final class ExtraRenderingInfo
    {
        private android.util.Size mLayoutSize;
        private float mTextSizeInPx = -1.0f;
        private int mTextSizeUnit = -1;

        ExtraRenderingInfo()
        {
        }

        public android.util.Size getLayoutSize()
        {
            return mLayoutSize;
        }

        public float getTextSizeInPx()
        {
            return mTextSizeInPx;
        }

        public int getTextSizeUnit()
        {
            return mTextSizeUnit;
        }
    }

    public static final class TouchDelegateInfo implements Parcelable
    {
        public static final Parcelable.Creator<TouchDelegateInfo> CREATOR = new Parcelable.Creator<TouchDelegateInfo>()
        {
            @Override
            public TouchDelegateInfo createFromParcel(Parcel source)
            {
                return new TouchDelegateInfo(new java.util.HashMap<Region, View>());
            }

            @Override
            public TouchDelegateInfo[] newArray(int size)
            {
                return new TouchDelegateInfo[size];
            }
        };

        private final ArrayList<Region> mRegions = new ArrayList<>();
        private final ArrayList<View> mTargets = new ArrayList<>();

        public TouchDelegateInfo(Map<Region, View> targetMap)
        {
            if (targetMap == null) return;

            for (Map.Entry<Region, View> entry : targetMap.entrySet())
            {
                mRegions.add(entry.getKey());
                mTargets.add(entry.getValue());
            }
        }

        public int getRegionCount()
        {
            return mRegions.size();
        }

        public Region getRegionAt(int index)
        {
            return mRegions.get(index);
        }

        public AccessibilityNodeInfo getTargetForRegion(Region region)
        {
            int index = mRegions.indexOf(region);
            return index < 0 ? null : createInfo(mTargets.get(index), UNDEFINED_ID);
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags)
        {
        }
    }
}
