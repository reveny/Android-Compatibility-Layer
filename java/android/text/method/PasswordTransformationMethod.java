package android.text.method;

import android.graphics.Rect;
import android.view.View;

public class PasswordTransformationMethod implements TransformationMethod
{
    private static final char DOT = '•';
    private static PasswordTransformationMethod sInstance;

    public static PasswordTransformationMethod getInstance()
    {
        if (sInstance == null) sInstance = new PasswordTransformationMethod();
        return sInstance;
    }

    @Override
    public CharSequence getTransformation(CharSequence source, View view)
    {
        if (source == null) return null;

        StringBuilder masked = new StringBuilder(source.length());
        for (int index = 0; index < source.length(); index++)
        {
            masked.append(DOT);
        }
        return masked;
    }

    @Override
    public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect)
    {
    }
}
