package android.content;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IntentFilter
{
    private final List<String> mActions = new ArrayList<>();
    private final List<String> mCategories = new ArrayList<>();
    private final List<String> mSchemes = new ArrayList<>();
    private final List<String> mTypes = new ArrayList<>();

    private int mPriority;

    public IntentFilter()
    {
    }

    public IntentFilter(String action)
    {
        addAction(action);
    }

    public IntentFilter(String action, String dataType)
    {
        addAction(action);
        addDataType(dataType);
    }

    public final void addAction(String action)
    {
        if (action != null && !mActions.contains(action)) mActions.add(action);
    }

    public final int countActions()
    {
        return mActions.size();
    }

    public final String getAction(int index)
    {
        return mActions.get(index);
    }

    public final boolean hasAction(String action)
    {
        return action != null && mActions.contains(action);
    }

    public final Iterator<String> actionsIterator()
    {
        return mActions.iterator();
    }

    public final void addCategory(String category)
    {
        if (category != null && !mCategories.contains(category)) mCategories.add(category);
    }

    public final boolean hasCategory(String category)
    {
        return category != null && mCategories.contains(category);
    }

    public final void addDataScheme(String scheme)
    {
        if (scheme != null && !mSchemes.contains(scheme)) mSchemes.add(scheme);
    }

    public final boolean hasDataScheme(String scheme)
    {
        return scheme != null && mSchemes.contains(scheme);
    }

    private final java.util.ArrayList<String> mSchemeSpecificParts = new java.util.ArrayList<>();
    private final java.util.ArrayList<String> mDataPaths = new java.util.ArrayList<>();

    public final void addDataSchemeSpecificPart(String part, int type)
    {
        if (part != null && !mSchemeSpecificParts.contains(part)) mSchemeSpecificParts.add(part);
    }

    public final boolean hasDataSchemeSpecificPart(String part)
    {
        return part != null && mSchemeSpecificParts.contains(part);
    }

    public final int countDataSchemeSpecificParts()
    {
        return mSchemeSpecificParts.size();
    }

    public final String getDataSchemeSpecificPart(int index)
    {
        return mSchemeSpecificParts.get(index);
    }

    public final void addDataPath(String path, int type)
    {
        if (path != null && !mDataPaths.contains(path)) mDataPaths.add(path);
    }

    public final boolean hasDataPath(String path)
    {
        return path != null && mDataPaths.contains(path);
    }

    public final int countDataPaths()
    {
        return mDataPaths.size();
    }

    private final java.util.ArrayList<AuthorityEntry> mDataAuthorities = new java.util.ArrayList<>();

    public final void addDataAuthority(String host, String port)
    {
        if (host == null) return;
        if (port != null) port = port.intern();

        mDataAuthorities.add(new AuthorityEntry(host.intern(), port));
    }

    public final int countDataAuthorities()
    {
        return mDataAuthorities.size();
    }

    public final AuthorityEntry getDataAuthority(int index)
    {
        return mDataAuthorities.get(index);
    }

    public final Iterator<AuthorityEntry> authoritiesIterator()
    {
        return mDataAuthorities.iterator();
    }

    public final boolean hasDataAuthority(android.net.Uri data)
    {
        for (AuthorityEntry authority : mDataAuthorities)
        {
            if (authority.match(data) >= 0) return true;
        }

        return false;
    }

    public final String getDataPath(int index)
    {
        return mDataPaths.get(index);
    }

    public void readFromXml(org.xmlpull.v1.XmlPullParser parser) throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int outerDepth = parser.getDepth();
        int type;

        while ((type = parser.next()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT
               && (type != org.xmlpull.v1.XmlPullParser.END_TAG || parser.getDepth() > outerDepth))
        {
            if (type != org.xmlpull.v1.XmlPullParser.START_TAG) continue;

            String tag = parser.getName();
            String name = parser.getAttributeValue(null, "name");

            if ("action".equals(tag))
            {
                if (name != null) addAction(name);
            }
            else if ("cat".equals(tag))
            {
                if (name != null) addCategory(name);
            }
            else if ("type".equals(tag))
            {
                if (name != null) addDataType(name);
            }
            else if ("scheme".equals(tag))
            {
                if (name != null) addDataScheme(name);
            }
            else if ("auth".equals(tag))
            {
                String host = parser.getAttributeValue(null, "host");
                if (host != null) addDataAuthority(host, parser.getAttributeValue(null, "port"));
            }
            else if ("path".equals(tag) || "ssp".equals(tag))
            {
                String literal = parser.getAttributeValue(null, "literal");
                String prefix = parser.getAttributeValue(null, "prefix");
                String glob = parser.getAttributeValue(null, "sglob");
                String value = literal != null ? literal : prefix != null ? prefix : glob;
                int matchType = literal != null ? 0 : prefix != null ? 1 : 2;

                if (value != null && "path".equals(tag)) addDataPath(value, matchType);
                else if (value != null) addDataSchemeSpecificPart(value, matchType);
            }

            skipCurrentTag(parser);
        }
    }

    private static void skipCurrentTag(org.xmlpull.v1.XmlPullParser parser)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int outerDepth = parser.getDepth();
        int type;

        while ((type = parser.next()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT
               && (type != org.xmlpull.v1.XmlPullParser.END_TAG || parser.getDepth() > outerDepth))
        {
        }
    }

    public static final class AuthorityEntry
    {
        private final String mOrigHost;
        private final String mHost;
        private final boolean mWild;
        private final int mPort;

        public AuthorityEntry(String host, String port)
        {
            mOrigHost = host;
            mWild = host.length() > 0 && host.charAt(0) == '*';
            mHost = mWild ? host.substring(1).intern() : host;
            mPort = port != null ? Integer.parseInt(port) : -1;
        }

        public String getHost()
        {
            return mOrigHost;
        }

        public int getPort()
        {
            return mPort;
        }

        public int match(android.net.Uri data)
        {
            String host = data.getHost();
            if (host == null) return NO_MATCH_DATA;

            if (mWild)
            {
                if (host.length() < mHost.length()) return NO_MATCH_DATA;
                host = host.substring(host.length() - mHost.length());
            }

            if (host.compareToIgnoreCase(mHost) != 0) return NO_MATCH_DATA;
            if (mPort >= 0 && mPort != data.getPort()) return NO_MATCH_DATA;

            return mPort >= 0 ? MATCH_CATEGORY_PORT : MATCH_CATEGORY_HOST;
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof AuthorityEntry)) return false;

            AuthorityEntry compared = (AuthorityEntry)other;
            return mWild == compared.mWild && mHost.equals(compared.mHost) && mPort == compared.mPort;
        }

        @Override
        public int hashCode()
        {
            return mHost.hashCode() * 31 + mPort;
        }
    }

    public final void addDataType(String type)
    {
        if (type != null && !mTypes.contains(type)) mTypes.add(type);
    }

    public final boolean hasDataType(String type)
    {
        return type != null && mTypes.contains(type);
    }

    public final void setPriority(int priority)
    {
        mPriority = priority;
    }

    public final int getPriority()
    {
        return mPriority;
    }

    public final boolean matchAction(String action)
    {
        return hasAction(action);
    }

    public final int countCategories()
    {
        return mCategories.size();
    }

    public final String getCategory(int index)
    {
        return mCategories.get(index);
    }

    public final Iterator<String> categoriesIterator()
    {
        return mCategories.iterator();
    }

    public final int countDataSchemes()
    {
        return mSchemes.size();
    }

    public final String getDataScheme(int index)
    {
        return mSchemes.get(index);
    }

    public final int countDataTypes()
    {
        return mTypes.size();
    }

    public final String getDataType(int index)
    {
        return mTypes.get(index);
    }

    public final int match(String action, String type, String scheme, android.net.Uri data, java.util.Set<String> categories,
                           String logTag)
    {
        if (!mActions.isEmpty() && (action == null || !mActions.contains(action))) return NO_MATCH_ACTION;

        if (!mCategories.isEmpty() || categories != null)
        {
            if (categories != null)
            {
                for (String category : categories)
                {
                    if (android.content.Intent.CATEGORY_DEFAULT.equals(category)) continue;
                    if (!mCategories.contains(category)) return NO_MATCH_CATEGORY;
                }
            }
        }

        if (!mSchemes.isEmpty())
        {
            String candidate = scheme != null ? scheme : (data != null ? data.getScheme() : null);
            if (candidate == null || !mSchemes.contains(candidate)) return NO_MATCH_DATA;
        }

        if (!mTypes.isEmpty())
        {
            if (type == null) return NO_MATCH_TYPE;
            if (!matchesType(type)) return NO_MATCH_TYPE;
        }

        return MATCH_CATEGORY_EMPTY + mPriority;
    }

    public final int match(ContentResolver resolver, Intent intent, boolean resolve, String logTag)
    {
        if (intent == null) return NO_MATCH_ACTION;

        return match(intent.getAction(), intent.getType(), intent.getScheme(), intent.getData(), intent.getCategories(), logTag);
    }

    private boolean matchesType(String type)
    {
        for (String candidate : mTypes)
        {
            if (candidate.equals(type)) return true;
            if (!candidate.endsWith("/*")) continue;

            String prefix = candidate.substring(0, candidate.length() - 1);
            if (type.startsWith(prefix)) return true;
        }

        return false;
    }

    public static final int NO_MATCH_ACTION = -3;
    public static final int NO_MATCH_CATEGORY = -4;
    public static final int NO_MATCH_DATA = -2;
    public static final int NO_MATCH_TYPE = -1;
    public static final int MATCH_CATEGORY_EMPTY = 0x00100000;
    public static final int MATCH_CATEGORY_SCHEME = 0x00200000;
    public static final int MATCH_CATEGORY_HOST = 0x00300000;
    public static final int MATCH_CATEGORY_PORT = 0x00400000;
    public static final int MATCH_CATEGORY_PATH = 0x00500000;
    public static final int MATCH_CATEGORY_SCHEME_SPECIFIC_PART = 0x00580000;
    public static final int MATCH_CATEGORY_TYPE = 0x00600000;
    public static final int MATCH_CATEGORY_MASK = 0x0fff0000;
    public static final int MATCH_ADJUSTMENT_MASK = 0x0000ffff;
    public static final int MATCH_ADJUSTMENT_NORMAL = 0x8000;
    public static final int SYSTEM_HIGH_PRIORITY = 1000;
    public static final int SYSTEM_LOW_PRIORITY = -1000;
}
