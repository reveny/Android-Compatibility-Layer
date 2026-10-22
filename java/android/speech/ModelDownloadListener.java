package android.speech;

public interface ModelDownloadListener
{
    void onError(int error);

    void onProgress(int completedPercent);

    void onScheduled();

    void onSuccess();
}
