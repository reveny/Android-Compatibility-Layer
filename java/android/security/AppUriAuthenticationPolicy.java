package android.security;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AppUriAuthenticationPolicy implements Parcelable
{
    public static final Parcelable.Creator<AppUriAuthenticationPolicy> CREATOR = new Parcelable.Creator<AppUriAuthenticationPolicy>()
    {
        @Override
        public AppUriAuthenticationPolicy createFromParcel(Parcel source)
        {
            Map<String, Map<Uri, String>> mappings = new LinkedHashMap<>();
            int appCount = source.readInt();
            for (int appIndex = 0; appIndex < appCount; appIndex++)
            {
                String packageName = source.readString();
                Map<Uri, String> uris = new LinkedHashMap<>();
                int uriCount = source.readInt();
                for (int uriIndex = 0; uriIndex < uriCount; uriIndex++)
                {
                    Uri uri = Uri.parse(source.readString());
                    uris.put(uri, source.readString());
                }
                mappings.put(packageName, uris);
            }
            return new AppUriAuthenticationPolicy(mappings);
        }

        @Override
        public AppUriAuthenticationPolicy[] newArray(int size)
        {
            return new AppUriAuthenticationPolicy[size];
        }
    };

    private final Map<String, Map<Uri, String>> mAppToUris;

    private AppUriAuthenticationPolicy(Map<String, Map<Uri, String>> appToUris)
    {
        mAppToUris = appToUris;
    }

    public Map<String, Map<Uri, String>> getAppAndUriMappings()
    {
        Map<String, Map<Uri, String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Uri, String>> entry : mAppToUris.entrySet())
        {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mAppToUris.size());
        for (Map.Entry<String, Map<Uri, String>> app : mAppToUris.entrySet())
        {
            destination.writeString(app.getKey());
            destination.writeInt(app.getValue().size());
            for (Map.Entry<Uri, String> uri : app.getValue().entrySet())
            {
                destination.writeString(uri.getKey().toString());
                destination.writeString(uri.getValue());
            }
        }
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof AppUriAuthenticationPolicy && ((AppUriAuthenticationPolicy)other).mAppToUris.equals(mAppToUris);
    }

    @Override
    public int hashCode()
    {
        return mAppToUris.hashCode();
    }

    @Override
    public String toString()
    {
        return "AppUriAuthenticationPolicy{mPackageNameToUris=" + mAppToUris + "}";
    }

    public static final class Builder
    {
        private final Map<String, Map<Uri, String>> mPackageNameToUris = new LinkedHashMap<>();

        public Builder()
        {
        }

        public Builder addAppAndUriMapping(String appPackageName, Uri uri, String alias)
        {
            mPackageNameToUris.computeIfAbsent(appPackageName, key -> new LinkedHashMap<>()).put(uri, alias);
            return this;
        }

        public AppUriAuthenticationPolicy build()
        {
            Map<String, Map<Uri, String>> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<Uri, String>> entry : mPackageNameToUris.entrySet())
            {
                copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
            }
            return new AppUriAuthenticationPolicy(copy);
        }
    }
}
