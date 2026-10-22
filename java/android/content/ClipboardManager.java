package android.content;

import java.util.ArrayList;
import java.util.List;

public class ClipboardManager extends android.text.ClipboardManager
{
    private ClipData mPrimary;
    private final List<OnPrimaryClipChangedListener> mListeners = new ArrayList<>();

    public interface OnPrimaryClipChangedListener
    {
        void onPrimaryClipChanged();
    }

    public void setPrimaryClip(ClipData clip)
    {
        mPrimary = clip;

        for (OnPrimaryClipChangedListener listener : new ArrayList<>(mListeners)) listener.onPrimaryClipChanged();
    }

    public ClipData getPrimaryClip()
    {
        return mPrimary;
    }

    public boolean hasPrimaryClip()
    {
        return mPrimary != null;
    }

    public ClipDescription getPrimaryClipDescription()
    {
        return mPrimary == null ? null : mPrimary.getDescription();
    }

    public void clearPrimaryClip()
    {
        setPrimaryClip(null);
    }

    public CharSequence getText()
    {
        if (mPrimary == null || mPrimary.getItemCount() == 0) return null;

        return mPrimary.getItemAt(0).getText();
    }

    public void setText(CharSequence text)
    {
        setPrimaryClip(ClipData.newPlainText("", text));
    }

    public boolean hasText()
    {
        CharSequence text = getText();
        return text != null && text.length() > 0;
    }

    public void addPrimaryClipChangedListener(OnPrimaryClipChangedListener listener)
    {
        if (listener != null && !mListeners.contains(listener)) mListeners.add(listener);
    }

    public void removePrimaryClipChangedListener(OnPrimaryClipChangedListener listener)
    {
        mListeners.remove(listener);
    }
}
