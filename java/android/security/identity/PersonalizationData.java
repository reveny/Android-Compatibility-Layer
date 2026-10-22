package android.security.identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PersonalizationData
{
    private final List<AccessControlProfile> mProfiles = new ArrayList<>();
    private final Map<String, Map<String, byte[]>> mNamespaces = new LinkedHashMap<>();
    private final Map<String, Map<String, Collection<AccessControlProfileId>>> mAccessControlProfileIds = new LinkedHashMap<>();

    private PersonalizationData()
    {
    }

    Collection<AccessControlProfile> getAccessControlProfiles()
    {
        return mProfiles;
    }

    Collection<String> getNamespaces()
    {
        return mNamespaces.keySet();
    }

    Map<String, byte[]> getEntries(String namespace)
    {
        return mNamespaces.get(namespace);
    }

    Collection<AccessControlProfileId> getAccessControlProfileIds(String namespace, String name)
    {
        Map<String, Collection<AccessControlProfileId>> entries = mAccessControlProfileIds.get(namespace);
        return entries != null ? entries.get(name) : null;
    }

    public static final class Builder
    {
        private final PersonalizationData mData = new PersonalizationData();

        public Builder()
        {
        }

        public Builder putEntry(String namespace, String name, Collection<AccessControlProfileId> accessControlProfileIds,
                byte[] value)
        {
            mData.mNamespaces.computeIfAbsent(namespace, key -> new LinkedHashMap<>()).put(name, value);
            mData.mAccessControlProfileIds.computeIfAbsent(namespace, key -> new LinkedHashMap<>()).put(name,
                    accessControlProfileIds);
            return this;
        }

        public Builder addAccessControlProfile(AccessControlProfile profile)
        {
            mData.mProfiles.add(profile);
            return this;
        }

        public PersonalizationData build()
        {
            return mData;
        }
    }
}
