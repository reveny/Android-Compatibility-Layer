package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Uri implements Parcelable, Comparable<Uri>
{
    public static final Uri EMPTY = new Uri(null, null, null, "", null, null);

    public static final Parcelable.Creator<Uri> CREATOR = new Parcelable.Creator<Uri>()
    {
        @Override
        public Uri createFromParcel(Parcel source)
        {
            String text = source.readString();
            return text == null ? null : parse(text);
        }

        @Override
        public Uri[] newArray(int size)
        {
            return new Uri[size];
        }
    };

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
    private static final String DEFAULT_ENCODING_ALLOWED = "_-!.~'()*";

    private final String mText;
    private final String mScheme;
    private final String mEncodedOpaquePart;
    private final String mEncodedAuthority;
    private final String mEncodedPath;
    private final String mEncodedQuery;
    private final String mEncodedFragment;

    private Uri(String text)
    {
        mText = text;

        int schemeSeparator = findSchemeSeparator(text);
        mScheme = schemeSeparator < 0 ? null : text.substring(0, schemeSeparator);

        int fragmentSeparator = text.indexOf('#', schemeSeparator < 0 ? 0 : schemeSeparator);
        mEncodedFragment = fragmentSeparator < 0 ? null : text.substring(fragmentSeparator + 1);

        String schemeSpecific = text.substring(schemeSeparator + 1, fragmentSeparator < 0 ? text.length() : fragmentSeparator);

        boolean hierarchical = schemeSeparator < 0 || schemeSpecific.startsWith("/");
        if (!hierarchical)
        {
            mEncodedOpaquePart = schemeSpecific;
            mEncodedAuthority = null;
            mEncodedPath = null;
            mEncodedQuery = null;
            return;
        }

        mEncodedOpaquePart = null;

        int querySeparator = schemeSpecific.indexOf('?');
        mEncodedQuery = querySeparator < 0 ? null : schemeSpecific.substring(querySeparator + 1);

        String rest = querySeparator < 0 ? schemeSpecific : schemeSpecific.substring(0, querySeparator);
        if (rest.startsWith("//"))
        {
            int slash = rest.indexOf('/', 2);
            mEncodedAuthority = slash < 0 ? rest.substring(2) : rest.substring(2, slash);
            mEncodedPath = slash < 0 ? "" : rest.substring(slash);
        }
        else
        {
            mEncodedAuthority = null;
            mEncodedPath = rest;
        }
    }

    private Uri(String scheme, String encodedOpaquePart, String encodedAuthority, String encodedPath, String encodedQuery,
                String encodedFragment)
    {
        mScheme = scheme;
        mEncodedOpaquePart = encodedOpaquePart;
        mEncodedAuthority = encodedOpaquePart != null ? null : encodedAuthority;
        mEncodedPath = encodedOpaquePart != null ? null : (encodedPath == null ? "" : encodedPath);
        mEncodedQuery = encodedOpaquePart != null ? null : encodedQuery;
        mEncodedFragment = encodedFragment;

        StringBuilder text = new StringBuilder();
        if (mScheme != null) text.append(mScheme).append(':');
        text.append(encodedSchemeSpecificPart());
        if (mEncodedFragment != null) text.append('#').append(mEncodedFragment);
        mText = text.toString();
    }

    private static int findSchemeSeparator(String text)
    {
        for (int index = 0; index < text.length(); index++)
        {
            char character = text.charAt(index);
            if (character == ':') return index;
            if (character == '/' || character == '?' || character == '#') return -1;
        }

        return -1;
    }

    private String encodedSchemeSpecificPart()
    {
        if (mEncodedOpaquePart != null) return mEncodedOpaquePart;

        StringBuilder schemeSpecific = new StringBuilder();
        if (mEncodedAuthority != null) schemeSpecific.append("//").append(mEncodedAuthority);
        if (mEncodedPath != null) schemeSpecific.append(mEncodedPath);
        if (mEncodedQuery != null) schemeSpecific.append('?').append(mEncodedQuery);
        return schemeSpecific.toString();
    }

    public static Uri parse(String text)
    {
        if (text == null) throw new NullPointerException("uriString");
        return new Uri(text);
    }

    public static Uri fromFile(File file)
    {
        if (file == null) throw new NullPointerException("file");
        return new Uri("file", null, "", encode(file.getAbsolutePath(), "/"), null, null);
    }

    public static Uri fromParts(String scheme, String schemeSpecificPart, String fragment)
    {
        if (scheme == null) throw new NullPointerException("scheme");
        if (schemeSpecificPart == null) throw new NullPointerException("ssp");

        return new Uri(scheme, encode(schemeSpecificPart), null, null, null, encode(fragment));
    }

    public static Uri withAppendedPath(Uri baseUri, String pathSegment)
    {
        return baseUri.buildUpon().appendEncodedPath(pathSegment).build();
    }

    public static void writeToParcel(Parcel out, Uri uri)
    {
        out.writeString(uri == null ? null : uri.toString());
    }

    public static String encode(String text)
    {
        return encode(text, null);
    }

    public static String encode(String text, String allow)
    {
        if (text == null) return null;

        StringBuilder encoded = null;
        int length = text.length();
        int current = 0;

        while (current < length)
        {
            int nextToEncode = current;
            while (nextToEncode < length && isAllowed(text.charAt(nextToEncode), allow)) nextToEncode++;

            if (nextToEncode == length)
            {
                if (current == 0) return text;
                encoded.append(text, current, length);
                return encoded.toString();
            }

            if (encoded == null) encoded = new StringBuilder();
            if (nextToEncode > current) encoded.append(text, current, nextToEncode);

            current = nextToEncode;
            int nextAllowed = current + 1;
            while (nextAllowed < length && !isAllowed(text.charAt(nextAllowed), allow)) nextAllowed++;

            byte[] bytes = text.substring(current, nextAllowed).getBytes(StandardCharsets.UTF_8);
            for (byte value : bytes)
            {
                encoded.append('%');
                encoded.append(HEX_DIGITS[(value & 0xf0) >> 4]);
                encoded.append(HEX_DIGITS[value & 0xf]);
            }

            current = nextAllowed;
        }

        return encoded == null ? text : encoded.toString();
    }

    private static boolean isAllowed(char character, String allow)
    {
        if (character >= 'A' && character <= 'Z') return true;
        if (character >= 'a' && character <= 'z') return true;
        if (character >= '0' && character <= '9') return true;
        if (DEFAULT_ENCODING_ALLOWED.indexOf(character) != -1) return true;

        return allow != null && allow.indexOf(character) != -1;
    }

    public static String decode(String text)
    {
        return decode(text, false);
    }

    private static String decode(String text, boolean convertPlus)
    {
        if (text == null) return null;
        if (text.indexOf('%') < 0 && (!convertPlus || text.indexOf('+') < 0)) return text;

        StringBuilder decoded = new StringBuilder(text.length());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int index = 0;

        while (index < text.length())
        {
            char character = text.charAt(index);

            if (character == '%' && index + 2 < text.length() && hexValue(text.charAt(index + 1)) >= 0
                && hexValue(text.charAt(index + 2)) >= 0)
            {
                bytes.write((hexValue(text.charAt(index + 1)) << 4) | hexValue(text.charAt(index + 2)));
                index += 3;
                continue;
            }

            if (bytes.size() > 0)
            {
                decoded.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                bytes.reset();
            }

            decoded.append(convertPlus && character == '+' ? ' ' : character);
            index++;
        }

        if (bytes.size() > 0) decoded.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));

        return decoded.toString();
    }

    private static int hexValue(char character)
    {
        if (character >= '0' && character <= '9') return character - '0';
        if (character >= 'a' && character <= 'f') return character - 'a' + 10;
        if (character >= 'A' && character <= 'F') return character - 'A' + 10;

        return -1;
    }

    public boolean isHierarchical()
    {
        return mEncodedOpaquePart == null;
    }

    public boolean isOpaque()
    {
        return !isHierarchical();
    }

    public boolean isRelative()
    {
        return mScheme == null;
    }

    public boolean isAbsolute()
    {
        return !isRelative();
    }

    public String getScheme()
    {
        return mScheme;
    }

    public String getSchemeSpecificPart()
    {
        return decode(getEncodedSchemeSpecificPart());
    }

    public String getEncodedSchemeSpecificPart()
    {
        return encodedSchemeSpecificPart();
    }

    public String getAuthority()
    {
        return decode(mEncodedAuthority);
    }

    public String getEncodedAuthority()
    {
        return mEncodedAuthority;
    }

    public String getEncodedUserInfo()
    {
        if (mEncodedAuthority == null) return null;

        int end = mEncodedAuthority.lastIndexOf('@');
        return end < 0 ? null : mEncodedAuthority.substring(0, end);
    }

    public String getUserInfo()
    {
        return decode(getEncodedUserInfo());
    }

    private int portSeparator()
    {
        if (mEncodedAuthority == null || mEncodedAuthority.endsWith("]")) return -1;

        int colon = mEncodedAuthority.lastIndexOf(':');
        int userInfoSeparator = mEncodedAuthority.lastIndexOf('@');
        return colon > userInfoSeparator ? colon : -1;
    }

    public String getHost()
    {
        if (mEncodedAuthority == null) return null;

        int userInfoSeparator = mEncodedAuthority.lastIndexOf('@');
        int portSeparator = portSeparator();
        String encodedHost = portSeparator < 0 ? mEncodedAuthority.substring(userInfoSeparator + 1)
                                               : mEncodedAuthority.substring(userInfoSeparator + 1, portSeparator);
        return decode(encodedHost);
    }

    public int getPort()
    {
        int portSeparator = portSeparator();
        if (portSeparator < 0) return -1;

        try
        {
            return Integer.parseInt(decode(mEncodedAuthority.substring(portSeparator + 1)));
        }
        catch (NumberFormatException malformed)
        {
            return -1;
        }
    }

    public String getPath()
    {
        return decode(mEncodedPath);
    }

    public String getEncodedPath()
    {
        return mEncodedPath;
    }

    public String getQuery()
    {
        return decode(mEncodedQuery);
    }

    public String getEncodedQuery()
    {
        return mEncodedQuery;
    }

    public String getFragment()
    {
        return decode(mEncodedFragment);
    }

    public String getEncodedFragment()
    {
        return mEncodedFragment;
    }

    public List<String> getPathSegments()
    {
        if (mEncodedPath == null) return Collections.emptyList();

        List<String> segments = new ArrayList<>();
        for (String segment : mEncodedPath.split("/"))
        {
            if (!segment.isEmpty()) segments.add(decode(segment));
        }

        return Collections.unmodifiableList(segments);
    }

    public String getLastPathSegment()
    {
        List<String> segments = getPathSegments();
        return segments.isEmpty() ? null : segments.get(segments.size() - 1);
    }

    public Set<String> getQueryParameterNames()
    {
        if (isOpaque()) throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        if (mEncodedQuery == null) return Collections.emptySet();

        Set<String> names = new LinkedHashSet<>();
        int start = 0;

        do
        {
            int next = mEncodedQuery.indexOf('&', start);
            int end = next == -1 ? mEncodedQuery.length() : next;

            int separator = mEncodedQuery.indexOf('=', start);
            if (separator > end || separator == -1) separator = end;

            names.add(decode(mEncodedQuery.substring(start, separator)));
            start = end + 1;
        }
        while (start < mEncodedQuery.length());

        return Collections.unmodifiableSet(names);
    }

    public List<String> getQueryParameters(String key)
    {
        if (isOpaque()) throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        if (key == null) throw new NullPointerException("key");
        if (mEncodedQuery == null) return Collections.emptyList();

        String encodedKey = encode(key, null);
        List<String> values = new ArrayList<>();
        int start = 0;

        do
        {
            int next = mEncodedQuery.indexOf('&', start);
            int end = next != -1 ? next : mEncodedQuery.length();

            int separator = mEncodedQuery.indexOf('=', start);
            if (separator > end || separator == -1) separator = end;

            if (separator - start == encodedKey.length() && mEncodedQuery.regionMatches(start, encodedKey, 0, encodedKey.length()))
            {
                values.add(separator == end ? "" : decode(mEncodedQuery.substring(separator + 1, end), true));
            }

            start = end + 1;
        }
        while (start < mEncodedQuery.length());

        return Collections.unmodifiableList(values);
    }

    public String getQueryParameter(String key)
    {
        if (isOpaque()) throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        if (key == null) throw new NullPointerException("key");
        if (mEncodedQuery == null) return null;

        String encodedKey = encode(key, null);
        int length = mEncodedQuery.length();
        int start = 0;

        do
        {
            int nextAmpersand = mEncodedQuery.indexOf('&', start);
            int end = nextAmpersand != -1 ? nextAmpersand : length;

            int separator = mEncodedQuery.indexOf('=', start);
            if (separator > end || separator == -1) separator = end;

            if (separator - start == encodedKey.length() && mEncodedQuery.regionMatches(start, encodedKey, 0, encodedKey.length()))
            {
                return separator == end ? "" : decode(mEncodedQuery.substring(separator + 1, end), true);
            }

            if (nextAmpersand == -1) break;
            start = nextAmpersand + 1;
        }
        while (true);

        return null;
    }

    public boolean getBooleanQueryParameter(String key, boolean defaultValue)
    {
        String flag = getQueryParameter(key);
        if (flag == null) return defaultValue;

        flag = flag.toLowerCase(Locale.ROOT);
        return !"false".equals(flag) && !"0".equals(flag);
    }

    public Builder buildUpon()
    {
        if (isOpaque())
        {
            return new Builder().scheme(mScheme).encodedOpaquePart(mEncodedOpaquePart).encodedFragment(mEncodedFragment);
        }

        return new Builder().scheme(mScheme).encodedAuthority(mEncodedAuthority).encodedPath(mEncodedPath)
                   .encodedQuery(mEncodedQuery).encodedFragment(mEncodedFragment);
    }

    public Uri normalizeScheme()
    {
        if (mScheme == null) return this;

        String lowered = mScheme.toLowerCase(Locale.ROOT);
        if (lowered.equals(mScheme)) return this;

        return buildUpon().scheme(lowered).build();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mText);
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
        private String mOpaquePart;
        private String mAuthority;
        private String mPath;
        private String mQuery;
        private String mFragment;

        public Builder()
        {
        }

        public Builder scheme(String scheme)
        {
            mScheme = scheme;
            return this;
        }

        public Builder opaquePart(String opaquePart)
        {
            mOpaquePart = encode(opaquePart);
            return this;
        }

        public Builder encodedOpaquePart(String opaquePart)
        {
            mOpaquePart = opaquePart;
            return this;
        }

        public Builder authority(String authority)
        {
            mOpaquePart = null;
            mAuthority = encode(authority);
            return this;
        }

        public Builder encodedAuthority(String authority)
        {
            mOpaquePart = null;
            mAuthority = authority;
            return this;
        }

        public Builder path(String path)
        {
            mOpaquePart = null;
            mPath = encode(path, "/");
            return this;
        }

        public Builder encodedPath(String path)
        {
            mOpaquePart = null;
            mPath = path;
            return this;
        }

        public Builder appendPath(String newSegment)
        {
            return appendEncodedPath(encode(newSegment));
        }

        public Builder appendEncodedPath(String newSegment)
        {
            mOpaquePart = null;

            String oldPath = mPath == null ? "" : mPath;
            if (oldPath.isEmpty()) mPath = "/" + newSegment;
            else if (oldPath.charAt(oldPath.length() - 1) == '/') mPath = oldPath + newSegment;
            else mPath = oldPath + "/" + newSegment;

            return this;
        }

        public Builder query(String query)
        {
            mOpaquePart = null;
            mQuery = encode(query);
            return this;
        }

        public Builder encodedQuery(String query)
        {
            mOpaquePart = null;
            mQuery = query;
            return this;
        }

        public Builder fragment(String fragment)
        {
            mFragment = encode(fragment);
            return this;
        }

        public Builder encodedFragment(String fragment)
        {
            mFragment = fragment;
            return this;
        }

        public Builder appendQueryParameter(String key, String value)
        {
            mOpaquePart = null;

            String encodedParameter = encode(key, null) + "=" + encode(value, null);
            mQuery = mQuery == null ? encodedParameter : mQuery + "&" + encodedParameter;
            return this;
        }

        public Builder clearQuery()
        {
            mQuery = null;
            return this;
        }

        public Uri build()
        {
            if (mOpaquePart != null)
            {
                if (mScheme == null) throw new UnsupportedOperationException("An opaque URI must have a scheme.");
                return new Uri(mScheme, mOpaquePart, null, null, null, mFragment);
            }

            String path = mPath == null ? "" : mPath;
            if ((mScheme != null || mAuthority != null) && !path.isEmpty() && !path.startsWith("/")) path = "/" + path;

            return new Uri(mScheme, null, mAuthority, path, mQuery, mFragment);
        }

        @Override
        public String toString()
        {
            return build().toString();
        }
    }
}
