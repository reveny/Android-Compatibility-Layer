package android.webkit;

import android.content.Context;

public abstract class WebSettings
{
    public static final int LOAD_DEFAULT = -1;
    @Deprecated
    public static final int LOAD_NORMAL = 0;
    public static final int LOAD_CACHE_ELSE_NETWORK = 1;
    public static final int LOAD_NO_CACHE = 2;
    public static final int LOAD_CACHE_ONLY = 3;

    public static final int MIXED_CONTENT_ALWAYS_ALLOW = 0;
    public static final int MIXED_CONTENT_NEVER_ALLOW = 1;
    public static final int MIXED_CONTENT_COMPATIBILITY_MODE = 2;

    public static final int FORCE_DARK_OFF = 0;
    public static final int FORCE_DARK_AUTO = 1;
    public static final int FORCE_DARK_ON = 2;

    public static final int MENU_ITEM_NONE = 0;
    public static final int MENU_ITEM_SHARE = 1;
    public static final int MENU_ITEM_WEB_SEARCH = 2;
    public static final int MENU_ITEM_PROCESS_TEXT = 4;

    public WebSettings()
    {
    }

    public static String getDefaultUserAgent(Context context)
    {
        return LocalWebSettings.defaultUserAgent();
    }

    @Deprecated
    public abstract void setSupportZoom(boolean support);

    public abstract boolean supportZoom();

    public abstract void setMediaPlaybackRequiresUserGesture(boolean require);

    public abstract boolean getMediaPlaybackRequiresUserGesture();

    public abstract void setBuiltInZoomControls(boolean enabled);

    public abstract boolean getBuiltInZoomControls();

    public abstract void setDisplayZoomControls(boolean enabled);

    public abstract boolean getDisplayZoomControls();

    public abstract void setAllowFileAccess(boolean allow);

    public abstract boolean getAllowFileAccess();

    public abstract void setAllowContentAccess(boolean allow);

    public abstract boolean getAllowContentAccess();

    public abstract void setLoadWithOverviewMode(boolean overview);

    public abstract boolean getLoadWithOverviewMode();

    @Deprecated
    public abstract void setEnableSmoothTransition(boolean enable);

    @Deprecated
    public abstract boolean enableSmoothTransition();

    @Deprecated
    public abstract void setSaveFormData(boolean save);

    @Deprecated
    public abstract boolean getSaveFormData();

    @Deprecated
    public abstract void setSavePassword(boolean save);

    @Deprecated
    public abstract boolean getSavePassword();

    public abstract void setTextZoom(int textZoom);

    public abstract int getTextZoom();

    @Deprecated
    public synchronized void setTextSize(TextSize textSize)
    {
        setTextZoom(textSize.mValue);
    }

    @Deprecated
    public synchronized TextSize getTextSize()
    {
        TextSize closestSize = null;
        int smallestDelta = Integer.MAX_VALUE;
        int textSize = getTextZoom();
        for (TextSize size : TextSize.values())
        {
            int delta = Math.abs(textSize - size.mValue);
            if (delta == 0) return size;
            if (delta < smallestDelta)
            {
                smallestDelta = delta;
                closestSize = size;
            }
        }
        return closestSize != null ? closestSize : TextSize.NORMAL;
    }

    @Deprecated
    public abstract void setDefaultZoom(ZoomDensity zoom);

    @Deprecated
    public abstract ZoomDensity getDefaultZoom();

    @Deprecated
    public abstract void setLightTouchEnabled(boolean enabled);

    @Deprecated
    public abstract boolean getLightTouchEnabled();

    public abstract void setUseWideViewPort(boolean use);

    public abstract boolean getUseWideViewPort();

    public abstract void setSupportMultipleWindows(boolean support);

    public abstract boolean supportMultipleWindows();

    public abstract void setLayoutAlgorithm(LayoutAlgorithm algorithm);

    public abstract LayoutAlgorithm getLayoutAlgorithm();

    public abstract void setStandardFontFamily(String font);

    public abstract String getStandardFontFamily();

    public abstract void setFixedFontFamily(String font);

    public abstract String getFixedFontFamily();

    public abstract void setSansSerifFontFamily(String font);

    public abstract String getSansSerifFontFamily();

    public abstract void setSerifFontFamily(String font);

    public abstract String getSerifFontFamily();

    public abstract void setCursiveFontFamily(String font);

