package android.view.accessibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CaptioningManager
{
    private static final CaptioningManager INSTANCE = new CaptioningManager();

    private final List<CaptioningChangeListener> mListeners = new ArrayList<>();

    CaptioningManager()
    {
    }

    public boolean isCallCaptioningEnabled()
    {
        return false;
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

    public CaptionStyle getUserStyle()
    {
        return CaptionStyle.DEFAULT;
    }

    public boolean isSystemAudioCaptioningEnabled()
    {
        return false;
    }

    public boolean isSystemAudioCaptioningUiEnabled()
    {
        return false;
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

        public void onUserStyleChanged(CaptionStyle userStyle)
        {
        }

        public void onSystemAudioCaptioningChanged(boolean enabled)
        {
        }

        public void onSystemAudioCaptioningUiChanged(boolean enabled)
        {
        }
    }

    public static final class CaptionStyle
    {
        public static final int EDGE_TYPE_UNSPECIFIED = -1;
        public static final int EDGE_TYPE_NONE = 0;
        public static final int EDGE_TYPE_OUTLINE = 1;
        public static final int EDGE_TYPE_DROP_SHADOW = 2;
        public static final int EDGE_TYPE_RAISED = 3;
        public static final int EDGE_TYPE_DEPRESSED = 4;

        private static final int COLOR_UNSPECIFIED = 0x00ffffff;

        static final CaptionStyle DEFAULT = new CaptionStyle(0xffffffff, 0xff000000, EDGE_TYPE_NONE, 0xff000000, COLOR_UNSPECIFIED);

        public final int foregroundColor;
        public final int backgroundColor;
        public final int edgeType;
        public final int edgeColor;
        public final int windowColor;

        private CaptionStyle(int foreground, int background, int edge, int edgeColorValue, int window)
        {
            foregroundColor = foreground;
            backgroundColor = background;
            edgeType = edge;
            edgeColor = edgeColorValue;
            windowColor = window;
        }

        public boolean hasBackgroundColor()
        {
            return backgroundColor != COLOR_UNSPECIFIED;
        }

        public boolean hasForegroundColor()
        {
            return foregroundColor != COLOR_UNSPECIFIED;
        }

        public boolean hasEdgeType()
        {
            return edgeType != EDGE_TYPE_UNSPECIFIED;
        }

        public boolean hasEdgeColor()
        {
            return edgeColor != COLOR_UNSPECIFIED;
        }

        public boolean hasWindowColor()
        {
            return windowColor != COLOR_UNSPECIFIED;
        }

        public android.graphics.Typeface getTypeface()
        {
            return null;
        }
    }
}
