package android.net;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Uri implements Comparable<Uri>
{
    public static final Uri EMPTY = new Uri("");

    private final String mText;
    private final String mScheme;
    private final String mAuthority;
    private final String mPath;
    private final String mQuery;
    private final String mFragment;

    private Uri(String text)
    {
        mText = text == null ? "" : text;

        String rest = mText;
        int fragment = rest.indexOf('#');
        if (fragment >= 0)
        {
            mFragment = rest.substring(fragment + 1);
            rest = rest.substring(0, fragment);
        }
        else
        {
            mFragment = null;
        }

        int query = rest.indexOf('?');
        if (query >= 0)
        {
            mQuery = rest.substring(query + 1);
            rest = rest.substring(0, query);
        }
        else
        {
            mQuery = null;
        }

        int colon = rest.indexOf(':');
        if (colon >= 0)
        {
            mScheme = rest.substring(0, colon);
            rest = rest.substring(colon + 1);
        }
        else
        {
            mScheme = null;
        }

        if (rest.startsWith("//"))
        {
            rest = rest.substring(2);
            int slash = rest.indexOf('/');
            if (slash >= 0)
            {
                mAuthority = rest.substring(0, slash);
                mPath = rest.substring(slash);
            }
            else
            {
                mAuthority = rest;
                mPath = "";
            }
        }
        else
        {
            mAuthority = null;
            mPath = rest;
        }
    }

    public static Uri parse(String text)
    {
        return new Uri(text);
    }

    public static Uri fromFile(File file)
    {
        return new Uri("file://" + file.getAbsolutePath());
    }

    public static Uri fromParts(String scheme, String schemeSpecificPart, String fragment)
    {
        String text = scheme + ":" + schemeSpecificPart;
        return new Uri(fragment == null ? text : text + "#" + fragment);
    }

    public static Uri withAppendedPath(Uri base, String segment)
    {
        if (base == null) return null;
        if (segment == null || segment.isEmpty()) return base;

        String text = base.mText;
        if (text.endsWith("/")) return new Uri(text + segment);

        return new Uri(text + "/" + segment);
    }

    public static String encode(String text)
    {
        return text;
    }

    public static String decode(String text)
    {
        return text;
    }

    public boolean isAbsolute()
    {
        return mScheme != null;
    }

    public boolean isRelative()
    {
        return mScheme == null;
    }

    public boolean isHierarchical()
    {
        return mAuthority != null || mPath.startsWith("/");
    }

    public boolean isOpaque()
    {
        return !isHierarchical();
    }

    public String getScheme()
    {
        return mScheme;
    }

    public String getAuthority()
    {
        return mAuthority;
    }

    public String getHost()
    {
        if (mAuthority == null) return null;

        int at = mAuthority.lastIndexOf('@');
        String host = at >= 0 ? mAuthority.substring(at + 1) : mAuthority;

        int port = host.lastIndexOf(':');
        return port >= 0 ? host.substring(0, port) : host;
    }

    public int getPort()
    {
        if (mAuthority == null) return -1;

        int at = mAuthority.lastIndexOf('@');
        String host = at >= 0 ? mAuthority.substring(at + 1) : mAuthority;

        int port = host.lastIndexOf(':');
        if (port < 0) return -1;

        try
        {
            return Integer.parseInt(host.substring(port + 1));
        }
        catch (NumberFormatException malformed)
        {
            return -1;
        }
    }

    public String getPath()
    {
        return mPath;
    }

    public String getQuery()
    {
        return mQuery;
    }

    public String getFragment()
    {
        return mFragment;
    }

    public String getSchemeSpecificPart()
    {
        int colon = mText.indexOf(':');
        if (colon < 0) return mText;

        int end = mText.indexOf('#');
        return end < 0 ? mText.substring(colon + 1) : mText.substring(colon + 1, end);
    }

    public List<String> getPathSegments()
    {
        List<String> segments = new ArrayList<>();
        for (String segment : mPath.split("/"))
        {
            if (!segment.isEmpty()) segments.add(segment);
        }

        return segments;
    }

    public String getLastPathSegment()
    {
        List<String> segments = getPathSegments();
        return segments.isEmpty() ? null : segments.get(segments.size() - 1);
    }

    public String getQueryParameter(String name)
    {
        if (mQuery == null || name == null) return null;

        for (String pair : mQuery.split("&"))
        {
            int equals = pair.indexOf('=');
            String key = equals < 0 ? pair : pair.substring(0, equals);
            if (!key.equals(name)) continue;

            return equals < 0 ? "" : pair.substring(equals + 1);
        }

        return null;
    }

    public Builder buildUpon()
    {
        Builder builder = new Builder().scheme(mScheme).authority(mAuthority).query(mQuery).fragment(mFragment);
        return mPath.isEmpty() ? builder : builder.path(mPath);
    }

    public Uri normalizeScheme()
    {
        if (mScheme == null) return this;

        String lowered = mScheme.toLowerCase();
        return lowered.equals(mScheme) ? this : new Uri(lowered + mText.substring(mScheme.length()));
    }

    @Override
    public int compareTo(Uri other)
    {
        return mText.compareTo(other.mText);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof Uri && ((Uri)other).mText.equals(mText);
    }

    @Override
    public int hashCode()
    {
        return mText.hashCode();
    }

    @Override
    public String toString()
    {
        return mText;
    }

    public static final class Builder
    {
        private String mScheme;
        private String mAuthority;
        private final StringBuilder mPath = new StringBuilder();
        private final StringBuilder mQuery = new StringBuilder();
        private String mFragment;

        public Builder scheme(String scheme)
        {
            mScheme = scheme;
            return this;
        }

        public Builder authority(String authority)
        {
            mAuthority = authority;
            return this;
        }

        public Builder path(String path)
        {
            mPath.setLength(0);
            if (path != null) mPath.append(path.startsWith("/") ? path : "/" + path);
            return this;
        }

        public Builder appendPath(String segment)
        {
            if (segment != null && !segment.isEmpty())
            {
                if (mPath.length() == 0 || mPath.charAt(mPath.length() - 1) != '/') mPath.append('/');
                mPath.append(segment);
            }

            return this;
        }

        public Builder appendEncodedPath(String segment)
        {
            return appendPath(segment);
        }

        public Builder encodedPath(String path)
        {
            return path(path);
        }

        public Builder query(String query)
        {
            mQuery.setLength(0);
            if (query != null) mQuery.append(query);
            return this;
        }

        public Builder appendQueryParameter(String name, String value)
        {
            if (mQuery.length() > 0) mQuery.append('&');
            mQuery.append(name).append('=').append(value == null ? "" : value);
            return this;
        }

        public Builder fragment(String fragment)
        {
            mFragment = fragment;
            return this;
        }

        public Uri build()
        {
            StringBuilder text = new StringBuilder();
            if (mScheme != null) text.append(mScheme).append(':');
            if (mAuthority != null) text.append("//").append(mAuthority);
            text.append(mPath);
            if (mQuery.length() > 0) text.append('?').append(mQuery);
            if (mFragment != null) text.append('#').append(mFragment);

            return new Uri(text.toString());
        }

        @Override
        public String toString()
        {
            return build().toString();
        }
    }
}