    public abstract String getCursiveFontFamily();

    public abstract void setFantasyFontFamily(String font);

    public abstract String getFantasyFontFamily();

    public abstract void setMinimumFontSize(int size);

    public abstract int getMinimumFontSize();

    public abstract void setMinimumLogicalFontSize(int size);

    public abstract int getMinimumLogicalFontSize();

    public abstract void setDefaultFontSize(int size);

    public abstract int getDefaultFontSize();

    public abstract void setDefaultFixedFontSize(int size);

    public abstract int getDefaultFixedFontSize();

    public abstract void setLoadsImagesAutomatically(boolean flag);

    public abstract boolean getLoadsImagesAutomatically();

    public abstract void setBlockNetworkImage(boolean flag);

    public abstract boolean getBlockNetworkImage();

    public abstract void setBlockNetworkLoads(boolean flag);

    public abstract boolean getBlockNetworkLoads();

    public abstract void setJavaScriptEnabled(boolean flag);

    @Deprecated
    public abstract void setAllowUniversalAccessFromFileURLs(boolean flag);

    @Deprecated
    public abstract void setAllowFileAccessFromFileURLs(boolean flag);

    @Deprecated
    public abstract void setPluginState(PluginState state);

    @Deprecated
    public abstract void setDatabasePath(String databasePath);

    @Deprecated
    public abstract void setGeolocationDatabasePath(String databasePath);

    public abstract void setDatabaseEnabled(boolean flag);

    public abstract void setDomStorageEnabled(boolean flag);

    public abstract boolean getDomStorageEnabled();

    @Deprecated
    public abstract String getDatabasePath();

    public abstract boolean getDatabaseEnabled();

    public abstract void setGeolocationEnabled(boolean flag);

    public abstract boolean getJavaScriptEnabled();

    public abstract boolean getAllowUniversalAccessFromFileURLs();

    public abstract boolean getAllowFileAccessFromFileURLs();

    @Deprecated
    public abstract PluginState getPluginState();

    public abstract void setJavaScriptCanOpenWindowsAutomatically(boolean flag);

    public abstract boolean getJavaScriptCanOpenWindowsAutomatically();

    public abstract void setDefaultTextEncodingName(String encoding);

    public abstract String getDefaultTextEncodingName();

    public abstract void setUserAgentString(String userAgent);

    public abstract String getUserAgentString();

    public abstract void setNeedInitialFocus(boolean flag);

    @Deprecated
    public abstract void setRenderPriority(RenderPriority priority);

    public abstract void setCacheMode(int mode);

    public abstract int getCacheMode();

    public abstract void setMixedContentMode(int mode);

    public abstract int getMixedContentMode();

    public abstract void setOffscreenPreRaster(boolean enabled);

    public abstract boolean getOffscreenPreRaster();

    public abstract void setSafeBrowsingEnabled(boolean enabled);

    public abstract boolean getSafeBrowsingEnabled();

    @Deprecated
    public void setForceDark(int forceDark)
    {
    }

    @Deprecated
    public int getForceDark()
    {
        return FORCE_DARK_AUTO;
    }

    public void setAlgorithmicDarkeningAllowed(boolean allow)
    {
    }

    public boolean isAlgorithmicDarkeningAllowed()
    {
        return false;
    }

    public abstract void setDisabledActionModeMenuItems(int menuItems);

    public abstract int getDisabledActionModeMenuItems();

    public enum LayoutAlgorithm
    {
        NORMAL,
        @Deprecated
        SINGLE_COLUMN,
        @Deprecated
        NARROW_COLUMNS,
        TEXT_AUTOSIZING
    }

    @Deprecated
    public enum TextSize
    {
        SMALLEST(50),
        SMALLER(75),
        NORMAL(100),
        LARGER(150),
        LARGEST(200);

        final int mValue;

        TextSize(int size)
        {
            mValue = size;
        }
    }

    public enum ZoomDensity
    {
        FAR(150),
        MEDIUM(100),
        CLOSE(75);

        final int mValue;

        ZoomDensity(int size)
        {
            mValue = size;
        }

        public int getValue()
        {
            return mValue;
        }
    }

    public enum RenderPriority
    {
        NORMAL,
        HIGH,
        LOW
    }

    public enum PluginState
    {
        ON,
        ON_DEMAND,
        OFF
    }
}
