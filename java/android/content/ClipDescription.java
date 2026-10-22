package android.content;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.ArrayList;

public class ClipDescription implements Parcelable
{
    public static final String MIMETYPE_TEXT_PLAIN = "text/plain";
    public static final String MIMETYPE_TEXT_HTML = "text/html";
    public static final String MIMETYPE_TEXT_URILIST = "text/uri-list";
    public static final String MIMETYPE_TEXT_INTENT = "text/vnd.android.intent";
    public static final String MIMETYPE_UNKNOWN = "application/octet-stream";
    public static final String EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE";
    public static final String EXTRA_IS_REMOTE_DEVICE = "android.content.extra.IS_REMOTE_DEVICE";

    public static final int CLASSIFICATION_NOT_COMPLETE = 1;
    public static final int CLASSIFICATION_NOT_PERFORMED = 2;
    public static final int CLASSIFICATION_COMPLETE = 3;

    public static final Parcelable.Creator<ClipDescription> CREATOR = new Parcelable.Creator<ClipDescription>()
    {
        @Override
        public ClipDescription createFromParcel(Parcel source)
        {
            CharSequence label = source.readString();
            int count = source.readInt();
            String[] mimeTypes = new String[Math.max(count, 0)];

            for (int index = 0; index < mimeTypes.length; index++)
            {
                mimeTypes[index] = source.readString();
            }

            ClipDescription description = new ClipDescription(label, mimeTypes);
            description.mTimestamp = source.readLong();
            return description;
        }

        @Override
        public ClipDescription[] newArray(int size)
        {
            return new ClipDescription[size];
        }
    };

    private final CharSequence mLabel;
    private final String[] mMimeTypes;
    private PersistableBundle mExtras;
    private long mTimestamp;

    public ClipDescription(CharSequence label, String[] mimeTypes)
    {
        if (mimeTypes == null) throw new NullPointerException("mimeTypes is null");

        mLabel = label;
        mMimeTypes = mimeTypes;
        mTimestamp = android.os.SystemClock.elapsedRealtime();
    }

    public ClipDescription(ClipDescription other)
    {
        mLabel = other.mLabel;
        mMimeTypes = other.mMimeTypes.clone();
        mExtras = other.mExtras;
        mTimestamp = other.mTimestamp;
    }

    public static boolean compareMimeTypes(String concreteType, String desiredType)
    {
        int typeLength = desiredType.length();
        if (typeLength == 3 && desiredType.equals("*/*")) return true;

        int slashPosition = desiredType.indexOf('/');
        if (slashPosition > 0)
        {
            if (typeLength == slashPosition + 2 && desiredType.charAt(slashPosition + 1) == '*')
            {
                if (desiredType.regionMatches(0, concreteType, 0, slashPosition + 1)) return true;
            }
            else if (desiredType.equals(concreteType))
            {
                return true;
            }
        }

        return false;
    }

    public long getTimestamp()
    {
        return mTimestamp;
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public boolean hasMimeType(String mimeType)
    {
        for (String candidate : mMimeTypes)
        {
            if (candidate != null && compareMimeTypes(candidate, mimeType)) return true;
        }

        return false;
    }

    public String[] filterMimeTypes(String mimeType)
    {
        ArrayList<String> array = null;

        for (String candidate : mMimeTypes)
        {
            if (!compareMimeTypes(candidate, mimeType)) continue;

            if (array == null) array = new ArrayList<>();
            array.add(candidate);
        }

        return array == null ? null : array.toArray(new String[0]);
    }

    public int getMimeTypeCount()
    {
        return mMimeTypes.length;
    }

    public String getMimeType(int index)
    {
        return mMimeTypes[index];
    }

    public PersistableBundle getExtras()
    {
        return mExtras;
    }

    public void setExtras(PersistableBundle extras)
    {
        mExtras = extras == null ? null : new PersistableBundle(extras);
    }

    public boolean isStyledText()
    {
        return false;
    }

    public float getConfidenceScore(String entityType)
    {
        throw new IllegalStateException("getConfidenceScore() invoked before classification happened");
    }

    public int getClassificationStatus()
    {
        return CLASSIFICATION_NOT_PERFORMED;
    }

    @Override
    public String toString()
    {
        StringBuilder described = new StringBuilder("ClipDescription { ");
        for (int index = 0; index < mMimeTypes.length; index++)
        {
            if (index > 0) described.append(' ');
            described.append(mMimeTypes[index]);
        }

        if (mLabel != null) described.append(" \"").append(mLabel).append('"');
        return described.append(" }").toString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mLabel == null ? null : mLabel.toString());
        destination.writeInt(mMimeTypes.length);

        for (String mimeType : mMimeTypes)
        {
            destination.writeString(mimeType);
        }

        destination.writeLong(mTimestamp);
    }
}
