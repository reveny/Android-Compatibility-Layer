package android.view.accessibility;

import android.os.Parcelable;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityRecord
{
    private final List<CharSequence> mText = new ArrayList<>();

    private int mSourceId = View.NO_ID;
    private boolean mChecked;
    private boolean mEnabled = true;
    private boolean mPassword;
    private boolean mFullScreen;
    private boolean mScrollable;
    private int mItemCount = -1;
    private int mCurrentItemIndex = -1;
    private int mFromIndex = -1;
    private int mToIndex = -1;
    private int mScrollX;
    private int mScrollY;
    private int mScrollDeltaX = -1;
    private int mScrollDeltaY = -1;
    private int mMaxScrollX;
    private int mMaxScrollY;
    private int mAddedCount = -1;
    private int mRemovedCount = -1;
    private CharSequence mClassName;
    private CharSequence mBeforeText;
    private CharSequence mContentDescription;
    private Parcelable mParcelableData;

    public AccessibilityRecord()
    {
    }

    public AccessibilityRecord(AccessibilityRecord record)
    {
        if (record == null) return;

        mText.addAll(record.mText);
        mSourceId = record.mSourceId;
        mChecked = record.mChecked;
        mEnabled = record.mEnabled;
        mPassword = record.mPassword;
        mFullScreen = record.mFullScreen;
        mScrollable = record.mScrollable;
        mItemCount = record.mItemCount;
        mCurrentItemIndex = record.mCurrentItemIndex;
        mFromIndex = record.mFromIndex;
        mToIndex = record.mToIndex;
        mScrollX = record.mScrollX;
        mScrollY = record.mScrollY;
        mScrollDeltaX = record.mScrollDeltaX;
        mScrollDeltaY = record.mScrollDeltaY;
        mMaxScrollX = record.mMaxScrollX;
        mMaxScrollY = record.mMaxScrollY;
        mAddedCount = record.mAddedCount;
        mRemovedCount = record.mRemovedCount;
        mClassName = record.mClassName;
        mBeforeText = record.mBeforeText;
        mContentDescription = record.mContentDescription;
        mParcelableData = record.mParcelableData;
    }

    public static AccessibilityRecord obtain()
    {
        return new AccessibilityRecord();
    }

    public static AccessibilityRecord obtain(AccessibilityRecord record)
    {
        return new AccessibilityRecord(record);
    }

    public void recycle()
    {
    }

    public void setSource(View source)
    {
        mSourceId = source == null ? View.NO_ID : source.getId();
    }

    public void setSource(View root, int virtualDescendantId)
    {
        mSourceId = virtualDescendantId;
    }

    public AccessibilityNodeInfo getSource()
    {
        return null;
    }

    public AccessibilityNodeInfo getSource(int prefetchingStrategy)
    {
        return null;
    }

    public int getDisplayId()
    {
        return 0;
    }

    public int getWindowId()
    {
        return -1;
    }

    public boolean isChecked()
    {
        return mChecked;
    }

    public void setChecked(boolean checked)
    {
        mChecked = checked;
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    public boolean isPassword()
    {
        return mPassword;
    }

    public void setPassword(boolean password)
    {
        mPassword = password;
    }

    public boolean isFullScreen()
    {
        return mFullScreen;
    }

    public void setFullScreen(boolean fullScreen)
    {
        mFullScreen = fullScreen;
    }

    public boolean isScrollable()
    {
        return mScrollable;
    }

    public void setScrollable(boolean scrollable)
    {
        mScrollable = scrollable;
    }

    public int getItemCount()
    {
        return mItemCount;
    }

    public void setItemCount(int itemCount)
    {
        mItemCount = itemCount;
    }

    public int getCurrentItemIndex()
    {
        return mCurrentItemIndex;
    }

    public void setCurrentItemIndex(int currentItemIndex)
    {
        mCurrentItemIndex = currentItemIndex;
    }

    public int getFromIndex()
    {
        return mFromIndex;
    }

    public void setFromIndex(int fromIndex)
    {
        mFromIndex = fromIndex;
    }

    public int getToIndex()
    {
        return mToIndex;
    }

    public void setToIndex(int toIndex)
    {
        mToIndex = toIndex;
    }

    public int getScrollX()
    {
        return mScrollX;
    }

    public void setScrollX(int scrollX)
    {
        mScrollX = scrollX;
    }

    public int getScrollY()
    {
        return mScrollY;
    }

    public void setScrollY(int scrollY)
    {
        mScrollY = scrollY;
    }

    public int getScrollDeltaX()
    {
        return mScrollDeltaX;
    }

    public void setScrollDeltaX(int scrollDeltaX)
    {
        mScrollDeltaX = scrollDeltaX;
    }

    public int getScrollDeltaY()
    {
        return mScrollDeltaY;
    }

    public void setScrollDeltaY(int scrollDeltaY)
    {
        mScrollDeltaY = scrollDeltaY;
    }

    public int getMaxScrollX()
    {
        return mMaxScrollX;
    }

    public void setMaxScrollX(int maxScrollX)
    {
        mMaxScrollX = maxScrollX;
    }

    public int getMaxScrollY()
    {
        return mMaxScrollY;
    }

    public void setMaxScrollY(int maxScrollY)
    {
        mMaxScrollY = maxScrollY;
    }

    public int getAddedCount()
    {
        return mAddedCount;
    }

    public void setAddedCount(int addedCount)
    {
        mAddedCount = addedCount;
    }

    public int getRemovedCount()
    {
        return mRemovedCount;
    }

    public void setRemovedCount(int removedCount)
    {
        mRemovedCount = removedCount;
    }

    public CharSequence getClassName()
    {
        return mClassName;
    }

    public void setClassName(CharSequence className)
    {
        mClassName = className;
    }

    public List<CharSequence> getText()
    {
        return mText;
    }

    public CharSequence getBeforeText()
    {
        return mBeforeText;
    }

    public void setBeforeText(CharSequence beforeText)
    {
        mBeforeText = beforeText;
    }

    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    public void setContentDescription(CharSequence contentDescription)
    {
        mContentDescription = contentDescription;
    }

    public Parcelable getParcelableData()
    {
        return mParcelableData;
    }

    public void setParcelableData(Parcelable parcelableData)
    {
        mParcelableData = parcelableData;
    }

    void writeRecordToParcel(android.os.Parcel dest, int flags)
    {
        dest.writeInt(mSourceId);
        dest.writeInt(mChecked ? 1 : 0);
        dest.writeInt(mEnabled ? 1 : 0);
        dest.writeInt(mPassword ? 1 : 0);
        dest.writeInt(mFullScreen ? 1 : 0);
        dest.writeInt(mScrollable ? 1 : 0);
        dest.writeInt(mItemCount);
        dest.writeInt(mCurrentItemIndex);
        dest.writeInt(mFromIndex);
        dest.writeInt(mToIndex);
        dest.writeInt(mScrollX);
        dest.writeInt(mScrollY);
        dest.writeInt(mScrollDeltaX);
        dest.writeInt(mScrollDeltaY);
        dest.writeInt(mMaxScrollX);
        dest.writeInt(mMaxScrollY);
        dest.writeInt(mAddedCount);
        dest.writeInt(mRemovedCount);
        android.text.TextUtils.writeToParcel(mClassName, dest, flags);
        android.text.TextUtils.writeToParcel(mContentDescription, dest, flags);
        android.text.TextUtils.writeToParcel(mBeforeText, dest, flags);
        dest.writeParcelable(mParcelableData, flags);
        dest.writeInt(mText.size());
        for (CharSequence text : mText) android.text.TextUtils.writeToParcel(text, dest, flags);
    }

    void readRecordFromParcel(android.os.Parcel source)
    {
        mSourceId = source.readInt();
        mChecked = source.readInt() != 0;
        mEnabled = source.readInt() != 0;
        mPassword = source.readInt() != 0;
        mFullScreen = source.readInt() != 0;
        mScrollable = source.readInt() != 0;
        mItemCount = source.readInt();
        mCurrentItemIndex = source.readInt();
        mFromIndex = source.readInt();
        mToIndex = source.readInt();
        mScrollX = source.readInt();
        mScrollY = source.readInt();
        mScrollDeltaX = source.readInt();
        mScrollDeltaY = source.readInt();
        mMaxScrollX = source.readInt();
        mMaxScrollY = source.readInt();
        mAddedCount = source.readInt();
        mRemovedCount = source.readInt();
        mClassName = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mContentDescription = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mBeforeText = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mParcelableData = source.readParcelable(null);

        mText.clear();
        int count = source.readInt();
        for (int index = 0; index < count; index++) mText.add(android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source));
    }

    @Override
    public String toString()
    {
        return "AccessibilityRecord[className=" + mClassName + " text=" + mText + "]";
    }
}
