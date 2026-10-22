package android.webkit;

import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URLUtil
{
    static final String ASSET_BASE = "file:///android_asset/";
    static final String RESOURCE_BASE = "file:///android_res/";
    static final String FILE_BASE = "file:";
    static final String PROXY_BASE = "file:///cookieless_proxy/";
    static final String CONTENT_BASE = "content:";

    private static final Pattern CONTENT_DISPOSITION = Pattern.compile(
        "attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_DISPOSITION_EXTENDED = Pattern.compile(
        "filename\\*\\s*=\\s*([^']*)'[^']*'([^;\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_DISPOSITION_PLAIN = Pattern.compile(
        "filename\\s*=\\s*(\"?)([^\";]*)\\1", Pattern.CASE_INSENSITIVE);

    public URLUtil()
    {
    }

    public static String guessUrl(String inUrl)
    {
        String original = inUrl;
        if (inUrl.length() == 0) return inUrl;
        if (inUrl.startsWith("about:")) return inUrl;
        if (inUrl.startsWith("data:")) return inUrl;
        if (inUrl.startsWith("file:")) return inUrl;
        if (inUrl.startsWith("javascript:")) return inUrl;

        if (inUrl.endsWith(".")) inUrl = inUrl.substring(0, inUrl.length() - 1);
        String candidate = inUrl.trim();
        if (candidate.contains(" ")) return original;

        String scheme = "http";
        int schemeEnd = candidate.indexOf("://");
        if (schemeEnd > 0)
        {
            scheme = candidate.substring(0, schemeEnd).toLowerCase(Locale.ROOT);
            candidate = candidate.substring(schemeEnd + 3);
        }

        int pathStart = candidate.length();
        for (int index = 0; index < candidate.length(); index++)
        {
            char character = candidate.charAt(index);
            if (character == '/' || character == '?' || character == '#')
            {
                pathStart = index;
                break;
            }
        }
        String authority = candidate.substring(0, pathStart);
        String path = candidate.substring(pathStart);
        if (authority.isEmpty()) return original;

        String userInfo = "";
        int at = authority.lastIndexOf('@');
        if (at >= 0)
        {
            userInfo = authority.substring(0, at + 1);
            authority = authority.substring(at + 1);
        }
        String host = authority;
        String port = "";
        int colon = authority.lastIndexOf(':');
        if (colon >= 0 && authority.indexOf(']') < colon)
        {
            host = authority.substring(0, colon);
            port = authority.substring(colon);
        }
        if (host.isEmpty()) return original;
        if (host.indexOf('.') == -1 && !host.startsWith("[")) host = "www." + host + ".com";
        if (path.isEmpty()) path = "/";
        return scheme + "://" + userInfo + host.toLowerCase(Locale.ROOT) + port + path;
    }

    public static String composeSearchUrl(String inQuery, String template, String queryPlaceHolder)
    {
        int placeHolderIndex = template.indexOf(queryPlaceHolder);
        if (placeHolderIndex < 0) return null;

        String query;
        StringBuilder buffer = new StringBuilder();
        buffer.append(template.substring(0, placeHolderIndex));
        try
        {
            query = URLEncoder.encode(inQuery, "utf-8");
            buffer.append(query);
        }
        catch (Exception exception)
        {
            return null;
        }
        buffer.append(template.substring(placeHolderIndex + queryPlaceHolder.length()));
        return buffer.toString();
    }

    public static byte[] decode(byte[] url) throws IllegalArgumentException
    {
        if (url.length == 0) return new byte[0];

        ByteArrayOutputStream output = new ByteArrayOutputStream(url.length);
        for (int index = 0; index < url.length; index++)
        {
            byte value = url[index];
            if (value == '%')
            {
                if (url.length - index > 2)
                {
                    value = (byte)(parseHex(url[index + 1]) * 16 + parseHex(url[index + 2]));
                    index += 2;
                }
                else
                {
                    throw new IllegalArgumentException("Invalid format");
                }
            }
            output.write(value);
        }
        return output.toByteArray();
    }

    private static int parseHex(byte value)
    {
        if (value >= '0' && value <= '9') return value - '0';
        if (value >= 'A' && value <= 'F') return value - 'A' + 10;
        if (value >= 'a' && value <= 'f') return value - 'a' + 10;
        throw new IllegalArgumentException("Invalid hex char '" + (char)value + "'");
    }

    public static boolean isAssetUrl(String url)
    {
        return url != null && url.startsWith(ASSET_BASE);
    }

    public static boolean isResourceUrl(String url)
    {
        return url != null && url.startsWith(RESOURCE_BASE);
    }

    public static boolean isCookielessProxyUrl(String url)
    {
        return url != null && url.startsWith(PROXY_BASE);
    }

    public static boolean isFileUrl(String url)
    {
        return url != null && url.startsWith(FILE_BASE) && !url.startsWith(ASSET_BASE) && !url.startsWith(PROXY_BASE);
    }

    public static boolean isAboutUrl(String url)
    {
        return url != null && url.startsWith("about:");
    }

    public static boolean isDataUrl(String url)
    {
        return url != null && url.startsWith("data:");
    }

    public static boolean isJavaScriptUrl(String url)
    {
        return url != null && url.startsWith("javascript:");
    }

    public static boolean isHttpUrl(String url)
    {
        return url != null && url.length() > 6 && url.substring(0, 7).equalsIgnoreCase("http://");
    }

    public static boolean isHttpsUrl(String url)
    {
        return url != null && url.length() > 7 && url.substring(0, 8).equalsIgnoreCase("https://");
    }

    public static boolean isNetworkUrl(String url)
    {
        if (url == null || url.length() == 0) return false;
        return isHttpUrl(url) || isHttpsUrl(url);
    }

    public static boolean isContentUrl(String url)
    {
        return url != null && url.startsWith(CONTENT_BASE);
    }

    public static boolean isValidUrl(String url)
    {
        if (url == null || url.length() == 0) return false;
        return isAssetUrl(url) || isResourceUrl(url) || isFileUrl(url) || isAboutUrl(url) || isHttpUrl(url)
            || isHttpsUrl(url) || isJavaScriptUrl(url) || isContentUrl(url);
    }

    public static String stripAnchor(String url)
    {
        int anchorIndex = url.indexOf('#');
        if (anchorIndex != -1) return url.substring(0, anchorIndex);
        return url;
    }

    public static String guessFileName(String url, String contentDisposition, String mimeType)
    {
        String filename = null;
        String extension = null;

        if (contentDisposition != null) filename = parseContentDisposition(contentDisposition);
        if (filename != null)
        {
            int index = filename.lastIndexOf('/') + 1;
            if (index > 0) filename = filename.substring(index);
        }

        if (filename == null)
        {
            String decodedUrl = Uri.decode(url);
            if (decodedUrl != null)
            {
                int queryIndex = decodedUrl.indexOf('?');
                if (queryIndex > 0) decodedUrl = decodedUrl.substring(0, queryIndex);
                if (!decodedUrl.endsWith("/"))
                {
                    int index = decodedUrl.lastIndexOf('/') + 1;
                    if (index > 0) filename = decodedUrl.substring(index);
                }
            }
        }

        if (filename == null || filename.isEmpty()) filename = "downloadfile";

        int dotIndex = filename.indexOf('.');
        if (dotIndex < 0)
        {
            if (mimeType != null)
            {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (extension != null) extension = "." + extension;
            }
            if (extension == null)
            {
                if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("text/"))
                {
                    extension = mimeType.equalsIgnoreCase("text/html") ? ".html" : ".txt";
                }
                else
                {
                    extension = ".bin";
                }
            }
        }
        else
        {
            if (mimeType != null)
            {
                int lastDotIndex = filename.lastIndexOf('.');
                String typeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(filename.substring(lastDotIndex + 1));
                if (typeFromExtension != null && !typeFromExtension.equalsIgnoreCase(mimeType))
                {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (extension != null) extension = "." + extension;
                }
            }
            if (extension == null) extension = filename.substring(dotIndex);
            filename = filename.substring(0, dotIndex);
        }

        return filename + extension;
    }

    static String parseContentDisposition(String contentDisposition)
    {
        try
        {
            Matcher extended = CONTENT_DISPOSITION_EXTENDED.matcher(contentDisposition);
            if (extended.find())
            {
                String charset = extended.group(1).isEmpty() ? "UTF-8" : extended.group(1);
                return java.net.URLDecoder.decode(extended.group(2), charset);
            }
            Matcher matcher = CONTENT_DISPOSITION.matcher(contentDisposition);
            if (matcher.find()) return matcher.group(2);
            Matcher plain = CONTENT_DISPOSITION_PLAIN.matcher(contentDisposition);
            if (plain.find()) return plain.group(2);
        }
        catch (Exception ignored)
        {
        }
        return null;
    }
}
