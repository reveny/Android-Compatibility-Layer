package android.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.Rect;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.print.PrintDocumentAdapter;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.textclassifier.TextClassifier;
import android.widget.AbsoluteLayout;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebView extends AbsoluteLayout implements ViewTreeObserver.OnGlobalFocusChangeListener,
    ViewGroup.OnHierarchyChangeListener
{
    public static final int RENDERER_PRIORITY_WAIVED = 0;
    public static final int RENDERER_PRIORITY_BOUND = 1;
    public static final int RENDERER_PRIORITY_IMPORTANT = 2;

    public static final String SCHEME_TEL = "tel:";
    public static final String SCHEME_MAILTO = "mailto:";
    public static final String SCHEME_GEO = "geo:0,0?q=";

    private static final String ABOUT_BLANK = "about:blank";
    private static final String STATE_URLS = "android.webkit.WebView.urls";
    private static final String STATE_ORIGINAL_URLS = "android.webkit.WebView.originalUrls";
    private static final String STATE_TITLES = "android.webkit.WebView.titles";
    private static final String STATE_INDEX = "android.webkit.WebView.index";
    private static final Pattern TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final WebViewClient DEFAULT_CLIENT = new WebViewClient();

    private static volatile boolean sWebContentsDebuggingEnabled;

    private final Looper mWebViewLooper;
    private final Handler mHandler;
    private final LocalWebSettings mSettings = new LocalWebSettings();
    private final LocalBackForwardList mHistory = new LocalBackForwardList();
    private final Map<String, Object> mJavascriptInterfaces = new HashMap<>();

    private WebViewClient mWebViewClient;
    private WebChromeClient mWebChromeClient;
    private WebViewRenderProcessClient mRenderProcessClient;
    private Executor mRenderProcessClientExecutor;
    private DownloadListener mDownloadListener;
    private FindListener mFindListener;
    private PictureListener mPictureListener;
    private TextClassifier mTextClassifier;
    private SslCertificate mCertificate;

    private String mUrl;
    private String mOriginalUrl;
    private String mTitle;
    private int mProgress = 100;
    private int mNavigation;
    private int mBackgroundColor = Color.WHITE;
    private int mInitialScale;
    private int mRendererRequestedPriority = RENDERER_PRIORITY_IMPORTANT;
    private boolean mRendererWaivedWhenNotVisible;
    private boolean mHorizontalScrollbarOverlay;
    private boolean mVerticalScrollbarOverlay;
    private boolean mMapTrackballToArrowKeys;
    private boolean mNetworkAvailable = true;
    private boolean mPaused;
    private boolean mDestroyed;

    public WebView(Context context)
    {
        this(context, null);
    }

    public WebView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public WebView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    public WebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        this(context, attrs, defStyleAttr, defStyleRes, null, false);
    }

    @Deprecated
    public WebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing)
    {
        this(context, attrs, defStyleAttr, 0, null, privateBrowsing);
    }

    protected WebView(Context context, AttributeSet attrs, int defStyleAttr, Map<String, Object> javaScriptInterfaces,
        boolean privateBrowsing)
    {
        this(context, attrs, defStyleAttr, 0, javaScriptInterfaces, privateBrowsing);
    }

    private WebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, Map<String, Object> javaScriptInterfaces,
        boolean privateBrowsing)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (context == null) throw new IllegalArgumentException("Invalid context argument");
        if (WebViewFactory.isWebViewDisabled())
        {
            throw new IllegalStateException("WebView.disableWebView() was called: WebView is disabled");
        }
        if (privateBrowsing) throw new IllegalArgumentException("Private browsing is not supported in WebView.");
        WebViewFactory.getProvider();

        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        mWebViewLooper = looper;
        mHandler = looper == null ? null : new Handler(looper);
        if (javaScriptInterfaces != null) mJavascriptInterfaces.putAll(javaScriptInterfaces);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
    }

    public static void setWebContentsDebuggingEnabled(boolean enabled)
    {
        sWebContentsDebuggingEnabled = enabled;
    }

    public static void setDataDirectorySuffix(String suffix)
    {
        WebViewFactory.setDataDirectorySuffix(suffix);
    }

    public static void disableWebView()
    {
        WebViewFactory.disableWebView();
    }

    public static void clearClientCertPreferences(Runnable onCleared)
    {
        WebViewFactory.getProvider().getStatics().clearClientCertPreferences(onCleared);
    }

    public static void startSafeBrowsing(Context context, ValueCallback<Boolean> callback)
    {
        WebViewFactory.getProvider().getStatics().initSafeBrowsing(context, callback);
    }

    public static void setSafeBrowsingWhitelist(List<String> hosts, ValueCallback<Boolean> callback)
    {
        WebViewFactory.getProvider().getStatics().setSafeBrowsingWhitelist(hosts, callback);
    }

    public static Uri getSafeBrowsingPrivacyPolicyUrl()
    {
        return WebViewFactory.getProvider().getStatics().getSafeBrowsingPrivacyPolicyUrl();
    }

    public static void enableSlowWholeDocumentDraw()
    {
        WebViewFactory.getProvider().getStatics().enableSlowWholeDocumentDraw();
    }

    @Deprecated
    public static String findAddress(String address)
    {
        return WebViewFactory.getProvider().getStatics().findAddress(address);
    }

    public static PackageInfo getCurrentWebViewPackage()
    {
        return WebViewFactory.getLoadedPackageInfo();
    }

    public static ClassLoader getWebViewClassLoader()
    {
        return WebViewFactory.getProvider().getWebViewClassLoader();
    }

    @Deprecated
    public void setHorizontalScrollbarOverlay(boolean overlay)
    {
        mHorizontalScrollbarOverlay = overlay;
    }

    @Deprecated
    public void setVerticalScrollbarOverlay(boolean overlay)
    {
        mVerticalScrollbarOverlay = overlay;
    }

    @Deprecated
    public boolean overlayHorizontalScrollbar()
    {
        return mHorizontalScrollbarOverlay;
    }

    @Deprecated
    public boolean overlayVerticalScrollbar()
    {
        return mVerticalScrollbarOverlay;
    }

    public SslCertificate getCertificate()
    {
        checkThread();
        return mCertificate;
    }

    @Deprecated
    public void setCertificate(SslCertificate certificate)
    {
        checkThread();
        mCertificate = certificate;
    }

    @Deprecated
    public void savePassword(String host, String username, String password)
    {
    }

    @Deprecated
    public void setHttpAuthUsernamePassword(String host, String realm, String username, String password)
    {
        checkThread();
        WebViewDatabase.getInstance(getContext()).setHttpAuthUsernamePassword(host, realm, username, password);
    }

    @Deprecated
    public String[] getHttpAuthUsernamePassword(String host, String realm)
    {
        checkThread();
        return WebViewDatabase.getInstance(getContext()).getHttpAuthUsernamePassword(host, realm);
    }

    public void destroy()
    {
        checkThread();
        mDestroyed = true;
        mNavigation++;
        mJavascriptInterfaces.clear();
    }

    @Deprecated
    public void setNetworkAvailable(boolean networkUp)
    {
        checkThread();
        mNetworkAvailable = networkUp;
    }

    public WebBackForwardList saveState(Bundle outState)
    {
        checkThread();
        if (outState == null || mHistory.getSize() == 0) return null;
        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> originalUrls = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        for (int index = 0; index < mHistory.getSize(); index++)
        {
            LocalHistoryItem item = mHistory.getItemAtIndex(index);
            urls.add(item.getUrl());
            originalUrls.add(item.getOriginalUrl());
            titles.add(item.getTitle());
        }
        outState.putStringArrayList(STATE_URLS, urls);
        outState.putStringArrayList(STATE_ORIGINAL_URLS, originalUrls);
        outState.putStringArrayList(STATE_TITLES, titles);
        outState.putInt(STATE_INDEX, mHistory.getCurrentIndex());
        return copyBackForwardList();
    }

    public WebBackForwardList restoreState(Bundle inState)
    {
        checkThread();
        if (inState == null) return null;
        ArrayList<String> urls = inState.getStringArrayList(STATE_URLS);
        ArrayList<String> originalUrls = inState.getStringArrayList(STATE_ORIGINAL_URLS);
        ArrayList<String> titles = inState.getStringArrayList(STATE_TITLES);
        if (urls == null || urls.isEmpty()) return null;
        int index = inState.getInt(STATE_INDEX);
        if (index < 0 || index >= urls.size()) return null;

        mHistory.clear();
        for (int position = 0; position < urls.size(); position++)
        {
            String original = originalUrls != null && position < originalUrls.size() ? originalUrls.get(position) : urls.get(position);
            String title = titles != null && position < titles.size() ? titles.get(position) : urls.get(position);
            mHistory.push(new LocalHistoryItem(urls.get(position), original, title));
        }
        mHistory.goBackOrForward(index - mHistory.getCurrentIndex());
        LocalHistoryItem current = mHistory.getCurrentItem();
        navigate(current.getUrl(), current.getOriginalUrl(), null, "GET", null, NavigationKind.HISTORY);
        return copyBackForwardList();
    }

    public void loadUrl(String url, Map<String, String> additionalHttpHeaders)
    {
        checkThread();
        if (url == null || mDestroyed) return;
        if (URLUtil.isJavaScriptUrl(url))
        {
            evaluateJavascript(url.substring("javascript:".length()), null);
            return;
        }
        navigate(url, url, null, "GET", additionalHttpHeaders, NavigationKind.NEW);
    }

    public void loadUrl(String url)
    {
        loadUrl(url, null);
    }

    public void postUrl(String url, byte[] postData)
    {
        checkThread();
        if (url == null || mDestroyed) return;
        if (URLUtil.isNetworkUrl(url))
        {
            navigate(url, url, null, "POST", null, NavigationKind.NEW);
            return;
        }
        loadUrl(url);
    }

    public void loadData(String data, String mimeType, String encoding)
    {
        checkThread();
        if (mDestroyed) return;
        String type = mimeType == null || mimeType.isEmpty() ? "text/html" : mimeType;
        boolean base64 = encoding != null && encoding.equalsIgnoreCase("base64");
        String url = "data:" + type + (base64 ? ";base64" : "") + "," + (data == null ? "" : data);
        navigate(url, url, base64 ? null : data, "GET", null, NavigationKind.NEW);
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl)
    {
        checkThread();
        if (mDestroyed) return;
        String url = baseUrl == null || baseUrl.isEmpty() ? ABOUT_BLANK : baseUrl;
        if (URLUtil.isDataUrl(url))
        {
            loadData(data, mimeType, encoding);
            return;
        }
        String original = historyUrl == null || historyUrl.isEmpty() ? ABOUT_BLANK : historyUrl;
        navigate(url, original, data == null ? "" : data, "GET", null, NavigationKind.INLINE);
    }

    public void evaluateJavascript(String script, ValueCallback<String> resultCallback)
    {
        checkThread();
        if (resultCallback == null) return;
        final ValueCallback<String> callback = resultCallback;
        postToWebViewThread(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onReceiveValue("null");
            }
        });
    }

    public void saveWebArchive(String filename)
    {
        checkThread();
    }

    public void saveWebArchive(String basename, boolean autoname, ValueCallback<String> callback)
    {
        checkThread();
        WebkitCallbacks.deliver(callback, null);
    }

    public void stopLoading()
    {
        checkThread();
        mNavigation++;
        mProgress = 100;
    }

    public void reload()
    {
        checkThread();
        LocalHistoryItem current = mHistory.getCurrentItem();
        if (current == null || mDestroyed) return;
        navigate(current.getUrl(), current.getOriginalUrl(), null, "GET", null, NavigationKind.RELOAD);
    }

    public boolean canGoBack()
    {
        checkThread();
        return mHistory.canGoBackOrForward(-1);
    }

    public void goBack()
    {
        goBackOrForward(-1);
    }

    public boolean canGoForward()
    {
        checkThread();
        return mHistory.canGoBackOrForward(1);
    }

    public void goForward()
    {
        goBackOrForward(1);
    }

    public boolean canGoBackOrForward(int steps)
    {
        checkThread();
        return mHistory.canGoBackOrForward(steps);
    }

    public void goBackOrForward(int steps)
    {
        checkThread();
        if (mDestroyed) return;
        LocalHistoryItem item = mHistory.goBackOrForward(steps);
        if (item == null) return;
        navigate(item.getUrl(), item.getOriginalUrl(), null, "GET", null, NavigationKind.HISTORY);
    }

    public boolean isPrivateBrowsingEnabled()
    {
        checkThread();
        return false;
    }

    public boolean pageUp(boolean top)
    {
        checkThread();
        return false;
    }

    public boolean pageDown(boolean bottom)
    {
        checkThread();
        return false;
    }

    public void postVisualStateCallback(final long requestId, final VisualStateCallback callback)
    {
        checkThread();
        if (callback == null) return;
        postToWebViewThread(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onComplete(requestId);
            }
        });
    }

    @Deprecated
    public void clearView()
    {
        checkThread();
        invalidate();
    }

    @Deprecated
    public Picture capturePicture()
    {
        checkThread();
        return new Picture();
    }

    public PrintDocumentAdapter createPrintDocumentAdapter()
    {
        return createPrintDocumentAdapter("default");
    }

    public PrintDocumentAdapter createPrintDocumentAdapter(String documentName)
    {
        checkThread();
        return new WebViewPrintDocumentAdapter(documentName);
    }

    @Deprecated
    public float getScale()
    {
        checkThread();
        return getResources().getDisplayMetrics().density * (mInitialScale > 0 ? mInitialScale / 100.0f : 1.0f);
    }

    public void setInitialScale(int scaleInPercent)
    {
        checkThread();
        mInitialScale = scaleInPercent;
    }

    public void invokeZoomPicker()
    {
        checkThread();
    }

    public HitTestResult getHitTestResult()
    {
        checkThread();
        return new HitTestResult();
    }

    public void requestFocusNodeHref(Message hrefMsg)
    {
        checkThread();
        if (hrefMsg == null) return;
        Bundle data = hrefMsg.getData();
        data.putString("url", null);
        data.putString("title", null);
        data.putString("src", null);
        hrefMsg.setData(data);
        if (hrefMsg.getTarget() != null) hrefMsg.sendToTarget();
    }

    public void requestImageRef(Message msg)
    {
        checkThread();
        if (msg == null) return;
        Bundle data = msg.getData();
        data.putString("url", null);
        msg.setData(data);
        if (msg.getTarget() != null) msg.sendToTarget();
    }

    public String getUrl()
    {
        checkThread();
        return mUrl;
    }

    public String getOriginalUrl()
    {
        checkThread();
        return mOriginalUrl;
    }

    public String getTitle()
    {
        checkThread();
        return mTitle;
    }

    public Bitmap getFavicon()
    {
        checkThread();
        return null;
    }

    public int getProgress()
    {
        checkThread();
        return mProgress;
    }

    public int getContentHeight()
    {
        checkThread();
        return 0;
    }

    public void pauseTimers()
    {
        checkThread();
    }

    public void resumeTimers()
    {
        checkThread();
    }

    public void onPause()
    {
        checkThread();
        mPaused = true;
    }

    public void onResume()
    {
        checkThread();
        mPaused = false;
    }

    @Deprecated
    public void freeMemory()
    {
        checkThread();
    }

    public void clearCache(boolean includeDiskFiles)
    {
        checkThread();
    }

    public void clearFormData()
    {
        checkThread();
    }

    public void clearHistory()
    {
        checkThread();
        mHistory.clearAllButCurrent();
    }

    public void clearSslPreferences()
    {
        checkThread();
    }

    public WebBackForwardList copyBackForwardList()
    {
        checkThread();
        return mHistory.clone();
    }

    public void setFindListener(FindListener listener)
    {
        checkThread();
        mFindListener = listener;
    }

    public void findNext(boolean forward)
    {
        checkThread();
        reportFindResult();
    }

    @Deprecated
    public int findAll(String find)
    {
        checkThread();
        reportFindResult();
        return 0;
    }

    public void findAllAsync(String find)
    {
        checkThread();
        reportFindResult();
    }

    @Deprecated
    public boolean showFindDialog(String text, boolean showIme)
    {
        checkThread();
        return false;
    }

    public void clearMatches()
    {
        checkThread();
    }

    public void documentHasImages(Message response)
    {
        checkThread();
        if (response == null) return;
        response.arg1 = 0;
        if (response.getTarget() != null) response.sendToTarget();
    }

    public void setWebViewClient(WebViewClient client)
    {
        checkThread();
        mWebViewClient = client;
    }

    public WebViewClient getWebViewClient()
    {
        checkThread();
        return mWebViewClient == null ? DEFAULT_CLIENT : mWebViewClient;
    }

    public WebViewRenderProcess getWebViewRenderProcess()
    {
        checkThread();
        return null;
    }

    public void setWebViewRenderProcessClient(Executor executor, WebViewRenderProcessClient webViewRenderProcessClient)
    {
        checkThread();
        if (executor == null) throw new IllegalArgumentException("executor must not be null");
        mRenderProcessClientExecutor = executor;
        mRenderProcessClient = webViewRenderProcessClient;
    }

    public void setWebViewRenderProcessClient(WebViewRenderProcessClient webViewRenderProcessClient)
    {
        checkThread();
        mRenderProcessClientExecutor = null;
        mRenderProcessClient = webViewRenderProcessClient;
    }

    public WebViewRenderProcessClient getWebViewRenderProcessClient()
    {
        checkThread();
        return mRenderProcessClient;
    }

    public void setDownloadListener(DownloadListener listener)
    {
        checkThread();
        mDownloadListener = listener;
    }

    public void setWebChromeClient(WebChromeClient client)
    {
        checkThread();
        mWebChromeClient = client;
    }

    public WebChromeClient getWebChromeClient()
    {
        checkThread();
        return mWebChromeClient;
    }

    @Deprecated
    public void setPictureListener(PictureListener listener)
    {
        checkThread();
        mPictureListener = listener;
    }

    public void addJavascriptInterface(Object object, String name)
    {
        checkThread();
        if (object == null || name == null) return;
        mJavascriptInterfaces.put(name, object);
    }

    public void removeJavascriptInterface(String name)
    {
        checkThread();
        mJavascriptInterfaces.remove(name);
    }

    public WebMessagePort[] createWebMessageChannel()
    {
        checkThread();
        return LocalWebMessagePort.createChannel();
    }

    public void postWebMessage(WebMessage message, Uri targetOrigin)
    {
        checkThread();
        if (targetOrigin == null) throw new IllegalArgumentException("targetOrigin cannot be null");
    }

    public WebSettings getSettings()
    {
        checkThread();
        return mSettings;
    }

    @Deprecated
    public void setMapTrackballToArrowKeys(boolean setMap)
    {
        checkThread();
        mMapTrackballToArrowKeys = setMap;
    }

    public void flingScroll(int vx, int vy)
    {
        checkThread();
    }

    @Deprecated
    public boolean canZoomIn()
    {
        checkThread();
        return false;
    }

    @Deprecated
    public boolean canZoomOut()
    {
        checkThread();
        return false;
    }

    public void zoomBy(float zoomFactor)
    {
        checkThread();
        if (zoomFactor < 0.01f) throw new IllegalArgumentException("zoomFactor must be greater than 0.01.");
        if (zoomFactor > 100.0f) throw new IllegalArgumentException("zoomFactor must be less than 100.");
    }

    public boolean zoomIn()
    {
        checkThread();
        return false;
    }

    public boolean zoomOut()
    {
        checkThread();
        return false;
    }

    public void setRendererPriorityPolicy(int rendererRequestedPriority, boolean waivedWhenNotVisible)
    {
        mRendererRequestedPriority = rendererRequestedPriority;
        mRendererWaivedWhenNotVisible = waivedWhenNotVisible;
    }

    public int getRendererRequestedPriority()
    {
        return mRendererRequestedPriority;
    }

    public boolean getRendererPriorityWaivedWhenNotVisible()
    {
        return mRendererWaivedWhenNotVisible;
    }

    public void setTextClassifier(TextClassifier textClassifier)
    {
        mTextClassifier = textClassifier;
    }

    public TextClassifier getTextClassifier()
    {
        return mTextClassifier == null ? TextClassifier.NO_OP : mTextClassifier;
    }

    public Looper getWebViewLooper()
    {
        return mWebViewLooper;
    }

    @Override
    public void onChildViewAdded(View parent, View child)
    {
    }

    @Override
    public void onChildViewRemoved(View parent, View child)
    {
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus)
    {
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return WebView.class.getName();
    }

    @Override
    public boolean onCheckIsTextEditor()
    {
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs)
    {
        return null;
    }

    public boolean onTrackballEvent(MotionEvent event)
    {
        return false;
    }

    public boolean isVisibleToUserForAutofill(int virtualId)
    {
        return isShown();
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY)
    {
    }

    @Override
    public void setBackgroundColor(int color)
    {
        mBackgroundColor = color;
        invalidate();
    }

    @Override
    public boolean shouldDelayChildPressedState()
    {
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if (Color.alpha(mBackgroundColor) != 0) canvas.drawColor(mBackgroundColor);
    }

    private void reportFindResult()
    {
        final FindListener listener = mFindListener;
        if (listener == null) return;
        postToWebViewThread(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onFindResultReceived(0, 0, true);
            }
        });
    }

    private void checkThread()
    {
        if (mWebViewLooper != null && Looper.myLooper() != mWebViewLooper)
        {
            throw new RuntimeException("A WebView method was called on thread '" + Thread.currentThread().getName() + "'. "
                + "All WebView methods must be called on the same thread. (Expected Looper " + mWebViewLooper + " called on "
                + Looper.myLooper() + ")");
        }
    }

    private void postToWebViewThread(Runnable action)
    {
        if (mHandler != null)
        {
            mHandler.post(action);
            return;
        }
        post(action);
    }

    private void navigate(final String url, final String originalUrl, final String inlineData, final String method,
        final Map<String, String> headers, final NavigationKind kind)
    {
        final int navigation = ++mNavigation;
        mProgress = 10;
        postToWebViewThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (navigation != mNavigation || mDestroyed) return;
                startNavigation(navigation, url, originalUrl, inlineData, method, headers, kind);
            }
        });
    }

    private void startNavigation(final int navigation, final String url, String originalUrl, String inlineData, String method,
        Map<String, String> headers, NavigationKind kind)
    {
        mUrl = url;
        mOriginalUrl = originalUrl;
        mCertificate = null;
        String title = displayTitle(url);
        if (kind == NavigationKind.NEW || kind == NavigationKind.INLINE)
        {
            mHistory.push(new LocalHistoryItem(url, originalUrl, title));
        }

        WebViewClient client = getWebViewClient();
        client.onPageStarted(this, url, null);
        if (navigation != mNavigation || mDestroyed) return;
        WebChromeClient chromeClient = mWebChromeClient;
        if (chromeClient != null) chromeClient.onProgressChanged(this, 10);

        LocalResourceRequest request = new LocalResourceRequest(Uri.parse(url), method, headers);
        LocalResourceError error = null;
        WebResourceResponse response = null;

        if (inlineData != null)
        {
            String extracted = extractTitle(inlineData);
            if (extracted != null) title = extracted;
        }
        else
        {
            if (kind != NavigationKind.INLINE) response = client.shouldInterceptRequest(this, request);
            if (response == null)
            {
                error = resolve(url, request);
            }
            else if (response.getStatusCode() >= 400)
            {
                client.onReceivedHttpError(this, request, response);
            }
        }

        if (navigation != mNavigation || mDestroyed) return;
        client.onLoadResource(this, url);
        if (error != null)
        {
            client.onReceivedError(this, request, error);
        }

        if (navigation != mNavigation || mDestroyed) return;
        LocalHistoryItem current = mHistory.getCurrentItem();
        if (current != null && url.equals(current.getUrl())) current.setTitle(title);
        mTitle = title;

        final String finalTitle = title;
        final boolean reload = kind == NavigationKind.RELOAD;
        postToWebViewThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (navigation != mNavigation || mDestroyed) return;
                finishNavigation(url, finalTitle, reload);
            }
        });
    }

    private void finishNavigation(String url, String title, boolean reload)
    {
        WebViewClient client = getWebViewClient();
        client.doUpdateVisitedHistory(this, url, reload);
        client.onPageCommitVisible(this, url);
        WebChromeClient chromeClient = mWebChromeClient;
        if (chromeClient != null)
        {
            chromeClient.onReceivedTitle(this, title);
            chromeClient.onProgressChanged(this, 100);
        }
        mProgress = 100;
        client.onPageFinished(this, url);
        PictureListener pictureListener = mPictureListener;
        if (pictureListener != null) pictureListener.onNewPicture(this, null);
        invalidate();
    }

    private LocalResourceError resolve(String url, WebResourceRequest request)
    {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.equals(ABOUT_BLANK) || URLUtil.isAboutUrl(url) || URLUtil.isDataUrl(url)) return null;
        if (URLUtil.isNetworkUrl(url))
        {
            if (mSettings.getBlockNetworkLoads())
            {
                return new LocalResourceError(WebViewClient.ERROR_UNKNOWN, "net::ERR_ACCESS_DENIED");
            }
            if (request.getUrl().getHost() == null || request.getUrl().getHost().isEmpty())
            {
                return new LocalResourceError(WebViewClient.ERROR_BAD_URL, "net::ERR_INVALID_URL");
            }
            return new LocalResourceError(WebViewClient.ERROR_HOST_LOOKUP, "net::ERR_INTERNET_DISCONNECTED");
        }
        if (URLUtil.isAssetUrl(url))
        {
            String path = Uri.decode(url.substring(URLUtil.ASSET_BASE.length()));
            int query = path.indexOf('?');
            if (query >= 0) path = path.substring(0, query);
            try (InputStream stream = getContext().getAssets().open(path))
            {
                return null;
            }
            catch (Exception exception)
            {
                return new LocalResourceError(WebViewClient.ERROR_FILE_NOT_FOUND, "net::ERR_FILE_NOT_FOUND");
            }
        }
        if (URLUtil.isResourceUrl(url)) return null;
        if (URLUtil.isFileUrl(url))
        {
            if (!mSettings.getAllowFileAccess())
            {
                return new LocalResourceError(WebViewClient.ERROR_UNKNOWN, "net::ERR_ACCESS_DENIED");
            }
            String path = request.getUrl().getPath();
            if (path == null || !new File(path).exists())
            {
                return new LocalResourceError(WebViewClient.ERROR_FILE_NOT_FOUND, "net::ERR_FILE_NOT_FOUND");
            }
            return null;
        }
        if (URLUtil.isContentUrl(url))
        {
            if (!mSettings.getAllowContentAccess())
            {
                return new LocalResourceError(WebViewClient.ERROR_UNKNOWN, "net::ERR_ACCESS_DENIED");
            }
            return null;
        }
        return new LocalResourceError(WebViewClient.ERROR_UNSUPPORTED_SCHEME, "net::ERR_UNKNOWN_URL_SCHEME");
    }

    private static String displayTitle(String url)
    {
        if (url == null) return null;
        if (URLUtil.isDataUrl(url)) return url.length() > 64 ? url.substring(0, 64) : url;
        int scheme = url.indexOf("://");
        return scheme > 0 ? url.substring(scheme + 3) : url;
    }

    private static String extractTitle(String html)
    {
        Matcher matcher = TITLE.matcher(html);
        if (!matcher.find()) return null;
        String title = matcher.group(1).trim();
        return title.isEmpty() ? null : title;
    }

    private enum NavigationKind
    {
        NEW,
        INLINE,
        RELOAD,
        HISTORY
    }

    private static final class LocalResourceRequest implements WebResourceRequest
    {
        private final Uri mUrl;
        private final String mMethod;
        private final Map<String, String> mHeaders;

        LocalResourceRequest(Uri url, String method, Map<String, String> headers)
        {
            mUrl = url;
            mMethod = method;
            mHeaders = headers == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(new HashMap<>(headers));
        }

        @Override
        public Uri getUrl()
        {
            return mUrl;
        }

        @Override
        public boolean isForMainFrame()
        {
            return true;
        }

        @Override
        public boolean isRedirect()
        {
            return false;
        }

        @Override
        public boolean hasGesture()
        {
            return false;
        }

        @Override
        public String getMethod()
        {
            return mMethod;
        }

        @Override
        public Map<String, String> getRequestHeaders()
        {
            return mHeaders;
        }
    }

    private static final class LocalResourceError extends WebResourceError
    {
        private final int mErrorCode;
        private final String mDescription;

        LocalResourceError(int errorCode, String description)
        {
            mErrorCode = errorCode;
            mDescription = description;
        }

        @Override
        public int getErrorCode()
        {
            return mErrorCode;
        }

        @Override
        public CharSequence getDescription()
        {
            return mDescription;
        }
    }

    public class WebViewTransport
    {
        private WebView mWebview;

        public WebViewTransport()
        {
        }

        public synchronized void setWebView(WebView webview)
        {
            mWebview = webview;
        }

        public synchronized WebView getWebView()
        {
            return mWebview;
        }
    }

    public static class HitTestResult
    {
        @Deprecated
        public static final int UNKNOWN_TYPE = 0;
        @Deprecated
        public static final int ANCHOR_TYPE = 1;
        public static final int PHONE_TYPE = 2;
        public static final int GEO_TYPE = 3;
        public static final int EMAIL_TYPE = 4;
        public static final int IMAGE_TYPE = 5;
        @Deprecated
        public static final int IMAGE_ANCHOR_TYPE = 6;
        public static final int SRC_ANCHOR_TYPE = 7;
        public static final int SRC_IMAGE_ANCHOR_TYPE = 8;
        public static final int EDIT_TEXT_TYPE = 9;

        private int mType;
        private String mExtra;

        public HitTestResult()
        {
            mType = UNKNOWN_TYPE;
        }

        public void setType(int type)
        {
            mType = type;
        }

        public void setExtra(String extra)
        {
            mExtra = extra;
        }

        public int getType()
        {
            return mType;
        }

        public String getExtra()
        {
            return mExtra;
        }
    }

    public interface FindListener
    {
        void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting);
    }

    public abstract static class VisualStateCallback
    {
        public VisualStateCallback()
        {
        }

        public abstract void onComplete(long requestId);
    }

    @Deprecated
    public interface PictureListener
    {
        @Deprecated
        void onNewPicture(WebView view, Picture picture);
    }

    public class PrivateAccess
    {
        public PrivateAccess()
        {
        }

        public int super_getScrollBarStyle()
        {
            return WebView.super.getScrollBarStyle();
        }

        public void super_scrollTo(int scrollX, int scrollY)
        {
            WebView.super.scrollTo(scrollX, scrollY);
        }

        public void super_computeScroll()
        {
            WebView.super.computeScroll();
        }

        public boolean super_onHoverEvent(MotionEvent event)
        {
            return WebView.super.onHoverEvent(event);
        }

        public boolean super_performLongClick()
        {
            return WebView.super.performLongClick();
        }

        public boolean super_dispatchKeyEvent(KeyEvent event)
        {
            return WebView.super.dispatchKeyEvent(event);
        }

        public boolean super_onGenericMotionEvent(MotionEvent event)
        {
            return WebView.super.onGenericMotionEvent(event);
        }

        public boolean super_requestFocus(int direction, Rect previouslyFocusedRect)
        {
            return WebView.super.requestFocus(direction, previouslyFocusedRect);
        }

        public void super_setLayoutParams(ViewGroup.LayoutParams params)
        {
            WebView.super.setLayoutParams(params);
        }

        public void setMeasuredDimension(int measuredWidth, int measuredHeight)
        {
            WebView.this.setMeasuredDimension(measuredWidth, measuredHeight);
        }

        public void onScrollChanged(int left, int top, int oldLeft, int oldTop)
        {
            WebView.this.onScrollChanged(left, top, oldLeft, oldTop);
        }
    }
}
