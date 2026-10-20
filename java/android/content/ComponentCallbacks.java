package android.content;

public interface ComponentCallbacks
{
    void onConfigurationChanged(android.content.res.Configuration newConfiguration);

    void onLowMemory();
}
