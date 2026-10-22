package android.view.accessibility;

import android.graphics.Rect;
import android.graphics.Region;
import android.os.Parcel;
import android.os.Parcelable;

public final class AccessibilityWindowInfo implements Parcelable
{
    public static final int TYPE_APPLICATION = 1;
    public static final int TYPE_INPUT_METHOD = 2;
    public static final int TYPE_SYSTEM = 3;
    public static final int TYPE_ACCESSIBILITY_OVERLAY = 4;
    public static final int TYPE_SPLIT_SCREEN_DIVIDER = 5;
    public static final int TYPE_MAGNIFICATION_OVERLAY = 6;
    public static final int TYPE_WINDOW_CONTROL = 7;

    public static final Parcelable.Creator<AccessibilityWindowInfo> CREATOR = new Parcelable.Creator<AccessibilityWindowInfo>()
    {
        @Override
        public AccessibilityWindowInfo createFromParcel(Parcel source)
        {
            AccessibilityWindowInfo info = new AccessibilityWindowInfo();
            info.mType = source.readInt();
            info.mLayer = source.readInt();
            info.mId = source.readInt();
            info.mDisplayId = source.readInt();
            info.mTitle = source.readString();
            return info;
        }

        @Override
        public AccessibilityWindowInfo[] newArray(int size)
        {
            return new AccessibilityWindowInfo[size];
        }
    };

    private final Rect mBounds = new Rect();
    private final java.util.ArrayList<AccessibilityWindowInfo> mChildren = new java.util.ArrayList<>();

    private int mType = TYPE_APPLICATION;
    private int mLayer;
    private int mId = -1;
    private int mDisplayId;
    private boolean mActive;
    private boolean mFocused;
    private boolean mAccessibilityFocused;
    private boolean mPictureInPicture;
    private long mTransitionTimeMillis;
    private CharSequence mTitle;
    private AccessibilityWindowInfo mParent;
    private AccessibilityNodeInfo mRoot;
    private AccessibilityNodeInfo mAnchor;

    public AccessibilityWindowInfo()
    {
    }

    public AccessibilityWindowInfo(AccessibilityWindowInfo info)
    {
        if (info == null) return;

        mBounds.set(info.mBounds);
        mChildren.addAll(info.mChildren);
        mType = info.mType;
        mLayer = info.mLayer;
        mId = info.mId;
        mDisplayId = info.mDisplayId;
        mActive = info.mActive;
        mFocused = info.mFocused;
        mAccessibilityFocused = info.mAccessibilityFocused;
        mPictureInPicture = info.mPictureInPicture;
        mTransitionTimeMillis = info.mTransitionTimeMillis;
        mTitle = info.mTitle;
        mParent = info.mParent;
        mRoot = info.mRoot;
        mAnchor = info.mAnchor;
    }

    public static AccessibilityWindowInfo obtain()
    {
        return new AccessibilityWindowInfo();
    }

    public static AccessibilityWindowInfo obtain(AccessibilityWindowInfo info)
    {
        return new AccessibilityWindowInfo(info);
    }

    public void recycle()
    {
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    public int getType()
    {
        return mType;
    }

    public int getLayer()
    {
        return mLayer;
    }

    public AccessibilityNodeInfo getRoot()
    {
        return mRoot;
    }

    public AccessibilityNodeInfo getRoot(int prefetchingStrategy)
    {
        return mRoot;
    }

    public AccessibilityNodeInfo getAnchor()
    {
        return mAnchor;
    }

    public boolean isInPictureInPictureMode()
    {
        return mPictureInPicture;
    }

    public AccessibilityWindowInfo getParent()
    {
        return mParent;
    }

    public int getId()
    {
        return mId;
    }

    public void getRegionInScreen(Region outRegion)
    {
        if (outRegion != null) outRegion.set(mBounds);
    }

    public void getBoundsInScreen(Rect outBounds)
    {
        if (outBounds != null) outBounds.set(mBounds);
    }

    public boolean isActive()
    {
        return mActive;
    }

    public boolean isFocused()
    {
        return mFocused;
    }

    public boolean isAccessibilityFocused()
    {
        return mAccessibilityFocused;
    }

    public int getChildCount()
    {
        return mChildren.size();
    }

    public AccessibilityWindowInfo getChild(int index)
    {
        return mChildren.get(index);
    }

    public int getDisplayId()
    {
        return mDisplayId;
    }

    public long getTransitionTimeMillis()
    {
        return mTransitionTimeMillis;
    }

    public android.os.LocaleList getLocales()
    {
        return android.os.LocaleList.getEmptyLocaleList();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mType);
        parcel.writeInt(mLayer);
        parcel.writeInt(mId);
        parcel.writeInt(mDisplayId);
        parcel.writeString(mTitle == null ? null : mTitle.toString());
    }

    @Override
    public int hashCode()
    {
        return mId;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof AccessibilityWindowInfo)) return false;

        return mId == ((AccessibilityWindowInfo)other).mId;
    }

    @Override
    public String toString()
    {
        return "AccessibilityWindowInfo[title=" + mTitle + ", id=" + mId + ", type=" + mType + ", layer=" + mLayer + ", bounds=" + mBounds
               + "]";
    }
}
