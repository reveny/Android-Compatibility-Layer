package android.view.textclassifier;

import android.os.Parcel;
import android.os.Parcelable;

public final class TextClassificationContext implements Parcelable
{
    private final String mPackageName;
    private final String mWidgetType;
    private final String mWidgetVersion;

    private TextClassificationContext(String packageName, String widgetType, String widgetVersion)
    {
        mPackageName = packageName;
        mWidgetType = widgetType;
        mWidgetVersion = widgetVersion;
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    public String getWidgetType()
    {
        return mWidgetType;
    }

    public String getWidgetVersion()
    {
        return mWidgetVersion;
    }

    @Override
    public String toString()
    {
        return "TextClassificationContext{packageName=" + mPackageName + ", widgetType=" + mWidgetType + ", widgetVersion="
               + mWidgetVersion + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mPackageName);
        dest.writeString(mWidgetType);
        dest.writeString(mWidgetVersion);
    }

    public static final Creator<TextClassificationContext> CREATOR = new Creator<TextClassificationContext>()
    {
        @Override
        public TextClassificationContext createFromParcel(Parcel source)
        {
            return new TextClassificationContext(source.readString(), source.readString(), source.readString());
        }

        @Override
        public TextClassificationContext[] newArray(int size)
        {
            return new TextClassificationContext[size];
        }
    };

    public static final class Builder
    {
        private final String mPackageName;
        private final String mWidgetType;
        private String mWidgetVersion;

        public Builder(String packageName, String widgetType)
        {
            if (packageName == null) throw new NullPointerException("packageName");
            if (widgetType == null) throw new NullPointerException("widgetType");

            mPackageName = packageName;
            mWidgetType = widgetType;
        }

        public Builder setWidgetVersion(String widgetVersion)
        {
            mWidgetVersion = widgetVersion;
            return this;
        }

        public TextClassificationContext build()
        {
            return new TextClassificationContext(mPackageName, mWidgetType, mWidgetVersion);
        }
    }
}
