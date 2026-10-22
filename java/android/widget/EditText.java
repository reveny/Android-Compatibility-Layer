package android.widget;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class EditText extends TextView
{
    private boolean mStyleShortcutsEnabled;

    public EditText(Context context)
    {
        this(context, null);
    }

    public EditText(Context context, AttributeSet attributes)
    {
        this(context, attributes, attribute(context, "editTextStyle"));
    }

    public EditText(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public EditText(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);
    }

    private static int attribute(Context context, String name)
    {
        if (context == null) return 0;

        return context.getResources().getIdentifier(name, "attr", "android");
    }

    @Override
    public boolean getFreezesText()
    {
        return true;
    }

    @Override
    protected boolean getDefaultEditable()
    {
        return true;
    }

    @Override
    protected MovementMethod getDefaultMovementMethod()
    {
        return ArrowKeyMovementMethod.getInstance();
    }

    @Override
    public Editable getText()
    {
        CharSequence text = super.getText();
        if (text == null) return null;
        if (text instanceof Editable) return (Editable)text;

        super.setText(text, BufferType.EDITABLE);
        return (Editable)super.getText();
    }

    @Override
    public void setText(CharSequence text, BufferType type)
    {
        super.setText(text, BufferType.EDITABLE);
    }

    public void setSelection(int start, int stop)
    {
        Selection.setSelection(getText(), start, stop);
    }

    public void setSelection(int index)
    {
        Selection.setSelection(getText(), index);
    }

    public void selectAll()
    {
        Selection.selectAll(getText());
    }

    public void extendSelection(int index)
    {
        Selection.extendSelection(getText(), index);
    }

    @Override
    public void setEllipsize(TextUtils.TruncateAt ellipsis)
    {
        if (ellipsis == TextUtils.TruncateAt.MARQUEE)
        {
            throw new IllegalArgumentException("EditText cannot use the ellipsize mode TextUtils.TruncateAt.MARQUEE");
        }

        super.setEllipsize(ellipsis);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return EditText.class.getName();
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event)
    {
        return super.onKeyShortcut(keyCode, event);
    }

    @Override
    public boolean onTextContextMenuItem(int id)
    {
        return super.onTextContextMenuItem(id);
    }

    public void setStyleShortcutsEnabled(boolean enabled)
    {
        mStyleShortcutsEnabled = enabled;
    }

    public boolean isStyleShortcutEnabled()
    {
        return mStyleShortcutsEnabled;
    }
}
