package android.text;

public abstract class ClipboardManager
{
    public ClipboardManager()
    {
    }

    public abstract CharSequence getText();

    public abstract void setText(CharSequence text);

    public abstract boolean hasText();
}
