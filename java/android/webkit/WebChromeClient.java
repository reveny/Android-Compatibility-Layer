package android.webkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.view.View;

public class WebChromeClient
{
    public WebChromeClient()
    {
    }

    public void onProgressChanged(WebView view, int newProgress)
    {
    }

    public void onReceivedTitle(WebView view, String title)
    {
    }

    public void onReceivedIcon(WebView view, Bitmap icon)
    {
    }

    public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed)
    {
    }

    public void onShowCustomView(View view, CustomViewCallback callback)
    {
    }

    @Deprecated
    public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback)
    {
    }

    public void onHideCustomView()
    {
    }

    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg)
    {
        return false;
    }

    public void onRequestFocus(WebView view)
    {
    }

    public void onCloseWindow(WebView window)
    {
    }

    public boolean onJsAlert(WebView view, String url, String message, JsResult result)
    {
        return false;
    }

    public boolean onJsConfirm(WebView view, String url, String message, JsResult result)
    {
        return false;
    }

    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result)
    {
        return false;
    }

    public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result)
    {
        return false;
    }

    @Deprecated
    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota, long estimatedDatabaseSize,
        long totalQuota, WebStorage.QuotaUpdater quotaUpdater)
    {
        if (quotaUpdater != null) quotaUpdater.updateQuota(quota);
    }

    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback)
    {
    }

    public void onGeolocationPermissionsHidePrompt()
    {
    }

    public void onPermissionRequest(PermissionRequest request)
    {
        if (request != null) request.deny();
    }

    public void onPermissionRequestCanceled(PermissionRequest request)
    {
    }

    @Deprecated
    public boolean onJsTimeout()
    {
        return true;
    }

    @Deprecated
    public void onConsoleMessage(String message, int lineNumber, String sourceID)
    {
    }

    public boolean onConsoleMessage(ConsoleMessage consoleMessage)
    {
        onConsoleMessage(consoleMessage.message(), consoleMessage.lineNumber(), consoleMessage.sourceId());
        return false;
    }

    public Bitmap getDefaultVideoPoster()
    {
        return null;
    }

    public View getVideoLoadingProgressView()
    {
        return null;
    }

    public void getVisitedHistory(ValueCallback<String[]> callback)
    {
    }

    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams)
    {
        return false;
    }

    public interface CustomViewCallback
    {
        void onCustomViewHidden();
    }

    public abstract static class FileChooserParams
    {
        public static final int MODE_OPEN = 0;
        public static final int MODE_OPEN_MULTIPLE = 1;
        public static final int MODE_SAVE = 3;

        public FileChooserParams()
        {
        }

        public static Uri[] parseResult(int resultCode, Intent data)
        {
            if (resultCode != Activity.RESULT_OK || data == null) return null;
            ClipData clipData = data.getClipData();
            if (clipData != null && clipData.getItemCount() > 0)
            {
                Uri[] uris = new Uri[clipData.getItemCount()];
                int count = 0;
                for (int index = 0; index < clipData.getItemCount(); index++)
                {
                    Uri uri = clipData.getItemAt(index).getUri();
                    if (uri != null) uris[count++] = uri;
                }
                if (count > 0)
                {
                    Uri[] result = new Uri[count];
                    System.arraycopy(uris, 0, result, 0, count);
                    return result;
                }
            }
            Uri uri = data.getData();
            return uri == null ? null : new Uri[] {uri};
        }

        public abstract int getMode();

        public abstract String[] getAcceptTypes();

        public abstract boolean isCaptureEnabled();

        public abstract CharSequence getTitle();

        public abstract String getFilenameHint();

        public abstract Intent createIntent();
    }
}
