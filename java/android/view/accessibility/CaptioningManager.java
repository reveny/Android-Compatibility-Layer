package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CaptioningManager
{
    private static final CaptioningManager INSTANCE = new CaptioningManager();

    private final List<CaptioningChangeListener> mListeners = new ArrayList<>();

    private CaptioningManager()
    {
    }

    public static CaptioningManager getInstance()
    {
        return INSTANCE;
    }

    public boolean isEnabled()
    {
        return false;
    }

    public Locale getLocale()
    {
        return null;
    }

    public float getFontScale()
    {
        return 1.0f;
    }

    public void addCaptioningChangeListener(CaptioningChangeListener listener)
    {
        if (listener == null || mListeners.contains(listener)) return;

        mListeners.add(listener);
    }

    public void removeCaptioningChangeListener(CaptioningChangeListener listener)
    {
        mListeners.remove(listener);
    }

    public abstract static class CaptioningChangeListener
    {
        public void onEnabledChanged(boolean enabled)
        {
        }

        public void onLocaleChanged(Locale locale)
        {
        }

        public void onFontScaleChanged(float fontScale)
        {
        }
    }
}
