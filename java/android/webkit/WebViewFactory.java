package android.webkit;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

public final class WebViewFactory
{
    public static final int LIBLOAD_SUCCESS = 0;
    public static final int LIBLOAD_WRONG_PACKAGE_NAME = 1;
    public static final int LIBLOAD_ADDRESS_SPACE_NOT_RESERVED = 2;
    public static final int LIBLOAD_FAILED_WAITING_FOR_RELRO = 3;
    public static final int LIBLOAD_FAILED_LISTING_WEBVIEW_PACKAGES = 4;
    public static final int LIBLOAD_FAILED_TO_OPEN_RELRO_FILE = 5;
    public static final int LIBLOAD_FAILED_TO_LOAD_LIBRARY = 6;
    public static final int LIBLOAD_FAILED_JNI_CALL = 7;
    public static final int LIBLOAD_FAILED_WAITING_FOR_WEBVIEW_REASON_UNKNOWN = 8;
    public static final int LIBLOAD_FAILED_TO_FIND_NAMESPACE = 10;

    private static final Object sProviderLock = new Object();
    private static WebViewFactoryProvider sProviderInstance;
    private static PackageInfo sPackageInfo;
    private static String sDataDirectorySuffix;
    private static boolean sWebViewDisabled;

    private WebViewFactory()
    {
    }

    public static String getWebViewLibrary(ApplicationInfo applicationInfo)
    {
        return null;
    }

    public static PackageInfo getLoadedPackageInfo()
    {
        synchronized (sProviderLock)
        {
            return sPackageInfo;
        }
    }

    public static Class<WebViewFactoryProvider> getWebViewProviderClass(ClassLoader classLoader) throws ClassNotFoundException
    {
        throw new ClassNotFoundException("No WebView provider is installed");
    }

    public static int loadWebViewNativeLibraryFromPackage(String packageName, ClassLoader classLoader)
    {
        return LIBLOAD_FAILED_TO_LOAD_LIBRARY;
    }

    public static void prepareWebViewInZygote()
    {
    }

    public static int onWebViewProviderChanged(PackageInfo packageInfo)
    {
        return 0;
    }

    static WebViewFactoryProvider getProvider()
    {
        synchronized (sProviderLock)
        {
            if (sProviderInstance != null) return sProviderInstance;
            sProviderInstance = new LocalWebViewFactoryProvider();
            return sProviderInstance;
        }
    }

    static void disableWebView()
    {
        synchronized (sProviderLock)
        {
            if (sProviderInstance != null)
            {
                throw new IllegalStateException("Can't disable WebView: WebView already initialized");
            }
            sWebViewDisabled = true;
        }
    }

    static boolean isWebViewDisabled()
    {
        synchronized (sProviderLock)
        {
            return sWebViewDisabled;
        }
    }

    static void setDataDirectorySuffix(String suffix)
    {
        synchronized (sProviderLock)
        {
            if (sProviderInstance != null)
            {
                throw new IllegalStateException("Can't set data directory suffix: WebView already initialized");
            }
            if (suffix.indexOf(java.io.File.separatorChar) >= 0)
            {
                throw new IllegalArgumentException("Suffix " + suffix + " contains a path separator");
            }
            sDataDirectorySuffix = suffix;
        }
    }

    static String getDataDirectorySuffix()
    {
        synchronized (sProviderLock)
        {
            return sDataDirectorySuffix;
        }
    }
}
