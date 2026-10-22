package android.webkit;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import java.util.Map;

public interface WebViewProvider
{
    void init(Map<String, Object> javaScriptInterfaces, boolean privateBrowsing);

    void setHorizontalScrollbarOverlay(boolean overlay);

    void setVerticalScrollbarOverlay(boolean overlay);

    boolean overlayHorizontalScrollbar();

    boolean overlayVerticalScrollbar();

    int getVisibleTitleHeight();

    SslCertificate getCertificate();

    void setCertificate(SslCertificate certificate);

    void savePassword(String host, String username, String password);

    void setHttpAuthUsernamePassword(String host, String realm, String username, String password);

    String[] getHttpAuthUsernamePassword(String host, String realm);

    void destroy();

    void setNetworkAvailable(boolean networkUp);

    WebBackForwardList saveState(Bundle outState);

    WebBackForwardList restoreState(Bundle inState);

    void loadUrl(String url, Map<String, String> additionalHttpHeaders);

    void loadUrl(String url);

    void postUrl(String url, byte[] postData);

    void loadData(String data, String mimeType, String encoding);

    void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl);

    void evaluateJavaScript(String script, ValueCallback<String> resultCallback);

    void saveWebArchive(String filename);

    void saveWebArchive(String basename, boolean autoname, ValueCallback<String> callback);

    void stopLoading();

    void reload();

    boolean canGoBack();

    void goBack();

    boolean canGoForward();

    void goForward();

    boolean canGoBackOrForward(int steps);

    void goBackOrForward(int steps);

    boolean isPrivateBrowsingEnabled();

    boolean pageUp(boolean top);

    boolean pageDown(boolean bottom);

    void insertVisualStateCallback(long requestId, WebView.VisualStateCallback callback);

    void clearView();

    Picture capturePicture();

    float getScale();

    void setInitialScale(int scaleInPercent);

    void invokeZoomPicker();

    WebView.HitTestResult getHitTestResult();

    void requestFocusNodeHref(Message hrefMsg);

    void requestImageRef(Message msg);

    String getUrl();

    String getOriginalUrl();

    String getTitle();

    Bitmap getFavicon();

    String getTouchIconUrl();

    int getProgress();

    int getContentHeight();

    int getContentWidth();

    void pauseTimers();

    void resumeTimers();

    void onPause();

    void onResume();

    boolean isPaused();

    void freeMemory();

    void clearCache(boolean includeDiskFiles);

    void clearFormData();

    void clearHistory();

    void clearSslPreferences();

    WebBackForwardList copyBackForwardList();

    void setFindListener(WebView.FindListener listener);

    void findNext(boolean forward);

    int findAll(String find);

    void findAllAsync(String find);

    boolean showFindDialog(String text, boolean showIme);

    void clearMatches();

    void documentHasImages(Message response);

    void setWebViewClient(WebViewClient client);

    WebViewClient getWebViewClient();

    void setDownloadListener(DownloadListener listener);

    void setWebChromeClient(WebChromeClient client);

    WebChromeClient getWebChromeClient();

    void setPictureListener(WebView.PictureListener listener);

    void addJavascriptInterface(Object object, String name);

    void removeJavascriptInterface(String interfaceName);

    WebMessagePort[] createWebMessageChannel();

    void postMessageToMainFrame(WebMessage message, Uri targetOrigin);

    WebSettings getSettings();

    void setMapTrackballToArrowKeys(boolean setMap);

    void flingScroll(int velocityX, int velocityY);

    View getZoomControls();

    boolean canZoomIn();

    boolean canZoomOut();

    boolean zoomBy(float zoomFactor);

    boolean zoomIn();

    boolean zoomOut();

    void dumpViewHierarchyWithProperties(java.io.BufferedWriter out, int level);

    View findHierarchyView(String className, int hashCode);

    void setRendererPriorityPolicy(int rendererRequestedPriority, boolean waivedWhenNotVisible);

    int getRendererRequestedPriority();

    boolean getRendererPriorityWaivedWhenNotVisible();

    ViewDelegate getViewDelegate();

    ScrollDelegate getScrollDelegate();

    void notifyFindDialogDismissed();

    interface ViewDelegate
    {
        boolean shouldDelayChildPressedState();

        AccessibilityNodeProvider getAccessibilityNodeProvider();

        void onProvideVirtualStructure(android.view.ViewStructure structure);

        boolean performAccessibilityAction(int action, Bundle arguments);

        void setOverScrollMode(int mode);

        void setScrollBarStyle(int style);

        void onDrawVerticalScrollBar(Canvas canvas, android.graphics.drawable.Drawable scrollBar, int left, int top,
            int right, int bottom);

        void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY);

        void onWindowVisibilityChanged(int visibility);

        void onDraw(Canvas canvas);

        void setLayoutParams(ViewGroup.LayoutParams layoutParams);

        boolean performLongClick();

        void onConfigurationChanged(Configuration newConfig);

        InputConnection onCreateInputConnection(EditorInfo outAttrs);

        boolean onDragEvent(android.view.DragEvent event);

        boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event);

        boolean onKeyDown(int keyCode, KeyEvent event);

        boolean onKeyUp(int keyCode, KeyEvent event);

        void onAttachedToWindow();

        void onDetachedFromWindow();

        void onMovedToDisplay(int displayId, Configuration config);

        void onVisibilityChanged(View changedView, int visibility);

        void onWindowFocusChanged(boolean hasWindowFocus);

        void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect);

        boolean setFrame(int left, int top, int right, int bottom);

        void onSizeChanged(int width, int height, int oldWidth, int oldHeight);

        void onScrollChanged(int left, int top, int oldLeft, int oldTop);

        boolean dispatchKeyEvent(KeyEvent event);

        boolean onTouchEvent(MotionEvent event);

        boolean onHoverEvent(MotionEvent event);

        boolean onGenericMotionEvent(MotionEvent event);

        boolean onTrackballEvent(MotionEvent event);

        boolean requestFocus(int direction, Rect previouslyFocusedRect);

        void onMeasure(int widthMeasureSpec, int heightMeasureSpec);

        boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate);

        void setBackgroundColor(int color);

        void setLayerType(int layerType, android.graphics.Paint paint);

        void preDispatchDraw(Canvas canvas);

        void onStartTemporaryDetach();

        void onFinishTemporaryDetach();

        void onActivityResult(int requestCode, int resultCode, android.content.Intent data);

        android.os.Handler getHandler(android.os.Handler originalHandler);

        View findFocus(View originalFocusedView);

        boolean onCheckIsTextEditor();
    }

    interface ScrollDelegate
    {
        int computeHorizontalScrollRange();

        int computeHorizontalScrollOffset();

        int computeVerticalScrollRange();

        int computeVerticalScrollOffset();

        int computeVerticalScrollExtent();

        void computeScroll();
    }
}
