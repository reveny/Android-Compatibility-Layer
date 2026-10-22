package android.webkit;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class MimeTypeMap
{
    private static final MimeTypeMap INSTANCE = new MimeTypeMap();

    private static final Pattern FILENAME = Pattern.compile("[a-zA-Z_0-9\\.\\-\\(\\)\\%]+");

    private static final String[][] TABLE = {
        {"application/andrew-inset", "ez"},
        {"application/epub+zip", "epub"},
        {"application/gzip", "gz", "tgz"},
        {"application/java-archive", "jar"},
        {"application/javascript", "js", "mjs"},
        {"application/json", "json"},
        {"application/mac-binhex40", "hqx"},
        {"application/msaccess", "mdb"},
        {"application/msword", "doc", "dot"},
        {"application/octet-stream", "bin"},
        {"application/ogg", "ogx"},
        {"application/pdf", "pdf"},
        {"application/pgp-keys", "key"},
        {"application/pgp-signature", "pgp"},
        {"application/pkcs7-mime", "p7c"},
        {"application/pkcs7-signature", "p7s"},
        {"application/pkix-cert", "cer"},
        {"application/pkix-crl", "crl"},
        {"application/postscript", "ps", "ai", "eps"},
        {"application/rar", "rar"},
        {"application/rdf+xml", "rdf"},
        {"application/rss+xml", "rss"},
        {"application/rtf", "rtf"},
        {"application/vnd.android.package-archive", "apk"},
        {"application/vnd.apple.mpegurl", "m3u8"},
        {"application/vnd.google-earth.kml+xml", "kml"},
        {"application/vnd.google-earth.kmz", "kmz"},
        {"application/vnd.ms-excel", "xls", "xlt"},
        {"application/vnd.ms-pki.stl", "stl"},
        {"application/vnd.ms-powerpoint", "ppt", "pot", "pps"},
        {"application/vnd.oasis.opendocument.graphics", "odg"},
        {"application/vnd.oasis.opendocument.presentation", "odp"},
        {"application/vnd.oasis.opendocument.spreadsheet", "ods"},
        {"application/vnd.oasis.opendocument.text", "odt"},
        {"application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"},
        {"application/vnd.openxmlformats-officedocument.presentationml.slideshow", "ppsx"},
        {"application/vnd.openxmlformats-officedocument.presentationml.template", "potx"},
        {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"},
        {"application/vnd.openxmlformats-officedocument.spreadsheetml.template", "xltx"},
        {"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"},
        {"application/vnd.openxmlformats-officedocument.wordprocessingml.template", "dotx"},
        {"application/vnd.rim.cod", "cod"},
        {"application/vnd.smaf", "mmf"},
        {"application/vnd.stardivision.calc", "sdc"},
        {"application/vnd.visio", "vsd"},
        {"application/vnd.wap.wbxml", "wbxml"},
        {"application/vnd.wap.wmlc", "wmlc"},
        {"application/wasm", "wasm"},
        {"application/x-7z-compressed", "7z"},
        {"application/x-bittorrent", "torrent"},
        {"application/x-bzip2", "bz2"},
        {"application/x-cpio", "cpio"},
        {"application/x-debian-package", "deb"},
        {"application/x-dvi", "dvi"},
        {"application/x-flac", "flac"},
        {"application/x-font", "pfa", "pfb", "gsf", "pcf"},
        {"application/x-freemind", "mm"},
        {"application/x-iso9660-image", "iso"},
        {"application/x-latex", "latex"},
        {"application/x-mpegURL", "m3u8"},
        {"application/x-pkcs12", "p12", "pfx"},
        {"application/x-pkcs7-certificates", "p7b", "spc"},
        {"application/x-rar-compressed", "rar"},
        {"application/x-sh", "sh"},
        {"application/x-shockwave-flash", "swf"},
        {"application/x-tar", "tar"},
        {"application/x-tex", "tex"},
        {"application/x-texinfo", "texinfo", "texi"},
        {"application/x-webarchive", "webarchive"},
        {"application/x-webarchive-xml", "webarchivexml"},
        {"application/x-x509-ca-cert", "crt", "der"},
        {"application/x-x509-server-cert", "crt"},
        {"application/x-x509-user-cert", "crt"},
        {"application/x-xcf", "xcf"},
        {"application/x-xpinstall", "xpi"},
        {"application/xhtml+xml", "xhtml", "xht"},
        {"application/xml", "xml"},
        {"application/zip", "zip"},
        {"audio/3gpp", "3gpp"},
        {"audio/aac", "aac"},
        {"audio/aac-adts", "aac"},
        {"audio/amr", "amr"},
        {"audio/amr-wb", "awb"},
        {"audio/basic", "snd"},
        {"audio/flac", "flac"},
        {"audio/imelody", "imy"},
        {"audio/midi", "mid", "midi", "kar", "xmf", "rtttl", "smf", "ota", "rtx"},
        {"audio/mobile-xmf", "mxmf"},
        {"audio/mp4", "m4a", "m4b", "m4p", "m4r"},
        {"audio/mpeg", "mp3", "mpga", "mpega", "mp2", "m4a"},
        {"audio/mpegurl", "m3u"},
        {"audio/ogg", "oga", "ogg", "spx", "opus"},
        {"audio/prs.sid", "sid"},
        {"audio/webm", "weba"},
        {"audio/x-aiff", "aif", "aiff", "aifc"},
        {"audio/x-gsm", "gsm"},
        {"audio/x-matroska", "mka"},
        {"audio/x-mpegurl", "m3u"},
        {"audio/x-ms-wax", "wax"},
        {"audio/x-ms-wma", "wma"},
        {"audio/x-pn-realaudio", "ra", "rm", "ram"},
        {"audio/x-realaudio", "ra"},
        {"audio/x-scpls", "pls"},
        {"audio/x-sd2", "sd2"},
        {"audio/x-wav", "wav"},
        {"font/otf", "otf"},
        {"font/ttf", "ttf"},
        {"font/woff", "woff"},
        {"font/woff2", "woff2"},
        {"image/avif", "avif"},
        {"image/bmp", "bmp"},
        {"image/gif", "gif"},
        {"image/heic", "heic"},
        {"image/heif", "heif"},
        {"image/ico", "cur"},
        {"image/ief", "ief"},
        {"image/jpeg", "jpg", "jpeg", "jpe"},
        {"image/pcx", "pcx"},
        {"image/png", "png"},
        {"image/svg+xml", "svg", "svgz"},
        {"image/tiff", "tiff", "tif"},
        {"image/vnd.djvu", "djvu", "djv"},
        {"image/vnd.wap.wbmp", "wbmp"},
        {"image/webp", "webp"},
        {"image/x-adobe-dng", "dng"},
        {"image/x-canon-cr2", "cr2"},
        {"image/x-cmu-raster", "ras"},
        {"image/x-coreldraw", "cdr"},
        {"image/x-icon", "ico"},
        {"image/x-jg", "art"},
        {"image/x-jng", "jng"},
        {"image/x-nikon-nef", "nef"},
        {"image/x-ms-bmp", "bmp"},
        {"image/x-photoshop", "psd"},
        {"image/x-portable-anymap", "pnm"},
        {"image/x-portable-bitmap", "pbm"},
        {"image/x-portable-graymap", "pgm"},
        {"image/x-portable-pixmap", "ppm"},
        {"image/x-rgb", "rgb"},
        {"image/x-sony-arw", "arw"},
        {"image/x-xbitmap", "xbm"},
        {"image/x-xpixmap", "xpm"},
        {"image/x-xwindowdump", "xwd"},
        {"message/rfc822", "eml"},
        {"model/gltf+json", "gltf"},
        {"model/gltf-binary", "glb"},
        {"text/calendar", "ics", "icz"},
        {"text/comma-separated-values", "csv"},
        {"text/css", "css"},
        {"text/csv", "csv"},
        {"text/h323", "323"},
        {"text/html", "html", "htm", "shtml"},
        {"text/iuls", "uls"},
        {"text/markdown", "md", "markdown"},
        {"text/mathml", "mml"},
        {"text/plain", "txt", "asc", "text", "diff", "po", "log", "conf"},
        {"text/richtext", "rtx"},
        {"text/rtf", "rtf"},
        {"text/tab-separated-values", "tsv"},
        {"text/texmacs", "ts"},
        {"text/text", "phps"},
        {"text/vcard", "vcf"},
        {"text/x-bibtex", "bib"},
        {"text/x-boo", "boo"},
        {"text/x-c++hdr", "hpp", "h++", "hxx", "hh"},
        {"text/x-c++src", "cpp", "c++", "cc", "cxx"},
        {"text/x-chdr", "h"},
        {"text/x-component", "htc"},
        {"text/x-csh", "csh"},
        {"text/x-csrc", "c"},
        {"text/x-dsrc", "d"},
        {"text/x-haskell", "hs"},
        {"text/x-java", "java"},
        {"text/x-literate-haskell", "lhs"},
        {"text/x-moc", "moc"},
        {"text/x-pascal", "p", "pas"},
        {"text/x-pcs-gcd", "gcd"},
        {"text/x-setext", "etx"},
        {"text/x-tcl", "tcl"},
        {"text/x-tex", "tex", "ltx", "sty", "cls"},
        {"text/x-vcalendar", "vcs"},
        {"text/x-vcard", "vcf"},
        {"text/xml", "xml"},
        {"video/3gpp", "3gpp", "3gp"},
        {"video/3gpp2", "3gpp2", "3g2"},
        {"video/avi", "avi"},
        {"video/dl", "dl"},
        {"video/dv", "dif", "dv"},
        {"video/fli", "fli"},
        {"video/m4v", "m4v"},
        {"video/mp2ts", "ts"},
        {"video/mp4", "mp4", "m4v"},
        {"video/mpeg", "mpeg", "mpg", "mpe", "vob"},
        {"video/ogg", "ogv"},
        {"video/quicktime", "qt", "mov"},
        {"video/vnd.mpegurl", "mxu"},
        {"video/webm", "webm"},
        {"video/x-flv", "flv"},
        {"video/x-la-asf", "lsf", "lsx"},
        {"video/x-matroska", "mkv"},
        {"video/x-mng", "mng"},
        {"video/x-ms-asf", "asf", "asx"},
        {"video/x-ms-wm", "wm"},
        {"video/x-ms-wmv", "wmv"},
        {"video/x-ms-wmx", "wmx"},
        {"video/x-ms-wvx", "wvx"},
        {"video/x-msvideo", "avi"},
        {"video/x-sgi-movie", "movie"},
        {"video/x-webex", "wrf"},
    };

    private static final Map<String, String> MIME_TO_EXTENSION = new HashMap<>();
    private static final Map<String, String> EXTENSION_TO_MIME = new HashMap<>();

    static
    {
        for (String[] row : TABLE)
        {
            String mimeType = row[0].toLowerCase(Locale.ROOT);
            if (!MIME_TO_EXTENSION.containsKey(mimeType)) MIME_TO_EXTENSION.put(mimeType, row[1]);
            for (int index = 1; index < row.length; index++)
            {
                if (!EXTENSION_TO_MIME.containsKey(row[index])) EXTENSION_TO_MIME.put(row[index], row[0]);
            }
        }
        EXTENSION_TO_MIME.put("mp3", "audio/mpeg");
        EXTENSION_TO_MIME.put("m4a", "audio/mp4");
        EXTENSION_TO_MIME.put("xml", "text/xml");
        EXTENSION_TO_MIME.put("csv", "text/comma-separated-values");
        EXTENSION_TO_MIME.put("rtf", "application/rtf");
        EXTENSION_TO_MIME.put("bmp", "image/bmp");
        EXTENSION_TO_MIME.put("ts", "video/mp2ts");
        EXTENSION_TO_MIME.put("vcf", "text/x-vcard");
        EXTENSION_TO_MIME.put("avi", "video/avi");
        EXTENSION_TO_MIME.put("rar", "application/rar");
        EXTENSION_TO_MIME.put("crt", "application/x-x509-ca-cert");
        EXTENSION_TO_MIME.put("aac", "audio/aac");
        EXTENSION_TO_MIME.put("m3u8", "application/vnd.apple.mpegurl");
        EXTENSION_TO_MIME.put("m4v", "video/mp4");
    }

    private MimeTypeMap()
    {
    }

    public static MimeTypeMap getSingleton()
    {
        return INSTANCE;
    }

    public static String getFileExtensionFromUrl(String url)
    {
        if (url != null && !url.isEmpty())
        {
            int fragment = url.lastIndexOf('#');
            if (fragment > 0) url = url.substring(0, fragment);

            int query = url.lastIndexOf('?');
            if (query > 0) url = url.substring(0, query);

            int filenamePosition = url.lastIndexOf('/');
            String filename = filenamePosition >= 0 ? url.substring(filenamePosition + 1) : url;

            if (!filename.isEmpty() && FILENAME.matcher(filename).matches())
            {
                int dotPosition = filename.lastIndexOf('.');
                if (dotPosition >= 0) return filename.substring(dotPosition + 1);
            }
        }
        return "";
    }

    public boolean hasMimeType(String mimeType)
    {
        return mimeType != null && !mimeType.isEmpty() && MIME_TO_EXTENSION.containsKey(mimeType.toLowerCase(Locale.ROOT));
    }

    public String getMimeTypeFromExtension(String extension)
    {
        if (extension == null || extension.isEmpty()) return null;
        return EXTENSION_TO_MIME.get(extension.toLowerCase(Locale.ROOT));
    }

    public boolean hasExtension(String extension)
    {
        return extension != null && !extension.isEmpty() && EXTENSION_TO_MIME.containsKey(extension.toLowerCase(Locale.ROOT));
    }

    public String getExtensionFromMimeType(String mimeType)
    {
        if (mimeType == null || mimeType.isEmpty()) return null;
        return MIME_TO_EXTENSION.get(mimeType.toLowerCase(Locale.ROOT));
    }
}
